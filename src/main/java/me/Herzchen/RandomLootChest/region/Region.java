package me.Herzchen.RandomLootChest.region;

import me.Herzchen.RandomLootChest.model.ChestType;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;

public class Region {
    private String id;
    private World world;
    private int xMin, xMax, yMin, yMax, zMin, zMax;
    private List<ChestType> chestTypes = new ArrayList<>();
    private List<String> chestTypeIds = new ArrayList<>();

    public Region(String id, World world, int xMin, int xMax, int yMin, int yMax, int zMin, int zMax) {
        this.id = id;
        this.world = world;
        this.xMin = Math.min(xMin, xMax);
        this.xMax = Math.max(xMin, xMax);
        this.yMin = Math.min(yMin, yMax);
        this.yMax = Math.max(yMin, yMax);
        this.zMin = Math.min(zMin, zMax);
        this.zMax = Math.max(zMin, zMax);
    }

    public String getId() { return id; }
    public World getWorld() { return world; }
    public int getXMin() { return xMin; }
    public int getXMax() { return xMax; }
    public int getYMin() { return yMin; }
    public int getYMax() { return yMax; }
    public int getZMin() { return zMin; }
    public int getZMax() { return zMax; }
    public List<ChestType> getChestTypes() { return chestTypes; }
    public List<String> getChestTypeIds() { return chestTypeIds; }

    public void setChestTypeIds(List<String> ids) { this.chestTypeIds = ids; }

    public void resolveChestTypes() {
        chestTypes.clear();
        for (String id : chestTypeIds) {
            ChestType ct = ChestType.getChestType(id);
            if (ct != null) chestTypes.add(ct);
        }
    }

    public ChestType getRandomChestType() {
        if (chestTypes.isEmpty()) return ChestType.getChestType("default");
        return chestTypes.get((int) (Math.random() * chestTypes.size()));
    }

    public boolean contains(Location loc) {
        if (!loc.getWorld().equals(world)) return false;
        int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
        return x >= xMin && x <= xMax && y >= yMin && y <= yMax && z >= zMin && z <= zMax;
    }

    public int getRandomX() {
        return xMin + (int) (Math.random() * (xMax - xMin + 1));
    }

    public int getRandomY() {
        return yMin + (int) (Math.random() * (yMax - yMin + 1));
    }

    public int getRandomZ() {
        return zMin + (int) (Math.random() * (zMax - zMin + 1));
    }
}