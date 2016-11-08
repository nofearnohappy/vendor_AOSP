LOCAL_PATH := $(call my-dir)

# ========================================================
# for LD_PRELOAD
# ========================================================
ifeq ($(MTK_USER_SPACE_DEBUG_FW),yes)
include $(CLEAR_VARS)
LOCAL_SRC_FILES := init.preload.eng.rc
LOCAL_MODULE := init.preload.rc
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_PATH := $(TARGET_ROOT_OUT)
LOCAL_MODULE_TAGS := eng
include $(BUILD_PREBUILT)
endif

