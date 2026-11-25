package me.Herzchen.RandomLootChest.database;

import me.Herzchen.RandomLootChest.Main;
import me.Herzchen.RandomLootChest.model.ChestType;
import me.Herzchen.RandomLootChest.model.FixedChestInfo;
import me.Herzchen.RandomLootChest.model.RandomChestInfo;
import me.Herzchen.RandomLootChest.listener.LootEvent;
import me.Herzchen.RandomLootChest.gui.OpenLootInventory;
import me.Herzchen.RandomLootChest.util.FindAvaliableLocation;
import me.Herzchen.RandomLootChest.util.MessageUtil;
import me.Herzchen.RandomLootChest.util.RLCUtils;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.Inventory;
import java.util.ArrayList;
import java.util.Map.Entry;

public class Timer {
   public static Timer instance = new Timer();
   private Database data;
   private LootEvent le;

   private Timer() { data = Database.instance; le = new LootEvent(); }

   private ConfigurationSection chests() { return data.data.getConfigurationSection("Chests"); }

   public boolean loadChests() {
      ConfigurationSection chestsSection = chests();
      if (chestsSection != null) {
         for (String s : chestsSection.getKeys(false)) {
            ConfigurationSection cs = chestsSection.getConfigurationSection(s);
            if (cs == null) continue;
            World world = RLCUtils.getWorld(cs.getString("World"));
            if (world == null) continue;
            Location loc = new Location(world, cs.getInt("X"), cs.getInt("Y"), cs.getInt("Z"));
            Main.pl.RandomChests.put(loc, new RandomChestInfo(
                    cs.getInt("TimeToDelete"), cs.getString("Block", "AIR"),
                    cs.getString("ChestTypeId", "default")));
            chestsSection.set(s, null);
         }
      }
      ConfigurationSection fixedSection = data.data.getConfigurationSection("FixedChests");
      if (fixedSection != null) {
         for (String key : fixedSection.getKeys(false)) {
            ConfigurationSection cd = fixedSection.getConfigurationSection(key);
            if (cd == null) continue;
            World world = RLCUtils.getWorld(cd.getString("World"));
            if (world == null) continue;
            Location loc = new Location(world, cd.getInt("X"), cd.getInt("Y"), cd.getInt("Z"));
            if (RLCUtils.isFixedChestType(loc))
                Main.pl.FixedChests.put(loc, new FixedChestInfo(cd.getInt("TimeLeft"), cd.getString("ChestTypeId", null)));
            else fixedSection.set(key, null);
         }
      }
      data.saveData();
      return true;
   }

   public void saveChests() {
       {
          int c = 0;
          for (Entry<Location, RandomChestInfo> e : Main.pl.RandomChests.entrySet()) {
             Location loc = e.getKey();
             ConfigurationSection s = chests().createSection("Chest" + c);
             s.set("World", loc.getWorld().getName());
             s.set("X", loc.getBlockX()); s.set("Y", loc.getBlockY()); s.set("Z", loc.getBlockZ());
             s.set("TimeToDelete", e.getValue().Time);
             s.set("Block", e.getValue().Block);
             s.set("ChestTypeId", e.getValue().ChestTypeId);
             c++;
          }
       }
       data.data.set("FixedChests", null);
       ConfigurationSection section = data.data.createSection("FixedChests");
       int c = 0;
       for (Entry<Location, FixedChestInfo> e : Main.pl.FixedChests.entrySet()) {
          c++;
          ConfigurationSection d = section.createSection(String.valueOf(c));
          Location l = e.getKey();
          d.set("World", l.getWorld().getName());
          d.set("X", l.getBlockX()); d.set("Y", l.getBlockY()); d.set("Z", l.getBlockZ());
          d.set("TimeLeft", e.getValue().Time);
          if (e.getValue().ChestTypeId != null) d.set("ChestTypeId", e.getValue().ChestTypeId);
       }
       data.saveData();
   }

   public void decrease() {
      Main.pl.getServer().getScheduler().scheduleSyncRepeatingTask(Main.pl, () -> {
         for (Entry<Location, RandomChestInfo> e : new ArrayList<>(Main.pl.RandomChests.entrySet())) {
            if (e.getValue().Time > 0) { e.getValue().Time--; }
            else {
               Location loc = e.getKey(); le.deleteChest(loc);
               if (Main.pl.getMessageOnKill() != null) {
                   MessageUtil.broadcastWithPlaceholders(
                           Main.pl.getMessageOnKill(),
                           "{X}", String.valueOf(loc.getBlockX()),
                           "{Y}", String.valueOf(loc.getBlockY()),
                           "{Z}", String.valueOf(loc.getBlockZ()));
               }
            }
         }
           for (Entry<Location, FixedChestInfo> e : Main.pl.FixedChests.entrySet()) {
              FixedChestInfo info = e.getValue();
              int k = info.Time - 1;
              if (k > 0) info.Time = k;
              else {
                 // Use ChestType's spawnTimeMin/Max if available, otherwise fallback to global
                 ChestType ct = info.ChestTypeId != null ? ChestType.getChestType(info.ChestTypeId) : null;
                 int timeMin = (ct != null) ? ct.getSpawnTimeMin() : Main.pl.getFixedChestUpdateTimeMin();
                 int timeMax = (ct != null) ? ct.getSpawnTimeMax() : Main.pl.getFixedChestUpdateTimeMax();
                 info.Time = Math.max(FindAvaliableLocation.getRandom(timeMin, timeMax), 0);
                 Block block = e.getKey().getBlock();
                 if (RLCUtils.isFixedChestType(block)) {
                    Inventory inv = RLCUtils.getInventory(block);
                    if (inv != null) {
                       inv.clear();
                       OpenLootInventory.fillInvenory(inv, ct);
                       if (Main.pl.getFixedChestSound() != null) Main.pl.getFixedChestSound().play(e.getKey());
                       if (Main.pl.getFixedChestEffect() != null) Main.pl.getFixedChestEffect().play(e.getKey());
                    }
                 }
              }
           }
      }, 20L, 20L);
   }
}