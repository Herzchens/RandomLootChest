package me.Herzchen.RandomLootChest;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Objects;

public class LootEvent implements Listener {
   Database data;
   OpenLootInventory olv;

   public LootEvent() {
      this.data = Database.instance;
      this.olv = OpenLootInventory.instance;
   }

   public boolean isChest(Location loc) {
      return Main.pl.RandomChests.containsKey(loc);
   }

   public void deleteChest(Location loc) {
      RandomChestInfo value = Main.pl.RandomChests.get(loc);
      Main.pl.RandomChests.remove(loc);
      BlockData blockData = this.parseBlockData(value.Block);
      Block block = loc.getBlock();
      block.setBlockData(blockData);
      BlockState blockState = block.getState();
      blockState.update(true);
   }

   BlockData parseBlockData(String s) {
      if (s != null) {
         if (s.startsWith("LEGACY_")) {
            String materialName = s.split(":")[0].replace("LEGACY_", "");
            Material material = Material.matchMaterial(materialName, true);
            if (material != null) {
               return material.createBlockData();
            }
         }
         try {
            return Bukkit.createBlockData(s);
         } catch (IllegalArgumentException e) {
            return Material.AIR.createBlockData();
         }
      }
      return Material.AIR.createBlockData();
   }

   public void killallchests() {
      Iterator var1 = (new ArrayList(Main.pl.RandomChests.entrySet())).iterator();

      while(var1.hasNext()) {
         Entry<Location, RandomChestInfo> item = (Entry)var1.next();
         this.deleteChest((Location)item.getKey());
      }

   }

   @EventHandler
   public void onBlockClick(PlayerInteractEvent e) {
      Player player = e.getPlayer();
      if (e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
         Location location = Objects.requireNonNull(e.getClickedBlock()).getLocation();
         String x1;
         if (Main.isFixedChestType(e.getClickedBlock()) && Main.pl.addChestplayers.containsKey(player)) {
            CommandManager.WaitChooseChest wcc = (CommandManager.WaitChooseChest)Main.pl.addChestplayers.get(player);
            wcc.cancel();
            Main.pl.addChestplayers.remove(player);
            if (!this.isChest(location)) {
               x1 = wcc.Command;
               byte var15 = -1;
               switch(x1.hashCode()) {
                  case 96417:
                     if (x1.equals("add")) {
                        var15 = 0;
                     }
                     break;
                  case 99339:
                     if (x1.equals("del")) {
                        var15 = 1;
                     }
               }

               Inventory inventory;
               switch(var15) {
                  case 0:
                     if (!Main.pl.FixedChests.containsKey(location)) {
                        BlockState blockState = location.getBlock().getState();
                        inventory = Main.getInventory(blockState);
                        if (inventory != null) {
                           inventory.clear();
                           OpenLootInventory.fillInvenory(inventory);
                           Main.pl.FixedChests.put(location, FindAvaliableLocation.getRandom(1, FindAvaliableLocation.getRandom(1, Integer.max(FindAvaliableLocation.getRandom(Main.pl.FixedChestUpdateTimeMin, Main.pl.FixedChestUpdateTimeMax), 0))));
                           Main.pl.FixedChestSound.play(location);
                           Main.pl.FixedChestEffect.play(location);
                           player.sendMessage("§arương cố định đã được thêm vào bộ sưu tập §f=)");
                        } else {
                           player.sendMessage("§cCó gì đó không ổn §f=(");
                        }
                     } else {
                        player.sendMessage("§cOops... rương này đã được thêm vào rồi §f=\\");
                     }
                     break;
                  case 1:
                     if (Main.pl.FixedChests.containsKey(location)) {
                        Main.pl.FixedChests.remove(location);
                        Block block = location.getBlock();
                        if (Main.isFixedChestType(block)) {
                           inventory = Main.getInventory(location);
                           if (inventory != null) {
                              inventory.clear();
                              Main.pl.FixedChestSound.play(location);
                              Main.pl.FixedChestEffect.play(location);
                           }
                        }

                        player.sendMessage("§arương cố định đã bị xóa khỏi bộ sưu tập §f=)");
                     } else {
                        player.sendMessage("§cOops... Cái rương này không phải của chúng ta §f=\\");
                     }
               }
            }

            e.setCancelled(true);
         } else if (Main.isRandomChestType(e.getClickedBlock()) && this.isChest(e.getClickedBlock().getLocation())) {
            Main.pl.RandomChestOpenSound.play(location);
            this.olv.openInvenory(player);
            if (Main.pl.commands != null) {
               Iterator x = Main.pl.commands.iterator();

               while(x.hasNext()) {
                  x1 = (String)x.next();
                  Main.pl.getServer().dispatchCommand(Main.pl.getServer().getConsoleSender(), x1.replace("{player}", player.getName()));
               }
            }

            this.deleteChest(location);
            e.setCancelled(true);
            if (Main.pl.MessageOnLoot != null) {
               Location loc1 = e.getClickedBlock().getLocation();
               x1 = String.valueOf(loc1.getBlockX());
               String y = String.valueOf(loc1.getBlockY());
               String z = String.valueOf(loc1.getBlockZ());
               String string = Main.pl.MessageOnLoot.replace("&", "§");
               String string2 = string.replace("{X}", x1);
               String string3 = string2.replace("{Y}", y);
               String string4 = string3.replace("{Z}", z);
               String string5 = string4.replace("{Player}", player.getName());
               Main.pl.getServer().broadcastMessage(string5);
            }
         }
      }

   }

   @EventHandler
   public void onChestBreak(BlockBreakEvent e) {
      Block block = e.getBlock();
      Location location = block.getLocation();
      if (Main.isRandomChestType(block) && this.isChest(location)) {
         if (!Main.pl.abletobreak.contains(e.getPlayer())) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(Objects.requireNonNull(Main.pl.getConfig().getString("NotAbleToBreakAchest")).replaceAll("&", "§"));
         } else {
            e.setCancelled(true);
            this.deleteChest(e.getBlock().getLocation());
            e.getPlayer().sendMessage("§aBạn đã phá được 1 hòm thính");
         }
      } else if (Main.isFixedChestType(block) && Main.pl.FixedChests.containsKey(location)) {
         if (!Main.pl.abletobreak.contains(e.getPlayer())) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(Objects.requireNonNull(Main.pl.getConfig().getString("NotAbleToBreakAchest")).replaceAll("&", "§"));
         } else {
            e.setCancelled(true);
            Main.pl.FixedChests.remove(location);
            Inventory inventory = Main.getInventory(block);
            if (inventory != null) {
               inventory.clear();
            }

            e.getPlayer().sendMessage("§arương cố định đã bị xóa khỏi bộ sưu tập");
         }
      }

   }
}