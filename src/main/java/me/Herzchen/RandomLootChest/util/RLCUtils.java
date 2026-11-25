package me.Herzchen.RandomLootChest.util;

import me.Herzchen.RandomLootChest.Main;
import me.Herzchen.RandomLootChest.model.ChestType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.inventory.Inventory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class RLCUtils {
    private static final Set<String> fixedChestMaterials = new HashSet<>(Arrays.asList(
            "CHEST", "TRAPPED_CHEST", "BARREL",
            "WHITE_SHULKER_BOX", "ORANGE_SHULKER_BOX", "MAGENTA_SHULKER_BOX",
            "LIGHT_BLUE_SHULKER_BOX", "YELLOW_SHULKER_BOX", "LIME_SHULKER_BOX",
            "PINK_SHULKER_BOX", "GRAY_SHULKER_BOX", "SILVER_SHULKER_BOX",
            "LIGHT_GRAY_SHULKER_BOX", "CYAN_SHULKER_BOX", "SHULKER_BOX",
            "PURPLE_SHULKER_BOX", "BLUE_SHULKER_BOX", "BROWN_SHULKER_BOX",
            "GREEN_SHULKER_BOX", "RED_SHULKER_BOX", "BLACK_SHULKER_BOX"
    ));
    private static Method getTypeMethod = null;
    private static final Map<Class, Method> getInventoryMethods = new HashMap<>();

    public static boolean isFixedChestType(Block block) {
        if (getTypeMethod == null) {
            try { getTypeMethod = Block.class.getMethod("getType"); }
            catch (NoSuchMethodException e) { throw new RuntimeException("Block.getType không tìm thấy", e); }
        }
        try { return fixedChestMaterials.contains(((Enum<?>) getTypeMethod.invoke(block)).name()); }
        catch (InvocationTargetException | IllegalAccessException e) { throw new RuntimeException("Lỗi Block.getType", e); }
    }

    public static boolean isFixedChestType(Location location) { return isFixedChestType(location.getBlock()); }

    public static boolean isRandomChestType(Material material) {
        return ChestType.getAllChestTypes().values().stream().anyMatch(t -> t.getMaterial().equals(material));
    }

    public static boolean isRandomChestType(Block block) { return isRandomChestType(block.getType()); }
    public static boolean isRandomChestType(Location location) { return isRandomChestType(location.getBlock()); }

    public static World getWorld(String worldName) {
        World world = Bukkit.getServer().getWorld(worldName);
        if (world == null) MessageUtil.sendConsole("<red>Không tìm thấy thế giới '<yellow>" + worldName + "<red>'.");
        return world;
    }

    public static Inventory getInventory(BlockState blockState) {
        if (blockState == null) return null;
        Class<? extends BlockState> cls = blockState.getClass();
        Method method = getInventoryMethods.get(cls);
        if (method == null && !getInventoryMethods.containsKey(cls)) {
            try { method = cls.getMethod("getInventory"); }
            catch (NoSuchMethodException e) { getInventoryMethods.put(cls, null); return null; }
            try { Inventory inv = (Inventory) method.invoke(blockState);
                getInventoryMethods.put(cls, method); return inv; }
            catch (InvocationTargetException | IllegalAccessException e) { getInventoryMethods.put(cls, null); return null; }
        }
        if (method == null) return null;
        try { return (Inventory) method.invoke(blockState); }
        catch (InvocationTargetException | IllegalAccessException e) { return null; }
    }

    public static Inventory getInventory(Block block) { return getInventory(block.getState()); }
    public static Inventory getInventory(Location location) { return getInventory(location.getBlock()); }

    /**
     * Xoay khối ngẫu nhiên theo đúng các hướng mà BlockData hỗ trợ:
     * CHEST/TRAPPED_CHEST → 4 hướng ngang, BARREL/SHULKER_BOX → đủ 6 hướng,
     * HOPPER → xuống + 4 hướng ngang. Khối không có hướng sẽ được bỏ qua.
     */
    public static void applyRandomFacing(BlockState state) {
        BlockData data = state.getBlockData();
        if (data instanceof Directional directional) {
            List<BlockFace> faces = new ArrayList<>(directional.getFaces());
            directional.setFacing(faces.get(FindAvaliableLocation.getRandom(0, faces.size() - 1)));
            state.setBlockData(directional);
        }
    }

    public static String formatLocation(Location loc) {
        return String.format("X: %d, Y: %d, Z: %d", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }
}