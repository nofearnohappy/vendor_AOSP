ifneq ($(strip $(MTK_PLATFORM)),)

LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := eng

LOCAL_SRC_FILES:=       \
	AudioSetParam.cpp
	
LOCAL_C_INCLUDES=       \
       $(MTK_PATH_SOURCE)/system/core/include \
       $(MTK_PATH_SOURCE)/hardware/libhardware/include \
       $(MTK_PATH_SOURCE)/hardware/libhardware_legacy/include \
       $(MTK_PATH_SOURCE)/frameworks/av/include \
       $(MTK_PATH_SOURCE)/frameworks/av/include/media \
       $(TOP)/frameworks/av/include/media

LOCAL_SHARED_LIBRARIES := libcutils libutils libbinder libmedia libaudioflinger

LOCAL_MODULE:= AudioSetParam
LOCAL_MULTILIB := 32

include $(BUILD_EXECUTABLE)

endif
