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
#define LOG_TAG "lsc_tbl"
#ifndef ENABLE_MY_LOG
#define ENABLE_MY_LOG           (1)
#define GLOBAL_ENABLE_MY_LOG    (1)
#endif

#include <ILscTbl.h>
#include <LscUtil.h>
#include <vector>
#include <liblsctrans/ShadingTblTransform.h>

#define MAX(a,b)  ((a) < (b) ? (b) : (a))
#define MIN(a,b)  ((a) < (b) ? (a) : (b))
#define ABS(a)    ((a) > 0 ? (a) : -(a))

using namespace NSIspTuning;
using namespace std;

/*******************************************************************************
 * ILscTbl::ConfigBlk
 * ILscTbl::Config
 *******************************************************************************/
ILscTbl::ConfigBlk::
ConfigBlk()
    : i4BlkX(0), i4BlkY(0), i4BlkW(0), i4BlkH(0), i4BlkLastW(0), i4BlkLastH(0)
{}

ILscTbl::ConfigBlk::
ConfigBlk(MINT32 i4ImgWd, MINT32 i4ImgHt, MINT32 i4GridX, MINT32 i4GridY)
{
    i4BlkX   = i4GridX - 2;
    i4BlkY   = i4GridY - 2;    
    i4BlkW  = (i4ImgWd)/(2*(i4BlkX+1));
    i4BlkH  = (i4ImgHt)/(2*(i4BlkY+1));
    i4BlkLastW = i4ImgWd/2 - (i4BlkX*i4BlkW);
    i4BlkLastH = i4ImgHt/2 - (i4BlkY*i4BlkH);
}

ILscTbl::ConfigBlk::
ConfigBlk(MINT32 _i4BlkX, MINT32 _i4BlkY, MINT32 _i4BlkW, MINT32 _i4BlkH, MINT32 _i4BlkLastW, MINT32 _i4BlkLastH)
    : i4BlkX(_i4BlkX), i4BlkY(_i4BlkY), i4BlkW(_i4BlkW), i4BlkH(_i4BlkH), i4BlkLastW(_i4BlkLastW), i4BlkLastH(_i4BlkLastH)
{}

ILscTbl::Config::
Config()
    : i4ImgWd(0), i4ImgHt(0), i4GridX(0), i4GridY(0), rCfgBlk()
{}

ILscTbl::Config::
Config(MINT32 _i4ImgWd, MINT32 _i4ImgHt, MINT32 _i4GridX, MINT32 _i4GridY)
    : i4ImgWd(_i4ImgWd), i4ImgHt(_i4ImgHt), i4GridX(_i4GridX), i4GridY(_i4GridY)
{
    if (i4GridX > 17 || i4GridX <= 2 || i4GridY > 17 || i4GridY <= 2)
    {
        // assert
        LSC_ERR("XGrid(%d), YGrids(%d)", i4GridX, i4GridY);
        i4GridX = 17;
        i4GridY = 17;
    }
    rCfgBlk = ConfigBlk(i4ImgWd, i4ImgHt, i4GridX, i4GridY);
}

ILscTbl::Config::
Config(const ILscTbl::ConfigBlk& _rCfgBlk)
{
    i4ImgWd = (_rCfgBlk.i4BlkW * _rCfgBlk.i4BlkX + _rCfgBlk.i4BlkLastW) * 2;
    i4ImgHt = (_rCfgBlk.i4BlkH * _rCfgBlk.i4BlkY + _rCfgBlk.i4BlkLastH) * 2;
    i4GridX = _rCfgBlk.i4BlkX + 2;
    i4GridY = _rCfgBlk.i4BlkY + 2;
    if (i4GridX > 17 || i4GridX <= 2 || i4GridY > 17 || i4GridY <= 2)
    {
        // assert
        LSC_ERR("XGrid(%d), YGrids(%d)", i4GridX, i4GridY);
        i4GridX = 17;
        i4GridY = 17;
    }
    rCfgBlk = ConfigBlk(i4ImgWd, i4ImgHt, i4GridX, i4GridY);
}

static MINT32
ConvertToHwTbl(float *p_pgn_float, unsigned int *p_lsc_tbl, int grid_x, int grid_y, int RawImgW, int RawImgH)
{
    float* afWorkingBuf = new float[BUFFERSIZE];
    if (!afWorkingBuf)
    {
        LSC_ERR("Allocate afWorkingBuf Fail");
        return -1;
    }

    MUINT32 u4RetLSCHwTbl =
        LscGaintoHWTbl(p_pgn_float,
                       p_lsc_tbl,
                       grid_x,
                       grid_y,
                       RawImgW,
                       RawImgH,
                       (void*)afWorkingBuf,
                       BUFFERSIZE);

    delete [] afWorkingBuf;

    return u4RetLSCHwTbl;
}

/*******************************************************************************
 * LscRatio
 *******************************************************************************/
class LscRatio
{
public:
    LscRatio();
    virtual ~LscRatio();

    virtual MBOOL updateCfg(const ILscTbl::Config& rCfg);
    virtual MBOOL genHwTbl(MUINT32 ratio, const MUINT32* pSrc, MUINT32* pDest);
    virtual MBOOL genGainTbl(MUINT32 ratio, const MUINT32* pSrc, MUINT32* pDest);

private:
    // ra
    MTKLscUtil*                     m_pLscUtilInterface;
    LSC_PARAM_T                     m_rRaLscConfig;
    LSC_RA_STRUCT                   m_rRaConfig;
};

