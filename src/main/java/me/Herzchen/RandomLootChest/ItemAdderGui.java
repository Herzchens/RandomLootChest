package me.Herzchen.RandomLootChest;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ItemAdderGui implements Listener {
   LoadChances lc = new LoadChances();

   public ConfigurationSection itdb() {
      return Main.pl.db.data.getConfigurationSection("ItemDatabase");
   }

   public void addOnMap(ItemStack item) {
      for(int i = 0; i < 100000; ++i) {
         if (Main.pl.itemstoadd.get(i) == null) {
            Main.pl.itemstoadd.put(i, item);
            Main.pl.itemstoadd.put(i, item);
            break;
         }
      }

   }

   public void loadItems() {
      Main.pl.db.loadData();

      for(int i = 0; i < 10000 && this.itdb().isConfigurationSection(String.valueOf(i)); ++i) {
         ItemStack item = this.itdb().getConfigurationSection(String.valueOf(i)).getItemStack("item");
         int chance = this.itdb().getConfigurationSection(String.valueOf(i)).getInt("chance");
         Main.pl.itemstoadd.put(i, item);
         Main.pl.chances.put(i, chance);
      }

   }

   public boolean isFull(Inventory inv) {
      for(int i = 0; i < 45; ++i) {
         if (inv.getItem(i) == null) {
            return false;
         }
      }

      return true;
   }

   public void saveItems() {
      Main.pl.db.loadData();
      int toset = 0;
      Main.pl.db.data.set("ItemDatabase", (Object)null);
      Main.pl.db.data.createSection("ItemDatabase");

      for(int i = 0; i < Main.pl.itemstoadd.size(); ++i) {
         if (Main.pl.itemstoadd.get(i) != null) {
            this.itdb().createSection(String.valueOf(toset));
            this.itdb().getConfigurationSection(String.valueOf(toset)).set("item", Main.pl.itemstoadd.get(i));
            if (Main.pl.chances.get(i) != null && (Integer)Main.pl.chances.get(i) != 0) {
               this.itdb().getConfigurationSection(String.valueOf(toset)).set("chance", Main.pl.chances.get(i));
            } else {
               this.itdb().getConfigurationSection(String.valueOf(toset)).set("chance", 50);
            }
         } else {
            --toset;
         }

         ++toset;
      }

      Main.pl.db.saveData();
      this.loadItems();
      this.lc.loaditems();
   }

   public void addchance(int id, int chancetoadd) {
      int currentchance = (Integer)Main.pl.chances.get(id);
      Main.pl.chances.put(id, currentchance + chancetoadd);
   }

   public void remove(int id, int chancetoremove) {
      int currentchance = (Integer)Main.pl.chances.get(id);
      Main.pl.chances.put(id, currentchance - chancetoremove);
   }

   public void additems(Player player, int page) {
      Inventory inv = player.getOpenInventory().getTopInventory();
      ItemStack arrow = new ItemStack(Material.ARROW);
      ItemMeta arrowmeta = arrow.getItemMeta();
      arrowmeta.setDisplayName("§cQuay lại");
      arrow.setItemMeta(arrowmeta);
      inv.setItem(45, arrow);
      arrowmeta.setDisplayName("§cTrang sau");
      arrow.setItemMeta(arrowmeta);
      inv.setItem(53, arrow);
      ItemStack paper = new ItemStack(Material.PAPER);
      ItemMeta papermeta = paper.getItemMeta();
      papermeta.setDisplayName("§6Thông tin:");
      List<String> lore = new ArrayList();
      lore.add("§aĐể thêm vật phẩm vào rương");
      lore.add("§achỉ cần thả chúng vào đây.");
      lore.add("§aĐể chỉnh sửa tỷ lệ của vật phẩm");
      lore.add("§anhấp chuột phải và trình chỉnh sửa tỷ lệ");
      lore.add("§asẽ mở ra.");
      lore.add("§cNếu bạn không chỉnh sửa tỷ lệ, nó sẽ");
      lore.add("§ctự động được đặt thành 50.");
      papermeta.setLore(lore);
      paper.setItemMeta(papermeta);
      inv.setItem(49, paper);
      int maxnumber;
      int firstnumber;
      if (page == 1) {
         firstnumber = page * 45 - 45;
         maxnumber = page * 45;
      } else {
         firstnumber = page * 45 - 45;
         maxnumber = page * 45;
      }

      int slotcounter = 0;

      for(int glass = firstnumber; glass < maxnumber; ++glass) {
         if (Main.pl.itemstoadd.get(glass) != null) {
            inv.setItem(slotcounter, (ItemStack)Main.pl.itemstoadd.get(glass));
         }

         ++slotcounter;
      }

      ItemStack var14 = new ItemStack(Material.STAINED_GLASS_PANE);
      ItemMeta glassmeta = var14.getItemMeta();
      glassmeta.setDisplayName(" ");
      var14.setItemMeta(glassmeta);
      var14.setDurability((short)7);
      inv.setItem(47, var14);
      inv.setItem(48, var14);
      inv.setItem(46, var14);
      inv.setItem(51, var14);
      inv.setItem(52, var14);
      inv.setItem(50, var14);
   }

   public void openPage(Player player, int page) {
      Inventory inv = Bukkit.createInventory((InventoryHolder)null, 54, "§8Trang: " + page + "/5");
      Main.pl.additem.add(player);
      Main.pl.currentpage.put(player, page);
      player.openInventory(inv);
      this.additems(player, page);
   }

   public void openGui(Player player) {
      this.loadItems();
      this.openPage(player, 1);
   }

   public ItemStack item(ItemStack item, String name, int durability) {
      ItemMeta itemmeta = item.getItemMeta();
      itemmeta.setDisplayName(name);
      item.setItemMeta(itemmeta);
      item.setDurability((short)durability);
      return item;
   }

   public int chancetoaddorremove(ItemStack item) {
      String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
      String name1 = name.replace("Thêm", "");
      String name2 = name1.replace("Xóa", "");
      String name3 = name2.replace(" ", "");
      return Integer.parseInt(name3);
   }

   @EventHandler
   public void onClick(InventoryClickEvent e) {
      Player player = (Player)e.getWhoClicked();
      if (Main.pl.additem.contains(player)) {
         if (e.getCurrentItem() == null) {
            return;
         }

         int inv;
         if (e.getSlot() == 53) {
            e.setCancelled(true);
            if ((Integer)Main.pl.currentpage.get(player) == 5) {
               return;
            }

            if (this.isFull(e.getInventory())) {
               inv = (Integer)Main.pl.currentpage.get(player) + 1;
               player.getOpenInventory().close();
               this.openPage(player, inv);
            } else {
               player.sendMessage("§cVui lòng điền đầy trang này trước khi chuyển sang trang tiếp theo.");
            }

            return;
         }

         if (e.getSlot() == 45) {
            e.setCancelled(true);
            if ((Integer)Main.pl.currentpage.get(player) == 1) {
               return;
            }

            inv = (Integer)Main.pl.currentpage.get(player) - 1;
            player.getOpenInventory().close();
            this.openPage(player, inv);
            return;
         }

         if (e.getSlot() > 45 && e.getSlot() < 54) {
            e.setCancelled(true);
         }

         if (e.getClick().equals(ClickType.RIGHT) || e.getClick().equals(ClickType.SHIFT_RIGHT)) {
            this.save(e.getInventory(), (Integer)Main.pl.currentpage.get(player));
            if (!e.getCurrentItem().getType().toString().equalsIgnoreCase("AIR") && e.getSlot() < 45) {
               e.setCancelled(true);
               if ((Integer)Main.pl.currentpage.get(player) == 1) {
                  inv = e.getSlot();
               } else {
                  inv = (Integer)Main.pl.currentpage.get(player) * 45 - 45 + e.getSlot();
               }

               this.openChanceEditor(player, inv, (Integer)Main.pl.currentpage.get(player));
            }
         }
      } else if (Main.pl.idediting.containsKey(player)) {
         Inventory inv1 = e.getInventory();
         e.setCancelled(true);
         int id = (Integer)Main.pl.idediting.get(player);
         int currentchance = (Integer)Main.pl.chances.get(id);
         int lastpage = (Integer)Main.pl.lastpageno.get(player);
         if (e.getCurrentItem().getType().equals(Material.ARROW)) {
            player.closeInventory();
            this.openPage(player, lastpage);
            return;
         }

         if (e.getCurrentItem().getType().equals(Material.STAINED_GLASS_PANE)) {
            int chancetoremove;
            if (e.getCurrentItem().getDurability() == 13) {
               chancetoremove = this.chancetoaddorremove(e.getCurrentItem());
               if (currentchance + chancetoremove > 100) {
                  player.sendMessage("§cTỷ lệ không thể lớn hơn 100");
               } else {
                  this.addchance(id, chancetoremove);
                  inv1.setItem(13, this.item(new ItemStack(Material.DIAMOND), "§6Tỷ lệ hiện tại: §c" + Main.pl.chances.get(id), 0));
               }
            } else if (e.getCurrentItem().getDurability() == 14) {
               chancetoremove = this.chancetoaddorremove(e.getCurrentItem());
               if (currentchance - chancetoremove < 1) {
                  player.sendMessage("§cTỷ lệ không thể nhỏ hơn 1");
               } else {
                  this.remove(id, chancetoremove);
                  inv1.setItem(13, this.item(new ItemStack(Material.DIAMOND), "§6Tỷ lệ hiện tại: §c" + Main.pl.chances.get(id), 0));
               }
            }
         }
      }

   }

   public void save(Inventory inv, int page) {
      int maplast;
      int mapfirst;
      if (page == 1) {
         maplast = page * 45;
         mapfirst = page * 45 - 45;
      } else {
         mapfirst = page * 45 - 45;
         maplast = page * 45;
      }

      int slotcounter = 0;

      for(int i = mapfirst; i < maplast; ++i) {
         Main.pl.itemstoadd.remove(i);
         if (inv.getItem(slotcounter) != null) {
            this.addOnMap(inv.getItem(slotcounter));
         }

         ++slotcounter;
      }

      this.saveItems();
   }

   public void openChanceEditor(Player player, int itemid, int currentpage) {
      Main.pl.idediting.put(player, itemid);
      Main.pl.lastpageno.put(player, currentpage);
      Inventory inv = Bukkit.createInventory((InventoryHolder)null, 27, "§aChỉnh sửa tỷ lệ");
      inv.setItem(10, this.item(new ItemStack(Material.STAINED_GLASS_PANE), "§aThêm 50", 13));
      inv.setItem(11, this.item(new ItemStack(Material.STAINED_GLASS_PANE), "§aThêm 10", 13));
      inv.setItem(12, this.item(new ItemStack(Material.STAINED_GLASS_PANE), "§aThêm 1", 13));
      inv.setItem(13, this.item(new ItemStack(Material.DIAMOND), "§6Tỷ lệ hiện tại: §c" + Main.pl.chances.get(itemid), 0));
      inv.setItem(14, this.item(new ItemStack(Material.STAINED_GLASS_PANE), "§aXóa 1", 14));
      inv.setItem(15, this.item(new ItemStack(Material.STAINED_GLASS_PANE), "§aXóa 10", 14));
      inv.setItem(16, this.item(new ItemStack(Material.STAINED_GLASS_PANE), "§aXóa 50", 14));
      inv.setItem(18, this.item(new ItemStack(Material.ARROW), "§cQuay lại", 0));

      for(int i = 0; i < 26; ++i) {
         if (inv.getItem(i) == null) {
            inv.setItem(i, this.item(new ItemStack(Material.STAINED_GLASS_PANE), " ", 8));
         }
      }

      player.openInventory(inv);
   }

   @EventHandler
   public void onInvClose(InventoryCloseEvent e) {
      HumanEntity entity = e.getPlayer();
      if (entity instanceof Player) {
         if (Main.pl.additem.contains(entity)) {
            Player player = (Player)e.getPlayer();
            this.save(e.getInventory(), (Integer)Main.pl.currentpage.get(player));
            e.getPlayer().sendMessage("§aDanh sách vật phẩm đã được cập nhật!");
            Main.pl.additem.remove(player);
            Main.pl.currentpage.remove(player);
         } else if (Main.pl.idediting.containsKey(entity)) {
            Main.pl.idediting.remove(entity);
            Main.pl.lastpageno.remove(entity);
            this.saveItems();
         }
      }

   }

   @EventHandler
   public void onQuit(PlayerQuitEvent e) {
      if (Main.pl.additem.contains(e.getPlayer())) {
         Main.pl.additem.remove(e.getPlayer());
         Main.pl.currentpage.remove(e.getPlayer());
      }

   }
}