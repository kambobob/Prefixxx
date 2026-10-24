package dev.example.prefix;

import org.bukkit.plugin.java.JavaPlugin;

public final class PrefixPlugin extends JavaPlugin {

    private PrefixManager prefixManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        prefixManager = new PrefixManager(this);
        prefixManager.load();

        getServer().getPluginManager().registerEvents(new ChatListener(this, prefixManager), this);
        getServer().getPluginManager().registerEvents(new JoinListener(prefixManager), this);

        PrefixCommand command = new PrefixCommand(this, prefixManager);
        getCommand("prefix").setExecutor(command);
        getCommand("prefix").setTabCompleter(command);

        getLogger().info("PrefixPlus enabled.");
    }

    @Override
    public void onDisable() {
        if (prefixManager != null) {
            prefixManager.saveNow();
        }
    }

    public PrefixManager getPrefixManager() {
        return prefixManager;
    }
}
