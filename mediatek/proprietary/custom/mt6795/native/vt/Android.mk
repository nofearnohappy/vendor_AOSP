
# Use project first
ifeq ($(wildcard $(MTK_PATH_CUSTOM)/native/vt),)

#ifeq ($(MTK_VT3G324M_SUPPORT), yes)

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
		custom_vt_video_enc_setting.cpp


LOCAL_PRELINK_MODULE:= false

LOCAL_MODULE:= libvt_custom
LOCAL_MULTILIB := 32
include $(BUILD_SHARED_LIBRARY)

#endif

endif