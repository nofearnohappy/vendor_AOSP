ifneq ($(MTK_EMULATOR_SUPPORT), yes)

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
  IPQService.cpp \
  PQService.cpp \
  PQClient.cpp

ifeq ($(strip $(MTK_PQ_COLOR_MODE)),MDP)
    LOCAL_CFLAGS += -DDISP_COLOR_OFF
else ifeq ($(strip $(MTK_PQ_COLOR_MODE)),OFF)
    LOCAL_CFLAGS += -DDISP_COLOR_OFF
endif

ifeq ($(strip $(MTK_PQ_COLOR_MODE)),MDP)
    LOCAL_CFLAGS += -DMDP_COLOR_ENABLE
else ifeq ($(strip $(MTK_PQ_COLOR_MODE)),DISP_MDP)
    LOCAL_CFLAGS += -DMDP_COLOR_ENABLE
endif

ifneq (,$(filter $(strip $(TARGET_BOARD_PLATFORM)), mt6580))
    LOCAL_CFLAGS += -DCCORR_OFF
endif

LOCAL_C_INCLUDES += \
        $(TOP)/$(MTK_PATH_SOURCE)/hardware/dpframework/include \
        $(TOP)/frameworks/base/include \
        $(TOP)/$(MTK_PATH_PLATFORM)/kernel/drivers/dispsys \
        $(MTK_PATH_PLATFORM)/hardware/pq \
        $(TOP)/$(MTK_PATH_SOURCE)/platform/$(LC_MTK_PLATFORM)/kernel/drivers/dispsys \
        $(TOP)/$(MTK_PATH_SOURCE)/kernel/drivers/video \
        $(TOP)/$(MTK_PATH_SOURCE)/kernel/include \
        $(TOP)/$(MTK_PATH_SOURCE)/hardware/pq/v2.0/include \
        $(TOP)/$(MTK_PATH_SOURCE)/hardware/pq/v2.0/lib \
        $(TOP)/$(MTK_ROOT)/frameworks-ext/native/include \


LOCAL_SHARED_LIBRARIES := \
    libutils \
    libcutils \
    libdl \
    libbinder \

LOCAL_MODULE:= libpqservice


include $(BUILD_SHARED_LIBRARY)

endif