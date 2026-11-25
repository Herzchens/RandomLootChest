package me.Herzchen.RandomLootChest.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import me.Herzchen.RandomLootChest.Main;
import me.Herzchen.RandomLootChest.gui.OpenLootInventory;
import me.Herzchen.RandomLootChest.gui.OpenLootInventory;
import me.Herzchen.RandomLootChest.model.ChestType;
import me.Herzchen.RandomLootChest.model.FixedChestInfo;
import me.Herzchen.RandomLootChest.model.LootTable;
import me.Herzchen.RandomLootChest.model.RandomChestInfo;
import me.Herzchen.RandomLootChest.model.Selection;
import me.Herzchen.RandomLootChest.region.Region;
import me.Herzchen.RandomLootChest.region.RegionConfig;
import me.Herzchen.RandomLootChest.util.FindAvaliableLocation;
import me.Herzchen.RandomLootChest.util.MessageUtil;
import me.Herzchen.RandomLootChest.util.RLCUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;
import static com.mojang.brigadier.builder.RequiredArgumentBuilder.argument;

/**
 * Brigadier-powered command dispatcher for the {@code /rlc} command.
 *
 * Replaces the old map-of-handlers {@code CommandManager} plus the manually
 * maintained {@code TabComplete} class. Every subcommand, its arguments, its
 * permission check and its tab-suggestions are declared in a single
 * {@link CommandDispatcher} tree (see {@link #buildCommandTree()}).
 */
