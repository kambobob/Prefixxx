package dev.example.prefix;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public final class PrefixCommand implements CommandExecutor, TabCompleter {

    private final PrefixPlugin plugin;
    private final PrefixManager manager;

    public PrefixCommand(PrefixPlugin plugin, PrefixManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "set" -> setOwn(sender, args);
            case "clear" -> clearOwn(sender);
            case "admin" -> admin(sender, args);
            case "reload" -> reload(sender);
            default -> sendUsage(sender);
        }
        return true;
    }

    // ---------- subcommands ----------

    private void setOwn(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            send(sender, NamedTextColor.RED, "Only players can set a prefix.");
            return;
        }
        if (!player.hasPermission("prefix.use")) {
            send(sender, NamedTextColor.RED, "You don't have permission to do that.");
            return;
        }
        if (args.length < 2) {
            send(sender, NamedTextColor.RED, "Usage: /prefix set <text>");
            return;
        }
        String raw = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).trim();
        String text = sanitize(sender, raw);
        String error = check(text);
        if (error != null) {
            send(sender, NamedTextColor.RED, error);
            return;
        }
        manager.setPrefix(player, text);
        send(sender, NamedTextColor.GREEN, "Your prefix has been set.");
    }

    private void clearOwn(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            send(sender, NamedTextColor.RED, "Only players can clear a prefix.");
            return;
        }
        manager.clearPrefix(player);
        send(sender, NamedTextColor.GREEN, "Your prefix has been cleared.");
    }

    private void admin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("prefix.admin")) {
            send(sender, NamedTextColor.RED, "You don't have permission to do that.");
            return;
        }
        if (args.length < 3) {
            send(sender, NamedTextColor.RED, "Usage: /prefix admin <set <player> <text> | clear <player>>");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            send(sender, NamedTextColor.RED, "That player is not online.");
            return;
        }

        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "set" -> {
                if (args.length < 4) {
                    send(sender, NamedTextColor.RED, "Usage: /prefix admin set <player> <text>");
                    return;
                }
                String raw = String.join(" ", Arrays.copyOfRange(args, 3, args.length)).trim();
                String text = sanitize(sender, raw);
                String error = check(text);
                if (error != null) {
                    send(sender, NamedTextColor.RED, error);
                    return;
                }
                manager.setPrefix(target, text);
                send(sender, NamedTextColor.GREEN, "Set prefix for " + target.getName() + ".");
            }
            case "clear" -> {
                manager.clearPrefix(target);
                send(sender, NamedTextColor.GREEN, "Cleared prefix for " + target.getName() + ".");
            }
            default -> send(sender, NamedTextColor.RED, "Usage: /prefix admin <set|clear> <player> ...");
        }
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission("prefix.admin")) {
            send(sender, NamedTextColor.RED, "You don't have permission to do that.");
            return;
        }
        plugin.reloadConfig();
        send(sender, NamedTextColor.GREEN, "PrefixPlus config reloaded.");
    }

    // ---------- validation ----------

    /** Players with prefix.color may use MiniMessage tags and &-codes. Others get plain text. */
    private String sanitize(CommandSender sender, String raw) {
        if (sender.hasPermission("prefix.color")) {
            return ColorCodes.toMiniMessage(raw);
        }
        return manager.mini().escapeTags(ColorCodes.strip(raw));
    }

    /** Returns an error message, or null if the prefix is acceptable. */
    private String check(String text) {
        String plain;
        try {
            plain = manager.plainText(text);
        } catch (Exception e) {
            return "That prefix has invalid formatting.";
        }

        int max = plugin.getConfig().getInt("max-length", 16);
        if (plain.isBlank()) {
            return "Prefix cannot be empty.";
        }
        if (plain.length() > max) {
            return "Prefix can be at most " + max + " characters (formatting doesn't count).";
        }

        String lower = plain.toLowerCase(Locale.ROOT);
        for (String blocked : plugin.getConfig().getStringList("blocked-words")) {
            if (lower.contains(blocked.toLowerCase(Locale.ROOT))) {
                return "That prefix contains a blocked word.";
            }
        }
        return null;
    }

    // ---------- helpers ----------

    private void sendUsage(CommandSender sender) {
        send(sender, NamedTextColor.YELLOW, "/prefix set <text> | clear | admin ... | reload");
    }

    private void send(CommandSender sender, NamedTextColor color, String text) {
        sender.sendMessage(Component.text(text, color));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("set", "clear", "admin", "reload"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("admin")) {
            return filter(List.of("set", "clear"), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("admin")) {
            List<String> names = Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            return filter(names, args[2]);
        }
        return List.of();
    }

    private static List<String> filter(Collection<String> options, String start) {
        String lower = start.toLowerCase(Locale.ROOT);
        return options.stream()
                .filter(o -> o.toLowerCase(Locale.ROOT).startsWith(lower))
                .toList();
    }
}
