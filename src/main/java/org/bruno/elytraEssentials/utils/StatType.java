package org.bruno.elytraEssentials.utils;

/**
 * Represents the types of player statistics that can be tracked for achievements
 * and other features.
 */
public enum StatType {
    TOTAL_DISTANCE,
    LONGEST_FLIGHT,
    TOTAL_FLIGHT_TIME,
    BOOSTS_USED,
    SUPER_BOOSTS_USED,
    SAVES,
    UNKNOWN; // A fallback for invalid config entries
}
