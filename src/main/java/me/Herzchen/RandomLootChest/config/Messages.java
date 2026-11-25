package me.Herzchen.RandomLootChest.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * Stateful loader for {@code messages.yml}. Follows the same pattern as {@code ChestTypeConfig}:
 * copy the bundled default on first run, expose {@link #reload()} and typed getters.
 *
 * <p>Messages are stored as raw MiniMessage strings. Callers pass them to
 * {@link me.Herzchen.RandomLootChest.util.MessageUtil#send(org.bukkit.command.CommandSender, String)}
 * which parses the tags. Placeholders use the existing {@code {X}} convention and are substituted
 * via {@link #getFormatted(String, String...)}.
 *
 * <p>Getters fall back to a default when a key is missing so a user deleting a line never crashes
 * the plugin. Each missing key is logged once.
 */
public class Messages {
    private static final Set<String> warnedMissing = new HashSet<>();

    private JavaPlugin plugin;
    private FileConfiguration config;
    private File configFile;

    public void setup(JavaPlugin plugin) {
        this.plugin = plugin;
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        configFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!configFile.exists()) plugin.saveResource("messages.yml", false);
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public void reload() {
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public FileConfiguration getConfig() { return config; }

    /** @return raw message string, or {@code null} if the key is absent or empty (used for optional messages). */
    public String get(String path) {
        if (!config.isSet(path)) return null;
        String value = config.getString(path);
        if (value == null || value.trim().isEmpty()) return null;
        return value;
    }

    /** @return raw message string, falling back to {@code def} when the key is absent. */
    public String get(String path, String def) {
        if (config.isSet(path)) return config.getString(path);
        warnMissing(path);
        return def;
    }

    /**
     * Substitute {@code {placeholder}} values into a message.
     *
     * @param path          message key
     * @param replacements  pairs of placeholder/value, e.g. {@code "{X}", "12", "{Player}", "Steve"}
     * @return formatted string, or the key itself when absent (with replacements applied)
     */
    public String getFormatted(String path, String... replacements) {
        return apply(get(path, path), replacements);
    }

    private String apply(String text, String... replacements) {
        if (text == null) return null;
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            text = text.replace(replacements[i], replacements[i + 1]);
        }
        return text;
    }

    private void warnMissing(String path) {
        if (plugin == null || !warnedMissing.add(path)) return;
        plugin.getLogger().warning("messages.yml: thiếu khóa '" + path + "', dùng giá trị mặc định.");
    }
}
