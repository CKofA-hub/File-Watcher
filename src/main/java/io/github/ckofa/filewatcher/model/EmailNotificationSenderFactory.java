package io.github.ckofa.filewatcher.model;

import java.util.Optional;
import io.github.ckofa.filewatcher.model.AppConfigManager.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A factory for creating {@link EmailNotificationSender} instances based on application configuration.
 * This is a utility class and cannot be instantiated.
 */
public final class EmailNotificationSenderFactory {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationSenderFactory.class);

    private EmailNotificationSenderFactory() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Creates an {@link EmailNotificationSender} based on the provided configuration.
     * <p>
     * The method checks if email notifications are enabled in the configuration. If not,
     * it returns an empty {@link Optional}. Otherwise, it attempts to create either a
     * corporate or public email sender, depending on the {@code MAIL_CORPORATE_ENABLE} setting.
     * <p>
     * If the creation of the sender fails for any reason (e.g., configuration issues),
     * an error is logged, and an empty {@link Optional} is returned.
     *
     * @param config The application configuration manager.
     * @return an {@link Optional} containing the configured {@link EmailNotificationSender},
     *         or an empty {@link Optional} if notifications are disabled or creation fails.
     */
    public static Optional<EmailNotificationSender> create(AppConfigManager config) {
        if (!config.isSettingEnabled(Settings.MAIL_SEND_ENABLED)) {
            return Optional.empty();
        }

        try {
            String email = config.getSettingValue(Settings.MAIL_SENDER_EMAIL);
            String password = config.getSettingValue(Settings.MAIL_SENDER_PASSWORD);
            String host = config.getSettingValue(Settings.MAIL_SMTP_HOST);
            int port = config.getIntSettingValue(Settings.MAIL_SMTP_PORT);

            EmailNotificationSender sender;
            if (config.isSettingEnabled(Settings.MAIL_CORPORATE_ENABLE)) {
                sender = EmailNotificationSender.createCorporateEmailNotificationSender(email, password, host, port);
            } else {
                sender = EmailNotificationSender.createPublicEmailNotificationSender(email, password, host, port);
            }
            return Optional.of(sender);
        } catch (Exception e) {
            log.error("Failed to create EmailNotificationSender due to a configuration error.", e);
            return Optional.empty();
        }
    }
}
