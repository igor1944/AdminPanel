package ru.example.adminpanel.gui;

import org.bukkit.GameMode;
import ru.example.adminpanel.Permissions;
import ru.example.adminpanel.api.MenuType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Реестр всех настраиваемых кнопок. Каждая кнопка привязана к меню, ключу в конфигурации
 * ({@code menus.<menu>.buttons.<key>}), внутреннему действию и (необязательно) праву.
 */
public enum ButtonType {

    // ----------------------------- Главное меню -----------------------------
    MAIN_ADMIN_SECTION(MenuType.MAIN, "admin-section", MenuAction.INFO, null, null),
    MAIN_HEAL(MenuType.MAIN, "heal", MenuAction.HEAL, Permissions.ACTION_HEAL, null),
    MAIN_FEED(MenuType.MAIN, "feed", MenuAction.FEED, Permissions.ACTION_FEED, null),
    MAIN_FLY(MenuType.MAIN, "fly", MenuAction.FLY, Permissions.ACTION_FLY, null),
    MAIN_GAMEMODE(MenuType.MAIN, "gamemode", MenuAction.OPEN_GAMEMODE_MENU, Permissions.ACTION_GAMEMODE, null),
    MAIN_SERVER_SECTION(MenuType.MAIN, "server-section", MenuAction.INFO, null, null),
    MAIN_TIME_DAY(MenuType.MAIN, "time-day", MenuAction.TIME_DAY, Permissions.ACTION_TIME, null),
    MAIN_TIME_NIGHT(MenuType.MAIN, "time-night", MenuAction.TIME_NIGHT, Permissions.ACTION_TIME, null),
    MAIN_WEATHER_CLEAR(MenuType.MAIN, "weather-clear", MenuAction.WEATHER_CLEAR, Permissions.ACTION_WEATHER, null),
    MAIN_WEATHER_RAIN(MenuType.MAIN, "weather-rain", MenuAction.WEATHER_RAIN, Permissions.ACTION_WEATHER, null),
    MAIN_WEATHER_THUNDER(MenuType.MAIN, "weather-thunder", MenuAction.WEATHER_THUNDER, Permissions.ACTION_WEATHER, null),
    MAIN_PREVIOUS_PAGE(MenuType.MAIN, "previous-page", MenuAction.PREVIOUS_PAGE, null, null),
    MAIN_PLAYERS_INFO(MenuType.MAIN, "players-info", MenuAction.INFO, null, null),
    MAIN_CLOSE(MenuType.MAIN, "close", MenuAction.CLOSE, null, null),
    MAIN_REFRESH(MenuType.MAIN, "refresh", MenuAction.REFRESH, null, null),
    MAIN_NEXT_PAGE(MenuType.MAIN, "next-page", MenuAction.NEXT_PAGE, null, null),

    // ----------------------------- Меню игрока ------------------------------
    PLAYER_TARGET_INFO(MenuType.PLAYER, "target-info", MenuAction.INFO, null, null),
    PLAYER_TELEPORT_TO(MenuType.PLAYER, "teleport-to", MenuAction.TELEPORT_TO, Permissions.ACTION_TELEPORT, null),
    PLAYER_TELEPORT_HERE(MenuType.PLAYER, "teleport-here", MenuAction.TELEPORT_HERE, Permissions.ACTION_TELEPORT, null),
    PLAYER_HEAL(MenuType.PLAYER, "heal", MenuAction.HEAL, Permissions.ACTION_HEAL, null),
    PLAYER_FEED(MenuType.PLAYER, "feed", MenuAction.FEED, Permissions.ACTION_FEED, null),
    PLAYER_FLY(MenuType.PLAYER, "fly", MenuAction.FLY, Permissions.ACTION_FLY, null),
    PLAYER_GAMEMODE(MenuType.PLAYER, "gamemode", MenuAction.OPEN_GAMEMODE_MENU, Permissions.ACTION_GAMEMODE, null),
    PLAYER_BACK(MenuType.PLAYER, "back", MenuAction.BACK, null, null),
    PLAYER_CLOSE(MenuType.PLAYER, "close", MenuAction.CLOSE, null, null),

    // -------------------------- Выбор режима игры ---------------------------
    GAMEMODE_SURVIVAL(MenuType.GAMEMODE, "survival", MenuAction.SET_GAMEMODE, Permissions.ACTION_GAMEMODE, GameMode.SURVIVAL),
    GAMEMODE_CREATIVE(MenuType.GAMEMODE, "creative", MenuAction.SET_GAMEMODE, Permissions.ACTION_GAMEMODE, GameMode.CREATIVE),
    GAMEMODE_ADVENTURE(MenuType.GAMEMODE, "adventure", MenuAction.SET_GAMEMODE, Permissions.ACTION_GAMEMODE, GameMode.ADVENTURE),
    GAMEMODE_SPECTATOR(MenuType.GAMEMODE, "spectator", MenuAction.SET_GAMEMODE, Permissions.ACTION_GAMEMODE, GameMode.SPECTATOR),
    GAMEMODE_BACK(MenuType.GAMEMODE, "back", MenuAction.BACK, null, null),
    GAMEMODE_CLOSE(MenuType.GAMEMODE, "close", MenuAction.CLOSE, null, null);

    private static final Map<MenuType, List<ButtonType>> BY_MENU = new EnumMap<>(MenuType.class);

    static {
        for (MenuType type : MenuType.values()) {
            BY_MENU.put(type, new ArrayList<>());
        }
        for (ButtonType button : values()) {
            BY_MENU.get(button.menu).add(button);
        }
        BY_MENU.replaceAll((type, list) -> Collections.unmodifiableList(list));
    }

    private final MenuType menu;
    private final String key;
    private final MenuAction action;
    private final String permission;
    private final GameMode gameMode;

    ButtonType(MenuType menu, String key, MenuAction action, String permission, GameMode gameMode) {
        this.menu = menu;
        this.key = key;
        this.action = action;
        this.permission = permission;
        this.gameMode = gameMode;
    }

    public static List<ButtonType> forMenu(MenuType menu) {
        return BY_MENU.get(menu);
    }

    public MenuType getMenu() {
        return menu;
    }

    public String getKey() {
        return key;
    }

    public MenuAction getAction() {
        return action;
    }

    /**
     * @return право, без которого к описанию кнопки добавляется пометка «нет доступа»; может быть {@code null}
     */
    public String getPermission() {
        return permission;
    }

    /**
     * @return игровой режим для кнопок {@link MenuAction#SET_GAMEMODE}, иначе {@code null}
     */
    public GameMode getGameMode() {
        return gameMode;
    }

    /**
     * @return путь секции кнопки в config.yml
     */
    public String getConfigPath() {
        return "menus." + menu.getConfigKey() + ".buttons." + key;
    }
}
