LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE:= libfgauge
LOCAL_MODULE_CLASS := SHARED_LIBRARIES
LOCAL_SRC_FILES_arm := libfgauge.so

#bobule workaround pdk build error, needing review
LOCAL_MULTILIB := 32
LOCAL_MODULE_SUFFIX := .so

include $(BUILD_PREBUILT)


