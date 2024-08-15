package cn.hairuosky.ximiningenergy;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Potion implements Listener {
    private final Map<UUID, Long> lastConsumeTime = new HashMap<>();
    private final Object lock = new Object();
    private static final Object lock_2 = new Object(); // 用于同步
    private static long lastRecoveryTime = 0; // 上次恢复的时间戳
    private static final long RECOVERY_TIME_LIMIT = 1000; // 时间限制，单位毫秒
    private BossBarManager bossBarManager;
    public class PotionData {
        private final String id;
        private final String name;
        private final String type;
        private final double amount;
        private final int customModelData;


        public PotionData(String id, String name, String type, double amount, int customModelData) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.amount = amount;
            this.customModelData = customModelData;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public double getAmount() {
            return amount;
        }

        public int getCustomModelData() {
            return customModelData;
        }
    }

    private final Plugin plugin;
    private final Map<String, PotionData> potions = new HashMap<>();
    private final Map<UUID, PlayerEnergyData> playerDataCache;

    public Potion(BossBarManager bossBarManager, Plugin plugin) {
        this.bossBarManager = bossBarManager;
        this.plugin = plugin;
        this.playerDataCache = ((XiMiningEnergy) plugin).getPlayerDataCache();
        if (this.playerDataCache == null) {
            plugin.getLogger().warning("Player data cache is null. Ensure it's properly initialized in XiMiningEnergy.");
        }
        loadPotions(plugin.getConfig());
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void loadPotions(FileConfiguration config) {
        if (config.contains("potion")) {
            for (String key : config.getConfigurationSection("potion").getKeys(false)) {
                String name = config.getString("potion." + key + ".name");
                String type = config.getString("potion." + key + ".type");
                double amount = config.getDouble("potion." + key + ".amount");
                int customModelData = config.getInt("potion." + key + ".custom-model-data");

                PotionData potionData = new PotionData(key, name, type, amount, customModelData);
                potions.put(key, potionData);
            }
        } else {
            plugin.getLogger().warning("Potion configuration section is missing.");
        }
    }

    PotionData getPotionDataById(String id) {
        return potions.get(id);
    }

    ItemStack createPotionItem(PotionData potionData) {
        ItemStack potion = new ItemStack(org.bukkit.Material.POTION); // 或其他合适的物品类型
        ItemMeta meta = potion.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(potionData.getName());
            meta.setCustomModelData(potionData.getCustomModelData());
            potion.setItemMeta(meta);
        }
        return potion;
    }




    @EventHandler
    public void onPlayerConsumePotion(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        plugin.getLogger().info("onPlayerConsumePotion called: Player = " + player.getName() + ", Timestamp = " + currentTime);

        synchronized (lock) {
            // 检查玩家上次使用药水的时间，如果时间间隔小于1秒（1000毫秒），则跳过处理
            if (lastConsumeTime.containsKey(playerUUID) && (currentTime - lastConsumeTime.get(playerUUID)) < 1000) {
                event.setCancelled(true);
                return;
            }

            // 记录这次的使用时间
            lastConsumeTime.put(playerUUID, currentTime);
        }

        // 继续处理药水消耗事件
        ItemStack item = event.getItem();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        int customModelData = meta.getCustomModelData();
        PotionData matchedPotionData = null;

        // 寻找与当前药水匹配的 PotionData
        for (PotionData potionData : potions.values()) {
            if (potionData.getCustomModelData() == customModelData) {
                matchedPotionData = potionData;
                break;
            }
        }

        if (matchedPotionData != null) {
            // 动态从插件的主类中获取 playerDataCache
            XiMiningEnergy plugin = (XiMiningEnergy) this.plugin;
            Map<UUID, PlayerEnergyData> playerDataCache = plugin.getPlayerDataCache();

            PlayerEnergyData playerData = playerDataCache.get(playerUUID);

            if (playerData != null) {
                double recoveryAmount;
                String type = matchedPotionData.getType();

                if (type.equalsIgnoreCase("percent")) {
                    // 按百分比恢复体力
                    recoveryAmount = calculateRecoveryByPercent(matchedPotionData.getAmount(), playerData);
                } else if (type.equalsIgnoreCase("amount")) {
                    // 按数量恢复体力
                    recoveryAmount = matchedPotionData.getAmount();
                } else {
                    plugin.getLogger().warning("Unknown potion type: " + type);
                    return;
                }

                // 恢复玩家体力并更新 BossBar
                recoverPlayerStamina(playerData, recoveryAmount);

            } else {
                plugin.getLogger().warning("Player data for " + playerUUID + " is null. Ensure player data is properly loaded.");
            }
        }
    }





    private double calculateRecoveryByPercent(double percent, PlayerEnergyData playerData) {
        double maxEnergy = playerData.getMaxEnergy();
        return maxEnergy * (percent / 100);
    }

    private void recoverPlayerStamina(PlayerEnergyData playerData, double amount) {
        long currentTime = System.currentTimeMillis();

        synchronized (lock_2) {
            // 检查上一次恢复的时间，如果时间间隔小于 RECOVERY_TIME_LIMIT，跳过处理
            if (currentTime - lastRecoveryTime < RECOVERY_TIME_LIMIT) {
                plugin.getLogger().info("Duplicate recovery skipped at " + currentTime);
                return;
            }

            double currentEnergy = playerData.getCurrentEnergy();
            double newEnergy = Math.min(currentEnergy + amount, playerData.getMaxEnergy());
            double recoveredAmount = newEnergy - currentEnergy; // 计算实际恢复的能量

            playerData.setCurrentEnergy(newEnergy);

            // 更新恢复时间
            lastRecoveryTime = currentTime;

            // 记录恢复数量和当前时间戳
            plugin.getLogger().info("RecoverPlayerStamina called: Recovered " + recoveredAmount + " energy at " + currentTime);

            // 获取玩家实例并发送提示消息
            Player player = Bukkit.getPlayer(playerData.getGameId());
            // 调用主类的 onPlayerEnergyUpdate 方法来更新 BossBar
            XiMiningEnergy plugin = (XiMiningEnergy) this.plugin;
            plugin.onPlayerEnergyUpdate(player, newEnergy, playerData.getMaxEnergy());

            if (player != null && player.isOnline()) {
                player.sendMessage("你恢复了 " + recoveredAmount + " 点能量。");
            }
        }
    }
}
