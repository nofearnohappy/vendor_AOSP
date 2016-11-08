LOCAL_PATH := $(call my-dir)

$(info Building ts_mu3h)

# Build the target version
include $(CLEAR_VARS)

MU3H_TS_SRC_FILES :=  cli_parser.c \
											auto_test_cmd.c \
											cli_command.c \
											cli_input.c \
											cli_modulecmd.c \
											hub_test_cmd.c \
											dbg_cmd.c \
											dev_test_cmd.c \
											dev_test_cmd.h \
											hcd_test_cmd.c \
											ring_test_cmd.c \
											loop_test_cmd.c \
											main.c \
											power_test_cmd.c \
											slot_test_cmd.c \
											stress_test_cmd.c \
											otg_test_cmd.c
                  
MU3H_TS_MODULE := ts_mu3h
                      
ifeq ($(DEBUG_TS_MU3H),yes)
MU3H_TS_CFLAGS := -DDEBUG_TS_MU3H
else                  
MU3H_TS_CFLAGS :=     
endif

LOCAL_SRC_FILES := $(MU3H_TS_SRC_FILES)
LOCAL_MODULE := $(MU3H_TS_MODULE)
LOCAL_CFLAGS += $(MU3H_TS_CFLAGS)
LOCAL_MODULE_TAGS := optional

include $(BUILD_EXECUTABLE)
