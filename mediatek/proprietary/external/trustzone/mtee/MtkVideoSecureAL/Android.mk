ifeq ($(strip $(MTK_SEC_VIDEO_PATH_SUPPORT)),yes)

LOCAL_PATH := $(call my-dir)

$(warning start lib_uree_mtk_video_secure_al)
include $(CLEAR_VARS)
LOCAL_MODULE := lib_uree_mtk_video_secure_al
LOCAL_SRC_FILES := MtkVideoSecureAL.c
LOCAL_MODULE_TAGS := optional
LOCAL_C_INCLUDES += \
    $(call include-path-for, trustzone) \
    $(call include-path-for, trustzone-uree)

LOCAL_SHARED_LIBRARIES :=       \
	libutils                \
        libcutils

ifeq ($(strip $(MTK_IN_HOUSE_TEE_SUPPORT)),yes)
LOCAL_CFLAGS += -DMTK_IN_HOUSE_TEE_SUPPORT
endif 

ifeq ($(strip $(MTK_SEC_WFD_VIDEO_PATH_SUPPORT)),yes)
LOCAL_CFLAGS += -DMTK_SEC_WFD_VIDEO_PATH_SUPPORT
endif

ifeq ($(strip $(MTK_SEC_VIDEO_PATH_SUPPORT)),yes)
LOCAL_CFLAGS += -DMTK_SEC_VIDEO_PATH_SUPPORT
endif

#LOCAL_CFLAGS += ${TZ_CFLAG}
#LOCAL_LDFLAGS += --gc-sections
#LOCAL_ASFLAGS += -DASSEMBLY
#LOCAL_STATIC_LIBRARIES += libc_tz libtest
LOCAL_SHARED_LIBRARIES += libtz_uree
include $(BUILD_SHARED_LIBRARY)

$(warning finish lib_uree_mtk_video_secure_al)
endif
