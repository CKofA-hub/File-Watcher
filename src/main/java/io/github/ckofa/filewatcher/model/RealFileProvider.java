package io.github.ckofa.filewatcher.model;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

/**
 * An implementation of {@link FileProvider} to work with a real file system.
 * Creates a File object in the application directory with the given name and provides access to it.
 */
public class RealFileProvider implements FileProvider {
    private final File file;

    /**
     * Creates a provider to work with a file whose name is specified by a parameter.
     *
     * @param fileName the name of the file to be accessed.
     * @throws RuntimeException if the path to the application directory could not be determined.
     */
    public RealFileProvider(String fileName) {
        // Returns folder if run from ide, returns path to JAR file if run from jar
        try {
            File appFile = new File(RealFileProvider.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI());
            // If the program is run from JAR, take the parent folder
            File appDir = appFile.isFile() ? appFile.getParentFile() : appFile;
            String appDirectoryPath = appDir.getAbsolutePath(); //get the absolute path regardless of where the application is launched from (IDE, JAR)
            this.file = new File(appDirectoryPath, fileName);
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed to determine the file path: " + fileName, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public File getConfigFile() {
        return file;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Reader createReader(File file) throws IOException {
        return new FileReader(file, StandardCharsets.UTF_8);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Writer createWriter(File file) throws IOException {
        return new FileWriter(file, StandardCharsets.UTF_8);
    }
}
