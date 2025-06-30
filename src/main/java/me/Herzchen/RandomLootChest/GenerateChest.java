package me.Herzchen.RandomLootChest;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.material.Chest;
import org.bukkit.material.MaterialData;

public class GenerateChest {
   Database data;
   static BlockFace[] chestFaces;

   public GenerateChest() {
      this.data = Database.instance;
   }

   public ConfigurationSection data() {
      return this.data.data;
   }

   public void GenerateChest(int time) {
      Main.pl.getServer().getScheduler().scheduleSyncRepeatingTask(Main.pl, new Runnable() {
         public void run() {
            GenerateChest.this.spawnchest();
         }
      }, (long)time * 20L, (long)time * 20L);
   }

   public void spawnchest() {
      Location loc = FindAvaliableLocation.FindLocation();
      if (loc != null) {
         this.saveChest(loc);
         loc.getBlock().setType(Main.RANDOM_CHEST_TYPE);
         BlockState state = loc.getBlock().getState();
         state.setData(new Chest(chestFaces[FindAvaliableLocation.getRandom(0, 3)]));
         state.update();
         if (Main.pl.MessageOnSpawn != null) {
            String x = String.valueOf(loc.getBlockX());
            String y = String.valueOf(loc.getBlockY());
            String z = String.valueOf(loc.getBlockZ());
            String string = Main.pl.MessageOnSpawn.replace("&", "§");
            String string2 = string.replace("{X}", x);
            String string3 = string2.replace("{Y}", y);
            String string4 = string3.replace("{Z}", z);
            Main.pl.getServer().broadcastMessage(string4);
         }
      }

   }

   void saveChest(Location location) {
      MaterialData md = location.getBlock().getState().getData();
      String mdText = String.format("%s:%d", md.getItemType(), md.getData());
      Main.pl.RandomChests.put(location, new RandomChestInfo(Main.pl.getConfig().getInt("KillChestAfterTime"), mdText));
      Main.pl.RandomChestSound.play(location);
      Main.pl.RandomChestEffect.play(location);
   }

   static {
      chestFaces = new BlockFace[]{BlockFace.EAST, BlockFace.WEST, BlockFace.NORTH, BlockFace.SOUTH};
   }
}
