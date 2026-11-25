package me.Herzchen.RandomLootChest;

import me.Herzchen.RandomLootChest.command.CommandManager;
import me.Herzchen.RandomLootChest.command.WaitChooseChest;
import me.Herzchen.RandomLootChest.config.ConfigManager;
import me.Herzchen.RandomLootChest.config.Messages;
import me.Herzchen.RandomLootChest.database.*;
import me.Herzchen.RandomLootChest.database.Timer;
import me.Herzchen.RandomLootChest.listener.*;
import me.Herzchen.RandomLootChest.model.FixedChestInfo;
import me.Herzchen.RandomLootChest.model.ItemEditSession;
import me.Herzchen.RandomLootChest.model.RandomChestInfo;
import me.Herzchen.RandomLootChest.generator.GenerateChest;
import me.Herzchen.RandomLootChest.region.RegionConfig;
import me.Herzchen.RandomLootChest.util.EffectWrapper;
import me.Herzchen.RandomLootChest.util.FindAvaliableLocation;
import me.Herzchen.RandomLootChest.util.MessageUtil;
import me.Herzchen.RandomLootChest.util.RLCUtils;
import me.Herzchen.RandomLootChest.util.SoundWrapper;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

public class Main extends JavaPlugin {
    public static Main pl;
    public Database db;
    public ConfigManager configManager;
    public Messages messages;
    public ChestTypeConfig chestTypeConfig;
    public LoadChances lc;
    public Timer timer;
    public GenerateChest gc;
    public LootEvent lootEvent;
    public ItemAdderGui itemAdderGui;
    public ArrayList<String> commands;
    public ArrayList<Player> abletobreak;
    public static HashMap<Integer, ItemStack> items = new HashMap<>();

    public static HashMap<Integer, Integer> itemMin = new HashMap<>();
    public static HashMap<Integer, Integer> itemMax = new HashMap<>();
    
    public HashMap<Player, ItemEditSession> editSessions = new HashMap<>();
    public HashMap<Player, WaitChooseChest> addChestplayers = new HashMap<>();
    public HashMap<Location, FixedChestInfo> FixedChests = new HashMap<>();
    public HashMap<Location, RandomChestInfo> RandomChests = new HashMap<>();
    public HashMap<Player, Boolean> addChestTypeSelector = new HashMap<>();
    private int SpawnChestPerTime, KillChestAfterTime, FixedChestUpdateTimeMin, FixedChestUpdateTimeMax;

    public Main() {
        pl = this;
        db = Database.instance; configManager = new ConfigManager(this);
        messages = new Messages();
        chestTypeConfig = new ChestTypeConfig(); lc = LoadChances.instance;
        timer = Timer.instance; gc = new GenerateChest();
        lootEvent = new LootEvent(); itemAdderGui = new ItemAdderGui();
        commands = new ArrayList<>(); abletobreak = new ArrayList<>();
    }

    public void onEnable() {
        saveDefaultConfig();
        messages.setup(this);
        configManager.saveLegalConstants("legal_effects.txt", "org.bukkit.Effect");
        configManager.saveLegalConstants("legal_sounds.txt", "org.bukkit.Sound");
        configManager.saveLegalConstants("legal_particles.txt", "org.bukkit.Particle");
        if (!configManager.loadConfig()) { pause(5); return; }
        db.setup(this); db.data.options().copyDefaults(true);
        chestTypeConfig.setup(this); chestTypeConfig.loadChestTypes();
        ensureDatabaseSections();
        new RegionConfig().loadRegions();
        CommandManager cmdManager = new CommandManager(this);
        Objects.requireNonNull(getCommand("rlc")).setExecutor(cmdManager);
        Objects.requireNonNull(getCommand("rlc")).setTabCompleter(cmdManager);
        getServer().getPluginManager().registerEvents(lootEvent, this);
        getServer().getPluginManager().registerEvents(itemAdderGui, this);
        getServer().getPluginManager().registerEvents(new WandListener(), this);
        getServer().getPluginManager().registerEvents(new QuitListener(), this);
        getServer().getPluginManager().registerEvents(new BlockPlaceProtection(), this);
        lc.loaditems(); syncConfig();
        if (!timer.loadChests()) {
            MessageUtil.sendConsole(messages.get("console.db_error_line1"));
            MessageUtil.sendConsole(messages.get("console.db_error_line2"));
            MessageUtil.sendConsole(messages.get("console.db_error_line3"));
            MessageUtil.sendConsole(messages.get("console.db_error_line4"));
            pause(5); return;
        }
        FindAvaliableLocation.init(); gc.GenerateChest(SpawnChestPerTime);
        if (configManager.getRandomChestEffect() != null) startParticles();
        if (configManager.isKillChest()) timer.decrease();
        startNotificationScheduler(); sendEnableMessage();
    }

    public void onDisable() {
        if (configManager != null && configManager.isPluginEnabled()) timer.saveChests();
    }

    public void reloadPlugin() {
        if (!configManager.loadConfig()) getLogger().warning("Config reload failed.");
        syncConfig();
        messages.reload();
        chestTypeConfig.reloadConfig(); chestTypeConfig.loadChestTypes();
        RegionConfig.getInstance().reload();
        db.reloadData(); lc.loaditems();
    }

