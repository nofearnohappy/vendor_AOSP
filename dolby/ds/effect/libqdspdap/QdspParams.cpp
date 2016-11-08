/******************************************************************************
 *  This program is protected under international and U.S. copyright laws as
 *  an unpublished work. This program is confidential and proprietary to the
 *  copyright owners. Reproduction or disclosure, in whole or in part, or the
 *  production of derivative works therefrom without the express permission of
 *  the copyright owners is prohibited.
 *
 *               Copyright (C) 2014 by Dolby Laboratories,
 *                             All rights reserved.
 ******************************************************************************/

#define LOG_TAG "DlbQdspDapParams"

#include "DlbLog.h"
#include <utils/KeyedVector.h>
#include "QdspParams.h"

namespace dolby {

static bool mInitialized = false;
static android::KeyedVector<DapParameterId, QdspParameterId> mParamIdToQdspId;
static android::KeyedVector<QdspParameterId, DapParameterId> mQdspIdToParamId;
static void init();

QdspParameterId qdspParamIdForParam(DapParameterId id)
{
    init();
    return mParamIdToQdspId.valueFor(id);
}

DapParameterId paramIdForQdspId(QdspParameterId id)
{
    init();
    return mQdspIdToParamId.valueFor(id);
}

static void init()
{
    if (mInitialized)
        return;
    mInitialized = true;
    // Set up the map from param ID to QDSP ID.
    mParamIdToQdspId.add(DAP1_PARAM_BVER, QDSP_INVALID_PARAM);
    mParamIdToQdspId.add(DAP1_PARAM_BNDL, QDSP_INVALID_PARAM);
    mParamIdToQdspId.add(DAP1_PARAM_OCF,  QDSP_INVALID_PARAM);
    mParamIdToQdspId.add(DAP_PARAM_PREG, QDSP_DAP_PARAM_PREG);
    mParamIdToQdspId.add(DAP1_PARAM_VDHE, QDSP_DAP_PARAM_VDHE);
    mParamIdToQdspId.add(DAP1_PARAM_VSPE, QDSP_DAP_PARAM_VSPE);
    mParamIdToQdspId.add(DAP_PARAM_DSSF, QDSP_DAP_PARAM_DSSF);
    mParamIdToQdspId.add(DAP1_PARAM_SCPE, QDSP_INVALID_PARAM);
    mParamIdToQdspId.add(DAP_PARAM_DVLI, QDSP_DAP_PARAM_DVLI);
    mParamIdToQdspId.add(DAP_PARAM_DVLO, QDSP_DAP_PARAM_DVLO);
    mParamIdToQdspId.add(DAP_PARAM_DVLE, QDSP_DAP_PARAM_DVLE);
    mParamIdToQdspId.add(DAP_PARAM_DVMC, QDSP_DAP_PARAM_DVMC);
    mParamIdToQdspId.add(DAP_PARAM_DVME, QDSP_DAP_PARAM_DVME);
    mParamIdToQdspId.add(DAP1_PARAM_IENB, QDSP_DAP_PARAM_IENB);
    mParamIdToQdspId.add(DAP1_PARAM_IEBF, QDSP_DAP_PARAM_IEBF);
    mParamIdToQdspId.add(DAP_PARAM_IEON, QDSP_DAP_PARAM_IEON);
    mParamIdToQdspId.add(DAP_PARAM_DEON, QDSP_DAP_PARAM_DEON);
    mParamIdToQdspId.add(DAP_PARAM_NGON, QDSP_DAP_PARAM_NGON);
    mParamIdToQdspId.add(DAP_PARAM_GEON, QDSP_DAP_PARAM_GEON);
    mParamIdToQdspId.add(DAP1_PARAM_GENB, QDSP_DAP_PARAM_GENB);
    mParamIdToQdspId.add(DAP1_PARAM_GEBF, QDSP_DAP_PARAM_GEBF);
    mParamIdToQdspId.add(DAP1_PARAM_AONB, QDSP_DAP_PARAM_AONB);
    mParamIdToQdspId.add(DAP1_PARAM_AOBF, QDSP_DAP_PARAM_AOBF);
    mParamIdToQdspId.add(DAP1_PARAM_AOBG, QDSP_DAP_PARAM_AOBG);
    mParamIdToQdspId.add(DAP_PARAM_AOON, QDSP_DAP_PARAM_AOON);
    mParamIdToQdspId.add(DAP1_PARAM_ARNB, QDSP_DAP_PARAM_ARNB);
    mParamIdToQdspId.add(DAP1_PARAM_ARBF, QDSP_DAP_PARAM_ARBF);
    mParamIdToQdspId.add(DAP_PARAM_PLB,  QDSP_DAP_PARAM_PLB);
    mParamIdToQdspId.add(DAP1_PARAM_PLMD, QDSP_DAP_PARAM_PLMD);
    mParamIdToQdspId.add(DAP1_PARAM_TEST, QDSP_INVALID_PARAM);
    mParamIdToQdspId.add(DAP1_PARAM_VEN,  QDSP_DAP_PARAM_VEN);
    mParamIdToQdspId.add(DAP1_PARAM_VNNB, QDSP_INVALID_PARAM);
    mParamIdToQdspId.add(DAP1_PARAM_VNBF, QDSP_INVALID_PARAM);
    mParamIdToQdspId.add(DAP1_PARAM_VNBG, QDSP_INVALID_PARAM);
    mParamIdToQdspId.add(DAP1_PARAM_VNBE, QDSP_INVALID_PARAM);
    mParamIdToQdspId.add(DAP_PARAM_VCNB, QDSP_DAP_PARAM_VCNB);
    mParamIdToQdspId.add(DAP_PARAM_VCBF, QDSP_DAP_PARAM_VCBF);
    mParamIdToQdspId.add(DAP1_PARAM_VCBG, QDSP_DAP_PARAM_VCBG);
    mParamIdToQdspId.add(DAP1_PARAM_VCBE, QDSP_DAP_PARAM_VCBE);
    mParamIdToQdspId.add(DAP1_PARAM_VMON, QDSP_DAP_PARAM_VMON);
    mParamIdToQdspId.add(DAP_PARAM_VMB,  QDSP_DAP_PARAM_VMB);
    mParamIdToQdspId.add(DAP1_PARAM_LCMF, QDSP_INVALID_PARAM);
    mParamIdToQdspId.add(DAP1_PARAM_LCVD, QDSP_INVALID_PARAM);
    mParamIdToQdspId.add(DAP1_PARAM_LCPT, QDSP_INVALID_PARAM);
    mParamIdToQdspId.add(DAP_PARAM_VER,  QDSP_INVALID_PARAM);
    mParamIdToQdspId.add(DAP_PARAM_PSTG, QDSP_DAP_PARAM_PSTG);
    mParamIdToQdspId.add(DAP_PARAM_VOL,  QDSP_INVALID_PARAM);
    mParamIdToQdspId.add(DAP1_PARAM_DHSB, QDSP_DAP_PARAM_DHSB);
    mParamIdToQdspId.add(DAP_PARAM_DHRG, QDSP_DAP_PARAM_DHRG);
    mParamIdToQdspId.add(DAP1_PARAM_DSSB, QDSP_DAP_PARAM_DSSB);
    mParamIdToQdspId.add(DAP_PARAM_DSSA, QDSP_DAP_PARAM_DSSA);
    mParamIdToQdspId.add(DAP_PARAM_DVLA, QDSP_DAP_PARAM_DVLA);
    mParamIdToQdspId.add(DAP1_PARAM_IEBT, QDSP_DAP_PARAM_IEBT);
    mParamIdToQdspId.add(DAP_PARAM_IEA,  QDSP_DAP_PARAM_IEA);
    mParamIdToQdspId.add(DAP_PARAM_DEA,  QDSP_DAP_PARAM_DEA);
    mParamIdToQdspId.add(DAP_PARAM_DED,  QDSP_DAP_PARAM_DED);
    mParamIdToQdspId.add(DAP1_PARAM_GEBG, QDSP_DAP_PARAM_GEBG);
    mParamIdToQdspId.add(DAP1_PARAM_AOCC, QDSP_DAP_PARAM_AOCC);
    mParamIdToQdspId.add(DAP1_PARAM_ARBI, QDSP_DAP_PARAM_ARBI);
    mParamIdToQdspId.add(DAP1_PARAM_ARBL, QDSP_DAP_PARAM_ARBL);
    mParamIdToQdspId.add(DAP1_PARAM_ARBH, QDSP_DAP_PARAM_ARBH);
    mParamIdToQdspId.add(DAP_PARAM_AROD, QDSP_DAP_PARAM_AROD);
    mParamIdToQdspId.add(DAP_PARAM_ARTP, QDSP_DAP_PARAM_ARTP);
    mParamIdToQdspId.add(DAP1_PARAM_ENDP, QDSP_DAP_PARAM_ENDP);
    // Set up the map from QDSP ID to DAP param ID.
    for (int i = 0; i < (int)mParamIdToQdspId.size(); i++)
    {
        QdspParameterId value = mParamIdToQdspId.valueAt(i);
        if (value != QDSP_INVALID_PARAM)
        {
            mQdspIdToParamId.add(value, mParamIdToQdspId.keyAt(i));
        }
    }
}

}
