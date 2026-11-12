package ru.example.adminpanel.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import ru.example.adminpanel.AdminPanelPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Обработчик событий инвентаря для меню AdminPanel.
 * <ul>
 *     <li>меню распознаётся по {@link AdminMenuHolder}, а не по заголовку;</li>
 *     <li>любое перемещение предметов, пока открыто меню, отменяется;</li>
 *     <li>действие определяется по внутренней кнопке слота и выполняется в следующем тике
 *     (безопасное открытие/закрытие инвентарей) с повторной проверкой прав.</li>
 * </ul>
 */
public final class MenuListener implements Listener {

    private final AdminPanelPlugin plugin;
    private final Map<UUID, Long> lastClick = new HashMap<>();

    public MenuListener(AdminPanelPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof AdminMenuHolder holder)) {
            return;
        }
        // Отменяем всё: клики, shift-клики из своего инвентаря, цифровые клавиши, двойной клик, выброс.
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player viewer)) {
            return;
        }
        int slot = event.getRawSlot();
        if (event.getClickedInventory() == null || slot < 0 || slot >= top.getSize()) {
            // Клик вне окна или по инвентарю игрока (нижняя часть) — игнорируем.
            return;
        }
        MenuButton button = holder.getButton(slot);
        if (button == null) {
            // Пустой или декоративный слот.
            return;
        }
        if (!viewer.getUniqueId().equals(holder.getViewerId())) {
            // Меню создано для другого игрока — не выполняем действий.
            return;
        }
        if (isOnCooldown(viewer)) {
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!viewer.isOnline()) {
                return;
            }
            // Игрок мог закрыть меню или открыть другое за этот тик.
            if (plugin.getMenuManager().getOpenMenu(viewer) != holder) {
                return;
            }
            plugin.getMenuManager().handleClick(viewer, holder, button);
        });
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof AdminMenuHolder)) {
            return;
        }
        int topSize = top.getSize();
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < topSize) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof AdminMenuHolder) {
            lastClick.remove(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastClick.remove(event.getPlayer().getUniqueId());
    }

    private boolean isOnCooldown(Player viewer) {
        long cooldown = plugin.getSettings().gui().clickCooldownMs();
        if (cooldown <= 0) {
            return false;
        }
        long now = System.currentTimeMillis();
        Long last = lastClick.get(viewer.getUniqueId());
        if (last != null && now - last < cooldown) {
            return true;
        }
        lastClick.put(viewer.getUniqueId(), now);
        return false;
    }
}
