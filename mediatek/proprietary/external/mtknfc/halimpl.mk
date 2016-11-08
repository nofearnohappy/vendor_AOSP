include $(CLEAR_VARS)

HALIMPL_FOLDER := halimpl

LOCAL_SRC_FILES :=  \
     mtk_nfc_android_main.c  \
     mtk_nfc_sys.c  \
     halimpl/src/mtk_nfc_hal_aosp_main.c  \
     utils/mtk_nfc_log.c

LOCAL_C_INCLUDES:= \
     $(LOCAL_PATH) \
     $(LOCAL_PATH)/inc  \
     $(LOCAL_PATH)/utils  \
     $(LOCAL_PATH)/halimpl/inc \
     
LOCAL_MODULE := nfc_nci.mt6605.default

LOCAL_MODULE_TAGS := optional

LOCAL_SHARED_LIBRARIES := libc libm liblog libcutils libhardware_legacy libmtknfc

LOCAL_CFLAGS := $(D_CFLAGS) -DNFC_HAL_TARGET=TRUE -DNFC_RW_ONLY=TRUE
LOCAL_CFLAGS += -DUSE_GCC -DSUPPORT_I2C  -DSUPPORT_SHARED_LIBRARY -DHALIMPL
LOCAL_CPPFLAGS := $(LOCAL_CFLAGS)