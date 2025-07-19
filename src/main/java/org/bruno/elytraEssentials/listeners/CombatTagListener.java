package org.bruno.elytraEssentials.listeners;

import org.bruno.elytraEssentials.handlers.CombatTagHandler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class CombatTagListener implements Listener {

    private final CombatTagHandler combatTagHandler;

    public CombatTagListener(CombatTagHandler combatTagHandler) {
        this.combatTagHandler = combatTagHandler;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageEvent event) {
        combatTagHandler.handleDamage(event);
        combatTagHandler.handleFallDamage(event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onGlideAttempt(EntityToggleGlideEvent event) {
        combatTagHandler.handleGlideAttempt(event);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        combatTagHandler.handlePlayerQuit(event);
    }
}