package me.Herzchen.RandomLootChest.model;

import me.Herzchen.RandomLootChest.util.EffectWrapper;
import me.Herzchen.RandomLootChest.util.SoundWrapper;
import org.bukkit.NamespacedKey;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

/**
 * Runtime representation of a chest type.
 *
 * <p>Refactoring goals vs. the original 83-line version:
 * <ul>
 *   <li>Configurable values live in the immutable {@link ChestTypeProperties} record
 *       instead of 10 separate fields with 9 setters.</li>
 *   <li>Loot table is wrapped in a {@link LootTable} object instead of a raw
 *       {@link ConfigurationSection}.</li>
 *   <li>Lazy effect/sound cache collapsed from five separate {@code transient}
 *       fields into one inner {@link CachedEffects} record — a single
 *       {@code null}-guard suffices because properties are immutable.</li>
 *   <li>No per-field cache invalidation: only {@link #setLootTable} can change
 *       runtime state, and loot table changes don't affect effect/sound strings.</li>
 * </ul>
 */
public class ChestType {
    private final String id;
    private final ChestTypeProperties props;
    private LootTable lootTable;
    private transient CachedEffects cache;

    private static final Map<String, ChestType> CHEST_TYPES = new HashMap<>();

    public ChestType(String id, ChestTypeProperties props) {
        this.id = id;
        this.props = props;
    }

    // ── basic getters (delegate to the immutable record) ──

    public String getId()                       { return id; }
    public Material getMaterial()               { return props.material(); }
    public String getDisplayName()              { return props.displayName(); }
    public int getSpawnTimeMin()                { return props.spawnTimeMin(); }
    public int getSpawnTimeMax()                { return props.spawnTimeMax(); }
    public int getKillTime()                    { return props.killTime(); }
    public String getSpawnEffect()              { return props.spawnEffect(); }
    public String getSpawnSound()               { return props.spawnSound(); }
    public String getOpenSound()                { return props.openSound(); }
    public String getKillEffect()               { return props.killEffect(); }
    public String getKillSound()                { return props.killSound(); }
    public ChestTypeProperties getProperties()  { return props; }

    // ── loot table ──

    public LootTable getLootTable()             { return lootTable; }
    public boolean hasLootTable()               { return lootTable != null && !lootTable.isEmpty(); }

    // ── spawn item count / unique picks ──

    /** {@code null} = không set trong chesttypes.yml (fallback ItemAmountToAdd ở tầng fill). */
    public Integer getItemCountMin()            { return props.itemCountMin(); }
    /** {@code null} = không set trong chesttypes.yml (fallback theo itemCountMin ở tầng fill). */
    public Integer getItemCountMax()            { return props.itemCountMax(); }
    /** {@code true} = không cho 2 item giống nhau trong cùng 1 rương. */
    public boolean isUniquePicks()              { return props.uniquePicks(); }
    /** Loot table vanilla (vd "chests/simple_dungeon"); {@code null} = tắt. */
    public String getVanillaTable()             { return props.vanillaTable(); }
    /** % mỗi pick custom bị bỏ qua; {@code null} = 0. */
    public Integer getNothingChance()           { return props.nothingChance(); }
    /** {@code true} = gộp các pick cùng loại thành 1 stack. */
    public boolean isMergeStacks()              { return props.mergeStacks(); }

    /** @see #resolveVanillaLoot(String) */
    public org.bukkit.loot.LootTable resolveVanillaLoot() { return resolveVanillaLoot(props.vanillaTable()); }

    /**
     * Resolve tên loot table vanilla thành {@link org.bukkit.loot.LootTable}.
     * Chấp nhận "chests/simple_dungeon" hoặc "minecraft:buried_treasure".
     *
     * @return {@code null} nếu không cấu hình / key sai / server không có table đó
     */
    public static org.bukkit.loot.LootTable resolveVanillaLoot(String vanillaTable) {
        if (vanillaTable == null || vanillaTable.trim().isEmpty()) return null;
        String key = vanillaTable.trim().toLowerCase(java.util.Locale.ROOT);
        NamespacedKey nk = key.contains(":") ? NamespacedKey.fromString(key)
                                             : NamespacedKey.minecraft(key);
        return nk == null ? null : org.bukkit.Bukkit.getLootTable(nk);
    }

    /** Wrap a raw section in our {@link LootTable} helper. */
    public void setLootTable(ConfigurationSection section) {
        this.lootTable = section != null ? new LootTable(section) : null;
    }

    // ── play helpers (lazy cache) ──

    private CachedEffects cache() {
        if (cache == null) cache = new CachedEffects();
        return cache;
    }

    public void playSpawnEffect(Location loc) {
        CachedEffects c = cache();
        if (c.spawnEffect == null && getSpawnEffect() != null)
            c.spawnEffect = EffectWrapper.createNotNull(getSpawnEffect(), null, s -> null);
        if (c.spawnEffect != null) c.spawnEffect.play(loc);
    }
    public void playSpawnSound(Location loc) {
        CachedEffects c = cache();
        if (c.spawnSound == null && getSpawnSound() != null)
            c.spawnSound = SoundWrapper.createNotNull(getSpawnSound(), null, s -> null);
        if (c.spawnSound != null) c.spawnSound.play(loc);
    }
    public void playOpenSound(Location loc) {
        CachedEffects c = cache();
        if (c.openSound == null && getOpenSound() != null)
            c.openSound = SoundWrapper.createNotNull(getOpenSound(), null, s -> null);
        if (c.openSound != null) c.openSound.play(loc);
    }
    public void playKillEffect(Location loc) {
        CachedEffects c = cache();
        if (c.killEffect == null && getKillEffect() != null)
            c.killEffect = EffectWrapper.createNotNull(getKillEffect(), null, s -> null);
        if (c.killEffect != null) c.killEffect.play(loc);
    }
    public void playKillSound(Location loc) {
        CachedEffects c = cache();
        if (c.killSound == null && getKillSound() != null)
            c.killSound = SoundWrapper.createNotNull(getKillSound(), null, s -> null);
        if (c.killSound != null) c.killSound.play(loc);
    }

    // ── registry ──

    public static void registerChestType(ChestType type) { CHEST_TYPES.put(type.getId(), type); }
    public static ChestType getChestType(String id)      { return CHEST_TYPES.get(id); }
    public static Map<String, ChestType> getAllChestTypes() { return CHEST_TYPES; }
    public static void clearAll()                         { CHEST_TYPES.clear(); }

    /** Single object holding all five lazy wrappers — one null-check suffices. */
    private static final class CachedEffects {
        EffectWrapper spawnEffect, killEffect;
        SoundWrapper spawnSound, openSound, killSound;
    }
}
