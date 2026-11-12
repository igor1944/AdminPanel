package ru.example.adminpanel.api;

import ru.example.adminpanel.Permissions;

/**
 * Тип административного действия. Передаётся в {@link ru.example.adminpanel.api.event.AdminActionEvent}.
 */
public enum AdminActionType {

    HEAL(Permissions.ACTION_HEAL, true),
    FEED(Permissions.ACTION_FEED, true),
    FLY(Permissions.ACTION_FLY, true),
    GAMEMODE(Permissions.ACTION_GAMEMODE, true),
    TELEPORT_TO(Permissions.ACTION_TELEPORT, true),
    TELEPORT_HERE(Permissions.ACTION_TELEPORT, true),
    TIME_DAY(Permissions.ACTION_TIME, false),
    TIME_NIGHT(Permissions.ACTION_TIME, false),
    WEATHER_CLEAR(Permissions.ACTION_WEATHER, false),
    WEATHER_RAIN(Permissions.ACTION_WEATHER, false),
    WEATHER_THUNDER(Permissions.ACTION_WEATHER, false),
    BROADCAST(Permissions.ACTION_BROADCAST, false);

    private final String permission;
    private final boolean targeted;

    AdminActionType(String permission, boolean targeted) {
        this.permission = permission;
        this.targeted = targeted;
    }

    /**
     * @return право, необходимое для выполнения действия
     */
    public String getPermission() {
        return permission;
    }

    /**
     * @return {@code true}, если у действия есть целевой игрок
     */
    public boolean isTargeted() {
        return targeted;
    }
}
