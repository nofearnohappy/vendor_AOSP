
################################################################################

LOCAL_PATH := $(call my-dir)

################################################################################
#
################################################################################
include $(CLEAR_VARS)

#-----------------------------------------------------------
sinclude $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam/mtkcam.mk

#-----------------------------------------------------------
#LOCAL_SRC_FILES += test_3a_irq.cpp
LOCAL_SRC_FILES += test_3a_statistic.cpp
MTKCAM_3A_PATH    := $(MTK_PATH_SOURCE)/hardware/mtkcam/aaa/source/$(MTKCAM_3A_VERSION)

#-----------------------------------------------------------
#-----------------------------------------------------------
LOCAL_C_INCLUDES += $(MTKCAM_C_INCLUDES)
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam/middleware/common/include
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/gralloc_extra/include
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam/ext/include

LOCAL_C_INCLUDES += system/media/camera/include
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/include
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_PLATFORM)/hardware/include
#
#LOCAL_C_INCLUDES += $(TOP)/bionic $(TOP)/external/stlport/stlport
#
LOCAL_C_INCLUDES += $(MTK_PATH_CUSTOM)/kernel/imgsensor/inc
LOCAL_C_INCLUDES += $(MTK_PATH_CUSTOM)/hal/inc/camera_feature
LOCAL_C_INCLUDES += $(MTK_PATH_CUSTOM_PLATFORM)/hal/inc/aaa
LOCAL_C_INCLUDES += $(MTK_PATH_CUSTOM_PLATFORM)/hal/inc/isp_tuning
LOCAL_C_INCLUDES += $(MTK_PATH_CUSTOM)/hal/inc
LOCAL_C_INCLUDES += $(MTK_PATH_CUSTOM)/hal/camera
LOCAL_C_INCLUDES += $(MTK_PATH_CUSTOM_PLATFORM)/hal/inc
LOCAL_C_INCLUDES += $(MTK_PATH_CUSTOM_PLATFORM)/hal/inc/aaa
LOCAL_C_INCLUDES += $(MTK_PATH_PLATFORM)/hardware/mtkcam/hal/aaa
LOCAL_C_INCLUDES += $(MTK_PATH_CUSTOM_PLATFORM)/cgen/cfgfileinc
LOCAL_C_INCLUDES += $(MTKCAM_3A_PATH)
LOCAL_C_INCLUDES += $(MTKCAM_3A_PATH)/buf_mgr
LOCAL_C_INCLUDES += $(MTKCAM_DRV_INCLUDE)
LOCAL_C_INCLUDES += $(MTKCAM_DRV_INCLUDE)/iopipe
LOCAL_C_INCLUDES += $(MTK_PATH_SOURCE)/hardware/mtkcam/drv/src/isp/$(PLATFORM)/inc/
LOCAL_C_INCLUDES += $(MTK_PATH_SOURCE)/hardware/mtkcam/drv/include/$(PLATFORM)/drv/
LOCAL_C_INCLUDES += $(MTK_PATH_SOURCE)/hardware/mtkcam/include/$(PLATFORM)
LOCAL_C_INCLUDES += $(MTK_PATH_SOURCE)/hardware/mtkcam/include
LOCAL_C_INCLUDES += $(MTK_PATH_SOURCE)/hardware/mtkcam/common/include/
LOCAL_C_INCLUDES += $(MTK_PATH_SOURCE)/hardware/mtkcam/common/include/metadata
LOCAL_C_INCLUDES += $(MTK_PATH_SOURCE)/hardware/mtkcam/common/
LOCAL_C_INCLUDES += $(MTK_PATH_SOURCE)/hardware/mtkcam/
LOCAL_C_INCLUDES += $(MTKCAM_3A_INCLUDE)/Hal3
#
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/include/mtkcam
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
    libcam.metadata \
    libcam.halsensor \
    libcam.metadataprovider

LOCAL_SHARED_LIBRARIES += libstdc++

LOCAL_SHARED_LIBRARIES += \
    libcam.hal3a.v3 \

#-----------------------------------------------------------
LOCAL_MODULE_TAGS := eng
LOCAL_MODULE := test_3a

#-----------------------------------------------------------
include $(BUILD_EXECUTABLE)
