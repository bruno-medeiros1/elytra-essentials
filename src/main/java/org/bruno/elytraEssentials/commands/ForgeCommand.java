package org.bruno.elytraEssentials.commands;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.utils.Constants;
import org.bruno.elytraEssentials.gui.ForgeHolder;
import org.bruno.elytraEssentials.helpers.GuiHelper;
import org.bruno.elytraEssentials.helpers.PermissionsHelper;
import org.bruno.elytraEssentials.interfaces.ISubCommand;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class ForgeCommand implements ISubCommand {

    private final ElytraEssentials plugin;

    public ForgeCommand(ElytraEssentials plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean Execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player))
            return true;

        if (!plugin.getConfigHandlerInstance().getIsArmoredElytraEnabled()){
            plugin.getMessagesHelper().sendPlayerMessage(player, plugin.getMessagesHandlerInstance().getFeatureNotEnabled());
            return true;
        }

        if (!PermissionsHelper.hasForgePermission(player)){
            plugin.getMessagesHelper().sendPlayerMessage(player, plugin.getMessagesHandlerInstance().getNoPermissionMessage());
            return true;
        }

        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 0.8f, 0.8f);
        OpenForgeGUI(player);
        return true;
    }

    private void OpenForgeGUI(Player player) {
        Inventory forge = Bukkit.createInventory(new ForgeHolder(), Constants.GUI.FORGE_INVENTORY_SIZE, Constants.GUI.FORGE_INVENTORY_NAME);

        ItemStack grayPane = GuiHelper.createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ");

        // First, fill the entire inventory with the gray filler pane
        for (int i = 0; i < Constants.GUI.FORGE_INVENTORY_SIZE; i++) {
            forge.setItem(i, grayPane);
        }

        // This creates the layout with inputs on the sides and the result in the middle.
        forge.setItem(Constants.GUI.FORGE_ELYTRA_SLOT, null);
        forge.setItem(Constants.GUI.FORGE_ARMOR_SLOT, null);
        forge.setItem(Constants.GUI.FORGE_RESULT_SLOT, null);

        // Place the instructional anvil item at the bottom
        ItemStack infoAnvil = GuiHelper.createGuiItem(Material.ANVIL,
                "§eElytra Forging",
                "§7Place an Elytra in the left slot",
                "§7and a Chestplate in the right slot.",
                "§7The result will appear in the middle."
        );
        forge.setItem(Constants.GUI.FORGE_INFO_ANVIL_SLOT, infoAnvil);

        // Open the GUI for the player
        player.openInventory(forge);
    }
}
