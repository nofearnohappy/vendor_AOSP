#define LOG_NDEBUG 0
#define LOG_TAG "[SensorHubService]"

#include <stdint.h>
#include <math.h>
#include <sys/types.h>
#include <sys/time.h>

#include <cutils/properties.h>

#include <utils/Errors.h>
#include <utils/String16.h>
#include <utils/String8.h>

#include <binder/BinderService.h>
#include <binder/IServiceManager.h>
#include <binder/PermissionCache.h>
#include <binder/IPCThreadState.h>

#include <ISensorHubServer.h>
#include <ISensorHubClient.h>

#include <SensorContext.h>
#include <SensorCondition.h>
#include <SensorData.h>
#include <SensorAction.h>
#include <SensorHubManager.h>

#include "SensorHubService.h"

namespace android {
// ---------------------------------------------------------------------------

#define INVALID (-1)
#define REQUEST_ID_BASE (1000)

//#define INPOCKET_VALUE (1U)

SensorHubService::SensorHubService()
:mInitCheck(NO_INIT), mQueueStarted(false), mActionSessionId(REQUEST_ID_BASE)
{
}

void SensorHubService::instantiate() {
    defaultServiceManager()->addService(String16(SensorHubService::getServiceName()), new SensorHubService());
}

void SensorHubService::onFirstRef()
{
    ALOGD("SensorHubService starting...");
    SensorHubDevice& dev(SensorHubDevice::getInstance());
    
    if (!mQueueStarted) {//start task queue
        mActionQueue.start();
        //mPollingQueue.start();
        mQueueStarted = true;
    }

    run("SensorHubService", PRIORITY_URGENT_DISPLAY);

    mInitCheck = NO_ERROR;
}

SensorHubService::~SensorHubService()
{
    ALOGD("SensorHubService stopping...");
    Mutex::Autolock _l(mLock);
    if (mQueueStarted) {//stop task queue
        mActionQueue.stop();
    }
    mClientList.clear();
    mConditionList.clear();

    mInitCheck = NO_INIT;
}

static const String16 sPermissionTrigger("com.mediatek.permission.WAKE_DEVICE_SENSORHUB");
static const String16 sPermissionReserved("com.mediatek.permission.UPDATE_SENSORHUB_ACTION");

int SensorHubService::createActionRequestId_l()
{
    return mActionSessionId++;
}

Vector<int> SensorHubService::getContextList()
{
    SensorHubDevice& dev(SensorHubDevice::getInstance());
    return dev.getContextList();
}

Vector<int> SensorHubService::getActionList()
{
    SensorHubDevice& dev(SensorHubDevice::getInstance());
    return dev.getActionList();
}
    
void SensorHubService::attachClient(const sp<ISensorHubClient>& client)
{
    Mutex::Autolock _l(mLock);
    sp<ClientHolder> find = findClient_l();
    if (find == NULL) {
        sp<ClientHolder> holder = new ClientHolder(client);
        mClientList.add(holder);
        ALOGV("attachClient: pid=%d, uid=%d", holder->pid(), holder->uid());
        /*
        class DeathObserver : public IBinder::DeathRecipient {
            SensorHubManager& SensorHubManager;
            virtual void binderDied(const wp<IBinder>& who) {
                ALOGW("sensorhubservice died [%p]", who.unsafe_get());
                mSensorHubManger.serverDied();
            }
        public:
            DeathObserver(mSensorHubManger& mgr) : mSensorHubManger(mgr) { }
        };

        mDeathObserver = new DeathObserver(*const_cast<SensorHubManager *>(this));
        mServer->asBinder()->linkToDeath(mDeathObserver);
        mServer.attachClient(this);
        */
    }
}

void SensorHubService::detachClient(const sp<ISensorHubClient>& client)
{
    Mutex::Autolock _l(mLock);
    for (Vector< sp<SensorHubService::ClientHolder> >::iterator iter = mClientList.begin();
        iter != mClientList.end(); iter++) {
        if ((*iter)->equalCaller()) {
            iter = mClientList.erase(iter);
            break;
        }
    }
}

void SensorHubService::dumpClient()
{
    String8 msg("clients=[");
    for (Vector< sp<SensorHubService::ClientHolder> >::iterator iter = mClientList.begin();
        iter != mClientList.end(); iter++) {
        msg.appendFormat("(pid=%d, uid=%d) ", (*iter)->pid(), (*iter)->uid());
    }
    msg.append("]");
    ALOGV("dumpClient: %s", msg.string());
}

sp<SensorHubService::ClientHolder> SensorHubService::findClient_l()
{
    for (Vector< sp<SensorHubService::ClientHolder> >::iterator iter = mClientList.begin();
        iter != mClientList.end(); iter++) {
        if ((*iter)->equalCaller()) {
            return *iter;
        }
    }
    return NULL;
}

sp<SensorHubService::ClientHolder> SensorHubService::findClient_l(pid_t pid, uid_t uid)
{
    dumpClient();
    for (Vector< sp<SensorHubService::ClientHolder> >::iterator iter = mClientList.begin();
        iter != mClientList.end(); iter++) {
        if ((*iter)->equalCaller(pid, uid)) {
            return *iter;
        }
    }

    //dumpClient();
    return NULL;
}

void SensorHubService::dumpCondition() 
{
    int no = 0;
    String8 msg;
    for (Vector< sp<ConditionHolder> >::iterator iter = mConditionList.begin();
            iter != mConditionList.end(); iter++) {
        msg.clear();
        msg.appendFormat("conditions[%d]=[cid=%d, actions( ", no++, (*iter)->cid);
        Vector< sp<ActionHolder> >& actions = (*iter)->actions;
        for (Vector< sp<ActionHolder> >::iterator aiter = actions.begin(); aiter != actions.end(); aiter++) {
            msg.appendFormat("(%d %d) ", (*aiter)->rid, (*aiter)->aid);
        }
        msg.append(")]");
        ALOGV("dumpCondition: %s", msg.string());
    }
}

sp<SensorHubService::ConditionHolder> SensorHubService::findCondition_l(const SensorCondition& condition)
{
    for (Vector< sp<ConditionHolder> >::iterator iter = mConditionList.begin();
        iter != mConditionList.end();iter++) {
        if ((*iter)->condition == condition) {
            return *iter;
        }
    }

    return NULL;
}

void SensorHubService::removeCondition_l(int cid)
{
    for (Vector< sp<ConditionHolder> >::iterator iter = mConditionList.begin();
            iter != mConditionList.end();) {
        ALOGV("removeCondition_l>>>cid=%d, itercid=%d, iter=%p, end=%p", cid, (*iter)->cid, iter, mConditionList.end());
        if ((*iter)->cid == cid) {
            iter = mConditionList.erase(iter);
            //break;
        } else {
            iter++;
        }
        ALOGV("removeCondition_l<<<iter=%p, begin=%p, end=%p", iter, mConditionList.begin(), mConditionList.end());
    }
    ALOGV("removeCondition_l<<<cid=%d", cid);
}

sp<SensorHubService::ActionHolder> SensorHubService::findAction_l(int requestId)
{
    ALOGV("findAction_l: rid=%d", requestId);
    dumpCondition();
    for (Vector< sp<ConditionHolder> >::iterator iter = mConditionList.begin();
            iter != mConditionList.end();iter++) {
        Vector< sp<ActionHolder> >& actions = (*iter)->actions;
        for (Vector< sp<ActionHolder> >::iterator aiter = actions.begin(); aiter != actions.end(); aiter++) {
            if ((*aiter)->rid == requestId) {
                return *aiter;
            }
        }
    }

    //dumpCondition();
    return NULL;
}

sp<SensorHubService::ActionHolder> SensorHubService::findAction_l(int cid, int aid)
{
    dumpCondition();
    for (Vector< sp<ConditionHolder> >::iterator iter = mConditionList.begin();
            iter != mConditionList.end();iter++) {
        if ((*iter)->cid == cid) {
            //ALOGV("findAction_l: cid=%d", (*iter)->cid);
            Vector< sp<ActionHolder> >& actions = (*iter)->actions;
            for (Vector< sp<ActionHolder> >::iterator aiter = actions.begin(); aiter != actions.end(); aiter++) {
                //ALOGV("findAction_l: aid=%d", (*aiter)->aid);
                if ((*aiter)->aid == aid) {
                    return *aiter;
                }
            }
            break;
        }
    }

    return NULL;
}

void SensorHubService::removeAction_l(const sp<ActionHolder>& action)
{
    Vector< sp<ActionHolder> >& actions = action->condition->actions;
    for (Vector< sp<ActionHolder> >::iterator iter = actions.begin(); iter != actions.end(); iter++) {
        if ((*iter)->rid == action->rid) {
            ALOGV("removeAction_l: rid=%d", action->rid);
            actions.erase(iter);
            if (actions.size() == 0) {
                ALOGV("removeAction_l: cid=%d", action->condition->cid);
                removeCondition_l(action->condition->cid);
            }
            break;
        }
    }
}

int SensorHubService::requestAction(const SensorCondition& condition, const SensorAction& action)
{
    if (mInitCheck != NO_ERROR) {
        return REQUEST_ID_INVALID;
    }
    Mutex::Autolock _l(mLock);
    int rid = createActionRequestId_l();
    sp<ConditionHolder> ch = findCondition_l(condition);
    if (ch == NULL || ch->actions.size() >= SHF_CONDITION_ACTION_SIZE) {
        ch = new ConditionHolder(condition);
        mConditionList.add(ch);
    }
    sp<ActionHolder> ah = new ActionHolder(rid, action, ch);
    ALOGV("requestAction: rid=%d, pid=%d, uid=%d", rid, ah->pid(), ah->uid());
    sp<SimpleEventQueue::Event> event = new ActionAddEvent(this, ah);
    mActionQueue.postEvent(event);
    return rid;
}

bool SensorHubService::updateAction(int reservedCid, const SensorCondition& condition, const SensorAction& action)
{
    Mutex::Autolock _l(mLock);
    sp<ConditionHolder> ch = new ConditionHolder(condition);
    ch->cid = reservedCid;
    ch->updating = true;
    sp<ActionHolder> ah = new ActionHolder(reservedCid, action, ch);
    ah->aid = 1;
    ALOGV("updateAction: cid=%d, aid=%d", ch->cid, ah->aid);
    sp<SimpleEventQueue::Event> event = new ActionUpdateEvent(this, ah);
    mActionQueue.postEvent(event);
    return true;
}

bool SensorHubService::cancelAction(int requestId)
{
    Mutex::Autolock _l(mLock);
    sp<ActionHolder> removed = findAction_l(requestId);
    if (removed != NULL) {
        if (!removed->equalCaller()) {
            ALOGW("cancelAction: rid=%d was not requested by current thread!", requestId);
            return false;
        }
        ALOGV("cancelAction: rid=%d", requestId);
        removeAction_l(removed);//remove local
        //if exist updating, adding, canceling, remove it
        mActionQueue.cancelEvent(requestId, COMMAND_TYPE_ACTION);
        
        //except canceled events, we find removed here.
        //it means, there maybe also a valid event is running.
        //so, post a remove event to check this case.
        sp<SimpleEventQueue::Event> event = new ActionRemoveEvent(this, removed);
        mActionQueue.postEvent(event);
    } else {
        ALOGW("cancelAction: rid=%d was not found!", requestId);
    }
    return true;
}

bool SensorHubService::updateCondition(int requestId, const SensorCondition& condition)
{
    Mutex::Autolock _l(mLock);
    sp<ActionHolder> oldah = findAction_l(requestId);
    if (oldah != NULL) {
        if (!oldah->equalCaller()) {
            ALOGE("updateCondition: rid=%d was not set by current thread!", requestId);
            return false;
        }
    } else {
        ALOGE("updateCondition: rid=%d does not exist!", requestId);
        return false;
    }

    //if exist updating, adding, canceling events, remove them
    //mActionQueue.cancelEvent(requestId, COMMAND_TYPE_ACTION);

    sp<ConditionHolder> oldch = oldah->condition;
    sp<ConditionHolder> newch = findCondition_l(condition);
    if (newch != NULL && newch == oldch) {
        ALOGD("updateCondition: same condition.");
        return true;
    }

    int oldaid = oldah->aid;
    removeAction_l(oldah);
    oldah->aid = 0;

    if (newch == NULL || newch->actions.size() >= SHF_CONDITION_ACTION_SIZE) {
        newch = new ConditionHolder(condition);
        mConditionList.add(newch);
    } 
    oldah->condition = newch;
    int size = newch->actions.size();
    oldah->aid = size + 1;
    int pos = newch->actions.insertAt(oldah, size);
    ALOGV("updateCondition: cid=%d, aid=%d, pos=%d", newch->cid, oldah->aid, pos);

    sp<SimpleEventQueue::Event> event = new ConditionUpdateEvent(this, requestId, oldaid, oldch->cid, newch);
    mActionQueue.postEvent(event);
    return true;
}

bool SensorHubService::enableGestureWakeup(bool enable) 
{
    Mutex::Autolock _l(mLock);
    SensorHubDevice& dev(SensorHubDevice::getInstance());
    return dev.enableGestureWakeup(enable);
}

bool SensorHubService::threadLoop()
{
    ALOGD("SensorHubService thread starting...");

    SensorHubDevice& dev(SensorHubDevice::getInstance());
    sensor_trigger_data_t trigger_data;
    ssize_t count = 0;
    do {
        count = dev.poll(&trigger_data, 1);
        //ALOGD("SensorHubService: poll data size=%d", count);
        if (count < 0) {
            ALOGE("sensorhub poll failed (%d)", count);
            break;
        }

        if (count > 0) {
            sp<SimpleEventQueue::Event> event = new NotifyEvent(this, trigger_data);
            mActionQueue.postEvent(event);
        }
    } while (count >= 0 || Thread::exitPending());
    ALOGW("Exiting SensorHubService::threadLoop => aborting...");
    abort();
    return false;
}

// ---------------------------------------------------------------------------
SensorHubService::ActionHolder::ActionHolder(int _rid, const SensorAction& _action, const sp<ConditionHolder>& _condition)
:aid(SHF_ACTION_INDEX_INVALID), rid(_rid), action(_action), condition(_condition)
{
    condition->actions.add(this);
}

SensorHubService::ActionHolder::~ActionHolder()
{
}

// ---------------------------------------------------------------------------
SensorHubService::ConditionHolder::ConditionHolder(const SensorCondition& _condition)
:cid(SHF_CONDITION_INDEX_INVALID), updating(false), condition(_condition)
{
}

SensorHubService::ConditionHolder::~ConditionHolder()
{
}

// ---------------------------------------------------------------------------
SensorHubService::ClientHolder::ClientHolder(const sp<ISensorHubClient>& _client)
:client(_client)
{
}

SensorHubService::ClientHolder::~ClientHolder()
{
}

// ---------------------------------------------------------------------------
SensorHubService::LocalEvent::LocalEvent(SensorHubService* _service, int _rid, int _type)
:Event(_rid, _type), service(_service)
{
}

SensorHubService::LocalEvent::~LocalEvent()
{
}

// ---------------------------------------------------------------------------
SensorHubService::ActionAddEvent::ActionAddEvent(SensorHubService* _service, const sp<ActionHolder>& _action)
    :SensorHubService::LocalEvent(_service, _action->rid, COMMAND_TYPE_ACTION), action(_action)
{
}

SensorHubService::ActionAddEvent::~ActionAddEvent()
{
}

void SensorHubService::ActionAddEvent::fire()
{
    SensorHubDevice& dev(SensorHubDevice::getInstance());
    int cid = action->condition->cid;
    if (cid != SHF_CONDITION_INDEX_INVALID) {//has the same condition, just add action
        shf_action_id_t at;
        action->action.getStruct(&at);
        int aid = dev.addAction(cid, at);
        if (aid == SHF_ACTION_INDEX_INVALID) {//invalid, should not occure
            ALOGE("addEvent(%d, %d): failed! cid=%d, aid=%d", id(), type(), cid, aid);
        } else {
            action->aid = aid;
            ALOGV("addEvent(%d, %d): succeed. cid=%d, aid=%d", id(), type(), cid, aid);
        }
    } else {//the first condition, add it
        shf_condition_t ct;
        memset(&ct, 0, sizeof(ct));
        action->condition->condition.getStruct(&ct);
        shf_action_id_t at;
        action->action.getStruct(&at);
        ct.action[0] = at;
        cid = dev.addCondition(&ct);
        if (cid == SHF_CONDITION_INDEX_INVALID) {
            ALOGE("addEvent(%d, %d): failed! cid=%d", id(), type(), cid);
        } else {
            action->condition->cid = cid;
            action->aid = 1;//0 is invalid, 1 is valid
            ALOGV("addEvent(%d, %d): succeed. cid=%d", id(), type(), cid);
        }
    }
}

// ---------------------------------------------------------------------------
SensorHubService::ActionUpdateEvent::ActionUpdateEvent(SensorHubService* _service, const sp<ActionHolder>& _action)
    :ActionAddEvent(_service, _action)
{
}

SensorHubService::ActionUpdateEvent::~ActionUpdateEvent()
{
}

void SensorHubService::ActionUpdateEvent::fire()
{
    int cid = action->condition->cid;
    if (cid != SHF_CONDITION_INDEX_INVALID) {//valid update, do it
        shf_condition_t ct;
        memset(&ct, 0, sizeof(ct));
        action->condition->condition.getStruct(&ct);
        shf_action_id_t at;
        action->action.getStruct(&at);
        ct.action[0] = at;
        SensorHubDevice& dev(SensorHubDevice::getInstance());
        status_t ret = dev.updateCondition(cid, &ct);
        if (ret == NO_ERROR) {
            ALOGV("updateReserveCondition: succeed. cid=%d, aid=%d", cid, action->aid);
        } else {
            ALOGE("updateReserveCondition: failed! cid=%d, aid=%d, ret=%d", cid, action->aid, ret);
        }
    } else {
        ALOGE("updateReserveCondition: failed! invalidCid=%d, aid=%d", cid, action->aid);
    }
    action->condition->updating = false;
}

// ---------------------------------------------------------------------------
SensorHubService::ActionRemoveEvent::ActionRemoveEvent(SensorHubService* _service, const sp<ActionHolder>& _action)
    :ActionAddEvent(_service, _action)
{
}
SensorHubService::ActionRemoveEvent::~ActionRemoveEvent()
{
}
void SensorHubService::ActionRemoveEvent::fire()
{
    int cid = action->condition->cid;
    int aid = action->aid;
    if (cid != SHF_CONDITION_INDEX_INVALID && aid != SHF_ACTION_INDEX_INVALID) {
        SensorHubDevice& dev(SensorHubDevice::getInstance());
        status_t ret = dev.removeAction(cid, aid);
        if (ret == NO_ERROR) {
            //set cid and aid invalid to avoid duplicated canceling
            //Note: action->condition->cid should not be set to invalid, since some other actions may still exist
            //action->condition->cid = SHF_CONDITION_INDEX_INVALID; 
            action->aid = SHF_ACTION_INDEX_INVALID;
            ALOGV("removeEvent(%d, %d): succeed. cid=%d, aid=%d", id(), type(), cid, aid);
        } else {
            ALOGE("removeEvent(%d, %d): failed! cid=%d, aid=%d, ret=%d", id(), type(), cid, aid, ret);
        }
    } else {
        ALOGV("removeEvent(%d, %d): unnecessary! cid=%d", id(), type(), cid);
    }
}

// ---------------------------------------------------------------------------
SensorHubService::ConditionUpdateEvent::ConditionUpdateEvent(SensorHubService* _service, int _rid, int _oldaid, 
    int _oldcid, const sp<ConditionHolder>& _newch)
        :SensorHubService::LocalEvent(_service, _rid, COMMAND_TYPE_ACTION), oldaid(_oldaid), oldcid(_oldcid), newch(_newch)
{
}

SensorHubService::ConditionUpdateEvent::~ConditionUpdateEvent()
{
}

void SensorHubService::ConditionUpdateEvent::fire()
{
    SensorHubDevice& dev(SensorHubDevice::getInstance());
    dev.removeAction(oldcid, oldaid);

    int newcid = newch->cid;
    shf_condition_t ct;
    memset(&ct, 0 , sizeof(ct));
    newch->condition.getStruct(&ct);
    int size = newch->actions.size();
    for (int i = 0; i < size; i++) {
        shf_action_id_t at;
        if (0 != newch->actions[i]->action.getAction()) {
            newch->actions[i]->action.getStruct(&at);
            ct.action[i] = at;
        }
    }

    if (newcid != SHF_CONDITION_INDEX_INVALID) {
        status_t ret = dev.updateCondition(newcid, &ct);
        if (ret != NO_ERROR) {
            ALOGE("updateCondition: failed to update condition! cid=%d, result=%d", newcid, ret);
        } else {
            ALOGV("updateCondition: update condition succeed. cid=%d", newcid);
        }
    } else {
        int cid = dev.addCondition(&ct);
        if (cid == SHF_CONDITION_INDEX_INVALID) {
            ALOGE("updateCondition: failed to add condition! cid=%d", cid);
        } else {
            newch->cid = cid;
            ALOGV("updateCondition: add condition succeed. newcid=%d", cid);
        }
    }
    newch->updating = false;
}

// ---------------------------------------------------------------------------
SensorHubService::NotifyEvent::NotifyEvent(SensorHubService* _service, sensor_trigger_data_t _data)
    :LocalEvent(_service, 0, COMMAND_TYPE_ACTION), trigger_data(_data)
{
}

SensorHubService::NotifyEvent::~NotifyEvent()
{
}

void SensorHubService::NotifyEvent::fire()
{
    //Mutex::Autolock _l(service->mLock);
    for (size_t i = 0; i < SHF_CONDITION_ACTION_SIZE; i++) {
        if (SHF_ACTION_ID_AP_WAKEUP != (trigger_data.aid[i] & SHF_ACTION_MASK_DATA)) {
            ALOGV("notifyEvent: aid[%d]=%d is invalid!", i, trigger_data.aid[i]);
            continue;
        }
        sp<ActionHolder> holder = service->findAction_l(trigger_data.cid, (i + 1));
        if (holder == NULL) {
            ALOGV("notifyEvent: no holder for cid=%d and aid=%d", trigger_data.cid, (i + 1));
            continue;
        }
        if (!holder->action.isRepeatable()) {
            service->removeAction_l(holder);
        }
        sp<SensorHubService::ClientHolder> client = service->findClient_l(holder->pid(), holder->uid());
        if (client == NULL) {
            ALOGV("notifyEvent: no client for pid=%d and uid=%d", holder->pid(), holder->uid());
            continue;
        }
        Vector<SensorData> v;
        SensorData::parse(&trigger_data, v);
    
        Parcel parcel;
        SensorData::flattenVector(v, parcel);
        parcel.setDataPosition(0);
        ALOGV("notifyEvent: notify client rid=%d, parcel=%p", holder->rid, &parcel);
        client->client->notify(SENSOR_TRIGGER_ACTION_DATA, holder->rid, 0, &parcel);
    }
}
// ---------------------------------------------------------------------------
}; // namespace android

