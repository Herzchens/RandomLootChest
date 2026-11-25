package me.Herzchen.RandomLootChest.listener;

import me.Herzchen.RandomLootChest.Main;
import me.Herzchen.RandomLootChest.database.Database;
import me.Herzchen.RandomLootChest.model.ChestType;
import me.Herzchen.RandomLootChest.model.FixedChestInfo;
import me.Herzchen.RandomLootChest.model.RandomChestInfo;
import me.Herzchen.RandomLootChest.gui.OpenLootInventory;
import me.Herzchen.RandomLootChest.command.WaitChooseChest;
import me.Herzchen.RandomLootChest.util.FindAvaliableLocation;
import me.Herzchen.RandomLootChest.util.MessageUtil;
import me.Herzchen.RandomLootChest.util.RLCUtils;
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
import java.util.Objects;

public class LootEvent implements Listener {
   Database data;
   OpenLootInventory olv;
   public LootEvent() { data = Main.pl.db; olv = new OpenLootInventory(); }

   public boolean isChest(Location loc) { return Main.pl.RandomChests.containsKey(loc); }

   public void deleteChest(Location loc) {
      RandomChestInfo v = Main.pl.RandomChests.remove(loc);
      BlockData bd = parseBlockData(v.Block);
      Block b = loc.getBlock(); b.setBlockData(bd); b.getState().update(true);
   }

   BlockData parseBlockData(String s) {
      if (s != null) {
         if (s.startsWith("LEGACY_")) {
            Material m = Material.matchMaterial(s.split(":")[0].replace("LEGACY_", ""), true);
            if (m != null) return m.createBlockData();
         }
         try { return Bukkit.createBlockData(s); } catch (Exception e) { return Material.AIR.createBlockData(); }
      }
      return Material.AIR.createBlockData();
   }

   public void killallchests() {
      for (Object o : new ArrayList<>(Main.pl.RandomChests.entrySet()))
         deleteChest((Location) ((java.util.Map.Entry) o).getKey());
   }

   @EventHandler
   public void onBlockClick(PlayerInteractEvent e) {
      Player player = e.getPlayer();
      if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
      Location location = Objects.requireNonNull(e.getClickedBlock()).getLocation();

      if (RLCUtils.isFixedChestType(e.getClickedBlock()) && Main.pl.addChestplayers.containsKey(player)) {
         WaitChooseChest wcc = Main.pl.addChestplayers.remove(player); wcc.cancel();
         if (!isChest(location)) {
            String cmd = wcc.getCommand(); Inventory inv;
              if ("add".equals(cmd) && !Main.pl.FixedChests.containsKey(location)) {
                 BlockState bs = location.getBlock().getState();
                 inv = RLCUtils.getInventory(bs);
                 if (inv != null) {
                    inv.clear();
                    // Fill lần đầu theo chest type đã chọn (thay vì pool global)
                    OpenLootInventory.fillInvenory(inv, ChestType.getChestType(wcc.getChestTypeName()));
                    String ctName = wcc.getChestTypeName();
                    Main.pl.FixedChests.put(location, new FixedChestInfo(FindAvaliableLocation.getRandom(1,
                            FindAvaliableLocation.getRandom(1, Math.max(FindAvaliableLocation.getRandom(
                                    Main.pl.getFixedChestUpdateTimeMin(), Main.pl.getFixedChestUpdateTimeMax()), 0))),
                            ctName));
                  if (Main.pl.getFixedChestSound() != null) Main.pl.getFixedChestSound().play(location);
                  if (Main.pl.getFixedChestEffect() != null) Main.pl.getFixedChestEffect().play(location);
                  MessageUtil.send(player, Main.pl.messages.get("gui.fixed_added", "<green>rương cố định đã được thêm vào bộ sưu tập <white>=)"));
               } else MessageUtil.send(player, Main.pl.messages.get("gui.fixed_add_error", "<red>Có gì đó không ổn <white>=("));
            } else if ("del".equals(cmd) && Main.pl.FixedChests.containsKey(location)) {
               Main.pl.FixedChests.remove(location);
               if (RLCUtils.isFixedChestType(location)) {
                  inv = RLCUtils.getInventory(location);
                  if (inv != null) inv.clear();
                  if (Main.pl.getFixedChestSound() != null) Main.pl.getFixedChestSound().play(location);
                  if (Main.pl.getFixedChestEffect() != null) Main.pl.getFixedChestEffect().play(location);
               }
               MessageUtil.send(player, Main.pl.messages.get("gui.fixed_removed", "<green>rương cố định đã bị xóa khỏi bộ sưu tập <white>=)"));
            }
         }
         e.setCancelled(true);
      } else if (RLCUtils.isRandomChestType(e.getClickedBlock()) && isChest(location)) {
         RandomChestInfo ci = Main.pl.RandomChests.get(location);
         ChestType ct = ChestType.getChestType(ci != null ? ci.ChestTypeId : "default");
         if (ct != null) ct.playOpenSound(location);
         else if (Main.pl.getRandomChestOpenSound() != null) Main.pl.getRandomChestOpenSound().play(location);
         olv.openInvenory(player, ct);
         if (Main.pl.getCommandsOnLoot() != null)
            for (String cmd : Main.pl.getCommandsOnLoot())
               Main.pl.getServer().dispatchCommand(Main.pl.getServer().getConsoleSender(), cmd.replace("{player}", player.getName()));
         deleteChest(location); e.setCancelled(true);
         if (Main.pl.getMessageOnLoot() != null) {
            Location l = e.getClickedBlock().getLocation();
            MessageUtil.broadcastWithPlaceholders(
                    Main.pl.getMessageOnLoot(),
                    "{X}", String.valueOf(l.getBlockX()),
                    "{Y}", String.valueOf(l.getBlockY()),
                    "{Z}", String.valueOf(l.getBlockZ()),
                    "{Player}", player.getName());
         }
      }
   }

   @EventHandler
   public void onChestBreak(BlockBreakEvent e) {
      Block block = e.getBlock(); Location loc = block.getLocation();
      if (RLCUtils.isRandomChestType(block) && isChest(loc)) {
         if (!Main.pl.abletobreak.contains(e.getPlayer())) {
            e.setCancelled(true);
            MessageUtil.send(e.getPlayer(), Main.pl.messages.get("chest.cannot_break", "<red>Bạn không thể phá rương loot!"));
         } else { e.setCancelled(true); deleteChest(loc); MessageUtil.send(e.getPlayer(), Main.pl.messages.get("chest.broke_random", "<green>Bạn đã phá được 1 hòm thính")); }
      } else if (RLCUtils.isFixedChestType(block) && Main.pl.FixedChests.containsKey(loc)) {
         if (!Main.pl.abletobreak.contains(e.getPlayer())) {
            e.setCancelled(true);
            MessageUtil.send(e.getPlayer(), Main.pl.messages.get("chest.cannot_break", "<red>Bạn không thể phá rương loot!"));
         } else { e.setCancelled(true); Main.pl.FixedChests.remove(loc);
            Inventory iv = RLCUtils.getInventory(block);
            if (iv != null) iv.clear();
            MessageUtil.send(e.getPlayer(), Main.pl.messages.get("chest.broke_fixed", "<green>rương cố định đã bị xóa khỏi bộ sưu tập")); }
      }
   }
}