package me.Herzchen.RandomLootChest.listener;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import me.Herzchen.RandomLootChest.Main;
import me.Herzchen.RandomLootChest.database.ChestTypeConfig;
import me.Herzchen.RandomLootChest.database.LoadChances;
import me.Herzchen.RandomLootChest.model.ChestType;
import me.Herzchen.RandomLootChest.model.ChestTypeProperties;
import me.Herzchen.RandomLootChest.model.ItemEditSession;
import me.Herzchen.RandomLootChest.region.Region;
import me.Herzchen.RandomLootChest.region.RegionConfig;
import me.Herzchen.RandomLootChest.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Item editor GUI. Each player gets an isolated {@link ItemEditSession} so
 * multiple admins can edit different chest types simultaneously without
 * clobbering each other's data.
 */
public class ItemAdderGui implements Listener {
    // Dùng singleton — loaditems() phải cập nhật đúng state mà OpenLootInventory đọc (cumulative/enchRolls)
    LoadChances lc = LoadChances.instance;

    public static final String GLOBAL_KEY = "__global__";

    /** Players waiting for chat input: player -> callback */
    private final Map<Player, java.util.function.Consumer<String>> chatInputHandlers = new ConcurrentHashMap<>();

    // ── session helpers ──

    private ItemEditSession session(Player p) {
        return Main.pl.editSessions.computeIfAbsent(p, k -> new ItemEditSession());
    }

    private void endSession(Player p) {
        Main.pl.editSessions.remove(p);
    }

    private boolean hasSession(Player p) {
        return Main.pl.editSessions.containsKey(p);
    }

    // ── data access ──

    public ConfigurationSection getItemSection(String chestTypeId) {
        ConfigurationSection root = Main.pl.db.data.getConfigurationSection("ItemDatabase");
        if (root == null) return null;
        if (chestTypeId == null || chestTypeId.equals(GLOBAL_KEY)) return root;
        ConfigurationSection ctSection = root.getConfigurationSection(chestTypeId);
        if (ctSection == null) ctSection = root.createSection(chestTypeId);
        return ctSection;
    }

    /**
     * Load session data from YAML for the given chest type.
     */
    public void loadSession(ItemEditSession session, String chestTypeId) {
        Main.pl.db.loadData();
        ConfigurationSection s = getItemSection(chestTypeId);
        if (s == null) return;
        session.clearAll();
        String key = chestTypeId == null || chestTypeId.equals(GLOBAL_KEY) ? null : chestTypeId;
        session.setEditingChestType(key);
        for (int i = 0; i < 10000 && s.isConfigurationSection(String.valueOf(i)); i++) {
            ConfigurationSection es = Objects.requireNonNull(s.getConfigurationSection(String.valueOf(i)));
            ItemStack item = es.getItemStack("item");
            int chance = es.getInt("chance");
            int aMin = es.getInt("amount-min", item.getAmount());
            int aMax = es.getInt("amount-max", aMin);
            session.setItem(i, item);
            session.setChance(i, chance);
            session.setAmountRange(i, aMin, aMax);
            session.setEnchRolls(i, es.getMapList("enchant-roll"));
        }
    }

    /**
     * Save session data to YAML for the given chest type.
     */
    public void saveSession(ItemEditSession session, String chestTypeId) {
        Main.pl.db.loadData();
        ConfigurationSection target = getItemSection(chestTypeId);
        if (target == null) return;

        // When saving global (null = root section), only clear numeric keys, NOT sub-sections
        boolean isGlobal = chestTypeId == null || chestTypeId.equals(GLOBAL_KEY);
        if (isGlobal) {
            for (String k : target.getKeys(false)) {
                if (k.matches("\\d+")) target.set(k, null);
            }
        } else {
            for (String k : target.getKeys(false)) target.set(k, null);
        }

        int toset = 0;
        for (int i = 0; i < 10000; i++) {
            ItemStack item = session.getItem(i);
            if (item != null) {
                target.createSection(String.valueOf(toset));
                ConfigurationSection sec = target.getConfigurationSection(String.valueOf(toset));
                assert sec != null;
                sec.set("item", item);
                sec.set("chance", session.getChance(i));
                int aMin = session.getAmountMin(i);
                int aMax = session.getAmountMax(i);
                if (aMin != 1 || aMax != item.getAmount()) {
                    sec.set("amount-min", aMin);
                    sec.set("amount-max", aMax);
                }
                if (session.hasEnchRolls(i)) sec.set("enchant-roll", session.getEnchRolls(i));
                toset++;
            }
        }
        Main.pl.db.saveData();
        if (chestTypeId == null) lc.loaditems();

        // Auto-link lootTable: khi lưu item cho 1 chest type cụ thể, đảm bảo
        // chesttypes.yml trỏ lootTable về section ItemDatabase.<id> vừa lưu.
        // Chỉ tự điền khi key đang bỏ trống — không ghi đè path tùy chỉnh của user.
        if (chestTypeId != null && !chestTypeId.equals(GLOBAL_KEY)) {
            ChestTypeConfig ctCfg = ChestTypeConfig.getInstance();
            if (ctCfg != null) {
                String current = ctCfg.getConfig().getString(chestTypeId + ".lootTable", "");
                if (current.isEmpty()) {
                    ctCfg.getConfig().set(chestTypeId + ".lootTable", "ItemDatabase." + chestTypeId);
                    ctCfg.saveConfig();
                }
                // Cập nhật loot table runtime để không cần /rlc reload
                ChestType ct = ChestType.getChestType(chestTypeId);
                if (ct != null) {
                    String path = ctCfg.getConfig().getString(chestTypeId + ".lootTable", "");
                    if (!path.isEmpty())
                        ct.setLootTable(Main.pl.db.data.getConfigurationSection(path));
                }
            }
        }
    }

