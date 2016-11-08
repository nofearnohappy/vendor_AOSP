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
#define LOG_TAG "lsc_buf"
#ifndef ENABLE_MY_LOG
#define ENABLE_MY_LOG           (1)
#define GLOBAL_ENABLE_MY_LOG    (1)
#endif

#include <LscUtil.h>
#include <ILscBuf.h>
#include <imem_drv.h>
#include <string>

using namespace NSIspTuning;

#define DEFAULT_TABLE_SIZE (16*16*4*4*4)
/*******************************************************************************
 * ILscBuf::LscBufImp
 *******************************************************************************/
class ILscBuf::LscBufImp
{
public:
                            LscBufImp(MUINT32 sensorDev, MUINT32 u4Id, const char* strName);
    virtual                 ~LscBufImp();

    virtual ILscBuf::Config getConfig() const;
    virtual MUINT32         getPhyAddr() const;
    virtual MBOOL           setConfig(ILscBuf::Config rCfg);
    virtual const MUINT32*  getTable() const;
    virtual MBOOL           setTable(const void* data, MUINT32 u4Size);
    virtual MBOOL           validate();
    virtual MBOOL           showInfo() const;
    virtual MBOOL           dump(const char* filename) const;

protected:
    virtual MBOOL           initMemBuf();
    virtual MBOOL           uninitMemBuf();
    virtual MBOOL           allocMemBuf(IMEM_BUF_INFO& rBufInfo, MUINT32 const u4Size);
    virtual MBOOL           freeMemBuf(IMEM_BUF_INFO& rBufInfo);

    MUINT32                 m_u4SensorDev;
    MUINT32                 m_u4Id;
    IMemDrv*                m_pIMemDrv;
    ILscBuf::Config         m_rConfig;
    IMEM_BUF_INFO           m_rRawLscInfo;
    std::string             m_strName;
};

ILscBuf::LscBufImp::
LscBufImp(MUINT32 sensorDev, MUINT32 u4Id, const char* strName)
    : m_u4SensorDev(sensorDev)
    , m_u4Id(u4Id)
    , m_pIMemDrv(NULL)
    , m_strName(strName)
{
    initMemBuf();
}

ILscBuf::LscBufImp::
~LscBufImp()
{
    uninitMemBuf();
}

MBOOL
ILscBuf::LscBufImp::
initMemBuf()
{
    LSC_LOG_BEGIN("m_u4SensorDev(0x%02x), id(%d), name(%s)", m_u4SensorDev, m_u4Id, m_strName.c_str());
    
    MBOOL ret = MFALSE;

    MUINT32 i = 0;

    ret = MTRUE;
    if (!m_pIMemDrv)
    {
        m_pIMemDrv = IMemDrv::createInstance();

        if (!m_pIMemDrv)
        {
            LSC_LOG("m_pIMemDrv create Fail.");
            ret = MFALSE;
        }
        else
        {
            ret = m_pIMemDrv->init();
            if (ret == MTRUE)
            {
                if (!allocMemBuf(m_rRawLscInfo, DEFAULT_TABLE_SIZE))
                {
                    LSC_ERR("allocMemBuf sensor(0x%02x), id(%d) FAILED", m_u4SensorDev, m_u4Id);
                }
                else
                {
                    LSC_LOG("sensor(0x%02x), id(%d), memID(%d), virtAddr(%p), phyAddr(%p), size(%d)",
                        m_u4SensorDev, m_u4Id, m_rRawLscInfo.memID, m_rRawLscInfo.virtAddr, m_rRawLscInfo.phyAddr, m_rRawLscInfo.size);
                }
            }
            else
            {
                LSC_ERR("m_pIMemDrv init Fail!");
            }
        }
    }
    else
    {
        LSC_LOG("m_pIMemDrv(%p) already exists.", m_pIMemDrv);
    }

    LSC_LOG_END();
    return ret;
}

MBOOL
ILscBuf::LscBufImp::
uninitMemBuf()
{
    MUINT32 ret = 0;
    MUINT32 i = 0;

    LSC_LOG_BEGIN("m_u4SensorDev(0x%02x), id(%d), name(%s)", m_u4SensorDev, m_u4Id, m_strName.c_str());
    
    freeMemBuf(m_rRawLscInfo);

    if (m_pIMemDrv)
    {
        m_pIMemDrv->uninit();
        m_pIMemDrv->destroyInstance();
        m_pIMemDrv = NULL;
    }
    
    LSC_LOG_END();
    return MTRUE;
}

MBOOL
ILscBuf::LscBufImp::
allocMemBuf(IMEM_BUF_INFO& rBufInfo, MUINT32 const u4Size)
{
    MBOOL ret = MFALSE;

    if (!rBufInfo.virtAddr)
    {
        rBufInfo.size = u4Size;
        if (0 == m_pIMemDrv->allocVirtBuf(&rBufInfo))
        {
            if (0 != m_pIMemDrv->mapPhyAddr(&rBufInfo))
            {
                LSC_ERR("mapPhyAddr error, virtAddr(%p), size(%d)\n", rBufInfo.virtAddr, rBufInfo.size);
                ret = MFALSE;
            }
            else
            {
                ret = MTRUE;
            }
        }
        else
        {
            LSC_ERR("allocVirtBuf error, size(%d)\n", rBufInfo.size);
            ret = MFALSE;
        }
    }
    else
    {
        ret = MTRUE;
        LSC_LOG("Already Exist! virtAddr(%p), size(%d)\n", rBufInfo.virtAddr, u4Size);
    }
    return ret;
}

