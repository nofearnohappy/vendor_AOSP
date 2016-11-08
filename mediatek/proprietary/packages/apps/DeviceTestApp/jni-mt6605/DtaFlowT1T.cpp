
#include "com_mediatek_nfc_dta.h"
#include "DtaFlow.h"
#include "DtaApi.h"

namespace android {

int T1TPlatformTest(void)
{
    int send_len = 0;

    //T1T_PLATFORM_RSEG = true, sub-case y=3 shall be performed
    //OpenNFC protocol will do T1T platform test
    transferMessageToJava((char*)"T1TPlatformTest");            
    DtaFlowTestEnd();

    return send_len;
}

 
}


