ifneq ($(strip $(MTK_PLATFORM)),)
LOCAL_PATH := $(my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
AudioMTKDcRemoval.cpp

LOCAL_C_INCLUDES := \
	$(MTK_PATH_SOURCE)/external/audiodcremoveflt

LOCAL_SHARED_LIBRARIES := \
    libaudiodcrflt \
    libnativehelper \
    libcutils \
    libutils 
	
LOCAL_MODULE := libaudiomtkdcremoval

LOCAL_MODULE_TAGS := optional
#ifeq ($(MTK_AUDIO_A64_SUPPORT),yes)#
LOCAL_MULTILIB := both
#else#
#LOCAL_MULTILIB := 32#
#endif#
include $(BUILD_SHARED_LIBRARY)
endif