LscRatio::
LscRatio()
    : m_pLscUtilInterface(NULL)
{
    m_pLscUtilInterface         = MTKLscUtil::createInstance();

    m_rRaConfig.working_buf     = (int*)new MUINT8[LSC_RA_BUFFER_SIZE];
    m_rRaConfig.in_data_type    = SHADING_TYPE_COEFF;
    m_rRaConfig.out_data_type   = SHADING_TYPE_COEFF; // TSF 4x frame, use coef table
    m_rRaConfig.pix_id          = (int)BAYER_B;
    m_rRaConfig.ra              = 32;
    //m_rRaConfig.lsc_config      = m_rRaLscConfig;

    if (m_pLscUtilInterface)
    {
        ::memset((MUINT8*)m_rRaConfig.working_buf, 0, LSC_RA_BUFFER_SIZE);
        m_pLscUtilInterface->LscRaSwInit((void*)m_rRaConfig.working_buf);
    }
    else
    {
        LSC_ERR("Fail to create MTKLscUtil!");
    }
}

LscRatio::
~LscRatio()
{
    if (m_rRaConfig.working_buf)
    {
        delete [] (MUINT8*) m_rRaConfig.working_buf;
        m_rRaConfig.working_buf = NULL;
    }

    if (m_pLscUtilInterface)
    {
        m_pLscUtilInterface->destroyInstance(m_pLscUtilInterface);
        m_pLscUtilInterface = NULL;
    }
}

MBOOL
LscRatio::
updateCfg(const ILscTbl::Config& rCfg)
{
    m_rRaLscConfig.raw_wd        = rCfg.i4ImgWd;
    m_rRaLscConfig.raw_ht        = rCfg.i4ImgHt;
    m_rRaLscConfig.crop_ini_x    = 0;
    m_rRaLscConfig.crop_ini_y    = 0;
    m_rRaLscConfig.block_wd      = rCfg.rCfgBlk.i4BlkW;
    m_rRaLscConfig.block_ht      = rCfg.rCfgBlk.i4BlkH;
    m_rRaLscConfig.x_grid_num    = rCfg.i4GridX;
    m_rRaLscConfig.y_grid_num    = rCfg.i4GridY;
    m_rRaLscConfig.block_wd_last = rCfg.rCfgBlk.i4BlkLastW;
    m_rRaLscConfig.block_ht_last = rCfg.rCfgBlk.i4BlkLastH;

    m_rRaConfig.lsc_config       = m_rRaLscConfig;
    return MTRUE;
}

MBOOL
LscRatio::
genHwTbl(MUINT32 ratio, const MUINT32* pSrc, MUINT32* pDest)
{
    MBOOL fgRet = MTRUE;
    LSC_RESULT ra_ret;

    if (m_pLscUtilInterface == NULL || m_rRaConfig.working_buf == NULL)
    {
        LSC_ERR("Null Util(%p, %p)", m_pLscUtilInterface, m_rRaConfig.working_buf);
        goto lbExit;
    }

    if (pSrc == NULL || pDest == NULL)
    {
        LSC_ERR("Null pointer(%p, %p)", pSrc, pDest);
        goto lbExit;
    }

    if (ratio > 32)
    {
        LSC_ERR("ratio(%d)", ratio);
        goto lbExit;
    }   

    m_rRaConfig.in_data_type    = SHADING_TYPE_COEFF;
    m_rRaConfig.out_data_type   = SHADING_TYPE_COEFF;
    m_rRaConfig.in_tbl          = (int*)pSrc;
    m_rRaConfig.out_tbl         = (int*)pDest;
    m_rRaConfig.ra              = ratio;

    if (ratio == 32)
    {
        MUINT32 u4Size = (m_rRaLscConfig.x_grid_num - 1) * (m_rRaLscConfig.y_grid_num - 1) * 4 * 4 * 4;
        u4Size = MIN(u4Size, 65536);
        ::memcpy(pDest, pSrc, u4Size);
    }
    else
    {
        // do ratio
        ra_ret = m_pLscUtilInterface->LscRaSwMain((void*)&m_rRaConfig);
    }
lbExit:
    return fgRet;
}

MBOOL
LscRatio::
genGainTbl(MUINT32 ratio, const MUINT32* pSrc, MUINT32* pDest)
{
    MBOOL fgRet = MTRUE;
    LSC_RESULT ra_ret;

    if (m_pLscUtilInterface == NULL || m_rRaConfig.working_buf == NULL)
    {
        LSC_ERR("Null Util(%p, %p)", m_pLscUtilInterface, m_rRaConfig.working_buf);
        goto lbExit;
    }

    if (pSrc == NULL || pDest == NULL)
    {
        LSC_ERR("Null pointer(%p, %p)", pSrc, pDest);
        goto lbExit;
    }

    if (ratio > 32)
    {
        LSC_ERR("ratio(%d)", ratio);
        goto lbExit;
    }

    m_rRaConfig.in_data_type    = SHADING_TYPE_COEFF;
    m_rRaConfig.out_data_type   = SHADING_TYPE_GAIN;
    m_rRaConfig.in_tbl          = (int*)pSrc;
    m_rRaConfig.out_tbl         = (int*)pDest;
    m_rRaConfig.ra              = ratio;
    // do ratio
    ra_ret = m_pLscUtilInterface->LscRaSwMain((void*)&m_rRaConfig);

lbExit:
    return fgRet;
}

