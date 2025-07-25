package org.bruno.elytraEssentials.commands;

import org.bruno.elytraEssentials.handlers.DatabaseHandler;
import org.bruno.elytraEssentials.handlers.EffectsHandler;
import org.bruno.elytraEssentials.gui.effects.EffectsGuiHandler;
import org.bruno.elytraEssentials.handlers.MessagesHandler;
import org.bruno.elytraEssentials.helpers.FoliaHelper;
import org.bruno.elytraEssentials.helpers.MessagesHelper;
import org.bruno.elytraEssentials.helpers.PermissionsHelper;
import org.bruno.elytraEssentials.utils.ElytraEffect;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EffectsCommand implements SubCommand {
    private final Logger logger;
    private final EffectsGuiHandler effectsGuiHandler;
    private final EffectsHandler effectsHandler;
    private final DatabaseHandler databaseHandler;
    private final FoliaHelper foliaHelper;
    private final MessagesHelper messagesHelper;
    private final MessagesHandler messagesHandler;

    public EffectsCommand(Logger logger, EffectsGuiHandler guiHandler, EffectsHandler effectsHandler, DatabaseHandler dbHandler, FoliaHelper fHelper,
                          MessagesHelper messagesHelper, MessagesHandler messagesHandler) {
        this.logger = logger;
        this.effectsGuiHandler = guiHandler;
        this.effectsHandler = effectsHandler;
        this.databaseHandler = dbHandler;
        this.foliaHelper = fHelper;
        this.messagesHelper = messagesHelper;
        this.messagesHandler = messagesHandler;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                messagesHelper.sendCommandSenderMessage(sender,"&cThis command can only be run by a player.");
                return true;
            }

            if (!PermissionsHelper.hasEffectsPermission(player)) {
                messagesHelper.sendPlayerMessage(player, messagesHandler.getNoPermissionMessage());
                return true;
            }

            player.playSound(player.getLocation(), Sound.UI_LOOM_SELECT_PATTERN, 0.8f, 0.8f);
            effectsGuiHandler.open(player);
            return true;
        }

        String action = args[0].toLowerCase();
        switch (action) {
            case "clear":
                handleClear(sender, args);
                break;
            case "give":
                handleGive(sender, args);
                break;
            case "remove":
                handleRemove(sender, args);
                break;
            case "list":
                handleList(sender, args);
                break;
            default:
                messagesHelper.sendCommandSenderMessage(sender,"&cUnknown subcommand. Usage: /ee effects <clear, give, remove, list>");
                break;
        }
        return true;
    }

    private void handleClear(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            messagesHelper.sendCommandSenderMessage(sender,"&cThis command can only be run by a player.");
            return;
        }
        if (!PermissionsHelper.hasClearEffectsCommandPermission(player)) {
            messagesHelper.sendPlayerMessage(player, messagesHandler.getNoPermissionMessage());
            return;
        }

        String activeEffectKey = effectsHandler.getActiveEffect(player.getUniqueId());
        if (activeEffectKey == null) {
            messagesHelper.sendPlayerMessage(player, messagesHandler.getNoActiveEffectToClear());
            return;
        }
        effectsHandler.handleDeselection(player, activeEffectKey);
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (!PermissionsHelper.hasGiveEffectPermission(sender)) {
            messagesHelper.sendCommandSenderMessage(sender, messagesHandler.getNoPermissionMessage());
            return;
        }
        if (args.length < 3) {
            messagesHelper.sendCommandSenderMessage(sender,"&cUsage: /ee effects give <player> <effect>");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            messagesHelper.sendCommandSenderMessage(sender, messagesHandler.getPlayerNotFound().replace("{0}", args[1]));
            return;
        }

        String effectKey = args[2].toUpperCase();
        if (!effectsHandler.getEffectsRegistry().containsKey(effectKey)) {
            messagesHelper.sendCommandSenderMessage(sender,"&cEffect '" + effectKey + "' not found.");
            return;
        }

        if (target.isOnline() && PermissionsHelper.hasAllEffectsPermission(target.getPlayer())) {
            messagesHelper.sendCommandSenderMessage(sender,"&e" + target.getName() + " has access to all effects via permissions.");
            return;
        }

        // Use the Folia-safe async task
        foliaHelper.runAsyncTask(() -> {
            try {
                List<String> ownedKeys = databaseHandler.getOwnedEffectKeys(target.getUniqueId());
                if (ownedKeys.contains(effectKey)) {
                    // Return to the main thread to send the message
                    foliaHelper.runTaskOnMainThread(() ->
                            messagesHelper.sendCommandSenderMessage(sender,"&c" + target.getName() + " already owns this effect.")
                    );
                    return;
                }

                databaseHandler.addOwnedEffect(target.getUniqueId(), effectKey);

                // Return to the main thread to send the message
                foliaHelper.runTaskOnMainThread(() ->
                        messagesHelper.sendCommandSenderMessage(sender, messagesHandler.getGiveEffectSuccess().replace("{0}", effectKey).replace("{1}", target.getName()))
                );

            } catch (SQLException e) {
                // Return to the main thread to send the error message
                foliaHelper.runTaskOnMainThread(() ->
                        messagesHelper.sendCommandSenderMessage(sender,"&cA database error occurred.")
                );
                logger.log(Level.SEVERE, "Failed to give effect to " + target.getName(), e);
            }
        });
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (!PermissionsHelper.hasRemoveEffectPermission(sender)) {
            messagesHelper.sendCommandSenderMessage(sender, messagesHandler.getNoPermissionMessage());
            return;
        }
        if (args.length < 3) {
            messagesHelper.sendCommandSenderMessage(sender,"&cUsage: /ee effects remove <player> <effect>");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            messagesHelper.sendCommandSenderMessage(sender, messagesHandler.getPlayerNotFound().replace("{0}",  args[1]));
            return;
        }

        String effectKey = args[2].toUpperCase();

        if (target.isOnline() && PermissionsHelper.hasAllEffectsPermission(target.getPlayer())) {
            messagesHelper.sendCommandSenderMessage(sender,"&eCannot remove effects from a player who has wildcard permissions.");
            return;
        }

        // Use the Folia-safe async task
        foliaHelper.runAsyncTask(() -> {
            try {
                List<String> ownedKeys = databaseHandler.getOwnedEffectKeys(target.getUniqueId());
                if (!ownedKeys.contains(effectKey)) {
                    foliaHelper.runTaskOnMainThread(() ->
                            messagesHelper.sendCommandSenderMessage(sender,"&c" + target.getName() + " does not own this effect.")
                    );
                    return;
                }

                databaseHandler.removeOwnedEffect(target.getUniqueId(), effectKey);

                foliaHelper.runTaskOnMainThread(() ->
                        messagesHelper.sendCommandSenderMessage(sender,"&aSuccessfully removed the " + effectKey + " effect from " + target.getName() + ".")
                );

            } catch (SQLException e) {
                foliaHelper.runTaskOnMainThread(() ->
                        messagesHelper.sendCommandSenderMessage(sender,"&cA database error occurred.")
                );
                logger.log(Level.SEVERE, "Failed to remove effect from " + target.getName(), e);
            }
        });
    }

    private void handleList(CommandSender sender, String[] args) {
        if (!PermissionsHelper.hasListEffectsPermission(sender)) {
            messagesHelper.sendCommandSenderMessage(sender, messagesHandler.getNoPermissionMessage());
            return;
        }
        if (args.length < 2) {
            messagesHelper.sendCommandSenderMessage(sender,"&cUsage: /ee effects list <player>");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            messagesHelper.sendCommandSenderMessage(sender,messagesHandler.getPlayerNotFound().replace("{0}",  args[1]));
            return;
        }

        messagesHelper.sendCommandSenderMessage(sender,"&eFetching owned effects for " + target.getName() + "...");

        // Use the Folia-safe async task
        foliaHelper.runAsyncTask(() -> {
            Set<String> allOwnedEffects;
            try {
                allOwnedEffects = new HashSet<>(databaseHandler.getOwnedEffectKeys(target.getUniqueId()));
            } catch (SQLException e) {
                foliaHelper.runTaskOnMainThread(() ->
                        messagesHelper.sendCommandSenderMessage(sender,"&cA database error occurred while fetching owned effects.")
                );
                logger.log(Level.SEVERE, "Failed to list effects for " + target.getName(), e);
                return;
            }

            // Permission checks are safe to do async
            if (target.isOnline()) {
                Player targetPlayer = target.getPlayer();
                if (targetPlayer != null) {
                    if (PermissionsHelper.hasAllEffectsPermission(targetPlayer)) {
                        // If they have the wildcard, add all registered effects.
                        allOwnedEffects.addAll(effectsHandler.getEffectsRegistry().keySet());
                    } else {
                        // Otherwise, check for each individual permission.
                        for (Map.Entry<String, ElytraEffect> entry : effectsHandler.getEffectsRegistry().entrySet()) {
                            if (targetPlayer.hasPermission(entry.getValue().getPermission())) {
                                allOwnedEffects.add(entry.getKey());
                            }
                        }
                    }
                }
            }

            // Switch back to the main thread to send the final message list
            foliaHelper.runTaskOnMainThread(() -> {
                sender.sendMessage("§6--- " + target.getName() + "'s Owned Effects ---");
                if (allOwnedEffects.isEmpty()) {
                    sender.sendMessage("§7This player does not have access to any effects.");
                } else {
                    for (String key : allOwnedEffects) {
                        sender.sendMessage("§e - " + key);
                    }
                }
            });
        });
    }
}