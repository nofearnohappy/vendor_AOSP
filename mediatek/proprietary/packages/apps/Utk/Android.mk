# Copyright 2007-2008 The Android Open Source Project

ifeq ($(strip $(MTK_C2K_SUPPORT)), yes)

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

ifeq (OP09,$(word 1,$(subst _, ,$(OPTR_SPEC_SEG_DEF))))
    LOCAL_MANIFEST_FILE := ct/AndroidManifest.xml
endif

LOCAL_MODULE_TAGS := optional

LOCAL_JAVA_LIBRARIES := telephony-common mediatek-framework
LOCAL_SRC_FILES := $(call all-subdir-java-files)

LOCAL_PACKAGE_NAME := Utk
LOCAL_CERTIFICATE := platform

include $(BUILD_PACKAGE)

endif