/*******************************************************************************
 * ILscTbl::LscTblImp
 *******************************************************************************/
class ILscTbl::LscTblImp
{
public:
                                    LscTblImp(ILscTbl::TBL_TYPE_T eType);
                                    LscTblImp(LscTblImp const& other);
    LscTblImp&                      operator=(LscTblImp const& other);

                                    LscTblImp(ILscTbl::TBL_TYPE_T eType, MINT32 i4W, MINT32 i4H, MINT32 i4GridX, MINT32 i4GridY);
    virtual                         ~LscTblImp();
    virtual ILscTbl::TBL_TYPE_T     getType() const {return m_eType;}
    virtual ILscTbl::TBL_BAYER_T    getBayer() const {return m_eBayer;}
    virtual const MVOID*            getData() const;
    virtual MUINT32                 getSize() const;
    virtual const ILscTbl::Config&  getConfig() const {return m_rCfg;}
    virtual MVOID*                  editData();
    virtual MBOOL                   setData(const MVOID* src, MUINT32 size);
    virtual MBOOL                   setConfig(MINT32 i4W, MINT32 i4H, MINT32 i4GridX, MINT32 i4GridY);
    virtual MBOOL                   setBayer(ILscTbl::TBL_BAYER_T bayer);
    virtual MBOOL                   dump(const char* filename) const;

    virtual MBOOL                   cropOut(const TransformCfg_T& trfm, ILscTbl& output) const;
    virtual MBOOL                   convert(ILscTbl& output) const;
    virtual MBOOL                   getRaTbl(MUINT32 u4Ratio, ILscTbl& output) const;
    virtual MBOOL                   toBuf(ILscBuf& buf) const;

protected:
    ILscTbl::TBL_TYPE_T             m_eType;
    ILscTbl::TBL_BAYER_T            m_eBayer;
    ILscTbl::Config                 m_rCfg;
    MUINT32                         m_u4Size;
    vector<MUINT8>                  m_vecData;
};

ILscTbl::LscTblImp::
LscTblImp(ILscTbl::TBL_TYPE_T eType)
    : m_eType(eType)
    , m_eBayer(ILscTbl::BAYER_B)
    , m_rCfg()
    , m_u4Size(0)
{
}

ILscTbl::LscTblImp::
LscTblImp(ILscTbl::TBL_TYPE_T eType, MINT32 i4W, MINT32 i4H, MINT32 i4GridX, MINT32 i4GridY)
    : m_eType(eType)
    , m_eBayer(ILscTbl::BAYER_B)
    , m_rCfg()
    , m_u4Size(0)
{
    m_vecData.clear();
    if (!setConfig(i4W, i4H, i4GridX, i4GridY))
    {
        LSC_ERR("Force grid as 17x17 due error config");
    }
}

ILscTbl::LscTblImp::
LscTblImp(ILscTbl::LscTblImp const& other)
    : m_eType(other.m_eType)
    , m_eBayer(other.m_eBayer)
    , m_rCfg(other.m_rCfg)
    , m_u4Size(other.m_u4Size)
    , m_vecData(other.m_vecData)
{
}

ILscTbl::LscTblImp&
ILscTbl::LscTblImp::
operator=(ILscTbl::LscTblImp const& other)
{
    if (this != &other)
    {
        m_eType = other.m_eType;
        m_eBayer = other.m_eBayer;
        m_rCfg = other.m_rCfg;
        m_u4Size = other.m_u4Size;
        m_vecData = other.m_vecData;
    }
    else
    {
        LSC_LOG("this(%p) == other(%p)", this, &other);
    }

    return *this;
}

ILscTbl::LscTblImp::
~LscTblImp()
{
}

MBOOL
ILscTbl::LscTblImp::
setConfig(MINT32 i4W, MINT32 i4H, MINT32 i4GridX, MINT32 i4GridY)
{
    MBOOL fgRet = MTRUE;

    if (i4GridX > 17 || i4GridX <= 2 || i4GridY > 17 || i4GridY <= 2)
    {
        // assert
        LSC_ERR("XGrid(%d), YGrids(%d)", i4GridX, i4GridY);
        i4GridX = 17;
        i4GridY = 17;
        fgRet = MFALSE;
    }

    // set config
    m_rCfg = Config(i4W, i4H, i4GridX, i4GridY);

    MUINT32 u4Size = 0;
    switch (m_eType)
    {
    default:
    case ILscTbl::HWTBL:
        u4Size = (m_rCfg.rCfgBlk.i4BlkX+1) * (m_rCfg.rCfgBlk.i4BlkY+1) * 4 * 4 * 4;
        break;
    case ILscTbl::GAIN_FIXED:
        u4Size = i4GridX * i4GridY * 4 * 2;
        break;
    case ILscTbl::GAIN_FLOAT:
        u4Size = i4GridX * i4GridY * 4 * sizeof(MFLOAT);
        break;
    }

    if (m_u4Size < u4Size)
    {
        m_u4Size = u4Size;
        m_vecData.resize(u4Size);
    }

    return fgRet;
}

