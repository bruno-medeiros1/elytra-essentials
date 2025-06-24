package org.bruno.elytraEssentials.helpers;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bruno.elytraEssentials.ElytraEssentials;
import org.bruno.elytraEssentials.utils.ServerVersion;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public class MessagesHelper {

    private final ElytraEssentials plugin;

    private static final ConsoleCommandSender consoleCommandSender = Bukkit.getConsoleSender();
    private String pluginPrefix;

    private static String messageColor;
    private static boolean debugEnabled;

    public MessagesHelper(ElytraEssentials plugin) {
        this.plugin = plugin;
        this.pluginPrefix = plugin.getMessagesHandlerInstance().getPrefixMessage();
    }

    public void sendActionBarMessage(Player player, String message) {
        String coloredMessage = ColorHelper.ParseColoredString(message);
        ServerVersion version = plugin.getServerVersion();

        if (version == ServerVersion.V_1_21) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(message));
        } else {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(coloredMessage));
        }
    }

    public void sendConsoleMessage(String string) {
        if (pluginPrefix == null) {
            pluginPrefix = plugin.getMessagesHandlerInstance().getPrefixMessage();
        }

        consoleCommandSender.sendMessage(ColorHelper.ParseColoredString(pluginPrefix + " &r" + string));
    }

    public void sendPlayerMessage(Player player, String string) {
        if (pluginPrefix == null) {
            pluginPrefix = plugin.getMessagesHandlerInstance().getPrefixMessage();
        }

        player.sendMessage(ColorHelper.ParseColoredString(pluginPrefix + " &r" + string));
    }

    public void sendConsoleLog(String object, String string) {
        if (pluginPrefix == null) {
            pluginPrefix = plugin.getMessagesHandlerInstance().getPrefixMessage();
        }

        if (object.equalsIgnoreCase("info")) {
            messageColor = "&a";
            object = "&a" + object;
        } else if (object.equalsIgnoreCase("warning")) {
            messageColor = "&e";
            object = "&e" + object;
        } else if (object.equalsIgnoreCase("error")) {
            messageColor = "&c";
            object = "&c" + object;
        } else if (object.equalsIgnoreCase("severe")) {
            messageColor = "&4";
            object = "&4" + object;
        } else {
            messageColor = "&7";
            object = "&7" + object;
        }
        consoleCommandSender.sendMessage(ColorHelper.ParseColoredString(pluginPrefix + " &r[" + object + "&r] - " + messageColor + string));
    }

    public void sendDebugMessage(String string) {
        if (debugEnabled) {
            consoleCommandSender.sendMessage(ColorHelper.ParseColoredString("&6Debug&7: " + string));
        }
    }

    public void setDebugMode(boolean value) {
        debugEnabled = value;
    }
}
