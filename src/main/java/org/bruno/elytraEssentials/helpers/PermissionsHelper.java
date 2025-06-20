package org.bruno.elytraEssentials.helpers;

import org.bukkit.entity.Player;

public class PermissionsHelper {

    //<editor-fold desc="BYPASSES">

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

    //</editor-fold>

    //<editor-fold desc="COMMANDS">

    public static boolean hasReloadPermission(Player player) {
        return player.hasPermission("elytraessentials.*") ||
                player.hasPermission("elytraessentials.command.*") ||
                player.hasPermission("elytraessentials.command.reload");
    }

    public static boolean hasFlightTimeCommandPermission(Player player){
        return player.hasPermission("elytraessentials.*") ||
                player.hasPermission("elytraessentials.command.*") ||
                player.hasPermission("elytraessentials.command.flighttime");
    }

    public static boolean hasEffectsPermission(Player player) {
        return player.hasPermission("elytraessentials.*") ||
                player.hasPermission("elytraessentials.command.*") ||
                player.hasPermission("elytraessentials.command.effects");
    }

    public static boolean hasShopPermission(Player player) {
        return player.hasPermission("elytraessentials.*") ||
                player.hasPermission("elytraessentials.command.*") ||
                player.hasPermission("elytraessentials.command.shop");
    }

    //</editor-fold>

    //<editor-fold desc="UPDATE">

    public static boolean hasUpdateNotifyPermission(Player player){
        return player.hasPermission("elytraessentials.*") ||
                player.hasPermission("elytraessentials.update.notify");
    }

    //</editor-fold>

    //<editor-fold desc="EFFECTS">

    public static boolean hasElytraEffectsPermission(Player player) {
        return player.hasPermission("elytraessentials.*") ||
                player.hasPermission("elytraessentials.effect.*");
    }

    //</editor-fold>
}