MBOOL
ILscTbl::LscTblImp::
setBayer(ILscTbl::TBL_BAYER_T bayer)
{
    m_eBayer = bayer;
    return MTRUE;
}

MBOOL
ILscTbl::LscTblImp::
setData(const MVOID* src, MUINT32 size)
{
    if (m_u4Size < size)
    {
        LSC_ERR("input data size(%d) is larger than this size(%d), please call setConfig to resize", size, m_u4Size);
        return MFALSE;
    }

    MBOOL fgRet = MTRUE;
    MUINT8* dest = m_vecData.data();
    if (dest && src)
    {
        ::memcpy(dest, src, size);
    }
    else
    {
        LSC_ERR("NULL pointer dest(%p), src(%p)", dest, src);
        fgRet = MFALSE;
    }

    return fgRet;
}

const MVOID*
ILscTbl::LscTblImp::
getData() const
{
    return m_vecData.data();
}

MVOID*
ILscTbl::LscTblImp::
editData()
{
    return m_vecData.data();
}

MUINT32
ILscTbl::LscTblImp::
getSize() const
{
    return m_u4Size;
}

MBOOL
ILscTbl::LscTblImp::
dump(const char* filename) const
{
    MINT32 i = m_u4Size / 4;
    FILE* fpdebug;
    fpdebug = fopen(filename, "w");
    if ( fpdebug == NULL )
    {
        LSC_ERR("Can't open: %s", filename);
        return MFALSE;
    }

    const MUINT32* pData = static_cast<const MUINT32*>(getData());
    while (i >= 4)
    {
        MUINT32 a = *pData++;
        MUINT32 b = *pData++;
        MUINT32 c = *pData++;
        MUINT32 d = *pData++;
        fprintf(fpdebug, "0x%08x    0x%08x    0x%08x    0x%08x\n", a, b, c, d);
        i -= 4;
    }

    while (i)
    {
        fprintf(fpdebug, "0x%08x    ", *pData++);
        i --;
    }
    
    fclose(fpdebug);
    return MTRUE;
}

MBOOL
ILscTbl::LscTblImp::
cropOut(const TransformCfg_T& trfm, ILscTbl& output) const
{
    MBOOL fgRet = MTRUE;
    LSC_RESULT result = S_LSC_CONVERT_OK;
    SHADIND_TRFM_CONF rTrfm;
    MUINT8* gWorkinBuffer = new MUINT8[SHADIND_FUNC_WORKING_BUFFER_SIZE];
    if (!gWorkinBuffer)
    {
        LSC_ERR("Fail to allocate gWorkinBuffer");
        fgRet = MFALSE;
    }
    else
    {
        LSC_LOG("gWorkinBuffer(%p)", gWorkinBuffer);
    }

    vector<MUINT8> tmpInput = m_vecData;
    if (!output.setConfig(trfm.u4W, trfm.u4H, trfm.u4GridX, trfm.u4GridY))
    {
        LSC_ERR("resize(%d,%d), grid(%d,%d), crop(%d,%d,%d,%d)",
            trfm.u4ResizeW, trfm.u4ResizeH, trfm.u4GridX, trfm.u4GridY, trfm.u4X, trfm.u4Y, trfm.u4W, trfm.u4H);
        goto lbExit;
    }
    output.setBayer(m_eBayer);

    rTrfm.working_buff_addr     = gWorkinBuffer;
    rTrfm.working_buff_size     = SHADIND_FUNC_WORKING_BUFFER_SIZE;
    rTrfm.afn                   = SHADING_AFN_R0D;

    rTrfm.input.img_width       = m_rCfg.i4ImgWd;
    rTrfm.input.img_height      = m_rCfg.i4ImgHt;
    rTrfm.input.offset_x        = 0;
    rTrfm.input.offset_y        = 0;
    rTrfm.input.crop_width      = m_rCfg.i4ImgWd;
    rTrfm.input.crop_height     = m_rCfg.i4ImgHt;
    rTrfm.input.bayer           = (BAYER_ID_T)m_eBayer;
    rTrfm.input.grid_x          = m_rCfg.i4GridX;           // Input gain
    rTrfm.input.grid_y          = m_rCfg.i4GridY;           // Input gain
    rTrfm.input.lwidth          = 0;
    rTrfm.input.lheight         = 0;
    rTrfm.input.ratio_idx       = 0;
    rTrfm.input.grgb_same       = SHADING_GRGB_SAME_NO;
    rTrfm.input.table           = (MUINT32*)tmpInput.data(); // Input gain

    switch (m_eType)
    {
    case ILscTbl::HWTBL:
        rTrfm.input.data_type  = SHADING_TYPE_COEFF;       // coef
        break;
    default:
    case ILscTbl::GAIN_FIXED:
        rTrfm.input.data_type  = SHADING_TYPE_GAIN;        // gain
        break;
    }

    rTrfm.output.img_width      = trfm.u4ResizeW;           // output width, resize from input width
    rTrfm.output.img_height     = trfm.u4ResizeH;           // output height, resize from input height
    rTrfm.output.offset_x       = trfm.u4X;                 // crop
    rTrfm.output.offset_y       = trfm.u4Y;                 // crop
    rTrfm.output.crop_width     = trfm.u4W;                 // crop
    rTrfm.output.crop_height    = trfm.u4H;                 // crop
    rTrfm.output.bayer          = (BAYER_ID_T)output.getBayer();
    rTrfm.output.grid_x         = trfm.u4GridX;             // output gain (alwasy 16x16)
    rTrfm.output.grid_y         = trfm.u4GridY;             // output gain
    rTrfm.output.lwidth         = 0;
    rTrfm.output.lheight        = 0;
    rTrfm.output.ratio_idx      = 0;
    rTrfm.output.grgb_same      = SHADING_GRGB_SAME_NO;
    rTrfm.output.table          = (MUINT32*)output.editData(); // output gain

    switch (output.getType())
    {
    case ILscTbl::HWTBL:
        rTrfm.output.data_type  = SHADING_TYPE_COEFF;       // coef
        break;
    default:
    case ILscTbl::GAIN_FIXED:
        rTrfm.output.data_type  = SHADING_TYPE_GAIN;        // gain
        break;
    }
    //_LogShadingSpec("input",  rTrfm.input);
    //_LogShadingSpec("output", rTrfm.output);

    result = shading_transform(rTrfm);
    if (S_LSC_CONVERT_OK != result)
    {
        LSC_ERR("Transform Error(%d)", result);
        fgRet = MFALSE;
    }
    else
    {
        LSC_LOG("Transform done.");
    }

lbExit:
    delete [] gWorkinBuffer;
    return fgRet;
}

