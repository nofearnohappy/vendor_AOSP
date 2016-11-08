LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
LOCAL_SRC_FILES:= LanunchPPPoe.c
LOCAL_CFLAGS := -Werror=format
LOCAL_MODULE:= launchpppoe
LOCAL_C_INCLUDES := system/core/include/netutils/ system/core/include/cutils/
LOCAL_SHARED_LIBRARIES := libcutils libnetutils                     

include $(BUILD_EXECUTABLE)
