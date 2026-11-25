package me.Herzchen.RandomLootChest.command;

import me.Herzchen.RandomLootChest.Main;
import me.Herzchen.RandomLootChest.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class WaitChooseChest extends BukkitRunnable {
    private Player Player;
    private String Command;
    private String ChestTypeName;
    private int Left;

    public void start(Player player, String command) {
        start(player, command, null);
    }

    public void start(Player player, String command, String chestTypeName) {
        Player = player;
        Command = command;
        ChestTypeName = chestTypeName;
        Left = 5;
        Main.pl.addChestplayers.put(player, this);
        MessageUtil.send(player, Main.pl.messages.getFormatted("wait.countdown",
                "{giay}", String.valueOf(Left)));
        runTaskTimer(Main.pl, 20L, 20L);
    }

    public String getCommand() { return Command; }
    public String getChestTypeName() { return ChestTypeName; }

    @Override
    public void run() {
        if (Player.isOnline()) {
            MessageUtil.send(Player, Main.pl.messages.getFormatted("wait.countdown",
                    "{giay}", String.valueOf(Left)));
        }
        if (Left-- < 0) {
            MessageUtil.send(Player, Main.pl.messages.get("wait.timeout", "<red>Hết thời gian. Bạn đã quá chậm <white>=("));
            Main.pl.addChestplayers.remove(Player);
            cancel();
        }
    }
}