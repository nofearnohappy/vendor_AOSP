LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_SRC_FILES := AudioMeta.cpp

ifeq ($(MTK_AUDIO_BLOUD_CUSTOMPARAMETER_REV),MTK_AUDIO_BLOUD_CUSTOMPARAMETER_V5)
  LOCAL_CFLAGS += -DMTK_AUDIO_BLOUD_CUSTOMPARAMETER_V5
else
  ifeq ($(strip $(MTK_AUDIO_BLOUD_CUSTOMPARAMETER_REV)),MTK_AUDIO_BLOUD_CUSTOMPARAMETER_V4)
    LOCAL_CFLAGS += -DMTK_AUDIO_BLOUD_CUSTOMPARAMETER_V4
  endif
endif

LOCAL_C_INCLUDES := $(MTK_PATH_SOURCE)/hardware/meta/common/inc \
                    $(MTK_PATH_SOURCE)/external/mhal/src/custom/inc \
                    $(MTK_PATH_SOURCE)/external/mhal/inc \
                    $(TOP)/$(MTK_PATH_CUSTOM)/kernel/imgsensor/inc \
                    $(MTK_PATH_CUSTOM)/hal/inc \
                    $(TOP)/$(MTK_PATH_CUSTOM)/hal/audioflinger/audio \

					
#FTM include
LOCAL_C_INCLUDES += ./$(TOPDIR)/mediatek/frameworks-ext/av \
					$(TOPDIR)/hardware/libhardware_legacy/include \
					$(TOPDIR)/frameworks/av/include \
					$(call include-path-for, audio-utils) \
					$(call include-path-for, audio-effects) \
					$(MTK_PATH_SOURCE)/hardware/audio/common/include \
					$(MTK_PATH_SOURCE)/external/nvram/libnvram \
					$(MTK_PATH_SOURCE)/external/AudioCompensationFilter \
					$(MTK_PATH_SOURCE)/external/AudioComponentEngine \
					$(MTK_PATH_SOURCE)/external/cvsd_plc_codec \
					$(MTK_PATH_SOURCE)/external/msbc_codec \
					$(MTK_PATH_SOURCE)/frameworks/av/include/media \
					$(MTK_PATH_SOURCE)/frameworks-ext/av/include/media \
					$(MTK_PATH_SOURCE)/frameworks-ext/av/include \
					$(MTK_PATH_SOURCE)/frameworks/av/include \
					$(MTK_PATH_SOURCE)/external/audiodcremoveflt \
					$(MTK_PATH_SOURCE)/external/audiocustparam \
					$(MTK_PATH_SOURCE)/external/AudioSpeechEnhancement/inc \
					$(MTK_PATH_SOURCE)/external/fft \
					$(MTK_PATH_SOURCE)/kernel/include \
					$(MTK_PATH_CUSTOM)/hal/audioflinger \
					$(MTK_PATH_CUSTOM)/hal/audioflinger/audio
					

LOCAL_STATIC_LIBRARIES := libft

ifeq ($(HAVE_AEE_FEATURE),yes)
    LOCAL_SHARED_LIBRARIES += libaed
    LOCAL_C_INCLUDES += \
    $(MTK_PATH_SOURCE)/external/aee/binary/inc
    LOCAL_CFLAGS += -DHAVE_AEE_FEATURE
endif

LOCAL_MODULE := libmeta_audio
LOCAL_PRELINK_MODULE := false
include $(BUILD_STATIC_LIBRARY)

###############################################################################
# SELF TEST
###############################################################################
BUILD_SELF_TEST := false
# BUILD_SELF_TEST := true
ifeq ($(BUILD_SELF_TEST), true)
include $(CLEAR_VARS)
LOCAL_SRC_FILES := AudioMeta.cpp
LOCAL_C_INCLUDES := $(MTK_PATH_SOURCE)/hardware/meta/common/inc
LOCAL_MODULE := meta_battery_test
LOCAL_ALLOW_UNDEFINED_SYMBOLS := true
LOCAL_SHARED_LIBRARIES := libmeta_battery 
LOCAL_STATIC_LIBRARIES := libft

ifeq ($(HAVE_AEE_FEATURE),yes)
    LOCAL_SHARED_LIBRARIES += libaed
    LOCAL_C_INCLUDES += \
    $(MTK_PATH_SOURCE)/external/aee/binary/inc
    LOCAL_CFLAGS += -DHAVE_AEE_FEATURE
endif

LOCAL_MODULE_PATH := $(TARGET_OUT_OPTIONAL_EXECUTABLES)
LOCAL_UNSTRIPPED_PATH := $(TARGET_ROOT_OUT_SBIN_UNSTRIPPED)
include $(BUILD_EXECUTABLE)
endif
