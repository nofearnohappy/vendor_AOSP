LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES:= xlog-filter.c base64.c

LOCAL_MODULE:= libxlog
LOCAL_MODULE_TAGS := optional

include $(BUILD_STATIC_LIBRARY)

include $(CLEAR_VARS)

LOCAL_SRC_FILES:= base64.c

LOCAL_MODULE:= libxlog
LOCAL_MODULE_TAGS := optional
LOCAL_MULTILIB := both

include $(BUILD_HOST_STATIC_LIBRARY)

$(mtklog_config_prop_file): PRIVATE_SRC_FILES := $(mtklog_config_prop_src)
$(mtklog_config_prop_file): $(mtklog_config_prop_src) | $(ACP)
	mkdir -p $(dir $@)
	$(ACP) $(PRIVATE_SRC_FILES) $(mtklog_config_prop_file)

xlog_filter_tags_file := $(TARGET_OUT)/etc/xlog-filter-tags
xlog_filter_default_file := $(TARGET_OUT)/etc/xlog-filter-default
xlog_filter_tags_src := $(LOCAL_PATH)/tags-default.xlog $(LOCAL_PATH)/tags-setting.xlog

$(xlog_filter_tags_file): PRIVATE_SRC_FILES := $(xlog_filter_tags_src)
$(xlog_filter_tags_file): $(xlog_filter_tags_src)
	mkdir -p $(dir $@)
	$(MTK_ROOT)/external/libxlog/libxlog/merge-xlog-filter-tags.py -t $@ $(PRIVATE_SRC_FILES)

$(xlog_filter_default_file): PRIVATE_SRC_FILES := $(xlog_filter_tags_src)
$(xlog_filter_default_file): $(xlog_filter_tags_src)
	mkdir -p $(dir $@)
	$(MTK_ROOT)/external/libxlog/libxlog/merge-xlog-filter-tags.py $(PRIVATE_XLOG_FLAGS) -f $@ $(PRIVATE_SRC_FILES)

ALL_DEFAULT_INSTALLED_MODULES += $(xlog_filter_tags_file) $(xlog_filter_default_file) $(mtklog_config_prop_file)
