#ifndef __DTA_FLOW_H__
#define __DTA_FLOW_H__

#include "DtaFlowT4T.h"
#include "DtaFlowT3T.h"
#include "DtaFlowT2T.h"
#include "DtaFlowT1T.h"
#include "DtaFlowP2P.h"

namespace android {

typedef void DtaCallbackFunction(
               void* pCallbackParameter,
               unsigned long nDataLength,
               unsigned long nResult );

typedef enum {
    DTA_SUCCESS,
    DTA_ERROR_FAIL,
    DTA_ERROR_TIMEOUT,

} DTA_ERROR_CODE;

typedef enum {
    DTA_TEST_PLATFORM   = 0x00,
    DTA_TEST_OPERATION  = 0x01,
    DTA_TEST_P2P        = 0x02,
    DTA_TEST_LISTEN     = 0x03,
    DTA_TEST_SWP        = 0x04,    
} DTA_TEST_TYPE;

typedef enum {
    DTA_SENSF_REQ_NA = 0,   //NA
    DTA_SENSF_REQ_0,        //12FC,00,00
    DTA_SENSF_REQ_1,        //FFFF,01,0F
    DTA_SENSF_REQ_2        //FFFF,00,03
}DTA_SENSF_REQ_et;


typedef enum {
    DTA_Reactivation_No = 0,
    DTA_Reactivation_Yes,
    DTA_Reactivation_NFC_F   //NFC-F only
}DTA_Reactivation_et;


typedef struct
{
    unsigned char   pattern_num;        //pattern number
    unsigned char   CON_POLL_B;
    unsigned char   CON_BITR_NFC_DEP;   //Desired bit rate: 0: maintain the bit rate, 1: 106 kbps, 2: 212 kbps, 3: 424 kbps
    unsigned char   NFC_F_BIT_RATE;     //0b: 212 kbps (default value), 1b: 424 kbps
    unsigned char   CON_LISTEN_DEP_F;
    unsigned char   CON_LISTEN_T3TP;    //1b in case the [ICS] indicates support for ¡§Listen for T3T platform¡¨, 0b otherwise
    DTA_SENSF_REQ_et   SENSF_REQ;
    DTA_SENSF_REQ_et   SENSF_REQ_React;
    DTA_Reactivation_et Reactivation;
}DTA_Config_Para_t;


void DtaPrintBuf(const char *message ,unsigned char *print_buf, int length);
void DtaFlowTestEnd(void);
int DtaSetConfig(int pattern_num);
DTA_Config_Para_t* DtaGetConfig(void);
int DtaSetType(DTA_TEST_TYPE type);
DTA_TEST_TYPE DtaGetType(void);


}

#endif  //__DTA_FLOW_H__

