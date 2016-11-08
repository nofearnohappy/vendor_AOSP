#ifndef ANDROID_SENSORHUB_SIMPLE_BINDER_HOLDER_H
#define ANDROID_SENSORHUB_SIMPLE_BINDER_HOLDER_H

#include <stdint.h>
#include <sys/types.h>

#include <utils/RefBase.h>
#include <binder/IPCThreadState.h>

// ----------------------------------------------------------------------------
namespace android {

// ----------------------------------------------------------------------------
class SimpleBinderHolder: public virtual RefBase {
public:
    SimpleBinderHolder();
    virtual ~SimpleBinderHolder();

    bool equalCaller(pid_t pid, uid_t uid);
    bool equalCaller();
    inline pid_t pid() { return mPid; }
    inline uid_t uid() { return mUid; }
private:
    pid_t mPid;
    uid_t mUid;
};

// ----------------------------------------------------------------------------
}; // namespace android

#endif // ANDROID_SENSORHUB_SIMPLE_BINDER_HOLDER_H
