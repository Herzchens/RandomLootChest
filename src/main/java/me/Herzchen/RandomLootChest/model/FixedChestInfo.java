package me.Herzchen.RandomLootChest.model;

public class FixedChestInfo {
    public int Time;
    public String ChestTypeId;

    public FixedChestInfo(int time, String chestTypeId) {
        this.Time = time;
        this.ChestTypeId = chestTypeId;
    }

    public FixedChestInfo(int time) {
        this.Time = time;
        this.ChestTypeId = null; // null = global/default
    }
}