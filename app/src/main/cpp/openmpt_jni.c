/*
 * Minimal JNI wrapper around libopenmpt for Android playback.
 *
 * Exposes the subset of libopenmpt needed by the Kotlin ModPlayer:
 *   open / close / readStereo / getTitle / getDuration
 *
 * All functions are thread-safe with respect to distinct module handles.
 * Caller must ensure a single handle is not used from multiple threads
 * concurrently (the Kotlin side serialises via its playback thread).
 */
#include <jni.h>
#include <stdlib.h>
#include <string.h>
#include <android/log.h>
#include "libopenmpt/libopenmpt.h"
#include "libopenmpt/libopenmpt_ext.h"

#define TAG "OpenMptJni"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,  TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static void log_func(const char *message, void *userdata) {
    (void)userdata;
    LOGI("openmpt: %s", message);
}

/*
 * Open a module from a byte array.
 * Returns a native pointer (as jlong) or 0 on failure.
 */
JNIEXPORT jlong JNICALL
Java_com_bigbangit_modfall_music_OpenMptJni_nativeOpen(
        JNIEnv *env, jclass clazz, jbyteArray data, jint length) {
    (void)clazz;
    jbyte *buf = (*env)->GetByteArrayElements(env, data, NULL);
    if (!buf) {
        LOGE("Failed to get byte array elements");
        return 0;
    }

    openmpt_module *mod = openmpt_module_create_from_memory2(
            buf, (size_t)length,
            log_func, NULL,   /* log */
            NULL, NULL,       /* error */
            NULL, NULL,       /* init control */
            NULL              /* initial control data */
    );

    (*env)->ReleaseByteArrayElements(env, data, buf, JNI_ABORT);

    if (!mod) {
        LOGW("openmpt_module_create_from_memory2 returned NULL");
        return 0;
    }

    /* Sensible defaults: stereo, reasonable interpolation */
    openmpt_module_set_render_param(mod,
            OPENMPT_MODULE_RENDER_INTERPOLATIONFILTER_LENGTH, 4);

    LOGI("Module opened successfully");
    return (jlong)(intptr_t)mod;
}

/*
 * Destroy a module previously returned by nativeOpen.
 */
JNIEXPORT void JNICALL
Java_com_bigbangit_modfall_music_OpenMptJni_nativeClose(
        JNIEnv *env, jclass clazz, jlong handle) {
    (void)env; (void)clazz;
    if (handle != 0) {
        openmpt_module_destroy((openmpt_module *)(intptr_t)handle);
    }
}

/*
 * Read interleaved stereo 16-bit PCM frames.
 * Returns the number of frames actually rendered (0 = end of module).
 */
JNIEXPORT jint JNICALL
Java_com_bigbangit_modfall_music_OpenMptJni_nativeReadStereo(
        JNIEnv *env, jclass clazz,
        jlong handle, jint sampleRate, jshortArray outBuf, jint frames) {
    (void)clazz;
    if (handle == 0) return 0;

    jshort *buf = (*env)->GetShortArrayElements(env, outBuf, NULL);
    if (!buf) return 0;

    size_t read = openmpt_module_read_interleaved_stereo(
            (openmpt_module *)(intptr_t)handle,
            sampleRate, (size_t)frames, buf);

    (*env)->ReleaseShortArrayElements(env, outBuf, buf, 0);
    return (jint)read;
}

/*
 * Get module title metadata, or NULL if unavailable.
 */
JNIEXPORT jstring JNICALL
Java_com_bigbangit_modfall_music_OpenMptJni_nativeGetTitle(
        JNIEnv *env, jclass clazz, jlong handle) {
    (void)clazz;
    if (handle == 0) return NULL;

    const char *title = openmpt_module_get_metadata(
            (openmpt_module *)(intptr_t)handle, "title");
    if (!title || title[0] == '\0') {
        openmpt_free_string(title);
        return NULL;
    }

    jstring result = (*env)->NewStringUTF(env, title);
    openmpt_free_string(title);
    return result;
}

/*
 * Get module duration in seconds.
 */
JNIEXPORT jdouble JNICALL
Java_com_bigbangit_modfall_music_OpenMptJni_nativeGetDuration(
        JNIEnv *env, jclass clazz, jlong handle) {
    (void)env; (void)clazz;
    if (handle == 0) return 0.0;
    return openmpt_module_get_duration_seconds(
            (openmpt_module *)(intptr_t)handle);
}
