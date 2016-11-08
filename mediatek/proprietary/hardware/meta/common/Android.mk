ifneq ($(TARGET_SIMULATOR),true)

#libft
LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)


LOCAL_SRC_FILES := src/PortHandle.cpp\
                   src/SerPort.cpp\
                   src/Device.cpp\
                   src/ExternalFunction.cpp

LOCAL_C_INCLUDES := $(LOCAL_PATH)/inc

LOCAL_MODULE:= libft
include $(BUILD_STATIC_LIBRARY)

META_DRIVER_PATH := $(MTK_PATH_SOURCE)/hardware/meta/adaptor

#meta_tst
include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := optional
CORE_SRC_FILES := src/tst_main.cpp

LOCAL_SRC_FILES := \
    $(CORE_SRC_FILES)\
    src/CmdTarget.cpp\
    src/Context.cpp\
    src/Device.cpp\
    src/Frame.cpp\
    src/FtModule.cpp\
    src/MdRxWatcher.cpp\
    src/Modem.cpp\
    src/SerPort.cpp\
    src/UsbRxWatcher.cpp\
    src/PortHandle.cpp\
    src/ExternalFunction.cpp

LOCAL_C_INCLUDES := $(LOCAL_PATH)/inc

MTK_META_AUDIO_SUPPORT := yes
MTK_META_CCAP_SUPPORT := yes
MTK_META_GSENSOR_SUPPORT := yes
MTK_META_MSENSOR_SUPPORT := yes
MTK_META_ALSPS_SUPPORT := yes
MTK_META_GYROSCOPE_SUPPORT := yes
MTK_META_TOUCH_SUPPORT := yes
MTK_META_LCDBK_SUPPORT := yes
MTK_META_KEYPADBK_SUPPORT := yes
MTK_META_LCD_SUPPORT := yes
MTK_META_VIBRATOR_SUPPORT := yes
MTK_META_CPU_SUPPORT := yes
MTK_META_SDCARD_SUPPORT := yes
MTK_META_ADC_SUPPORT := yes
MTK_META_NVRAM_SUPPORT := yes
MTK_META_GPIO_SUPPORT := yes
MTK_META_NFC_SUPPORT := yes
MTK_META_C2K_SUPPORT := yes

#inlcude libft
LOCAL_STATIC_LIBRARIES += libft


#CCCI interface
LOCAL_C_INCLUDES += $(MTK_PATH_SOURCE)/hardware/ccci/include

ifeq ($(MTK_C2K_SUPPORT),yes)
ifeq ($(MTK_META_C2K_SUPPORT),yes)
ifneq ($(MTK_ECCCI_C2K),yes)
LOCAL_C_INCLUDES += $(MTK_PATH_SOURCE)/hardware/c2k/include
LOCAL_SHARED_LIBRARIES += libc2kutils
endif
LOCAL_CFLAGS += \
    -DTST_C2K_SUPPORT
endif
endif

LOCAL_SHARED_LIBRARIES += libdl libhwm libhardware_legacy libmedia libcutils liblog libutils

ifeq ($(MTK_BASIC_PACKAGE), yes)
LOCAL_SHARED_LIBRARIES += \
                          libselinux \
                          libsparse
endif

# DriverInterface Begin

ifeq ($(MTK_WLAN_SUPPORT),yes)
LOCAL_C_INCLUDES += $(META_DRIVER_PATH)/wifi
LOCAL_STATIC_LIBRARIES += libmeta_wifi
LOCAL_SHARED_LIBRARIES += libnetutils
LOCAL_CFLAGS += \
    -DFT_WIFI_FEATURE
endif

ifeq ($(MTK_GPS_SUPPORT),yes)
LOCAL_C_INCLUDES += $(META_DRIVER_PATH)/gps
LOCAL_STATIC_LIBRARIES += libmeta_gps
LOCAL_CFLAGS += \
    -DFT_GPS_FEATURE
endif

ifeq ($(MTK_META_NFC_SUPPORT),yes)
ifeq ($(MTK_NFC_SUPPORT),yes)
LOCAL_C_INCLUDES += $(META_DRIVER_PATH)/nfc

LOCAL_STATIC_LIBRARIES += libmeta_nfc
AOSP_NFC_PATH=$(strip $(wildcard $(MTK_PATH_SOURCE)/hardware/nfc/Android.mk))
MTK_NFC_PATH=$(strip $(wildcard $(MTK_PATH_SOURCE)/external/mtknfc/Android.mk))

