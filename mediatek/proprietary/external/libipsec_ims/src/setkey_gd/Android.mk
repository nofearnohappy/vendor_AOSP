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

LOCAL_SRC_FILES := \
                   ../libipsec_good/pfkey.c \
                   ../libipsec_good/ipsec_strerror.c \
                   ../libipsec_good/policy_token.c \
                   ../libipsec_good/policy_parse.c \
                   ../libipsec_good/pfkey_dump.c \
                   ../libipsec_good/key_debug.c \
                   ../libipsec_good/ipsec_get_policylen.c \
                   ../libipsec_good/ipsec_dump_policy.c \
                   setkey.c \
	           parse.c \
                   token.c 
LOCAL_C_INCLUDES += \
	$(LOCAL_PATH) \
	$(LOCAL_PATH)/../include-glibc \
	$(LOCAL_PATH)/../libipsec \
        $(LOCAL_PATH)/../setkey 

LOCAL_SHARED_LIBRARIES := libcutils libcrypto

LOCAL_CFLAGS := -DANDROID_CHANGES -DHAVE_CONFIG_H 

LOCAL_CFLAGS += -Wno-sign-compare -Wno-missing-field-initializers 

LOCAL_MODULE := setkey_gd

LOCAL_MODULE_TAGS := eng


include $(BUILD_EXECUTABLE)



