################################################################################
#  This program is protected under international and U.S. copyright laws as
#  an unpublished work. This program is confidential and proprietary to the
#  copyright owners. Reproduction or disclosure, in whole or in part, or the
#  production of derivative works therefrom without the express permission of
#  the copyright owners is prohibited.
#
#                 Copyright (C) 2011-2014 by Dolby Laboratories,
#                             All rights reserved.
################################################################################

#DOLBY_DAP
ifdef DOLBY_DAP

ifeq ($(DOLBY_DAX_VERSION),1)

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_MODULE_OWNER := dolby

LOCAL_SRC_FILES := $(call all-subdir-java-files)

LOCAL_JAVA_LIBRARIES += dolby_ds

LOCAL_PACKAGE_NAME := Ds

LOCAL_CERTIFICATE := platform

LOCAL_MODULE_PATH := $(TARGET_OUT_VENDOR_APPS)

include $(BUILD_PACKAGE)

endif
#DOLBY_DAX_VERSION 1
endif
#DOLBY_DAP_END
