package io.github.ckofa.filewatcher.model;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * A {@link PathMatcher} that matches any regular file.
 * <p>
 * This matcher returns {@code true} if the given {@link Path} points to a file,
 * and not a directory or other non-regular file.
 */
public final class AnyFilePathMatcher implements PathMatcher {

    /**
     * Creates a new instance of {@code AnyFilePathMatcher}.
     */
    public AnyFilePathMatcher() {
    }

    /**
     * Checks if the given path corresponds to a regular file.
     *
     * @param path the path to check.
     * @return {@code true} if the path is a regular file, {@code false} otherwise.
     */
    @Override
    public boolean matches(Path path) {
        Objects.requireNonNull(path, "Path cannot be null");
        return Files.isRegularFile(path);
    }
}
