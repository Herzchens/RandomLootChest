package me.Herzchen.RandomLootChest.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Utility class for parsing messages using MiniMessage format.
 * Supports both MiniMessage tags (e.g. <red>, <bold>) and legacy & codes.
 */
public class MessageUtil {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    /**
     * Parse a MiniMessage string to Component.
     * Also supports legacy & codes (converted to § first).
     */
    public static Component parse(String text) {
        if (text == null || text.isEmpty()) return Component.empty();

        // Convert legacy & codes to § for MiniMessage to handle
        text = text.replace("&", "§");

        // TODO: why the fuck minimessage don't handle ts rn?
        return MINI_MESSAGE.deserialize(text);
    }

    /**
     * Parse a MiniMessage string and send to CommandSender.
     */
    public static void send(CommandSender sender, String text) {
        sender.sendMessage(parse(text));
    }

    /**
     * Parse a MiniMessage string and send to Player.
     */
    public static void send(Player player, String text) {
        player.sendMessage(parse(text));
    }

    /**
     * Parse a MiniMessage string and broadcast to all players.
     */
    public static void broadcast(String text) {
        org.bukkit.Bukkit.broadcast(parse(text));
    }

    /**
     * Parse a MiniMessage string and send to console.
     */
    public static void sendConsole(String text) {
        org.bukkit.Bukkit.getConsoleSender().sendMessage(parse(text));
    }

    /**
     * Convert a MiniMessage string to legacy § string (for inventory titles, etc.)
     */
    public static String toLegacy(String text) {
        if (text == null || text.isEmpty()) return "";
        text = text.replace("&", "§");
        Component component = MINI_MESSAGE.deserialize(text);
        return LEGACY.serialize(component);
    }

    /**
     * Parse MiniMessage and return legacy § string.
     */
    public static String parseLegacy(String text) {
        return toLegacy(text);
    }

    /**
     * Replace placeholders in a MiniMessage string and parse.
     * Example: parse("<red>{player} joined!", "{player}", "Steve")
     */
    public static Component parseWithPlaceholders(String text, String... replacements) {
        if (text == null || text.isEmpty()) return Component.empty();
        for (int i = 0; i < replacements.length - 1; i += 2) {
            text = text.replace(replacements[i], replacements[i + 1]);
        }
        return parse(text);
    }

    /**
     * Replace placeholders and send to CommandSender.
     */
    public static void sendWithPlaceholders(CommandSender sender, String text, String... replacements) {
        sender.sendMessage(parseWithPlaceholders(text, replacements));
    }

    /**
     * Replace placeholders and broadcast.
     */
    public static void broadcastWithPlaceholders(String text, String... replacements) {
        org.bukkit.Bukkit.broadcast(parseWithPlaceholders(text, replacements));
    }
}