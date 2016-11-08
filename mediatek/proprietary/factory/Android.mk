# Copyright Statement:
#
# This software/firmware and related documentation ("MediaTek Software") are
# protected under relevant copyright laws. The information contained herein
# is confidential and proprietary to MediaTek Inc. and/or its licensors.
# Without the prior written permission of MediaTek inc. and/or its licensors,
# any reproduction, modification, use or disclosure of MediaTek Software,
# and information contained herein, in whole or in part, shall be strictly prohibited.

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

#ifeq ($(TARGET_BUILD_PDK),)

ifneq ($(TARGET_PRODUCT),generic)
ifneq ($(TARGET_SIMULATOR),true)
#ifeq ($(TARGET_ARCH),arm)
ifneq ($(MTK_EMULATOR_SUPPORT),yes)

# factory program
LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
-include $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam/mtkcam.mk
GENERIC_CUSTOM_PATH := $(MTK_ROOT)/custom/generic/factory
HAVE_CUST_FOLDER := $(shell test -d $(MTK_PATH_CUSTOM)/factory && echo yes)

ifeq ($(HAVE_CUST_FOLDER),yes)
CUSTOM_PATH := $(MTK_PATH_CUSTOM)/factory
else
CUSTOM_PATH := $(GENERIC_CUSTOM_PATH)
endif

commands_factory_local_path := $(LOCAL_PATH)

CORE_SRC_FILES := \
	src/factory.cpp \
	src/item.cpp \
	src/util/at_command.cpp \
	src/util/utils.cpp \
	src/util/uart_op.cpp \
	src/util/port.cpp \
	src/util/iniconfig.cpp \
	src/util/version.cpp\
	src/medrv/me_result.cpp \
	src/medrv/me_osal.cpp \
	src/medrv/me_comm.cpp \
	src/medrv/me_func.cpp \
	src/medrv/me_func_cb.cpp \
	src/medrv/me_parser.cpp \
	src/medrv/me_device.cpp \
	src/medrv/me_connection.cpp \
       src/ATE_factory.cpp

TEST_SRC_FILES := \
	src/test/ftm.cpp\
	src/test/ftm_sp_ata.cpp\
	src/test/ftm_mods.cpp\
 	src/test/ftm_keys.c\
 	src/test/ftm_lcd.c\
 	src/test/ftm_lcm.c\
 	src/test/ftm_backlight.c\
 	src/test/ftm_led.c\
 	src/test/ftm_memcard.c\
	src/test/ftm_rtc.cpp\
	src/test/ftm_gsensor.c\
	src/test/ftm_gs_cali.c\
 	src/test/ftm_msensor.c\
 	src/test/ftm_touch.c\
 	src/test/ftm_touch_auto.c\
	src/test/ftm_signaltest.cpp \
 	src/test/ftm_usb.cpp\
 	src/test/ftm_otg.cpp\
	src/test/ftm_idle.c \
 	src/test/ftm_jogball.c \
 	src/test/ftm_ofn.c \
	src/test/ftm_alsps.c \
	src/test/ftm_barometer.c \
	src/test/ftm_humidity.c \
 	src/test/ftm_gyroscope.c \
 	src/test/ftm_gyro_cali.c \
	src/test/ftm_sim.c \
	src/test/ftm_camera.cpp \
	src/test/ftm_rftest.c\
	src/test/ftm_strobe.cpp \
	src/test/ftm_strobe_drv.cpp \
	src/test/ftm_hrm.c \
	src/test/ftm_bts.c \
	src/test/ftm_efuse.c \
	src/test/ftm_lens.cpp \
	src/test/ftm_c2k_rftest.c \
	src/test/ftm_irtx_led_test.c \
#	src/test/ftm_emi.c \

### hotknot
ifeq ($(strip $(MTK_HOTKNOT_SUPPORT)), yes)
TEST_SRC_FILES += \
     src/test/ftm_hotknot.cpp
endif
### hotknot

ifeq ($(strip $(BOARD_USES_MTK_AUDIO)),true)
TEST_SRC_FILES += \
	src/test/ftm_audio.cpp\
	src/test/ftm_audio_debug.cpp\
	src/test/ftm_audio_Common.cpp\
	src/test/ftm_headset.cpp\
	src/test/ftm_vibrator.c\
	src/test/ftm_speaker.cpp
endif

ifeq ($(MTK_WLAN_SUPPORT), yes)
TEST_SRC_FILES += \
	src/test/ftm_wifi.c \
	src/test/ftm_wifi_op.c
