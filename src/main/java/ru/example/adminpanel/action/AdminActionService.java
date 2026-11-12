package ru.example.adminpanel.action;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import ru.example.adminpanel.AdminPanelPlugin;
import ru.example.adminpanel.api.AdminActionType;
import ru.example.adminpanel.api.event.AdminActionEvent;
import ru.example.adminpanel.config.Messages;
import ru.example.adminpanel.config.ServerControlSettings;

import java.util.ArrayList;
import java.util.List;

/**
 * Выполняет административные действия. Каждый метод:
 * <ol>
 *     <li>проверяет право на действие;</li>
 *     <li>проверяет, что целевой игрок онлайн;</li>
 *     <li>вызывает отменяемое {@link AdminActionEvent};</li>
 *     <li>выполняет действие и отправляет сообщения.</li>
 * </ol>
 * Все методы вызываются только из основного потока и возвращают {@code true} при успехе.
 */
public final class AdminActionService {

    private final AdminPanelPlugin plugin;

    public AdminActionService(AdminPanelPlugin plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------ действия над игроками

    public boolean heal(CommandSender admin, Player target) {
        if (prepare(admin, AdminActionType.HEAL, target, null, null) == null) {
            return false;
        }
        AttributeInstance maxHealth = target.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        double max = maxHealth != null ? maxHealth.getValue() : 20.0;
        target.setHealth(Math.max(0.0, max));
        target.setFireTicks(0);
        notifyTargeted(admin, target, "healed-self", "healed-target", "healed-by-admin");
        log(admin, AdminActionType.HEAL, target, null);
        return true;
    }

    public boolean feed(CommandSender admin, Player target) {
        if (prepare(admin, AdminActionType.FEED, target, null, null) == null) {
            return false;
        }
        target.setFoodLevel(20);
        target.setSaturation(20.0f);
        target.setExhaustion(0.0f);
        notifyTargeted(admin, target, "fed-self", "fed-target", "fed-by-admin");
        log(admin, AdminActionType.FEED, target, null);
        return true;
    }

    public boolean toggleFly(CommandSender admin, Player target) {
        if (prepare(admin, AdminActionType.FLY, target, null, null) == null) {
            return false;
        }
        boolean enable = !target.getAllowFlight();
        target.setAllowFlight(enable);
        if (!enable) {
            target.setFlying(false);
        }
        Messages messages = plugin.getMessages();
        String state = messages.state(enable);
        notifyTargeted(admin, target, "fly-self", "fly-target", "fly-by-admin", "state", state);
        log(admin, AdminActionType.FLY, target, enable ? "on" : "off");
        return true;
    }

    public boolean setGameMode(CommandSender admin, Player target, GameMode mode) {
        if (mode == null || prepare(admin, AdminActionType.GAMEMODE, target, mode, null) == null) {
            return false;
        }
        target.setGameMode(mode);
        String modeName = plugin.getMessages().gameMode(mode);
        notifyTargeted(admin, target, "gamemode-self", "gamemode-target", "gamemode-by-admin", "mode", modeName);
        log(admin, AdminActionType.GAMEMODE, target, mode.name());
        return true;
    }

    /**
     * Телепортирует администратора к игроку.
     */
    public boolean teleportTo(Player admin, Player target) {
        if (admin.equals(target)) {
            if (hasPermission(admin, AdminActionType.TELEPORT_TO)) {
                plugin.getMessages().send(admin, "teleport-self");
            } else {
                plugin.getMessages().send(admin, "no-permission", "permission", AdminActionType.TELEPORT_TO.getPermission());
            }
            return false;
        }
        if (prepare(admin, AdminActionType.TELEPORT_TO, target, null, null) == null) {
            return false;
        }
        Messages messages = plugin.getMessages();
        if (!admin.teleport(target.getLocation(), PlayerTeleportEvent.TeleportCause.COMMAND)) {
            messages.send(admin, "teleport-failed");
            return false;
        }
        messages.send(admin, "teleported-to", "target", target.getName());
        log(admin, AdminActionType.TELEPORT_TO, target, null);
        return true;
    }

    /**
     * Телепортирует игрока к администратору.
     */
    public boolean teleportHere(Player admin, Player target) {
        if (admin.equals(target)) {
            if (hasPermission(admin, AdminActionType.TELEPORT_HERE)) {
                plugin.getMessages().send(admin, "teleport-self");
            } else {
                plugin.getMessages().send(admin, "no-permission", "permission", AdminActionType.TELEPORT_HERE.getPermission());
            }
            return false;
        }
        if (prepare(admin, AdminActionType.TELEPORT_HERE, target, null, null) == null) {
            return false;
        }
        Messages messages = plugin.getMessages();
        if (!target.teleport(admin.getLocation(), PlayerTeleportEvent.TeleportCause.COMMAND)) {
            messages.send(admin, "teleport-failed");
            return false;
        }
        messages.send(admin, "teleported-here", "target", target.getName());
        messages.send(target, "teleported-by-admin", "admin", admin.getName());
        log(admin, AdminActionType.TELEPORT_HERE, target, null);
        return true;
    }

    // ------------------------------------------------------------------ управление сервером

    public boolean setTime(CommandSender admin, boolean day) {
        AdminActionType type = day ? AdminActionType.TIME_DAY : AdminActionType.TIME_NIGHT;
        if (prepare(admin, type, null, null, null) == null) {
            return false;
        }
        List<World> worlds = resolveWorlds(admin);
        if (worlds.isEmpty()) {
            plugin.getMessages().send(admin, "no-worlds");
            return false;
        }
        ServerControlSettings settings = plugin.getSettings().serverControl();
        long time = day ? settings.dayTime() : settings.nightTime();
        for (World world : worlds) {
            world.setTime(time);
        }
        plugin.getMessages().send(admin, day ? "time-day" : "time-night", "worlds", worldNames(worlds));
        log(admin, type, null, worldNames(worlds));
        return true;
    }

    public boolean setWeather(CommandSender admin, AdminActionType type) {
        if (type != AdminActionType.WEATHER_CLEAR && type != AdminActionType.WEATHER_RAIN
                && type != AdminActionType.WEATHER_THUNDER) {
            throw new IllegalArgumentException("Не погодное действие: " + type);
        }
        if (prepare(admin, type, null, null, null) == null) {
            return false;
        }
        List<World> worlds = resolveWorlds(admin);
        if (worlds.isEmpty()) {
            plugin.getMessages().send(admin, "no-worlds");
            return false;
        }
        int ticks = plugin.getSettings().serverControl().weatherDurationSeconds() * 20;
        for (World world : worlds) {
            switch (type) {
                case WEATHER_CLEAR -> {
                    world.setStorm(false);
                    world.setThundering(false);
                    world.setClearWeatherDuration(ticks);
                }
                case WEATHER_RAIN -> {
                    world.setStorm(true);
                    world.setThundering(false);
                    world.setWeatherDuration(ticks);
                }
                default -> {
                    world.setStorm(true);
                    world.setThundering(true);
                    world.setWeatherDuration(ticks);
                    world.setThunderDuration(ticks);
                }
            }
        }
        String key = switch (type) {
            case WEATHER_CLEAR -> "weather-clear";
            case WEATHER_RAIN -> "weather-rain";
            default -> "weather-thunder";
        };
        plugin.getMessages().send(admin, key, "worlds", worldNames(worlds));
        log(admin, type, null, worldNames(worlds));
        return true;
    }

    /**
     * Отправляет объявление всем игрокам и в консоль.
     */
    public boolean broadcast(CommandSender admin, String text) {
        AdminActionEvent event = prepare(admin, AdminActionType.BROADCAST, null, null, text);
        if (event == null) {
            return false;
        }
        String message = event.getMessage() == null ? "" : event.getMessage();
        if (message.isBlank()) {
            return false;
        }
        String formatted = plugin.getMessages().get("broadcast-format", "message", message,
                "admin", plugin.getMessages().senderName(admin));
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(formatted);
        }
        Bukkit.getConsoleSender().sendMessage(formatted);
        if (admin instanceof Player) {
            plugin.getMessages().send(admin, "broadcast-sent");
        }
        log(admin, AdminActionType.BROADCAST, null, message);
        return true;
    }