    public boolean isFull(Inventory inv) {
        for (int i = 0; i < 45; i++) if (inv.getItem(i) == null) return false;
        return true;
    }

    // ── chest type selector ──

    public void openChestTypeSelector(Player player) {
        ItemEditSession sess = session(player);
        String filter = sess.getSearchFilter();

        // Collect matching chest types
        List<ChestType> allTypes = new ArrayList<>(ChestType.getAllChestTypes().values());
        List<ChestType> filteredTypes;
        if (filter.isEmpty()) {
            filteredTypes = allTypes;
        } else {
            String lower = filter.toLowerCase();
            filteredTypes = new ArrayList<>();
            // Always include "global" as a match when filter matches "global"
            for (ChestType ct : allTypes) {
                if (ct.getId().toLowerCase().contains(lower) || ct.getDisplayName().toLowerCase().contains(lower))
                    filteredTypes.add(ct);
            }
        }

        // +1 slot for global, +1 slot for search bar
        int extraSlots = 2; // global + search
        if (!filter.isEmpty()) extraSlots++; // clear button
        int count = filteredTypes.size() + extraSlots;
        int rows = Math.max(1, (int) Math.ceil(count / 9.0));
        int size = Math.min(54, rows * 9);

        Component title = filter.isEmpty()
                ? MessageUtil.parse("<dark_gray>Chọn loại rương để chỉnh sửa")
                : MessageUtil.parse("<dark_gray>Tìm: <white>" + filter);

        Gui gui = Gui.gui()
                .title(title)
                .rows(size / 9)
                .create();

        // ── Search bar ──
        GuiItem searchItem = ItemBuilder.from(Material.COMPASS)
                .name(MessageUtil.parse("<aqua><bold>Tìm kiếm</bold></aqua>"))
                .lore(
                        MessageUtil.parse("<gray>Click để nhập từ khoá tìm kiếm"),
                        filter.isEmpty()
                                ? MessageUtil.parse("<gray>(đang hiện tất cả)")
                                : MessageUtil.parse("<gray>Đang lọc: <white>" + filter)
                )
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    player.closeInventory();
                    // Clear previous chat handlers
                    chatInputHandlers.remove(player);
                    requestChatInput(player,
                            "<yellow>Nhập từ khoá tìm kiếm (hoặc nhấn ESC để hủy):",
                            input -> {
                                String trimmed = input.trim();
                                ItemEditSession s = session(player);
                                if (trimmed.equalsIgnoreCase("clear")) {
                                    s.setSearchFilter("");
                                } else {
                                    s.setSearchFilter(trimmed);
                                }
                                openChestTypeSelector(player);
                            });
                });
        gui.addItem(searchItem);

        // ── Global Items ──
        GuiItem globalItem = ItemBuilder.from(Material.CHEST)
                .name(MessageUtil.parse("<gold><bold>Global Items</bold>"))
                .lore(
                        MessageUtil.parse("<gray>Items dùng chung cho tất cả"),
                        MessageUtil.parse("<gray>loại rương không có loot table riêng")
                )
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    ItemEditSession s = session(player);
                    s.setEditingChestType(null);
                    MessageUtil.send(player, Main.pl.messages.get("gui.selected_global", "<green>Đã chọn: <white>Global Items"));
                    player.closeInventory();
                    loadSession(s, GLOBAL_KEY);
                    s.setCurrentPage(1);
                    openPage(player);
                });
        gui.addItem(globalItem);

        // ── Filtered chest types ──
        for (ChestType ct : filteredTypes) {
            String ctId = ct.getId();
            GuiItem ctItem = ItemBuilder.from(ct.getMaterial())
                    .name(MessageUtil.parse("<gold><bold>" + ct.getDisplayName() + "</bold>"))
                    .lore(
                            MessageUtil.parse("<gray>ID: <white>" + ct.getId()),
                            MessageUtil.parse("<gray>Click để chỉnh sửa items cho loại rương này")
                    )
                    .asGuiItem(event -> {
                        event.setCancelled(true);
                        ItemEditSession s = session(player);
                        s.setEditingChestType(ctId);
                        MessageUtil.send(player, Main.pl.messages.getFormatted("gui.selected_type", "{ten}", ct.getDisplayName()));
                        player.closeInventory();
                        loadSession(s, ctId);
                        s.setCurrentPage(1);
                        openPage(player);
                    });
            gui.addItem(ctItem);
        }

        // ── Clear filter button (only visible when filter is active) ──
        if (!filter.isEmpty()) {
            GuiItem clearItem = ItemBuilder.from(Material.BARRIER)
                    .name(MessageUtil.parse("<red><bold>Xoá lọc</bold></red>"))
                    .lore(MessageUtil.parse("<gray>Hiện tất cả loại rương"))
                    .asGuiItem(event -> {
                        event.setCancelled(true);
                        ItemEditSession s = session(player);
                        s.setSearchFilter("");
                        player.closeInventory();
                        openChestTypeSelector(player);
                    });
            gui.addItem(clearItem);
        }

        gui.open(player);
    }

    // ── item editor page ──

    public void openPage(Player player) {
        ItemEditSession session = session(player);
        int page = session.getCurrentPage();
        String ctId = session.getEditingChestType();

        Component title;
        if (ctId == null || ctId.equals(GLOBAL_KEY)) {
            title = MessageUtil.parse("<dark_gray>Global - Trang: <white>" + page + "/5");
        } else {
            ChestType ct = ChestType.getChestType(ctId);
            String displayName = (ct != null) ? ct.getDisplayName() : ctId;
            title = MessageUtil.parse("<dark_gray>" + displayName + " - Trang: <white>" + page + "/5");
        }

        Gui gui = Gui.gui()
                .title(title)
                .rows(6)
                .create();

        // Fill items (slots 0-44)
        int fn = page * 45 - 45, mn = page * 45, sc = 0;
        for (int g = fn; g < mn; g++) {
            final int slot = sc;
            final int itemId = g;
            if (session.hasItem(itemId)) {
                ItemStack rawItem = session.getItem(itemId).clone();
                applyEditorLore(rawItem, session, itemId);

                GuiItem gi = ItemBuilder.from(rawItem).asGuiItem(event -> {
                    if (event.getClick() == ClickType.RIGHT || event.getClick() == ClickType.SHIFT_RIGHT) {
                        // Right click → edit chance
                        event.setCancelled(true);
                        saveCurrentPage(player, gui.getInventory(), page);
                        session.setKeepOnClose(true);    // don't let closeGuiAction end the session
                        player.closeInventory();
                        session.setKeepOnClose(false);
                        openChanceEditor(player, itemId);
                    } else if (event.isShiftClick() && event.getClick().isLeftClick()) {
                        // Shift + Left click → delete item
                        event.setCancelled(true);
                        session.removeItem(itemId);
                        gui.getInventory().setItem(slot, null);
                        MessageUtil.send(player, "<gray>Đã xoá item khỏi danh sách.");
                    }
                    // Plain left click: allow free movement (not cancelled)
                });
                gui.setItem(slot, gi);
            }
            sc++;
        }

        // Decorative panes in bottom row
        GuiItem pane = ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE)
                .name(Component.text(" "))
                .asGuiItem(event -> event.setCancelled(true));
        for (int s : new int[]{46, 47, 48, 50, 52}) gui.setItem(s, pane);

        // Settings button (slot 51) — mở GUI cài đặt chest type
        GuiItem settings = ItemBuilder.from(Material.COMPARATOR)
                .name(MessageUtil.parse("<gold>Cài đặt loại rương"))
                .lore(
                        MessageUtil.parse("<gray>Số item (itemCountMin/Max), uniquePicks,"),
                        MessageUtil.parse("<gray>mergeStacks, nothingChance, vanillaTable."),
                        Component.empty(),
                        MessageUtil.parse("<yellow>Click để mở")
                )
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    ItemEditSession sess = session(player);
                    if (sess.isGlobal()) {
                        MessageUtil.send(player, Main.pl.messages.get("gui.settings_global_denied",
                                "<red>Cài đặt loại rương chỉ áp dụng cho chest type riêng — không áp dụng cho global."));
                        return;
                    }
                    saveCurrentPage(player, gui.getInventory(), page);
                    settingsDrafts.remove(player); // draft mới từ props hiện tại
                    sess.setKeepOnClose(true);
                    player.closeInventory();
                    sess.setKeepOnClose(false);
                    openChestTypeSettings(player);
                });
        gui.setItem(51, settings);

        // Info paper (slot 49)
        GuiItem info = ItemBuilder.from(Material.PAPER)
                .name(MessageUtil.parse("<gold>Thông tin:"))
                .lore(
                        MessageUtil.parse("<green>Thả item vào đây để thêm vào rương."),
                        MessageUtil.parse("<green>Kéo thả item tự do giữa các ô."),
                        MessageUtil.parse("<green>Nhấp chuột phải vào item để"),
                        MessageUtil.parse("<green>chỉnh sửa tỷ lệ."),
                        MessageUtil.parse("<gold>Shift+Click trái: xoá item"),
                        MessageUtil.parse("<red>Nếu không chỉnh tỷ lệ, mặc định là 50.")
                )
                .asGuiItem(event -> event.setCancelled(true));
        gui.setItem(49, info);

        // Back button (slot 45)
        GuiItem back = ItemBuilder.from(Material.ARROW)
                .name(MessageUtil.parse("<red>Quay lại"))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    saveCurrentPage(player, gui.getInventory(), page);
                    if (page > 1) {
                        session.setKeepOnClose(true);
                        player.closeInventory();
                        session.setKeepOnClose(false);
                        session.setCurrentPage(page - 1);
                        openPage(player);
                    } else {
                        // First page → go back to chest type selector
                        String key = session.getStorageKey();
                        saveSession(session, key);
                        endSession(player);
                        player.closeInventory();
                        openChestTypeSelector(player);
                    }
                });
        gui.setItem(45, back);

        // Next page button (slot 53)
        GuiItem next = ItemBuilder.from(Material.ARROW)
                .name(MessageUtil.parse("<red>Trang sau"))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    if (page >= 5) return;
                    if (isFull(gui.getInventory())) {
                        saveCurrentPage(player, gui.getInventory(), page);
                        session.setKeepOnClose(true);
                        player.closeInventory();
                        session.setKeepOnClose(false);
                        session.setCurrentPage(page + 1);
                        openPage(player);
                    } else {
                        MessageUtil.send(player, Main.pl.messages.get("gui.page_full", "<red>Vui lòng điền đầy trang này trước khi chuyển sang trang tiếp theo."));
                    }
                });
        gui.setItem(53, next);

        // Close action: save items (unless navigating to chance editor / another page)
        gui.setCloseGuiAction(event -> {
            Player p = (Player) event.getPlayer();
            ItemEditSession sess = Main.pl.editSessions.get(p);
            if (sess == null) return;
            if (sess.isKeepOnClose()) return; // navigation — don't end session
            saveCurrentPage(p, gui.getInventory(), page);
            saveSession(sess, sess.getStorageKey());
            MessageUtil.send(p, Main.pl.messages.get("gui.items_updated", "<green>Danh sách vật phẩm đã được cập nhật!"));
            endSession(p);
        });

        gui.open(player);
    }

    private void saveCurrentPage(Player player, Inventory inv, int page) {
        ItemEditSession session = session(player);
        int mf = page * 45 - 45, ml = page * 45;
        // Only remove items, NOT chances — preserve chance values that were set via chance editor
        for (int i = mf; i < ml; i++) session.removeItemOnly(i);
        for (int i = mf, sc = 0; i < ml; i++, sc++) {
            ItemStack it = inv.getItem(sc);
            if (it != null && it.getType() != Material.AIR) {
                session.setItem(i, stripEditorLore(it));
            }
        }
    }

    // ── chest type settings GUI ──

    /** Draft cài đặt — sống qua GUI close để chat input (vanillaTable) vẫn cập nhật được. */
    private final Map<Player, SettingsDraft> settingsDrafts = new ConcurrentHashMap<>();

    private static final class SettingsDraft {
        Integer itemCountMin, itemCountMax;   // null = chưa set (fallback ItemAmountToAdd)
        Integer nothingChance;                // null = 0
        boolean uniquePicks, mergeStacks;
        String vanillaTable;                  // null/empty = tắt
    }

    private static int currentGlobalAmount() {
        return Math.max(1, Main.pl.getConfig().getInt("ItemAmountToAdd", 5));
    }

    public void openChestTypeSettings(Player player) {
        ItemEditSession session = session(player);
        String ctId = session.getEditingChestType();
        ChestType ct = (ctId == null || ctId.equals(GLOBAL_KEY)) ? null : ChestType.getChestType(ctId);
        if (ct == null) {
            MessageUtil.send(player, Main.pl.messages.get("gui.settings_global_denied",
                    "<red>Cài đặt loại rương chỉ áp dụng cho chest type riêng — không áp dụng cho global."));
            endSession(player);
            player.closeInventory();
            openChestTypeSelector(player);
            return;
        }

        // Draft: tạo mới từ props hiện tại; giữ nguyên nếu đang quay lại từ chat input
        SettingsDraft draft = settingsDrafts.computeIfAbsent(player, p -> {
            SettingsDraft d = new SettingsDraft();
            ChestTypeProperties props = ct.getProperties();
            d.itemCountMin = props.itemCountMin();
            d.itemCountMax = props.itemCountMax();
            d.nothingChance = props.nothingChance();
            d.uniquePicks = props.uniquePicks();
            d.mergeStacks = props.mergeStacks();
            d.vanillaTable = props.vanillaTable();
            return d;
        });

        Gui gui = Gui.gui()
                .title(MessageUtil.parse("<dark_gray>Cài đặt: <white>" + ct.getDisplayName()))
                .rows(5)
                .disableAllInteractions()
                .create();

        // Row 2 — itemCountMin + uniquePicks
        gui.setItem(10, settingsIntDisplay(Material.PAPER, "<gold>Số item tối thiểu (itemCountMin)",
                draft.itemCountMin,
                "Chỉ set Min → số item cố định bằng Min.",
                "Set cả Min+Max → random trong khoảng.",
                "Không set gì → dùng ItemAmountToAdd (" + currentGlobalAmount() + ")."));
        gui.setItem(11, settingsIntButton(player, gui, draft, "min", +1));
        gui.setItem(12, settingsIntButton(player, gui, draft, "min", +5));
        gui.setItem(14, settingsIntButton(player, gui, draft, "min", -1));
        gui.setItem(15, settingsIntButton(player, gui, draft, "min", -5));
        gui.setItem(13, settingsToggle(player, gui, draft, "uniquePicks",
                "<gold>uniquePicks", "Không cho 2 item giống nhau trong cùng 1 rương."));

        // Row 3 — itemCountMax + vanillaTable
        gui.setItem(19, settingsIntDisplay(Material.PAPER, "<gold>Số item tối đa (itemCountMax)",
                draft.itemCountMax,
                "Set cả Min+Max → random trong khoảng.",
                "Chỉ set Max → random trong [1, Max].",
                "Không set gì → dùng ItemAmountToAdd (" + currentGlobalAmount() + ")."));
        gui.setItem(20, settingsIntButton(player, gui, draft, "max", +1));
        gui.setItem(21, settingsIntButton(player, gui, draft, "max", +5));
        gui.setItem(23, settingsIntButton(player, gui, draft, "max", -1));
        gui.setItem(24, settingsIntButton(player, gui, draft, "max", -5));
        gui.setItem(22, vanillaTableButton(player, draft));

        // Row 4 — nothingChance + mergeStacks
        gui.setItem(28, nothingChanceDisplay(draft));
        gui.setItem(29, settingsNothingButton(player, gui, draft, +5));
        gui.setItem(30, settingsNothingButton(player, gui, draft, +10));
        gui.setItem(32, settingsNothingButton(player, gui, draft, -5));
        gui.setItem(33, settingsNothingButton(player, gui, draft, -10));
        gui.setItem(31, settingsToggle(player, gui, draft, "mergeStacks",
                "<gold>mergeStacks", "Gộp các pick cùng loại thành 1 stack."));

        // Panes cho các slot trống
        GuiItem pane = ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE)
                .name(Component.text(" "))
                .asGuiItem(event -> event.setCancelled(true));
        for (int s : new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 16, 17, 18, 25, 26, 27, 34, 35, 37, 38, 39, 41, 42, 43, 44})
            gui.setItem(s, pane);

        GuiItem info = ItemBuilder.from(Material.PAPER)
                .name(MessageUtil.parse("<gold>Thông tin:"))
                .lore(
                        MessageUtil.parse("<gray>itemCountMin/Max: số item trong rương."),
                        MessageUtil.parse("<gray>Không set → dùng ItemAmountToAdd (" + currentGlobalAmount() + ")."),
                        MessageUtil.parse("<gray>vanillaTable: roll thêm loot vanilla,"),
                        MessageUtil.parse("<gray>ra ngoài số itemCountMin/Max."),
                        Component.empty(),
                        MessageUtil.parse("<green>Mọi thay đổi được lưu khi rời GUI.")
                )
                .asGuiItem(event -> event.setCancelled(true));
        gui.setItem(40, info);

        GuiItem back = ItemBuilder.from(Material.ARROW)
                .name(MessageUtil.parse("<red>Quay lại <gray>(lưu cài đặt)"))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    saveSettings(player, draft, session);
                    settingsDrafts.remove(player);
                    session.setKeepOnClose(true);
                    player.closeInventory();
                    session.setKeepOnClose(false);
                    openPage(player);
                });
        gui.setItem(36, back);

        gui.setCloseGuiAction(event -> {
            Player p = (Player) event.getPlayer();
            if (session.isKeepOnClose()) return; // navigation — không save/end
            saveSettings(p, draft, session);
            saveSession(session, session.getStorageKey()); // flush luôn items đang sửa dở
            settingsDrafts.remove(p);
            endSession(p);
        });

        gui.open(player);
    }

    // ── editor lore (hiển thị chance % / amount / enchant roll — KHÔNG lưu vào database) ──

    private static final net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer PLAIN =
            net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText();

    /** Gắn lore hiển thị vào bản clone trong GUI editor; được strip trước khi lưu. */
    private void applyEditorLore(ItemStack stack, ItemEditSession session, int id) {
        java.util.List<Component> lore = new ArrayList<>();
        if (stack.hasItemMeta() && stack.getItemMeta().lore() != null)
            lore.addAll(stack.getItemMeta().lore());
        lore.add(MessageUtil.parse("<dark_gray>──────────"));
        int total = 0;
        for (Map.Entry<Integer, Integer> e : session.getChances().entrySet())
            if (session.hasItem(e.getKey())) total += e.getValue();
        int chance = session.getChance(id);
        String pct = total > 0
                ? " <dark_gray>(" + String.format(java.util.Locale.US, "%.1f%%", chance * 100.0 / total) + ")"
                : "";
        lore.add(MessageUtil.parse("<gray>Chance: <white>" + chance + pct));
        lore.add(MessageUtil.parse("<gray>Số lượng: <white>" + session.getAmountDisplay(id)));
        if (session.hasEnchRolls(id))
            lore.add(MessageUtil.parse("<gray>Enchant roll: <white>" + session.getEnchRolls(id).size() + " hiệu ứng"));
        var meta = stack.getItemMeta();
        meta.lore(lore);
        stack.setItemMeta(meta);
    }

    /** Loại bỏ lore do editor gắn vào (Chance/Số lượng/Enchant roll/đường kẻ) trước khi lưu vào database.yml. */
    private ItemStack stripEditorLore(ItemStack stack) {
        if (!stack.hasItemMeta() || stack.getItemMeta().lore() == null) return stack;
        var meta = stack.getItemMeta();
        java.util.List<Component> cleaned = new ArrayList<>();
        for (Component line : meta.lore()) {
            String t = PLAIN.serialize(line).trim();
            if (t.startsWith("Chance:") || t.startsWith("Số lượng:") || t.startsWith("Enchant roll:")
                    || t.startsWith("───")) continue;
            cleaned.add(line);
        }
        meta.lore(cleaned.isEmpty() ? null : cleaned);
        ItemStack out = stack.clone();
        out.setItemMeta(meta);
        return out;
    }

    /** Ghi draft vào chesttypes.yml, re-register chest types, re-resolve regions (fix stale object). */
    private void saveSettings(Player player, SettingsDraft draft, ItemEditSession session) {
        String ctId = session.getEditingChestType();
        if (ctId == null || ctId.equals(GLOBAL_KEY)) return;
        ChestType ct = ChestType.getChestType(ctId);
        if (ct == null) return;

        ChestTypeProperties old = ct.getProperties();
        ChestTypeProperties props = new ChestTypeProperties(
                old.material(), old.displayName(),
                old.spawnTimeMin(), old.spawnTimeMax(), old.killTime(),
                old.spawnEffect(), old.spawnSound(),
                old.openSound(), old.killEffect(), old.killSound(),
                old.lootTablePath(),
                draft.itemCountMin, draft.itemCountMax, draft.uniquePicks,
                draft.vanillaTable, draft.nothingChance, draft.mergeStacks);

        ChestTypeConfig ctCfg = ChestTypeConfig.getInstance();
        if (ctCfg != null) {
            ctCfg.writeProperties(ctCfg.getConfig(), ctId, props);
            ctCfg.saveConfig();
            ctCfg.loadChestTypes(); // re-register + re-link lootTable
            // Region giữ reference ChestType object → re-resolve để không dùng object cũ
            for (Region region : RegionConfig.getInstance().getAllRegions())
                region.resolveChestTypes();
        }
        if (draft.vanillaTable != null && !draft.vanillaTable.isEmpty()
                && ChestType.resolveVanillaLoot(draft.vanillaTable) == null) {
            MessageUtil.send(player, Main.pl.messages.getFormatted("gui.settings_vanilla_invalid",
                    "<red>vanillaTable '<white>{bang}<red>' không tìm thấy trên server — sẽ bị bỏ qua!",
                    "{bang}", draft.vanillaTable));
        }
        MessageUtil.send(player, Main.pl.messages.getFormatted("gui.settings_saved",
                "<green>Đã lưu cài đặt cho loại rương '<yellow>{loai}<green>'!", "{loai}", ctId));
    }

    private void refreshSettingsDisplays(Gui gui, Player player, SettingsDraft draft) {
        gui.updateItem(10, settingsIntDisplay(Material.PAPER, "<gold>Số item tối thiểu (itemCountMin)",
                draft.itemCountMin,
                "Chỉ set Min → số item cố định bằng Min.",
                "Set cả Min+Max → random trong khoảng.",
                "Không set gì → dùng ItemAmountToAdd (" + currentGlobalAmount() + ")."));
        gui.updateItem(19, settingsIntDisplay(Material.PAPER, "<gold>Số item tối đa (itemCountMax)",
                draft.itemCountMax,
                "Set cả Min+Max → random trong khoảng.",
                "Chỉ set Max → random trong [1, Max].",
                "Không set gì → dùng ItemAmountToAdd (" + currentGlobalAmount() + ")."));
        gui.updateItem(13, settingsToggle(player, gui, draft, "uniquePicks",
                "<gold>uniquePicks", "Không cho 2 item giống nhau trong cùng 1 rương."));
        gui.updateItem(31, settingsToggle(player, gui, draft, "mergeStacks",
                "<gold>mergeStacks", "Gộp các pick cùng loại thành 1 stack."));
        gui.updateItem(28, nothingChanceDisplay(draft));
        gui.updateItem(22, vanillaTableButton(player, draft));
    }

    private GuiItem settingsIntDisplay(Material mat, String baseName, Integer value, String... loreLines) {
        String val = (value == null) ? "<gray>chưa set" : "<white>" + value;
        java.util.List<Component> lore = new ArrayList<>();
        for (String l : loreLines) lore.add(MessageUtil.parse("<gray>" + l));
        lore.add(Component.empty());
        lore.add(MessageUtil.parse("<gray>Dùng nút <green>xanh <gray>/<red>đỏ <gray>bên cạnh để chỉnh."));
        return ItemBuilder.from(mat)
                .name(MessageUtil.parse(baseName + ": " + val))
                .lore(lore)
                .asGuiItem(event -> event.setCancelled(true));
    }

    private GuiItem settingsIntButton(Player player, Gui gui, SettingsDraft draft, String which, int delta) {
        boolean add = delta > 0;
        String label = (add ? "<green>+" : "<red>-") + Math.abs(delta);
        return ItemBuilder.from(add ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE)
                .name(MessageUtil.parse(label))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    if ("min".equals(which)) {
                        int cur = (draft.itemCountMin == null) ? currentGlobalAmount() : draft.itemCountMin;
                        draft.itemCountMin = Math.max(1, cur + delta);
                        if (draft.itemCountMax != null && draft.itemCountMax < draft.itemCountMin)
                            draft.itemCountMax = draft.itemCountMin;
                    } else {
                        int cur = (draft.itemCountMax == null) ? currentGlobalAmount() : draft.itemCountMax;
                        draft.itemCountMax = Math.max(1, cur + delta);
                        if (draft.itemCountMin != null && draft.itemCountMin > draft.itemCountMax)
                            draft.itemCountMin = draft.itemCountMax;
                    }
                    refreshSettingsDisplays(gui, player, draft);
                });
    }

    private GuiItem settingsToggle(Player player, Gui gui, SettingsDraft draft, String which, String name, String descLine) {
        boolean on = "uniquePicks".equals(which) ? draft.uniquePicks : draft.mergeStacks;
        return ItemBuilder.from(on ? Material.LIME_DYE : Material.GRAY_DYE)
                .name(MessageUtil.parse(name + ": " + (on ? "<green>BẬT" : "<red>TẮT")))
                .lore(
                        MessageUtil.parse("<gray>" + descLine),
                        Component.empty(),
                        MessageUtil.parse("<gray>Click để bật/tắt")
                )
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    if ("uniquePicks".equals(which)) draft.uniquePicks = !draft.uniquePicks;
                    else draft.mergeStacks = !draft.mergeStacks;
                    refreshSettingsDisplays(gui, player, draft);
                });
    }

    private GuiItem nothingChanceDisplay(SettingsDraft draft) {
        int nc = (draft.nothingChance == null) ? 0 : draft.nothingChance;
        return ItemBuilder.from(Material.PAPER)
                .name(MessageUtil.parse("<gold>nothingChance: <white>" + nc + "%"))
                .lore(
                        MessageUtil.parse("<gray>% mỗi lần pick item bị <white>bỏ qua<gray>,"),
                        MessageUtil.parse("<gray>tạo rương thưa — không lúc nào cũng đầy."),
                        Component.empty(),
                        MessageUtil.parse(progressBar(nc))
                )
                .asGuiItem(event -> event.setCancelled(true));
    }

    private GuiItem settingsNothingButton(Player player, Gui gui, SettingsDraft draft, int delta) {
        boolean add = delta > 0;
        return ItemBuilder.from(add ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE)
                .name(MessageUtil.parse((add ? "<green>+" : "<red>-") + Math.abs(delta) + "%"))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    int cur = (draft.nothingChance == null) ? 0 : draft.nothingChance;
                    draft.nothingChance = Math.max(0, Math.min(100, cur + delta));
                    refreshSettingsDisplays(gui, player, draft);
                });
    }

    private GuiItem vanillaTableButton(Player player, SettingsDraft draft) {
        String cur = (draft.vanillaTable == null || draft.vanillaTable.isEmpty()) ? null : draft.vanillaTable;
        return ItemBuilder.from(Material.BOOK)
                .name(MessageUtil.parse("<gold>vanillaTable: " + (cur == null ? "<gray>TẮT" : "<white>" + cur)))
                .lore(
                        MessageUtil.parse("<gray>Roll thêm item từ loot table vanilla của server."),
                        MessageUtil.parse("<gray>Ví dụ: <white>chests/simple_dungeon<gray>, <white>chests/buried_treasure"),
                        MessageUtil.parse("<gray>Item vanilla ra ngoài số itemCountMin/Max."),
                        Component.empty(),
                        MessageUtil.parse("<yellow>Click để nhập (gõ <white>none<yellow> để tắt)")
                )
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    session(player).setKeepOnClose(true);
                    player.closeInventory();
                    session(player).setKeepOnClose(false);
                    requestChatInput(player,
                            "<yellow>Nhập vanillaTable (vd: chests/simple_dungeon), gõ <white>none<yellow> để tắt:",
                            input -> {
                                String v = input.trim();
                                if (v.equalsIgnoreCase("none") || v.isEmpty()) {
                                    draft.vanillaTable = null;
                                    MessageUtil.send(player, Main.pl.messages.get("gui.settings_vanilla_cleared",
                                            "<green>Đã tắt vanillaTable."));
                                } else {
                                    if (ChestType.resolveVanillaLoot(v) == null)
                                        MessageUtil.send(player, Main.pl.messages.getFormatted("gui.settings_vanilla_invalid",
                                                "<red>vanillaTable '<white>{bang}<red>' không tìm thấy trên server — sẽ bị bỏ qua!",
                                                "{bang}", v));
                                    draft.vanillaTable = v;
                                }
                                openChestTypeSettings(player);
                            });
                });
    }

    // ── chance editor ──

    public void openChanceEditor(Player player, int id) {
        ItemEditSession session = session(player);
        session.setEditingId(id);
        session.setLastPageNo(session.getCurrentPage());

        ItemStack item = session.getItem(id);
        if (item == null) return;
        item = item.clone();
        int chanceValue = session.getChance(id);

        Gui gui = Gui.gui()
                .title(MessageUtil.parse("<dark_gray>Chỉnh sửa tỷ lệ"))
                .rows(3)
                .disableAllInteractions()
                .create();

        // Decorations (NOT including slot 13 and 22; those are chance/amount displays)
        GuiItem pane = ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).asGuiItem(event -> event.setCancelled(true));
        List<Integer> deco = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 19, 20, 21, 23, 24, 25, 26);
        gui.setItem(deco, pane);

        // Current chance display (slot 13)
        GuiItem currentDisplay = buildChanceDisplay(session, id, item, chanceValue, player);
        gui.setItem(13, currentDisplay);

        // Amount display (slot 22) — clickable to edit via chat
        GuiItem amountDisplay = buildAmountDisplay(session, id, item, player);
        gui.setItem(22, amountDisplay);

        // Add buttons
        gui.setItem(10, addChanceButton(1, id, gui, player));
        gui.setItem(11, addChanceButton(10, id, gui, player));
        gui.setItem(12, addChanceButton(50, id, gui, player));

        // Remove buttons
        gui.setItem(14, removeChanceButton(1, id, gui, player));
        gui.setItem(15, removeChanceButton(10, id, gui, player));
        gui.setItem(16, removeChanceButton(50, id, gui, player));

        // Back button
        GuiItem back = ItemBuilder.from(Material.ARROW)
                .name(MessageUtil.parse("<red>Quay lại"))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    player.closeInventory();
                    session.setCurrentPage(session.getLastPageNo());
                    openPage(player);
                });
        gui.setItem(18, back);

        // Close action
        String chestTypeId = session.getStorageKey();
        gui.setCloseGuiAction(event -> {
            Player p = (Player) event.getPlayer();
            session.setEditingId(-1);
            if (chestTypeId != null) {
                saveSession(session, chestTypeId);
            }
        });

        gui.open(player);
    }

    /** Build the amount display (slot 9). Click to edit via chat: type "5" or "2-5". */
    private GuiItem buildAmountDisplay(ItemEditSession session, int id, ItemStack item, Player player) {
        String display = session.getAmountDisplay(id);
        return ItemBuilder.from(Material.REPEATER)
                .name(MessageUtil.parse("<gold>Số lượng: <white>" + display))
                .lore(
                        MessageUtil.parse("<gray>Click để nhập số lượng"),
                        MessageUtil.parse("<gray>qua chat."),
                        Component.empty(),
                        MessageUtil.parse("<gray>Nhập <white>5<gray> → số lượng cố định là 5"),
                        MessageUtil.parse("<gray>Nhập <white>2-5<gray> → số lượng ngẫu nhiên 2-5"),
                        MessageUtil.parse("<gray>(tự động đảo nếu nhập 5-2)")
                )
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    player.closeInventory();
                    requestChatInput(player,
                            "<yellow>Nhập số lượng (vd: 5) hoặc khoảng (vd: 2-5): <white>(ESC để hủy)",
                            input -> {
                                String trimmed = input.trim();
                                if (trimmed.contains("-")) {
                                    // Range
                                    String[] parts = trimmed.split("-", 2);
                                    try {
                                        int a = Integer.parseInt(parts[0].trim());
                                        int b = Integer.parseInt(parts[1].trim());
                                        if (a < 1 || b < 1 || a > 99 || b > 99) {
                                            MessageUtil.send(player, Main.pl.messages.get("gui.amount_out_of_range", "<red>Số lượng phải từ 1 đến 99!"));
                                        } else {
                                            session.setAmountRange(id, a, b);
                                            MessageUtil.send(player, "<green>Đã đặt số lượng: <white>" + session.getAmountDisplay(id));
                                        }
                                    } catch (NumberFormatException e) {
                                        MessageUtil.send(player, "<red>Định dạng không hợp lệ! Vui lòng nhập '5' hoặc '2-5'.");
                                    }
                                } else {
                                    // Fixed amount
                                    try {
                                        int val = Integer.parseInt(trimmed);
                                        if (val < 1 || val > 99) {
                                            MessageUtil.send(player, Main.pl.messages.get("gui.amount_out_of_range", "<red>Số lượng phải từ 1 đến 99!"));
                                        } else {
                                            session.setFixedAmount(id, val);
                                            MessageUtil.send(player, Main.pl.messages.getFormatted("gui.amount_set", "{so_luong}", session.getAmountDisplay(id)));
                                        }
                                    } catch (NumberFormatException e) {
                                        MessageUtil.send(player, "<red>Định dạng không hợp lệ! Vui lòng nhập '5' hoặc '2-5'.");
                                    }
                                }
                                openChanceEditor(player, id);
                            });
                });
    }

    /** Build the chance-display icon (slot 13). Click to edit chance via chat. */
    private GuiItem buildChanceDisplay(ItemEditSession session, int id, ItemStack item, int chance, Player player) {
        String bar = progressBar(chance);
        return ItemBuilder.from(item)
                .name(MessageUtil.parse("<gold>Tỷ lệ: <red>" + chance))
                .lore(
                        MessageUtil.parse("<gray>Click để nhập tỷ lệ qua chat."),
                        Component.empty(),
                        MessageUtil.parse(bar)
                )
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    player.closeInventory();
                    requestChatInput(player,
                            Main.pl.messages.get("gui.chance_prompt", "<yellow>Nhập tỷ lệ mới (1-100) cho vật phẩm này: <white>(ESC để hủy)"),
                            input -> {
                                try {
                                    int val = Integer.parseInt(input.trim());
                                    if (val < 1 || val > 100) {
                                        MessageUtil.send(player, Main.pl.messages.get("gui.chance_out_of_range", "<red>Tỷ lệ phải từ 1 đến 100!"));
                                    } else {
                                        session.setChance(id, val);
                                        MessageUtil.send(player, Main.pl.messages.getFormatted("gui.chance_set", "{ty_le}", String.valueOf(val)));
                                    }
                                } catch (NumberFormatException e) {
                                    MessageUtil.send(player, Main.pl.messages.get("gui.chance_invalid_number", "<red>Số không hợp lệ! Vui lòng nhập lại."));
                                }
                                openChanceEditor(player, id);
                            });
                });
    }

    /** Format a progress-bar string: {@code <dark_green>[██████░░░░] 60%}. */
    private static String progressBar(int chance) {
        int filled = Math.max(0, Math.min(10, chance / 10));
        int empty = 10 - filled;
        return "<dark_green>[<green>" + "█".repeat(filled) + "<gray>" + "░".repeat(empty) + "<dark_green>] <white>" + chance + "%";
    }

    private GuiItem addChanceButton(int amount, int id, Gui gui, Player player) {
        ItemEditSession session = session(player);
        return ItemBuilder.from(Material.GREEN_STAINED_GLASS_PANE)
                .name(MessageUtil.parse("<green>Thêm " + amount))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    int cur = session.getChance(id);
                    if (cur + amount > 100) {
                        MessageUtil.send(player, Main.pl.messages.get("gui.chance_too_high", "<red>Tỷ lệ không thể lớn hơn 100!"));
                    } else {
                        session.addChance(id, amount);
                        gui.updateItem(13, buildChanceDisplay(session, id, session.getItem(id).clone(), session.getChance(id), player));
                    }
                });
    }

    private GuiItem removeChanceButton(int amount, int id, Gui gui, Player player) {
        ItemEditSession session = session(player);
        return ItemBuilder.from(Material.RED_STAINED_GLASS_PANE)
                .name(MessageUtil.parse("<red>Xoá " + amount))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    int cur = session.getChance(id);
                    if (cur - amount < 1) {
                        MessageUtil.send(player, Main.pl.messages.get("gui.chance_too_low", "<red>Tỷ lệ không thể nhỏ hơn 1!"));
                    } else {
                        session.addChance(id, -amount);
                        gui.updateItem(13, buildChanceDisplay(session, id, session.getItem(id).clone(), session.getChance(id), player));
                    }
                });
    }

    // ── public entry points ──

    public void openGuiDirect(Player player, String chestTypeId) {
        ItemEditSession session = session(player);
        String ctId = (chestTypeId == null) ? GLOBAL_KEY : chestTypeId;
        session.setEditingChestType(chestTypeId);
        loadSession(session, ctId);
        session.setCurrentPage(1);
        openPage(player);
    }

    public void openGui(Player player) {
        openChestTypeSelector(player);
    }

    // ── chat input ──

    public void requestChatInput(Player player, String prompt, java.util.function.Consumer<String> callback) {
        chatInputHandlers.put(player, callback);
        MessageUtil.send(player, prompt);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        if (chatInputHandlers.containsKey(player)) {
            e.setCancelled(true);
            String msg = e.getMessage();
            if (msg.equalsIgnoreCase("cancel") || msg.equalsIgnoreCase("hủy")) {
                chatInputHandlers.remove(player);
                MessageUtil.send(player, Main.pl.messages.get("gui.cancelled", "<yellow>Đã hủy."));
                return;
            }
            java.util.function.Consumer<String> handler = chatInputHandlers.remove(player);
            if (handler != null) {
                org.bukkit.Bukkit.getScheduler().runTask(Main.pl, () -> handler.accept(msg));
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        chatInputHandlers.remove(p);
        settingsDrafts.remove(p);
        endSession(p);
    }

    // ── legacy helpers (kept for binary compat; delegate to session) ──

    @Deprecated
    public void addOnMap(ItemStack item) {
        // No-op; items are now managed per-session.
    }

    public void loadItems() { loadItems(null); }

    @Deprecated
    public void loadItems(String chestTypeId) {
        // Legacy — called from LoadChances / old code.
        // Ignored; real loading happens via loadSession().
    }

    public void saveItems() { saveItems(null); }

    @Deprecated
    public void saveItems(String chestTypeId) {
        // Legacy — ignored; real saving happens via saveSession().
    }

    public void addchance(int id, int v) {
        // Legacy — ignored.
    }

    public void remove(int id, int v) {
        // Legacy — ignored.
    }

    @Deprecated
    public int chancetoaddorremove(ItemStack item) {
        return Integer.parseInt(ChatColor.stripColor(item.getItemMeta().getDisplayName()).replace("Thêm", "").replace("Xóa", "").replace(" ", ""));
    }

    @Deprecated
    public void save(Inventory inv, int page) {
        // Legacy — ignored.
    }

    @Deprecated
    public boolean hasChatInput(Player player) {
        return chatInputHandlers.containsKey(player);
    }

    @Deprecated
    public void handleChatInput(Player player, String message) {
        java.util.function.Consumer<String> handler = chatInputHandlers.remove(player);
        if (handler != null) handler.accept(message);
    }
}
