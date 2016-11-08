LOCAL_PATH := $(call my-dir)

###########################################################################
# MTK BT CHIP INIT LIBRARY FOR BLUEDROID
###########################################################################
include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
  mtk.c \
  radiomgr.c \
  radiomod.c

LOCAL_C_INCLUDES := \
  system/bt/hci/include \
  $(MTK_PATH_SOURCE)/external/nvram/libnvram \
  $(MTK_PATH_CUSTOM)/cgen/cfgfileinc \
  $(MTK_PATH_CUSTOM)/cgen/cfgdefault \
  $(MTK_PATH_CUSTOM)/cgen/inc \
  $(MTK_PATH_CUSTOM)/hal/bluetooth


ifneq ($(filter MTK_MT6628,$(MTK_BT_CHIP)),)
LOCAL_CFLAGS += -DMTK_MT6628
endif
ifneq ($(filter MTK_MT6630,$(MTK_BT_CHIP)),)
LOCAL_CFLAGS += -DMTK_MT6630
endif
ifneq ($(filter MTK_CONSYS_MT6582,$(MTK_BT_CHIP)),)
LOCAL_CFLAGS += -DMTK_CONSYS_MT6582
endif
ifneq ($(filter MTK_CONSYS_MT6592,$(MTK_BT_CHIP)),)
LOCAL_CFLAGS += -DMTK_CONSYS_MT6592
endif
ifneq ($(filter MTK_CONSYS_MT6752,$(MTK_BT_CHIP)),)
LOCAL_CFLAGS += -DMTK_CONSYS_MT6752
endif
ifneq ($(filter MTK_CONSYS_MT6735,$(MTK_BT_CHIP)),)
LOCAL_CFLAGS += -DMTK_CONSYS_MT6735
endif
ifneq ($(filter MTK_CONSYS_MT6580,$(MTK_BT_CHIP)),)
LOCAL_CFLAGS += -DMTK_CONSYS_MT6580
endif
ifneq ($(filter MTK_CONSYS_MT6755,$(MTK_BT_CHIP)),)
LOCAL_CFLAGS += -DMTK_CONSYS_MT6755
endif
ifeq ($(MTK_MERGE_INTERFACE_SUPPORT), yes)
LOCAL_CFLAGS += -D__MTK_MERGE_INTERFACE_SUPPORT__
endif

ifeq ($(TARGET_BUILD_VARIANT), eng)
LOCAL_CFLAGS += -DBD_ADDR_AUTOGEN
endif

LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := libbluetooth_mtk
LOCAL_SHARED_LIBRARIES := liblog libcutils libnvram
LOCAL_PRELINK_MODULE := false
include $(BUILD_SHARED_LIBRARY)

###########################################################################
# MTK BT DRIVER FOR BLUEDROID
###########################################################################
include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
  bt_drv.c

LOCAL_C_INCLUDES := \
  system/bt/hci/include \
  $(MTK_PATH_CUSTOM)/cgen/cfgfileinc

LOCAL_CFLAGS :=

# For include build flag in mdroid_buildcfg.h
ifeq ($(MTK_BT_BLUEDROID_PLUS),yes)
LOCAL_CFLAGS += -DMTK_BT_COMMON
LOCAL_CFLAGS += -DHAS_MDROID_BUILDCFG
LOCAL_C_INCLUDES += \
  system/bt/include
endif

LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := libbt-vendor
LOCAL_SHARED_LIBRARIES := liblog libbluetooth_mtk
LOCAL_PRELINK_MODULE := false
include $(BUILD_SHARED_LIBRARY)
