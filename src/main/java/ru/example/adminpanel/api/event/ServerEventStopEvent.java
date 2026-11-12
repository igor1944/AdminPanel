package ru.example.adminpanel.api.event;

import org.bukkit.command.CommandSender;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import ru.example.adminpanel.serverevent.ServerEventDefinition;
import ru.example.adminpanel.serverevent.StopReason;

/**
 * Вызывается в основном потоке после остановки серверного события
 * (командой, по таймеру или при выключении плагина). Событие не отменяется:
 * к моменту вызова серверное событие уже остановлено.
 */
public class ServerEventStopEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final ServerEventDefinition definition;
    private final StopReason reason;
    private final CommandSender stopper;
    private final long elapsedSeconds;

    public ServerEventStopEvent(ServerEventDefinition definition, StopReason reason,
                                CommandSender stopper, long elapsedSeconds) {
        this.definition = definition;
        this.reason = reason;
        this.stopper = stopper;
        this.elapsedSeconds = elapsedSeconds;
    }

    /**
     * @return описание остановленного события
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
     * @return причина остановки
     */
    public StopReason getReason() {
        return reason;
    }

    /**
     * @return кто остановил событие; {@code null}, если оно завершилось по таймеру или при выключении плагина
     */
    public CommandSender getStopper() {
        return stopper;
    }

    /**
     * @return сколько секунд событие фактически длилось
     */
    public long getElapsedSeconds() {
        return elapsedSeconds;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
