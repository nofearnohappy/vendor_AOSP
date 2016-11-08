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

# Get path to this makefile before including any other file
dolby_path := $(dir $(lastword $(MAKEFILE_LIST)))

# Include custom configuration file if available
-include $(TOPDIR)dolby-config.mk

## Default configuration
# Enable UDC Stagefright integration
DOLBY_UDC ?= true
# Enable UDC for streams
DOLBY_UDC_STREAMING_HLS ?= true
# Enable headphone virtualization of JOC content
DOLBY_UDC_VIRTUALIZE_AUDIO ?= true
# Enable ouptut from UDC to be in QMF always
DOLBY_UDC_OUTPUT_IN_QMF ?= true
# Platform has single primary speaker
DOLBY_MONO_SPEAKER ?= false
# Enable Dolby audio processing
DOLBY_DAP ?= true
# Select between DAP v1 and v2
DOLBY_DAP2 ?= true
ifeq ($(strip $(DOLBY_DAP2)), true)
        DOLBY_DAP1 := false
        DOLBY_DAP_HW := false
        DOLBY_DAP2_BACKWARD_COMPATIBLE ?= true
else
        DOLBY_DAP1 := true
endif
# Select DAX version java components
DOLBY_DAX_VERSION ?= 2
# Enable Dolby audio processing using software module
DOLBY_DAP_SW ?= true
# Enable Dolby audio processing using hardware offload module
DOLBY_DAP_HW ?= true
# Enable HAL API usage on platforms using QDSP
DOLBY_DAP_HW_QDSP_HAL_API ?= true
# Enable pregain calculation & reporting for Dolby audio processing
DOLBY_DAP_PREGAIN ?= true
# Enable postgain reporting for Dolby audio processing
DOLBY_DAP_POSTGAIN ?= false
# Enable moving Dolby audio processing effect to currently active output thread
DOLBY_DAP_MOVE_EFFECT ?= true
# Move effects feature is not needed when offloaded effect is used.
ifeq ($(strip $(DOLBY_DAP_HW)), true)
	DOLBY_DAP_MOVE_EFFECT := false
endif
# Enable bypassing Dolby audio processing for notification & ringtones
DOLBY_DAP_BYPASS_SOUND_TYPES ?= false
# Build & install consumer control panel application
DOLBY_CONSUMER_APP ?= true
# Set log level for debug output
DOLBY_LOG_LEVEL ?= 2
# Dump raw PCM audio from various places
DOLBY_AUDIO_DUMP ?= false
# Enable multichannel output for non-joc contents in case of HP and SPK endpoints
DOLBY_UDC_MULTICHANNEL_PCM_OFFLOAD ?= true
# Multichannel pcm offload feature should be enabled only if offload effect is used
ifneq ($(strip $(DOLBY_DAP_HW)), true)
	DOLBY_UDC_MULTICHANNEL_PCM_OFFLOAD := false
