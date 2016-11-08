
#include "com_mediatek_nfc_dta.h"
#include "DtaFlow.h"
#include "DtaApi.h"

namespace android
{

/*
static const unsigned char DTA_T2T_CMD_0_REQ[] = {0xA2,0x04,0x03,0x00,0xFE,0x00};
static const unsigned char DTA_T2T_CMD_0_RSP[] = {0x0A};
static const unsigned char DTA_T2T_CMD_1_REQ[] = {0x30,0x04};
static const unsigned char DTA_T2T_CMD_1_RSP[] = {0x03,0x00,0xFE,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00};
static const unsigned char DTA_T2T_CMD_2_REQ[] = {0xC2,0xFF};
static const unsigned char DTA_T2T_CMD_2_RSP[] = {0x0A};
static const unsigned char DTA_T2T_CMD_3_REQ[] = {0x01,0x00,0x00,0x00};
static const unsigned char DTA_T2T_CMD_3_RSP[] = {0x0A};
static const unsigned char DTA_T2T_CMD_4_REQ[] = {0x50,0x00};
*/

static const unsigned char DTA_T2T_CMD_0_REQ[] = {0x30,0x06};
static const unsigned char DTA_T2T_CMD_0_RSP[] = {0x03,0x00,0xFE,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00};
static const unsigned char DTA_T2T_CMD_1_REQ[] = {0x50,0x00};

static const unsigned char DTA_T2T_CMD_1_RSP[] = {};
static const unsigned char DTA_T2T_CMD_2_REQ[] = {};
static const unsigned char DTA_T2T_CMD_2_RSP[] = {};
static const unsigned char DTA_T2T_CMD_3_REQ[] = {};
static const unsigned char DTA_T2T_CMD_3_RSP[] = {};
static const unsigned char DTA_T2T_CMD_4_REQ[] = {};

static void T2TCmdRsp_4(void* pCallbackParameter, unsigned long nDataLength, unsigned long nResult)
{
    unsigned char *pRspBuffer = (unsigned char *)pCallbackParameter;

    LOGD("%s: nDataLength(%ld), nResult(0x%lX)", __FUNCTION__, nDataLength, nResult);
    DtaPrintBuf("pRspBuffer:", pRspBuffer, nDataLength);
    if(DtaGetErrorCode(nResult) != DTA_SUCCESS)
    {
        goto end;
    }

end:
    DtaFlowTestEnd();
}

static void T2TCmdRsp_3(void* pCallbackParameter, unsigned long nDataLength, unsigned long nResult)
{
    unsigned char *pRspBuffer = (unsigned char *)pCallbackParameter;
    int doNext = 0;

    LOGD("%s: nDataLength(%ld), nResult(0x%lX)", __FUNCTION__, nDataLength, nResult);
    DtaPrintBuf("pRspBuffer:", pRspBuffer, nDataLength);

    if(sizeof(DTA_T2T_CMD_3_RSP) == 0)
    {
        goto end;
    }

    if(DtaGetErrorCode(nResult) != DTA_SUCCESS)
    {
        goto end;
    }

    //compare OK
    if(memcmp(pRspBuffer, DTA_T2T_CMD_3_RSP, sizeof(DTA_T2T_CMD_3_RSP)) == 0)
    {
        if(sizeof(DTA_T2T_CMD_4_REQ) == 0)
        {
            goto end;
        }
        doNext = DtaRfCmd(DTA_T2T_CMD_4_REQ,
                          sizeof(DTA_T2T_CMD_4_REQ),
                          T2TCmdRsp_4);
        goto end;
    }


end:

    if(0 == doNext)
        DtaFlowTestEnd();
}

static void T2TCmdRsp_2(void* pCallbackParameter, unsigned long nDataLength, unsigned long nResult)
{
    unsigned char *pRspBuffer = (unsigned char *)pCallbackParameter;
    int doNext = 0;

    LOGD("%s: nDataLength(%ld), nResult(0x%lX)", __FUNCTION__, nDataLength, nResult);
    DtaPrintBuf("pRspBuffer:", pRspBuffer, nDataLength);

    if(sizeof(DTA_T2T_CMD_2_RSP) == 0)
    {
        goto end;
    }

    if(DtaGetErrorCode(nResult) != DTA_SUCCESS)
    {
        goto end;
    }

    //compare OK
    if(memcmp(pRspBuffer, DTA_T2T_CMD_2_RSP, sizeof(DTA_T2T_CMD_2_RSP)) == 0)
    {
        if(sizeof(DTA_T2T_CMD_3_REQ) == 0 )
        {
            goto end;
        }
        doNext = DtaRfCmd(DTA_T2T_CMD_3_REQ,
                          sizeof(DTA_T2T_CMD_3_REQ),
                          T2TCmdRsp_3);
        goto end;
    }

end:

    if(0 == doNext)
        DtaFlowTestEnd();
}


static void T2TCmdRsp_1(void* pCallbackParameter, unsigned long nDataLength, unsigned long nResult)
{
    unsigned char *pRspBuffer = (unsigned char *)pCallbackParameter;
    int doNext = 0;

    LOGD("%s: nDataLength(%ld), nResult(0x%lX)", __FUNCTION__, nDataLength, nResult);
    DtaPrintBuf("pRspBuffer:", pRspBuffer, nDataLength);

    if(sizeof(DTA_T2T_CMD_1_RSP) == 0)
    {
        goto end;
    }

    if(DtaGetErrorCode(nResult) != DTA_SUCCESS)
    {
        goto end;
    }

    //compare OK
    if(memcmp(pRspBuffer, DTA_T2T_CMD_1_RSP, sizeof(DTA_T2T_CMD_1_RSP)) == 0)
    {
        if(sizeof(DTA_T2T_CMD_2_REQ) == 0)
        {
            goto end;
        }
        doNext = DtaRfCmd(DTA_T2T_CMD_2_REQ,
                          sizeof(DTA_T2T_CMD_2_REQ),
                          T2TCmdRsp_2);
        goto end;
    }

end:

    if(0 == doNext)
        DtaFlowTestEnd();
}


static void T2TCmdRsp_0(void* pCallbackParameter, unsigned long nDataLength, unsigned long nResult)
{
    unsigned char *pRspBuffer = (unsigned char *)pCallbackParameter;
    int doNext = 0;

    LOGD("%s: nDataLength(%ld), nResult(0x%lX)", __FUNCTION__, nDataLength, nResult);
    DtaPrintBuf("pRspBuffer:", pRspBuffer, nDataLength);

    if(DtaGetErrorCode(nResult) != DTA_SUCCESS)
    {
        goto end;
    }

    //compare OK
    if(memcmp(pRspBuffer, DTA_T2T_CMD_0_RSP, sizeof(DTA_T2T_CMD_0_RSP)) == 0)
    {
        if(sizeof(DTA_T2T_CMD_1_REQ) == 0 )
        {
            goto end;
        }
        doNext = DtaRfCmd(DTA_T2T_CMD_1_REQ,
                          sizeof(DTA_T2T_CMD_1_REQ),
                          T2TCmdRsp_1);
        goto end;
    }


end:

    if(0 == doNext)
        DtaFlowTestEnd();
}

int T2TPlatformTest(void)
{
    int send_len = 0;

    transferMessageToJava((char*)"T2TPlatformTest");
    LOGD("%s:", __FUNCTION__);
    if(sizeof(DTA_T2T_CMD_0_REQ) == 0 )
    {
         DtaFlowTestEnd();
    }
    else
    {
        send_len = DtaRfCmd(DTA_T2T_CMD_0_REQ,
                            sizeof(DTA_T2T_CMD_0_REQ),
                            T2TCmdRsp_0);
    
        if(0 == send_len)
        {
            DtaFlowTestEnd();
        }
    }

    return send_len;
}


}

