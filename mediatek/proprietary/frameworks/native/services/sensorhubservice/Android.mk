ifeq ($(strip $(MTK_SENSOR_HUB_SUPPORT)),yes)

LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
	SimpleBinderHolder.cpp \
	SimpleEventQueue.cpp \
    SensorHubDevice.cpp \
    SensorHubService.cpp

LOCAL_SHARED_LIBRARIES := \
	libcutils \
	libhardware \
	libhardware_legacy \
	libutils \
	liblog \
	libbinder \
	libpowermanager \
	libhwsensorhub \
	libsensorhub
	
LOCAL_C_INCLUDES+= \
    $(MTK_PATH_SOURCE)/hardware/sensorhub/ \
    $(MTK_PATH_SOURCE)/frameworks/native/libs/sensorhub/include/

LOCAL_MODULE:= libsensorhubservice

include $(BUILD_SHARED_LIBRARY)

endif
