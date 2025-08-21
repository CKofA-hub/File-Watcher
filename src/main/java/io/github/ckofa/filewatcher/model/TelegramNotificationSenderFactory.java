package io.github.ckofa.filewatcher.model;

import io.github.ckofa.filewatcher.model.AppConfigManager.Settings;
import java.util.Optional;

/**
 * A factory for creating configured instances of {@link TelegramNotificationSender}
 * based on the application settings from an {@link AppConfigManager}.
 */
public class TelegramNotificationSenderFactory {

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

        String token = config.getSettingValue(Settings.TELEGRAM_BOT_TOKEN);
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        if (config.isSettingEnabled(Settings.PROXY_ENABLED)) {
            String proxyHost = config.getSettingValue(Settings.PROXY_HOST);
            int proxyPort = config.getIntSettingValue(Settings.PROXY_PORT);

            if (config.isSettingEnabled(Settings.PROXY_AUTH_ENABLED)) {
                String proxyUser = config.getSettingValue(Settings.PROXY_USERNAME);
                String proxyPassword = config.getSettingValue(Settings.PROXY_PASSWORD);
                return Optional.of(
                        TelegramNotificationSender.createWithAuthenticatedProxy(token, proxyHost, proxyPort, proxyUser, proxyPassword));
            } else {
                return Optional.of(TelegramNotificationSender.createWithProxy(token, proxyHost, proxyPort));
            }
        } else {
            return Optional.of(TelegramNotificationSender.create(token));
        }
    }
}
