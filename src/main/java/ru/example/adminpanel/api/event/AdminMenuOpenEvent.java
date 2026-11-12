package ru.example.adminpanel.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import ru.example.adminpanel.api.MenuType;

/**
 * Вызывается в основном потоке перед открытием любого меню AdminPanel.
 * Отмена события запрещает открытие меню.
 */
public class AdminMenuOpenEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player admin;
    private final MenuType menuType;
    private final Player target;
    private final int page;
    private boolean cancelled;

    public AdminMenuOpenEvent(Player admin, MenuType menuType, Player target, int page) {
        this.admin = admin;
        this.menuType = menuType;
        this.target = target;
        this.page = page;
    }

    /**
     * @return администратор, открывающий меню
     */
    public Player getAdmin() {
        return admin;
    }

    /**
     * @return тип открываемого меню
     */
    public MenuType getMenuType() {
        return menuType;
    }

    /**
     * @return целевой игрок для меню {@link MenuType#PLAYER} и {@link MenuType#GAMEMODE};
     * {@code null} для главного меню
     */
    public Player getTarget() {
        return target;
    }

    /**
     * @return номер страницы (с нуля) для главного меню, иначе 0
     */
    public int getPage() {
        return page;
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
