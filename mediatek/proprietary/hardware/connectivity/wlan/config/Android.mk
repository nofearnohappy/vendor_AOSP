# --------------------------------------------------------------------
# configuration files for AOSP wpa_supplicant_8
# --------------------------------------------------------------------
ifeq ($(WPA_SUPPLICANT_VERSION),VER_0_8_X)
WPA_SUPPLICANT_BUILD := yes
endif
ifeq ($(WPA_SUPPLICANT_VERSION),VER_0_8_X_MTK)
WPA_SUPPLICANT_BUILD := yes
endif

ifeq ($(WPA_SUPPLICANT_BUILD), yes)
########################
local_target_dir := $(TARGET_OUT)/etc/wifi
$(warning 1-- target_out=$(TARGET_OUT))

LOCAL_PATH := $(call my-dir)

$(warning 2-- target_out=$(TARGET_OUT))
include $(CLEAR_VARS)
LOCAL_MODULE := wpa_supplicant.conf
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_PATH := $(local_target_dir)
LOCAL_SRC_FILES := mtk-wpa_supplicant.conf
include $(BUILD_PREBUILT)

#################Add overlay file################
$(warning 3-- target_out=$(TARGET_OUT))
include $(CLEAR_VARS)
LOCAL_MODULE := wpa_supplicant_overlay.conf
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_PATH := $(local_target_dir)
ifeq ($(findstring OP01, $(strip $(OPTR_SPEC_SEG_DEF))),OP01)
LOCAL_SRC_FILES := cmcc-wpa_supplicant-overlay.conf
else
LOCAL_SRC_FILES := mtk-wpa_supplicant-overlay.conf
endif
include $(BUILD_PREBUILT)

$(warning 4-- target_out=$(TARGET_OUT))
include $(CLEAR_VARS)
LOCAL_MODULE := p2p_supplicant_overlay.conf
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_PATH := $(local_target_dir)
LOCAL_SRC_FILES := mtk-p2p_wpa_supplicant-overlay.conf
include $(BUILD_PREBUILT)
endif #ifeq($(WPA_SUPPLICANT_VERSION),VER_0_8_X)