    // ------------------------------------------------------------------ вспомогательное

    public boolean hasPermission(CommandSender sender, AdminActionType type) {
        return sender.hasPermission(type.getPermission());
    }

    /**
     * Проверяет право, наличие цели и вызывает {@link AdminActionEvent}.
     *
     * @return событие, если действие можно выполнять, иначе {@code null} (сообщение уже отправлено)
     */
    private AdminActionEvent prepare(CommandSender admin, AdminActionType type, Player target,
                                     GameMode mode, String message) {
        Messages messages = plugin.getMessages();
        if (!hasPermission(admin, type)) {
            messages.send(admin, "no-permission", "permission", type.getPermission());
            return null;
        }
        if (type.isTargeted() && (target == null || !target.isOnline())) {
            messages.send(admin, "player-offline");
            return null;
        }
        AdminActionEvent event = new AdminActionEvent(admin, type, target, mode, message);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            messages.send(admin, "action-cancelled");
            return null;
        }
        // Обработчик мог кикнуть цель.
        if (type.isTargeted() && !target.isOnline()) {
            messages.send(admin, "player-offline");
            return null;
        }
        return event;
    }

    private void notifyTargeted(CommandSender admin, Player target, String selfKey, String targetKey,
                                String byAdminKey, String... extra) {
        Messages messages = plugin.getMessages();
        if (admin instanceof Player player && player.equals(target)) {
            messages.send(admin, selfKey, extra);
            return;
        }
        messages.send(admin, targetKey, withPair(extra, "target", target.getName()));
        messages.send(target, byAdminKey, withPair(extra, "admin", messages.senderName(admin)));
    }

    private static String[] withPair(String[] extra, String key, String value) {
        String[] result = new String[extra.length + 2];
        System.arraycopy(extra, 0, result, 0, extra.length);
        result[extra.length] = key;
        result[extra.length + 1] = value;
        return result;
    }

    /**
     * @return миры из server-control.worlds (отсутствующие пропускаются с сообщением)
     * или все обычные миры, если список пуст
     */
    private List<World> resolveWorlds(CommandSender admin) {
        List<String> names = plugin.getSettings().serverControl().worlds();
        List<World> result = new ArrayList<>();
        if (names.isEmpty()) {
            for (World world : Bukkit.getWorlds()) {
                if (world.getEnvironment() == World.Environment.NORMAL) {
                    result.add(world);
                }
            }
            return result;
        }
        for (String name : names) {
            World world = Bukkit.getWorld(name);
            if (world == null) {
                plugin.getMessages().send(admin, "world-not-found", "world", name);
            } else {
                result.add(world);
            }
        }
        return result;
    }

    private static String worldNames(List<World> worlds) {
        List<String> names = new ArrayList<>(worlds.size());
        for (World world : worlds) {
            names.add(world.getName());
        }
        return String.join(", ", names);
    }

    private void log(CommandSender admin, AdminActionType type, Player target, String details) {
        if (!plugin.getSettings().logActions()) {
            return;
        }
        StringBuilder line = new StringBuilder("[Аудит] ").append(admin.getName()).append(" -> ").append(type);
        if (target != null) {
            line.append(" (цель: ").append(target.getName()).append(')');
        }
        if (details != null && !details.isEmpty()) {
            line.append(" [").append(details).append(']');
        }
        plugin.getLogger().info(line.toString());
    }
}
