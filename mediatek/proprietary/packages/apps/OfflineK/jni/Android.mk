LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_LDLIBS := -L$(SYSROOT)/usr/lib -llog -lm
LOCAL_SHARED_LIBRARIES := liblog libcutils
LOCAL_MODULE    := libSdioETT
LOCAL_SRC_FILES := SdioETT.cpp ett_core.cpp
LOCAL_C_INCLUDES := $(JNI_H_INCLUDE) $(LOCAL_PATH) $(MTK_ROOT)/external/meta/common/inc $(LOCAL_PATH)/../chip

include $(BUILD_SHARED_LIBRARY)
#include $(BUILD_EXECUTABLE)
