package me.Herzchen.RandomLootChest;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

public class OpenLootInventory {
   static OpenLootInventory instance = new OpenLootInventory();

   public static ItemStack getrandomitem() {
      int all = Main.items.size();
      if (all != 0) {
         Random air1 = new Random();
         int ra = air1.nextInt(all) + 0;
         ItemStack item = (ItemStack)Main.items.get(ra);
         return item;
      } else {
         ItemStack air = new ItemStack(Material.AIR);
         return air;
      }
   }

   public static int findavaliablerandomSlot(Inventory inv) {
      int size = inv.getSize();
      int current = 1000;

      for(int i = 0; i < size; ++i) {
         Random random = new Random();
         int ra = random.nextInt(size) + 0;
         if (inv.getItem(ra) == null) {
            current = ra;
            break;
         }
      }

      return current;
   }

   public void openInvenory(Player player) {
      String invname = Main.pl.getConfig().getString("Inventory_Name").replaceAll("&", "§");
      int slots = Main.pl.getConfig().getInt("Inventory_Slots");
      Inventory inv = Main.pl.getServer().createInventory((InventoryHolder)null, slots, invname);
      fillInvenory(inv);
      player.openInventory(inv);
   }

   public static void fillInvenory(Inventory inv) {
      int itemamount = Main.pl.getConfig().getInt("ItemAmountToAdd");

      for(int i = 0; i < itemamount; ++i) {
         if (findavaliablerandomSlot(inv) != 1000) {
            int slot = findavaliablerandomSlot(inv);
            inv.setItem(slot, getrandomitem());
         }
      }

   }
}
