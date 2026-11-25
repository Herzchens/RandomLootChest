package me.Herzchen.RandomLootChest.model;

import org.bukkit.Location;
import java.util.HashMap;
import java.util.UUID;

public class Selection {
    public Location pos1;
    public Location pos2;
    public static final HashMap<UUID, Selection> selections = new HashMap<>();
}