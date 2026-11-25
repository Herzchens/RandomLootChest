package me.Herzchen.RandomLootChest.model;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Một dòng {@code enchant-roll} trong loot table: enchant được áp dụng NGẪU NHIÊN
 * (level trong khoảng [min, max]) mỗi lần item được pick — thay vì phải tạo sẵn
 * từng variant enchant khác nhau trong database.yml.
 *
 * <p>YAML (đặt trong section của từng item):
 * <pre>{@code
 * enchant-roll:
 *   - {ench: SHARPNESS, min: 1, max: 5}
 *   - {ench: UNBREAKING, min: 1, max: 3}
 * }</pre>
 */
public record EnchRoll(Enchantment enchantment, int min, int max) {

    /** Parse danh sách map thô từ YAML ({@code getMapList("enchant-roll")}); bỏ qua dòng lỗi. */
    public static List<EnchRoll> parseList(List<Map<?, ?>> raw) {
        List<EnchRoll> out = new ArrayList<>();
        if (raw == null) return out;
        for (Map<?, ?> m : raw) {
            Object nameObj = m.get("ench");
            if (nameObj == null) nameObj = m.get("enchant");
            if (nameObj == null) continue;
            Enchantment ench = resolve(String.valueOf(nameObj));
            if (ench == null) continue;
            int mn = intOf(m.get("min"), 1);
            int mx = intOf(m.get("max"), mn);
            if (mx < mn) { int t = mn; mn = mx; mx = t; }
            out.add(new EnchRoll(ench, Math.max(1, mn), Math.max(1, mx)));
        }
        return out;
    }

    private static int intOf(Object o, int def) {
        if (o instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(o)); } catch (NumberFormatException e) { return def; }
    }

    /**
     * Nhận cả tên legacy Bukkit ({@code SHARPNESS}, {@code DAMAGE_ALL}...)
     * lẫn key modern ({@code sharpness}, {@code minecraft:sharpness}).
     */
    public static Enchantment resolve(String name) {
        String n = name.trim();
        if (n.contains(":")) n = n.substring(n.indexOf(':') + 1);
        n = n.toUpperCase(Locale.ROOT).replace(' ', '_');
        Enchantment e = Enchantment.getByName(n); // legacy mapping (deprecated nhưng vẫn chạy)
        if (e != null) return e;
        try {
            return Registry.ENCHANTMENT.get(NamespacedKey.minecraft(n.toLowerCase(Locale.ROOT)));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /** Roll level ngẫu nhiên trong [min, max] và add vào item (không quan tâm conflict). */
    public void applyTo(ItemStack stack) {
        if (stack == null || enchantment == null) return;
        int lvl = min + ThreadLocalRandom.current().nextInt(Math.max(1, max - min + 1));
        stack.addUnsafeEnchantment(enchantment, lvl);
    }

    /** Tiện ích: áp cả danh sách roll lên 1 item. */
    public static void applyAll(ItemStack stack, List<EnchRoll> rolls) {
        if (rolls == null) return;
        for (EnchRoll r : rolls) r.applyTo(stack);
    }
}
