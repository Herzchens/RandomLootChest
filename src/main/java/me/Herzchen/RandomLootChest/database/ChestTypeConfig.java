package me.Herzchen.RandomLootChest.database;

import me.Herzchen.RandomLootChest.Main;
import me.Herzchen.RandomLootChest.model.ChestType;
import me.Herzchen.RandomLootChest.model.ChestTypeProperties;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;

/**
 * Loads chest type definitions from {@code chesttypes.yml} into runtime
 * {@link ChestType} objects registered in the static registry.
 *
 * <p>Simplified from the original: instead of calling 13 separate setters on
 * each {@link ChestType}, all configurable values are gathered into a single
 * {@link ChestTypeProperties} record and passed to the constructor.
 */
public class ChestTypeConfig {
    private static ChestTypeConfig instance;
    private FileConfiguration config;
    private File configFile;

    public ChestTypeConfig() { instance = this; }
    public static ChestTypeConfig getInstance() { return instance; }

    public void setup(Plugin plugin) {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        configFile = new File(plugin.getDataFolder(), "chesttypes.yml");
        if (!configFile.exists()) plugin.saveResource("chesttypes.yml", false);
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public void reloadConfig()      { config = YamlConfiguration.loadConfiguration(configFile); }
    public FileConfiguration getConfig() { return config; }
    public void saveConfig() {
        try { config.save(configFile); }
        catch (IOException e) { Bukkit.getLogger().severe(ChatColor.RED + "Không thể lưu chesttypes.yml!"); }
    }

    /**
     * Read one chest type's YAML section into a {@link ChestTypeProperties} record.
     * Missing values fall back to the same defaults the old manual-setter code used.
     */
    public static ChestTypeProperties readProperties(ConfigurationSection s, String fallbackName) {
        Material mat = Material.matchMaterial(s.getString("material", "CHEST"));
        if (mat == null) {
            Bukkit.getLogger().warning("[RandomLootChest] Material không hợp lệ cho chest type '"
                    + fallbackName + "' — fallback về CHEST.");
            mat = Material.CHEST;
        }
        Integer countMin = s.isSet("itemCountMin") ? Math.max(1, s.getInt("itemCountMin")) : null;
        Integer countMax = s.isSet("itemCountMax") ? Math.max(1, s.getInt("itemCountMax")) : null;
        Integer nothingChance = s.isSet("nothingChance")
                ? Math.min(100, Math.max(0, s.getInt("nothingChance"))) : null;
        return new ChestTypeProperties(
            mat,
            s.getString("displayName", fallbackName),
            s.getInt("spawnTimeMin", 10),
            s.getInt("spawnTimeMax", 20),
            s.getInt("killTime", 60),
            s.getString("spawnEffect", "MOBSPAWNER_FLAMES"),
            s.getString("spawnSound", "NONE"),
            s.getString("openSound", "CHEST_OPEN|BLOCK_CHEST_OPEN"),
            s.getString("killEffect", "EXPLOSION"),
            s.getString("killSound", "DIG_GRASS|BLOCK_GRASS_BREAK"),
            s.getString("lootTable"),
            countMin,
            countMax,
            s.getBoolean("uniquePicks", false),
            s.getString("vanillaTable"),
            nothingChance,
            s.getBoolean("mergeStacks", false)
        );
    }

    /**
     * Write a {@link ChestTypeProperties} record into a YAML config section.
     * Used by {@code /rlc addchest} to create new chest types from in-game.
     */
    public static void writeProperties(FileConfiguration cfg, String id, ChestTypeProperties props) {
        String prefix = id + ".";
        cfg.set(prefix + "material",       props.material() != null ? props.material().name() : "CHEST");
        cfg.set(prefix + "displayName",     props.displayName());
        cfg.set(prefix + "spawnTimeMin",    props.spawnTimeMin());
        cfg.set(prefix + "spawnTimeMax",    props.spawnTimeMax());
        cfg.set(prefix + "killTime",        props.killTime());
        cfg.set(prefix + "spawnEffect",     props.spawnEffect());
        cfg.set(prefix + "spawnSound",      props.spawnSound());
        cfg.set(prefix + "openSound",       props.openSound());
        cfg.set(prefix + "killEffect",      props.killEffect());
        cfg.set(prefix + "killSound",       props.killSound());
        cfg.set(prefix + "lootTable",       props.lootTablePath() != null ? props.lootTablePath() : "");
        if (props.itemCountMin() != null) cfg.set(prefix + "itemCountMin", props.itemCountMin());
        if (props.itemCountMax() != null) cfg.set(prefix + "itemCountMax", props.itemCountMax());
        cfg.set(prefix + "uniquePicks",     props.uniquePicks());
        if (props.vanillaTable() != null && !props.vanillaTable().isEmpty())
            cfg.set(prefix + "vanillaTable", props.vanillaTable());
        if (props.nothingChance() != null) cfg.set(prefix + "nothingChance", props.nothingChance());
        cfg.set(prefix + "mergeStacks",     props.mergeStacks());
    }

    public void loadChestTypes() {
        ChestType.clearAll();
        for (String key : config.getKeys(false)) {
            ConfigurationSection s = config.getConfigurationSection(key);
            if (s == null) continue;
            ChestTypeProperties props = readProperties(s, key);
            if (!isLootContainer(props.material()))
                Main.pl.getLogger().warning("[RandomLootChest] Chest type '" + key + "': material '"
                        + props.material() + "' không phải khối chứa đồ — rương spawn ra sẽ KHÔNG có item!");
            if (props.vanillaTable() != null && !props.vanillaTable().isEmpty()
                    && ChestType.resolveVanillaLoot(props.vanillaTable()) == null)
                Main.pl.getLogger().warning("[RandomLootChest] Chest type '" + key + "': vanillaTable '"
                        + props.vanillaTable() + "' không tìm thấy trên server — sẽ bị bỏ qua.");
            ChestType type = new ChestType(key, props);
            String path = props.lootTablePath();
            if (path != null && !path.isEmpty()) {
                type.setLootTable(Main.pl.db.data.getConfigurationSection(path));
            }
            ChestType.registerChestType(type);
        }
        if (ChestType.getAllChestTypes().isEmpty()) {
            ChestType.registerChestType(new ChestType("default", ChestTypeProperties.defaults()));
        }
    }

    /**
     * Material được phép làm "rương loot" (khối có inventory chứa đồ).
     * Ngoài danh sách này vẫn load được, nhưng sẽ bị cảnh báo vì item không thể fill vào.
     */
    private static boolean isLootContainer(Material m) {
        if (m == Material.CHEST || m == Material.TRAPPED_CHEST || m == Material.BARREL) return true;
        if (org.bukkit.Tag.SHULKER_BOXES.isTagged(m)) return true;
        switch (m) {
            case HOPPER: case DROPPER: case DISPENSER:
            case FURNACE: case BLAST_FURNACE: case SMOKER:
                return true;
            default: return false;
        }
    }
}
