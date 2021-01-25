package com.marcusyates.iouring.testing;

import com.marcusyates.iouring.IoUring;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class WriteToFileIntegrationTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private IoUring ioUring;

    @Before
    public void setUp() throws Exception {
        temporaryFolder.create();
        ioUring = IoUring.create(16);
    }

    @After
    public void tearDown() {
        ioUring.close();
    }

    @Test
    public void shouldWriteTextToFileInOneGo() throws IOException {
        final File file = temporaryFolder.newFile("output.txt");

        //noinspection OptionalGetWithoutIsPresent
        final Path path = TestResources.getResource("example.txt").get();
        final String fileContents = Files.readString(path);
        final byte[] bytes = fileContents.getBytes(UTF_8);

        final ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length);
        buffer.put(bytes);

        try (final FileOutputStream output = new FileOutputStream(file)) {
            ioUring.submitWrite(output.getFD(), buffer, 0);
            ioUring.waitForCompletion();
        }

        final String actual = Files.readString(file.toPath());
        assertThat(actual.length(), is(fileContents.length()));
        assertThat(actual, is(fileContents));
    }
}
