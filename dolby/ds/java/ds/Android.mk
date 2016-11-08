################################################################################
#  This program is protected under international and U.S. copyright laws as
#  an unpublished work. This program is confidential and proprietary to the
#  copyright owners. Reproduction or disclosure, in whole or in part, or the
#  production of derivative works therefrom without the express permission of
#  the copyright owners is prohibited.
#
#                 Copyright (C) 2012-2014 by Dolby Laboratories,
#                             All rights reserved.
################################################################################

ifdef DOLBY_DAP
ifeq ($(DOLBY_DAX_VERSION),2)

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

DAP_JAVA_SUBDIRS := android/dolby
ds_java_libs := bouncycastle core-libart core-junit ext framework dolby_ds2
ds_java_src := \
    android/dolby/DsClient.java \
    android/dolby/DsClientSettings.java \
    android/dolby/IDsClientEvents.java \
    android/dolby/IDsApParamEvents.java \
    android/dolby/IDsVisualizerEvents.java

LOCAL_SRC_FILES := $(ds_java_src)

LOCAL_NO_STANDARD_LIBRARIES := true
LOCAL_JAVA_LIBRARIES := $(ds_java_libs)

LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := dolby_ds
LOCAL_MODULE_CLASS := JAVA_LIBRARIES

LOCAL_NO_EMMA_INSTRUMENT := true
LOCAL_NO_EMMA_COMPILE := true

LOCAL_DX_FLAGS := --core-library

LOCAL_MODULE_OWNER := dolby

include $(BUILD_JAVA_LIBRARY)

# FIXME: Fix android errors
#-include $(LOCAL_PATH)/Android.stubs.mk

endif # DOLBY_DAX_VERSION 2
endif # DOLBY_DAP END
