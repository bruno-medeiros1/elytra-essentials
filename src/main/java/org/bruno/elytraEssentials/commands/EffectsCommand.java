package org.bruno.elytraEssentials.commands;

import org.bruno.elytraEssentials.ElytraEssentials;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EffectsCommand implements ISubCommand {
    private final ElytraEssentials plugin;

    public EffectsCommand(ElytraEssentials plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean Execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            // Logic for /ee effects (opens GUI)
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "This command can only be run by a player.");
                return true;
            }
            if (!PermissionsHelper.hasEffectsPermission(player)) {
                plugin.getMessagesHelper().sendPlayerMessage(player, plugin.getMessagesHandlerInstance().getNoPermissionMessage());
                return true;
            }
            OpenOwnedEffects(player);
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
        if (!sender.hasPermission("elytraessentials.effects.give")) {
            sender.sendMessage(plugin.getMessagesHandlerInstance().getNoPermissionMessage());
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

        if (target.isOnline() && PermissionsHelper.hasElytraEffectsPermission(target.getPlayer())) {
            sender.sendMessage(ChatColor.YELLOW + target.getName() + " has access to all effects via permissions.");
            return;
        }

        try {
            plugin.getDatabaseHandler().AddOwnedEffect(target.getUniqueId(), effectKey);
            sender.sendMessage(ChatColor.GREEN + "Successfully gave the " + effectKey + " effect to " + target.getName() + ".");
        } catch (SQLException e) {
            sender.sendMessage(ChatColor.RED + "A database error occurred.");
            e.printStackTrace();
        }
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (!sender.hasPermission("elytraessentials.effects.remove")) {
            sender.sendMessage(plugin.getMessagesHandlerInstance().getNoPermissionMessage());
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

        if (target.isOnline() && PermissionsHelper.hasElytraEffectsPermission(target.getPlayer())) {
            sender.sendMessage(ChatColor.YELLOW + target.getName() + " has access to all effects via permissions.");
            return;
        }

        try {
            plugin.getDatabaseHandler().removeOwnedEffect(target.getUniqueId(), effectKey);
            sender.sendMessage(ChatColor.GREEN + "Successfully removed the " + effectKey + " effect from " + target.getName() + ".");
        } catch (SQLException e) {
            sender.sendMessage(ChatColor.RED + "A database error occurred.");
            e.printStackTrace();
        }
    }

    private void handleList(CommandSender sender, String[] args) {
        if (!sender.hasPermission("elytraessentials.effects.list")) {
            sender.sendMessage(plugin.getMessagesHandlerInstance().getNoPermissionMessage());
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
                try {
                    List<String> ownedKeys = plugin.getDatabaseHandler().GetOwnedEffectKeys(target.getUniqueId());
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            sender.sendMessage(ChatColor.GOLD + "--- " + target.getName() + "'s Owned Effects ---");
                            if (ownedKeys.isEmpty()) {
                                sender.sendMessage(ChatColor.GRAY + "This player does not own any effects.");
                            } else {
                                for (String key : ownedKeys) {
                                    sender.sendMessage(ChatColor.YELLOW + " - " + key);
                                }
                            }
                        }
                    }.runTask(plugin);
                } catch (SQLException e) {
                    sender.sendMessage(ChatColor.RED + "A database error occurred.");
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    public void OpenOwnedEffects(Player player) {
        Inventory ownedEffects = Bukkit.createInventory(new EffectsHolder(), Constants.GUI.EFFECTS_INVENTORY_SIZE, Constants.GUI.EFFECTS_INVENTORY_NAME);

        try {
            List<String> keysToDisplay;

            if (PermissionsHelper.hasElytraEffectsPermission(player)) {
                keysToDisplay = new ArrayList<>(plugin.getEffectsHandler().getEffectsRegistry().keySet());
            } else {
                keysToDisplay = plugin.getDatabaseHandler().GetOwnedEffectKeys(player.getUniqueId());
            }

            // Add the static control buttons to the bottom row
            addControlButtons(ownedEffects);

            // Now, check if the final list of keys to display is empty.
            if (keysToDisplay.isEmpty()) {
                ItemStack item = plugin.getEffectsHandler().createEmptyItemStack();
                ownedEffects.setItem(13, item); // Center slot
            } else {
                // If there are keys to display, populate the GUI with them.
                populateOwnedItems(ownedEffects, player, keysToDisplay);
            }

            player.openInventory(ownedEffects);

        } catch (SQLException e) {
            player.sendMessage(ChatColor.RED + "An error occurred while fetching your effects.");
            e.printStackTrace();
        }
    }

    //  Fills the GUI with the player's owned effects, up to the display limit.
    private void populateOwnedItems(Inventory inv, Player player, List<String> playerOwnedKeys) throws SQLException {
        Map<String, ElytraEffect> allEffects = plugin.getEffectsHandler().getEffectsRegistry();

        for (int i = 0; i < playerOwnedKeys.size(); i++) {
            if (i >= Constants.GUI.EFFECTS_ITEM_DISPLAY_LIMIT)
                break;

            String effectKey = playerOwnedKeys.get(i);
            ElytraEffect elytraEffect = allEffects.get(effectKey);

            if (elytraEffect != null) {
                if (!PermissionsHelper.hasElytraEffectsPermission(player)) {
                    boolean isActive = plugin.getDatabaseHandler().GetIsActiveOwnedEffect(player.getUniqueId(), effectKey);
                    elytraEffect.setIsActive(isActive);
                }

                ItemStack item = plugin.getEffectsHandler().createOwnedItem(effectKey, elytraEffect);
                inv.setItem(i, item);
            }
        }
    }

    //  Creates and places the static control buttons at the bottom of the GUI.
    private void addControlButtons(Inventory inv) {
        inv.setItem(Constants.GUI.EFFECTS_SHOP_SLOT, GuiHelper.createGuiItem(Material.CHEST, "§aShop", "§7Click here to buy more effects."));
        inv.setItem(Constants.GUI.EFFECTS_PREVIOUS_PAGE_SLOT, GuiHelper.createGuiItem(Material.RED_STAINED_GLASS_PANE, "§cPrevious Page", "§7You are on the first page."));
        inv.setItem(Constants.GUI.EFFECTS_PAGE_INFO_SLOT, GuiHelper.createGuiItem(Material.COMPASS, "§ePage 1/1"));
        inv.setItem(Constants.GUI.EFFECTS_NEXT_PAGE_SLOT, GuiHelper.createGuiItem(Material.GREEN_STAINED_GLASS_PANE, "§aNext Page", "§7You are on the last page."));
        inv.setItem(Constants.GUI.EFFECTS_CLOSE_SLOT, GuiHelper.createGuiItem(Material.BARRIER, "§cClose Menu", "§7Click to exit."));
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