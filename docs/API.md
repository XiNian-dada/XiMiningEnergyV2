## XiEnergyAPI 开发文档
### 概述
XiEnergyAPI 提供了一组方法，用于管理和操作玩家的能量系统。你可以通过这些方法获取和修改玩家的能量值、设置恢复速率、检查能量状态等。
### 方法列表
#### double getMaxEnergy(Player player)
获取指定玩家的最大能量值。
参数：
- player：目标玩家对象。
  返回值：
- 玩家最大能量值（double 类型）。
  示例：
```java
double maxEnergy = xiEnergyAPI.getMaxEnergy(player);
```
#### double getCurrentEnergy(Player player)
获取指定玩家的当前能量值。
参数：
- player：目标玩家对象。
  返回值：
- 玩家当前能量值（double 类型）。
  示例：
```java
double currentEnergy = xiEnergyAPI.getCurrentEnergy(player);
```
#### void setCurrentEnergy(Player player, double amount)
设置指定玩家的当前能量值。
参数：
- player：目标玩家对象。
- amount：要设置的能量值（double 类型）。
  示例：
```java
xiEnergyAPI.setCurrentEnergy(player, 50.0);
```
#### void addEnergy(Player player, double amount)
向指定玩家的当前能量中添加指定的能量值。如果添加后能量超过最大值，则将其限制为最大值。
参数：
- player：目标玩家对象。
- amount：要添加的能量值（double 类型）。
  示例：
```java
xiEnergyAPI.addEnergy(player, 10.0);
```
#### void removeEnergy(Player player, double amount)
从指定玩家的当前能量中移除指定的能量值。如果移除后能量低于零，则将其限制为零。
参数：
- player：目标玩家对象。
- amount：要移除的能量值（double 类型）。
  示例：
```java
xiEnergyAPI.removeEnergy(player, 5.0);
```
#### void setMaxEnergy(Player player, double amount)
设置指定玩家的最大能量值，并更新当前能量值以确保不超过新的最大值。
参数：
- player：目标玩家对象。
- amount：要设置的最大能量值（double 类型）。
  示例：
```java
xiEnergyAPI.setMaxEnergy(player, 100.0);
```
#### void resetEnergy(Player player)
将指定玩家的当前能量值重置为最大能量值。
参数：
- player：目标玩家对象。
  示例：
```java
xiEnergyAPI.resetEnergy(player);

```
#### boolean isEnergyFull(Player player)
检查指定玩家的能量是否已满。
参数：
- player：目标玩家对象。
  返回值：
- 如果玩家的能量已满，则返回 true；否则返回 false（boolean 类型）。
  示例：
```java
boolean isFull = xiEnergyAPI.isEnergyFull(player);
```
#### double getEnergyCapacity(Player player)
获取指定玩家能量的最大值。
参数：
- player：目标玩家对象。
  返回值：
- 玩家能量的最大值（double 类型）。
  示例：
```java
double capacity = xiEnergyAPI.getEnergyCapacity(player);
```
#### double getEnergyPercentage(Player player)
获取指定玩家能量的剩余百分比。
参数：
- player：目标玩家对象。
  返回值：
- 玩家当前能量的百分比（double 类型，范围从 0 到 100）。
  示例：
```java
double percentage = xiEnergyAPI.getEnergyPercentage(player);
```
#### void setRegenRate(Player player, double rate)
设置指定玩家能量的恢复速率。
参数：
- player：目标玩家对象。
- rate：恢复速率（每秒恢复的能量值，double 类型）。
  示例：
```java
xiEnergyAPI.setRegenRate(player, 2.0);
```
#### double getRegenRate(Player player)
获取指定玩家的能量恢复速率。
参数：
- player：目标玩家对象。
  返回值：
- 玩家当前的能量恢复速率（double 类型）。
  示例：
```java
double regenRate = xiEnergyAPI.getRegenRate(player);
```
#### boolean isEnergyInRange(Player player, double min, double max)
检查玩家能量是否在指定范围内。
参数：
- player：目标玩家对象。
- min：最小能量值（double 类型）。
- max：最大能量值（double 类型）。
  返回值：
- 如果玩家的能量在指定范围内，则返回 true；否则返回 false（boolean 类型）。
  示例：
```java
boolean inRange = xiEnergyAPI.isEnergyInRange(player, 20.0, 80.0);
```
#### long getLastOnlineTimestamp(Player player)
获取指定玩家的最后在线时间戳。
参数：
- player：目标玩家对象。
  返回值：
- 玩家最后在线时间戳（long 类型）。
  示例：
```java
long lastOnline = xiEnergyAPI.getLastOnlineTimestamp(player);
```
#### void setLastOnlineTimestamp(Player player, long timestamp)
设置指定玩家的最后在线时间戳。
参数：
- player：目标玩家对象。
- timestamp：最后在线时间戳（long 类型）。
  示例：
```java
xiEnergyAPI.setLastOnlineTimestamp(player, System.currentTimeMillis());
```