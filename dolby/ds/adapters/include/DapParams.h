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

#ifndef DOLBY_DAP_PARAMS_H_
#define DOLBY_DAP_PARAMS_H_
#include <utils/String8.h>

namespace dolby {

typedef int dap_param_value_t;

// The DAP parameter id are composed by 4 characters, and they are calculated
// using following formula:
//      id = name[0] + (name[1] << 8) + (name[2] << 16) + (name[3] << 24)

enum DapParameterId {
    DAP_PARAM_INVALID = -1,
    DAP_PARAM_VOL  = 0x006c6f76,
    DAP_PARAM_PSTG = 0x67747370,
    DAP_PARAM_PREG = 0x67657270,
    DAP_PARAM_PLB  = 0x00626c70,
    DAP_PARAM_NGON = 0x6e6f676e,
    DAP_PARAM_DEON = 0x6e6f6564,
    DAP_PARAM_DEA  = 0x00616564,
    DAP_PARAM_DED  = 0x00646564,
    DAP_PARAM_VMB  = 0x00626d76,
    DAP_PARAM_DVME = 0x656d7664,
    DAP_PARAM_DVMC = 0x636d7664,
    DAP_PARAM_DVLE = 0x656c7664,
    DAP_PARAM_DVLA = 0x616c7664,
    DAP_PARAM_DVLI = 0x696c7664,
    DAP_PARAM_DVLO = 0x6f6c7664,
    DAP_PARAM_IEON = 0x6e6f6569,
    DAP_PARAM_IEA  = 0x00616569,
    DAP_PARAM_AROD = 0x646f7261,
    DAP_PARAM_ARTP = 0x70747261,
    DAP_PARAM_DSSF = 0x66737364,
    DAP_PARAM_DSSA = 0x61737364,
    DAP_PARAM_DHRG = 0x67726864,
    DAP_PARAM_GEON = 0x6e6f6567,
    DAP_PARAM_AOON = 0x6e6f6f61,
    DAP_PARAM_VCNB = 0x626e6376,
    DAP_PARAM_VCBF = 0x66626376,
    DAP_PARAM_VER  = 0x00726576,
    DAP1_PARAM_BVER  = 0x72657662,
    DAP1_PARAM_BNDL  = 0x6c646e62,
    DAP1_PARAM_OCF   = 0x0066636f,
    DAP1_PARAM_VDHE  = 0x65686476,
    DAP1_PARAM_VSPE  = 0x65707376,
    DAP1_PARAM_SCPE  = 0x65706373,
    DAP1_PARAM_IENB  = 0x626e6569,
    DAP1_PARAM_IEBF  = 0x66626569,
    DAP1_PARAM_GENB  = 0x626e6567,
    DAP1_PARAM_GEBF  = 0x66626567,
    DAP1_PARAM_AONB  = 0x626e6f61,
    DAP1_PARAM_AOBF  = 0x66626f61,
    DAP1_PARAM_AOBG  = 0x67626f61,
    DAP1_PARAM_ARNB  = 0x626e7261,
    DAP1_PARAM_ARBF  = 0x66627261,
    DAP1_PARAM_PLMD  = 0x646d6c70,
    DAP1_PARAM_TEST  = 0x74736574,
    DAP1_PARAM_VEN   = 0x006e6576,
    DAP1_PARAM_VNNB  = 0x626e6e76,
    DAP1_PARAM_VNBF  = 0x66626e76,
    DAP1_PARAM_VNBG  = 0x67626e76,
    DAP1_PARAM_VNBE  = 0x65626e76,
    DAP1_PARAM_VCBG  = 0x67626376,
    DAP1_PARAM_VCBE  = 0x65626376,
    DAP1_PARAM_VMON  = 0x6e6f6d76,
    DAP1_PARAM_LCMF  = 0x666d636c,
    DAP1_PARAM_LCVD  = 0x6476636c,
    DAP1_PARAM_LCPT  = 0x7470636c,
    DAP1_PARAM_DHSB  = 0x62736864,
    DAP1_PARAM_DSSB  = 0x62737364,
    DAP1_PARAM_IEBT  = 0x74626569,
    DAP1_PARAM_GEBG  = 0x67626567,
    DAP1_PARAM_AOCC  = 0x63636f61,
    DAP1_PARAM_ARBI  = 0x69627261,
    DAP1_PARAM_ARBL  = 0x6c627261,
    DAP1_PARAM_ARBH  = 0x68627261,
    DAP1_PARAM_ENDP  = 0x70646e65,
    DAP2_PARAM_IEBS = 0x73626569,
    DAP2_PARAM_ARON = 0x6e6f7261,
    DAP2_PARAM_ARDE = 0x65647261,
    DAP2_PARAM_ARRA = 0x61727261,
    DAP2_PARAM_ARBS = 0x73627261,
    DAP2_PARAM_ARTI = 0x69747261,
    DAP2_PARAM_VTON = 0x6e6f7476,
    DAP2_PARAM_DOM  = 0x006d6f64,
    DAP2_PARAM_DSB  = 0x00627364,
    DAP2_PARAM_GEBS = 0x73626567,
    DAP2_PARAM_BEON = 0x6e6f6562,
    DAP2_PARAM_BEB  = 0x00626562,
    DAP2_PARAM_BECF = 0x66636562,
    DAP2_PARAM_BEW  = 0x00776562,
    DAP2_PARAM_VBM  = 0x006d6276,
    DAP2_PARAM_VBSF = 0x66736276,
    DAP2_PARAM_VBOG = 0x676f6276,
    DAP2_PARAM_VBSG = 0x67736276,
    DAP2_PARAM_VBHG = 0x67686276,
    DAP2_PARAM_VBMF = 0x666d6276,
    DAP2_PARAM_AOBS = 0x73626f61,
    DAP2_PARAM_MSCE = 0x6563736d,
    DAP2_PARAM_MIEE = 0x6565696d,
    DAP2_PARAM_MDLE = 0x656c646d,
    DAP2_PARAM_MDEE = 0x6565646d,
    DAP2_PARAM_VCBS = 0x73626376,
    DAP2_PARAM_VNBS = 0x73626e76,
    DAP2_PARAM_DMC  = 0x00636d64,
    DAP2_PARAM_LATE = 0x6574616c,
    DAP2_PARAM_POON = 0x6e6f6f70,
    DAP2_PARAM_POBS = 0x73626f70,
    DAP2_PARAM_DFSA = 0x61736664,
    DAP2_PARAM_DHSA = 0x61736864,
    DAP2_PARAM_DHFM = 0x6d666864,
    DAP2_PARAM_BEXE = 0x65786562,
    DAP2_PARAM_BEXF = 0x66786562
};

static inline android::String8 dapParamName(DapParameterId param)
{
    unsigned p = static_cast<unsigned>(param);
    char name[] = {
        static_cast<char>(p >>  0),
        static_cast<char>(p >>  8),
        static_cast<char>(p >> 16),
        static_cast<char>(p >> 24),
        '\0',
    };
    return android::String8(name);
}

static inline android::String8 dapParamValue(const dap_param_value_t *values, int length)
{
    switch (length)
    {
    case 0:
        return android::String8("{no values}");
        break;
    case 1:
        return android::String8::format("%hd", *values);
        break;
    default:
        return android::String8::format("#%d[%hd ... %hd]", length, *values, values[length-1]);
    }
}

static inline android::String8 dapParamNameValue(DapParameterId param, const dap_param_value_t *values, int length)
{
    return dapParamName(param) + " = " + dapParamValue(values, length);
}

}
#endif//DOLBY_DAP_PARAMS_H_
