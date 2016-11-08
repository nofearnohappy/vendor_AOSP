
#include "com_mediatek_nfc_dta.h"
#include "DtaFlow.h"
#include "DtaApi.h"

namespace android {

static DTA_TEST_TYPE dtaTestType = DTA_TEST_PLATFORM;
static DTA_Config_Para_t *DTA_Config_Current = NULL;
const DTA_Config_Para_t DTA_ConfigParaTbl[] =
{
    {0x00, 1, 0, 0, 1, 1, DTA_SENSF_REQ_NA, DTA_SENSF_REQ_NA, DTA_Reactivation_No},// 0
    {0x01, 1, 0, 0, 1, 1, DTA_SENSF_REQ_0, DTA_SENSF_REQ_NA, DTA_Reactivation_No},// 1
    {0x02, 1, 0, 0, 1, 1, DTA_SENSF_REQ_0, DTA_SENSF_REQ_NA, DTA_Reactivation_No},// 2
    {0x03, 1, 0, 0, 1, 1, DTA_SENSF_REQ_0, DTA_SENSF_REQ_NA, DTA_Reactivation_No},// 3
    {0x04, 1, 0, 0, 1, 1, DTA_SENSF_REQ_0, DTA_SENSF_REQ_NA, DTA_Reactivation_No},// 4
    {0x05, 1, 0, 0, 1, 1, DTA_SENSF_REQ_0, DTA_SENSF_REQ_NA, DTA_Reactivation_No},// 5
    {0x06, 1, 0, 1, 1, 1, DTA_SENSF_REQ_0, DTA_SENSF_REQ_NA, DTA_Reactivation_No},// 6
    {0x07, 1, 0, 0, 1, 1, DTA_SENSF_REQ_1,  DTA_SENSF_REQ_2,  DTA_Reactivation_Yes},// 7
    {0x08, 1, 0, 1, 1, 1, DTA_SENSF_REQ_1,  DTA_SENSF_REQ_2,  DTA_Reactivation_NFC_F},// 8
    {0x09, 1, 3, 0, 1, 1, DTA_SENSF_REQ_NA, DTA_SENSF_REQ_NA, DTA_Reactivation_No},// 9
    {0x0A, 0, 0, 1, 1, 1, DTA_SENSF_REQ_NA, DTA_SENSF_REQ_NA, DTA_Reactivation_No},// A
    {0x0B, 0, 0, 0, 1, 1, DTA_SENSF_REQ_NA, DTA_SENSF_REQ_NA, DTA_Reactivation_No},// B
    {0x0C, 1, 0, 0, 0, 1, DTA_SENSF_REQ_NA, DTA_SENSF_REQ_NA, DTA_Reactivation_No},// C
    {0x0D, 1, 0, 0, 1, 0, DTA_SENSF_REQ_NA, DTA_SENSF_REQ_NA, DTA_Reactivation_No} // D
};

void DtaPrintBuf(const char *message ,unsigned char *print_buf, int length)
{
    char * temp = new char[ (length + 1)*3];
    char ascii_chars[] = "0123456789ABCDEF";
    int i, j;

    for(i=0, j=0; i < length; i++) {
        temp[j++] = ascii_chars[ ( print_buf[i] >> 4 ) & 0x0F ];
        temp[j++] = ascii_chars[ print_buf[i] & 0x0F ];
        temp[j++] = ' ';
    }
    temp[j] = '\0';
    LOGD("%s : %s", message, temp);
    delete [] temp;
}

void DtaFlowTestEnd(void)
{
    transferMessageToJava((char*)"Next Loop");

    DtaDisableDiscovery();
    DtaEnableDiscovery();

    //switchTestState();
}

int DtaSetConfig(int pattern_num)
{
    if ( 0 <= pattern_num && pattern_num <= 0x000D)
    {
        DTA_Config_Current = (DTA_Config_Para_t*)&DTA_ConfigParaTbl[pattern_num];
        return 0;
    }

    LOGE("%s : pattern number fail 0x%x", __FUNCTION__, pattern_num);
    return -1;
}

DTA_Config_Para_t* DtaGetConfig(void)
{
    return DTA_Config_Current;
}

int DtaSetType(DTA_TEST_TYPE type)
{
    dtaTestType = type;
    return 0;
}

DTA_TEST_TYPE DtaGetType(void)
{
    return dtaTestType;
}

}

