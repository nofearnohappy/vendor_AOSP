#define LOG_TAG "PQClient"
#define MTK_LOG_ENABLE 1
#include <cutils/log.h>
#include <binder/IServiceManager.h>
#include <binder/ProcessState.h>
#include "PQClient.h"
#include "IPQService.h"

namespace android {


ANDROID_SINGLETON_STATIC_INSTANCE(PQClient);

PQClient::PQClient()
{
    assertStateLocked();
}

status_t PQClient::assertStateLocked() const
{
    int count = 0;
    if (mPQService == NULL)
    {
        // try for one second
        const String16 name("PQ");
        do {
            status_t err = getService(name, &mPQService);
            if (err == NAME_NOT_FOUND) {
                if (count < 3)
                {
                    ALOGW("PQService not published, waiting...");
                    usleep(10000); //0.1sec
                    count++;
                    continue;
                }
                return err;
            }
            if (err != NO_ERROR) {
                return err;
            }
            break;
        } while (true);

        class DeathObserver : public IBinder::DeathRecipient {
            PQClient & mPQClient;
            virtual void binderDied(const wp<IBinder>& who) {
                ALOGW("PQ Service died [%p]", who.unsafe_get());
                mPQClient.serviceDied();
            }
        public:
            DeathObserver(PQClient & service) : mPQClient(service) { }
        };

        mDeathObserver = new DeathObserver(*const_cast<PQClient*>(this));
        IInterface::asBinder(mPQService)->linkToDeath(mDeathObserver);
    }
    return NO_ERROR;
}

void PQClient::serviceDied()
{
    Mutex::Autolock _l(mLock);
    mPQService.clear();
}

status_t PQClient::setPQIndex(int32_t level, int32_t scenario, int32_t tuning_mode, int32_t index)
{
    status_t err;
    Mutex::Autolock _l(mLock);
    err = assertStateLocked();
    if (err == NO_ERROR)
        err = mPQService->setPQIndex(level,scenario,tuning_mode,index);

    return err;
}

status_t PQClient::getMappedColorIndex(DISP_PQ_PARAM *index, int32_t scenario, int32_t mode)
{
    status_t err;
    Mutex::Autolock _l(mLock);
    err = assertStateLocked();
    if (err == NO_ERROR)
        err = mPQService->getMappedColorIndex(index,scenario,mode);

    return err;
}

status_t PQClient::getMappedTDSHPIndex(DISP_PQ_PARAM *index, int32_t scenario, int32_t mode)
{
    status_t err;
    Mutex::Autolock _l(mLock);
    err = assertStateLocked();
    if (err == NO_ERROR)
        err = mPQService->getMappedTDSHPIndex(index,scenario,mode);

    return err;
}


status_t PQClient::setPQDCIndex(int32_t level, int32_t index )
{
    status_t err;
    Mutex::Autolock _l(mLock);
    err = assertStateLocked();
    if (err == NO_ERROR)
        err = mPQService->setPQDCIndex(level,index);

    return err;
}

status_t PQClient::getPQDCIndex(DISP_PQ_DC_PARAM *dcparam, int32_t index )
{
    status_t err;
    Mutex::Autolock _l(mLock);
    err = assertStateLocked();
    if (err == NO_ERROR)
        err = mPQService->getPQDCIndex(dcparam,index);

    return err;
}

status_t PQClient::getPQDSIndex(DISP_PQ_DS_PARAM *dsparam)
{
    status_t err;
    Mutex::Autolock _l(mLock);
    err = assertStateLocked();
    if (err == NO_ERROR)
        err = mPQService->getPQDSIndex(dsparam);

    return err;
}
status_t PQClient::getColorCapInfo(MDP_COLOR_CAP *param)
{
    status_t err;
    Mutex::Autolock _l(mLock);
    err = assertStateLocked();
    if (err == NO_ERROR)
        err = mPQService->getColorCapInfo(param);

    return err;
}
status_t PQClient::getTDSHPReg(MDP_TDSHP_REG *param)
{
    status_t err;
    Mutex::Autolock _l(mLock);
    err = assertStateLocked();
    if (err == NO_ERROR)
        err = mPQService->getTDSHPReg(param);

    return err;
}
status_t PQClient::getColorIndex(DISP_PQ_PARAM *index, int32_t scenario, int32_t mode)
{
    status_t err;
    Mutex::Autolock _l(mLock);
    err = assertStateLocked();
    if (err == NO_ERROR)
        err = mPQService->getColorIndex(index,scenario,mode);

    return err;
}

status_t PQClient::getTDSHPIndex(DISP_PQ_PARAM *index, int32_t scenario, int32_t mode)
{
    status_t err;
    Mutex::Autolock _l(mLock);
    err = assertStateLocked();
    if (err == NO_ERROR)
        err = mPQService->getTDSHPIndex(index,scenario,mode);

    return err;
}
status_t PQClient::getTDSHPFlag(int32_t *TDSHPFlag)
{
    status_t err;
    Mutex::Autolock _l(mLock);
    err = assertStateLocked();
    if (err == NO_ERROR)
        err = mPQService->getTDSHPFlag(TDSHPFlag);

    return err;
}

status_t PQClient::setTDSHPFlag(int32_t TDSHPFlag)
{
    status_t err;
    Mutex::Autolock _l(mLock);
    err = assertStateLocked();
    if (err == NO_ERROR)
        err = mPQService->setTDSHPFlag(TDSHPFlag);

    return err;
}

status_t PQClient::setPQMode(int32_t mode)
{
    status_t err;
    Mutex::Autolock _l(mLock);
    err = assertStateLocked();
    if (err == NO_ERROR)
        err = mPQService->setPQMode(mode);

    return err;
}
status_t PQClient::setColorRegion(int32_t split_en,int32_t start_x,int32_t start_y,int32_t end_x,int32_t end_y)
{
    status_t err;
    Mutex::Autolock _l(mLock);
    err = assertStateLocked();
    if (err == NO_ERROR)
        err = mPQService->setColorRegion(split_en, start_x,start_y,end_x, end_y);

    return err;
}

status_t PQClient::getColorRegion(DISP_PQ_WIN_PARAM *win_param)
{
    status_t err;
    Mutex::Autolock _l(mLock);
    err = assertStateLocked();
    if (err == NO_ERROR)
        err = mPQService->getColorRegion(win_param);

    return err;
}
status_t PQClient::setDISPScenario(int32_t scenario)
{
    status_t err;
    Mutex::Autolock _l(mLock);
    err = assertStateLocked();
    if (err == NO_ERROR)
        err = mPQService->setDISPScenario(scenario);

    return err;
}

status_t PQClient::setFeatureSwitch(IPQService::PQFeatureID id, uint32_t value)
{
    status_t err;
    Mutex::Autolock _l(mLock);
    err = assertStateLocked();
    if (err == NO_ERROR)
        err = mPQService->setFeatureSwitch(id, value);

    return err;
}

status_t PQClient::getFeatureSwitch(IPQService::PQFeatureID id, uint32_t *value)
{
    status_t err;
    Mutex::Autolock _l(mLock);
    err = assertStateLocked();
    if (err == NO_ERROR)
        err = mPQService->getFeatureSwitch(id, value);

    return err;
}



};

