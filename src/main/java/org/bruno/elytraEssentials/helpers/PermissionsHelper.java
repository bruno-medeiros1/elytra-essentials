package org.bruno.elytraEssentials.helpers;

import org.bruno.elytraEssentials.utils.Constants;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * A utility class for handling all plugin permission checks.
 */
public final class PermissionsHelper {

    private PermissionsHelper() {}

    //<editor-fold desc="BYPASS PERMISSIONS (Player-Specific)">

    public static boolean playerBypassTimeLimit(Player player) {
        return player.hasPermission(Constants.Permissions.ALL) ||
                player.hasPermission(Constants.Permissions.ALL_BYPASS) ||
                player.hasPermission(Constants.Permissions.BYPASS_TIME_LIMIT);
    }

    public static boolean playerBypassSpeedLimit(Player player) {
        return player.hasPermission(Constants.Permissions.ALL) ||
                player.hasPermission(Constants.Permissions.ALL_BYPASS) ||
                player.hasPermission(Constants.Permissions.BYPASS_SPEED_LIMIT);
    }

    public static boolean playerBypassBoostCooldown(Player player) {
        return player.hasPermission(Constants.Permissions.ALL) ||
                player.hasPermission(Constants.Permissions.ALL_BYPASS) ||
                player.hasPermission(Constants.Permissions.BYPASS_BOOST_COOLDOWN);
    }

    public static boolean playerBypassElytraEquip(Player player) {
        return player.hasPermission(Constants.Permissions.ALL) ||
                player.hasPermission(Constants.Permissions.ALL_BYPASS) ||
                player.hasPermission(Constants.Permissions.BYPASS_EQUIPMENT);
    }

    public static boolean playerBypassCombatTag(Player player){
        return player.hasPermission(Constants.Permissions.ALL) ||
                player.hasPermission(Constants.Permissions.ALL_BYPASS) ||
                player.hasPermission(Constants.Permissions.BYPASS_COMBAT_TAG);
    }

    //</editor-fold>

    //<editor-fold desc="COMMAND PERMISSIONS (Sender-Agnostic)">

    public static boolean hasHelpPermission(CommandSender sender) {
        return sender.hasPermission(Constants.Permissions.ALL) ||
                sender.hasPermission(Constants.Permissions.ALL_COMMANDS) ||
                sender.hasPermission(Constants.Permissions.CMD_HELP);
    }

    public static boolean hasReloadPermission(CommandSender sender) {
        return sender.hasPermission(Constants.Permissions.ALL) ||
                sender.hasPermission(Constants.Permissions.ALL_COMMANDS) ||
                sender.hasPermission(Constants.Permissions.CMD_RELOAD);
    }

    public static boolean hasFlightTimeCommandPermission(CommandSender sender){
        return sender.hasPermission(Constants.Permissions.ALL) ||
                sender.hasPermission(Constants.Permissions.ALL_COMMANDS) ||
                sender.hasPermission(Constants.Permissions.CMD_FLIGHT_TIME);
    }

    public static boolean hasEffectsPermission(CommandSender sender) {
        return sender.hasPermission(Constants.Permissions.ALL) ||
                sender.hasPermission(Constants.Permissions.ALL_COMMANDS) ||
                sender.hasPermission(Constants.Permissions.CMD_EFFECTS);
    }

    public static boolean hasClearEffectsCommandPermission(CommandSender sender){
        return sender.hasPermission(Constants.Permissions.ALL) ||
                sender.hasPermission(Constants.Permissions.ALL_COMMANDS) ||
                sender.hasPermission(Constants.Permissions.CMD_EFFECTS_CLEAR);
    }

    public static boolean hasGiveEffectPermission(CommandSender sender) {
        return sender.hasPermission(Constants.Permissions.ALL) ||
                sender.hasPermission(Constants.Permissions.ALL_COMMANDS) ||
                sender.hasPermission(Constants.Permissions.CMD_EFFECTS_GIVE);
    }

    public static boolean hasRemoveEffectPermission(CommandSender sender) {
        return sender.hasPermission(Constants.Permissions.ALL) ||
                sender.hasPermission(Constants.Permissions.ALL_COMMANDS) ||
                sender.hasPermission(Constants.Permissions.CMD_EFFECTS_REMOVE);
    }

