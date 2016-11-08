
#include "com_mediatek_nfc_dta.h"
#include "DtaFlow.h"
#include "DtaApi.h"

namespace android {

static const unsigned char DTA_T4T_SOT_REQ[] = {0x00,0xA4,0x04,0x00,0x0E,0x32,0x4E,0x46,
                                                0x43,0x2E,0x53,0x59,0x53,0x2E,0x44,0x44,
                                                0x46,0x30,0x31,0x00};

static const unsigned char DTA_T4T_EOT_RSP[] = {0xFF,0xFF,0xFF,0x01,0x01,0x90,0x00};

static const unsigned char DTA_T4T_RAPDU_RSP_TAIL[] = {0x90,0x00};

static int deselect_retry = 0;

#define DESEL_RETRY_MAX (2)

static void T4TDeselectRsp(void* pCallbackParameter, unsigned long nDataLength, unsigned long nResult)
{
    unsigned char *pRspBuffer = (unsigned char *)pCallbackParameter;
    int doNext = 0;
    
	LOGD("%s: nDataLength(%ld), nResult(0x%lX)", __FUNCTION__, nDataLength, nResult);
    DtaPrintBuf("pRspBuffer:", pRspBuffer, nDataLength);

//middleware handle
/*
	if(DtaGetErrorCode(nResult) != DTA_SUCCESS)
    {
        if(deselect_retry < DESEL_RETRY_MAX)
        {    
            //DESELECT again
            if(0 == DtaIsoDslCmd(T4TDeselectRsp))
                goto end;
        
            deselect_retry ++;
            doNext = 1;
            goto end;
        }

        goto end;
	}
*/
end:

    if(0 == doNext)
        DtaFlowTestEnd();
}

static void T4TLoopbackRsp(void* pCallbackParameter, unsigned long nDataLength, unsigned long nResult)
{
    unsigned char *pRspBuffer = (unsigned char *)pCallbackParameter;
    int doNext = 0;
    int tail_len = sizeof(DTA_T4T_RAPDU_RSP_TAIL);
    
	LOGD("%s: nDataLength(%ld), nResult(0x%lX)", __FUNCTION__, nDataLength, nResult);
    DtaPrintBuf("pRspBuffer:", pRspBuffer, nDataLength);

	if(DtaGetErrorCode(nResult) != DTA_SUCCESS)
    {
		goto end;
	}

    //compare EOT
    if(memcmp(pRspBuffer, DTA_T4T_EOT_RSP, sizeof(DTA_T4T_EOT_RSP)) == 0)
    {        
        //DESELECT
        if(0 == DtaIsoDslCmd(T4TDeselectRsp))
            goto end;

        deselect_retry = 0;        
        doNext = 1;       
		goto end;
    }

	if(nDataLength <= (unsigned long)tail_len)
    {
		goto end;
	}    
    
    if(memcmp(&pRspBuffer[nDataLength-tail_len], DTA_T4T_RAPDU_RSP_TAIL, tail_len) == 0)
    {
        //Loop back
        doNext = 1;

        DtaIsoDepExchangeData((const unsigned char*)pRspBuffer,
                                 nDataLength-tail_len,  //drop 0x90 0x00
                                 T4TLoopbackRsp);
        
		goto end;
    }
    
end:

    if(0 == doNext)
        DtaFlowTestEnd();
}


int T4TPlatformTest()
{
    int send_len =0;

    transferMessageToJava((char*)"T4TPlatformTest");
    if(sizeof(DTA_T4T_SOT_REQ)== 0)
    {
        DtaFlowTestEnd();
    }
    else
    {
        send_len = DtaIsoDepExchangeData(DTA_T4T_SOT_REQ,
                                        sizeof(DTA_T4T_SOT_REQ),
                                        T4TLoopbackRsp); 

        if(0 == send_len)
        {
            DtaFlowTestEnd();
        }
    }
    return send_len;
}

}

