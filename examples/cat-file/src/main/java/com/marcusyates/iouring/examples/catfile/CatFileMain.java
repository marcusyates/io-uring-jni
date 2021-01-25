package com.marcusyates.iouring.examples.catfile;

import com.marcusyates.iouring.IoUring;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.charset.StandardCharsets.UTF_8;

public class CatFileMain implements AutoCloseable {
    private static final CharsetDecoder DECODER = UTF_8.newDecoder();
    private static final int BLOCK_SIZE = 1024 * 8;
    private static final int NUM_BLOCKS = 16;
    private static final CharBuffer CHAR_BUFFER = CharBuffer.allocate(BLOCK_SIZE * 4);

    private final Path path;
    private final IoUring ioUring;
    private final ByteBuffer[] byteBuffer;
    private final PrintStream out;

    private FileInputStream fileInputStream = null;

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: catfile <path-to-file>");
            System.exit(1);
        }
        final Path inputFilePath = Paths.get(args[0]);
        final ByteBuffer[] byteBuffer = allocateDirectBuffers(BLOCK_SIZE, NUM_BLOCKS);
        final PrintStream printStream = System.out;

        try (CatFileMain cat =
                     new CatFileMain(
                             inputFilePath,
                             IoUring.create(NUM_BLOCKS * 4),
                             byteBuffer,
                             printStream)) {
            cat.run();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public CatFileMain(Path path, IoUring ioUring, ByteBuffer[] byteBuffer, PrintStream out) {
        this.path = path;
        this.ioUring = ioUring;
        this.byteBuffer = byteBuffer;
        this.out = out;
    }

    private void run() {
        try {
            fileInputStream = new FileInputStream(path.toFile());
            read();

            out.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void read() throws IOException {
        final FileDescriptor fd = fileInputStream.getFD();

        final int submissionRequestBatchSize = NUM_BLOCKS / 4;

        // front-load submissionRequestBatchSize read requests
        for (int i = 0; i < submissionRequestBatchSize; i++) {
            ioUring.submitRead(fd, byteBuffer[i % NUM_BLOCKS], i * BLOCK_SIZE);
        }

        int pos = 0;
        boolean running = true;

        while (running) {
            for (int i = 0; i < submissionRequestBatchSize; i++) {
                final int idx = pos + submissionRequestBatchSize + i;
                ioUring.submitRead(fd, byteBuffer[idx % NUM_BLOCKS], idx * BLOCK_SIZE);
            }

            for (int i = 0; i < submissionRequestBatchSize; i++) {
                final int size = ioUring.waitForCompletion();
                if (size <= 0) {
                    running = false;
                    break;
                }
                final int idx = (pos + i) % NUM_BLOCKS;
                out.print(readBytesToString(byteBuffer[idx].limit(size)));
            }

            pos += submissionRequestBatchSize;
        }
    }

    private char[] readBytesToString(ByteBuffer buffer) {
        DECODER.decode(buffer, CHAR_BUFFER, false);
        CHAR_BUFFER.flip();
        buffer.flip();
        return CHAR_BUFFER.array();
    }

    @SuppressWarnings("SameParameterValue")
    private static ByteBuffer[] allocateDirectBuffers(int blockSize, int numBlocks) {
        final ByteBuffer arena = ByteBuffer.allocateDirect(blockSize * numBlocks);

        final ByteBuffer[] byteBuffer = new ByteBuffer[numBlocks];
        for (int i = 0; i < numBlocks; i++) {
            arena.position(i * blockSize);
            arena.limit((i + 1) * blockSize);
            byteBuffer[i] = arena.slice();
        }

        return byteBuffer;
    }

    @Override
    public void close() throws Exception {
        if (fileInputStream != null) {
            fileInputStream.close();
        }
        ioUring.close();
    }
}
