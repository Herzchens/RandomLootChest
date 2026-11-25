package me.Herzchen.RandomLootChest.listener;

import me.Herzchen.RandomLootChest.Main;
import me.Herzchen.RandomLootChest.util.MessageUtil;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockPlaceProtection implements Listener {
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        Location loc = e.getBlock().getLocation();
        if (Main.pl.RandomChests.containsKey(loc) || Main.pl.FixedChests.containsKey(loc)) {
            e.setCancelled(true);
            MessageUtil.send(e.getPlayer(), Main.pl.messages.get("chest.cannot_place", "<red>Không thể đặt block tại vị trí RLC Chest!"));
        }
    }
}