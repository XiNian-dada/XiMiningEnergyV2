package cn.hairuosky.ximiningenergy;

import dev.lone.itemsadder.api.CustomBlock;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import cn.hairuosky.ximiningenergy.api.XiEnergyAPI;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class XiMiningEnergy extends JavaPlugin implements Listener,XiEnergyAPI{
    Economy economy;
    public Potion potionHandler;
    private final Map<UUID, Long> cooldownMap = new HashMap<>();
    final long cooldownTime = 5000; // 冷却时间，单位：毫秒
    DatabaseManager databaseManager;
    HashMap<UUID, PlayerEnergyData> playerDataCache;
    private boolean applyToAllBlocks;
    private BossBarManager bossBarManager;
    boolean itemsAdderEnabled;
    boolean debugModeSwitch = true;
    private Map<String, Double> itemsAdderEnergyConsumption;
    private class EnergyRegenTask extends BukkitRunnable {
        @Override
        public void run() {
            for (UUID uuid : playerDataCache.keySet()) {
                PlayerEnergyData data = playerDataCache.get(uuid);
                if (data != null) {
                    double currentEnergy = data.getCurrentEnergy();
                    double maxEnergy = data.getMaxEnergy();
                    double regenRate = data.getRegenRate();

                    if (currentEnergy < maxEnergy) {
                        double newEnergy = Math.min(currentEnergy + regenRate, maxEnergy);
                        data.setCurrentEnergy(newEnergy);

                        // 输出日志以确认恢复过程
                        debugModePrint("info","玩家 " + Objects.requireNonNull(getServer().getPlayer(uuid)).getName() + " 的体力恢复至: " + newEnergy);
                        //getLogger().info("玩家 " + getServer().getPlayer(uuid).getName() + " 的体力恢复至: " + newEnergy);
                        // 更新 BossBar 显示
                        onPlayerEnergyUpdate(Objects.requireNonNull(getServer().getPlayer(uuid)).getPlayer(), data.getCurrentEnergy(), data.getMaxEnergy());
                    }
                }
            }
        }
    }
    private class AutoSavePlayerData extends BukkitRunnable{
        @Override
        public void run(){
            saveAllPlayerDataToDatabase();
            Bukkit.broadcastMessage("玩家数据已自动存储");
        }
    }
    @Override
    public void onEnable() {
        getLogger().info("插件启动中...");
        try {
            // 加载配置文件
            saveDefaultConfig();
            applyToAllBlocks = getConfig().getBoolean("apply-energy-consumption-to-all-blocks", false);
            if (applyToAllBlocks) {
                getLogger().info("全方块减体力 已启用");
            } else {
                getLogger().info("全方块减体力 已关闭");
            }
            potionHandler = new Potion(bossBarManager, this);
            getLogger().info("配置文件加载完成。");
            itemsAdderEnabled = getConfig().getBoolean("itemsadder.enabled", false);

            // 检查 PlaceholderAPI 插件是否存在
            if (getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
                getLogger().severe("PlaceholderAPI 插件未找到！插件将停用。");
                getServer().getPluginManager().disablePlugin(this);
                return;
            } else {
                getLogger().info("PlaceholderAPI 插件已找到！");
                new XiMiningEnergyPlaceholder(this).register();
                getLogger().info("PlaceholderAPI 成功注册！");
            }

            // 检查 Vault 插件是否存在
            if (!setupEconomy()) {
                getLogger().severe("Vault 插件未找到！插件将停用。");
                getServer().getPluginManager().disablePlugin(this);
                return;
            } else {
                getLogger().info("Vault 插件已找到！");
            }

            if (itemsAdderEnabled) {
                if (getServer().getPluginManager().getPlugin("ItemsAdder") == null) {
                    getLogger().severe("ItemsAdder 支持已启用，但未检测到 ItemsAdder 插件！插件将停用。");
                    getServer().getPluginManager().disablePlugin(this); // 停用插件
                    return;
                } else {
                    getLogger().info("检测到 ItemsAdder 插件，正在加载自定义方块配置...");
                    // 读取 ItemsAdder 自定义方块配置
                    itemsAdderEnergyConsumption = new HashMap<>();
                    ConfigurationSection itemsAdderSection = getConfig().getConfigurationSection("itemsadder.energy-consumption");
                    if (itemsAdderSection != null) {
                        for (String key : itemsAdderSection.getKeys(false)) {
                            double consumption = itemsAdderSection.getDouble(key);
                            itemsAdderEnergyConsumption.put(key, consumption);
                        }
                    }
                }
            } else {
                itemsAdderEnergyConsumption = new HashMap<>();
            }
            // 打印配置的方块及其能量消耗
            printEnergyConsumptionConfig();
            // 初始化数据库管理器
            databaseManager = new DatabaseManager(this);
            databaseManager.initialize();
            getLogger().info("数据库管理器初始化完成。");

            // 初始化玩家数据缓存
            playerDataCache = new HashMap<>();

            getLogger().info("玩家数据缓存初始化完成。");

            // 注册事件
            getServer().getPluginManager().registerEvents(this, this);
            getServer().getPluginManager().registerEvents(new MiningEnergyCommandExecutor(this), this);
            getServer().getPluginManager().registerEvents(new Potion(bossBarManager, this), this);
            // 初始化 BossBar 管理器
            bossBarManager = new BossBarManager(getConfig(), this);
            getLogger().info("事件监听器注册完成。");
            // 注册 EnergyAPI
            getServer().getServicesManager().register(XiEnergyAPI.class, this, this, ServicePriority.Normal);
            // 注册指令
            Objects.requireNonNull(getCommand("miningenergy")).setTabCompleter(new MiningEnergyTabCompleter(getConfig()));
            if (this.getCommand("miningenergy") == null) {
                getLogger().severe("无法注册 'miningenergy' 指令，因为它没有在 plugin.yml 文件中定义。");
                return; // 如果命令无法注册，退出插件启用过程
            }
            Objects.requireNonNull(this.getCommand("miningenergy")).setExecutor(new MiningEnergyCommandExecutor(this));
            getLogger().info("指令执行器注册完成。");
            new EnergyRegenTask().runTaskTimer(this, 0L, 1200L);
            startCooldownCleanupTask();
            if (getConfig().getBoolean("auto-save")){
                new AutoSavePlayerData().runTaskTimer(this,0L,getConfig().getInt("auto-save-delay",600)*20L);
            }
            getLogger().info("XiMiningEnergy 插件已启用！");
        } catch (Exception e) {
            getLogger().severe("插件启用过程中发生错误！");
            e.printStackTrace();
        }


    }




    @Override
    public void onDisable() {
        // 保存所有玩家的数据到数据库
        saveAllPlayerDataToDatabase();

        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("XiMiningEnergy 插件已禁用！");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        // 如果缓存中没有玩家数据，从数据库中加载
        if (!playerDataCache.containsKey(playerUUID)) {
            PlayerEnergyData data = databaseManager.loadPlayerData(playerUUID);

            if (data == null) {
                // 如果数据库中也没有记录，使用默认值
                data = new PlayerEnergyData(
                        getDefaultMaxEnergy(),          // currentEnergy
                        getDefaultMaxEnergy(),          // maxEnergy
                        getDefaultRegenRate(),          // regenRate
                        System.currentTimeMillis(),     // lastOnlineTimestamp
                        player.getName()               // gameId
                );

                // 输出默认数据
                getLogger().info("玩家 " + player.getName() + " 的数据在数据库中未找到，使用默认值:");
            } else {
                // 输出从数据库加载的数据
                getLogger().info("玩家 " + player.getName() + " 的数据从数据库加载:");
            }

            printPlayerDataFromDatabase(playerUUID, data);

            playerDataCache.put(playerUUID, data);
            player.sendMessage("玩家数据已加载或创建。");
        }
    }

    private void printPlayerDataFromDatabase(UUID playerUUID, PlayerEnergyData data) {
        getLogger().info("玩家UUID: " + playerUUID.toString());
        getLogger().info("当前体力: " + data.getCurrentEnergy());
        getLogger().info("最大体力: " + data.getMaxEnergy());
        getLogger().info("恢复速率: " + data.getRegenRate());
        getLogger().info("最后一次下线时间戳: " + data.getLastOnlineTimestamp());
        getLogger().info("游戏ID: " + data.getGameId());
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        PlayerEnergyData data = playerDataCache.get(playerUUID);
        if (data != null) {
            double currentEnergy = data.getCurrentEnergy();
            double energyConsumption = 1.0;  // 默认能量消耗值

            Block block = event.getBlock();

            // 检查是否启用了 ItemsAdder 支持
            if (itemsAdderEnabled) {
                CustomBlock customBlock = CustomBlock.byAlreadyPlaced(block);
                if (customBlock != null) {
                    String customBlockID = customBlock.getNamespace() + ":" + customBlock.getId();
                    getLogger().info("已检测到 ItemsAdder 自定义方块: " + customBlockID);

                    // 从配置中获取该自定义方块的能量消耗
                    energyConsumption = itemsAdderEnergyConsumption.getOrDefault(customBlockID, 1.0);
                    getLogger().info("ItemsAdder 自定义方块能量消耗: " + energyConsumption);
                } else {
                    getLogger().info("未检测到 ItemsAdder 自定义方块，方块类型为: " + block.getType());
                }
            }

            // 如果不是 ItemsAdder 方块，应用普通方块的能量消耗逻辑
            if (!itemsAdderEnabled || CustomBlock.byAlreadyPlaced(block) == null) {
                if (applyToAllBlocks) {
                    // 对所有方块应用能量消耗逻辑
                    energyConsumption = calculateEnergyConsumption(block);
                    if (energyConsumption == 0.0) {
                        energyConsumption = 1.0;  // 方块不在配置中，使用默认消耗值
                    }
                } else {
                    // 只处理配置中的方块
                    energyConsumption = calculateEnergyConsumption(block);
                }
            }

            // 输出调试信息
            getLogger().info("玩家 " + player.getName() + " 正在挖掘方块: " + block.getType());
            getLogger().info("方块能量消耗: " + energyConsumption);
            getLogger().info("玩家当前体力: " + currentEnergy);

            if (currentEnergy >= energyConsumption) {
                // 扣除体力
                data.setCurrentEnergy(currentEnergy - energyConsumption);
                getLogger().info("体力已扣除: " + energyConsumption);
                getLogger().info("玩家剩余体力: " + data.getCurrentEnergy());
                // 更新 BossBar 显示
                onPlayerEnergyUpdate(player, data.getCurrentEnergy(), data.getMaxEnergy());
            } else {
                // 体力不足，取消挖掘事件
                event.setCancelled(true);
                player.sendMessage("你太累了，无法继续挖掘！");
                getLogger().info("体力不足，取消挖掘事件。");
                // 更新 BossBar 显示
                onPlayerEnergyUpdate(player, data.getCurrentEnergy(), data.getMaxEnergy());
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        // 从缓存中获取玩家数据
        PlayerEnergyData data = playerDataCache.get(playerUUID);
        BossBar bossBar = bossBarManager.getPlayerBossBar(player);
        if (bossBar != null) {
            bossBar.removePlayer(player);
            bossBarManager.removePlayerBossBar(player);
        }
        if (data != null) {
            // 更新最后一次下线时间戳
            data.setLastOnlineTimestamp(System.currentTimeMillis());

            // 输出所有要保存的值
            getLogger().info("玩家数据将被保存到数据库:");
            getLogger().info("玩家名称: " + player.getName());
            printPlayerDataFromDatabase(playerUUID, data);

            // 保存玩家数据到数据库
            savePlayerDataToDatabase(playerUUID);
            // 从缓存中移除
            playerDataCache.remove(playerUUID);
            getLogger().info("玩家数据已保存到数据库: " + player.getName());
        }
    }

    private double calculateEnergyConsumption(Block block) {
        String blockType = block.getType().toString(); // 获取方块类型的字符串表示
        return getConfig().getDouble("energy-consumption." + blockType, 0.0); // 默认值为 0.0
    }

    private double getDefaultMaxEnergy() {
        return getConfig().getDouble("default-max-energy", 100.0);
    }

    private double getDefaultRegenRate() {
        return getConfig().getDouble("default-regen-rate", 1.0);
    }

    public void savePlayerDataToDatabase(UUID uuid) {
        PlayerEnergyData data = playerDataCache.get(uuid);
        if (data != null) {
            // 更新最后一次下线时间戳
            data.setLastOnlineTimestamp(System.currentTimeMillis());

            // 将更新后的数据保存到数据库
            databaseManager.updatePlayerData(uuid, data);
            getLogger().info("玩家数据已保存到数据库: " + uuid);
        }
    }

    private void saveAllPlayerDataToDatabase() {
        for (UUID uuid : playerDataCache.keySet()) {
            savePlayerDataToDatabase(uuid);
        }
        getLogger().info("所有玩家数据已保存到数据库。");
    }

    public PlayerEnergyData getPlayerData(UUID uuid) {
        return playerDataCache.get(uuid);
    }

    public void setPlayerData(UUID uuid, PlayerEnergyData data) {
        playerDataCache.put(uuid, data);
    }


    private void printEnergyConsumptionConfig() {
        getLogger().info("原版方块能量消耗配置：");
        ConfigurationSection section = getConfig().getConfigurationSection("energy-consumption");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                getLogger().info(key + ": " + section.getDouble(key));
            }
        }

        if (itemsAdderEnabled) {
            if (!itemsAdderEnergyConsumption.isEmpty()) {
                getLogger().info("ItemsAdder 自定义方块能量消耗配置：");
                for (Map.Entry<String, Double> entry : itemsAdderEnergyConsumption.entrySet()) {
                    getLogger().info(entry.getKey() + ": " + entry.getValue());
                }
            } else {
                getLogger().info("未找到任何 ItemsAdder 自定义方块能量消耗配置。");
            }
        } else {
            getLogger().info("ItemsAdder 支持未启用。");
        }
    }
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    public void startCooldownCleanupTask() {
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            long currentTime = System.currentTimeMillis();
            cooldownMap.entrySet().removeIf(entry -> (currentTime - entry.getValue()) >= cooldownTime);
        }, 600L, 600L); // 每30秒执行一次
    }
    public Map<UUID, Long> getCooldownMap() {
        return cooldownMap;
    }
    public HashMap<UUID, PlayerEnergyData> getPlayerDataCache() {
        if (playerDataCache == null) {
            getLogger().severe("Player data cache is null! Initialization might have failed.");

        }else {
            return playerDataCache;
        }
        return null;
    }
    public Potion getPotionHandler() {
        return potionHandler;
    }
    public void onPlayerEnergyUpdate(Player player, double currentEnergy, double maxEnergy) {
        if (bossBarManager != null) {
            bossBarManager.updateBossBar(player, currentEnergy, maxEnergy);
        }
    }
    @Override
    public double getMaxEnergy(Player player) {
        PlayerEnergyData data = getPlayerData(player.getUniqueId());
        return data != null ? data.getMaxEnergy() : 0;
    }

    @Override
    public double getCurrentEnergy(Player player) {
        PlayerEnergyData data = getPlayerData(player.getUniqueId());
        return data != null ? data.getCurrentEnergy() : 0;
    }

    @Override
    public void setCurrentEnergy(Player player, double amount) {
        PlayerEnergyData data = getPlayerData(player.getUniqueId());
        if (data != null) {
            data.setCurrentEnergy(amount);
            if (bossBarManager != null) {
                bossBarManager.updateBossBar(player, amount, data.getMaxEnergy());
            }
            // 可能需要保存到数据库或其他地方
        }
    }
    @Override
    public void addEnergy(Player player, double amount) {
        PlayerEnergyData data = getPlayerData(player.getUniqueId());
        if (data != null) {
            double newEnergy = Math.min(data.getCurrentEnergy() + amount, data.getMaxEnergy());
            data.setCurrentEnergy(newEnergy);
            if (bossBarManager != null) {
                bossBarManager.updateBossBar(player, newEnergy, data.getMaxEnergy());
            }
            // 可能需要保存到数据库或其他地方
        }
    }

    @Override
    public void removeEnergy(Player player, double amount) {
        PlayerEnergyData data = getPlayerData(player.getUniqueId());
        if (data != null) {
            double newEnergy = Math.max(data.getCurrentEnergy() - amount, 0);
            data.setCurrentEnergy(newEnergy);
            if (bossBarManager != null) {
                bossBarManager.updateBossBar(player, newEnergy, data.getMaxEnergy());
            }
            // 可能需要保存到数据库或其他地方
        }
    }

    @Override
    public void setMaxEnergy(Player player, double amount) {
        PlayerEnergyData data = getPlayerData(player.getUniqueId());
        if (data != null) {
            data.setMaxEnergy(amount);
            // 如果当前能量值超过新最大值，则进行调整
            double newEnergy = Math.min(data.getCurrentEnergy(), amount);
            data.setCurrentEnergy(newEnergy);
            if (bossBarManager != null) {
                bossBarManager.updateBossBar(player, newEnergy, amount);
            }
            // 可能需要保存到数据库或其他地方
        }
    }

    @Override
    public void resetEnergy(Player player) {
        PlayerEnergyData data = getPlayerData(player.getUniqueId());
        if (data != null) {
            double maxEnergy = data.getMaxEnergy();
            data.setCurrentEnergy(maxEnergy);
            if (bossBarManager != null) {
                bossBarManager.updateBossBar(player, maxEnergy, maxEnergy);
            }
            // 可能需要保存到数据库或其他地方
        }
    }

    @Override
    public boolean isEnergyFull(Player player) {
        PlayerEnergyData data = getPlayerData(player.getUniqueId());
        return data != null && data.getCurrentEnergy() >= data.getMaxEnergy();
    }

    @Override
    public double getEnergyCapacity(Player player) {
        PlayerEnergyData data = getPlayerData(player.getUniqueId());
        return data != null ? data.getMaxEnergy() : 0;
    }

    @Override
    public double getEnergyPercentage(Player player) {
        PlayerEnergyData data = getPlayerData(player.getUniqueId());
        if (data != null) {
            double maxEnergy = data.getMaxEnergy();
            if (maxEnergy > 0) {
                return (data.getCurrentEnergy() / maxEnergy) * 100;
            }
        }
        return 0;
    }

    @Override
    public void setRegenRate(Player player, double rate) {
        PlayerEnergyData data = getPlayerData(player.getUniqueId());
        if (data != null) {
            data.setRegenRate(rate);
            // 可能需要保存到数据库或其他地方
        }
    }

    @Override
    public double getRegenRate(Player player) {
        PlayerEnergyData data = getPlayerData(player.getUniqueId());
        return data != null ? data.getRegenRate() : 0;
    }

    @Override
    public boolean isEnergyInRange(Player player, double min, double max) {
        PlayerEnergyData data = getPlayerData(player.getUniqueId());
        if (data != null) {
            double currentEnergy = data.getCurrentEnergy();
            return currentEnergy >= min && currentEnergy <= max;
        }
        return false;
    }

    @Override
    public long getLastOnlineTimestamp(Player player) {
        PlayerEnergyData data = getPlayerData(player.getUniqueId());
        return data != null ? data.getLastOnlineTimestamp() : 0;
    }

    @Override
    public void setLastOnlineTimestamp(Player player, long timestamp) {
        PlayerEnergyData data = getPlayerData(player.getUniqueId());
        if (data != null) {
            data.setLastOnlineTimestamp(timestamp);
            // 可能需要保存到数据库或其他地方
        }
    }
    public void debugModePrint(String importance, String text){
        String debugPrefix = "[DEBUG-XME]";
        if(debugModeSwitch) {
            if (importance.equalsIgnoreCase("info")) {
                getLogger().info(debugPrefix + text);
            } else if (importance.equalsIgnoreCase("warning")) {
                getLogger().warning(debugPrefix + text);
            } else if (importance.equalsIgnoreCase("severe")) {
                getLogger().severe(debugPrefix + text);
            }
        }
    }
}
