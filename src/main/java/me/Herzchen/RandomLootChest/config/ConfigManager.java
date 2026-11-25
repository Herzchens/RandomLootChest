package me.Herzchen.RandomLootChest.config;

import me.Herzchen.RandomLootChest.Main;
import me.Herzchen.RandomLootChest.util.EffectWrapper;
import me.Herzchen.RandomLootChest.util.MaterialCondition;
import me.Herzchen.RandomLootChest.util.MessageUtil;
import me.Herzchen.RandomLootChest.util.SoundWrapper;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.FileConfiguration;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.logging.Level;

public class ConfigManager {
    private final Main plugin;
    private EffectWrapper randomChestEffect, randomChestOpenSound, fixedChestEffect;
    private SoundWrapper randomChestSound, fixedChestSound;
    private MaterialCondition spawnBlockCondition_Positive, spawnBlockCondition_Negative;
    private MaterialCondition underBlockCondition_Positive, underBlockCondition_Negative;
    private MaterialCondition sideBlockCondition_Positive, sideBlockCondition_Negative;
    private int spawnChestPerTime, killChestAfterTime, fixedChestUpdateTimeMin, fixedChestUpdateTimeMax;
    private ArrayList<String> commandsOnLoot;
    private boolean killChest, pluginEnabled;

    public ConfigManager(Main plugin) { this.plugin = plugin; }

    public boolean loadConfig() {
        plugin.reloadConfig();
        FileConfiguration cfg = plugin.getConfig();

        // Tự động thêm các key mặc định nếu thiếu (không block plugin)
        cfg.options().copyDefaults(true);
        plugin.saveConfig();
        plugin.reloadConfig();
        cfg = plugin.getConfig();

        // Kiểm tra EnablePlugin
        if (!cfg.getBoolean("EnablePlugin")) {
            MessageUtil.sendConsole(plugin.messages.get("console.disabled_line1"));
            MessageUtil.sendConsole(plugin.messages.get("console.disabled_line2"));
            MessageUtil.sendConsole(plugin.messages.get("console.disabled_line3"));
            MessageUtil.sendConsole(plugin.messages.get("console.disabled_line4"));
            return false;
        }

        spawnBlockCondition_Positive = new MaterialCondition(cfg.getString("SpawnBlockCondition"), false);
        spawnBlockCondition_Negative = new MaterialCondition(cfg.getString("SpawnBlockCondition"), true);
        underBlockCondition_Positive = new MaterialCondition(cfg.getString("UnderBlockCondition"), false);
        underBlockCondition_Negative = new MaterialCondition(cfg.getString("UnderBlockCondition"), true);
        sideBlockCondition_Positive = new MaterialCondition(cfg.getString("SideBlockCondition"), false);
        sideBlockCondition_Negative = new MaterialCondition(cfg.getString("SideBlockCondition"), true);

        randomChestEffect = findEffect(cfg.getString("RandomChestEffect"), findEffect("MOBSPAWNER_FLAMES", null, false), true);
        randomChestSound = findSound(cfg.getString("RandomChestSound"), findSound("NONE", null, false), true);
        randomChestOpenSound = findEffect(cfg.getString("RandomChestOpenSound"), findEffect("CHEST_OPEN|BLOCK_CHEST_OPEN", null, false), true);
        fixedChestEffect = findEffect(cfg.getString("FixedChestEffect"), findEffect("EXPLOSION", null, false), true);
        fixedChestSound = findSound(cfg.getString("FixedChestSound"), findSound("DIG_GRASS|BLOCK_GRASS_BREAK", null, false), true);

        spawnChestPerTime = cfg.getInt("SpawnChestPerTime");
        fixedChestUpdateTimeMin = cfg.getInt("FixedChestUpdateTimeMin", 3600);
        fixedChestUpdateTimeMax = cfg.getInt("FixedChestUpdateTimeMax", 3600);
        killChestAfterTime = cfg.getInt("KillChestAfterTime");
        killChest = cfg.getBoolean("KillChest");

        commandsOnLoot = new ArrayList<>();
        if (cfg.getList("CommandsToExecuteOnLoot") != null)
            commandsOnLoot = (ArrayList<String>) cfg.getStringList("CommandsToExecuteOnLoot");

        pluginEnabled = true;
        return true;
    }

    public EffectWrapper getRandomChestEffect() { return randomChestEffect; }
    public SoundWrapper getRandomChestSound() { return randomChestSound; }
    public EffectWrapper getRandomChestOpenSound() { return randomChestOpenSound; }
    public EffectWrapper getFixedChestEffect() { return fixedChestEffect; }
    public SoundWrapper getFixedChestSound() { return fixedChestSound; }

    public MaterialCondition getSpawnBlockCondition_Positive() { return spawnBlockCondition_Positive; }
    public MaterialCondition getSpawnBlockCondition_Negative() { return spawnBlockCondition_Negative; }
    public MaterialCondition getUnderBlockCondition_Positive() { return underBlockCondition_Positive; }
    public MaterialCondition getUnderBlockCondition_Negative() { return underBlockCondition_Negative; }
    public MaterialCondition getSideBlockCondition_Positive() { return sideBlockCondition_Positive; }
    public MaterialCondition getSideBlockCondition_Negative() { return sideBlockCondition_Negative; }

    public String getMessageOnSpawn() { return plugin.messages.get("chest.spawn"); }
    public String getMessageOnLoot() { return plugin.messages.get("chest.loot"); }
    public String getMessageOnKill() { return plugin.messages.get("chest.kill"); }
    public int getSpawnChestPerTime() { return spawnChestPerTime; }
    public int getKillChestAfterTime() { return killChestAfterTime; }
    public int getFixedChestUpdateTimeMin() { return fixedChestUpdateTimeMin; }
    public int getFixedChestUpdateTimeMax() { return fixedChestUpdateTimeMax; }
    public ArrayList<String> getCommandsOnLoot() { return commandsOnLoot; }
    public boolean isKillChest() { return killChest; }
    public boolean isPluginEnabled() { return pluginEnabled; }

    private SoundWrapper findSound(String str, SoundWrapper defaultValue, boolean showError) {
        return SoundWrapper.createNotNull(str, defaultValue, s -> {
            if (showError) plugin.getLogger().log(Level.WARNING, "Không tìm thấy âm thanh ''{0}''", s);
            return defaultValue;
        });
    }

    private EffectWrapper findEffect(String str, EffectWrapper defaultValue, boolean showError) {
        return EffectWrapper.createNotNull(str, defaultValue, s -> {
            if (showError) plugin.getLogger().log(Level.WARNING, "Không tìm thấy hiệu ứng ''{0}''", s);
            return defaultValue;
        });
    }

    public void saveLegalConstants(String fileName, String enumName) {
        try {
            Class<?> cls = Class.forName(enumName);
            File outFile = new File(plugin.getDataFolder(), fileName);
            if (!outFile.exists()) {
                List<String> list = Arrays.stream((Enum<?>[]) cls.getEnumConstants()).map(Enum::name).sorted().collect(java.util.stream.Collectors.toList());
                try (FileWriter out = new FileWriter(outFile)) { out.write(String.join(System.lineSeparator(), list)); }
            }
        } catch (Exception ignored) {}
    }
}