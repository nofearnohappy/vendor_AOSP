###################################################################
# Include Project Feautre  (cust_bldr.h)
###################################################################

#ifeq ("$(MTK_EMMC_SUPPORT)","yes")
ifdef MTK_EMMC_SUPPORT
CFG_BOOT_DEV :=BOOTDEV_SDMMC
else
CFG_BOOT_DEV :=BOOTDEV_NAND
endif
MTK_PMIC_RST_KEY := 32

CFG_BOOT_ARGUMENT_BY_ATAG :=1