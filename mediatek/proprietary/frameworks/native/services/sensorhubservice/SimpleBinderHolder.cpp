#include <binder/IPCThreadState.h>

#include "SimpleBinderHolder.h"

namespace android {
// ---------------------------------------------------------------------------

SimpleBinderHolder::SimpleBinderHolder()
{
    IPCThreadState* ipcState = IPCThreadState::self();
    mPid = ipcState->getCallingPid();
    mUid = ipcState->getCallingUid();
}

SimpleBinderHolder::~SimpleBinderHolder()
{
    mPid = 0;
    mUid = 0;
}

bool SimpleBinderHolder::equalCaller()
{
    IPCThreadState* ipcState = IPCThreadState::self();
    pid_t pid = ipcState->getCallingPid();
    uid_t uid = ipcState->getCallingUid();
    return (mPid == pid) && (mUid == uid);
}

bool SimpleBinderHolder::equalCaller(pid_t pid, uid_t uid)
{
    return ((mPid == pid) && (mUid == uid));
}

};
