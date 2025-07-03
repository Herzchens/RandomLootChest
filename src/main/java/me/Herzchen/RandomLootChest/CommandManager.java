package me.Herzchen.RandomLootChest;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

public class CommandManager implements CommandExecutor {
   ItemAdderGui itg = new ItemAdderGui();
   GenerateChest gc = new GenerateChest();
   LoadChances lc;
   LootEvent le;

   public CommandManager() {
      this.lc = LoadChances.instance;
      this.le = new LootEvent();
   }

   public ConfigurationSection itdb() {
      return Main.pl.db.data.getConfigurationSection("ItemDatabase");
   }

   public void save() {
      Main.pl.db.saveData();
   }

   public void sendhelp(Player player) {
      player.sendMessage("§c*-----§4§lRandomLootChest§r§c*-----*");
      player.sendMessage("§c§lLệnh:");
      player.sendMessage("§c/rlc additem §6(Mở GUI thêm vật phẩm)");
      player.sendMessage("§c/rlc addchest §6(Thêm rương cố định bằng cách nhấn chuột phải)");
      player.sendMessage("§c/rlc delchest §6(Xóa rương cố định bằng cách nhấn chuột phải)");
      player.sendMessage("§c/rlc delall §6(Xóa tất cả rương cố định)");
      player.sendMessage("§c/rlc killall §6(Xóa tất cả rương ngẫu nhiên)");
      player.sendMessage("§c/rlc togglebreak §6(Bật/tắt chế độ phá rương)");
      player.sendMessage("§c/rlc forcespawn §6(Tạo rương ngẫu nhiên ngay lập tức)");
      player.sendMessage("§c/rlc rndtime §6(Ngẫu nhiên hóa thời gian tồn tại rương)");
      player.sendMessage("§c/rlc wand §6(Nhận que wand để chọn vùng)");
      player.sendMessage("§c/rlc set §6(Set tất cả rương trong vùng thành RLC chest)");
      player.sendMessage("§c/rlc unset §6(Unset tất cả RLC chest trong vùng)");
      player.sendMessage("§c*-----§4§lRandomLootChest§r§c*-----*");
   }

   public void sendconsolehelp(ConsoleCommandSender sender) {
      sender.sendMessage("§c*-----§4§lRandomLootChest§r§c*-----*");
      sender.sendMessage("§c§lLệnh Console:");
      sender.sendMessage("§c/rlc delall §6(Xóa tất cả rương cố định)");
      sender.sendMessage("§c/rlc killall §6(Xóa tất cả rương ngẫu nhiên)");
      sender.sendMessage("§c/rlc forcespawn §6(Tạo rương ngẫu nhiên ngay lập tức)");
      sender.sendMessage("§c/rlc rndtime §6(Ngẫu nhiên hóa thời gian tồn tại/cập nhật rương)");
      sender.sendMessage("§c*-----§4§lRandomLootChest§r§c*-----*");
   }

   public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
      boolean handled = false;

      if (Main.pl.addChestplayers.containsKey(sender)) {
         sender.sendMessage("§cĐã hủy §fo_O");
         ((CommandManager.WaitChooseChest)Main.pl.addChestplayers.get(sender)).cancel();
         Main.pl.addChestplayers.remove(sender);
      }

