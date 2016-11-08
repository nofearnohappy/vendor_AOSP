LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := rpmb_svc
LOCAL_SRC_FILES := rpmb_svc.c \
                   rpmb_api.c \
                   uree_rpmb.c

LOCAL_MODULE_TAGS := optional
LOCAL_C_INCLUDES += \
    $(call include-path-for, trustzone) \
    $(call include-path-for, trustzone-uree)

LOCAL_CFLAGS += -Wall -Wno-unused-parameter -Werror

LOCAL_SHARED_LIBRARIES += libtz_uree
LOCAL_SHARED_LIBRARIES += liblog
include $(BUILD_EXECUTABLE)

include $(CLEAR_VARS)
LOCAL_MODULE := rpmb_test

LOCAL_SRC_FILES := rpmb_api.c \
		   rpmb_test.c \
		   uree_rpmb.c

LOCAL_MODULE_TAGS := optional
LOCAL_C_INCLUDES += \
    $(call include-path-for, trustzone) \
    $(call include-path-for, trustzone-uree)

LOCAL_SHARED_LIBRARIES += libtz_uree
LOCAL_SHARED_LIBRARIES += liblog
include $(BUILD_EXECUTABLE)

include $(CLEAR_VARS)
LOCAL_MODULE := rpmb_key_sts

LOCAL_SRC_FILES := rpmb_api.c \
                   rpmb_key_sts.c \

LOCAL_MODULE_TAGS := optional

LOCAL_C_INCLUDES += \
    $(call include-path-for, trustzone) \
    $(call include-path-for, trustzone-uree)

LOCAL_SHARED_LIBRARIES += liblog
include $(BUILD_EXECUTABLE)
