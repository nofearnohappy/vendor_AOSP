# Copyright Statement:
#
# This software/firmware and related documentation ("MediaTek Software") are
# protected under relevant copyright laws. The information contained herein
# is confidential and proprietary to MediaTek Inc. and/or its licensors.
# Without the prior written permission of MediaTek inc. and/or its licensors,
# any reproduction, modification, use or disclosure of MediaTek Software,
# and information contained herein, in whole or in part, shall be strictly prohibited.
#
# MediaTek Inc. (C) 2010. All rights reserved.
#
# BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
# THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
# RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
# AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
# EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
# MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
# NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
# SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
# SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
# THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
# THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
# CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
# SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
# STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
# CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
# AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
# OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
# MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
#
# The following software/firmware and/or related documentation ("MediaTek Software")
# have been modified by MediaTek Inc. All revisions are subject to any receiver's
# applicable license agreements with MediaTek Inc.

#  only if use yusu audio will build this.
#ifneq ($(TARGET_BUILD_PDK),true)
ifeq ($(strip $(BOARD_USES_MTK_AUDIO)),true)

LOCAL_PATH:= $(call my-dir)
LOCAL_COMMON_PATH:=../common

include $(CLEAR_VARS)

ifeq ($(MTK_PLATFORM),MT6795)

ifeq ($(findstring MTK_AOSP_ENHANCEMENT,  $(COMMON_GLOBAL_CPPFLAGS)),)
	LOCAL_CFLAGS += -DMTK_BASIC_PACKAGE
endif

ifeq ($(strip $(MTK_HIGH_RESOLUTION_AUDIO_SUPPORT)),yes)
    LOCAL_CFLAGS += -DMTK_HD_AUDIO_ARCHITECTURE
#    LOCAL_CFLAGS += -DMTK_DYNAMIC_CHANGE_HAL_BUFFER_SIZE
endif

ifeq ($(AUDIO_POLICY_TEST),true)
  ENABLE_AUDIO_DUMP := true
endif

ifeq ($(strip $(TARGET_BUILD_VARIANT)),eng)
  LOCAL_CFLAGS += -DDEBUG_AUDIO_PCM
endif

ifeq ($(MTK_DIGITAL_MIC_SUPPORT),yes)
  LOCAL_CFLAGS += -DMTK_DIGITAL_MIC_SUPPORT
endif

ifeq ($(strip $(MTK_AUDENH_SUPPORT)),yes)
  LOCAL_CFLAGS += -DMTK_AUDENH_SUPPORT
endif

ifeq ($(strip $(MTK_BESLOUDNESS_SUPPORT)),yes)
  LOCAL_CFLAGS += -DMTK_BESLOUDNESS_SUPPORT
endif

ifeq ($(strip $(MTK_BESSURROUND_SUPPORT)),yes)
  LOCAL_CFLAGS += -DMTK_BESSURROUND_SUPPORT
endif

ifeq ($(strip $(MTK_HDMI_MULTI_CHANNEL_SUPPORT)),yes)
  LOCAL_CFLAGS += -DMTK_HDMI_MULTI_CHANNEL_SUPPORT
endif

ifeq ($(strip $(MTK_2IN1_SPK_SUPPORT)),yes)
  LOCAL_CFLAGS += -DUSING_2IN1_SPEAKER
endif

ifeq ($(strip $(MTK_USE_ANDROID_MM_DEFAULT_CODE)),yes)
  LOCAL_CFLAGS += -DANDROID_DEFAULT_CODE
endif

ifeq ($(strip $(DMNR_TUNNING_AT_MODEMSIDE)),yes)
  LOCAL_CFLAGS += -DDMNR_TUNNING_AT_MODEMSIDE
endif

ifeq ($(MTK_DUAL_MIC_SUPPORT),yes)
  LOCAL_CFLAGS += -DMTK_DUAL_MIC_SUPPORT
endif

ifeq ($(MTK_MAGICONFERENCE_SUPPORT),yes)
LOCAL_CFLAGS += -MTK_MAGICONFERENCE_SUPPORT
endif

ifeq ($(MTK_VIBSPK_SUPPORT),yes)
  LOCAL_CFLAGS += -DMTK_VIBSPK_SUPPORT
endif

ifeq ($(NXP_SMARTPA_SUPPORT),yes)
  LOCAL_CFLAGS += -DNXP_SMARTPA_SUPPORT
  LOCAL_CFLAGS += -DEXT_SPK_SUPPORT
