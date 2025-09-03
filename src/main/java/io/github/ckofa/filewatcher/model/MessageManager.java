package io.github.ckofa.filewatcher.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Manages and dispatches messages to multiple {@link MessageSender} implementations.
 * <p>
 * This class acts as a central hub for sending notifications across various configured channels.
 * It ensures that a failure in one sending mechanism does not prevent others from executing.
 * It follows the Dependency Injection pattern, receiving its dependencies (senders) from an external source.
 */
public class MessageManager {

    private static final Logger log = LoggerFactory.getLogger(MessageManager.class);

    /**
     * A list of configured message senders.
     */
    private final List<MessageSender> senders;

    /**
     * Constructs a MessageManager with a provided list of message senders.
     *
     * @param senders A list of {@link MessageSender} instances to be used for dispatching messages.
     *                The provided list is defensively copied. Must not be null.
     */
    public MessageManager(List<MessageSender> senders) {
        // Defensive copy to prevent external modification of the internal list.
        this.senders = new ArrayList<>(Objects.requireNonNull(senders, "Senders list cannot be null"));
    }

    /**
     * Sends a message to all registered senders.
     * <p>
     * This method iterates through all available senders and calls their {@code sendMessage} method.
     * It includes error handling to ensure that if one sender throws an exception,
     * the process will continue with the next one, making the system more fault-tolerant.
     *
     * @param msg The content of the message to be sent.
     */
    public void sendMessage(String msg) {
        if (senders.isEmpty()) {
            log.warn("No message senders configured. Message will not be sent: \"{}\"", msg);
            return;
        }

        for (MessageSender sender : senders) {
            try {
                sender.sendMessage(msg);
            } catch (Exception e) {
                log.error("Failed to send message via sender: {}", sender.getClass().getSimpleName(), e);
            }
        }
    }
}
