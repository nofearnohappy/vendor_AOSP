################################################################################
#  This program is protected under international and U.S. copyright laws as
#  an unpublished work. This program is confidential and proprietary to the
#  copyright owners. Reproduction or disclosure, in whole or in part, or the
#  production of derivative works therefrom without the express permission of
#  the copyright owners is prohibited.
#
#                 Copyright (C) 2014 by Dolby Laboratories,
#                             All rights reserved.
################################################################################

include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
	tests/test_main.cpp \
	DlbBufferProvider.cpp \
	tests/test_DlbBufferProvider.cpp

LOCAL_C_INCLUDES := \
	$(LOCAL_PATH)/include \
	$(LOCAL_PATH)/../common/include \
	$(LOCAL_PATH)/../../adapters/include/ \
	$(LOCAL_PATH)/../../prebuilt/ds/include \
	external/gtest/include

LOCAL_STATIC_LIBRARIES += libgtest_host liblog libcutils libutils

LOCAL_MODULE := libdap-gtest
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_OWNER := dolby

include $(BUILD_HOST_EXECUTABLE)

libdap_gtest_exe := $(HOST_OUT_EXECUTABLES)/$(LOCAL_MODULE)

.PHONY: test-libdap
test-libdap: $(libdap_gtest_exe)
	$< --gtest_color=yes
	@echo All libdap tests PASSED

.PHONY: valgrind-libdap
valgrind-libdap: $(libdap_gtest_exe)
	valgrind --leak-check=full $< --gtest_color=yes
