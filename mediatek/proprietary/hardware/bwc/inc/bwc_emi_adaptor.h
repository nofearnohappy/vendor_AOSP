#ifndef __BWC_EMI_ADAPTOR_H__
#define __BWC_EMI_ADAPTOR_H__

#include    "bandwidth_control.h"


class BWC_EMI_Adaptor
{
public:
        int emi_ctrl_str_generate(
            BWC_PROFILE_TYPE profile_type,
            BWC_VCODEC_TYPE codec_type,
            bool bOn, char* out_str );
};


#endif
