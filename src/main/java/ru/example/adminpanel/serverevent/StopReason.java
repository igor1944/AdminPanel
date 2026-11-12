package ru.example.adminpanel.serverevent;

/**
 * Причина остановки серверного события.
 */
public enum StopReason {
    /** Остановлено командой {@code /adminpanel event stop}. */
    COMMAND,
    /** Истекла длительность события. */
    TIMER,
    /** Остановлено другим плагином через API. */
    API,
    /** Плагин выключается (остановка сервера или выгрузка плагина). */
    PLUGIN_DISABLE
}
