package ru.example.adminpanel.serverevent;

import org.bukkit.Bukkit;
import org.bukkit.boss.BossBar;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import ru.example.adminpanel.AdminPanelPlugin;
import ru.example.adminpanel.api.event.ServerEventStartEvent;
import ru.example.adminpanel.api.event.ServerEventStopEvent;
import ru.example.adminpanel.config.Messages;
import ru.example.adminpanel.util.Text;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Управляет настраиваемыми серверными событиями: одновременно может идти только одно.
 * Все методы должны вызываться из основного потока сервера.
 * Плагин не выполняет никаких команд — другие плагины реагируют на
 * {@link ServerEventStartEvent} и {@link ServerEventStopEvent}.
 */
public final class ServerEventManager {

    /**
     * Результат попытки запуска события.
     */
    public enum StartResult {
        STARTED,
        UNKNOWN_EVENT,
        ALREADY_ACTIVE,
        CANCELLED
    }

    private final AdminPanelPlugin plugin;
    private ActiveServerEvent active;

    public ServerEventManager(AdminPanelPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * @return активное событие, если оно есть
     */
    public Optional<ActiveServerEvent> getActive() {
        return Optional.ofNullable(active);
    }

    public boolean isActive() {
        return active != null;
    }

    /**
     * Запускает событие с указанным ID из конфигурации.
     *
     * @param id        ID события (регистр не важен)
     * @param initiator кто запускает (может быть {@code null} при запуске через API)
     */
    public StartResult start(String id, CommandSender initiator) {
        ensureMainThread();
        if (active != null) {
            return StartResult.ALREADY_ACTIVE;
        }
        ServerEventDefinition definition = plugin.getSettings().events().get(id.toLowerCase(Locale.ROOT));
        if (definition == null) {
            return StartResult.UNKNOWN_EVENT;
        }

        ServerEventStartEvent startEvent = new ServerEventStartEvent(definition, initiator, definition.durationSeconds());
        Bukkit.getPluginManager().callEvent(startEvent);
        if (startEvent.isCancelled()) {
            return StartResult.CANCELLED;
        }
        // Обработчик события мог сам запустить другое событие.
        if (active != null) {
            return StartResult.ALREADY_ACTIVE;
        }

        ActiveServerEvent event = new ActiveServerEvent(definition, initiator, startEvent.getDurationSeconds());
        active = event;

        ServerEventDefinition.BossBarSettings barSettings = definition.bossBar();
        if (barSettings.enabled()) {
            BossBar bossBar = Bukkit.createBossBar(bossBarTitle(event), barSettings.color(), barSettings.style());
            bossBar.setProgress(1.0);
            for (Player player : Bukkit.getOnlinePlayers()) {
                bossBar.addPlayer(player);
            }
            event.setBossBar(bossBar);
        }

        broadcastLines(definition.startMessage(), event);
        sendStartTitle(event);

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> tick(event), 20L, 20L);
        event.setTask(task);

        plugin.getLogger().info("Серверное событие '" + definition.id() + "' запущено на "
                + event.getDurationSeconds() + " с. Инициатор: " + (initiator == null ? "API" : initiator.getName()));
        return StartResult.STARTED;
    }

    /**
     * Останавливает активное событие.
     *
     * @param reason  причина
     * @param stopper кто остановил (может быть {@code null})
     * @return {@code false}, если активного события не было
     */
    public boolean stop(StopReason reason, CommandSender stopper) {
        ensureMainThread();
        ActiveServerEvent event = active;
        if (event == null) {
            return false;
        }
        // Сначала сбрасываем состояние, чтобы обработчики ServerEventStopEvent могли запустить новое событие.
        active = null;
        BukkitTask task = event.getTask();
        if (task != null) {
            task.cancel();
        }
        BossBar bossBar = event.getBossBar();
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar.setVisible(false);
        }

        broadcastLines(event.getDefinition().stopMessage(), event);

        Bukkit.getPluginManager().callEvent(new ServerEventStopEvent(event.getDefinition(), reason, stopper,
                event.getElapsedSeconds()));
        plugin.getLogger().info("Серверное событие '" + event.getDefinition().id() + "' остановлено. Причина: " + reason);
        return true;
    }

    /**
     * Показывает боссбар активного события вошедшему игроку.
     */
    public void handleJoin(Player player) {
        if (active != null && active.getBossBar() != null) {
            active.getBossBar().addPlayer(player);
        }
    }

    /**
     * Вызывается при выключении плагина.
     */
    public void shutdown() {
        stop(StopReason.PLUGIN_DISABLE, null);
    }

    private void tick(ActiveServerEvent event) {
        if (active != event) {
            // Событие уже остановлено — задача будет отменена, но подстрахуемся.
            BukkitTask task = event.getTask();
            if (task != null) {
                task.cancel();
            }
            return;
        }
        int remaining = event.getRemainingSeconds() - 1;
        event.setRemainingSeconds(remaining);
        if (remaining <= 0) {
            stop(StopReason.TIMER, null);
            return;
        }
        BossBar bossBar = event.getBossBar();
        if (bossBar != null) {
            bossBar.setTitle(bossBarTitle(event));
            double progress = (double) remaining / Math.max(1, event.getDurationSeconds());
            bossBar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
        }
    }

    private String bossBarTitle(ActiveServerEvent event) {
        return Text.color(applyPlaceholders(event.getDefinition().bossBar().text(), event));
    }

    private void sendStartTitle(ActiveServerEvent event) {
        ServerEventDefinition definition = event.getDefinition();
        if (definition.title().isEmpty() && definition.subtitle().isEmpty()) {
            return;
        }
        String title = Text.color(applyPlaceholders(definition.title(), event));
        String subtitle = Text.color(applyPlaceholders(definition.subtitle(), event));
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle(title, subtitle, 10, 70, 20);
        }
    }

    private void broadcastLines(List<String> lines, ActiveServerEvent event) {
        if (lines.isEmpty()) {
            return;
        }
        for (String line : lines) {
            String message = Text.color(applyPlaceholders(line, event));
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(message);
            }
            Bukkit.getConsoleSender().sendMessage(message);
        }
    }

    private String applyPlaceholders(String template, ActiveServerEvent event) {
        Messages messages = plugin.getMessages();
        ServerEventDefinition definition = event.getDefinition();
        String withPrefix = template.replace("{prefix}", messages.raw("prefix"));
        return Text.replace(withPrefix,
                "id", definition.id(),
                "name", definition.displayName(),
                "duration", Text.formatDuration(event.getDurationSeconds()),
                "remaining", Text.formatDuration(event.getRemainingSeconds()));
    }

    private static void ensureMainThread() {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("ServerEventManager можно использовать только из основного потока сервера");
        }
    }
}
