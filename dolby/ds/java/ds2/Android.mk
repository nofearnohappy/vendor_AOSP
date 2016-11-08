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

ds2_java_subdirs := com/dolby
ds2_src_files := $(call all-java-files-under, $(ds2_java_subdirs)) \
				 $(call all-Iaidl-files-under, $(ds2_java_subdirs))
ds2_java_libs := bouncycastle core-libart core-junit ext framework

LOCAL_SRC_FILES := $(ds2_src_files)
LOCAL_NO_STANDARD_LIBRARIES := true
LOCAL_JAVA_LIBRARIES := $(ds2_java_libs)

LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := dolby_ds2
LOCAL_MODULE_CLASS := JAVA_LIBRARIES

LOCAL_NO_EMMA_INSTRUMENT := true
LOCAL_NO_EMMA_COMPILE := true

LOCAL_DX_FLAGS := --core-library

LOCAL_MODULE_OWNER := dolby

include $(BUILD_JAVA_LIBRARY)

include $(CLEAR_VARS)
LOCAL_SRC_FILES := $(ds2_src_files)
LOCAL_MODULE := dolby_ds2_static
include $(BUILD_STATIC_JAVA_LIBRARY)

# FIXME: Fix android errors
#-include $(LOCAL_PATH)/Android.stubs.mk

endif # DOLBY_DAX_VERSION 2
endif # DOLBY_DAP END
