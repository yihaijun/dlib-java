package com.github.radium226.commons;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class Libraries {

    @FunctionalInterface
    public interface LibraryLoader {

        void loadLibrary(String libraryName);

    }

    public static LibraryLoader SYSTEM = libraryName -> System.loadLibrary(libraryName);

    public static LibraryLoader RESOURCES = libraryName -> {
        try {
            // We unzip the SO
            InputStream inputStream = Libraries.class.getClassLoader().getResourceAsStream(libraryFileName(libraryName));
            if (inputStream != null) {
                Path temporaryFolderPath = Files.createTempDirectory(libraryName);
                Path libraryFilePath = temporaryFolderPath.resolve(libraryFileName(libraryName));
                Files.copy(inputStream, libraryFilePath);

                // We load it
                System.load(libraryFilePath.toString());
            } else {
                throw new UnsatisfiedLinkError(libraryName);
            }
        } catch (IOException e) {
            throw new UnsatisfiedLinkError(libraryName);
        }
    };

    public static LibraryLoader SWIG_GENERATED_RESOURCES = libraryName -> {
        Path libraryFilePath = Paths
                .get(System.getProperty("user.dir"))
                .resolve("target/generated-resources/swig/")
                .resolve(libraryFileName(libraryName));
        if (Files.exists(libraryFilePath)) {
            System.load(libraryFilePath.toString());
        } else {
            throw new UnsatisfiedLinkError(libraryName);
        }
    };

    public static LibraryLoader OPENCV_JAVA = libraryName -> {
        Path libraryFilePath = Paths.get("/usr/share/opencv/java").resolve(libraryFileName(libraryName));
        if (Files.exists(libraryFilePath)) {
            System.load(libraryFilePath.toString());
        } else {
            throw new UnsatisfiedLinkError(libraryName);
        }
    };

    public static String libraryFileName(String libraryName) {
        return "lib" + libraryName + ".so";
    }

    public static List<LibraryLoader> libraryLoadersFor(String libraryName) {
        return Arrays.asList(SYSTEM, RESOURCES, SWIG_GENERATED_RESOURCES, OPENCV_JAVA);
    }

    public static void loadLibrary(String libraryName) {
        UnsatisfiedLinkError lastUnsatisfiedLinkError = null;
        for (LibraryLoader libraryLoader : libraryLoadersFor(libraryName)) {
            try {
                libraryLoader.loadLibrary(libraryName);
                lastUnsatisfiedLinkError = null;
            } catch (UnsatisfiedLinkError unsatisfiedLinkError) {
                lastUnsatisfiedLinkError = unsatisfiedLinkError;
            }

            if (lastUnsatisfiedLinkError == null) {
                break;
            }
        }

        if (lastUnsatisfiedLinkError != null) {
            throw lastUnsatisfiedLinkError;
        }
    }

}
