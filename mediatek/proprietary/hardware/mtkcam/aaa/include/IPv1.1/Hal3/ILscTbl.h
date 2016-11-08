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
#ifndef __I_LSC_TBL_H__
#define __I_LSC_TBL_H__

#include <aaa_types.h>
#include <ILscBuf.h>

namespace NSIspTuning
{
    class ILscTbl
    {
    public:
        typedef enum
        {
            HWTBL       = 0,
            GAIN_FIXED  = 1,
            GAIN_FLOAT  = 2
        } TBL_TYPE_T;

        typedef enum
        {
            BAYER_B     = 0,
            BAYER_GB    = 1,
            BAYER_GR    = 2,
            BAYER_R     = 3
        } TBL_BAYER_T;

        struct ConfigBlk
        {
            ConfigBlk();
            ConfigBlk(MINT32 i4ImgWd, MINT32 i4ImgHt, MINT32 i4GridX, MINT32 i4GridY);
            ConfigBlk(MINT32 i4BlkX, MINT32 i4BlkY, MINT32 i4BlkW, MINT32 i4BlkH, MINT32 i4BlkLastW, MINT32 i4BlkLastH);

            MINT32 i4BlkX;
            MINT32 i4BlkY;
            MINT32 i4BlkW;
            MINT32 i4BlkH;
            MINT32 i4BlkLastW;
            MINT32 i4BlkLastH;
        };

        struct Config
        {
            Config();
            Config(MINT32 _i4ImgWd, MINT32 _i4ImgHt, MINT32 _i4GridX, MINT32 _i4GridY);
            Config(const ConfigBlk& rCfgBlk);

            MINT32 i4ImgWd;
            MINT32 i4ImgHt;
            MINT32 i4GridX;
            MINT32 i4GridY;
            ConfigBlk rCfgBlk;
        };

        struct TransformCfg_T
        {
            TransformCfg_T(
                MUINT32 _u4ResizeW, MUINT32 _u4ResizeH, MUINT32 _u4GridX, MUINT32 _u4GridY,
                MUINT32 _u4X, MUINT32 _u4Y, MUINT32 _u4W, MUINT32 _u4H)
                    : u4ResizeW(_u4ResizeW)
                    , u4ResizeH(_u4ResizeH)
                    , u4GridX(_u4GridX)
                    , u4GridY(_u4GridY)
                    , u4X(_u4X)
                    , u4Y(_u4Y)
                    , u4W(_u4W)
                    , u4H(_u4H) {}
            TransformCfg_T()
                : u4ResizeW(0)
                , u4ResizeH(0)
                , u4GridX(0)
                , u4GridY(0)
                , u4X(0)
                , u4Y(0)
                , u4W(0)
                , u4H(0) {}
            MUINT32  u4ResizeW;
            MUINT32  u4ResizeH;
            MUINT32  u4GridX;
            MUINT32  u4GridY;
            MUINT32  u4X;
            MUINT32  u4Y;
            MUINT32  u4W;
            MUINT32  u4H;
        };

                                ILscTbl();
                                ILscTbl(TBL_TYPE_T eType);
                                ILscTbl(TBL_TYPE_T eType, MINT32 i4W, MINT32 i4H, MINT32 i4GridX, MINT32 i4GridY);
                                ILscTbl(ILscTbl const& other);
        virtual ILscTbl&        operator=(ILscTbl const& other);
        virtual                 ~ILscTbl();

        virtual TBL_TYPE_T      getType() const;
        virtual TBL_BAYER_T     getBayer() const;
        virtual const MVOID*    getData() const;
        virtual MUINT32         getSize() const;
        virtual const Config&   getConfig() const;
        virtual MVOID*          editData();
        virtual MBOOL           setData(const MVOID* src, MUINT32 size);
        virtual MBOOL           setConfig(const Config& rCfg);
        virtual MBOOL           setConfig(MINT32 i4W, MINT32 i4H, MINT32 i4GridX, MINT32 i4GridY);
        virtual MBOOL           setBayer(TBL_BAYER_T bayer);
        virtual MBOOL           dump(const char* filename) const;

        virtual MBOOL           cropOut(const TransformCfg_T& trfm, ILscTbl& output) const;
        virtual MBOOL           convert(ILscTbl& output) const;
        virtual MBOOL           getRaTbl(MUINT32 u4Ratio, ILscTbl& output) const;
        virtual MBOOL           toBuf(ILscBuf& buf) const;

        static MINT32           shadingAlign(const ILscTbl& golden, const ILscTbl& unit, const ILscTbl& input, ILscTbl& output);

        class LscTblImp;

    protected:
        LscTblImp*              m_pImp;
    };
};

#endif //__I_LSC_TBL_H__