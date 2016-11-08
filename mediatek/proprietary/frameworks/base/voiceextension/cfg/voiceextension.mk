#
# Copyright (C) 2008 The Android Open Source Project
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

LOCAL_MTK_PATH:= vendor/mediatek/proprietary/frameworks/base/voiceextension/cfg/

PRODUCT_COPY_FILES += \
       $(LOCAL_MTK_PATH)/model/GMMModel1.bin:system/etc/voiceextension/model/GMMModel1.bin \
       $(LOCAL_MTK_PATH)/model/GMMModel2.bin:system/etc/voiceextension/model/GMMModel2.bin \
       $(LOCAL_MTK_PATH)/model/GMMModel3.bin:system/etc/voiceextension/model/GMMModel3.bin \
       $(LOCAL_MTK_PATH)/model/Model1.bin:system/etc/voiceextension/model/Model1.bin \
       $(LOCAL_MTK_PATH)/model/Model2.bin:system/etc/voiceextension/model/Model2.bin \
       $(LOCAL_MTK_PATH)/model/Model3.bin:system/etc/voiceextension/model/Model3.bin \
       $(LOCAL_MTK_PATH)/model/Model4.bin:system/etc/voiceextension/model/Model4.bin \
       $(LOCAL_MTK_PATH)/model/Model5.bin:system/etc/voiceextension/model/Model5.bin \
       $(LOCAL_MTK_PATH)/model/GMN.bin:system/etc/voiceextension/model/GMN.bin \
       $(LOCAL_MTK_PATH)/model/dic1:system/etc/voiceextension/model/dic1 \
       $(LOCAL_MTK_PATH)/model/meta:system/etc/voiceextension/model/meta \
       $(LOCAL_MTK_PATH)/model/splst:system/etc/voiceextension/model/splst
