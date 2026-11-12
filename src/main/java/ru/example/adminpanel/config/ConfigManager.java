package ru.example.adminpanel.config;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import ru.example.adminpanel.api.MenuType;
import ru.example.adminpanel.gui.ButtonType;
import ru.example.adminpanel.serverevent.ServerEventDefinition;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Загружает {@code plugins/AdminPanel/config.yml}, проверяет значения и строит неизменяемый
 * {@link PluginSettings}. Любое некорректное значение заменяется значением по умолчанию
 * (из config.yml внутри JAR или жёстко заданным) с предупреждением в консоли.
 */
public final class ConfigManager {

    private static final String FILE_NAME = "config.yml";
    private static final String FALLBACK_LANGUAGE = "ru";
    private static final Pattern EVENT_ID_PATTERN = Pattern.compile("[a-z0-9_-]{1,32}");
    private static final int MAX_EVENT_DURATION = 7 * 24 * 60 * 60;

    private final Plugin plugin;
    private final Logger logger;
    private volatile PluginSettings settings;

    public ConfigManager(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    /**
     * @return текущий снимок настроек (никогда не {@code null} после первого {@link #reload()})
     */
    public PluginSettings getSettings() {
        return settings;
    }

    public Messages getMessages() {
        return settings.messages();
    }

    /**
     * Перечитывает config.yml с диска. Не трогает другие плагины и не вызывает {@code Bukkit.reload()}.
     *
     * @return количество предупреждений (0 — конфигурация корректна)
     */
    public int reload() {
        Parser parser = new Parser(loadDefaults(), null);
        YamlConfiguration user = loadUserConfig(parser);
        user.setDefaults(parser.defaults);
        parser.config = user;
        this.settings = parser.parse();
        if (parser.warnings > 0) {
            logger.warning("Конфигурация загружена с предупреждениями: " + parser.warnings
                    + ". Для некорректных значений использованы значения по умолчанию.");
        } else {
            logger.info("Конфигурация загружена без ошибок.");
        }
        return parser.warnings;
    }

    private YamlConfiguration loadDefaults() {
        try (InputStream in = plugin.getResource(FILE_NAME)) {
            if (in == null) {
                logger.severe("В JAR отсутствует " + FILE_NAME + " — используются встроенные значения.");
                return new YamlConfiguration();
            }
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return YamlConfiguration.loadConfiguration(reader);
            }
        } catch (IOException e) {
            logger.severe("Не удалось прочитать встроенный " + FILE_NAME + ": " + e.getMessage());
            return new YamlConfiguration();
        }
    }

