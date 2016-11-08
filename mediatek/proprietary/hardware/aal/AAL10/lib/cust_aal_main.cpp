#define LOG_TAG "AALCust"

#define MTK_LOG_ENABLE 1
#include <cutils/log.h>
#include <stdint.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <sys/types.h>
#include <unistd.h>

#include "cust_aal.h"
#include "ddp_drv.h"

extern int lcmCnt;
extern struct CUST_AAL_ALS_DATA aALSCalData;
extern struct CUST_AAL_LCM_DATA aLCMCalData[];
extern struct CUST_AAL_PARAM aAALParam[];
    
static int getLCMIdx()
{
    static int lcmIdx = -1;

    if (lcmIdx == -1) {
        int drvID = open("/dev/mtk_disp", O_RDONLY, 0);
        if (drvID >= 0) {
            ioctl(drvID, DISP_IOCTL_GET_LCMINDEX, &lcmIdx);
            close(drvID);
            if (lcmIdx >= lcmCnt)
            {
                ALOGE("Invalod LCM index %d, LCM count %d", lcmIdx, lcmCnt);
                lcmIdx = 0;
            }
        }
        else {
            ALOGE("Fail to open disp driver!");
            lcmIdx = 0;
        }
    }    

    ALOGI("LCM index: %d/%d", lcmIdx, lcmCnt);
    return lcmIdx;
}


struct CUST_AAL_ALS_DATA *getALSCalData()
{
    return &aALSCalData;
}

struct CUST_AAL_LCM_DATA *getLCMCalData()
{
    return &aLCMCalData[getLCMIdx()];
}


struct CUST_AAL_PARAM *getAALParam()
{
    return &aAALParam[getLCMIdx()];
}

