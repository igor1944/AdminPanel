package ru.example.adminpanel.gui;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import ru.example.adminpanel.AdminPanelPlugin;
import ru.example.adminpanel.Permissions;
import ru.example.adminpanel.action.AdminActionService;
import ru.example.adminpanel.api.AdminActionType;
import ru.example.adminpanel.api.MenuType;
import ru.example.adminpanel.api.event.AdminMenuOpenEvent;
import ru.example.adminpanel.config.ButtonSettings;
import ru.example.adminpanel.config.GuiSettings;
import ru.example.adminpanel.config.Messages;
import ru.example.adminpanel.config.MenuSettings;
import ru.example.adminpanel.util.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Открывает, отрисовывает и обновляет меню, а также выполняет действия кнопок.
 * Все методы вызываются только из основного потока.
 */
public final class MenuManager {

    private final AdminPanelPlugin plugin;
    private final AdminActionService actions;

    public MenuManager(AdminPanelPlugin plugin, AdminActionService actions) {
        this.plugin = plugin;
        this.actions = actions;
    }

    // ================================================================== открытие

    /**
     * Открывает главное меню на указанной странице (номер страницы с нуля, выход за границы корректируется).
     */
    public boolean openMain(Player viewer, int page) {
        return open(viewer, MenuType.MAIN, null, page, page);
    }

    /**
     * Открывает меню игрока.
     *
     * @param returnPage страница главного меню, на которую вернёт кнопка «Назад»
     */
    public boolean openPlayer(Player viewer, Player target, int returnPage) {
        return open(viewer, MenuType.PLAYER, target, 0, returnPage);
    }

    /**
     * Открывает меню выбора игрового режима.
     *
     * @param target     игрок, чей режим меняется; {@code null} — сам администратор
     * @param returnPage страница главного меню для кнопки «Назад»
     */
    public boolean openGamemode(Player viewer, Player target, int returnPage) {
        return open(viewer, MenuType.GAMEMODE, target, 0, returnPage);
    }

    private boolean open(Player viewer, MenuType type, Player target, int page, int returnPage) {
        Messages messages = plugin.getMessages();
        if (!viewer.hasPermission(Permissions.USE)) {
            messages.send(viewer, "no-permission", "permission", Permissions.USE);
            return false;
        }
        if (target != null && !target.isOnline()) {
            messages.send(viewer, "player-offline");
            return false;
        }
        MenuSettings menu = plugin.getSettings().gui().menu(type);

        int clampedPage = 0;
        int pages = 1;
        if (type == MenuType.MAIN) {
            pages = pageCount(menu, visiblePlayers(viewer).size());
            clampedPage = Math.max(0, Math.min(page, pages - 1));
        }

        Player eventTarget = type == MenuType.GAMEMODE && target == null ? viewer : target;
        AdminMenuOpenEvent event = new AdminMenuOpenEvent(viewer, type, eventTarget, clampedPage);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            messages.send(viewer, "menu-cancelled");
            return false;
        }
        if (!viewer.isOnline() || (target != null && !target.isOnline())) {
            return false;
        }

