package ru.example.adminpanel.config;

import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import ru.example.adminpanel.util.Text;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Сообщения плагина выбранного языка. Строки хранятся «сырыми»,
 * плейсхолдеры подставляются и цвета применяются при каждом получении.
 * Пустая строка в конфигурации отключает сообщение.
 */
public final class Messages {

    private final String language;
    private final Map<String, String> messages;
    private final String prefix;

    public Messages(String language, Map<String, String> messages) {
        this.language = language;
        this.messages = Collections.unmodifiableMap(new HashMap<>(messages));
        this.prefix = this.messages.getOrDefault("prefix", "");
    }

    public String getLanguage() {
        return language;
    }

    /**
     * @return сырая строка (без цветов и плейсхолдеров) или ключ, если сообщения нет
     */
    public String raw(String key) {
        return messages.getOrDefault(key, key);
    }

    /**
     * @return готовое сообщение с подставленными плейсхолдерами и цветами
     */
    public String get(String key, String... placeholders) {
        String template = raw(key);
        if (template.isEmpty()) {
            return "";
        }
        String withPrefix = template.replace("{prefix}", prefix);
        return Text.color(Text.replace(withPrefix, placeholders));
    }

    /**
     * Отправляет сообщение получателю, если оно не отключено пустой строкой.
     */
    public void send(CommandSender receiver, String key, String... placeholders) {
        if (receiver == null) {
            return;
        }
        String message = get(key, placeholders);
        if (!message.isEmpty()) {
            receiver.sendMessage(message);
        }
    }

    /**
     * @return цветная строка «включён/выключен»
     */
    public String state(boolean enabled) {
        return get(enabled ? "state-on" : "state-off");
    }

    /**
     * @return локализованное название игрового режима
     */
    public String gameMode(GameMode mode) {
        if (mode == null) {
            return "";
        }
        return get("gamemode-" + mode.name().toLowerCase(java.util.Locale.ROOT));
    }

    /**
     * @return имя отправителя для сообщений (у консоли — локализованное)
     */
    public String senderName(CommandSender sender) {
        if (sender == null || !(sender instanceof org.bukkit.entity.Player)) {
            return get("console-name");
        }
        return sender.getName();
    }
}