    public static boolean hasListEffectsPermission(CommandSender sender) {
        return sender.hasPermission(Constants.Permissions.ALL) ||
                sender.hasPermission(Constants.Permissions.ALL_COMMANDS) ||
                sender.hasPermission(Constants.Permissions.CMD_EFFECTS_LIST);
    }

    public static boolean hasShopPermission(CommandSender sender) {
        return sender.hasPermission(Constants.Permissions.ALL) ||
                sender.hasPermission(Constants.Permissions.ALL_COMMANDS) ||
                sender.hasPermission(Constants.Permissions.CMD_SHOP);
    }

    public static boolean hasStatsPermission(CommandSender sender) {
        return sender.hasPermission(Constants.Permissions.ALL) ||
                sender.hasPermission(Constants.Permissions.ALL_COMMANDS) ||
                sender.hasPermission(Constants.Permissions.CMD_STATS);
    }

    public static boolean hasStatsOthersPermission(CommandSender sender){
        return sender.hasPermission(Constants.Permissions.ALL) ||
                sender.hasPermission(Constants.Permissions.ALL_COMMANDS) ||
                sender.hasPermission(Constants.Permissions.CMD_STATS_OTHERS);
    }

    public static boolean hasTopPermission(CommandSender sender) {
        return sender.hasPermission(Constants.Permissions.ALL) ||
                sender.hasPermission(Constants.Permissions.ALL_COMMANDS) ||
                sender.hasPermission(Constants.Permissions.CMD_TOP);
    }

    public static boolean hasForgePermission(CommandSender sender){
        return sender.hasPermission(Constants.Permissions.ALL) ||
                sender.hasPermission(Constants.Permissions.ALL_COMMANDS) ||
                sender.hasPermission(Constants.Permissions.CMD_FORGE);
    }

    public static boolean hasArmorPermission(CommandSender sender) {
        return sender.hasPermission(Constants.Permissions.ALL) ||
                sender.hasPermission(Constants.Permissions.ALL_COMMANDS) ||
                sender.hasPermission(Constants.Permissions.CMD_ARMOR);
    }

    public static boolean hasRepairPermission(CommandSender sender){
        return sender.hasPermission(Constants.Permissions.ALL) ||
                sender.hasPermission(Constants.Permissions.ALL_COMMANDS) ||
                sender.hasPermission(Constants.Permissions.CMD_REPAIR);
    }

    public static boolean hasImportDbPermission(CommandSender sender){
        return sender.hasPermission(Constants.Permissions.ALL) ||
                sender.hasPermission(Constants.Permissions.ALL_COMMANDS) ||
                sender.hasPermission(Constants.Permissions.CMD_IMPORT_DB);
    }

    public static boolean hasAchievementsPermission(CommandSender sender){
        return sender.hasPermission(Constants.Permissions.ALL) ||
                sender.hasPermission(Constants.Permissions.ALL_COMMANDS) ||
                sender.hasPermission(Constants.Permissions.CMD_ACHIEVEMENTS);
    }

    //</editor-fold>

    //<editor-fold desc="FEATURE PERMISSIONS (Player-Specific)">

    public static boolean hasUpdateNotifyPermission(Player player){
        return player.hasPermission(Constants.Permissions.ALL) ||
                player.hasPermission(Constants.Permissions.NOTIFY_UPDATE);
    }

    public static boolean hasAllEffectsPermission(Player player) {
        return player.hasPermission(Constants.Permissions.ALL) ||
                player.hasPermission(Constants.Permissions.ALL_EFFECTS);
    }

    public static boolean hasElytraBoostPermission(Player player) {
        return player.hasPermission(Constants.Permissions.ALL) ||
                player.hasPermission(Constants.Permissions.BOOST);
    }

    public static boolean hasElytraSuperBoostPermission(Player player) {
        return player.hasPermission(Constants.Permissions.ALL) ||
                player.hasPermission(Constants.Permissions.SUPER_BOOST);
    }

    public static boolean hasChargedJumpPermission(Player player){
        return player.hasPermission(Constants.Permissions.ALL) ||
                player.hasPermission(Constants.Permissions.CHARGED_JUMP);
    }

    public static boolean hasAutoDeployPermission(Player player){
        return player.hasPermission(Constants.Permissions.ALL) ||
                player.hasPermission(Constants.Permissions.AUTO_DEPLOY);
    }

    //</editor-fold>
}
