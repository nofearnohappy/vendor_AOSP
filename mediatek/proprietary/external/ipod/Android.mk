#
# ipo daemon
#

ifeq (yes, $(strip $(MTK_IPO_SUPPORT)))

LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES:= ipodmain.cpp \
                  ipodcommon.cpp \
                  ipodlights.cpp \
                  bootlogo.cpp \
				  peripheral_control.cpp \
				  charging_control.cpp \
				  key_control.cpp

ifeq ($(MTK_PUMP_EXPRESS_SUPPORT), yes)
LOCAL_CFLAGS += -DMTK_PUMP_EXPRESS_SUPPORT
endif

ifeq ($(MTK_PUMP_EXPRESS_PLUS_SUPPORT), yes)
LOCAL_CFLAGS += -DMTK_PUMP_EXPRESS_PLUS_SUPPORT
endif

ifeq ($(MTK_TB_WIFI_3G_MODE), WIFI_ONLY)
LOCAL_CFLAGS += -DMTK_TB_WIFI_ONLY
endif

ifeq ($(strip $(MTK_IPO_MDRST_SUPPORT)),yes)
#$(warning reset_modem = 1)
	LOCAL_CFLAGS += -DMTK_RESET_MODEM=1
else
#$(warning reset_modem = 0)
	LOCAL_CFLAGS += -DMTK_RESET_MODEM=0
endif

ifeq ($(MTK_IPOH_SUPPORT), yes)
LOCAL_CFLAGS += -DMTK_IPOH_SUPPORT
endif
LOCAL_CFLAGS += -Wno-unused-variable

ifeq ($(TRUSTONIC_TEE_SUPPORT), yes)
LOCAL_CFLAGS += -DTRUSTONIC_TEE_SUPPORT
endif

LOCAL_C_INCLUDES += $(LOCAL_PATH)/../libshowlogo/ \
 $(LOCAL_PATH)/include \
 $(TOP)/external/zlib/


LOCAL_MODULE:= ipod
LOCAL_MULTILIB := 32

LOCAL_SHARED_LIBRARIES := libcutils libutils libc libstdc++ libz \
	libdl liblog libgui libui libshowlogo \
	libbinder libpowermanager \
	libhardware_legacy

LOCAL_PRELINK_MODULE := false

include $(BUILD_EXECUTABLE)

endif
