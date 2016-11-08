################################################################################
#
################################################################################

LOCAL_PATH := $(call my-dir)

################################################################################
#
################################################################################
include $(CLEAR_VARS)

#-----------------------------------------------------------
LOCAL_SRC_FILES += AcdkCCAPTest.cpp

#-----------------------------------------------------------
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam/legacy/include
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam/legacy/include/mtkcam/
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam/legacy/platform/mt6795/include
#
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam/legacy/platform/mt6795/acdk/inc/acdk
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam/legacy/platform/mt6795/acdk/inc/cct
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_COMMON)/kernel/imgsensor/inc
LOCAL_C_INCLUDES += $(MTK_PATH_CUSTOM_PLATFORM)/hal/inc

#-----------------------------------------------------------
LOCAL_WHOLE_STATIC_LIBRARIES +=
#
LOCAL_STATIC_LIBRARIES += libacdk_entry_cctif
LOCAL_STATIC_LIBRARIES += libacdk_entry_mdk

#-----------------------------------------------------------
LOCAL_SHARED_LIBRARIES += liblog libcutils
LOCAL_SHARED_LIBRARIES += libdl

#-----------------------------------------------------------
LOCAL_MODULE_TAGS := eng
LOCAL_MODULE := ccaptest

#-----------------------------------------------------------
ifneq (yes,$(strip $(MTK_EMULATOR_SUPPORT)))
include $(BUILD_EXECUTABLE)
endif
