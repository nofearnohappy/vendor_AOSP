#ifndef __MFLLFEATUREMRP_H__
#define __MFLLFEATUREMRP_H__

#include "IMfllCore.h"
#include "IMfllEventListener.h"

namespace mfll {
class MfllFeatureMrp : public IMfllEventListener {
public:
    MfllFeatureMrp(void);
    virtual ~MfllFeatureMrp(void);

/* implementation */
public:
    virtual void onEvent(enum EventType t, MfllEventStatus_t &status, void *mfllCore, void *param1 = NULL, void *param2 = NULL);
    virtual void doneEvent(enum EventType t, MfllEventStatus_t &status, void *mfllCore, void *param1 = NULL, void *param2 = NULL);
    virtual vector<enum EventType> getListenedEventTypes(void);

/* interface */
public:
    inline void setMemoryReducePlanMode(const enum MrpMode &m) { m_mrpMode = m; }
    inline enum MrpMode getMemoryReducePlanMode(void) { return m_mrpMode; }

private:
    /* release RAW buffers */
    void releaseReferenceRawBuffers(IMfllCore *pCore);
    void releaseBaseRawBuffer(IMfllCore *pCore);

    /* release ME/MC related buffers */
    void releaseMemcRelatedBuffers(IMfllCore *pCore, unsigned int index);

private:
    enum MrpMode m_mrpMode;
}; /* class MfllFeatureMrp */
}; /* namespace mfll */
#endif//__MFLLFEATUREMRP_H__

