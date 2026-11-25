package me.Herzchen.RandomLootChest.listener;

import me.Herzchen.RandomLootChest.model.Selection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class QuitListener implements Listener {
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        Selection.selections.remove(e.getPlayer().getUniqueId());
    }
}