ifneq ($(MTK_NFC_PATH),null)
    LOCAL_C_INCLUDES += $(MTK_PATH_SOURCE)/external/mtknfc/inc
else
    LOCAL_C_INCLUDES += $(MTK_PATH_SOURCE)/hardware/nfc/inc
endif
LOCAL_CFLAGS += \
    -DFT_NFC_FEATURE
endif
endif

ifeq ($(MTK_BT_SUPPORT),yes)
LOCAL_C_INCLUDES += $(META_DRIVER_PATH)/bluetooth
LOCAL_STATIC_LIBRARIES += libmeta_bluetooth
LOCAL_CFLAGS += \
    -DFT_BT_FEATURE
endif

ifeq ($(MTK_FM_SUPPORT),yes)
LOCAL_C_INCLUDES += $(META_DRIVER_PATH)/fm
LOCAL_STATIC_LIBRARIES += libmeta_fm
LOCAL_CFLAGS += \
    -DFT_FM_FEATURE
endif

ifeq ($(MTK_META_AUDIO_SUPPORT),yes)
LOCAL_C_INCLUDES += $(META_DRIVER_PATH)/Audio
#LOCAL_SHARED_LIBRARIES += libaudio.primary.default
LOCAL_STATIC_LIBRARIES += libmeta_audio
LOCAL_CFLAGS += \
    -DFT_AUDIO_FEATURE
endif

ifeq ($(MTK_META_CCAP_SUPPORT),yes)
#temp mark, need add constraint after adding D2 folder

ifeq ($(strip $(TARGET_BOARD_PLATFORM)),mt6735m)
LOCAL_C_INCLUDES += $(META_DRIVER_PATH)/cameratool/$(shell echo $(MTK_PLATFORM) | tr A-Z a-z)/D2/CCAP\
                    $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam/inc/acdk \
                    $(TOP)/$(MTK_PATH_SOURCE)/hardware/camera/inc/acdk\
                    $(TOP)/$(MTK_MTKCAM_PLATFORM)/inc/acdk \
                    $(TOP)/$(MTK_MTKCAM_PLATFORM)/include \
                    $(TOP)/$(MTK_MTKCAM_PLATFORM)/acdk/inc/cct\
                    $(TOP)/$(MTKCAM_C_INCLUDES)/.. \
                    $(TOP)/$(MTKCAM_C_INCLUDES) \
                    $(TOP)/$(MTK_PATH_SOURCE)/hardware/include\
                    $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam/include\
                    $(TOP)/$(MTK_PATH_PLATFORM)/hardware/include/D2 \
                    $(TOP)/$(MTK_PATH_COMMON)/kernel/imgsensor/inc \
                    $(MTK_PATH_CUSTOM_PLATFORM)/hal/D2/inc \
                    $(MTK_PATH_CUSTOM_PLATFORM)/hal/D2/inc\aaa \

else ifeq ($(strip $(TARGET_BOARD_PLATFORM)),$(filter $(TARGET_BOARD_PLATFORM),mt6735 mt6753))
LOCAL_C_INCLUDES += $(META_DRIVER_PATH)/cameratool/$(shell echo $(MTK_PLATFORM) | tr A-Z a-z)/D1/CCAP \
                    $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam/inc/acdk \
                    $(TOP)/$(MTK_MTKCAM_PLATFORM)/inc/acdk \
                    $(TOP)/$(MTK_MTKCAM_PLATFORM)/acdk/inc/cct\
                    $(TOP)/$(MTK_MTKCAM_PLATFORM) \
                    $(TOP)/$(MTK_MTKCAM_PLATFORM)/include \
                    $(TOP)/$(MTKCAM_C_INCLUDES)/..\
                    $(TOP)/$(MTKCAM_C_INCLUDES) \
                    $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam/include\
                    $(TOP)/$(MTK_PATH_COMMON)/kernel/imgsensor/inc \
                    $(MTK_PATH_CUSTOM_PLATFORM)/hal/D1/inc \
                    $(MTK_PATH_CUSTOM_PLATFORM)/hal/D1/inc/aaa \
                    $(MTK_PATH_SOURCE)/hardware/mtkcam/legacy/platform/mt6735/acdk/inc/cct \
                    $(MTK_PATH_SOURCE)/hardware/mtkcam/legacy/platform/mt6735/acdk/inc/acdk \
                    $(MTK_PATH_SOURCE)/hardware/mtkcam/legacy/platform/mt6735/acdk/inc

