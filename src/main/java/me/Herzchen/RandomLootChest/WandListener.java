package me.Herzchen.RandomLootChest;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class WandListener implements Listener {
    private final SelectionManager selectionManager;
    private final Main plugin;

    public WandListener(SelectionManager selectionManager, Main plugin) {
        this.selectionManager = selectionManager;
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        Block clickedBlock = event.getClickedBlock();

        if (!isRLCWand(item)) return;
        if (clickedBlock == null) return;

        UUID playerId = player.getUniqueId();
        Location loc = clickedBlock.getLocation();

        if (!player.hasPermission("randomlootchest.set.region")) {
            player.sendMessage(ChatColor.RED + "Bạn không có quyền dùng wand!");
            return;
        }

        event.setCancelled(true);
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            selectionManager.setPos1(playerId, loc);
            player.sendMessage(ChatColor.GREEN + "Đã chọn §ePosition 1 §atại: §7" + formatLocation(loc));
        }
        else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            selectionManager.setPos2(playerId, loc);
            player.sendMessage(ChatColor.GREEN + "Đã chọn §ePosition 2 §atại: §7" + formatLocation(loc));
        }
    }

    private boolean isRLCWand(ItemStack item) {
        return item != null &&
                item.getType() == Material.STICK &&
                item.hasItemMeta() &&
                item.getItemMeta().getDisplayName().equals(ChatColor.GOLD + "RLC Region Wand");
    }

    private String formatLocation(Location loc) {
        return loc.getWorld().getName() + " | X:" + loc.getBlockX() + " Y:" + loc.getBlockY() + " Z:" + loc.getBlockZ();
    }
}