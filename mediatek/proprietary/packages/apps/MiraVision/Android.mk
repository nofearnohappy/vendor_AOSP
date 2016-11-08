ifeq ($(strip $(MTK_MIRAVISION_SETTING_SUPPORT)),yes)

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_JAVA_LIBRARIES := bouncycastle 
LOCAL_JAVA_LIBRARIES += mediatek-framework

LOCAL_STATIC_JAVA_LIBRARIES := android-support-v4

# for projects can't support 1080p video, so replaced with 720p resource
ifeq ($(MTK_GMO_RAM_OPTIMIZE),yes)
    LOCAL_ASSET_DIR := $(LOCAL_PATH)/assets_slim
endif

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_PACKAGE_NAME := MiraVision
LOCAL_CERTIFICATE := platform

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

include $(BUILD_PACKAGE)

# Use the folloing include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))

endif
