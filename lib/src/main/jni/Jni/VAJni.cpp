
// VirtualApp Native Project
//
#include <Foundation/IORelocator.h>
#include <Foundation/Log.h>
#include <sys/ptrace.h>
#include <unistd.h>
#include <sys/wait.h>
#include "VAJni.h"
#include<sys/prctl.h>

#include <utils/controllerManagerNative.h>
#include <utils/zJNIEnv.h>
#include <utils/utils.h>

static void jni_nativeLaunchEngine(JNIEnv *env, jclass clazz, jobjectArray javaMethods,
                                   jstring packageName,
                                   jboolean isArt, jint apiLevel, jint cameraMethodType,
                                   jint audioRecordMethodType) {
    hookAndroidVM(env, javaMethods, packageName, isArt, apiLevel, cameraMethodType,
                  audioRecordMethodType);
}


static void
jni_nativeEnableIORedirect(JNIEnv *env, jclass, jstring soPath, jstring soPath64,
                           jstring nativePath, jint apiLevel,
                           jint preview_api_level) {
    ScopeUtfString so_path(soPath);
    ScopeUtfString so_path_64(soPath64);
    ScopeUtfString native_path(nativePath);
    IOUniformer::startUniformer(so_path.c_str(), so_path_64.c_str(), native_path.c_str(), apiLevel,
                                preview_api_level);
}

static void jni_nativeIOWhitelist(JNIEnv *env, jclass jclazz, jstring _path) {
    ScopeUtfString path(_path);
    IOUniformer::whitelist(path.c_str());
}

static void jni_nativeIOForbid(JNIEnv *env, jclass jclazz, jstring _path) {
    ScopeUtfString path(_path);
    IOUniformer::forbid(path.c_str());
}

static void jni_nativeIOReadOnly(JNIEnv *env, jclass jclazz, jstring _path) {
    ScopeUtfString path(_path);
    IOUniformer::readOnly(path.c_str());
}


static void jni_nativeIORedirect(JNIEnv *env, jclass jclazz, jstring origPath, jstring newPath) {
    ScopeUtfString orig_path(origPath);
    ScopeUtfString new_path(newPath);
    IOUniformer::relocate(orig_path.c_str(), new_path.c_str());

}

static jstring jni_nativeGetRedirectedPath(JNIEnv *env, jclass jclazz, jstring origPath) {
    ScopeUtfString orig_path(origPath);
    char buffer[PATH_MAX];
    const char *redirected_path = IOUniformer::query(orig_path.c_str(), buffer, sizeof(buffer));
    if (redirected_path != NULL) {
        return env->NewStringUTF(redirected_path);
    }
    return NULL;
}

static jstring jni_nativeReverseRedirectedPath(JNIEnv *env, jclass jclazz, jstring redirectedPath) {
    ScopeUtfString redirected_path(redirectedPath);
    char buffer[PATH_MAX];
    const char *orig_path = IOUniformer::reverse(redirected_path.c_str(), buffer, sizeof(buffer));
    return env->NewStringUTF(orig_path);
}

static void jni_bypassHiddenAPIEnforcementPolicy(JNIEnv *env, jclass jclazz, jint apiLevel, jint previewApiLevel) {
    bypassHiddenAPIEnforcementPolicy(apiLevel, previewApiLevel);
}

static jboolean jni_nativeCloseAllSocket(JNIEnv *env, jclass jclazz){
    return (jboolean)closeAllSockets();
}
static void jni_nativeChangeDecryptState(JNIEnv *env,jclass jclazz,jboolean state){
    changeDecryptState(state,0);
}
static jboolean jni_nativeGetDecryptState(JNIEnv *env,jclass jclazz){
    return getDecryptState();
}

static void jni_nativeAddEncryptPkgName(JNIEnv *env,jclass jclazz, jstring name) {
    ScopeUtfString packageName(name);
    addEncryptPkgName(packageName.c_str());
}

static void jni_nativeDelEncryptPkgName(JNIEnv *env,jclass jclazz, jstring name) {
    ScopeUtfString packageName(name);
    delEncryptPkgName(packageName.c_str());
}
static jboolean jni_nativeConfigEncryptPkgName(JNIEnv *env, jclass jclazz, jobjectArray pkgName) {
    jboolean  ret = 1;

    if (pkgName == nullptr) {
        return ret;
    }
    int count = env->GetArrayLength(pkgName);
    char const** namesUTF = (char const**)malloc(sizeof(char*) * count);
    for(int i = 0 ; i < count; i++) {
        namesUTF[i] = env->GetStringUTFChars((jstring)env->GetObjectArrayElement(pkgName, i), NULL);
    }

    ret = static_cast<jboolean>(configSafePkgName(namesUTF, count));

    for (int i = 0; i < count; i++) {
        env->ReleaseStringUTFChars((jstring) env->GetObjectArrayElement(pkgName, i), namesUTF[i]);
    }
    free(namesUTF);
    return ret;
}

