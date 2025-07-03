package org.bruno.elytraEssentials.commands;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.helpers.PermissionsHelper;
import org.bruno.elytraEssentials.interfaces.ISubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HelpCommand implements ISubCommand {

    private final ElytraEssentials plugin;

    public HelpCommand(ElytraEssentials plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean Execute(CommandSender sender, String[] args) {
        ChatColor primary = ChatColor.GOLD;
        ChatColor secondary = ChatColor.YELLOW;
        ChatColor text = ChatColor.GRAY;
        String arrow = "» ";

        sender.sendMessage(primary + "§m----------------------------------------------------");
        sender.sendMessage("");
        sender.sendMessage(primary + "§lElytraEssentials " + secondary + "v" + plugin.getDescription().getVersion());
        sender.sendMessage(text + "All available commands are listed below.");
        sender.sendMessage("");

        boolean canReload = !(sender instanceof Player) || PermissionsHelper.hasReloadPermission((Player) sender);
        if (canReload) {
            sender.sendMessage(secondary + arrow + "/ee reload" + text + " - Reloads the plugin's configuration files.");
        }

        boolean canShop = !(sender instanceof Player) || PermissionsHelper.hasShopPermission((Player) sender);
        if (canShop) {
            sender.sendMessage(secondary + arrow + "/ee shop" + text + " - Opens the effects shop GUI.");
        }

        boolean canEffects = !(sender instanceof Player) || PermissionsHelper.hasEffectsPermission((Player) sender);
        if (canEffects) {
            sender.sendMessage(secondary + arrow + "/ee effects" + text + " - Opens the owned effects GUI.");
        }

        boolean canFt = !(sender instanceof Player) || PermissionsHelper.hasFlightTimeCommandPermission((Player) sender);
        if (canFt) {
            sender.sendMessage(secondary + arrow + "/ee ft <action> <player> <time>" + text + " - Manages player flight time.");
        }

        boolean canStats = !(sender instanceof Player) || PermissionsHelper.hasStatsPermission((Player) sender);
        if (canStats) {
            sender.sendMessage(secondary + arrow + "/ee stats" + text + " - View your personal flight statistics.");
        }

        boolean canTop = !(sender instanceof Player) || PermissionsHelper.hasTopPermission((Player) sender);
        if (canTop) {
            sender.sendMessage(secondary + arrow + "/ee top <category>" + text + " - View leaderboard statistics.");
        }

        boolean canForge = !(sender instanceof Player) || PermissionsHelper.hasForgePermission((Player) sender);
        if (canForge) {
            sender.sendMessage(secondary + arrow + "/ee forge" + text + " - Opens the forge GUI.");
        }

        boolean canArmor = !(sender instanceof Player) || PermissionsHelper.hasArmorPermission((Player) sender);
        if (canArmor) {
            sender.sendMessage(secondary + arrow + "/ee armor" + text + " - Outputs the status and remaining Armor Plating durability of a worn Armored Elytra.");
        }

        boolean canImport = !(sender instanceof Player) || PermissionsHelper.hasImportDbPermission((Player) sender);
        if (canImport) {
            sender.sendMessage(secondary + arrow + "/ee importdb <backup_filename> --confirm" + text + " - Restores a SQLite database backup.");
        }

        sender.sendMessage("");
        sender.sendMessage(primary + "§m----------------------------------------------------");

        return true;
    }
}