endif

#ifeq ($(strip $(TARGET_BUILD_VARIANT)),eng)
#  LOCAL_CFLAGS += -DCONFIG_MT_ENG_BUILD
#endif

LOCAL_CFLAGS += -DUPLINK_LOW_LATENCY
LOCAL_CFLAGS += -DDOWNLINK_LOW_LATENCY

LOCAL_CFLAGS += -DMTK_SUPPORT_AUDIO_DEVICE_API3

ifeq ($(MTK_AUDIO_BLOUD_CUSTOMPARAMETER_REV),MTK_AUDIO_BLOUD_CUSTOMPARAMETER_V5)
  LOCAL_CFLAGS += -DMTK_AUDIO_BLOUD_CUSTOMPARAMETER_V5
else
  ifeq ($(strip $(MTK_AUDIO_BLOUD_CUSTOMPARAMETER_REV)),MTK_AUDIO_BLOUD_CUSTOMPARAMETER_V4)
    LOCAL_CFLAGS += -DMTK_AUDIO_BLOUD_CUSTOMPARAMETER_V4
  endif
endif

#LOCAL_GENERATE_CUSTOM_FOLDER := custom:hal/audioflinger

# Playback must be 24bit when using sram
LOCAL_CFLAGS += -DPLAYBACK_USE_24BITS_ONLY

$(warning $(TOPDIR))

LOCAL_C_INCLUDES:= \
    ./$(TOPDIR)/mediatek/frameworks-ext/av \
    $(TOPDIR)/hardware/libhardware_legacy/include \
    $(TOPDIR)/hardware/libhardware/include \
    $(MTK_PATH_SOURCE)/hardware/ccci/include \
    $(TOPDIR)/frameworks/av/include \
    external/tinyxml \
    $(call include-path-for, audio-utils) \
    $(call include-path-for, audio-effects) \
    $(MTK_PATH_SOURCE)/hardware/audio/mt6795/include \
    $(MTK_PATH_SOURCE)/hardware/audio/common/V3/include \
    $(MTK_PATH_SOURCE)/hardware/audio/common/include \
    $(MTK_PATH_SOURCE)/hardware/audio/common/V3/aud_drv \
    $(MTK_PATH_SOURCE)/external/nvram/libnvram \
    $(MTK_PATH_SOURCE)/external/AudioCompensationFilter \
    $(MTK_PATH_SOURCE)/external/AudioComponentEngine \
    $(MTK_PATH_SOURCE)/external/mp3dec \
    $(MTK_PATH_SOURCE)/external/cvsd_plc_codec \
    $(MTK_PATH_SOURCE)/external/msbc_codec \
    $(MTK_PATH_SOURCE)/hardware/connectivity/bluetooth/driver/inc \
    vendor/mediatek/proprietary/hardware/connectivity/bluetooth/driver/pure/inc \
    $(MTK_PATH_SOURCE)/frameworks/av/include/media \
    $(MTK_PATH_SOURCE)/frameworks-ext/av/include/media \
    $(MTK_PATH_SOURCE)/frameworks-ext/av/include \
    $(MTK_PATH_SOURCE)/frameworks/av/include \
    $(MTK_PATH_SOURCE)/external/audiodcremoveflt \
    $(MTK_PATH_SOURCE)/external/audiocustparam \
    $(MTK_PATH_SOURCE)/external/AudioSpeechEnhancement/V3/inc \
    $(MTK_PATH_SOURCE)/external/dfo/featured \
    $(TARGET_OUT_HEADERS)/dfo \
    $(MTK_PATH_CUSTOM)/custom \
    $(MTK_PATH_CUSTOM)/custom/audio \
    $(MTK_PATH_CUSTOM)/cgen/inc \
    $(MTK_PATH_CUSTOM)/cgen/cfgfileinc \
    $(MTK_PATH_CUSTOM)/cgen/cfgdefault \
    $(MTK_PATH_SOURCE)/external/blisrc/blisrc32 \
    $(MTK_PATH_SOURCE)/external/limiter \
    $(MTK_PATH_SOURCE)/external/shifter \
    $(MTK_PATH_SOURCE)/external/bessound_HD \
    $(MTK_PATH_SOURCE)/external/bessound \
    $(MTK_PATH_SOURCE)/external/blisrc/blisrc16 \
    $(MTK_PATH_CUSTOM)/hal/audioflinger/audio  \
    external/tinyalsa/include  \
    external/tinycompress/include

