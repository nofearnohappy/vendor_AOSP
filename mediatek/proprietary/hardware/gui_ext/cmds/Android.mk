LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
	main_guiext.cpp 

LOCAL_SHARED_LIBRARIES := \
    libutils \
    libcutils \
    libbinder \
    libgui_ext \

LOCAL_C_INCLUDES := \
    $(TOP)/$(MTK_PATH_SOURCE)/hardware/gui_ext/inc


LOCAL_MODULE:= guiext-server

LOCAL_MODULE_TAGS := optional

include $(BUILD_EXECUTABLE)