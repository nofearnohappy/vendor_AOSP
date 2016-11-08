LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := mtee_logctrl
LOCAL_SRC_FILES := mtee_logctrl.c
LOCAL_MODULE_TAGS := optional
LOCAL_C_INCLUDES += \
    $(call include-path-for, trustzone) \
    $(call include-path-for, trustzone-uree)

LOCAL_CFLAGS += -Wall -Wno-unused-parameter -Werror

#LOCAL_CFLAGS += ${TZ_CFLAG}
#LOCAL_LDFLAGS += --gc-sections
#LOCAL_ASFLAGS += -DASSEMBLY
LOCAL_SHARED_LIBRARIES += libtz_uree
include $(BUILD_EXECUTABLE)