    void syncConfig() {
        SpawnChestPerTime = configManager.getSpawnChestPerTime();
        KillChestAfterTime = configManager.getKillChestAfterTime();
        FixedChestUpdateTimeMin = configManager.getFixedChestUpdateTimeMin();
        FixedChestUpdateTimeMax = configManager.getFixedChestUpdateTimeMax();
        commands = configManager.getCommandsOnLoot();
    }

    void ensureDatabaseSections() {
        for (String s : new String[]{"Chests", "ItemDatabase", "LocationDatabase"})
            if (!db.data.isConfigurationSection(s)) { db.data.createSection(s); db.saveData(); }
    }

    void startParticles() {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            for (Entry<Location, RandomChestInfo> e : RandomChests.entrySet())
                configManager.getRandomChestEffect().play(e.getKey());
        }, 20L, 20L);
    }

    void startNotificationScheduler() {
        int interval = getConfig().getInt("NotificationInterval", 300);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            if (!RandomChests.isEmpty()) {
                Location l = RandomChests.entrySet().iterator().next().getKey();
                if (configManager.getMessageOnSpawn() != null) {
                    MessageUtil.broadcastWithPlaceholders(
                            configManager.getMessageOnSpawn(),
                            "{X}", String.valueOf(l.getBlockX()),
                            "{Y}", String.valueOf(l.getBlockY()),
                            "{Z}", String.valueOf(l.getBlockZ()));
                }
            }
        }, 0L, interval * 20L);
    }

    void pause(int s) {
        MessageUtil.sendConsole(messages.getFormatted("console.pause", "{giay}", String.valueOf(s)));
        try { TimeUnit.SECONDS.sleep(s); } catch (InterruptedException ignored) {}
    }

    void sendEnableMessage() {
        MessageUtil.sendConsole(messages.get("console.enabled_line1"));
        MessageUtil.sendConsole(messages.get("console.enabled_line2"));
        MessageUtil.sendConsole(messages.get("console.enabled_line3"));
        MessageUtil.sendConsole(messages.get("console.enabled_line4"));
        MessageUtil.sendConsole(messages.get("console.enabled_line5"));
    }

    public EffectWrapper getRandomChestEffect() { return configManager.getRandomChestEffect(); }
    public SoundWrapper getRandomChestSound() { return configManager.getRandomChestSound(); }
    public EffectWrapper getRandomChestOpenSound() { return configManager.getRandomChestOpenSound(); }
    public EffectWrapper getFixedChestEffect() { return configManager.getFixedChestEffect(); }
    public SoundWrapper getFixedChestSound() { return configManager.getFixedChestSound(); }
    public String getMessageOnSpawn() { return messages.get("chest.spawn"); }
    public String getMessageOnLoot() { return messages.get("chest.loot"); }
    public String getMessageOnKill() { return messages.get("chest.kill"); }
    public int getSpawnChestPerTime() { return SpawnChestPerTime; }
    public int getKillChestAfterTime() { return KillChestAfterTime; }
    public int getFixedChestUpdateTimeMin() { return FixedChestUpdateTimeMin; }
    public int getFixedChestUpdateTimeMax() { return FixedChestUpdateTimeMax; }
    public ArrayList<String> getCommandsOnLoot() { return commands; }
    public LootEvent getLootEvent() { return lootEvent; }
    public GenerateChest getGenerateChest() { return gc; }
    public ItemAdderGui getItemAdderGui() { return itemAdderGui; }

    public boolean canSpawnChest(Location loc) {
        Block block = loc.getBlock();
        if (block instanceof BlockState) return false;
        Material m = block.getType();
        if (!configManager.getSpawnBlockCondition_Positive().isMatch(m)) return false;
        if (!configManager.getSpawnBlockCondition_Negative().isMatch(m)) return false;
        loc = loc.clone().add(0, -1, 0);
        m = loc.getBlock().getType();
        if (!configManager.getUnderBlockCondition_Positive().isMatch(m)) return false;
        if (!configManager.getUnderBlockCondition_Negative().isMatch(m)) return false;
        return checkSide(loc.clone().add(1, 1, 0).getBlock().getType())
            && checkSide(loc.clone().add(0, 1, 1).getBlock().getType())
            && checkSide(loc.clone().add(-1, 1, 0).getBlock().getType())
            && checkSide(loc.clone().add(0, 1, -1).getBlock().getType());
    }

    boolean checkSide(Material m) {
        return configManager.getSideBlockCondition_Positive().isMatch(m)
            && configManager.getSideBlockCondition_Negative().isMatch(m)
            && !RLCUtils.isRandomChestType(m);
    }

    public void randomizeRandomChestsTimeLeft() {
        int t = Math.max(getConfig().getInt("KillChestAfterTime"), 0);
        for (RandomChestInfo i : RandomChests.values()) i.Time = FindAvaliableLocation.getRandom(0, t);
    }

    public void randomizeFixedChestsTimeLeft() {
        int t = Math.max(FixedChestUpdateTimeMax, 0);
        FixedChests.replaceAll((l, v) -> { v.Time = FindAvaliableLocation.getRandom(0, t); return v; });
    }
}