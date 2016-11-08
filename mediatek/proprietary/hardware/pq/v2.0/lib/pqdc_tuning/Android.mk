LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
	pqdc_tuning_jni.cpp

LC_MTK_PLATFORM := $(shell echo $(MTK_PLATFORM) | tr A-Z a-z )

LOCAL_C_INCLUDES := $(JNI_H_INCLUDE)

LOCAL_C_INCLUDES += \
    $(TOP)/$(MTK_PATH_SOURCE)/platform/$(LC_MTK_PLATFORM)/kernel/drivers/dispsys \
    $(TOP)/$(MTK_PATH_SOURCE)/hardware/pq/v2.0/include \
    $(TOP)/frameworks/base/include \
    
LOCAL_SHARED_LIBRARIES := \
    libutils \
    libcutils \
    libdpframework \
    libbinder \
    libpqservice 
  
LOCAL_MODULE := libPQDCjni
LOCAL_MULTILIB := both

include $(BUILD_SHARED_LIBRARY)

include $(call all-makefiles-under,$(LOCAL_PATH))