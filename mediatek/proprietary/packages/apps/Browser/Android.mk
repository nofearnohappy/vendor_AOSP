LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional
LOCAL_CERTIFICATE := platform

LOCAL_STATIC_JAVA_LIBRARIES := \
        android-common \
        guava \
        android-support-v13 \
        android-support-v4 \
        com.mediatek.browser.ext

LOCAL_JAVA_LIBRARIES += mediatek-framework telephony-common
LOCAL_SRC_FILES := \
        $(call all-java-files-under, src) \
        src/com/android/browser/EventLogTags.logtags

LOCAL_OVERRIDES_PACKAGES := Browser
LOCAL_PACKAGE_NAME := MtkBrowser

LOCAL_PROGUARD_FLAG_FILES := proguard.flags
#EMMA_INSTRUMENT := true
LOCAL_EMMA_COVERAGE_FILTER := @$(LOCAL_PATH)/browser-emma-filter.txt 

# We need the sound recorder for the Media Capture API.
LOCAL_REQUIRED_MODULES := SoundRecorder

include $(BUILD_PACKAGE)

# additionally, build tests in sub-folders in a separate .apk
include $(call all-makefiles-under,$(LOCAL_PATH))
