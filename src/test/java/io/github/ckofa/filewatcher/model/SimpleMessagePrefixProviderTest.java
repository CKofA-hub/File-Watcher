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
        String expectedDefaultText = "File Watcher";
        String defaultFormatter = "dd-MM-yyyy HH:mm:ss";
        DateTimeFormatter expectedFormatter = DateTimeFormatter.ofPattern(defaultFormatter);
        SimpleMessagePrefixProvider provider = new SimpleMessagePrefixProvider();

        // Act
        String prefix = provider.getMessagePrefix();

        // Assert
        assertNotNull(prefix);
        assertTrue(prefix.contains(expectedDefaultText));
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
        String customText = "My Log Entry";
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

    @Test
    @DisplayName("Should create prefix with custom text and no timestamp when using single-argument constructor")
    void getMessagePrefix_withSingleArgumentConstructor_shouldNotIncludeTimestamp() {
        // Arrange
        String customText = "My Log Entry";
        SimpleMessagePrefixProvider provider = new SimpleMessagePrefixProvider(customText);

        // Act
        String prefix = provider.getMessagePrefix();

        // Assert
        assertNotNull(prefix);
        assertTrue(prefix.contains(customText));
        assertTrue(prefix.endsWith(" : "));
        assertEquals(customText + " : ", prefix, "Prefix should only contain custom text and separator.");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when prefixText is null in single-argument constructor")
    void constructor_singleArgument_nullPrefixText_shouldThrowException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> new SimpleMessagePrefixProvider(null),
                "Constructor should throw IllegalArgumentException for null prefix text.");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when prefixText is blank in single-argument constructor")
    void constructor_singleArgument_blankPrefixText_shouldThrowException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> new SimpleMessagePrefixProvider("   "),
                "Constructor should throw IllegalArgumentException for blank prefix text.");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when prefixText is null in two-argument constructor")
    void constructor_twoArguments_nullPrefixText_shouldThrowException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> new SimpleMessagePrefixProvider(null, DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                "Constructor should throw IllegalArgumentException for null prefix text.");
    }
}