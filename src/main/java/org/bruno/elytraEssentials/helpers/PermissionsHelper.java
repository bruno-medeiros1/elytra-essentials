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

    public static boolean PlayerBypassCombatTag(Player player){
        return player.hasPermission("elytraessentials.*") ||
                player.hasPermission("elytraessentials.bypass.*") ||
                player.hasPermission("elytraessentials.bypass.combattag");
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

    public static boolean hasClearEffectsCommandPermission(Player player){
        return player.hasPermission("elytraessentials.*") ||
                player.hasPermission("elytraessentials.command.*") ||
                player.hasPermission("elytraessentials.command.effects.clear");
    }

    public static boolean hasShopPermission(Player player) {
        return player.hasPermission("elytraessentials.*") ||
                player.hasPermission("elytraessentials.command.*") ||
                player.hasPermission("elytraessentials.command.shop");
    }

    public static boolean hasStatsPermission(Player player) {
        return player.hasPermission("elytraessentials.*") ||
                player.hasPermission("elytraessentials.command.*") ||
                player.hasPermission("elytraessentials.command.stats");
    }

    public static boolean hasOthersStatsPermission(Player player){
        return player.hasPermission("elytraessentials.*") ||
                player.hasPermission("elytraessentials.command.*") ||
                player.hasPermission("elytraessentials.command.stats.others");
    }

    public static boolean hasTopPermission(Player player) {
        return player.hasPermission("elytraessentials.*") ||
                player.hasPermission("elytraessentials.command.*") ||
                player.hasPermission("elytraessentials.command.top");
    }

    public static boolean hasForgePermission(Player player){
        return player.hasPermission("elytraessentials.*") ||
                player.hasPermission("elytraessentials.command.*") ||
                player.hasPermission("elytraessentials.command.forge");
    }

    public static boolean hasArmorPermission(Player player){
        return player.hasPermission("elytraessentials.*") ||
                player.hasPermission("elytraessentials.command.*") ||
                player.hasPermission("elytraessentials.command.armor");
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

    //<editor-fold desc="BOOSTS">

    public static boolean hasElytraBoostPermission(Player player) {
        return player.hasPermission("elytraessentials.*") ||
                player.hasPermission("elytraessentials.elytra.boost");
    }

    public static boolean hasElytraSuperBoostPermission(Player player) {
        return player.hasPermission("elytraessentials.*") ||
                player.hasPermission("elytraessentials.elytra.superboost");
    }

    //</editor-fold>
}
