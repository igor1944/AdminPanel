package ru.example.adminpanel.gui;

import org.bukkit.GameMode;

import java.util.UUID;

/**
 * Кнопка, размещённая в конкретном слоте открытого меню.
 *
 * @param action   внутреннее действие
 * @param gameMode режим для {@link MenuAction#SET_GAMEMODE}, иначе {@code null}
 * @param playerId игрок для {@link MenuAction#OPEN_PLAYER}, иначе {@code null}
 */
public record MenuButton(MenuAction action, GameMode gameMode, UUID playerId) {

    public static MenuButton of(ButtonType type) {
        return new MenuButton(type.getAction(), type.getGameMode(), null);
    }

    public static MenuButton player(UUID playerId) {
        return new MenuButton(MenuAction.OPEN_PLAYER, null, playerId);
    }
}
