package io.github.ckofa.filewatcher.model;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

/**
 * An interface for providing file access and creating I/O streams.
 * Defines methods to retrieve a file and create a reader/writer to work with the file system.
 */
public interface FileProvider {

    /**
     * Returns the master file associated with this provider (for example, a settings file).
     *
     * @return {@link File} object representing the main file.
     */
    File getConfigFile();

    /**
     * Creates a reader for the specified file.
     *
     * @param file file object for which a reader is created.
     * @return reader {@link Reader} object to read the contents of the file.
     * @throws IOException if the file could not be opened for reading.
     */
    Reader createReader(File file) throws IOException;

    /**
     * Creates a writer for the specified file.
     *
     * @param file file object for which a writer is created.
     * @return writer {@link Writer} object to write to a file.
     * @throws IOException if the file could not be opened for writing.
     */
    Writer createWriter(File file) throws IOException;
}
