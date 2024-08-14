package cn.hairuosky.ximiningenergy;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class XiMiningEnergyPlaceholder extends PlaceholderExpansion {

    private final XiMiningEnergy plugin;

    public XiMiningEnergyPlaceholder(XiMiningEnergy plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "miningenergy";
    }

    @Override
    public @NotNull String getAuthor() {
        return "XiNian_dada";  // 插件作者的名字
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();  // 插件的版本号
    }

    @Override
    public boolean persist() {
        return true;  // 确保占位符在重载时不会消失
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null) {
            return "";
        }

        // 获取玩家的数据
        PlayerEnergyData data = plugin.getPlayerData(player.getUniqueId());

        if (data == null) {
            return "";
        }

        // 定义你的占位符
        switch (identifier) {
            case "current_energy":
                return String.valueOf(data.getCurrentEnergy());
            case "max_energy":
                return String.valueOf(data.getMaxEnergy());
            case "regen_rate":
                return String.valueOf(data.getRegenRate());
            // 新增占位符: 升级最大体力所需金币
            case "upgrade_max_energy_cost":
                double maxEnergy = data.getMaxEnergy();
                double upgradeMaxCost = Math.pow(maxEnergy, 2) / 20;
                return String.format("%.2f", upgradeMaxCost);

            // 新增占位符: 升级恢复速率所需金币
            case "upgrade_regen_rate_cost":
                double regenRate = data.getRegenRate();
                double upgradeRegenCost = Math.pow(regenRate, 3) / 2;
                return String.format("%.2f", upgradeRegenCost);
            // 新增占位符: 距离恢复满的时间
            case "time_to_full_energy":
                double currentEnergy = data.getCurrentEnergy();
                double regenRate_2 = data.getRegenRate();
                double maxEnergy_2 = data.getMaxEnergy();
                if (currentEnergy >= maxEnergy_2) {
                    return "0";  // 如果当前体力已经是最大体力
                } else {
                    double timeToFull = Math.ceil((maxEnergy_2 - currentEnergy) / regenRate_2);
                    return String.format("%.0f", timeToFull);
                }
            default:
                return "";
        }
    }
}
