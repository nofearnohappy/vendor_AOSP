#ifndef __CUST_TDSHP_H__
#define __CUST_TDSHP_H__

#include "ddp_drv.h"


#define TDSHP_FLAG_NORMAL           (0x00000000)
#define TDSHP_FLAG_TUNING           (0x00000001)
#define TDSHP_FLAG_DC_TUNING        (0x00000002)
#define TDSHP_FLAG_NCS_SHP_TUNING   (0x00000004)
#define TDSHP_FLAG_DS_TUNING        (0x00000008)
#define SUPPORT_PQ_PATH0 "/sdcard/SUPPORT_PQ"
#define SUPPORT_PQ_PATH1 "/storage/sdcard1/SUPPORT_PQ"

// for debug
#define PQ_DBG_SHP_TUNING_DEFAULT                   "0" // 0: disable, 1: enable, 2: default
#define PQ_DBG_SHP_TUNING_STR                       "debug.pq.shp.tuning"

extern const DISPLAY_TDSHP_T tdshpindex;

#endif

