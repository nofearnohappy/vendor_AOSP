LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES:= mmc.c
LOCAL_MODULE := mmc_ffu
LOCAL_SHARED_LIBRARIES := libcutils libc
LOCAL_C_INCLUDES += \
    bionic/libc/kernel/uapi/linux/mmc \
    device/mediatek/common/kernel-headers/linux/mmc
include $(BUILD_EXECUTABLE)
