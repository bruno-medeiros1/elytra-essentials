package org.bruno.elytraEssentials.commands;

import org.bruno.elytraEssentials.gui.achievements.AchievementsGuiHandler;
import org.bruno.elytraEssentials.helpers.MessagesHelper;
import org.bruno.elytraEssentials.helpers.PermissionsHelper;
import org.bruno.elytraEssentials.interfaces.ISubCommand;
import org.bruno.elytraEssentials.utils.StatType;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AchievementsCommand implements ISubCommand {
    private final AchievementsGuiHandler achievementsGuiHandler;
    private final MessagesHelper messagesHelper;

    public AchievementsCommand(AchievementsGuiHandler achievementsGuiHandler, MessagesHelper messagesHelper) {
        this.achievementsGuiHandler = achievementsGuiHandler;
        this.messagesHelper = messagesHelper;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            messagesHelper.sendCommandSenderMessage(sender,"&cThis command can only be run by a player.");
            return true;
        }
        if (!PermissionsHelper.hasAchievementsPermission(player)) {
            messagesHelper.sendCommandSenderMessage(sender, "&cYou do not have permission to view achievements.");
            return true;
        }

        // Delegate directly to the handler
        achievementsGuiHandler.open(player, 0, StatType.UNKNOWN);
        return true;
    }
}