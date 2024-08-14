package cn.hairuosky.ximiningenergy;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.sql.*;
import java.util.UUID;

public class DatabaseManager {

    private static HikariDataSource dataSource;
    private final Plugin plugin;
    private static final int TEST_COUNT = 5;

    public DatabaseManager(Plugin plugin) {
        this.plugin = plugin;
        initialize(); // 确保初始化连接池只执行一次
    }

    synchronized void initialize() {
        if (dataSource != null && !dataSource.isClosed()) {
            return; // 连接池已经存在且未关闭，直接返回
        }

        FileConfiguration config = plugin.getConfig();
        String host = config.getString("mysql.host");
        int port = config.getInt("mysql.port");
        String database = config.getString("mysql.database");
        String username = config.getString("mysql.username");
        String password = config.getString("mysql.password");

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true");
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setConnectionTimeout(30000);
        hikariConfig.setIdleTimeout(600000);
        hikariConfig.setMaxLifetime(1800000);

        dataSource = new HikariDataSource(hikariConfig);
        plugin.getLogger().info("成功连接到数据库！");

        // 创建表
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "CREATE TABLE IF NOT EXISTS player_energy (" +
                             "uuid VARCHAR(36) PRIMARY KEY, " +
                             "current_energy DOUBLE, " +
                             "max_energy DOUBLE, " +
                             "regen_rate DOUBLE, " +
                             "last_online_timestamp BIGINT, " +
                             "game_id VARCHAR(255) NOT NULL)")) {
            statement.executeUpdate();
            plugin.getLogger().info("数据库表 `player_energy` 已创建或确认存在。");
        } catch (SQLException e) {
            plugin.getLogger().severe("无法连接到数据库！");
            e.printStackTrace();
        }
    }

    public PlayerEnergyData loadPlayerData(UUID uuid) {
        plugin.getLogger().info("加载玩家数据: " + uuid.toString());
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT current_energy, max_energy, regen_rate, last_online_timestamp, game_id FROM player_energy WHERE uuid = ?")) {
            statement.setString(1, uuid.toString());
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                double currentEnergy = rs.getDouble("current_energy");
                double maxEnergy = rs.getDouble("max_energy");
                double regenRate = rs.getDouble("regen_rate");
                long lastOnlineTimestamp = rs.getLong("last_online_timestamp");
                String gameId = rs.getString("game_id");
                plugin.getLogger().info("玩家数据加载成功: " + uuid.toString());
                return new PlayerEnergyData(currentEnergy, maxEnergy, regenRate, lastOnlineTimestamp, gameId);
            } else {
                Player player = plugin.getServer().getPlayer(uuid);
                String gameId = (player != null) ? player.getName() : "unknownGameId";
                FileConfiguration config = plugin.getConfig();
                double defaultMaxEnergy = config.getDouble("default-max-energy");
                double defaultRegenRate = config.getDouble("default-regen-rate");
                plugin.getLogger().info("玩家数据不存在，使用默认值: " + uuid.toString());
                return new PlayerEnergyData(defaultMaxEnergy, defaultMaxEnergy, defaultRegenRate, 0, gameId);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("加载玩家数据失败: " + uuid.toString());
            e.printStackTrace();
        }
        return null;
    }

    public void updatePlayerData(UUID uuid, PlayerEnergyData data) {
        plugin.getLogger().info("更新玩家数据: " + uuid.toString());
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "REPLACE INTO player_energy (uuid, current_energy, max_energy, regen_rate, last_online_timestamp, game_id) VALUES (?, ?, ?, ?, ?, ?)")) {
            statement.setString(1, uuid.toString());
            statement.setDouble(2, data.getCurrentEnergy());
            statement.setDouble(3, data.getMaxEnergy());
            statement.setDouble(4, data.getRegenRate());
            statement.setLong(5, data.getLastOnlineTimestamp());
            statement.setString(6, data.getGameId());
            int rowsAffected = statement.executeUpdate();
            plugin.getLogger().info("玩家数据更新成功: " + uuid.toString() + ", 受影响的行数: " + rowsAffected);
        } catch (SQLException e) {
            plugin.getLogger().severe("更新玩家数据失败: " + uuid.toString());
            e.printStackTrace();
        }
    }

    public void close() {
        plugin.getLogger().info("关闭数据库连接...");
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("数据库连接池已关闭。");
        }
    }

    public long getConnectionDelay() {
        if (dataSource == null) {
            plugin.getLogger().severe("数据库连接池未初始化！");
            return -1;
        }

        long totalDelay = 0;
        int count = 0;

        for (int i = 0; i < TEST_COUNT; i++) {
            long startTime = System.nanoTime();
            try (Connection connection = dataSource.getConnection()) {
                if (connection != null && !connection.isClosed()) {
                    try (Statement statement = connection.createStatement()) {
                        statement.execute("SELECT 1");
                        long endTime = System.nanoTime();
                        totalDelay += (endTime - startTime);
                        count++;
                    } catch (SQLException e) {
                        plugin.getLogger().severe("查询执行失败！");
                        e.printStackTrace();
                    }
                } else {
                    plugin.getLogger().severe("连接对象不可用！");
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("获取数据库连接失败！");
                e.printStackTrace();
            }
        }

        if (count > 0) {
            long averageDelay = totalDelay / count;
            plugin.getLogger().info("数据库连接延迟: " + (averageDelay / 1_000_000) + " ms"); // 转换为毫秒
            return averageDelay / 1_000_000; // 转换为毫秒
        }

        return -1;
    }

    public long getDatabaseUsage() {
        if (dataSource == null) {
            plugin.getLogger().severe("数据库连接池未初始化！");
            return -1;
        }

        String databaseName = plugin.getConfig().getString("mysql.database");
        if (databaseName == null || databaseName.isEmpty()) {
            plugin.getLogger().severe("数据库名称未配置！");
            return -1;
        }

        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "SELECT ROUND(SUM(data_length + index_length) / 1024 / 1024, 2) AS size_mb " +
                             "FROM information_schema.tables WHERE table_schema = ?"
             )) {
            ps.setString(1, databaseName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long sizeMb = rs.getLong("size_mb");
                    //plugin.getLogger().info("数据库大小: " + sizeMb + " MB");
                    return sizeMb; // 返回数据库大小
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("获取数据库大小失败！");
            e.printStackTrace();
        }
        return -1; // 返回 -1 代表获取失败
    }

}
