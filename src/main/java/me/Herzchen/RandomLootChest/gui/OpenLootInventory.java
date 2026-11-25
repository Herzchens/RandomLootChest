package me.Herzchen.RandomLootChest.gui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import me.Herzchen.RandomLootChest.Main;
import me.Herzchen.RandomLootChest.database.LoadChances;
import me.Herzchen.RandomLootChest.model.ChestType;
import me.Herzchen.RandomLootChest.model.EnchRoll;
import me.Herzchen.RandomLootChest.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class OpenLootInventory {
   public static OpenLootInventory instance = new OpenLootInventory();
   private static final Random RNG = new Random();

    public static ItemStack getrandomitem(ChestType ct) {
        // Per-type loot table (delegates weighted pick to LootTable)
        if (ct != null && ct.hasLootTable())
            return ct.getLootTable().pickRandom();

        // Fallback to global pool (loaded by LoadChances into Main.items) — weighted pick
        int pick = LoadChances.instance.rollIndex();
        if (pick < 0) return new ItemStack(Material.AIR);
        ItemStack result = Main.items.get(pick);
        if (result == null) return new ItemStack(Material.AIR);
        result = result.clone();
        int min = Main.itemMin.getOrDefault(pick, 1);
        int max = Math.max(min, Main.itemMax.getOrDefault(pick, result.getAmount()));
        result.setAmount(min + RNG.nextInt(max - min + 1));
        EnchRoll.applyAll(result, LoadChances.instance.getEnchRolls(pick));
        return result;
    }

   // ── spawn-count resolution ──

   /**
    * Số item sẽ đặt vào rương cho lần fill này:
    * <ul>
    *   <li>itemCountMin + itemCountMax đều set → random trong [min, max]</li>
    *   <li>chỉ set itemCountMin → cố định bằng min</li>
    *   <li>chỉ set itemCountMax → random trong [1, max]</li>
    *   <li>không set gì → dùng ItemAmountToAdd trong config.yml</li>
    * </ul>
    * Kết quả luôn được giới hạn bởi số ô của inventory.
    */
   private static int countToSpawn(ChestType ct, int maxSlots) {
      Integer min = (ct != null) ? ct.getItemCountMin() : null;
      Integer max = (ct != null) ? ct.getItemCountMax() : null;
      int global = Math.max(1, Main.pl.getConfig().getInt("ItemAmountToAdd", 5));
      int lo, hi;
      if (min != null && max != null) {
         lo = Math.max(1, Math.min(min, max));
         hi = Math.max(1, Math.max(min, max));
      } else if (min != null) {
         lo = hi = Math.max(1, min);
      } else if (max != null) {
         lo = 1;
         hi = Math.max(1, max);
      } else {
         lo = hi = global;
      }
      int n = lo + RNG.nextInt(hi - lo + 1);
      return Math.min(Math.max(0, n), maxSlots);
   }

   /** Fisher–Yates shuffle toàn bộ slot index → đặt item vào slot ngẫu nhiên không trùng. */
   private static int[] shuffledSlots(int size) {
      int[] slots = new int[size];
      for (int i = 0; i < size; i++) slots[i] = i;
      for (int i = size - 1; i > 0; i--) {
         int j = RNG.nextInt(i + 1);
         int t = slots[i]; slots[i] = slots[j]; slots[j] = t;
      }
      return slots;
   }

   /**
    * Chọn 1 item cho 1 slot. Khi {@code uniquePlaced != null} (uniquePicks = true),
    * item trùng (so sánh {@code isSimilar} — bỏ qua amount) sẽ bị bỏ qua.
    *
    * @return item để đặt, hoặc {@code null} nếu pool cạn / không còn item mới.
    */
   private static ItemStack pickItem(ChestType ct, List<ItemStack> uniquePlaced) {
      if (uniquePlaced == null) {
         ItemStack it = getrandomitem(ct);
         return (it == null || it.getType() == Material.AIR) ? null : it;
      }
      for (int attempt = 0; attempt < 50; attempt++) {
         ItemStack it = getrandomitem(ct);
         if (it == null || it.getType() == Material.AIR) continue;
         boolean dup = false;
         for (ItemStack p : uniquePlaced) {
            if (p.isSimilar(it)) { dup = true; break; }
         }
         if (!dup) return it;
      }
      return null;
   }

   public void openInvenory(Player player, ChestType chestType) {
       String name = Main.pl.messages.get("chest.inventory_name", "<red>Chúc mừng!");
       int slots = Main.pl.getConfig().getInt("Inventory_Slots", 27);
       int rows = slots / 9;

       Gui gui = Gui.gui()
               .title(MessageUtil.parse(name))
               .rows(rows)
               .disableAllInteractions()
               .create();

       fillInvenory(gui, chestType);
       gui.open(player);
   }

   public static void fillInvenory(Inventory inv) { fillInventory(inv, null); }

   public static void fillInvenory(Inventory inv, ChestType ct) { fillInventory(inv, ct); }

   /**
    * Fills an inventory safely, with special handling for block-based containers
    * (chests, shulkers, barrels, etc.) to avoid deadlocks and performance issues.
    */
   public static void fillInventory(Inventory inv, ChestType ct) {
      if (inv == null) return;

      InventoryHolder holder = inv.getHolder();

      // For block containers (chests, shulkers, etc.)
      if (holder instanceof BlockInventoryHolder blockHolder) {
         try {
            org.bukkit.block.BlockState state = blockHolder.getBlock().getState();

            // Use snapshot inventory to avoid triggering heavy server logic
            // (updateNeighbourForOutputSignal, PalettedContainer deadlocks, etc.)
            if (state instanceof Container container) {
               Inventory snapshotInv = container.getSnapshotInventory();
               fillInventoryDirect(snapshotInv, ct);

               // Force update without physics/neighbor updates
               state.update(true, false);
               return;
            }
         } catch (Exception e) {
            // Fallback if something goes wrong (e.g. unloaded chunk)
            Bukkit.getLogger().warning("Failed to use snapshot inventory: " + e.getMessage());
         }
      }

      // Fallback for player inventories, ender chests, custom inventories, etc.
      fillInventoryDirect(inv, ct);
   }

   // ── vanilla loot table (vanillaTable) ──

   /** Roll item từ loot table vanilla nếu chest type cấu hình {@code vanillaTable}. */
   private static List<ItemStack> getVanillaItems(ChestType ct) {
      if (ct == null) return Collections.emptyList();
      org.bukkit.loot.LootTable vanilla = ct.resolveVanillaLoot();
      if (vanilla == null) return Collections.emptyList();
      try {
         // Roll vào inventory ảo rồi đọc ra — để vẫn giữ quyền sắp slot/merge theo logic của plugin
         // LootContext cần 1 Location; chest tables không phụ thuộc context nên dùng world đầu tiên
         if (Bukkit.getWorlds().isEmpty()) return Collections.emptyList();
         LootContext ctx = new LootContext.Builder(Bukkit.getWorlds().get(0).getSpawnLocation()).build();
         Inventory temp = Bukkit.createInventory(null, 54);
         vanilla.fillInventory(temp, new Random(), ctx);
         List<ItemStack> out = new ArrayList<>();
         for (ItemStack it : temp.getContents())
            if (it != null && it.getType() != Material.AIR) out.add(it.clone());
         return out;
      } catch (Exception e) {
         Bukkit.getLogger().warning("[RandomLootChest] Không roll được vanillaTable '"
                 + ct.getVanillaTable() + "': " + e.getMessage());
         return Collections.emptyList();
      }
   }

   /** Gộp item vào list nếu {@code merge} và có stack tương tự chưa đầy; ngược lại thêm phần còn lại như slot mới. */
   private static void addWithMerge(List<ItemStack> list, ItemStack item, boolean merge) {
      if (item == null || item.getType() == Material.AIR || item.getAmount() <= 0) return;
      if (merge) {
         for (ItemStack in : list) {
            if (in.isSimilar(item) && in.getAmount() < in.getMaxStackSize()) {
               int move = Math.min(item.getAmount(), in.getMaxStackSize() - in.getAmount());
               in.setAmount(in.getAmount() + move);
               item.setAmount(item.getAmount() - move);
               if (item.getAmount() <= 0) return;
            }
         }
      }
      list.add(item);
   }

   /**
    * Roll toàn bộ nội dung 1 rương:
    * <ol>
    *   <li>Item từ loot table vanilla (nếu set {@code vanillaTable}) — RA KHỎI itemCountMin/Max</li>
    *   <li>N pick custom theo itemCountMin/Max (mỗi pick có thể bị bỏ qua theo {@code nothingChance})</li>
    * </ol>
    * Áp dụng {@code mergeStacks} + {@code uniquePicks} (unique tính cả item vanilla).
    * Public để {@code /rlc roll} mô phỏng.
    */
   public static List<ItemStack> rollChestContents(ChestType ct, int maxSlots) {
      List<ItemStack> out = new ArrayList<>();
      boolean merge = ct != null && ct.isMergeStacks();
      for (ItemStack v : getVanillaItems(ct)) addWithMerge(out, v, merge);

      int n = countToSpawn(ct, maxSlots);
      Integer nc = (ct != null) ? ct.getNothingChance() : null;
      int nothing = (nc == null) ? 0 : Math.min(100, Math.max(0, nc));
      boolean unique = ct != null && ct.isUniquePicks();
      for (int i = 0; i < n; i++) {
         if (nothing > 0 && RNG.nextInt(100) < nothing) continue;
         ItemStack it = pickItem(ct, unique ? out : null);
         if (it == null) break; // pool cạn / unique không còn item mới
         addWithMerge(out, it, merge);
      }
      return out;
   }

   private static void fillInventoryDirect(Inventory inv, ChestType ct) {
      inv.clear();
      int size = inv.getSize();
      if (size <= 0) return;

      List<ItemStack> items = rollChestContents(ct, size);
      int[] slots = shuffledSlots(size);
      for (int i = 0; i < items.size() && i < size; i++) {
         inv.setItem(slots[i], items.get(i));
      }
   }

   public static void fillInvenory(Gui gui, ChestType ct) {
      int size = gui.getInventory().getSize();
      List<ItemStack> items = rollChestContents(ct, size);
      int[] slots = shuffledSlots(size);

      // Đặt vào slot ngẫu nhiên (thay vì tuần tự dồn từ slot 0)
      for (int i = 0; i < items.size() && i < size; i++) {
         GuiItem gi = ItemBuilder.from(items.get(i)).asGuiItem(event -> event.setCancelled(true));
         gui.setItem(slots[i], gi);
      }
   }
}