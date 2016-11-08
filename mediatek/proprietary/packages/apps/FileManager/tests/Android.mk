    LOCAL_PATH:= $(call my-dir)
    include $(CLEAR_VARS)

    LOCAL_MODULE_TAGS := tests

    LOCAL_JAVA_LIBRARIES := android.test.runner \
                            mediatek-framework

    # Include all test java files.
    LOCAL_SRC_FILES := $(call all-java-files-under, src)

    LOCAL_PACKAGE_NAME := FileManagerTest

    LOCAL_CERTIFICATE := platform

    LOCAL_INSTRUMENTATION_FOR := FileManager

    #Include librobotium
    LOCAL_STATIC_JAVA_LIBRARIES := librobotium4

    include $(BUILD_PACKAGE)

    # Use the following include to make our test apk.
    include $(call all-makefiles-under,$(LOCAL_PATH))
