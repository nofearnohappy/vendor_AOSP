#ifndef ANDROID_GUI_SENSOR_DATA_H
#define ANDROID_GUI_SENSOR_DATA_H

#include <stdint.h>
#include <sys/types.h>

#include <utils/Flattenable.h>
#include <utils/Vector.h>

#include <shf_define.h>
#include <shf_hal.h>

#define  FLOAT_MULTIPLY_FACTOR  (1024)

// ----------------------------------------------------------------------------
namespace android {
// ----------------------------------------------------------------------------

class Parcel;
enum {
    DATA_TYPE_UNKNOWN   = SHF_DATA_TYPE_INVALID,
    DATA_TYPE_UINT32    = SHF_DATA_TYPE_UINT32,
    DATA_TYPE_UINT64    = SHF_DATA_TYPE_UINT64,
    DATA_TYPE_FLOAT     = SHF_DATA_TYPE_FLOAT,
};
// ----------------------------------------------------------------------------
class SensorData : public LightFlattenablePod<SensorData>
{
public:
    SensorData(int dataIndex, bool isLast, uint32_t value);
    SensorData(int dataIndex, bool isLast, uint64_t value);
    SensorData(int dataIndex, bool isLast, float value);
    SensorData();
    SensorData(const SensorData& data);
    virtual ~SensorData();

    inline uint32_t getDataIndex() const { return mIndex; }
    inline bool isLast() const { return mIsLast; }
    inline uint32_t getType() const { return mType; }
    inline uint32_t getIntValue() const { return mIntValue; }
    inline uint64_t getLongValue() const { return mLongValue; }
    inline float getFloatValue() const { return mFloatValue; }

    static void flattenVector(const Vector<SensorData>& v, Parcel& parcel);
    static void unflattenVector(const Parcel& parcel, Vector<SensorData>& v);
    static void parse(sensor_trigger_data_t* trigger, Vector<SensorData>& v);
private:
    uint32_t mIndex;
    bool mIsLast;
    uint32_t mType;
    uint32_t mIntValue;
    uint64_t mLongValue;
    float mFloatValue;
};

// ----------------------------------------------------------------------------
}; // namespace android

#endif // ANDROID_GUI_SENSOR_DATA_H
