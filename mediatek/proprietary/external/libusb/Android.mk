
$(warning $(call all-subdir-makefiles))

ifeq ($(MTK_ICUSB_SUPPORT),yes)
    include $(call all-subdir-makefiles)
endif


