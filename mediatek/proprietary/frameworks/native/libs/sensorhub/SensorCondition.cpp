#define LOG_NDEBUG 0
#define LOG_TAG "[SensorCondition]"
#include <utils/Log.h>

#include <SensorCondition.h>
#include <SensorData.h>

namespace android {
// ----------------------------------------------------------------------------

SensorConditionItem::SensorConditionItem(int index, bool isLast, int operation, uint32_t value)
    :mBracketLeft(false), mBracketRight(false), mCombine(0),
    mIndex1(index), mIsLast1(isLast), mOperation(operation),
    mIndex2(0), mIsLast2(false),
    mDataType(DATA_TYPE_UINT32), mIntValue(value), mLongValue(0L), mFloatValue(0.0F),
    mConditionType(CONDITION_TYPE_DATA)
{
}
   
SensorConditionItem::SensorConditionItem(int index, bool isLast, int operation, uint64_t value)
    :mBracketLeft(false), mBracketRight(false), mCombine(0),
    mIndex1(index), mIsLast1(isLast), mOperation(operation),
    mIndex2(0), mIsLast2(false),
    mDataType(DATA_TYPE_UINT64), mIntValue(0), mLongValue(value), mFloatValue(0.0F),
    mConditionType(CONDITION_TYPE_DATA)
{
}

SensorConditionItem::SensorConditionItem(int index, bool isLast, int operation, float value)
    :mBracketLeft(false), mBracketRight(false), mCombine(0),
    mIndex1(index), mIsLast1(isLast), mOperation(operation),
    mIndex2(0), mIsLast2(false),
    mDataType(DATA_TYPE_FLOAT), mIntValue(0), mLongValue(0L), mFloatValue(value),
    mConditionType(CONDITION_TYPE_DATA)
{
}

SensorConditionItem::SensorConditionItem(int index1, bool isLast1, int operation, int index2, bool isLast2)
    :mBracketLeft(false), mBracketRight(false), mCombine(0),
    mIndex1(index1), mIsLast1(isLast1), mOperation(operation),
    mIndex2(index2), mIsLast2(isLast2),
    mDataType(0), mIntValue(0), mLongValue(0L), mFloatValue(0.0F),
    mConditionType(CONDITION_TYPE_CMP)
{
}

SensorConditionItem::SensorConditionItem(int index1, bool isLast1, int operation, int index2, bool isLast2, uint32_t value)
    :mBracketLeft(false), mBracketRight(false), mCombine(0),
    mIndex1(index1), mIsLast1(isLast1), mOperation(operation),
    mIndex2(index2), mIsLast2(isLast2),
    mDataType(DATA_TYPE_UINT32), mIntValue(value), mLongValue(0L), mFloatValue(0.0F),
    mConditionType(CONDITION_TYPE_DIF)
{
}

SensorConditionItem::SensorConditionItem(int index1, bool isLast1, int operation, int index2, bool isLast2, uint64_t value)
    :mBracketLeft(false), mBracketRight(false), mCombine(0),
    mIndex1(index1), mIsLast1(isLast1), mOperation(operation),
    mIndex2(index2), mIsLast2(isLast2),
    mDataType(DATA_TYPE_UINT64), mIntValue(0), mLongValue(value), mFloatValue(0.0F),
    mConditionType(CONDITION_TYPE_DIF)
{
}

SensorConditionItem::SensorConditionItem(int index1, bool isLast1, int operation, int index2, bool isLast2, float value)
    :mBracketLeft(false), mBracketRight(false), mCombine(0),
    mIndex1(index1), mIsLast1(isLast1), mOperation(operation),
    mIndex2(index2), mIsLast2(isLast2),
    mDataType(DATA_TYPE_FLOAT), mIntValue(0), mLongValue(0L), mFloatValue(value),
    mConditionType(CONDITION_TYPE_DIF)
{
}

SensorConditionItem::SensorConditionItem(int index1, bool isLast1, int operation, int index2, bool isLast2,
        int dataType, uint32_t ivalue, uint64_t lvalue, float fvalue,
        bool bracketLeft, bool bracketRight, int combine,
        int conditionType)
    :mBracketLeft(bracketLeft), mBracketRight(bracketRight), mCombine(combine),
    mIndex1(index1), mIsLast1(isLast1), mOperation(operation),
    mIndex2(index2), mIsLast2(isLast2),
    mDataType(dataType), mIntValue(ivalue), mLongValue(lvalue), mFloatValue(fvalue),
    mConditionType(conditionType)
{
}

SensorConditionItem::SensorConditionItem()
    :mBracketLeft(false), mBracketRight(false), mCombine(0),
    mIndex1(SHF_DATA_INDEX_INVALID), mIsLast1(false), mOperation(OPERATION_INVALID),
    mIndex2(SHF_DATA_INDEX_INVALID), mIsLast2(false),
    mDataType(0), mIntValue(0), mLongValue(0L), mFloatValue(0.0F),
    mConditionType(CONDITION_TYPE_CMP)
{
}

SensorConditionItem::SensorConditionItem(const SensorConditionItem& item)
    :mBracketLeft(item.mBracketLeft), mBracketRight(item.mBracketRight), mCombine(item.mCombine),
    mIndex1(item.mIndex1), mIsLast1(item.mIsLast1), mOperation(item.mOperation),
    mIndex2(item.mIndex2), mIsLast2(item.mIsLast2),
    mDataType(item.mDataType), mIntValue(item.mIntValue), mLongValue(item.mLongValue), mFloatValue(item.mFloatValue),
    mConditionType(item.mConditionType)
{
}

void SensorConditionItem::getStruct(shf_condition_item_t* item) const
{
    memset(item, 0, sizeof(shf_condition_item_t));
    item->dindex1 = mIndex1;
    item->dindex2 = mIndex2;
    item->op = mOperation;
    item->combine = mCombine;	
    switch (mDataType) {
    case DATA_TYPE_UINT32: {
        item->value.duint32 = mIntValue;
    }
        break;
    case DATA_TYPE_UINT64: {
        ALOGV("getStruct: value=%lld", mLongValue);
        timestamp_set(&item->value.dtime, mLongValue);
    }
        break;
    case DATA_TYPE_FLOAT: {
        uint32_t temp = mFloatValue * FLOAT_MULTIPLY_FACTOR;
        ALOGV("getStruct: float(%f) -> int(%u)", mFloatValue, temp);		
        item->value.duint32 = temp;
    }
        break;
    default:
        break;
    }
    if (mIsLast1) {
        item->op |= SHF_OPERATION_MASK_OLD1;
    }
    if (mIsLast2) {
        item->op |= SHF_OPERATION_MASK_OLD2;
    }
    if (mConditionType == CONDITION_TYPE_CMP) {
        item->op |= SHF_OPERATION_MASK_CMP;
    } else if (mConditionType == CONDITION_TYPE_DIF) {
        item->op |= SHF_OPERATION_MASK_DIF;
    }
    if (mBracketLeft) {
        item->combine |= SHF_COMBINE_MASK_BRACKET_LEFT;
    }
}

void SensorConditionItem::getIndexList(Vector<int>& v) const
{
    if (mConditionType == CONDITION_TYPE_CMP) {
        v.add(mIndex1);
        v.add(mIndex2);
    } else if (mConditionType == CONDITION_TYPE_DIF) {
        v.add(mIndex1);
        v.add(mIndex2);
    } else if (mConditionType == CONDITION_TYPE_DATA) {
        v.add(mIndex1);
    }
}

bool SensorConditionItem::operator==(const SensorConditionItem& other) const
{
    bool result = mConditionType == other.mConditionType && mBracketLeft == other.mBracketLeft 
		&& mBracketRight == other.mBracketRight && mCombine == other.mCombine 
		&& mIndex1 == other.mIndex1 && mIndex2 == other.mIndex2 
		&& mIsLast1 == other.mIsLast1 && mIsLast2 == other.mIsLast2 
		&& mOperation == other.mOperation;
	switch (mDataType) {
        case DATA_TYPE_UINT32:
			result &= (mIntValue == other.mIntValue);
			break;
		case DATA_TYPE_UINT64:
			result &= (mLongValue == other.mLongValue);
			break;
		case DATA_TYPE_FLOAT:
			result &= (mFloatValue == other.mFloatValue);
			break;
	}

	return result;
}
// ----------------------------------------------------------------------------
SensorCondition::SensorCondition()
{
}

SensorCondition::~SensorCondition()
{
    mList.clear();
}

SensorCondition::SensorCondition(const SensorCondition& condition)
{
    mList.appendVector(condition.mList);
}

void SensorCondition::add(const SensorConditionItem& item)
{
    mList.add(item);
}

bool SensorCondition::operator==(const SensorCondition& other) const
{
    size_t size = mList.size();
    if (size == other.mList.size()) {
        for(size_t i = 0; i < size; i ++) {
            if (!(mList[i] == other.mList[i])) {
                return false;
            }
        }
        return true;
    }

    return false;
}

size_t SensorCondition::getFlattenedSize() const
{
    size_t len = mList.size();
    size_t fixedSize = sizeof(size_t);
    size_t variableSize = 0;
    for(size_t i = 0; i < len; i ++) {
        variableSize += mList[i].getFlattenedSize();
    }
    return fixedSize + variableSize;
}

status_t SensorCondition::flatten(void* buffer, size_t size) const {
    if (size < getFlattenedSize()) {
        return NO_MEMORY;
    }
    size_t len = mList.size();
    FlattenableUtils::write(buffer, size, len);
    for(size_t i = 0; i < len; i++) {
        FlattenableUtils::write(buffer, size, mList[i]);
    }
    return NO_ERROR;
}

status_t SensorCondition::unflatten(void const* buffer, size_t size) {
    if (size < sizeof(size_t)) {
        return NO_MEMORY;
    }
    size_t len = 0;
    FlattenableUtils::read(buffer, size, len);
    if (size < len * sizeof(SensorConditionItem)) {
        return NO_MEMORY;
    }

    mList.clear();
    SensorConditionItem item;
    for(size_t i = 0; i < len; i++) {
        FlattenableUtils::read(buffer, size, item);
        mList.add(item);
    }

    return NO_ERROR;
}

SensorCondition SensorCondition::create(int index, bool isLast, int operation, uint32_t value)
{
    SensorConditionItem item(index, isLast, operation, value);
    SensorCondition condition;
    condition.mList.add(item);
    return condition;
}

SensorCondition SensorCondition::create(int index, bool isLast, int operation, uint64_t value)
{
    SensorConditionItem item(index, isLast, operation, value);
    SensorCondition condition;
    condition.mList.add(item);
    return condition;
}

SensorCondition SensorCondition::create(int index, bool isLast, int operation, float value)
{
    SensorConditionItem item(index, isLast, operation, value);
    SensorCondition condition;
    condition.mList.add(item);
    return condition;
}

SensorCondition SensorCondition::create(int index1, bool isLast1, int operation, int index2, bool isLast2)
{
    SensorConditionItem item(index1, isLast1, operation, index2, isLast2);
    SensorCondition condition;
    condition.mList.add(item);
    return condition;
}

SensorCondition SensorCondition::create(int index1, bool isLast1, int operation, int index2, bool isLast2, uint32_t value)
{
    SensorConditionItem item(index1, isLast1, operation, index2, isLast2, value);
    SensorCondition condition;
    condition.mList.add(item);
    return condition;
}

SensorCondition SensorCondition::create(int index1, bool isLast1, int operation, int index2, bool isLast2, uint64_t value)
{
    SensorConditionItem item(index1, isLast1, operation, index2, isLast2, value);
    SensorCondition condition;
    condition.mList.add(item);
    return condition;
}

SensorCondition SensorCondition::create(int index1, bool isLast1, int operation, int index2, bool isLast2, float value)
{
    SensorConditionItem item(index1, isLast1, operation, index2, isLast2, value);
    SensorCondition condition;
    condition.mList.add(item);
    return condition;
}

SensorCondition SensorCondition::combineWithAnd(const SensorCondition& c1, const SensorCondition& c2)
{
    SensorCondition condition;
    condition.mList.appendVector(c1.mList);
    size_t size = condition.mList.size();
    condition.mList.appendVector(c2.mList);
    condition.mList.editItemAt(size).setCombine(COMBINE_AND);
    return condition;
}

SensorCondition SensorCondition::combineWithOr(const SensorCondition& c1, const SensorCondition& c2)
{
    SensorCondition condition;
    condition.mList.appendVector(c1.mList);
    size_t size = condition.mList.size();
    condition.mList.appendVector(c2.mList);
    condition.mList.editItemAt(size).setCombine(COMBINE_OR);
    return condition;
}

SensorCondition SensorCondition::combineWithBracket(const SensorCondition& c)
{
    SensorCondition condition;
    condition.mList.appendVector(c.mList);
    condition.mList.editItemAt(0).setBracketLeft();
    condition.mList.editItemAt(condition.mList.size() - 1).setBracketRight();
    //condition.mList[condition.mList.size() - 1].setBracketRight();
    return condition;
}

void SensorCondition::getStruct(shf_condition_t* condition) const
{
    size_t len = mList.size();
    bool lastBracketRight = false;
    for(size_t i = 0; i < len; i ++) {
        shf_condition_item_t citem;
        mList[i].getStruct(&citem);
        memcpy(condition->item + i, &citem, sizeof(shf_condition_item_t));
        if (lastBracketRight) {
            (condition->item + i)->combine |= SHF_COMBINE_MASK_BRACKET_RIGHT;
        }
        lastBracketRight = mList[i].getBracketRight();
    }
}

void SensorCondition::getIndexList(Vector<int>& v) const
{
    size_t size = mList.size();
    for(size_t i = 0; i < size; i ++) {
        Vector<int> temp;
        mList[i].getIndexList(temp);
        v.appendVector(temp);
    }
}

};