endif
# @@DOLBY_DAP_HW_TEST_HAMMERHEAD
# Use offloaded effect only for offload thread. This switch will use software
# DAP for all playback files except mp3. For mp3 a dummy offload effect is used.
# This flag is only used for internal testing and not exposed to customers.
DOLBY_DAP_HW_TEST_HAMMERHEAD ?= false
# @@DOLBY_DAP_HW_TEST_HAMMERHEAD_END
# Don't spam stdout, because envsetup.sh may be scraping values from it.
ifneq ($(CALLED_FROM_SETUP),true)
# Print the configuration to STDOUT if not silenced
ifneq ($(strip $(DOLBY_SILENT_BUILD_CONFIG)), true)
$(info ============================================)
$(info   DOLBY_UDC=$(DOLBY_UDC))
$(info   DOLBY_UDC_STREAMING_HLS=$(DOLBY_UDC_STREAMING_HLS))
$(info   DOLBY_UDC_VIRTUALIZE_AUDIO=$(DOLBY_UDC_VIRTUALIZE_AUDIO))
$(info   DOLBY_UDC_OUTPUT_IN_QMF=$(DOLBY_UDC_OUTPUT_IN_QMF))
$(info   DOLBY_MONO_SPEAKER=$(DOLBY_MONO_SPEAKER))
$(info   DOLBY_DAP=$(DOLBY_DAP))
$(info   DOLBY_DAP1=$(DOLBY_DAP1))
$(info   DOLBY_DAP2=$(DOLBY_DAP2))
$(info   DOLBY_DAP2_BACKWARD_COMPATIBLE=$(DOLBY_DAP2_BACKWARD_COMPATIBLE))
$(info   DOLBY_DAX_VERSION=$(DOLBY_DAX_VERSION))
$(info   DOLBY_DAP_SW=$(DOLBY_DAP_SW))
$(info   DOLBY_DAP_HW=$(DOLBY_DAP_HW))
$(info   DOLBY_DAP_HW_QDSP_HAL_API=$(DOLBY_DAP_HW_QDSP_HAL_API))
$(info   DOLBY_DAP_PREGAIN=$(DOLBY_DAP_PREGAIN))
$(info   DOLBY_DAP_POSTGAIN=$(DOLBY_DAP_POSTGAIN))
$(info   DOLBY_DAP_MOVE_EFFECT=$(DOLBY_DAP_MOVE_EFFECT))
$(info   DOLBY_DAP_BYPASS_SOUND_TYPES=$(DOLBY_DAP_BYPASS_SOUND_TYPES))
$(info   DOLBY_CONSUMER_APP=$(DOLBY_CONSUMER_APP))
$(info   DOLBY_LOG_LEVEL=$(DOLBY_LOG_LEVEL))
$(info   DOLBY_UDC_MULTICHANNEL_PCM_OFFLOAD=$(DOLBY_UDC_MULTICHANNEL_PCM_OFFLOAD))
$(info   DOLBY_AUDIO_DUMP=$(DOLBY_AUDIO_DUMP))
# @@DOLBY_DAP_HW_TEST_HAMMERHEAD
$(info   DOLBY_DAP_HW_TEST_HAMMERHEAD=$(DOLBY_DAP_HW_TEST_HAMMERHEAD))
# @@DOLBY_DAP_HW_TEST_HAMMERHEAD_END
$(info ============================================)
endif # DOLBY_SILENT_BUILD_CONFIG
endif # CALLED_FROM_SETUP

define define-dolby-flag
	ifeq ($($(strip $1)), true)
		# Add variable to CFLAG
		dolby_cflags += -D$(strip $1)
	else
		# Clear the variable so it can be tested with ifdef in other Makefiles
		$(strip $1) =
	endif
endef

$(eval $(call define-dolby-flag, DOLBY_DAP))
ifdef DOLBY_DAP
define_dolby_common_flags := true
dolby_cflags += -DDOLBY_DAP
$(eval $(call define-dolby-flag, DOLBY_DAP1))
$(eval $(call define-dolby-flag, DOLBY_DAP2))
$(eval $(call define-dolby-flag, DOLBY_DAP2_BACKWARD_COMPATIBLE))
$(eval $(call define-dolby-flag, DOLBY_DAP_SW))
$(eval $(call define-dolby-flag, DOLBY_DAP_HW))
$(eval $(call define-dolby-flag, DOLBY_DAP_PREGAIN))
$(eval $(call define-dolby-flag, DOLBY_DAP_POSTGAIN))
$(eval $(call define-dolby-flag, DOLBY_CONSUMER_APP))
$(eval $(call define-dolby-flag, DOLBY_DAP_MOVE_EFFECT))
$(eval $(call define-dolby-flag, DOLBY_DAP_BYPASS_SOUND_TYPES))
$(eval $(call define-dolby-flag, DOLBY_DAP_HW_QDSP_HAL_API))
# @@DOLBY_DAP_HW_TEST_HAMMERHEAD
$(eval $(call define-dolby-flag, DOLBY_DAP_HW_TEST_HAMMERHEAD))
# @@DOLBY_DAP_HW_TEST_HAMMERHEAD_END
endif

$(eval $(call define-dolby-flag, DOLBY_UDC))
ifdef DOLBY_UDC
define_dolby_common_flags := true
dolby_cflags += -DDOLBY_UDC
$(eval $(call define-dolby-flag, DOLBY_MONO_SPEAKER))
$(eval $(call define-dolby-flag, DOLBY_UDC_STREAMING_HLS))
$(eval $(call define-dolby-flag, DOLBY_UDC_VIRTUALIZE_AUDIO))
$(eval $(call define-dolby-flag, DOLBY_UDC_OUTPUT_IN_QMF))
$(eval $(call define-dolby-flag, DOLBY_UDC_MULTICHANNEL_PCM_OFFLOAD))
endif

ifdef define_dolby_common_flags
dolby_cflags += -I$(dolby_path)include
dolby_cflags += -DDOLBY_LOG_LEVEL=$(DOLBY_LOG_LEVEL)
$(eval $(call define-dolby-flag, DOLBY_AUDIO_DUMP))
endif

COMMON_GLOBAL_CFLAGS += $(dolby_cflags)
