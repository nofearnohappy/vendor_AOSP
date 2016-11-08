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

# enable this build only when platform library is available
ifeq ($(TARGET_BUILD_JAVA_SUPPORT_LEVEL),platform)

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_AIDL_INCLUDES := $(LOCAL_PATH)/src/java

LOCAL_SRC_FILES := \
    src/java/com/android/internal/tedongle/IPhoneStateListener.aidl \
    src/java/com/android/internal/tedongle/ICallServiceAdapter.aidl \
    src/java/com/android/internal/tedongle/ISub.aidl \
    src/java/com/android/internal/tedongle/IIccPhoneBook.aidl \
    src/java/com/android/internal/tedongle/IWapPushManager.aidl \
    src/java/com/android/internal/tedongle/ITelephonyRegistry.aidl \
    src/java/com/android/internal/tedongle/ITelephony.aidl \
    src/java/com/android/internal/tedongle/IPhoneSubInfo.aidl \
    src/java/com/android/internal/tedongle/ISms.aidl \
    src/java/com/android/internal/tedongle/IMms.aidl \
    src/java/com/android/internal/tedongle/ICallService.aidl \
    src/java/com/android/internal/tedongle/ITedongle.aidl \
    src/java/com/android/internal/tedongle/ITedongleStateListener.aidl \
    src/java/com/android/internal/tedongle/IOnSubscriptionsChangedListener.aidl  \
    src/java/com/android/internal/tedongle/ICarrierConfigLoader.aidl

LOCAL_SRC_FILES += $(call all-java-files-under, src/java) \
                   $(call all-logtags-files-under, src/java) \
                   $(call all-java-files-under, ../../base/telecomm/java) \
                   $(call all-Iaidl-files-under, ../../base/telecomm/java) \

#LOCAL_JAVA_LIBRARIES := voip-common ims-common mediatek-framework
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := tedongle-telephony

include $(BUILD_JAVA_LIBRARY)

# Include subdirectory makefiles
# ============================================================
include $(call all-makefiles-under,$(LOCAL_PATH))

endif # JAVA platform
