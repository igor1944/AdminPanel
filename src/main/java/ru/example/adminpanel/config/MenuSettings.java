package ru.example.adminpanel.config;

import org.bukkit.Material;
import ru.example.adminpanel.gui.ButtonType;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Настройки одного меню.
 *
 * @param title          заголовок (сырой, с плейсхолдерами)
 * @param size           размер инвентаря (кратен 9, от 9 до 54)
 * @param filler         материал заполнителя пустых слотов или {@code null}, если заполнитель выключен
 * @param buttons        настройки кнопок этого меню (только включённые и с корректными слотами)
 * @param playerSlots    слоты для голов игроков (только для главного меню, иначе пустой список)
 * @param playerHead     шаблон головы игрока (только для главного меню, иначе {@code null})
 */
public record MenuSettings(String title, int size, Material filler, Map<ButtonType, ButtonSettings> buttons,
                           List<Integer> playerSlots, ButtonSettings playerHead) {

    public MenuSettings {
        Map<ButtonType, ButtonSettings> copy = new EnumMap<>(ButtonType.class);
        copy.putAll(buttons);
        buttons = Collections.unmodifiableMap(copy);
        playerSlots = List.copyOf(playerSlots);
    }

    /**
     * @return настройки кнопки или {@code null}, если кнопка выключена
     */
    public ButtonSettings button(ButtonType type) {
        ButtonSettings settings = buttons.get(type);
        return settings != null && settings.enabled() ? settings : null;
    }
}
