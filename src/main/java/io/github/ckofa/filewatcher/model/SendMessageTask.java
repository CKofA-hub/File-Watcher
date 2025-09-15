package io.github.ckofa.filewatcher.model;

import java.util.TimerTask;

/**
 * Represents a task that sends a message using a {@link MessageManager}.
 * This class is intended to be used with a {@link java.util.Timer} to schedule message sending.
 */
public class SendMessageTask extends TimerTask {

    private final MessageManager messageManager;
    private final String message;

    /**
     * Creates a new SendMessageTask.
     *
     * @param messageManager The message manager used to send the message.
     * @param message        The message to be sent.
     */
    public SendMessageTask(MessageManager messageManager, String message) {
        this.messageManager = messageManager;
        this.message = message;
    }

    /**
     * Executes the task of sending the message.
     * This method is called by the timer when the task is scheduled for execution.
     */
    @Override
    public void run() {
        messageManager.sendMessage(message);
    }
}
