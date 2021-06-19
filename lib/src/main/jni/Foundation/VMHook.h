//
// VirtualApp Native Project
//

#ifndef FOUNDATION_VM_HOOK
#define FOUNDATION_VM_HOOK


#include <jni.h>
#include <dlfcn.h>
#include <stddef.h>
#include <fcntl.h>
#include <sys/system_properties.h>
#include "Jni/Helper.h"


enum METHODS {
    OPEN_DEX = 0,
    CAMERA_SETUP,
    AUDIO_NATIVE_CHECK_PERMISSION,
    MEDIA_RECORDER_SETUP,
    AUDIO_RECORD_SETUP,
    CAMERA_STARTPREVIEW,
    CAMERA_TAKEPICTURE,
    AUDIO_RECORD_START,
    MEDIA_RECORDER_PREPARE
};

void bypassHiddenAPIEnforcementPolicy(jint apiLevel, jint previewApiLevel);

void hookAndroidVM(JNIEnv *env, jobjectArray javaMethods,
                   jstring packageName, jboolean isArt, jint apiLevel, jint cameraMethodType,
                   jint audioRecordMethodType);


#endif //NDK_HOOK_NATIVE_H
