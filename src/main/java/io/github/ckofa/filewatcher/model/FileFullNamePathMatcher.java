package io.github.ckofa.filewatcher.model;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * A {@link PathMatcher} that matches a regular file against a specific full file name.
 * <p>
 * This matcher returns {@code true} if the given {@link Path} points to a regular file
 * and its name (including extension) is equal to the one provided in the constructor.
 */
public final class FileFullNamePathMatcher implements PathMatcher {

    private final String fullName;

    /**
     * Creates a new instance of {@code FileFullNamePathMatcher}.
     *
     * @param fullName the full file name to match against. Must not be null or empty.
     */
    public FileFullNamePathMatcher(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Full name cannot be null or empty.");
        }
        this.fullName = fullName;
    }

    /**
     * Checks if the given path corresponds to a regular file with a matching full name.
     *
     * @param path the path to check. Must not be null.
     * @return {@code true} if the path is a regular file and its name matches, {@code false} otherwise.
     */
    @Override
    public boolean matches(Path path) {
        Objects.requireNonNull(path, "Path cannot be null");

        return Files.isRegularFile(path) && fullName.equals(path.getFileName().toString());
    }
}