else ifeq ($(strip $(TARGET_BOARD_PLATFORM)),$(filter $(TARGET_BOARD_PLATFORM),mt6797))
LOCAL_C_INCLUDES += $(META_DRIVER_PATH)/cameratool/$(shell echo $(MTK_PLATFORM) | tr A-Z a-z)/CCAP \
                    $(MTK_PATH_SOURCE)/custom/common/kernel/imgsensor/inc \
                    $(MTK_PATH_SOURCE)/hardware/meta/common/inc \
                    $(MTK_PATH_SOURCE)/hardware/include \
                    $(MTKCAM_DRV_INCLUDE) \
                    $(MTK_PATH_SOURCE)/hardware/mtkcam/acdk/common/include \
                    $(MTK_PATH_SOURCE)/hardware/mtkcam/acdk/common/include/acdk \
                    $(MTK_PATH_SOURCE)/hardware/mtkcam/acdk/$(TARGET_BOARD_PLATFORM)/inc \
                    $(MTK_PATH_SOURCE)/hardware/mtkcam/acdk/$(TARGET_BOARD_PLATFORM)/inc/cct \
                    $(MTK_PATH_SOURCE)/hardware/meta/adaptor/cameratool/$(TARGET_BOARD_PLATFORM)/CCAP \
                    $(MTK_PATH_CUSTOM_PLATFORM)/hal/inc \
                    $(MTK_PATH_CUSTOM_PLATFORM)/hal/inc/aaa \
                    $(TOP)/$(MTK_PATH_COMMON)/kernel/imgsensor/inc

else
LOCAL_C_INCLUDES += $(META_DRIVER_PATH)/cameratool/$(shell echo $(MTK_PLATFORM) | tr A-Z a-z)/CCAP\
                    $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam/inc/acdk \
                    $(TOP)/$(MTK_PATH_PLATFORM)/hardware/mtkcam/inc/acdk \
                    $(TOP)/$(MTK_PATH_PLATFORM)/hardware/mtkcam/acdk/inc/cct \
                    $(MTK_PATH_SOURCE)/hardware/mtkcam/legacy/platform/$(shell echo $(MTK_PLATFORM) | tr A-Z a-z)/acdk/inc/cct \
                    $(MTK_PATH_SOURCE)/hardware/mtkcam/legacy/platform/$(shell echo $(MTK_PLATFORM) | tr A-Z a-z)/include \
                    $(TOP)/$(MTK_PATH_SOURCE)/hardware/include\
                    $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam/legacy/include\
                    $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam/legacy/include/mtkcam\
                    $(TOP)/$(MTK_PATH_PLATFORM)/hardware/include\
                    $(TOP)/$(MTK_PATH_COMMON)/kernel/imgsensor/inc \
                    $(MTK_PATH_CUSTOM_PLATFORM)/hal/inc \
                    $(MTK_PATH_CUSTOM_PLATFORM)/hal/inc/aaa \
                    $(MTKCAM_DRV_INCLUDE) \
                    $(MTK_PATH_SOURCE)/hardware/mtkcam/drv/include/$(shell echo $(MTK_PLATFORM) | tr A-Z a-z) \
                    $(MTK_PATH_SOURCE)/hardware/mtkcam/acdk/$(shell echo $(MTK_PLATFORM) | tr A-Z a-z)/inc/cct \
                    $(MTK_PATH_SOURCE)/hardware/mtkcam/acdk/common/include \
                    $(MTK_PATH_SOURCE)/hardware/mtkcam/legacy/platform/$(shell echo $(MTK_PLATFORM) | tr A-Z a-z)/acdk/inc/cct \
                    $(MTK_PATH_SOURCE)/hardware/mtkcam/legacy/platform/$(shell echo $(MTK_PLATFORM) | tr A-Z a-z)/acdk/inc/acdk \
                    $(MTK_PATH_SOURCE)/hardware/mtkcam/legacy/platform/$(shell echo $(MTK_PLATFORM) | tr A-Z a-z)/acdk/inc
endif

LOCAL_STATIC_LIBRARIES += libccap
LOCAL_CFLAGS += \
    -DFT_CCAP_FEATURE
endif

ifeq ($(MTK_META_GSENSOR_SUPPORT),yes)
LOCAL_C_INCLUDES += $(META_DRIVER_PATH)/gsensor
LOCAL_STATIC_LIBRARIES += libmeta_gsensor
LOCAL_CFLAGS += \
    -DFT_GSENSOR_FEATURE