      Player player;
      ConsoleCommandSender player1;
      if (command.getName().equalsIgnoreCase("rlc")) {
         if (sender instanceof Player) {
            player = (Player)sender;
            if (!player.hasPermission("randomlootchest.general")) {
               player.sendMessage("§cKhông đủ quyền hạn.");
               return false;
            }
         }

         if (args.length == 0) {
            if (sender instanceof Player) {
               player = (Player)sender;
               this.sendhelp(player);
            } else {
               player1 = (ConsoleCommandSender)sender;
               this.sendconsolehelp(player1);
            }
            handled = true;
         } else if (args.length == 1) {
            if (args[0].equalsIgnoreCase("wand")) {
               if (!(sender instanceof Player)) {
                  sender.sendMessage("§cChỉ người chơi được dùng lệnh này");
                  handled = true;
                  return true;
               }
               player = (Player) sender;
               if (!player.hasPermission("randomlootchest.command.wand")) {
                  player.sendMessage("§cKhông đủ quyền hạn!");
                  handled = true;
                  return true;
               }

               ItemStack wand = new ItemStack(Material.STICK);
               ItemMeta meta = wand.getItemMeta();
               meta.setDisplayName("§6RLC Wand");
               List<String> lore = new ArrayList<>();
               lore.add("§7Chuột trái: Chọn pos1/Set RLC chest");
               lore.add("§7Chuột phải: Chọn pos2/Unset RLC chest");
               meta.setLore(lore);
               wand.setItemMeta(meta);

               player.getInventory().addItem(wand);
               player.sendMessage("§aBạn đã nhận được que wand!");
               handled = true;
               return true;
            }
            else if (args[0].equalsIgnoreCase("set")) {
               if (!(sender instanceof Player)) {
                  sender.sendMessage("§cChỉ người chơi được dùng lệnh này");
                  handled = true;
                  return true;
               }
               player = (Player) sender;

               if (!player.hasPermission("randomlootchest.command.set")) {
                  player.sendMessage("§cKhông đủ quyền hạn!");
                  handled = true;
                  return true;
               }

               Main.Selection sel = Main.selections.get(player.getUniqueId());
               if (sel == null || sel.pos1 == null || sel.pos2 == null) {
                  player.sendMessage("§cVui lòng chọn vùng bằng wand trước!");
                  handled = true;
                  return true;
               }

               int count = 0;
               int alreadySet = 0;
               for (int x = Math.min(sel.pos1.getBlockX(), sel.pos2.getBlockX());
                    x <= Math.max(sel.pos1.getBlockX(), sel.pos2.getBlockX()); x++) {
                  for (int y = Math.min(sel.pos1.getBlockY(), sel.pos2.getBlockY());
                       y <= Math.max(sel.pos1.getBlockY(), sel.pos2.getBlockY()); y++) {
                     for (int z = Math.min(sel.pos1.getBlockZ(), sel.pos2.getBlockZ());
                          z <= Math.max(sel.pos1.getBlockZ(), sel.pos2.getBlockZ()); z++) {

                        Location loc = new Location(sel.pos1.getWorld(), x, y, z);
                        Block block = loc.getBlock();

                        if (Main.isFixedChestType(block)) {
                           if (Main.pl.FixedChests.containsKey(loc)) {
                              alreadySet++;
                           } else {
                              Main.pl.FixedChests.put(loc,
                                      FindAvaliableLocation.getRandom(
                                              Main.pl.FixedChestUpdateTimeMin,
                                              Main.pl.FixedChestUpdateTimeMax
                                      )
                              );
                              count++;
                           }
                        }
                     }
                  }
               }

               if (count > 0) {
                  player.sendMessage("§aĐã set " + count + " rương thành RLC chest!");
                  if (alreadySet > 0) {
                     player.sendMessage("§e" + alreadySet + " rương đã là RLC chest từ trước!");
                  }
               } else if (alreadySet > 0) {
                  player.sendMessage("§eTất cả " + alreadySet + " rương trong vùng đã là RLC chest!");
               } else {
                  player.sendMessage("§eKhông tìm thấy rương nào trong vùng được chọn!");
               }
               handled = true;
               return true;
            }
            else if (args[0].equalsIgnoreCase("unset")) {
               if (!(sender instanceof Player)) {
                  sender.sendMessage("§cChỉ người chơi được dùng lệnh này");
                  handled = true;
                  return true;
               }
               player = (Player) sender;

               if (!player.hasPermission("randomlootchest.command.unset")) {
                  player.sendMessage("§cKhông đủ quyền hạn!");
                  handled = true;
                  return true;
               }

               Main.Selection sel = Main.selections.get(player.getUniqueId());
               if (sel == null || sel.pos1 == null || sel.pos2 == null) {
                  player.sendMessage("§cVui lòng chọn vùng bằng wand trước!");
                  handled = true;
                  return true;
               }

               int count = 0;
               int notSet = 0;
               for (int x = Math.min(sel.pos1.getBlockX(), sel.pos2.getBlockX());
                    x <= Math.max(sel.pos1.getBlockX(), sel.pos2.getBlockX()); x++) {
                  for (int y = Math.min(sel.pos1.getBlockY(), sel.pos2.getBlockY());
                       y <= Math.max(sel.pos1.getBlockY(), sel.pos2.getBlockY()); y++) {
                     for (int z = Math.min(sel.pos1.getBlockZ(), sel.pos2.getBlockZ());
                          z <= Math.max(sel.pos1.getBlockZ(), sel.pos2.getBlockZ()); z++) {

                        Location loc = new Location(sel.pos1.getWorld(), x, y, z);
                        Block block = loc.getBlock();

                        if (Main.isFixedChestType(block)) {
                           if (Main.pl.FixedChests.containsKey(loc)) {
                              Main.pl.FixedChests.remove(loc);
                              count++;
                           } else {
                              notSet++;
                           }
                        }
                     }
                  }
               }

               if (count > 0) {
                  player.sendMessage("§aĐã unset " + count + " RLC chest!");
                  if (notSet > 0) {
                     player.sendMessage("§e" + notSet + " rương không phải RLC chest!");
                  }
               } else if (notSet > 0) {
                  player.sendMessage("§eKhông có RLC chest nào trong vùng được chọn! (" + notSet + " rương thường)");
               } else {
                  player.sendMessage("§eKhông tìm thấy rương nào trong vùng được chọn!");
               }
               handled = true;
               return true;
            }
            else if (args[0].equalsIgnoreCase("togglebreak")) {
               if (!(sender instanceof Player)) {
                  this.sendconsolehelp((ConsoleCommandSender)sender);
                  handled = true;
                  return false;
               }

               player = (Player)sender;
               if (!player.hasPermission("randomlootchest.togglebreak")) {
                  player.sendMessage("§cKhông đủ quyền hạn.");
                  handled = true;
                  return false;
               }

               if (Main.pl.abletobreak.contains(player)) {
                  Main.pl.abletobreak.remove(player);
                  player.sendMessage("§6Đã tắt chế độ phá rương §cTắt!");
               } else {
                  Main.pl.abletobreak.add(player);
                  player.sendMessage("§6Đã bật chế độ phá rương §aBật!");
               }
               handled = true;
            } else if (args[0].equalsIgnoreCase("killall")) {
               if (sender instanceof Player && !sender.hasPermission("randomlootchest.killall")) {
                  sender.sendMessage("§cKhông đủ quyền hạn.");
                  handled = true;
                  return false;
               }

               this.le.killallchests();
               sender.sendMessage("§aĐã xóa tất cả rương thành công!");
               handled = true;
            } else if (args[0].equalsIgnoreCase("forcespawn")) {
               if (!sender.hasPermission("randomlootchest.forcespawn")) {
                  sender.sendMessage("§cKhông đủ quyền hạn.");
                  handled = true;
                  return false;
               }

               this.gc.spawnchest();
               handled = true;
            } else if (args[0].equalsIgnoreCase("rndtime")) {
               if (!sender.hasPermission("randomlootchest.rndtime")) {
                  sender.sendMessage("§cKhông đủ quyền hạn.");
                  handled = true;
                  return false;
               }

               Main.pl.randomizeRandomChestsTimeLeft();
               Main.pl.randomizeFixedChestsTimeLeft();
               sender.sendMessage("§eĐã ngẫu nhiên hóa thời gian thành công.");
               handled = true;
            } else if (args[0].equalsIgnoreCase("addchest")) {
               if (!sender.hasPermission("randomlootchest.fixedchest")) {
                  sender.sendMessage("§cKhông đủ quyền hạn.");
                  handled = true;
                  return false;
               }

               player = (Player)sender;
               (new WaitChooseChest()).start(player, "add");
               handled = true;
            } else if (args[0].equalsIgnoreCase("delchest")) {
               if (!sender.hasPermission("randomlootchest.fixedchest")) {
                  sender.sendMessage("§cKhông đủ quyền hạn.");
                  handled = true;
                  return false;
               }

               player = (Player)sender;
               (new WaitChooseChest()).start(player, "del");
               handled = true;
            } else if (args[0].equalsIgnoreCase("delall")) {
               if (sender instanceof Player && !sender.hasPermission("randomlootchest.delall")) {
                  sender.sendMessage("§cKhông đủ quyền hạn.");
                  handled = true;
                  return false;
               }

               Iterator var7 = Main.pl.FixedChests.entrySet().iterator();

               while(var7.hasNext()) {
                  Entry<Location, Integer> kv = (Entry)var7.next();
                  Location location = (Location)kv.getKey();
                  if (Main.isFixedChestType(location)) {
                     Inventory inventory = Main.getInventory(location);
                     if (inventory != null) {
                        inventory.clear();
                     }
                  }
               }

               Main.pl.FixedChests.clear();
               sender.sendMessage("§aĐã xóa toàn bộ rương cố định!");
               handled = true;
            }
         }
      }

