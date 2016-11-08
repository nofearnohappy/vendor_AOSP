LOCAL_PATH:= $(call my-dir)
#================================================
include $(CLEAR_VARS)

LOCAL_MODULE := org.simalliance.openmobileapi.xml
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/permissions
LOCAL_SRC_FILES := $(LOCAL_MODULE)

include $(BUILD_PREBUILT)
#================================================
include $(CLEAR_VARS)

LOCAL_MODULE:= org.simalliance.openmobileapi.jar
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_CLASS := JAVA_LIBRARIES
LOCAL_MODULE_PATH := $(TARGET_OUT_JAVA_LIBRARIES)
LOCAL_SRC_FILES := $(LOCAL_MODULE)

include $(BUILD_PREBUILT)

