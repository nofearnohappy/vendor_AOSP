LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_SRC_FILES := \
	xlog.c \
	xlog_filter.c

ifeq ($(HAVE_AEE_FEATURE),yes)
LOCAL_CFLAGS += -DHAVE_AEE_FEATURE
LOCAL_SHARED_LIBRARIES := \
	liblog \
	libcutils \
	libaed
else
LOCAL_SHARED_LIBRARIES := \
	liblog \
	libcutils
endif
	
LOCAL_MODULE := xlog
LOCAL_MULTILIB := 32
LOCAL_MODULE_TAGS := optional
include $(BUILD_EXECUTABLE)

include $(CLEAR_VARS)
LOCAL_SRC_FILES := \
	xlog_filter.c \
	xlog_filter_test.c
LOCAL_MODULE := xlog_filter_test
LOCAL_MODULE_TAGS = optional
include $(BUILD_HOST_EXECUTABLE)