MBOOL
ILscTbl::LscTblImp::
convert(ILscTbl& output) const
{
    if (ILscTbl::GAIN_FLOAT != m_eType && ILscTbl::GAIN_FLOAT != output.getType())
    {
        return cropOut(TransformCfg_T(m_rCfg.i4ImgWd, m_rCfg.i4ImgHt, m_rCfg.i4GridX, m_rCfg.i4GridY, 0, 0, m_rCfg.i4ImgWd, m_rCfg.i4ImgHt), output);
    }
    else
    {
        if (ILscTbl::GAIN_FLOAT == output.getType())
        {
            switch (m_eType)
            {
            case ILscTbl::HWTBL:
                {
                    // HWTBL -> FLOAT
                    ILscTbl tmp(ILscTbl::GAIN_FIXED);
                    if (cropOut(TransformCfg_T(m_rCfg.i4ImgWd, m_rCfg.i4ImgHt, m_rCfg.i4GridX, m_rCfg.i4GridY, 0, 0, m_rCfg.i4ImgWd, m_rCfg.i4ImgHt), tmp))
                    {
                        return tmp.convert(output);
                    }
                }
                return MFALSE;
            case ILscTbl::GAIN_FIXED:
                {
                    // FIXED -> FLOAT
                    output.setConfig(m_rCfg);
                    MINT32 i4Count = m_u4Size / sizeof(MUINT16);
                    const MUINT16* pSrc = static_cast<const MUINT16*>(getData());
                    MFLOAT* pDest = static_cast<MFLOAT*>(output.editData());
                    while (i4Count--)
                    {
                        *pDest++ = (MFLOAT) *pSrc++ / 8192.0f;
                    }
                }
                return MTRUE;
            default: // impossible case
            case ILscTbl::GAIN_FLOAT:
                MY_ERR("impossible case");
                return MFALSE; 
            }
        }
        else // (ILscTbl::GAIN_FLOAT == m_eType)
        {
            MY_ERR("impossible case");
            return MFALSE;
        }
    }
}

MBOOL
ILscTbl::LscTblImp::
getRaTbl(MUINT32 u4Ratio, ILscTbl& output) const
{
    MBOOL fgRet = MFALSE;

    if (u4Ratio > 32)
    {
        LSC_ERR("ratio(%d)", u4Ratio);
        return MFALSE;
    }

    switch (m_eType)
    {
    case ILscTbl::HWTBL: // input type
        {
            TBL_TYPE_T eType = output.getType();
            if (ILscTbl::GAIN_FLOAT != eType)
            {
                LscRatio rLscRatio;
                rLscRatio.updateCfg(m_rCfg);
                output.setBayer(m_eBayer);
                output.setConfig(m_rCfg);
                if (ILscTbl::HWTBL == eType)
                {
                    fgRet = rLscRatio.genHwTbl(u4Ratio, static_cast<const MUINT32*>(getData()), static_cast<MUINT32*>(output.editData()));
                }
                else
                {
                    fgRet = rLscRatio.genGainTbl(u4Ratio, static_cast<const MUINT32*>(getData()), static_cast<MUINT32*>(output.editData()));
                }
            }
            else
            {
                // ILscTbl::GAIN_FLOAT
            }
        }
        break;
    default:
        LSC_ERR("Not support");
        break;
    }

    return fgRet;
}

