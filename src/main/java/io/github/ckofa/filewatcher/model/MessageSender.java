package io.github.ckofa.filewatcher.model;

/**
 * Interface for sending messages
 */
public interface MessageSender {

    /**
     * Method for sending messages
     *
     * @param str String to send
     */
    void sendMessage(String str);
}
