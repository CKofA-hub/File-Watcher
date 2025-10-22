package io.github.ckofa.filewatcher.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * A simple implementation of {@link MessagePrefixProvider} that creates a message prefix.
 * The prefix can contain optionally the current timestamp and a static text identifier, or just the static text identifier.
 * <p>
 * When a timestamp is included, the default format is "dd-MM-yyyy HH:mm:ss : File Watcher : ".
 * When only a static text identifier is used, the format is "File Watcher : ".
 * This class is thread-safe.
 * </p>
 */
public class SimpleMessagePrefixProvider implements MessagePrefixProvider {

    private static final String DEFAULT_PREFIX_TEXT = "File Watcher";
    private static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    private final String prefixText;
    private final DateTimeFormatter formatter;

    /**
     * Creates a formatter with the default prefix text ("File Watcher") and date-time format ("dd-MM-yyyy HH:mm:ss").
     */
    public SimpleMessagePrefixProvider() {
        this(DEFAULT_PREFIX_TEXT, DEFAULT_FORMATTER);
    }

    /**
     * Creates a formatter with custom prefix text and no timestamp.
     * The format will be "prefixText : ".
     *
     * @param prefixText the static text to include in the prefix.
     */
    public SimpleMessagePrefixProvider(String prefixText) {
        this(prefixText, null);
    }

    /**
     * Creates a formatter with custom prefix text and date-time formatter.
     *
     * @param prefixText the static text to include in the prefix.
     * @param formatter  the formatter for the timestamp, or {@code null} if no timestamp should be included.
     * @throws IllegalArgumentException if {@code prefixText} is null or empty.
     */
    public SimpleMessagePrefixProvider(String prefixText, DateTimeFormatter formatter) {
        if (prefixText == null || prefixText.isBlank()) {
            throw new IllegalArgumentException("Prefix text cannot be null or empty.");
        }
        this.prefixText = prefixText;
        this.formatter = formatter;
    }

    /**
     * Generates the message prefix based on the configured prefix text and optional timestamp.
     * If a {@link DateTimeFormatter} was provided during construction, the prefix will include
     * the current timestamp. Otherwise, it will only contain the static prefix text.
     *
     * @return A formatted string representing the message prefix.
     */
    @Override
    public String getMessagePrefix() {
        if (formatter != null) {
            String currentTime = LocalDateTime.now().format(formatter);
            return String.format("%s : %s : ", currentTime, prefixText);
        } else {
            return String.format("%s : ", prefixText);
        }
    }
}
