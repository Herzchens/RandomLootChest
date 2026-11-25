package me.Herzchen.RandomLootChest.model;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Encapsulates a loot table — a weighted collection of items backed by a
 * {@link ConfigurationSection} in {@code database.yml}.
 *
 * <p>Replaces the raw {@code ConfigurationSection} previously stored directly
 * on {@link ChestType}.  Provides a clean {@link #pickRandom()} method that
 * all consumers (chest spawning, loot GUI, command display) can share.
 *
 * <p>The section is expected to look like:
 * <pre>{@code
 * ItemDatabase:
 *   dungeon:
 *     0: { item: DIAMOND, chance: 5, amount-min: 1, amount-max: 3 }
 *     1: { item: EMERALD, chance: 10, amount-min: 2, amount-max: 5,
 *          enchant-roll: [ {ench: SHARPNESS, min: 1, max: 5} ] }
 * }</pre>
 */
public class LootTable {
    private final ConfigurationSection section;
    private final Random rng = new Random();

    public LootTable(ConfigurationSection section) {
        this.section = section;
    }

    /** @return the backing section (may be {@code null}). */
    public ConfigurationSection getSection() { return section; }

    /** @return {@code true} when there is no section or it has no keys. */
    public boolean isEmpty() {
        return section == null || section.getKeys(false).isEmpty();
    }

    /** @return every numeric or string key that is a sub-section. */
    public Collection<String> getKeys() {
        return section != null ? section.getKeys(false) : Collections.emptyList();
    }

    /**
     * Resolve a single entry (item, chance, amount range, enchant-roll).
     *
     * @return the entry, or {@code null} if the key is missing or invalid.
     */
    public LootEntry getEntry(String key) {
        if (section == null) return null;
        ConfigurationSection s = section.getConfigurationSection(key);
        if (s == null) return null;
        ItemStack item = s.getItemStack("item");
        int chance = s.getInt("chance", 0);
        int amountMin = s.getInt("amount-min", item != null ? item.getAmount() : 1);
        int amountMax = s.getInt("amount-max", amountMin);
        List<EnchRoll> enchRolls = EnchRoll.parseList(s.getMapList("enchant-roll"));
        return new LootEntry(item, chance, amountMin, amountMax, enchRolls);
    }

    /** Tất cả entry hợp lệ (item != null, chance &gt; 0). */
    public List<LootEntry> getValidEntries() {
        List<LootEntry> out = new ArrayList<>();
        for (String k : getKeys()) {
            LootEntry e = getEntry(k);
            if (e != null && e.item() != null && e.chance() > 0) out.add(e);
        }
        return out;
    }

    /** Tổng weight của table (để quy đổi % hiển thị). */
    public int totalWeight() {
        int total = 0;
        for (LootEntry e : getValidEntries()) total += e.chance();
        return total;
    }

    /**
     * Weighted random pick bằng cumulative weight (không nhân bản entry vào pool).
     *
     * @return a cloned, amount-adjusted {@link ItemStack} (đã roll enchant nếu có),
     *         or {@code ItemStack(Material.AIR)} if the table is empty.
     */
    public ItemStack pickRandom() {
        List<LootEntry> entries = getValidEntries();
        int total = 0;
        for (LootEntry e : entries) total += e.chance();
        if (total <= 0) return new ItemStack(Material.AIR);

        int roll = rng.nextInt(total);
        LootEntry picked = entries.get(entries.size() - 1);
        for (LootEntry e : entries) {
            roll -= e.chance();
            if (roll < 0) { picked = e; break; }
        }
        return buildItem(picked);
    }

    /** Clone item + roll amount theo [amountMin, amountMax] + áp enchant-roll. */
    public ItemStack buildItem(LootEntry picked) {
        ItemStack result = picked.item().clone();
        int min = Math.max(1, picked.amountMin());
        int max = Math.max(min, picked.amountMax());
        result.setAmount(min + rng.nextInt(max - min + 1));
        EnchRoll.applyAll(result, picked.enchRolls());
        return result;
    }

    /** Immutable view of a single loot-table row. */
    public record LootEntry(
        ItemStack item,
        int chance,
        int amountMin,
        int amountMax,
        List<EnchRoll> enchRolls
    ) {}
}
