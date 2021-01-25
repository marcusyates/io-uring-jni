package com.marcusyates.iouring.testing;

import com.marcusyates.iouring.IoUring;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ReadFileIntegrationTest {
    private IoUring ioUring;
    private Path inputFile;

    @Before
    public void setUp() throws Exception {
        ioUring = IoUring.create(16);
        //noinspection OptionalGetWithoutIsPresent
        inputFile = TestResources.getResource("example.txt").get();
    }

    @After
    public void tearDown() {
        ioUring.close();
    }

    @Test
    public void shouldReadTextFileInOneGo() throws IOException {
        final int fileSize = (int) Files.size(inputFile);

        // allocate enough memory to store the whole file
        final int blockSize = 1024;
        final int numBlocks = 1 + (fileSize / blockSize);
        final ByteBuffer buffer = ByteBuffer.allocateDirect(numBlocks * blockSize);

        try (final FileInputStream input = new FileInputStream(inputFile.toFile())) {
            ioUring.submitRead(input.getFD(), buffer, 0);

            final int numBytesRead = ioUring.waitForCompletion();
            buffer.limit(numBytesRead);

            final String fileContents = UTF_8.decode(buffer).toString();

            assertThat(fileContents.length(), is(fileSize));
            assertThat(fileContents, is(Files.readString(inputFile)));
        }
    }

    @Test
    public void shouldReadTextFileInChunks() throws IOException {
        final int fileSize = (int) Files.size(inputFile);
        final int blockSize = 1024;
        final ByteBuffer buffer = ByteBuffer.allocateDirect(blockSize);

        final StringBuilder fileContents = new StringBuilder();
        try (final FileInputStream input = new FileInputStream(inputFile.toFile())) {

            int pos = 0;
            while (true) {
                ioUring.submitRead(input.getFD(), buffer, pos++ * blockSize);

                final int numBytesRead = ioUring.waitForCompletion();
                if (numBytesRead == 0) {
                    break;
                }

                buffer.limit(numBytesRead);
                fileContents.append(UTF_8.decode(buffer));
                buffer.flip();
            }

            assertThat(fileContents.length(), is(fileSize));
            assertThat(fileContents.toString(), is(Files.readString(inputFile)));
        }
    }

    @Test
    public void shouldReadTextFileInMultipleChunks() throws IOException {
        final int fileSize = (int) Files.size(inputFile);
        final int blockSize = 1024;

        final ByteBuffer buffer1 = ByteBuffer.allocateDirect(blockSize);
        final ByteBuffer buffer2 = ByteBuffer.allocateDirect(blockSize);
        final ByteBuffer buffer3 = ByteBuffer.allocateDirect(blockSize);
        final ByteBuffer buffer4 = ByteBuffer.allocateDirect(blockSize);

        final StringBuilder fileContents = new StringBuilder();
        try (final FileInputStream input = new FileInputStream(inputFile.toFile())) {

            int pos = 0;
            while (true) {
                ioUring.submitRead(input.getFD(), buffer1, pos++ * blockSize);
                ioUring.submitRead(input.getFD(), buffer2, pos++ * blockSize);
                ioUring.submitRead(input.getFD(), buffer3, pos++ * blockSize);
                ioUring.submitRead(input.getFD(), buffer4, pos++ * blockSize);

                final int numBytesRead1 = ioUring.waitForCompletion();
                final int numBytesRead2 = ioUring.waitForCompletion();
                final int numBytesRead3 = ioUring.waitForCompletion();
                final int numBytesRead4 = ioUring.waitForCompletion();

                buffer1.limit(numBytesRead1);
                buffer2.limit(numBytesRead2);
                buffer3.limit(numBytesRead3);
                buffer4.limit(numBytesRead4);

                fileContents.append(UTF_8.decode(buffer1));
                fileContents.append(UTF_8.decode(buffer2));
                fileContents.append(UTF_8.decode(buffer3));
                fileContents.append(UTF_8.decode(buffer4));

                buffer1.flip();
                buffer2.flip();
                buffer3.flip();
                buffer4.flip();

                if (numBytesRead1 == 0 || numBytesRead2 == 0 || numBytesRead3 == 0 || numBytesRead4 == 0) {
                    break;
                }
            }

            assertThat(fileContents.length(), is(fileSize));
            assertThat(fileContents.toString(), is(Files.readString(inputFile)));
        }
    }
}