endif

ifeq ($(HAVE_CMMB_FEATURE),yes)
TEST_SRC_FILES += \
	src/test/ftm_cmmb_impl.cpp \
	src/test/ftm_cmmb.c
endif


ifeq ($(MTK_GPS_SUPPORT),yes)
TEST_SRC_FILES += \
	src/test/ftm_gps.c
endif

ifeq ($(MTK_NFC_SUPPORT),yes)
TEST_SRC_FILES += \
	src/test/ftm_nfc.c
endif

ifeq ($(strip $(BOARD_USES_MTK_AUDIO)),true)
ifeq ($(MTK_FM_SUPPORT), yes)
ifeq ($(MTK_FM_RX_SUPPORT), yes)
TEST_SRC_FILES += \
	src/test/ftm_fm.c
endif
ifeq ($(MTK_FM_TX_SUPPORT), yes)
TEST_SRC_FILES += \
	src/test/ftm_fmtx.c
endif
endif
endif

ifeq ($(HAVE_MATV_FEATURE),yes)

ifeq ($(MTK_MATV_ANALOG_SUPPORT),yes)
LOCAL_CFLAGS += \
  -DANALOG_AUDIO
endif

TEST_SRC_FILES += \
  src/test/ftm_matv_auto.cpp \
  src/test/ftm_matv_preview.cpp \
  src/test/ftm_matv_common.cpp

# copy resources to rootfs/res for non-factory image mode

copy_to := \
  res/matv/matv_pattern.jpg

copy_from := \
  factory/

copy_to := $(addprefix $(TARGET_OUT)/,$(copy_to))
copy_from_custom := $(addprefix $(MTK_PATH_CUSTOM)/,$(copy_from))

$(copy_to) : $(TARGET_OUT)/% : $(copy_from_custom)/% | $(ACP)
	@if [ ! -h $(TARGET_ROOT_OUT)/res ]; then mkdir -p $(TARGET_ROOT_OUT); ln -s /system/res $(TARGET_ROOT_OUT)/res || echo "Makelink failed !!" ;fi
	$(transform-prebuilt-to-target)
#ALL_PREBUILT += $(copy_to)

endif

ifeq ($(findstring MTK_AOSP_ENHANCEMENT,  $(COMMON_GLOBAL_CPPFLAGS)),)
	LOCAL_CFLAGS += -DMTK_BASIC_PACKAGE
endif

ifeq ($(MTK_BT_SUPPORT), yes)
TEST_SRC_FILES += \
	src/test/ftm_bt.c\
	src/test/ftm_bt_op.c
endif

ifeq ($(MTK_EMMC_SUPPORT),yes)
TEST_SRC_FILES += \
	src/test/ftm_emmc.c
else
TEST_SRC_FILES += \
	src/test/ftm_flash.c
endif

ifeq ($(MTK_NCP1851_SUPPORT),yes)
TEST_SRC_FILES += \
	src/test/ftm_battery_ncp1851.c
else
  ifeq ($(MTK_BQ24196_SUPPORT),yes)
  TEST_SRC_FILES += \
	  src/test/ftm_battery_bq24196.c
else
TEST_SRC_FILES += \
  src/test/ftm_battery.c\
  src/test/ftm_ext_buck.c\
  src/test/ftm_ext_vbat_boost.c
endif
endif

ifeq ($(MTK_HDMI_SUPPORT), yes)
LOCAL_CFLAGS += -DFEATURE_FTM_HDMI

TEST_SRC_FILES += \
  src/test/ftm_hdmi.c
endif

HAVE_CUST_INC_PATH := $(shell test -d $(MTK_PATH_CUSTOM)/factory/inc && echo yes)

ifeq ($(HAVE_CUST_INC_PATH),yes)
  $(info Apply factory custom include path for $(TARGET_DEVICE))
else
  $(info No factory custom include path for $(TARGET_DEVICE))
endif

ifeq ($(HAVE_CUST_INC_PATH),yes)
	LOCAL_CUST_INC_PATH := $(CUSTOM_PATH)/inc
else
	LOCAL_CUST_INC_PATH := $(GENERIC_CUSTOM_PATH)/inc
endif

ifeq ($(MTK_SENSOR_SUPPORT),yes)
LOCAL_CFLAGS += \
    -DMTK_SENSOR_SUPPORT

ifeq ($(MTK_SENSOR_MAGNETOMETER),yes)
LOCAL_CFLAGS += \
    -DMTK_SENSOR_MAGNETOMETER
endif

