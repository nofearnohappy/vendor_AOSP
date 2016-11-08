#ifndef ANDROID_GUI_SENSOR_CONTEXT_H
#define ANDROID_GUI_SENSOR_CONTEXT_H

#include <stdint.h>
#include <sys/types.h>

// ----------------------------------------------------------------------------
namespace android {
// ----------------------------------------------------------------------------
class SensorContext
{
public:
    class Clock {
    public:
        static const int CONTEXT_TYPE;
        static const int DATA_INDEX_TIME;
    };
    
    class Pedometer {
    public:
        static const int CONTEXT_TYPE;
        static const int DATA_INDEX_LENGTH;
        static const int DATA_INDEX_FREQUENCY;
        static const int DATA_INDEX_COUNT;
        static const int DATA_INDEX_DISTANCE;
        static const int DATA_INDEX_TIMESTAMP;
    };
    
    class Activity {
    public:
        static const int CONTEXT_TYPE;
        static const int DATA_INDEX_IN_VEHICLE;
        static const int DATA_INDEX_ON_BICYCLE;
        static const int DATA_INDEX_ON_FOOT;
        static const int DATA_INDEX_STILL;
        static const int DATA_INDEX_UNKNOWN;
        static const int DATA_INDEX_TILTING;
        static const int DATA_INDEX_TIMESTAMP;
    };
    
    class InPocket {
    public:
        static const int CONTEXT_TYPE;
        static const int DATA_INDEX_INPOCKE;
        static const int DATA_INDEX_TIMESTAMP;
    };
    
    class MostProbableActivity {
    public:
        static const int CONTEXT_TYPE;
        static const int DATA_INDEX_ACTIVITY;
        static const int DATA_INDEX_CONFIDENCE;
        static const int DATA_INDEX_TIMESTAMP;
    };
    
    class SignificantMotion {
    public:
        static const int CONTEXT_TYPE;
        static const int DATA_INDEX_MOTION_VALUE;
        static const int DATA_INDEX_TIMESTAMP;
    };

    class Pickup {
    public:
        static const int CONTEXT_TYPE;
        static const int DATA_INDEX_PICKUP_VALUE;
        static const int DATA_INDEX_TIMESTAMP;
    };

    class FaceDown {
    public:
        static const int CONTEXT_TYPE;
        static const int DATA_INDEX_FACEDOWN_VALUE;
        static const int DATA_INDEX_TIMESTAMP;
    };

    class Shake {
    public:
        static const int CONTEXT_TYPE;
        static const int DATA_INDEX_SHAKE_VALUE;
        static const int DATA_INDEX_TIMESTAMP;
    };

    class Gesture {
    public:
        static const int CONTEXT_TYPE;
        static const int DATA_INDEX_GESTURE_VALUE;
        static const int DATA_INDEX_TIMESTAMP;
    };

    static int getDataType(int dataIndex);
private:
    SensorContext(void);
    ~SensorContext(void);
};

// ----------------------------------------------------------------------------
}; // namespace android

#endif // ANDROID_GUI_SENSOR_CONTEXT_H
