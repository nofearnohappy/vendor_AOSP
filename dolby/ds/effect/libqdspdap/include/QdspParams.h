/******************************************************************************
 *  This program is protected under international and U.S. copyright laws as
 *  an unpublished work. This program is confidential and proprietary to the
 *  copyright owners. Reproduction or disclosure, in whole or in part, or the
 *  production of derivative works therefrom without the express permission of
 *  the copyright owners is prohibited.
 *
 *                 Copyright (C) 2014 by Dolby Laboratories,
 *                             All rights reserved.
 ******************************************************************************/
#ifndef DOLBY_QDSP_PARAMS_H_
#define DOLBY_QDSP_PARAMS_H_

#include "DapParams.h"

#define QDSP_PARAM_HEADER_DEVICE_IDX 0
#define QDSP_PARAM_HEADER_PARAM_IDX  1
#define QDSP_PARAM_HEADER_OFFSET_IDX 2
#define QDSP_PARAM_HEADER_LENGTH_IDX 3
#define QDSP_PARAM_VALUES_START_IDX  4

#define QDSP_PARAM_HEADER_LEN        4
#define QDSP_PARAM_SINGLE_VALUE_LEN  (QDSP_PARAM_HEADER_LEN + 1)
#define QDSP_PARAM_PAYLOAD_LEN       124
#define QDSP_PARAM_TOTAL_LEN         (QDSP_PARAM_HEADER_LEN + QDSP_PARAM_PAYLOAD_LEN)

#define QDSP_VISUALIZER_LENGTH_IDX   0
#define QDSP_VISUALIZER_VALUES_IDX   1
#define QDSP_VISUALIZER_PAYLOAD_LEN  40
#define QDSP_VISUALIZER_TOTAL_LEN    41

#define DS1_USR_COMMIT_ALL_TO_DSP     0x70000001
#define DS1_USR_COMMIT_TO_DSP         0x70000002
#define DS1_USR_USE_CACHE             0x70000003
#define DS1_USR_AUTO_ENDP             0x70000004
#define DS1_USR_AUTO_ENDDEP_PARAMS    0x70000005

namespace dolby {

enum QdspParameterId {
    QDSP_INVALID_PARAM  = -1,
    QDSP_DAP_PARAM_VDHE = 0x0001074D,
    QDSP_DAP_PARAM_VSPE = 0x00010750,
    QDSP_DAP_PARAM_DSSF = 0x00010753,
    QDSP_DAP_PARAM_DVLI = 0x0001073E,
    QDSP_DAP_PARAM_DVLO = 0x0001073F,
    QDSP_DAP_PARAM_DVLE = 0x0001073C,
    QDSP_DAP_PARAM_DVMC = 0x00010741,
    QDSP_DAP_PARAM_DVME = 0x00010740,
    QDSP_DAP_PARAM_IENB = 0x00010744,
    QDSP_DAP_PARAM_IEBF = 0x00010745,
    QDSP_DAP_PARAM_IEON = 0x00010743,
    QDSP_DAP_PARAM_DEON = 0x00010738,
    QDSP_DAP_PARAM_NGON = 0x00010736,
    QDSP_DAP_PARAM_GEON = 0x00010748,
    QDSP_DAP_PARAM_GENB = 0x00010749,
    QDSP_DAP_PARAM_GEBF = 0x0001074A,
    QDSP_DAP_PARAM_AONB = 0x0001075B,
    QDSP_DAP_PARAM_AOBF = 0x0001075C,
    QDSP_DAP_PARAM_AOBG = 0x0001075D,
    QDSP_DAP_PARAM_AOON = 0x00010759,
    QDSP_DAP_PARAM_ARNB = 0x0001075F,
    QDSP_DAP_PARAM_ARBF = 0x00010760,
    QDSP_DAP_PARAM_PLB  = 0x00010768,
    QDSP_DAP_PARAM_PLMD = 0x00010767,
    QDSP_DAP_PARAM_DHSB = 0x0001074E,
    QDSP_DAP_PARAM_DHRG = 0x0001074F,
    QDSP_DAP_PARAM_DSSB = 0x00010751,
    QDSP_DAP_PARAM_DSSA = 0x00010752,
    QDSP_DAP_PARAM_DVLA = 0x0001073D,
    QDSP_DAP_PARAM_IEBT = 0x00010746,
    QDSP_DAP_PARAM_IEA  = 0x0001076A,
    QDSP_DAP_PARAM_DEA  = 0x00010739,
    QDSP_DAP_PARAM_DED  = 0x0001073A,
    QDSP_DAP_PARAM_GEBG = 0x0001074B,
    QDSP_DAP_PARAM_AOCC = 0x0001075A,
    QDSP_DAP_PARAM_ARBI = 0x00010761,
    QDSP_DAP_PARAM_ARBL = 0x00010762,
    QDSP_DAP_PARAM_ARBH = 0x00010763,
    QDSP_DAP_PARAM_AROD = 0x00010764,
    QDSP_DAP_PARAM_ARTP = 0x00010765,
    QDSP_DAP_PARAM_VMON = 0x00010756,
    QDSP_DAP_PARAM_VMB  = 0x00010757,
    QDSP_DAP_PARAM_VCNB = 0x00010733,
    QDSP_DAP_PARAM_VCBF = 0x00010734,
    QDSP_DAP_PARAM_PREG = 0x00010728,
    QDSP_DAP_PARAM_VEN  = 0x00010732,
    QDSP_DAP_PARAM_PSTG = 0x00010729,
    QDSP_DAP_PARAM_VCBG = 0x00010730,
    QDSP_DAP_PARAM_VCBE = 0x00010731,
    QDSP_DAP_PARAM_ENDP = 0x00010727,
};

QdspParameterId qdspParamIdForParam(DapParameterId id);
DapParameterId  paramIdForQdspId(QdspParameterId id);

} // namespace dolby

#endif//DOLBY_QDSP_DAP_PARAMS_H_
