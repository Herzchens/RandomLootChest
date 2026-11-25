package me.Herzchen.RandomLootChest.model;

public class RandomChestInfo {
    public int Time;
    public String Block;
    public String ChestTypeId;

    public RandomChestInfo(int time, String block, String chestTypeId) {
        this.Time = time;
        this.Block = block;
        this.ChestTypeId = chestTypeId;
    }

    public RandomChestInfo(int time, String block) {
        this.Time = time;
        this.Block = block;
        this.ChestTypeId = "default";
    }
}