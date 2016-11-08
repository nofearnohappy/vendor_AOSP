#
# Copyright (C) 2011 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

LOCAL_PATH := $(call my-dir)


include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := \
                   src/libipsec/pfkey.c \
                   src/libipsec/ipsec_strerror.c \
                   src/libipsec/policy_token.c \
                   src/libipsec/policy_parse.c \
                   src/libipsec/pfkey_dump.c \
                   src/libipsec/key_debug.c \
                   src/libipsec/ipsec_get_policylen.c \
                   src/libipsec/ipsec_dump_policy.c \
                   src/setkey/setkey.c \
                   src/setkey/log_setky.c \
	           src/setkey/parse.c \
                   src/setkey/token.c \
                   src/setkey_fileio/setkey_fileio.c \
                   test.c

LOCAL_C_INCLUDES += \
	$(LOCAL_PATH) \
	$(LOCAL_PATH)/src/include-glibc \
	$(LOCAL_PATH)/src/libipsec \
        $(LOCAL_PATH)/src/setkey   \
        $(LOCAL_PATH)/src/setkey_fileio 


LOCAL_SHARED_LIBRARIES := libcutils libcrypto

LOCAL_CFLAGS := -DANDROID_CHANGES -DHAVE_CONFIG_H 
LOCAL_CFLAGS += -Wno-sign-compare -Wno-missing-field-initializers
LOCAL_MODULE := libipsec_ims

include $(BUILD_STATIC_LIBRARY)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES:= test.c
LOCAL_MODULE := test_setkey

LOCAL_C_INCLUDES := $(KERNEL_HEADERS)
LOCAL_STATIC_LIBRARIES += libipsec_ims 
LOCAL_SHARED_LIBRARIES := libcutils libcrypto 

include $(BUILD_EXECUTABLE)

include $(call first-makefiles-under,$(LOCAL_PATH)/src)