ifeq ($(EVDO_DT_VIA_SUPPORT),yes)
  LOCAL_C_INCLUDES += $(LOCAL_COMMON_PATH)/V3/include \
    $(MTK_PATH_PLATFORM)/kernel/core/include
endif
ifeq ($(MTK_C2K_SUPPORT),yes)
  LOCAL_C_INCLUDES += $(LOCAL_COMMON_PATH)/V3/include \
    $(MTK_PATH_PLATFORM)/kernel/core/include
endif

ifeq ($(NXP_SMARTPA_SUPPORT),yes)
    LOCAL_C_INCLUDES += \
        $(MTK_PATH_SOURCE)/external/nxp/
endif

LOCAL_SRC_FILES+= \
    $(LOCAL_COMMON_PATH)/aud_drv/audio_hw_hal.cpp \
    $(LOCAL_COMMON_PATH)/aud_drv/AudioMTKFilter.cpp \
    $(LOCAL_COMMON_PATH)/aud_drv/AudioMTKHeadsetMessager.cpp \
    $(LOCAL_COMMON_PATH)/aud_drv/AudioUtility.cpp \
    $(LOCAL_COMMON_PATH)/aud_drv/AudioFtmBase.cpp \
    $(LOCAL_COMMON_PATH)/aud_drv/WCNChipController.cpp \
    $(LOCAL_COMMON_PATH)/speech_driver/SpeechDriverFactory.cpp \
    $(LOCAL_COMMON_PATH)/speech_driver/SpeechDriverDummy.cpp \
    $(LOCAL_COMMON_PATH)/speech_driver/SpeechEnhancementController.cpp \
    $(LOCAL_COMMON_PATH)/speech_driver/SpeechBGSPlayer.cpp \
    $(LOCAL_COMMON_PATH)/speech_driver/SpeechPcm2way.cpp \
    $(LOCAL_COMMON_PATH)/V3/aud_drv/AudioALSAFMController.cpp \
    $(LOCAL_COMMON_PATH)/V3/aud_drv/AudioSpeechEnhanceInfo.cpp \
    $(LOCAL_COMMON_PATH)/V3/aud_drv/AudioVUnlockDL.cpp \
    $(LOCAL_COMMON_PATH)/V3/aud_drv/AudioSpeechEnhLayer.cpp \
    $(LOCAL_COMMON_PATH)/V3/aud_drv/AudioPreProcess.cpp \
    $(LOCAL_COMMON_PATH)/V3/aud_drv/AudioALSADriverUtility.cpp \
    $(LOCAL_COMMON_PATH)/V3/aud_drv/AudioALSASampleRateController.cpp \
    $(LOCAL_COMMON_PATH)/V3/aud_drv/AudioALSAHardware.cpp \
    $(LOCAL_COMMON_PATH)/V3/aud_drv/AudioALSADataProcessor.cpp \
    $(LOCAL_COMMON_PATH)/V3/aud_drv/AudioALSAPlaybackHandlerBase.cpp \
    $(LOCAL_COMMON_PATH)/V3/aud_drv/AudioALSAPlaybackHandlerNormal.cpp \
    $(LOCAL_COMMON_PATH)/V3/aud_drv/AudioALSAPlaybackHandlerFast.cpp \
    $(LOCAL_COMMON_PATH)/V3/aud_drv/AudioALSAPlaybackHandlerVoice.cpp \
    $(LOCAL_COMMON_PATH)/V3/aud_drv/AudioALSAPlaybackHandlerFMTransmitter.cpp \
    $(LOCAL_COMMON_PATH)/V3/aud_drv/AudioALSAPlaybackHandlerBTSCO.cpp \
    $(LOCAL_COMMON_PATH)/V3/aud_drv/AudioALSAPlaybackHandlerBTCVSD.cpp \
    $(LOCAL_COMMON_PATH)/V3/aud_drv/AudioALSAPlaybackHandlerHDMI.cpp \
    $(LOCAL_COMMON_PATH)/V3/aud_drv/AudioALSACaptureHandlerBase.cpp \
    $(LOCAL_COMMON_PATH)/V3/aud_drv/AudioALSACaptureHandlerNormal.cpp \
    $(LOCAL_COMMON_PATH)/V3/aud_drv/AudioALSACaptureHandlerVoice.cpp \
    $(LOCAL_COMMON_PATH)/V3/aud_drv/AudioALSACaptureHandlerFMRadio.cpp \
    $(LOCAL_COMMON_PATH)/V3/aud_drv/AudioALSACaptureHandlerANC.cpp \
    $(LOCAL_COMMON_PATH)/V3/aud_drv/AudioALSACaptureHandlerBT.cpp \
    $(LOCAL_COMMON_PATH)/V3/aud_drv/AudioALSACaptureHandlerAEC.cpp \
    $(LOCAL_COMMON_PATH)/V3/aud_drv/AudioALSACaptureDataClient.cpp \
    $(LOCAL_COMMON_PATH)/V3/aud_drv/AudioALSACaptureDataProviderBase.cpp \
    $(LOCAL_COMMON_PATH)/V3/aud_drv/AudioALSACaptureDataProviderNormal.cpp \
    $(LOCAL_COMMON_PATH)/V3/aud_drv/AudioALSACaptureDataProviderVoice.cpp \
    $(LOCAL_COMMON_PATH)/V3/aud_drv/AudioALSACaptureDataProviderVoiceUL.cpp \
    $(LOCAL_COMMON_PATH)/V3/aud_drv/AudioALSACaptureDataProviderVoiceDL.cpp \
    $(LOCAL_COMMON_PATH)/V3/aud_drv/AudioALSACaptureDataProviderVoiceMix.cpp \
    $(LOCAL_COMMON_PATH)/V3/aud_drv/AudioALSACaptureDataProviderFMRadio.cpp \
    $(LOCAL_COMMON_PATH)/V3/aud_drv/AudioALSACaptureDataProviderANC.cpp \
    $(LOCAL_COMMON_PATH)/V3/aud_drv/AudioALSACaptureDataProviderBTSCO.cpp \
    $(LOCAL_COMMON_PATH)/V3/aud_drv/AudioALSACaptureDataProviderBTCVSD.cpp \
    $(LOCAL_COMMON_PATH)/V3/aud_drv/AudioALSACaptureDataProviderEchoRef.cpp \
    $(LOCAL_COMMON_PATH)/V3/aud_drv/AudioALSACaptureDataProviderEchoRefBTSCO.cpp \
    $(LOCAL_COMMON_PATH)/V3/aud_drv/AudioALSACaptureDataProviderEchoRefExt.cpp \
    $(LOCAL_COMMON_PATH)/V3/aud_drv/AudioALSACaptureDataProviderTDM.cpp \
    $(LOCAL_COMMON_PATH)/V3/aud_drv/AudioALSACodecDeviceBase.cpp \
    $(LOCAL_COMMON_PATH)/V3/aud_drv/AudioALSACodecDeviceOutReceiverPMIC.cpp \
    $(LOCAL_COMMON_PATH)/V3/aud_drv/AudioALSACodecDeviceOutEarphonePMIC.cpp \
    $(LOCAL_COMMON_PATH)/V3/aud_drv/AudioALSACodecDeviceOutSpeakerPMIC.cpp \
    $(LOCAL_COMMON_PATH)/V3/aud_drv/AudioALSACodecDeviceOutSpeakerEarphonePMIC.cpp \
    $(LOCAL_COMMON_PATH)/V3/aud_drv/AudioALSADeviceConfigManager.cpp \
    $(LOCAL_COMMON_PATH)/V3/aud_drv/AudioALSAParamTuner.cpp \
    $(LOCAL_COMMON_PATH)/V3/aud_drv/LoopbackManager.cpp \
    $(LOCAL_COMMON_PATH)/V3/aud_drv/AudioALSALoopbackController.cpp \
    $(LOCAL_COMMON_PATH)/V3/aud_drv/AudioALSADeviceParser.cpp \
    $(LOCAL_COMMON_PATH)/V3/aud_drv/AudioBTCVSDControl.cpp \
    $(LOCAL_COMMON_PATH)/V3/aud_drv/AudioVolumeFactory.cpp \
    $(LOCAL_COMMON_PATH)/V3/aud_drv/SpeechDataProcessingHandler.cpp \
    $(LOCAL_COMMON_PATH)/V3/speech_driver/SpeechDriverLAD.cpp \
    $(LOCAL_COMMON_PATH)/V3/speech_driver/SpeechMessengerECCCI.cpp \
    $(LOCAL_COMMON_PATH)/V3/speech_driver/SpeechVMRecorder.cpp \
    $(LOCAL_COMMON_PATH)/V3/speech_driver/SpeechANCController.cpp \
    $(LOCAL_COMMON_PATH)/V3/speech_driver/AudioALSASpeechLoopbackController.cpp \
    aud_drv/AudioALSACaptureHandlerTDM.cpp \
    aud_drv/AudioALSAHardwareResourceManager.cpp \
    aud_drv/AudioALSAStreamIn.cpp \
    aud_drv/AudioALSAStreamManager.cpp \
    aud_drv/AudioALSAStreamOut.cpp \
    aud_drv/AudioALSAVoiceWakeUpController.cpp \
    aud_drv/AudioALSAVolumeController.cpp \
    aud_drv/AudioFtm.cpp \
    speech_driver/AudioALSASpeechPhoneCallController.cpp


