#define LOG_NDEBUG 0
#define LOG_TAG "[SensorHubManager]"
#include <utils/Log.h>

#include <stdint.h>
#include <sys/types.h>

#include <utils/Errors.h>
#include <utils/RefBase.h>
#include <utils/Singleton.h>

#include <binder/IBinder.h>
#include <binder/IServiceManager.h>

#include <SensorContext.h>
#include <SensorAction.h>
#include <SensorData.h>
#include <SensorCondition.h>

#include <ISensorHubServer.h>
#include <ISensorHubClient.h>

#include <SensorHubManager.h>

// ----------------------------------------------------------------------------
namespace android {
// ----------------------------------------------------------------------------

//ANDROID_SINGLETON_STATIC_INSTANCE(SensorHubManager)

SensorHubManager::SensorHubManager()
:mSessionId(0)
{
    // okay we're not locked here, but it's not needed during construction
    assertStateLocked();
}

SensorHubManager::~SensorHubManager()
{
    if (mServer != NULL) {
        mServer->detachClient(this);
    }
}

void SensorHubManager::serverDied()
{
    ALOGW("SensorHubService died!!");
    Mutex::Autolock _l(mLock);
    mServer.clear();
    mServer = NULL;
    mActionList.clear();
    mContextList.clear();
    mDataList.clear();
    mTriggerList.clear();
}

status_t SensorHubManager::assertStateLocked()
{
    if (mServer == NULL) {
        // try for one second
        const String16 name("SensorHubService");
        for (int i=0 ; i<4 ; i++) {
            status_t err = getService(name, &mServer);
            if (err == NAME_NOT_FOUND) {
                usleep(250000);
                continue;
            }
            if (err != NO_ERROR) {
                return err;
            }
            break;
        }

        class DeathObserver : public IBinder::DeathRecipient {
            SensorHubManager& mSensorHubManager;
            virtual void binderDied(const wp<IBinder>& who) {
                ALOGW("sensorhubservice died [%p]", who.unsafe_get());
                mSensorHubManager.serverDied();
            }
        public:
            DeathObserver(SensorHubManager& mgr) : mSensorHubManager(mgr) { }
        };

        mDeathObserver = new DeathObserver(*const_cast<SensorHubManager *>(this));
        IInterface::asBinder(mServer)->linkToDeath(mDeathObserver);
        mServer->attachClient(this);

        mActionList = mServer->getActionList();
        mContextList = mServer->getContextList();

        mDataList.clear();
        if (validContext(SensorContext::Clock::CONTEXT_TYPE)) {
            mDataList.add(SensorContext::Clock::DATA_INDEX_TIME);
        }
        if (validContext(SensorContext::Pedometer::CONTEXT_TYPE)) {
            mDataList.add(SensorContext::Pedometer::DATA_INDEX_LENGTH);
            mDataList.add(SensorContext::Pedometer::DATA_INDEX_FREQUENCY);
            mDataList.add(SensorContext::Pedometer::DATA_INDEX_COUNT);
            mDataList.add(SensorContext::Pedometer::DATA_INDEX_DISTANCE);
            mDataList.add(SensorContext::Pedometer::DATA_INDEX_TIMESTAMP);
        }
        if (validContext(SensorContext::Activity::CONTEXT_TYPE)) {
            mDataList.add(SensorContext::Activity::DATA_INDEX_IN_VEHICLE);
            mDataList.add(SensorContext::Activity::DATA_INDEX_ON_BICYCLE);
            mDataList.add(SensorContext::Activity::DATA_INDEX_ON_FOOT);
            mDataList.add(SensorContext::Activity::DATA_INDEX_STILL);
            mDataList.add(SensorContext::Activity::DATA_INDEX_UNKNOWN);
            mDataList.add(SensorContext::Activity::DATA_INDEX_TILTING);
            mDataList.add(SensorContext::Activity::DATA_INDEX_TIMESTAMP);
        }
        if (validContext(SensorContext::InPocket::CONTEXT_TYPE)) {
            mDataList.add(SensorContext::InPocket::DATA_INDEX_INPOCKE);
            mDataList.add(SensorContext::InPocket::DATA_INDEX_TIMESTAMP);
        }
        if (validContext(SensorContext::MostProbableActivity::CONTEXT_TYPE)) {
            mDataList.add(SensorContext::MostProbableActivity::DATA_INDEX_ACTIVITY);
            mDataList.add(SensorContext::MostProbableActivity::DATA_INDEX_CONFIDENCE);
            mDataList.add(SensorContext::MostProbableActivity::DATA_INDEX_TIMESTAMP);
        }
        if (validContext(SensorContext::SignificantMotion::CONTEXT_TYPE)) {
            mDataList.add(SensorContext::SignificantMotion::DATA_INDEX_MOTION_VALUE);
            mDataList.add(SensorContext::SignificantMotion::DATA_INDEX_TIMESTAMP);
        }
        if (validContext(SensorContext::Pickup::CONTEXT_TYPE)) {
            mDataList.add(SensorContext::Pickup::DATA_INDEX_PICKUP_VALUE);
            mDataList.add(SensorContext::Pickup::DATA_INDEX_TIMESTAMP);
        }
        if (validContext(SensorContext::FaceDown::CONTEXT_TYPE)) {
            mDataList.add(SensorContext::FaceDown::DATA_INDEX_FACEDOWN_VALUE);
            mDataList.add(SensorContext::FaceDown::DATA_INDEX_TIMESTAMP);
        }
        if (validContext(SensorContext::Shake::CONTEXT_TYPE)) {
            mDataList.add(SensorContext::Shake::DATA_INDEX_SHAKE_VALUE);
            mDataList.add(SensorContext::Shake::DATA_INDEX_TIMESTAMP);
        }
        if (validContext(SensorContext::Gesture::CONTEXT_TYPE)) {
            mDataList.add(SensorContext::Gesture::DATA_INDEX_GESTURE_VALUE);
            mDataList.add(SensorContext::Gesture::DATA_INDEX_TIMESTAMP);
        }
    }
    return NO_ERROR;
}

bool SensorHubManager::validCondition(const SensorCondition& condition) const
{
    Vector<int> v;
    condition.getIndexList(v);
    size_t size = v.size();
    for (size_t i = 0; i < size; i++) {
        if (!validData(v[i])) {
            return false;
        }
    }
    return true;
}

bool SensorHubManager::validData(int index) const
{
    for (Vector<int>::iterator iter = mDataList.begin(); iter != mDataList.end(); iter++) {
        if (*iter == index) {
            return true;
        }
    }
    return false;
}

bool SensorHubManager::validAction(const SensorAction& action) const
{
    for (Vector<int>::iterator iter = mActionList.begin(); iter != mActionList.end(); iter++) {
        if (*iter == action.getAction()) {
            return true;
        }
    }
    return false;
}

bool SensorHubManager::validContext(int contextType) const
{
    for (Vector<int>::iterator iter = mContextList.begin(); iter != mContextList.end(); iter++) {
        if (*iter == contextType) {
            return true;
        }
    }
    return false;
}

Vector<int> SensorHubManager::getContextList()
{
    Mutex::Autolock _l(mLock);
    assertStateLocked();
    return mContextList;
}

Vector<int> SensorHubManager::getActionList()
{
    Mutex::Autolock _l(mLock);
    assertStateLocked();
    return mActionList;
}

int SensorHubManager::requestAction(const SensorCondition& condition, const SensorAction& action)
{
    Mutex::Autolock _l(mLock);
	int rid = REQUEST_ID_INVALID;
    if (assertStateLocked() == NO_ERROR) {
        rid = mServer->requestAction(condition, action);
        if (rid != REQUEST_ID_INVALID) {
            ActionHolder holder(rid, action.isRepeatable(), action.getListener());
            mTriggerList.add(holder);
        }
    }
    ALOGV("requestAction: rid=%d, condition=%p, action=%p", rid, &condition, &action);
    return rid;
}

bool SensorHubManager::cancelAction(int requestId)
{
    Mutex::Autolock _l(mLock);
    bool ret = false;
    removeActionAlways(requestId);
    if (assertStateLocked() == NO_ERROR) {
        ret = mServer->cancelAction(requestId);
    }
    ALOGV("cancelAction: rid=%d, ret=%d", requestId, ret);
    return ret;
}

bool SensorHubManager::updateCondition(int requestId, const SensorCondition& condition)
{
    Mutex::Autolock _l(mLock);
	bool ret = false;
    if (assertStateLocked() == NO_ERROR) {
        ret = mServer->updateCondition(requestId, condition);
    }
    ALOGV("updateCondition: rid=%d, condition=%p, ret=%d", requestId, &condition, ret);
    return ret;
}

bool SensorHubManager::enableGestureWakeup(bool enable)
{
    Mutex::Autolock _l(mLock);
	bool ret = false;
    if (assertStateLocked() == NO_ERROR) {
        ret = mServer->enableGestureWakeup(enable);
    }
    ALOGV("enableGestureWakeup: enable=%d, ret=%d", enable, ret);
    return ret;
}

sp<SensorTriggerListener> SensorHubManager::removeActionAlways(int rid)
{
    for (Vector<ActionHolder>::iterator iter = mTriggerList.begin(); iter != mTriggerList.end(); ) {
        if (iter->rid == rid) {
            ALOGV("removeActionAlways: rid=%d, repeatable=%d", rid, iter->repeatable);
            sp<SensorTriggerListener> listener = iter->listener;
            iter = mTriggerList.erase(iter);
            return listener;
        } else {
            iter++;
        }
    }

    ALOGW("removeActionAlways: no client for rid=%d", rid);
    return NULL;
}

sp<SensorTriggerListener> SensorHubManager::removeActionIfNeed(int rid)
{
    for (Vector<ActionHolder>::iterator iter = mTriggerList.begin(); iter != mTriggerList.end();) {
        if (iter->rid == rid) {
            ALOGV("removeActionIfNeed: rid=%d, repeatable=%d", rid, iter->repeatable);
            sp<SensorTriggerListener> listener = iter->listener;
            if (!iter->repeatable) {//remove non-repeatable listener
                iter = mTriggerList.erase(iter);
            }
            return listener;
        } else {
            iter++;
        }
    }

    ALOGW("removeActionIfNeed: no client for rid=%d", rid);
    return NULL;
}

void SensorHubManager::notify(int msg, int ext1, int ext2, const Parcel *obj)
{
    ALOGV("notify: msg=%d, ext1=%d, ext2=%d, parcel=%p", msg, ext1, ext2, obj);
    switch (msg) {
    case SENSOR_TRIGGER_NOP: // interface test message
        break;
    case SENSOR_TRIGGER_ACTION_DATA: {
        int rid = ext1;
        sp<SensorTriggerListener> listener = NULL;
        {
            Mutex::Autolock _l(mLock);
            listener = removeActionIfNeed(rid);
        }
        if (listener != NULL) {
            listener->onTrigger(msg, ext1, ext2, obj);
        } else {
            ALOGW("notify: no client for rid=%d", rid);
        }
    } break;
    default:
        break;
    }
}

SensorHubManager::ActionHolder::ActionHolder()
:rid(REQUEST_ID_INVALID), repeatable(false), listener(NULL)
{
}
SensorHubManager::ActionHolder::ActionHolder(int _rid, bool _repeatable, sp<SensorTriggerListener> _listener)
:rid(_rid), repeatable(_repeatable), listener(_listener)
{
}

// ----------------------------------------------------------------------------
}; // namespace android
