package io.github.ckofa.filewatcher.model;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * A {@link PathMatcher} that matches a regular file if its full name contains a specific substring.
 * <p>
 * The full name includes the base name and the extension. This matcher returns {@code true}
 * if the given {@link Path} points to a regular file and its name contains the substring
 * provided in the constructor. The comparison is case-sensitive.
 */
public final class FileFullNameContainsPathMatcher implements PathMatcher {

    private final String namePart;

    /**
     * Creates a new instance of {@code FileFullNameContainsPathMatcher}.
     *
     * @param namePart the substring to search for within the file's full name. Must not be null or empty.
     */
    public FileFullNameContainsPathMatcher(String namePart) {
        if (namePart == null || namePart.isBlank()) {
            // Corrected the error message to be more general.
            throw new IllegalArgumentException("The name part to search for cannot be null or empty.");
        }
        this.namePart = namePart;
    }

    /**
     * Checks if the given path corresponds to a regular file whose name contains the specified substring.
     *
     * @param path the path to check. Must not be null.
     * @return {@code true} if the path is a regular file and its name contains the substring, {@code false} otherwise.
     */
    @Override
    public boolean matches(Path path) {
        Objects.requireNonNull(path, "Path cannot be null");
        if (!Files.isRegularFile(path)) {
            return false;
        }

        String fileName = path.getFileName().toString();
        return fileName.contains(this.namePart);
    }
}
