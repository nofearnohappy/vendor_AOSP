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

ifdef DOLBY_DAP
    PRODUCT_PACKAGES += dolby_ds Ds
    ifeq ($(DOLBY_DAX_VERSION),2)
        PRODUCT_PACKAGES += dolby_ds2
    endif

    ifdef DOLBY_CONSUMER_APP
        PRODUCT_PACKAGES += DsUI
    endif

    ifdef DOLBY_DAP_SW
        PRODUCT_PACKAGES += libswdap
    endif

    ifdef DOLBY_DAP_HW
        PRODUCT_PACKAGES += libhwdap
    endif

    ifdef DOLBY_DAP_HW_QDSP_HAL_API
        PRODUCT_PACKAGES += libhwdaphal
    endif
    
    ifdef DOLBY_MONO_SPEAKER
        ifneq ($(strip $(DOLBY_MONO_SPEAKER)), )
            PRODUCT_PROPERTY_OVERRIDES += dolby.monospeaker=$(DOLBY_MONO_SPEAKER)
        endif
    endif

    ifeq ($(DOLBY_DAX_VERSION),1)
        PRODUCT_BOOT_JARS := $(PRODUCT_BOOT_JARS) dolby_ds
    endif
    ifeq ($(DOLBY_DAX_VERSION),2)
        PRODUCT_BOOT_JARS := $(PRODUCT_BOOT_JARS) dolby_ds dolby_ds2
    endif

endif #DOLBY_DAP

ifdef DOLBY_UDC
    PRODUCT_PACKAGES += libstagefright_soft_ddpdec
endif #DOLBY_UDC