ifeq ($(MTK_SENSOR_ACCELEROMETER),yes)
LOCAL_CFLAGS += \
    -DMTK_SENSOR_ACCELEROMETER
endif

ifeq ($(MTK_SENSOR_ALSPS),yes)
LOCAL_CFLAGS += \
    -DMTK_SENSOR_ALSPS
endif

ifeq ($(MTK_SENSOR_GYROSCOPE),yes)
LOCAL_CFLAGS += \
    -DMTK_SENSOR_GYROSCOPE
endif

endif

ifeq ($(MTK_TB_WIFI_3G_MODE),3GDATA_ONLY)
LOCAL_CFLAGS += \
    -DMTK_TB_WIFI_3G_MODE_3GDATA_ONLY
endif

ifeq ($(MTK_TB_WIFI_3G_MODE),3GDATA_SMS)
LOCAL_CFLAGS += \
    -DMTK_TB_WIFI_3G_MODE_3GDATA_SMS
endif

ifeq ($(MTK_TB_WIFI_3G_MODE),WIFI_ONLY)
LOCAL_CFLAGS += \
    -DMTK_TB_WIFI_ONLY
endif


ifeq ($(MTK_NFC_SUPPORT),yes)
LOCAL_CFLAGS += \
    -DMTK_NFC_SUPPORT_FM
ifeq ($(MTK_NFC_SE_NUM),0)
LOCAL_CFLAGS += \
    -DMTK_NFC_NO_SE
endif
ifeq ($(MTK_NFC_SE_NUM),1)
LOCAL_CFLAGS += \
    -DMTK_NFC_SE_SIM1
endif
ifeq ($(MTK_NFC_SE_NUM),2)
LOCAL_CFLAGS += \
    -DMTK_NFC_SE_SIM2
endif
ifeq ($(MTK_NFC_SE_NUM),3)
LOCAL_CFLAGS += \
    -DMTK_NFC_SE_SIM1
LOCAL_CFLAGS += \
   -DMTK_NFC_SE_SIM2
endif
ifeq ($(MTK_NFC_SE_NUM),4)
LOCAL_CFLAGS += \
   -DMTK_NFC_SE_SD
endif
ifeq ($(MTK_NFC_SE_NUM),5)
LOCAL_CFLAGS += \
   -DMTK_NFC_SE_SIM1
LOCAL_CFLAGS += \
    -DMTK_NFC_SE_SD
endif
ifeq ($(MTK_NFC_SE_NUM),6)
LOCAL_CFLAGS += \
    -DMTK_NFC_SE_SIM2
LOCAL_CFLAGS += \
    -DMTK_NFC_SE_SD
endif
ifeq ($(MTK_NFC_SE_NUM),7)
LOCAL_CFLAGS += \
    -DMTK_NFC_SE_SIM1
LOCAL_CFLAGS += \
    -DMTK_NFC_SE_SIM2
LOCAL_CFLAGS += \
    -DMTK_NFC_SE_SD
endif
endif

ifeq (yes,$(GEMINI))
    LOCAL_CFLAGS += -DGEMINI
endif

ifeq ($(MTK_DIGITAL_MIC_SUPPORT),yes)
  LOCAL_CFLAGS += -DMTK_DIGITAL_MIC_SUPPORT
endif

ifeq ($(MTK_VIBSPK_SUPPORT),yes)
   LOCAL_CFLAGS += -DMTK_VIBSPK_SUPPORT
endif
#MTKBEGIN   [mtk80625][DualTalk]
ifeq ($(MTK_DT_SUPPORT),yes)
LOCAL_CFLAGS += -DMTK_DT_SUPPORT
endif

#MTKEND   [mtk80625][DualTalk]
ifeq ($(MTK_SPEAKER_MONITOR_SUPPORT),yes)
   LOCAL_CFLAGS += -DMTK_SPEAKER_MONITOR_SUPPORT
endif

ifeq ($(MTK_AUDIO_BLOUD_CUSTOMPARAMETER_REV),MTK_AUDIO_BLOUD_CUSTOMPARAMETER_V5)
  LOCAL_CFLAGS += -DMTK_AUDIO_BLOUD_CUSTOMPARAMETER_V5
else
  ifeq ($(strip $(MTK_AUDIO_BLOUD_CUSTOMPARAMETER_REV)),MTK_AUDIO_BLOUD_CUSTOMPARAMETER_V4)
    LOCAL_CFLAGS += -DMTK_AUDIO_BLOUD_CUSTOMPARAMETER_V4
  endif
