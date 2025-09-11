package io.github.ckofa.filewatcher.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import io.github.ckofa.filewatcher.model.AppConfigManager.Settings;

/**
 * An adapter that implements the {@link MessageSender} interface to send notifications via email.
 * <p>
 * This class acts as a bridge between the application's generic messaging interface
 * and the specific email sending logic provided by {@link EmailNotificationSender}.
 * It retrieves email configuration from {@link AppConfigManager} and handles the
 * asynchronous sending of messages. If email settings are not configured,
 * sending operations will be silently ignored.
 * </p>
 */
public class EmailSenderAdapter implements MessageSender{

    private static final Logger log = LoggerFactory.getLogger(EmailSenderAdapter.class);

    private final String subject;
    private final String recipientMail;
    private final MessagePrefixProvider messagePrefixProvider;

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private final Optional<EmailNotificationSender> sender;

    /**
     * Constructs an EmailSenderAdapter with configuration provided by an {@link AppConfigManager}.
     * <p>
     * The constructor initializes the underlying email sender using {@link EmailNotificationSenderFactory}
     * and retrieves necessary settings like recipient email and subject from the configuration manager.
     * </p>
     *
     * @param appConfigManager the configuration manager to retrieve email settings from.
     */
    public EmailSenderAdapter(AppConfigManager appConfigManager, MessagePrefixProvider messagePrefixProvider) {
        this.sender = EmailNotificationSenderFactory.create(appConfigManager);
        this.recipientMail = appConfigManager.getSettingValue(Settings.MAIL_RECIPIENT_EMAIL);
        this.subject = appConfigManager.getSettingValue(Settings.MAIL_SUBJECT);
        this.messagePrefixProvider = messagePrefixProvider;
    }

    /**
     * Asynchronously sends a message via email.
     * <p>
     * If the email sender was successfully initialized, this method will attempt to send the provided
     * message to the configured recipient. The operation is non-blocking.
     * Any failures during the sending process are logged as errors.
     * </p>
     *
     * @param msg The message content to be sent.
     */
    @Override
    public void sendMessage(String msg) {
        String fullMessage = messagePrefixProvider.getMessagePrefix() + msg;

        sender.ifPresent(s -> {
            if (recipientMail != null && !recipientMail.isBlank()) {
                s.sendMessageAsync(fullMessage, subject, recipientMail)
                        .whenComplete((response, throwable) -> {
                            if (throwable != null) {
                                log.error("Failed to send Email message.", throwable);
                            } else {
                                log.debug("Email message sent successfully.");
                            }
                        });
            } else {
                log.warn("Attempted to send an email, but recipient address is not configured. Message not sent: \"{}\"", msg);
            }
        });
    }
}
