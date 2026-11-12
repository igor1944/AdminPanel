package ru.example.adminpanel.config;

import org.bukkit.Sound;
import ru.example.adminpanel.api.MenuType;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Общие настройки GUI и настройки всех меню.
 *
 * @param menus           настройки меню по типу
 * @param clickCooldownMs минимальный интервал между кликами одного игрока
 * @param noAccessLore    строка, добавляемая к описанию кнопки без доступа (сырая)
 * @param soundsEnabled   воспроизводить ли звуки
 * @param clickSound      звук успешного действия (может быть {@code null})
 * @param denySound       звук отказа (может быть {@code null})
 * @param volume          громкость
 * @param pitch           высота тона
 */
public record GuiSettings(Map<MenuType, MenuSettings> menus, long clickCooldownMs, String noAccessLore,
                          boolean soundsEnabled, Sound clickSound, Sound denySound, float volume, float pitch) {

    public GuiSettings {
        Map<MenuType, MenuSettings> copy = new EnumMap<>(MenuType.class);
        copy.putAll(menus);
        menus = Collections.unmodifiableMap(copy);
    }

    public MenuSettings menu(MenuType type) {
        return menus.get(type);
    }
}
