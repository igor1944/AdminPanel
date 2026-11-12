package ru.example.adminpanel.util;

import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Утилиты для работы с текстом: цветовые коды {@code &}, HEX-цвета {@code &#RRGGBB},
 * плейсхолдеры {@code {key}} и форматирование длительности.
 */
public final class Text {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private Text() {
    }

    /**
     * Переводит коды {@code &} и {@code &#RRGGBB} в цвета Minecraft.
     */
    public static String color(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        Matcher matcher = HEX_PATTERN.matcher(input);
        StringBuilder buffer = new StringBuilder(input.length() + 32);
        while (matcher.find()) {
            String replacement = net.md_5.bungee.api.ChatColor.of("#" + matcher.group(1)).toString();
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    public static List<String> color(List<String> lines) {
        List<String> result = new ArrayList<>(lines.size());
        for (String line : lines) {
            result.add(color(line));
        }
        return result;
    }

    /**
     * Заменяет плейсхолдеры вида {@code {key}}.
     *
     * @param template     исходная строка
     * @param placeholders пары «ключ, значение»: {@code "player", "Steve", "mode", "Творческий"}
     */
    public static String replace(String template, String... placeholders) {
        if (template == null || template.isEmpty() || placeholders.length == 0) {
            return template == null ? "" : template;
        }
        String result = template;
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            String value = placeholders[i + 1];
            result = result.replace("{" + placeholders[i] + "}", value == null ? "" : value);
        }
        return result;
    }

    public static List<String> replace(List<String> lines, String... placeholders) {
        List<String> result = new ArrayList<>(lines.size());
        for (String line : lines) {
            result.add(replace(line, placeholders));
        }
        return result;
    }

    /**
     * Форматирует длительность как {@code ЧЧ:ММ:СС} или {@code ММ:СС}.
     */
    public static String formatDuration(long totalSeconds) {
        long seconds = Math.max(0, totalSeconds);
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, secs);
        }
        return String.format("%02d:%02d", minutes, secs);
    }

    /**
     * Объединяет массив аргументов команды начиная с индекса {@code from}.
     */
    public static String join(String[] args, int from) {
        StringBuilder builder = new StringBuilder();
        for (int i = from; i < args.length; i++) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(args[i]);
        }
        return builder.toString();
    }
}
