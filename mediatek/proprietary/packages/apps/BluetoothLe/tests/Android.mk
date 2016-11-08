ifeq ($(strip $(MTK_BT_TEST)),yes)
ifeq ($(strip $(ASPECTS_ORIENTED_INSTRUMENT)),true)

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

# We only want this apk build for tests.
LOCAL_MODULE_TAGS := optional
LOCAL_CERTIFICATE := platform

LOCAL_JAVA_LIBRARIES := android.test.runner
LOCAL_JAVA_LIBRARIES += mediatek-framework

LOCAL_STATIC_JAVA_LIBRARIES := mockito-lib dexmaker-lib dexmaker-mockito

# Include all test java files.
LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_PACKAGE_NAME := BluetoothLeTests

LOCAL_INSTRUMENTATION_FOR := BluetoothLe

LOCAL_PROGUARD_ENABLED := disabled

include $(BUILD_PACKAGE)

##################################################
include $(CLEAR_VARS)

LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := mockito-lib:libs/mockito-all-1.9.5.jar 
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += dexmaker-lib:libs/dexmaker-1.0.jar
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += dexmaker-mockito:libs/dexmaker-mockito-1.0.jar

include $(BUILD_MULTI_PREBUILT)

endif
endif