ifeq ($(MTK_C2K_SUPPORT),yes)
      LOCAL_SRC_FILES += $(LOCAL_COMMON_PATH)/V3/speech_driver/SpeechMessengerEVDO.cpp
      LOCAL_CFLAGS += -DMTK_C2K_SUPPORT
endif
ifeq ($(MTK_DT_SUPPORT),yes)
    ifeq ($(EVDO_DT_VIA_SUPPORT),yes)
        LOCAL_SRC_FILES += $(LOCAL_COMMON_PATH)/V3/speech_driver/SpeechDriverVendEVDO.cpp \
                           $(LOCAL_COMMON_PATH)/V3/speech_driver/SpeechMessengerVendEVDO.cpp
        LOCAL_CFLAGS += -DEVDO_DT_VEND_SUPPORT
    endif
    ifeq ($(EVDO_DT_VIA_SUPPORT),no)
#        ifeq ($(MTK_C2K_SUPPORT),yes)
#            LOCAL_SRC_FILES += $(LOCAL_COMMON_PATH)/V3/speech_driver/SpeechMessengerEVDO.cpp
#            LOCAL_CFLAGS += -DMTK_C2K_SUPPORT
#        endif
    endif
    ifeq ($(MTK_C2K_SUPPORT),no)
        LOCAL_SRC_FILES += $(LOCAL_COMMON_PATH)/V3/speech_driver/SpeechDriverDSDA.cpp \
                           $(LOCAL_COMMON_PATH)/V3/speech_driver/SpeechMessengerDSDA.cpp
        LOCAL_CFLAGS += -DDSDA_SUPPORT
    endif
