package me.Herzchen.RandomLootChest;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Iterator;
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
         } else if (args.length == 1) {
            if (args[0].equalsIgnoreCase("togglebreak")) {
               if (!(sender instanceof Player)) {
                  this.sendconsolehelp((ConsoleCommandSender)sender);
                  return false;
               }

               player = (Player)sender;
               if (!player.hasPermission("randomlootchest.togglebreak")) {
                  player.sendMessage("§cKhông đủ quyền hạn.");
                  return false;
               }

               if (Main.pl.abletobreak.contains(player)) {
                  Main.pl.abletobreak.remove(player);
                  player.sendMessage("§6Đã tắt chế độ phá rương §cTắt!");
               } else {
                  Main.pl.abletobreak.add(player);
                  player.sendMessage("§6Đã bật chế độ phá rương §aBật!");
               }
            } else if (args[0].equalsIgnoreCase("killall")) {
               if (sender instanceof Player && !sender.hasPermission("randomlootchest.killall")) {
                  sender.sendMessage("§cKhông đủ quyền hạn.");
                  return false;
               }

               this.le.killallchests();
               sender.sendMessage("§aĐã xóa tất cả rương thành công!");
            } else if (args[0].equalsIgnoreCase("forcespawn")) {
               if (!sender.hasPermission("randomlootchest.forcespawn")) {
                  sender.sendMessage("§cKhông đủ quyền hạn.");
                  return false;
               }

               this.gc.spawnchest();
            } else if (args[0].equalsIgnoreCase("rndtime")) {
               if (!sender.hasPermission("randomlootchest.rndtime")) {
                  sender.sendMessage("§cKhông đủ quyền hạn.");
                  return false;
               }

               Main.pl.randomizeRandomChestsTimeLeft();
               Main.pl.randomizeFixedChestsTimeLeft();
               sender.sendMessage("§eĐã ngẫu nhiên hóa thời gian thành công.");
            } else if (args[0].equalsIgnoreCase("addchest")) {
               if (!sender.hasPermission("randomlootchest.fixedchest")) {
                  sender.sendMessage("§cKhông đủ quyền hạn.");
                  return false;
               }

               player = (Player)sender;
               (new WaitChooseChest()).start(player, "add");
            } else if (args[0].equalsIgnoreCase("delchest")) {
               if (!sender.hasPermission("randomlootchest.fixedchest")) {
                  sender.sendMessage("§cKhông đủ quyền hạn.");
                  return false;
               }

               player = (Player)sender;
               (new WaitChooseChest()).start(player, "del");
            } else if (args[0].equalsIgnoreCase("delall")) {
               if (sender instanceof Player && !sender.hasPermission("randomlootchest.delall")) {
                  sender.sendMessage("§cKhông đủ quyền hạn.");
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
            }
         }
      }

      if (args.length >= 1 && args[0].equalsIgnoreCase("additem")) {
         if (!(sender instanceof Player)) {
            player1 = (ConsoleCommandSender)sender;
            this.sendconsolehelp(player1);
            return false;
         }

         player = (Player)sender;
         if (!player.hasPermission("randomlootchest.additem")) {
            player.sendMessage("§cKhông đủ quyền hạn.");
            return false;
         }

         this.itg.openGui(player);
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