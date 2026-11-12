# AdminPanel

GUI-панель администратора для **Purpur 1.18.2** (работает на любом форке Paper 1.18.2).
Java 17, Maven, Paper API 1.18.2 (`provided`). Без NMS, Reflection и сторонних библиотек.

Возможности:

* меню на 54 слота: действия над собой (здоровье, голод, полёт, игровой режим);
* список игроков онлайн в виде голов с постраничной навигацией → меню игрока
  (телепорт к нему / к себе, лечение, еда, полёт, режим игры);
* управление сервером: день/ночь, ясно/дождь/гроза для миров из конфигурации;
* настраиваемые серверные события с таймером, боссбаром и сообщениями;
* собственные Bukkit-события для интеграции с другими плагинами;
* все тексты, кнопки, слоты, материалы и сообщения настраиваются в `config.yml` (`&` и `&#RRGGBB`).

## Структура проекта

```
AdminPanel/
├── pom.xml
└── src/main/
    ├── resources/
    │   ├── plugin.yml
    │   └── config.yml
    └── java/ru/example/adminpanel/
        ├── AdminPanelPlugin.java            главный класс плагина
        ├── Permissions.java                 константы прав
        ├── action/
        │   └── AdminActionService.java      выполнение действий + AdminActionEvent
        ├── api/
        │   ├── AdminActionType.java         тип действия (и нужное право)
        │   ├── MenuType.java                тип меню
        │   └── event/
        │       ├── AdminMenuOpenEvent.java      (Cancellable)
        │       ├── AdminActionEvent.java        (Cancellable)
        │       ├── ServerEventStartEvent.java   (Cancellable)
        │       └── ServerEventStopEvent.java
        ├── command/
        │   └── AdminPanelCommand.java       /adminpanel + tab-complete
        ├── config/
        │   ├── ConfigManager.java           загрузка и проверка config.yml
        │   ├── Messages.java                сообщения выбранного языка
        │   ├── PluginSettings.java          неизменяемый снимок настроек
        │   ├── GuiSettings.java
        │   ├── MenuSettings.java
        │   ├── ButtonSettings.java
        │   └── ServerControlSettings.java
        ├── gui/
        │   ├── AdminMenuHolder.java         собственный InventoryHolder
        │   ├── ButtonType.java              реестр кнопок: ключ конфига → действие
        │   ├── MenuAction.java              внутренние ID действий
        │   ├── MenuButton.java              кнопка в слоте
        │   ├── MenuManager.java             открытие, отрисовка, действия кнопок
        │   ├── MenuListener.java            InventoryClick/Drag/Close
        │   └── ItemFactory.java             создание предметов
        ├── listener/
        │   └── PlayerConnectionListener.java  вход/выход игроков
        ├── serverevent/
        │   ├── ServerEventManager.java      запуск/остановка/таймер
        │   ├── ServerEventDefinition.java   описание события из конфига
        │   ├── ActiveServerEvent.java       состояние запущенного события
        │   └── StopReason.java
        └── util/
            └── Text.java                    цвета, плейсхолдеры, формат времени
```

## Сборка

Нужны JDK 17+ и Maven 3.6+:

```bash
mvn clean package
```

Maven скачает `paper-api:1.18.2-R0.1-SNAPSHOT` из `https://repo.papermc.io/repository/maven-public/`.

## Установка

1. Скопируйте **`target/AdminPanel-1.0.0.jar`** в папку `plugins/` сервера Purpur 1.18.2.
2. Запустите сервер — будет создан `plugins/AdminPanel/config.yml`.
3. Отредактируйте конфигурацию и выполните `/adminpanel reload` (перезагружается только конфигурация плагина,
   `Bukkit.reload()` не вызывается).

## Права

| Право | Что даёт |
|---|---|
| `adminpanel.use` | `/adminpanel` и использование меню |
| `adminpanel.reload` | `/adminpanel reload` |
| `adminpanel.event` | `/adminpanel event start/stop/list` |
| `adminpanel.action.heal` | восстановить здоровье |
| `adminpanel.action.feed` | восстановить голод |
| `adminpanel.action.fly` | включить/выключить полёт |
| `adminpanel.action.gamemode` | сменить игровой режим |
| `adminpanel.action.teleport` | телепорт к игроку / игрока к себе |
| `adminpanel.action.weather` | погода |
| `adminpanel.action.time` | день / ночь |
| `adminpanel.action.broadcast` | `/adminpanel broadcast` |
| `adminpanel.action.*` | все действия |
| `adminpanel.*` | всё перечисленное |

Все права по умолчанию `default: op`. В меню право проверяется **при каждом клике**; для кнопок, на которые
у администратора нет права, в описании показывается строка `gui.no-access-lore`.

Выдача прав через LuckPerms:

```
/lp group admin permission set adminpanel.* true
# или выборочно:
/lp group moderator permission set adminpanel.use true
/lp group moderator permission set adminpanel.action.teleport true
/lp group moderator permission set adminpanel.action.heal true
/lp user Steve permission set adminpanel.event true
```

## Команды

Алиасы: `/apanel`, `/ap`.

| Команда | Право | Описание |
|---|---|---|
| `/adminpanel` | `adminpanel.use` | открыть главное меню (только игрок) |
| `/adminpanel help` | — | список доступных подкоманд |
| `/adminpanel reload` | `adminpanel.reload` | перезагрузить config.yml |
| `/adminpanel event start <id>` | `adminpanel.event` | запустить событие |
| `/adminpanel event stop` | `adminpanel.event` | остановить активное событие |
| `/adminpanel event list` | `adminpanel.event` | список событий и активное событие |
| `/adminpanel broadcast <текст>` | `adminpanel.action.broadcast` | объявление всем игрокам |

Примеры:

```
/adminpanel
/adminpanel event start double-xp
/adminpanel event stop
/ap event list
/adminpanel broadcast &cРестарт сервера через 5 минут!
/adminpanel reload
```

Консоль может выполнять все подкоманды, кроме открытия меню.

## Конфигурация

* `language` — язык сообщений (`ru`, `en`; секция `messages.<язык>`).
* `server-control.worlds` — миры для времени и погоды (`[]` — все обычные миры).
* `menus.<main|player|gamemode>` — заголовок, размер, заполнитель и кнопки
  (`enabled`, `slot`, `material`, `name`, `lore`, `glow`). Действие кнопки определяется её **ключом**,
  а не названием, поэтому названия можно менять свободно.
* `server-events.<id>` — `name`, `duration` (секунды), `start-message`, `stop-message`,
  необязательные `title`/`subtitle` и `bossbar`.

При любой ошибке (неизвестный материал, слот вне меню, неверный размер, синтаксическая ошибка YAML и т. д.)
используется безопасное значение по умолчанию, а в консоль пишется предупреждение с путём к параметру.

## API для других плагинов

Все события вызываются синхронно в основном потоке. `AdminMenuOpenEvent`, `AdminActionEvent` и
`ServerEventStartEvent` реализуют `Cancellable`.

`plugin.yml` вашего плагина:

```yaml
depend: [AdminPanel]   # или softdepend
```

`pom.xml` — установите AdminPanel в локальный репозиторий (`mvn install`) и подключите:

```xml
<dependency>
    <groupId>ru.example</groupId>
    <artifactId>AdminPanel</artifactId>
    <version>1.0.0</version>
    <scope>provided</scope>
</dependency>
```

Пример слушателя:

```java
package com.example.hook;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import ru.example.adminpanel.api.AdminActionType;
import ru.example.adminpanel.api.event.AdminActionEvent;
import ru.example.adminpanel.api.event.AdminMenuOpenEvent;
import ru.example.adminpanel.api.event.ServerEventStartEvent;
import ru.example.adminpanel.api.event.ServerEventStopEvent;

public final class AdminPanelHook implements Listener {

    private boolean doubleXp;

    // Запрещаем открывать панель в мире "lobby".
    @EventHandler
    public void onMenuOpen(AdminMenuOpenEvent event) {
        if (event.getAdmin().getWorld().getName().equals("lobby")) {
            event.setCancelled(true);
        }
    }

    // Запрещаем телепортировать к себе игроков, находящихся в режиме наблюдателя, и логируем действия.
    @EventHandler
    public void onAdminAction(AdminActionEvent event) {
        Player target = event.getTarget();
        if (event.getAction() == AdminActionType.TELEPORT_HERE && target != null
                && target.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
            event.setCancelled(true);
            return;
        }
        if (event.getAction() == AdminActionType.BROADCAST && event.getMessage() != null) {
            event.setMessage(event.getMessage() + ChatColor.GRAY + " (администрация)");
        }
    }

    // Реализуем логику события "double-xp" из config.yml AdminPanel.
    @EventHandler
    public void onEventStart(ServerEventStartEvent event) {
        if (event.getEventId().equals("double-xp")) {
            doubleXp = true;
        }
    }

    @EventHandler
    public void onEventStop(ServerEventStopEvent event) {
        if (event.getEventId().equals("double-xp")) {
            doubleXp = false;
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (doubleXp) {
            event.setDroppedExp(event.getDroppedExp() * 2);
        }
    }
}
```

Регистрация в `onEnable()` вашего плагина:

```java
getServer().getPluginManager().registerEvents(new AdminPanelHook(), this);
```

Запуск и остановка событий из кода (только в основном потоке):

```java
AdminPanelPlugin adminPanel = JavaPlugin.getPlugin(AdminPanelPlugin.class);
adminPanel.getServerEventManager().start("double-xp", null);
adminPanel.getServerEventManager().stop(StopReason.API, null);
```
