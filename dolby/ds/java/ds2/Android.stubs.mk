################################################################################
#  This program is protected under international and U.S. copyright
#  laws as an unpublished work. This program is confidential and proprietary to the
#  copyright owners. Reproduction or disclosure, in whole or in part,
#  or the production of derivative works therefrom without the express permission of
#  the copyright owners is prohibited.
# 
#                  Copyright (C) 2014 by Dolby Laboratories,
#                              All rights reserved.
################################################################################

# Build Java documentation and SDK library stub
dolby_ds2_internal_api_file := $(TARGET_OUT_COMMON_INTERMEDIATES)/PACKAGING/dolby_ds2_api.txt

# Generate stub sources using DroidDoc
include $(CLEAR_VARS)
LOCAL_SRC_FILES := $(ds2_java_src)
LOCAL_JAVA_LIBRARIES := $(ds2_java_libs) dolby_ds2
LOCAL_MODULE_CLASS := JAVA_LIBRARIES
LOCAL_DROIDDOC_HTML_DIR :=
LOCAL_DROIDDOC_CUSTOM_TEMPLATE_DIR := build/tools/droiddoc/templates-sdk
LOCAL_DROIDDOC_OPTIONS := \
    -stubs $(call intermediates-dir-for,JAVA_LIBRARIES,dolby_ds2_stubs,,COMMON)/src \
    -api $(dolby_ds2_internal_api_file) \
    -stubpackages com.dolby.api \
    com.dolby.api
LOCAL_UNINSTALLABLE_MODULE := true
LOCAL_MODULE := dolby_ds2_stubs
include $(BUILD_DROIDDOC)
dolby_ds2_stubs_stamp := $(full_target)

# Associate docs with generated api txt file
$(dolby_ds2_internal_api_file): dolby_ds2_stubs-docs

# Build the stub source files into dolby_ds2_stubs.jar
include $(CLEAR_VARS)
LOCAL_MODULE := dolby_ds2_stubs
LOCAL_SOURCE_FILES_ALL_GENERATED := true
include $(BUILD_JAVA_LIBRARY)
$(full_classes_compiled_jar) : $(dolby_ds2_stubs_stamp)
dolby_ds2_stubs_classes_path := $(call intermediates-dir-for,JAVA_LIBRARIES,dolby_ds2_stubs,,COMMON)/classes

dolby_ds2_api_classes := com/dolby/api/DsAccess.class \
                         com/dolby/api/DsAccessException.class \
                         com/dolby/api/DsBase.class \
                         com/dolby/api/DsConstants.class \
                         com/dolby/api/DsFocus.class \
                         com/dolby/api/DsParams.class \
                         com/dolby/api/IDsAccessEvents.class \
                         com/dolby/api/IDsEvents.class \
                         com/dolby/api/IDsVisualizerEvents.class \
                         com/dolby/api/IDsDeathHandler.class
dolby_ds2_stubs.jar: $(full_classes_compiled_jar)
	jar cvf $@ $(foreach param, $(dolby_ds2_api_classes), -C $(dolby_ds2_stubs_classes_path) $(param))
.PHONY: make-dolby_ds2-sdk
make-dolby_ds2-sdk: dolby_ds2_stubs.jar
