ifeq ($(strip $(MTK_NFC_SUPPORT)), yes)

LOCAL_PATH:= vendor/mediatek/proprietary/external/mtknfc
########################################
# MTK NFC Package Configuration
# MTK_NFC_PACKAGE = 0 , Google hal
# MTK_NFC_PACKAGE = 1 , AOSP hal (for Basic,Bsp)
# MTK_NFC_PACKAGE = 2 , MTK hal (for TK) 
########################################
ifeq ($(strip $(MTK_NFC_PACKAGE)), 1)
    BUILD_HALIMPL=yes
else
    BUILD_HALIMPL=no
endif

########################################
# MTK NFC Clock Type & Rate Configuration
########################################

ifeq ($(wildcard device/mediatek/$(MTK_TARGET_PROJECT)/nfc.cfg),)
    PRODUCT_COPY_FILES += $(LOCAL_PATH)/nfc.cfg:system/etc/nfc.cfg
else
    PRODUCT_COPY_FILES += device/mediatek/$(MTK_TARGET_PROJECT)/nfc.cfg:system/etc/nfc.cfg
endif

#Copy Mifare lincense file
PRODUCT_COPY_FILES += $(LOCAL_PATH)/MTKNfclicense.lic:system/etc/MTKNfclicense.lic    

ifeq ($(BUILD_HALIMPL), yes)
    PRODUCT_COPY_FILES += $(LOCAL_PATH)/nfcse.cfg:system/etc/nfcse.cfg
    PRODUCT_PACKAGES += nfc_nci.mt6605.default
    PRODUCT_PACKAGES += NfcNci
    PRODUCT_PACKAGES += libmtknfc
    PRODUCT_PACKAGES += libnfc_nci_jni
    PRODUCT_PACKAGES += libnfc-nci
# NFC configure file
PRODUCT_COPY_FILES += \
     $(LOCAL_PATH)/halimpl/libnfc-brcm.conf:system/etc/libnfc-brcm.conf \
     $(LOCAL_PATH)/halimpl/libnfc-brcm-20791b05.conf:system/etc/libnfc-brcm-20791b05.conf
endif
endif

