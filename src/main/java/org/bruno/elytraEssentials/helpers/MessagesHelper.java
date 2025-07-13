package org.bruno.elytraEssentials.helpers;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.utils.ServerVersion;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * A centralized utility class for sending all formatted player and console messages.
 */
public class MessagesHelper {

    private final ElytraEssentials plugin;
    private final String pluginPrefix;
    private boolean debugEnabled = false;

    public MessagesHelper(ElytraEssentials plugin) {
        this.plugin = plugin;
        this.pluginPrefix = plugin.getMessagesHandlerInstance().getPrefixMessage();
    }

    /**
     * Creates and sends a clickable update notification to a player.
     * @param player The player to send the message to.
     * @param latestVersion The latest version string to display.
     */
    public void sendUpdateNotification(Player player, String latestVersion) {
        String downloadUrl = "https://www.spigotmc.org/resources/126002/";

        String text = this.pluginPrefix + "§7A new version is available (§av" + latestVersion + "§7)! ";

        TextComponent message = new TextComponent(TextComponent.fromLegacyText(ColorHelper.parse(text)));
        TextComponent linkComponent = new TextComponent("§e§l[Click Here to Download]");

        linkComponent.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, downloadUrl));

        BaseComponent[] hoverText = new TextComponent[]{
                new TextComponent(TextComponent.fromLegacyText("§bClick to open the plugin page\n§bin your web browser."))
        };
        linkComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hoverText)));

        message.addExtra(linkComponent);
        player.spigot().sendMessage(message);
    }

    /**
     * Sends a formatted action bar message to a player.
     * @param player The player who will receive the message.
     * @param message The raw string message to send (supports '&' color codes).
     */
    public void sendActionBarMessage(Player player, String message) {
        String coloredMessage = ColorHelper.parse(message);
        ServerVersion version = plugin.getServerVersion();

        if (version == ServerVersion.V_1_21) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(coloredMessage));
        } else {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(coloredMessage));
        }
    }

    /**
     * Sends a standard, prefixed message to the server console.
     * @param message The message to send.
     */
    public void sendConsoleMessage(String message) {
        String formattedMessage = ColorHelper.parse(pluginPrefix + "&r" + message);
        Bukkit.getConsoleSender().sendMessage(formattedMessage);
    }

    /**
     * Sends a standard, prefixed message to a specific player.
     * @param player The player to send the message to.
     * @param message The message to send.
     */
    public void sendPlayerMessage(Player player, String message) {
        String formattedMessage = ColorHelper.parse(pluginPrefix + "&r" + message);
        player.sendMessage(formattedMessage);
    }

    public void sendCommandSenderMessage(CommandSender sender, String message){
        String formattedMessage = ColorHelper.parse(pluginPrefix + "&r" + message);
        sender.sendMessage(formattedMessage);
    }

    /**
     * Sends a debug message to the console, only if debug mode is enabled.
     * @param message The debug message to send.
     */
    public void sendDebugMessage(String message) {
        if (debugEnabled) {
            String formattedMessage = ColorHelper.parse(pluginPrefix + "&r[&6Debug&r] - " + message);
            Bukkit.getConsoleSender().sendMessage(formattedMessage);
        }
    }

    /**
     * Enables or disables debug mode for this helper instance.
     * @param enabled true to enable, false to disable.
     */
    public void setDebugMode(boolean enabled) {
        this.debugEnabled = enabled;
    }
}
