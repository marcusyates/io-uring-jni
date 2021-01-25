package com.marcusyates.iouring;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class NativeLibraryLoader {
    public static void loadLibrary(String name) {
        try {
            System.loadLibrary(name);
        } catch (UnsatisfiedLinkError unsatisfiedLinkError) {
            tryExtractAndLoadLibrary(name);
        }
    }

    private static void tryExtractAndLoadLibrary(String name) {
        try {
            final File libFile = extractFromJar(name);
            try {
                System.load(libFile.getAbsolutePath());
            } finally {
                libFile.deleteOnExit();
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to extract and load " + name, e);
        }
    }

    private static File extractFromJar(String name) throws IOException {
        final File temporaryDir = Files.createTempDirectory("NativeLibs").toFile();
        temporaryDir.deleteOnExit();

        final String filename = "lib" + name + ".so";
        final File extractLibraryFile = new File(temporaryDir, filename);

        try (final InputStream is = getResourceAsStream("native/" + filename)) {
            if (null == is) {
                throw new FileNotFoundException("File " + filename + " was not found inside jar.");
            }

            Files.copy(is, extractLibraryFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            extractLibraryFile.deleteOnExit();
            throw e;
        }

        return extractLibraryFile;
    }

    private static InputStream getResourceAsStream(String path) {
        return NativeLibraryLoader.class
                .getClassLoader()
                .getResourceAsStream(path);
    }
}
