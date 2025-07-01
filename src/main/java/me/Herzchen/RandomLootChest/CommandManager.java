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
   int num = 0;
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
      player.sendMessage("§c§lCommands:");
      player.sendMessage("§c/rlc additem §6(Open the item sumbition Gui)");
      player.sendMessage("§c/rlc addchest §6(Add fixed chest to collection by right-click on it)");
      player.sendMessage("§c/rlc delchest §6(Delete fixed chest from collection by right-click on it)");
      player.sendMessage("§c/rlc delall §6(Delete all fixed chests from collection)");
      player.sendMessage("§c/rlc killall §6(Clear all random chests from the server)");
      player.sendMessage("§c/rlc togglebreak §6(Be able to clear a random chest by breaking it or delete fixed chest)");
      player.sendMessage("§c/rlc forcespawn §6(Forcespawn a random chest)");
      player.sendMessage("§c/rlc rndtime §6(Randomize time left to kill/update chests)");
      player.sendMessage("§c*-----§4§lRandomLootChest§r§c*-----*");
   }

   public void sendconsolehelp(ConsoleCommandSender sender) {
      sender.sendMessage("§c*-----§4§lRandomLootChest§r§c*-----*");
      sender.sendMessage("§c§lConsole Commands:");
      sender.sendMessage("§c/rlc delall §6(Delete all fixed chests from collection)");
      sender.sendMessage("§c/rlc killall §6(Clear all random chests from the server)");
      sender.sendMessage("§c/rlc forcespawn §6(Forcespawn a chest)");
      sender.sendMessage("§c/rlc rndtime §6(Randomize time left to kill (random) or update (fixed) chests)");
      sender.sendMessage("§c*-----§4§lRandomLootChest§r§c*-----*");
   }

   public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
      if (Main.pl.addChestplayers.containsKey(sender)) {
         sender.sendMessage("§cCanceled §fo_O");
         ((CommandManager.WaitChooseChest)Main.pl.addChestplayers.get(sender)).cancel();
         Main.pl.addChestplayers.remove(sender);
      }

      Player player;
      ConsoleCommandSender player1;
      if (command.getName().equalsIgnoreCase("rlc")) {
         if (sender instanceof Player) {
            player = (Player)sender;
            if (!player.hasPermission("randomlootchest.general")) {
               player.sendMessage("§cInsufficient permissions.");
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
                  player.sendMessage("§cInsufficient permissions.");
                  return false;
               }

               if (Main.pl.abletobreak.contains(player)) {
                  Main.pl.abletobreak.remove(player);
                  player.sendMessage("§6You toggled ChestBreak §cOff!");
               } else {
                  Main.pl.abletobreak.add(player);
                  player.sendMessage("§6You toggled ChestBreak §aOn!");
               }
            } else if (args[0].equalsIgnoreCase("killall")) {
               if (sender instanceof Player && !sender.hasPermission("randomlootchest.killall")) {
                  sender.sendMessage("§cInsufficient permissions.");
                  return false;
               }

               this.le.killallchests();
               sender.sendMessage("§aAll the chests have been successfully deleted!");
            } else if (args[0].equalsIgnoreCase("forcespawn")) {
               if (!sender.hasPermission("randomlootchest.forcespawn")) {
                  sender.sendMessage("§cInsufficient permissions.");
                  return false;
               }

               this.gc.spawnchest();
            } else if (args[0].equalsIgnoreCase("rndtime")) {
               if (!sender.hasPermission("randomlootchest.rndtime")) {
                  sender.sendMessage("§cInsufficient permissions.");
                  return false;
               }

               Main.pl.randomizeRandomChestsTimeLeft();
               Main.pl.randomizeFixedChestsTimeLeft();
               sender.sendMessage("§eRandomize time left completed.");
            } else if (args[0].equalsIgnoreCase("addchest")) {
               if (!sender.hasPermission("randomlootchest.fixedchest")) {
                  sender.sendMessage("§cInsufficient permissions.");
                  return false;
               }

               player = (Player)sender;
               (new CommandManager.WaitChooseChest()).start(player, "add");
            } else if (args[0].equalsIgnoreCase("delchest")) {
               if (!sender.hasPermission("randomlootchest.fixedchest")) {
                  sender.sendMessage("§cInsufficient permissions.");
                  return false;
               }

               player = (Player)sender;
               (new CommandManager.WaitChooseChest()).start(player, "del");
            } else if (args[0].equalsIgnoreCase("delall")) {
               if (sender instanceof Player && !sender.hasPermission("randomlootchest.delall")) {
                  sender.sendMessage("§cInsufficient permissions.");
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
               sender.sendMessage("§aAll fixed chests has been deleted from collection!");
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
            player.sendMessage("§cInsufficient permissions.");
            return false;
         }

         this.itg.openGui(player);
      }

      return false;
   }

   public class WaitChooseChest extends BukkitRunnable {
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
         this.Player.sendMessage(String.format("§aYou have %d seconds left to right click chest §f=O", this.Left));
         if (this.Left-- < 0) {
            this.Player.sendMessage("§cTime is over. You are late §f=(");
            Main.pl.addChestplayers.remove(this.Player);
            this.cancel();
         }

      }
   }
}
