package io.github.ckofa.filewatcher.model;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The class represents a timer waiting for the second file event.
 * Sends notifications when the timer expires.
 */
public class FileEventTimer {

    private Timer timer;

    /**
     * Duration of waiting for the second file event, milliseconds.
     */
    private final long duration;

    /**
     * The period of time after which repeated messages will be sent when the timer expires, milliseconds.
     */
    private long period = 1000;

    /**
     * Number of repeat messages to be sent after the timer expires, non-negative number.
     */
    private int iterations = 1;

    /**
     * Variable enables repeated notifications.
     */
    private boolean enableNotificationRepeat = false;

    /**
     * Counter of sent notifications.
     */
    private final AtomicInteger counter = new AtomicInteger(0);

    /**
     * Indicates whether the timer is running.
     */
    private boolean isRunning = false;

    /**
     * Creates a new timer with the specified duration.
     *
     * @param duration timer duration, cannot be a negative number.
     * @throws IllegalArgumentException if the duration is less than zero.
     */
    public FileEventTimer(long duration) {
        if (duration < 0) throw new IllegalArgumentException("Duration cannot be negative");
        this.duration = duration;
    }

    /**
     * Starting the timer according to the settings
     *
     * @throws IllegalStateException if the timer is already running.
     */
    public void start() {
        if (isRunning) throw new IllegalStateException("Timer is already running!"); //check that the timer cannot be started twice
        isRunning = true;
        this.timer = new Timer(); //for reuse, each time the method is run a new timer is created
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                sendNotifications();
            }
        };
        if (enableNotificationRepeat) {
            timer.schedule(timerTask, duration, period);
        } else {
            timer.schedule(timerTask, duration);
        }
    }

    private void sendNotifications() {
        if (counter.getAndIncrement() < iterations) {
            System.out.println("сообщение"); // высылаем сообщения
            //расширение логики
        } else {
            stop();
        }
    }

    /**
     * Enables repeated notifications when the timer expires.
     *
     * @param period time period between repeated alerts, milliseconds, cannot be less than or equal to zero.
     * @param iterations number of repeated alerts, cannot be less than or equal to zero.
     * @throws IllegalArgumentException if long period and int iterations less than or equal to zero.
     * @throws IllegalStateException if the timer is already running.
     */
    public void setRepeatNotifications(long period, int iterations) {
        if (isRunning) throw new IllegalStateException("Cannot change repeat settings while the timer is running!");
        if (period <= 0 || iterations <= 0) throw new IllegalArgumentException("Period and iterations must be greater than zero");
        this.period = period;
        this.iterations = iterations;
        this.enableNotificationRepeat = true;
    }

    /**
     * Stops the timer.
     */
    public void stop() {
        if (timer != null) timer.cancel();
        isRunning = false;
        counter.set(0);
    }
}
