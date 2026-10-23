package dev.example.prefix;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores prefixes and keeps the tab list and nametag in sync.
 *
 * Folia threading rules used here:
 *  - Per-player data (playerListName) is changed on the player's entity scheduler.
 *  - Scoreboard teams are shared state, so they are changed on the global region scheduler.
 *  - File I/O runs on the async scheduler.
 */
public final class PrefixManager {

    private final PrefixPlugin plugin;
    private final MiniMessage mini = MiniMessage.miniMessage();
    private final PlainTextComponentSerializer plain = PlainTextComponentSerializer.plainText();
    private final Map<UUID, String> prefixes = new ConcurrentHashMap<>();
    private final File dataFile;

    public PrefixManager(PrefixPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
    }

    // ---------- persistence ----------

    public void load() {
        if (!dataFile.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(dataFile);
        for (String key : yaml.getKeys(false)) {
            try {
                prefixes.put(UUID.fromString(key), yaml.getString(key));
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("Skipping invalid UUID in data.yml: " + key);
            }
        }
    }

    /** Blocking save, used on shutdown. */
    public void saveNow() {
        writeToDisk(serialize());
    }

    /** Non-blocking save, used during normal play. */
    public void saveAsync() {
        String content = serialize();
        Bukkit.getAsyncScheduler().runNow(plugin, task -> writeToDisk(content));
    }

    private String serialize() {
        YamlConfiguration yaml = new YamlConfiguration();
        prefixes.forEach((uuid, raw) -> yaml.set(uuid.toString(), raw));
        return yaml.saveToString();
    }

    private void writeToDisk(String content) {
        try {
            plugin.getDataFolder().mkdirs();
            Files.writeString(dataFile.toPath(), content);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save data.yml: " + e.getMessage());
        }
    }

    // ---------- prefix access ----------

    public MiniMessage mini() {
        return mini;
    }

    public boolean hasPrefix(UUID id) {
        return prefixes.containsKey(id);
    }

    /** Returns the prefix as a component with a trailing space, or Component.empty() if none. */
    public Component renderPrefix(UUID id) {
        String raw = prefixes.get(id);
        if (raw == null) return Component.empty();
        // Reset after the prefix so its color and bold don't leak into the name or message
        return mini.deserialize(raw + "<reset>").append(Component.space());
    }

    public String plainText(String miniMessage) {
        return plain.serialize(mini.deserialize(miniMessage));
    }

    public void setPrefix(Player player, String raw) {
        prefixes.put(player.getUniqueId(), raw);
        saveAsync();
        applyDisplay(player);
    }

    public void clearPrefix(Player player) {
        prefixes.remove(player.getUniqueId());
        saveAsync();
        applyDisplay(player);
    }

    // ---------- display (tab list + nametag) ----------

    public void applyDisplay(Player player) {
        final boolean has = hasPrefix(player.getUniqueId());
        final Component prefix = renderPrefix(player.getUniqueId());
        final String name = player.getName();
        final UUID uuid = player.getUniqueId();

        // Shared scoreboard state -> global region scheduler
        Bukkit.getGlobalRegionScheduler().run(plugin, task -> updateTeam(uuid, name, has, prefix));

        // Per-player tab list name -> the player's own region/scheduler
        player.getScheduler().run(plugin, task -> {
            if (has) {
                // Wrapping in an empty root keeps the name from inheriting the prefix's style
                player.playerListName(Component.empty().append(prefix).append(Component.text(name)));
            } else {
                player.playerListName(null); // null resets to default
            }
        }, null);
    }

    private void updateTeam(UUID uuid, String name, boolean has, Component prefix) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        String teamName = teamName(uuid);
        Team team = board.getTeam(teamName);

        if (!has) {
            if (team != null) team.unregister();
            return;
        }
        if (team == null) {
            team = board.registerNewTeam(teamName);
        }
        team.prefix(prefix);
        if (!team.hasEntry(name)) {
            team.addEntry(name);
        }
    }

    /** Team names are capped at 16 characters on older clients, so keep it short. */
    private static String teamName(UUID uuid) {
        return "pfx" + uuid.toString().replace("-", "").substring(0, 13);
    }
}
