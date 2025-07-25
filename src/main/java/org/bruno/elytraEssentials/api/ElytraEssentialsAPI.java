package org.bruno.elytraEssentials.api;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * The official API for ElytraEssentials.
 * Other plugins can use this to interact with player data and plugin features.
 */
public interface ElytraEssentialsAPI {

    /**
    * Gets a player's remaining flight time.
    * @param playerUUID The UUID of the player.
    * @return The remaining flight time in seconds.
    */
    int getFlightTime(UUID playerUUID);

    /**
     * Consumes a specified amount of flight time from a player.
     * @param playerUUID The UUID of the player.
     * @param secondsToConsume The amount of time to remove.
     */
    void consumeFlightTime(UUID playerUUID, int secondsToConsume);

    /**
     * Gets the key of the player's currently active cosmetic effect.
     *
     * @param playerUUID The UUID of the player.
     * @return The string key of the active effect (e.g., "RAINBOW"), or {@code null} if none is active.
     */
    @Nullable
    String getActiveEffectKey(UUID playerUUID);
}
