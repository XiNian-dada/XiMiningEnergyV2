package cn.hairuosky.ximiningenergy;

public class PlayerEnergyData {
    private double currentEnergy;
    private double maxEnergy;
    private double regenRate;
    private long lastOnlineTimestamp; // 确保有这个字段
    private String gameId; // 或其他用于唯一标识玩家的字段

    // 构造函数、getter 和 setter
    public PlayerEnergyData(double currentEnergy, double maxEnergy, double regenRate, long lastOnlineTimestamp, String gameId) {
        this.currentEnergy = currentEnergy;
        this.maxEnergy = maxEnergy;
        this.regenRate = regenRate;
        this.lastOnlineTimestamp = lastOnlineTimestamp;
        this.gameId = gameId;
    }

    // Getters 和 Setters
    public double getCurrentEnergy() { return currentEnergy; }
    public void setCurrentEnergy(double currentEnergy) { this.currentEnergy = currentEnergy; }

    public double getMaxEnergy() { return maxEnergy; }
    public void setMaxEnergy(double maxEnergy) { this.maxEnergy = maxEnergy; }

    public double getRegenRate() { return regenRate; }
    public void setRegenRate(double regenRate) { this.regenRate = regenRate; }

    public long getLastOnlineTimestamp() { return lastOnlineTimestamp; }
    public void setLastOnlineTimestamp(long lastOnlineTimestamp) { this.lastOnlineTimestamp = lastOnlineTimestamp; }

    public String getGameId() { return gameId; }
    public void setGameId(String gameId) { this.gameId = gameId; }
}
