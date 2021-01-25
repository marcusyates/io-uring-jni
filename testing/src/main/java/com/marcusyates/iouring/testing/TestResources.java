package com.marcusyates.iouring.testing;

import java.net.URL;
import java.nio.file.Path;
import java.util.Optional;

public class TestResources {
    public static Optional<Path> getResource(String path) {
        return Optional
                .ofNullable(Thread
                        .currentThread()
                        .getContextClassLoader()
                        .getResource(path))
                .map(URL::getPath)
                .map(Path::of);
    }
}
