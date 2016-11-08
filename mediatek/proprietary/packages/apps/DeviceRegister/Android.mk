ifeq ($(strip $(OPTR_SPEC_SEG_DEF)),OP09_SPEC0212_SEGDEFAULT)
MTK_DEVREG_APP := yes
endif

ifeq ($(strip $(CT6M_SUPPORT)),yes)
MTK_DEVREG_APP := yes
endif

ifeq ($(strip $(MTK_DEVREG_APP)),yes)

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_PROGUARD_FLAG_FILES :=proguard.cfg
LOCAL_PROGUARD_ENABLED := disabled

LOCAL_SRC_FILES := $(call all-java-files-under,src)

LOCAL_PACKAGE_NAME := DeviceRegister
LOCAL_CERTIFICATE := platform

LOCAL_JAVA_LIBRARIES += mediatek-framework \
                        telephony-common \
                        mediatek-telephony-common

include $(BUILD_PACKAGE)

endif
