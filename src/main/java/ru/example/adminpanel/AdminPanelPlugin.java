package ru.example.adminpanel;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import ru.example.adminpanel.action.AdminActionService;
import ru.example.adminpanel.command.AdminPanelCommand;
import ru.example.adminpanel.config.ConfigManager;
import ru.example.adminpanel.config.Messages;
import ru.example.adminpanel.config.PluginSettings;
import ru.example.adminpanel.gui.MenuListener;
import ru.example.adminpanel.gui.MenuManager;
import ru.example.adminpanel.listener.PlayerConnectionListener;
import ru.example.adminpanel.serverevent.ServerEventManager;

/**
 * Главный класс плагина AdminPanel.
 */
public final class AdminPanelPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private AdminActionService actionService;
    private MenuManager menuManager;
    private ServerEventManager serverEventManager;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        configManager.reload();

        actionService = new AdminActionService(this);
        menuManager = new MenuManager(this, actionService);
        serverEventManager = new ServerEventManager(this);

        PluginCommand command = getCommand("adminpanel");
        if (command == null) {
            getLogger().severe("Команда 'adminpanel' не объявлена в plugin.yml — плагин отключён.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        AdminPanelCommand executor = new AdminPanelCommand(this);
        command.setExecutor(executor);
        command.setTabCompleter(executor);

        Bukkit.getPluginManager().registerEvents(new MenuListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerConnectionListener(this), this);

        getLogger().info("AdminPanel " + getDescription().getVersion() + " включён. Язык сообщений: "
                + getMessages().getLanguage());
    }

    @Override
    public void onDisable() {
        if (serverEventManager != null) {
            serverEventManager.shutdown();
        }
        if (menuManager != null) {
            menuManager.closeAll();
        }
        Bukkit.getScheduler().cancelTasks(this);
    }

    /**
     * @return менеджер конфигурации
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }

    /**
     * @return текущий снимок настроек
     */
    public PluginSettings getSettings() {
        return configManager.getSettings();
    }

    /**
     * @return сообщения выбранного языка
     */
    public Messages getMessages() {
        return configManager.getMessages();
    }

    /**
     * @return сервис административных действий (можно использовать из других плагинов)
     */
    public AdminActionService getActionService() {
        return actionService;
    }

    public MenuManager getMenuManager() {
        return menuManager;
    }

    /**
     * @return менеджер серверных событий (можно использовать из других плагинов)
     */
    public ServerEventManager getServerEventManager() {
        return serverEventManager;
    }
}
