#
# create loop device bind on IPO-H file for nand project

ifeq ($(MTK_IPOH_SUPPORT), yes)
ifeq ($(MTK_MLC_NAND_SUPPORT),yes)

LOCAL_PATH:=$(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= ipo_swap.c

LOCAL_MODULE:= ipo_swap

LOCAL_C_INCLUDES += vendor/mediatek/proprietary/external/mtd-utils/include

LOCAL_SHARED_LIBRARIES += liblog 

include $(BUILD_EXECUTABLE)

endif
endif
