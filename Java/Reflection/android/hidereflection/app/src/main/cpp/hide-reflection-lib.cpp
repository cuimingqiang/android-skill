//
// Created by cuimingqiang on 2021/9/21.
//
#include "art.h"
#include <jni.h>
#include <android/log.h>
#include <vector>
#include <string>
#include <stdlib.h>
#include <sys/system_properties.h>

#define TAG "hide-log"
#define LOG_D(...) __android_log_print(ANDROID_LOG_DEBUG,TAG ,__VA_ARGS__)


extern "C"
JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *vm, void *unused) {
    LOG_D("onLoad");

    return JNI_VERSION_1_6;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_hide_reflection_HideReflection_nativeInit(JNIEnv *env, jclass clazz,
                                                   jint targetSdkVersion) {
    return unseal(env,targetSdkVersion);
}