MBOOL
ILscBuf::LscBufImp::
freeMemBuf(IMEM_BUF_INFO& rBufInfo)
{
    MBOOL ret = MTRUE;

    if (!m_pIMemDrv || rBufInfo.virtAddr == 0)
    {
        LSC_ERR("Null m_pIMemDrv driver \n");
        return MFALSE;
    }

    if (0 == m_pIMemDrv->unmapPhyAddr(&rBufInfo))
    {
        if (0 == m_pIMemDrv->freeVirtBuf(&rBufInfo))
        {
            LSC_LOG("freeVirtBuf OK, memID(%d), virtAddr(%p), phyAddr(%p)", rBufInfo.memID, rBufInfo.virtAddr, rBufInfo.phyAddr);
            rBufInfo.virtAddr = 0;
            ret = MTRUE;
        }
        else
        {
            LSC_LOG("freeVirtBuf Fail, memID(%d), virtAddr(%p), phyAddr(%p)", rBufInfo.memID, rBufInfo.virtAddr, rBufInfo.phyAddr);
            ret = MFALSE;
        }
    }
    else
    {
        LSC_ERR("memID(%d) unmapPhyAddr error", rBufInfo.memID);
        ret = MFALSE;
    }

    return ret;
}

MBOOL
ILscBuf::LscBufImp::
showInfo() const
{
    LSC_LOG("sensor(0x%02x), name(%s), id(%d), memID(%d), virtAddr(%p), phyAddr(%p), size(%d), Img(%dx%d), Blk(%d,%d,%d,%d,%d,%d)",
        m_u4SensorDev, m_strName.c_str(), m_u4Id, m_rRawLscInfo.memID, m_rRawLscInfo.virtAddr, m_rRawLscInfo.phyAddr, m_rRawLscInfo.size,
        m_rConfig.i4ImgWd, m_rConfig.i4ImgHt, m_rConfig.i4BlkX, m_rConfig.i4BlkY, m_rConfig.i4BlkW, m_rConfig.i4BlkH, m_rConfig.i4BlkLastW, m_rConfig.i4BlkLastH);
    return MTRUE;
}

ILscBuf::Config
ILscBuf::LscBufImp::
getConfig() const
{
    return m_rConfig;
}

MUINT32
ILscBuf::LscBufImp::
getPhyAddr() const
{
    return m_rRawLscInfo.phyAddr;
}

const MUINT32*
ILscBuf::LscBufImp::
getTable() const
{
    return (MUINT32*)m_rRawLscInfo.virtAddr;
}

MBOOL
ILscBuf::LscBufImp::
setConfig(ILscBuf::Config rConfig)
{
    if (rConfig.i4BlkX >= 32 || rConfig.i4BlkX == 0 || rConfig.i4BlkY >= 32 || rConfig.i4BlkY == 0)
    {
        // assert
        LSC_ERR("XNum(%d), YNum(%d)", rConfig.i4BlkX, rConfig.i4BlkY);
        return MFALSE;
    }

    m_rConfig.i4ImgWd = rConfig.i4ImgWd;
    m_rConfig.i4ImgHt = rConfig.i4ImgHt;
    m_rConfig.i4BlkX  = rConfig.i4BlkX;
    m_rConfig.i4BlkY  = rConfig.i4BlkY;
    m_rConfig.i4BlkW  = (rConfig.i4ImgWd)/(2*(rConfig.i4BlkX+1));
    m_rConfig.i4BlkH  = (rConfig.i4ImgHt)/(2*(rConfig.i4BlkY+1));
    m_rConfig.i4BlkLastW = rConfig.i4ImgWd/2 - (rConfig.i4BlkX*m_rConfig.i4BlkW);
    m_rConfig.i4BlkLastH = rConfig.i4ImgHt/2 - (rConfig.i4BlkY*m_rConfig.i4BlkH);

    MUINT32 u4Size = (rConfig.i4BlkX+1) * (rConfig.i4BlkY+1) * 4 * 4 * 4;
    if (u4Size > m_rRawLscInfo.size)
    {
        // need to reallocate memory
        if (!freeMemBuf(m_rRawLscInfo))
        {
            LSC_ERR("Fail to free buf (%d, %d)", m_u4SensorDev, m_u4Id);
        }
        else
        {
            if (!allocMemBuf(m_rRawLscInfo, u4Size))
            {
                LSC_LOG("allocMemBuf sensor(0x%02x), id(%d) FAILED", m_u4SensorDev, m_u4Id);
            }
            else
            {
                LSC_LOG("sensor(0x%02x), id(%d), memID(%d), virtAddr(%p), phyAddr(%p), size(%d)",
                    m_u4SensorDev, m_u4Id, m_rRawLscInfo.memID, m_rRawLscInfo.virtAddr, m_rRawLscInfo.phyAddr, m_rRawLscInfo.size);
            }
        }
    }

    return MTRUE;
}

