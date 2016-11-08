LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

ifeq ($(MTK_CAM_HDR_SUPPORT), yes)

ifeq ($(TARGET_BOARD_PLATFORM), $(filter $(TARGET_BOARD_PLATFORM), mt6735m mt6580))

LOCAL_MODULE := libhdrproc

LOCAL_ADDITIONAL_DEPENDENCIES := $(LOCAL_PATH)/Android.mk

MTK_PATH_CAM := $(MTK_PATH_SOURCE)/hardware/mtkcam
MTK_PATH_CAM_LEGACY := $(MTK_PATH_SOURCE)/hardware/mtkcam/legacy

LOCAL_SRC_FILES := \
	HdrProc.cpp \
	Hdr.cpp \
	HDRAlgo.cpp \
	HDRUtils.cpp \
	utils/ImageBufferUtils.cpp

LOCAL_C_INCLUDES := \
	$(TOP)/system/media/camera/include \
	$(MTK_PATH_CAM)/feature/include \
	$(MTK_PATH_SOURCE)/hardware/gralloc_extra/include

ifeq ($(TARGET_BOARD_PLATFORM), $(filter $(TARGET_BOARD_PLATFORM), mt6735m mt6580))
LOCAL_C_INCLUDES += \
	$(MTK_PATH_CAM_LEGACY)/include \
	$(MTK_PATH_CAM_LEGACY)/include/mtkcam \
	$(MTK_PATH_CAM_LEGACY)/platform/$(TARGET_BOARD_PLATFORM)/include \
	$(MTK_PATH_CAM_LEGACY)/platform/$(TARGET_BOARD_PLATFORM)/v1/hal/adapter/inc
endif

ifeq ($(TARGET_BOARD_PLATFORM), mt6735m)
LOCAL_C_INCLUDES += \
	$(MTK_PATH_CUSTOM_PLATFORM)/hal/D2/inc
endif

ifeq ($(TARGET_BOARD_PLATFORM), mt6580)
LOCAL_C_INCLUDES += \
	$(MTK_PATH_CUSTOM_PLATFORM)/hal/inc
endif

LOCAL_SHARED_LIBRARIES := \
	libutils \
	libcutils \
	libcamalgo \
	libfeatureio \
	libcam_utils \
	libcam.iopipe \
	libcameracustom \
	libcamdrv \
	libm4u \
	libcam.camshot \
	libmedia \

LOCAL_MODULE_TAGS := optional

include $(BUILD_SHARED_LIBRARY)

endif # TARGET_BOARD_PLATFORM

endif # MTK_CAM_HDR_SUPPORT