      if (args.length >= 1 && args[0].equalsIgnoreCase("additem")) {
         if (!(sender instanceof Player)) {
            player1 = (ConsoleCommandSender)sender;
            this.sendconsolehelp(player1);
            handled = true;
            return false;
         }

         player = (Player)sender;
         if (!player.hasPermission("randomlootchest.additem")) {
            player.sendMessage("§cKhông đủ quyền hạn.");
            handled = true;
            return false;
         }

         this.itg.openGui(player);
         handled = true;
      }

      if (command.getName().equalsIgnoreCase("rlc") && !handled) {
         sender.sendMessage("§cLệnh không tồn tại! Sử dụng §f/rlc §cđể xem trợ giúp.");
         return true;
      }

      return false;
   }

   public static class WaitChooseChest extends BukkitRunnable {
      int Left = 4;
      Player Player;
      String Command;

      public void start(Player player, String command) {
         this.Command = command;
         this.Player = player;
         Main.pl.addChestplayers.put(this.Player, this);
         this.runTaskTimer(Main.pl, 20L, 20L);
      }

      public void run() {
         this.Player.sendMessage(String.format("§aBạn còn %d giây để nhấn chuột phải vào rương §f=O", this.Left));
         if (this.Left-- < 0) {
            this.Player.sendMessage("§cHết thời gian. Bạn đã quá chậm §f=(");
            Main.pl.addChestplayers.remove(this.Player);
            this.cancel();
         }

      }
   }
}