endif

ifeq ($(MTK_INT_MD_SPE_FOR_EXT_MD),yes)
  LOCAL_CFLAGS += -DMTK_INT_MD_SPE_FOR_EXT_MD
endif

ifeq ($(ENABLE_AUDIO_DUMP),true)
  LOCAL_SRC_FILES += AudioDumpInterface.cpp
  LOCAL_CFLAGS += -DENABLE_AUDIO_DUMP
endif

#ifeq ($(MTK_VIBSPK_SUPPORT),yes)
  LOCAL_SRC_FILES += $(LOCAL_COMMON_PATH)/aud_drv/AudioVIBSPKControl.cpp
#endif

ifeq ($(MTK_SPEAKER_MONITOR_SUPPORT),yes)
  LOCAL_CFLAGS += -DMTK_SPEAKER_MONITOR_SUPPORT
  #LOCAL_CFLAGS += -DMTK_SPEAKER_MONITOR_SPEECH_SUPPORT
  LOCAL_SRC_FILES += aud_drv/AudioALSASpeakerMonitor.cpp
  LOCAL_SRC_FILES += $(LOCAL_COMMON_PATH)/V3/aud_drv/AudioALSACaptureHandlerSpkFeed.cpp
  LOCAL_SRC_FILES += $(LOCAL_COMMON_PATH)/V3/aud_drv/AudioALSACaptureDataProviderSpkFeed.cpp
  LOCAL_SRC_FILES += $(LOCAL_COMMON_PATH)/V3/aud_drv/AudioALSACaptureHandlerModemDai.cpp
  LOCAL_SRC_FILES += $(LOCAL_COMMON_PATH)/V3/aud_drv/AudioALSACaptureDataProviderModemDai.cpp
  LOCAL_SRC_FILES += $(LOCAL_COMMON_PATH)/V3/aud_drv/AudioALSAPlaybackHandlerSphDL.cpp
  LOCAL_SRC_FILES += $(LOCAL_COMMON_PATH)/V3/speech_driver/AudioALSASpeechStreamController.cpp
  LOCAL_C_INCLUDES+= \
    $(MTK_PATH_SOURCE)/external/fft
