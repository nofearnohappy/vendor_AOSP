
################################################################################

LOCAL_PATH := $(call my-dir)

################################################################################
#
################################################################################
include $(CLEAR_VARS)

#-----------------------------------------------------------
-include $(TOP)/$(MTK_MTKCAM_PLATFORM)/mtkcam.mk
-include $(TOP)/$(MTK_MTKCAM_PLATFORM)/v1/adapter.mk

#-----------------------------------------------------------
LOCAL_SRC_FILES += $(call all-c-cpp-files-under, .)

#-----------------------------------------------------------
LOCAL_C_INCLUDES += $(MTKCAM_C_INCLUDES)
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/gralloc_extra/include
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam/ext/include

LOCAL_C_INCLUDES += $(MY_ADAPTER_C_INCLUDES)
LOCAL_C_INCLUDES += $(TOP)/$(MTKCAM_C_INCLUDES)/..
LOCAL_C_INCLUDES += $(TOP)/$(MTK_MTKCAM_PLATFORM)/include
#
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam/ext/include
#
LOCAL_C_INCLUDES += $(MY_ADAPTER_C_INCLUDES_PATH)/inc
LOCAL_C_INCLUDES += $(TOP)/system/media/camera/include
#

#-----------------------------------------------------------
LOCAL_CFLAGS += $(MTKCAM_CFLAGS)

#-----------------------------------------------------------
LOCAL_WHOLE_STATIC_LIBRARIES += 
#
LOCAL_STATIC_LIBRARIES += 

#-----------------------------------------------------------
# vector
LOCAL_SHARED_LIBRARIES := \
    liblog \
    libcutils \
    libutils \

LOCAL_SHARED_LIBRARIES += \
    libcam.camadapter \
    libcam.halsensor \
    libfeatureio \

#-----------------------------------------------------------
LOCAL_MODULE_TAGS := eng
LOCAL_MODULE := testAdapter

#-----------------------------------------------------------
include $(BUILD_EXECUTABLE)

