package io.github.ckofa.filewatcher.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileBaseNamePathMatcherTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("The constructor must throw an exception if the base name is null")
    void constructor_whenBaseNameIsNull_shouldThrowException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            new FileBaseNamePathMatcher(null);
        });
        assertEquals("Base name cannot be null or empty.", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   "})
    @DisplayName("The constructor should throw an exception if the base name is empty")
    void constructor_whenBaseNameIsBlank_shouldThrowException(String blankName) {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            new FileBaseNamePathMatcher(blankName);
        });
        assertEquals("Base name cannot be null or empty.", exception.getMessage());
    }

    @ParameterizedTest
    @CsvSource({
            "file.txt,         file",
            "document,         document",
            ".config,          .config",
            "archive.tar.gz,   archive.tar",
            "image.,           image"
    })
    @DisplayName("Should return true for valid base names")
    void matches_forCorrectBaseNames_shouldReturnTrue(String fileName, String expectedBaseName) throws IOException {
        // Arrange
        Path testFile = Files.createFile(tempDir.resolve(fileName));
        FileBaseNamePathMatcher matcher = new FileBaseNamePathMatcher(expectedBaseName);

        // Act
        boolean result = matcher.matches(testFile);

        // Assert
        assertTrue(result, "Expected true for the file '" + fileName + "' and the base name '" + expectedBaseName + "'");
    }

    @ParameterizedTest
    @CsvSource({
            "file.txt,         file.txt",
            "document,         doc",
            ".config,          config",
            "archive.tar.gz,   archive"
    })
    @DisplayName("Should return false for invalid base names")
    void matches_forIncorrectBaseNames_shouldReturnFalse(String fileName, String incorrectBaseName) throws IOException {
        // Arrange
        Path testFile = Files.createFile(tempDir.resolve(fileName));
        FileBaseNamePathMatcher matcher = new FileBaseNamePathMatcher(incorrectBaseName);

        // When
        boolean result = matcher.matches(testFile);

        // Then
        assertFalse(result, "Expected false for the file '" + fileName + "' and the base name '" + incorrectBaseName + "'");
    }

    @Test
    @DisplayName("Should return false if the path is a directory with the same name")
    void matches_whenPathIsDirectory_shouldReturnFalse() throws IOException {
        // Arrange
        String dirName = "my-directory";
        Path testDir = Files.createDirectory(tempDir.resolve(dirName));
        FileBaseNamePathMatcher matcher = new FileBaseNamePathMatcher(dirName);

        // Act
        boolean result = matcher.matches(testDir);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Should return false if the path does not exist")
    void matches_whenPathDoesNotExist_shouldReturnFalse() {
        // Arrange
        Path nonExistentFile = tempDir.resolve("non-existent.txt");
        FileBaseNamePathMatcher matcher = new FileBaseNamePathMatcher("non-existent");

        // Act
        boolean result = matcher.matches(nonExistentFile);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Should throw a NullPointerException if the path is null")
    void matches_whenPathIsNull_shouldThrowException() {
        // Arrange
        FileBaseNamePathMatcher matcher = new FileBaseNamePathMatcher("any-base-name");

        // Act & Assert
        NullPointerException exception = assertThrows(NullPointerException.class, () -> {
            matcher.matches(null);
        });
        assertEquals("Path cannot be null", exception.getMessage());
    }


}