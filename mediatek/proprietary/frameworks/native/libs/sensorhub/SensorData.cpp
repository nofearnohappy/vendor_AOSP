#define LOG_NDEBUG 0
#define LOG_TAG "SensorHub"

#include <binder/Parcel.h>
#include <utils/Log.h>

#include <SensorData.h>
#include <SensorContext.h>

namespace android {
// ----------------------------------------------------------------------------
SensorData::SensorData(int dataIndex, bool isLast, uint32_t value)
    :mIndex(dataIndex), mIsLast(isLast), mType(DATA_TYPE_UINT32),
    mIntValue(value), mLongValue(0L), mFloatValue(0.0F)
{
}

SensorData::SensorData(int dataIndex, bool isLast, uint64_t value)
    :mIndex(dataIndex), mIsLast(isLast), mType(DATA_TYPE_UINT64),
    mIntValue(0), mLongValue(value), mFloatValue(0.0F)
{
}

SensorData::SensorData(int dataIndex, bool isLast, float value)
    :mIndex(dataIndex), mIsLast(isLast), mType(DATA_TYPE_FLOAT),
    mIntValue(0), mLongValue(0L), mFloatValue(value)
{
}

SensorData::SensorData()
    :mIndex(SHF_DATA_INDEX_INVALID), mIsLast(false), mType(SHF_DATA_TYPE_INVALID),
    mIntValue(0), mLongValue(0L), mFloatValue(0.0F)
{
}

SensorData::SensorData(const SensorData& data)
    :mIndex(data.mIndex), mIsLast(data.mIsLast), mType(data.mType),
    mIntValue(data.mIntValue), mLongValue(data.mLongValue), mFloatValue(data.mFloatValue)
{
}

SensorData::~SensorData()
{
    mIndex = 0;
    mIsLast = false;
    mType = DATA_TYPE_UNKNOWN;
    mIntValue = 0;
    mLongValue = 0L;
    mFloatValue = 0.0F;
}

void SensorData::flattenVector(const Vector<SensorData>& v, Parcel& parcel)
{
    size_t n = v.size();
    ALOGV("flattenVector>>>size=%d, pos=%d, psize=%d", n, parcel.dataPosition(), parcel.dataSize());	
    parcel.writeInt32(n);
    for (size_t i = 0; i < n; i++) {
        int status = parcel.write(v[i]);
        //ALOGV("flattenVector: index=%d, result=%d, pos=%d, size=%d", i, status, parcel.dataPosition(), parcel.dataSize());	
        ALOGV("flattenVector: d[%d]=[%d %d %d %lld %f]", i, v[i].getDataIndex(), v[i].getType(), 
            v[i].getIntValue(), v[i].getLongValue(), v[i].getFloatValue());
    }
    ALOGV("flattenVector<<<pos=%d, psize=%d", parcel.dataPosition(), parcel.dataSize());
}

void SensorData::unflattenVector(const Parcel& parcel, Vector<SensorData>& v)
{
    int32_t n = parcel.readInt32();
    ALOGV("unflattenVector>>>size=%d, pos=%d, psize=%d", n, parcel.dataPosition(), parcel.dataSize());	
    while (n--) {
        SensorData data;
        int status = parcel.read(data);
        //ALOGV("unflattenVector: index=%d, result=%d, pos=%d, size=%d", n, status, parcel.dataPosition(), parcel.dataSize());
        ALOGV("unflattenVector: d[%d]=[%d %d %d %lld %f]", n, data.getDataIndex(), data.getType(), 
            data.getIntValue(), data.getLongValue(), data.getFloatValue());
        v.add(data);
    }
    ALOGV("unflattenVector<<<");	
}

void SensorData::parse(sensor_trigger_data_t* trigger, Vector<SensorData>& v)
{
    for (size_t i = 0; i < SHF_CONDITION_ITEM_SIZE * 2; i++) {
        trigger_data_t* pd = trigger->data + i;
        if (!pd->index) {
            break;
        }
        switch(pd->type) {
        case DATA_TYPE_UINT32: {
            int type = SensorContext::getDataType(pd->index);
            if(DATA_TYPE_FLOAT == type) {
                SensorData t1(pd->index, false, (float)pd->value.duint32/FLOAT_MULTIPLY_FACTOR);
                v.add(t1);
                SensorData t2(pd->index, true, (float)pd->last.duint32/FLOAT_MULTIPLY_FACTOR);
                v.add(t2);
            } else {
                SensorData t1(pd->index, false, pd->value.duint32);
                v.add(t1);
                SensorData t2(pd->index, true, pd->last.duint32);
                v.add(t2);
            }
            ALOGV("parse: index=%d, current=%d, last=%d", pd->index, pd->value.duint32, pd->last.duint32);
        }
            break;
        case DATA_TYPE_UINT64: {
            SensorData t1(pd->index, false, *((uint64_t*)(&pd->value.dtime)));
            v.add(t1);
            SensorData t2(pd->index, true, *((uint64_t*)(&pd->last.dtime)));
            v.add(t2);
            ALOGV("parse2: index=%d, current=%lld, last=%lld", pd->index, pd->value.dtime, pd->last.dtime);
        }
            break;
        default:
            ALOGW("parse: unsupported type %d", pd->type);			
            break;
        }
    }
}
};
