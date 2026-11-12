package ru.example.adminpanel.api.event;

import org.bukkit.command.CommandSender;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import ru.example.adminpanel.serverevent.ServerEventDefinition;

/**
 * Вызывается в основном потоке перед запуском серверного события.
 * Отмена события запрещает запуск. Длительность можно изменить.
 */
public class ServerEventStartEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final ServerEventDefinition definition;
    private final CommandSender initiator;
    private int durationSeconds;
    private boolean cancelled;

    public ServerEventStartEvent(ServerEventDefinition definition, CommandSender initiator, int durationSeconds) {
        this.definition = definition;
        this.initiator = initiator;
        this.durationSeconds = durationSeconds;
    }

    /**
     * @return описание запускаемого события из config.yml
     */
    public ServerEventDefinition getDefinition() {
        return definition;
    }

    /**
     * @return ID события (как в config.yml)
     */
    public String getEventId() {
        return definition.id();
    }

    /**
     * @return кто запускает событие (игрок, консоль или {@code null}, если запуск выполнен через API)
     */
    public CommandSender getInitiator() {
        return initiator;
    }

    /**
     * @return длительность события в секундах
     */
    public int getDurationSeconds() {
        return durationSeconds;
    }

    /**
     * Изменяет длительность запускаемого события.
     *
     * @param durationSeconds длительность в секундах, больше нуля
     */
    public void setDurationSeconds(int durationSeconds) {
        if (durationSeconds <= 0) {
            throw new IllegalArgumentException("Длительность события должна быть больше нуля");
        }
        this.durationSeconds = durationSeconds;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
