package io.github.ckofa.filewatcher.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * A thread-safe class for monitoring file system changes in a specified folder.
 * <p>
 * This monitor runs in a separate background thread, allowing the main application
 * to remain responsive. It can be started and stopped gracefully. Observers can be
 * added or removed at any time to be notified of file events that match the
 * specified criteria.
 * <p>
 * <b>Important:</b> To ensure proper resource cleanup and graceful termination of the
 * background monitoring thread, the {@link #stop()} method <b>must always be called</b>
 * when the monitor is no longer needed. Failure to do so will result in the Java Virtual
 * Machine (JVM) not exiting, as the non-daemon background thread will remain active,
 * and system resources (like the {@link WatchService}) will not be released.
 * </p>
 * <p>
 * It is highly recommended to use a {@code try-finally} block to guarantee that {@link #stop()}
 * is invoked, even if exceptions occur during the application's lifecycle.
 * </p>
 * <pre>{@code
 * FolderMonitor monitor = null;
 * try {
 *     monitor = new FolderMonitor(...);
 *     monitor.start();
 *
 *     //... Your application's main logic ...
 *     //... For example wait for user input ...
 *     System.out.println("Monitoring started. Press Enter to stop...");
 *     System.in.read(); // Blocks until Enter is pressed
 *
 * } catch (IOException | InterruptedException e) {
 *     System.err.println("An error occurred: " + e.getMessage());
 * } finally {
 *     if (monitor != null) {
 *        System.out.println("Stopping monitor...");
 *        monitor.stop();
 *     }
 * }
 * }</pre>
 */
public class FolderMonitor {

    private static final Logger log = LoggerFactory.getLogger(FolderMonitor.class);

    private final Path watchPath;
    private final PathMatcher matcher;
    private final WatchEvent.Kind<Path> eventKind;
    private final List<WatchEventObserver> observers = new CopyOnWriteArrayList<>();

    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private ExecutorService executorService;
    private WatchService watchService;

    /**
     * Constructs a new FolderMonitor.
     *
     * @param watchPath The path to the directory to monitor.
     * @param matcher   A {@link PathMatcher} to filter files by name (e.g., "*.txt").
     * @param eventKind The type of event to watch for (e.g., {@link StandardWatchEventKinds#ENTRY_CREATE}).
     * @param observer  An initial observer to be notified of events. Can be {@code null}.
     */
    public FolderMonitor(Path watchPath, PathMatcher matcher, WatchEvent.Kind<Path> eventKind, WatchEventObserver observer) {
        this.watchPath = watchPath;
        this.matcher = matcher;
        this.eventKind = eventKind;
        if (observer != null) {
            this.observers.add(observer);
        }
    }

    /**
     * Starts the folder monitoring in a background thread.
     * <p>
     * This method is non-blocking. If the monitor is already running, this call will be ignored
     * and a warning will be logged.
     * @throws IOException if an I/O error occurs, for example, when creating the {@link WatchService}.
     */
    public synchronized void start() throws IOException {
        if (isRunning.getAndSet(true)) {
            log.warn("Attempted to start an already running monitor for folder '{}' [event: {}, matcher: {}]. Ignoring call.",
                    watchPath,
                    eventKind,
                    matcher.getDescription());
            return;
        }

        if (observers.isEmpty()) {
            log.warn("Monitor for folder '{}' [event: {}, matcher: {}] is starting with no observers. File events will be detected but ignored.",
                    watchPath,
                    eventKind,
                    matcher.getDescription());
        }

        log.info("Starting monitor for folder '{}' [event: {}, matcher: {}]",
                watchPath,
                eventKind,
                matcher.getDescription());

        watchService = FileSystems.getDefault().newWatchService();
        watchPath.register(watchService, eventKind);

        // Launch the monitoring task in a separate thread.
        ThreadFactory namedThreadFactory = r -> new Thread(r, "folder-monitor-" + watchPath.getFileName());
        executorService = Executors.newSingleThreadExecutor(namedThreadFactory);
        executorService.submit(this::processEvents);
    }

    private void processEvents() {
        while (isRunning.get()) {
            try {
                WatchKey watchKey = watchService.take();
                // Re-checking the flag. Necessary for race conditions when `stop()`
                // is called simultaneously with a file event. This ensures that we do not
                // process an event that arrived after the stop command.
                if (!isRunning.get()) {
                    break;
                }
                watchKey.pollEvents().forEach(this::notifyListeners);
                boolean valid = watchKey.reset();
                if (!valid) {
                    log.warn("Monitoring key for folder '{}' [event: {}, matcher: {}] has become invalid (folder might have been deleted). Stopping thread.",
                            watchPath,
                            eventKind,
                            matcher.getDescription());
                    break; // Folder deleted or unavailable
                }
            } catch (InterruptedException e) {
                log.warn("The monitoring thread for folder '{}' [event: {}, matcher: {}] was interrupted.",
                        watchPath,
                        eventKind,
                        matcher.getDescription(),
                        e);
                Thread.currentThread().interrupt();
                break;
            } catch (ClosedWatchServiceException e) {
                // This is an expected exception during a graceful shutdown via stop().
                log.info("Graceful shutdown: WatchService for folder '{}' [event: {}, matcher: {}] has been closed. The monitoring thread is terminating.",
                        watchPath,
                        eventKind,
                        matcher.getDescription());
                break;
            }
        }
        log.info("The background monitoring thread for folder '{}' [event: {}, matcher: {}] has completed its execution.",
                watchPath,
                eventKind,
                matcher.getDescription());
    }

    private void notifyListeners(WatchEvent<?> event) {
        WatchEvent.Kind<?> kind = event.kind();

        // Handle the special OVERFLOW event, which indicates that some events may have been lost.
        if (kind == StandardWatchEventKinds.OVERFLOW) {
            log.warn("File system event overflow detected for folder '{}' [event: {}, matcher: {}]. Some events may have been lost.",
                    watchPath,
                    eventKind,
                    matcher.getDescription());
            return;
        }

        // Process the event only if it's the kind we are watching for and its context is a valid Path.
        if (kind == eventKind && event.context() instanceof Path context) {
            if (matcher.matches(context)) {
                CustomWatchEvent customWatchEvent = new CustomWatchEvent(event, watchPath);
                observers.forEach(observer -> observer.onEvent(customWatchEvent));
            }
        }

    }

    /**
     * Stops the folder monitoring gracefully.
     * <p>
     * This method signals the background thread to terminate, closes the {@link WatchService},
     * and waits for the thread to finish. If the monitor is already stopped, this call is ignored.
     */
    public synchronized void stop() {
        // Atomically set the flag to false and check whether it was already false.
        if (!isRunning.getAndSet(false)) {
            log.info("The monitor for folder '{}' [event: {}, matcher: {}] has already been stopped.",
                    watchPath,
                    eventKind,
                    matcher.getDescription());
            return;
        }

        log.info("Stopping monitor for folder '{}' [event: {}, matcher: {}]",
                watchPath,
                eventKind,
                matcher.getDescription());

        // Close WatchService to interrupt the blocking call to .take() in the background thread.
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                log.error("Error closing WatchService for folder '{}' [event: {}, matcher: {}]",
                        watchPath,
                        eventKind,
                        matcher.getDescription(),
                        e);
            }
        }

        // Correctly stop the thread pool (graceful shutdown)
        if (executorService != null) {
            executorService.shutdown(); // Disable new tasks from being submitted
            try {
                // Give the thread some time to terminate gracefully
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.warn("Background thread for folder '{}' [event: {}, matcher: {}] did not terminate in 5 seconds, forcing shutdown.",
                            watchPath,
                            eventKind,
                            matcher.getDescription());
                    executorService.shutdownNow(); // Interrupt currently executing tasks
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

    }

    /**
     * Adds an observer to the list of listeners.
     * <p>
     * The observer will be notified of subsequent file events.
     *
     * @param observer The observer to add. Must not be {@code null}.
     */
    public void addObserver(WatchEventObserver observer) {
        observers.add(observer);
    }

    /**
     * Removes an observer from the list of listeners.
     * <p>
     * The observer will no longer receive notifications for file events.
     *
     * @param observer The observer to remove.
     */
    public void removeObserver(WatchEventObserver observer) {
        observers.remove(observer);
    }

}
