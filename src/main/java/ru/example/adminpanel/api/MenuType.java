package ru.example.adminpanel.api;

/**
 * Типы меню AdminPanel. Ключ используется как имя секции в {@code menus.<key>} конфигурации.
 */
public enum MenuType {

    /** Главное меню: действия над собой, игроки онлайн, управление сервером. */
    MAIN("main"),
    /** Меню выбранного игрока. */
    PLAYER("player"),
    /** Выбор игрового режима (для себя или для выбранного игрока). */
    GAMEMODE("gamemode");

    private final String configKey;

    MenuType(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigKey() {
        return configKey;
    }
}
