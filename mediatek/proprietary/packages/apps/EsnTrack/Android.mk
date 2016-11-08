ifeq ($(MTK_ESN_TRACK_APP),yes)

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_PROGUARD_FLAG_FILES :=proguard.cfg
LOCAL_PROGUARD_ENABLED := disabled

LOCAL_SRC_FILES := $(call all-java-files-under,src)

LOCAL_PACKAGE_NAME := EsnTrack
LOCAL_CERTIFICATE := platform

LOCAL_JAVA_LIBRARIES += mediatek-framework \
                        telephony-common
LOCAL_JAVA_LIBRARIES += mediatek-telephony-common
include $(BUILD_PACKAGE)

endif
