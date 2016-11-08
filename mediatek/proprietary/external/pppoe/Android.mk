LOCAL_PATH := $(call my-dir)
include vendor/mediatek/proprietary/external/pppoe/src/libevent/Android.mk

src_dir := src

local_c_includes := \
	src

local_c_includes += $(log_c_includes)

include $(CLEAR_VARS)

VERSION=3.11

sbindir=
PPPOE_PATH=$(sbindir)/pppoe
PPPD_PATH=pppd
PLUGIN_PATH=
PPPOESERVER_PPPD_OPTIONS=
PATHS='-DPPPOE_PATH="$(PPPOE_PATH)"' '-DPPPD_PATH="$(PPPD_PATH)"' \
	'-DPLUGIN_PATH="$(PLUGIN_PATH)"' \
	'-DPPPOE_SERVER_OPTIONS="$(PPPOESERVER_PPPD_OPTIONS)"'

LOCAL_SRC_FILES := \
	${src_dir}/pppoe.c \
	${src_dir}/if.c \
	${src_dir}/debug.c \
	${src_dir}/common.c \
	${src_dir}/ppp.c \
	${src_dir}/discovery.c

LOCAL_CFLAGS := '-DVERSION="$(VERSION)"'
LOCAL_C_INCLUDES := $(local_c_includes)
LOCAL_SHARED_LIBRARIES := libcutils
LOCAL_SHARED_LIBRARIES += $(log_shared_libraries)

LOCAL_MODULE := pppoe

include $(BUILD_EXECUTABLE)

include $(CLEAR_VARS)
LOCAL_CFLAGS := '-DVERSION="$(VERSION)"'
LOCAL_C_INCLUDES := $(local_c_includes)
LOCAL_SHARED_LIBRARIES := libcutils
LOCAL_SHARED_LIBRARIES += $(log_shared_libraries)

LOCAL_SRC_FILES := \
	${src_dir}/relay.c \
	${src_dir}/if.c \
	${src_dir}/debug.c \
	${src_dir}/common.c

LOCAL_MODULE := pppoe-relay

include $(BUILD_EXECUTABLE)


include $(CLEAR_VARS)
LOCAL_CFLAGS := '-DVERSION="$(VERSION)"'
LOCAL_C_INCLUDES := $(local_c_includes)
LOCAL_SHARED_LIBRARIES := libcutils
LOCAL_SHARED_LIBRARIES += $(log_shared_libraries)

LOCAL_SRC_FILES := \
	${src_dir}/pppoe-sniff.c \
	${src_dir}/if.c \
	${src_dir}/debug.c \
	${src_dir}/common.c

LOCAL_MODULE := pppoe-sniff

include $(BUILD_EXECUTABLE)


include $(CLEAR_VARS)
LOCAL_CFLAGS := '-DVERSION="$(VERSION)"' $(PATHS)
#LOCAL_C_INCLUDES := $(local_c_includes)
LOCAL_C_INCLUDES := \
	${LOCAL_PATH}/src/libevent

LOCAL_SHARED_LIBRARIES := libcutils
LOCAL_STATIC_LIBRARIES := libpppoeevent
LOCAL_SHARED_LIBRARIES += $(log_shared_libraries)

LOCAL_SRC_FILES := \
	${src_dir}/pppoe-server.c \
	${src_dir}/if.c \
	${src_dir}/debug.c \
	${src_dir}/common.c \
	${src_dir}/md5.c

LOCAL_MODULE := pppoe-server

include $(BUILD_EXECUTABLE)
