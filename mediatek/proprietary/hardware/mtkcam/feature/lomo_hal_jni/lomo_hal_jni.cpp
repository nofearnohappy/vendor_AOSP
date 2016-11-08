#define LOG_TAG "lomohj_v0912_00_L"  //eCACHECTRL_INVALID after allocation pHeap->createImageBuffer

#include "MyUtils.h"
#include "lomo_hal_jni_imp.h"

/**************************************************************************
*
**************************************************************************/
LomoHalJniImp::LomoHalJniImp()
            :LomoHalJni()
            , gu32pvCBWidth(0)
            , gu32pvCBHeight(0)
            , gu32pvCBformat(0)
            , gu32ppSrcImgWidth(0)
            , gu32ppSrcImgHeight(0)
{
}

/**************************************************************************
*
**************************************************************************/
LomoHalJniImp::~LomoHalJniImp()
{
}

/**************************************************************************
*
**************************************************************************/
LomoHalJni *LomoHalJni::createInstance( void )
{
	 return NULL; 
}

/**************************************************************************
*
**************************************************************************/
MVOID LomoHalJni::destroyInstance( void )
{

}

/**************************************************************************
*
**************************************************************************/
MINT32 LomoHalJniImp::init()
{
	 return 0; 
}

/**************************************************************************
*
**************************************************************************/
MINT32 LomoHalJniImp::uninit()
{
	 return 0; 
}


MINT32 LomoHalJniImp::AllocLomoSrcImage(MUINT32 pvCBWidth, \
                                                                     MUINT32 pvCBHeight, \
                                                                     MUINT32 pvCBformat, \
                                                                     MUINT32 ppSrcImgWidth, \
                                                                     MUINT32 ppSrcImgHeight)
{
	return 0; 
}


MINT32 LomoHalJniImp::AllocLomoDstImage(MUINT32 Number, \
                                                                     MUINT8** ppDstImgVA \
                                                                       )
{
     return 0; 	
}

MINT32 LomoHalJniImp::UploadLomoSrcImage(MUINT8* pvCBVA)
{

    return 0; 
    
}



/**
*/
MINT32 LomoHalJniImp::FreeLomoSrcImage(void)
{
	  return 0; 
}


MINT32 LomoHalJniImp::LomoImageEnque(MUINT8** ppDstImgVA, MINT32 ppEffectIdx)
{
	  return 0; 
}


MINT32 LomoHalJniImp::LomoImageDeque(MUINT8** ppDstImgMVA, MINT32 ppEffectIdx)
{
	  return 0; 
}

MINT32 LomoHalJniImp::FreeLomoDstImage(void)
{
	  return 0; 
}

MUINT32 LomoHalJniImp::ColorEffectSetting(MUINT32 caseNum)
{
    return 0;
}

/*******************************************************************************
*
********************************************************************************/
MVOID *LomoHalJniImp::LomoDequeThreadLoop(MVOID *arg)
{
	return NULL; 
}

/*******************************************************************************
*
********************************************************************************/
MVOID LomoHalJniImp::SetLomoState(const LOMO_STATE_ENUM &aState)
{
}


/*******************************************************************************
*
********************************************************************************/
LOMO_STATE_ENUM LomoHalJniImp::GetLomoState()
{
	  return LOMO_STATE_NONE ; 
    
}

MVOID LomoHalJniImp::ChangeThreadSetting()
{
}


/*******************************************************************************
*
********************************************************************************/
MVOID LomoHalJniImp::LomoDequeTrigger()
{

}

/*******************************************************************************
*
********************************************************************************/
LOMOHALJNI_RETURN_TYPE LomoHalJniImp::LomoDequeBuffer()
{
	  return LOMOHALJNI_NO_ERROR ;
}


