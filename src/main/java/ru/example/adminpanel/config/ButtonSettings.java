package ru.example.adminpanel.config;

import org.bukkit.Material;

import java.util.List;

/**
 * Настройки внешнего вида кнопки. Строки «сырые» — плейсхолдеры и цвета применяются при отрисовке.
 *
 * @param enabled  показывать ли кнопку
 * @param slot     слот в инвентаре (для шаблона головы игрока не используется)
 * @param material материал предмета
 * @param name     название
 * @param lore     описание
 * @param glow     эффект свечения
 */
public record ButtonSettings(boolean enabled, int slot, Material material, String name, List<String> lore,
                             boolean glow) {

    public ButtonSettings {
        lore = List.copyOf(lore);
    }
}
