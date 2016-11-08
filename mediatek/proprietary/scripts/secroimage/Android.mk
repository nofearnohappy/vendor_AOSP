#
# Copyright (C) 2009-2011 The Android-x86 Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#

ifneq ($(strip $(MTK_PROJECT_NAME)),)

MTK_SEC_SECRO_AC_SUPPORT := yes
INSTALLED_SECROIMAGE_TARGET = $(OUT_DIR)/target/product/$(MTK_TARGET_PROJECT)/secro.img

SECRO_POST_SCRIPT := vendor/mediatek/proprietary/scripts/secroimage/secro_post.pl
SECRO_TYPE := GMP

ifeq ($(strip $(SECRO_TYPE)),GMP)
SECRO_CONFIG := vendor/mediatek/proprietary/custom/common/secro/SECRO_GMP.ini
else
SECRO_CONFIG := vendor/mediatek/proprietary/custom/common/secro/SECRO_DEFAULT_LOCK_CFG.ini
endif

secroimage: 
	@perl $(SECRO_POST_SCRIPT) $(SECRO_CONFIG) $(MTK_PROJECT) $(MTK_PATH_CUSTOM) $(MTK_SEC_SECRO_AC_SUPPORT) $(INSTALLED_SECROIMAGE_TARGET) $(SECRO_TYPE) $(MTK_TARGET_PROJECT) $(OUT_DIR) $(HOST_OS)

droidcore: secroimage

endif
