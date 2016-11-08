ifeq ($(OP01_COMPATIBLE), no)
LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
LOCAL_SHARED_LIBRARIES := liblog libcutils
LOCAL_STATIC_LIBRARIES := libselinux
LOCAL_SRC_FILES := backup_restore_service.c
LOCAL_MODULE := br_app_data_service
LOCAL_MODULE_TAGS := optional
include $(BUILD_EXECUTABLE)
endif
