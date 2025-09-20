package io.github.ckofa.filewatcher.model;

import java.nio.file.Path;

/**
 * Defines a strategy for matching a {@link Path} against a specific condition.
 * <p>
 * Implementations of this interface can be used to filter files or directories
 * based on various criteria, such as name, extension, or other properties.
 */
public interface PathMatcher {

    /**
     * Checks if the given path matches the condition defined by this matcher.
     *
     * @param path the path to check. Must not be null.
     * @return {@code true} if the path matches the condition, {@code false} otherwise.
     */
    boolean matches(Path path);
}
