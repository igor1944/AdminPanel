package ru.example.adminpanel.api.event;

import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import ru.example.adminpanel.api.AdminActionType;

/**
 * Вызывается в основном потоке перед выполнением административного действия
 * (после проверки прав). Отмена события запрещает выполнение действия.
 */
public class AdminActionEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final CommandSender admin;
    private final AdminActionType action;
    private final Player target;
    private final GameMode gameMode;
    private String message;
    private boolean cancelled;

    public AdminActionEvent(CommandSender admin, AdminActionType action, Player target,
                            GameMode gameMode, String message) {
        this.admin = admin;
        this.action = action;
        this.target = target;
        this.gameMode = gameMode;
        this.message = message;
    }

    /**
     * @return кто выполняет действие (игрок-администратор или консоль)
     */
    public CommandSender getAdmin() {
        return admin;
    }

    /**
     * @return администратор как игрок или {@code null}, если действие выполняет консоль
     */
    public Player getAdminPlayer() {
        return admin instanceof Player player ? player : null;
    }

    /**
     * @return тип действия
     */
    public AdminActionType getAction() {
        return action;
    }

    /**
     * @return целевой игрок или {@code null} для действий без цели (время, погода, объявление)
     */
    public Player getTarget() {
        return target;
    }

    /**
     * @return устанавливаемый режим для {@link AdminActionType#GAMEMODE}, иначе {@code null}
     */
    public GameMode getGameMode() {
        return gameMode;
    }

    /**
     * @return текст объявления для {@link AdminActionType#BROADCAST}, иначе {@code null}
     */
    public String getMessage() {
        return message;
    }

    /**
     * Изменяет текст объявления для {@link AdminActionType#BROADCAST}.
     * Для остальных действий значение игнорируется.
     */
    public void setMessage(String message) {
        this.message = message;
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
