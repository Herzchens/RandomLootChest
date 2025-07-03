package me.Herzchen.RandomLootChest;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.*;
import java.util.stream.Collectors;

public class TabComplete implements TabCompleter {

    private static final Map<String, String> SUBCOMMANDS = new LinkedHashMap<String, String>() {{
        put("additem", "randomlootchest.additem");
        put("addchest", "randomlootchest.fixedchest");
        put("delchest", "randomlootchest.fixedchest");
        put("delall", "randomlootchest.delall");
        put("killall", "randomlootchest.killall");
        put("togglebreak", "randomlootchest.togglebreak");
        put("forcespawn", "randomlootchest.forcespawn");
        put("rndtime", "randomlootchest.rndtime");
    }};

    private static final List<String> CONSOLE_ONLY = Arrays.asList(
            "delall", "killall", "forcespawn", "rndtime"
    );

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
         if (!command.getName().equalsIgnoreCase("rlc")) {
            return null;
        }

        switch (args.length) {
            case 1:
                return getFilteredSubcommands(sender, args[0]);
            case 2:
                return handleSubcommand(sender, args[0].toLowerCase(), args);
            default:
                return Collections.emptyList();
        }
    }

    private List<String> getFilteredSubcommands(CommandSender sender, String prefix) {
        List<String> completions = new ArrayList<>();

        for (Map.Entry<String, String> entry : SUBCOMMANDS.entrySet()) {
            String subCommand = entry.getKey();
            String permission = entry.getValue();

            if (canUseCommand(sender, subCommand, permission)) {
                if (prefix.isEmpty() || StringUtil.startsWithIgnoreCase(subCommand, prefix)) {
                    completions.add(subCommand);
                }
            }
        }
        Collections.sort(completions);
        return completions;
    }

    private List<String> handleSubcommand(CommandSender sender, String subCommand, String[] args) {
        String permission = SUBCOMMANDS.get(subCommand);
        if (permission != null && !sender.hasPermission(permission)) {
            return Collections.emptyList();
        }

        switch (subCommand) {
            case "addchest":
            case "delchest":
                if (args[1].isEmpty()) {
                    return Arrays.asList("normal", "trapped", "shulker");
                }
                break;

            case "give":
                if (args.length == 2) {
                    return Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(name -> StringUtil.startsWithIgnoreCase(name, args[1]))
                            .collect(Collectors.toList());
                } else if (args.length == 3) {
                    return Main.items.keySet().stream()
                            .map(String::valueOf)
                            .filter(id -> StringUtil.startsWithIgnoreCase(id, args[2]))
                            .collect(Collectors.toList());
                }
                break;

        }

        return Collections.emptyList();
    }

    private boolean canUseCommand(CommandSender sender, String subCommand, String permission) {
        if (!(sender instanceof Player) && !CONSOLE_ONLY.contains(subCommand)) {
            return false;
        }

        return permission == null || sender.hasPermission(permission);
    }
}