package me.Herzchen.RandomLootChest.model;

import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Encapsulates a single player's item-editing session: the items, chances,
 * which chest type is being edited, and UI navigation state.
 *
 * <p>Previously these were scattered across separate maps on {@code Main}.
 * Grouping them makes multi-player editing safe and the code easier to follow.
 */
public class ItemEditSession {

    /** Item ID → ItemStack. */
    private final Map<Integer, ItemStack> items = new HashMap<>();

    /** Item ID → chance (1–100). */
    private final Map<Integer, Integer> chances = new HashMap<>();

    private String editingChestType; // null or GLOBAL_KEY → global; otherwise a chest type ID
    private int currentPage = 1;
    private int editingId = -1;
    private int lastPageNo = 1;

    // ── items ──

    public Map<Integer, ItemStack> getItems() { return items; }
    public Map<Integer, Integer> getChances() { return chances; }

    public ItemStack getItem(int id) { return items.get(id); }
    public void setItem(int id, ItemStack stack) { items.put(id, stack); }
    public void removeItem(int id) { items.remove(id); chances.remove(id); }
    /** Remove only the item, preserving the chance and amount range values. */
    public void removeItemOnly(int id) { items.remove(id); }
    public boolean hasItem(int id) { return items.containsKey(id); }

    public int getChance(int id) { return chances.getOrDefault(id, 50); }
    public void setChance(int id, int value) { chances.put(id, value); }
    public void addChance(int id, int delta) {
        chances.merge(id, delta, (old, d) -> Math.max(1, Math.min(100, old + d)));
    }

    // ── enchant-roll (đọc/lưu nguyên trạng từ YAML, GUI không sửa trực tiếp) ──

    /** Item ID → danh sách map thô của key {@code enchant-roll} trong database.yml. */
    private final Map<Integer, List<Map<?, ?>>> enchRolls = new HashMap<>();

    public List<Map<?, ?>> getEnchRolls(int id) { return enchRolls.getOrDefault(id, Collections.emptyList()); }
    public void setEnchRolls(int id, List<Map<?, ?>> raw) {
        if (raw == null || raw.isEmpty()) enchRolls.remove(id); else enchRolls.put(id, raw);
    }
    public boolean hasEnchRolls(int id) { return enchRolls.containsKey(id); }

    // ── amount range ──

    private final Map<Integer, Integer> amountMin = new HashMap<>();
    private final Map<Integer, Integer> amountMax = new HashMap<>();

    public int getAmountMin(int id) { return amountMin.getOrDefault(id, 1); }
    public int getAmountMax(int id) { return amountMax.getOrDefault(id, getItemAmount(id)); }

    /** Set a fixed amount (min == max). */
    public void setFixedAmount(int id, int amount) {
        int clamped = Math.max(1, Math.min(99, amount));
        amountMin.put(id, clamped);
        amountMax.put(id, clamped);
        setItemAmount(id, clamped);
    }

    /** Set a range. If min > max, they are swapped. */
    public void setAmountRange(int id, int min, int max) {
        int a = Math.max(1, Math.min(99, Math.min(min, max)));
        int b = Math.max(1, Math.min(99, Math.max(min, max)));
        amountMin.put(id, a);
        amountMax.put(id, b);
        setItemAmount(id, b); // display default = max
    }

    /** True if the item has a range (min != max). */
    public boolean hasAmountRange(int id) {
        return getAmountMin(id) != getAmountMax(id);
    }

    /** User-friendly display: "5" or "3-8". */
    public String getAmountDisplay(int id) {
        int mn = getAmountMin(id);
        int mx = getAmountMax(id);
        return mn == mx ? String.valueOf(mn) : mn + "-" + mx;
    }

    public void clearAmountRange(int id) {
        amountMin.remove(id);
        amountMax.remove(id);
    }

    public void clearAll() { items.clear(); chances.clear(); amountMin.clear(); amountMax.clear(); enchRolls.clear(); }

    /** Get the stored ItemStack's amount (fallback 1 if not set or null). */
    public int getItemAmount(int id) {
        ItemStack s = items.get(id);
        return s == null ? 1 : s.getAmount();
    }

    /** Set the ItemStack's amount for the given item. */
    public void setItemAmount(int id, int newAmount) {
        ItemStack s = items.get(id);
        if (s != null) {
            s.setAmount(Math.max(1, Math.min(99, newAmount)));
        }
    }

    // ── search filter (used by chest type selector) ──

    private String searchFilter = "";

    /** Current search filter for the chest type selector. Empty = show all. */
    public String getSearchFilter() { return searchFilter; }
    public void setSearchFilter(String v) { searchFilter = v == null ? "" : v; }

    /** When true, {@code closeGuiAction} must NOT end this session (e.g. navigating to chance editor). */
    private boolean keepOnClose = false;

    public boolean isKeepOnClose() { return keepOnClose; }
    public void setKeepOnClose(boolean v) { keepOnClose = v; }

    // ── session state ──

    public String getEditingChestType() { return editingChestType; }
    public void setEditingChestType(String v) { editingChestType = v; }

    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int v) { currentPage = v; }

    public int getEditingId() { return editingId; }
    public void setEditingId(int v) { editingId = v; }

    public int getLastPageNo() { return lastPageNo; }
    public void setLastPageNo(int v) { lastPageNo = v; }

    /** True if the chest type is global (null or GLOBAL_KEY). */
    public boolean isGlobal() {
        return editingChestType == null || "__global__".equals(editingChestType);
    }

    /** The storage key used for YAML: null for global, otherwise the type ID. */
    public String getStorageKey() {
        return isGlobal() ? null : editingChestType;
    }
}
