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
 TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE
 *     FEES OR SERVICE CHARGE PAID BY BUYER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 *     THE TRANSACTION CONTEMPLATED HEREUNDER SHALL BE CONSTRUED IN ACCORDANCE WITH THE LAWS
 *     OF THE STATE OF CALIFORNIA, USA, EXCLUDING ITS CONFLICT OF LAWS PRINCIPLES.
 ************************************************************************************************/
#ifndef __I_LSC_BUF_H__
#define __I_LSC_BUF_H__

#include <aaa_types.h>

namespace NSIspTuning
{
class ILscBuf
{
public:
    struct Config
    {
        MINT32 i4ImgWd;
        MINT32 i4ImgHt;
        MINT32 i4BlkX;
        MINT32 i4BlkY;
        MINT32 i4BlkW;
        MINT32 i4BlkH;
        MINT32 i4BlkLastW;
        MINT32 i4BlkLastH;
    };

                            ILscBuf(MUINT32 sensorDev, MUINT32 u4Id, const char* strName);
    virtual                 ~ILscBuf();

    virtual ILscBuf::Config getConfig() const;
    virtual MUINT32         getPhyAddr() const;
    
    virtual MBOOL           setConfig(ILscBuf::Config rCfg);
    virtual const MUINT32*  getTable() const;
    virtual MBOOL           setTable(const void* data, MUINT32 u4Size);
    virtual MBOOL           validate();
    virtual MBOOL           showInfo() const;
    virtual MBOOL           dump(const char* filename) const;

protected:
                            class LscBufImp;
    LscBufImp*              m_pImp;
};
};
#endif //__LSC_UTIL_H__