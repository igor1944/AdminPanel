package ru.example.adminpanel.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import ru.example.adminpanel.api.MenuType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Собственный {@link InventoryHolder} для всех меню AdminPanel. Меню распознаётся по типу
 * владельца инвентаря ({@code holder instanceof AdminMenuHolder}), а не по заголовку.
 * Хранит состояние меню и привязку «слот → внутренняя кнопка».
 */
public final class AdminMenuHolder implements InventoryHolder {

    private final MenuType type;
    private final UUID viewerId;
    private final UUID targetId;
    private int page;
    private final int returnPage;
    private final Map<Integer, MenuButton> buttons = new HashMap<>();
    private Inventory inventory;

    /**
     * @param type       тип меню
     * @param viewerId   администратор, для которого создано меню
     * @param targetId   целевой игрок (для меню игрока и выбора режима); {@code null} — сам администратор
     * @param page       текущая страница (для главного меню)
     * @param returnPage страница главного меню, на которую вернёт кнопка «Назад»
     */
    AdminMenuHolder(MenuType type, UUID viewerId, UUID targetId, int page, int returnPage) {
        this.type = type;
        this.viewerId = viewerId;
        this.targetId = targetId;
        this.page = page;
        this.returnPage = returnPage;
    }

    public MenuType getType() {
        return type;
    }

    public UUID getViewerId() {
        return viewerId;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public int getPage() {
        return page;
    }

    void setPage(int page) {
        this.page = page;
    }

    public int getReturnPage() {
        return returnPage;
    }

    /**
     * @return кнопка в слоте или {@code null}, если слот пустой/декоративный
     */
    public MenuButton getButton(int slot) {
        return buttons.get(slot);
    }

    void clearButtons() {
        buttons.clear();
    }

    void setButton(int slot, MenuButton button) {
        buttons.put(slot, button);
    }

    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
