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

class FileFullNameContainsPathMatcherTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("The constructor must throw an exception if the substring is null")
    void constructor_whenPartIsNull_shouldThrowException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            new FileFullNameContainsPathMatcher(null);
        });
        assertEquals("The name part to search for cannot be null or empty.", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t"})
    @DisplayName("The constructor must throw an exception if the substring is empty or consists of spaces")
    void constructor_whenPartIsBlank_shouldThrowException(String blankPart) {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            new FileFullNameContainsPathMatcher(blankPart);
        });
        assertEquals("The name part to search for cannot be null or empty.", exception.getMessage());
    }

    @ParameterizedTest
    @CsvSource({
            "report-2024-final.pdf,  2024",
            "draft-notes.txt,        draft",
            "archive.zip,            .zip",
            "archive.tar.gz,         .tar.gz",
            "MyFile.txt,             MyFile"
    })
    @DisplayName("Should return true if the filename contains a substring")
    void matches_whenNameContainsSubstring_shouldReturnTrue(String fileName, String partToMatch) throws IOException {
        // Arrange
        Path testFile = Files.createFile(tempDir.resolve(fileName));
        FileFullNameContainsPathMatcher matcher = new FileFullNameContainsPathMatcher(partToMatch);

        // Act
        boolean result = matcher.matches(testFile);

        // Assert
        assertTrue(result, "Expected true for the file '" + fileName + "' and substrings '" + partToMatch + "'");
    }

    @ParameterizedTest
    @CsvSource({
            "data.csv,         report",   // A completely different name
            "MyFile.txt,       myfile"    // Checking case sensitivity
    })
    @DisplayName("Should return false if the filename does not contain a substring")
    void matches_whenNameDoesNotContainSubstring_shouldReturnFalse(String fileName, String partToMatch) throws IOException {
        // Arrange
        Path testFile = Files.createFile(tempDir.resolve(fileName));
        FileFullNameContainsPathMatcher matcher = new FileFullNameContainsPathMatcher(partToMatch);

        // Act
        boolean result = matcher.matches(testFile);

        // Assert
        assertFalse(result, "Expected false for the file '" + fileName + "' and substrings '" + partToMatch + "'");
    }

    @Test
    @DisplayName("Should return false if the path is a directory")
    void matches_whenPathIsDirectory_shouldReturnFalse() throws IOException {
        // Arrange
        Path testDir = Files.createDirectory(tempDir.resolve("my-report-dir"));
        FileFullNameContainsPathMatcher matcher = new FileFullNameContainsPathMatcher("report");

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
        FileExtensionPathMatcher matcher = new FileExtensionPathMatcher("existent");

        // Act
        boolean result = matcher.matches(nonExistentFile);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Should throw a NullPointerException if the path is null")
    void matches_whenPathIsNull_shouldThrowException() {
        // Arrange
        FileFullNameContainsPathMatcher matcher = new FileFullNameContainsPathMatcher("any-substring");

        // Act & Assert
        NullPointerException exception = assertThrows(NullPointerException.class, () -> {
            matcher.matches(null);
        });
        assertEquals("Path cannot be null", exception.getMessage());
    }


}