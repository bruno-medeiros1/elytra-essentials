package org.bruno.elytraEssentials.utils;

import java.util.UUID;

public class PlayerStats {
    private final UUID uuid;
    private double totalDistance;
    private long totalTimeSeconds;
    private double longestFlight;
    private int boostsUsed;
    private int superBoostsUsed;
    private int pluginSaves;

    public PlayerStats(UUID uuid) {
        this.uuid = uuid;
        this.totalDistance = 0;
        this.totalTimeSeconds = 0;
        this.longestFlight = 0;
        this.boostsUsed = 0;
        this.superBoostsUsed = 0;
        this.pluginSaves = 0;
    }

    public UUID getUuid() { return uuid; }
    public double getTotalDistance() { return totalDistance; }
    public void setTotalDistance(double totalDistance) { this.totalDistance = totalDistance; }
    public long getTotalTimeSeconds() { return totalTimeSeconds; }
    public void setTotalTimeSeconds(long totalTimeSeconds) { this.totalTimeSeconds = totalTimeSeconds; }
    public double getLongestFlight() { return longestFlight; }
    public void setLongestFlight(double longestFlight) { this.longestFlight = longestFlight; }
    public int getBoostsUsed() { return boostsUsed; }
    public void setBoostsUsed(int boostsUsed) { this.boostsUsed = boostsUsed; }
    public int getSuperBoostsUsed() { return superBoostsUsed; }
    public void setSuperBoostsUsed(int superBoostsUsed) { this.superBoostsUsed = superBoostsUsed; }
    public int getPluginSaves() { return pluginSaves; }
    public void setPluginSaves(int pluginSaves) { this.pluginSaves = pluginSaves; }

    public void addDistance(double distance) { this.totalDistance += distance; }
    public void addTime(long seconds) { this.totalTimeSeconds += seconds; }
    public void incrementBoostsUsed() { this.boostsUsed++; }
    public void incrementSuperBoostsUsed() { this.superBoostsUsed++; }
    public void incrementPluginSaves() { this.pluginSaves++; }
}
