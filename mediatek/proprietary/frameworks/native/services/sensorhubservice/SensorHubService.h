#ifndef ANDROID_SENSORHUB_SERVICE_H
#define ANDROID_SENSORHUB_SERVICE_H
  
#include <stdint.h>
#include <sys/types.h>
  
#include <utils/Vector.h>
#include <utils/threads.h>

#include <binder/BinderService.h>

#include <ISensorHubServer.h>
#include <ISensorHubClient.h>

#include "SimpleBinderHolder.h"
#include "SimpleEventQueue.h"
#include "SensorHubDevice.h"
// ---------------------------------------------------------------------------

namespace android {
// ---------------------------------------------------------------------------

class SensorHubService :
	  public BinderService<SensorHubService>,
	  public BnSensorHubServer,
	  protected Thread
{
    //---------------------------------------------------------------------------
    class ConditionHolder;
    class ActionHolder: public SimpleBinderHolder {
    public:
        int aid;
        int rid;//for client id
        SensorAction action;
        sp<ConditionHolder> condition;

        ActionHolder(int _rid, const SensorAction& _action, const sp<ConditionHolder>& _condition);
        virtual ~ActionHolder();
    };

    class ConditionHolder: public RefBase {
    public:
        int cid;
        bool updating;
        SensorCondition condition;
        Vector< sp<ActionHolder> > actions;
        
        ConditionHolder(const SensorCondition& _condition);
        ~ConditionHolder();

        bool equalCondition(const SensorCondition& _condition);
    };

    class ClientHolder: public SimpleBinderHolder {
    public:
        sp<ISensorHubClient> client;

        ClientHolder(const sp<ISensorHubClient>& _client);
        virtual ~ClientHolder();
    };

    //---------------------------------------------------------------------------
    enum {
        COMMAND_TYPE_HISTORY = 1,
        COMMAND_TYPE_ACTION,
    };

    class LocalEvent: public SimpleEventQueue::Event {
    public:
        SensorHubService* service;

        LocalEvent(SensorHubService* _service, int _rid, int _type);
        virtual ~LocalEvent();
    };

    class ActionAddEvent: public LocalEvent {
    public:
        sp<ActionHolder> action;

        ActionAddEvent(SensorHubService* _service, const sp<ActionHolder>& _action);
        virtual ~ActionAddEvent();

        virtual void fire();
    };

    class ActionUpdateEvent: public ActionAddEvent {
    public:
        ActionUpdateEvent(SensorHubService* service, const sp<ActionHolder>& _action);
        virtual ~ActionUpdateEvent();

        virtual void fire();
    };

    class ActionRemoveEvent: public ActionAddEvent {
    public:
        ActionRemoveEvent(SensorHubService* service, const sp<ActionHolder>& _action);
        virtual ~ActionRemoveEvent();

        virtual void fire();
    };

    class ConditionUpdateEvent: public LocalEvent {
    public:
        sp<ConditionHolder> newch;
        int oldcid;
        int oldaid;

        ConditionUpdateEvent(SensorHubService* _service, int _rid, int _oldaid, 
            int _oldcid, const sp<ConditionHolder>& _newch);
        virtual ~ConditionUpdateEvent();
        virtual void fire();
    };

    class NotifyEvent: public LocalEvent
    {
    public:
        NotifyEvent(SensorHubService* _service, sensor_trigger_data_t _data);
        virtual ~NotifyEvent();
        virtual void fire();

    private:
        sensor_trigger_data_t trigger_data;
    };

    //---------------------------------------------------------------------------
    friend class BinderService<SensorHubService>;
    //friend class HistoryEvent;
    friend class ActionAddEvent;
    friend class ActionUpdateEvent;
    friend class ActionRemoveEvent;
    //friend class PollingEvent;

    SensorHubService();
    virtual ~SensorHubService();

    virtual void onFirstRef();

    // Thread interface
    virtual bool threadLoop();

    status_t mInitCheck;//init flag
    bool mQueueStarted;
    
    // protected by mLock
    mutable Mutex mLock;

    Vector< sp<SensorHubService::ClientHolder> > mClientList;
    Vector< sp<SensorHubService::ConditionHolder> > mConditionList;
    void dumpClient();
    sp<SensorHubService::ClientHolder> findClient_l();
    sp<SensorHubService::ClientHolder> findClient_l(pid_t pid, uid_t uid);
    void dumpCondition();
    sp<SensorHubService::ConditionHolder> findCondition_l(const SensorCondition& condition);
    void removeCondition_l(int cid);
    sp<SensorHubService::ActionHolder> findAction_l(int requestId);
    sp<SensorHubService::ActionHolder> findAction_l(int cid, int aid);
    void removeAction_l(const sp<ActionHolder>& action);

    SimpleEventQueue mActionQueue;
    //SimpleEventQueue mPollingQueue;
    int mActionSessionId;
    int createActionRequestId_l();
    
public:
    static void instantiate();

    static char const* getServiceName() { return "SensorHubService"; }

    // ISensorHubServer interface
    virtual Vector<int> getContextList();
    virtual Vector<int> getActionList();
    virtual void attachClient(const sp<ISensorHubClient>& client);
    virtual void detachClient(const sp<ISensorHubClient>& client);
    virtual int requestAction(const SensorCondition& condition, const SensorAction& action);
    virtual bool updateAction(int requestId, const SensorCondition& condition, const SensorAction& action);
    virtual bool updateCondition(int requestId, const SensorCondition& condition);
    virtual bool cancelAction(int requestId);
    virtual bool enableGestureWakeup(bool enabled);
};

// ---------------------------------------------------------------------------
}; // namespace android
  
#endif // ANDROID_SENSORHUB_SERVICE_H
  
