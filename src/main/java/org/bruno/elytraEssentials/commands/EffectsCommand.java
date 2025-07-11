package org.bruno.elytraEssentials.commands;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.helpers.ColorHelper;
import org.bruno.elytraEssentials.utils.Constants;
import org.bruno.elytraEssentials.gui.EffectsHolder;
import org.bruno.elytraEssentials.helpers.GuiHelper;
import org.bruno.elytraEssentials.helpers.PermissionsHelper;
import org.bruno.elytraEssentials.interfaces.ISubCommand;
import org.bruno.elytraEssentials.utils.ElytraEffect;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class EffectsCommand implements ISubCommand {
    private final ElytraEssentials plugin;

    public EffectsCommand(ElytraEssentials plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean Execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "This command can only be run by a player.");
                return true;
            }
            if (!PermissionsHelper.hasEffectsPermission(player)) {
                plugin.getMessagesHelper().sendPlayerMessage(player, plugin.getMessagesHandlerInstance().getNoPermissionMessage());
                return true;
            }
            openOwnedEffects(player);
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
                sender.sendMessage(ChatColor.RED + "Unknown subcommand. Usage: /ee effects <clear, give, remove, list>");
                break;
        }
        return true;
    }


    private void handleClear(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be run by a player.");
            return;
        }
        if (!PermissionsHelper.hasClearEffectsCommandPermission(player)) {
            plugin.getMessagesHelper().sendPlayerMessage(player, plugin.getMessagesHandlerInstance().getNoPermissionMessage());
            return;
        }

        String activeEffectKey = plugin.getEffectsHandler().getActiveEffect(player.getUniqueId());
        if (activeEffectKey == null) {
            player.sendMessage(ChatColor.RED + "You do not have an active effect to clear.");
            return;
        }
        plugin.getEffectsHandler().handleDeselection(player, activeEffectKey);
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (!PermissionsHelper.hasGiveEffectPermission(sender)) {
            sender.sendMessage(ColorHelper.parse(plugin.getMessagesHandlerInstance().getNoPermissionMessage()));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /ee effects give <player> <effect_id>");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(ChatColor.RED + "Player '" + args[1] + "' not found.");
            return;
        }

        String effectKey = args[2].toUpperCase();
        if (!plugin.getEffectsHandler().getEffectsRegistry().containsKey(effectKey)) {
            sender.sendMessage(ChatColor.RED + "Effect ID '" + effectKey + "' not found.");
            return;
        }

        if (target.isOnline() && PermissionsHelper.hasAllEffectsPermission(target.getPlayer())) {
            sender.sendMessage(ChatColor.YELLOW + target.getName() + " has access to all effects via permissions.");
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    List<String> ownedKeys = plugin.getDatabaseHandler().GetOwnedEffectKeys(target.getUniqueId());
                    if (ownedKeys.contains(effectKey)) {
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                sender.sendMessage(ChatColor.RED + target.getName() + " already owns this effect.");
                            }
                        }.runTask(plugin);
                        return;
                    }

                    plugin.getDatabaseHandler().AddOwnedEffect(target.getUniqueId(), effectKey);
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            sender.sendMessage(ChatColor.GREEN + "Successfully gave the " + effectKey + " effect to " + target.getName() + ".");
                        }
                    }.runTask(plugin);

                } catch (SQLException e) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            sender.sendMessage(ChatColor.RED + "A database error occurred.");
                        }
                    }.runTask(plugin);
                    plugin.getLogger().log(java.util.logging.Level.SEVERE, "Failed to give effect to " + target.getName(), e);
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (!PermissionsHelper.hasRemoveEffectPermission(sender)) {
            sender.sendMessage(ColorHelper.parse(plugin.getMessagesHandlerInstance().getNoPermissionMessage()));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /ee effects remove <player> <effect_id>");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(ChatColor.RED + "Player '" + args[1] + "' not found.");
            return;
        }

        String effectKey = args[2].toUpperCase();

        if (target.isOnline() && PermissionsHelper.hasAllEffectsPermission(target.getPlayer())) {
            sender.sendMessage(ChatColor.YELLOW + "Cannot remove effects from a player who has wildcard permissions.");
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    List<String> ownedKeys = plugin.getDatabaseHandler().GetOwnedEffectKeys(target.getUniqueId());
                    if (!ownedKeys.contains(effectKey)) {
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                sender.sendMessage(ChatColor.RED + target.getName() + " does not own this effect.");
                            }
                        }.runTask(plugin);
                        return;
                    }

                    plugin.getDatabaseHandler().removeOwnedEffect(target.getUniqueId(), effectKey);
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            sender.sendMessage(ChatColor.GREEN + "Successfully removed the " + effectKey + " effect from " + target.getName() + ".");
                        }
                    }.runTask(plugin);

                } catch (SQLException e) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            sender.sendMessage(ChatColor.RED + "A database error occurred.");
                        }
                    }.runTask(plugin);
                    plugin.getLogger().log(java.util.logging.Level.SEVERE, "Failed to remove effect from " + target.getName(), e);
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    private void handleList(CommandSender sender, String[] args) {
        if (!PermissionsHelper.hasListEffectsPermission(sender)) {
            sender.sendMessage(ColorHelper.parse(plugin.getMessagesHandlerInstance().getNoPermissionMessage()));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /ee effects list <player>");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(ChatColor.RED + "Player '" + args[1] + "' not found.");
            return;
        }

        sender.sendMessage(ChatColor.YELLOW + "Fetching owned effects for " + target.getName() + "...");

        new BukkitRunnable() {
            @Override
            public void run() {
                Set<String> allOwnedEffects;

                try {
                    // Get effects from the database
                    allOwnedEffects = new HashSet<>(plugin.getDatabaseHandler().GetOwnedEffectKeys(target.getUniqueId()));
                } catch (SQLException e) {
                    sender.sendMessage(ChatColor.RED + "A database error occurred while fetching owned effects.");
                    plugin.getLogger().log(java.util.logging.Level.SEVERE, "Failed to list effects for " + target.getName(), e);
                    return; // Stop if the database fails
                }

                // If the player is online, check their permissions
                if (target.isOnline()) {
                    Player targetPlayer = target.getPlayer();
                    if (targetPlayer != null) {
                        if (PermissionsHelper.hasAllEffectsPermission(targetPlayer)) {
                            // If they have the wildcard, add all registered effects.
                            allOwnedEffects.addAll(plugin.getEffectsHandler().getEffectsRegistry().keySet());
                        } else {
                            // Otherwise, check for each individual permission.
                            for (Map.Entry<String, ElytraEffect> entry : plugin.getEffectsHandler().getEffectsRegistry().entrySet()) {
                                if (targetPlayer.hasPermission(entry.getValue().getPermission())) {
                                    allOwnedEffects.add(entry.getKey());
                                }
                            }
                        }
                    }
                }

                // Switch back to the main thread to send the message
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        sender.sendMessage(ChatColor.GOLD + "--- " + target.getName() + "'s Owned Effects ---");
                        if (allOwnedEffects.isEmpty()) {
                            sender.sendMessage(ChatColor.GRAY + "This player does not have access to any effects.");
                        } else {
                            for (String key : allOwnedEffects) {
                                sender.sendMessage(ChatColor.YELLOW + " - " + key);
                            }
                        }
                    }
                }.runTask(plugin);
            }
        }.runTaskAsynchronously(plugin);
    }

    public void openOwnedEffects(Player player) {
        Inventory ownedEffects = Bukkit.createInventory(new EffectsHolder(), Constants.GUI.EFFECTS_INVENTORY_SIZE, Constants.GUI.EFFECTS_INVENTORY_NAME);

        Set<String> keysToDisplay;
        try {
            // Get effects from the database
            keysToDisplay = new HashSet<>(plugin.getDatabaseHandler().GetOwnedEffectKeys(player.getUniqueId()));
        } catch (SQLException e) {
            player.sendMessage(ChatColor.RED + "An error occurred while fetching your effects.");
            plugin.getLogger().log(Level.SEVERE, "Failed to open owned effects GUI for " + player.getName(), e);
            return;
        }

        // Add effects from permissions
        if (PermissionsHelper.hasAllEffectsPermission(player)) {
            keysToDisplay.addAll(plugin.getEffectsHandler().getEffectsRegistry().keySet());
        } else {
            for (Map.Entry<String, ElytraEffect> entry : plugin.getEffectsHandler().getEffectsRegistry().entrySet()) {
                if (player.hasPermission(entry.getValue().getPermission())) {
                    keysToDisplay.add(entry.getKey());
                }
            }
        }

        addControlButtons(ownedEffects);

        if (keysToDisplay.isEmpty()) {
            ItemStack item = plugin.getEffectsHandler().createEmptyItemStack();
            ownedEffects.setItem(13, item);
        } else {
            populateOwnedItems(ownedEffects, player, new ArrayList<>(keysToDisplay));
        }

        player.openInventory(ownedEffects);
    }

    private void populateOwnedItems(Inventory inv, Player player, List<String> playerOwnedKeys) {
        Map<String, ElytraEffect> allEffects = plugin.getEffectsHandler().getEffectsRegistry();
        String activeEffectKey = plugin.getEffectsHandler().getActiveEffect(player.getUniqueId());

        for (int i = 0; i < playerOwnedKeys.size(); i++) {
            if (i >= Constants.GUI.EFFECTS_ITEM_DISPLAY_LIMIT) break;

            String effectKey = playerOwnedKeys.get(i);
            ElytraEffect templateEffect = allEffects.get(effectKey);

            if (templateEffect != null) {
                ElytraEffect playerSpecificEffect = new ElytraEffect(templateEffect);
                playerSpecificEffect.setIsActive(effectKey.equals(activeEffectKey));
                ItemStack item = plugin.getEffectsHandler().createOwnedItem(effectKey, playerSpecificEffect);
                inv.setItem(i, item);
            }
        }
    }

    private void addControlButtons(Inventory inv) {
        inv.setItem(Constants.GUI.EFFECTS_SHOP_SLOT, GuiHelper.createGuiItem(Material.CHEST, "§aShop", "§7Click here to buy more effects."));
        inv.setItem(Constants.GUI.EFFECTS_PREVIOUS_PAGE_SLOT, GuiHelper.createPreviousPageButton(false));
        inv.setItem(Constants.GUI.EFFECTS_PAGE_INFO_SLOT, GuiHelper.createPageInfoItem(1, 1));
        inv.setItem(Constants.GUI.EFFECTS_NEXT_PAGE_SLOT, GuiHelper.createNextPageButton(false));
        inv.setItem(Constants.GUI.EFFECTS_CLOSE_SLOT, GuiHelper.createCloseButton());
    }

    @Override
    public List<String> getSubcommandCompletions(CommandSender sender, String[] args) {
        if (args.length == 2) {
            if (!(sender instanceof Player player))
                return List.of();

            List<String> actions = new ArrayList<>();
            if (PermissionsHelper.hasClearEffectsCommandPermission(player)) actions.add("clear");
            if (PermissionsHelper.hasGiveEffectPermission(player)) actions.add("give");
            if (PermissionsHelper.hasRemoveEffectPermission(player)) actions.add("remove");
            if (PermissionsHelper.hasListEffectsPermission(player)) actions.add("list");

            return actions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 3) {
            String action = args[1].toLowerCase();
            if (action.equals("give") || action.equals("remove") || action.equals("list")) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        if (args.length == 4) {
            String action = args[1].toLowerCase();
            if (action.equals("give") || action.equals("remove")) {
                return plugin.getEffectsHandler().getEffectsRegistry().keySet().stream()
                        .map(String::toLowerCase)
                        .filter(key -> key.startsWith(args[3].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        return List.of();
    }
}