#include "SpeechDriverFactory.h"
#include "SpeechType.h"
#include "SpeechDriverInterface.h"
#include "SpeechDriverDummy.h"
#include "SpeechDriverLAD.h"
#include "AudioUtility.h"

#include <utils/threads.h>
#ifdef MTK_BASIC_PACKAGE
#include "AudioTypeExt.h"
#endif

#ifdef EVDO_DT_VEND_SUPPORT  //evod modem speech driver
#include "SpeechDriverVendEVDO.h"
#endif

#if defined (DSDA_SUPPORT)
#include "SpeechDriverDSDA.h"
#endif

#define LOG_TAG "SpeechDriverFactory"

namespace android
{

SpeechDriverFactory *SpeechDriverFactory::mSpeechDriverFactory = NULL;
SpeechDriverFactory *SpeechDriverFactory::GetInstance()
{
    static Mutex mGetInstanceLock;
    Mutex::Autolock _l(mGetInstanceLock);
    ALOGV("%s()", __FUNCTION__);

    if (mSpeechDriverFactory == NULL)
    {
        mSpeechDriverFactory = new SpeechDriverFactory();
    }
    ASSERT(mSpeechDriverFactory != NULL);
    return mSpeechDriverFactory;
}

SpeechDriverFactory::SpeechDriverFactory()
{
    ALOGV("%s()", __FUNCTION__);

    mSpeechDriver1 = NULL;
    mSpeechDriver2 = NULL;
    mSpeechDriverExternal = NULL;

#if defined (__MTK_ENABLE_MD1__)
    mActiveModemIndex = MODEM_1; // default use modem 1
#elif defined (__MTK_ENABLE_MD2__)
    mActiveModemIndex = MODEM_2; // if modem 1 not enabled, default use modem 2
#elif defined (__MTK_ENABLE_MD5__)
    mActiveModemIndex = MODEM_EXTERNAL; // if modem 1 not enabled, default use modem 2
#elif defined(EVDO_DT_VEND_SUPPORT)
    mActiveModemIndex = MODEM_EXTERNAL; // if modem vend evdo,default use modem external
#elif defined(MTK_C2K_SUPPORT)
    mActiveModemIndex = MODEM_EXTERNAL; // if modem evdo(c2k),default use modem external
#else
    ALOGW("mActiveModemIndex default use modem 1 !!");
    mActiveModemIndex = MODEM_1; // default use modem 1
#endif

    CreateSpeechDriverInstances();

    ALOGD("-%s(), mActiveModemIndex = %d", __FUNCTION__, mActiveModemIndex);
}
status_t SpeechDriverFactory::CreateSpeechDriverInstances()
{
    /// Create mSpeechDriver for modem 1
#ifdef __MTK_ENABLE_MD1__
    // for internal modem_1, always return LAD
    ALOGD("Create SpeechDriverLAD for MODEM_1");
    mSpeechDriver1 = SpeechDriverLAD::GetInstance(MODEM_1);
#else
    ALOGW("Create SpeechDriverDummy for MODEM_1");
    mSpeechDriver1 = new SpeechDriverDummy(MODEM_1);
#endif


    /// Create mSpeechDriver for modem 2
#ifdef __MTK_ENABLE_MD2__
    // for modem_2, might use internal/external modem
    ALOGD("Create SpeechDriverLAD for MODEM_2");
    mSpeechDriver2 = SpeechDriverLAD::GetInstance(MODEM_2);
#else
    ALOGW("Create SpeechDriverDummy for MODEM_2");
    mSpeechDriver2 = new SpeechDriverDummy(MODEM_2);
#endif

    /// Create mSpeechDriver for modem external
#ifdef __MTK_ENABLE_MD5__
#if defined (DSDA_SUPPORT)
    ALOGD("Create SpeechDriverDSDA for MODEM_EXTERNAL");
    mSpeechDriverExternal = SpeechDriverDSDA::GetInstance(MODEM_EXTERNAL);
#else
    ALOGD("Create SpeechDriverLAD for MODEM_EXTERNAL"); // for external modem LTE: use EEMCS; c2k
    mSpeechDriverExternal = SpeechDriverLAD::GetInstance(MODEM_EXTERNAL);
#endif
#else
#ifdef EVDO_DT_VEND_SUPPORT  //vend evdo modem speech driver
    ALOGD("Create SpeechDriverVendEVDO for MODEM_EXTERNAL");
    mSpeechDriverExternal = SpeechDriverVendEVDO::GetInstance(MODEM_EXTERNAL);
#elif MTK_C2K_SUPPORT
    ALOGD("Create SpeechDriverEVDO for MODEM_EXTERNAL");
    mSpeechDriverExternal = SpeechDriverLAD::GetInstance(MODEM_EXTERNAL);
#else
    ALOGW("Create SpeechDriverDummy for MODEM_EXTERNAL");
    mSpeechDriverExternal = new SpeechDriverDummy(MODEM_EXTERNAL);
#endif
#endif

    return NO_ERROR;
}

status_t SpeechDriverFactory::DestroySpeechDriverInstances()
{
    if (mSpeechDriver1 != NULL)
    {
        delete mSpeechDriver1;
        mSpeechDriver1 = NULL;
    }

    if (mSpeechDriver2 != NULL)
    {
        delete mSpeechDriver2;
        mSpeechDriver2 = NULL;
    }

    if (mSpeechDriverExternal != NULL)
    {
        delete mSpeechDriverExternal;
        mSpeechDriverExternal = NULL;
    }
    return NO_ERROR;
}

SpeechDriverFactory::~SpeechDriverFactory()
{
    DestroySpeechDriverInstances();
}

SpeechDriverInterface *SpeechDriverFactory::GetSpeechDriver()
{
    SpeechDriverInterface *pSpeechDriver = NULL;
    ALOGD("%s(), mActiveModemIndex=%d", __FUNCTION__, mActiveModemIndex);

    switch (mActiveModemIndex)
    {
        case MODEM_1:
            pSpeechDriver = mSpeechDriver1;
            break;
        case MODEM_2:
            pSpeechDriver = mSpeechDriver2;
            break;
        case MODEM_EXTERNAL:
            pSpeechDriver = mSpeechDriverExternal;
            break;
        default:
            ALOGE("%s: no such modem index %d", __FUNCTION__, mActiveModemIndex);
            break;
    }

    ASSERT(pSpeechDriver != NULL);
    return pSpeechDriver;
}

/**
 * NO GUARANTEE that the returned pointer is not NULL!!
 * Be careful to use this function!!
 */
SpeechDriverInterface *SpeechDriverFactory::GetSpeechDriverByIndex(const modem_index_t modem_index)
{
    SpeechDriverInterface *pSpeechDriver = NULL;
    ALOGD("%s(), modem_index=%d", __FUNCTION__, modem_index);

    switch (modem_index)
    {
        case MODEM_1:
            pSpeechDriver = mSpeechDriver1;
            break;
        case MODEM_2:
            pSpeechDriver = mSpeechDriver2;
            break;
        case MODEM_EXTERNAL:
            pSpeechDriver = mSpeechDriverExternal;
            break;
        default:
            ALOGE("%s: no such modem index %d", __FUNCTION__, modem_index);
            break;
    }

    return pSpeechDriver;
}


modem_index_t SpeechDriverFactory::GetActiveModemIndex() const
{
    ALOGD("%s(), active modem index = %d", __FUNCTION__, mActiveModemIndex);
    return mActiveModemIndex;
}

status_t SpeechDriverFactory::SetActiveModemIndex(const modem_index_t modem_index)
{
    ALOGD("%s(), old modem index = %d, new modem index = %d", __FUNCTION__, mActiveModemIndex, modem_index);
    mActiveModemIndex = modem_index;
    return NO_ERROR;
}


status_t SpeechDriverFactory::SetActiveModemIndexByAudioMode(const audio_mode_t audio_mode)
{
    status_t return_status = NO_ERROR;

    switch (audio_mode)
    {
        case AUDIO_MODE_IN_CALL:
            return_status = SetActiveModemIndex(MODEM_1);
            break;
        case AUDIO_MODE_IN_CALL_2:
            return_status = SetActiveModemIndex(MODEM_2);
            break;
        case AUDIO_MODE_IN_CALL_EXTERNAL:
            return_status = SetActiveModemIndex(MODEM_EXTERNAL);
            break;
        default:
            ALOGE("%s() mode(%d) is neither MODE_IN_CALL nor MODE_IN_CALL_2!!", __FUNCTION__, audio_mode);
            return_status = INVALID_OPERATION;
            break;
    }
    return return_status;
}


} // end of namespace android

