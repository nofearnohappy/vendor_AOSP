# Copyright 2006 The Android Open Source Project

ifeq ($(MTK_EXTERNAL_DONGLE_SUPPORT),yes)
LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
    ril.cpp \
    ril_event.cpp

LOCAL_SHARED_LIBRARIES := \
    liblog \
    libutils \
    libbinder \
    libcutils \
    libhardware_legacy \
    librilutils

#LOCAL_CFLAGS := -DANDROID_MULTI_SIM -DDSDA_RILD1

ifeq ($(SIM_COUNT), 2)
    LOCAL_CFLAGS += -DANDROID_SIM_COUNT_2
endif

LOCAL_MODULE:= libril_dongle

include $(BUILD_SHARED_LIBRARY)


# For RdoServD which needs a static library
# =========================================
ifneq ($(ANDROID_BIONIC_TRANSITION),)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
    ril.cpp

LOCAL_STATIC_LIBRARIES := \
    libutils_static \
    libcutils \
    librilutils_dongle_static

LOCAL_CFLAGS :=

LOCAL_MODULE:= libril_dongle_static

include $(BUILD_STATIC_LIBRARY)
endif # ANDROID_BIONIC_TRANSITION
endif #(($(MTK_EXTERNAL_DONGLE_SUPPORT),yes)
