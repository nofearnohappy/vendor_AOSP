LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_JAVA_LIBRARIES := okhttp

# Include all the java files.
LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_MODULE_TAGS := optional

# The name of the jar file to create.
LOCAL_MODULE := xcap

# Build a static jar file.
include $(BUILD_STATIC_JAVA_LIBRARY)

