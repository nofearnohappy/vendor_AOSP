ifeq ($(MTK_TC1_FEATURE),yes)
LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
LOCAL_SHARED_LIBRARIES := libcutils libnvram libtc1part
LOCAL_SRC_FILES := lgerft.c

ifeq ($(MTK_GPT_SCHEME_SUPPORT), y)
LOCAL_C_INCLUDES := $(MTK_PATH_SOURCE)/external/nvram/libnvram \
		$(MTK_PATH_SOURCE)/external/tc1_interface/gpt \
    ${LOCAL_PATH}/../libwifitest
else
LOCAL_C_INCLUDES := $(MTK_PATH_SOURCE)/external/nvram/libnvram \
		$(MTK_PATH_SOURCE)/external/tc1_interface/pmt \
    ${LOCAL_PATH}/../libwifitest
endif

LOCAL_CFLAGS += -Wall -Werror
LOCAL_PRELINK_MODULE := false
LOCAL_MODULE := libtc1rft
include $(BUILD_SHARED_LIBRARY)

BUILD_RFT_TEST_APP = true
ifeq ($(BUILD_RFT_TEST_APP),true)
  include $(CLEAR_VARS)
  LOCAL_SHARED_LIBRARIES := libnvram libcutils libtc1rft
  LOCAL_MODULE_TAGS := eng
  LOCAL_SRC_FILES := lgerft_sample.c
  LOCAL_CFLAGS += -Wall -Werror
  LOCAL_MODULE := lgerfttest
  include $(BUILD_EXECUTABLE)
endif
endif
