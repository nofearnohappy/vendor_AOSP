# Copyright 2006 The Android Open Source Project
ifeq ($(MTK_EXTERNAL_DONGLE_SUPPORT),yes)

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
	rild.c


LOCAL_SHARED_LIBRARIES := \
	liblog \
	libcutils \
	libril_dongle \
	libdl

# temporary hack for broken vendor rils
LOCAL_WHOLE_STATIC_LIBRARIES := \
	librilutils_dongle_static

LOCAL_CFLAGS := -DRIL_SHLIB
#LOCAL_CFLAGS += -DANDROID_MULTI_SIM

ifeq ($(SIM_COUNT), 2)
    LOCAL_CFLAGS += -DANDROID_SIM_COUNT_2
endif

LOCAL_MODULE:= rild_dongle
LOCAL_MODULE_TAGS := optional

include $(BUILD_EXECUTABLE)
endif #(($(MTK_EXTERNAL_DONGLE_SUPPORT),yes)

ifeq ($(MTK_EXTERNAL_DONGLE_SUPPORT),yes)
# For radiooptions binary
# =======================
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
	radiooptions.c

LOCAL_SHARED_LIBRARIES := \
	liblog \
	libcutils \

LOCAL_CFLAGS := \

LOCAL_MODULE:= radiooptions_dongle
LOCAL_MODULE_TAGS := debug

include $(BUILD_EXECUTABLE)
endif #(($(MTK_EXTERNAL_DONGLE_SUPPORT),yes)
