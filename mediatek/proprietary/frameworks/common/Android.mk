# limitations under the License.
#

# This makefile shows how to build your own shared library that can be
# shipped on the system of a phone, and included additional examples of
# including JNI code with the library and writing client applications against it.

ifneq ($(TARGET_BUILD_PDK),true)
LOCAL_PATH := $(call my-dir)

# MediaTek common library.
# ============================================================
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_MODULE := mediatek-common

LOCAL_MODULE_CLASS := JAVA_LIBRARIES

LOCAL_SRC_FILES := $(call all-java-files-under,src)

LOCAL_AIDL_INCLUDES += $(FRAMEWORKS_BASE_JAVA_SRC_DIRS)

LOCAL_SRC_FILES += src/com/mediatek/common/dm/DmAgent.aidl \
                   src/com/mediatek/common/ppl/IPplManager.aidl \
                   src/com/mediatek/common/voicecommand/IVoiceCommandListener.aidl \
                   src/com/mediatek/common/voicecommand/IVoiceCommandManagerService.aidl \
                   src/com/mediatek/common/operamax/ILoaderStateListener.aidl \
                   src/com/mediatek/common/operamax/ILoaderService.aidl \
                   src/com/mediatek/common/voiceextension/IVoiceExtCommandListener.aidl \
                   src/com/mediatek/common/voiceextension/IVoiceExtCommandManager.aidl \
                   src/com/mediatek/common/audioprofile/IAudioProfileService.aidl \
                   src/com/mediatek/common/audioprofile/IAudioProfileListener.aidl

#LOCAL_SRC_FILES += src/com/mediatek/common/IMyModuleCallback.aidl \
                   src/com/mediatek/common/dm/DmAgent.aidl \

LOCAL_STATIC_JAVA_LIBRARIES := mplugin

# Always use the latest prebuilt Android library.
LOCAL_SDK_VERSION := 19

include $(BUILD_JAVA_LIBRARY)

# Put certificates for MPlugin's security check to plug-in specific folder.
# Specify install path for MTK CIP solution.
ifeq ($(strip $(MTK_CIP_SUPPORT)),yes)
MPLUGIN_CERTIFICATE_PATH := $(TARGET_CUSTOM_OUT)/plugin/Signatures
else
MPLUGIN_CERTIFICATE_PATH := $(PRODUCT_OUT)/system/plugin/Signatures
endif

MPLUGIN_CERTIFICATE_OUT := $(addprefix $(MPLUGIN_CERTIFICATE_PATH)/,mplugin_guard.xml)

$(MPLUGIN_CERTIFICATE_OUT): 
	@echo $@: $<
	mkdir -p $(dir $@)
	DEFAULT_SYSTEM_DEV_CERTIFICATE="$(dir $(DEFAULT_SYSTEM_DEV_CERTIFICATE))" \
	vendor/mediatek/proprietary/frameworks/opt/plugin/mplugin-guard.py vendor/mediatek/proprietary/frameworks/opt/plugin/config.txt vendor/mediatek/proprietary/frameworks/opt/plugin/mplugin_guard.xml -o $@

ALL_MODULES.$(LOCAL_MODULE).INSTALLED += $(MPLUGIN_CERTIFICATE_OUT)
$(LOCAL_MODULE): $(MPLUGIN_CERTIFICATE_OUT)

ifeq ($(strip $(BUILD_MTK_API_DEP)), yes)
# mediatek-common API table.
# ============================================================
LOCAL_MODULE := mediatek-common-api

LOCAL_STATIC_JAVA_LIBRARIES := 
LOCAL_MODULE_CLASS := JAVA_LIBRARIES

LOCAL_DROIDDOC_OPTIONS:= \
		-stubs $(TARGET_OUT_COMMON_INTERMEDIATES)/JAVA_LIBRARIES/mediatek-common-api_intermediates/src \
		-api $(TARGET_OUT_COMMON_INTERMEDIATES)/PACKAGING/mediatek-common-api.txt \
		-nodocs \
        -hidden

include $(BUILD_DROIDDOC)
endif
endif
