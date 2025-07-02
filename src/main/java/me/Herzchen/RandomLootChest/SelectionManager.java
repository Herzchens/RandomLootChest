package me.Herzchen.RandomLootChest;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SelectionManager implements Listener {
    private final Map<UUID, Location> pos1Map = new HashMap<>();
    private final Map<UUID, Location> pos2Map = new HashMap<>();

    public void setPos1(UUID playerId, Location loc) {
        pos1Map.put(playerId, loc);
    }

    public void setPos2(UUID playerId, Location loc) {
        pos2Map.put(playerId, loc);
    }

    public boolean hasCompleteSelection(UUID playerId) {
        return pos1Map.containsKey(playerId) && pos2Map.containsKey(playerId);
    }

    public Location[] getSelection(UUID playerId) {
        return new Location[]{pos1Map.get(playerId), pos2Map.get(playerId)};
    }

    public void clearSelection(UUID playerId) {
        pos1Map.remove(playerId);
        pos2Map.remove(playerId);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        clearSelection(playerId);
    }
}