endif

ifeq ($(MTK_META_MSENSOR_SUPPORT),yes)
LOCAL_C_INCLUDES += $(META_DRIVER_PATH)/msensor
LOCAL_STATIC_LIBRARIES += libmeta_msensor
LOCAL_CFLAGS += \
    -DFT_MSENSOR_FEATURE
endif

ifeq ($(MTK_META_ALSPS_SUPPORT),yes)
LOCAL_C_INCLUDES += $(META_DRIVER_PATH)/alsps
LOCAL_STATIC_LIBRARIES += libmeta_alsps
LOCAL_CFLAGS += \
    -DFT_ALSPS_FEATURE
endif

ifeq ($(MTK_META_GYROSCOPE_SUPPORT),yes)
LOCAL_C_INCLUDES += $(META_DRIVER_PATH)/gyroscope
LOCAL_STATIC_LIBRARIES += libmeta_gyroscope
LOCAL_CFLAGS += \
    -DFT_GYROSCOPE_FEATURE
endif

ifeq ($(MTK_META_TOUCH_SUPPORT),yes)
LOCAL_C_INCLUDES += $(META_DRIVER_PATH)/touch
LOCAL_STATIC_LIBRARIES += libmeta_touch
LOCAL_CFLAGS += \
    -DFT_TOUCH_FEATURE
endif

ifeq ($(MTK_META_LCDBK_SUPPORT),yes)
LOCAL_C_INCLUDES += $(META_DRIVER_PATH)/LCDBK
LOCAL_STATIC_LIBRARIES += libmeta_lcdbk
LOCAL_CFLAGS += \
    -DFT_LCDBK_FEATURE
endif

ifeq ($(MTK_META_KEYPADBK_SUPPORT),yes)
LOCAL_C_INCLUDES += $(META_DRIVER_PATH)/keypadbk
LOCAL_STATIC_LIBRARIES += libmeta_keypadbk
LOCAL_CFLAGS += \
    -DFT_KEYPADBK_FEATURE
endif

ifeq ($(MTK_META_LCD_SUPPORT),yes)
LOCAL_C_INCLUDES += $(META_DRIVER_PATH)/lcd
LOCAL_STATIC_LIBRARIES += libmeta_lcd
LOCAL_CFLAGS += \
    -DFT_LCD_FEATURE
endif

ifeq ($(MTK_META_VIBRATOR_SUPPORT),yes)
LOCAL_C_INCLUDES += $(META_DRIVER_PATH)/vibrator
LOCAL_STATIC_LIBRARIES += libmeta_vibrator
LOCAL_CFLAGS += \
    -DFT_VIBRATOR_FEATURE
endif

ifeq ($(MTK_META_SDCARD_SUPPORT),yes)
LOCAL_C_INCLUDES += $(META_DRIVER_PATH)/sdcard
LOCAL_STATIC_LIBRARIES += libmeta_sdcard
LOCAL_CFLAGS += \
    -DFT_SDCARD_FEATURE
endif

ifeq ($(MTK_EMMC_SUPPORT),yes)
LOCAL_C_INCLUDES += $(META_DRIVER_PATH)/emmc\
            $(META_DRIVER_PATH)/cryptfs

LOCAL_STATIC_LIBRARIES += libmeta_clr_emmc \
                          libext4_utils_static \
                          libmeta_cryptfs\
                          libstorageutil \
                          libselinux \
                          libsparse_static \
                          libz
LOCAL_CFLAGS += \
    -DFT_EMMC_FEATURE
LOCAL_CFLAGS += \
    -DFT_CRYPTFS_FEATURE
endif

ifeq ($(MTK_META_ADC_SUPPORT),yes)
LOCAL_C_INCLUDES += $(META_DRIVER_PATH)/ADC
LOCAL_STATIC_LIBRARIES += libmeta_adc_old
LOCAL_CFLAGS += \
    -DFT_ADC_FEATURE
endif

ifeq ($(MTK_DX_HDCP_SUPPORT),yes)
LOCAL_C_INCLUDES += $(META_DRIVER_PATH)/hdcp
LOCAL_SHARED_LIBRARIES += libDxHdcp
LOCAL_STATIC_LIBRARIES += libmeta_hdcp
LOCAL_CFLAGS += \
    -DFT_HDCP_FEATURE
endif

