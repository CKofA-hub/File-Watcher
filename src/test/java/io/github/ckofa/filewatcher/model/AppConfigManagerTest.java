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
    @DisplayName("When initializing the object, the settings from the config file must be read out")
    void initialization_whenConfigFileExist() throws IOException {
        // Prepare: config file exists, mockReader with test settings is created
        when(configFile.exists()).thenReturn(true);
        Reader mockReader = new StringReader("mail.send.enabled=true\nmail.smtp.host=smtp.test.com\nmail.smtp.port=220");
        when(fileProvider.createReader(configFile)).thenReturn(mockReader);

        // Action: object initialization
        appConfigManager = AppConfigManager.getInstance(fileProvider);

        //Checking FileProvider method calls
        verify(fileProvider, times(1)).createReader(configFile);
        verify(fileProvider, never()).createWriter(any(File.class));

        //Checking that the settings in memory correspond to those passed through mockReader
        assertAll(
                () -> assertTrue(appConfigManager.isSettingEnabled(AppConfigManager.Settings.MAIL_SEND_ENABLED)),
                () -> assertEquals(appConfigManager.getSettingValue(AppConfigManager.Settings.MAIL_SMTP_HOST), "smtp.test.com"),
                () -> assertEquals(appConfigManager.getIntSettingValue(AppConfigManager.Settings.MAIL_SMTP_PORT), 220)
        );

    }

    @Test
    @DisplayName("When the object is initialized, a settings file must be created and default settings must be written to it")
    void initialization_whenConfigFileNotExist() throws IOException {
        // Prepare: config file does not exist, mockWriter is created to check the customization that will be written to the “file”
        when(configFile.exists()).thenReturn(false);
        Writer mockWriter = new StringWriter();
        when(fileProvider.createWriter(configFile)).thenReturn(mockWriter);

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
            assertNotNull(writtenValue, "Setting " + setting.getKey() + " should be written to file");
            assertEquals(setting.getDefaultValue(), writtenValue,
                    "Setting " + setting.getKey() + " in file should match default value");
        }
    }

    @Test
    @DisplayName("When initializing an object, it should throw an exception if it fails to read the settings file")
    void initialization_whenReadingFails_shouldThrowsIOException() throws IOException {
        // Prepare: config file exists, but reading settings causes an exception
        String exceptionMsg = "Read error config file";
        when(configFile.exists()).thenReturn(true);
        when(fileProvider.createReader(configFile)).thenThrow(new IOException(exceptionMsg));

        //Checking the type of exception thrown and its message
        IOException ioException = assertThrows(IOException.class, () -> {
            AppConfigManager.getInstance(fileProvider);
        });
        assertEquals(exceptionMsg, ioException.getMessage());

        //Checking FileProvider method calls
        verify(fileProvider, times(1)).createReader(configFile);
        verify(fileProvider, never()).createWriter(any(File.class));
    }

    @Test
    @DisplayName("When initializing an object, it should throw an exception if it fails to write settings to a file")
    void initialization_whenWritingFails_ThrowsIOException() throws IOException {
        // Prepare: config file does not exist, writing settings causes an exception
        String exceptionMsg = "Error writing to a config file";
        when(configFile.exists()).thenReturn(false);
        when(fileProvider.createWriter(configFile)).thenThrow(new IOException(exceptionMsg));

        //Checking the type of exception thrown and its message
        IOException ioException = assertThrows(IOException.class, () -> {
            AppConfigManager.getInstance(fileProvider);
        });
        assertEquals(exceptionMsg, ioException.getMessage());

        //Checking FileProvider method calls
        verify(fileProvider, times(1)).createWriter(configFile);
        verify(fileProvider, never()).createReader(any(File.class));
    }


    @Test
    @DisplayName("Checking that the settings in memory and in the “file” contain the settings passed to the method")
    void setSettingValue() throws IOException {
        // Prepare: config file does not exist, mockWriter is created to check the customization that will be written to the “file”
        when(configFile.exists()).thenReturn(false);
        Writer mockWriter = new StringWriter();
        when(fileProvider.createWriter(configFile)).thenReturn(mockWriter);

        // Action: object initialization and write setting
        appConfigManager = AppConfigManager.getInstance(fileProvider);
        appConfigManager.setSettingValue(AppConfigManager.Settings.MAIL_SMTP_HOST, "smtp.gmail.com");

        //Checking that the transferred settings are stored in memory
        assertEquals(appConfigManager.getSettingValue(AppConfigManager.Settings.MAIL_SMTP_HOST), "smtp.gmail.com");

        //Checking that the setting has been written to a "file"
        Properties writtenProps = new Properties();
        writtenProps.load(new StringReader(mockWriter.toString()));
        assertEquals(writtenProps.getProperty("mail.smtp.host"), "smtp.gmail.com");

        //Checking FileProvider method calls
        verify(fileProvider, times(2)).createWriter(configFile);
    }

    @Test
    @DisplayName("Checking that the received settings correspond to the specified test settings")
    void getSettingValue_getIntSettingValue_isSettingEnabled() throws IOException {
        // Prepare: file does not exist, initialize with Writer
        when(configFile.exists()).thenReturn(false);
        when(fileProvider.createWriter(configFile)).thenReturn(new StringWriter());

        //Action: create an instance and set test values (string. boolean, int)
        appConfigManager = AppConfigManager.getInstance(fileProvider);
        appConfigManager.setSettingValue(AppConfigManager.Settings.MAIL_SEND_ENABLED, "true");
        appConfigManager.setSettingValue(AppConfigManager.Settings.MAIL_SMTP_PORT, "578");
        appConfigManager.setSettingValue(AppConfigManager.Settings.MAIL_SMTP_HOST, "smtp.test.com");

        //Checking that the resulting settings correspond to the specified test settings
        assertAll(
                () -> assertTrue(appConfigManager.isSettingEnabled(AppConfigManager.Settings.MAIL_SEND_ENABLED)),
                () -> assertEquals(578, appConfigManager.getIntSettingValue(AppConfigManager.Settings.MAIL_SMTP_PORT)),
                () -> assertEquals("smtp.test.com", appConfigManager.getSettingValue(AppConfigManager.Settings.MAIL_SMTP_HOST))
        );
    }

    @Test
    @DisplayName("When the setting has no representation of type int throw an exception")
    void getIntSettingValue_whenInvalidValue_ThrowsException() throws IOException {
        // Prepare: file does not exist, initialize with Writer
        when(configFile.exists()).thenReturn(false);
        when(fileProvider.createWriter(configFile)).thenReturn(new StringWriter());

        //Action: create an instance and set an invalid value
        appConfigManager = AppConfigManager.getInstance(fileProvider);
        appConfigManager.setSettingValue(AppConfigManager.Settings.MAIL_SMTP_PORT, "invalid");

        //Check: expect an exception
        assertThrows(IllegalArgumentException.class,
                () -> appConfigManager.getIntSettingValue(AppConfigManager.Settings.MAIL_SMTP_PORT));
    }

    @Test
    @DisplayName("The AppConfigManager object is initialized with the defined settings and then they are overwritten with the default settings")
    void resetSettingsToDefault() throws IOException {
        // Prepare: config file exists, a mockReader with test settings and a mockWriter to check the settings that will be written to the "file" are created
        when(configFile.exists()).thenReturn(true);
        Reader mockReader = new StringReader("mail.send.enabled=true\nmail.smtp.host=smtp.test.com\nmail.smtp.port=220");
        when(fileProvider.createReader(configFile)).thenReturn(mockReader);
        StringWriter mockWriter = new StringWriter();
        when(fileProvider.createWriter(configFile)).thenReturn(mockWriter);

        // Action: object initialization
        appConfigManager = AppConfigManager.getInstance(fileProvider);

        //Checking that the settings in memory correspond to those passed through mockReader
        assertAll(
                () -> assertTrue(appConfigManager.isSettingEnabled(AppConfigManager.Settings.MAIL_SEND_ENABLED)),
                () -> assertEquals(appConfigManager.getSettingValue(AppConfigManager.Settings.MAIL_SMTP_HOST), "smtp.test.com"),
                () -> assertEquals(appConfigManager.getIntSettingValue(AppConfigManager.Settings.MAIL_SMTP_PORT), 220)
        );

        // Action: reset setting
        appConfigManager.resetSettingsToDefault();

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
            assertNotNull(writtenValue, "Setting " + setting.getKey() + " should be written to file");
            assertEquals(setting.getDefaultValue(), writtenValue,
                    "Setting " + setting.getKey() + " in file should match default value");
        }

        //Checking FileProvider method calls
        verify(fileProvider, times(1)).createWriter(configFile);
        verify(fileProvider, times(1)).createReader(configFile);
    }

    @Test
    @DisplayName("Should return the name of the config file and the path to the directory where the program is launched from")
    void getAppConfigFileName_And_getAppDirectoryPath() throws IOException {
        // Prepare: config file not exists, mock for name and path
        when(configFile.exists()).thenReturn(false);
        when(fileProvider.createWriter(configFile)).thenReturn(new StringWriter());
        when(configFile.getName()).thenReturn("test.properties");
        when(configFile.getParent()).thenReturn("/app/dir");

        // Action: create an instance
        appConfigManager = AppConfigManager.getInstance(fileProvider);

        //Checking
        assertEquals("test.properties", appConfigManager.getAppConfigFileName());
        assertEquals("/app/dir", appConfigManager.getAppDirectoryPath());
    }

    /**
     * A private method for resetting Singleton.
     */
    private void resetSingleton() {
        String fieldName = "instance"; //The field where the class instance is stored
        try {
            Field instance = AppConfigManager.class.getDeclaredField(fieldName);
            instance.setAccessible(true); // Open access to the private field
            instance.set(null, null); // Reset Singleton value
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to reset Singleton", e);
        }
    }
}