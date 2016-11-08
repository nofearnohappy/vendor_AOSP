LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
				  fgauge_main.cpp \
				  fg_log.cpp 

LOCAL_CFLAGS += $(MTK_CDEFS)

LOCAL_SHARED_LIBRARIES := libcutils libutils libdl

LOCAL_MODULE:= fuelgauged
#bobule workaround pdk build error, needing review
LOCAL_MULTILIB := 32
LOCAL_PRELINK_MODULE := false
include $(BUILD_EXECUTABLE)