ifeq ($(TRUSTONIC_TEE_SUPPORT), yes)
ifeq ($(MTK_DRM_KEY_MNG_SUPPORT), yes)
LOCAL_CFLAGS += -DFT_DRM_KEY_MNG_FEATURE
LOCAL_C_INCLUDES += $(MTK_PATH_SOURCE)/trustzone/trustonic/source/trustlets/keyinstall/common/TlcKeyInstall/public
LOCAL_SHARED_LIBRARIES += liburee_meta_drmkeyinstall
endif
endif

ifeq ($(strip $(MTK_IN_HOUSE_TEE_SUPPORT)),yes)
ifeq ($(strip $(MTK_DRM_KEY_MNG_SUPPORT)), yes)
LOCAL_CFLAGS += -DFT_DRM_KEY_MNG_FEATURE
LOCAL_C_INCLUDES += $(META_DRIVER_PATH)/drmkey
LOCAL_SHARED_LIBRARIES += libtz_uree liburee_meta_drmkeyinstall_v2
LOCAL_STATIC_LIBRARIES += liburee_meta_drmkey_if
endif
endif

ifeq ($(strip $(MTK_META_NVRAM_SUPPORT)),yes)
LOCAL_STATIC_LIBRARIES += libfft
LOCAL_SHARED_LIBRARIES += libnvram
LOCAL_SHARED_LIBRARIES += libfile_op
LOCAL_C_INCLUDES += $(MTK_PATH_SOURCE)/external/nvram/libfile_op
LOCAL_C_INCLUDES += $(META_DRIVER_PATH)/Meta_APEditor
LOCAL_STATIC_LIBRARIES += libmeta_apeditor
LOCAL_CFLAGS += \
    -DFT_NVRAM_FEATURE
endif

ifeq ($(strip $(MTK_META_GPIO_SUPPORT)),yes)
LOCAL_C_INCLUDES += $(META_DRIVER_PATH)/gpio
LOCAL_STATIC_LIBRARIES += libmeta_gpio
LOCAL_CFLAGS += \
    -DFT_GPIO_FEATURE
endif


ifeq ($(GEMINI),yes)
LOCAL_CFLAGS += \
    -DGEMINI
endif

ifeq ($(MTK_GEMINI_3SIM_SUPPORT),yes)
LOCAL_CFLAGS += \
    -DMTK_GEMINI_3SIM_SUPPORT
endif

ifeq ($(MTK_GEMINI_4SIM_SUPPORT),yes)
LOCAL_CFLAGS += \
    -DMTK_GEMINI_4SIM_SUPPORT
endif

ifeq ($(MTK_SPEAKER_MONITOR_SUPPORT),yes)
LOCAL_CFLAGS += \
    -DMTK_SPEAKER_MONITOR_SUPPORT
endif

ifeq ($(MTK_ENABLE_MD1),yes)
LOCAL_CFLAGS +=-DMTK_ENABLE_MD1
endif

ifeq ($(MTK_ENABLE_MD2),yes)
LOCAL_CFLAGS +=-DMTK_ENABLE_MD2
endif

ifeq ($(MTK_ENABLE_MD3),yes)
LOCAL_CFLAGS +=-DMTK_ENABLE_MD3
endif

ifeq ($(MTK_ENABLE_MD5),yes)
LOCAL_CFLAGS +=-DMTK_ENABLE_MD5
endif

ifeq ($(MTK_DT_SUPPORT),yes)
LOCAL_CFLAGS += -DMTK_DT_SUPPORT
endif

ifeq ($(MTK_EXTMD_NATIVE_DOWNLOAD_SUPPORT),yes)
LOCAL_CFLAGS += -DMTK_EXTMD_NATIVE_DOWNLOAD_SUPPORT
endif

ifeq ($(MTK_C2K_SUPPORT),yes)
LOCAL_CFLAGS += -DMTK_C2K_SUPPORT
endif

ifneq ($(MTK_EXTERNAL_MODEM_SLOT),0)
LOCAL_CFLAGS += -DMTK_EXTERNAL_MODEM
endif

ifeq ($(MTK_ECCCI_C2K),yes)
LOCAL_CFLAGS += -DMTK_ECCCI_C2K
endif
# DriverInterface End

LOCAL_ALLOW_UNDEFINED_SYMBOLS := true
LOCAL_MODULE:=meta_tst


include $(BUILD_EXECUTABLE)

endif   # !TARGET_SIMULATOR