        UUID targetId = target == null ? null : target.getUniqueId();
        AdminMenuHolder holder = new AdminMenuHolder(type, viewer.getUniqueId(), targetId, clampedPage, returnPage);
        String title = switch (type) {
            case MAIN -> Text.replace(menu.title(),
                    "page", String.valueOf(clampedPage + 1),
                    "pages", String.valueOf(pages),
                    "online", String.valueOf(visiblePlayers(viewer).size()));
            case PLAYER -> Text.replace(menu.title(), playerPlaceholders(target));
            case GAMEMODE -> Text.replace(menu.title(), playerPlaceholders(eventTarget));
        };
        Inventory inventory = Bukkit.createInventory(holder, menu.size(), Text.color(title));
        holder.setInventory(inventory);
        if (!render(holder, viewer)) {
            messages.send(viewer, "player-offline");
            return false;
        }
        viewer.openInventory(inventory);
        return true;
    }

    // ================================================================== отрисовка

    /**
     * Заполняет инвентарь меню. Используется и при открытии, и при обновлении.
     *
     * @return {@code false}, если целевой игрок вышел с сервера
     */
    boolean render(AdminMenuHolder holder, Player viewer) {
        GuiSettings gui = plugin.getSettings().gui();
        MenuSettings menu = gui.menu(holder.getType());
        Inventory inventory = holder.getInventory();

        Player target;
        if (holder.getTargetId() == null) {
            target = viewer;
        } else {
            target = Bukkit.getPlayer(holder.getTargetId());
            if (target == null) {
                return false;
            }
        }

        inventory.clear();
        holder.clearButtons();
        int size = inventory.getSize();
        if (menu.filler() != null) {
            var fillerItem = ItemFactory.filler(menu.filler());
            for (int slot = 0; slot < size; slot++) {
                inventory.setItem(slot, fillerItem);
            }
        }

        switch (holder.getType()) {
            case MAIN -> renderMain(holder, viewer, menu, gui);
            case PLAYER, GAMEMODE -> renderButtons(holder, viewer, target, menu, gui,
                    playerPlaceholders(target));
        }
        return true;
    }

    private void renderMain(AdminMenuHolder holder, Player viewer, MenuSettings menu, GuiSettings gui) {
        Inventory inventory = holder.getInventory();
        List<Player> players = visiblePlayers(viewer);
        int perPage = menu.playerSlots().size();
        int pages = pageCount(menu, players.size());
        int page = Math.max(0, Math.min(holder.getPage(), pages - 1));
        holder.setPage(page);

        for (int slot : menu.playerSlots()) {
            inventory.setItem(slot, null);
        }
        if (perPage > 0) {
            int from = page * perPage;
            int to = Math.min(players.size(), from + perPage);
            for (int i = from; i < to; i++) {
                Player player = players.get(i);
                int slot = menu.playerSlots().get(i - from);
                inventory.setItem(slot, ItemFactory.button(menu.playerHead(), player, false, null,
                        playerPlaceholders(player)));
                holder.setButton(slot, MenuButton.player(player.getUniqueId()));
            }
        }

        String[] placeholders = concat(playerPlaceholders(viewer),
                "page", String.valueOf(page + 1),
                "pages", String.valueOf(pages),
                "online", String.valueOf(players.size()),
                "worlds", managedWorldNames());
        for (ButtonType type : ButtonType.forMenu(MenuType.MAIN)) {
            if (type == ButtonType.MAIN_PREVIOUS_PAGE && page <= 0) {
                continue;
            }
            if (type == ButtonType.MAIN_NEXT_PAGE && page >= pages - 1) {
                continue;
            }
            placeButton(holder, viewer, viewer, menu, gui, type, placeholders);
        }
    }

    private void renderButtons(AdminMenuHolder holder, Player viewer, Player target, MenuSettings menu,
                               GuiSettings gui, String[] placeholders) {
        for (ButtonType type : ButtonType.forMenu(holder.getType())) {
            placeButton(holder, viewer, target, menu, gui, type, placeholders);
        }
    }

    private void placeButton(AdminMenuHolder holder, Player viewer, Player subject, MenuSettings menu,
                             GuiSettings gui, ButtonType type, String[] placeholders) {
        ButtonSettings settings = menu.button(type);
        if (settings == null) {
            return;
        }
        boolean glow = switch (type.getAction()) {
            case FLY -> subject.getAllowFlight();
            case SET_GAMEMODE -> subject.getGameMode() == type.getGameMode();
            default -> false;
        };
        String noAccess = type.getPermission() != null && !viewer.hasPermission(type.getPermission())
                ? gui.noAccessLore() : null;
        Player skullOwner = settings.material() == Material.PLAYER_HEAD ? subject : null;
        holder.getInventory().setItem(settings.slot(),
                ItemFactory.button(settings, skullOwner, glow, noAccess, placeholders));
        holder.setButton(settings.slot(), MenuButton.of(type));
    }

    // ================================================================== обновление

    /**
     * Перерисовывает меню, открытое у игрока (если это меню AdminPanel).
     */
    public void refresh(Player viewer) {
        AdminMenuHolder holder = getOpenMenu(viewer);
        if (holder != null && !render(holder, viewer)) {
            plugin.getMessages().send(viewer, "player-offline");
            openMain(viewer, holder.getReturnPage());
        }
    }

    /**
     * Перерисовывает все открытые главные меню (например, при входе/выходе игроков).
     */
    public void refreshMainMenus() {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            AdminMenuHolder holder = getOpenMenu(viewer);
            if (holder != null && holder.getType() == MenuType.MAIN) {
                render(holder, viewer);
            }
        }
    }

    /**
     * Возвращает в главное меню администраторов, у которых открыто меню вышедшего игрока.
     */
    public void handleTargetQuit(UUID quitterId) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            AdminMenuHolder holder = getOpenMenu(viewer);
            if (holder != null && quitterId.equals(holder.getTargetId())) {
                plugin.getMessages().send(viewer, "player-offline");
                openMain(viewer, holder.getReturnPage());
            }
        }
    }

    /**
     * Закрывает все открытые меню AdminPanel (при перезагрузке и выключении плагина).
     */
    public void closeAll() {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (getOpenMenu(viewer) != null) {
                viewer.closeInventory();
            }
        }
    }

    /**
     * @return меню AdminPanel, открытое у игрока, или {@code null}
     */
    public AdminMenuHolder getOpenMenu(Player viewer) {
        InventoryHolder holder = viewer.getOpenInventory().getTopInventory().getHolder();
        return holder instanceof AdminMenuHolder menuHolder ? menuHolder : null;
    }

    // ================================================================== клики

    /**
     * Выполняет действие кнопки. Права проверяются заново при каждом клике.
     */
    void handleClick(Player viewer, AdminMenuHolder holder, MenuButton button) {
        Messages messages = plugin.getMessages();
        if (!viewer.hasPermission(Permissions.USE)) {
            messages.send(viewer, "no-permission", "permission", Permissions.USE);
            playSound(viewer, false);
            viewer.closeInventory();
            return;
        }

        boolean success;
        switch (button.action()) {
            case INFO -> {
                return;
            }
            case CLOSE -> {
                viewer.closeInventory();
                success = true;
            }
            case REFRESH -> {
                refresh(viewer);
                success = true;
            }
            case PREVIOUS_PAGE -> success = openMain(viewer, holder.getPage() - 1);
            case NEXT_PAGE -> success = openMain(viewer, holder.getPage() + 1);
            case BACK -> success = goBack(viewer, holder);
            case OPEN_PLAYER -> {
                Player target = button.playerId() == null ? null : Bukkit.getPlayer(button.playerId());
                if (target == null || !viewer.canSee(target)) {
                    messages.send(viewer, "player-offline");
                    refresh(viewer);
                    success = false;
                } else {
                    success = openPlayer(viewer, target, holder.getPage());
                }
            }
            case OPEN_GAMEMODE_MENU -> {
                if (!viewer.hasPermission(Permissions.ACTION_GAMEMODE)) {
                    messages.send(viewer, "no-permission", "permission", Permissions.ACTION_GAMEMODE);
                    success = false;
                } else if (holder.getType() == MenuType.MAIN) {
                    success = openGamemode(viewer, null, holder.getPage());
                } else {
                    Player target = resolveTarget(viewer, holder);
                    success = target != null && openGamemode(viewer, target, holder.getReturnPage());
                }
            }
            case SET_GAMEMODE -> {
                Player target = resolveTarget(viewer, holder);
                success = target != null && actions.setGameMode(viewer, target, button.gameMode());
                refreshIfOpen(viewer, holder);
            }
            case HEAL -> {
                Player target = resolveTarget(viewer, holder);
                success = target != null && actions.heal(viewer, target);
                refreshIfOpen(viewer, holder);
            }
            case FEED -> {
                Player target = resolveTarget(viewer, holder);
                success = target != null && actions.feed(viewer, target);
                refreshIfOpen(viewer, holder);
            }
            case FLY -> {
                Player target = resolveTarget(viewer, holder);
                success = target != null && actions.toggleFly(viewer, target);
                refreshIfOpen(viewer, holder);
            }
            case TELEPORT_TO -> {
                Player target = resolveTarget(viewer, holder);
                success = target != null && actions.teleportTo(viewer, target);
                if (success) {
                    viewer.closeInventory();
                }
            }
            case TELEPORT_HERE -> {
                Player target = resolveTarget(viewer, holder);
                success = target != null && actions.teleportHere(viewer, target);
                refreshIfOpen(viewer, holder);
            }
            case TIME_DAY -> success = actions.setTime(viewer, true);
            case TIME_NIGHT -> success = actions.setTime(viewer, false);
            case WEATHER_CLEAR -> success = actions.setWeather(viewer, AdminActionType.WEATHER_CLEAR);
            case WEATHER_RAIN -> success = actions.setWeather(viewer, AdminActionType.WEATHER_RAIN);
            case WEATHER_THUNDER -> success = actions.setWeather(viewer, AdminActionType.WEATHER_THUNDER);
            default -> success = false;
        }
        playSound(viewer, success);
    }

    private boolean goBack(Player viewer, AdminMenuHolder holder) {
        if (holder.getType() == MenuType.GAMEMODE && holder.getTargetId() != null) {
            Player target = Bukkit.getPlayer(holder.getTargetId());
            if (target != null) {
                return openPlayer(viewer, target, holder.getReturnPage());
            }
            plugin.getMessages().send(viewer, "player-offline");
        }
        return openMain(viewer, holder.getReturnPage());
    }

    /**
     * @return цель действия: сам администратор в главном меню / своём меню режима,
     * либо выбранный игрок. Если игрок вышел — сообщает об этом, открывает главное меню и возвращает {@code null}.
     */
    private Player resolveTarget(Player viewer, AdminMenuHolder holder) {
        if (holder.getType() == MenuType.MAIN || holder.getTargetId() == null) {
            return viewer;
        }
        Player target = Bukkit.getPlayer(holder.getTargetId());
        if (target == null) {
            plugin.getMessages().send(viewer, "player-offline");
            openMain(viewer, holder.getReturnPage());
            return null;
        }
        return target;
    }

    private void refreshIfOpen(Player viewer, AdminMenuHolder holder) {
        if (getOpenMenu(viewer) == holder) {
            refresh(viewer);
        }
    }

    private void playSound(Player viewer, boolean success) {
        GuiSettings gui = plugin.getSettings().gui();
        if (!gui.soundsEnabled()) {
            return;
        }
        Sound sound = success ? gui.clickSound() : gui.denySound();
        if (sound != null) {
            viewer.playSound(viewer.getLocation(), sound, gui.volume(), gui.pitch());
        }
    }

    // ================================================================== данные

    private List<Player> visiblePlayers(Player viewer) {
        List<Player> players = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.equals(viewer) || viewer.canSee(player)) {
                players.add(player);
            }
        }
        players.sort(Comparator.comparing(player -> player.getName().toLowerCase(Locale.ROOT)));
        return players;
    }

    private static int pageCount(MenuSettings menu, int playerCount) {
        int perPage = menu.playerSlots().size();
        if (perPage <= 0 || playerCount <= 0) {
            return 1;
        }
        return (playerCount + perPage - 1) / perPage;
    }

    private String[] playerPlaceholders(Player player) {
        Messages messages = plugin.getMessages();
        AttributeInstance maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        double max = maxHealth != null ? maxHealth.getValue() : 20.0;
        GameMode mode = player.getGameMode();
        return new String[]{
                "player", player.getName(),
                "health", formatNumber(player.getHealth()),
                "max_health", formatNumber(max),
                "food", String.valueOf(player.getFoodLevel()),
                "gamemode", messages.gameMode(mode),
                "world", player.getWorld().getName(),
                "ping", String.valueOf(player.getPing()),
                "fly", messages.state(player.getAllowFlight())
        };
    }

    private String managedWorldNames() {
        List<String> configured = plugin.getSettings().serverControl().worlds();
        if (!configured.isEmpty()) {
            return String.join(", ", configured);
        }
        List<String> names = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() == World.Environment.NORMAL) {
                names.add(world.getName());
            }
        }
        return String.join(", ", names);
    }

    private static String formatNumber(double value) {
        double rounded = Math.round(value * 10.0) / 10.0;
        if (rounded == Math.rint(rounded)) {
            return String.valueOf((long) rounded);
        }
        return String.format(Locale.ROOT, "%.1f", rounded);
    }

    private static String[] concat(String[] first, String... second) {
        String[] result = new String[first.length + second.length];
        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }
}
