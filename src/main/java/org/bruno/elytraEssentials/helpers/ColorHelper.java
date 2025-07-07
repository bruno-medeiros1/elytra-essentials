package org.bruno.elytraEssentials.helpers;

import org.bukkit.ChatColor;

public class ColorHelper {

    //  TODO: Review this method
    //  criar um método para poder evitar estes casos
    //  ChatColor.translateAlternateColorCodes('&', plugin.getMessagesHandlerInstance().getEffectGuiOwned())
    public static String ParseColoredString(String input) {
        // Splitting the input string into an array, keeping the '&' delimiter in the result.
        // The regex splits the string while preserving the '&' by using lookbehind and lookahead.
        String[] stringArray = input.split(String.format("((?<=%1$s)|(?=%1$s))", "&"));

        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < stringArray.length; ++i) {
            if (stringArray[i].equalsIgnoreCase("&")) {
                if (stringArray[++i].charAt(0) == '#') {
                    stringBuilder.append(net.md_5.bungee.api.ChatColor.of(stringArray[i].substring(0, 7)))
                            .append(stringArray[i].substring(7));
                    continue;
                }
                stringBuilder.append(ChatColor.translateAlternateColorCodes('&', "&" + stringArray[i]));
                continue;
            }
            // If the part is not '&', append it as is to the final string.
            stringBuilder.append(stringArray[i]);
        }

        // Return the fully processed string with color codes applied.
        return stringBuilder.toString();
    }
}
