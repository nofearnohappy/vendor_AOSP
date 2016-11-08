################################################################################
#  This program is protected under international and U.S. copyright laws as
#  an unpublished work. This program is confidential and proprietary to the
#  copyright owners. Reproduction or disclosure, in whole or in part, or the
#  production of derivative works therefrom without the express permission of
#  the copyright owners is prohibited.
#
#                 Copyright (C) 2014 by Dolby Laboratories,
#                             All rights reserved.
################################################################################

$(call inherit-product, device/lge/hammerhead/aosp_hammerhead.mk)

PRODUCT_NAME := dax1_hammerhead_sw
PRODUCT_DEVICE := hammerhead

DOLBY_DAX_VERSION            := 1
DOLBY_DAP                    := true
DOLBY_DAP2                   := false
DOLBY_DAP_SW                 := true
DOLBY_DAP_HW                 := false
DOLBY_DAP_PREGAIN            := true
DOLBY_DAP_HW_QDSP_HAL_API    := false
DOLBY_DAP_MOVE_EFFECT        := true
DOLBY_DAP_BYPASS_SOUND_TYPES := false
DOLBY_CONSUMER_APP           := true
DOLBY_UDC                    := true
DOLBY_UDC_VIRTUALIZE_AUDIO   := false
DOLBY_MONO_SPEAKER           := true

include vendor/dolby/ds/dolby-buildspec.mk
$(call inherit-product, vendor/dolby/ds/dolby-product.mk)

PRODUCT_COPY_FILES := \
    vendor/dolby/device/dax1_hammerhead_sw/media_codecs.xml:system/etc/media_codecs.xml:dolby \
    vendor/dolby/device/dax1_hammerhead_sw/audio_effects.conf:system/vendor/etc/audio_effects.conf:dolby \
    vendor/dolby/device/dax1_hammerhead_sw/ds1-default.xml:system/vendor/etc/dolby/ds1-default.xml:dolby \
    $(PRODUCT_COPY_FILES)

PRODUCT_RESTRICT_VENDOR_FILES := false
