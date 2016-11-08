#
# camshottest
#
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

#
LOCAL_SRC_FILES := \
    main.cpp \
    test_singleshot.cpp \
    #test_multishot.cpp \

#
# Note: "/bionic" and "/external/stlport/stlport" is for stlport.
#LOCAL_C_INCLUDES += $(TOP)/bionic
#LOCAL_C_INCLUDES += $(TOP)/external/stlport/stlport
# 
# camera Hardware 
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_PLATFORM)/hardware/mtkcam/
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_PLATFORM)/hardware/mtkcam/inc
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_PLATFORM)/hardware/mtkcam/inc/common
LOCAL_C_INCLUDES += $(MTKCAM_HAL_INCLUDE)
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam/inc
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/gralloc_extra/include
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam

# vector
LOCAL_SHARED_LIBRARIES := \
    libcutils \
    libutils \
    #libstlport \

# shot 
LOCAL_SHARED_LIBRARIES += \
    libcam.camshot \

# sensor hal
LOCAL_SHARED_LIBRARIES += \
    libcam.halsensor \

# camUtils
LOCAL_SHARED_LIBRARIES += \
    libcam_utils \

#
LOCAL_STATIC_LIBRARIES := \

#
LOCAL_WHOLE_STATIC_LIBRARIES := \

#
LOCAL_MODULE := camshottest

#
LOCAL_MODULE_TAGS := eng

#
LOCAL_PRELINK_MODULE := false
#

#
# Start of common part ------------------------------------
-include $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam/mtkcam.mk

#-----------------------------------------------------------
LOCAL_CFLAGS += $(MTKCAM_CFLAGS)

#-----------------------------------------------------------
LOCAL_C_INCLUDES += $(MTKCAM_C_INCLUDES)

#-----------------------------------------------------------
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/include
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_PLATFORM)/hardware/include/D1

# End of common part ---------------------------------------
#
include $(BUILD_EXECUTABLE)


#
#include $(call all-makefiles-under,$(LOCAL_PATH))