endif

include $(LOCAL_PATH)/src/miniui/font.mk

LOCAL_SRC_FILES := \
	$(CORE_SRC_FILES)\
	$(TEST_SRC_FILES)

LOCAL_C_INCLUDES:= \
	$(LOCAL_PATH)/inc/ \
	$(MTK_PATH_COMMON)/factory/inc \
	$(MTK_PATH_CUSTOM)/kernel/flashlight/inc \
	$(MTK_PATH_CUSTOM_PLATFORM)/kernel/flashlight/inc \
        $(MTK_PATH_CUSTOM_PLATFORM)/hal/inc \
	$(MTK_PATH_SOURCE)/frameworks/av/media/libs \
	$(LOCAL_CUST_INC_PATH) \
	$(MTK_PATH_SOURCE)/external/mhal/src/custom/inc \
	$(MTK_PATH_SOURCE)/external/mhal/inc \
	frameworks/av/include/media \
	$(MTK_PATH_SOURCE)/hardware/audio/common/include \
	$(call include-path-for, audio-utils) \
	$(call include-path-for, audio-effects) \
	$(MTK_PATH_SOURCE)/external/audiocustparam \
	$(MTK_PATH_SOURCE)/frameworks-ext/av/include \
	$(MTK_PATH_CUSTOM)/hal/audioflinger/audio \
	$(MTK_PATH_SOURCE)/external/nvram/libnvram \
	$(MTK_PATH_SOURCE)/external/AudioSpeechEnhancement/inc \
	$(MTK_PATH_SOURCE)/external/matvctrl \
	$(MTK_PATH_SOURCE)/external/fft \
	$(MTK_PATH_SOURCE)/external/sensor-tools \
	$(MTK_PATH_SOURCE)/external/aee/binary/inc \
	$(MTK_PATH_SOURCE)/kernel/drivers/video \
	$(TOP)/$(MTK_PATH_PLATFORM)/kernel/drivers/hdmitx \
	$(MTK_PATH_CUSTOM)/hal/inc \
	$(MTK_PATH_SOURCE)/external/audiodcremoveflt \
	$(MTK_ROOT_CUSTOM_OUT)/kernel/dct \
	$(MTK_PATH_CUSTOM)/kernel/dct \
	system/extras/ext4_utils \
	$(MTK_PATH_SOURCE)/external/AudioCompensationFilter \
	$(MTK_PATH_SOURCE)/external/cvsd_plc_codec \
	$(MTK_PATH_SOURCE)/external/msbc_codec \
	$(TOP)/kernel \
	$(MTK_ROOT)/external/AudioComponentEngine \
	$(TOPDIR)/hardware/libhardware_legacy/include \
	$(TOPDIR)/hardware/libhardware/include \
	$(MTK_ROOT_CUSTOM_OUT)/hal/audioflinger \
	$(MTK_ROOT_CUSTOM_OUT)/hal/audioflinger/audio \
	$(MTK_PATH_SOURCE)/hardware/ccci/include


LOCAL_MODULE := factory

LOCAL_MODULE_TAGS := optional

LOCAL_STATIC_LIBRARIES :=
LOCAL_STATIC_LIBRARIES += libminzip libmtdutil libmincrypt libm
LOCAL_STATIC_LIBRARIES += libpng libz libcutils libselinux
#LOCAL_STATIC_LIBRARIES += libstdc++ libc libunz
LOCAL_STATIC_LIBRARIES += libfft

## ==> HAVE_CMMB_FEATURE
ifeq ($(HAVE_CMMB_FEATURE),yes)

LOCAL_C_INCLUDES += \
   $(TOP)/$(MTK_PATH_PLATFORM)/external/meta/cmmb/include \
   $(TOP)/$(MTK_PATH_PLATFORM)/external/meta/cmmb

ifneq (,$(findstring Innofidei,$(MTK_CMMB_CHIP)))
LOCAL_C_INCLUDES += \
        $(TOP)/$(MTK_PATH_PLATFORM)/external/meta/cmmb/innofidei
LOCAL_CFLAGS += -DCMMB_CHIP_INNO
# siano chip used
else
LOCAL_C_INCLUDES += \
        $(TOP)/$(MTK_PATH_PLATFORM)/external/meta/cmmb/siano \
        $(TOP)/$(MTK_PATH_PLATFORM)/external/meta/cmmb/siano/hostlib \
        $(TOP)/$(MTK_PATH_PLATFORM)/external/meta/cmmb/siano/osw/include \
        $(TOP)/$(MTK_PATH_PLATFORM)/external/meta/cmmb/siano/osw/linux \
        $(TOP)/$(MTK_PATH_PLATFORM)/external/meta/cmmb/siano/siano_appdriver_new
