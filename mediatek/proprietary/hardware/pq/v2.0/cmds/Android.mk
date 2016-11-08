LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
	main_pq.cpp

LOCAL_SHARED_LIBRARIES := \
    libutils \
    libcutils \
    libpq_cust \
    libdpframework \
    libbinder \
    libdl \
    libpqservice

LC_MTK_PLATFORM := $(shell echo $(MTK_PLATFORM) | tr A-Z a-z )

LOCAL_C_INCLUDES += \
    $(TOP)/frameworks/base/include \
    $(MTK_PATH_SOURCE)/hardware/pq/v2.0/include \
    $(TOP)/$(MTK_PATH_SOURCE)/platform/$(LC_MTK_PLATFORM)/kernel/drivers/dispsys \
    $(TOP)/$(MTK_ROOT)/frameworks-ext/native/include 

LOCAL_MODULE:= pq

LOCAL_MODULE_CLASS := EXECUTABLES

include $(BUILD_EXECUTABLE)

include $(call all-makefiles-under,$(LOCAL_PATH))
