package dev.example.prefix;

import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public final class ChatListener implements Listener {

    private final PrefixPlugin plugin;
    private final PrefixManager manager;

    public ChatListener(PrefixPlugin plugin, PrefixManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!manager.hasPrefix(player.getUniqueId())) {
            return; // leave the default chat rendering alone
        }

        final Component prefix = manager.renderPrefix(player.getUniqueId());
        final String format = plugin.getConfig().getString(
                "chat-format", "<prefix><name><dark_gray> » </dark_gray><message>");

        event.renderer(ChatRenderer.viewerUnaware((source, name, message) -> {
            TagResolver resolver = TagResolver.resolver(
                    Placeholder.component("prefix", prefix),
                    Placeholder.component("name", name),
                    Placeholder.component("message", message)
            );
            return manager.mini().deserialize(format, resolver);
        }));
    }
}
