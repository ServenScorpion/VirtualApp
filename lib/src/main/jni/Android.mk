LOCAL_PATH := $(call my-dir)
MAIN_LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

ifeq ($(TARGET_ARCH_ABI),arm64-v8a)
LOCAL_MODULE := v++_64
LOCAL_CFLAGS := -DCORE_SO_NAME=\"libv++_64.so\"
else
LOCAL_MODULE := v++
LOCAL_CFLAGS := -DCORE_SO_NAME=\"libv++.so\"
endif

LOCAL_CFLAGS += -Wno-error=format-security -fpermissive -O2
LOCAL_CFLAGS += -DLOG_TAG=\"VA++\"
LOCAL_CFLAGS += -fno-rtti -fno-exceptions
LOCAL_CPPFLAGS += -std=c++11

LOCAL_C_INCLUDES += $(MAIN_LOCAL_PATH)
LOCAL_C_INCLUDES += $(MAIN_LOCAL_PATH)/Foundation
LOCAL_C_INCLUDES += $(MAIN_LOCAL_PATH)/Jni

LOCAL_SRC_FILES := Jni/VAJni.cpp \
				   Jni/Helper.cpp \
				   Foundation/syscall/BinarySyscallFinder.cpp \
				   Foundation/fake_dlfcn.cpp \
				   Foundation/canonicalize_md.c \
				   Foundation/MapsRedirector.cpp \
				   Foundation/IORelocator.cpp \
				   Foundation/VMHook.cpp \
				   Foundation/Symbol.cpp \
				   Foundation/SandboxFs.cpp \
				   Substrate/hde64.c \
                   Substrate/SubstrateDebug.cpp \
                   Substrate/SubstrateHook.cpp \
                   Substrate/SubstratePosixMemory.cpp \
                   Substrate/And64InlineHook.cpp \
                   transparentED/ff_Recognizer.cpp \
                   transparentED/EncryptFile.cpp \
                   transparentED/originalInterface.cpp \
                   transparentED/ctr/caesar_cipher.cpp \
                   transparentED/ctr/crypter.cpp \
                   transparentED/ctr/ctr.cpp \
                   transparentED/ctr/rng.cpp \
                   transparentED/ctr/SpookyV2.cpp \
                   transparentED/ctr/util.cpp \
                   transparentED/ctr/sm4.c \
                   transparentED/ctr/sm4_cipher.cpp \
                   transparentED/virtualFileSystem.cpp \
                   transparentED/fileCoder1.cpp \
                   transparentED/TemplateFile.cpp \
                   transparentED/IgnoreFile.cpp \
                   transparentED/encryptInfoMgr.cpp \
                   safekey/safekey_jni.cpp \
                   utils/zJNIEnv.cpp \
                   utils/utils.cpp \
                   utils/md5.c \
                   utils/zMd5.cpp \
                   utils/controllerManagerNative.cpp

ifeq ($(TARGET_ARCH_ABI),arm64-v8a)
LOCAL_SRC_FILES := Jni/VAJni.cpp \
				   Jni/Helper.cpp \
				   Foundation/syscall/BinarySyscallFinder.cpp \
				   Foundation/fake_dlfcn.cpp \
				   Foundation/canonicalize_md.c \
				   Foundation/MapsRedirector.cpp \
				   Foundation/IORelocator64.cpp \
				   Foundation/VMHook.cpp \
				   Foundation/Symbol.cpp \
				   Foundation/SandboxFs.cpp \
				   Substrate/hde64.c \
                   Substrate/SubstrateDebug.cpp \
                   Substrate/SubstrateHook.cpp \
                   Substrate/SubstratePosixMemory.cpp \
                   Substrate/And64InlineHook.cpp \
                   transparentED/ff_Recognizer.cpp \
                   transparentED/EncryptFile.cpp \
                   transparentED/originalInterface.cpp \
                   transparentED/ctr/caesar_cipher.cpp \
                   transparentED/ctr/crypter.cpp \
                   transparentED/ctr/ctr.cpp \
                   transparentED/ctr/rng.cpp \
                   transparentED/ctr/SpookyV2.cpp \
                   transparentED/ctr/util.cpp \
                   transparentED/ctr/sm4.c \
                   transparentED/ctr/sm4_cipher.cpp \
                   transparentED/virtualFileSystem.cpp \
                   transparentED/fileCoder1.cpp \
                   transparentED/TemplateFile.cpp \
                   transparentED/IgnoreFile.cpp \
                   transparentED/encryptInfoMgr.cpp \
                   safekey/safekey_jni.cpp \
                   utils/zJNIEnv.cpp \
                   utils/utils.cpp \
                   utils/md5.c \
                   utils/zMd5.cpp \
                   utils/controllerManagerNative.cpp
else
LOCAL_SRC_FILES := Jni/VAJni.cpp \
				   Jni/Helper.cpp \
				   Foundation/syscall/BinarySyscallFinder.cpp \
				   Foundation/fake_dlfcn.cpp \
				   Foundation/canonicalize_md.c \
				   Foundation/MapsRedirector.cpp \
				   Foundation/IORelocator.cpp \
				   Foundation/VMHook.cpp \
				   Foundation/Symbol.cpp \
				   Foundation/SandboxFs.cpp \
				   Substrate/hde64.c \
                   Substrate/SubstrateDebug.cpp \
                   Substrate/SubstrateHook.cpp \
                   Substrate/SubstratePosixMemory.cpp \
                   Substrate/And64InlineHook.cpp \
                   transparentED/ff_Recognizer.cpp \
                   transparentED/EncryptFile.cpp \
                   transparentED/originalInterface.cpp \
                   transparentED/ctr/caesar_cipher.cpp \
                   transparentED/ctr/crypter.cpp \
                   transparentED/ctr/ctr.cpp \
                   transparentED/ctr/rng.cpp \
                   transparentED/ctr/SpookyV2.cpp \
                   transparentED/ctr/util.cpp \
                   transparentED/ctr/sm4.c \
                   transparentED/ctr/sm4_cipher.cpp \
                   transparentED/virtualFileSystem.cpp \
                   transparentED/fileCoder1.cpp \
                   transparentED/TemplateFile.cpp \
                   transparentED/IgnoreFile.cpp \
                   transparentED/encryptInfoMgr.cpp \
                   safekey/safekey_jni.cpp \
                   utils/zJNIEnv.cpp \
                   utils/utils.cpp \
                   utils/md5.c \
                   utils/zMd5.cpp \
                   utils/controllerManagerNative.cpp
endif
LOCAL_LDLIBS := -llog -latomic

include $(BUILD_SHARED_LIBRARY)