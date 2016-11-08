
ifeq ($(MTK_AAL_SUPPORT),yes)
LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
	tuning.cpp 

LOCAL_SHARED_LIBRARIES := \
    libutils \
    libcutils \
    libbinder \
    liblog \
    libaal \

LC_MTK_PLATFORM := $(shell echo $(MTK_PLATFORM) | tr A-Z a-z )

LOCAL_C_INCLUDES := \
    $(TOP)/$(MTK_PATH_PLATFORM)/hardware/aal/inc \
    $(TOP)/$(MTK_PATH_SOURCE)/platform/$(LC_MTK_PLATFORM)/kernel/drivers/dispsys \
    $(TOP)/$(MTK_PATH_SOURCE)/hardware/dpframework/inc


LOCAL_MODULE_TAGS := tests

LOCAL_MODULE:= aal-tune

include $(BUILD_EXECUTABLE)
endif
