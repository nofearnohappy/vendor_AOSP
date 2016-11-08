#
# Copyright (C) 2009-2011 The Android-x86 Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#

MTK_PROJECT_NAME := $(subst full_,,$(TARGET_PRODUCT))
MTK_PROJECT_LOCATION := $(shell find device/* -maxdepth 1 -name $(MTK_PROJECT_NAME))
PRJ_MF := $(MTK_PROJECT_LOCATION)/ProjectConfig.mk
include $(PRJ_MF)

export MTK_NAND_PAGE_SIZE
export MTK_EMMC_SUPPORT
export MTK_NAND_UBIFS_SUPPORT
export FULL_PROJECT
export MTK_COMBO_NAND_SUPPORT
export TRUSTONIC_TEE_SUPPORT
export MTK_SEC_SECRO_AC_SUPPORT
export MTK_NAND_PAGE_SIZE
export MTK_TARGET_PROJECT_FOLDER=$(MTK_PROJECT_LOCATION)

SHOWTIMECMD   =  date "+%Y/%m/%d %H:%M:%S"
SHOWTIME      =  $(shell $(SHOWTIMECMD))
LOG_DIR =  out/target/product/
OUT_PRJ_DIR =  $(LOG_DIR)$(MTK_PROJECT_NAME)
TOOL_DIR = vendor/mediatek/proprietary/scripts/sign-image

DEAL_STDOUT_SIGN_IMAGE := 2>&1 | tee -a $(LOG_DIR)$(MTK_PROJECT_NAME)_sign-image.log

# if MTK_BASE_PROJECT is empty, set to MTK_PROJECT as MTK_BASE_PROJECT(base project)
ifeq ($(strip $(MTK_BASE_PROJECT)),)
  MTK_BASE_PROJECT := $(MTK_PROJECT)
endif

SEC_IMG_CUST_DIR = vendor/mediatek/proprietary/custom/$(MTK_BASE_PROJECT)/security/image_auth

# set to base project if MTK_PROJECT_FOLDER is empty, MTK_BASE_PROJECT represents base project
ifeq ($(strip $(MTK_PATH_CUSTOM)),)
  MTK_PATH_CUSTOM := $(MTK_BASE_PROJECT)
endif

SIGN_TOOL := $(TOOL_DIR)/SignTool.pl
SIGN_SCRIPT := $(TOOL_DIR)/SignTool.sh

.PHONY: sign-image

sign-image:
ifeq ($(MTK_DEPENDENCY_AUTO_CHECK), true)
	-@echo [Update] $@: $?
endif
	$(hide) echo $(SHOWTIME) $@ ing ...
	$(hide) echo -e \\t\\t\\t\\b\\b\\b\\bLOG: $(LOG_DIR)$(MTK_PROJECT_NAME)_$@.log
	$(hide) rm -f $(LOG_DIR)$(MTK_PROJECT_NAME)_$@.log $(LOG_DIR)$(MTK_PROJECT_NAME)_$@.log_err
	$(hide) perl $(SIGN_TOOL) $(MTK_BASE_PROJECT) $(MTK_PROJECT_NAME) $(MTK_PATH_CUSTOM) $(MTK_SEC_SECRO_AC_SUPPORT) $(MTK_NAND_PAGE_SIZE) $(PRODUCT_OUT) $(OUT_DIR) $(DEAL_STDOUT_SIGN_IMAGE) 

