
#include "com_mediatek_nfc_dta.h"
#include "DtaFlow.h"
#include "DtaApi.h"

namespace android {

static const unsigned char DTA_T3T_UPDATE_REQ[] =
{
    0x08,
    0x02,0xFE,0x00,0x01,0x02,0x03,0x04,0x05, //'(NFCID2)
    0x01,
    0x09,0x00,
    0x01,
    0x80,0x01,
    0xFF,0xFF,0xFF,0xFF,0xFF,0xFF,0xFF,0xFF,0xFF,0xFF,0xFF,0xFF,0xFF,0xFF,0xFF,0xFF
};

static const unsigned char DTA_T3T_UPDATE_RSP[] =
{
    0x09,
    0x02,0xFE,0x00,0x01,0x02,0x03,0x04,0x05,
    0x00,
    0x00
};

static const unsigned char DTA_T3T_CHECK_REQ[] =
{
    0x06,
    0x02,0xFE,0x00,0x01,0x02,0x03,0x04,0x05,
    0x01,
    0x09,0x00,
    0x01,
    0x80,0x01
};

static const unsigned char DTA_T3T_CHECK_RSP[] =
{
    0x07,
    0x02,0xFE,0x00,0x01,0x02,0x03,0x04,0x05,
    0x00,
    0x00,
    0x01,
    0xFF,0xFF,0xFF,0xFF,0xFF,0xFF,0xFF,0xFF,0xFF,0xFF,0xFF,0xFF,0xFF,0xFF,0xFF,0xFF
};

static const unsigned char DTA_T3T_UPDATE_ACK_REQ[] =
{
    0x08,
    0x02,0xFE,0x00,0x01,0x02,0x03,0x04,0x05,    //'(NFCID2) 
    0x01,
    0x09,0x00,
    0x01,
    0x80,0x01,
    0x0F,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00
};

static void T3TUpdateAckRsp(void* pCallbackParameter, unsigned long nDataLength, unsigned long nResult)
{
    unsigned char *pRspBuffer = (unsigned char *)pCallbackParameter;

    LOGD("%s: nDataLength(%ld), nResult(0x%lX)", __FUNCTION__, nDataLength, nResult);
    DtaPrintBuf("pRspBuffer:", pRspBuffer, nDataLength);
    DtaGetErrorCode(nResult);
    if(DtaGetErrorCode(nResult) != DTA_SUCCESS)
    {
        goto end;
    }
end:
    DtaFlowTestEnd();
}

static void T3TCheckRsp(void* pCallbackParameter, unsigned long nDataLength, unsigned long nResult)
{
    unsigned char *pRspBuffer = (unsigned char *)pCallbackParameter;
    int doNext = 0;

    LOGD("%s: nDataLength(%ld), nResult(0x%lX)", __FUNCTION__, nDataLength, nResult);
    DtaPrintBuf("pRspBuffer:", pRspBuffer, nDataLength);

    if(DtaGetErrorCode(nResult) != DTA_SUCCESS)
    {
        goto end;
    }

    if(sizeof(DTA_T3T_UPDATE_ACK_REQ) == 0)
    {
        goto end;
    }
    //compare OK
    if(memcmp(pRspBuffer, DTA_T3T_CHECK_RSP, sizeof(DTA_T3T_CHECK_RSP)) == 0)
    {
        doNext = DtaIsoDepExchangeData(DTA_T3T_UPDATE_ACK_REQ,
                                       sizeof(DTA_T3T_UPDATE_ACK_REQ),
                                       T3TUpdateAckRsp);
        goto end;
    }

end:

    if(0 == doNext)
        DtaFlowTestEnd();
}

static void T3TUpdateRsp(void* pCallbackParameter, unsigned long nDataLength, unsigned long nResult)
{
    unsigned char *pRspBuffer = (unsigned char *)pCallbackParameter;
    int doNext = 0;

    LOGD("%s: nDataLength(%ld), nResult(0x%lX)", __FUNCTION__, nDataLength, nResult);
    DtaPrintBuf("pRspBuffer:", pRspBuffer, nDataLength);

	if(DtaGetErrorCode(nResult) != DTA_SUCCESS)
    {
		goto end;
	}
    if(sizeof(DTA_T3T_CHECK_REQ)==0)
    {
        goto end;
    }
    //compare OK
    if(memcmp(pRspBuffer, DTA_T3T_UPDATE_RSP, sizeof(DTA_T3T_UPDATE_RSP)) == 0)
    {
        doNext = DtaIsoDepExchangeData(DTA_T3T_CHECK_REQ,
                                       sizeof(DTA_T3T_CHECK_REQ),
                                       T3TCheckRsp); 
        goto end;
    }    

end:

    if(0 == doNext)
        DtaFlowTestEnd();
}

int T3TPlatformTest(void)
{
    int send_len = 0;

    transferMessageToJava((char*)"T3TPlatformTest");
	LOGD("%s:", __FUNCTION__);
    if(sizeof(DTA_T3T_UPDATE_REQ) == 0)
    {
        DtaFlowTestEnd();
    }
    else
    {
        send_len = DtaIsoDepExchangeData(DTA_T3T_UPDATE_REQ,
                                         sizeof(DTA_T3T_UPDATE_REQ),
                                         T3TUpdateRsp); 

        if(0 == send_len)
            DtaFlowTestEnd();
    }
    return send_len;
}


}

