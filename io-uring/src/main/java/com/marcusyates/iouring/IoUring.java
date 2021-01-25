package com.marcusyates.iouring;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;

public final class IoUring implements AutoCloseable {
    private final long handle;

    private static native long ioUringCreate(int entries) throws IOException;

    private static native void ioUringDestroy(long handle);

    private static native void submitRead(long handle, FileDescriptor fd, ByteBuffer byteBuffer, int offset) throws IOException;

    private static native void submitWrite(long handle, FileDescriptor fd, ByteBuffer byteBuffer, int offset) throws IOException;

    private static native int waitForCompletion(long handle) throws IOException;

    static {
        NativeLibraryLoader.loadLibrary("io-uring-jni");
    }

    public static IoUring create(final int entries) throws IOException {
        final long handle = ioUringCreate(entries);
        return new IoUring(handle);
    }

    private IoUring(final long handle) {
        this.handle = handle;
    }

    @Override
    public void close() {
        ioUringDestroy(handle);
    }

    public void submitRead(FileDescriptor fd, ByteBuffer byteBuffer, int offset) throws IOException {
        submitRead(handle, fd, byteBuffer, offset);
    }

    public int waitForCompletion() throws IOException {
        return waitForCompletion(handle);
    }

    public void submitWrite(FileDescriptor fd, ByteBuffer byteBuffer, int offset) throws IOException {
        submitWrite(handle, fd, byteBuffer, offset);
    }
}