    private YamlConfiguration loadUserConfig(Parser parser) {
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        if (!file.exists()) {
            plugin.saveResource(FILE_NAME, false);
            logger.info("Создан файл конфигурации по умолчанию: " + file.getPath());
        }
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            parser.warn("Не удалось прочитать " + file.getPath() + ": " + e.getMessage()
                    + " — весь файл заменён значениями по умолчанию до исправления ошибки.");
            return new YamlConfiguration();
        }
        return config;
    }

    /**
     * Разбор и проверка значений. Отдельный объект на каждую перезагрузку.
     */
    private final class Parser {

        private final YamlConfiguration defaults;
        private YamlConfiguration config;
        private int warnings;

        private Parser(YamlConfiguration defaults, YamlConfiguration config) {
            this.defaults = defaults;
            this.config = config;
        }

        private void warn(String message) {
            warnings++;
            logger.warning("[config.yml] " + message);
        }

        private PluginSettings parse() {
            String language = parseLanguage();
            Messages messages = new Messages(language, parseMessages(language));
            boolean logActions = readBoolean("log-actions", true);
            ServerControlSettings serverControl = parseServerControl();
            GuiSettings gui = parseGui();
            Map<String, ServerEventDefinition> events = parseEvents();
            return new PluginSettings(messages, logActions, serverControl, gui, events);
        }

        // ------------------------------------------------------------------ язык и сообщения

        private String parseLanguage() {
            String language = readString("language", FALLBACK_LANGUAGE).trim().toLowerCase(Locale.ROOT);
            if (!config.isConfigurationSection("messages." + language)) {
                warn("Язык '" + language + "' не найден в секции messages — используется '" + FALLBACK_LANGUAGE + "'.");
                return FALLBACK_LANGUAGE;
            }
            return language;
        }

        private Map<String, String> parseMessages(String language) {
            Map<String, String> result = new HashMap<>();
            // 1) встроенный русский, 2) встроенный выбранный язык, 3) пользовательский выбранный язык
            putMessages(result, defaults.getConfigurationSection("messages." + FALLBACK_LANGUAGE));
            putMessages(result, defaults.getConfigurationSection("messages." + language));
            ConfigurationSection userSection = config.getConfigurationSection("messages." + language);
            if (userSection != null) {
                Set<String> known = new HashSet<>(result.keySet());
                for (String key : userSection.getKeys(false)) {
                    if (!known.isEmpty() && !known.contains(key)) {
                        warn("Неизвестный ключ сообщения messages." + language + "." + key + " — будет проигнорирован.");
                    }
                }
                putMessages(result, userSection);
            }
            return result;
        }

        private void putMessages(Map<String, String> target, ConfigurationSection section) {
            if (section == null) {
                return;
            }
            for (String key : section.getKeys(false)) {
                Object value = section.get(key);
                if (value instanceof List<?> list) {
                    List<String> lines = new ArrayList<>();
                    for (Object line : list) {
                        lines.add(String.valueOf(line));
                    }
                    target.put(key, String.join("\n", lines));
                } else if (value instanceof ConfigurationSection) {
                    warn("Сообщение " + section.getCurrentPath() + "." + key + " должно быть строкой.");
                } else if (value != null) {
                    target.put(key, String.valueOf(value));
                }
            }
        }

        // ------------------------------------------------------------------ сервер

        private ServerControlSettings parseServerControl() {
            List<String> worlds = new ArrayList<>();
            Object rawWorlds = config.get("server-control.worlds");
            if (rawWorlds instanceof List<?> list) {
                for (Object entry : list) {
                    String name = String.valueOf(entry).trim();
                    if (name.isEmpty()) {
                        continue;
                    }
                    if (!worlds.contains(name)) {
                        worlds.add(name);
                    }
                }
            } else if (rawWorlds != null) {
                warn("server-control.worlds должен быть списком — используются все обычные миры.");
            }
            for (String name : worlds) {
                if (Bukkit.getWorld(name) == null) {
                    warn("Мир '" + name + "' из server-control.worlds сейчас не загружен — он будет пропускаться.");
                }
            }
            long day = readInt("server-control.day-time", 1000, 0, 23999);
            long night = readInt("server-control.night-time", 13000, 0, 23999);
            int weather = readInt("server-control.weather-duration", 600, 1, 86400);
            return new ServerControlSettings(worlds, day, night, weather);
        }

        // ------------------------------------------------------------------ GUI

        private GuiSettings parseGui() {
            long cooldown = readInt("gui.click-cooldown-ms", 250, 0, 10000);
            String noAccess = readString("gui.no-access-lore", "&c✖ Недостаточно прав");
            boolean sounds = readBoolean("gui.sounds.enabled", true);
            Sound click = readSound("gui.sounds.click");
            Sound deny = readSound("gui.sounds.deny");
            float volume = (float) readDouble("gui.sounds.volume", 0.7, 0.0, 10.0);
            float pitch = (float) readDouble("gui.sounds.pitch", 1.0, 0.5, 2.0);

            Map<MenuType, MenuSettings> menus = new EnumMap<>(MenuType.class);
            for (MenuType type : MenuType.values()) {
                menus.put(type, parseMenu(type));
            }
            return new GuiSettings(menus, cooldown, noAccess, sounds, click, deny, volume, pitch);
        }

        private MenuSettings parseMenu(MenuType type) {
            String base = "menus." + type.getConfigKey();
            int hardSize = type == MenuType.MAIN ? 54 : 27;
            String title = readString(base + ".title", "&8AdminPanel");

            int size = readInt(base + ".size", hardSize, 9, 54);
            if (size % 9 != 0) {
                int fallback = defaults.getInt(base + ".size", hardSize);
                warn(base + ".size = " + size + " — размер должен быть кратен 9 (9, 18, 27, 36, 45, 54). Используется " + fallback + ".");
                size = fallback;
            }

            Material filler = null;
            if (readBoolean(base + ".filler.enabled", true)) {
                filler = readMaterial(base + ".filler.material", Material.GRAY_STAINED_GLASS_PANE);
            }

            warnUnknownButtons(type, base + ".buttons");
            Map<ButtonType, ButtonSettings> buttons = new EnumMap<>(ButtonType.class);
            Set<Integer> usedSlots = new HashSet<>();
            for (ButtonType buttonType : ButtonType.forMenu(type)) {
                ButtonSettings button = parseButton(buttonType.getConfigPath(), size, usedSlots);
                if (button != null) {
                    buttons.put(buttonType, button);
                }
            }

            List<Integer> playerSlots = List.of();
            ButtonSettings playerHead = null;
            if (type == MenuType.MAIN) {
                playerSlots = parsePlayerSlots(base + ".player-slots", size, usedSlots);
                playerHead = new ButtonSettings(true, -1, Material.PLAYER_HEAD,
                        readString(base + ".player-head.name", "&e{player}"),
                        readLines(base + ".player-head.lore"), false);
            }
            return new MenuSettings(title, size, filler, buttons, playerSlots, playerHead);
        }

        private void warnUnknownButtons(MenuType type, String path) {
            ConfigurationSection section = config.getConfigurationSection(path);
            if (section == null) {
                return;
            }
            Set<String> known = new LinkedHashSet<>();
            for (ButtonType buttonType : ButtonType.forMenu(type)) {
                known.add(buttonType.getKey());
            }
            for (String key : section.getKeys(false)) {
                if (!known.contains(key)) {
                    warn("Неизвестная кнопка " + path + "." + key + " — допустимые: " + String.join(", ", known) + ".");
                }
            }
        }

        /**
         * @return настройки кнопки или {@code null}, если кнопка выключена или некорректна
         */
        private ButtonSettings parseButton(String path, int menuSize, Set<Integer> usedSlots) {
            if (!config.isConfigurationSection(path)) {
                warn("Отсутствует секция кнопки " + path + " — кнопка скрыта.");
                return null;
            }
            if (!readBoolean(path + ".enabled", true)) {
                return null;
            }
            int slot = readInt(path + ".slot", -1, -1, 53);
            if (slot < 0 || slot >= menuSize) {
                warn(path + ".slot = " + slot + " — слот вне меню размером " + menuSize + ". Кнопка скрыта.");
                return null;
            }
            if (!usedSlots.add(slot)) {
                warn(path + ".slot = " + slot + " — слот уже занят другой кнопкой. Кнопка скрыта.");
                return null;
            }
            Material material = readMaterial(path + ".material", Material.STONE);
            String name = readString(path + ".name", "");
            List<String> lore = readLines(path + ".lore");
            boolean glow = readBoolean(path + ".glow", false);
            return new ButtonSettings(true, slot, material, name, lore, glow);
        }

        private List<Integer> parsePlayerSlots(String path, int menuSize, Set<Integer> usedSlots) {
            Object raw = config.get(path);
            List<Object> entries = new ArrayList<>();
            if (raw instanceof List<?> list) {
                entries.addAll(list);
            } else if (raw != null) {
                entries.add(raw);
            }
            Set<Integer> slots = new LinkedHashSet<>();
            for (Object entry : entries) {
                String text = String.valueOf(entry).trim();
                try {
                    if (text.contains("-")) {
                        String[] parts = text.split("-", 2);
                        int from = Integer.parseInt(parts[0].trim());
                        int to = Integer.parseInt(parts[1].trim());
                        if (from > to) {
                            int swap = from;
                            from = to;
                            to = swap;
                        }
                        for (int slot = from; slot <= to; slot++) {
                            addPlayerSlot(path, slot, menuSize, usedSlots, slots);
                        }
                    } else {
                        addPlayerSlot(path, Integer.parseInt(text), menuSize, usedSlots, slots);
                    }
                } catch (NumberFormatException ignored) {
                    warn(path + ": '" + text + "' не является номером слота или диапазоном вида 18-44.");
                }
            }
            if (slots.isEmpty()) {
                warn(path + ": нет ни одного корректного слота — список игроков не будет отображаться.");
            }
            return new ArrayList<>(slots);
        }

        private void addPlayerSlot(String path, int slot, int menuSize, Set<Integer> usedSlots, Set<Integer> slots) {
            if (slot < 0 || slot >= menuSize) {
                warn(path + ": слот " + slot + " вне меню размером " + menuSize + " — пропущен.");
            } else if (usedSlots.contains(slot)) {
                warn(path + ": слот " + slot + " занят кнопкой — пропущен.");
            } else {
                slots.add(slot);
            }
        }

        // ------------------------------------------------------------------ серверные события

        private Map<String, ServerEventDefinition> parseEvents() {
            Map<String, ServerEventDefinition> events = new LinkedHashMap<>();
            ConfigurationSection root;
            if (config.contains("server-events", true)) {
                root = config.getConfigurationSection("server-events");
                if (root == null) {
                    warn("server-events должен быть секцией — серверные события не загружены.");
                    return events;
                }
            } else {
                // Секции нет в пользовательском файле — берём события по умолчанию.
                root = defaults.getConfigurationSection("server-events");
                if (root == null) {
                    return events;
                }
            }
            for (String key : root.getKeys(false)) {
                String id = key.toLowerCase(Locale.ROOT);
                String path = "server-events." + key;
                if (!EVENT_ID_PATTERN.matcher(id).matches()) {
                    warn(path + ": некорректный ID — допустимы латинские буквы, цифры, '_' и '-' (до 32 символов). Событие пропущено.");
                    continue;
                }
                if (events.containsKey(id)) {
                    warn(path + ": событие с ID '" + id + "' уже объявлено. Повтор пропущен.");
                    continue;
                }
                if (!config.isConfigurationSection(path)) {
                    warn(path + " должен быть секцией. Событие пропущено.");
                    continue;
                }
                events.put(id, parseEvent(id, path));
            }
            return events;
        }

        private ServerEventDefinition parseEvent(String id, String path) {
            String name = readString(path + ".name", id);
            if (config.get(path + ".duration") == null) {
                warn(path + ".duration не указан — используется 300 секунд.");
            }
            int duration = readInt(path + ".duration", 300, 1, MAX_EVENT_DURATION);

            List<String> start = readLines(path + ".start-message");
            if (start.isEmpty() && config.get(path + ".start-message") == null) {
                warn(path + ".start-message не указан — используется стандартное сообщение.");
                start = List.of("&aСобытие «{name}&a» началось! Длительность: &f{duration}");
            }
            List<String> stop = readLines(path + ".stop-message");
            if (stop.isEmpty() && config.get(path + ".stop-message") == null) {
                warn(path + ".stop-message не указан — используется стандартное сообщение.");
                stop = List.of("&cСобытие «{name}&c» завершено.");
            }
            String title = readString(path + ".title", "");
            String subtitle = readString(path + ".subtitle", "");

            boolean barEnabled = readBoolean(path + ".bossbar.enabled", false);
            String barText = readString(path + ".bossbar.text", "{name} &7— &f{remaining}");
            BarColor color = readEnum(path + ".bossbar.color", BarColor.class, BarColor.WHITE);
            BarStyle style = readEnum(path + ".bossbar.style", BarStyle.class, BarStyle.SOLID);
            return new ServerEventDefinition(id, name, duration, start, stop, title, subtitle,
                    new ServerEventDefinition.BossBarSettings(barEnabled, barText, color, style));
        }

        // ------------------------------------------------------------------ примитивы

        private String readString(String path, String hardDefault) {
            Object value = config.get(path);
            if (value == null) {
                return defaults.getString(path, hardDefault);
            }
            if (value instanceof ConfigurationSection || value instanceof List<?>) {
                String fallback = defaults.getString(path, hardDefault);
                warn(path + " должен быть строкой — используется значение по умолчанию.");
                return fallback;
            }
            return String.valueOf(value);
        }

        private boolean readBoolean(String path, boolean hardDefault) {
            Object value = config.get(path);
            boolean fallback = defaults.isBoolean(path) ? defaults.getBoolean(path) : hardDefault;
            if (value == null) {
                return fallback;
            }
            if (value instanceof Boolean bool) {
                return bool;
            }
            String text = String.valueOf(value).trim();
            if (text.equalsIgnoreCase("true") || text.equalsIgnoreCase("false")) {
                return Boolean.parseBoolean(text);
            }
            warn(path + " = '" + text + "' — ожидается true или false. Используется " + fallback + ".");
            return fallback;
        }

        private int readInt(String path, int hardDefault, int min, int max) {
            int fallback = defaults.isInt(path) ? defaults.getInt(path) : hardDefault;
            Object value = config.get(path);
            if (value == null) {
                return fallback;
            }
            int result;
            if (value instanceof Integer || value instanceof Long || value instanceof Short || value instanceof Byte) {
                long number = ((Number) value).longValue();
                if (number < Integer.MIN_VALUE || number > Integer.MAX_VALUE) {
                    warn(path + " = " + number + " — слишком большое число. Используется " + fallback + ".");
                    return fallback;
                }
                result = (int) number;
            } else {
                try {
                    result = Integer.parseInt(String.valueOf(value).trim());
                } catch (NumberFormatException ignored) {
                    warn(path + " = '" + value + "' — ожидается целое число. Используется " + fallback + ".");
                    return fallback;
                }
            }
            if (result < min || result > max) {
                warn(path + " = " + result + " — допустимый диапазон " + min + ".." + max + ". Используется " + fallback + ".");
                return fallback;
            }
            return result;
        }

        private double readDouble(String path, double hardDefault, double min, double max) {
            double fallback = defaults.isDouble(path) || defaults.isInt(path) ? defaults.getDouble(path) : hardDefault;
            Object value = config.get(path);
            if (value == null) {
                return fallback;
            }
            double result;
            if (value instanceof Number number) {
                result = number.doubleValue();
            } else {
                try {
                    result = Double.parseDouble(String.valueOf(value).trim());
                } catch (NumberFormatException ignored) {
                    warn(path + " = '" + value + "' — ожидается число. Используется " + fallback + ".");
                    return fallback;
                }
            }
            if (Double.isNaN(result) || result < min || result > max) {
                warn(path + " = " + result + " — допустимый диапазон " + min + ".." + max + ". Используется " + fallback + ".");
                return fallback;
            }
            return result;
        }

        /**
         * Читает строку или список строк. Отсутствующее значение — пустой список.
         */
        private List<String> readLines(String path) {
            Object value = config.get(path);
            List<String> lines = new ArrayList<>();
            if (value instanceof List<?> list) {
                for (Object line : list) {
                    lines.add(line == null ? "" : String.valueOf(line));
                }
            } else if (value instanceof ConfigurationSection) {
                warn(path + " должен быть строкой или списком строк — значение проигнорировано.");
            } else if (value != null) {
                String text = String.valueOf(value);
                if (!text.isEmpty()) {
                    lines.add(text);
                }
            }
            return lines;
        }

        private Material readMaterial(String path, Material hardDefault) {
            Material fallback = parseMaterial(defaults.getString(path));
            if (fallback == null) {
                fallback = hardDefault;
            }
            String name = config.getString(path);
            if (name == null) {
                return fallback;
            }
            Material material = parseMaterial(name);
            if (material == null) {
                warn(path + " = '" + name + "' — неизвестный материал или он не является предметом. Используется " + fallback + ".");
                return fallback;
            }
            return material;
        }

        private Material parseMaterial(String name) {
            if (name == null || name.isBlank()) {
                return null;
            }
            Material material = Material.matchMaterial(name.trim());
            if (material == null || material.name().startsWith("LEGACY_") || !material.isItem() || material.isAir()) {
                return null;
            }
            return material;
        }

        private Sound readSound(String path) {
            Sound fallback = parseEnumValue(Sound.class, defaults.getString(path));
            String name = config.getString(path);
            if (name == null || name.isBlank()) {
                return fallback;
            }
            Sound sound = parseEnumValue(Sound.class, name);
            if (sound == null) {
                warn(path + " = '" + name + "' — неизвестный звук. Используется " + fallback + ".");
                return fallback;
            }
            return sound;
        }

        private <E extends Enum<E>> E readEnum(String path, Class<E> type, E hardDefault) {
            E fallback = parseEnumValue(type, defaults.getString(path));
            if (fallback == null) {
                fallback = hardDefault;
            }
            String name = config.getString(path);
            if (name == null || name.isBlank()) {
                return fallback;
            }
            E value = parseEnumValue(type, name);
            if (value == null) {
                warn(path + " = '" + name + "' — недопустимое значение. Используется " + fallback + ".");
                return fallback;
            }
            return value;
        }

        private <E extends Enum<E>> E parseEnumValue(Class<E> type, String name) {
            if (name == null || name.isBlank()) {
                return null;
            }
            try {
                return Enum.valueOf(type, name.trim().toUpperCase(Locale.ROOT).replace('.', '_').replace(' ', '_'));
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
    }
}
