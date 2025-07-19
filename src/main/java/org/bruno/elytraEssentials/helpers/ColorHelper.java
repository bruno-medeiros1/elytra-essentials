package org.bruno.elytraEssentials.helpers;

import net.md_5.bungee.api.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A utility class for handling chat color codes, including legacy and hex colors.
 */
public final class ColorHelper {
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private ColorHelper() {}

    /**
     * Parses a string containing both legacy (&) and hex (&#RRGGBB) color codes.
     * This is the primary method that should be used for all color parsing.
     *
     * @param input The string to parse.
     * @return The fully colored string.
     */
    public static String parse(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        // Manually parse hex codes first for reliability
        Matcher matcher = HEX_PATTERN.matcher(input);
        StringBuilder buffer = new StringBuilder(input.length() + 4 * 8);

        while (matcher.find()) {
            String group = matcher.group(1);
            matcher.appendReplacement(buffer, ChatColor.of("#" + group).toString());
        }

        String hexParsedString = matcher.appendTail(buffer).toString();

        return ChatColor.translateAlternateColorCodes('&', hexParsedString);
    }
}
