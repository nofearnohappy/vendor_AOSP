ifeq ($(MTK_NFC_SUPPORT), yes)

LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_PACKAGE_NAME := DeviceTestApp
LOCAL_CERTIFICATE := platform

LOCAL_JNI_SHARED_LIBRARIES := libdta_mt6605_jni libdta_dynamic_load_jni

LOCAL_PROGUARD_ENABLED := disabled

include $(BUILD_PACKAGE)

# Use the folloing include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))

endif
