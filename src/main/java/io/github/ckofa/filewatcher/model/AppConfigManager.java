package io.github.ckofa.filewatcher.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.EnumSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton class for controlling the main settings of the program.
 * <p>
 * On first startup, it creates a file with default settings if it is missing.
 * Allows you to read and write settings to a file.
 * Supports encryption of sensitive data.
 * </p>
 *
 * <h4>Example Usage:</h4>
 * try {
 *         AppConfigManager config = AppConfigManager.getInstance();
 *         String smtpHost = config.getSettingValue(AppConfigManager.Settings.MAIL_SMTP_HOST);
 *     } catch (IOException e) {
 *           // Initialization error handling
 *     }
 *
 */
public final class AppConfigManager {

    /**
     * Enum for storing application settings.
     * Contains the setting keys and their default values.
     */
    public enum Settings {

        //Email settings
        MAIL_SEND_ENABLED("mail.send.enabled", "false"), //send an e-mail
        MAIL_SMTP_HOST("mail.smtp.host", ""), //SMTP Server Address, example: smtp.gmail.com
        MAIL_SMTP_PORT("mail.smtp.port", "25"), //SMTP Port Number
        MAIL_RECIPIENT_EMAIL("mail.recipient.email", ""), //recipient's email address
        MAIL_SENDER_EMAIL("mail.sender.email", ""), //sender's email address
        MAIL_SENDER_PASSWORD("mail.sender.password", ""), //sender's email password
        MAIL_CORPORATE_ENABLE("mail.corporate.enable", "false"), // for corporate email, when login doesn't contain a domain name

        //Telegram settings
        TELEGRAM_SEND_ENABLED("telegram.send.enabled", "false"), //send a message to telegram
        TELEGRAM_BOT_TOKEN("telegram.bot.token", ""), //bot token
        TELEGRAM_CHAT_ID("telegram.chat.id", ""), //telegram chat ID where to send messages

        //Proxy server settings
        PROXY_ENABLED("proxy.enabled", "false"), //proxy server is used
        PROXY_HOST("proxy.host", ""), //proxy server address
        PROXY_PORT("proxy.port", "8080"), //proxy server port
        PROXY_AUTH_ENABLED("proxy.auth.enabled", "false"), //proxy server authorization is used
        PROXY_USERNAME("proxy.username", ""), //login for proxy server
        PROXY_PASSWORD("proxy.password", ""); //password for proxy server

        private final String key;
        private final String defaultValue;

        /**
         * Creates a setting with the specified key and default value.
         *
         * @param key setting key.
         * @param defaultValue default value.
         */
        Settings(String key, String defaultValue) {
            this.key = key;
            this.defaultValue = defaultValue;
        }

        /**
         * Returns the setting key.
         *
         * @return setting string key.
         */
        public String getKey() {
            return key;
        }

        /**
         * Returns the value of the default setting.
         *
         * @return default value.
         */
        public String getDefaultValue() {
            return defaultValue;
        }
    }

    private static final Logger log = LoggerFactory.getLogger(AppConfigManager.class);
    private static volatile AppConfigManager instance;

    /**
     * The name of the application config file.
     */
    private static final String DEFAULT_APP_CONFIG_FILE_NAME = "app.properties";

    /**
     * List of parameters that require encryption.
     */
    private static final Set<Settings> encryptedSettings = EnumSet.of(
            Settings.PROXY_PASSWORD,
            Settings.MAIL_SENDER_PASSWORD,
            Settings.TELEGRAM_BOT_TOKEN,
            Settings.TELEGRAM_CHAT_ID);

    /**
     * Contains program settings.
     */
    private final ConcurrentHashMap<String, String> properties = new ConcurrentHashMap<>();

    /**
     * Provider for file management.
     */
    private final FileProvider fileProvider;

    /**
     * The main settings file of the program.
     */
    private final File configFile;

    /**
     * Creates a configuration manager instance with the specified file provider.
     * Reads the config file if it exists, or if it does not exist, it creates a file with default settings.
     *
     * @throws IOException if an existing settings file could not be loaded or a new one created.
     */
    private AppConfigManager(FileProvider fileProvider) throws IOException {
        this.fileProvider = fileProvider;
        this.configFile = fileProvider.getConfigFile();

        if (configFile.exists()) { //if the file exists, load it
            loadPropertiesFromFile(configFile);
        } else { //if the config file does not exist, create a default settings file
            createDefaultConfigFile(configFile);
        }
    }

    /**
     * Returns a single instance of the configuration manager with the default provider.
     *
     * @return instance {@link AppConfigManager}
     * @throws IOException if settings could not be initialized.
     */
    public static AppConfigManager getInstance() throws IOException {
        return getInstance(new RealFileProvider(DEFAULT_APP_CONFIG_FILE_NAME));
    }

