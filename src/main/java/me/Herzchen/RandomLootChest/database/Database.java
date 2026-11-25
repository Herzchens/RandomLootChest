package me.Herzchen.RandomLootChest.database;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;

public class Database {
   public static Database instance = new Database();
   public FileConfiguration data;
   public File dfile;

   public void setup(Plugin p) {
      if (!p.getDataFolder().exists()) p.getDataFolder().mkdirs();
      dfile = new File(p.getDataFolder(), "database.yml");
      if (!dfile.exists()) {
         try { dfile.createNewFile(); }
         catch (IOException e) { Bukkit.getLogger().severe(ChatColor.RED + "Không thể tạo database.yml!"); }
      }
      data = YamlConfiguration.loadConfiguration(dfile);
   }

   public void saveData() {
      try { data.save(dfile); }
      catch (IOException e) { Bukkit.getLogger().severe(ChatColor.RED + "Không thể lưu database.yml!"); }
   }

   public void loadData() {
      try { data.load(dfile); }
      catch (IOException | InvalidConfigurationException e) { Bukkit.getLogger().severe(ChatColor.RED + "Không thể nạp database.yml!"); }
   }

   public void reloadData() { data = YamlConfiguration.loadConfiguration(dfile); }
}