package org.bruno.elytraEssentials.helpers;

import org.bruno.elytraEssentials.ElytraEssentials;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public class MessagesHelper {
    private static final ConsoleCommandSender consoleCommandSender = Bukkit.getConsoleSender();

    private static final String pluginPrefix = ElytraEssentials.getPlugin(ElytraEssentials.class).getMessagesHandlerInstance().getPrefixMessage();
    private static String messageColor;

    private static boolean debugEnabled;

    public static void sendConsoleMessage(String string) {
        consoleCommandSender.sendMessage(ColorHelper.ParseColoredString(pluginPrefix + " &r" + string));
    }

    public static void sendPlayerMessage(Player player, String string) {
        player.sendMessage(ColorHelper.ParseColoredString(pluginPrefix + " &r" + string));
    }

    public static void sendConsoleLog(String object, String string) {
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

    public static void SendDebugMessage(String string) {
        if (debugEnabled) {
            consoleCommandSender.sendMessage(ColorHelper.ParseColoredString("&CDebug&7: " + string));
        }
    }

    public static void SetDebugMode(boolean value) {
        debugEnabled = value;
    }
}
