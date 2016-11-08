# limitations under the License.
#

# This makefile shows how to build your own shared library that can be
# shipped on the system of a phone, and included additional examples of
# including JNI code with the library and writing client applications against it.

ifneq ($(strip $(MTK_TABLET_PLUGIN_BUILD)),yes)

LOCAL_PATH := $(call my-dir)
    
# MediaTek tablet library.
# ============================================================
include $(CLEAR_VARS)
   
LOCAL_PACKAGE_NAME := TabletPlugin
		
LOCAL_CERTIFICATE := platform
		
LOCAL_SRC_FILES := $(call all-java-files-under, java)
    
LOCAL_PROGUARD_ENABLED := disabled
    
# Put plugin apk together to specific folder
# Specify install path for MTK CIP solution
ifeq ($(strip $(MTK_CIP_SUPPORT)),yes)
	LOCAL_MODULE_PATH := $(TARGET_CUSTOM_OUT)/plugin
else
	LOCAL_MODULE_PATH := $(PRODUCT_OUT)/system/plugin
endif
   
include $(BUILD_PACKAGE)
    
# Include plug-in's makefile to automated generate .mpinfo
include vendor/mediatek/proprietary/frameworks/opt/plugin/mplugin.mk

endif
