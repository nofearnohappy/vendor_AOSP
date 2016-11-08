#ifndef ANDROID_SENSORHUB_SIMPLE_COMMAND_QUEUE_H
#define ANDROID_SENSORHUB_SIMPLE_COMMAND_QUEUE_H

#include <stdint.h>
#include <sys/types.h>

#include <pthread.h>

#include <utils/List.h>
#include <utils/threads.h>

#include <powermanager/IPowerManager.h>

#include "SimpleBinderHolder.h"

// ----------------------------------------------------------------------------
namespace android {
// ----------------------------------------------------------------------------

class Parcel;

// ----------------------------------------------------------------------------
class SimpleEventQueue {
public:
    class Event: public SimpleBinderHolder {
    public:
        Event(int id, int type);
        virtual ~Event();
        virtual void fire() = 0;

        inline bool equal(int id, int type) { return (mId == id) && (mType == type);};
        inline void cancel() { mCanceled = true; }
        inline bool isCanceled() { return mCanceled; }
        inline int id() { return mId; }
        inline int type() { return mType; }
    private:
        int mId;
        int mType;
        volatile bool mCanceled;
    };

    SimpleEventQueue();
    //SimpleEventQueue(String8& name);
    ~SimpleEventQueue();
    bool postEvent(const sp<Event> &event);
    bool postTimedEvent(const sp<Event> &event, int64_t realtime_us);
    bool cancelEvent(int id, int type);
    void start();
    void stop(bool flush = false);

    void clearPowerManager();

    static int64_t getRealTimeUs();
private:
    class PMDeathRecipient : public IBinder::DeathRecipient {
    public:
        PMDeathRecipient(SimpleEventQueue *queue) : mQueue(queue) {}
        virtual ~PMDeathRecipient() {}
        // IBinder::DeathRecipient
        virtual void binderDied(const wp<IBinder>& who);
    private:
        PMDeathRecipient(const PMDeathRecipient&);
        PMDeathRecipient& operator = (const PMDeathRecipient&);
        SimpleEventQueue *mQueue;
    };

    struct QueueItem {
        sp<Event> event;
        int64_t realtime_us;
        bool has_wakelock;
    };

    struct StopEvent : public SimpleEventQueue::Event {
        SimpleEventQueue* mQueue;
        StopEvent(SimpleEventQueue* queue):Event(0, 0), mQueue(queue){}
        virtual void fire() {
            mQueue->mStopped = true;
        }
    };
    
    //String8* mName;
    // protected by mLock
    pthread_t mThread;
    static void *ThreadWrapper(void *me);
    void threadEntry();

    bool mRunning;
    bool mStopped;

    mutable Mutex mLock;
    List<QueueItem> mQueue;
    Condition mQueueNotEmptyCondition;
    Condition mQueueHeadChangedCondition;

    sp<SimpleEventQueue::Event> removeEventFromQueue_l(bool *wakeLocked);

    sp<IPowerManager>          mPowerManager;
    sp<IBinder>                mWakeLockToken;
    const sp<PMDeathRecipient> mDeathRecipient;
    uint32_t                   mWakeLockCount;

    void acquireWakeLock_l();
    void releaseWakeLock_l(bool force = false);

    SimpleEventQueue(const SimpleEventQueue &);
    SimpleEventQueue &operator=(const SimpleEventQueue &);
};

// ----------------------------------------------------------------------------
}; // namespace android

#endif // ANDROID_SENSORHUB_SIMPLE_COMMAND_QUEUE_H
