package me.Herzchen.RandomLootChest.database;

import me.Herzchen.RandomLootChest.Main;
import me.Herzchen.RandomLootChest.model.EnchRoll;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

public class LoadChances {
   public static LoadChances instance = new LoadChances();

   /** Prefix-sum weight: cumulative.get(i) = tổng chance của entry 0..i (weighted pick O(log n)). */
   private final List<Integer> cumulative = new ArrayList<>();
   private final Random rng = new Random();
   /** enchant-roll theo entry index (chỉ dùng cho pool global). */
   private final Map<Integer, List<EnchRoll>> enchRolls = new HashMap<>();

   public ConfigurationSection itdb() {
      return Main.pl.db.data.getConfigurationSection("ItemDatabase");
   }

   public void loaditems() {
      clear();
      ConfigurationSection section = itdb();
      if (section == null) return;
      int idx = 0, total = 0;
      for (int i = 0; i < 100000 && section.isConfigurationSection(String.valueOf(i)); i++) {
         ConfigurationSection es = Objects.requireNonNull(section.getConfigurationSection(String.valueOf(i)));
         ItemStack item = es.getItemStack("item");
         int chance = es.getInt("chance");
         if (item == null || chance <= 0) continue;
         int aMin = Math.max(1, es.getInt("amount-min", 1));
         int aMax = Math.max(aMin, es.getInt("amount-max", item.getAmount()));
         Main.items.put(idx, item);
         Main.itemMin.put(idx, aMin);
         Main.itemMax.put(idx, aMax);
         enchRolls.put(idx, EnchRoll.parseList(es.getMapList("enchant-roll")));
         total += chance;
         cumulative.add(total);
         idx++;
      }
      if (idx == 0) {
         Main.pl.getLogger().warning("Global loot table (ItemDatabase) is empty! Placeholder items may be given.");
      }
   }

   /** @return index entry được pick theo trọng số, hoặc -1 nếu pool rỗng. */
   public int rollIndex() {
      if (cumulative.isEmpty()) return -1;
      int total = cumulative.get(cumulative.size() - 1);
      int roll = rng.nextInt(total);
      int lo = 0, hi = cumulative.size() - 1;
      while (lo < hi) {
         int mid = (lo + hi) >>> 1;
         if (cumulative.get(mid) <= roll) lo = mid + 1; else hi = mid;
      }
      return lo;
   }

   public List<EnchRoll> getEnchRolls(int idx) { return enchRolls.getOrDefault(idx, Collections.emptyList()); }

   /** Tổng weight của pool global (để quy đổi %). */
   public int totalWeight() { return cumulative.isEmpty() ? 0 : cumulative.get(cumulative.size() - 1); }

   public void clear() { Main.items.clear(); Main.itemMin.clear(); Main.itemMax.clear(); cumulative.clear(); enchRolls.clear(); }
}