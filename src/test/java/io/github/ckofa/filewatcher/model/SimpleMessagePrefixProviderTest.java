package io.github.ckofa.filewatcher.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import static org.junit.jupiter.api.Assertions.*;

class SimpleMessagePrefixProviderTest {

    @Test
    @DisplayName("Should create prefix with default text and format when using default constructor")
    void getMessagePrefix_withDefaultConstructor_shouldUseDefaults() {
        // Arrange
        String defaultText = "File Watcher";
        String defaultFormatter = "dd-MM-yyyy HH:mm:ss";
        DateTimeFormatter expectedFormatter = DateTimeFormatter.ofPattern(defaultFormatter);
        SimpleMessagePrefixProvider provider = new SimpleMessagePrefixProvider();

        // Act
        String prefix = provider.getMessagePrefix();

        // Assert
        assertNotNull(prefix);
        assertTrue(prefix.contains(defaultText));
        assertTrue(prefix.endsWith(" : "));

        String dateTimePart = prefix.split(" : ")[0];
        assertDoesNotThrow(
                () -> expectedFormatter.parse(dateTimePart),
                String.format("The date-time part '%s' does not match the expected format 'dd-MM-yyyy HH:mm:ss'", dateTimePart));
    }

    @Test
    @DisplayName("Should create prefix with custom text and format when using custom constructor")
    void getMessagePrefix_withCustomConstructor_shouldUseCustomValues() {
        // Arrange
        String customText = "My Custom App";
        String customPattern = "yyyy/MM/dd HH:mm";
        DateTimeFormatter customFormatter = DateTimeFormatter.ofPattern(customPattern);
        MessagePrefixProvider provider = new SimpleMessagePrefixProvider(customText, customFormatter);

        // Act
        String prefix = provider.getMessagePrefix();

        // Assert
        assertNotNull(prefix);
        assertTrue(prefix.contains(customText));
        assertTrue(prefix.endsWith(" : "));

        String dateTimePart = prefix.split(" : ")[0];
        assertDoesNotThrow(
                () -> customFormatter.parse(dateTimePart),
                String.format("The date-time part '%s' does not match the expected format '%s'", dateTimePart, customPattern));

        DateTimeFormatter defaultFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        assertThrows(DateTimeParseException.class,
                () -> defaultFormatter.parse(dateTimePart),
                "The date part should not be parsable with the default formatter.");
    }
}