endif

ifeq ($(strip $(MTK_TTY_SUPPORT)),yes)
  LOCAL_CFLAGS += -DMTK_TTY_SUPPORT
endif


LOCAL_STATIC_LIBRARIES := \
    libmedia_helper \
    libfft

ifeq ($(NXP_SMARTPA_SUPPORT),yes)
    LOCAL_CFLAGS += -DNXP_SMARTPA_SUPPORT
    LOCAL_CFLAGS += -DNXP_SMARTPA_SUPPORT_TFA9887
    LOCAL_CFLAGS += -DEXT_SPK_SUPPORT
    LOCAL_SRC_FILES += $(LOCAL_COMMON_PATH)/V3/aud_drv/AudioALSACodecDeviceOutSpeakerNXP.cpp
LOCAL_SHARED_LIBRARIES += \
        libtfa9887_interface
endif

ifeq ($(strip $(NXP_SMARTPA_SUPPORT)),tfa9890)
    LOCAL_CFLAGS += -DNXP_SMARTPA_SUPPORT
    LOCAL_CFLAGS += -DNXP_SMARTPA_SUPPORT_TFA9890
    LOCAL_CFLAGS += -DEXT_SPK_SUPPORT
    LOCAL_SRC_FILES += $(LOCAL_COMMON_PATH)/V3/aud_drv/AudioALSACodecDeviceOutSpeakerNXP.cpp
    LOCAL_C_INCLUDES += $(MTK_PATH_SOURCE)/external/nxp
    LOCAL_SHARED_LIBRARIES += libtfa9890_interface
endif

ifeq ($(strip $(MTK_ENABLE_MD1)),yes)
  LOCAL_CFLAGS += -D__MTK_ENABLE_MD1__
endif

ifeq ($(strip $(MTK_ENABLE_MD2)),yes)
  LOCAL_CFLAGS += -D__MTK_ENABLE_MD2__
endif

ifeq ($(strip $(MTK_ENABLE_MD5)),yes)
  LOCAL_CFLAGS += -D__MTK_ENABLE_MD5__
endif

LOCAL_SHARED_LIBRARIES += \
    libmedia \
    libcutils \
    libutils \
    libbinder \
    libhardware_legacy \
    libhardware \
    libblisrc \
    libdl \
    libnvram \
    libspeech_enh_lib \
    libpowermanager \
    libaudiocustparam \
    libaudiosetting \
    libaudiocompensationfilter \
    libcvsd_mtk \
    libmsbc_mtk \
    libaudioutils \
    libaudiocomponentengine \
    libtinyalsa \
    libtinycompress \
    libaudiodcrflt \
    libcustom_nvram \
    libtinyxml

ifeq ($(HAVE_AEE_FEATURE),yes)
    LOCAL_SHARED_LIBRARIES += libaed
    LOCAL_C_INCLUDES += \
    $(MTK_PATH_SOURCE)/external/aee/binary/inc
    LOCAL_CFLAGS += -DHAVE_AEE_FEATURE
endif

ifneq ($(findstring MTK_AOSP_ENHANCEMENT,  $(COMMON_GLOBAL_CPPFLAGS)),)
ifeq ($(MTK_BT_SUPPORT),yes)
LOCAL_SHARED_LIBRARIES += libbluetooth_mtk_pure
#    libbluetoothdrv
endif
endif

ifeq ($(EVDO_DT_VIA_SUPPORT),yes)
  LOCAL_SHARED_LIBRARIES += libdl
endif

ifeq ($(TELEPHONY_DFOSET),yes)
    LOCAL_SHARED_LIBRARIES += libdfo
endif

ifeq ($(MTK_WB_SPEECH_SUPPORT),yes)
  LOCAL_CFLAGS += -DMTK_WB_SPEECH_SUPPORT
endif

