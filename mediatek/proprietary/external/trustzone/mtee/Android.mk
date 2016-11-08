ifeq ($(filter $(MTK_IN_HOUSE_TEE_SUPPORT) $(MTK_GOOGLE_TRUSTY_SUPPORT),yes),yes)

LOCAL_PATH := $(call my-dir)
include $(call all-makefiles-under,$(LOCAL_PATH))

endif   # MTK_IN_HOUSE_TEE_SUPPORT || MTK_GOOGLE_TRUSTY_SUPPORT
