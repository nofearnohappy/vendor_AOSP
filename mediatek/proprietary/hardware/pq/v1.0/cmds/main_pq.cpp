#define LOG_TAG "PQ"

#define MTK_LOG_ENABLE 1
#include <cutils/log.h>
#include <stdint.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <sys/types.h>
#include <unistd.h>

#include "ddp_drv.h"
#include "ddp_color_index.h"
#include "ddp_tdshp_index.h"

int drvID = -1;
unsigned long lcmindex = 0; 

const DISP_PQ_PARAM pqparam = 
{
    u4SHPGain:2,
    u4SatGain:0,
    u4HueAdj:{9,9,12,12},
    u4SatAdj:{0,2,6,6}
};

const DISP_PQ_PARAM pqparam_camera =
{
    u4SHPGain:0,
    u4SatGain:0,
    u4HueAdj:{9,9,9,9},
    u4SatAdj:{0,0,0,0}
};



//COLOR_TUNING_INDEX : 10


//DISPLAY_PQ_T pqindex;
int main(int argc, char** argv) 
{
    int actionID=0, RegBase = 0, RegValue = 0, err = 0;
    char fileName[256];

    ALOGD("PQ init start...");
    if(drvID == -1) //initial
        drvID = open("/dev/mtk_disp", O_RDONLY, 0);

    ALOGD("PQ test...");
    ioctl(drvID, DISP_IOCTL_SET_PQINDEX, &pqindex);
    ioctl(drvID, DISP_IOCTL_SET_PQPARAM, &pqparam);
    ioctl(drvID, DISP_IOCTL_SET_GAMMALUT, &gammaindex);
    ioctl(drvID, DISP_IOCTL_SET_TDSHPINDEX, &tdshpindex);
    ioctl(drvID, DISP_IOCTL_SET_PQ_CAM_PARAM, &pqparam_camera);
    ioctl(drvID, DISP_IOCTL_SET_PQ_GAL_PARAM, &pqparam);

    ALOGD("PQ init end !");

    if (drvID > 0)
    {
        close(drvID);
    }

    return 0;
}
