ifneq ($(strip $(MTK_PLATFORM)),)
LOCAL_PATH := $(my-dir)

include $(CLEAR_VARS)

ifeq ($(findstring MTK_AOSP_ENHANCEMENT,  $(COMMON_GLOBAL_CPPFLAGS)),)
    LOCAL_CFLAGS += -DMTK_BASIC_PACKAGE
endif

LOCAL_SRC_FILES := \
AudioCompFltCustParam.cpp

LOCAL_C_INCLUDES := \
    $(MTK_PATH_SOURCE)/external/audiocustparam \
    $(MTK_PATH_SOURCE)/external/nvram/libnvram \
    $(MTK_PATH_CUSTOM)/cgen/inc \
    $(MTK_PATH_CUSTOM)/cgen/cfgfileinc \
    $(MTK_PATH_CUSTOM)/cgen/cfgdefault



LOCAL_SHARED_LIBRARIES := \
    libcustom_nvram \
    libnvram \
    libnativehelper \
    libcutils \
    libutils \
    libaudiocustparam

# MTK Audio Tuning Tool Version
LOCAL_C_INCLUDES += \
        external/libxml2/include \
        external/icu/icu4c/source/common \
        $(JNI_H_INCLUDE) \
        $(MTK_PATH_SOURCE)/external/AudioParamParser/include \
        $(MTK_PATH_SOURCE)/external/AudioParamParser

ifneq ($(MTK_AUDIO_TUNING_TOOL_VERSION),)
  ifneq ($(strip $(MTK_AUDIO_TUNING_TOOL_VERSION)),V1)
    MTK_AUDIO_TUNING_TOOL_V2_PHASE:=$(shell echo $(MTK_AUDIO_TUNING_TOOL_VERSION) | sed 's/V2.//g')
    LOCAL_CFLAGS += -DMTK_AUDIO_HIERARCHICAL_PARAM_SUPPORT
    LOCAL_CFLAGS += -DMTK_AUDIO_TUNING_TOOL_V2_PHASE=$(MTK_AUDIO_TUNING_TOOL_V2_PHASE)
    LOCAL_SHARED_LIBRARIES += libaudio_param_parser
    LOCAL_SHARED_LIBRARIES += libmedia
  endif
endif
# MTK Audio Tuning Tool Version

ifeq ($(MTK_STEREO_SPK_ACF_TUNING_SUPPORT),yes)
  LOCAL_CFLAGS += -DMTK_STEREO_SPK_ACF_TUNING_SUPPORT
endif

ifeq ($(MTK_AUDIO_BLOUD_CUSTOMPARAMETER_REV),MTK_AUDIO_BLOUD_CUSTOMPARAMETER_V5)
  LOCAL_CFLAGS += -DMTK_AUDIO_BLOUD_CUSTOMPARAMETER_V5
else
  ifeq ($(strip $(MTK_AUDIO_BLOUD_CUSTOMPARAMETER_REV)),MTK_AUDIO_BLOUD_CUSTOMPARAMETER_V4)
    LOCAL_CFLAGS += -DMTK_AUDIO_BLOUD_CUSTOMPARAMETER_V4
  endif
endif

LOCAL_MODULE := libaudiocompensationfilter

LOCAL_MODULE_TAGS := optional

#ifeq ($(MTK_AUDIO_A64_SUPPORT),yes)
LOCAL_MULTILIB := both
#else
#LOCAL_MULTILIB := 32
#endif

include $(BUILD_SHARED_LIBRARY)
endif