ifeq ($(strip $(MTK_FM_SUPPORT)),yes)
    ifeq ($(strip $(MTK_FM_TX_SUPPORT)),yes)
        ifeq ($(strip $(MTK_FM_TX_AUDIO)),FM_DIGITAL_OUTPUT)
            LOCAL_CFLAGS += -DFM_DIGITAL_OUT_SUPPORT
        endif
        ifeq ($(strip $(MTK_FM_TX_AUDIO)),FM_ANALOG_OUTPUT)
            LOCAL_CFLAGS += -DFM_ANALOG_OUT_SUPPORT
        endif
    endif
    ifeq ($(strip $(MTK_FM_RX_SUPPORT)),yes)
        ifeq ($(strip $(MTK_FM_RX_AUDIO)),FM_DIGITAL_INPUT)
            LOCAL_CFLAGS += -DFM_DIGITAL_IN_SUPPORT
        endif
        ifeq ($(strip $(MTK_FM_RX_AUDIO)),FM_ANALOG_INPUT)
            LOCAL_CFLAGS += -DFM_ANALOG_IN_SUPPORT
        endif
    endif
endif

ifeq ($(HAVE_MATV_FEATURE),yes)
    LOCAL_CFLAGS += -DMATV_AUDIO_SUPPORT
endif

ifeq ($(MTK_BT_SUPPORT),yes)
  ifeq ($(MTK_BT_PROFILE_A2DP),yes)
  LOCAL_CFLAGS += -DWITH_A2DP
  endif
else
  ifeq ($(strip $(BOARD_HAVE_BLUETOOTH)),yes)
    LOCAL_CFLAGS += -DWITH_A2DP
  endif
endif

# SRS Processing
ifeq ($(strip $(HAVE_SRSAUDIOEFFECT_FEATURE)),yes)
LOCAL_CFLAGS += -DHAVE_SRSAUDIOEFFECT
endif
# SRS Processing

# Audio HD Record
ifeq ($(MTK_AUDIO_HD_REC_SUPPORT),yes)
    LOCAL_CFLAGS += -DMTK_AUDIO_HD_REC_SUPPORT
endif
# Audio HD Record

# MTK VoIP
ifeq ($(MTK_VOIP_ENHANCEMENT_SUPPORT),yes)
    LOCAL_CFLAGS += -DMTK_VOIP_ENHANCEMENT_SUPPORT
endif
# MTK VoIP

# DMNR 3.0
ifeq ($(strip $(MTK_HANDSFREE_DMNR_SUPPORT)),yes)
  LOCAL_CFLAGS += -DMTK_HANDSFREE_DMNR_SUPPORT
endif
# DMNR 3.0

# Native Audio Preprocess
ifeq ($(strip $(NATIVE_AUDIO_PREPROCESS_ENABLE)),yes)
    LOCAL_CFLAGS += -DNATIVE_AUDIO_PREPROCESS_ENABLE
endif
# Native Audio Preprocess

#Temp tag for FM support WIFI-Display output
LOCAL_CFLAGS += -DMTK_FM_SUPPORT_WFD_OUTPUT

ifeq ($(HAVE_MATV_FEATURE),yes)
    ifeq ($(MTK_MATV_ANALOG_SUPPORT),yes)
        LOCAL_CFLAGS += -DMATV_AUDIO_LINEIN_PATH
    endif
endif

LOCAL_ARM_MODE := arm
LOCAL_MODULE := audio.primary.$(TARGET_BOARD_PLATFORM)
#LOCAL_MODULE_PATH := $(TARGET_OUT_SHARED_LIBRARIES)/hw
LOCAL_MODULE_RELATIVE_PATH := hw
LOCAL_MODULE_TAGS := optional
LOCAL_MULTILIB := both
include $(BUILD_SHARED_LIBRARY)

ifeq ($(findstring MTK_AOSP_ENHANCEMENT,  $(COMMON_GLOBAL_CPPFLAGS)),)
ifneq ($(USE_LEGACY_AUDIO_POLICY), 1)
include $(CLEAR_VARS)
LOCAL_CLANG := false
LOCAL_SRC_FILES:= \
    $(LOCAL_COMMON_PATH)/aud_policy/AudioPolicyManagerCustom.cpp
LOCAL_CFLAGS += -DMTK_AUDIO
LOCAL_CFLAGS += -DMTK_BASIC_PACKAGE
LOCAL_SHARED_LIBRARIES := \
    libcutils \
    libutils \
    libbinder \
    libdl \
    liblog \
    libmedia \
    libhardware \
    libhardware_legacy \
    libaudiopolicymanagerdefault \
    libsoundtrigger