MBOOL
ILscBuf::LscBufImp::
setTable(const void* data, MUINT32 u4Size)
{
    if (data == NULL || u4Size > m_rRawLscInfo.size)
    {
        LSC_ERR("src(%p), dest(%p), size(%d)", data, m_rRawLscInfo.virtAddr, u4Size);
        return MFALSE;
    }
    ::memcpy((MUINT32*)m_rRawLscInfo.virtAddr, data, u4Size);
    return MTRUE;
}

MBOOL
ILscBuf::LscBufImp::
validate()
{
    m_pIMemDrv->cacheSyncbyRange(IMEM_CACHECTRL_ENUM_FLUSH, &m_rRawLscInfo);
    return MTRUE;
}

MBOOL
ILscBuf::LscBufImp::
dump(const char* filename) const
{
    char strFilename[512];
    FILE *fhwtbl,*fsdblk;

    MUINT32* pData = (MUINT32*)m_rRawLscInfo.virtAddr;
    showInfo();

    if (pData == NULL)
    {
        LSC_ERR("NULL buffer");
        return MFALSE;
    }

    LSC_LOG_BEGIN();

    sprintf(strFilename, "%s.sdblk", filename);
    fsdblk = fopen(strFilename, "w");
    if ( fsdblk == NULL )
    {
        LSC_ERR("Can't open: %s", strFilename);
        return MFALSE;
    }

    sprintf(strFilename, "%s.hwtbl", filename);
    fhwtbl = fopen(strFilename, "w"); 
    if ( fhwtbl == NULL )
    {
        LSC_ERR("Can't open: %s", strFilename);
        return MFALSE;
    }

    fprintf(fsdblk," %8d  %8d  %8d  %8d  %8d  %8d  %8d  %8d\n",
            0 /*LscConfig.ctl1.bits.SDBLK_XOFST*/,
            0 /*LscConfig.ctl1.bits.SDBLK_YOFST*/,
            m_rConfig.i4BlkW /*LscConfig.ctl2.bits.LSC_SDBLK_WIDTH*/,
            m_rConfig.i4BlkH /*LscConfig.ctl3.bits.LSC_SDBLK_HEIGHT*/,
            m_rConfig.i4BlkX /*LscConfig.ctl2.bits.LSC_SDBLK_XNUM*/,
            m_rConfig.i4BlkY /*LscConfig.ctl3.bits.LSC_SDBLK_YNUM*/,
            m_rConfig.i4BlkLastW /*LscConfig.lblock.bits.LSC_SDBLK_lWIDTH*/,
            m_rConfig.i4BlkLastH /*LscConfig.lblock.bits.LSC_SDBLK_lHEIGHT*/);

    MINT32 x_num = m_rConfig.i4BlkX + 1;
    MINT32 y_num = m_rConfig.i4BlkY + 1;

    MINT32 numCoef = x_num * y_num * 4 * 4;
    MINT32 i, c = 0;

    for (i = numCoef-1; i >= 0; i--)
    {
        MUINT32 coef1, coef2, coef3;
        MUINT32 val = *pData++;
        coef3 = (val& 0x3FF00000) >> 20;
        coef2 = (val& 0x000FFC00) >> 10;
        coef1 = val& 0x000003FF;
        fprintf(fsdblk, " %8d %8d %8d", coef1, coef2, coef3);
        fprintf(fhwtbl,"0x%08x, ", val);
        c ++;

        if (c == 4)
        {
            c = 0;
            fprintf(fhwtbl,"\n");
            fprintf(fsdblk,"\n");
        }
    }

    fclose(fhwtbl);
    fclose(fsdblk);

    LSC_LOG_END();

    return 0;
}
/*******************************************************************************
 * ILscBuf
 *******************************************************************************/
ILscBuf::
ILscBuf(MUINT32 sensorDev, MUINT32 u4Id, const char* strName)
    : m_pImp(new LscBufImp(sensorDev, u4Id, strName))
{
}

ILscBuf::
~ILscBuf()
{
    if (m_pImp) delete m_pImp;
}

ILscBuf::Config
ILscBuf::
getConfig() const
{
    return m_pImp->getConfig();
}

MUINT32
ILscBuf::
getPhyAddr() const
{
    return m_pImp->getPhyAddr();
}

const MUINT32*
ILscBuf::
getTable() const
{
    return m_pImp->getTable();
}

MBOOL
ILscBuf::
setConfig(ILscBuf::Config rCfg)
{
    return m_pImp->setConfig(rCfg);
}

MBOOL
ILscBuf::
setTable(const void* data, MUINT32 u4Size)
{
    return m_pImp->setTable(data, u4Size);
}

MBOOL
ILscBuf::
validate()
{
    return m_pImp->validate();
}

MBOOL
ILscBuf::
showInfo() const
{
    return m_pImp->showInfo();
}

MBOOL
ILscBuf::
dump(const char* strFile) const
{
    return m_pImp->dump(strFile);
}
