package me.Herzchen.RandomLootChest.region;

import me.Herzchen.RandomLootChest.Main;
import me.Herzchen.RandomLootChest.util.RLCUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class RegionConfig {
    private static RegionConfig instance;
    private final Map<String, Region> regions = new LinkedHashMap<>();

    public RegionConfig() { instance = this; }
    public static RegionConfig getInstance() { return instance; }

    public void loadRegions() {
        regions.clear();
        FileConfiguration config = Main.pl.getConfig();
        ConfigurationSection regionSection = config.getConfigurationSection("Regions");
        if (regionSection == null) {
            // Fallback to global config if no regions defined
            createFallbackRegion();
            return;
        }
        for (String key : regionSection.getKeys(false)) {
            ConfigurationSection s = regionSection.getConfigurationSection(key);
            if (s == null) continue;
            String worldName = s.getString("world", config.getString("World", "world"));
            World world = RLCUtils.getWorld(worldName);
            if (world == null) {
                Bukkit.getLogger().warning("[RandomLootChest] Region '" + key + "': world '" + worldName + "' not found, skipping!");
                continue;
            }
            int xMin = s.getInt("bounds.x_min", config.getInt("SmallestDinctance_X", -10));
            int xMax = s.getInt("bounds.x_max", config.getInt("LargestDinctance_X", 10));
            int yMin = s.getInt("bounds.y_min", config.getInt("SmallestDinctance_Y", 0));
            int yMax = s.getInt("bounds.y_max", config.getInt("LargestDinctance_Y", 255));
            int zMin = s.getInt("bounds.z_min", config.getInt("SmallestDinctance_Z", -10));
            int zMax = s.getInt("bounds.z_max", config.getInt("LargestDinctance_Z", 10));

            Region region = new Region(key, world, xMin, xMax, yMin, yMax, zMin, zMax);
            List<String> typeIds = s.getStringList("chestTypes");
            if (typeIds.isEmpty()) {
                // Use all available chest types
                typeIds = new ArrayList<>(me.Herzchen.RandomLootChest.model.ChestType.getAllChestTypes().keySet());
            }
            region.setChestTypeIds(typeIds);
            region.resolveChestTypes();
            regions.put(key, region);
        }
        if (regions.isEmpty()) createFallbackRegion();
    }

    private void createFallbackRegion() {
        FileConfiguration config = Main.pl.getConfig();
        String worldName = config.getString("World", "world");
        World world = RLCUtils.getWorld(worldName);
        if (world == null) return;
        Region region = new Region("global",
                world,
                config.getInt("SmallestDinctance_X", -10),
                config.getInt("LargestDinctance_X", 10),
                config.getInt("SmallestDinctance_Y", 0),
                config.getInt("LargestDinctance_Y", 255),
                config.getInt("SmallestDinctance_Z", -10),
                config.getInt("LargestDinctance_Z", 10));
        region.setChestTypeIds(new ArrayList<>(me.Herzchen.RandomLootChest.model.ChestType.getAllChestTypes().keySet()));
        region.resolveChestTypes();
        regions.put("global", region);
    }

    public Collection<Region> getAllRegions() { return regions.values(); }
    public Region getRegion(String id) { return regions.get(id); }

    public Region findRegionAt(org.bukkit.Location loc) {
        for (Region r : regions.values()) {
            if (r.contains(loc)) return r;
        }
        return null;
    }

    public void reload() { loadRegions(); }
}