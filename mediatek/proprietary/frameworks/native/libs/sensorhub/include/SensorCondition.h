#ifndef ANDROID_GUI_SENSOR_CONDITION_H
#define ANDROID_GUI_SENSOR_CONDITION_H

#include <stdint.h>
#include <sys/types.h>

#include <utils/Errors.h>
#include <utils/Flattenable.h>
#include <utils/String8.h>
#include <utils/Vector.h>

#include <shf_define.h>
//#include "shf_define.h"

// ----------------------------------------------------------------------------
namespace android {

enum {
    CONDITION_TYPE_INVALID  = 0x00,
    CONDITION_TYPE_CMP      = 0x20,
    CONDITION_TYPE_DIF      = 0x10,
    CONDITION_TYPE_DATA     = 0x0F,
};
enum {
    OPERATION_ANY                   = SHF_OPERATION_ANY,
    OPERATION_INVALID               = SHF_OPERATION_INVALID,
    OPERATION_MORE_THAN             = SHF_OPERATION_MORE_THAN,
    OPERATION_MORE_THAN_OR_EQUAL    = SHF_OPERATION_MORE_THAN_OR_EQUAL,
    OPERATION_LESS_THAN             = SHF_OPERATION_LESS_THAN,
    OPERATION_LESS_THAN_OR_EQUAL    = SHF_OPERATION_LESS_THAN_OR_EQUAL,
    OPERATION_EQUAL                 = SHF_OPERATION_EQUAL,
    OPERATION_NOT_EQUAL             = SHF_OPERATION_NOT_EQUAL,
    OPERATION_MOD                   = SHF_OPERATION_MOD,
};
enum {
    COMBINE_INVALID                 = SHF_COMBINE_INVALID,
    COMBINE_AND                     = SHF_COMBINE_AND,
    COMBINE_OR                      = SHF_COMBINE_OR,
};
// ----------------------------------------------------------------------------
class SensorConditionItem : public LightFlattenablePod<SensorConditionItem>
{
public:
    SensorConditionItem(int index, bool isLast, int operation, uint32_t value);
    SensorConditionItem(int index, bool isLast, int operation, uint64_t value);
    SensorConditionItem(int index, bool isLast, int operation, float value);
    SensorConditionItem(int index1, bool isLast1, int operation, int index2, bool isLast2);
    SensorConditionItem(int index1, bool isLast1, int operation, int index2, bool isLast2, uint32_t value);
    SensorConditionItem(int index1, bool isLast1, int operation, int index2, bool isLast2, uint64_t value);
    SensorConditionItem(int index1, bool isLast1, int operation, int index2, bool isLast2, float value);
    SensorConditionItem(int index1, bool isLast1, int operation, int index2, bool isLast2,
        int dataType, uint32_t ivalue, uint64_t lvalue, float fvalue,
        bool bracketLeft, bool bracketRight, int combine,
        int conditionType);

    SensorConditionItem();
    SensorConditionItem(const SensorConditionItem& item);

    inline void setCombine(int combineType) {
        mCombine = combineType;
    }
    
    inline void setBracketLeft() {
        mBracketLeft = true;
    }
    
    inline void setBracketRight() {
        mBracketRight = true;
    }
    
    inline bool getBracketRight() const{
        return mBracketRight;
    }
    
    inline bool getBracketLeft() const{
        return mBracketLeft;
    }
    
    void getStruct(shf_condition_item_t* item) const;
    void getIndexList(Vector<int>& v) const;
    bool operator==(const SensorConditionItem& other) const;

private:
    mutable bool mBracketLeft;
    mutable bool mBracketRight;
    mutable int mCombine;

    int mIndex1;
    bool mIsLast1;
    int mOperation;

    int mIndex2;
    bool mIsLast2;

    int mDataType;//data type
    uint32_t mIntValue;
    uint64_t mLongValue;
    float mFloatValue;

    int mConditionType;
};

// ----------------------------------------------------------------------------
class SensorCondition : public LightFlattenable<SensorCondition>
{
public:
    SensorCondition();
    SensorCondition(const SensorCondition& condition);
    virtual ~SensorCondition();

    static SensorCondition create(int index, bool isLast, int operation, uint32_t value);
    static SensorCondition create(int index, bool isLast, int operation, uint64_t value);
    static SensorCondition create(int index, bool isLast, int operation, float value);
    static SensorCondition create(int index1, bool isLast1, int operation, int index2, bool isLast2);
    static SensorCondition create(int index1, bool isLast1, int operation, int index2, bool isLast2, uint32_t value);
    static SensorCondition create(int index1, bool isLast1, int operation, int index2, bool isLast2, uint64_t value);
    static SensorCondition create(int index1, bool isLast1, int operation, int index2, bool isLast2, float value);
    static SensorCondition combineWithAnd(const SensorCondition& c1, const SensorCondition& c2);
    static SensorCondition combineWithOr(const SensorCondition& c1, const SensorCondition& c2);
    static SensorCondition combineWithBracket(const SensorCondition& c);

    void add(const SensorConditionItem& item);
    void getStruct(shf_condition_t* condition) const;
    void getIndexList(Vector<int>& v) const;
    bool operator==(const SensorCondition& other) const;	

    // LightFlattenable protocol
    inline bool isFixedSize() const { return false; }
    virtual size_t getFlattenedSize() const;
    virtual status_t flatten(void* buffer, size_t size) const;
    virtual status_t unflatten(void const* buffer, size_t size);
private:
    mutable Vector<SensorConditionItem> mList;
};

// ----------------------------------------------------------------------------
}; // namespace android

#endif // ANDROID_GUI_SENSOR_CONDITION_H
