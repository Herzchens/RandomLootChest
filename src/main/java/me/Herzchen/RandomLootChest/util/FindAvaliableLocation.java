package me.Herzchen.RandomLootChest.util;

import me.Herzchen.RandomLootChest.Main;
import me.Herzchen.RandomLootChest.region.Region;
import me.Herzchen.RandomLootChest.region.RegionConfig;
import org.bukkit.Location;
import org.bukkit.World;
import java.util.Random;

public final class FindAvaliableLocation {
   private static int biggestx, smallestx, biggestz, smallestz, biggesty, smallesty, worldMaxY;
   private static World world;
   private static boolean inited = false;

   private FindAvaliableLocation() {}

   public static boolean init() {
      if (inited) return true;
      String worldName = Main.pl.getConfig().getString("World");
      world = RLCUtils.getWorld(worldName);
      if (world == null) return false;
      worldMaxY = world.getMaxHeight() - 1;
      biggestx = Main.pl.getConfig().getInt("LargestDinctance_X");
      smallestx = Main.pl.getConfig().getInt("SmallestDinctance_X");
      biggestz = Main.pl.getConfig().getInt("LargestDinctance_Z");
      smallestz = Main.pl.getConfig().getInt("SmallestDinctance_Z");
      biggesty = Math.min(Main.pl.getConfig().getInt("LargestDinctance_Y"), worldMaxY);
      smallesty = Math.min(Main.pl.getConfig().getInt("SmallestDinctance_Y"), worldMaxY);
      if (smallesty > biggesty) { int a = smallesty; smallesty = biggesty; biggesty = a; }
      inited = true;
      return true;
   }

   public static int getRandom(int no1, int no2) {
      int max = Math.max(no1, no2), min = Math.min(no1, no2);
      return new Random().nextInt(max - min + 1) + min;
   }

   /**
    * Find a spawn location within a specific region.
    */
   public static Location FindLocation(Region region) {
      if (!init()) return null;
      World w = region.getWorld();
      int bY = region.getYMax(), sY = region.getYMin();
      if (sY > bY) { int t = sY; sY = bY; bY = t; }
      if (sY < 0) sY = 0;
      if (bY > w.getMaxHeight() - 1) bY = w.getMaxHeight() - 1;

      for (int k = 0; k < 100; k++) {
         int randomX = region.getRandomX(), randomZ = region.getRandomZ();
         int randomY = getRandom(sY, bY);
         Location loc1 = new Location(w, randomX, randomY, randomZ);
         Location loc2 = loc1.clone();
         int n = Math.max(bY - randomY, randomY - sY);
         for (int i = 1; i <= n; i++) {
            if (loc1.getBlockY() >= sY) {
               if (Main.pl.canSpawnChest(loc1)) return loc1;
               loc1.add(0, -1, 0);
            }
            if (loc2.getBlockY() < bY) {
               loc2.add(0, 1, 0);
               if (Main.pl.canSpawnChest(loc2)) return loc2;
            }
         }
      }
      return null;
   }

   /**
    * Legacy: Find location using global bounds. Used as fallback.
    */
   public static Location FindLocation() {
      if (!init() || biggesty < 0 || smallesty > worldMaxY) return null;
      for (int k = 0; k < 100; k++) {
         int randomX = getRandom(smallestx, biggestx), randomZ = getRandom(smallestz, biggestz);
         int randomY = getRandom(smallesty, biggesty);
         Location loc1 = new Location(world, randomX, randomY, randomZ);
         Location loc2 = loc1.clone();
         int n = Math.max(biggesty - randomY, randomY - smallesty);
         for (int i = 1; i <= n; i++) {
            if (loc1.getBlockY() >= smallesty) {
               if (Main.pl.canSpawnChest(loc1)) return loc1;
               loc1.add(0, -1, 0);
            }
            if (loc2.getBlockY() < biggesty) {
               loc2.add(0, 1, 0);
               if (Main.pl.canSpawnChest(loc2)) return loc2;
            }
         }
      }
      return null;
   }
}
