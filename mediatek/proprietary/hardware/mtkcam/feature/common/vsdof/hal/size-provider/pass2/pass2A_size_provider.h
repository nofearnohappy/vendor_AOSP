/********************************************************************************************
 *     LEGAL DISCLAIMER
 *
 *     (Header of MediaTek Software/Firmware Release or Documentation)
 *
 *     BY OPENING OR USING THIS FILE, BUYER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 *     THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE") RECEIVED
 *     FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO BUYER ON AN "AS-IS" BASIS
 *     ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES, EXPRESS OR IMPLIED,
 *     INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR
 *     A PARTICULAR PURPOSE OR NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY
 *     WHATSOEVER WITH RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 *     INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND BUYER AGREES TO LOOK
 *     ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. MEDIATEK SHALL ALSO
 *     NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE RELEASES MADE TO BUYER'S SPECIFICATION
 *     OR TO CONFORM TO A PARTICULAR STANDARD OR OPEN FORUM.
 *
 *     BUYER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND CUMULATIVE LIABILITY WITH
 *     RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION,
 *     TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE
 *     FEES OR SERVICE CHARGE PAID BY BUYER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 *     THE TRANSACTION CONTEMPLATED HEREUNDER SHALL BE CONSTRUED IN ACCORDANCE WITH THE LAWS
 *     OF THE STATE OF CALIFORNIA, USA, EXCLUDING ITS CONFLICT OF LAWS PRINCIPLES.
 ************************************************************************************************/
#ifndef _PASS2A_SIZE_PROVIDER_BASE_H_
#define _PASS2A_SIZE_PROVIDER_BASE_H_

#include <vsdof/hal/pass2_size_provider_base.h>

class Pass2A_SizeProvider: public Pass2SizeProviderBase<Pass2A_SizeProvider>
{
public:
    friend class Pass2SizeProviderBase<Pass2A_SizeProvider>;

    virtual StereoArea getWDMAArea( ENUM_STEREO_SCENARIO eScenario ) const {
        switch(eScenario) {
            case eSTEREO_SCENARIO_PREVIEW:
            case eSTEREO_SCENARIO_RECORD:
                return MSize(1920, 1080);
                break;
            case eSTEREO_SCENARIO_CAPTURE:
                return MSize(3072, 1728);
                break;
            default:
                break;
        }

        return MSIZE_ZERO;
    }

    virtual MSize getWROTSize( ENUM_STEREO_SCENARIO eScenario ) const  {
        return StereoArea(MSize(960, 544)).rotatedByModule().size;
    }

    virtual StereoArea getFEOInputArea( ENUM_STEREO_SCENARIO eScenario ) const {
        return StereoArea(1600, 900);
    }

    virtual MSize getIMG2OSize( ENUM_STEREO_SCENARIO eScenario ) const {
        return MSize(640, 360); //For FD
    }
protected:
    Pass2A_SizeProvider() {}
    virtual ~Pass2A_SizeProvider() {}
private:

};

#endif