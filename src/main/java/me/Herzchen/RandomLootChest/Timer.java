package me.Herzchen.RandomLootChest;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.Inventory;

class Timer {
   static Timer instance = new Timer();
   private Database data;
   private LootEvent le;

   private Timer() {
      this.data = Database.instance;
      this.le = new LootEvent();
   }

   private ConfigurationSection chests() {
      return this.data.data.getConfigurationSection("Chests");
   }

   boolean loadChests() {
      Iterator var1 = this.chests().getKeys(true).iterator();

      int x;
      int z;
      while(var1.hasNext()) {
         String s = (String)var1.next();
         if (!s.contains(".")) {
            World world = Main.getWorld(this.chests().getConfigurationSection(s).getString("World"));
            if (world == null) {
               return false;
            }

            x = this.chests().getConfigurationSection(s).getInt("X");
            int y = this.chests().getConfigurationSection(s).getInt("Y");
            x = this.chests().getConfigurationSection(s).getInt("Z");
            Location loc = new Location(world, (double)x, (double)y, (double)x);
            z = this.chests().getConfigurationSection(s).getInt("TimeToDelete");
            String block = this.chests().getConfigurationSection(s).getString("Block", "AIR");
            Main.pl.RandomChests.put(loc, new RandomChestInfo(z, block));
            this.chests().set(s, (Object)null);
         }
      }

      ConfigurationSection section = this.data.data.getConfigurationSection("FixedChests");
      if (section != null) {
         Iterator var12 = ((Set)section.getKeys(true).stream().filter((xx) -> {
            return !xx.contains(".");
         }).collect(Collectors.toSet())).iterator();

         while(var12.hasNext()) {
            String key = (String)var12.next();
            ConfigurationSection data = section.getConfigurationSection(key);
            World world = Main.getWorld(data.getString("World"));
            if (world == null) {
               return false;
            }

            x = data.getInt("X");
            int y = data.getInt("Y");
            z = data.getInt("Z");
            int time = data.getInt("TimeLeft");
            Location location = new Location(world, (double)x, (double)y, (double)z);
            if (Main.isFixedChestType(location)) {
               Main.pl.FixedChests.put(location, time);
            } else {
               section.set(key, (Object)null);
            }
         }
      }

      this.data.saveData();
      return true;
   }

   void saveChests() {
      int counter = 0;

      for(Iterator var3 = Main.pl.RandomChests.entrySet().iterator(); var3.hasNext(); ++counter) {
         Entry<Location, RandomChestInfo> e = (Entry)var3.next();
         Location loc = (Location)e.getKey();
         this.chests().createSection("Chest" + counter);
         this.chests().getConfigurationSection("Chest" + counter).set("World", loc.getWorld().getName());
         this.chests().getConfigurationSection("Chest" + counter).set("X", loc.getBlockX());
         this.chests().getConfigurationSection("Chest" + counter).set("Y", loc.getBlockY());
         this.chests().getConfigurationSection("Chest" + counter).set("Z", loc.getBlockZ());
         this.chests().getConfigurationSection("Chest" + counter).set("TimeToDelete", ((RandomChestInfo)e.getValue()).Time);
         this.chests().getConfigurationSection("Chest" + counter).set("Block", ((RandomChestInfo)e.getValue()).Block);
      }

      this.data.data.set("FixedChests", (Object)null);
      ConfigurationSection section = this.data.data.createSection("FixedChests");
      counter = 0;
      Iterator var8 = Main.pl.FixedChests.entrySet().iterator();

      while(var8.hasNext()) {
         Entry<Location, Integer> item = (Entry)var8.next();
         Location location = (Location)item.getKey();
         Object[] var10002 = new Object[1];
         ++counter;
         var10002[0] = counter;
         ConfigurationSection data = section.createSection(String.format("%d", var10002));
         data.set("World", location.getWorld().getName());
         data.set("X", location.getBlockX());
         data.set("Y", location.getBlockY());
         data.set("Z", location.getBlockZ());
         data.set("TimeLeft", item.getValue());
      }

      this.data.saveData();
   }

   void decrease() {
      Main.pl.getServer().getScheduler().scheduleSyncRepeatingTask(Main.pl, () -> {
         Iterator var1 = (new ArrayList(Main.pl.RandomChests.entrySet())).iterator();

         Entry kv;
         while(var1.hasNext()) {
            kv = (Entry)var1.next();
            RandomChestInfo value = (RandomChestInfo)kv.getValue();
            int timeToDelete = value.Time;
            if (timeToDelete > 0) {
               value.Time = timeToDelete - 1;
            } else {
               Location loc1 = (Location)kv.getKey();
               this.le.deleteChest(loc1);
               if (Main.pl.MessageOnKill != null) {
                  String string = Main.pl.MessageOnKill.replace("&", "§");
                  String x1 = String.valueOf(loc1.getBlockX());
                  String y1 = String.valueOf(loc1.getBlockY());
                  String z1 = String.valueOf(loc1.getBlockZ());
                  String string2 = string.replace("{X}", x1);
                  String string3 = string2.replace("{Y}", y1);
                  String string4 = string3.replace("{Z}", z1);
                  Main.pl.getServer().broadcastMessage(string4);
               }
            }
         }

         var1 = Main.pl.FixedChests.entrySet().iterator();

         while(var1.hasNext()) {
            kv = (Entry)var1.next();
            int k = (Integer)kv.getValue() - 1;
            if (k > 0) {
               kv.setValue(k);
            } else {
               kv.setValue(Integer.max(FindAvaliableLocation.getRandom(Main.pl.FixedChestUpdateTimeMin, Main.pl.FixedChestUpdateTimeMax), 0));
               Location location = (Location)kv.getKey();
               Block block = location.getBlock();
               if (Main.isFixedChestType(block)) {
                  Inventory inv = Main.getInventory(block);
                  if (inv != null) {
                     inv.clear();
                     OpenLootInventory.fillInvenory(inv);
                     Main.pl.FixedChestSound.play(location);
                     Main.pl.FixedChestEffect.play(location);
                  }
               }
            }
         }

      }, 20L, 20L);
   }
}
