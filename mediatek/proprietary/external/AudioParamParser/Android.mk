ifeq ($(strip $(BOARD_USES_MTK_AUDIO)),true)

LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

ifeq ($(strip $(MTK_APP_FORCE_ENABLE_CUS_XML_SUPPORT)),yes)
	LOCAL_CFLAGS += APP_FORCE_ENABLE_CUS_XML
endif

ifeq ($(strip $(TARGET_BUILD_VARIANT)),eng)
	LOCAL_CFLAGS += -DCONFIG_MT_ENG_BUILD
endif

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES:= \
	AudioCategory.c \
	AudioParam.c \
	AudioParamFieldInfo.c \
	AudioParamParser.c \
	AudioParamTreeView.c \
	AudioParamUnit.c \
	AudioType.c \
	AudioUtils.c \
	UnitTest.c \
	guicon.cpp

LOCAL_C_INCLUDES= \
	external/libxml2/include \
	external/icu/icu4c/source/common \
	$(JNI_H_INCLUDE) \
	$(LOCAL_PATH)/include \
	$(MTK_PATH_SOURCE)/external/audiocustparam

LOCAL_SHARED_LIBRARIES += \
	libicuuc \
	libutils \
	libcutils \
	libbinder \
	libmedia \
	libaudiocustparam

LOCAL_STATIC_LIBRARIES := libxml2

LOCAL_MODULE := libaudio_param_parser

LOCAL_MULTILIB := both

include $(BUILD_SHARED_LIBRARY)


include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := eng

LOCAL_SRC_FILES:= \
	test/main.c

LOCAL_C_INCLUDES= \
	$(MTK_PATH_SOURCE)/external/AudioParamParser/ \
	$(MTK_PATH_SOURCE)/external/AudioParamParser/include

LOCAL_SHARED_LIBRARIES += \
	libaudio_param_parser

LOCAL_MODULE := audio_param_test

#LOCAL_MULTILIB := 32

include $(BUILD_EXECUTABLE)

include $(LOCAL_PATH)/DeployAudioParam.mk

include $(LOCAL_PATH)/GenAudioParamOptionsXml.mk

endif
