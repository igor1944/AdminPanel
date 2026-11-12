package ru.example.adminpanel.gui;

/**
 * Внутренний идентификатор действия кнопки. Обработчик кликов работает только с ним
 * и никогда не опирается на название или материал предмета.
 */
public enum MenuAction {
    /** Информационная кнопка без действия (заголовки разделов, сведения об игроке). */
    INFO,
    HEAL,
    FEED,
    FLY,
    /** Открыть меню выбора игрового режима. */
    OPEN_GAMEMODE_MENU,
    /** Установить игровой режим, указанный в кнопке. */
    SET_GAMEMODE,
    TIME_DAY,
    TIME_NIGHT,
    WEATHER_CLEAR,
    WEATHER_RAIN,
    WEATHER_THUNDER,
    /** Открыть меню игрока, указанного в кнопке (головы игроков). */
    OPEN_PLAYER,
    TELEPORT_TO,
    TELEPORT_HERE,
    PREVIOUS_PAGE,
    NEXT_PAGE,
    REFRESH,
    BACK,
    CLOSE
}
