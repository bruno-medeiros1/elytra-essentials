package org.bruno.elytraEssentials.listeners;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.helpers.PermissionsHelper;
import org.bruno.elytraEssentials.utils.ServerVersion;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

public class ElytraUpdaterListener implements Listener {
    private final ElytraEssentials plugin;

    public ElytraUpdaterListener(ElytraEssentials plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        boolean isCheckForUpdatesEnabled = plugin.getConfigHandlerInstance().getIsCheckForUpdatesEnabled();
        if (!isCheckForUpdatesEnabled) return;

        Player player = event.getPlayer();

        // Check for updates and send the interactive notification
        if (PermissionsHelper.hasUpdateNotifyPermission(player) && plugin.isNewerVersionAvailable) {
            sendUpdateNotification(player);
        }
    }

    /**
     * Creates and sends a clickable update notification to a player.
     * @param player The player to send the message to.
     */
    private void sendUpdateNotification(Player player) {
        String downloadUrl = "https://www.spigotmc.org/resources/126002/";

        TextComponent message = null;
        String text = ChatColor.translateAlternateColorCodes('&', plugin.getMessagesHandlerInstance().getPrefixMessage()) +
                " §7A new version is available (§av" +
                plugin.latestVersion +
                "§7)\n";

        if (plugin.getServerVersion().ordinal() >= ServerVersion.V_1_21.ordinal()) {
            message = new TextComponent(TextComponent.fromLegacy(text));
        } else {
            message = new TextComponent(TextComponent.fromLegacyText(text));
        }

        TextComponent linkComponent = getTextComponent(downloadUrl);
        message.addExtra(linkComponent);
        player.spigot().sendMessage(message);
    }

    private static @NotNull TextComponent getTextComponent(String downloadUrl) {
        TextComponent linkComponent = new TextComponent("§e§n" + downloadUrl);

        // Add the click event to open the URL
        linkComponent.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, downloadUrl));

        // Add a helpful hover message
        BaseComponent[] hoverText = new TextComponent[]{
                new TextComponent("§eClick here to open the plugin \n§epage in your web browser.")
        };
        linkComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hoverText)));
        return linkComponent;
    }
}
