package cn.hairuosky.ximiningenergy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class MiningEnergyCommandExecutor implements CommandExecutor, Listener {

    private final XiMiningEnergy plugin;
    private final Potion potionHandler; // 确保 Potion 类已被正确初始化

    public MiningEnergyCommandExecutor(XiMiningEnergy plugin) {
        this.plugin = plugin;
        // 确保 Potion 类已被正确初始化
        this.potionHandler = plugin.getPotionHandler();

    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("此命令只能由玩家执行！");
            return true;
        }

        Player player = (Player) sender;
        PlayerEnergyData data = plugin.getPlayerData(player.getUniqueId());
        DatabaseManager dbManager = new DatabaseManager(plugin); // 创建 DatabaseManager 实例

        if (args.length == 0) {
            player.sendMessage("请输入子命令！使用 /miningenergy help 查看帮助信息。");
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "check":
                if (player.hasPermission("ximiningenergy.command.check")) {
                    player.sendMessage("当前体力: " + data.getCurrentEnergy());
                    player.sendMessage("最大体力: " + data.getMaxEnergy());
                    player.sendMessage("恢复速率: " + data.getRegenRate());
                    return true;
                } else {
                    player.sendMessage("你没有权限执行此命令！");
                    return true;
                }

            case "update":
                if (player.hasPermission("ximiningenergy.command.update")) {
                    openUpgradeMenu(player);
                    return true;
                } else {
                    player.sendMessage("你没有权限执行此命令！");
                    return true;
                }
            case "givepotion":
                if (player.hasPermission("ximiningenergy.command.givepotion")) {
                    if (args.length > 2) {
                        String targetPlayerName = args[1];
                        String potionId = args[2].toLowerCase();

                        Player targetPlayer = plugin.getServer().getPlayer(targetPlayerName);
                        if (targetPlayer != null) {
                            Potion.PotionData potionData = potionHandler.getPotionDataById(potionId);

                            if (potionData != null) {
                                ItemStack potion = potionHandler.createPotionItem(potionData);
                                targetPlayer.getInventory().addItem(potion);
                                sender.sendMessage("成功给予玩家 " + targetPlayerName + " 药水: " + potionData.getName());
                                targetPlayer.sendMessage("你收到了药水: " + potionData.getName() + "!");
                            } else {
                                sender.sendMessage("无效的药水 ID！");
                            }
                        } else {
                            sender.sendMessage("找不到玩家 " + targetPlayerName + "！");
                        }
                    } else {
                        sender.sendMessage("请指定玩家名称和药水 ID！");
                    }
                    return true;
                } else {
                    player.sendMessage("你没有权限执行此命令！");
                    return true;
                }


            case "addmax":
                if (player.hasPermission("ximiningenergy.command.addmax")) {
                    if (args.length > 1) {
                        try {
                            int amount = Integer.parseInt(args[1]);
                            addMaxEnergy(player, amount);
                            player.sendMessage("成功增加最大体力！");
                        } catch (NumberFormatException e) {
                            player.sendMessage("无效的数量！");
                        }
                    } else {
                        player.sendMessage("请指定增加的数量！");
                    }
                    return true;
                } else {
                    player.sendMessage("你没有权限执行此命令！");
                    return true;
                }

            case "addregen":
                if (player.hasPermission("ximiningenergy.command.addregen")) {
                    if (args.length > 1) {
                        try {
                            int amount = Integer.parseInt(args[1]);
                            addRegenRate(player, amount);
                            player.sendMessage("成功增加恢复速率！");
                        } catch (NumberFormatException e) {
                            player.sendMessage("无效的数量！");
                        }
                    } else {
                        player.sendMessage("请指定增加的数量！");
                    }
                    return true;
                } else {
                    player.sendMessage("你没有权限执行此命令！");
                    return true;
                }

            case "setmax":
                if (player.hasPermission("ximiningenergy.command.setmax")) {
                    if (args.length > 2) {
                        String targetPlayerName = args[1];
                        try {
                            int maxEnergy = Integer.parseInt(args[2]);
                            setMaxEnergy(targetPlayerName, maxEnergy);
                            player.sendMessage("成功设置玩家 " + targetPlayerName + " 的最大体力！");
                        } catch (NumberFormatException e) {
                            player.sendMessage("无效的体力值！");
                        }
                    } else {
                        player.sendMessage("请指定玩家名称和最大体力值！");
                    }
                    return true;
                } else {
                    player.sendMessage("你没有权限执行此命令！");
                    return true;
                }

            case "setregen":
                if (player.hasPermission("ximiningenergy.command.setregen")) {
                    if (args.length > 2) {
                        String targetPlayerName = args[1];
                        try {
                            int regenRate = Integer.parseInt(args[2]);
                            setRegenRate(targetPlayerName, regenRate);
                            player.sendMessage("成功设置玩家 " + targetPlayerName + " 的恢复速率！");
                        } catch (NumberFormatException e) {
                            player.sendMessage("无效的恢复速率！");
                        }
                    } else {
                        player.sendMessage("请指定玩家名称和恢复速率！");
                    }
                    return true;
                } else {
                    player.sendMessage("你没有权限执行此命令！");
                    return true;
                }

            case "reload":
                if (player.hasPermission("ximiningenergy.command.reload")) {
                    try {
                        // 重新加载配置文件
                        plugin.reloadConfig();

                        // 重新初始化语言文件
                        plugin.initializeLangFile();

                        // 重新加载 ItemsAdder 配置（如果启用）
                        boolean itemsAdderEnabled = plugin.getConfig().getBoolean("itemsadder.enabled", false);
                        if (itemsAdderEnabled) {
                            if (plugin.getServer().getPluginManager().getPlugin("ItemsAdder") == null) {
                                plugin.getLogger().severe("ItemsAdder 支持已启用，但未检测到 ItemsAdder 插件！");
                            } else {
                                // 读取 ItemsAdder 自定义方块配置
                                plugin.itemsAdderEnergyConsumption = new HashMap<>();
                                ConfigurationSection itemsAdderSection = plugin.getConfig().getConfigurationSection("itemsadder.energy-consumption");
                                if (itemsAdderSection != null) {
                                    for (String key : itemsAdderSection.getKeys(false)) {
                                        double consumption = itemsAdderSection.getDouble(key);
                                        plugin.itemsAdderEnergyConsumption.put(key, consumption);
                                    }
                                }
                            }
                        } else {
                            plugin.itemsAdderEnergyConsumption = new HashMap<>();
                        }
                        // 移除所有现有 BossBar
                        for (Player playerInServer : plugin.getServer().getOnlinePlayers()) {
                            plugin.bossBarManager.removePlayerBossBar(playerInServer);
                            // 移除所有带有特定标签的 BossBar
                            plugin.bossBarManager.removeTaggedBossBars();
                        }

                        // 重新初始化 BossBar 管理器
                        plugin.bossBarManager = new BossBarManager(plugin.getConfig(), plugin);

                        // 重新启动 EnergyRegenTask
                        Bukkit.getScheduler().cancelTasks(plugin);
                        new XiMiningEnergy.EnergyRegenTask(plugin).runTaskTimer(plugin, 0L, 1200L);

                        // 重新启动 AutoSavePlayerData 任务（如果启用）
                        if (plugin.getConfig().getBoolean("auto-save")) {
                            int autoSaveDelay = plugin.getConfig().getInt("auto-save-delay", 600) * 20;
                            new XiMiningEnergy.AutoSavePlayerData(plugin).runTaskTimer(plugin, 0L, autoSaveDelay);
                        }
                        Potion potionHandler = plugin.getPotionHandler();
                        potionHandler.reloadPotions(plugin.getConfig());

                        //player.sendMessage(plugin.getMessage("config-reloaded")); // 使用带前缀的消息
                    } catch (Exception e) {
                        //player.sendMessage(plugin.getMessage("reload-failed")); // 使用带前缀的消息
                        plugin.getLogger().severe("插件重载过程中发生错误！");
                        e.printStackTrace();
                    }
                    return true;
                } else {
                    //player.sendMessage(plugin.getMessage("no-permission")); // 使用带前缀的消息
                    return true;
                }


            case "info":
                if (player.hasPermission("ximiningenergy.command.info")) {
                    player.sendMessage("插件信息：XiMiningEnergy v1.0");

                    // 检查 PlaceholderAPI 的状态
                    if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
                        player.sendMessage("PlaceholderAPI 状态: 已连接");
                    } else {
                        player.sendMessage("PlaceholderAPI 状态: 未连接");
                    }

                    // 检查 Vault 的状态
                    if (plugin.getServer().getPluginManager().getPlugin("Vault") != null) {
                        player.sendMessage("Vault 状态: 已启用");
                    } else {
                        player.sendMessage("Vault 状态: 未启用");
                    }

                    // 检查 ItemsAdder 的状态
                    if (plugin.itemsAdderEnabled) {
                        player.sendMessage("ItemsAdder 状态: 已启用");
                    } else {
                        player.sendMessage("ItemsAdder 状态: 未启用");
                    }

                    // 获取数据库连接延迟和占用情况

                    long connectionDelay = dbManager.getConnectionDelay();
                    long databaseUsage = dbManager.getDatabaseUsage();

                    player.sendMessage("数据库连接延迟: " + connectionDelay + " ms");
                    player.sendMessage("数据库当前占用: " + databaseUsage + " MB");

                    return true;
                } else {
                    player.sendMessage("你没有权限执行此命令！");
                    return true;
                }

            case "help":
                if (player.hasPermission("ximiningenergy.command.help")) {
                    player.sendMessage("可用命令：");
                    player.sendMessage("/miningenergy check - 查看体力信息");
                    player.sendMessage("/miningenergy update - 打开升级菜单");
                    player.sendMessage("/miningenergy addmax <amount> - 增加最大体力");
                    player.sendMessage("/miningenergy addregen <amount> - 增加恢复速率");
                    player.sendMessage("/miningenergy setmax <playername> <amount> - 设置玩家最大体力");
                    player.sendMessage("/miningenergy setregen <playername> <amount> - 设置玩家恢复速率");
                    player.sendMessage("/miningenergy reload - 重新加载配置");
                    player.sendMessage("/miningenergy info - 查看插件信息");
                    player.sendMessage("/miningenergy help - 查看帮助信息");
                    return true;
                } else {
                    player.sendMessage("你没有权限执行此命令！");
                    return true;
                }

            default:
                player.sendMessage("无效的子命令！使用 /miningenergy help 查看帮助信息。");
                return true;
        }
    }


    private void openUpgradeMenu(Player player) {
        // 读取配置
        FileConfiguration config = plugin.getConfig();
        String title = ChatColor.translateAlternateColorCodes('&', config.getString("upgrade-menu.title", "升级体力和恢复速率"));
        int size = config.getInt("upgrade-menu.size", 9);

        Inventory inv = Bukkit.createInventory(null, size, title);

        // 获取玩家数据
        PlayerEnergyData data = plugin.getPlayerData(player.getUniqueId());
        if (data == null) return;

        double maxEnergy = data.getMaxEnergy();
        double regenRate = data.getRegenRate();

        ConfigurationSection itemsSection = config.getConfigurationSection("upgrade-menu.items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);

                // 读取物品配置
                int slot = itemSection.getInt("slot");
                Material material = Material.getMaterial(itemSection.getString("material", "STONE").toUpperCase());
                String name = ChatColor.translateAlternateColorCodes('&', itemSection.getString("name", "未命名"));
                int customModelData = itemSection.getInt("custom-model-data", 0);

                // 创建物品
                ItemStack item = new ItemStack(material);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(name);
                    if (customModelData != 0) {
                        meta.setCustomModelData(customModelData);
                    }

                    // 处理 lore
                    List<String> lore = itemSection.getStringList("lore");
                    if (lore != null && !lore.isEmpty()) {
                        List<String> translatedLore = new ArrayList<>();
                        for (String line : lore) {
                            // 替换 lore 中的占位符
                            line = line.replace("{max_energy}", String.valueOf(maxEnergy));
                            line = line.replace("{regen_rate}", String.valueOf(regenRate));
                            // 计算升级所需金币
                            if (line.contains("{upgrade_cost}")) {
                                double upgradeCost = 0;
                                if (itemSection.getString("type", "").equals("max-energy")) {
                                    upgradeCost = Math.pow(maxEnergy, 2) / 20;
                                } else if (itemSection.getString("type", "").equals("regen-rate")) {
                                    upgradeCost = Math.pow(regenRate, 3) / 2;
                                }
                                line = line.replace("{upgrade_cost}", String.valueOf(upgradeCost));
                            }
                            translatedLore.add(ChatColor.translateAlternateColorCodes('&', line));
                        }
                        meta.setLore(translatedLore);
                    }

                    item.setItemMeta(meta);
                }

                // 设置物品在菜单中的位置
                inv.setItem(slot, item);
            }
        }

        player.openInventory(inv);
    }
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        InventoryView view = event.getView();
        String menuTitle = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("upgrade-menu.title", "升级体力和恢复速率"));

        // 使用 translateAlternateColorCodes 处理标题的颜色代码
        if (ChatColor.stripColor(view.getTitle()).equals(ChatColor.stripColor(menuTitle))) {
            plugin.getLogger().info("玩家 " + player.getName() + " 点击了升级菜单");

            event.setCancelled(true); // 防止玩家取走物品

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem != null && clickedItem.getType() != Material.AIR) {
                ItemMeta meta = clickedItem.getItemMeta();
                if (meta != null && meta.hasDisplayName()) {
                    String itemName = meta.getDisplayName();
                    plugin.getLogger().info("点击的物品名称: " + itemName);

                    ConfigurationSection itemsSection = plugin.getConfig().getConfigurationSection("upgrade-menu.items");
                    if (itemsSection != null) {
                        boolean itemProcessed = false;
                        for (String key : itemsSection.getKeys(false)) {
                            ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
                            if (itemSection == null) continue; // 确保配置节存在

                            String type = itemSection.getString("type", "").trim(); // 获取物品的类型
                            String displayName = ChatColor.translateAlternateColorCodes('&', itemSection.getString("name", "")); // 获取物品的名称

                            // 如果点击的物品名称与配置中的 displayName 匹配
                            if (itemName.equals(displayName)) {
                                plugin.getLogger().info("找到匹配的物品类型: " + type);

                                switch (type) {
                                    case "max-energy":
                                        plugin.getLogger().info("处理最大体力升级");
                                        upgradeMaxEnergy(player);
                                        openUpgradeMenu(player); // 重新打开菜单
                                        itemProcessed = true;
                                        break;
                                    case "regen-rate":
                                        plugin.getLogger().info("处理恢复速率升级");
                                        upgradeRegenRate(player);
                                        openUpgradeMenu(player); // 重新打开菜单
                                        itemProcessed = true;
                                        break;
                                    default:
                                        plugin.getLogger().info("处理其他类型物品: " + type);
                                        break;
                                }
                            }
                        }

                        if (!itemProcessed) {
                            plugin.getLogger().warning("没有找到匹配的物品配置: " + itemName);
                        }
                    } else {
                        plugin.getLogger().warning("升级菜单配置中的项部分为空！");
                    }
                } else {
                    plugin.getLogger().warning("点击的物品没有 ItemMeta 或没有显示名称！");
                }
            } else {
                plugin.getLogger().warning("点击的物品为空或类型为 AIR！");
            }
        } else {
            plugin.getLogger().info("玩家 " + player.getName() + " 点击的菜单标题不匹配: " + view.getTitle());
        }
    }




    private void upgradeMaxEnergy(Player player) {
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        // 获取主类中的冷却时间 map
        Map<UUID, Long> cooldownMap = plugin.getCooldownMap();

        // 检查冷却时间
        if (cooldownMap.containsKey(playerId) && (currentTime - cooldownMap.get(playerId)) < plugin.cooldownTime) {
            player.sendMessage("请稍后再试！");
            return;
        }
        PlayerEnergyData data = plugin.getPlayerData(playerId);
        if (data != null) {
            double currentMaxEnergy = data.getMaxEnergy();
            double maxEnergyLimit = plugin.getConfig().getDouble("max-energy-limit", 200);

            // 检查是否已经达到最大体力上限
            if (currentMaxEnergy >= maxEnergyLimit) {
                player.sendMessage("已达到最大体力上限，无法继续升级！");
                return;
            }

            // 计算升级所需的金币数
            double upgradeCost = Math.pow(currentMaxEnergy, 2) / 20;
            if (!hasEnoughGold(player, upgradeCost)) {
                player.sendMessage("金币不足，无法升级最大体力！");
                return;
            }

            // 扣除金币并升级
            deductGold(player, upgradeCost);
            double newMaxEnergy = currentMaxEnergy + 10;
            data.setMaxEnergy(newMaxEnergy);
            player.sendMessage("最大体力升级到 " + newMaxEnergy);
            plugin.setPlayerData(playerId, data);
            plugin.getLogger().info("玩家 " + player.getName() + " 的最大体力升级到 " + newMaxEnergy);
            savePlayerDataToDatabaseAsync(playerId);

            // 设置冷却时间
            cooldownMap.put(playerId, currentTime);
        }
    }

    private void upgradeRegenRate(Player player) {
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        // 获取主类中的冷却时间 map
        Map<UUID, Long> cooldownMap = plugin.getCooldownMap();

        // 检查冷却时间
        if (cooldownMap.containsKey(playerId)) {
            long lastUpgradeTime = cooldownMap.get(playerId);
            long timeSinceLastUpgrade = currentTime - lastUpgradeTime;

            plugin.getLogger().info("玩家 " + player.getName() + " 的冷却时间检测中，剩余时间: " + (plugin.cooldownTime - timeSinceLastUpgrade) + " 毫秒");

            if (timeSinceLastUpgrade < plugin.cooldownTime) {
                player.sendMessage("请稍后再试！");
                return;
            }
        }

        PlayerEnergyData data = plugin.getPlayerData(playerId);
        if (data != null) {
            double currentRegenRate = data.getRegenRate();
            double regenRateLimit = plugin.getConfig().getDouble("regen-rate-limit", 10);

            // 检查是否已经达到恢复速率上限
            if (currentRegenRate >= regenRateLimit) {
                player.sendMessage("已达到恢复速率上限，无法继续升级！");
                return;
            }

            // 计算升级所需的金币数
            double upgradeCost = Math.pow(currentRegenRate, 3) / 2;
            if (!hasEnoughGold(player, upgradeCost)) {
                player.sendMessage("金币不足，无法升级恢复速率！");
                return;
            }

            // 扣除金币并升级
            deductGold(player, upgradeCost);
            double newRegenRate = currentRegenRate + 1;
            data.setRegenRate(newRegenRate);
            player.sendMessage("恢复速率升级到 " + newRegenRate);
            plugin.setPlayerData(playerId, data);
            plugin.getLogger().info("玩家 " + player.getName() + " 的恢复速率升级到 " + newRegenRate);
            savePlayerDataToDatabaseAsync(playerId);

            // 设置冷却时间
            cooldownMap.put(playerId, currentTime);
            plugin.getLogger().info("玩家 " + player.getName() + " 的冷却时间已设置为 " + currentTime + " 毫秒");
        }
    }


    // 使用 Vault 检查玩家是否有足够的金币
    private boolean hasEnoughGold(Player player, double amount) {
        return plugin.economy.has(player, amount);
    }

    // 使用 Vault 扣除玩家金币
    private void deductGold(Player player, double amount) {
        plugin.economy.withdrawPlayer(player, amount);
    }
    private void savePlayerDataToDatabaseAsync(UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            savePlayerDataToDatabase(uuid);
        });
    }
    private void savePlayerDataToDatabase(UUID uuid) {
        PlayerEnergyData data = plugin.playerDataCache.get(uuid);
        if (data != null) {
            // 将更新后的数据保存到数据库
            plugin.databaseManager.updatePlayerData(uuid, data);
            plugin.getLogger().info("玩家数据已保存到数据库: " + uuid);
        }
    }
    private void addMaxEnergy(Player player, int amount) {
        PlayerEnergyData data = plugin.getPlayerData(player.getUniqueId());
        if (data != null) {
            data.setMaxEnergy(data.getMaxEnergy() + amount);
            savePlayerDataToDatabase(player.getUniqueId());
        }
    }

    private void addRegenRate(Player player, int amount) {
        PlayerEnergyData data = plugin.getPlayerData(player.getUniqueId());
        if (data != null) {
            data.setRegenRate(data.getRegenRate() + amount);
            savePlayerDataToDatabase(player.getUniqueId());
        }
    }

    private void setMaxEnergy(String playerName, int maxEnergy) {
        Player targetPlayer = Bukkit.getPlayer(playerName);
        if (targetPlayer != null) {
            PlayerEnergyData data = plugin.getPlayerData(targetPlayer.getUniqueId());
            if (data != null) {
                data.setMaxEnergy(maxEnergy);
                savePlayerDataToDatabase(targetPlayer.getUniqueId());
            }
        }
    }

    private void setRegenRate(String playerName, int regenRate) {
        Player targetPlayer = Bukkit.getPlayer(playerName);
        if (targetPlayer != null) {
            PlayerEnergyData data = plugin.getPlayerData(targetPlayer.getUniqueId());
            if (data != null) {
                data.setRegenRate(regenRate);
                savePlayerDataToDatabase(targetPlayer.getUniqueId());
            }
        }
    }


}
