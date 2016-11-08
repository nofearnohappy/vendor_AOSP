#include <ISensorHubClient.h>
#include <ISensorHubServer.h>
namespace android {
// ----------------------------------------------------------------------------

SensorAction::SensorAction(sp<SensorTriggerListener>& listener, bool repeatable, bool checkLast)
:mListener(listener), mAction(ACTION_ID_AP_WAKEUP), mRepeatable(repeatable), mCheckLast(checkLast)
{
}
SensorAction::SensorAction(sp<SensorTriggerListener>& listener, bool repeatable)
:mListener(listener), mAction(ACTION_ID_AP_WAKEUP), mRepeatable(repeatable), mCheckLast(true)
{
}
SensorAction::SensorAction(int action, bool repeatable, bool checkLast)
:mListener(NULL), mAction(action), mRepeatable(repeatable), mCheckLast(checkLast)
{
}
SensorAction::SensorAction()
:mListener(NULL), mAction(ACTION_ID_INVALID), mRepeatable(false), mCheckLast(true)
{
}
SensorAction::SensorAction(const SensorAction& action)
:mListener(action.mListener), mAction(action.mAction), mRepeatable(action.mRepeatable), mCheckLast(action.mCheckLast)
{
}

SensorAction::~SensorAction()
{
}

void SensorAction::getStruct(shf_action_id_t* action) const
{
    *action = mAction;
    if (mRepeatable) {
        *action |= SHF_ACTION_MASK_REPEAT;
    }
    if (mCheckLast) {
        *action |= SHF_ACTION_MASK_CHECK_LAST;
    }
}

size_t SensorAction::getFlattenedSize() const
{
    return sizeof(int) + sizeof(bool) * 2;
}

status_t SensorAction::flatten(void* buffer, size_t size) const {
    if (size < getFlattenedSize()) {
        return NO_MEMORY;
    }
    FlattenableUtils::write(buffer, size, mAction);
    FlattenableUtils::write(buffer, size, mRepeatable);
    FlattenableUtils::write(buffer, size, mCheckLast);
    return NO_ERROR;
}

status_t SensorAction::unflatten(void const* buffer, size_t size) {
    if (size < getFlattenedSize()) {
        return NO_MEMORY;
    }
    FlattenableUtils::read(buffer, size, mAction);
    FlattenableUtils::read(buffer, size, mRepeatable);
    FlattenableUtils::read(buffer, size, mCheckLast);
    return NO_ERROR;
}

};
