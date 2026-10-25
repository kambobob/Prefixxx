package dev.example.prefix;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.OfflinePlayer;

/**
 * Exposes %prefixplus_tab%: the PrefixPlus prefix if the player has one,
 * otherwise the LuckPerms prefix. TAB reads this placeholder for the tab list.
 */
public final class PrefixExpansion extends PlaceholderExpansion {

    private final PrefixPlugin plugin;
    private final PrefixManager manager;

    public PrefixExpansion(PrefixPlugin plugin, PrefixManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public String getIdentifier() {
        return "prefixplus";
    }

    @Override
    public String getAuthor() {
        return "you";
    }

    @Override
    public String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true; // keep registered across /papi reload
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null) {
            return "";
        }

        if (params.equalsIgnoreCase("tab")) {
            if (manager.hasPrefix(player.getUniqueId())) {
                // Legacy section codes are what TAB and PlaceholderAPI output expects
                return LegacyComponentSerializer.legacySection()
                        .serialize(manager.renderPrefix(player.getUniqueId()));
            }
            return fallbackLuckPermsPrefix(player);
        }

        return null; // unknown placeholder
    }

    /** Returns the LuckPerms prefix, or "" if LuckPerms isn't loaded. */
    private String fallbackLuckPermsPrefix(OfflinePlayer player) {
        String result = PlaceholderAPI.setPlaceholders(player, "%luckperms_prefix%");
        // If LuckPerms isn't registered, PlaceholderAPI returns the placeholder text unchanged
        if (result == null || result.contains("%")) {
            return "";
        }
        return result;
    }
}