public class CommandManager implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private final CommandDispatcher<CommandSender> dispatcher = new CommandDispatcher<>();

    public CommandManager(Main plugin) {
        this.plugin = plugin;
        buildCommandTree();
    }

    // ───────────────────────────────────────────────────────────────
    //  Suggestion providers (tab completion)
    // ───────────────────────────────────────────────────────────────

    /** All registered chest types. */
    private static final SuggestionProvider<CommandSender> SUGGEST_TYPES = (ctx, b) -> {
        for (ChestType t : ChestType.getAllChestTypes().values())
            if (t.getId().toLowerCase().startsWith(b.getRemainingLowerCase())) b.suggest(t.getId());
        return b.buildFuture();
    };

    /** Chest types + the special "global" pseudo-target (additem / loottable). */
    private static final SuggestionProvider<CommandSender> SUGGEST_TYPES_OR_GLOBAL = (ctx, b) -> {
        if ("global".startsWith(b.getRemainingLowerCase())) b.suggest("global");
        for (ChestType t : ChestType.getAllChestTypes().values())
            if (t.getId().toLowerCase().startsWith(b.getRemainingLowerCase())) b.suggest(t.getId());
        return b.buildFuture();
    };

    /** Registered region ids. */
    private static final SuggestionProvider<CommandSender> SUGGEST_REGIONS = (ctx, b) -> {
        for (Region r : RegionConfig.getInstance().getAllRegions())
            if (r.getId().toLowerCase().startsWith(b.getRemainingLowerCase())) b.suggest(r.getId());
        return b.buildFuture();
    };

    /** resettimer target selector (random|fixedchest). */
    private static final SuggestionProvider<CommandSender> SUGGEST_TARGET = (ctx, b) -> {
        for (String s : new String[]{"random", "fixedchest"})
            if (s.startsWith(b.getRemainingLowerCase())) b.suggest(s);
        return b.buildFuture();
    };

    // ───────────────────────────────────────────────────────────────
    //  Command tree
    // ───────────────────────────────────────────────────────────────

    /**
     * javac cannot thread the {@code CommandSender} type argument through
     * nested generic builder chains, so we pin it down with these small
     * non-generic wrappers.
     */
    private static LiteralArgumentBuilder<CommandSender> r(String name) {
        return literal(name);
    }

    private static <T> RequiredArgumentBuilder<CommandSender, T> a(String name, ArgumentType<T> type) {
        return argument(name, type);
    }

    private void buildCommandTree() {
        dispatcher.register(
            LiteralArgumentBuilder.<CommandSender>literal("rlc")
                // Leaf commands (no arguments)
                .then(r("wand").executes(c -> wand(c)))
                .then(r("unset").executes(c -> unset(c)))
                .then(r("togglebreak").executes(c -> toggleBreak(c)))
                .then(r("killall").executes(c -> killAll(c)))
                .then(r("forcespawn").executes(c -> forceSpawn(c)))
                .then(r("rndtime").executes(c -> rndTime(c)))
                .then(r("delchest").executes(c -> delChest(c)))
                .then(r("chesttypes").executes(c -> chestTypes(c)))
                .then(r("regions").executes(c -> regions(c)))
                .then(r("reload").executes(c -> reload(c)))

                // Optional chest-type argument
                .then(r("set")
                        .executes(c -> setCmd(c))
                        .then(a("type", word()).suggests(SUGGEST_TYPES).executes(c -> setCmd(c))))
                .then(r("addchest")
                        .executes(c -> addChest(c))
                        .then(a("type", word()).suggests(SUGGEST_TYPES).executes(c -> addChest(c))))
                .then(r("addchesttype") // kept as alias — same handler as addchest
                        .executes(c -> addChest(c))
                        .then(a("type", word()).suggests(SUGGEST_TYPES).executes(c -> addChest(c))))
                .then(r("additem")
                        .executes(c -> addItem(c))
                        .then(a("type", word()).suggests(SUGGEST_TYPES_OR_GLOBAL).executes(c -> addItem(c))))

                // Required chest-type argument
                .then(r("chesttypeinfo")
                        .then(a("type", word()).suggests(SUGGEST_TYPES).executes(c -> chestTypeInfo(c))))
                .then(r("delchesttype")
                        .then(a("type", word()).suggests(SUGGEST_TYPES).executes(c -> delChestType(c))))
                .then(r("resettimer")
                        .executes(c -> usage(c, "command.resettimer_usage"))
                        .then(a("target", word()).suggests(SUGGEST_TARGET)
                                .executes(c -> resetTimer(c))
                                .then(a("type", word()).suggests(SUGGEST_TYPES)
                                        .executes(c -> resetTimer(c)))))

                // /rlc spawn <type> <pos1> <pos2> [extra] [amount]
                // "extra" is either the world name or the amount (positional
                // ambiguity kept from the original command).
                .then(r("spawn")
                        .then(a("type", word()).suggests(SUGGEST_TYPES)
                                .then(a("pos1", string())
                                        .then(a("pos2", string())
                                                .executes(c -> spawn(c))
                                                .then(a("extra", word())
                                                        .executes(c -> spawn(c))
                                                        .then(a("amount", integer(1, 100))
                                                                .executes(c -> spawn(c))))))))

                // /rlc spawnregion <type> <region> [amount]
                .then(r("spawnregion")
                        .then(a("type", word()).suggests(SUGGEST_TYPES)
                                .then(a("region", word()).suggests(SUGGEST_REGIONS)
                                        .executes(c -> spawnRegion(c))
                                        .then(a("amount", integer(1, 100))
                                                .executes(c -> spawnRegion(c))))))

                // /rlc loottable [reset] <type>
                .then(r("loottable")
                        .executes(c -> usage(c, "command.loottable_usage"))
                        .then(r("reset")
                                .then(a("type", word()).suggests(SUGGEST_TYPES_OR_GLOBAL)
                                        .executes(c -> lootTableReset(c))))
                        .then(a("type", word()).suggests(SUGGEST_TYPES_OR_GLOBAL)
                                .executes(c -> lootTable(c))))
                // /rlc roll <type> — mô phỏng 1 lần fill không cần spawn
                .then(r("roll")
                        .then(a("type", word()).suggests(SUGGEST_TYPES_OR_GLOBAL)
                                .executes(c -> rollLoot(c))))
        );
    }

    // ───────────────────────────────────────────────────────────────
    //  Small shared helpers
    // ───────────────────────────────────────────────────────────────

    /** Safely read a string argument (null if absent). */
    private static String arg(CommandContext<CommandSender> ctx, String name) {
        try { return StringArgumentType.getString(ctx, name); }
        catch (IllegalArgumentException e) { return null; }
    }

    /** Safely read an integer argument (-1 if absent). */
    private static int argInt(CommandContext<CommandSender> ctx, String name) {
        try { return IntegerArgumentType.getInteger(ctx, name); }
        catch (IllegalArgumentException e) { return -1; }
    }

    /** True if the named argument node was present in the parsed input. */
    private static boolean has(CommandContext<CommandSender> ctx, String name) {
        for (var node : ctx.getNodes())
            if (node.getNode().getName().equals(name)) return true;
        return false;
    }

    private boolean hasPerm(CommandSender s, String perm) {
        if (s.hasPermission(perm)) return true;
        MessageUtil.send(s, plugin.messages.get("general.no_permission"));
        return false;
    }

    private boolean requirePlayer(CommandSender s) {
        if (s instanceof Player) return true;
        MessageUtil.send(s, plugin.messages.get("general.player_only"));
        return false;
    }

    private int usage(CommandContext<CommandSender> ctx, String msgKey) {
        MessageUtil.send(ctx.getSource(), plugin.messages.get(msgKey,
                "<red>Sai cú pháp. Xem <yellow>/rlc <red>để biết cách dùng."));
        return 1;
    }

    private static boolean isSameChestType(String storedChestTypeId, String chestTypeId) {
        String normalized = storedChestTypeId == null ? "default" : storedChestTypeId;
        return normalized.equalsIgnoreCase(chestTypeId);
    }

    // ───────────────────────────────────────────────────────────────
    //  Handlers — each returns Brigadier's int status (≠ 0 = success)
    // ───────────────────────────────────────────────────────────────

    // ── /rlc wand ────────────────────────────────────────────────────
    private int wand(CommandContext<CommandSender> ctx) {
        CommandSender s = ctx.getSource();
        if (!requirePlayer(s)) return 0;
        Player p = (Player) s;
        if (!hasPerm(p, "randomlootchest.wand")) return 0;

        ItemStack wand = new ItemStack(Material.STICK);
        ItemMeta meta = wand.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtil.parse("<gold><bold>RLC Wand</bold>"));
            meta.lore(List.of(
                    MessageUtil.parse("<gray>Chuột trái: Chọn pos1"),
                    MessageUtil.parse("<gray>Chuột phải: Chọn pos2"),
                    MessageUtil.parse("<gray>Dùng <yellow>/rlc set [type]"),
                    MessageUtil.parse("<gray>và <yellow>/rlc unset <gray>để thao tác")));
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(Main.pl, "randomlootchest_wand"), PersistentDataType.BOOLEAN, true);
            wand.setItemMeta(meta);
        }
        p.getInventory().addItem(wand);
        MessageUtil.send(p, plugin.messages.get("wand.received", "<green>Bạn đã nhận được que wand!"));
        return 1;
    }

    // ── /rlc set [type] ──────────────────────────────────────────────
    private int setCmd(CommandContext<CommandSender> ctx) {
        CommandSender s = ctx.getSource();
        if (!requirePlayer(s)) return 0;
        Player p = (Player) s;
        if (!hasPerm(p, "randomlootchest.set")) return 0;

        String typeArg = arg(ctx, "type");
        String chestTypeId = null;
        if (typeArg != null && !typeArg.isEmpty()) {
            chestTypeId = typeArg.toLowerCase();
            if (!chestTypeId.equalsIgnoreCase("default") && ChestType.getChestType(chestTypeId) == null) {
                MessageUtil.send(p, plugin.messages.getFormatted("general.chest_type_not_found",
                        "{loai}", chestTypeId) + " <red>Sẽ dùng default.");
                chestTypeId = null;
            }
        }

        Selection sel = Selection.selections.getOrDefault(p.getUniqueId(), new Selection());
        if (sel.pos1 == null || sel.pos2 == null) {
            MessageUtil.send(p, plugin.messages.get("command.addchest_select_no_region",
                    "<red>Vui lòng chọn vùng bằng wand!"));
            return 1;
        }

        ChestType ct = chestTypeId != null ? ChestType.getChestType(chestTypeId) : null;
        int timeMin = ct != null ? ct.getSpawnTimeMin() : plugin.getFixedChestUpdateTimeMin();
        int timeMax = ct != null ? ct.getSpawnTimeMax() : plugin.getFixedChestUpdateTimeMax();

        int cnt = 0, already = 0;
        for (int x = Math.min(sel.pos1.getBlockX(), sel.pos2.getBlockX()); x <= Math.max(sel.pos1.getBlockX(), sel.pos2.getBlockX()); x++)
            for (int y = Math.min(sel.pos1.getBlockY(), sel.pos2.getBlockY()); y <= Math.max(sel.pos1.getBlockY(), sel.pos2.getBlockY()); y++)
                for (int z = Math.min(sel.pos1.getBlockZ(), sel.pos2.getBlockZ()); z <= Math.max(sel.pos1.getBlockZ(), sel.pos2.getBlockZ()); z++) {
                    Location loc = new Location(sel.pos1.getWorld(), x, y, z);
                    if (RLCUtils.isFixedChestType(loc.getBlock()))
                        if (plugin.FixedChests.containsKey(loc)) already++;
                        else {
                            plugin.FixedChests.put(loc, new FixedChestInfo(
                                    FindAvaliableLocation.getRandom(timeMin, timeMax), chestTypeId));
                            Inventory inv = RLCUtils.getInventory(loc.getBlock());
                            if (inv != null) {
                                inv.clear();
                                OpenLootInventory.fillInvenory(inv, ct);
                            }
                            if (plugin.getFixedChestSound() != null) plugin.getFixedChestSound().play(loc);
                            if (plugin.getFixedChestEffect() != null) plugin.getFixedChestEffect().play(loc);
                            cnt++;
                        }
                }

        if (cnt > 0) {
            if (chestTypeId != null)
                MessageUtil.send(p, plugin.messages.getFormatted("command.addchest_set_count",
                        "{so_luong}", String.valueOf(cnt), "{loai}", chestTypeId));
            else
                MessageUtil.send(p, plugin.messages.getFormatted("command.addchest_set_count_no_type",
                        "{so_luong}", String.valueOf(cnt)));
            if (already > 0)
                MessageUtil.send(p, plugin.messages.getFormatted("command.addchest_already",
                        "{so_luong}", String.valueOf(already)));
        } else {
            MessageUtil.send(p, plugin.messages.get("command.addchest_none_found",
                    "<yellow>Không tìm thấy rương nào!"));
        }
        return 1;
    }

    // ── /rlc unset ───────────────────────────────────────────────────
    private int unset(CommandContext<CommandSender> ctx) {
        CommandSender s = ctx.getSource();
        if (!requirePlayer(s)) return 0;
        Player p = (Player) s;
        if (!hasPerm(p, "randomlootchest.unset")) return 0;

        Selection sel = Selection.selections.getOrDefault(p.getUniqueId(), new Selection());
        if (sel.pos1 == null || sel.pos2 == null) {
            MessageUtil.send(p, plugin.messages.get("command.addchest_select_no_region",
                    "<red>Vui lòng chọn vùng bằng wand!"));
            return 1;
        }
        int cnt = 0, notSet = 0;
        for (int x = Math.min(sel.pos1.getBlockX(), sel.pos2.getBlockX()); x <= Math.max(sel.pos1.getBlockX(), sel.pos2.getBlockX()); x++)
            for (int y = Math.min(sel.pos1.getBlockY(), sel.pos2.getBlockY()); y <= Math.max(sel.pos1.getBlockY(), sel.pos2.getBlockY()); y++)
                for (int z = Math.min(sel.pos1.getBlockZ(), sel.pos2.getBlockZ()); z <= Math.max(sel.pos1.getBlockZ(), sel.pos2.getBlockZ()); z++) {
                    Location loc = new Location(sel.pos1.getWorld(), x, y, z);
                    if (RLCUtils.isFixedChestType(loc.getBlock()))
                        if (plugin.FixedChests.containsKey(loc)) { plugin.FixedChests.remove(loc); cnt++; }
                        else notSet++;
                }

        if (cnt > 0) {
            MessageUtil.send(p, plugin.messages.getFormatted("command.unset_done", "{so_luong}", String.valueOf(cnt)));
            if (notSet > 0)
                MessageUtil.send(p, plugin.messages.getFormatted("command.unset_normal", "{so_luong}", String.valueOf(notSet)));
        } else if (notSet > 0)
            MessageUtil.send(p, plugin.messages.getFormatted("command.unset_none_with_normal", "{so_luong}", String.valueOf(notSet)));
        else
            MessageUtil.send(p, plugin.messages.get("command.unset_none", "<yellow>Không tìm thấy rương nào!"));
        return 1;
    }

    // ── /rlc togglebreak ─────────────────────────────────────────────
    private int toggleBreak(CommandContext<CommandSender> ctx) {
        CommandSender s = ctx.getSource();
        if (!requirePlayer(s)) { sendConsoleHelp(s); return 1; }
        Player p = (Player) s;
        if (!hasPerm(p, "randomlootchest.togglebreak")) return 0;

        if (plugin.abletobreak.contains(p)) {
            plugin.abletobreak.remove(p);
            MessageUtil.send(p, plugin.messages.get("command.togglebreak_off", "<gold>Đã tắt chế độ phá rương <red>Tắt!"));
        } else {
            plugin.abletobreak.add(p);
            MessageUtil.send(p, plugin.messages.get("command.togglebreak_on", "<gold>Đã bật chế độ phá rương <green>Bật!"));
        }
        return 1;
    }

    // ── /rlc killall ─────────────────────────────────────────────────
    private int killAll(CommandContext<CommandSender> ctx) {
        CommandSender s = ctx.getSource();
        if (s instanceof Player p && !hasPerm(p, "randomlootchest.killall")) return 0;
        plugin.getLootEvent().killallchests();
        MessageUtil.send(s, plugin.messages.get("command.killall_done", "<green>Đã xóa tất cả rương!"));
        return 1;
    }

    // ── /rlc forcespawn ──────────────────────────────────────────────
    private int forceSpawn(CommandContext<CommandSender> ctx) {
        CommandSender s = ctx.getSource();
        if (!hasPerm(s, "randomlootchest.forcespawn")) return 0;
        plugin.getGenerateChest().spawnchest();
        return 1;
    }

    // ── /rlc rndtime ─────────────────────────────────────────────────
    private int rndTime(CommandContext<CommandSender> ctx) {
        CommandSender s = ctx.getSource();
        if (!hasPerm(s, "randomlootchest.rndtime")) return 0;
        plugin.randomizeRandomChestsTimeLeft();
        plugin.randomizeFixedChestsTimeLeft();
        MessageUtil.send(s, plugin.messages.get("command.rndtime_done", "<yellow>Đã ngẫu nhiên hóa thời gian."));
        return 1;
    }

    // ── /rlc addchest [type]  (addchesttype = alias) ───────────────────
    private int addChest(CommandContext<CommandSender> ctx) {
        CommandSender s = ctx.getSource();
        if (!requirePlayer(s)) return 0;
        Player p = (Player) s;
        if (!hasPerm(p, "randomlootchest.fixedchest")) return 0;

        String typeArg = arg(ctx, "type");
        if (typeArg != null && !typeArg.isEmpty()) {
            String ctName = typeArg.toLowerCase();
            if (ChestType.getChestType(ctName) == null) {
                MessageUtil.send(s, plugin.messages.getFormatted("general.chest_type_not_found", "{loai}", ctName));
                return 1;
            }
            new WaitChooseChest().start(p, "add", ctName);
            return 1;
        }
        new WaitChooseChest().start(p, "add");
        return 1;
    }

    // ── /rlc delchest ────────────────────────────────────────────────
    private int delChest(CommandContext<CommandSender> ctx) {
        CommandSender s = ctx.getSource();
        if (!requirePlayer(s)) return 0;
        Player p = (Player) s;
        if (!hasPerm(p, "randomlootchest.fixedchest")) return 0;
        new WaitChooseChest().start(p, "del");
        return 1;
    }

    // ── /rlc chesttypes ──────────────────────────────────────────────
    private int chestTypes(CommandContext<CommandSender> ctx) {
        CommandSender s = ctx.getSource();
        if (s instanceof Player p && !hasPerm(p, "randomlootchest.chesttypes")) return 0;
        MessageUtil.send(s, plugin.messages.get("command.chesttypes_header"));
        for (ChestType t : ChestType.getAllChestTypes().values())
            MessageUtil.send(s, plugin.messages.getFormatted("command.chesttypes_line",
                    "{loai}", t.getId(), "{vat_lieu}", t.getMaterial().name()));
        MessageUtil.send(s, plugin.messages.get("command.chesttypes_footer"));
        return 1;
    }

    // ── /rlc chesttypeinfo <type> ─────────────────────────────────────
    private int chestTypeInfo(CommandContext<CommandSender> ctx) {
        CommandSender s = ctx.getSource();
        if (s instanceof Player p && !hasPerm(p, "randomlootchest.chesttypes")) return 0;

        String type = arg(ctx, "type");
        ChestType ct = ChestType.getChestType(type);
        if (ct == null) {
            MessageUtil.send(s, plugin.messages.getFormatted("general.chest_type_not_found", "{loai}", type));
            return 1;
        }
        MessageUtil.send(s, plugin.messages.getFormatted("command.chesttypeinfo_header", "{loai}", ct.getId()));
        MessageUtil.send(s, plugin.messages.getFormatted("command.chesttypeinfo_name", "{ten}", ct.getDisplayName()));
        MessageUtil.send(s, plugin.messages.getFormatted("command.chesttypeinfo_material", "{vat_lieu}", ct.getMaterial().name()));
        MessageUtil.send(s, plugin.messages.getFormatted("command.chesttypeinfo_spawn_time",
                "{min}", String.valueOf(ct.getSpawnTimeMin()), "{max}", String.valueOf(ct.getSpawnTimeMax())));
        MessageUtil.send(s, plugin.messages.getFormatted("command.chesttypeinfo_kill_time", "{thoi_gian}", String.valueOf(ct.getKillTime())));
        MessageUtil.send(s, plugin.messages.getFormatted("command.chesttypeinfo_spawn_effect", "{gia_tri}", String.valueOf(ct.getSpawnEffect())));
        MessageUtil.send(s, plugin.messages.getFormatted("command.chesttypeinfo_spawn_sound", "{gia_tri}", String.valueOf(ct.getSpawnSound())));
        MessageUtil.send(s, plugin.messages.getFormatted("command.chesttypeinfo_open_sound", "{gia_tri}", String.valueOf(ct.getOpenSound())));
        MessageUtil.send(s, plugin.messages.getFormatted("command.chesttypeinfo_kill_effect", "{gia_tri}", String.valueOf(ct.getKillEffect())));
        MessageUtil.send(s, plugin.messages.getFormatted("command.chesttypeinfo_kill_sound", "{gia_tri}", String.valueOf(ct.getKillSound())));
        MessageUtil.send(s, plugin.messages.getFormatted("command.chesttypeinfo_loot_table",
                "{gia_tri}", ct.hasLootTable() ? "Có" : "Không (dùng global)"));
        MessageUtil.send(s, plugin.messages.get("command.chesttypes_footer"));
        return 1;
    }

    // ── /rlc delchesttype <type> ─────────────────────────────────────
    private int delChestType(CommandContext<CommandSender> ctx) {
        CommandSender s = ctx.getSource();
        if (!hasPerm(s, "randomlootchest.fixedchest")) return 0;

        String type = arg(ctx, "type");
        if (type == null || ChestType.getChestType(type) == null) {
            MessageUtil.send(s, plugin.messages.getFormatted("general.chest_type_not_found", "{loai}", type));
            return 1;
        }
        if (type.equalsIgnoreCase("default")) {
            MessageUtil.send(s, plugin.messages.get("command.delchesttype_cannot_default",
                    "<red>Không thể xóa loại rương mặc định!"));
            return 1;
        }
        Main.pl.chestTypeConfig.getConfig().set(type, null);
        Main.pl.chestTypeConfig.saveConfig();
        Main.pl.chestTypeConfig.loadChestTypes();
        MessageUtil.send(s, plugin.messages.getFormatted("command.delchesttype_done", "{loai}", type));
        return 1;
    }

    // ── /rlc resettimer [random|fixedchest] <type> ───────────────────
    private int resetTimer(CommandContext<CommandSender> ctx) {
        CommandSender s = ctx.getSource();
        if (!hasPerm(s, "randomlootchest.resettimer")) return 0;

        String target = arg(ctx, "target");
        String type = arg(ctx, "type");
        boolean resetRandom, resetFixed;
        if (type == null) {
            // /rlc resettimer <type> — reset both
            type = target;
            resetRandom = resetFixed = true;
        } else {
            resetRandom = target.equalsIgnoreCase("random");
            resetFixed = target.equalsIgnoreCase("fixedchest");
            // Safety net: /rlc resettimer random <unknown> — treat as both
            if (!resetRandom && !resetFixed) { type = target; resetRandom = resetFixed = true; }
        }

        ChestType ct = ChestType.getChestType(type);
        if (ct == null) {
            MessageUtil.send(s, plugin.messages.getFormatted("general.chest_type_not_found", "{loai}", type));
            return 1;
        }
        String chestTypeId = ct.getId();
        int randomCount = 0, fixedCount = 0;

        if (resetRandom)
            for (var e : plugin.RandomChests.entrySet())
                if (isSameChestType(e.getValue().ChestTypeId, chestTypeId)) { e.getValue().Time = 0; randomCount++; }

        if (resetFixed)
            for (var e : plugin.FixedChests.entrySet())
                if (isSameChestType(e.getValue().ChestTypeId, chestTypeId)) {
                    ChestType fixedCt = e.getValue().ChestTypeId != null ? ChestType.getChestType(e.getValue().ChestTypeId) : null;
                    int min = fixedCt != null ? fixedCt.getSpawnTimeMin() : plugin.getFixedChestUpdateTimeMin();
                    int max = fixedCt != null ? fixedCt.getSpawnTimeMax() : plugin.getFixedChestUpdateTimeMax();
                    e.getValue().Time = Math.max(FindAvaliableLocation.getRandom(min, max), 0);
                    fixedCount++;
                }

        if (resetRandom && resetFixed)
            MessageUtil.send(s, plugin.messages.getFormatted("command.resettimer_done",
                    "{random}", String.valueOf(randomCount), "{fixed}", String.valueOf(fixedCount), "{loai}", chestTypeId));
        else if (resetRandom)
            MessageUtil.send(s, plugin.messages.getFormatted("command.resettimer_done_random",
                    "{random}", String.valueOf(randomCount), "{loai}", chestTypeId));
        else
            MessageUtil.send(s, plugin.messages.getFormatted("command.resettimer_done_fixed",
                    "{fixed}", String.valueOf(fixedCount), "{loai}", chestTypeId));
        return 1;
    }

    // ── /rlc reload ─────────────────────────────────────────────────
    private int reload(CommandContext<CommandSender> ctx) {
        CommandSender s = ctx.getSource();
        if (!hasPerm(s, "randomlootchest.reload")) return 0;
        plugin.reloadPlugin();
        MessageUtil.send(s, plugin.messages.get("command.reload_done", "<green>Đã reload config, chest types và database!"));
        return 1;
    }

    // ── /rlc additem [global|type] ───────────────────────────────────
    private int addItem(CommandContext<CommandSender> ctx) {
        CommandSender s = ctx.getSource();
        if (!requirePlayer(s)) { sendConsoleHelp(s); return 1; }
        Player p = (Player) s;
        if (!hasPerm(p, "randomlootchest.additem")) return 0;

        String type = arg(ctx, "type");
        if (type != null && !type.isEmpty()) {
            if (type.equalsIgnoreCase("global") || ChestType.getChestType(type) != null) {
                plugin.getItemAdderGui().openGuiDirect(p, type.equalsIgnoreCase("global") ? null : type);
                return 1;
            }
            MessageUtil.send(s, plugin.messages.getFormatted("general.chest_type_not_found", "{loai}", type));
            return 1;
        }
        plugin.getItemAdderGui().openGui(p);
        return 1;
    }

    // ── /rlc regions ────────────────────────────────────────────────
    private int regions(CommandContext<CommandSender> ctx) {
        CommandSender s = ctx.getSource();
        if (s instanceof Player p && !hasPerm(p, "randomlootchest.regions")) return 0;

        Collection<Region> regions = RegionConfig.getInstance().getAllRegions();
        if (regions.isEmpty()) {
            MessageUtil.send(s, plugin.messages.get("command.regions_empty",
                    "<yellow>Không có region nào. Đang dùng chế độ global."));
            return 1;
        }
        MessageUtil.send(s, plugin.messages.get("command.regions_header"));
        for (Region r : regions) {
            MessageUtil.send(s, plugin.messages.getFormatted("command.regions_line",
                    "{region}", r.getId(), "{world}", r.getWorld().getName(),
                    "{x1}", String.valueOf(r.getXMin()), "{y1}", String.valueOf(r.getYMin()), "{z1}", String.valueOf(r.getZMin()),
                    "{x2}", String.valueOf(r.getXMax()), "{y2}", String.valueOf(r.getYMax()), "{z2}", String.valueOf(r.getZMax())));
            if (!r.getChestTypes().isEmpty()) {
                StringBuilder types = new StringBuilder();
                for (ChestType ct : r.getChestTypes()) types.append(ct.getId()).append(", ");
                MessageUtil.send(s, plugin.messages.getFormatted("command.regions_types",
                        "{cac_loai}", types.substring(0, types.length() - 2)));
            }
        }
        MessageUtil.send(s, plugin.messages.get("command.regions_footer"));
        return 1;
    }

    // ── /rlc spawn <type> <pos1> <pos2> [world|amount] [amount] ──────
    private int spawn(CommandContext<CommandSender> ctx) {
        CommandSender s = ctx.getSource();
        if (!hasPerm(s, "randomlootchest.spawn")) return 0;

        String type = arg(ctx, "type");
        ChestType ct = ChestType.getChestType(type);
        if (ct == null) {
            MessageUtil.send(s, plugin.messages.getFormatted("general.chest_type_not_found", "{loai}", type));
            return 1;
        }

        Location[] corners = parseCorners(arg(ctx, "pos1"), arg(ctx, "pos2"));
        if (corners == null) {
            MessageUtil.send(s, plugin.messages.get("command.spawn_bad_coords", "<red>Tọa độ không hợp lệ! Định dạng: x,y,z"));
            return 1;
        }

        World world;
        int amount;
        String extra = arg(ctx, "extra");
        boolean hasAmount = has(ctx, "amount");

        if (hasAmount) {
            // <type> <p1> <p2> <world> <amount>
            world = resolveWorld(s, extra);
            if (world == null) return 1;
            amount = Math.max(1, Math.min(argInt(ctx, "amount"), 100));
        } else if (extra != null && extra.matches("-?\\d+")) {
            // <type> <p1> <p2> <amount>  (no world)
            if (!requirePlayer(s)) return 0;
            world = ((Player) s).getWorld();
            amount = Math.max(1, Math.min(parseInt(extra), 100));
        } else if (extra != null) {
            // <type> <p1> <p2> <world>
            world = resolveWorld(s, extra);
            if (world == null) return 1;
            amount = 1;
        } else {
            // <type> <p1> <p2>  (use executor's world)
            if (!requirePlayer(s)) return 0;
            world = ((Player) s).getWorld();
            amount = 1;
        }

        Region spawnRegion = new Region("__spawn__", world,
                corners[0].getBlockX(), corners[1].getBlockX(),
                corners[0].getBlockY(), corners[1].getBlockY(),
                corners[0].getBlockZ(), corners[1].getBlockZ());
        spawnRegion.setChestTypeIds(Collections.singletonList(ct.getId()));
        spawnRegion.resolveChestTypes();

        int spawned = spawnLoop(ct, spawnRegion, amount);
        MessageUtil.send(s, plugin.messages.getFormatted("command.spawn_done",
                "{da_spawn}", String.valueOf(spawned), "{yeu_cau}", String.valueOf(amount), "{loai}", ct.getId()));
        return 1;
    }

    // ── /rlc spawnregion <type> <region> [amount] ────────────────────
    private int spawnRegion(CommandContext<CommandSender> ctx) {
        CommandSender s = ctx.getSource();
        if (!hasPerm(s, "randomlootchest.spawnregion")) return 0;

        String type = arg(ctx, "type");
        ChestType ct = ChestType.getChestType(type);
        if (ct == null) {
            MessageUtil.send(s, plugin.messages.getFormatted("general.chest_type_not_found", "{loai}", type));
            return 1;
        }
        String regionId = arg(ctx, "region");
        Region region = RegionConfig.getInstance().getRegion(regionId);
        if (region == null) {
            MessageUtil.send(s, plugin.messages.getFormatted("command.spawnregion_not_found", "{ten}", regionId));
            return 1;
        }

        int amount = has(ctx, "amount") ? Math.max(1, Math.min(argInt(ctx, "amount"), 100)) : 1;
        int spawned = spawnLoop(ct, region, amount);
        MessageUtil.send(s, plugin.messages.getFormatted("command.spawnregion_done",
                "{da_spawn}", String.valueOf(spawned), "{yeu_cau}", String.valueOf(amount),
                "{loai}", ct.getId(), "{region}", regionId));
        return 1;
    }

    /** Shared spawn loop for {@code spawn} and {@code spawnregion}. */
    private int spawnLoop(ChestType ct, Region region, int amount) {
        int spawned = 0;
        for (int i = 0; i < amount * 10 && spawned < amount; i++) {
            Location loc = FindAvaliableLocation.FindLocation(region);
            if (loc == null) continue;

            plugin.RandomChests.put(loc, new RandomChestInfo(ct.getKillTime(),
                    ct.getMaterial().name() + ":0", ct.getId()));
            loc.getBlock().setType(ct.getMaterial());

            BlockState state = loc.getBlock().getState();
            // Random hướng cho mọi khối có hướng (chest 4 hướng ngang, barrel/shulker 6 hướng)
            RLCUtils.applyRandomFacing(state);
            state.update();

            ct.playSpawnEffect(loc);
            ct.playSpawnSound(loc);
            spawned++;
        }
        return spawned;
    }

    private World resolveWorld(CommandSender s, String name) {
        World world = RLCUtils.getWorld(name);
        if (world == null)
            MessageUtil.send(s, plugin.messages.getFormatted("command.spawn_world_not_found", "{ten}", name));
        return world;
    }

    private static int parseInt(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 1; }
    }

    private static Location[] parseCorners(String s1, String s2) {
        try {
            String[] p1 = s1.split(",");
            String[] p2 = s2.split(",");
            if (p1.length < 3 || p2.length < 3) return null;
            return new Location[]{
                    new Location(null, Double.parseDouble(p1[0]), Double.parseDouble(p1[1]), Double.parseDouble(p1[2])),
                    new Location(null, Double.parseDouble(p2[0]), Double.parseDouble(p2[1]), Double.parseDouble(p2[2]))
            };
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ── /rlc loottable <type> ───────────────────────────────────────
    private int lootTable(CommandContext<CommandSender> ctx) {
        CommandSender s = ctx.getSource();
        if (s instanceof Player p && !hasPerm(p, "randomlootchest.loottable")) return 0;

        String type = arg(ctx, "type");
        if (type.equalsIgnoreCase("global")) {
            MessageUtil.send(s, plugin.messages.getFormatted("command.loottable_header", "{loai}", "global"));
            sendGlobalLootTable(s);
            MessageUtil.send(s, plugin.messages.get("command.loottable_footer"));
            return 1;
        }
        ChestType ct = ChestType.getChestType(type);
        if (ct == null) {
            MessageUtil.send(s, plugin.messages.getFormatted("general.chest_type_not_found", "{loai}", type));
            return 1;
        }

        MessageUtil.send(s, plugin.messages.getFormatted("command.loottable_header", "{loai}", ct.getId()));
        LootTable lt = ct.getLootTable();
        if (lt == null || lt.isEmpty()) {
            MessageUtil.send(s, plugin.messages.get("command.loottable_global_note",
                    "<gray>Loại rương này dùng global items."));
            sendGlobalLootTable(s);
        } else {
            int total = lt.totalWeight();
            for (LootTable.LootEntry entry : lt.getValidEntries()) {
                MessageUtil.send(s, plugin.messages.getFormatted("command.loottable_line",
                        "{vat_lieu}", entry.item().getType().name(),
                        "{so_luong}", amountDisplay(entry.amountMin(), entry.amountMax()),
                        "{ty_le}", chanceDisplay(entry.chance(), total)));
            }
        }
        MessageUtil.send(s, plugin.messages.get("command.loottable_footer"));
        return 1;
    }

    /** Danh sách item global (ItemDatabase root) kèm % quy đổi. */
    private void sendGlobalLootTable(CommandSender s) {
        ConfigurationSection globalSec = Main.pl.db.data.getConfigurationSection("ItemDatabase");
        if (globalSec == null) return;
        int total = 0;
        for (String k : globalSec.getKeys(false)) {
            ConfigurationSection itemSec = globalSec.getConfigurationSection(k);
            if (itemSec != null && itemSec.getItemStack("item") != null)
                total += Math.max(0, itemSec.getInt("chance", 0));
        }
        for (String k : globalSec.getKeys(false)) {
            ConfigurationSection itemSec = globalSec.getConfigurationSection(k);
            if (itemSec == null) continue;
            ItemStack item = itemSec.getItemStack("item");
            int chance = itemSec.getInt("chance", 0);
            if (item == null || chance <= 0) continue;
            int aMin = itemSec.getInt("amount-min", item.getAmount());
            int aMax = Math.max(aMin, itemSec.getInt("amount-max", item.getAmount()));
            MessageUtil.send(s, plugin.messages.getFormatted("command.loottable_line",
                    "{vat_lieu}", item.getType().name(), "{so_luong}", amountDisplay(aMin, aMax),
                    "{ty_le}", chanceDisplay(chance, total)));
        }
    }

    /** "5 (12.5%)" — weight gốc + % quy đổi trên tổng weight. */
    private static String chanceDisplay(int chance, int total) {
        if (total <= 0) return String.valueOf(chance);
        return chance + " (" + String.format(java.util.Locale.US, "%.1f%%", chance * 100.0 / total) + ")";
    }

    /** "3" hoặc "3-5" từ amount-min/max. */
    private static String amountDisplay(int aMin, int aMax) {
        int lo = Math.min(aMin, aMax), hi = Math.max(aMin, aMax);
        return lo == hi ? String.valueOf(lo) : lo + "-" + hi;
    }

    // ── /rlc roll <type> — mô phỏng 1 lần fill, không cần spawn ────
    private int rollLoot(CommandContext<CommandSender> ctx) {
        CommandSender s = ctx.getSource();
        if (!hasPerm(s, "randomlootchest.loottable")) return 0;

        String type = arg(ctx, "type");
        boolean global = type.equalsIgnoreCase("global");
        ChestType ct = global ? null : ChestType.getChestType(type);
        if (!global && ct == null) {
            MessageUtil.send(s, plugin.messages.getFormatted("general.chest_type_not_found", "{loai}", type));
            return 1;
        }

        int slots = Math.max(9, plugin.getConfig().getInt("Inventory_Slots", 27));
        java.util.List<ItemStack> items = OpenLootInventory.rollChestContents(ct, slots);
        MessageUtil.send(s, plugin.messages.getFormatted("command.roll_header",
                "{loai}", ct != null ? ct.getId() : "global"));
        if (items.isEmpty()) {
            MessageUtil.send(s, plugin.messages.get("command.roll_empty"));
        } else {
            int i = 1;
            for (ItemStack it : items)
                MessageUtil.send(s, plugin.messages.getFormatted("command.roll_line",
                        "{st}", String.valueOf(i++), "{vat_lieu}", it.getType().name(),
                        "{so_luong}", String.valueOf(it.getAmount())));
        }
        MessageUtil.send(s, plugin.messages.get("command.roll_footer"));
        return 1;
    }

    // ── /rlc loottable reset <type> ─────────────────────────────────
    private int lootTableReset(CommandContext<CommandSender> ctx) {
        CommandSender s = ctx.getSource();
        if (!hasPerm(s, "randomlootchest.loottable")) return 0;

        String type = arg(ctx, "type");
        ChestType ct = ChestType.getChestType(type);
        if (ct == null) {
            MessageUtil.send(s, plugin.messages.getFormatted("general.chest_type_not_found", "{loai}", type));
            return 1;
        }
        String resetTarget = type.equalsIgnoreCase("global") ? null : ct.getId();
        ConfigurationSection target = Main.pl.db.data.getConfigurationSection("ItemDatabase");
        if (target != null) {
            if (resetTarget == null) {
                for (String k : target.getKeys(false))
                    if (k.matches("\\d+")) target.set(k, null);
            } else {
                ConfigurationSection sub = target.getConfigurationSection(resetTarget);
                if (sub != null)
                    for (String k : sub.getKeys(false)) sub.set(k, null);
            }
            Main.pl.db.saveData();
            Main.pl.lc.loaditems();
        }
        MessageUtil.send(s, plugin.messages.getFormatted("command.loottable_reset_done",
                "<green>Đã reset loot table cho '<yellow>{loai}<green>'!", "{loai}", ct.getId()));
        return 1;
    }

    // ───────────────────────────────────────────────────────────────
    //  Help
    // ───────────────────────────────────────────────────────────────

    private void sendHelp(Player p) {
        MessageUtil.send(p, plugin.messages.get("command.help_header"));
        MessageUtil.send(p, plugin.messages.get("command.help_commands"));
        String[] keys = {
                "command.help_line_additem", "command.help_line_addchest", "command.help_line_addchesttype",
                "command.help_line_delchest", "command.help_line_delall", "command.help_line_killall",
                "command.help_line_togglebreak", "command.help_line_forcespawn", "command.help_line_rndtime",
                "command.help_line_wand", "command.help_line_set", "command.help_line_unset",
                "command.help_line_chesttypes", "command.help_line_chesttypeinfo", "command.help_line_delchesttype",
                "command.help_line_resettimer", "command.help_line_reload", "command.help_line_spawn",
                "command.help_line_spawnregion", "command.help_line_loottable", "command.help_line_regions"
        };
        for (String k : keys) MessageUtil.send(p, plugin.messages.get(k, ""));
        MessageUtil.send(p, plugin.messages.get("command.help_footer"));
    }

    private int sendConsoleHelp(CommandSender s) {
        MessageUtil.send(s, plugin.messages.get("command.console_help_header"));
        String[] lines = {
                "<yellow>/rlc delall", "<yellow>/rlc killall", "<yellow>/rlc forcespawn", "<yellow>/rlc rndtime",
                "<yellow>/rlc chesttypes", "<yellow>/rlc chesttypeinfo <italic>loại</italic>",
                "<yellow>/rlc delchesttype <italic>loại</italic>", "<yellow>/rlc resettimer [random/fixedchest] <italic>loại</italic>",
                "<yellow>/rlc reload", "<yellow>/rlc regions",
                "<yellow>/rlc spawn <italic>loại</italic> <italic>x1,y1,z1</italic> <italic>x2,y2,z2</italic> <italic>world</italic> [số lượng]",
                "<yellow>/rlc spawnregion <italic>loại</italic> <italic>regionId</italic> [số lượng]",
                "<yellow>/rlc loottable <italic>loại</italic>"
        };
        for (String l : lines) MessageUtil.send(s, "<red> " + l);
        MessageUtil.send(s, plugin.messages.get("command.console_help_footer"));
        return 1;
    }

    // ───────────────────────────────────────────────────────────────
    //  Bukkit CommandExecutor / TabCompleter → Brigadier
    // ───────────────────────────────────────────────────────────────

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Cancel any pending chest-pick (exactly like the legacy behaviour)
        if (plugin.addChestplayers.containsKey(sender)) {
            MessageUtil.send(sender, plugin.messages.get("wait.cancelled", "<red>Đã hủy <gray>fo_O"));
            plugin.addChestplayers.get(sender).cancel();
            plugin.addChestplayers.remove(sender);
        }

        if (args.length == 0) {
            if (sender instanceof Player p) sendHelp(p); else sendConsoleHelp(sender);
            return true;
        }

        String cmdLine = "rlc " + String.join(" ", args);
        try {
            dispatcher.execute(cmdLine, sender);
        } catch (CommandSyntaxException ex) {
            MessageUtil.send(sender, plugin.messages.get("general.unknown_command",
                    "<red>Lệnh không tồn tại! Sử dụng <yellow>/rlc <red>để xem trợ giúp."));
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING, "Lỗi khi thực thi /rlc: " + cmdLine, ex);
            MessageUtil.send(sender, plugin.messages.get("general.unknown_command",
                    "<red>Lệnh không tồn tại! Sử dụng <yellow>/rlc <red>để xem trợ giúp."));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String cmdLine = "rlc " + String.join(" ", args);
        try {
            var parse = dispatcher.parse(new StringReader(cmdLine), sender);
            CompletableFuture<Suggestions> future = dispatcher.getCompletionSuggestions(parse);
            return future.get().getList().stream()
                    .map(Suggestion::getText)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}