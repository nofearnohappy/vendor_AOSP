LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_PACKAGE_NAME := VoiceExtension
#LOCAL_CERTIFICATE := media
LOCAL_CERTIFICATE := platform

LOCAL_JNI_SHARED_LIBRARIES := libvie_jni
LOCAL_JAVA_LIBRARIES += mediatek-framework
LOCAL_MULTILIB := 32

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

LOCAL_EMMA_COVERAGE_FILTER := +com.mediatek.voiceextension.*\
                              +com.mediatek.voiceextension.command.*\
                              +com.mediatek.voiceextension.common.*\
                              +com.mediatek.voiceextension.swip.*

#EMMA_INSTRUMENT := true

include $(BUILD_PACKAGE)

# Use the following include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))