static jboolean jni_nativeConfigNetStrategy(JNIEnv *env, jclass jclazz, jobjectArray netStrategy,jint type) {
    jboolean ret = 1;
    if(netStrategy == nullptr) {
        return ret;
    }
    int count = env->GetArrayLength(netStrategy);
    char const** strategyUTF = (char const**)malloc(sizeof(char*) * count);
    for(int i = 0; i < count; i++) {
        strategyUTF[i] = env->GetStringUTFChars((jstring)env->GetObjectArrayElement(netStrategy, i), NULL);
    }
    ret = static_cast<jboolean>(configNetStrategy(strategyUTF,type,count));
    for(int i = 0; i < count; i++) {
        env->ReleaseStringUTFChars((jstring) env->GetObjectArrayElement(netStrategy, i), strategyUTF[i]);
    }
    free(strategyUTF);
    return ret;
}

static void jni_nativeConfigNetworkState(JNIEnv *env, jclass jclazz, jboolean netonOroff) {
    configNetworkState(netonOroff);
}

static void jni_nativeConfigWhiteOrBlack(JNIEnv *env, jclass jclazz, jboolean isWhiteOrBlack) {
    configWhiteOrBlack(isWhiteOrBlack);
}

static void jni_nativeConfigDomainToIp(JNIEnv *env, jclass jclazz) {
    cofingDomainToIp();
}

jclass nativeEngineClass;
JavaVM *vm;

jclass vskmClass;
jclass vsckmsClass;

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *_vm, void *) {
    vm = _vm;
    JNIEnv *env;
    _vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6);
    nativeEngineClass = (jclass) env->NewGlobalRef(env->FindClass(JNI_CLASS_NAME));
    static JNINativeMethod methods[] = {
            {"nativeLaunchEngine",                     "([Ljava/lang/Object;Ljava/lang/String;ZIII)V",                (void *) jni_nativeLaunchEngine},
            {"nativeReverseRedirectedPath",            "(Ljava/lang/String;)Ljava/lang/String;",                      (void *) jni_nativeReverseRedirectedPath},
            {"nativeGetRedirectedPath",                "(Ljava/lang/String;)Ljava/lang/String;",                      (void *) jni_nativeGetRedirectedPath},
            {"nativeIORedirect",                       "(Ljava/lang/String;Ljava/lang/String;)V",                     (void *) jni_nativeIORedirect},
            {"nativeIOWhitelist",                      "(Ljava/lang/String;)V",                                       (void *) jni_nativeIOWhitelist},
            {"nativeIOForbid",                         "(Ljava/lang/String;)V",                                       (void *) jni_nativeIOForbid},
            {"nativeIOReadOnly",                       "(Ljava/lang/String;)V",                                       (void *) jni_nativeIOReadOnly},
            {"nativeEnableIORedirect",                 "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;II)V", (void *) jni_nativeEnableIORedirect},
            {"nativeBypassHiddenAPIEnforcementPolicy", "(II)V",                                                         (void *) jni_bypassHiddenAPIEnforcementPolicy},
            {"nativeGetDecryptState",                  "()Z",                                                         (void *) jni_nativeGetDecryptState},
            {"nativeChangeDecryptState",               "(Z)V",                                                        (void *) jni_nativeChangeDecryptState},
            {"nativeCloseAllSocket",                   "()Z",                                                         (void *) jni_nativeCloseAllSocket},
            {"nativeConfigEncryptPkgName",             "([Ljava/lang/String;)Z",                                      (void *)jni_nativeConfigEncryptPkgName},
            {"nativeAddEncryptPkgName",                "(Ljava/lang/String;)V",                                       (void *) jni_nativeAddEncryptPkgName},
            {"nativeDelEncryptPkgName",                "(Ljava/lang/String;)V",                                       (void *) jni_nativeDelEncryptPkgName},
            {"nativeConfigNetStrategy",                 "([Ljava/lang/String;I)Z",                                    (void *) jni_nativeConfigNetStrategy},
            {"nativeConfigNetworkState",                "(Z)V",                                                       (void *) jni_nativeConfigNetworkState},
            {"nativeConfigWhiteOrBlack",                "(Z)V",                                                       (void *) jni_nativeConfigWhiteOrBlack},
            {"nativeConfigDomainToIp",                  "()V",                                                         (void *) jni_nativeConfigDomainToIp},
    };

    if (env->RegisterNatives(nativeEngineClass, methods, 19) < 0) {
        return JNI_ERR;
    }

    jclass VSKMClass = env->FindClass("com/xdja/zs/VSafekeyManager");
    vskmClass = (jclass) env->NewGlobalRef(VSKMClass);
    env->DeleteLocalRef(VSKMClass);

    jclass VSCKMSClass = env->FindClass("com/xdja/zs/VSafekeyCkmsManager");
    vsckmsClass = (jclass)env->NewGlobalRef(VSCKMSClass);
    env->DeleteLocalRef(VSCKMSClass);

    zJNIEnv::initial(vm);
    controllerManagerNative::initial();

    return JNI_VERSION_1_6;
}

JNIEnv *getEnv() {
    JNIEnv *env;
    vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6);
    return env;
}

JNIEnv *ensureEnvCreated() {
    JNIEnv *env = getEnv();
    if (env == NULL) {
        vm->AttachCurrentThread(&env, NULL);
    }
    return env;
}

extern "C" __attribute__((constructor)) void _init(void) {
    IOUniformer::init_env_before_all();
}