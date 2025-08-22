package io.github.ckofa.filewatcher.model;

import io.github.ckofa.filewatcher.model.AppConfigManager.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * A factory for creating configured instances of {@link TelegramNotificationSender}
 * based on the application settings from an {@link AppConfigManager}.
 */
public final class TelegramNotificationSenderFactory {

    private static final Logger log = LoggerFactory.getLogger(TelegramNotificationSenderFactory.class);

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private TelegramNotificationSenderFactory() { throw new UnsupportedOperationException("Utility class"); }

    /**
     * Creates a TelegramNotificationSender based on the provided configuration.
     * <p>
     * This method reads the relevant settings (token, proxy, authentication)
     * from the AppConfigManager and constructs the sender accordingly.
     *
     * @param config The application configuration manager.
     * @return An {@link Optional} containing the configured sender if sending is enabled
     *         and the token is valid, or an empty Optional otherwise.
     */
    public static Optional<TelegramNotificationSender> create(AppConfigManager config) {
        if (!config.isSettingEnabled(Settings.TELEGRAM_SEND_ENABLED)) {
            return Optional.empty();
        }
        try {
            String token = config.getSettingValue(Settings.TELEGRAM_BOT_TOKEN);
            TelegramNotificationSender sender;

            if (config.isSettingEnabled(Settings.PROXY_ENABLED)) {
                String proxyHost = config.getSettingValue(Settings.PROXY_HOST);
                int proxyPort = config.getIntSettingValue(Settings.PROXY_PORT);
                if (config.isSettingEnabled(Settings.PROXY_AUTH_ENABLED)) {
                    String proxyUser = config.getSettingValue(Settings.PROXY_USERNAME);
                    String proxyPassword = config.getSettingValue(Settings.PROXY_PASSWORD);
                    sender = TelegramNotificationSender.createWithAuthenticatedProxy(token, proxyHost, proxyPort, proxyUser, proxyPassword);
                } else {
                    sender = TelegramNotificationSender.createWithProxy(token, proxyHost, proxyPort);
                }
            } else {
                sender = TelegramNotificationSender.create(token);
            }
            return Optional.of(sender);
        } catch (Exception e) {
            log.error("Failed to create TelegramNotificationSender due to a configuration error.", e);
            return Optional.empty();
        }
    }
}
