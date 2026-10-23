package dev.example.prefix;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Translates legacy &-codes (&6, &l, &r, ...) into MiniMessage tags.
 *
 * Like vanilla, a color code resets any active formatting, so &l&6 is gold
 * but not bold. Write &6&l to get gold bold.
 */
public final class ColorCodes {

    private static final Map<Character, String> COLORS = Map.ofEntries(
            Map.entry('0', "black"),
            Map.entry('1', "dark_blue"),
            Map.entry('2', "dark_green"),
            Map.entry('3', "dark_aqua"),
            Map.entry('4', "dark_red"),
            Map.entry('5', "dark_purple"),
            Map.entry('6', "gold"),
            Map.entry('7', "gray"),
            Map.entry('8', "dark_gray"),
            Map.entry('9', "blue"),
            Map.entry('a', "green"),
            Map.entry('b', "aqua"),
            Map.entry('c', "red"),
            Map.entry('d', "light_purple"),
            Map.entry('e', "yellow"),
            Map.entry('f', "white")
    );

    private static final Map<Character, String> FORMATS = Map.of(
            'k', "obfuscated",
            'l', "bold",
            'm', "strikethrough",
            'n', "underlined",
            'o', "italic"
    );

    private static final Pattern CODE = Pattern.compile("&([0-9a-fk-orA-FK-OR])");

    private ColorCodes() {
    }

    /** Converts &-codes to MiniMessage tags. Other text is left as-is. */
    public static String toMiniMessage(String raw) {
        return CODE.matcher(raw).replaceAll(result -> {
            char code = Character.toLowerCase(result.group(1).charAt(0));
            if (code == 'r') {
                return "<reset>";
            }
            if (COLORS.containsKey(code)) {
                return "<reset><" + COLORS.get(code) + ">";
            }
            return "<" + FORMATS.get(code) + ">";
        });
    }

    /** Removes &-codes entirely. Used for players without prefix.color. */
    public static String strip(String raw) {
        return CODE.matcher(raw).replaceAll("");
    }
}
