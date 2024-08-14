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

public class BossBarManager {
    private final FileConfiguration config;
    private final Map<Player, BossBar> playerBossBars = new HashMap<>();
    private final String titleTemplate;
    private final BarColor color;
    private final BarStyle style;
    private final boolean isPermanent;
    private final int displayTime;
    private final Plugin plugin;

    public BossBarManager(FileConfiguration config, Plugin plugin) {
        this.config = config;
        this.titleTemplate = config.getString("bossbar.title", "&a当前能量: {current_energy} / {max_energy}");
        this.color = BarColor.valueOf(config.getString("bossbar.color", "GREEN").toUpperCase());
        this.style = BarStyle.valueOf(config.getString("bossbar.style", "SOLID").toUpperCase());
        this.isPermanent = config.getString("bossbar.display-mode", "temporary").equalsIgnoreCase("permanent");
        this.displayTime = config.getInt("bossbar.display-time", 10);
        this.plugin = plugin;
    }

    public void updateBossBar(Player player, double currentEnergy, double maxEnergy) {
        if (!config.getBoolean("bossbar.enabled", true)) {
            return;
        }

        // 输出当前的 BossBar 状态
        plugin.getLogger().info("更新 BossBar: 玩家 = " + player.getName() + ", 当前能量 = " + currentEnergy + ", 最大能量 = " + maxEnergy);

        BossBar existingBossBar = playerBossBars.get(player);
        if (existingBossBar != null) {
            // 输出移除旧 BossBar 的信息
            plugin.getLogger().info("移除旧 BossBar: 玩家 = " + player.getName());

            // Remove the old BossBar before creating a new one
            existingBossBar.removePlayer(player);
            // Remove the old BossBar from the map
            playerBossBars.remove(player);
        }

        BossBar newBossBar = Bukkit.createBossBar(
                formatTitle(currentEnergy, maxEnergy),
                color,
                style
        );
        newBossBar.setProgress(currentEnergy / maxEnergy);
        newBossBar.addPlayer(player);

        // 输出新 BossBar 创建的信息
        plugin.getLogger().info("创建新 BossBar: 玩家 = " + player.getName() + ", 标题 = " + formatTitle(currentEnergy, maxEnergy) + ", 进度 = " + (currentEnergy / maxEnergy));

        playerBossBars.put(player, newBossBar);

        if (!isPermanent) {
            final BossBar finalBossBar = newBossBar; // Create a final copy for use inside the anonymous class
            new BukkitRunnable() {
                @Override
                public void run() {
                    // 输出 BossBar 被移除的信息
                    plugin.getLogger().info("BossBar 超时移除: 玩家 = " + player.getName());
                    if (playerBossBars.get(player) == finalBossBar) {
                        finalBossBar.removePlayer(player);
                        playerBossBars.remove(player);
                    }
                }
            }.runTaskLater(Bukkit.getPluginManager().getPlugin("XiMiningEnergy"), displayTime * 20L);
        }
    }




    private String formatTitle(double currentEnergy, double maxEnergy) {
        return titleTemplate
                .replace("{current_energy}", String.valueOf(currentEnergy))
                .replace("{max_energy}", String.valueOf(maxEnergy));
    }
    public void removePlayerBossBar(Player player) {
        playerBossBars.remove(player);
    }
    public BossBar getPlayerBossBar(Player player) {
        return playerBossBars.get(player);
    }
}
