#
# Copyright (C) 2015 MediaTek Inc.
# Modification based on code covered by the mentioned copyright
# and/or permission notice(s).
#
# Copyright 2005 The Android Open Source Project

LOCAL_PATH:= $(call my-dir)

# --

ifneq (,$(filter userdebug eng,$(TARGET_BUILD_VARIANT)))
init_options += -DALLOW_LOCAL_PROP_OVERRIDE=1 -DALLOW_DISABLE_SELINUX=1
init_options += -DINIT_ENG_BUILD
else
ifeq ($(strip $(MTK_BUILD_ROOT)),yes)
init_options += -DALLOW_LOCAL_PROP_OVERRIDE=1 -DALLOW_DISABLE_SELINUX=1
else
init_options += -DALLOW_LOCAL_PROP_OVERRIDE=0 -DALLOW_DISABLE_SELINUX=0
endif
endif

# add mtk fstab flags support
init_options += -DMTK_FSTAB_FLAGS
# end

# add for mtk init
ifneq ($(BUILD_MTK_LDVT), yes)
init_options += -DMTK_INIT
endif
# end

ifeq ($(strip $(MTK_TC7_FEATURE)),yes)
LOCAL_CFLAGS += -DMTK_TC7_FEATURE
endif

init_options += -DLOG_UEVENTS=0

init_cflags += \
    $(init_options) \
    -Wall -Wextra \
    -Wno-unused-parameter \
    -Werror \

init_clang := true

# --

include $(CLEAR_VARS)
SYSDIR := ../../../../../../system/core/init
LOCAL_CPPFLAGS := $(init_cflags)
LOCAL_SRC_FILES:= \
    $(SYSDIR)/bootchart.cpp \
    $(SYSDIR)/builtins.cpp \
    $(SYSDIR)/devices.cpp \
    init.cpp \
    $(SYSDIR)/keychords.cpp \
    $(SYSDIR)/property_service.cpp \
    $(SYSDIR)/signal_handler.cpp \
    $(SYSDIR)/ueventd.cpp \
    $(SYSDIR)/ueventd_parser.cpp \
    $(SYSDIR)/watchdogd.cpp \

ifeq ($(strip $(MTK_NAND_UBIFS_SUPPORT)),yes)
LOCAL_CFLAGS += -DMTK_UBIFS_SUPPORT
endif
LOCAL_MODULE:= multi_init
LOCAL_C_INCLUDES += \
    system/extras/ext4_utils \
    system/core/mkbootimg \
    system/core/init

LOCAL_FORCE_STATIC_EXECUTABLE := true
LOCAL_MODULE_PATH := $(TARGET_ROOT_OUT_SBIN)
LOCAL_UNSTRIPPED_PATH := $(TARGET_ROOT_OUT_SBIN_UNSTRIPPED)

LOCAL_STATIC_LIBRARIES := \
    libinit \
    libfs_mgr \
    libsquashfs_utils \
    liblogwrap \
    libbase \
    libext4_utils_static \
    libcutils \
    libutils \
    liblog \
    libc \
    libselinux \
    libmincrypt \
    libc++_static \
    libdl \
    libsparse_static \
    libz


LOCAL_CLANG := $(init_clang)
include $(BUILD_EXECUTABLE)

