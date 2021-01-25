#include <jni.h>

#include <stdio.h>
#include <stdlib.h>
#include <fcntl.h>
#include <string.h>
#include <unistd.h>
#include <assert.h>
#include <errno.h>
#include <inttypes.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/ioctl.h>
#include <bits/types/sigset_t.h>

// https://github.com/axboe/liburing/commit/df6b9a96f30349a78a353b5bd625491547411881
#define typeof(x) __typeof__(x)
#include <liburing.h>
#undef typeof

#include "jni_exceptions.h"


int jniGetFDFromFileDescriptor(JNIEnv *env, jobject fileDescriptor)
{
    // TODO: load classes on lib load
    jclass fdclass = (*env)->FindClass(env, "java/io/FileDescriptor");
    jfieldID desc = (*env)->GetFieldID(env, fdclass, "fd", "I");
    return (*env)->GetIntField(env, fileDescriptor, desc);
}

/// -----------------

JNIEXPORT jlong JNICALL Java_com_marcusyates_iouring_IoUring_ioUringCreate(
    JNIEnv *env, jobject obj, jint entries)
{
    struct io_uring *ring = malloc(sizeof(struct io_uring));
    unsigned flags = 0;

    int retc = io_uring_queue_init((unsigned) entries, ring, flags);
    if (retc < 0)
    {
        free(ring);
        jniThrowIOException(env, retc);
        return -1;
    }

	return (jlong) ring;
}


JNIEXPORT void JNICALL Java_com_marcusyates_iouring_IoUring_ioUringDestroy(
    JNIEnv *env, jobject obj, jlong addr)
{
    struct io_uring* ring = (struct io_uring*) addr;
    io_uring_queue_exit(ring);
    free(ring);
}

JNIEXPORT void JNICALL Java_com_marcusyates_iouring_IoUring_submitRead(
    JNIEnv *env, jobject obj, jlong addr, jobject fileDescriptor, jobject buf, jint offset)
{
    struct io_uring* ring = (struct io_uring*) addr;

    int fd = jniGetFDFromFileDescriptor(env, fileDescriptor);
    char* buffer = (char*)(*env)->GetDirectBufferAddress(env, buf);
    size_t len = (*env)->GetDirectBufferCapacity(env, buf);

    struct io_uring_sqe* sqe = io_uring_get_sqe(ring);
    struct iovec* data = malloc(sizeof(struct iovec));
    data->iov_base = buffer;
    data->iov_len = len;

    io_uring_prep_readv(sqe, fd, data, 1, offset);
    io_uring_sqe_set_data(sqe, data);
    io_uring_submit(ring);
}

JNIEXPORT void JNICALL Java_com_marcusyates_iouring_IoUring_submitWrite(
    JNIEnv *env, jobject obj, jlong addr, jobject fileDescriptor, jobject buf, jint offset)
{
    struct io_uring* ring = (struct io_uring*) addr;

    int fd = jniGetFDFromFileDescriptor(env, fileDescriptor);
    char* buffer = (char*)(*env)->GetDirectBufferAddress(env, buf);
    size_t len = (*env)->GetDirectBufferCapacity(env, buf);

    struct io_uring_sqe* sqe = io_uring_get_sqe(ring);
    struct iovec* data = malloc(sizeof(struct iovec));
    data->iov_base = buffer;
    data->iov_len = len;

    io_uring_prep_writev(sqe, fd, data, 1, offset);
    io_uring_sqe_set_data(sqe, data);
    io_uring_submit(ring);
}

JNIEXPORT jint JNICALL Java_com_marcusyates_iouring_IoUring_waitForCompletion(
    JNIEnv *env, jobject obj, jlong addr)
{
    struct io_uring* ring = (struct io_uring*) addr;

    struct io_uring_cqe *cqe;
    int retc = io_uring_wait_cqe(ring, &cqe);
    if (retc < 0)
    {
        jniThrowIOException(env, retc);
        return -1;
    }
    if (cqe->res < 0) {
        jniThrowIOException(env, cqe->res);
        return -1;
    }

    retc = cqe->res;

    struct iovec* data = io_uring_cqe_get_data(cqe);
    free(data);
    io_uring_cqe_seen(ring, cqe);

    return retc;
}

