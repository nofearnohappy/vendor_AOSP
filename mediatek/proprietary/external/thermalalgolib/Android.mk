#
# libthermalalgo
#


LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE:= libthermalalgo
LOCAL_MODULE_CLASS := SHARED_LIBRARIES
LOCAL_SRC_FILES_arm := libthermalalgo.so

#bobule workaround pdk build error, needing review
LOCAL_MULTILIB := 32
LOCAL_MODULE_SUFFIX := .so

include $(BUILD_PREBUILT)


include $(CLEAR_VARS)
LOCAL_SRC_FILES:= ta_main.cpp
LOCAL_CFLAGS += $(MTK_CDEFS)
LOCAL_SHARED_LIBRARIES := libcutils libutils libdl
LOCAL_MODULE:= thermalloadalgod
#bobule workaround pdk build error, needing review
LOCAL_MULTILIB := 32
LOCAL_PRELINK_MODULE := false
include $(BUILD_EXECUTABLE)