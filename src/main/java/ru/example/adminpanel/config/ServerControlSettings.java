package ru.example.adminpanel.config;

import java.util.List;

/**
 * Настройки управления временем и погодой.
 *
 * @param worlds                 имена управляемых миров; пустой список — все обычные миры
 * @param dayTime                время дня в тиках
 * @param nightTime              время ночи в тиках
 * @param weatherDurationSeconds длительность установленной погоды в секундах
 */
public record ServerControlSettings(List<String> worlds, long dayTime, long nightTime, int weatherDurationSeconds) {

    public ServerControlSettings {
        worlds = List.copyOf(worlds);
    }
}
