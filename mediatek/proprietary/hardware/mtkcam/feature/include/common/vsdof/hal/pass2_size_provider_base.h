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
#ifndef _PASS2_SIZE_PROVIDER_BASE_H_
#define _PASS2_SIZE_PROVIDER_BASE_H_

#include "stereo_common.h"
#include <utils/threads.h>
#include <cutils/atomic.h>
#include <IHalSensor.h>

using android::Mutex;
using namespace StereoHAL;

enum ENUM_PASS2_ROUND
{
    PASS2A,
    PASS2A_2,
    PASS2A_3,
    PASS2A_P,
    PASS2A_P_2,
    PASS2A_P_3
};

//================================================
//  For Pass 2 Output
//================================================
struct Pass2SizeInfo
{
    StereoArea areaWDMA;
    MSize szWROT;
    MSize szIMG2O;
    MSize szIMG3O;
    StereoArea areaFEO;

    Pass2SizeInfo() {
        ::memset(this, 0, sizeof(Pass2SizeInfo));
    }

    ~Pass2SizeInfo() {}

    Pass2SizeInfo(StereoArea wdma, MSize wrot, MSize img2o, MSize img3o, StereoArea feArea) {
        areaWDMA = wdma;
        szWROT = wrot;
        szIMG2O = img2o;
        szIMG3O = img3o;
        areaFEO = feArea;
    }
};

const Pass2SizeInfo PASS2_SIZE_ZERO;

template <class T>
class Pass2SizeProviderBase
{
private:
    static Mutex       mLock;
public:
    static T *instance() {
        Mutex::Autolock lock(mLock);

        static T *_instance = NULL;
        if(NULL == _instance) {
            _instance = new T();
        }

        return _instance;
    }

    virtual Pass2SizeInfo sizeInfo( ENUM_STEREO_SCENARIO eScenario ) const {
        return Pass2SizeInfo( getWDMAArea(eScenario),       //WDMA
                              getWROTSize(eScenario),       //WROT
                              getIMG2OSize(eScenario),      //IMG2O
                              getIMG3OSize(eScenario),      //IMG3O
                              getFEOInputArea(eScenario)    //FEO Input
                            );
    }

    virtual StereoArea getWDMAArea( ENUM_STEREO_SCENARIO eScenario ) const { return STEREO_AREA_ZERO; }
    virtual MSize getWROTSize( ENUM_STEREO_SCENARIO eScenario ) const { return MSIZE_ZERO; }
    virtual MSize getIMG2OSize( ENUM_STEREO_SCENARIO eScenario ) const { return MSIZE_ZERO; }
    virtual MSize getIMG3OSize( ENUM_STEREO_SCENARIO eScenario ) const { return MSIZE_ZERO; }
    virtual StereoArea getFEOInputArea( ENUM_STEREO_SCENARIO eScenario ) const { return STEREO_AREA_ZERO; }
protected:
    Pass2SizeProviderBase() {}

    virtual ~Pass2SizeProviderBase() {}
};

template <class T>
Mutex Pass2SizeProviderBase<T>::mLock;

#endif