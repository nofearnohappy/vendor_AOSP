LOCAL_PATH:= $(call my-dir)
ifeq ($(TARGET_DEVICE), mt6595_phone_v1)
include $(call all-makefiles-under,$(LOCAL_PATH))
endif
