LOCAL_PATH := $(call my-dir)

LOCAL_SRC_FILES := \
	pq_tuning_jni.cpp

LC_MTK_PLATFORM := $(shell echo $(MTK_PLATFORM) | tr A-Z a-z )

LOCAL_C_INCLUDES := $(JNI_H_INCLUDE)

LOCAL_C_INCLUDES += \
    $(TOP)/$(MTK_PATH_SOURCE)/platform/$(LC_MTK_PLATFORM)/kernel/drivers/dispsys \
    $(TOP)/$(MTK_PATH_SOURCE)/hardware/pq/v2.0/include \
    $(TOP)/frameworks/base/include \

LOCAL_SHARED_LIBRARIES := \
  libutils \
  libcutils \
  libdpframework

ifeq (,$(filter $(strip $(TARGET_BOARD_PLATFORM)), mt6572 mt6582 mt6592 mt8127 mt8163 mt2601))
    LOCAL_SHARED_LIBRARIES += \
        libpqservice \
        libbinder
endif

LOCAL_MODULE := libPQjni
LOCAL_MULTILIB := both

include $(BUILD_SHARED_LIBRARY)

include $(call all-makefiles-under,$(LOCAL_PATH))
