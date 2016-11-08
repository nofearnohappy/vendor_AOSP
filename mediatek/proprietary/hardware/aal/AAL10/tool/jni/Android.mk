LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
    aaltool_jni.cpp

LOCAL_C_INCLUDES := \
    $(JNI_H_INCLUDE) \
    $(TOP)/$(MTK_PATH_SOURCE)/hardware/aal/include

LOCAL_SHARED_LIBRARIES := \
    libandroid_runtime \
    libutils \
    libbinder \
    liblog \
    libaal \
    libdpframework

LOCAL_PRELINK_MODULE := false

LOCAL_MODULE := libaaltool_jni

LOCAL_MODULE_TAGS := tests

include $(BUILD_SHARED_LIBRARY)

