####################################################################
LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)


LOCAL_SRC_FILES  := show_logo_common.c\
                  decompress_common.c\
                  show_animation_common.c\
                  charging_animation.cpp

ifeq ($(MTK_PUMP_EXPRESS_SUPPORT), yes)
LOCAL_CFLAGS += -DMTK_PUMP_EXPRESS_SUPPORT
endif
ifeq ($(MTK_PUMP_EXPRESS_PLUS_SUPPORT), yes)
LOCAL_CFLAGS += -DMTK_PUMP_EXPRESS_PLUS_SUPPORT
endif
LOCAL_SHARED_LIBRARIES := libcutils libutils libc libstdc++ libz libdl liblog libgui libui

LOCAL_STATIC_LIBRARIES += libfs_mgr

LOCAL_C_INCLUDES += $(MTK_PATH_CUSTOM)/lk/include/target
LOCAL_C_INCLUDES += $(MTK_PATH_PLATFORM)/lk/include/target
LOCAL_C_INCLUDES += $(LOCAL_PATH)/include
LOCAL_C_INCLUDES += $(TOP)/external/zlib/
LOCAL_C_INCLUDES += system/core/fs_mgr/include

LOCAL_MODULE := libshowlogo
LOCAL_MULTILIB := 32

LOCAL_PRELINK_MODULE := false

include $(BUILD_SHARED_LIBRARY)
