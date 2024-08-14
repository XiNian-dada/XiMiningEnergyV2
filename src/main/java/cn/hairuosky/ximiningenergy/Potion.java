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
        ItemStack item = event.getItem();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        int customModelData = meta.getCustomModelData();
        for (PotionData potionData : potions.values()) {
            if (potionData.getCustomModelData() == customModelData) {
                double amount = potionData.getAmount();
                String type = potionData.getType();

                Player player = event.getPlayer();
                // 动态获取 playerDataCache
                Map<UUID, PlayerEnergyData> playerDataCache = ((XiMiningEnergy) plugin).getPlayerDataCache();
                PlayerEnergyData playerData = playerDataCache.get(player.getUniqueId());

                if (playerData != null) {
                    double recoveryAmount;
                    if (type.equalsIgnoreCase("percent")) {
                        // 按百分比恢复体力
                        recoveryAmount = calculateRecoveryByPercent(amount, playerData);
                    } else if (type.equalsIgnoreCase("amount")) {
                        // 按数量恢复体力
                        recoveryAmount = amount;
                    } else {
                        plugin.getLogger().warning("Unknown potion type: " + type);
                        return;
                    }

                    recoverPlayerStamina(playerData, recoveryAmount);
                }
                break;
            }
        }
    }


    private double calculateRecoveryByPercent(double percent, PlayerEnergyData playerData) {
        double maxEnergy = playerData.getMaxEnergy();
        return maxEnergy * (percent / 100);
    }

    private void recoverPlayerStamina(PlayerEnergyData playerData, double amount) {
        double currentEnergy = playerData.getCurrentEnergy();
        double newEnergy = Math.min(currentEnergy + amount, playerData.getMaxEnergy());
        double recoveredAmount = newEnergy - currentEnergy; // 计算实际恢复的能量

        playerData.setCurrentEnergy(newEnergy);

        // 获取玩家实例并发送提示消息
        Player player = Bukkit.getPlayer(playerData.getGameId());
        if (bossBarManager != null) {
            bossBarManager.updateBossBar(player, amount, playerData.getMaxEnergy());
        }
        if (player != null && player.isOnline()) {
            player.sendMessage("你恢复了 " + recoveredAmount + " 点能量。");
        }
    }
}
