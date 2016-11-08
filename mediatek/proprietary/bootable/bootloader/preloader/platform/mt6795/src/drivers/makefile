###################################################################
# flag
###################################################################

MOD_SRC  =
MOD_OBJ  = $(PRELOADER_OUT)/obj

###################################################################
# source files
# - add your file here !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
###################################################################

MOD_SRC  := \
	circbuf.c \
	mmc_common_inter.c \
	mmc_core.c \
	mmc_rpmb.c \
	mmc_test.c \
	msdc.c \
	msdc_utils.c \
	i2c.c \
	keypad.c \
	usbd.c \
	usbphy.c \
	usbtty.c \
	timer.c \
	wdt.c \
	efi.c \
	platform.c \
	pll.c \
	device_apc.c \
	gpio.c \
	uart.c \
 	pmic_wrap_init.c \
	pmic.c \
	leds.c \
	rtc.c \
	da9210.c \
	memory_test.s \
	sync_write.c \
	mt_ptp2.c \
	hotplug.s \
	pmic_auxadc.c \
	clkbuf_ctl.c
	
ifeq ("$(MACH_TYPE)","6595")
	MOD_SRC  +=	mt_ptp.c
	MOD_SRC  +=	emi.c
	MOD_SRC  +=	dramc_pi_basic_api.c
	MOD_SRC  +=	dramc_pi_calibration_api.c
	MOD_SRC  +=	memory.c	
	MOD_SRC  +=	spm_mtcmos.c	
else
	MOD_SRC  +=	mt_ptp_64.c
	MOD_SRC  +=	emi_64.c
	MOD_SRC  +=	dramc_pi_basic_api_64.c
	MOD_SRC  +=	dramc_pi_calibration_api_64.c
	MOD_SRC  +=	memory_64.c
	MOD_SRC  +=	spm_mtcmos_64.c
endif

ifneq ("$(MTK_EMMC_SUPPORT)","yes")
	MOD_SRC  += 	nand.c
	MOD_SRC  +=	bmt.c
endif

ifeq ("$(PL_MODE)","DUMMY_AP")
	MOD_SRC  += gpio_init.c
endif

ifeq ("$(PL_MODE)","TINY")
	MOD_SRC  += gpio_init.c
endif

ifeq ("$(MTK_BQ24160_SUPPORT)","yes")
	MOD_SRC += bq24160.c	
endif

ifeq ("$(CFG_RAM_CONSOLE)", "1")
	MOD_SRC	+= ram_console.c
endif

###################################################################
# target
###################################################################

include $(COMMON_FILE_MK)

###################################################################
# trigger emigen
###################################################################
$(MOD_OBJ)/emi.o: $(EMIGEN_H_OUT)/custom_emi.h
$(MOD_OBJ)/emi_64.o: $(EMIGEN_H_OUT)/custom_emi.h
