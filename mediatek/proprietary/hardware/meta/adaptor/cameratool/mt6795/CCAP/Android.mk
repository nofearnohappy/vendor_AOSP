LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
-include $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam/mtkcam.mk
LOCAL_ARM_MODE := arm
LOCAL_PRELINK_MODULE := false

LOCAL_SRC_FILES:= Meta_CCAP_Para.cpp

LOCAL_C_INCLUDES += \
    $(TOP)/$(MTKCAM_C_INCLUDES) \
    $(MTK_PATH_SOURCE)/hardware/meta/common/inc \
    $(TOP)/$(MTK_MTKCAM_PLATFORM)/acdk/inc/cct \
    $(TOP)/$(MTK_MTKCAM_PLATFORM)/acdk/inc/acdk \
    $(MTK_PATH_SOURCE)/hardware/jpeg/inc/mhal \
    $(MTK_PATH_CUSTOM)/kernel/imgsensor/inc \
    $(TOP)/$(MTK_PATH_COMMON)/kernel/imgsensor/inc \
    $(MTK_PATH_CUSTOM)/hal/inc \
    $(MTK_PATH_CUSTOM_PLATFORM)/hal/inc \

#-----------------------------------------------------------
LOCAL_WHOLE_STATIC_LIBRARIES += libacdk_entry_cctif
LOCAL_WHOLE_STATIC_LIBRARIES += libacdk_entry_mdk

#-----------------------------------------------------------
LOCAL_SHARED_LIBRARIES := libcutils libc libstdc++
LOCAL_STATIC_LIBRARIES += libft

LOCAL_MODULE := libccap

#
# Start of common part ------------------------------------
sinclude $(TOP)/$(MTK_MTKCAM_PLATFORM)/mtkcam.mk

#-----------------------------------------------------------
LOCAL_CFLAGS += $(MTKCAM_CFLAGS)

#-----------------------------------------------------------
LOCAL_C_INCLUDES += $(MTKCAM_C_INCLUDES)

#-----------------------------------------------------------

LOCAL_C_INCLUDES += $(TOP)/$(MTK_MTKCAM_PLATFORM)/include

# End of common part ---------------------------------------
#

include $(BUILD_STATIC_LIBRARY)

