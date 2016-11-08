
# workaround: disable this module to avoid build error
# error: MODULE.TARGET.JAVA_LIBRARIES.hamcrest-library already defined by external/hamcrest/library

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := librobotium4:robotium-solo-4.3.1.jar
include $(BUILD_MULTI_PREBUILT)
include $(CLEAR_VARS)
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := librobotium:robotium.jar
include $(BUILD_MULTI_PREBUILT)
include $(CLEAR_VARS)
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := junit-report:android-junit-report.jar
include $(BUILD_MULTI_PREBUILT)
include $(CLEAR_VARS)
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := junit-report-dev:android-junit-report-dev.jar
include $(BUILD_MULTI_PREBUILT)
include $(CLEAR_VARS)
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := webdriver_library:android_webdriver_library.jar
include $(BUILD_MULTI_PREBUILT)
include $(CLEAR_VARS)
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := webdriver-library:android_webdriver_library.jar
include $(BUILD_MULTI_PREBUILT)
include $(CLEAR_VARS)
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := hamcrest-core:hamcrest-core.jar
include $(BUILD_MULTI_PREBUILT)
include $(CLEAR_VARS)
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := hamcrest-core-SNAPSHOT:hamcrest-core-SNAPSHOT.jar
include $(BUILD_MULTI_PREBUILT)

# workaround since this module was defined in external/hamcrest/library.
ifeq ($(I_CAN_BUILD_PASS),"yes")
include $(CLEAR_VARS)
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := hamcrest-library:hamcrest-library.jar
include $(BUILD_MULTI_PREBUILT)
endif

include $(CLEAR_VARS)
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := hamcrest-library-SNAPSHOT:hamcrest-library-SNAPSHOT.jar
include $(BUILD_MULTI_PREBUILT)
include $(call all-makefiles-under,$(LOCAL_PATH))
