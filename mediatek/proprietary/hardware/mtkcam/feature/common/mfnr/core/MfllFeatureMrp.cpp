#include "MfllFeatureMrp.h"
#include "MfllLog.h"
#include "MfllCore.h"

using namespace mfll;
using std::vector;

static vector<enum EventType> g_eventsToListen = {
    /* release RAW buffers if necessary */
    EventType_Bss, // when BSS done, we need to release frame buffers but not included base frame.
    EventType_EncodeYuvGolden, // to release base RAW frame

    /* release ME related buffers */
    EventType_MotionEstimation,

    /* release MC related buffers */
    EventType_MotionCompensation
};

MfllFeatureMrp::MfllFeatureMrp(void)
{
    m_mrpMode = (enum MrpMode)MFLL_MRP_SUPPORT_MODE;
}

MfllFeatureMrp::~MfllFeatureMrp(void)
{
}

void MfllFeatureMrp::onEvent(enum EventType t, MfllEventStatus_t &status, void *mfllCore, void *param1, void *param2)
{
    mfllLogD("Mrp-onEvent: %s", IMfllCore::getEventTypeName(t));
}

void MfllFeatureMrp::doneEvent(enum EventType t, MfllEventStatus_t &status, void *mfllCore, void *param1, void *param2)
{
    enum MfllErr err = MfllErr_Ok;

    mfllLogD("Mrp-doneEvent: %s", IMfllCore::getEventTypeName(t));
    mfllLogD("status.ignore = %d", status.ignore);

    switch(t) {
    /* release RAW buffers */
    case EventType_EncodeYuvGolden:
        releaseBaseRawBuffer((IMfllCore*)mfllCore);
        break;

    case EventType_Bss:
        releaseReferenceRawBuffers((IMfllCore*)mfllCore);
        break;

    case EventType_MotionEstimation:
        break;
    } // switch(t)
}

vector<enum EventType> MfllFeatureMrp::getListenedEventTypes(void)
{
    return g_eventsToListen;
}

void MfllFeatureMrp::releaseReferenceRawBuffers(IMfllCore *pCore)
{
    enum MfllErr err = MfllErr_Ok;
    MfllCore *c = (MfllCore*)pCore;
    unsigned int frameNum = c->getCaptureFrameNum();

    mfllFunctionIn();

    for (int i = 1; i < (int)c->getCaptureFrameNum(); i++) {
        mfllLogD("%s: set m_imgRaw[%d] to NULL to release", __FUNCTION__, i);
        c->m_imgRaws[i] = NULL;
    }

    mfllFunctionOut();
}

void MfllFeatureMrp::releaseBaseRawBuffer(IMfllCore *pCore)
{
    enum MfllErr err = MfllErr_Ok;
    mfllFunctionIn();

    IMfllImageBuffer *base_img = pCore->getRawBuffer(0);
    mfllLogD("%s: to release base raw buffer addr=%p", __FUNCTION__, (void*)base_img);
    if (base_img == NULL) {
        mfllLogD("%s: base raw is NULL, ignored", __FUNCTION__);
        goto lbExit;
    }

    err = base_img->releaseBuffer();
    if (err != MfllErr_Ok) {
        mfllLogE("%s: release base raw buffer failed(%d)", __FUNCTION__, (int)err);
    }

lbExit:
    mfllFunctionOut();
}

void MfllFeatureMrp::releaseMemcRelatedBuffers(IMfllCore *pCore, unsigned int index)
{
    enum MfllErr err = MfllErr_Ok;
    MfllCore *c = (MfllCore*)pCore;
    unsigned int frameBlend = c->getBlendFrameNum();

    mfllFunctionIn();
    // TODO: finish this part
    mfllFunctionOut();
}

