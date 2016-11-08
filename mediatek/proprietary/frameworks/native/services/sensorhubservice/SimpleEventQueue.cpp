/*
* Copyright (C) 2011-2014 MediaTek Inc.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

#define LOG_NDEBUG 0
#define LOG_TAG "[SensorHubService]"

#include "SimpleEventQueue.h"

#include <stdint.h>
#include <sys/prctl.h>
#include <sys/time.h>

#include <binder/IServiceManager.h>
#include <powermanager/PowerManager.h>

namespace android {

#ifndef __INT64_C
#define __INT64_C(c)     c ## LL
#endif

#ifndef __UINT64_C
#define __UINT64_C(c)     c ## ULL
#endif

#ifndef INT64_MIN
#define INT64_MIN        (__INT64_C(-9223372036854775807)-1)
#endif

#ifndef INT64_MAX
#define INT64_MAX        (__INT64_C(9223372036854775807))
#endif

static int64_t kWakelockMinDelay = 100000ll;  // 100ms

// ---------------------------------------------------------------------------
SimpleEventQueue::SimpleEventQueue()
    :mRunning(false),
     mStopped(false),
     mDeathRecipient(new PMDeathRecipient(this))
{
}

SimpleEventQueue::~SimpleEventQueue()
{
    stop();
    if (mPowerManager != 0) {
        sp<IBinder> binder = IInterface::asBinder(mPowerManager);
        binder->unlinkToDeath(mDeathRecipient);
    }
}

void SimpleEventQueue::start() {
    ALOGV("start: running=%d", mRunning);
    if (mRunning) {
        return;
    }

    mStopped = false;

    pthread_attr_t attr;
    pthread_attr_init(&attr);
    pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_JOINABLE);

    pthread_create(&mThread, &attr, ThreadWrapper, this);

    pthread_attr_destroy(&attr);

    mRunning = true;
}

void SimpleEventQueue::stop(bool flush) {
    ALOGV("stop: flush=%d, running=%d", flush, mRunning);
    if (!mRunning) {
        return;
    }

    if (flush) {
        postTimedEvent(new StopEvent(this), getRealTimeUs());
    } else {
        postTimedEvent(new StopEvent(this), INT64_MIN);
    }

    void *dummy;
    pthread_join(mThread, &dummy);

    // some events may be left in the queue if we did not flush and the wake lock
    // must be released.
    if (!mQueue.empty()) {
        releaseWakeLock_l(true /*force*/);
    }
    mQueue.clear();

    mRunning = false;
}

bool SimpleEventQueue::cancelEvent(int id, int type)
{
    Mutex::Autolock _l(mLock);
    //find the item and cancel it
    List<QueueItem>::iterator iter = mQueue.begin();
    while (iter != mQueue.end()) {
        if (!iter->event->equal(id, type)) {
            iter++;
            continue;
        }

        if (!iter->event->equalCaller()) {
            ALOGW("cancelEvent: require[id=%d, type=%d], event[id=%d, type=%d]", 
                id, type, iter->event->id(), iter->event->type());			
            return false;
        }

        if (iter == mQueue.begin()) {
            mQueueHeadChangedCondition.signal();
        }
        ALOGV("cancelEvent: id=%d, type=%d", id, type);

        iter->event->cancel();
        if (iter->has_wakelock) {
            releaseWakeLock_l();
        }
        iter = mQueue.erase(iter);

        break;
    }
    return true;
}

bool SimpleEventQueue::postEvent(const sp<Event>& event)
{
    return postTimedEvent(event, INT64_MIN + 1);
}

bool SimpleEventQueue::postTimedEvent(const sp<Event>& event, int64_t realtime_us)
{
    Mutex::Autolock _l(mLock);

    List<QueueItem>::iterator iter = mQueue.begin();
    while (iter != mQueue.end() && realtime_us >= iter->realtime_us) {
        ++iter;
    }

    QueueItem item;
    item.event = event;
    item.realtime_us = realtime_us;//getRealTimeUs();
    item.has_wakelock = false;

    if (iter == mQueue.begin()) {
        mQueueHeadChangedCondition.signal();
    }

    if (realtime_us > getRealTimeUs() + kWakelockMinDelay) {
        acquireWakeLock_l();
        item.has_wakelock = true;
    }
    mQueue.insert(iter, item);

    mQueueNotEmptyCondition.signal();

    return true;
}

// static
void *SimpleEventQueue::ThreadWrapper(void *me) 
{
    androidSetThreadPriority(0, ANDROID_PRIORITY_FOREGROUND);
    static_cast<SimpleEventQueue *>(me)->threadEntry();
    return NULL;
}

