LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
LOCAL_SRC_FILES = \
    mtk_ion_test.c  \

LOCAL_C_INCLUDES += \
    $(TOP)/system/core/include \
    $(TOP)/vendor/mediatek/proprietary/external/libion_mtk/include \
    $(TOP)/vendor/mediatek/proprietary/external/include \

LOCAL_MODULE_TAGS := eng
LOCAL_MODULE := mtk_ion_test

LOCAL_MULTILIB := both

LOCAL_SHARED_LIBRARIES := libcutils libc libion libion_mtk
include $(BUILD_NATIVE_TEST)
