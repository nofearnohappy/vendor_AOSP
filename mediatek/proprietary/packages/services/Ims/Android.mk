LOCAL_PATH:= $(call my-dir)

# Build the Ims OEM implementation including imsservice, imsadapter, imsriladapter.
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_JAVA_LIBRARIES += telephony-common
LOCAL_JAVA_LIBRARIES += mediatek-framework
LOCAL_JAVA_LIBRARIES += ims-common
LOCAL_STATIC_JAVA_LIBRARIES += wfo-common

# Use SimServs.jar for VoLTE MMTelSS Package
LOCAL_STATIC_JAVA_LIBRARIES += Simservs

LOCAL_PACKAGE_NAME := ImsService
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true

LOCAL_PROGUARD_ENABLED := disabled
LOCAL_PROGUARD_FLAGS := $(proguard.flags)

include $(BUILD_PACKAGE)
