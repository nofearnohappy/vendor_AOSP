LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

CORE_SRC_FILES := \
	main.c

LOCAL_SRC_FILES := \
	$(CORE_SRC_FILES)

LOCAL_C_INCLUDES := $(TOP)/external/e2fsprogs/lib/

LOCAL_SHARED_LIBRARIES  := \
	libcutils \
	liblog \
	libext2fs \
	libext2_com_err \
	libext2_e2p \
        liblogwrap

LOCAL_MODULE := resize_ext4
LOCAL_MODULE_TAGS := optional

include $(BUILD_EXECUTABLE)
