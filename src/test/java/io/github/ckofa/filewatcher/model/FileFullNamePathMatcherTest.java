package io.github.ckofa.filewatcher.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileFullNamePathMatcherTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("The constructor should throw an exception if the name is null")
    void constructor_whenNameIsNull_shouldThrowException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new FileFullNamePathMatcher(null)
        );
        assertEquals("Full name cannot be null or empty.", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   "})
    @DisplayName("The constructor should throw an exception if the name is empty or consists of spaces")
    void constructor_whenNameIsBlank_shouldThrowException(String blankName) {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            new FileFullNamePathMatcher(blankName);
        });
        assertEquals("Full name cannot be null or empty.", exception.getMessage());
    }

    @Test
    @DisplayName("Should return true if the full filename matches")
    void matches_whenFullNameMatches_shouldReturnTrue() throws IOException {
        // Arrange
        String expectedFileName = "exact-file-name.log";
        Path testFile = Files.createFile(tempDir.resolve(expectedFileName));
        FileFullNamePathMatcher matcher = new FileFullNamePathMatcher(expectedFileName);

        // Act
        boolean result = matcher.matches(testFile);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Should return false if the full filename does not match")
    void matches_whenFullNameDoesNotMatch_shouldReturnFalse() throws IOException {
        // Arrange
        Path testFile = Files.createFile(tempDir.resolve("actual-file.txt"));
        FileFullNamePathMatcher matcher = new FileFullNamePathMatcher("different-file.txt");

        // Act
        boolean result = matcher.matches(testFile);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Should return false if the path is a directory with the same name")
    void matches_whenPathIsDirectory_shouldReturnFalse() throws IOException {
        // Arrange
        String directoryName = "my-directory";
        Path testDirectory = Files.createDirectory(tempDir.resolve(directoryName));
        FileFullNamePathMatcher matcher = new FileFullNamePathMatcher(directoryName);

        // Act
        boolean result = matcher.matches(testDirectory);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Should return false if the path does not exist")
    void matches_whenPathDoesNotExist_shouldReturnFalse() {
        // Arrange
        Path nonExistentFile = tempDir.resolve("non-existent.txt");
        FileFullNamePathMatcher matcher = new FileFullNamePathMatcher("non-existent.txt");

        // Act
        boolean result = matcher.matches(nonExistentFile);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Should throw a NullPointerException if the path is null")
    void matches_whenPathIsNull_shouldThrowException() {
        // Arrange
        FileFullNamePathMatcher matcher = new FileFullNamePathMatcher("any-name.txt");

        // Act & Assert
        NullPointerException exception = assertThrows(NullPointerException.class,
                () -> matcher.matches(null)
        );
        assertEquals("Path cannot be null", exception.getMessage());
    }
}