LOCAL_STATIC_LIBRARIES := \
    libmedia_helper

LOCAL_C_INCLUDES:= \
    $(TOPDIR)/hardware/libhardware_legacy/include \
    $(TOPDIR)/hardware/libhardware/include \
    $(TOPDIR)/frameworks/av/include \
    $(TOPDIR)/bionic/libc/kernel/common \
    $(call include-path-for, audio-utils) \
    $(call include-path-for, audio-effects) \
    $(MTK_PATH_SOURCE)/hardware/audio/common/include \
    $(MTK_PATH_SOURCE)/hardware/audio/common/include/hardware \
    $(MTK_PATH_SOURCE)/hardware/audio/common/aud_policy \
    $(MTK_PATH_SOURCE)/external/nvram/libnvram \
    $(MTK_PATH_SOURCE)/external/AudioCompensationFilter \
    $(MTK_PATH_SOURCE)/external/AudioComponentEngine \
    $(MTK_PATH_SOURCE)/external/cvsd_plc_codec \
    $(MTK_PATH_SOURCE)/external/msbc_codec \
    $(TOP)/$(MTK_ROOT)/hardware/connectivity/bluetooth/driver/inc \
    $(TOP)/$(MTK_ROOT)/vendor/mediatek/proprietary/hardware/connectivity/bluetooth/driver/pure/inc \
    $(MTK_PATH_SOURCE)/external/audiocustparam \
    $(MTK_PATH_SOURCE)/external/AudioSpeechEnhancement/V3/inc \
    $(MTK_PATH_SOURCE)/external/blisrc \
    $(MTK_PATH_SOURCE)/external/bessound_HD \
    $(MTK_PATH_SOURCE)/external/bessound \
    $(MTK_PATH_SOURCE)/external/blisrc32 \
    $(MTK_PATH_SOURCE)/external/shifter \
    $(MTK_PATH_SOURCE)/external/limiter \
    $(MTK_PATH_SOURCE)/external/AudioDCRemoval \
    $(MTK_PATH_SOURCE)/external/audiodcremoveflt \
    $(TARGET_OUT_HEADERS)/dfo \
    $(MTK_PATH_CUSTOM)/custom \
    $(MTK_PATH_CUSTOM)/custom/audio \
    $(MTK_PATH_CUSTOM)/cgen/inc \
    $(MTK_PATH_CUSTOM)/cgen/cfgfileinc \
    $(MTK_PATH_CUSTOM)/cgen/cfgdefault \
    $(MTK_PATH_CUSTOM)/hal/audioflinger/audio \
    $(MTK_PATH_CUSTOM)/dfo/include \
    $(TOPDIR)frameworks/av/services/audiopolicy \
    $(TOPDIR)frameworks/av/services/audiopolicy/engine/interface \
    $(TOPDIR)frameworks/av/services/audiopolicy/common/include \
    $(TOPDIR)frameworks/av/services/audiopolicy/common/managerdefinitions/include

LOCAL_MODULE:= libaudiopolicymanagercustom
include $(BUILD_SHARED_LIBRARY)

ifeq ($(USE_CUSTOM_AUDIO_POLICY), 1)
include $(CLEAR_VARS)
LOCAL_CLANG := false
LOCAL_SRC_FILES:= \
    $(LOCAL_COMMON_PATH)/aud_policy/AudioPolicyFactory.cpp
LOCAL_SHARED_LIBRARIES := \
    libaudiopolicymanagercustom
LOCAL_C_INCLUDES := \
    $(TOPDIR)frameworks/av/services/audiopolicy \
    $(TOPDIR)frameworks/av/services/audiopolicy/engine/interface \
    $(TOPDIR)frameworks/av/services/audiopolicy/common/include \
    $(TOPDIR)frameworks/av/services/audiopolicy/common/managerdefinitions/include \
    $(MTK_PATH_SOURCE)/hardware/audio/common/include \
    $(MTK_PATH_SOURCE)/hardware/audio/common/include/hardware \
    $(MTK_PATH_SOURCE)/hardware/audio/common/aud_policy
LOCAL_MODULE:= libaudiopolicymanager
include $(BUILD_SHARED_LIBRARY)
endif

endif
endif

endif
endif
