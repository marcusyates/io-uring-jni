#include <string.h>
#include <jni.h>
#include "jni_exceptions.h"

static void getExceptionSummary(JNIEnv* env, jthrowable excep, char* buf, size_t bufLen)
{
    if (NULL == excep)
    {
        return;
    }

    jclass clazz = (*env)->GetObjectClass(env, excep);
    jclass jlc = (*env)->GetObjectClass(env, clazz);
    jmethodID getNameMethod = (*env)->GetMethodID(env, jlc, "getName", "()Ljava/lang/String;");
    jstring className = (*env)->CallObjectMethod(env, clazz, getNameMethod);

    const char* nameStr = (*env)->GetStringUTFChars(env, className, NULL);
    if (NULL == nameStr)
    {
        snprintf(buf, bufLen, "%s", "Out of memory creating summary");
        (*env)->ExceptionClear(env);
        return;
    }

    jmethodID getThrowableMessage = (*env)->GetMethodID(env, clazz, "getMessage", "()Ljava/lang/String;");
    jstring message = (*env)->CallObjectMethod(env, excep, getThrowableMessage);

    if (NULL == message)
    {
        strncpy(buf, nameStr, bufLen);
        buf[bufLen - 1] = '\0';
    }
    else
    {
        const char* messageStr = (*env)->GetStringUTFChars(env, message, NULL);
        snprintf(buf, bufLen, "%s: %s", nameStr, messageStr);
        if (NULL == messageStr)
        {
            (*env)->ExceptionClear(env);
        }
        else
        {
            (*env)->ReleaseStringUTFChars(env, message, messageStr);
        }
    }

    (*env)->ReleaseStringUTFChars(env, className, nameStr);
}

int jniThrowException(JNIEnv* env, const char* className, const char* msg)
{
    jclass exceptionClass;
    if ((*env)->ExceptionCheck(env))
    {
        char buf[256];
        jthrowable excep = (*env)->ExceptionOccurred(env);
        (*env)->ExceptionClear(env);
        getExceptionSummary(env, excep, buf, sizeof(buf));
    }

    exceptionClass = (*env)->FindClass(env, className);
    if (exceptionClass == NULL)
    {
        /* ClassNotFoundException now pending */
        return -1;
    }

    if ((*env)->ThrowNew(env, exceptionClass, msg) != JNI_OK)
    {
        /* an exception, most likely OOM, will now be pending */
        return -1;
    }

    return 0;
}

int jniThrowIOException(JNIEnv* env, int errnum)
{
    return jniThrowException(env, "java/io/IOException", strerror(errnum));
}

int jniThrowRuntimeException(JNIEnv* env, const char* msg)
{
    return jniThrowException(env, "java/lang/RuntimeException", msg);
}
