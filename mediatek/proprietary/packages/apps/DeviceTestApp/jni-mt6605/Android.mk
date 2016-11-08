ifeq ($(MTK_NFC_SUPPORT), yes)

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

MY_LOCAL_PATH := $(LOCAL_PATH)


LOCAL_SRC_FILES:= \
    com_mediatek_nfc_dta_NativeDtaManager.cpp \
    com_mediatek_nfc_dta.cpp \
    DtaApi.cpp \
    DtaFlow.cpp \
    DtaFlowT1T.cpp \
    DtaFlowT2T.cpp \
    DtaFlowT3T.cpp \
    DtaFlowT4T.cpp \
    DtaFlowP2P.cpp
  
LOCAL_C_INCLUDES += \
    $(JNI_H_INCLUDE)  \
    packages/apps/Nfc/mtk-nfc/jni-mt6605 \
    vendor/mediatek/proprietary/external/mtknfc/inc \
    $(MY_LOCAL_PATH)


LOCAL_SHARED_LIBRARIES := \
    libnativehelper \
    libcutils \
    libutils \
    libnfc_mt6605_jni


#LOCAL_CFLAGS += -O0 -g

LOCAL_MODULE := libdta_mt6605_jni
LOCAL_MODULE_TAGS := optional eng

include $(BUILD_SHARED_LIBRARY)

endif
