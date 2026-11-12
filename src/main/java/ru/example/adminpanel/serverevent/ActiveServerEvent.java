package ru.example.adminpanel.serverevent;

import org.bukkit.boss.BossBar;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitTask;

/**
 * Состояние запущенного серверного события. Изменяется только в основном потоке.
 */
public final class ActiveServerEvent {

    private final ServerEventDefinition definition;
    private final CommandSender initiator;
    private final int durationSeconds;
    private final long startedAtMillis;
    private int remainingSeconds;
    private BukkitTask task;
    private BossBar bossBar;

    ActiveServerEvent(ServerEventDefinition definition, CommandSender initiator, int durationSeconds) {
        this.definition = definition;
        this.initiator = initiator;
        this.durationSeconds = durationSeconds;
        this.remainingSeconds = durationSeconds;
        this.startedAtMillis = System.currentTimeMillis();
    }

    public ServerEventDefinition getDefinition() {
        return definition;
    }

    public CommandSender getInitiator() {
        return initiator;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public int getRemainingSeconds() {
        return remainingSeconds;
    }

    public long getStartedAtMillis() {
        return startedAtMillis;
    }

    public long getElapsedSeconds() {
        return Math.max(0, (System.currentTimeMillis() - startedAtMillis) / 1000L);
    }

    void setRemainingSeconds(int remainingSeconds) {
        this.remainingSeconds = remainingSeconds;
    }

    BukkitTask getTask() {
        return task;
    }

    void setTask(BukkitTask task) {
        this.task = task;
    }

    BossBar getBossBar() {
        return bossBar;
    }

    void setBossBar(BossBar bossBar) {
        this.bossBar = bossBar;
    }
}
