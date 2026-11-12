package ru.example.adminpanel.config;

import ru.example.adminpanel.serverevent.ServerEventDefinition;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Неизменяемый снимок всей конфигурации. При {@code /adminpanel reload} создаётся новый снимок,
 * поэтому код, получивший старый снимок, всегда видит согласованные данные.
 *
 * @param messages      сообщения выбранного языка
 * @param logActions    писать ли административные действия в консоль
 * @param serverControl настройки времени и погоды
 * @param gui           настройки GUI
 * @param events        серверные события по ID (в порядке из конфигурации)
 */
public record PluginSettings(Messages messages, boolean logActions, ServerControlSettings serverControl,
                             GuiSettings gui, Map<String, ServerEventDefinition> events) {

    public PluginSettings {
        events = Collections.unmodifiableMap(new LinkedHashMap<>(events));
    }
}
