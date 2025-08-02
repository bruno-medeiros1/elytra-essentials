package org.bruno.elytraEssentials.gui;

import org.bruno.elytraEssentials.gui.achievements.AchievementsHolder;
import org.bruno.elytraEssentials.gui.effects.EffectsHolder;
import org.bruno.elytraEssentials.gui.forge.ForgeHolder;
import org.bruno.elytraEssentials.gui.shop.ShopHolder;
import org.bruno.elytraEssentials.gui.achievements.AchievementsGuiHandler;
import org.bruno.elytraEssentials.gui.effects.EffectsGuiHandler;
import org.bruno.elytraEssentials.gui.forge.ForgeGuiHandler;
import org.bruno.elytraEssentials.gui.shop.ShopGuiHandler;
import org.bruno.elytraEssentials.gui.upgrade.UpgradeGuiHandler;
import org.bruno.elytraEssentials.gui.upgrade.UpgradeHolder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class GuiListener implements Listener {

    private final ShopGuiHandler shopGuiHandler;
    private final ForgeGuiHandler forgeGuiHandler;
    private final EffectsGuiHandler effectsGuiHandler;
    private final AchievementsGuiHandler achievementsGuiHandler;
    private final UpgradeGuiHandler upgradeGuiHandler;

    public GuiListener(ShopGuiHandler shopGuiHandler, ForgeGuiHandler forgeGuiHandler, EffectsGuiHandler effectsGuiHandler, AchievementsGuiHandler achievementsGuiHandler,
                       UpgradeGuiHandler upgradeGuiHandler) {
        this.shopGuiHandler = shopGuiHandler;
        this.forgeGuiHandler = forgeGuiHandler;
        this.effectsGuiHandler = effectsGuiHandler;
        this.achievementsGuiHandler = achievementsGuiHandler;
        this.upgradeGuiHandler = upgradeGuiHandler;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() == null || !(event.getWhoClicked() instanceof Player)) {
            return;
        }

        if (event.getInventory().getHolder() instanceof ShopHolder){
            shopGuiHandler.handleClick(event);
        }
        else if (event.getInventory().getHolder() instanceof ForgeHolder) {
            forgeGuiHandler.handleClick(event);
        }
        else if (event.getInventory().getHolder() instanceof EffectsHolder) {
            effectsGuiHandler.handleClick(event);
        }
        else if (event.getInventory().getHolder() instanceof AchievementsHolder) {
            achievementsGuiHandler.handleClick(event);
        }
        else if (event.getInventory().getHolder() instanceof UpgradeHolder) {
            upgradeGuiHandler.handleClick(event);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        shopGuiHandler.clearPlayerData(player);
        forgeGuiHandler.clearPlayerData(player);
        achievementsGuiHandler.clearPlayerData(player);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof ForgeHolder) {
            forgeGuiHandler.handleClose(event);
        }
    }
}