MBOOL
ILscTbl::LscTblImp::
toBuf(ILscBuf& buf) const
{
    if (ILscTbl::HWTBL == m_eType)
    {
        ILscBuf::Config rBufCfg;
        rBufCfg.i4ImgWd = m_rCfg.i4ImgWd;
        rBufCfg.i4ImgHt = m_rCfg.i4ImgHt;
        rBufCfg.i4BlkX  = m_rCfg.rCfgBlk.i4BlkX;
        rBufCfg.i4BlkY  = m_rCfg.rCfgBlk.i4BlkY;
        rBufCfg.i4BlkW  = m_rCfg.rCfgBlk.i4BlkW;
        rBufCfg.i4BlkH  = m_rCfg.rCfgBlk.i4BlkH;
        rBufCfg.i4BlkLastW = m_rCfg.rCfgBlk.i4BlkLastW;
        rBufCfg.i4BlkLastH = m_rCfg.rCfgBlk.i4BlkLastH;
        if (buf.setConfig(rBufCfg))
        {
            MBOOL fgResult = buf.setTable(getData(), getSize());
            if (!fgResult)
            {
                LSC_ERR("Fail to set table");
            }
            return fgResult;
        }
        else
        {
            LSC_ERR("Fail to set config");
            return MFALSE;
        }
    }
    else
    {
        ILscTbl tmp(ILscTbl::HWTBL);
        if (convert(tmp))
        {
            return tmp.toBuf(buf);
        }
        else
        {
            LSC_ERR("Fail to convert");
            return MFALSE;
        }
    }
    return MFALSE;
}
/*******************************************************************************
 * LscTblFloatImp
 *******************************************************************************/
class LscTblFloatImp : public ILscTbl::LscTblImp
{
public:
                                    LscTblFloatImp();
                                    LscTblFloatImp(ILscTbl::LscTblImp const& other);
                                    LscTblFloatImp(MINT32 i4W, MINT32 i4H, MINT32 i4GridX, MINT32 i4GridY);
    virtual MBOOL                   dump(const char* filename) const;
    
    virtual MBOOL                   cropOut(const ILscTbl::TransformCfg_T& trfm, ILscTbl& output) const;
    virtual MBOOL                   convert(ILscTbl& output) const;
};

LscTblFloatImp::
LscTblFloatImp()
    : ILscTbl::LscTblImp(ILscTbl::GAIN_FLOAT)
{}

LscTblFloatImp::
LscTblFloatImp(MINT32 i4W, MINT32 i4H, MINT32 i4GridX, MINT32 i4GridY)
    : ILscTbl::LscTblImp(ILscTbl::GAIN_FLOAT, i4W, i4H, i4GridX, i4GridY)
{}

LscTblFloatImp::
LscTblFloatImp(ILscTbl::LscTblImp const& other)
    : ILscTbl::LscTblImp(other)
{}

MBOOL
LscTblFloatImp::
dump(const char* filename) const
{
    MINT32 i = m_u4Size / sizeof(MFLOAT);
    FILE* fpdebug;
    fpdebug = fopen(filename, "w");
    if ( fpdebug == NULL )
    {
        LSC_ERR("Can't open: %s", filename);
        return MFALSE;
    }

    const MFLOAT* pData = static_cast<const MFLOAT*>(getData());
    while (i >= 4)
    {
        MFLOAT a = *pData++;
        MFLOAT b = *pData++;
        MFLOAT c = *pData++;
        MFLOAT d = *pData++;
        fprintf(fpdebug, "%3.6f    %3.6f    %3.6f    %3.6f\n", a, b, c, d);
        i -= 4;
    }

    while (i)
    {
        fprintf(fpdebug, "%3.6f    ", *pData++);
        i --;
    }
    
    fclose(fpdebug);
    return MTRUE;
}

MBOOL
LscTblFloatImp::
convert(ILscTbl& output) const
{
    output.setConfig(m_rCfg);
    switch (output.getType())
    {
    case ILscTbl::HWTBL:
        {
            LSC_LOG("F2H");
            if (S_LSC_CONVERT_OK != 
                    ConvertToHwTbl(
                        (float*)m_vecData.data(),
                        (unsigned int*)output.editData(),
                        m_rCfg.i4GridX,
                        m_rCfg.i4GridY,
                        m_rCfg.i4ImgWd,
                        m_rCfg.i4ImgHt))
            {
                return MFALSE;
            }
            return MTRUE;
        }
        break;
    case ILscTbl::GAIN_FIXED:
        {
            LSC_LOG("float to fixed");
            MINT32 i4Count = m_u4Size / sizeof(MFLOAT);
            const MFLOAT* pSrc = static_cast<const MFLOAT*>(getData());
            MUINT16* pDest = static_cast<MUINT16*>(output.editData());
            while (i4Count--)
            {
                MFLOAT fVal = (*pSrc++);
                *pDest++ = MIN(65535, (fVal * 8192.0f + 0.5f));
            }
        }
        return MTRUE;
    default:
    case ILscTbl::GAIN_FLOAT:
        output.setData(getData(), m_u4Size);
        return MTRUE;
    }

    return MTRUE;
}

MBOOL
LscTblFloatImp::
cropOut(const ILscTbl::TransformCfg_T& trfm, ILscTbl& output) const
{
    return MFALSE;
}

/*******************************************************************************
 * ILscTbl
 *******************************************************************************/
ILscTbl::
ILscTbl()
    : m_pImp(new ILscTbl::LscTblImp(ILscTbl::HWTBL))
{
}

ILscTbl::
ILscTbl(ILscTbl::TBL_TYPE_T eType)
{
    if (ILscTbl::GAIN_FLOAT == eType)
    {
        m_pImp = new LscTblFloatImp();
    }
    else
    {
        m_pImp = new ILscTbl::LscTblImp(eType);
    }
}

