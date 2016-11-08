ifeq ($(TARGET_BUILD_PDK),)
ifneq ($(strip $(MTK_PLATFORM)),)
LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := eng

ifeq ($(findstring MTK_AOSP_ENHANCEMENT,  $(COMMON_GLOBAL_CPPFLAGS)),)
	LOCAL_CFLAGS += -DMTK_BASIC_PACKAGE
endif

LOCAL_SRC_FILES:=       \
	audiocommand.cpp
	
LOCAL_C_INCLUDES=       \
       $(MTK_PATH_SOURCE)/frameworks/base/include/media \
       external/tinyalsa/include

       
LOCAL_C_INCLUDES+=       \
    $(MTK_PATH_SOURCE)/platform/common/hardware/audio/include

LOCAL_SHARED_LIBRARIES := libcutils libutils libbinder libmedia libaudioflinger libtinyalsa

#LOCAL_SHARED_LIBRARIES += libaudio.primary.default

ifeq ($(HAVE_AEE_FEATURE),yes)
    LOCAL_SHARED_LIBRARIES += libaed
    LOCAL_C_INCLUDES += \
    $(MTK_PATH_SOURCE)/external/aee/binary/inc
    LOCAL_CFLAGS += -DHAVE_AEE_FEATURE
endif
ifeq ($(MTK_AUDIO),yes)
LOCAL_C_INCLUDES+= \
   $(TOP)/$(MTK_PATH_SOURCE)/hardware/audio/common/include
endif

LOCAL_MODULE:= audiocommand

LOCAL_MULTILIB := 32

include $(BUILD_EXECUTABLE)
endif
endif #ifeq ($(TARGET_BUILD_PDK),)
