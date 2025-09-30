package io.github.ckofa.filewatcher.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AnyFilePathMatcherTest {

    private AnyFilePathMatcher matcher;

    @TempDir
    private Path tempDir;

    @BeforeEach
    void setUp() {
        matcher = new AnyFilePathMatcher();
    }

    @Test
    @DisplayName("Should return true if the path is a regular file")
    void matches_whenPathIsRegularFile_shouldReturnTrue() throws IOException {
        // Arrange
        Path testFile = Files.createFile(tempDir.resolve("testfile.txt"));

        // Act
        boolean result = matcher.matches(testFile);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Should return false if the path is a directory")
    void matches_whenPathIsDirectory_shouldReturnFalse() {
        // Act
        boolean result = matcher.matches(tempDir);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Should return false if the path does not exist")
    void matches_whenPathDoesNotExist_shouldReturnFalse() {
        // Arrange
        Path nonExistentFile = tempDir.resolve("nonexistent.txt");

        // Act
        boolean result = matcher.matches(nonExistentFile);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Should throw a NullPointerException if the path is null")
    void matches_whenPathIsNull_shouldThrowException() {

        // Act & Assert
        assertThrows(NullPointerException.class,
                () -> matcher.matches(null),
                "Path cannot be null"
        );
    }
}