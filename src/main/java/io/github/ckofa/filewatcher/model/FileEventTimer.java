package io.github.ckofa.filewatcher.model;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A reusable timer that executes a given task (TimerTask) after a specified delay.
 * <p>
 * The timer can be configured for a single execution or for a limited number of repeated executions.
 * After the timer is stopped (either by completing its work or by a manual call to {@link #stop()}),
 * it can be started again with the same or new settings.
 * <p>
 * Note: This implementation is based on {@link java.util.Timer}.
 */
public class FileEventTimer {

    private final TimerTask timerTask;
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
    private volatile boolean isRunning = false;

    /**
     * Creates a new timer with the specified duration.
     *
     * @param duration timer duration, cannot be a negative number.
     * @throws IllegalArgumentException if the duration is less than zero.
     */
    public FileEventTimer(TimerTask timerTask, long duration) {
        if (duration < 0) throw new IllegalArgumentException("Duration cannot be negative");
        this.duration = duration;
        this.timerTask = timerTask;
    }

    /**
     * Starts the timer.
     * <p>
     * A new internal {@link Timer} instance is created on each call, allowing the timer to be reused
     * after it has been stopped. The timer will execute the task either once or repeatedly,
     * based on the settings provided via {@link #setRepeatNotifications(long, int)}.
     *
     * @throws IllegalStateException if the timer is already running.
     */
    public void start() {
        if (isRunning) throw new IllegalStateException("Timer is already running!"); //check that the timer cannot be started twice
        isRunning = true;
        this.timer = new Timer(); //for reuse, each time the method is run a new timer is created

        if (enableNotificationRepeat) {
            counter.set(0); // Resetting the counter every time we start
            TimerTask wrapperTask = new TimerTask() {
                @Override
                public void run() {
                    try {
                        timerTask.run();
                    } finally {
                        // Repetition logic processing
                        if (counter.incrementAndGet() >= iterations) {
                            stop(); // Stop the timer when the limit is reached
                        }
                    }
                }
            };
            timer.schedule(wrapperTask, duration, period);
        } else {
            TimerTask singleShotWrapper = new TimerTask() {
                @Override
                public void run() {
                    try {
                        timerTask.run();
                    } finally {
                        isRunning = false;
                    }
                }
            };
            timer.schedule(singleShotWrapper, duration);
        }
    }

    /**
     * Configures the timer for repeated executions.
     * If this method is not called, the timer will execute the task only once.
     *
     * @param period     the time in milliseconds between subsequent task executions. Must be greater than zero.
     * @param iterations the total number of times the task should be executed. Must be greater than zero.
     * @throws IllegalArgumentException if period or iterations are not positive numbers.
     * @throws IllegalStateException    if the timer is already running and its settings cannot be changed.
     */
    public void setRepeatNotifications(long period, int iterations) {
        if (isRunning) throw new IllegalStateException("Cannot change repeat settings while the timer is running!");
        if (period <= 0 || iterations <= 0) throw new IllegalArgumentException("Period and iterations must be greater than zero");
        this.period = period;
        this.iterations = iterations;
        this.enableNotificationRepeat = true;
    }

    /**
     * Forcibly stops the timer, canceling all scheduled tasks.
     * <p>
     * This method resets the timer's internal state, making it ready to be started again
     * via the {@link #start()} method.
     */
    public void stop() {
        if (timer != null) timer.cancel();
        isRunning = false;
        counter.set(0);
    }
}
