package ru.example.adminpanel.serverevent;

import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;

import java.util.List;

/**
 * Неизменяемое описание серверного события из секции {@code server-events} config.yml.
 * Все строки хранятся «сырыми» (с кодами {@code &}); цвета применяются при отправке.
 *
 * @param id              ID события (ключ секции, в нижнем регистре)
 * @param displayName     отображаемое название
 * @param durationSeconds длительность в секундах
 * @param startMessage    строки сообщения при запуске (может быть пустым)
 * @param stopMessage     строки сообщения при завершении (может быть пустым)
 * @param title           титул при запуске (пустая строка — без титула)
 * @param subtitle        подзаголовок при запуске
 * @param bossBar         настройки боссбара
 */
public record ServerEventDefinition(
        String id,
        String displayName,
        int durationSeconds,
        List<String> startMessage,
        List<String> stopMessage,
        String title,
        String subtitle,
        BossBarSettings bossBar
) {

    public ServerEventDefinition {
        startMessage = List.copyOf(startMessage);
        stopMessage = List.copyOf(stopMessage);
    }

    /**
     * Настройки боссбара с оставшимся временем.
     */
    public record BossBarSettings(boolean enabled, String text, BarColor color, BarStyle style) {
    }
}
