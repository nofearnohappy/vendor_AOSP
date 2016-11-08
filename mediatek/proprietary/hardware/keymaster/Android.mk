LOCAL_PATH    := $(call my-dir)

# TRUSTONIC Tee Keymaster
ifeq ($(TRUSTONIC_TEE_SUPPORT), yes)

ifeq (,$(filter MT6582, $(MTK_PLATFORM)))

include $(CLEAR_VARS)

LOCAL_MODULE := keystore.$(TARGET_BOARD_PLATFORM)
LOCAL_MODULE_TAGS := debug eng optional
LOCAL_MODULE_RELATIVE_PATH := hw

# Add new source files here
LOCAL_SRC_FILES +=\
    keymaster_mt_tbase.cpp

LOCAL_C_INCLUDES +=\
    $(LOCAL_PATH)/inc \
    external/boringssl/include \
    system/core/include \

LOCAL_SHARED_LIBRARIES := libMcClient liblog libMcTeeKeymaster libcrypto

include $(BUILD_SHARED_LIBRARY)

endif

endif
