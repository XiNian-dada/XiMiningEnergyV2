package cn.hairuosky.ximiningenergy;

import org.bukkit.Bukkit;
import org.bukkit.boss.BossBar;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BossBarManager {
    private final FileConfiguration config;
    private final Map<UUID, BossBar> bossBarMap = new HashMap<>();
    private final Map<Player, UUID> playerBossBarMap = new HashMap<>();
    private final String titleTemplate;
    private final BarColor color;
    private final BarStyle style;
    private final boolean isPermanent;
    private final int displayTime;
    private final Plugin plugin;
    private final String tag;

    public BossBarManager(FileConfiguration config, Plugin plugin) {
        this.config = config;
        this.titleTemplate = config.getString("bossbar.title", "&a当前能量: {current_energy} / {max_energy}");
        this.color = BarColor.valueOf(config.getString("bossbar.color", "GREEN").toUpperCase());
        this.style = BarStyle.valueOf(config.getString("bossbar.style", "SOLID").toUpperCase());
        this.isPermanent = config.getString("bossbar.display-mode", "temporary").equalsIgnoreCase("permanent");
        this.displayTime = config.getInt("bossbar.display-time", 10);
        this.plugin = plugin;
        this.tag = "XiMiningEnergy"; // 自定义标签
        // 清除已存在的 BossBar
        for (Player player : Bukkit.getOnlinePlayers()) {
            removePlayerBossBar(player);
        }
    }

    public void updateBossBar(Player player, double currentEnergy, double maxEnergy) {
        if (!config.getBoolean("bossbar.enabled", true)) {
            return;
        }

        plugin.getLogger().info("更新 BossBar: 玩家 = " + player.getName() + ", 当前能量 = " + currentEnergy + ", 最大能量 = " + maxEnergy);

        UUID existingBossBarId = playerBossBarMap.get(player);
        if (existingBossBarId != null) {
            plugin.getLogger().info("移除旧 BossBar: 玩家 = " + player.getName());

            // 移除旧 BossBar
            BossBar existingBossBar = bossBarMap.get(existingBossBarId);
            if (existingBossBar != null) {
                existingBossBar.removePlayer(player);
            }
            bossBarMap.remove(existingBossBarId);
            playerBossBarMap.remove(player);
        }

        BossBar newBossBar = Bukkit.createBossBar(
                formatTitle(currentEnergy, maxEnergy),
                color,
                style
        );
        newBossBar.setProgress(currentEnergy / maxEnergy);
        newBossBar.addPlayer(player);

        // 生成新的 UUID 作为 BossBar 的标识
        UUID newBossBarId = UUID.randomUUID();
        bossBarMap.put(newBossBarId, newBossBar);
        playerBossBarMap.put(player, newBossBarId);

        plugin.getLogger().info("创建新 BossBar: 玩家 = " + player.getName() + ", 标题 = " + formatTitle(currentEnergy, maxEnergy) + ", 进度 = " + (currentEnergy / maxEnergy));

        if (!isPermanent) {
            final BossBar finalBossBar = newBossBar; // 创建一个 final 副本以供匿名类使用
            new BukkitRunnable() {
                @Override
                public void run() {
                    plugin.getLogger().info("BossBar 超时移除: 玩家 = " + player.getName());
                    UUID bossBarId = playerBossBarMap.get(player);
                    if (bossBarId != null && bossBarMap.get(bossBarId) == finalBossBar) {
                        finalBossBar.removePlayer(player);
                        bossBarMap.remove(bossBarId);
                        playerBossBarMap.remove(player);
                    }
                }
            }.runTaskLater(plugin, displayTime * 20L);
        }
    }

    private String formatTitle(double currentEnergy, double maxEnergy) {
        return titleTemplate
                .replace("{current_energy}", String.valueOf(currentEnergy))
                .replace("{max_energy}", String.valueOf(maxEnergy));
    }

    public void removePlayerBossBar(Player player) {
        UUID bossBarId = playerBossBarMap.get(player);
        if (bossBarId != null) {
            BossBar existingBossBar = bossBarMap.get(bossBarId);
            if (existingBossBar != null) {
                existingBossBar.removePlayer(player);
            }
            bossBarMap.remove(bossBarId);
            playerBossBarMap.remove(player);
        }
    }

    public BossBar getPlayerBossBar(Player player) {
        UUID bossBarId = playerBossBarMap.get(player);
        return bossBarId != null ? bossBarMap.get(bossBarId) : null;
    }

    public void removeTaggedBossBars() {
        for (Map.Entry<UUID, BossBar> entry : bossBarMap.entrySet()) {
            UUID bossBarId = entry.getKey();
            BossBar bossBar = entry.getValue();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (playerBossBarMap.get(player) == bossBarId) {
                    bossBar.removePlayer(player);
                    playerBossBarMap.remove(player);
                }
            }
        }
        bossBarMap.clear();
    }
}
