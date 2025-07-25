package io.github.ckofa.filewatcher;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

public class TestWatch {



    public static void main(String[] args) {
        WatchService watchService;
        WatchKey watchKey;
        Path watchPath = Paths.get("D:\\_TestFolder\\watchTest");
        try {
            watchService = FileSystems.getDefault().newWatchService();
            watchPath.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY);
            while ((watchKey = watchService.take()) != null) {
                List<WatchEvent<?>> watchEvents = watchKey.pollEvents();
                for (WatchEvent<?> watchEvent : watchEvents) {
                    System.out.println("Event kind:" + watchEvent.kind()
                            + ". File affected: " + watchEvent.context() + ".");
                }
                watchKey.reset();
            }
        } catch (IOException e) {
            System.err.println("Error when creating WatchService instance: " + e);
        } catch (InterruptedException e) {
            System.err.println("Interrupted while waiting watch key: " + e);
        }
    }

}
