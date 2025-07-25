package org.bruno.elytraEssentials.api;

import org.bruno.elytraEssentials.handlers.EffectsHandler;
import org.bruno.elytraEssentials.handlers.FlightHandler;

import javax.annotation.Nullable;
import java.util.UUID;

public class ElytraEssentialsAPIImpl implements ElytraEssentialsAPI {
    private final FlightHandler flightHandler;
    private final EffectsHandler effectsHandler;

    public ElytraEssentialsAPIImpl(FlightHandler flightHandler, EffectsHandler effectsHandler) {
        this.flightHandler = flightHandler;
        this.effectsHandler = effectsHandler;
    }

    @Override
    public int getFlightTime(UUID playerUUID) {
        return flightHandler.getCurrentFlightTime(playerUUID);
    }

    @Override
    public void consumeFlightTime(UUID playerUUID, int secondsToConsume) {
        flightHandler.removeFlightTime(playerUUID, secondsToConsume, null);
    }

    @Override
    @Nullable
    public String getActiveEffectKey(UUID playerUUID) {
        return effectsHandler.getActiveEffect(playerUUID);
    }
}
