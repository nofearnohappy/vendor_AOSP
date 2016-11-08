LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := \
        $(call all-java-files-under, src)

#Path to aspect root folder
ifeq ($(strip $(MTK_BT_TEST)),yes)
ifeq ($(strip $(ASPECTS_ORIENTED_INSTRUMENT)),true)

LOCAL_ASPECTS_DIR := aspect/BluetoothLe/src

endif
endif


LOCAL_PACKAGE_NAME := BluetoothLe

LOCAL_CERTIFICATE := platform

LOCAL_JAVA_LIBRARIES += mediatek-framework
LOCAL_JAVA_LIBRARIES += telephony-common
#LOCAL_JAVA_LIBRARIES += mediatek-telephony-common

LOCAL_STATIC_JAVA_LIBRARIES := joda-time-2.3
LOCAL_STATIC_JAVA_LIBRARIES += com.mediatek.bluetoothle.ext

#AspectJ runtime
ifeq ($(strip $(MTK_BT_TEST)),yes)
ifeq ($(strip $(ASPECTS_ORIENTED_INSTRUMENT)),true)
LOCAL_STATIC_JAVA_LIBRARIES += aspectj-runtime
endif
endif

# LOCAL_REQUIRED_MODULES := Add the required module

ifeq ($(strip $(MTK_BT_TEST)), yes)
LOCAL_PROGUARD_ENABLED := disabled
else
# Add for Proguard
LOCAL_PROGUARD_ENABLED := custom
LOCAL_PROGUARD_FLAG_FILES := proguard.flags
# Add for Proguard
endif

# Added for JPE begin
LOCAL_JAVASSIST_ENABLED := true
LOCAL_JAVASSIST_OPTIONS := $(LOCAL_PATH)/jpe.config
# Added For JPE end

include $(BUILD_PACKAGE)

##################################################
include $(CLEAR_VARS)

LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := joda-time-2.3:lib/joda-time-2.3.jar
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += aspectj-runtime:../../../../../../prebuilts/tools/common/aspectj/aspectjrt.jar

include $(BUILD_MULTI_PREBUILT)

include $(call all-makefiles-under,$(LOCAL_PATH))
