#ifndef __CUST_COLOR_H__
#define __CUST_COLOR_H__

#include "ddp_drv.h"
#include "ddp_gamma.h"

#define PQ_DEVICE_NODE                      "/proc/mtk_mira"

#define PQ_PIC_MODE_STANDARD (0)
#define PQ_PIC_MODE_VIVID    (1)
#define PQ_PIC_MODE_USER_DEF (2)
#define PQ_PREDEFINED_MODE_COUNT (2) //count for pic mode except user_def
#define PQ_SCENARIO_COUNT (3) //image, video, camera
#define PQ_PARAM_TABLE_SIZE (PQ_PREDEFINED_MODE_COUNT * PQ_SCENARIO_COUNT + 1) //add user_def

#define PQ_PIC_MODE_DEFAULT                 "0"     // default is STANDARD
#define PQ_PIC_MODE_PROPERTY_STR            "persist.sys.pq.picmode"
#define PQ_CONTRAST_INDEX_RANGE_NUM         (10)
#define PQ_CONTRAST_INDEX_DEFAULT           "4"
#define PQ_CONTRAST_PROPERTY_STR            "persist.sys.pq.contrastidx"
#define PQ_GSAT_INDEX_RANGE_NUM             (10)
#define PQ_GSAT_INDEX_DEFAULT               "4"
#define PQ_GSAT_PROPERTY_STR                "persist.sys.pq.gsatidx"
#define PQ_PIC_BRIGHT_INDEX_RANGE_NUM       (10)
#define PQ_PIC_BRIGHT_INDEX_DEFAULT         "4"
#define PQ_PIC_BRIGHT_PROPERTY_STR          "persist.sys.pq.brightidx"
#define PQ_TDSHP_INDEX_RANGE_NUM            (10)
#define PQ_TDSHP_STANDARD_DEFAULT           "2"
#define PQ_TDSHP_VIVID_DEFAULT              "2"
#define PQ_TDSHP_USER_DEFAULT               "2"     // sync with VIVID
#define PQ_TDSHP_INDEX_DEFAULT              PQ_TDSHP_STANDARD_DEFAULT
#define PQ_TDSHP_PROPERTY_STR               "persist.sys.pq.shp.idx"
#define PQ_ADL_INDEX_RANGE_NUM              (1)
#define PQ_ADL_INDEX_DEFAULT                "0"    // default off
#define PQ_ADL_PROPERTY_STR                 "persist.sys.pq.adl.idx"
#define PQ_MDP_COLOR_EN_INDEX_RANGE_NUM     (1)
#define PQ_MDP_COLOR_EN_DEFAULT             "0"    // 0: disable, 1: enable
#define PQ_MDP_COLOR_EN_STR                 "persist.sys.pq.mdp.color.idx"
#define PQ_MDP_COLOR_DBG_EN_INDEX_RANGE_NUM (1)
#define PQ_MDP_COLOR_DBG_EN_DEFAULT         "0"    // 0: disable, 1: enable
#define PQ_MDP_COLOR_DBG_EN_STR             "persist.sys.pq.mdp.color.dbg"

// color_ex switch
#define PQ_COLOREX_INDEX_RANGE_NUM          (1)
#define PQ_COLOREX_INDEX_DEFAULT            "0"    // default off
#define PQ_COLOREX_PROPERTY_STR             "persist.service.swiqi2.enable"


// for debug
#define PQ_DBG_SHP_EN_DEFAULT               "2" // 0: disable, 1: enable, 2: default
#define PQ_DBG_SHP_EN_STR                   "debug.pq.shp.en"

#define PQ_DBG_ADL_EN_DEFAULT               "2" // 0: disable, 1: enable, 2: default
#define PQ_DBG_ADL_EN_STR                   "debug.pq.adl.en"
#define PQ_DBG_ADL_DEBUG_DEFAULT            "0" // 0: disable, 255: log all on
#define PQ_DBG_ADL_DEBUG_STR                "debug.pq.adl.dbg"

#define PQ_DBG_DSHP_EN_DEFAULT              "2" // 0: disable, 1: enable, 2: default
#define PQ_DBG_DSHP_EN_STR                  "debug.pq.dshp.en"
#define PQ_DBG_DSHP_DEBUG_DEFAULT           "0" // 0: disable, 0xff: log all on
#define PQ_DBG_DSHP_DEBUG_STR               "debug.pq.dshp.dbg"

#define PQ_COLOR_MODE_DEFAULT               "1"    // 1: DISP_COLOR
#define PQ_COLOR_MODE_STR                   "ro.mtk_pq_color_mode"

extern const DISP_PQ_PARAM pqparam_standard;
extern const DISP_PQ_PARAM pqparam_vivid;
extern const DISP_PQ_PARAM pqparam_camera;
extern const DISPLAY_PQ_T pqindex;
extern const DISPLAY_PQ_T primary_pqindex;
extern const DISPLAY_PQ_T secondary_pqindex;
extern const DISP_PQ_MAPPING_PARAM pqparam_mapping;
extern const DISP_PQ_PARAM pqparam_table[PQ_PARAM_TABLE_SIZE];
extern const unsigned int g_ccorr_matrix[DISP_CCORR_TOTAL][3][3];

#endif

