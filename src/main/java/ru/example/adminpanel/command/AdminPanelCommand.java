package ru.example.adminpanel.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import ru.example.adminpanel.AdminPanelPlugin;
import ru.example.adminpanel.Permissions;
import ru.example.adminpanel.config.Messages;
import ru.example.adminpanel.serverevent.ActiveServerEvent;
import ru.example.adminpanel.serverevent.ServerEventDefinition;
import ru.example.adminpanel.serverevent.ServerEventManager;
import ru.example.adminpanel.serverevent.StopReason;
import ru.example.adminpanel.util.Text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Обработчик команды {@code /adminpanel} и подсказок (tab-complete).
 * <pre>
 * /adminpanel                      — открыть меню (только игрок, adminpanel.use)
 * /adminpanel help                 — справка
 * /adminpanel reload               — перезагрузить конфигурацию (adminpanel.reload)
 * /adminpanel event start &lt;id&gt;     — запустить событие (adminpanel.event)
 * /adminpanel event stop           — остановить событие (adminpanel.event)
 * /adminpanel event list           — список событий (adminpanel.event)
 * /adminpanel broadcast &lt;текст&gt;    — объявление (adminpanel.action.broadcast)
 * </pre>
 */
public final class AdminPanelCommand implements TabExecutor {

    private static final List<String> EVENT_SUBCOMMANDS = List.of("start", "stop", "list");

    private final AdminPanelPlugin plugin;

    public AdminPanelCommand(AdminPanelPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Messages messages = plugin.getMessages();
        if (args.length == 0) {
            openMenu(sender, label);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "help", "?" -> sendHelp(sender, label);
            case "reload" -> reload(sender);
            case "event", "events" -> event(sender, label, args);
            case "broadcast", "bc" -> broadcast(sender, label, args);
            default -> messages.send(sender, "unknown-subcommand", "label", label);
        }
        return true;
    }

    // ------------------------------------------------------------------ подкоманды

    private void openMenu(CommandSender sender, String label) {
        Messages messages = plugin.getMessages();
        if (!(sender instanceof Player player)) {
            messages.send(sender, "players-only", "label", label);
            return;
        }
        if (!checkPermission(sender, Permissions.USE)) {
            return;
        }
        plugin.getMenuManager().openMain(player, 0);
    }

    private void sendHelp(CommandSender sender, String label) {
        Messages messages = plugin.getMessages();
        messages.send(sender, "help-header", "label", label);
        if (sender instanceof Player && sender.hasPermission(Permissions.USE)) {
            messages.send(sender, "help-open", "label", label);
        }
        if (sender.hasPermission(Permissions.RELOAD)) {
            messages.send(sender, "help-reload", "label", label);
        }
        if (sender.hasPermission(Permissions.EVENT)) {
            messages.send(sender, "help-event-start", "label", label);
            messages.send(sender, "help-event-stop", "label", label);
            messages.send(sender, "help-event-list", "label", label);
        }
        if (sender.hasPermission(Permissions.ACTION_BROADCAST)) {
            messages.send(sender, "help-broadcast", "label", label);
        }
    }

    private void reload(CommandSender sender) {
        if (!checkPermission(sender, Permissions.RELOAD)) {
            return;
        }
        // Закрываем меню, чтобы никто не нажимал кнопки старой конфигурации.
        plugin.getMenuManager().closeAll();
        int warnings = plugin.getConfigManager().reload();
        Messages messages = plugin.getMessages();
        if (warnings == 0) {
            messages.send(sender, "reload-success");
        } else {
            messages.send(sender, "reload-warnings", "count", String.valueOf(warnings));
        }
    }

