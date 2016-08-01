package uk.ac.ebi.chembl.util;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;


/**
 * Classpath resource utilities
 */
public class Resources {

    /**
     * Private constructor so that this class can't be instantiated
     */
    private Resources() { }


    /**
     * Loads a native library embedded as a classpath resource
     */
    public static void loadResourceLibrary(Path resourcePath) throws IOException {
        Path filePath = extractResourceIntoTmp(resourcePath);
        System.load(filePath.toAbsolutePath().toString());
    }


    /**
     * Extracts a classpath resource into a temporary file
     */
    public static Path extractResourceIntoTmp(Path resourcePath) throws IOException {
        String fileName = resourcePath.getFileName().toString();

        Path temp = Files.createTempFile(fileName, null);

        File tempFile = temp.toFile();
        tempFile.deleteOnExit();

        try (InputStream in = Resources.class.getResourceAsStream(resourcePath.toString())) {
            FileUtils.copyInputStreamToFile(in, tempFile);
        }

        return temp;
    }
}