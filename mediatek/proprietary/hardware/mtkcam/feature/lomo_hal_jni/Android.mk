
LOCAL_PATH := $(call my-dir)

################################################################################
#
################################################################################
include $(CLEAR_VARS)

-include $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam/mtkcam.mk
#After Jonas' review in KK
#-----------------------------------------------------------
LOCAL_SRC_FILES += $(call all-c-cpp-files-under, .)

LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam/legacy/include
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/gralloc_extra/include
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam/common/include


#-----------------------------------------------------------
LOCAL_MODULE := libcam.jni.lomohaljni

LOCAL_MODULE_TAGS := optional

#-----------------------------------------------------------
LOCAL_CFLAGS += $(MTKCAM_CFLAGS)
include $(BUILD_SHARED_LIBRARY)


################################################################################
#
################################################################################
#include $(CLEAR_VARS)
include $(call all-makefiles-under,$(LOCAL_PATH))

