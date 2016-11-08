ifeq ($(MTK_HOTKNOT_SUPPORT), yes)

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

#DEFAULT_APP_TARGET_SDK := 19

LOCAL_SRC_FILES := $(call all-java-files-under, src) \
                    src/com/mediatek/hotknotbeam/IHotKnotBeamService.aidl \

LOCAL_PACKAGE_NAME := HotKnotBeam
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true

include $(BUILD_PACKAGE)

# Use the folloing include to make our test apk.
# include $(call all-makefiles-under,$(LOCAL_PATH))

endif
