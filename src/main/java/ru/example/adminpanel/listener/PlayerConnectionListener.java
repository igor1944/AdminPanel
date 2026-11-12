package ru.example.adminpanel.listener;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import ru.example.adminpanel.AdminPanelPlugin;

import java.util.UUID;

/**
 * Поддерживает актуальность меню и боссбара серверного события при входе и выходе игроков.
 */
public final class PlayerConnectionListener implements Listener {

    private final AdminPanelPlugin plugin;

    public PlayerConnectionListener(AdminPanelPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        plugin.getServerEventManager().handleJoin(event.getPlayer());
        // Новая голова в списке игроков у открытых главных меню.
        Bukkit.getScheduler().runTask(plugin, () -> plugin.getMenuManager().refreshMainMenus());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        UUID quitterId = event.getPlayer().getUniqueId();
        // В момент события игрок ещё числится онлайн, поэтому обновляем меню в следующем тике.
        Bukkit.getScheduler().runTask(plugin, () -> {
            plugin.getMenuManager().handleTargetQuit(quitterId);
            plugin.getMenuManager().refreshMainMenus();
        });
    }
}
