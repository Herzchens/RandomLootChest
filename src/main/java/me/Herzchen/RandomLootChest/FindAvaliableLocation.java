package me.Herzchen.RandomLootChest;

import java.util.Random;
import org.bukkit.Location;
import org.bukkit.World;

public class FindAvaliableLocation {
   static FindAvaliableLocation instance = new FindAvaliableLocation();
   static int biggestx;
   static int smallestx;
   static int biggestz;
   static int smallestz;
   static int biggesty;
   static int smallesty;
   static World world;
   static int worldMaxY;
   private static boolean inited = false;

   public static boolean init() {
      if (inited) {
         return true;
      } else {
         String worldName = Main.pl.getConfig().getString("World");
         world = Main.getWorld(worldName);
         if (world == null) {
            return false;
         } else {
            worldMaxY = world.getMaxHeight() - 1;
            biggestx = Main.pl.getConfig().getInt("LargestDinctance_X");
            smallestx = Main.pl.getConfig().getInt("SmallestDinctance_X");
            biggestz = Main.pl.getConfig().getInt("LargestDinctance_Z");
            smallestz = Main.pl.getConfig().getInt("SmallestDinctance_Z");
            biggesty = Integer.min(Main.pl.getConfig().getInt("LargestDinctance_Y"), worldMaxY);
            smallesty = Integer.min(Main.pl.getConfig().getInt("SmallestDinctance_Y"), worldMaxY);
            if (smallesty > biggesty) {
               int a = smallesty;
               smallesty = biggesty;
               biggesty = a;
            }

            inited = true;
            return true;
         }
      }
   }

   public static int getRandom(int no1, int no2) {
      int max;
      int min;
      if (no1 > no2) {
         max = no1;
         min = no2;
      } else {
         max = no2;
         min = no1;
      }

      Random rand = new Random();
      int randomNum = rand.nextInt(max - min + 1) + min;
      return randomNum;
   }

   public static Location FindLocation() {
      if (!init()) {
         return null;
      } else if (biggesty >= 0 && smallesty <= worldMaxY) {
         for(int k = 0; k < 100; ++k) {
            int randomX = getRandom(smallestx, biggestx);
            int randomZ = getRandom(smallestz, biggestz);
            world.getHighestBlockYAt(randomX, randomZ);
            int randomY = getRandom(smallesty, biggesty);
            Location loc1 = new Location(world, (double)randomX, (double)randomY, (double)randomZ);
            Location loc2 = loc1.clone();
            int n = Math.max(biggesty - randomY, randomY - smallesty);

            for(int i = 1; i <= n; ++i) {
               if (loc1.getBlockY() >= smallesty) {
                  if (Main.pl.canSpawnChest(loc1)) {
                     return loc1;
                  }

                  loc1.add(0.0D, -1.0D, 0.0D);
               }

               if (loc2.getBlockY() < biggesty) {
                  loc2.add(0.0D, 1.0D, 0.0D);
                  if (Main.pl.canSpawnChest(loc2)) {
                     return loc2;
                  }
               }
            }
         }

         return null;
      } else {
         return null;
      }
   }
}
