/*
* Copyright (C) 2011-2014 MediaTek Inc.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

#define LOG_NDEBUG 0
#define LOG_TAG "SensorHub"
#include <utils/Log.h>

#include <binder/Parcel.h>
#include <ISensorHubClient.h>

namespace android {

enum {
    NOTIFY = IBinder::FIRST_CALL_TRANSACTION,
};

class BpSensorHubClient: public BpInterface<ISensorHubClient>
{
public:
    BpSensorHubClient(const sp<IBinder>& impl)
        : BpInterface<ISensorHubClient>(impl)
    {
    }

    virtual void notify(int msg, int ext1, int ext2, const Parcel *obj)
    {
        Parcel data, reply;
        data.writeInterfaceToken(ISensorHubClient::getInterfaceDescriptor());
        data.writeInt32(msg);
        data.writeInt32(ext1);
        data.writeInt32(ext2);
        if (obj && obj->dataSize() > 0) {
            ALOGV("BpSensorHubClient: srcPos=%d, srcSize=%d, dstPos=%d, dstSize=%d", 
                obj->dataPosition(), obj->dataSize(), data.dataPosition(), data.dataSize());
            data.appendFrom(const_cast<Parcel *>(obj), 0, obj->dataSize());
            ALOGV("BpSensorHubClient: dstPos=%d, dstSize=%d", data.dataPosition(), data.dataSize());
        }
        remote()->transact(NOTIFY, data, &reply, IBinder::FLAG_ONEWAY);
    }
};

IMPLEMENT_META_INTERFACE(SensorHubClient, "com.mediatek.sensorhub.ISensorHubClient");

// ----------------------------------------------------------------------

status_t BnSensorHubClient::onTransact(
    uint32_t code, const Parcel& data, Parcel* reply, uint32_t flags)
{
    switch (code) {
        case NOTIFY: {
            CHECK_INTERFACE(ISensorHubClient, data, reply);
            int msg = data.readInt32();
            int ext1 = data.readInt32();
            int ext2 = data.readInt32();
            ALOGV("BnSensorHubClient: srcPos=%d, srcSize=%d", data.dataPosition(), data.dataSize());			
            Parcel obj;
            if (data.dataAvail() > 0) {
                obj.appendFrom(const_cast<Parcel *>(&data), data.dataPosition(), data.dataAvail());
                ALOGV("BnSensorHubClient: dstPos=%d, dstSize=%d", obj.dataPosition(), obj.dataSize()); 				
                obj.setDataPosition(0);				
            }

            notify(msg, ext1, ext2, &obj);
            return NO_ERROR;
        } break;
        default:
            return BBinder::onTransact(code, data, reply, flags);
    }
}

}; // namespace android
