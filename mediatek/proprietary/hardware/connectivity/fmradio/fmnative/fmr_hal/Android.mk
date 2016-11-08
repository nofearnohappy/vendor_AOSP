ifeq ($(MTK_FM_SUPPORT), yes)

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

ifeq ($(findstring MT6625_FM,$(MTK_FM_CHIP)),MT6625_FM)
LOCAL_CFLAGS+= \
    -DMT6627_FM
endif

LOCAL_MODULE := radio.fm.$(TARGET_BOARD_PLATFORM)
LOCAL_MODULE_RELATIVE_PATH := hw
LOCAL_SRC_FILES := fmr_core.c \
	fmr_err.c \
	common.c \
	radio_hw_hal.c

LOCAL_SHARED_LIBRARIES := liblog libdl libmedia libcutils libradio_metadata
LOCAL_MODULE_TAGS := optional
LOCAL_32_BIT_ONLY := true

LOCAL_PRELINK_MODULE := false

include $(BUILD_SHARED_LIBRARY)

endif
