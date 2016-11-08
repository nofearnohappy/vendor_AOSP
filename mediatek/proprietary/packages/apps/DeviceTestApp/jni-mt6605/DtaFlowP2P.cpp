
#include "com_mediatek_nfc_dta.h"
#include "DtaFlow.h"
#include "DtaApi.h"

namespace android
{

static const unsigned char DTA_P2P_SOT[] = {0x00, 0x40, 0x00, 0x01, 0x10, 0x02, 0x01, 0x0E};
static const unsigned char DTA_P2P_EOT_DSL[] = {0xFF, 0xFF, 0xFF, 0x01, 0x01};
static const unsigned char DTA_P2P_EOT_RLS[] = {0xFF, 0xFF, 0xFF, 0x01, 0x02};
static const unsigned char DTA_P2P_PATTERN_NUMBER[] = {0xFF, 0x00, 0x00, 0x00};


static void P2PDslRsp(void* pCallbackParameter, unsigned long nDataLength, unsigned long nResult)
{
    uint8_t *pRspBuffer = (uint8_t *)pCallbackParameter;
    int doNext = 0;

    LOGD("%s: nDataLength(%ld), nResult(0x%lX)", __FUNCTION__, nDataLength, nResult);
    DtaPrintBuf("pRspBuffer:", pRspBuffer, nDataLength);

    if(DtaGetErrorCode(nResult) != DTA_SUCCESS)
    {
        goto end;
    }

end:

    if(0 == doNext)
        DtaFlowTestEnd();
    return;
}

static void P2PRlsRsp(void* pCallbackParameter, unsigned long nDataLength, unsigned long nResult)
{
    uint8_t *pRspBuffer = (uint8_t *)pCallbackParameter;
    int doNext = 0;

    LOGD("%s: nDataLength(%ld), nResult(0x%lX)", __FUNCTION__, nDataLength, nResult);
    DtaPrintBuf("pRspBuffer:", pRspBuffer, nDataLength);

    if(DtaGetErrorCode(nResult) != DTA_SUCCESS)
    {
        goto end;
    }

end:

    if(0 == doNext)
        DtaFlowTestEnd();
    return;
}

static void P2PInitiatorDepRsp(void* pCallbackParameter, unsigned long nDataLength, unsigned long nResult)
{
    uint8_t *pRspBuffer = (uint8_t *)pCallbackParameter;
    int doNext = 0;

    LOGD("%s: nDataLength(%ld), nResult(0x%lX)", __FUNCTION__, nDataLength, nResult);
    DtaPrintBuf("pRspBuffer:", pRspBuffer, nDataLength);

    transferMessageToJava((char*)"P2PInitiatorDepRsp");

    if(DtaGetErrorCode(nResult) != DTA_SUCCESS)
    {
        goto end;
    }

    //compare EOT DSL
    if(memcmp(pRspBuffer, DTA_P2P_EOT_DSL, sizeof(DTA_P2P_EOT_DSL)) == 0)
    {
        doNext = DtaNfcDslCmd(P2PDslRsp);

        goto end;
    }

    //compare EOT RLS
    if(memcmp(pRspBuffer, DTA_P2P_EOT_RLS, sizeof(DTA_P2P_EOT_RLS)) == 0)
    {
        doNext = DtaNfcRlsCmd(P2PRlsRsp);

        goto end;
    }

    //loop back
    doNext = DtaNfcDepExchangeData(pRspBuffer,
                                   nDataLength,
                                   P2PInitiatorDepRsp);

end:

    if(0 == doNext)
        DtaFlowTestEnd();

    return;
}

static void P2PTargetDepRsp(void* pCallbackParameter, unsigned long nDataLength, unsigned long nResult)
{
    uint8_t *pRspBuffer = (uint8_t *)pCallbackParameter;
    int doNext = 0;

    LOGD("%s: nDataLength(%ld), nResult(0x%lX)", __FUNCTION__, nDataLength, nResult);
    DtaPrintBuf("pRspBuffer:", pRspBuffer, nDataLength);

    transferMessageToJava((char*)"P2PTargetDepRsp");

    if(DtaGetErrorCode(nResult) != DTA_SUCCESS)
    {
        goto end;
    }

    //Check return length
    if(nDataLength == 0)
    {
        goto end;
    }

    //compare pattern number
    if(memcmp(pRspBuffer, DTA_P2P_PATTERN_NUMBER, sizeof(DTA_P2P_PATTERN_NUMBER)) == 0)
    {
        DtaSetConfig((int)pRspBuffer[nDataLength-1]);
    }

    //loop back
    doNext = DtaNfcDepExchangeData(pRspBuffer,
                                   nDataLength,
                                   P2PTargetDepRsp);

end:

    //if(0 == doNext)
    //    DtaFlowTestEnd();

    return;
}

int P2PTest(DTA_P2P_TYPE type)
{
    int send_len = 0;

    LOGD("%s:", __FUNCTION__);

    switch(type)
    {
    case DTA_P2P_INITIATOR:
        transferMessageToJava((char*)"P2P INITIATOR");
        send_len = DtaNfcDepExchangeData(DTA_P2P_SOT,
                                         sizeof(DTA_P2P_SOT),
                                         P2PInitiatorDepRsp);
        break;

    case DTA_P2P_TARGET:
        transferMessageToJava((char*)"P2P TARGET");
        //no receive DEP here
        //send_len = DtaNfcDepExchangeData(NULL,
        //                                 0,
        //                                 P2PTargetDepRsp);
        break;

    default:
        break;
    }

    //if(0 == send_len)
    //    DtaFlowTestEnd();

    return send_len;
}


}

