#ifndef __BWC_SMI_ADAPTOR_H__
#define __BWC_SMI_ADAPTOR_H__


#include    "bandwidth_control.h"
#include    "mt_smi.h"


class BWC_SMI_Adaptor
{
public:
        MTK_SMI_BWC_SCEN map_bwc_profile_to_smi( BWC_PROFILE_TYPE bwc_profile );
        int smi_bw_ctrl_set_ext(
            BWC_PROFILE_TYPE profile_type,
            BWC_VCODEC_TYPE codec_type,
            bool bOn );
};


#endif
