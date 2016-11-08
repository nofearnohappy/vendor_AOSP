ifeq ($(MTK_RCS_SUPPORT), yes)

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := $(call all-java-files-under, src) $(call all-Iaidl-files-under, src)
LOCAL_AIDL_INCLUDES += $(LOCAL_PATH)/src
LOCAL_PACKAGE_NAME := RcsPublicAccount

LOCAL_STATIC_JAVA_LIBRARIES := \
	lib_commons_io_2_4 \
	lib_joda_time_2_6 \
	com.android.vcard \
	android-support-v4 \

LOCAL_JAVA_LIBRARIES += mediatek-framework \
			mediatek-common \
			telephony-common \
			ims-common \
			com.cmcc.ccs \
			okhttp \


#LOCAL_CERTIFICATE := platform
LOCAL_PROGUARD_ENABLED := disabled

include $(BUILD_PACKAGE)

include $(CLEAR_VARS)

LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := lib_commons_io_2_4:framework/commons-io-2.4.jar lib_joda_time_2_6:framework/joda-time-2.6.jar

include $(BUILD_MULTI_PREBUILT)

include $(call all-makefiles-under,$(LOCAL_PATH))

endif