ILscTbl::
ILscTbl(ILscTbl::TBL_TYPE_T eType, MINT32 i4W, MINT32 i4H, MINT32 i4GridX, MINT32 i4GridY)
    : m_pImp(new ILscTbl::LscTblImp(eType, i4W, i4H, i4GridX, i4GridY))
{
    if (ILscTbl::GAIN_FLOAT == eType)
    {
        m_pImp = new LscTblFloatImp(i4W, i4H, i4GridX, i4GridY);
    }
    else
    {
        m_pImp = new ILscTbl::LscTblImp(eType, i4W, i4H, i4GridX, i4GridY);
    }
}

ILscTbl::
ILscTbl(ILscTbl const& other)
    : m_pImp(new ILscTbl::LscTblImp(*(other.m_pImp)))
{
    if (ILscTbl::GAIN_FLOAT == other.getType())
    {
        m_pImp = new LscTblFloatImp(*(other.m_pImp));
    }
    else
    {
        m_pImp = new ILscTbl::LscTblImp(*(other.m_pImp));
    }
}

ILscTbl&
ILscTbl::
operator=(ILscTbl const& other)
{
    if (this != &other) {
        delete m_pImp;
        if (ILscTbl::GAIN_FLOAT == other.getType())
        {
            m_pImp = new LscTblFloatImp(*(other.m_pImp));
        }
        else
        {
            m_pImp = new ILscTbl::LscTblImp(*(other.m_pImp));
        }
    }
    else {
        LSC_LOG("this(%p) == other(%p)", this, &other);
    }

    return *this;
}

ILscTbl::
~ILscTbl()
{
    if (m_pImp) delete m_pImp;
}

ILscTbl::TBL_TYPE_T
ILscTbl::
getType() const
{
    return m_pImp->getType();
}

ILscTbl::TBL_BAYER_T
ILscTbl::
getBayer() const
{
    return m_pImp->getBayer();
}

const MVOID*
ILscTbl::
getData() const
{
    return m_pImp->getData();
}

MUINT32
ILscTbl::
getSize() const
{
    return m_pImp->getSize();
}

const ILscTbl::Config&
ILscTbl::
getConfig() const
{
    return m_pImp->getConfig();
}

MVOID*
ILscTbl::
editData()
{
    return m_pImp->editData();
}

MBOOL
ILscTbl::
setData(const MVOID* src, MUINT32 size)
{
    return m_pImp->setData(src, size);
}

MBOOL
ILscTbl::
setConfig(const ILscTbl::Config& rCfg)
{
    return m_pImp->setConfig(rCfg.i4ImgWd, rCfg.i4ImgHt, rCfg.i4GridX, rCfg.i4GridY);
}

MBOOL
ILscTbl::
setConfig(MINT32 i4W, MINT32 i4H, MINT32 i4GridX, MINT32 i4GridY)
{
    return m_pImp->setConfig(i4W, i4H, i4GridX, i4GridY);
}

MBOOL
ILscTbl::
setBayer(ILscTbl::TBL_BAYER_T bayer)
{
    return m_pImp->setBayer(bayer);
}

MBOOL
ILscTbl::
dump(const char* filename) const
{
    return m_pImp->dump(filename);
}

MBOOL
ILscTbl::
cropOut(const TransformCfg_T& trfm, ILscTbl& output) const
{
    return m_pImp->cropOut(trfm, output);
}

MBOOL
ILscTbl::
convert(ILscTbl& output) const
{
    return m_pImp->convert(output);
}

MBOOL
ILscTbl::
getRaTbl(MUINT32 u4Ratio, ILscTbl& output) const
{
    return m_pImp->getRaTbl(u4Ratio, output);
}

MBOOL
ILscTbl::
toBuf(ILscBuf& buf) const
{
    return m_pImp->toBuf(buf);
}
/*******************************************************************************
 * shadingAlign
 *******************************************************************************/
