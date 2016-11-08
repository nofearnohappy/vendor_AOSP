LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

ifeq ($(MTK_CAM_HDR_SUPPORT), yes)

ifeq ($(TARGET_BOARD_PLATFORM), $(filter $(TARGET_BOARD_PLATFORM), mt6755))

LOCAL_MODULE := libhdrproc

LOCAL_ADDITIONAL_DEPENDENCIES := $(LOCAL_PATH)/Android.mk

MTK_PATH_CAM := $(MTK_PATH_SOURCE)/hardware/mtkcam
MTK_PATH_CAM_LEGACY := $(MTK_PATH_SOURCE)/hardware/mtkcam/legacy
MTK_PATH_HDR_PLATFORM := ../$(TARGET_BOARD_PLATFORM)

LOCAL_SRC_FILES := \
	HDRProc.cpp \
	HDRAlgo.cpp \
	HDRUtils.cpp \
	$(MTK_PATH_HDR_PLATFORM)/Platform.cpp \
	utils/ImageBufferUtils.cpp \
	utils/Timer.cpp

LOCAL_C_INCLUDES := \
	$(MTK_PATH_CAM)/feature/include \
	$(MTK_PATH_SOURCE)/hardware/gralloc_extra/include

ifeq ($(TARGET_BOARD_PLATFORM), $(filter $(TARGET_BOARD_PLATFORM), mt6755))
LOCAL_C_INCLUDES += \
	$(MTK_PATH_CAM_LEGACY)/include \
	$(MTK_PATH_CAM_LEGACY)/include/mtkcam \
	$(MTK_PATH_CAM_LEGACY)/platform/$(TARGET_BOARD_PLATFORM) \
	$(MTK_PATH_CAM_LEGACY)/platform/$(TARGET_BOARD_PLATFORM)/include \
	$(MTK_PATH_CUSTOM_PLATFORM)/hal/inc \
	$(MTK_PATH_CUSTOM_PLATFORM)/hal/inc/isp_tuning
endif

LOCAL_SHARED_LIBRARIES := \
	libutils \
	libcutils \
	libcamalgo \
	libfeatureio \
	libcam_utils \
	libcam.iopipe \
	libcameracustom \
	libcam.hal3a.v3 \
	libcam.halsensor

LOCAL_MODULE_TAGS := optional

LOCAL_CFLAGS := -DLOG_TAG=\"hdrproc\"

LOCAL_CFLAGS += -DUSE_SYSTRACE

ifeq ($(MTK_PERFSERVICE_SUPPORT), yes)
LOCAL_CFLAGS += -DUSE_PERFSERVICE
LOCAL_C_INCLUDES += $(MTK_PATH_SOURCE)/hardware/perfservice
LOCAL_SHARED_LIBRARIES += libperfservicenative
endif

include $(BUILD_SHARED_LIBRARY)

endif # TARGET_BOARD_PLATFORM

endif # MTK_CAM_HDR_SUPPORT
