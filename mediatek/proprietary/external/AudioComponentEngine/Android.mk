ifneq ($(strip $(MTK_PLATFORM)),)
LOCAL_PATH := $(my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
MtkAudioBitConverter.cpp \
MtkAudioSrc.cpp \
MtkAudioLoud.cpp

LOCAL_C_INCLUDES := \
    $(MTK_PATH_SOURCE)/external/nvram/libnvram \
    $(MTK_PATH_SOURCE)/external/AudioCompensationFilter \
    $(MTK_PATH_SOURCE)/external/blisrc/blisrc32 \
    $(MTK_PATH_SOURCE)/external/limiter \
    $(MTK_PATH_SOURCE)/external/shifter \
    $(MTK_PATH_SOURCE)/external/bessound_HD


LOCAL_SHARED_LIBRARIES := \
    libaudiocompensationfilter \
    libnvram \
    libnativehelper \
    libcutils \
    libutils \
    libblisrc32 \
    libbessound_hd_mtk \
    libmtklimiter \
    libmtkshifter

ifeq ($(MTK_AUDIO_BLOUD_CUSTOMPARAMETER_REV),MTK_AUDIO_BLOUD_CUSTOMPARAMETER_V5)
  LOCAL_CFLAGS += -DMTK_AUDIO_BLOUD_CUSTOMPARAMETER_V5
else
  ifeq ($(strip $(MTK_AUDIO_BLOUD_CUSTOMPARAMETER_REV)),MTK_AUDIO_BLOUD_CUSTOMPARAMETER_V4)
    LOCAL_CFLAGS += -DMTK_AUDIO_BLOUD_CUSTOMPARAMETER_V4
  endif
endif

LOCAL_MODULE := libaudiocomponentengine

LOCAL_MODULE_TAGS := optional
#ifeq ($(MTK_AUDIO_A64_SUPPORT),yes)
LOCAL_MULTILIB := both
#else
#LOCAL_MULTILIB := 32
#endif
include $(BUILD_SHARED_LIBRARY)
endif
