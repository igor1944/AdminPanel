package ru.example.adminpanel.gui;

import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import ru.example.adminpanel.config.ButtonSettings;
import ru.example.adminpanel.util.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Создаёт предметы для кнопок меню.
 */
final class ItemFactory {

    private ItemFactory() {
    }

    /**
     * @param settings     настройки кнопки
     * @param skullOwner   владелец головы, если материал — PLAYER_HEAD (может быть {@code null})
     * @param glow         принудительное свечение (в дополнение к настройке)
     * @param extraLore    дополнительная строка описания (может быть {@code null})
     * @param placeholders пары «ключ, значение»
     */
    static ItemStack button(ButtonSettings settings, OfflinePlayer skullOwner, boolean glow, String extraLore,
                            String... placeholders) {
        List<String> lore = new ArrayList<>(Text.replace(settings.lore(), placeholders));
        if (extraLore != null && !extraLore.isEmpty()) {
            lore.add(extraLore);
        }
        return create(settings.material(), Text.replace(settings.name(), placeholders), lore,
                glow || settings.glow(), skullOwner);
    }

    static ItemStack filler(Material material) {
        return create(material, " ", List.of(), false, null);
    }

    static ItemStack create(Material material, String name, List<String> lore, boolean glow, OfflinePlayer skullOwner) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.setDisplayName(name.isEmpty() ? " " : Text.color(name));
        if (!lore.isEmpty()) {
            meta.setLore(Text.color(lore));
        }
        if (glow) {
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
        }
        meta.addItemFlags(ItemFlag.values());
        if (skullOwner != null && meta instanceof SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(skullOwner);
        }
        item.setItemMeta(meta);
        return item;
    }
}
