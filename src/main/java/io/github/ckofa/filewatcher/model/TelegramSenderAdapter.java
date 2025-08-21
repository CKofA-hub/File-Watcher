package io.github.ckofa.filewatcher.model;

import java.util.Optional;
import java.util.OptionalLong;

import io.github.ckofa.filewatcher.model.AppConfigManager.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An adapter that provides a simple, fire-and-forget interface for sending Telegram messages.
 * It handles all configuration and error logging internally.
 */
public class TelegramSenderAdapter implements MessageSender{

    private static final Logger log = LoggerFactory.getLogger(TelegramSenderAdapter.class);

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private final Optional<TelegramNotificationSender> sender;

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private final OptionalLong chatId;

    public TelegramSenderAdapter(AppConfigManager appConfigManager) {
        this.sender = TelegramNotificationSenderFactory.create(appConfigManager);
        OptionalLong chatIdValue;
        try {
            chatIdValue = OptionalLong.of(appConfigManager.getLongSettingsValue(Settings.TELEGRAM_CHAT_ID));
        } catch (IllegalArgumentException e) {
            log.error("Invalid Telegram Chat ID in configuration. Telegram notifications will be disabled.", e);
            chatIdValue = OptionalLong.empty();
        }
        this.chatId = chatIdValue;
    }

    /**
     * Asynchronously sends a message. This method returns immediately.
     * Any errors during the sending process will be logged internally.
     *
     * @param msg The message to send.
     */
    @Override
    public void sendMessage(String msg) {
        chatId.ifPresentOrElse(
                //Action if chatId EXISTS
                chatId -> sender.ifPresent(sender -> sender.sendMessageAsync(chatId, msg)
                        .whenComplete((response, throwable) -> {
                            if (throwable != null) {
                                log.error("Failed to send Telegram message.", throwable);
                            } else if (!response.isOk()) {
                                log.error("Telegram API returned an error. Code: {}, Description: {}",
                                        response.errorCode(), response.description());
                            } else {
                                log.debug("Telegram message sent successfully.");
                            }
                        })),
                //Action if chatId is NOT present
                () -> log.warn("Attempted to send a Telegram message, but Chat ID is not configured or invalid. Message not sent: \"{}\"", msg)
        );
    }
}
