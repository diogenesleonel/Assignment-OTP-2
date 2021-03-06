#include <string.h>
#include <jni.h>
#include <stdlib.h>
#include "otp.h"

#define LOGD(...) \
  ((void)__android_log_print(ANDROID_LOG_DEBUG, "EncryptionLib-JNI", __VA_ARGS__))


extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_datablink_diogenes_otp_MainActivity_generateOtp(JNIEnv *env, jobject thiz, jstring key) {

    const char *constKeyLocal = env->GetStringUTFChars(key, 0);
    char* keyLocal = (char*)constKeyLocal;

    int keysize = 32;

    char* digest= (char*)malloc(20);
    memset(digest, 0, 20);

    generateotp(keyLocal,keysize,digest);

    jbyte*digestLocal = (jbyte*)calloc(sizeof(jbyte), 20);
    for(int i=0; i <= 20; i++){
        digestLocal[i] =  (jbyte) digest[i];
    }

    jbyteArray digestByteArray = env->NewByteArray(20);
    env->SetByteArrayRegion(digestByteArray, 0, 20 , digestLocal);

    free(digestLocal);
    free(digest);
    free(keyLocal);

    return digestByteArray;
}


extern "C" JNIEXPORT  jint JNICALL
Java_com_datablink_diogenes_otp_MainActivity_generateOtpDigits(JNIEnv *env, jobject instance,
                                                               jstring key) {

    const char *constKeyLocal = env->GetStringUTFChars(key, 0);
    char* keyLocal = (char*)constKeyLocal;

    int keysize = 32;

    char* digest= (char*)malloc(20);
    memset(digest, 0, 20);

    int digits = generateOtpDigits(keyLocal,keysize, digest);

    free(digest);
    free(keyLocal);


    return digits;

}

