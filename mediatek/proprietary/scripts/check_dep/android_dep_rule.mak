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

# note: modem makefile operation is moved from mediatek/build/libs/custom.mk for AOSP use
# get modem feature option
ifneq ($(strip $(CUSTOM_MODEM)),)
  mtk_modem_file := $(wildcard $(foreach m,$(CUSTOM_MODEM),$(MTK_PATH_CUSTOM)/modem/$(m)/modem_*_*_*.mak $(MTK_ROOT)/modem/$(m)/modem_*_*_*.mak))
  mtk_modem_feature_md :=
  mtk_modem_feature_ap :=
  ifeq ($(mtk_modem_file),)
    $(warning Warning: Invalid CUSTOM_MODEM = $(CUSTOM_MODEM) in ProjectConfig.mk)
    $(warning Cannot find $(foreach m,$(CUSTOM_MODEM),$(MTK_PATH_CUSTOM)/modem/$(m)) for modem_*_*_*.mak)
  endif
  $(info mtk_modem_file = $(mtk_modem_file))
  $(foreach modem_make,$(mtk_modem_file),\
	$(eval modem_name_x_yy_z := $(subst modem,MODEM,$(notdir $(basename $(modem_make)))))\
	$(eval modem_name_x := $(word 1,$(subst _, ,$(modem_name_x_yy_z)))_$(word 2,$(subst _, ,$(modem_name_x_yy_z))))\
	$(eval modem_text := $(shell cat $(modem_make) | sed -e '/^\s*#/'d | awk -F# '{print $$1}' | sed -n '/^\S\+\s*=\s*\S\+/'p | sed -e 's/\s/\?/g' ))\
	$(foreach modem_line,$(modem_text),\
		$(eval modem_option := $(subst ?, ,$(modem_line)))\
		$(eval $(modem_name_x_yy_z)_$(modem_option))\
	)\
	$(foreach modem_line,$(modem_text),\
		$(eval modem_option := $(subst ?, ,$(modem_line)))\
		$(eval modem_feature := $(word 1,$(subst =, ,$(modem_option))))\
		$(eval MODEM_$(modem_feature) := $(sort $(MODEM_$(modem_feature)) $($(modem_name_x_yy_z)_$(modem_feature))))\
		$(eval $(modem_name_x)_$(modem_feature) := $(sort $($(modem_name_x)_$(modem_feature)) $($(modem_name_x_yy_z)_$(modem_feature))))\
		$(eval mtk_modem_feature_md := $(sort $(mtk_modem_feature_md) $(modem_feature)))\
		$(eval mtk_modem_feature_ap := $(sort $(mtk_modem_feature_ap) MODEM_$(modem_feature) $(modem_name_x)_$(modem_feature) $(modem_name_x_yy_z)_$(modem_feature)))\
	)\
  )
  modem_name_x_yy_z :=
  modem_text :=
  modem_option :=
  modem_feature :=
endif

#######################################################
# dependency check between AP side & modem side

ifeq (yes,$(strip $(MTK_TTY_SUPPORT)))
  ifneq ($(filter FALSE,$(strip $(MODEM_CTM_SUPPORT))),)
    $(call dep-err-ona-or-offb,CTM_SUPPORT,MTK_TTY_SUPPORT)
  endif
endif

#######################################################
ifeq (MT6572,$(strip $(MTK_PLATFORM)))
  ifneq (yes,$(strip $(MTK_EMMC_SUPPORT)))
    ifneq (yes,$(strip $(MTK_CACHE_MERGE_SUPPORT)))
      $(call dep-err-ona-or-onb,MTK_CACHE_MERGE_SUPPORT,MTK_EMMC_SUPPORT)
    endif
  endif
endif
###############################################################
ifeq (yes, $(strip $(MTK_FLIGHT_MODE_POWER_OFF_MD)))
  ifneq (yes, $(strip $(MTK_MD_SHUT_DOWN_NT)))
    $(call dep-err-ona-or-offb, MTK_MD_SHUT_DOWN_NT, MTK_FLIGHT_MODE_POWER_OFF_MD)
  endif
endif
###########################################################
ifeq (yes,$(strip $(MTK_DIALER_SEARCH_SUPPORT)))
  ifneq (yes, $(strip $(MTK_SEARCH_DB_SUPPORT)))
     $(call dep-err-ona-or-offb,MTK_SEARCH_DB_SUPPORT,MTK_DIALER_SEARCH_SUPPORT)
  endif
endif
############################################################
# for wapi feature

ifeq (yes,$(strip $(MTK_WAPI_SUPPORT)))
  ifneq (yes, $(strip $(MTK_WLAN_SUPPORT)))
    $(call dep-err-ona-or-offb, MTK_WLAN_SUPPORT, MTK_WAPI_SUPPORT)
  endif
endif

ifeq (yes,$(strip $(MTK_CTA_SUPPORT)))
  ifneq (yes, $(strip $(MTK_WAPI_SUPPORT)))
    $(call dep-err-ona-or-offb, MTK_WAPI_SUPPORT, MTK_CTA_SUPPORT)
  endif
endif

##############################################################
# for matv feature

ifeq (yes,$(strip $(HAVE_MATV_FEATURE)))
  ifeq (,$(strip $(CUSTOM_HAL_MATV)))
     $(call dep-err-common, PLEASE turn off HAVE_MATV_FEATURE or set CUSTOM_HAL_MATV)
  endif
endif

ifeq (yes,$(strip $(HAVE_MATV_FEATURE)))
  ifeq (,$(strip $(CUSTOM_KERNEL_MATV)))
     $(call dep-err-common, PLEASE turn off HAVE_MATV_FEATURE or set CUSTOM_KERNEL_MATV)
  endif
endif

##############################################################
# for gps feature

ifeq (yes,$(strip $(MTK_AGPS_APP)))
  ifeq (no,$(strip $(MTK_GPS_SUPPORT)))
     $(call dep-err-ona-or-offb, MTK_GPS_SUPPORT, MTK_AGPS_APP)
  endif
endif

##############################################################
# for GEMINI feature

ifeq (yes, $(strip $(MTK_MULTISIM_RINGTONE_SUPPORT)))
  ifneq (yes, $(strip $(GEMINI)))
    $(call dep-err-ona-or-offb, GEMINI, MTK_MULTISIM_RINGTONE_SUPPORT)
  endif
endif

##############################################################
# for MTK_FOTA_SUPPORT feature

ifneq (yes, $(strip $(MTK_DM_APP)))
  ifneq (yes, $(strip $(MTK_MDM_SCOMO)))
    ifeq (yes, $(strip $(MTK_FOTA_SUPPORT)))
      $(call dep-err-ona-or-offb, MTK_DM_APP, MTK_FOTA_SUPPORT)
    endif
  endif
endif

ifeq (no, $(strip $(MTK_DM_APP)))
  ifeq (yes, $(strip $(MTK_SCOMO_ENTRY)))
    $(call dep-err-ona-or-offb, MTK_DM_APP, MTK_SCOMO_ENTRY)
  endif
endif

ifeq (no, $(strip $(MTK_FOTA_SUPPORT)))
  ifeq (yes, $(strip $(MTK_FOTA_ENTRY)))
    $(call dep-err-ona-or-offb, MTK_FOTA_SUPPORT, MTK_FOTA_ENTRY)
  endif
endif

##############################################################
# for MtkWeatherProvider

ifeq (yes,$(MTK_WEATHER_WIDGET_APP))
  ifeq (no,$(MTK_WEATHER_PROVIDER_APP))
    $(call dep-err-ona-or-offb, MTK_WEATHER_PROVIDER_APP, MTK_WEATHER_WIDGET_APP)
  endif
endif

##############################################################
# for FD feature

ifeq (yes,$(MTK_FD_SUPPORT))
  ifeq ($(findstring _3g,$(MTK_MODEM_SUPPORT) $(MTK_MD2_SUPPORT)),)
#     $(call dep-err-common, please turn off MTK_FD_SUPPORT or set MTK_MODEM_SUPPORT/MTK_MD2_SUPPORT as modem_3g_tdd/modem_3g_fdd)
  endif
endif


##############################################################
# for SNS feature

ifeq (yes,$(MTK_SNS_SINAWEIBO_APP))
  ifeq (no,$(MTK_SNS_SUPPORT))
     $(call dep-err-ona-or-offb, MTK_SNS_SUPPORT,MTK_SNS_SINAWEIBO_APP)
  endif
endif
#############################################################
# VOLD for partition generation
ifeq (yes, $(strip $(MTK_FAT_ON_NAND)))
  ifneq (yes, $(strip $(MTK_MULTI_STORAGE_SUPPORT)))
     $(call dep-err-ona-or-offb, MTK_MULTI_STORAGE_SUPPORT, MTK_FAT_ON_NAND)
  endif
endif
##############################################################
# for VT voice answer feature

ifeq (yes,$(MTK_PHONE_VT_VOICE_ANSWER))
  ifneq (OP01_SPEC0200_SEGC,$(OPTR_SPEC_SEG_DEF))
     $(call dep-err-seta-or-offb, OPTR_SPEC_SEG_DEF,OP01_SPEC0200_SEGC,MTK_PHONE_VT_VOICE_ANSWER)
  endif
endif
##############################################################
# for BT 

ifeq (no,$(strip $(MTK_BT_SUPPORT)))
  ifeq (yes,$(strip $(MTK_BT_21_SUPPORT)))
    $(call dep-err-ona-or-offb, MTK_BT_SUPPORT, MTK_BT_21_SUPPORT)
  endif
  ifeq (yes,$(strip $(MTK_BT_30_SUPPORT)))
    $(call dep-err-ona-or-offb, MTK_BT_SUPPORT, MTK_BT_30_SUPPORT)
  endif
  ifeq (yes,$(strip $(MTK_BT_30_HS_SUPPORT)))
    $(call dep-err-ona-or-offb, MTK_BT_SUPPORT, MTK_BT_30_HS_SUPPORT)
  endif
  ifeq (yes,$(strip $(MTK_BT_40_SUPPORT)))
    $(call dep-err-ona-or-offb, MTK_BT_SUPPORT, MTK_BT_40_SUPPORT)
  endif
  ifeq (yes,$(strip $(MTK_BT_FM_OVER_BT_VIA_CONTROLLER)))
    $(call dep-err-ona-or-offb, MTK_BT_SUPPORT, MTK_BT_FM_OVER_BT_VIA_CONTROLLER)
  endif
endif

ifeq (no,$(strip $(MTK_BT_40_SUPPORT)))
endif

##############################################################
# for emmc feature
ifneq (yes,$(strip $(MTK_EMMC_SUPPORT)))
  ifeq (yes,$(strip $(MTK_FSCK_TUNE)))
    $(call dep-err-ona-or-offb, MTK_EMMC_SUPPORT, MTK_FSCK_TUNE)
  endif
endif
##############################################################
# for emmc otp
ifeq (yes,$(strip $(MTK_EMMC_SUPPORT_OTP)))
  ifneq (yes,$(strip $(MTK_EMMC_SUPPORT)))
    $(call dep-err-ona-or-offb, MTK_EMMC_SUPPORT, MTK_EMMC_SUPPORT_OTP)
  endif
endif

ifeq (yes,$(strip $(MTK_EMMC_SUPPORT_OTP)))
  ifeq (FALSE,$(strip $(MODEM_OTP_SUPPORT)))
    $(call dep-err-ona-or-offb, MODEM_OTP_SUPPORT, MTK_EMMC_SUPPORT_OTP)
  endif
endif

ifeq (TRUE,$(strip $(MODEM_OTP_SUPPORT)))
  ifneq (yes,$(strip $(MTK_EMMC_SUPPORT_OTP)))
    ifneq (yes,$(strip $(NAND_OTP_SUPPORT)))
      $(call dep-err-ona-or-onb, MTK_EMMC_SUPPORT_OTP,NAND_OTP_SUPPORT)
    endif
  endif
endif

ifeq (yes, $(strip $(MTK_COMBO_NAND_SUPPORT)))
  ifeq (yes, $(strip $(MTK_EMMC_SUPPORT)))
    $(call dep-err-common, Please turn off MTK_COMBO_NAND_SUPPORT or turn off MTK_EMMC_SUPPORT)
  endif
endif
##############################################################
# for NFC feature
ifeq (yes,$(strip $(MTK_NFC_SUPPORT)))
  ifeq (no,$(strip $(CONFIG_NFC_PN544)))
    $(call dep-err-ona-or-offb, CONFIG_NFC_PN544, MTK_NFC_SUPPORT)
  endif
endif

ifeq (no,$(strip $(MTK_NFC_SUPPORT)))
  ifeq (yes,$(strip $(CONFIG_NFC_PN544)))
    $(call dep-err-ona-or-offb, MTK_NFC_SUPPORT, CONFIG_NFC_PN544)
  endif
endif

ifeq (yes,$(strip $(MTK_NFC_SUPPORT)))
  ifeq (no,$(strip $(CONFIG_MTK_NFC)))
    $(call dep-err-ona-or-offb, CONFIG_MTK_NFC, MTK_NFC_SUPPORT)
  endif
endif

ifeq (no,$(strip $(MTK_NFC_SUPPORT)))
  ifeq (yes,$(strip $(CONFIG_MTK_NFC)))
    $(call dep-err-ona-or-offb, MTK_NFC_SUPPORT, CONFIG_MTK_NFC)
  endif
endif

ifeq (no,$(strip $(MTK_NFC_SUPPORT)))
  ifeq (yes,$(strip $(MTK_BEAM_PLUS_SUPPORT)))
    $(call dep-err-ona-or-offb, MTK_NFC_SUPPORT, MTK_BEAM_PLUS_SUPPORT)
  endif
endif

ifneq (yes,$(strip $(MTK_NFC_SUPPORT)))
  ifeq (yes,$(strip $(MTK_NFC_ADDON_SUPPORT)))
    $(call dep-err-ona-or-offb, MTK_NFC_SUPPORT, MTK_NFC_ADDON_SUPPORT)
  endif
endif

ifeq (yes,$(strip $(MTK_WIFIWPSP2P_NFC_SUPPORT)))
  ifneq (yes,$(strip $(MTK_WIFI_P2P_SUPPORT)))
    $(call dep-err-ona-or-offb, MTK_WIFI_P2P_SUPPORT, MTK_WIFIWPSP2P_NFC_SUPPORT)
  endif
  ifneq (yes,$(strip $(MTK_NFC_SUPPORT)))
    $(call dep-err-ona-or-offb, MTK_NFC_SUPPORT, MTK_WIFIWPSP2P_NFC_SUPPORT)
  endif
endif
##############################################################
# for fm feature
ifeq (no,$(strip $(MTK_FM_SUPPORT)))
  ifeq (yes,$(strip $(MTK_FM_RECORDING_SUPPORT)))
    $(call dep-err-ona-or-offb, MTK_FM_SUPPORT, MTK_FM_RECORDING_SUPPORT)
  endif
endif

##############################################################
# for Brazil customization
ifeq (no,$(strip $(MTK_BRAZIL_CUSTOMIZATION)))
  ifeq (yes,$(strip $(MTK_BRAZIL_CUSTOMIZATION_TIM)))
    $(call dep-err-ona-or-offb, MTK_BRAZIL_CUSTOMIZATION, MTK_BRAZIL_CUSTOMIZATION_TIM)
  endif
endif

##############################################################
# for mtk brazil customization feature

ifeq (no,$(strip $(MTK_BRAZIL_CUSTOMIZATION)))
  ifeq (yes,$(strip $(MTK_BRAZIL_CUSTOMIZATION_VIVO)))
    $(call dep-err-common, please set MTK_BRAZIL_CUSTOMIZATION_VIVO=no when MTK_BRAZIL_CUSTOMIZATION=no)
  endif
  ifeq (yes,$(strip $(MTK_BRAZIL_CUSTOMIZATION_CLARO)))
    $(call dep-err-common, please set MTK_BRAZIL_CUSTOMIZATION_CLARO=no when MTK_BRAZIL_CUSTOMIZATION=no)
  endif
endif



##############################################################
# for MD DB
ifeq (no,$(strip $(MTK_MDLOGGER_SUPPORT)))
  ifeq (yes,$(strip $(MTK_INCLUDE_MODEM_DB_IN_IMAGE)))
     $(call dep-err-ona-or-offb, MTK_MDLOGGER_SUPPORT, MTK_INCLUDE_MODEM_DB_IN_IMAGE)
  endif
endif

#############################################################
# MTK_MDM_APP, MTK_DM_APP and MTK_RSDM_APP are exclusive. (Only one can be enabled at the same time.)
ifeq ($(strip $(MTK_MDM_APP)),yes)
  ifeq ($(strip $(MTK_DM_APP)),yes)
     $(call dep-err-offa-or-offb, MTK_MDM_APP, MTK_DM_APP)
  endif
  ifeq ($(strip $(MTK_RSDM_APP)),yes)
     $(call dep-err-offa-or-offb, MTK_MDM_APP, MTK_RSDM_APP)
  endif
endif
###########################################################
ifeq (yes, $(strip $(MTK_MDM_LAWMO)))
  ifneq (yes,$(strip $(MTK_MDM_APP)))
     $(call dep-err-ona-or-offb,MTK_MDM_APP,MTK_MDM_LAWMO)
  endif
endif
ifeq (yes, $(strip $(MTK_MDM_FUMO)))
  ifneq (yes,$(strip $(MTK_MDM_APP)))
     $(call dep-err-ona-or-offb,MTK_MDM_APP,MTK_MDM_FUMO)
  endif
  ifneq (yes,$(strip $(MTK_FOTA_SUPPORT)))
     $(call dep-err-ona-or-offb,MTK_FOTA_SUPPORTP,MTK_MDM_FUMO)
  endif
endif
ifeq (yes, $(strip $(MTK_MDM_SCOMO)))
  ifneq (yes,$(strip $(MTK_MDM_APP)))
     $(call dep-err-ona-or-offb,MTK_MDM_APP,MTK_MDM_SCOMO)
  endif
endif

############################################################
ifeq ($(strip $(MTK_COMBO_CHIP)),MT6628)
  ifneq ($(strip $(MTK_BT_FM_OVER_BT_VIA_CONTROLLER)),no)
    $(call dep-err-seta-or-setb,MTK_BT_FM_OVER_BT_VIA_CONTROLLER,no,MTK_COMBO_CHIP,none MT6628)
  endif
endif
ifeq ($(strip $(MTK_BT_CHIP)),MTK_MT6628)
  ifneq ($(strip $(MTK_COMBO_CHIP)),MT6628)
     $(call dep-err-seta-or-setb,MTK_BT_CHIP, none MTK_MT6628,MTK_COMBO_CHIP,MT6628)
  endif
endif
ifeq ($(strip $(MTK_FM_CHIP)),MT6628_FM)  
  ifneq ($(strip $(MTK_COMBO_CHIP)),MT6628)
    $(call dep-err-seta-or-setb,MTK_FM_CHIP, none MT6628_FM,MTK_COMBO_CHIP,MT6628)
  endif
endif
ifeq ($(strip $(MTK_WLAN_CHIP)),MT6628)
  ifneq ($(strip $(MTK_COMBO_CHIP)),MT6628)
    $(call dep-err-seta-or-setb,MTK_WLAN_CHIP, none MT6628,MTK_COMBO_CHIP,MT6628)
  endif
endif
ifeq ($(strip $(MTK_GPS_CHIP)),MTK_GPS_MT6628)
  ifneq ($(strip $(MTK_COMBO_CHIP)),MT6628)
    $(call dep-err-seta-or-setb,MTK_GPS_CHIP, none MTK_GPS_MT6628,MTK_COMBO_CHIP,MT6628)
  endif
endif

############################################################
ifeq ($(strip $(MTK_AP_SPEECH_ENHANCEMENT)),yes)
  ifneq ($(strip $(MTK_AUDIO_HD_REC_SUPPORT)),yes)
    $(call dep-err-ona-or-offb, MTK_AUDIO_HD_REC_SUPPORT, MTK_AP_SPEECH_ENHANCEMENT)
  endif
endif
############################################################
ifneq ($(strip $(MTK_LOG2SERVER_APP)),yes)
  ifeq ($(strip $(MTK_LOG2SERVER_INTERNAL)),yes)
    $(call dep-err-ona-or-offb, MTK_LOG2SERVER_APP, MTK_LOG2SERVER_INTERNAL)
  endif
endif
############################################################
ifeq ($(strip $(MTK_INTERNAL_HDMI_SUPPORT)),yes)
  ifeq ($(strip $(MTK_INTERNAL_MHL_SUPPORT)),yes)
    $(call dep-err-offa-or-offb, MTK_INTERNAL_HDMI_SUPPORT, MTK_INTERNAL_MHL_SUPPORT)
  endif
  ifneq ($(strip $(MTK_HDMI_SUPPORT)),yes)
    $(call dep-err-ona-or-offb, MTK_HDMI_SUPPORT, MTK_INTERNAL_HDMI_SUPPORT)
  endif
else
  ifeq ($(strip $(MTK_INTERNAL_MHL_SUPPORT)),yes)
    ifneq ($(strip $(MTK_HDMI_SUPPORT)),yes)
      $(call dep-err-ona-or-offb, MTK_HDMI_SUPPORT, MTK_INTERNAL_MHL_SUPPORT)
    endif
  endif
endif
############################################################
ifneq ($(strip $(OPTR_SPEC_SEG_DEF)), NONE)
  ifneq ($(strip $(MTK_NETWORK_TYPE_ALWAYS_ON)), no)
     $(call dep-err-common, Please set OPTR_SPEC_SEG_DEF as NONE or set MTK_NETWORK_TYPE_ALWAYS_ON as no)
  endif
endif
############################################################
ifeq ($(strip $(MTK_GEMINI_3SIM_SUPPORT)),yes)
  ifneq ($(strip $(GEMINI)),yes)
    $(call dep-err-ona-or-offb, GEMINI, MTK_GEMINI_3SIM_SUPPORT)
  endif
endif
ifeq ($(strip $(MTK_GEMINI_4SIM_SUUPORT)),yes)
  ifneq ($(strip $(GEMINI)),yes)
    $(call dep-err-ona-or-offb, GEMINI, MTK_GEMINI_4SIM_SUUPORT)
  endif
endif
############################################################
ifeq ($(strip $(MTK_MT8193_HDCP_SUPPORT)),yes)
  ifneq ($(strip $(MTK_MT8193_HDMI_SUPPORT)),yes)
    $(call dep-err-ona-or-offb, MTK_MT8193_HDMI_SUPPORT, MTK_MT8193_HDCP_SUPPORT)
  endif
endif
ifeq ($(strip $(MTK_MT8193_HDMI_SUPPORT)),yes)
  ifneq ($(strip $(MTK_MT8193_SUPPORT)),yes)
    $(call dep-err-ona-or-offb, MTK_MT8193_SUPPORT, MTK_MT8193_HDMI_SUPPORT)
  endif
endif
ifeq ($(strip $(MTK_MT8193_NFI_SUPPORT)),yes)
  ifneq ($(strip $(MTK_MT8193_SUPPORT)),yes)
    $(call dep-err-ona-or-offb, MTK_MT8193_SUPPORT, MTK_MT8193_NFI_SUPPORT)
  endif
endif
############################################################
ifeq (yes, $(strip $(MTK_SIM_HOT_SWAP)))
  ifneq (no, $(strip $(MTK_RADIOOFF_POWER_OFF_MD)))
    $(call dep-err-ona-or-offb, MTK_RADIOOFF_POWER_OFF_MD, MTK_SIM_HOT_SWAP)
  endif
endif
############################################################
ifneq ($(filter OP02%, $(OPTR_SPEC_SEG_DEF)),)
  ifeq ($(strip $(MTK_SIP_SUPPORT)),yes)
    $(call dep-err-common, Please do not set OPTR_SPEC_SEG_DEF as OP02* or set MTK_SIP_SUPPORT as no)
  endif
endif
############################################################
ifeq (yes, $(strip $(MTK_WVDRM_L1_SUPPORT)))
  ifneq (yes , $(strip $(MTK_IN_HOUSE_TEE_SUPPORT)))
    ifneq (yes, $(strip $(TRUSTONIC_TEE_SUPPORT)))
      ifneq (yes, $(strip $(MTK_GOOGLE_TRUSTY_SUPPORT)))
        $(call dep-err-common, Please turn on MTK_IN_HOUSE_TEE_SUPPORT or turn on TRUSTONIC_TEE_SUPPORT or MTK_GOOGLE_TRUSTY_SUPPORT when MTK_WVDRM_L1_SUPPORT set as yes)
      endif
    endif
  endif
  ifneq (yes , $(strip $(MTK_DRM_KEY_MNG_SUPPORT)))
    #$(call dep-err-ona-or-offb,MTK_DRM_KEY_MNG_SUPPORT,MTK_WVDRM_L1_SUPPORT)
  endif
  ifneq (yes , $(strip $(MTK_SEC_VIDEO_PATH_SUPPORT)))
    $(call dep-err-ona-or-offb,MTK_SEC_VIDEO_PATH_SUPPORT,MTK_WVDRM_L1_SUPPORT)
  endif
endif
ifeq (yes, $(strip $(MTK_DRM_KEY_MNG_SUPPORT)))
  ifneq (yes, $(strip $(MTK_IN_HOUSE_TEE_SUPPORT)))
    ifneq (yes, $(strip $(TRUSTONIC_TEE_SUPPORT)))
      ifneq (yes, $(strip $(MTK_GOOGLE_TRUSTY_SUPPORT)))
        $(call dep-err-common, Please turn on MTK_IN_HOUSE_TEE_SUPPORT or turn on TRUSTONIC_TEE_SUPPORT or MTK_GOOGLE_TRUSTY_SUPPORT when MTK_DRM_KEY_MNG_SUPPORT set as yes)
      endif
    endif
  endif
endif
ifeq (yes , $(strip $(MTK_SEC_VIDEO_PATH_SUPPORT)))
  ifneq (yes, $(strip $(MTK_IN_HOUSE_TEE_SUPPORT)))
    ifneq (yes, $(strip $(TRUSTONIC_TEE_SUPPORT)))
      ifneq (yes, $(strip $(MTK_GOOGLE_TRUSTY_SUPPORT)))
        $(call dep-err-common, Please turn on MTK_IN_HOUSE_TEE_SUPPORT or turn on TRUSTONIC_TEE_SUPPORT or turn on MTK_GOOGLE_TRUSTY_SUPPORT when MTK_SEC_VIDEO_PATH_SUPPORT set as yes)
      endif
    endif
  endif
endif

ifneq (yes,$(strip $(MTK_AUDIO_HD_REC_SUPPORT)))
  ifeq (yes,$(strip $(MTK_VOIP_ENHANCEMENT_SUPPORT)))
    $(call dep-err-ona-or-offb, MTK_AUDIO_HD_REC_SUPPORT, MTK_VOIP_ENHANCEMENT_SUPPORT)
  endif
  ifeq (yes,$(strip $(MTK_HANDSFREE_DMNR_SUPPORT)))
    $(call dep-err-ona-or-offb, MTK_AUDIO_HD_REC_SUPPORT, MTK_HANDSFREE_DMNR_SUPPORT)
  endif
endif

################################################################
ifeq (yes, $(strip $(MTK_AUTO_SANITY)))
  ifneq (yes, $(strip $(HAVE_AEE_FEATURE)))
     $(call dep-err-ona-or-offb,HAVE_AEE_FEATURE,MTK_AUTO_SANITY)
  endif
endif
###############################################################
ifeq (OP09_SPEC0212_SEGDEFAULT,$(OPTR_SPEC_SEG_DEF))
  ifneq (yes,$(strip $(MTK_C2K_SUPPORT)))
    $(call dep-err-common,pelease set OPTR_SPEC_SEG_DEF as non OP09_SPEC0212_SEGDEFAULT or turn on MTK_C2K_SUPPORT)
  endif
  ifneq (yes,$(strip $(MTK_ENABLE_MD3)))
    $(call dep-err-common,pelease set OPTR_SPEC_SEG_DEF as non OP09_SPEC0212_SEGDEFAULT or turn on MTK_ENABLE_MD3)
  endif
  ifneq (yes,$(strip $(MTK_IRAT_SUPPORT)))
    $(call dep-err-common,pelease set OPTR_SPEC_SEG_DEF as non OP09_SPEC0212_SEGDEFAULT or turn on MTK_IRAT_SUPPORT)
  endif
  ifneq (no,$(strip $(MTK_TTY_SUPPORT)))
    $(call dep-err-common,pelease set OPTR_SPEC_SEG_DEF as non OP09_SPEC0212_SEGDEFAULT or turn off MTK_TTY_SUPPORT)
  endif
  ifeq ($(filter ct_%, $(BOOT_LOGO)),)
    $(call dep-err-common,pelease set OPTR_SPEC_SEG_DEF as non OP09_SPEC0212_SEGDEFAULT or BOOT_LOGO must include ct_)
  endif
  ifneq (0x9,$(strip $(MTK_MD_SBP_CUSTOM_VALUE)))
    $(call dep-err-common,pelease set OPTR_SPEC_SEG_DEF as non OP09_SPEC0212_SEGDEFAULT or set MTK_MD_SBP_CUSTOM_VALUE=0x9)
  endif
  ifneq (yes,$(strip $(MTK_DISABLE_CAPABILITY_SWITCH)))
    $(call dep-err-common,pelease set OPTR_SPEC_SEG_DEF as non OP09_SPEC0212_SEGDEFAULT or turn on MTK_DISABLE_CAPABILITY_SWITCH)
  endif
  ifeq (yes,$(strip $(MTK_SIP_SUPPORT)))
    $(call dep-err-common,pelease set OPTR_SPEC_SEG_DEF as non OP09_SPEC0212_SEGDEFAULT or turn off MTK_SIP_SUPPORT)
  endif
  ifneq (no,$(strip $(MTK_WORLD_PHONE)))
    $(call dep-err-common,pelease set OPTR_SPEC_SEG_DEF as non OP09_SPEC0212_SEGDEFAULT or turn off MTK_WORLD_PHONE)
  endif
  ifneq (no,$(strip $(MTK_FLIGHT_MODE_POWER_OFF_MD)))
    $(call dep-err-common,pelease set OPTR_SPEC_SEG_DEF as non OP09_SPEC0212_SEGDEFAULT or turn off MTK_FLIGHT_MODE_POWER_OFF_MD)
  endif
endif
###############################################################
#for mp release  check release package OPTR_SPEC_SEG_DEF 
#ifneq ($(filter OP01%,$(strip $(OPTR_SPEC_SEG_DEF))),)
#  ifneq ($(filter rel_customer_operator_cmcc, $(MTK_RELEASE_PACKAGE)),rel_customer_operator_cmcc)
#    $(call dep-err-common, please use rel_customer_operator_cmcc as optr release package in MTK_RELEASE_PACKAGE When OPTR_SEPEC_SEG_DEF set as OP01)
#  endif
#endif
#ifneq ($(filter OP02%,$(strip $(OPTR_SPEC_SEG_DEF))),)
#  ifneq ($(filter rel_customer_operator_cu, $(MTK_RELEASE_PACKAGE)),rel_customer_operator_cu)
#    $(call dep-err-common, please use rel_customer_operator_cu as optr release package in to MTK_RELEASE_PACKAGE When OPTR_SEPEC_SEG_DEF set as OP02)
#  endif
#endif
#ifneq ($(filter OP03%,$(strip $(OPTR_SPEC_SEG_DEF))),)
#  ifneq ($(filter rel_customer_operator_orange, $(MTK_RELEASE_PACKAGE)),rel_customer_operator_orange)
#    $(call dep-err-common, please use rel_customer_operator_orange as optr release package in to MTK_RELEASE_PACKAGE When OPTR_SEPEC_SEG_DEF set as OP03)
#  endif
#endif

#ifneq ($(filter OP06%,$(strip $(OPTR_SPEC_SEG_DEF))),)
#  ifneq ($(filter rel_customer_operator_vodafone, $(MTK_RELEASE_PACKAGE)),rel_customer_operator_vodafone)
#    $(call dep-err-common, please use rel_customer_operator_vodafone as optr release package in to MTK_RELEASE_PACKAGE When OPTR_SEPEC_SEG_DEF set as OP06)
#  endif
#endif
#ifneq ($(filter OP07%,$(strip $(OPTR_SPEC_SEG_DEF))),)
#  ifneq ($(filter rel_customer_operator_att, $(MTK_RELEASE_PACKAGE)),rel_customer_operator_att)
#    $(call dep-err-common, please use rel_customer_operator_att as optr release package in to MTK_RELEASE_PACKAGE When OPTR_SEPEC_SEG_DEF set as OP07)
#  endif
#endif
#ifneq ($(filter OP08%,$(strip $(OPTR_SPEC_SEG_DEF))),)
#  ifneq ($(filter rel_customer_operator_tmo_us, $(MTK_RELEASE_PACKAGE)),rel_customer_operator_tmo_us)
#    $(call dep-err-common, please use rel_customer_operator_tmo_us as optr release package in to MTK_RELEASE_PACKAGE When OPTR_SEPEC_SEG_DEF set as OP08)
#  endif
#endif
#ifneq ($(filter OP09%,$(strip $(OPTR_SPEC_SEG_DEF))),)
#  ifneq ($(filter rel_customer_operator_ct, $(MTK_RELEASE_PACKAGE)),rel_customer_operator_ct)
#    $(call dep-err-common, please use rel_customer_operator_ct as optr release package in to MTK_RELEASE_PACKAGE When OPTR_SEPEC_SEG_DEF set as OP09)
#  endif
#endif


#ifeq ($(strip $(MTK_BSP_PACKAGE)),yes)
#  ifneq ($(filter rel_customer_bsp, $(MTK_RELEASE_PACKAGE)),rel_customer_bsp)
#     $(call dep-err-common, please add rel_customer_bsp in to MTK_RELEASE_PACKAGE When MTK_BSP_PACKAGE is yes)
#  endif
#endif
#ifneq (,$(strip $(MTK_PLATFORM)))
#  ifeq ($(filter rel_customer_platform%,$(MTK_RELEASE_PACKAGE)),)
#     $(call dep-err-common, please add rel_customer_platform_xxx in to MTK_RELEASE_PACKAGE)
#  endif
#endif
#############################################################
ifeq (yes,$(strip $(MTK_AIV_SUPPORT)))
  ifneq (yes, $(strip $(MTK_DRM_PLAYREADY_SUPPORT)))
    $(call dep-err-ona-or-offb,MTK_AIV_SUPPORT,MTK_DRM_PLAYREADY_SUPPORT)
  endif
endif
################################################################
ifneq (yes, $(strip $(MTK_DUAL_MIC_SUPPORT)))
    ifeq (yes, $(strip $(MTK_ASR_SUPPORT)))
       $(call dep-err-ona-or-offb,MTK_DUAL_MIC_SUPPORT,MTK_ASR_SUPPORT)
    endif
    ifeq (yes, $(strip $(MTK_VOIP_NORMAL_DMNR)))
       $(call dep-err-ona-or-offb,MTK_DUAL_MIC_SUPPORT,MTK_VOIP_NORMAL_DMNR)
    endif 
    ifeq (yes, $(strip $(MTK_VOIP_NORMAL_DMNR)))
       $(call dep-err-ona-or-offb,MTK_DUAL_MIC_SUPPORT,MTK_VOIP_NORMAL_DMNR)
    endif
    ifeq (yes, $(strip $(MTK_VOIP_HANDSFREE_DMNR)))
       $(call dep-err-ona-or-offb,MTK_DUAL_MIC_SUPPORT,MTK_VOIP_HANDSFREE_DMNR)
    endif
    ifeq (yes, $(strip $(MTK_INCALL_HANDSFREE_DMNR)))
       $(call dep-err-ona-or-offb,MTK_DUAL_MIC_SUPPORT,MTK_INCALL_HANDSFREE_DMNR)
    endif
    ifeq (yes, $(strip $(MTK_INCALL_NORMAL_DMNR)))
       $(call dep-err-ona-or-offb,MTK_DUAL_MIC_SUPPORT,MTK_INCALL_NORMAL_DMNR)
    endif
else
    ifneq (yes, $(strip $(MTK_INCALL_NORMAL_DMNR)))
       $(call dep-err-ona-or-offb,MTK_INCALL_NORMAL_DMNR,MTK_DUAL_MIC_SUPPORT)
    endif
endif
ifneq (yes, $(strip $(MTK_VOIP_ENHANCEMENT_SUPPORT)))
   ifeq (yes, $(strip $(MTK_VOIP_NORMAL_DMNR)))
       $(call dep-err-ona-or-offb,MTK_VOIP_ENHANCEMENT_SUPPORT,MTK_VOIP_NORMAL_DMNR)
    endif 
   ifeq (yes, $(strip $(MTK_VOIP_HANDSFREE_DMNR)))
       $(call dep-err-ona-or-offb,MTK_VOIP_ENHANCEMENT_SUPPORT,MTK_VOIP_HANDSFREE_DMNR)
    endif
endif
################################################################
ifeq (yes,$(strip $(MTK_WFD_HDCP_TX_SUPPORT)))
  ifneq (yes, $(strip $(MTK_DRM_KEY_MNG_SUPPORT)))
    $(call dep-err-ona-or-offb,MTK_DRM_KEY_MNG_SUPPORT,MTK_WFD_HDCP_TX_SUPPORT)
  endif
  ifneq (yes, $(strip $(MTK_IN_HOUSE_TEE_SUPPORT)))
    $(call dep-err-ona-or-offb,MTK_IN_HOUSE_TEE_SUPPORT,MTK_WFD_HDCP_TX_SUPPORT)
  endif
endif
ifneq (yes, $(strip $(MTK_REGIONALPHONE_SUPPORT)))
   ifeq (yes, $(strip $(MTK_TER_SERVICE)))
     $(call dep-err-ona-or-offb,TK_REGIONALPHONE_SUPPORT,MTK_TER_SERVICE)
   endif
endif

ifeq (yes,$(strip $(MTK_SEC_WFD_VIDEO_PATH_SUPPORT)))
  ifneq (yes,$(strip $(MTK_WFD_HDCP_TX_SUPPORT)))
    $(call dep-err-ona-or-offb,MTK_WFD_HDCP_TX_SUPPORT,MTK_SEC_WFD_VIDEO_PATH_SUPPORT)
  endif
  ifneq (yes,$(strip $(MTK_SEC_VIDEO_PATH_SUPPORT)))
    $(call dep-err-ona-or-offb,MTK_SEC_VIDEO_PATH_SUPPORT,MTK_SEC_WFD_VIDEO_PATH_SUPPORT)
  endif
  ifneq (yes,$(strip $(MTK_IN_HOUSE_TEE_SUPPORT)))
    $(call dep-err-ona-or-offb,MTK_IN_HOUSE_TEE_SUPPORT,MTK_SEC_WFD_VIDEO_PATH_SUPPORT)
  endif
endif

ifeq (yes,$(strip $(MTK_WFD_SINK_SUPPORT)))
  ifneq (yes,$(strip $(MTK_WFD_SUPPORT)))
    $(call dep-err-ona-or-offb,MTK_WFD_SUPPORT,MTK_WFD_SINK_SUPPORT)
  endif
else
  ifeq (yes,$(strip $(MTK_WFD_SINK_UIBC_SUPPORT)))
    $(call dep-err-ona-or-offb,MTK_WFD_SINK_SUPPORT,MTK_WFD_SINK_UIBC_SUPPORT)
  endif
endif
#######################################################################
ifeq (yes, $(strip $(MTK_SIM_HOT_SWAP_COMMON_SLOT)))
  ifneq (yes, $(strip $(MTK_SIM_HOT_SWAP)))
    $(call dep-err-ona-or-offb,MTK_SIM_HOT_SWAP,MTK_SIM_HOT_SWAP_COMMON_SLOT)
  endif
endif

ifeq (yes,$(strip $(MTK_INTERNAL)))
  ifeq ($(strip $(OPTR_SPEC_SEG_DEF)), OP01_SPEC0200_SEGC)
     ifneq (yes,$(strip $(MTK_CTSC_MTBF_INTERNAL_SUPPORT)))
       $(call dep-err-ona-or-offb, MTK_CTSC_MTBF_INTERNAL_SUPPORT, MTK_INTERNAL)
     endif
 else
     ifeq (yes,$(strip $(MTK_CTSC_MTBF_INTERNAL_SUPPORT)))
       $(call dep-err-common, turn off MTK_CTSC_MTBF_INTERNAL_SUPPORT or set OPTR_SPEC_SEG_DEF as OP01_SPEC0200_SEGC)
     endif
 endif
else
    ifeq (yes,$(strip $(MTK_CTSC_MTBF_INTERNAL_SUPPORT)))
      $(call dep-err-ona-or-offb, MTK_INTERNAL, MTK_CTSC_MTBF_INTERNAL_SUPPORT)
    endif
endif
############################
ifeq ($(strip $(MTK_SWIP_WMAPRO)), yes)
  ifneq (yes, $(strip $(MTK_WMV_PLAYBACK_SUPPORT)))
    $(call dep-err-ona-or-offb,MTK_WMV_PLAYBACK_SUPPORT,MTK_SWIP_WMAPRO)
  endif
endif
################################################
ifeq (OP01_SPEC0200_SEGC, $(strip $(OPTR_SPEC_SEG_DEF)))
   ifeq ($(filter cmcc_%, $(BOOT_LOGO)),)
      $(call dep-err-common, Please set BOOT_LOGO as cmcc_% or set OPTR_SPEC_SEG_DEF as not OP01_SPEC0200_SEGC)
   endif
   ifneq ($(MTK_UMTS_TDD128_MODE),yes)
      $(call dep-err-common, Please turn on MTK_UMTS_TDD128_MODE or set OPTR_SPEC_SEG_DEF as not OP01_SPEC0200_SEGC)
   endif
   ifneq ($(MTK_WML_SUPPORT),yes)
      $(call dep-err-common, Please turn on MTK_WML_SUPPORT or set OPTR_SPEC_SEG_DEF as not OP01_SPEC0200_SEGC)
   endif
   ifeq ($(MTK_DATA_TRANSFER_APP),yes)
      $(call dep-err-common, Please turn off   or set OPTR_SPEC_SEG_DEF as not OP01_SPEC0200_SEGC)
   endif
   ifneq ($(MTK_RTSP_BITRATE_ADAPTATION_SUPPORT),yes)
      $(call dep-err-common, Please turn on MTK_RTSP_BITRATE_ADAPTATION_SUPPORT  or set OPTR_SPEC_SEG_DEF as not OP01_SPEC0200_SEGC)
   endif
   ifeq ($(MTK_STREAMING_VIDEO_SUPPORT),yes)
      $(call dep-err-common, Please turn off MTK_STREAMING_VIDEO_SUPPORT or set OPTR_SPEC_SEG_DEF as not OP01_SPEC0200_SEGC)
   endif
ifeq ($(MTK_INTERNAL), yes)
   ifeq ($(MTK_DM_APP),yes)
      $(call dep-err-common, Please turn off MTK_DM_APP or set OPTR_SPEC_SEG_DEF as not OP01_SPEC0200_SEGC When MTK_INTERNAL set as yes)
   endif
endif
endif

ifeq (yes,$(strip $(MTK_PERMISSION_CONTROL)))
  ifneq (yes,$(strip $(MTK_MOBILE_MANAGEMENT)))
    $(call dep-err-ona-or-offb,MTK_MOBILE_MANAGEMENT,MTK_PERMISSION_CONTROL)
  endif
endif

ifeq (yes,$(strip $(MTK_GAMELOFT_GLL_ULC_CN_APP)))
  ifeq (yes,$(strip $(MTK_GAMELOFT_GLL_ULC_WW_APP)))
    $(call dep-err-offa-or-offb,MTK_GAMELOFT_GLL_ULC_CN_APP,MTK_GAMELOFT_GLL_ULC_WW_APP)
  endif
endif
ifeq (yes,$(strip $(MTK_GAMELOFT_AVENGERS_ULC_CN_APP)))
  ifeq (yes,$(strip $(MTK_GAMELOFT_AVENGERS_ULC_WW_APP)))
    $(call dep-err-offa-or-offb,MTK_GAMELOFT_AVENGERS_ULC_CN_APP,MTK_GAMELOFT_AVENGERS_ULC_WW_APP)
  endif
endif
ifeq (yes,$(strip $(MTK_GAMELOFT_LBC_ULC_CN_APP)))
  ifeq (yes,$(strip $(MTK_GAMELOFT_LBC_ULC_WW_APP)))
    $(call dep-err-offa-or-offb,MTK_GAMELOFT_LBC_ULC_CN_APP,MTK_GAMELOFT_LBC_ULC_WW_APP)
  endif
endif
ifeq (yes,$(strip $(MTK_GAMELOFT_WONDERZOO_ULC_CN_APP)))
  ifeq (yes,$(strip $(MTK_GAMELOFT_WONDERZOO_ULC_WW_APP)))
    $(call dep-err-offa-or-offb,MTK_GAMELOFT_WONDERZOO_ULC_CN_APP,MTK_GAMELOFT_WONDERZOO_ULC_WW_APP)
  endif
endif
##############################################################################################################
ifeq (yes, $(strip $(MTK_GAMELOFT_KINGDOMANDLORDS_CN_APP)))
  ifeq (yes, $(strip $(MTK_GAMELOFT_KINGDOMANDLORDS_WW_APP)))
        $(call dep-err-common, Please turn off MTK_GAMELOFT_KINGDOMANDLORDS_CN_APP or turn off MTK_GAMELOFT_KINGDOMANDLORDS_WW_APP)
  endif
endif
ifeq (yes, $(strip $(MTK_GAMELOFT_UNOANDFRIENDS_CN_APP)))
  ifeq (yes, $(strip $(MTK_GAMELOFT_UNOANDFRIENDS_WW_APP)))
        $(call dep-err-common, Please turn off MTK_GAMELOFT_UNOANDFRIENDS_CN_APP or turn off MTK_GAMELOFT_UNOANDFRIENDS_WW_APP)
  endif
endif
ifeq (yes, $(strip $(MTK_GAMELOFT_WONDERZOO_CN_APP)))
  ifeq (yes, $(strip $(MTK_GAMELOFT_WONDERZOO_WW_APP)))
        $(call dep-err-common, Please turn off MTK_GAMELOFT_WONDERZOO_CN_APP or turn off MTK_GAMELOFT_WONDERZOO_WW_APP)
  endif
endif
############################################################
ifeq (yes,$(strip $(MTK_EMMC_SUPPORT)))
  ifeq (yes,$(strip $(MTK_COMBO_NAND_SUPPORT)))
    $(call dep-err-offa-or-offb,MTK_EMMC_SUPPORT,MTK_COMBO_NAND_SUPPORT)
  endif
endif
#############################################################
ifeq (yes,$(strip $(MTK_PLAYREADY_SUPPORT)))
  ifneq (yes, $(strip $(MTK_SEC_VIDEO_PATH_SUPPORT)))
    $(call dep-err-ona-or-offb,MTK_SEC_VIDEO_PATH_SUPPORT,MTK_PLAYREADY_SUPPORT)
  endif
endif
#############################################################
ifeq (yes,$(strip $(MTK_DX_HDCP_SUPPORT)))
  ifneq (yes, $(strip $(MTK_PERSIST_PARTITION_SUPPORT)))
    $(call dep-err-ona-or-offb,MTK_PERSIST_PARTITION_SUPPORT,MTK_DX_HDCP_SUPPORT)
  endif
endif
ifeq (yes,$(strip $(MTK_PLAYREADY_SUPPORT)))
  ifneq (yes, $(strip $(MTK_PERSIST_PARTITION_SUPPORT)))
    $(call dep-err-ona-or-offb,MTK_PERSIST_PARTITION_SUPPORT,MTK_PLAYREADY_SUPPORT)
  endif
endif
#############################################################
ifeq (yes, $(strip $(MTK_GAMELOFT_SD_CN_APP)))
  ifeq (yes, $(strip $(MTK_GAMELOFT_SD_WW_APP)))
    $(call dep-err-offa-or-offb, MTK_GAMELOFT_SD_CN_APP, MTK_GAMELOFT_SD_WW_APP)
  endif
endif
ifeq (yes, $(strip $(MTK_GAMELOFT_LBC_CN_APP)))
  ifeq (yes, $(strip $(MTK_GAMELOFT_LBC_WW_APP)))
    $(call dep-err-offa-or-offb, MTK_GAMELOFT_LBC_CN_APP, MTK_GAMELOFT_LBC_WW_APP)
  endif
endif
ifeq (yes, $(strip $(MTK_GAMELOFT_GLL_CN_APP)))
  ifeq (yes, $(strip $(MTK_GAMELOFT_GLL_WW_APP)))
    $(call dep-err-offa-or-offb, MTK_GAMELOFT_GLL_CN_APP, MTK_GAMELOFT_GLL_WW_APP)
  endif
endif
############################################################
ifeq (yes,$(strip $(MTK_EMMC_SUPPORT)))
  ifeq (yes,$(strip $(MTK_MLC_NAND_SUPPORT)))
    $(call dep-err-offa-or-offb,MTK_EMMC_SUPPORT,MTK_MLC_NAND_SUPPORT)
  endif
endif
ifeq (yes,$(strip $(MTK_SPI_NAND_SUPPORT)))
  ifeq (yes,$(strip $(MTK_MLC_NAND_SUPPORT)))
    $(call dep-err-offa-or-offb,MTK_SPI_NAND_SUPPORT,MTK_MLC_NAND_SUPPORT)
  endif
endif
############################################################
ifeq (TRUE,$(strip $(TEE_SECURE_SHAREDMEM_SUPPORT)))
  ifneq (yes,$(strip $(MTK_TEE_CCCI_SECURE_SHARE_MEM_SUPPORT)))
    $(call dep-err-ona-or-offb, MTK_TEE_CCCI_SECURE_SHARE_MEM_SUPPORT,TEE_SECURE_SHAREDMEM_SUPPORT)
  endif
endif
ifeq (FALSE,$(strip $(TEE_SECURE_SHAREDMEM_SUPPORT)))
  ifeq (yes,$(strip $(MTK_TEE_CCCI_SECURE_SHARE_MEM_SUPPORT)))
    $(call dep-err-ona-or-offb, TEE_SECURE_SHAREDMEM_SUPPORT,MTK_TEE_CCCI_SECURE_SHARE_MEM_SUPPORT)
  endif
endif
#######################################################
ifeq ($(strip $(MTK_ANT_SUPPORT)),yes)
  ifneq ($(strip $(MTK_COMBO_CHIP)), MT6630)
     $(call dep-err-common,please turn off dep-err-common or set MTK_COMBO_CHIP as MTK6630)
  endif
endif

ifeq ($(strip $(MTK_BT_BLE_MANAGER_SUPPORT)),yes)
  ifneq ($(strip $(MTK_BT_40_SUPPORT)),yes)
    $(call dep-err-ona-or-offb,MTK_BT_40_SUPPORT,MTK_BT_BLE_MANAGER_SUPPORT)
  endif
endif
###############################################################
ifeq (yes, $(strip $(MTK_VOW_SUPPORT)))
  ifneq (no, $(strip $(MTK_VOICE_UNLOCK_SUPPORT)))
    $(call dep-err-offa-or-offb, MTK_VOW_SUPPORT, MTK_VOICE_UNLOCK_SUPPORT)
  endif
endif

ifeq (yes, $(strip $(MTK_INT_MD_SPE_FOR_EXT_MD)))
  ifneq (yes, $(strip $(MTK_DT_SUPPORT)))
    $(call dep-err-ona-or-offb, MTK_DT_SUPPORT, MTK_INT_MD_SPE_FOR_EXT_MD)
  endif
endif
###############################################################
ifeq ($(strip $(SIM_ME_LOCK)),1)
  ifneq (yes,$(strip $(TRUSTONIC_TEE_SUPPORT)))
    $(call dep-err-common, please set SIM_ME_LOCK=0 or turn on TRUSTONIC_TEE_SUPPORT)
  endif
endif

ifeq (yes, $(strip $(MTK_LTE_SUPPORT)))
  ifeq (yes, $(strip $(MTK_RAT_WCDMA_PREFERRED)))
    $(call dep-err-offa-or-offb, MTK_LTE_SUPPORT, MTK_RAT_WCDMA_PREFERRED)
  endif
endif

ifeq (yes, $(strip $(MTK_16X_SLOWMOTION_VIDEO_SUPPORT)))
  ifneq (yes, $(strip $(MTK_CLEARMOTION_SUPPORT)))
    $(call dep-err-ona-or-offb, MTK_CLEARMOTION_SUPPORT, MTK_16X_SLOWMOTION_VIDEO_SUPPORT)
  endif
  ifneq (yes, $(strip $(MTK_SLOW_MOTION_VIDEO_SUPPORT)))
    $(call dep-err-ona-or-offb, MTK_SLOW_MOTION_VIDEO_SUPPORT, MTK_16X_SLOWMOTION_VIDEO_SUPPORT)
  endif
endif
###############################################################
ifeq ($(strip $(MTK_CAM_DEPTH_AF_SUPPORT)),yes)
  ifneq ($(strip $(MTK_CAM_STEREO_CAMERA_SUPPORT)),yes)
    $(call dep-err-ona-or-offb, MTK_CAM_STEREO_CAMERA_SUPPORT, MTK_CAM_DEPTH_AF_SUPPORT)
  endif
endif

ifeq ($(strip $(MTK_IPOH_SUPPORT)),yes)
  ifneq ($(strip $(MTK_IPO_SUPPORT)),yes)
    $(call dep-err-ona-or-offb, MTK_IPO_SUPPORT, MTK_IPOH_SUPPORT)
  endif
endif
###############################################################
ifeq (OP01_SPEC0200_SEGC,$(OPTR_SPEC_SEG_DEF))
  ifneq (yes , $(strip $(MTK_NFC_OMAAC_SUPPORT)))
     $(call dep-err-common,pelease set OPTR_SPEC_SEG_DEF as non OP01_SPEC0200_SEGC or turn on MTK_NFC_OMAAC_SUPPORT)
  endif
endif
############################################################
ifeq ($(strip $(TARGET_BOARD_PLATFORM)),mt6735m)
  ifneq ($(strip $(MTK_C2K_SUPPORT)),no)
    $(call dep-err-seta-or-setb,MTK_C2K_SUPPORT,no,TARGET_BOARD_PLATFORM,none mt6735m)
  endif
endif
##############################################################
ifeq (yes,$(strip $(MTK_SRLTE_SUPPORT)))
  ifeq (yes,$(strip $(MTK_SVLTE_SUPPORT)))
    $(call dep-err-offa-or-offb, MTK_SRLTE_SUPPORT, MTK_SVLTE_SUPPORT)
  endif
endif
ifeq (yes,$(strip $(MTK_IRAT_SUPPORT)))
  ifeq ($(filter yes,$(strip $(MTK_SRLTE_SUPPORT)) $(strip $(MTK_SVLTE_SUPPORT))),)
    $(call dep-err-ona-or-onb,MTK_SRLTE_SUPPORT,MTK_SVLTE_SUPPORT)
  endif
endif
##############################################################
ifeq (yes,$(strip $(MTK_VOLTE_SUPPORT)))
  ifneq (yes,$(strip $(MTK_IMS_SUPPORT)))
    $(call dep-err-ona-or-offb,MTK_IMS_SUPPORT,MTK_VOLTE_SUPPORT)
  endif
endif
##############################################################
ifneq (yes,$(strip $(MTK_C2K_SUPPORT)))
  ifeq (yes,$(strip $(MTK_C2K_DATA_PPP_SUPPORT)))
    $(call dep-err-ona-or-offb, MTK_C2K_SUPPORT, MTK_C2K_DATA_PPP_SUPPORT)
  endif
endif

ifneq (yes,$(strip $(MTK_AAL_SUPPORT)))
  ifeq (yes,$(strip $(MTK_ULTRA_DIMMING_SUPPORT)))
    $(call dep-err-ona-or-offb, MTK_AAL_SUPPORT, MTK_ULTRA_DIMMING_SUPPORT)
  endif
endif

ifneq (yes,$(strip $(MTK_INTERNAL)))
  ifeq (mt8321,$(strip $(MTK_TABLET_HARDWARE)))
    ifneq (yes,$(strip $(MTK_PLATFORM_OPTIMIZE)))
      $(call dep-err-common,pelease turn on MTK_PLATFORM_OPTIMIZE)
    endif
    ifneq (yes,$(strip $(MTK_HW_ENHANCE)))
      $(call dep-err-common,pelease turn on MTK_HW_ENHANCE)
    endif
  endif
endif
###############################################################
ifeq (yes,$(strip $(MTK_VT3G324M_SUPPORT)))
  ifneq ($(filter FALSE,$(MODEM_SP_VIDEO_CALL_SUPPORT)),)
    $(call dep-err-ona-or-offb,SP_VIDEO_CALL_SUPPORT,MTK_VT3G324M_SUPPORT)
  endif
endif
###############################################################
ifeq (yes,$(strip $(MTK_CTA_SET)))
  ifeq (yes,$(strip $(MTK_RUNTIME_PERMISSION_SUPPORT)))
    $(call dep-err-offa-or-offb,MTK_CTA_SET,MTK_RUNTIME_PERMISSION_SUPPORT)
  endif
endif
###############################################################
#                                MODEM_MTK_AUDIO_HIERARCHICAL_PARAM_SUPPORT
#                               +-----------------------------------------+
#                               |                  | FALSE | TRUE | EMPTY |
#                               |=========================================|
#                               |        V1        |   V   |   X  |   V   |
#                               |------------------+-------+------+-------|
#                               | Starting with V  |   X   |   V  |   X   |
#                               | but not V1       |       |      |       |
#                               |------------------+-------+------+-------|
# MTK_AUDIO_TUNING_TOOL_VERSION |       Empty      |   V   |   X  |   V   |
#                               |------------------+-------+------+-------+
#                               |       Others     |   X   |   X  |   X   |
#                               +-----------------------------------------+
#
ifneq ($(mtk_modem_file),)
ifeq (,$(strip $(MTK_AUDIO_TUNING_TOOL_VERSION)))
  ifneq ($(filter TRUE,$(strip $(MODEM_MTK_AUDIO_HIERARCHICAL_PARAM_SUPPORT))),)
    $(call dep-err-common,MODEM_MTK_AUDIO_HIERARCHICAL_PARAM_SUPPORT cannot be TRUE when MTK_AUDIO_TUNING_TOOL_VERSION is not defined)
  endif
else
  ifeq (V1,$(strip $(MTK_AUDIO_TUNING_TOOL_VERSION)))
    ifneq ($(filter TRUE,$(strip $(MODEM_MTK_AUDIO_HIERARCHICAL_PARAM_SUPPORT))),)
      $(call dep-err-common,MODEM_MTK_AUDIO_HIERARCHICAL_PARAM_SUPPORT cannot be TRUE when MTK_AUDIO_TUNING_TOOL_VERSION is $(strip $(MTK_AUDIO_TUNING_TOOL_VERSION)))
    endif
  else
    ifneq (,$(shell echo '$(strip $(MTK_AUDIO_TUNING_TOOL_VERSION))' | egrep '^V[0-9]+(\.[0-9]+)?'))
      ifeq ($(filter TRUE,$(strip $(MODEM_MTK_AUDIO_HIERARCHICAL_PARAM_SUPPORT))),)
        $(call dep-err-common,MODEM_MTK_AUDIO_HIERARCHICAL_PARAM_SUPPORT must be TRUE instead of "$(MODEM_MTK_AUDIO_HIERARCHICAL_PARAM_SUPPORT)" when MTK_AUDIO_TUNING_TOOL_VERSION is $(strip $(MTK_AUDIO_TUNING_TOOL_VERSION)))
      endif
    else
        $(call dep-err-common,Invalid MTK_AUDIO_TUNING_TOOL_VERSION $(strip $(MTK_AUDIO_TUNING_TOOL_VERSION)))
    endif
  endif
endif
endif # ifneq ($(mtk_modem_file),)
