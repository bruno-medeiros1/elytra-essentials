package org.bruno.elytraEssentials.helpers;

import org.bukkit.entity.Player;

public class PermissionsHelper {

    public static boolean PlayerBypassTimeLimit(Player player) {
        return player.hasPermission("elytraessentials.*") ||
                player.hasPermission("elytraessentials.bypass.*") ||
                    player.hasPermission("elytraessentials.bypass.timelimit");
    }

    public static boolean PlayerBypassSpeedLimit(Player player) {
        return player.hasPermission("elytraessentials.*") ||
                player.hasPermission("elytraessentials.bypass.*") ||
                    player.hasPermission("elytraessentials.bypass.speedlimit");
    }

    public static boolean PlayerBypassBoostCooldown(Player player) {
        return player.hasPermission("elytraessentials.*") ||
                player.hasPermission("elytraessentials.bypass.*") ||
                    player.hasPermission("elytraessentials.bypass.boostcooldown");
    }

    public static boolean PlayerBypassElytraEquip(Player player) {
        return player.hasPermission("elytraessentials.*") ||
                player.hasPermission("elytraessentials.bypass.*") ||
                player.hasPermission("elytraessentials.bypass.equipment");
    }
}
