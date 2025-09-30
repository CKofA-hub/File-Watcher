package io.github.ckofa.filewatcher.model;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * A {@link PathMatcher} that matches a regular file against a specific file extension.
 * <p>
 * The extension is defined as the part of the file name after the last dot ('.').
 * Files without a dot, where the dot is the first character (e.g., ".bashrc"),
 * or where the dot is the last character (e.g., "file.") are considered to have no extension.
 * <p>
 * Providing an empty string ("") to the constructor will create a matcher that
 * specifically looks for files without an extension.
 */
public final class FileExtensionPathMatcher implements PathMatcher {

    private final String extension;

    /**
     * Creates a new instance of {@code FileExtensionPathMatcher}.
     *
     * @param extension the file extension to match against. Cannot be null or contain only whitespace.
     *                  An empty string is allowed for matching files without an extension.
     */
    public FileExtensionPathMatcher(String extension) {
        if (extension == null || (extension.isBlank() && !extension.isEmpty())) {
            throw new IllegalArgumentException("File extension cannot be null or contain only whitespace.");
        }
        // Store the extension without the leading dot, if it exists.
        // This is more robust than contains().
        this.extension = extension.startsWith(".") ? extension.substring(1) : extension;
    }

    /**
     * Checks if the given path corresponds to a regular file with a matching extension.
     *
     * @param path the path to check. Must not be null.
     * @return {@code true} if the path is a regular file and its extension matches, {@code false} otherwise.
     */
    @Override
    public boolean matches(Path path) {
        Objects.requireNonNull(path, "Path cannot be null");
        if (!Files.isRegularFile(path)) {
            return false;
        }

        String fileName = path.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');

        String actualExtension;
        // No extension if dot is not found, is the first char, or is the last char.
        if (dotIndex <= 0 || dotIndex == fileName.length() - 1) {
            actualExtension = "";
        } else {
            actualExtension = fileName.substring(dotIndex + 1);
        }

        return actualExtension.equals(this.extension);
    }
}
