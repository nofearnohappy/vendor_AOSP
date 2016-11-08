LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := hdrproc_test

LOCAL_ADDITIONAL_DEPENDENCIES := $(LOCAL_PATH)/Android.mk

MTK_PATH_CAM := $(MTK_PATH_SOURCE)/hardware/mtkcam
MTK_PATH_CAM_LEGACY := $(MTK_PATH_SOURCE)/hardware/mtkcam/legacy

# camera3test_fixtures.cpp add for start camera3 preview
LOCAL_SRC_FILES := \
	HdrEffectHal_test.cpp \
	main.cpp \
	camera3test_fixtures.cpp

LOCAL_C_INCLUDES := \
	$(TOP)/system/media/camera/include \
	$(MTK_PATH_CAM)/feature/include \
	$(MTK_PATH_SOURCE)/hardware/gralloc_extra/include \
	$(MTK_PATH_CUSTOM_PLATFORM)/hal/inc

ifeq ($(TARGET_BOARD_PLATFORM), mt6580)
LOCAL_C_INCLUDES += \
	$(MTK_PATH_CAM_LEGACY)/include \
	$(MTK_PATH_CAM_LEGACY)/include/mtkcam \
	$(MTK_PATH_CAM_LEGACY)/platform/$(TARGET_BOARD_PLATFORM)/include
endif

# libhardware,libcamera_metadata, libdl add for start camera3 preview
LOCAL_SHARED_LIBRARIES := \
    libcutils \
    libutils \
    libhdrproc  \
    libcam_utils \
    libcamdrv \
    libhardware \
    libcamera_metadata \
    libdl

LOCAL_MODULE_TAGS := tests

# Build the binary to $(TARGET_OUT_DATA_NATIVE_TESTS)/$(LOCAL_MODULE)
# to integrate with auto-test framework.
include $(BUILD_NATIVE_TEST)
