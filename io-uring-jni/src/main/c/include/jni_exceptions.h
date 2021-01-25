#ifndef IO_URING_JNI_EXCEPTIONS_H_
#define IO_URING_JNI_EXCEPTIONS_H_

int jniThrowException(JNIEnv* env, const char* className, const char* msg);
int jniThrowIOException(JNIEnv* env, int errnum);
int jniThrowRuntimeException(JNIEnv* env, const char* msg);

#endif