#ifndef __MFLLEVENTLISTENER_H__
#define __MFLLEVENTLISTENER_H__

#include "MfllTypes.h"

#include <utils/RefBase.h> // android::sp
#include <vector> // std::vector

using android::sp;

namespace mfll {

/**
 *  THIS IS A PURE VIRTUAL CLASS.
 *
 *  A basic event template, we won't REALLY implement this class because user
 *  may want to implement customized event by inheriting this class
 */
class MfllEventListener : public android::RefBase {
public:
    MfllEventListener(void);
    virtual ~MfllEventListener(void);

public:
    /**
     *  The event which is invoked while an operaion is just being to be executed.
     *  @param t                - enum EventType
     *  @param *mfllCore        - A pointer to the caller (MfllCore)
     *  @param *param1          - Some events pass this argument. See MfllType.h for more information
     *  @param *param2          - Some events pass this argument. See MfllType.h for more information
     *  @notice                 - This function is called synchronized therefore do not block operation flow.
     */
    virtual void onEvent(enum EventType t, void *mfllCore, void *param1 = NULL, void *param2 = NULL) = 0;

    /**
     *  The event which is invoked while an operaion has been executed done.
     *  @param t                - enum EventType
     *  @param *mfllCore        - A pointer to the caller (MfllCore)
     *  @param *param1          - Some events pass this argument. See MfllType.h for more information
     *  @param *param2          - Some events pass this argument. See MfllType.h for more information
     *  @notice                 - This function is called synchronized therefore do not block operation flow.
     */
    virtual void doneEvent(enum EventType t, void *mfllCore, void *param1 = NULL, void *param2 = NULL) = 0;
};

};//namespace mfll

#endif//__MFLLEVENTLISTENER_H__
