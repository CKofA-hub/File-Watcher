package io.github.ckofa.filewatcher.model;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * A {@link PathMatcher} that matches a regular file against a specific base name.
 * <p>
 * The base name is defined as the part of the file name before the last dot ('.').
 * If the file has no extension, or if the dot is the first character (e.g., ".bashrc"),
 * the full name is considered the base name.
 * This matcher returns {@code true} if the given {@link Path} points to a regular file
 * and its calculated base name is equal to the one provided in the constructor.
 */
public final class FileBaseNamePathMatcher implements PathMatcher {

    private final String baseName;

    /**
     * Creates a new instance of {@code FileBaseNamePathMatcher}.
     *
     * @param baseName the base file name to match against. Must not be null or empty.
     */
    public FileBaseNamePathMatcher(String baseName) {
        if (baseName == null || baseName.isBlank()) {
            throw new IllegalArgumentException("Base name cannot be null or empty.");
        }
        this.baseName = baseName;
    }

    /**
     * Checks if the given path corresponds to a regular file with a matching base name.
     *
     * @param path the path to check. Must not be null.
     * @return {@code true} if the path is a regular file and its base name matches, {@code false} otherwise.
     */
    @Override
    public boolean matches(Path path) {
        Objects.requireNonNull(path, "Path cannot be null");
        if (!Files.isRegularFile(path)) {
            return false;
        }

        String fileName = path.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');

        String actualBaseName;
        // If no dot is found, or the dot is the first character (e.g., ".bashrc"),
        // the whole name is considered the base name.
        if (dotIndex <= 0) { // -1 for no dot, 0 for dot at the beginning
            actualBaseName = fileName;
        } else {
            // Otherwise, it's the part before the last dot.
            actualBaseName = fileName.substring(0, dotIndex);
        }

        return actualBaseName.equals(this.baseName);
    }
}