endif


endif
## <== HAVE_CMMB_FEATURE

LOCAL_SHARED_LIBRARIES:= libc libcutils libnvram libdl libhwm libaudiocustparam libext4_utils libfile_op libcustom_nvram
LOCAL_SHARED_LIBRARIES += libjpeg libminiui
ifeq ($(MTK_C2K_SUPPORT), yes)
LOCAL_SHARED_LIBRARIES += libc2kutils 
endif

LOCAL_WHOLE_STATIC_LIBRARIES := libmedia_helper

LOCAL_CFLAGS += -D$(MTK_PLATFORM)

#camera{

LOCAL_WHOLE_STATIC_LIBRARIES +=
LOCAL_STATIC_LIBRARIES += libacdk_entry_mdk


LOCAL_C_INCLUDES += $(TOP)/$(MTK_MTKCAM_PLATFORM)/core/featureio/drv/inc
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/include
LOCAL_C_INCLUDES += $(TOP)/$(MTKCAM_C_INCLUDES)
ifeq ($(IS_LEGACY), 0)
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam/acdk/common/include
endif
ifeq ($(MTK_NFC_PACKAGE),AOSP_B)
#$(info A MTK_PATH_SOURCE=$(TOP)/$(MTK_PATH_SOURCE)/hardware/nfc/inc)
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/nfc/inc
else
#$(info B MTK_PATH_SOURCE=$(TOP)/$(MTK_PATH_SOURCE))
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/external/mtknfc/inc
endif
ifeq ($(MTK_C2K_SUPPORT), yes)
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/include/c2k
endif
#}camera

LOCAL_C_INCLUDES += system/core/fs_mgr/include

LOCAL_STATIC_LIBRARIES += libfs_mgr

LOCAL_SHARED_LIBRARIES += libutils
ifeq ($(HAVE_MATV_FEATURE),yes)
LOCAL_STATIC_LIBRARIES +=
LOCAL_SHARED_LIBRARIES += libmatv_cust
LOCAL_CFLAGS += -DHAVE_MATV_FEATURE

LOCAL_SHARED_LIBRARIES += libjpeg

LOCAL_C_INCLUDES += \
  external/jpeg

endif
#ifeq ($(MTK_NFC_SUPPORT),yes)
#LOCAL_SHARED_LIBRARIES += libmtknfc_dynamic_load_jni
#endif

ifeq ($(MTK_DUAL_MIC_SUPPORT),yes)
LOCAL_CFLAG += -DMTK_DUAL_MIC_SUPPORT
LOCAL_CFLAGS += -DFEATURE_FTM_ACSLB
endif

ifeq ($(MTK_ENABLE_MD1),yes)
LOCAL_CFLAGS +=-DMTK_ENABLE_MD1
endif

ifeq ($(MTK_ENABLE_MD2),yes)
LOCAL_CFLAGS +=-DMTK_ENABLE_MD2
endif

ifeq ($(MTK_ENABLE_MD5),yes)
LOCAL_CFLAGS +=-DMTK_ENABLE_MD5
endif

ifeq ($(CUSTOM_KERNEL_SOUND),amp_6329pmic_spk)
LOCAL_CFLAGS += -DFEATURE_FTM_SPK_OC
endif

ifeq ($(MTK_C2K_SUPPORT), yes)
LOCAL_CFLAGS += -DMTK_FTM_C2K_SUPPORT
$(warning MTK_C2K_SUPPORT=yes)
endif

ifeq ($(EVDO_DT_SUPPORT), yes)
LOCAL_CFLAGS += -DEVDO_FTM_DT_SUPPORT
$(warning EVDO_FTM_DT_SUPPORT=yes)
endif

ifeq ($(MTK_SVLTE_SUPPORT), yes)
LOCAL_CFLAGS += -DMTK_FTM_SVLTE_SUPPORT
$(warning MTK_FTM_SVLTE_SUPPORT=yes)
endif

ifeq ($(EVDO_DT_VIA_SUPPORT), yes)
LOCAL_CFLAGS += -DEVDO_FTM_DT_VIA_SUPPORT
$(warning EVDO_FTM_DT_VIA_SUPPORT=yes)
endif

ifeq ($(HAVE_CMMB_FEATURE),yes)
LOCAL_CFLAGS += -DHAVE_CMMB_FEATURE
endif

