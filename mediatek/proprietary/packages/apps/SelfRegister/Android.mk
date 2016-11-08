ifeq ($(strip $(OPTR_SPEC_SEG_DEF)),OP09_SPEC0212_SEGDEFAULT)
ENABLE_DATA_REGISTER := true
endif

ifeq ($(strip $(CT6M_SUPPORT)),yes)
ENABLE_DATA_REGISTER := true
endif

ifeq ($(strip $(MTK_CT4GREG_APP)),yes)
ENABLE_DATA_REGISTER := true
endif

ifeq ($(strip $(ENABLE_DATA_REGISTER)),true)

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_PROGUARD_FLAG_FILES :=proguard.cfg
LOCAL_PROGUARD_ENABLED := disabled

LOCAL_SRC_FILES := $(call all-java-files-under,src)

LOCAL_PACKAGE_NAME := SelfRegister
LOCAL_CERTIFICATE := platform

LOCAL_JAVA_LIBRARIES += mediatek-framework \
                        org.apache.http.legacy \
                        telephony-common

include $(BUILD_PACKAGE)

endif
