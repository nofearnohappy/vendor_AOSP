LOCAL_PATH := $(call my-dir)
BUILD_SELF_TEST := true

ifeq ($(BUILD_SELF_TEST), true)
include $(CLEAR_VARS)
LOCAL_SRC_FILES := autok_main.cpp node_ops.cpp nodes_data.cpp param_utility.cpp autok_flow.cpp nvram_utility.cpp uevent_utility.cpp
LOCAL_C_INCLUDES := $(MTK_PATH_CUSTOM)/hal/inc \
    $(MTK_PATH_SOURCE)/external/nvram/libnvram \
  
LOCAL_SHARED_LIBRARIES := libstdc++ libnvram libcustom_nvram libcutils libdl liblog
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := autokd
include $(BUILD_EXECUTABLE)
endif
