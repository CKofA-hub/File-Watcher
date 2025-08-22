package io.github.ckofa.filewatcher.model;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.*;
import java.lang.reflect.Field;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class AppConfigManagerTest {

    @Mock
    private FileProvider fileProvider;
    @Mock
    private File configFile;
    private AppConfigManager appConfigManager;

    @BeforeEach
    void setUp() {
        //Configuring fileProvider behavior
        when(fileProvider.getConfigFile()).thenReturn(configFile);
    }

    @AfterEach
    void tearDown() {
        //Reset Singleton after each test
        resetSingleton();
    }

    @Test
    @DisplayName("When initializing, should read settings from an existing config file")
    void initialization_whenConfigFileExist_shouldReadSettings() throws IOException {
        // Prepare: config file exists, mockReader with test settings is created
        String fileContent = "mail.send.enabled=true\nmail.smtp.host=smtp.test.com\nmail.smtp.port=220";
        setupForExistingConfigFileRead(fileContent);

        // Action: object initialization
        appConfigManager = AppConfigManager.getInstance(fileProvider);

        // Asserts
        verify(fileProvider, times(1)).createReader(configFile);
        verify(fileProvider, never()).createWriter(any(File.class));

        //Checking that the settings in memory correspond to those passed through mockReader
        assertAll("Verify settings loaded from file",
                () -> assertTrue(appConfigManager.isSettingEnabled(AppConfigManager.Settings.MAIL_SEND_ENABLED)),
                () -> assertEquals("smtp.test.com", appConfigManager.getSettingValue(AppConfigManager.Settings.MAIL_SMTP_HOST)),
                () -> assertEquals(220, appConfigManager.getIntSettingValue(AppConfigManager.Settings.MAIL_SMTP_PORT))
        );
    }

    @Test
    @DisplayName("When initializing and config file does not exist, should create it with default settings")
    void initialization_whenConfigFileDoesNotExist_shouldCreateWithDefaults() throws IOException {
        // Prepare: config file does not exist, mockWriter is created to check the customization that will be written to the “file”
        StringWriter mockWriter = setupForNewConfigFileCreation();

        // Action: object initialization
        appConfigManager = AppConfigManager.getInstance(fileProvider);

        //Checking FileProvider method calls
        verify(fileProvider, times(1)).createWriter(configFile);
        verify(fileProvider, never()).createReader(configFile);

        //Checking that all settings in memory have default values
        for (AppConfigManager.Settings setting : AppConfigManager.Settings.values()) {
            assertEquals(setting.getDefaultValue(), appConfigManager.getSettingValue(setting),
                    "Setting " + setting.getKey() + " should have default value");
        }

        //Checking that all settings are written to a “file” with default values
        Properties writtenProps = new Properties();
        writtenProps.load(new StringReader(mockWriter.toString()));
        for (AppConfigManager.Settings setting : AppConfigManager.Settings.values()) {
            String writtenValue = writtenProps.getProperty(setting.getKey());
            assertEquals(setting.getDefaultValue(), writtenValue,
                    "Setting " + setting.getKey() + " in file should match default value");
        }
    }

    @Test
    @DisplayName("Should throw IOException if reading the config file fails")
    void initialization_whenReadingFails_shouldThrowsIOException() throws IOException {
        // Prepare: config file exists, but reading settings causes an exception
        String exceptionMsg = "Read error config file";
        when(configFile.exists()).thenReturn(true);
        when(fileProvider.createReader(configFile)).thenThrow(new IOException(exceptionMsg));

        // Act & Assert
        IOException ioException = assertThrows(IOException.class, () -> {
            AppConfigManager.getInstance(fileProvider);
        });
        assertEquals(exceptionMsg, ioException.getMessage());
    }

    @Test
    @DisplayName("Should throw IOException if writing the new config file fails")
    void initialization_whenWritingFails_shouldThrowsIOException() throws IOException {
        // Prepare: config file does not exist, writing settings causes an exception
        String exceptionMsg = "Error writing to a config file";
        when(configFile.exists()).thenReturn(false);
        when(fileProvider.createWriter(configFile)).thenThrow(new IOException(exceptionMsg));

        // Act & Assert
        //Checking the type of exception thrown and its message
        IOException ioException = assertThrows(IOException.class, () -> {
            AppConfigManager.getInstance(fileProvider);
        });
        assertEquals(exceptionMsg, ioException.getMessage());
    }


    @Test
    @DisplayName("setSettingValue should update value in memory and in the file")
    void setSettingValue_shouldUpdateInMemoryAndInFile() throws IOException {
        // Prepare: config file does not exist, mockWriter is created to check the customization that will be written to the “file”
        StringWriter mockWriter = setupForNewConfigFileCreation();

        // Action: object initialization and write setting
        appConfigManager = AppConfigManager.getInstance(fileProvider);
        appConfigManager.setSettingValue(AppConfigManager.Settings.MAIL_SMTP_HOST, "smtp.gmail.com");

        // Assert
        // Checking that the transferred settings are stored in memory
        assertEquals("smtp.gmail.com", appConfigManager.getSettingValue(AppConfigManager.Settings.MAIL_SMTP_HOST));

        //Checking that the setting has been written to a "file"
        Properties writtenProps = new Properties();
        writtenProps.load(new StringReader(mockWriter.toString()));
        assertEquals("smtp.gmail.com", writtenProps.getProperty("mail.smtp.host"));

        //Checking FileProvider method calls
        verify(fileProvider, times(2)).createWriter(configFile);
    }

    @Test
    @DisplayName("getSettingValue should return the correct string value")
    void getSettingValue_shouldReturnCorrectStringValue() throws IOException {
        // Arrange: file does not exist, initialize with Writer
        when(configFile.exists()).thenReturn(false);
        when(fileProvider.createWriter(configFile)).thenReturn(new StringWriter());
        appConfigManager = AppConfigManager.getInstance(fileProvider);
        appConfigManager.setSettingValue(AppConfigManager.Settings.MAIL_SMTP_HOST, "smtp.test.com");

        // Act: get setting value
        String actualValue = appConfigManager.getSettingValue(AppConfigManager.Settings.MAIL_SMTP_HOST);

        //Assert
        assertEquals("smtp.test.com", actualValue);
    }

    @Test
    @DisplayName("isSettingEnabled should return the correct boolean value")
    void isSettingEnabled_shouldReturnCorrectBooleanValue() throws IOException {
        // Arrange: file does not exist, initialize with Writer
        when(configFile.exists()).thenReturn(false);
        when(fileProvider.createWriter(configFile)).thenReturn(new StringWriter());
        appConfigManager = AppConfigManager.getInstance(fileProvider);
        appConfigManager.setSettingValue(AppConfigManager.Settings.MAIL_SEND_ENABLED, "true");

        // Act: get setting value
        boolean isEnabled = appConfigManager.isSettingEnabled(AppConfigManager.Settings.MAIL_SEND_ENABLED);

        // Assert
        assertTrue(isEnabled);
    }

    @Test
    @DisplayName("getIntSettingValue should return the correct integer value")
    void getIntSettingValue_shouldReturnCorrectIntValue() throws IOException {
        // Arrange: file does not exist, initialize with Writer
        when(configFile.exists()).thenReturn(false);
        when(fileProvider.createWriter(configFile)).thenReturn(new StringWriter());
        appConfigManager = AppConfigManager.getInstance(fileProvider);
        appConfigManager.setSettingValue(AppConfigManager.Settings.MAIL_SMTP_PORT, "578");

        // Act: get setting value
        int actualPort = appConfigManager.getIntSettingValue(AppConfigManager.Settings.MAIL_SMTP_PORT);

        // Assert
        assertEquals(578, actualPort);
    }

    @Test
    @DisplayName("getLongSettingsValue should return the correct long value")
    void getLongSettingsValue_shouldReturnCorrectLongValue() throws IOException {
        // Arrange
        when(configFile.exists()).thenReturn(false);
        when(fileProvider.createWriter(configFile)).thenReturn(new StringWriter());
        appConfigManager = AppConfigManager.getInstance(fileProvider);
        appConfigManager.setSettingValue(AppConfigManager.Settings.TELEGRAM_CHAT_ID, "1234567890");

        // Act
        long actualChatId = appConfigManager.getLongSettingValue(AppConfigManager.Settings.TELEGRAM_CHAT_ID);

        // Assert: get setting value
        assertEquals(1234567890L, actualChatId);
    }


    @Test
    @DisplayName("getIntSettingValue should throw exception for a non-integer value")
    void getIntSettingValue_whenInvalidValue_shouldThrowsException() throws IOException {
        // Prepare: file does not exist, initialize with Writer
        setupForNewConfigFileCreation();
        appConfigManager = AppConfigManager.getInstance(fileProvider);
        appConfigManager.setSettingValue(AppConfigManager.Settings.MAIL_SMTP_PORT, "not-a-number");

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> appConfigManager.getIntSettingValue(AppConfigManager.Settings.MAIL_SMTP_PORT));
    }

    @Test
    @DisplayName("getLongSettingsValue should throw exception for a non-long value")
    void getLongSettingValue_whenInvalidValue_shouldThrowException() throws IOException {
        // Prepare: file does not exist, initialize with Writer
        setupForNewConfigFileCreation();
        appConfigManager = AppConfigManager.getInstance(fileProvider);
        appConfigManager.setSettingValue(AppConfigManager.Settings.TELEGRAM_CHAT_ID, "not-a-number");

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> appConfigManager.getLongSettingValue(AppConfigManager.Settings.TELEGRAM_CHAT_ID));
    }

    @Test
    @DisplayName("The AppConfigManager object is initialized with the defined settings and then they are overwritten with the default settings")
    void resetSettingsToDefault_shouldRestoreDefaults() throws IOException {
        // Arrange: Start with a file with custom settings
        String fileContent = "mail.send.enabled=true\nmail.smtp.host=smtp.test.com";
        setupForExistingConfigFileRead(fileContent);
        appConfigManager = AppConfigManager.getInstance(fileProvider);

        // Arrange a writer for the reset operation
        StringWriter mockWriter = new StringWriter();
        when(fileProvider.createWriter(configFile)).thenReturn(mockWriter);

        // Act
        appConfigManager.resetSettingsToDefault();

        // Assert: Check that all settings in memory are now default
        for (AppConfigManager.Settings setting : AppConfigManager.Settings.values()) {
            assertEquals(setting.getDefaultValue(), appConfigManager.getSettingValue(setting),
                    "Setting " + setting.getKey() + " should have default value after reset");
        }
    }

    @Test
    @DisplayName("Should return the name of the config file and the path to the directory where the program is launched from")
    void getAppConfigFileName_And_getAppDirectoryPath() throws IOException {
        // Prepare: config file not exists, mock for name and path
        when(configFile.exists()).thenReturn(false);
        when(fileProvider.createWriter(configFile)).thenReturn(new StringWriter());
        when(configFile.getName()).thenReturn("test.properties");
        when(configFile.getParent()).thenReturn("/app/dir");

        // Act
        appConfigManager = AppConfigManager.getInstance(fileProvider);

        // Assert
        assertEquals("test.properties", appConfigManager.getAppConfigFileName());
        assertEquals("/app/dir", appConfigManager.getAppDirectoryPath());
    }

    // ---- Helper Methods ----

    /**
     * Sets up mocks for a scenario where the config file does not exist and will be created.
     * @return A StringWriter to capture the output.
     * @throws IOException if mock setup fails.
     */
    private StringWriter setupForNewConfigFileCreation() throws IOException {
        when(configFile.exists()).thenReturn(false);
        StringWriter mockWriter = new StringWriter();
        when(fileProvider.createWriter(configFile)).thenReturn(mockWriter);
        return mockWriter;
    }

    /**
     * Sets up mocks for a scenario where the config file already exists.
     * @param fileContent The string content to be "read" from the file.
     * @throws IOException if mock setup fails.
     */
    private void setupForExistingConfigFileRead(String fileContent) throws IOException {
        when(configFile.exists()).thenReturn(true);
        StringReader mockReader = new StringReader(fileContent);
        when(fileProvider.createReader(configFile)).thenReturn(mockReader);
    }

    /**
     * Resets the Singleton instance of AppConfigManager using reflection.
     * This is a common pattern for testing singletons to ensure test isolation.
     */
    private void resetSingleton() {
        String fieldName = "instance"; //The field where the class instance is stored
        try {
            Field instance = AppConfigManager.class.getDeclaredField(fieldName);
            instance.setAccessible(true); // Open access to the private field
            instance.set(null, null); // Reset Singleton value
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to reset AppConfigManager singleton", e);
        }
    }
}