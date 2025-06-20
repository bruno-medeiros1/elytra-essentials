package org.bruno.elytraEssentials.commands;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.gui.EffectsHolder;
import org.bruno.elytraEssentials.handlers.MessagesHandler;
import org.bruno.elytraEssentials.helpers.MessagesHelper;
import org.bruno.elytraEssentials.helpers.PermissionsHelper;
import org.bruno.elytraEssentials.interfaces.ISubCommand;
import org.bruno.elytraEssentials.utils.ElytraEffect;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class EffectsCommand implements ISubCommand {

    private final ElytraEssentials plugin;

    public EffectsCommand(ElytraEssentials plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean Execute(CommandSender sender, String[] args) {
        MessagesHandler messagesHandler = this.plugin.getMessagesHandlerInstance();
        MessagesHelper messagesHelper = this.plugin.getMessagesHelper();

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        boolean canOpen = PermissionsHelper.hasEffectsPermission((Player) sender);
        if (!canOpen) {
            messagesHelper.sendPlayerMessage((Player) sender, messagesHandler.getNoPermissionMessage());
            return true;
        }

        OpenOwnedEffects((Player) sender);
        return true;
    }

    private void OpenOwnedEffects(Player player) {
        Inventory ownedEffects = Bukkit.createInventory(new EffectsHolder(), 27, ChatColor.GOLD + "§lOwned Effects");

        try {
            Map<String, ElytraEffect> effects = plugin.getEffectsHandler().getEffectsRegistry();

            List<String> playerEffects = plugin.getDatabaseHandler().GetOwnedEffectKeys(player.getUniqueId());
            if (playerEffects.isEmpty()) {
                ItemStack item = plugin.getEffectsHandler().createEmptyItemStack();
                ownedEffects.setItem(13, item);
            }
            else {
                for (int i = 0; i < playerEffects.size(); i++) {
                    ElytraEffect elytraEffect = effects.getOrDefault(playerEffects.get(i), null);
                    if (elytraEffect != null) {
                        elytraEffect.setIsActive(plugin.getDatabaseHandler().GetIsActiveOwnedEffect(player.getUniqueId(), playerEffects.get(i)));

                        ItemStack item = plugin.getEffectsHandler().createOwnedItem(playerEffects.get(i) ,elytraEffect);
                        ownedEffects.setItem(i, item);
                    }
                }
            }

            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);
            player.openInventory(ownedEffects);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}