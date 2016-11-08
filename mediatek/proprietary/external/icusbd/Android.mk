ifeq ($(MTK_ICUSB_SUPPORT),yes)
LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MULTILIB := 32
LOCAL_MODULE_TAGS:= optional
LOCAL_MODULE := icusbd

LOCAL_SRC_FILES:= \
  icusb_util.c \
  icusb_ccci.c \
  icusb_ccid.c \
  icusb_storage.c
  
LOCAL_SRC_FILES += icusb_main.c 

LOCAL_C_INCLUDES = \
 $(TOP)/vendor/mediatek/proprietary/external/libusb/ \
 $(LOCAL_PATH)/ \
 $(TOPDIR)/hardware/libhardware_legacy/include \
 $(TOPDIR)/hardware/libhardware/include \
 $(MTK_PATH_SOURCE)/hardware/ccci/include

LOCAL_SHARED_LIBRARIES := libc libusb libcutils

include $(BUILD_EXECUTABLE)
endif
