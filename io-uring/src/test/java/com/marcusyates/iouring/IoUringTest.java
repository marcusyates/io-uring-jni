package com.marcusyates.iouring;


import org.junit.Test;

import java.io.IOException;

public class IoUringTest {
    @Test
    public void shouldCreateAnIoUring() throws IOException {
        final int entries = 8;
        //noinspection EmptyTryBlock
        try (IoUring ignored = IoUring.create(entries)) {
        }
    }
}