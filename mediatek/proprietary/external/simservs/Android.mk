
LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_JAVA_LIBRARIES := okhttp
LOCAL_STATIC_JAVA_LIBRARIES := xcap

# Include all the java files.
LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_MODULE_TAGS := optional

# The name of the jar file to create.
LOCAL_MODULE := Simservs

# Build a static jar file.
include $(BUILD_STATIC_JAVA_LIBRARY)

