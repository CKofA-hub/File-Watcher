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

class FileExtensionPathMatcherTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("The constructor should throw an exception if the extension is null")
    void constructor_whenExtensionIsNull_shouldThrowException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            new FileExtensionPathMatcher(null);
        });
        assertEquals("File extension cannot be null or contain only whitespace.", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", "   ", "\t"})
    @DisplayName("The constructor must throw an exception if the extension consists of spaces")
    void constructor_whenExtensionIsWhitespace_shouldThrowException(String whitespaceExtension) {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            new FileExtensionPathMatcher(whitespaceExtension);
        });
        assertEquals("File extension cannot be null or contain only whitespace.", exception.getMessage());
    }

    @ParameterizedTest
    @CsvSource({
            "file.txt,         txt",
            "archive.tar.gz,   gz",
            "photo.JPEG,       JPEG",
            "data.log,         .log" // Check that the constructor handles the point
    })
    @DisplayName("Should return true for valid extensions")
    void matches_forCorrectExtensions_shouldReturnTrue(String fileName, String extensionToMatch) throws IOException {
        // Arrange
        Path testFile = Files.createFile(tempDir.resolve(fileName));
        FileExtensionPathMatcher matcher = new FileExtensionPathMatcher(extensionToMatch);

        // Act
        boolean result = matcher.matches(testFile);

        // Assert
        assertTrue(result, "Expected true for the file '" + fileName + "' and extensions '" + extensionToMatch + "'");
    }

    @ParameterizedTest
    @ValueSource(strings = {"document", ".bashrc", "file."})
    @DisplayName("Should return true for files with no extension if looking for \"\"")
    void matches_forFilesWithNoExtension_whenMatchingEmptyString_shouldReturnTrue(String fileName) throws IOException {
        // Arrange
        Path testFile = Files.createFile(tempDir.resolve(fileName));
        // Looking for files WITHOUT extension
        FileExtensionPathMatcher matcher = new FileExtensionPathMatcher("");

        // Act
        boolean result = matcher.matches(testFile);

        // Assert
        assertTrue(result, "It was expected that the file '" + fileName + "' will be treated as a file without an extension");
    }

    @ParameterizedTest
    @CsvSource({
            "file.txt,         log",      // Incorrect extension
            ".bashrc,          bashrc",   // Hidden file (counts without extension)
            "file.txt,         TXT"       // Checking case sensitivity
    })
    @DisplayName("Should return false for invalid or missing extensions")
    void matches_forIncorrectOrMissingExtensions_shouldReturnFalse(String fileName, String extensionToMatch) throws IOException {
        // Arrange
        Path testFile = Files.createFile(tempDir.resolve(fileName));
        FileExtensionPathMatcher matcher = new FileExtensionPathMatcher(extensionToMatch);

        // Act
        boolean result = matcher.matches(testFile);

        // Assert
        assertFalse(result, "Expected false for the file '" + fileName + "' and extensions '" + extensionToMatch + "'");
    }

    @Test
    @DisplayName("Should return false if the path is a directory with the same name")
    void matches_whenPathIsDirectory_shouldReturnFalse() throws IOException {
        // Arrange
        Path testDir = Files.createDirectory(tempDir.resolve("my-dir.txt"));
        FileExtensionPathMatcher matcher = new FileExtensionPathMatcher("txt");

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
        FileExtensionPathMatcher matcher = new FileExtensionPathMatcher(".txt");

        // Act
        boolean result = matcher.matches(nonExistentFile);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Should throw a NullPointerException if the path is null")
    void matches_whenPathIsNull_shouldThrowException() {
        // Arrange
        FileExtensionPathMatcher matcher = new FileExtensionPathMatcher("txt");

        // Act & Assert
        NullPointerException exception = assertThrows(NullPointerException.class, () -> {
            matcher.matches(null);
        });
        assertEquals("Path cannot be null", exception.getMessage());
    }

}