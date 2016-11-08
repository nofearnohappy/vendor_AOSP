LOCAL_PATH := $(call my-dir)

###########################################################################
# MTK BT CHIP INIT LIBRARY INDEPENDENT OF STACK
###########################################################################
include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
  mtk.c \
  radiomgr.c \
  radiomod.c

LOCAL_C_INCLUDES := \
  $(MTK_PATH_SOURCE)/external/nvram/libnvram \
  $(MTK_PATH_CUSTOM)/cgen/cfgfileinc \
  $(MTK_PATH_CUSTOM)/cgen/cfgdefault \
  $(MTK_PATH_CUSTOM)/cgen/inc \
  $(MTK_PATH_CUSTOM)/hal/bluetooth \
  $(LOCAL_PATH)/../inc

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

LOCAL_MODULE := libbluetooth_mtk_pure
LOCAL_SHARED_LIBRARIES := liblog libcutils libnvram
LOCAL_PRELINK_MODULE := false
include $(BUILD_SHARED_LIBRARY)


###########################################################################
# BT ENGINEER MODE
###########################################################################
BUILD_BT_EM := true
ifeq ($(BUILD_BT_EM), true)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := bt_em.c

LOCAL_C_INCLUDES := \
  $(LOCAL_PATH)/../inc

LOCAL_MODULE :=libbluetoothem_mtk
LOCAL_SHARED_LIBRARIES := liblog libdl
LOCAL_PRELINK_MODULE := false
include $(BUILD_SHARED_LIBRARY)
endif

################ BT RELAYER ##################
BUILD_BT_RELAYER := true
ifeq ($(BUILD_BT_RELAYER), true)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := bt_relayer.c

LOCAL_C_INCLUDES := \
  $(LOCAL_PATH)/../inc

LOCAL_MODULE := libbluetooth_relayer
LOCAL_SHARED_LIBRARIES := liblog libcutils libbluetoothem_mtk
LOCAL_PRELINK_MODULE := false
include $(BUILD_SHARED_LIBRARY)
endif
