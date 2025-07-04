package me.Herzchen.RandomLootChest;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class Main extends JavaPlugin implements Listener {
   static Main pl;
   static GenerateChest gc = new GenerateChest();
   Database db;
   LoadChances lc;
   Timer timer;
   ArrayList<String> commands;
   ArrayList<Player> abletobreak;
   ArrayList<Player> additem;
   static HashMap<Integer, ItemStack> items = new HashMap();
   HashMap<Integer, ItemStack> itemstoadd;
   HashMap<Integer, Integer> chances;
   HashMap<Player, Integer> currentpage;
   HashMap<Player, Integer> idediting;
   HashMap<Player, Integer> lastpageno;
   HashMap<Player, CommandManager.WaitChooseChest> addChestplayers = new HashMap();
   HashMap<Location, Integer> FixedChests = new HashMap();
   private int SpawnChestPerTime;
   private int KillChestAfterTime;
   int FixedChestUpdateTimeMin;
   int FixedChestUpdateTimeMax;
   EffectWrapper RandomChestEffect;
   SoundWrapper RandomChestSound;
   SoundWrapper RandomChestOpenSound;
   EffectWrapper FixedChestEffect;
   SoundWrapper FixedChestSound;
   String MessageOnSpawn;
   String MessageOnLoot;
   String MessageOnKill;
   boolean PluginenabledEnabled;
   Main.MaterialCondition SpawnBlockCondition_Positive = new MaterialCondition();
   Main.MaterialCondition SpawnBlockCondition_Negative = new MaterialCondition();
   Main.MaterialCondition UnderBlockCondition_Positive = new MaterialCondition();
   Main.MaterialCondition UnderBlockCondition_Negative = new MaterialCondition();
   Main.MaterialCondition SideBlockCondition_Positive = new MaterialCondition();
   Main.MaterialCondition SideBlockCondition_Negative = new MaterialCondition();
   HashMap<Location, RandomChestInfo> RandomChests = new HashMap();
   private static final Set<String> fixedChestMaterials = new HashSet(Arrays.asList("CHEST", "TRAPPED_CHEST", "WHITE_SHULKER_BOX", "ORANGE_SHULKER_BOX", "MAGENTA_SHULKER_BOX", "LIGHT_BLUE_SHULKER_BOX", "YELLOW_SHULKER_BOX", "LIME_SHULKER_BOX", "PINK_SHULKER_BOX", "GRAY_SHULKER_BOX", "SILVER_SHULKER_BOX", "LIGHT_GRAY_SHULKER_BOX", "CYAN_SHULKER_BOX", "SHULKER_BOX", "PURPLE_SHULKER_BOX", "BLUE_SHULKER_BOX", "BROWN_SHULKER_BOX", "GREEN_SHULKER_BOX", "RED_SHULKER_BOX", "BLACK_SHULKER_BOX"));
   private static Method getTypeMethod = null;
   public static final Material RANDOM_CHEST_TYPE;
   private static Map<Class, Method> getInventoryMethods;

   public Main() {
      pl = this;
      this.db = Database.instance;
      this.lc = LoadChances.instance;
      this.timer = Timer.instance;
      this.commands = new ArrayList();
      this.abletobreak = new ArrayList();
      this.additem = new ArrayList();
      this.itemstoadd = new HashMap();
      this.chances = new HashMap();
      this.currentpage = new HashMap();
      this.idediting = new HashMap();
      this.lastpageno = new HashMap();
   }

   static boolean isFixedChestType(Block block) {
      if (getTypeMethod == null) {
         try {
            getTypeMethod = Block.class.getMethod("getType");
         } catch (NoSuchMethodException var3) {
            throw new RuntimeException("Phương thức Block.getType không thể tìm thấy", var3);
         }
      }

      try {
         return fixedChestMaterials.contains(((Enum)getTypeMethod.invoke(block)).name());
      } catch (InvocationTargetException | IllegalAccessException var2) {
         throw new RuntimeException("Gọi Block.getType gây ra lỗi", var2);
      }
   }

   static boolean isFixedChestType(Location location) {
      return isFixedChestType(location.getBlock());
   }

   private static boolean isRandomChestType(Material material) {
      return RANDOM_CHEST_TYPE.equals(material);
   }

   public static class Selection {
      public Location pos1;
      public Location pos2;
   }

   public static HashMap<UUID, Selection> selections = new HashMap<>();

   static boolean isRandomChestType(Block block) {
      return isRandomChestType(block.getType());
   }

   static boolean isRandomChestType(Location location) {
      return isRandomChestType(location.getBlock());
   }

   public static class WandListener implements Listener {
      @EventHandler
      public void onPlayerInteract(PlayerInteractEvent e) {
         Player player = e.getPlayer();
         ItemStack item = player.getInventory().getItemInMainHand();

         if (!item.hasItemMeta() ||
                 !item.getItemMeta().getDisplayName().equals("§6RLC Wand")) {
            return;
         }

         if (!player.hasPermission("randomlootchest.select")) {
            player.sendMessage("§cKhông đủ quyền!");
            return;
         }

         Block clickedBlock = e.getClickedBlock();
         if (clickedBlock == null) return;

         if (Main.isFixedChestType(clickedBlock)) {
            Location loc = clickedBlock.getLocation();
            if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
               Main.pl.FixedChests.put(loc,
                       FindAvaliableLocation.getRandom(
                               Main.pl.FixedChestUpdateTimeMin,
                               Main.pl.FixedChestUpdateTimeMax
                       )
               );
               player.sendMessage("§aĐã set rương thành RLC chest!");
            }
            else if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
               Main.pl.FixedChests.remove(loc);
               player.sendMessage("§aĐã unset rương!");
            }
            e.setCancelled(true);
            return;
         }

         Selection sel = Main.selections.getOrDefault(
                 player.getUniqueId(),
                 new Selection()
         );

         if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
            sel.pos1 = clickedBlock.getLocation();
            player.sendMessage("§aĐã đặt pos1 tại: " + formatLocation(sel.pos1));
         }
         else if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            sel.pos2 = clickedBlock.getLocation();
            player.sendMessage("§aĐã đặt pos2 tại: " + formatLocation(sel.pos2));
         }

         Main.selections.put(player.getUniqueId(), sel);
         e.setCancelled(true);
      }

      private String formatLocation(Location loc) {
         return String.format("X: %d, Y: %d, Z: %d",
                 loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
      }
   }

   public static class QuitListener implements Listener {
      @EventHandler
      public void onPlayerQuit(PlayerQuitEvent e) {
         selections.remove(e.getPlayer().getUniqueId());
      }
   }

   private SoundWrapper findSound(String str, SoundWrapper defaultValue, boolean showError) {
      return SoundWrapper.createNotNull(str, defaultValue, (s) -> {
         if (showError) {
            this.getLogger().log(Level.WARNING, "Không tìm thấy âm thanh ''{0}''", str);
         }

         return defaultValue;
      });
   }

   private EffectWrapper findEffect(String str, EffectWrapper defaultValue, boolean showError) {
      return EffectWrapper.createNotNull(str, defaultValue, (s) -> {
         if (showError) {
            this.getLogger().log(Level.WARNING, "Không tìm thấy hiệu ứng ''{0}''", str);
         }

         return defaultValue;
      });
   }

   private void backConfig() {
      try {
         Files.copy((new File(this.getDataFolder(), "config.yml")).toPath(), (new File(this.getDataFolder().getPath(), "config_" + System.currentTimeMillis() + ".yml")).toPath(), StandardCopyOption.REPLACE_EXISTING);
      } catch (IOException var2) {
         throw new RuntimeException(var2.getMessage());
      }
   }

   private boolean checkConfig() {
      FileConfiguration cfg = this.getConfig();
      Configuration org = cfg.getDefaults();
      Set<String> cfgKeys = cfg.getKeys(true);
      Set<String> orgKeys = Objects.requireNonNull(org).getKeys(true);
      if (!orgKeys.stream().anyMatch((x) -> {
         return !cfgKeys.contains(x);
      }) && !cfgKeys.stream().anyMatch((x) -> {
         return !orgKeys.contains(x);
      })) {
         return true;
      } else {
         InputStream in = this.getResource("config.yml");
         File outFile = new File(this.getDataFolder(), "config_example.yml");
         FileOutputStream out;

         try {
            out = new FileOutputStream(outFile);
            byte[] buf = new byte[1024];

            int len;
            while((len = Objects.requireNonNull(in).read(buf)) > 0) {
               out.write(buf, 0, len);
            }

            out.close();
            in.close();
            return false;
         } catch (IOException var10) {
            throw new RuntimeException("Lỗi khi lưu tệp cấu hình mặc định", var10);
         }
      }
   }

   private void pause(int seconds) {
      this.getServer().getConsoleSender().sendMessage(String.format("§fTạm dừng %d giây ...", seconds));

      try {
         TimeUnit.SECONDS.sleep((long)seconds);
      } catch (InterruptedException var3) {
      }

   }

   private void saveLegalConstants(String fileName, String enumName) {
      Class cls;
      try {
         cls = Class.forName(enumName);
      } catch (Exception var8) {
         return;
      }

      File outFile = new File(this.getDataFolder(), fileName);
      if (!outFile.exists()) {
         List<String> list = Arrays.stream((Enum[]) cls.getEnumConstants()).map(Enum::name).collect(Collectors.toList());
         list.sort(String::compareTo);

         try {
            FileWriter out;
            out = new FileWriter(outFile);
            out.write(String.join(System.lineSeparator(), list));
            out.close();
         } catch (IOException var7) {
            throw new RuntimeException("Lỗi khi lưu " + fileName, var7);
         }
      }

   }

   public void onEnable() {
      this.saveDefaultConfig();
      this.saveLegalConstants("legal_effects.txt", "org.bukkit.Effect");
      this.saveLegalConstants("legal_sounds.txt", "org.bukkit.Sound");
      this.saveLegalConstants("legal_particles.txt", "org.bukkit.Particle");
      if (!this.checkConfig()) {
         this.getServer().getConsoleSender().sendMessage("§c============= §bRandomLootChest CẢNH BÁO §c===============");
         this.getServer().getConsoleSender().sendMessage("§c==§e                                                 §c==");
         this.getServer().getConsoleSender().sendMessage("§c==§e     THIẾU HOẶC THỪA TÙY CHỌN TRONG CONFIG.YML      §c==");
         this.getServer().getConsoleSender().sendMessage("§c==§e VUI LÒNG CHUYỂN TÙY CHỌN TỪ CONFIG_EXAMPLE.YML §c==");
         this.getServer().getConsoleSender().sendMessage("§c==§e                                                 §c==");
         this.getServer().getConsoleSender().sendMessage("§c========================§c§c=============================");
         this.pause(5);
         return;
      } else if (!this.getConfig().getBoolean("EnablePlugin")) {
         this.getServer().getConsoleSender().sendMessage("§c================= §bRandomLootChest §c===================");
         this.getServer().getConsoleSender().sendMessage("§c==§e                                                 §c==");
         this.getServer().getConsoleSender().sendMessage("§c==§e               PLUGIN ĐÃ BỊ TẮT                  §c==");
         this.getServer().getConsoleSender().sendMessage("§c==§e     BẠN CẦN CẤU HÌNH NÓ. XEM CONFIG.YML    §c==");
         this.getServer().getConsoleSender().sendMessage("§c==§e                                                 §c==");
         this.getServer().getConsoleSender().sendMessage("§c========================§c§c=============================");
         this.pause(5);
         return;
      } else {
         this.SpawnBlockCondition_Positive = new MaterialCondition(this.getConfig().getString("SpawnBlockCondition"), false);
         this.SpawnBlockCondition_Negative = new MaterialCondition(this.getConfig().getString("SpawnBlockCondition"), true);
         this.UnderBlockCondition_Positive = new MaterialCondition(this.getConfig().getString("UnderBlockCondition"), false);
         this.UnderBlockCondition_Negative = new MaterialCondition(this.getConfig().getString("UnderBlockCondition"), true);
         this.SideBlockCondition_Positive = new MaterialCondition(this.getConfig().getString("SideBlockCondition"), false);
         this.SideBlockCondition_Negative = new MaterialCondition(this.getConfig().getString("SideBlockCondition"), true);
         this.MessageOnSpawn = this.getConfig().getString("MessageOnSpawn");
         this.MessageOnLoot = this.getConfig().getString("MessageOnLoot");
         this.MessageOnKill = this.getConfig().getString("MessageOnKill");
         if (this.MessageOnSpawn != null && this.MessageOnSpawn.trim().isEmpty()) {
            this.MessageOnSpawn = null;
         }

         if (this.MessageOnLoot != null && this.MessageOnLoot.trim().isEmpty()) {
            this.MessageOnLoot = null;
         }

         if (this.MessageOnKill != null && this.MessageOnKill.trim().isEmpty()) {
            this.MessageOnKill = null;
         }

         this.RandomChestEffect = this.findEffect("MOBSPAWNER_FLAMES", null, false);
         this.RandomChestSound = this.findSound("NONE", null, false);
         this.RandomChestOpenSound = this.findSound("CHEST_OPEN|BLOCK_CHEST_OPEN", null, false);
         this.FixedChestEffect = this.findEffect("EXPLOSION", null, false);
         this.FixedChestSound = this.findSound("DIG_GRASS|BLOCK_GRASS_BREAK", null, false);
         this.RandomChestEffect = this.findEffect(this.getConfig().getString("RandomChestEffect"), this.RandomChestEffect, true);
         this.RandomChestSound = this.findSound(this.getConfig().getString("RandomChestSound"), this.RandomChestSound, true);
         this.RandomChestOpenSound = this.findSound(this.getConfig().getString("RandomChestOpenSound"), this.RandomChestSound, true);
         this.FixedChestEffect = this.findEffect(this.getConfig().getString("FixedChestEffect"), this.FixedChestEffect, true);
         this.FixedChestSound = this.findSound(this.getConfig().getString("FixedChestSound"), this.FixedChestSound, true);
         this.db.setup(this);
         this.db.data.options().copyDefaults(true);
         this.SpawnChestPerTime = this.getConfig().getInt("SpawnChestPerTime");
         this.FixedChestUpdateTimeMin = this.getConfig().getInt("FixedChestUpdateTimeMin", 3600);
         this.FixedChestUpdateTimeMax = this.getConfig().getInt("FixedChestUpdateTimeMax", 3600);
         this.KillChestAfterTime = pl.getConfig().getInt("KillChestAfterTime");
         if (!this.db.data.isConfigurationSection("Chests")) {
            this.db.data.createSection("Chests");
            this.db.saveData();
         }

         if (!this.db.data.isConfigurationSection("ItemDatabase")) {
            this.db.data.createSection("ItemDatabase");
            this.db.saveData();
         }

         if (!this.db.data.isConfigurationSection("LocationDatabase")) {
            this.db.data.createSection("LocationDatabase");
            this.db.saveData();
         }

         Objects.requireNonNull(this.getCommand("rlc")).setExecutor(new CommandManager());
         Objects.requireNonNull(this.getCommand("rlc")).setTabCompleter(new TabComplete());
         this.getServer().getPluginManager().registerEvents(new LootEvent(), this);
         this.getServer().getPluginManager().registerEvents(new ItemAdderGui(), this);

         this.getServer().getPluginManager().registerEvents(new WandListener(), this);
         this.getServer().getPluginManager().registerEvents(new QuitListener(), this);

         this.lc.loaditems();
         if (this.getConfig().getList("CommandsToExecuteOnLoot") != null) {
            this.commands = (ArrayList)this.getConfig().getStringList("CommandsToExecuteOnLoot");
         }

         if (!this.timer.loadChests()) {
            this.getServer().getConsoleSender().sendMessage("§c============== §bRandomLootChest LỖI §c================");
            this.getServer().getConsoleSender().sendMessage("§c==§e                                                 §c==");
            this.getServer().getConsoleSender().sendMessage("§c==§e         LỖI KHI ĐỌC CƠ SỞ DỮ LIỆU            §c==");
            this.getServer().getConsoleSender().sendMessage("§c==§e        SỬA HOẶC XÓA DATABASE.YML           §c==");
            this.getServer().getConsoleSender().sendMessage("§c==§e                                                 §c==");
            this.getServer().getConsoleSender().sendMessage("§c========================§c§c=============================");
            this.pause(5);
            return;
         } else {
            FindAvaliableLocation.init();
            gc.GenerateChest(this.SpawnChestPerTime);
            if (this.RandomChestEffect != null) {
               this.Particles();
            }

            if (this.getConfig().getBoolean("KillChest")) {
               this.timer.decrease();
            }

            this.PluginenabledEnabled = true;

            this.getServer().getConsoleSender().sendMessage("§c==================== §6RandomLootChest §c===================");
            this.getServer().getConsoleSender().sendMessage("§a==§f                                                §a==§f==");
            this.getServer().getConsoleSender().sendMessage("§a==§f                  PLUGIN ĐÃ ĐƯỢC BẬT            §a==§f==");
            this.getServer().getConsoleSender().sendMessage("§a==§f            MỌI VẤN ĐỀ XIN LIÊN HỆ DISCORD      §a==§f==");
            this.getServer().getConsoleSender().sendMessage("§a==§f                   itztli_herzchen              §a==§f==");
            this.getServer().getConsoleSender().sendMessage("§a==§f                                                §a==§f==");
            this.getServer().getConsoleSender().sendMessage("§c============================§c§c============================");
         }
      }

      int notificationInterval = this.getConfig().getInt("NotificationInterval", 300);

      Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
         if (!RandomChests.isEmpty()) {
            Entry<Location, RandomChestInfo> randomEntry = RandomChests.entrySet().iterator().next();
            Location location = randomEntry.getKey();

            String message = MessageOnSpawn
                    .replace("{X}", String.valueOf(location.getBlockX()))
                    .replace("{Y}", String.valueOf(location.getBlockY()))
                    .replace("{Z}", String.valueOf(location.getBlockZ()));

            for (Player player : Bukkit.getOnlinePlayers()) {
               player.sendMessage(message);
            }
         }
      }, 0L, notificationInterval * 20L);
   }

   public void onDisable() {
      if (this.PluginenabledEnabled) {
         this.timer.saveChests();
      }
   }

   private void Particles() {
      Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
         Iterator var1 = this.RandomChests.entrySet().iterator();

         while(var1.hasNext()) {
            Entry<Location, RandomChestInfo> e = (Entry)var1.next();
            Location location = (Location)e.getKey();
            this.RandomChestEffect.play(location);
         }

      }, 20L, 20L);
   }

   void randomizeRandomChestsTimeLeft() {
      int t = Integer.max(pl.getConfig().getInt("KillChestAfterTime"), 0);

      Entry e;
      for(Iterator var2 = this.RandomChests.entrySet().iterator(); var2.hasNext(); ((RandomChestInfo)e.getValue()).Time = FindAvaliableLocation.getRandom(0, t)) {
         e = (Entry)var2.next();
      }

   }

   void randomizeFixedChestsTimeLeft() {
      int t = Integer.max(this.FixedChestUpdateTimeMax, 0);
      Iterator var2 = this.FixedChests.entrySet().iterator();

      while(var2.hasNext()) {
         Entry<Location, Integer> e = (Entry)var2.next();
         e.setValue(FindAvaliableLocation.getRandom(0, t));
      }

   }

   boolean checkSide(Material material) {
      return this.SideBlockCondition_Positive.isMatch(material) && this.SideBlockCondition_Negative.isMatch(material) && !isRandomChestType(material);
   }

   boolean canSpawnChest(Location location) {
      Block block = location.getBlock();
      if (block instanceof BlockState) {
         return false;
      } else {
         Material material = block.getType();
         if (!this.SpawnBlockCondition_Positive.isMatch(material)) {
            return false;
         } else if (!this.SpawnBlockCondition_Negative.isMatch(material)) {
            return false;
         } else {
            location = location.clone().add(0.0D, -1.0D, 0.0D);
            Block block0 = location.getBlock();
            material = block0.getType();
            if (!this.UnderBlockCondition_Positive.isMatch(material)) {
               return false;
            } else if (!this.UnderBlockCondition_Negative.isMatch(material)) {
               return false;
            } else {
               return this.checkSide(location.add(1.0D, 1.0D, 0.0D).getBlock().getType()) && this.checkSide(location.add(-1.0D, 0.0D, 1.0D).getBlock().getType()) && this.checkSide(location.add(-1.0D, 0.0D, -1.0D).getBlock().getType()) && this.checkSide(location.add(1.0D, 0.0D, -1.0D).getBlock().getType());
            }
         }
      }
   }

   public static World getWorld(String worldName) {
      World world = Bukkit.getServer().getWorld(worldName);
      if (world == null) {
         pl.getServer().getConsoleSender().sendMessage(String.format("[%s] §cKhông tìm thấy thế giới '%s'.", pl.getDescription().getName(), worldName));
      }

      return world;
   }

   @Nullable
   static Inventory getInventory(BlockState blockState) {
      if (blockState == null) {
         return null;
      } else {
         Class<? extends BlockState> cls = blockState.getClass();
         Method method = getInventoryMethods.get(cls);
         if (method == null) {
            if (!getInventoryMethods.containsKey(cls)) {
               try {
                  method = cls.getMethod("getInventory");
               } catch (NoSuchMethodException var6) {
                  getInventoryMethods.put(cls, null);
                  return null;
               }

               try {
                  Inventory inventory = (Inventory)method.invoke(blockState);
                  getInventoryMethods.put(cls, method);
                  return inventory;
               } catch (InvocationTargetException | IllegalAccessException var5) {
                  getInventoryMethods.put(cls, null);
                  return null;
               }
            } else {
               return null;
            }
         } else {
            try {
               return (Inventory)method.invoke(blockState);
            } catch (InvocationTargetException | IllegalAccessException var7) {
               return null;
            }
         }
      }
   }

   @Nullable
   static Inventory getInventory(Block block) {
      return getInventory(block.getState());
   }

   @Nullable
   static Inventory getInventory(Location location) {
      return getInventory(location.getBlock());
   }

   static {
      RANDOM_CHEST_TYPE = Material.CHEST;
      getInventoryMethods = new HashMap();
   }

   private static class MaterialCondition {
      private boolean empty = true;
      private boolean negative;
      private boolean Fuel;
      private boolean Record;
      private boolean Occluding;
      private boolean Transparent;
      private boolean Block;
      private boolean Burnable;
      private boolean Edible;
      private boolean Flammable;
      private boolean Solid;
      private boolean Gravity;
      public HashSet<Material> Materials = new HashSet();

      MaterialCondition() {
      }

      MaterialCondition(String txt, boolean negative) {
         if (txt != null) {
            this.negative = negative;
            HashMap<String, Material> allMaterial = new HashMap(Arrays.stream(Material.values()).collect(Collectors.toMap((x) -> x.name().toUpperCase(), (x) -> x)));
            String[] var5 = txt.split("[\\s;,]+");
            int var6 = var5.length;

            for(int var7 = 0; var7 < var6; ++var7) {
               String word = var5[var7];
               boolean isWordNegative = word.startsWith("!");
               if (isWordNegative == negative) {
                  this.empty = false;
                  String upperWord = (isWordNegative ? word.substring(1) : word).toUpperCase();
                  byte var12 = -1;
                  switch(upperWord.hashCode()) {
                     case -1758617780:
                        if (upperWord.equals("_TRANSPARENT_")) {
                           var12 = 3;
                        }
                        break;
                     case -1507950486:
                        if (upperWord.equals("_FUEL_")) {
                           var12 = 0;
                        }
                        break;
                     case -650799017:
                        if (upperWord.equals("_BURNABLE_")) {
                           var12 = 5;
                        }
                        break;
                     case -506939991:
                        if (upperWord.equals("_FLAMMABLE_")) {
                           var12 = 7;
                        }
                        break;
                     case -358209272:
                        if (upperWord.equals("_OCCLUDING_")) {
                           var12 = 2;
                        }
                        break;
                     case -136163089:
                        if (upperWord.equals("_RECORD_")) {
                           var12 = 1;
                        }
                        break;
                     case 375635633:
                        if (upperWord.equals("_BLOCK_")) {
                           var12 = 4;
                        }
                        break;
                     case 865017939:
                        if (upperWord.equals("_SOLID_")) {
                           var12 = 8;
                        }
                        break;
                     case 979433808:
                        if (upperWord.equals("_GRAVITY_")) {
                           var12 = 9;
                        }
                        break;
                     case 1187709903:
                        if (upperWord.equals("_EDIBLE_")) {
                           var12 = 6;
                        }
                  }

                  switch(var12) {
                     case 0:
                        this.Fuel = true;
                        break;
                     case 1:
                        this.Record = true;
                        break;
                     case 2:
                        this.Occluding = true;
                        break;
                     case 3:
                        this.Transparent = true;
                        break;
                     case 4:
                        this.Block = true;
                        break;
                     case 5:
                        this.Burnable = true;
                        break;
                     case 6:
                        this.Edible = true;
                        break;
                     case 7:
                        this.Flammable = true;
                        break;
                     case 8:
                        this.Solid = true;
                        break;
                     case 9:
                        this.Gravity = true;
                        break;
                     default:
                        Material m = allMaterial.get(upperWord);
                        if (m != null) {
                           this.Materials.add(m);
                        } else {
                           Main.pl.getServer().getConsoleSender().sendMessage(String.format("§ccảnh báo: Vật liệu không xác định '§e%s§c' trong điều kiện sẽ bị bỏ qua. Kiểm tra config.yml của bạn", upperWord));
                        }
                  }
               }
            }

         }
      }

      boolean isMatch(Material material) {
         return this.empty || this.negative != (this.Fuel && material.isFuel() || this.Record && material.isRecord() || this.Occluding && material.isOccluding() || this.Transparent && material.isTransparent() || this.Block && material.isBlock() || this.Burnable && material.isBurnable() || this.Edible && material.isEdible() || this.Flammable && material.isFlammable() || this.Solid && material.isSolid() || this.Gravity && material.hasGravity() || this.Materials.contains(material));
      }
   }
}