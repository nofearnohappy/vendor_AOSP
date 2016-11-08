LOCAL_PATH := $(call my-dir)

$(info Building ts_mu3d)

# Build the target version
include $(CLEAR_VARS)

MU3D_TS_SRC_FILES :=  auto_test_cmd.c \
											cli_input.c \
											cli_parser.c \
											dev_test_cmd.c \
											hub_test_cmd.c \
											main.c \
											ring_test_cmd.c \
											stress_test_cmd.c \
											cli_command.c \
											cli_modulecmd.c \
											dbg_cmd.c \
											hcd_test_cmd.c \
											loop_test_cmd.c \
											power_test_cmd.c \
											slot_test_cmd.c

MU3D_TS_MODULE := ts_mu3d

ifeq ($(DEBUG_TS_MU3D),yes)
MU3D_TS_CFLAGS := -DDEBUG_TS_MU3D
else
MU3D_TS_CFLAGS :=
endif


LOCAL_SRC_FILES := $(MU3D_TS_SRC_FILES)
LOCAL_MODULE := $(MU3D_TS_MODULE)
LOCAL_CFLAGS += $(MU3D_TS_CFLAGS)
LOCAL_MODULE_TAGS := optional

include $(BUILD_EXECUTABLE)
