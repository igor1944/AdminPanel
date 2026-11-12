package ru.example.adminpanel;

/**
 * Все права плагина. Должны совпадать с секцией {@code permissions} в plugin.yml.
 */
public final class Permissions {

    public static final String USE = "adminpanel.use";
    public static final String RELOAD = "adminpanel.reload";
    public static final String EVENT = "adminpanel.event";

    public static final String ACTION_HEAL = "adminpanel.action.heal";
    public static final String ACTION_FEED = "adminpanel.action.feed";
    public static final String ACTION_FLY = "adminpanel.action.fly";
    public static final String ACTION_GAMEMODE = "adminpanel.action.gamemode";
    public static final String ACTION_TELEPORT = "adminpanel.action.teleport";
    public static final String ACTION_WEATHER = "adminpanel.action.weather";
    public static final String ACTION_TIME = "adminpanel.action.time";
    public static final String ACTION_BROADCAST = "adminpanel.action.broadcast";

    private Permissions() {
    }
}
