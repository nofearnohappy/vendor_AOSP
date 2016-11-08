# Build the unit tests
LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := mflltest_ut
LOCAL_MODULE_TAGS := tests

# camera3test_fixtures.cpp add for start camera3 preview
LOCAL_SRC_FILES := \
	main.cpp \

# libhardware,libcamera_metadata, libdl add for start camera3 preview
LOCAL_SHARED_LIBRARIES := \
	$(MFLL_CORE_LIB_NAME) \
	$(MFLL_SHARED_LIBS) \

LOCAL_C_INCLUDES += $(MFLL_INCLUDE_PATH)

ifeq ($(strip $(MFLL_LOG_STDOUT)),yes)
LOCAL_CFLAGS += -DMFLL_LOG_STDOUT
endif

include $(BUILD_EXECUTABLE)

