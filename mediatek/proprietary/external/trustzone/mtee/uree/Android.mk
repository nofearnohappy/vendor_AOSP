LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := libtz_uree
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := uree.c \
                   uree_mem.c \
                   dump_sec_mem.c \
                   uree_dbg.c

LOCAL_C_INCLUDES += \
    $(call include-path-for, trustzone) \
    $(call include-path-for, trustzone-uree) \
    $(word 1,$(TARGET_BOARD_KERNEL_HEADERS))/trustzone \
    $(TOP)/kernel-3.18/include/trustzone \
     $(MTK_PATH_SOURCE)/external/trustzone/mtee/include/

LOCAL_CFLAGS += -Wall -Wno-unused-parameter -Werror
LOCAL_SHARED_LIBRARIES += liblog


#LOCAL_CFLAGS += ${TZ_CFLAG}
#LOCAL_LDFLAGS += --gc-sections
#LOCAL_ASFLAGS += -DASSEMBLY
#LOCAL_STATIC_LIBRARIES += libc_tz libtest
include $(BUILD_SHARED_LIBRARY)

