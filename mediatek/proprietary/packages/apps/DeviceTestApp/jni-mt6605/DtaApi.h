#ifndef __DTA_API_H__
#define __DTA_API_H__

#include "DtaFlow.h"

namespace android {

int DtaInit(void);
int DtaDeinit(void);
int DtaEnableDiscovery(void);
int DtaDisableDiscovery(void);
int DtaSetDeviceDtaMode(int mode);
int DtaSetDeviceDtaQuickMode(int mode);
int DtaIsoDslCmd(DtaCallbackFunction *pCallback);
int DtaRfCmd(const unsigned char *cmd, unsigned char cmd_len, DtaCallbackFunction *pCallback);
int DtaNfcDepExchangeData(const unsigned char *cmd, unsigned char cmd_len, DtaCallbackFunction *pCallback);
int DtaNfcDslCmd(DtaCallbackFunction *pCallback);
int DtaNfcRlsCmd(DtaCallbackFunction *pCallback);
int DtaIsoDepExchangeData(const unsigned char *cmd, unsigned char cmd_len, DtaCallbackFunction *pCallback);
DTA_ERROR_CODE DtaGetErrorCode(unsigned long error);

int DtaSetListenConfig(int  seld, int uidLevel, int did, int fsci);
int DtaNormalFlowSetPatternNum(int patternNumber);  // for NDEF
int DtaSetConfigPath(unsigned char * path, unsigned int length); //for set config path
}

#endif  //__DTA_API_H__

