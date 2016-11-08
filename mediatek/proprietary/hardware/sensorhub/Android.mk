ifeq ($(strip $(MTK_SENSOR_HUB_SUPPORT)),yes)

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_PRELINK_MODULE := false

LOCAL_SHARED_LIBRARIES := \
	liblog \
	libcutils \
	libutils
	
LOCAL_SRC_FILES := \
	shf_types.cpp \
	shf_debug.cpp \
	shf_communicator.cpp \
	shf_hal.cpp
	
LOCAL_C_INCLUDES+= \
        $(MTK_PATH_SOURCE)/hardware/sensorhub/

LOCAL_MODULE := libhwsensorhub

LOCAL_MODULE_TAGS := optional

include $(BUILD_SHARED_LIBRARY)

else

include $(MTK_PATH_SOURCE)/hardware/sensorhub/cwmcu/Android.mk

endif