    /**
     * Returns a single instance of the configuration manager with the specified provider.
     * Used for testing or customization with an arbitrary file.
     *
     * @return instance {@link AppConfigManager}
     * @throws IOException if settings could not be initialized.
     */
    public static AppConfigManager getInstance(FileProvider fileProvider) throws IOException {
        if (instance == null) {
            synchronized (AppConfigManager.class) {
                if (instance == null) {
                    instance = new AppConfigManager(fileProvider);
                }
            }
        }
        return instance;
    }

    /**
     * Writes the setting to the config file.
     * If the setting is included in the {@link #encryptedSettings} list, the value will be encrypted before writing.
     *
     * @param setting   setting name.
     * @param value     setting value.
     * @throws IOException if the config file could not be written to disk.
     */
    public synchronized void setSettingValue(Settings setting, String value) throws IOException {
        if (encryptedSettings.contains(setting)) {
            value = CipherUtil.encode(value);
        }
        properties.put(setting.getKey(), value);
        writeSettingsToFile(configFile);
    }

    /**
     * Returns the value of the setting.
     * If the setting is not in the file, the default value from {@link Settings} is returned.
     *
     * @param setting   The setting to retrieve.
     * @return setting  The setting value as a {@code String}
     */
    public String getSettingValue(Settings setting) {
        String value = properties.getOrDefault(setting.getKey(), setting.getDefaultValue());
        if (encryptedSettings.contains(setting)) {
            return CipherUtil.decode(value);
        }
        return value;
    }

    /**
     * Returns the setting value in integer format.
     *
     * @param setting   The setting to retrieve.
     * @return setting  The setting value as a {@code int}
     * @throws IllegalArgumentException if the setting's value cannot be parsed as an integer.
     */
    public int getIntSettingValue(Settings setting) {
        String value = properties.getOrDefault(setting.getKey(), setting.getDefaultValue());
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Setting '" + setting.getKey() + "' with value '" + value + "' cannot be converted to an integer: ", e);
        }
    }

    /**
     * Returns the setting value in long format.
     *
     * @param setting   The setting to retrieve.
     * @return setting  The setting value as a {@code long}
     * @throws IllegalArgumentException if the setting's value cannot be parsed as a long.
     */
    public long getLongSettingValue(Settings setting) {
        String value = properties.getOrDefault(setting.getKey(), setting.getDefaultValue());
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Setting '" + setting.getKey() + "' with value '" + value + "' cannot be converted to a long.", e);
        }
    }

    /**
     * Returns the setting value in boolean format.
     *
     * @param setting   The setting to retrieve.
     * @return setting  The setting value as a {@code boolean}
     */
    public boolean isSettingEnabled(Settings setting) {
        String value = properties.getOrDefault(setting.getKey(), setting.getDefaultValue());
        return Boolean.parseBoolean(value);
    }

    /**
     * Loads program settings from a config file.
     *
     * @param configFile    file containing program settings.
     * @throws IOException if the program config file could not be read.
     */
    private void loadPropertiesFromFile(File configFile) throws IOException {
        try(Reader in = fileProvider.createReader(configFile)) {
            Properties temp = new Properties();
            temp.load(in);
            temp.forEach((key, value) -> properties.put((String) key, (String) value));
            log.debug("The settings are loaded from a file: '{}'", configFile);
        }
    }

    /**
     * Creates a configuration file with default settings and writes it to disk.
     *
     * @param configFile    file containing program settings.
     * @throws IOException if the config file could not be written to disk.
     */
    private void createDefaultConfigFile(File configFile) throws IOException {
        properties.clear();
        for (Settings setting : Settings.values()) {
            properties.put(setting.getKey(), setting.getDefaultValue());
        }
        writeSettingsToFile(configFile);
        log.debug("Config file: '{}' was created with default settings", configFile);
    }

    /**
     * Writes the current properties object to a file.
     *
     * @param configFile    file containing program settings.
     * @throws IOException if the config file could not be written to disk.
     */
    private void writeSettingsToFile(File configFile) throws IOException {
        try(Writer out = fileProvider.createWriter(configFile)) {
            Properties temp = new Properties();
            properties.forEach(temp::setProperty);
            temp.store(out, "File Watcher settings");
            log.debug("The '{}' settings file has been updated", configFile);
        }
    }

    /**
     * Resets program settings to default values.
     *
     * @throws IOException if the config file could not be written to disk.
     */
    public synchronized void resetSettingsToDefault() throws IOException {
        createDefaultConfigFile(configFile);
        log.debug("The settings: '{}' file has been reset to default values", configFile);
    }

    /**
     * Returns the name of the application config file.
     *
     * @return the name of the application config file.
     */
    public String getAppConfigFileName() {
        return configFile.getName();
    }

    /**
     * Returns the absolute path to the folder where the application is running.
     *
     * @return the absolute path to the folder where the application is running.
     */
    public String getAppDirectoryPath() {
        return configFile.getParent();
    }

}
