package cn.hairuosky.ximiningenergy;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class MiningEnergyTabCompleter implements TabCompleter {

    private final FileConfiguration config;

    public MiningEnergyTabCompleter(FileConfiguration config) {
        this.config = config;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!(sender instanceof Player)) {
            return completions;
        }

        Player player = (Player) sender;

        // 主命令的补全
        if (args.length == 1) {
            if (player.hasPermission("ximiningenergy.command.check")) {
                completions.add("check");
            }
            if (player.hasPermission("ximiningenergy.command.update")) {
                completions.add("update");
            }
            if (player.hasPermission("ximiningenergy.command.addmax")) {
                completions.add("addmax");
            }
            if (player.hasPermission("ximiningenergy.command.addregen")) {
                completions.add("addregen");
            }
            if (player.hasPermission("ximiningenergy.command.setmax")) {
                completions.add("setmax");
            }
            if (player.hasPermission("ximiningenergy.command.setregen")) {
                completions.add("setregen");
            }
            if (player.hasPermission("ximiningenergy.command.reload")) {
                completions.add("reload");
            }
            if (player.hasPermission("ximiningenergy.command.info")) {
                completions.add("info");
            }
            if (player.hasPermission("ximiningenergy.command.help")) {
                completions.add("help");
            }
            if (player.hasPermission("ximiningenergy.command.givepotion")) {
                completions.add("givepotion");
            }
        } else if (args.length == 2) {
            // 补全第二个参数
            if (args[0].equalsIgnoreCase("addmax") || args[0].equalsIgnoreCase("addregen") ||
                    args[0].equalsIgnoreCase("setmax") || args[0].equalsIgnoreCase("setregen" )
                    || args[0].equalsIgnoreCase("givepotion")) {
                // 提供玩家名称的补全
                List<String> playerNames = new ArrayList<>();
                for (Player onlinePlayer : player.getServer().getOnlinePlayers()) {
                    playerNames.add(onlinePlayer.getName());
                }
                return playerNames;
            }
        } else if (args.length == 3) {
            // 补全第三个参数
            if (args[0].equalsIgnoreCase("setmax") || args[0].equalsIgnoreCase("setregen") ||
                    args[0].equalsIgnoreCase("addmax") || args[0].equalsIgnoreCase("addregen")) {
                // 提供数字补全
                return Arrays.asList("10", "20", "50", "100", "200", "500");
            } else if (args[0].equalsIgnoreCase("givepotion")) {
                // 提供药水ID补全
                List<String> potionIds = new ArrayList<>();
                if (config.contains("potion")) {
                    for (String key : Objects.requireNonNull(config.getConfigurationSection("potion")).getKeys(false)) {
                        potionIds.add(key);
                    }
                }
                return potionIds;
            }
        }

        return completions;
    }
}
