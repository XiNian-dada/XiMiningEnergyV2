package cn.hairuosky.ximiningenergy;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.sql.*;
import java.util.UUID;


public class DatabaseManager {

    private static HikariDataSource dataSource;
    private final Plugin plugin;
    private Connection sqliteConnection;
    private boolean useMySQL = true; // 默认使用 MySQL
    private static final int TEST_COUNT = 5;

    public DatabaseManager(Plugin plugin) {
        this.plugin = plugin;
        initialize(); // 确保初始化连接池只执行一次
    }

    synchronized void initialize() {
        FileConfiguration config = plugin.getConfig();
        int dbType = config.getInt("database.type", 1); // 从配置中获取数据库类型，1 为 MySQL，2 为 SQLite

        if (dbType == 1) {
            useMySQL = true;
            initializeMySQL();
        } else if (dbType == 2) {
            useMySQL = false;
            initializeSQLite();
        } else {
            plugin.getLogger().severe("无效的数据库类型配置，请在配置文件中设置 database.type 为 1 (MySQL) 或 2 (SQLite)！");
        }
    }

    private void initializeMySQL() {
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
        plugin.getLogger().info(XiMiningEnergy.getRawMessageStatic("database-connect"));

        createTableMySQL();
    }

    private void initializeSQLite() {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }

            File dbFile = new File(dataFolder, "player_data.db");
            if (!dbFile.exists()) {
                dbFile.createNewFile(); // 确保数据库文件存在
            }

            sqliteConnection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            plugin.getLogger().info("SQLite 数据库连接成功");

