package me.Herzchen.RandomLootChest.model;

import org.bukkit.Material;

/**
 * Immutable snapshot of every YAML-configurable property for a {@link ChestType}.
 *
 * <p>Loaded by {@link me.Herzchen.RandomLootChest.database.ChestTypeConfig} and
 * applied to the runtime {@link ChestType} object.  Keeping all raw values in a
 * single record means:
 * <ul>
 *   <li>Adding/changing a property = one field, no scattered setters</li>
 *   <li>Default values live in one place ({@link #defaults()})</li>
 *   <li>No per-field cache-invalidation boilerplate</li>
 * </ul>
 *
 * @param material       block the chest renders as
 * @param displayName    human-readable name (shown in lore / commands)
 * @param spawnTimeMin   minimum seconds between re-spawns
 * @param spawnTimeMax   maximum seconds between re-spawns
 * @param killTime       lifetime in seconds before the chest auto-deletes
 * @param spawnEffect    particle/spawn effect string
 * @param spawnSound     sound played on spawn
 * @param openSound      sound played when the chest is opened
 * @param killEffect     particle effect when the chest despawns
 * @param killSound      sound played when the chest despawns
 * @param lootTablePath  dotted path inside {@code database.yml} → {@code ItemDatabase};
 *                       {@code null} or empty means use the global item pool
 * @param itemCountMin   số item tối thiểu trong rương; {@code null} = không set
 *                       (fallback: chỉ set Min → cố định; không set gì → ItemAmountToAdd)
 * @param itemCountMax   số item tối đa trong rương; {@code null} = theo itemCountMin
 * @param uniquePicks    {@code true} = không cho 2 item giống nhau trong cùng 1 rương
 * @param vanillaTable   loot table vanilla (vd "chests/simple_dungeon"); item roll thêm
 *                       RA KHỎI số itemCountMin/Max; {@code null} = tắt
 * @param nothingChance  % mỗi pick custom bị bỏ qua (0-100); {@code null} = 0
 * @param mergeStacks    {@code true} = gộp các pick cùng loại thành 1 stack
 */
public record ChestTypeProperties(
    Material material,
    String displayName,
    int spawnTimeMin,
    int spawnTimeMax,
    int killTime,
    String spawnEffect,
    String spawnSound,
    String openSound,
    String killEffect,
    String killSound,
    String lootTablePath,
    Integer itemCountMin,
    Integer itemCountMax,
    boolean uniquePicks,
    String vanillaTable,
    Integer nothingChance,
    boolean mergeStacks
) {

    /** Factory with the same defaults the plugin used before the refactor. */
    public static ChestTypeProperties defaults() {
        return new ChestTypeProperties(
            Material.CHEST,
            "Default Chest",
            10, 20, 60,
            "MOBSPAWNER_FLAMES", "NONE",
            "CHEST_OPEN|BLOCK_CHEST_OPEN", "EXPLOSION",
            "DIG_GRASS|BLOCK_GRASS_BREAK",
            null,
            null, null, false,
            null, null, false
        );
    }
}
