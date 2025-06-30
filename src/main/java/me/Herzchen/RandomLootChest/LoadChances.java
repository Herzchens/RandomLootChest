package me.Herzchen.RandomLootChest;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public class LoadChances {
   static LoadChances instance = new LoadChances();

   public ConfigurationSection itdb() {
      return Main.pl.db.data.getConfigurationSection("ItemDatabase");
   }

   public void save() {
      Main.pl.db.saveData();
   }

   public void loaditems() {
      this.clear();

      for(int i = 0; i < 100000 && this.itdb().isConfigurationSection(String.valueOf(i)); ++i) {
         ItemStack item = this.itdb().getConfigurationSection(String.valueOf(i)).getItemStack("item");
         int chance = this.itdb().getConfigurationSection(String.valueOf(i)).getInt("chance");

         for(int j = 0; j < chance; ++j) {
            for(int k = 0; k < 32000; ++k) {
               if (!Main.items.containsKey(k)) {
                  Main.items.put(k, item);
                  break;
               }
            }
         }
      }

   }

   public void clear() {
      Main.items.clear();
   }
}
