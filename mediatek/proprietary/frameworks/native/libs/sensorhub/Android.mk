ifeq ($(strip $(MTK_SENSOR_HUB_SUPPORT)),yes)

LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SHARED_LIBRARIES := \
	libbinder \
	libcutils \
	libgui \
	libutils \
	liblog \
	libhardware \
	libhwsensorhub

LOCAL_SRC_FILES:= \
	SensorContext.cpp \
	SensorData.cpp \
	SensorAction.cpp \
	SensorCondition.cpp \
	ISensorHubClient.cpp \
	ISensorHubServer.cpp \
	SensorHubManager.cpp \
	
LOCAL_C_INCLUDES+= \
        $(MTK_PATH_SOURCE)/hardware/sensorhub/ \
        $(MTK_PATH_SOURCE)/frameworks/native/libs/sensorhub/include/

LOCAL_MODULE:= libsensorhub

include $(BUILD_SHARED_LIBRARY)

endif