ifneq ($(RECEIVER_HEADSET_AUTOTEST),no)
LOCAL_CFLAGS += -DRECEIVER_HEADSET_AUTOTEST
endif
#ifeq ($(TARGET_NO_FACTORYIMAGE),true)
#  LOCAL_MODULE_PATH := $(TARGET_ROOT_OUT_SBIN)
#endif

ifeq ($(MTK_WLAN_SUPPORT),yes)
#LOCAL_CFLAGS += \
#    -DWIFI_DRIVER_MODULE_NAME=\"wlan_$(shell echo $(strip $(MTK_WLAN_CHIP)) | tr A-Z a-z)\"
endif

ifneq ($(DISABLE_EARPIECE),yes)
LOCAL_CFLAGS += -DFEATURE_FTM_RECEIVER
endif

include $(BUILD_EXECUTABLE)

# copy etc/init.rc to rootfs/factory_init.rc for non-factory image mode
#$(shell cp $(CUSTOM_PATH)/init.rc $(LOCAL_PATH)/etc/init.rc)
#$(shell chmod 777 $(LOCAL_PATH)/etc/init.rc)

#ifeq ($(TARGET_NO_FACTORYIMAGE),true)
#$(shell echo -e "\nservice factory /system/bin/logwrapper /system/bin/factory\n    oneshot" >> $(LOCAL_PATH)/etc/init.rc)
#include $(CLEAR_VARS)
#LOCAL_MODULE := init.factory.rc
#LOCAL_MODULE_TAGS := optional

#LOCAL_MODULE_CLASS := ETC
#OCAL_SRC_FILES := etc/init.rc
#LOCAL_MODULE_PATH := $(TARGET_ROOT_OUT)
#include $(BUILD_PREBUILT)
#else
#$(shell echo -e "\nservice factory /system/bin/logwrapper /sbin/factory\n    oneshot" >> $(LOCAL_PATH)/etc/init.rc)
#endif

include $(commands_factory_local_path)/src/mtdutil/Android.mk
include $(commands_factory_local_path)/src/miniui/Android.mk
include $(commands_factory_local_path)/src/mmutil/Android.mk

# audio resource

# copy resources to rootfs/res for test pattern
# already defined in build/target/product/common.mk
# Cannot add $(TARGET_ROOT_OUT)/res to ALL_DEFAULT_INSTALLED_MODULES because symbolic link source is not existing


#################################################################
LOCAL_PATH :=  $(CUSTOM_PATH)

include $(CLEAR_VARS)
-include $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam/mtkcam.mk
ifeq ($(TARGET_NO_FACTORYIMAGE),true)

# copy resources to rootfs/res for non-factory image mode

copy_from := \
	res/images/lcd_test_00.png \
	res/images/lcd_test_01.png \
	res/images/lcd_test_02.png

copy_to := $(addprefix $(TARGET_OUT)/,$(copy_from))
ifeq ($(strip $(MTK_GMO_ROM_OPTIMIZE)), yes)
copy_from := $(addprefix $(LOCAL_PATH)/,$(copy_from))
else
copy_from := $(addprefix vendor/mediatek/proprietary/custom/common/factory/,$(copy_from))
endif

$(copy_to) : $(TARGET_OUT)/% : $(LOCAL_PATH)/% | $(ACP)
	@if [ ! -h $(TARGET_ROOT_OUT)/res ]; then mkdir -p $(TARGET_ROOT_OUT); ln -s /system/res $(TARGET_ROOT_OUT)/res || echo "Makelink failed !!" ;fi
	$(transform-prebuilt-to-target)
#ALL_PREBUILT += $(copy_to)
else
$(shell if [ -h $(TARGET_ROOT_OUT)/res ] ;then rm -f $(TARGET_ROOT_OUT)/res ;  fi  )
endif
include $(CLEAR_VARS)
-include $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam/mtkcam.mk
include $(MTK_PATH_SOURCE)/factory/src/miniui/font.mk
LOCAL_MODULE := factory.ini
LOCAL_MODULE_TAGS := optional

ifeq ($(MTK_FACTORY_MODE_IN_GB2312),yes)
	LOCAL_SRC_FILES := factory.chn.ini
else
	LOCAL_SRC_FILES := factory.ini
endif

LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)
include $(BUILD_PREBUILT)
##################################################################
#endif   # TARGET_ARCH == arm

endif
endif
endif

#endif # ifeq ($(TARGET_BUILD_PDK),)


