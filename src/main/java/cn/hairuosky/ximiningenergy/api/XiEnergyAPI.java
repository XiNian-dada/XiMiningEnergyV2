package cn.hairuosky.ximiningenergy.api;

import org.bukkit.entity.Player;

public interface XiEnergyAPI {
    /**
     * 获取指定玩家的最大能量值。
     *
     * @param player 目标玩家对象
     * @return 玩家最大能量值
     */
    double getMaxEnergy(Player player);

    /**
     * 获取指定玩家的当前能量值。
     *
     * @param player 目标玩家对象
     * @return 玩家当前能量值
     */
    double getCurrentEnergy(Player player);

    /**
     * 设置指定玩家的当前能量值。
     *
     * @param player 目标玩家对象
     * @param amount 要设置的能量值
     */
    void setCurrentEnergy(Player player, double amount);

    /**
     * 向指定玩家的当前能量中添加指定的能量值。如果添加后能量超过最大值，则将其限制为最大值。
     *
     * @param player 目标玩家对象
     * @param amount 要添加的能量值
     */
    void addEnergy(Player player, double amount);

    /**
     * 从指定玩家的当前能量中移除指定的能量值。如果移除后能量低于零，则将其限制为零。
     *
     * @param player 目标玩家对象
     * @param amount 要移除的能量值
     */
    void removeEnergy(Player player, double amount);

    /**
     * 设置指定玩家的最大能量值，并更新当前能量值以确保不超过新的最大值。
     *
     * @param player 目标玩家对象
     * @param amount 要设置的最大能量值
     */
    void setMaxEnergy(Player player, double amount);

    /**
     * 将指定玩家的当前能量值重置为最大能量值。
     *
     * @param player 目标玩家对象
     */
    void resetEnergy(Player player);

    /**
     * 检查指定玩家的能量是否已满。
     *
     * @param player 目标玩家对象
     * @return 如果玩家的能量已满，则返回 true；否则返回 false
     */
    boolean isEnergyFull(Player player);

    /**
     * 获取指定玩家能量的最大值。
     *
     * @param player 目标玩家对象
     * @return 玩家能量的最大值
     */
    double getEnergyCapacity(Player player);

    /**
     * 获取指定玩家能量的剩余百分比。
     *
     * @param player 目标玩家对象
     * @return 玩家当前能量的百分比（0到100之间的值）
     */
    double getEnergyPercentage(Player player);

    /**
     * 设置指定玩家能量的恢复速率。
     *
     * @param player 目标玩家对象
     * @param rate   恢复速率（每秒恢复的能量值）
     */
    void setRegenRate(Player player, double rate);

    /**
     * 获取指定玩家的能量恢复速率。
     *
     * @param player 目标玩家对象
     * @return 玩家当前的能量恢复速率
     */
    double getRegenRate(Player player);

    /**
     * 检查玩家能量是否在指定范围内。
     *
     * @param player 目标玩家对象
     * @param min    最小能量值
     * @param max    最大能量值
     * @return 如果玩家的能量在指定范围内，则返回 true；否则返回 false
     */
    boolean isEnergyInRange(Player player, double min, double max);

    /**
     * 获取指定玩家的最后在线时间戳。
     *
     * @param player 目标玩家对象
     * @return 玩家最后在线时间戳
     */
    long getLastOnlineTimestamp(Player player);

    /**
     * 设置指定玩家的最后在线时间戳。
     *
     * @param player 目标玩家对象
     * @param timestamp 最后在线时间戳
     */
    void setLastOnlineTimestamp(Player player, long timestamp);
}