void SimpleEventQueue::threadEntry() 
{
    prctl(PR_SET_NAME, (unsigned long)"SimpleEventQueue", 0, 0, 0);

    while(true) {
        int64_t now_us = 0;
        sp<Event> event;
        bool wakeLocked = false;

        {
            Mutex::Autolock _l(mLock);

            if (mStopped) {
                break;
            }

            while (mQueue.empty()) {
                mQueueNotEmptyCondition.wait(mLock);
            }

            for (;;) {
                if (mQueue.empty()) {
                    // The only event in the queue could have been cancelled
                    // while we were waiting for its scheduled time.
                    break;
                }

                List<QueueItem>::iterator iter = mQueue.begin();

                now_us = getRealTimeUs();
                int64_t when_us = iter->realtime_us;

                int64_t delay_us;
                if (when_us < 0 || when_us == INT64_MAX) {
                    delay_us = 0;
                } else {
                    delay_us = when_us - now_us;
                }

                if (delay_us <= 0) {
                    break;
                }

                static int64_t kMaxTimeoutUs = 10000000ll;  // 10 secs
                bool timeoutCapped = false;
                if (delay_us > kMaxTimeoutUs) {
                    ALOGW("delay_us exceeds max timeout: %lld us", delay_us);

                    // We'll never block for more than 10 secs, instead
                    // we will split up the full timeout into chunks of
                    // 10 secs at a time. This will also avoid overflow
                    // when converting from us to ns.
                    delay_us = kMaxTimeoutUs;
                    timeoutCapped = true;
                }

                status_t err = mQueueHeadChangedCondition.waitRelative(
                        mLock, delay_us * 1000ll);

                if (!timeoutCapped && err == -ETIMEDOUT) {
                    // We finally hit the time this event is supposed to
                    // trigger.
                    now_us = getRealTimeUs();
                    break;
                }
            }

            // The event w/ this id may have been cancelled while we're
            // waiting for its trigger-time, in that case
            // removeEventFromQueue_l will return NULL.
            // Otherwise, the QueueItem will be removed
            // from the queue and the referenced event returned.
            event = removeEventFromQueue_l(&wakeLocked);
        }

        if (event != NULL && !event->isCanceled()) {
            event->fire();
            if (wakeLocked) {
                Mutex::Autolock _l(mLock);
                releaseWakeLock_l();
            }
        }
    }
}

sp<SimpleEventQueue::Event> SimpleEventQueue::removeEventFromQueue_l(bool *wakeLocked)
{
    List<QueueItem>::iterator iter = mQueue.begin();
    if (iter != mQueue.end()) {
        sp<Event> event = iter->event;
        *wakeLocked = iter->has_wakelock;
        mQueue.erase(iter);
        return event;
    }

    ALOGW("removeEventFromQueue_l: none was not found in the queue, already cancelled?");
    return NULL;
}

void SimpleEventQueue::acquireWakeLock_l()
{
    ALOGV("acquireWakeLock: count=%d", mWakeLockCount);
    if (mWakeLockCount == 0) {
        if (mPowerManager == 0) {
            // use checkService() to avoid blocking if power service is not up yet
            sp<IBinder> binder =
                defaultServiceManager()->checkService(String16("power"));
            if (binder == 0) {
                ALOGW("acquireWakeLock_l: cannot connect to the power manager service");
            } else {
                mPowerManager = interface_cast<IPowerManager>(binder);
                binder->linkToDeath(mDeathRecipient);
            }
        }
        if (mPowerManager != 0) {
            sp<IBinder> binder = new BBinder();
            int64_t token = IPCThreadState::self()->clearCallingIdentity();
            status_t status = mPowerManager->acquireWakeLock(POWERMANAGER_PARTIAL_WAKE_LOCK,
                                                             binder,
                                                             String16("SimpleEventQueue"),
                                                             String16("sensorhub"));
            IPCThreadState::self()->restoreCallingIdentity(token);
            if (status == NO_ERROR) {
                mWakeLockToken = binder;
                mWakeLockCount++;
                ALOGV("acquireWakeLock: acquired lock %p", binder.get());				
            }
        }
    } else {
        mWakeLockCount++;
    }
}

void SimpleEventQueue::releaseWakeLock_l(bool force)
{
    ALOGV("releaseWakeLock: count=%d", mWakeLockCount);
    if (mWakeLockCount == 0) {
        return;
    }
    if (force) {
        mWakeLockCount = 1;
    }
    if (--mWakeLockCount == 0) {
        if (mPowerManager != 0) {
            int64_t token = IPCThreadState::self()->clearCallingIdentity();		
            mPowerManager->releaseWakeLock(mWakeLockToken, 0);
            ALOGV("releaseWakeLock: released lock %p", mWakeLockToken.get());	
            IPCThreadState::self()->restoreCallingIdentity(token);
        }
        mWakeLockToken.clear();
    }
}

void SimpleEventQueue::clearPowerManager()
{
    Mutex::Autolock _l(mLock);
    releaseWakeLock_l(true /*force*/);
    mPowerManager.clear();
}

int64_t SimpleEventQueue::getRealTimeUs()
{
    return systemTime(SYSTEM_TIME_MONOTONIC) / 1000ll;
}

void SimpleEventQueue::PMDeathRecipient::binderDied(const wp<IBinder>& who)
{
    mQueue->clearPowerManager();
}

// ---------------------------------------------------------------------------
SimpleEventQueue::Event::Event(int id, int type)
:mId(id), mType(type), mCanceled(false)
{
}

SimpleEventQueue::Event::~Event()
{
    mId = -1;
    mType = -1;
}

};