            createTableSQLite();
        } catch (SQLException e) {
            plugin.getLogger().severe("无法连接到 SQLite 数据库！");
            e.printStackTrace();
        } catch (java.io.IOException e) {
            plugin.getLogger().severe("无法创建 SQLite 数据库文件！");
            e.printStackTrace();
        }
    }

    private void createTableMySQL() {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS player_energy (" +
                "uuid VARCHAR(36) PRIMARY KEY, " +
                "current_energy DOUBLE, " +
                "max_energy DOUBLE, " +
                "regen_rate DOUBLE, " +
                "last_online_timestamp BIGINT, " +
                "game_id VARCHAR(255) NOT NULL)";
        try (Connection connection = getMySQLConnection();
             PreparedStatement statement = connection.prepareStatement(createTableSQL)) {
            statement.executeUpdate();
            plugin.getLogger().info(XiMiningEnergy.getRawMessageStatic("database-check"));
        } catch (SQLException e) {
            plugin.getLogger().severe("无法创建或检查 MySQL 数据库表！");
            e.printStackTrace();
        }
    }

    private void createTableSQLite() {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS player_energy (" +
                "uuid TEXT PRIMARY KEY, " +
                "current_energy REAL, " +
                "max_energy REAL, " +
                "regen_rate REAL, " +
                "last_online_timestamp INTEGER, " +
                "game_id TEXT NOT NULL)";
        try (Connection connection = getSQLiteConnection();
             PreparedStatement statement = connection.prepareStatement(createTableSQL)) {
            statement.executeUpdate();
            plugin.getLogger().info(XiMiningEnergy.getRawMessageStatic("database-check"));
        } catch (SQLException e) {
            plugin.getLogger().severe("无法创建或检查 SQLite 数据库表！");
            e.printStackTrace();
        }
    }

    private Connection getMySQLConnection() throws SQLException {
        if (dataSource != null) {
            return dataSource.getConnection();
        }
        throw new SQLException("MySQL 数据库连接池未初始化！");
    }

    private Connection getSQLiteConnection() throws SQLException {
        if (sqliteConnection != null && !sqliteConnection.isClosed()) {
            return sqliteConnection;
        }

        // 连接未初始化或已关闭，尝试重新建立连接
        try {
            if (sqliteConnection != null) {
                // 如果连接已经存在，先关闭它
                sqliteConnection.close();
            }

            // 重新建立数据库连接
            Class.forName("org.sqlite.JDBC");
            File dbFile = new File(plugin.getDataFolder(), "player_data.db");
            sqliteConnection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
        } catch (ClassNotFoundException | SQLException e) {
            // 记录错误信息，并重新抛出异常
            plugin.getLogger().severe("Failed to reconnect to SQLite database: " + e.getMessage());
            throw new SQLException("Failed to reconnect to SQLite database", e);
        }

        return sqliteConnection;
    }

    private Connection getConnection() throws SQLException {
        if (useMySQL) {
            plugin.getLogger().info("使用 MySQL 连接");
            return getMySQLConnection();
        } else {
            plugin.getLogger().info("使用 SQLite 连接");
            return getSQLiteConnection();
        }
    }

    private PlayerEnergyData createDefaultPlayerData(UUID uuid) {
        Player player = plugin.getServer().getPlayer(uuid);
        String gameId = (player != null) ? player.getName() : "unknownGameId";
        FileConfiguration config = plugin.getConfig();
        double defaultMaxEnergy = config.getDouble("default-max-energy");
        double defaultRegenRate = config.getDouble("default-regen-rate");
        XiMiningEnergy.debugModePrintStatic("info", "玩家数据不存在，使用默认值: " + uuid.toString());
        return new PlayerEnergyData(defaultMaxEnergy, defaultMaxEnergy, defaultRegenRate, 0, gameId);
    }

    public void updatePlayerData(UUID uuid, PlayerEnergyData data) {
        XiMiningEnergy.debugModePrintStatic("info", "更新玩家数据: " + uuid.toString());
        String sql;
        if (useMySQL) {
            sql = "REPLACE INTO player_energy (uuid, current_energy, max_energy, regen_rate, last_online_timestamp, game_id) VALUES (?, ?, ?, ?, ?, ?)";
        } else {
            sql = "INSERT OR REPLACE INTO player_energy (uuid, current_energy, max_energy, regen_rate, last_online_timestamp, game_id) VALUES (?, ?, ?, ?, ?, ?)";
        }

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setDouble(2, data.getCurrentEnergy());
            statement.setDouble(3, data.getMaxEnergy());
            statement.setDouble(4, data.getRegenRate());
            statement.setLong(5, data.getLastOnlineTimestamp());
            statement.setString(6, data.getGameId());
            int rowsAffected = statement.executeUpdate();
            XiMiningEnergy.debugModePrintStatic("info", "玩家数据更新成功: " + uuid.toString() + ", 受影响的行数: " + rowsAffected);
        } catch (SQLException e) {
            XiMiningEnergy.debugModePrintStatic("severe", "更新玩家数据失败: " + uuid.toString());
            e.printStackTrace();
        }
    }

    public PlayerEnergyData loadPlayerData(UUID uuid) {
        XiMiningEnergy.debugModePrintStatic("info", "加载玩家数据: " + uuid.toString());
        String sql = "SELECT * FROM player_energy WHERE uuid = ?";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                double currentEnergy = resultSet.getDouble("current_energy");
                double maxEnergy = resultSet.getDouble("max_energy");
                double regenRate = resultSet.getDouble("regen_rate");
                long lastOnlineTimestamp = resultSet.getLong("last_online_timestamp");
                String gameId = resultSet.getString("game_id");
                return new PlayerEnergyData(currentEnergy, maxEnergy, regenRate, lastOnlineTimestamp, gameId);
            } else {
                // 数据库中没有记录，创建默认数据
                return createDefaultPlayerData(uuid);
            }
        } catch (SQLException e) {
            XiMiningEnergy.debugModePrintStatic("severe", "加载玩家数据失败: " + uuid.toString());
            e.printStackTrace();
            // 返回默认数据以避免空指针异常
            return createDefaultPlayerData(uuid);
        }
    }


    public boolean checkDatabaseConnection() {
        XiMiningEnergy.debugModePrintStatic("info", "检查数据库连接");
        try (Connection connection = getConnection()) {
            if (connection != null && !connection.isClosed()) {
                plugin.getLogger().info("数据库连接正常");
                return true;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("数据库连接检查失败！");
            e.printStackTrace();
        }
        return false;
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
        if (sqliteConnection != null) {
            try {
                sqliteConnection.close();
            } catch (SQLException e) {
                plugin.getLogger().severe("关闭 SQLite 数据库连接时发生错误！");
                e.printStackTrace();
            }
        }
    }




    public long getConnectionDelay() {
        if (dataSource == null) {
            XiMiningEnergy.debugModePrintStatic("severe",XiMiningEnergy.getRawMessageStatic("database-not-initialized"));
            ///plugin.getLogger().severe("数据库连接池未初始化！");
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
            XiMiningEnergy.debugModePrintStatic("info","数据库连接延迟: " + (averageDelay / 1_000_000) + " ms"); // 转换为毫秒
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
