
#include "com_mediatek_nfc_dta.h"
#include "DtaFlow.h"
#include "DtaApi.h"

typedef struct s_mtk_nfc_dta_listen_config
{
    unsigned long   Seld;                   // SWIO1:1, SWIO2:2, SWIO3:3
    unsigned long   UIDLevel;            /* 1~3 */
    unsigned long   DIDSupport;       /* 1:on ; 0:off*/
    unsigned long   FSCI;                 /* 0~8 */
}s_mtk_nfc_dta_listen_config_t;

namespace android
{

//============================================================================
// MT6605 JNI exported APIs
//============================================================================
extern int dta_nfc_jni_initialize(void *handler);
extern int dta_nfc_jni_deinitialize(void);
extern void dta_nfc_jni_set_dta_mode(bool flag);
extern void dta_nfc_jni_set_dta_quick_mode(int flag);
extern void dta_NormalFlow_Set_PatternNum(unsigned int Num);  ///for NDEF test
extern int dta_nfc_jni_set_config_path(unsigned char * path, unsigned int length); //set config path for operation test @mingyen

//extern int dta_doTransceive(unsigned char *cmd, int *cmd_len);
extern int JniDtaRfCmd(const unsigned char *cmd, unsigned char cmd_len, unsigned char** rsp_cmd, unsigned long *rsp_cmd_len);
extern int JniDtaDepExchange(const unsigned char *cmd, unsigned char cmd_len, unsigned char* rsp_cmd, unsigned long *rsp_cmd_len);
extern bool JniDtaEnableDiscovery(unsigned char test_type, int pattern_num);
//extern bool JniDtaEnableDiscovery(unsigned char test_type, int pattern_num, s_mtk_nfc_dta_listen_config_t* p_listen_config);
extern bool JniDtaDisableDiscovery(void);
extern int JniDtaIsoDslCmd(void);
extern int JniDtaNfcDslCmd(void);
extern int JniDtaNfcRlsCmd(void);
//============================================================================
// END
//============================================================================

static s_mtk_nfc_dta_listen_config DTA_Listen_Config;

int DtaIsoDslCmd(DtaCallbackFunction *pCallback)
{
    int rtn = 0;
    unsigned long nStatus = 0;
    void* ptr = NULL;

    LOGD("%s:", __FUNCTION__);

    rtn = JniDtaIsoDslCmd();

    if (0 == rtn ) // success
    {
        nStatus = 0x0000;
    }
    else if (0x0D == rtn) // timeout
    {
        nStatus = 0x0001;
    }
    else
    {
        nStatus = 0x0002;
    }
    pCallback(ptr,0,nStatus);
    return 1;
}

int DtaRfCmd(const unsigned char *cmd, unsigned char cmd_len, DtaCallbackFunction *pCallback)
{
    unsigned char* pRspBuf = NULL;
    unsigned long RspBufLen = 0;
    int result = 0;

    LOGD("%s:", __FUNCTION__);

    result = JniDtaRfCmd(cmd,cmd_len,&pRspBuf,&RspBufLen);
    LOGD("response buffer [%p]:length(%lu)", pRspBuf,RspBufLen);
    pCallback((void*)pRspBuf, RspBufLen, result);
    return cmd_len;
}

int DtaNfcDepExchangeData(const unsigned char *cmd, unsigned char cmd_len, DtaCallbackFunction *pCallback)
{
    unsigned char RspBuf[1024];
    unsigned long RspBufLen = 0;
    unsigned long nStatus = 0;
    int result = 0;

    LOGD("%s:", __FUNCTION__);
    LOGD("request buffer [%p]:length(%ld)", cmd, (unsigned long)cmd_len);

    memset(RspBuf, 0x00, 1024);
    result = JniDtaDepExchange(cmd,cmd_len,RspBuf,&RspBufLen);

    LOGD("response buffer [%p]:length(%ld),%d", RspBuf,RspBufLen, result);

    if (result == 1) // success
    {
        nStatus = 0x0000;
    }
    else
    {
        nStatus = 0x0001;
    }
    pCallback((void*)RspBuf, RspBufLen, nStatus);

    RspBufLen = 0;
    return cmd_len;
}

int DtaNfcDslCmd(DtaCallbackFunction *pCallback)
{
    int rtn = 0;
    unsigned long nStatus = 0;
    void* ptr = NULL;

    LOGD("%s:", __FUNCTION__);

    rtn = JniDtaNfcDslCmd();

    if (0 == rtn ) // success
    {
        nStatus = 0x0000;
    }
    else if (0x0D == rtn) // timeout
    {
        nStatus = 0x0001;
    }
    else
    {
        nStatus = 0x0002;
    }
    pCallback(ptr,0,nStatus);
    return 1;
}

int DtaNfcRlsCmd(DtaCallbackFunction *pCallback)
{
    int rtn = 0;
    unsigned long nStatus = 0;
    void* ptr = NULL;

    LOGD("%s:", __FUNCTION__);

    rtn = JniDtaNfcRlsCmd();

    if (0 == rtn ) // success
    {
        nStatus = 0x0000;
    }
    else if (0x0D == rtn) // timeout
    {
        nStatus = 0x0001;
    }
    else
    {
        nStatus = 0x0002;
    }
    pCallback(ptr,0,nStatus);
    return 1;
}


int DtaIsoDepExchangeData(const unsigned char *cmd, unsigned char cmd_len, DtaCallbackFunction *pCallback)
{
#if 0
    int len = cmd_len;
    int result = false;
    unsigned char *TransceiveCmd;
    LOGD("%s:", __FUNCTION__);

    TransceiveCmd = (unsigned char *)cmd;

    LOGD("DtaIsoDepExchangeData,Len(%d)",len);
    result = dta_doTransceive(TransceiveCmd, &len);
    LOGD("DtaIsoDepExchangeData,result(%d),Receive_Len(%d)",result,len);

    /*
    typedef void DtaCallbackFunction(
                   void* pCallbackParameter,
                   unsigned long nDataLength,
                   unsigned long nResult );

    */

    //if(result == TRUE)
    {
        pCallback(TransceiveCmd, len, result);
    }

    return len;
#else
    unsigned char* pRspBuf = NULL;
    unsigned long RspBufLen = 0;
    int result = 0;

    LOGD("%s:", __FUNCTION__);

    result = JniDtaRfCmd(cmd,cmd_len,&pRspBuf,&RspBufLen);
    LOGD("response buffer [%p]:length(%lu)", pRspBuf,RspBufLen);
    pCallback((void*)pRspBuf, RspBufLen, result);
    return cmd_len;
#endif
}

DTA_ERROR_CODE DtaGetErrorCode(unsigned long error)
{
    DTA_ERROR_CODE rtnCode = DTA_ERROR_FAIL;

    switch(error)
    {
    case 0x0000:
        rtnCode = DTA_SUCCESS;
        LOGD("Return Code %s", "DTA_SUCCESS");
        break;
    case 0x0001:
        rtnCode = DTA_ERROR_TIMEOUT;
        LOGD("Return Code %s", "DTA_ERROR_TIMEOUT");
        break;
    default:
        rtnCode = DTA_ERROR_FAIL;
        LOGD("Return Code %s", "DTA_ERROR_FAIL");
        break;
    }

    return rtnCode;
}

static void static_DetectionHandler(unsigned long type, unsigned long nError)
{
    LOGD("%s:", __FUNCTION__);

    switch(type)
    {
    case 0x0001:
        T1TPlatformTest();
        break;
    case 0x0002:
        T2TPlatformTest();
        break;
    case 0x0003:
        T3TPlatformTest();
        break;
    case 0x0004:
        T4TPlatformTest();
        break;
    case 0x0005:
        P2PTest(DTA_P2P_INITIATOR);
        break;
    case 0x0006:
        P2PTest(DTA_P2P_TARGET);
        break;
    default:
        DtaFlowTestEnd();
        break;
    }

}

int DtaInit(void)
{
    int retVal = 0;

    retVal = dta_nfc_jni_initialize((void *)&static_DetectionHandler);

    LOGD("%s:", __FUNCTION__);

    return retVal;
}

int DtaDeinit(void)
{
    int retVal = 0;

    retVal = dta_nfc_jni_deinitialize();

    LOGD("%s:", __FUNCTION__);

    return retVal;
}


int DtaEnableDiscovery(void)
{
    int retVal = 0;

    unsigned char pattern_num = DtaGetConfig()->pattern_num;
    DTA_TEST_TYPE type = DtaGetType();

    LOGD("MTK_NFC %s: type(0x%x) pattern(0x%x)", __FUNCTION__, type, pattern_num);

    switch(type)
    {
    case DTA_TEST_PLATFORM:
    case DTA_TEST_P2P:
    case DTA_TEST_LISTEN:
    case DTA_TEST_SWP:
        //if (!JniDtaEnableDiscovery((unsigned char)type,pattern_num, &DTA_Listen_Config))
        if (!JniDtaEnableDiscovery((unsigned char)type,pattern_num))
        {
            retVal = -1;
        }
        break;

    default:
        LOGW("MTK_NFC no support type 0x%X", type);
        transferMessageToJava((char*)"no support type");
        retVal = -1;
        break;
    }


end:
    return retVal;
}

int DtaDisableDiscovery(void)
{
    int retVal = 0;

    LOGD("%s:", __FUNCTION__);

    if (!JniDtaDisableDiscovery())
    {
        retVal = -1;
    }

    return retVal;
}

int DtaSetDeviceDtaMode(int mode)
{
    dta_nfc_jni_set_dta_mode(mode);
    if (mode == 0 )
    {
        LOGD("Disable");
        return mode;
    }
    else if (mode == 1)
    {
        LOGD("Enable");
        return mode;
    }

    return -1;
}

/**
 *  0: clear DTA quick config file
 *  1: init DTA quick config file
 * -1: forace clear nfcstackp & nfc service
 */
int DtaSetDeviceDtaQuickMode(int mode)
{
    LOGD("[QE]%s: DTA quick Mode %d", __FUNCTION__, mode);
    dta_nfc_jni_set_dta_quick_mode(mode);

    return 1;
}

int DtaSetListenConfig(int seld, int uidLevel, int did, int fsci)
{
    DTA_Listen_Config.Seld= seld;
    DTA_Listen_Config.UIDLevel = uidLevel;
    DTA_Listen_Config.DIDSupport = did;
    DTA_Listen_Config.FSCI = fsci;
    return 0;
}

int DtaNormalFlowSetPatternNum(int patternNumber)
{
    LOGD("%s: number %d", __FUNCTION__, patternNumber);

	dta_NormalFlow_Set_PatternNum(patternNumber);

	return patternNumber;
}

int DtaSetConfigPath(unsigned char * path, unsigned int length)
{
	LOGD("%s: config path %s, length, %d", __FUNCTION__, path, length);

	return dta_nfc_jni_set_config_path(path, length);
}

}