    private void event(CommandSender sender, String label, String[] args) {
        if (!checkPermission(sender, Permissions.EVENT)) {
            return;
        }
        Messages messages = plugin.getMessages();
        if (args.length < 2) {
            messages.send(sender, "event-usage", "label", label);
            return;
        }
        ServerEventManager manager = plugin.getServerEventManager();
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "start" -> {
                if (args.length < 3) {
                    messages.send(sender, "event-start-usage", "label", label, "events", eventIds());
                    return;
                }
                startEvent(sender, label, args[2]);
            }
            case "stop" -> {
                Optional<ActiveServerEvent> active = manager.getActive();
                if (active.isEmpty()) {
                    messages.send(sender, "event-not-active");
                    return;
                }
                String name = active.get().getDefinition().displayName();
                manager.stop(StopReason.COMMAND, sender);
                messages.send(sender, "event-stopped", "name", name);
            }
            case "list" -> listEvents(sender);
            default -> messages.send(sender, "event-usage", "label", label);
        }
    }

    private void startEvent(CommandSender sender, String label, String id) {
        Messages messages = plugin.getMessages();
        ServerEventManager manager = plugin.getServerEventManager();
        ServerEventManager.StartResult result = manager.start(id, sender);
        switch (result) {
            case STARTED -> {
                ActiveServerEvent active = manager.getActive().orElse(null);
                if (active != null) {
                    messages.send(sender, "event-started",
                            "name", active.getDefinition().displayName(),
                            "duration", Text.formatDuration(active.getDurationSeconds()));
                }
            }
            case UNKNOWN_EVENT -> messages.send(sender, "event-unknown", "id", id, "events", eventIds());
            case ALREADY_ACTIVE -> {
                ActiveServerEvent active = manager.getActive().orElse(null);
                messages.send(sender, "event-already-active", "label", label,
                        "name", active == null ? "?" : active.getDefinition().displayName(),
                        "remaining", active == null ? "?" : Text.formatDuration(active.getRemainingSeconds()));
            }
            case CANCELLED -> messages.send(sender, "event-cancelled");
        }
    }

    private void listEvents(CommandSender sender) {
        Messages messages = plugin.getMessages();
        Map<String, ServerEventDefinition> events = plugin.getSettings().events();
        ActiveServerEvent active = plugin.getServerEventManager().getActive().orElse(null);
        if (events.isEmpty() && active == null) {
            messages.send(sender, "event-list-empty");
            return;
        }
        messages.send(sender, "event-list-header");
        boolean activeListed = false;
        for (ServerEventDefinition definition : events.values()) {
            if (active != null && active.getDefinition().id().equals(definition.id())) {
                sendActiveEntry(sender, active);
                activeListed = true;
            } else {
                messages.send(sender, "event-list-entry",
                        "id", definition.id(),
                        "name", definition.displayName(),
                        "duration", Text.formatDuration(definition.durationSeconds()));
            }
        }
        // Активное событие могло быть удалено из конфигурации после reload — всё равно показываем его.
        if (active != null && !activeListed) {
            sendActiveEntry(sender, active);
        }
    }

    private void sendActiveEntry(CommandSender sender, ActiveServerEvent active) {
        plugin.getMessages().send(sender, "event-list-active",
                "id", active.getDefinition().id(),
                "name", active.getDefinition().displayName(),
                "duration", Text.formatDuration(active.getDurationSeconds()),
                "remaining", Text.formatDuration(active.getRemainingSeconds()));
    }

    private void broadcast(CommandSender sender, String label, String[] args) {
        if (!checkPermission(sender, Permissions.ACTION_BROADCAST)) {
            return;
        }
        String text = Text.join(args, 1).trim();
        if (text.isEmpty()) {
            plugin.getMessages().send(sender, "broadcast-usage", "label", label);
            return;
        }
        plugin.getActionService().broadcast(sender, text);
    }

    // ------------------------------------------------------------------ подсказки

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            options.add("help");
            if (sender.hasPermission(Permissions.RELOAD)) {
                options.add("reload");
            }
            if (sender.hasPermission(Permissions.EVENT)) {
                options.add("event");
            }
            if (sender.hasPermission(Permissions.ACTION_BROADCAST)) {
                options.add("broadcast");
            }
            return filter(args[0], options);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if ((sub.equals("event") || sub.equals("events")) && sender.hasPermission(Permissions.EVENT)) {
            if (args.length == 2) {
                return filter(args[1], EVENT_SUBCOMMANDS);
            }
            if (args.length == 3 && args[1].equalsIgnoreCase("start")) {
                return filter(args[2], new ArrayList<>(plugin.getSettings().events().keySet()));
            }
        }
        return Collections.emptyList();
    }

    // ------------------------------------------------------------------ вспомогательное

    private boolean checkPermission(CommandSender sender, String permission) {
        if (sender.hasPermission(permission)) {
            return true;
        }
        plugin.getMessages().send(sender, "no-permission", "permission", permission);
        return false;
    }

    private String eventIds() {
        Map<String, ServerEventDefinition> events = plugin.getSettings().events();
        return events.isEmpty() ? "-" : String.join(", ", events.keySet());
    }

    private static List<String> filter(String token, List<String> options) {
        List<String> result = new ArrayList<>();
        StringUtil.copyPartialMatches(token, options, result);
        Collections.sort(result);
        return result;
    }
}