MINT32
ILscTbl::
shadingAlign(const ILscTbl& golden, const ILscTbl& unit, const ILscTbl& input, ILscTbl& output)
{
    MINT32 i4Ret = 0;

    SHADIND_ALIGN_CONF rSdAlignCfg;
    MUINT8* gWorkinBuffer = NULL;

    // check input golden gain, unit gain, and golden hw table
    if (ILscTbl::GAIN_FIXED != golden.getType() ||
        ILscTbl::GAIN_FIXED != unit.getType())
    {
        LSC_ERR("Not supported types (%d,%d,%d)", golden.getType(), unit.getType(), input.getType());
        return -2;
    }

    gWorkinBuffer = new MUINT8[SHADIND_FUNC_WORKING_BUFFER_SIZE];
    if (!gWorkinBuffer)
    {
        LSC_ERR("Fail to allocate gWorkinBuffer");
        return -1;
    }
    else
    {
        LSC_LOG("gWorkinBuffer(%p)", gWorkinBuffer);
    }

    Config rCfg = input.getConfig();
    Config rGolgenCfg = golden.getConfig();
    Config rUnitCfg = unit.getConfig();
    ILscTbl tmpGolden = golden;
    ILscTbl tmpUnit = unit;
    ILscTbl tmpInput = input;

    output.setConfig(rCfg);

    rSdAlignCfg.working_buff_addr   = (void*) gWorkinBuffer;
    rSdAlignCfg.working_buff_size   = SHADIND_FUNC_WORKING_BUFFER_SIZE;

    rSdAlignCfg.golden.img_width    = rCfg.i4ImgWd;
    rSdAlignCfg.golden.img_height   = rCfg.i4ImgHt;
    rSdAlignCfg.golden.offset_x     = 0;
    rSdAlignCfg.golden.offset_y     = 0;
    rSdAlignCfg.golden.crop_width   = rCfg.i4ImgWd;
    rSdAlignCfg.golden.crop_height  = rCfg.i4ImgHt;
    rSdAlignCfg.golden.bayer        = (BAYER_ID_T)golden.getBayer();
    rSdAlignCfg.golden.grid_x       = rGolgenCfg.i4GridX;
    rSdAlignCfg.golden.grid_y       = rGolgenCfg.i4GridY;    
    rSdAlignCfg.golden.lwidth       = 0;
    rSdAlignCfg.golden.lheight      = 0;
    rSdAlignCfg.golden.ratio_idx    = 0;
    rSdAlignCfg.golden.grgb_same    = SHADING_GRGB_SAME_NO;
    rSdAlignCfg.golden.data_type    = SHADING_TYPE_GAIN;
    rSdAlignCfg.golden.table        = (MUINT32*)tmpGolden.editData();

    rSdAlignCfg.cali.img_width      = rCfg.i4ImgWd;
    rSdAlignCfg.cali.img_height     = rCfg.i4ImgHt;
    rSdAlignCfg.cali.offset_x       = 0;
    rSdAlignCfg.cali.offset_y       = 0;
    rSdAlignCfg.cali.crop_width     = rCfg.i4ImgWd;
    rSdAlignCfg.cali.crop_height    = rCfg.i4ImgHt;
    rSdAlignCfg.cali.bayer          = (BAYER_ID_T)unit.getBayer();
    rSdAlignCfg.cali.grid_x         = rUnitCfg.i4GridX;
    rSdAlignCfg.cali.grid_y         = rUnitCfg.i4GridY;
    rSdAlignCfg.cali.lwidth         = 0;
    rSdAlignCfg.cali.lheight        = 0;
    rSdAlignCfg.cali.ratio_idx      = 0;
    rSdAlignCfg.cali.grgb_same      = SHADING_GRGB_SAME_NO;
    rSdAlignCfg.cali.data_type      = SHADING_TYPE_GAIN;
    rSdAlignCfg.cali.table          = (MUINT32*)tmpUnit.editData();

    rSdAlignCfg.input.img_width     = rCfg.i4ImgWd;
    rSdAlignCfg.input.img_height    = rCfg.i4ImgHt;
    rSdAlignCfg.input.offset_x      = 0;
    rSdAlignCfg.input.offset_y      = 0;
    rSdAlignCfg.input.crop_width    = rCfg.i4ImgWd;
    rSdAlignCfg.input.crop_height   = rCfg.i4ImgHt;
    rSdAlignCfg.input.bayer         = (BAYER_ID_T)BAYER_B;
    rSdAlignCfg.input.grid_x        = rCfg.i4GridX;             // Golden Coef
    rSdAlignCfg.input.grid_y        = rCfg.i4GridY;             // Golden Coef
    rSdAlignCfg.input.lwidth        = 0;
    rSdAlignCfg.input.lheight       = 0;
    rSdAlignCfg.input.ratio_idx     = 0;
    rSdAlignCfg.input.grgb_same     = SHADING_GRGB_SAME_NO;
    rSdAlignCfg.input.data_type     = SHADING_TYPE_COEFF;       // coef
    rSdAlignCfg.input.table         = (MUINT32*)tmpInput.editData();     // Golden Coef

    rSdAlignCfg.output.img_width    = rCfg.i4ImgWd;
    rSdAlignCfg.output.img_height   = rCfg.i4ImgHt;
    rSdAlignCfg.output.offset_x     = 0;                        // crop
    rSdAlignCfg.output.offset_y     = 0;                        // crop
    rSdAlignCfg.output.crop_width   = rCfg.i4ImgWd;             // crop
    rSdAlignCfg.output.crop_height  = rCfg.i4ImgHt;             // crop
    rSdAlignCfg.output.bayer        = (BAYER_ID_T)BAYER_B;
    rSdAlignCfg.output.grid_x       = rCfg.i4GridX;             // Golden Coef
    rSdAlignCfg.output.grid_y       = rCfg.i4GridY;             // Golden Coef
    rSdAlignCfg.output.lwidth       = 0;
    rSdAlignCfg.output.lheight      = 0;
    rSdAlignCfg.output.ratio_idx    = 0;
    rSdAlignCfg.output.grgb_same    = SHADING_GRGB_SAME_NO;
    rSdAlignCfg.output.data_type    = SHADING_TYPE_COEFF;       // coef
    rSdAlignCfg.output.table        = (MUINT32*)output.editData();         // Golden Coef

    //_LogShadingSpec("golden", rSdAlignCfg.golden);
    //_LogShadingSpec("cali",   rSdAlignCfg.cali);
    //_LogShadingSpec("input",  rSdAlignCfg.input);
    //_LogShadingSpec("output", rSdAlignCfg.output);

    LSC_RESULT result = S_LSC_CONVERT_OK;

    result = shading_align_golden(rSdAlignCfg);
    if (S_LSC_CONVERT_OK != result)
    {
        LSC_ERR("Align Error(%d)", result);
        i4Ret = -1;
    }
    else
    {
        LSC_LOG("Align done.");
    }

    delete [] gWorkinBuffer;

    return i4Ret;
}
