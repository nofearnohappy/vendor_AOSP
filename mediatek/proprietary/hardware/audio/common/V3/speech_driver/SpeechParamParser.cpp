#include "SpeechParamParser.h"

#include <utils/Log.h>
#include "AudioUtility.h"//Mutex/assert
#include "AudioALSAStreamManager.h"

#define LOG_TAG "SpeechParamParser"
//#define APP_TEST

namespace android
{
#define XML_FOLDER "/system/etc/audio_param/"

#define MAX_BYTE_PARAM_SPEECH 4096

//--------------------------------------------------------------------------------
//audio type: Speech
#define MAX_NUM_CATEGORY_TYPE_SPEECH 4
#define MAX_NUM_PARAM_SPEECH 3
#define NUM_BAND_SPEECH 2
#define NUM_VOLUME_SPEECH 7
#define NUM_NETWORK_SPEECH 1
const String8 audioType_Speech_CategoryType[ ] = {String8("Band"), String8("Profile"), String8("VolIndex"), String8("Network")};
const char audioType_Speech_CategoryName1[NUM_BAND_SPEECH][128] = {"NB", "WB"};
const char audioType_Speech_CategoryName2[SPEECH_PROFILE_MAX_NUM][128] = {"Normal", "4_pole_Headset", "Handsfree", "BT_Earphone", "BT_NREC_Off", "MagiConference", "HAC", "Lpbk_Handset", "Lpbk_Headset", "Lpbk_Handsfree"};
const char audioType_Speech_CategoryName3[NUM_VOLUME_SPEECH][128] = {"0", "1", "2", "3", "4", "5", "6"};
const char audioType_Speech_CategoryName4[NUM_NETWORK_SPEECH][128] = {"GSM"};
const String8 audioType_Speech_ParamName[ ] = {String8("speech_mode_para"), String8("sph_in_fir"), String8("sph_out_fir")};

//--------------------------------------------------------------------------------
//audio type: SpeechDMNR
#define MAX_NUM_CATEGORY_TYPE_SPEECH_DMNR 2
#define MAX_NUM_PARAM_SPEECH_DMNR 1
const String8 audioType_SpeechDMNR_CategoryType[ ] = {String8("Band"), String8("Profile")};
const char audioType_SpeechDMNR_CategoryName1[2][128] = {"NB", "WB"};
const char audioType_SpeechDMNR_CategoryName2[2][128] = {"Handset", "MagiConference"};
const String8 audioType_SpeechDMNR_ParamName[ ] = {String8("dmnr_para")};
const char audioType_SpeechDMNR_NumPerCategory[2] = {2, 2};

//--------------------------------------------------------------------------------
//audio type: SpeechGeneral
#define MAX_NUM_CATEGORY_TYPE_SPEECH_GENERAL 1
#define MAX_NUM_PARAM_SPEECH_GENERAL 2
const String8 audioType_SpeechGeneral_CategoryType[ ] = {String8("CategoryLayer")};
const char audioType_SpeechGeneral_CategoryName1[1][128] = {"Common"};
const String8 audioType_SpeechGeneral_ParamName[ ] = {String8("speech_common_para"), String8("debug_info")};

//--------------------------------------------------------------------------------
//audio type: SpeechMagiClarity
#define MAX_NUM_CATEGORY_TYPE_SPEECH_MAGICLARITY 1
#define MAX_NUM_PARAM_SPEECH_MAGICLARITY 1
const String8 audioType_SpeechMagiClarity_CategoryType[ ] = {String8("CategoryLayer")};
const char audioType_SpeechMagiClarity_CategoryName1[1][128] = {"Common"};
const String8 audioType_SpeechMagiClarity_ParamName[ ] = {String8("shape_rx_fir_para")};



//--------------------------------------------------------------------------------
const unsigned short nb_speech_mode_para_table[6][48] =
{
    0x60, 0xFD, 0x4004, 0x1F, 0xE007, 0x31F, 0x190, 0x40, 0x50, 0x10E5, 0x263, 0x0, 0x5008, 0x0, 0x0, 0x2000, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0,
    0x0, 0xBD, 0x2A04, 0x1F, 0xE007, 0x1F, 0x190, 0x40, 0x50, 0x10E5, 0x263, 0x0, 0x5008, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0,
    0x60, 0xE0, 0x1488, 0x1F, 0xE007, 0x601F, 0x190, 0x84, 0x54, 0x10E5, 0x263, 0x0, 0x5008, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0,
    0x0, 0xFD, 0x2A04, 0x1F, 0xD007, 0x1F, 0x190, 0x0, 0x50, 0x10E5, 0x263, 0x0, 0xD008, 0x0, 0x0, 0x56, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0,
    0x60, 0xE0, 0x1488, 0x1F, 0xE007, 0x601F, 0x190, 0x84, 0x54, 0x10E5, 0x263, 0x0, 0x2008, 0x373, 0x17, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0,
    0x60, 0xFD, 0x4004, 0x1F, 0xE007, 0x31F, 0x190, 0x40, 0x50, 0x10E5, 0x263, 0x0, 0x5008, 0x0, 0x0, 0x2000, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0
};
const unsigned short wb_speech_mode_para_table[6][48] =
{
    0x60, 0xFD, 0x4004, 0x1F, 0xE107, 0x31F, 0x190, 0x40, 0x50, 0x10E5, 0x263, 0x0, 0x4008, 0x0, 0x0, 0x2000, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0,
    0x0, 0xBD, 0x2A04, 0x1F, 0xE107, 0x1F, 0x190, 0x40, 0x50, 0x10E5, 0x263, 0x0, 0x4008, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0,
    0x60, 0xE0, 0x1488, 0x1F, 0xE107, 0x601F, 0x190, 0x84, 0x54, 0x10E5, 0x263, 0x0, 0x4008, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0,
    0x0, 0xFD, 0x2A04, 0x1F, 0xD107, 0x1F, 0x190, 0x0, 0x50, 0x10E5, 0x263, 0x0, 0xC008, 0x0, 0x0, 0x56, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0,
    0x60, 0xE0, 0x1488, 0x1F, 0xE107, 0x601F, 0x190, 0x84, 0x54, 0x10E5, 0x263, 0x0, 0x2008, 0x373, 0x17, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0,
    0x60, 0xFD, 0x4004, 0x1F, 0xE107, 0x31F, 0x190, 0x40, 0x50, 0x10E5, 0x263, 0x0, 0x4008, 0x0, 0x0, 0x2000, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0

};

const short nb_sph_in_fir_table[6][45] = {32767, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                                          0xFC69, 0x3C7, 0xFC7F, 0x37F, 0xFD33, 0x24A, 0xFD26, 0x39B, 0xFC7A, 0x3D6, 0xFB7D, 0x514, 0xFA95, 0x60D, 0xF96F, 0x7BA, 0xF53E, 0xF26, 0xEDF8, 0x1383, 0xE40C, 0x5A9D, 0x5A9D, 0xE40C, 0x1383, 0xEDF8, 0xF26, 0xF53E, 0x7BA, 0xF96F, 0x60D, 0xFA95, 0x514, 0xFB7D, 0x3D6, 0xFC7A, 0x39B, 0xFD26, 0x24A, 0xFD33, 0x37F, 0xFC7F, 0x3C7, 0xFC69, 0x0,
                                          0xFECE, 0x139, 0xFEA0, 0x15D, 0xFF31, 0xF1, 0xFEC6, 0xCF, 0xFEBC, 0x1F3, 0xFF36, 0xF1, 0xFD29, 0x290, 0xFD62, 0x482, 0xFD53, 0x220, 0xF8B6, 0x3CF, 0xFEDE, 0x50C3, 0x50C3, 0xFEDE, 0x3CF, 0xF8B6, 0x220, 0xFD53, 0x482, 0xFD62, 0x290, 0xFD29, 0xF1, 0xFF36, 0x1F3, 0xFEBC, 0xCF, 0xFEC6, 0xF1, 0xFF31, 0x15D, 0xFEA0, 0x139, 0xFECE, 0x0,
                                          0xC6, 0xFF7B, 0x170, 0xFF26, 0x30, 0xFE7F, 0xFEFE, 0xFF4E, 0xFF4F, 0x198, 0xFF3D, 0x2AA, 0xFD41, 0x492, 0xFC97, 0x895, 0xF96B, 0xA87, 0xEF40, 0x12BA, 0xDB8E, 0x6A76, 0x6A76, 0xDB8E, 0x12BA, 0xEF40, 0xA87, 0xF96B, 0x895, 0xFC97, 0x492, 0xFD41, 0x2AA, 0xFF3D, 0x198, 0xFF4F, 0xFF4E, 0xFEFE, 0xFE7F, 0x30, 0xFF26, 0x170, 0xFF7B, 0xC6, 0x0,
                                          0xE4, 0xFF1D, 0x209, 0x4D, 0x29F, 0x90, 0x1AD, 0xB0, 0xFF03, 0x15E, 0xFE58, 0xC6, 0xF8D2, 0x15B, 0xF5F2, 0x476, 0xEDB1, 0x346, 0xE3AF, 0x1791, 0xBB4C, 0x65AC, 0x65AC, 0xBB4C, 0x1791, 0xE3AF, 0x346, 0xEDB1, 0x476, 0xF5F2, 0x15B, 0xF8D2, 0xC6, 0xFE58, 0x15E, 0xFF03, 0xB0, 0x1AD, 0x90, 0x29F, 0x4D, 0x209, 0xFF1D, 0xE4, 0x0,
                                          0xFBED, 0x31B, 0xFE33, 0x255, 0xFFB1, 0xFB0B, 0x98, 0xF678, 0x83F, 0xF5BD, 0x828, 0xF780, 0xFD7E, 0x304, 0xF729, 0x1287, 0xE850, 0x1153, 0xD2FE, 0x1291, 0xCE65, 0x5A9D, 0x5A9D, 0xCE65, 0x1291, 0xD2FE, 0x1153, 0xE850, 0x1287, 0xF729, 0x304, 0xFD7E, 0xF780, 0x828, 0xF5BD, 0x83F, 0xF678, 0x98, 0xFB0B, 0xFFB1, 0x255, 0xFE33, 0x31B, 0xFBED, 0x0

                                         };

const short nb_sph_out_fir_table[6][45] = {32767, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                                           0xFE50, 0x180, 0xFE51, 0x12F, 0xFDCB, 0x9, 0xFE4A, 0xC9, 0xFE82, 0x150, 0xFD80, 0x17E, 0xFCF4, 0x518, 0xFE5C, 0x67F, 0x3E6, 0x6D2, 0xF987, 0x10CA, 0xFBF2, 0x5A9D, 0x5A9D, 0xFBF2, 0x10CA, 0xF987, 0x6D2, 0x3E6, 0x67F, 0xFE5C, 0x518, 0xFCF4, 0x17E, 0xFD80, 0x150, 0xFE82, 0xC9, 0xFE4A, 0x9, 0xFDCB, 0x12F, 0xFE51, 0x180, 0xFE50, 0x0,
                                           0x9B, 0xFED0, 0xB8, 0xFF0D, 0xFFFA, 0xFF19, 0xFFDD, 0xFFD0, 0x24, 0x1C6, 0xFE08, 0x1BB, 0xFFBB, 0x4F4, 0xFAD7, 0x785, 0xFBE2, 0x537, 0xEA2D, 0x102F, 0xE591, 0x6118, 0x6118, 0xE591, 0x102F, 0xEA2D, 0x537, 0xFBE2, 0x785, 0xFAD7, 0x4F4, 0xFFBB, 0x1BB, 0xFE08, 0x1C6, 0x24, 0xFFD0, 0xFFDD, 0xFF19, 0xFFFA, 0xFF0D, 0xB8, 0xFED0, 0x9B, 0x0,
                                           0x50C3, 0xEC22, 0x217, 0xF746, 0xFF64, 0xF28F, 0xFBD7, 0xFA64, 0xF9AB, 0xF9B8, 0xFA57, 0xFB04, 0xFC57, 0xFC88, 0xFDBE, 0xFF5D, 0xFEFD, 0xFF63, 0x2A, 0xFF91, 0x16, 0x5F, 0xFF8F, 0xFC, 0x16, 0x88, 0xE1, 0xA3, 0x133, 0x10E, 0x153, 0x138, 0xE7, 0xF0, 0x118, 0xD8, 0xC9, 0xF4, 0xDB, 0x7B, 0xB4, 0xFFEB, 0xFFDF, 0xFFB9, 0xFF1D,
                                           0xFE6C, 0x39, 0xFDBA, 0xFEBB, 0xFEFB, 0xFCEE, 0x1E2, 0xFC40, 0x45D, 0xFCD8, 0x2FD, 0xFF02, 0x4D, 0xA3E, 0xFA09, 0x1465, 0xF996, 0x1076, 0xE23A, 0x16F4, 0xDF7E, 0x5A9D, 0x5A9D, 0xDF7E, 0x16F4, 0xE23A, 0x1076, 0xF996, 0x1465, 0xFA09, 0xA3E, 0x4D, 0xFF02, 0x2FD, 0xFCD8, 0x45D, 0xFC40, 0x1E2, 0xFCEE, 0xFEFB, 0xFEBB, 0xFDBA, 0x39, 0xFE6C, 0x0,
                                           0xFEF4, 0xFE46, 0xFFDE, 0xFE0A, 0xFF41, 0xFDB7, 0xFF5D, 0xFD52, 0xFD48, 0xFDCC, 0xFB7C, 0xFEA8, 0xF9BA, 0x612, 0xF9B7, 0xC65, 0xF7E8, 0xD2E, 0xEC61, 0xF76, 0xED35, 0x50C3, 0x50C3, 0xED35, 0xF76, 0xEC61, 0xD2E, 0xF7E8, 0xC65, 0xF9B7, 0x612, 0xF9BA, 0xFEA8, 0xFB7C, 0xFDCC, 0xFD48, 0xFD52, 0xFF5D, 0xFDB7, 0xFF41, 0xFE0A, 0xFFDE, 0xFE46, 0xFEF4, 0x0

                                          };

const short wb_sph_in_fir_table[6][90] = {32767, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                                          0x1A, 0xF, 0xFFC8, 0x1B, 0xFFEF, 0x27, 0xFFEE, 0xFFD4, 0x28, 0x5, 0x26, 0xFFC1, 0x2F, 0xFFB1, 0x34, 0xFFEA, 0x3E, 0xFFD1, 0x4, 0x18, 0xFFC9, 0x2E, 0xFFE4, 0x7B, 0xFEED, 0xF1, 0xFF30, 0x148, 0xFF0C, 0xFFF3, 0x62, 0xB0, 0xFF94, 0xFD16, 0x1DC, 0x6F, 0x67D, 0xF7A8, 0xCE, 0xF955, 0x1555, 0xE903, 0xACA, 0xFAB6, 0x4CC8, 0x4CC8, 0xFAB6, 0xACA, 0xE903, 0x1555, 0xF955, 0xCE, 0xF7A8, 0x67D, 0x6F, 0x1DC, 0xFD16, 0xFF94, 0xB0, 0x62, 0xFFF3, 0xFF0C, 0x148, 0xFF30, 0xF1, 0xFEED, 0x7B, 0xFFE4, 0x2E, 0xFFC9, 0x18, 0x4, 0xFFD1, 0x3E, 0xFFEA, 0x34, 0xFFB1, 0x2F, 0xFFC1, 0x26, 0x5, 0x28, 0xFFD4, 0xFFEE, 0x27, 0xFFEF, 0x1B, 0xFFC8, 0xF, 0x1A,
                                          0xFFF3, 0x29, 0xFFC9, 0x2E, 0xFFD5, 0xFFF6, 0xFFF7, 0xFFDF, 0x58, 0xFFE4, 0x5F, 0xFFA8, 0x7D, 0xFF86, 0x28, 0xFF8A, 0xFFE6, 0xE, 0xFFDC, 0x70, 0xFFE5, 0x131, 0xFF78, 0xD1, 0xFDFF, 0xAB, 0xFEFA, 0xCA, 0xFFAC, 0xFFB5, 0xF1, 0xFF85, 0x259, 0xFE09, 0x38D, 0xFB9F, 0x33E, 0xFA09, 0x24E, 0xFAD2, 0x62C, 0xF97E, 0xCC6, 0x1101, 0x372E, 0x372E, 0x1101, 0xCC6, 0xF97E, 0x62C, 0xFAD2, 0x24E, 0xFA09, 0x33E, 0xFB9F, 0x38D, 0xFE09, 0x259, 0xFF85, 0xF1, 0xFFB5, 0xFFAC, 0xCA, 0xFEFA, 0xAB, 0xFDFF, 0xD1, 0xFF78, 0x131, 0xFFE5, 0x70, 0xFFDC, 0xE, 0xFFE6, 0xFF8A, 0x28, 0xFF86, 0x7D, 0xFFA8, 0x5F, 0xFFE4, 0x58, 0xFFDF, 0xFFF7, 0xFFF6, 0xFFD5, 0x2E, 0xFFC9, 0x29, 0xFFF3,
                                          0xFFD1, 0xFFD9, 0x2, 0xFFF6, 0xFFAE, 0x11, 0x2, 0xFFF9, 0x13, 0x2D, 0x4C, 0xE, 0x97, 0x51, 0xF9, 0x42, 0x6A, 0x16, 0xFFDE, 0xFF47, 0xFEE2, 0xFFD7, 0xFF74, 0xFFC8, 0xFF3F, 0x7A, 0xFF5B, 0x104, 0xFF7B, 0x2E5, 0xFFC5, 0xFED8, 0x98, 0x36F, 0x25C, 0xF7ED, 0x4CE, 0xFD73, 0x49A, 0xEBA5, 0xBB5, 0xF99E, 0x5AC, 0xEBBE, 0x5E69, 0x5E69, 0xEBBE, 0x5AC, 0xF99E, 0xBB5, 0xEBA5, 0x49A, 0xFD73, 0x4CE, 0xF7ED, 0x25C, 0x36F, 0x98, 0xFED8, 0xFFC5, 0x2E5, 0xFF7B, 0x104, 0xFF5B, 0x7A, 0xFF3F, 0xFFC8, 0xFF74, 0xFFD7, 0xFEE2, 0xFF47, 0xFFDE, 0x16, 0x6A, 0x42, 0xF9, 0x51, 0x97, 0xE, 0x4C, 0x2D, 0x13, 0xFFF9, 0x2, 0x11, 0xFFAE, 0xFFF6, 0x2, 0xFFD9, 0xFFD1,
                                          0x99, 0xB0, 0xFF55, 0xFF2C, 0xFEFF, 0x32, 0xE0, 0x9B, 0x11E, 0xFEC5, 0x8, 0xFF74, 0x56, 0xFF21, 0x3D, 0x35, 0x57, 0x125, 0xFF44, 0xFEFD, 0xFF84, 0x2D4, 0xFE60, 0x1F6, 0xFBE7, 0xFFBA, 0xFE69, 0x508, 0x490, 0xFF0F, 0xFCEC, 0xF423, 0x496, 0x426, 0xE58, 0xF9C4, 0xF6E6, 0xF013, 0x11EF, 0x348, 0x16DC, 0xE266, 0xF552, 0xF398, 0x7FFF, 0x7FFF, 0xF398, 0xF552, 0xE266, 0x16DC, 0x348, 0x11EF, 0xF013, 0xF6E6, 0xF9C4, 0xE58, 0x426, 0x496, 0xF423, 0xFCEC, 0xFF0F, 0x490, 0x508, 0xFE69, 0xFFBA, 0xFBE7, 0x1F6, 0xFE60, 0x2D4, 0xFF84, 0xFEFD, 0xFF44, 0x125, 0x57, 0x35, 0x3D, 0xFF21, 0x56, 0xFF74, 0x8, 0xFEC5, 0x11E, 0x9B, 0xE0, 0x32, 0xFEFF, 0xFF2C, 0xFF55, 0xB0, 0x99,
                                          0x99, 0xB0, 0xFF55, 0xFF2C, 0xFEFF, 0x32, 0xE0, 0x9B, 0x11E, 0xFEC5, 0x8, 0xFF74, 0x56, 0xFF21, 0x3D, 0x35, 0x57, 0x125, 0xFF44, 0xFEFD, 0xFF84, 0x2D4, 0xFE60, 0x1F6, 0xFBE7, 0xFFBA, 0xFE69, 0x508, 0x490, 0xFF0F, 0xFCEC, 0xF423, 0x496, 0x426, 0xE58, 0xF9C4, 0xF6E6, 0xF013, 0x11EF, 0x348, 0x16DC, 0xE266, 0xF552, 0xF398, 0x7FFF, 0x7FFF, 0xF398, 0xF552, 0xE266, 0x16DC, 0x348, 0x11EF, 0xF013, 0xF6E6, 0xF9C4, 0xE58, 0x426, 0x496, 0xF423, 0xFCEC, 0xFF0F, 0x490, 0x508, 0xFE69, 0xFFBA, 0xFBE7, 0x1F6, 0xFE60, 0x2D4, 0xFF84, 0xFEFD, 0xFF44, 0x125, 0x57, 0x35, 0x3D, 0xFF21, 0x56, 0xFF74, 0x8, 0xFEC5, 0x11E, 0x9B, 0xE0, 0x32, 0xFEFF, 0xFF2C, 0xFF55, 0xB0, 0x99
                                         };


const short wb_sph_out_fir_table[6][90] = {32767, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                                           0xFFAA, 0x49, 0xFF67, 0x9B, 0xFF61, 0x2E, 0x23, 0xFF13, 0x135, 0xFE24, 0xC5, 0xFEC3, 0xFFF9, 0xFFE0, 0xFF56, 0xFFCE, 0x2C, 0xFFCE, 0xFF54, 0x11B, 0xFE9D, 0xE2, 0xFE84, 0x1C5, 0xFBE7, 0x493, 0xFBA3, 0x2DD, 0x270, 0xFAA7, 0xBF1, 0xF286, 0xE92, 0xFBE3, 0x1DE, 0xCE8, 0xF034, 0xDCD, 0xF3CB, 0xB28, 0x10D0, 0xCFD8, 0x5A9D, 0xBA67, 0x5A9D, 0x5A9D, 0xBA67, 0x5A9D, 0xCFD8, 0x10D0, 0xB28, 0xF3CB, 0xDCD, 0xF034, 0xCE8, 0x1DE, 0xFBE3, 0xE92, 0xF286, 0xBF1, 0xFAA7, 0x270, 0x2DD, 0xFBA3, 0x493, 0xFBE7, 0x1C5, 0xFE84, 0xE2, 0xFE9D, 0x11B, 0xFF54, 0xFFCE, 0x2C, 0xFFCE, 0xFF56, 0xFFE0, 0xFFF9, 0xFEC3, 0xC5, 0xFE24, 0x135, 0xFF13, 0x23, 0x2E, 0xFF61, 0x9B, 0xFF67, 0x49, 0xFFAA,
                                           0x7, 0xFFF4, 0xFFC6, 0x18, 0xFFDF, 0x16, 0xFFE5, 0xFFF5, 0xFFF3, 0xFFA5, 0x2B, 0xFFC8, 0x96, 0xFFFF, 0x6F, 0x1F, 0x80, 0x15, 0xFFA4, 0xF, 0xFF85, 0xFFD8, 0xFF54, 0xFFC7, 0xFF10, 0xDB, 0xFEA5, 0x1BE, 0xFFB1, 0x177, 0x9D, 0xFE5B, 0x157, 0xFF49, 0x5F9, 0xFF3F, 0xFC89, 0xF5E1, 0x452, 0xFC83, 0xF65, 0xEC6C, 0x14A1, 0xEF27, 0x500F, 0x500F, 0xEF27, 0x14A1, 0xEC6C, 0xF65, 0xFC83, 0x452, 0xF5E1, 0xFC89, 0xFF3F, 0x5F9, 0xFF49, 0x157, 0xFE5B, 0x9D, 0x177, 0xFFB1, 0x1BE, 0xFEA5, 0xDB, 0xFF10, 0xFFC7, 0xFF54, 0xFFD8, 0xFF85, 0xF, 0xFFA4, 0x15, 0x80, 0x1F, 0x6F, 0xFFFF, 0x96, 0xFFC8, 0x2B, 0xFFA5, 0xFFF3, 0xFFF5, 0xFFE5, 0x16, 0xFFDF, 0x18, 0xFFC6, 0xFFF4, 0x7,
                                           0xC7, 0xFFAF, 0xFFBA, 0xFFAB, 0xB, 0x5D, 0xFF79, 0x1, 0xFF11, 0xFFE5, 0xFF6D, 0x1B, 0xFF0B, 0xFF58, 0xFF49, 0xFFBB, 0xFF64, 0xFF38, 0xFFDA, 0xFECB, 0x110, 0xFEBD, 0x21A, 0xFE0D, 0x1A0, 0x38, 0x126, 0x37C, 0xFF38, 0x2BB, 0xFDD5, 0x401, 0x92, 0x3BE, 0xFF40, 0x4B6, 0xF9CB, 0xFCF7, 0x4E4, 0xFD9C, 0x12B3, 0xDBB8, 0xDBB8, 0x5A9D, 0x22B6, 0x22B6, 0xDBB8, 0x12B3, 0xFD9C, 0x4E4, 0xFCF7, 0xF9CB, 0x4B6, 0xFF40, 0x3BE, 0x92, 0x401, 0xFDD5, 0x2BB, 0xFF38, 0x37C, 0x126, 0x38, 0x1A0, 0xFE0D, 0x21A, 0xFEBD, 0x110, 0xFECB, 0xFFDA, 0xFF38, 0xFF64, 0xFFBB, 0xFF49, 0xFF58, 0xFF0B, 0x1B, 0xFF6D, 0xFFE5, 0xFF11, 0x1, 0xFF79, 0x5D, 0xB, 0xFFAB, 0xFFBA, 0xFFAF, 0xC7, 0xB0, 0x99,
                                           0xFFAA, 0x49, 0xFF67, 0x9B, 0xFF61, 0x2E, 0x23, 0xFF13, 0x135, 0xFE24, 0xC5, 0xFEC3, 0xFFF9, 0xFFE0, 0xFF56, 0xFFCE, 0x2C, 0xFFCE, 0xFF54, 0x11B, 0xFE9D, 0xE2, 0xFE84, 0x1C5, 0xFBE7, 0x493, 0xFBA3, 0x2DD, 0x270, 0xFAA7, 0xBF1, 0xF286, 0xE92, 0xFBE3, 0x1DE, 0xCE8, 0xF034, 0xDCD, 0xF3CB, 0xB28, 0x10D0, 0xCFD8, 0x5A9D, 0xBA67, 0x5A9D, 0x5A9D, 0xBA67, 0x5A9D, 0xCFD8, 0x10D0, 0xB28, 0xF3CB, 0xDCD, 0xF034, 0xCE8, 0x1DE, 0xFBE3, 0xE92, 0xF286, 0xBF1, 0xFAA7, 0x270, 0x2DD, 0xFBA3, 0x493, 0xFBE7, 0x1C5, 0xFE84, 0xE2, 0xFE9D, 0x11B, 0xFF54, 0xFFCE, 0x2C, 0xFFCE, 0xFF56, 0xFFE0, 0xFFF9, 0xFEC3, 0xC5, 0xFE24, 0x135, 0xFF13, 0x23, 0x2E, 0xFF61, 0x9B, 0xFF67, 0x49, 0xFFAA,
                                           0xC7, 0xFFAF, 0xFFBA, 0xFFAB, 0xB, 0x5D, 0xFF79, 0x1, 0xFF11, 0xFFE5, 0xFF6D, 0x1B, 0xFF0B, 0xFF58, 0xFF49, 0xFFBB, 0xFF64, 0xFF38, 0xFFDA, 0xFECB, 0x110, 0xFEBD, 0x21A, 0xFE0D, 0x1A0, 0x38, 0x126, 0x37C, 0xFF38, 0x2BB, 0xFDD5, 0x401, 0x92, 0x3BE, 0xFF40, 0x4B6, 0xF9CB, 0xFCF7, 0x4E4, 0xFD9C, 0x12B3, 0xDBB8, 0xDBB8, 0x5A9D, 0x22B6, 0x22B6, 0xDBB8, 0x12B3, 0xFD9C, 0x4E4, 0xFCF7, 0xF9CB, 0x4B6, 0xFF40, 0x3BE, 0x92, 0x401, 0xFDD5, 0x2BB, 0xFF38, 0x37C, 0x126, 0x38, 0x1A0, 0xFE0D, 0x21A, 0xFEBD, 0x110, 0xFECB, 0xFFDA, 0xFF38, 0xFF64, 0xFFBB, 0xFF49, 0xFF58, 0xFF0B, 0x1B, 0xFF6D, 0xFFE5, 0xFF11, 0x1, 0xFF79, 0x5D, 0xB, 0xFFAB, 0xFFBA, 0xFFAF, 0xC7, 0xB0, 0x99
                                          };

/*==============================================================================
 *                     Singleton Pattern
 *============================================================================*/

SpeechParamParser *SpeechParamParser::UniqueSpeechParamParser = NULL;


SpeechParamParser *SpeechParamParser::getInstance()
{
    static Mutex mGetInstanceLock;
    Mutex::Autolock _l(mGetInstanceLock);
    ALOGD("%s()", __FUNCTION__);

    if (UniqueSpeechParamParser == NULL)
    {
        UniqueSpeechParamParser = new SpeechParamParser();
    }
    ASSERT(UniqueSpeechParamParser != NULL);
    return UniqueSpeechParamParser;
}
/*==============================================================================
 *                     Constructor / Destructor / Init / Deinit
 *============================================================================*/

SpeechParamParser::SpeechParamParser()
{
    ALOGD("%s()", __FUNCTION__);
    Init();
}

SpeechParamParser::~SpeechParamParser()
{
    ALOGD("%s()", __FUNCTION__);

}

void SpeechParamParser::Init()
{
    ALOGD("%s()", __FUNCTION__);
    InitAppParser();

    mSphParamInfo.SpeechMode = SPEECH_MODE_NORMAL;
    mSphParamInfo.u4VolumeIndex = 3;
    mSphParamInfo.bBtHeadsetNrecOn = false;
    mSphParamInfo.bLPBK = false;
}

void SpeechParamParser::Deinit()
{
    ALOGD("%s()", __FUNCTION__);
}


/*==============================================================================
 *                     SpeechParamParser Imeplementation
 *============================================================================*/
bool SpeechParamParser::GetSpeechParamSupport(void)
{
    bool mSupport;

#if defined(MTK_AUDIO_HIERARCHICAL_PARAM_SUPPORT)
    mSupport = true;
#else
    mSupport = false;
#endif
    ALOGD("%s(), GetSpeechParamSupport:%d", __FUNCTION__, mSupport);
    return mSupport;
}


void SpeechParamParser::InitAppParser()
{
    ALOGD("+%s()", __FUNCTION__);
#if defined(APP_PARSER_SUPPORT)
    /* Init AppHandle */
    ALOGD("%s() appHandleGetInstance", __FUNCTION__);
    mAppHandle = appHandleGetInstance();
    ALOGD("%s() appHandleRegXmlChangedCb", __FUNCTION__);

#endif
}

status_t SpeechDataDump(uint16_t uSpeechTypeIndex, const char *nameParam, const char *SpeechParamData)
{
    ALOGV("+%s(), uSpeechTypeIndex=%d", __FUNCTION__, uSpeechTypeIndex);
    int u4I = 0, idxDump = 0;
    //speech parameter dump

    switch (uSpeechTypeIndex)
    {
        case AUDIO_TYPE_SPEECH:
        {
            if (strcmp(nameParam, "speech_mode_para") == 0)
            {
                idxDump = 16;
            }
            else if (strcmp(nameParam, "sph_in_fir") == 0)
            {
                idxDump = 5;
            }
            else if (strcmp(nameParam, "sph_out_fir") == 0)
            {
                idxDump = 5;
            }
            break;
        }
        case AUDIO_TYPE_SPEECH_GENERAL:
        {
            if (strcmp(nameParam, "speech_common_para") == 0)
            {
                idxDump = 12;
            }
            else if (strcmp(nameParam, "debug_info") == 0)
            {
                idxDump = 8;
            }
            break;
        }
    }
    ALOGD("%s(), idxDump=%d", __FUNCTION__, idxDump);
    for (u4I = 0; u4I < idxDump; u4I++)
    {
        ALOGD("%s() SpeechParam[%d]=%d", __FUNCTION__, u4I, *((uint16_t *)SpeechParamData + u4I));
    }

    return NO_ERROR;
}


status_t SpeechParamParser::GetSpeechParamFromAppParser(uint16_t uSpeechTypeIndex, AUDIO_TYPE_SPEECH_LAYERINFO_STRUCT *mParamLayerInfo, char *pPackedParamUnit, uint16_t *sizeByteTotal)
{
    ALOGD("+%s(), mParamLayerInfo->numCategoryType=0x%x", __FUNCTION__, mParamLayerInfo->numCategoryType);
#if defined(APP_PARSER_SUPPORT)

    if (&mAppHandle == NULL)
    {
        ALOGE("%s() &mAppHandle == NULL, Assert!!!", __FUNCTION__);
        ASSERT(0);
        return UNKNOWN_ERROR;
    }

    char *categoryPath = NULL;
    UT_string *uts_categoryPath = NULL;
    ParamUnit *paramUnit = NULL;
    uint16_t  sizeByteParam = 0, u4Index;
    Param  *SpeechParam;

    /* If user select a category path, just like "NarrowBand / Normal of Handset / Level0" */
    utstring_new(uts_categoryPath);
#if defined(APP_TEST)
    utstring_printf(uts_categoryPath, "%s,%s,", audioType_Speech_CategoryType[0].string(), audioType_Speech_CategoryName1[0]);
    utstring_printf(uts_categoryPath, "%s,%s,", audioType_Speech_CategoryType[1].string(), audioType_Speech_CategoryName2[0]);
    utstring_printf(uts_categoryPath, "%s,%s,", audioType_Speech_CategoryType[2].string(), audioType_Speech_CategoryName3[0]);
    utstring_printf(uts_categoryPath, "%s,%s", audioType_Speech_CategoryType[3].string(), audioType_Speech_CategoryName4[0]);

#else
    ALOGV("%s(), categoryType.size=%d, paramName.size=%d", __FUNCTION__, mParamLayerInfo->categoryType.size(), mParamLayerInfo->paramName.size());
    for (u4Index = 0; u4Index < mParamLayerInfo->categoryType.size() ; u4Index++)
    {
        ALOGV("%s(), categoryType[%d]= %s", __FUNCTION__, u4Index, mParamLayerInfo->categoryType.at(u4Index).string());
    }
    for (u4Index = 0; u4Index < mParamLayerInfo->categoryName.size() ; u4Index++)
    {
        ALOGV("%s(), categoryName[%d]= %s", __FUNCTION__, u4Index, mParamLayerInfo->categoryName.at(u4Index).string());
    }


    for (u4Index = 0; u4Index < mParamLayerInfo->numCategoryType ; u4Index++)
    {
        if (u4Index == mParamLayerInfo->numCategoryType - 1)
        {
            //last time concat
            utstring_printf(uts_categoryPath, "%s,%s", (char *)(mParamLayerInfo->categoryType.at(u4Index).string()), (char *)(mParamLayerInfo->categoryName.at(u4Index).string()));
        }
        else
        {
            utstring_printf(uts_categoryPath, "%s,%s,", (char *)(mParamLayerInfo->categoryType.at(u4Index).string()), (char *)(mParamLayerInfo->categoryName.at(u4Index).string()));
        }
    }
#endif

    if (uts_categoryPath)
    {
        categoryPath = strdup(utstring_body(uts_categoryPath));
        utstring_free(uts_categoryPath);

        ALOGD("%s(), categoryPath= %s", __FUNCTION__, categoryPath);
    }

#if defined(APP_TEST)

    ALOGD("%s() audioTypeName=%s", __FUNCTION__, audioTypeNameList[0]);
    /* Query AudioType */
    AudioType *audioType = appHandleGetAudioTypeByName(mAppHandle, audioTypeNameList[0]);

#else


    ALOGV("%s() audioTypeName=%s", __FUNCTION__, mParamLayerInfo->audioTypeName);
    /* Query AudioType */
    AudioType *audioType = appHandleGetAudioTypeByName(mAppHandle, mParamLayerInfo->audioTypeName);
#endif

    if (!audioType)
    {
        free(categoryPath);
        ALOGE("%s() can't find audioTypeName=%s, Assert!!!", __FUNCTION__, mParamLayerInfo->audioTypeName);
        ASSERT(0);
        return UNKNOWN_ERROR;
    }
    ALOGD("%s() audioType=%s", __FUNCTION__, audioType->name);

    /* Query the ParamUnit */
    audioTypeReadLock(audioType, __FUNCTION__);
    paramUnit = audioTypeGetParamUnit(audioType, categoryPath);
    if (!paramUnit)
    {
        free(categoryPath);
        audioTypeUnlock(audioType);
        ALOGE("%s() can't find paramUnit, Assert!!!", __FUNCTION__);
        ASSERT(0);
        return UNKNOWN_ERROR;
    }
    ALOGD("%s() paramId=%d", __FUNCTION__, paramUnit->paramId);

#if defined(APP_TEST)
    ALOGD("%s() APP_TEST audioType_Speech_ParamName[0].string()=%s", __FUNCTION__, audioType_Speech_ParamName[0].string());

    SpeechParam = paramUnitGetParamByName(paramUnit, (char *)audioType_Speech_ParamName[0].string());
    sizeByteParam = sizeByteParaData((DATA_TYPE)SpeechParam->paramInfo->dataType, SpeechParam->arraySize);
    memcpy(pPackedParamUnit + *sizeByteTotal, SpeechParam->data, sizeByteParam);
    *sizeByteTotal += sizeByteParam;

    ALOGD("%s() APP_TEST audioType_Speech_ParamName[1].string()=%s", __FUNCTION__, audioType_Speech_ParamName[1].string());
    SpeechParam = paramUnitGetParamByName(paramUnit, (char *)audioType_Speech_ParamName[1].string());
    sizeByteParam = sizeByteParaData((DATA_TYPE)SpeechParam->paramInfo->dataType, SpeechParam->arraySize);
    memcpy(pPackedParamUnit + *sizeByteTotal, SpeechParam->data, sizeByteParam);
    *sizeByteTotal += sizeByteParam;

    ALOGD("%s() APP_TEST audioType_Speech_ParamName[2].string()=%s", __FUNCTION__, audioType_Speech_ParamName[2].string());
    SpeechParam = paramUnitGetParamByName(paramUnit, (char *)audioType_Speech_ParamName[2].string());
    sizeByteParam = sizeByteParaData((DATA_TYPE)SpeechParam->paramInfo->dataType, SpeechParam->arraySize);
    memcpy(pPackedParamUnit + *sizeByteTotal, SpeechParam->data, sizeByteParam);
    *sizeByteTotal += sizeByteParam;


#else
    for (u4Index = 0; u4Index < (*mParamLayerInfo).numParam ; u4Index++)
    {

        SpeechParam = paramUnitGetParamByName(paramUnit, (const char *)mParamLayerInfo->paramName.at(u4Index).string());
        sizeByteParam = sizeByteParaData((DATA_TYPE)SpeechParam->paramInfo->dataType, SpeechParam->arraySize);
        memcpy(pPackedParamUnit + *sizeByteTotal, SpeechParam->data, sizeByteParam);
        *sizeByteTotal += sizeByteParam;
        ALOGD("%s() paramName=%s, sizeByteParam=%d", __FUNCTION__, mParamLayerInfo->paramName.at(u4Index).string(), sizeByteParam);
        //speech parameter dump
        SpeechDataDump(uSpeechTypeIndex, (const char *)mParamLayerInfo->paramName.at(u4Index).string(), (const char *)SpeechParam->data);
    }
#endif

    audioTypeUnlock(audioType);
    free(categoryPath);
#endif
    return NO_ERROR;
}

uint16_t SpeechParamParser::sizeByteParaData(DATA_TYPE dataType, uint16_t arraySize)
{
    uint16_t sizeUnit = 4;
    switch (dataType)
    {
        case TYPE_INT:
            sizeUnit = 4;
            break;
        case TYPE_UINT:
            sizeUnit = 4;
            break;
        case TYPE_FLOAT:
            sizeUnit = 4;
            break;
        case TYPE_BYTE_ARRAY:
            sizeUnit = arraySize;
            break;
        case TYPE_UBYTE_ARRAY:
            sizeUnit = arraySize;
            break;
        case TYPE_SHORT_ARRAY:
            sizeUnit = arraySize << 1;
            break;
        case TYPE_USHORT_ARRAY:
            sizeUnit = arraySize << 1;
            break;
        case TYPE_INT_ARRAY:
            sizeUnit = arraySize << 2;
            break;
        case TYPE_UINT_ARRAY:
            sizeUnit = arraySize << 2;
            break;
        default:
            ALOGE("%s(), Not an available dataType(%d)", __FUNCTION__, dataType);

            break;

    }

    ALOGV("-%s(), arraySize=%d, sizeUnit=%d", __FUNCTION__, arraySize, sizeUnit);

    return sizeUnit;


}


int SpeechParamParser::GetDmnrParamUnit(char *pPackedParamUnit)
{
    ALOGD("+%s()", __FUNCTION__);
    uint16_t size = 0, u4Index = 0, u4Index2 = 0, sizeByteFromApp = 0;
    uint16_t DataHeader;
    SPEECH_DYNAMIC_PARAM_UNIT_HDR_STRUCT eParamUnitHdr;
    memset(&eParamUnitHdr, 0, sizeof(eParamUnitHdr));

    eParamUnitHdr.SphParserVer = 1;
    eParamUnitHdr.NumLayer = 0x2;
    eParamUnitHdr.NumEachLayer = 0x22;
    eParamUnitHdr.ParamHeader[0] = 0x3;//OutputDeviceType
    eParamUnitHdr.ParamHeader[1] = 0x3;//VoiceBand
    eParamUnitHdr.SphUnitMagiNum = 0xAA03;

    memcpy(pPackedParamUnit + size, &eParamUnitHdr, sizeof(eParamUnitHdr));
    size += sizeof(eParamUnitHdr);

    char *pPackedParamUnitFromApp = new char [MAX_BYTE_PARAM_SPEECH];
    memset(pPackedParamUnitFromApp, 0, MAX_BYTE_PARAM_SPEECH);

#ifdef APP_PARSER_SUPPORT
    ALOGD("%s(), APP_PARSER_SUPPORT", __FUNCTION__);
    AUDIO_TYPE_SPEECH_LAYERINFO_STRUCT pParamLayerInfo;

    pParamLayerInfo.audioTypeName = (char *) audioTypeNameList[AUDIO_TYPE_SPEECH_DMNR];
    pParamLayerInfo.numCategoryType = MAX_NUM_CATEGORY_TYPE_SPEECH_DMNR;//4
    pParamLayerInfo.numParam = MAX_NUM_PARAM_SPEECH_DMNR;//4

    pParamLayerInfo.categoryType.assign(audioType_SpeechDMNR_CategoryType, audioType_SpeechDMNR_CategoryType + pParamLayerInfo.numCategoryType);
    pParamLayerInfo.paramName.assign(audioType_SpeechDMNR_ParamName, audioType_SpeechDMNR_ParamName + pParamLayerInfo.numParam);

    ALOGD("%s(), categoryType.size=%d, paramName.size=%d", __FUNCTION__, pParamLayerInfo.categoryType.size(), pParamLayerInfo.paramName.size());
    for (u4Index = 0; u4Index < pParamLayerInfo.paramName.size() ; u4Index++)
    {
        ALOGV("%s(), paramName[%d]= %s", __FUNCTION__, u4Index, pParamLayerInfo.paramName.at(u4Index).string());
    }


    for (u4Index = 0; u4Index < audioType_SpeechDMNR_NumPerCategory[0] ; u4Index++) //NB, WB
    {
        for (u4Index2 = 0; u4Index2 < audioType_SpeechDMNR_NumPerCategory[1] ; u4Index2++)
        {
            sizeByteFromApp = 0;
            DataHeader = ((u4Index + 1) << 4) + (u4Index2 + 1);
            memcpy(pPackedParamUnit + size, &DataHeader, sizeof(DataHeader));
            size += sizeof(DataHeader);

            pParamLayerInfo.categoryName.push_back(String8(audioType_SpeechDMNR_CategoryName1[u4Index]));
            pParamLayerInfo.categoryName.push_back(String8(audioType_SpeechDMNR_CategoryName2[u4Index2]));

            GetSpeechParamFromAppParser(AUDIO_TYPE_SPEECH_DMNR, &pParamLayerInfo, pPackedParamUnitFromApp, &sizeByteFromApp);

            memcpy(pPackedParamUnit + size, pPackedParamUnitFromApp, sizeByteFromApp);
            size += sizeByteFromApp;
            ALOGD("%s(), DataHeader=0x%x, categoryName[%d,%d]= %s,%s, sizeByteFromApp=%d", __FUNCTION__, DataHeader, u4Index, u4Index2, pParamLayerInfo.categoryName.at(0).string(), pParamLayerInfo.categoryName.at(1).string(), sizeByteFromApp);
            pParamLayerInfo.categoryName.pop_back();
            pParamLayerInfo.categoryName.pop_back();

        }
    }


#else

    unsigned short dmnr_para_nb[44] =
    {
        0, 0, 0, 0, 0,
        0, 0, 0, 0, 0,
        0, 0, 0, 0, 0,
        0, 0, 0, 0, 0,
        0, 0, 0, 0, 0,
        0, 0, 0, 0, 0,
        0, 0, 0, 0, 0,
        0, 0, 0, 0, 0,
        68, 0, 0, 0
    };


    unsigned short dmnr_para_wb[76] =
    {
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 68, 0, 0, 0

    };
    DataHeader = 0x0011;
    memcpy(pPackedParamUnit + size, &DataHeader, sizeof(DataHeader));
    size += sizeof(DataHeader);
    memcpy(pPackedParamUnit + size, &dmnr_para_nb, sizeof(dmnr_para_nb));
    size += sizeof(dmnr_para_nb);
    ALOGD("%s(), After NB size=%d", __FUNCTION__, size);

    DataHeader = 0x0012;
    memcpy(pPackedParamUnit + size, &DataHeader, sizeof(DataHeader));
    size += sizeof(DataHeader);
    memcpy(pPackedParamUnit + size, &dmnr_para_nb, sizeof(dmnr_para_nb));

    size += sizeof(dmnr_para_nb);
    ALOGD("%s(), After NB size=%d", __FUNCTION__, size);


    DataHeader = 0x0021;
    memcpy(pPackedParamUnit + size, &DataHeader, sizeof(DataHeader));
    size += sizeof(DataHeader);
    memcpy(pPackedParamUnit + size, &dmnr_para_wb, sizeof(dmnr_para_wb));
    size += sizeof(dmnr_para_wb);
    ALOGD("%s(), After WB size=%d", __FUNCTION__, size);

    DataHeader = 0x0022;
    memcpy(pPackedParamUnit + size, &DataHeader, sizeof(DataHeader));
    size += sizeof(DataHeader);
    memcpy(pPackedParamUnit + size, &dmnr_para_wb, sizeof(dmnr_para_wb));
    size += sizeof(dmnr_para_wb);

#endif
    if (pPackedParamUnitFromApp != NULL)
    {
        delete[] pPackedParamUnitFromApp;
    }

    ALOGD("-%s(), total size byte=%d", __FUNCTION__, size);
    return size;
}

int SpeechParamParser::GetGeneralParamUnit(char *pPackedParamUnit)
{
    ALOGD("+%s()", __FUNCTION__);
    uint16_t size = 0, u4Index = 0, u4Index2 = 0, sizeByteFromApp = 0;
    uint16_t DataHeader;
    SPEECH_DYNAMIC_PARAM_UNIT_HDR_STRUCT eParamUnitHdr;
    memset(&eParamUnitHdr, 0, sizeof(eParamUnitHdr));

    eParamUnitHdr.SphParserVer = 1;
    eParamUnitHdr.NumLayer = 0x1;
    eParamUnitHdr.NumEachLayer = 0x1;
    eParamUnitHdr.ParamHeader[0] = 0x1;//Common
    eParamUnitHdr.SphUnitMagiNum = 0xAA02;

    memcpy(pPackedParamUnit + size, &eParamUnitHdr, sizeof(eParamUnitHdr));
    size += sizeof(eParamUnitHdr);

    char *pPackedParamUnitFromApp = new char [MAX_BYTE_PARAM_SPEECH];
    memset(pPackedParamUnitFromApp, 0, MAX_BYTE_PARAM_SPEECH);
#ifdef APP_PARSER_SUPPORT
    AUDIO_TYPE_SPEECH_LAYERINFO_STRUCT pParamLayerInfo;

    pParamLayerInfo.audioTypeName = (char *) audioTypeNameList[AUDIO_TYPE_SPEECH_GENERAL];
    pParamLayerInfo.numCategoryType = MAX_NUM_CATEGORY_TYPE_SPEECH_GENERAL;//4
    pParamLayerInfo.numParam = MAX_NUM_PARAM_SPEECH_GENERAL;//4

    pParamLayerInfo.categoryType.assign(audioType_SpeechGeneral_CategoryType, audioType_SpeechGeneral_CategoryType + pParamLayerInfo.numCategoryType);
    pParamLayerInfo.paramName.assign(audioType_SpeechGeneral_ParamName, audioType_SpeechGeneral_ParamName + pParamLayerInfo.numParam);

    ALOGV("%s(), categoryType.size=%d, paramName.size=%d", __FUNCTION__, pParamLayerInfo.categoryType.size(), pParamLayerInfo.paramName.size());
    for (u4Index = 0; u4Index < pParamLayerInfo.paramName.size() ; u4Index++)
    {
        ALOGV("%s(), paramName[%d]= %s", __FUNCTION__, u4Index, pParamLayerInfo.paramName.at(u4Index).string());
    }


    DataHeader = 0x000F;
    memcpy(pPackedParamUnit + size, &DataHeader, sizeof(DataHeader));
    size += sizeof(DataHeader);

    pParamLayerInfo.categoryName.push_back(String8(audioType_SpeechGeneral_CategoryName1[0]));

    GetSpeechParamFromAppParser(AUDIO_TYPE_SPEECH_GENERAL, &pParamLayerInfo, pPackedParamUnitFromApp, &sizeByteFromApp);

    memcpy(pPackedParamUnit + size, pPackedParamUnitFromApp, sizeByteFromApp);
    size += sizeByteFromApp;



#else

    //NB
    DataHeader = 0x000F;
    memcpy(pPackedParamUnit + size, &DataHeader, sizeof(DataHeader));
    size += sizeof(DataHeader);
    ALOGD("%s(), After hdr size=%d", __FUNCTION__, size);


    SPEECH_GENERAL_PARAM_STRUCT eSphParamGeneral;

    memset(&eSphParamGeneral, 0, sizeof(eSphParamGeneral));

    unsigned short speech_common_para[12] =
    {
        0,  55997,  31000,    10752,      32769,      0,      0,      0, \
        0,      0,      0,      0
    };

    unsigned short debug_info[16] =
    {
        0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0
    };

    memcpy(&eSphParamGeneral.speech_common_para, &speech_common_para, sizeof(speech_common_para));
    memcpy(&eSphParamGeneral.debug_info, &debug_info, sizeof(debug_info));

    memcpy(pPackedParamUnit + size, &eSphParamGeneral, sizeof(eSphParamGeneral));
    size += sizeof(eSphParamGeneral);
    ALOGD("%s(), After NB size=%d", __FUNCTION__, size);
#endif
    ALOGD("-%s(), total size byte=%d", __FUNCTION__, size);
    if (pPackedParamUnitFromApp != NULL)
    {
        delete[] pPackedParamUnitFromApp;
    }

    return size;
}

speech_profile_t SpeechParamParser::GetSpeechProfile(const speech_mode_t IdxMode, bool bBtHeadsetNrecOn)
{
    speech_profile_t index_speech_profile;

    if (mSphParamInfo.bLPBK)
    {
        switch (IdxMode)
        {
            case SPEECH_MODE_NORMAL:
                index_speech_profile = SPEECH_PROFILE_LPBK_HANDSET;

                break;

            case SPEECH_MODE_EARPHONE:
                index_speech_profile = SPEECH_PROFILE_LPBK_HEADSET;
                break;
            case SPEECH_MODE_LOUD_SPEAKER:
                index_speech_profile = SPEECH_PROFILE_LPBK_HANDSFREE;
                break;
            default:
                index_speech_profile = SPEECH_PROFILE_LPBK_HANDSET;

                break;
        }
    }
    else
    {
        switch (IdxMode)
        {
            case SPEECH_MODE_NORMAL:
                index_speech_profile = SPEECH_PROFILE_HANDSET;

                break;

            case SPEECH_MODE_EARPHONE:
                index_speech_profile = SPEECH_PROFILE_4_POLE_HEADSET;
                break;
            case SPEECH_MODE_LOUD_SPEAKER:
                index_speech_profile = SPEECH_PROFILE_HANDSFREE;
                break;
            case SPEECH_MODE_BT_EARPHONE:
            case SPEECH_MODE_BT_CORDLESS:
            case SPEECH_MODE_BT_CARKIT:
                if (bBtHeadsetNrecOn == true)
                {
                    index_speech_profile = SPEECH_PROFILE_BT_EARPHONE;
                }
                else
                {
                    index_speech_profile = SPEECH_PROFILE_BT_NREC_OFF;
                }
                break;
            case SPEECH_MODE_MAGIC_CON_CALL:
                index_speech_profile = SPEECH_PROFILE_MAGICONFERENCE;
                break;
            case SPEECH_MODE_HAC:
                index_speech_profile = SPEECH_PROFILE_HAC;
                break;
            default:
                index_speech_profile = SPEECH_PROFILE_HANDSET;

                break;
        }
    }
    ALOGD("%s(), IdxMode = %d, IdxPfrfile = %d", __FUNCTION__, IdxMode, index_speech_profile);

    return index_speech_profile;
}

int SpeechParamParser::GetSpeechParamUnit(char *pPackedParamUnit, int *p4ParamArg)
//int SpeechParamParser::GetSpeechParamUnit(char *pPackedParamUnit, speech_mode_t IdxMode, char IdxVolume)
{
    uint16_t size = 0, u4Index, sizeByteFromApp = 0;
    uint16_t DataHeader, IdxInfo = 0, IdxTmp = 0;
    short IdmVolumeFIR = 0;
    SPEECH_DYNAMIC_PARAM_UNIT_HDR_STRUCT eParamUnitHdr;

    char *pPackedParamUnitFromApp = new char [MAX_BYTE_PARAM_SPEECH];
    memset(pPackedParamUnitFromApp, 0, MAX_BYTE_PARAM_SPEECH);

    speech_mode_t IdxMode = (speech_mode_t) * ((int *)p4ParamArg);
    int IdxVolume = *((int *)p4ParamArg + 1);
    bool bBtHeadsetNrecOn = (bool) * ((int *)p4ParamArg + 2);
    mSphParamInfo.bBtHeadsetNrecOn = bBtHeadsetNrecOn;
    mSphParamInfo.u4VolumeIndex = IdxVolume;
    mSphParamInfo.SpeechMode = IdxMode;

    ALOGD("+%s(), IdxMode=0x%x, IdxVolume=0x%x, bBtHeadsetNrecOn=0x%x", __FUNCTION__, IdxMode, IdxVolume, bBtHeadsetNrecOn);
    int IdxPfrfile = GetSpeechProfile(IdxMode, bBtHeadsetNrecOn);
    memset(&eParamUnitHdr, 0, sizeof(eParamUnitHdr));

    eParamUnitHdr.SphParserVer = 1;
    eParamUnitHdr.NumLayer = 0x2;
    eParamUnitHdr.NumEachLayer = 0x21;
    eParamUnitHdr.ParamHeader[0] = 0x1F;//Network: bit0: GSM, bit1: WCDMA,.bit2: CDMA, bit3: VoLTE, bit4:C2K
    eParamUnitHdr.ParamHeader[1] = 0x3;//VoiceBand
    eParamUnitHdr.SphUnitMagiNum = 0xAA01;

    memcpy(pPackedParamUnit + size, &eParamUnitHdr, sizeof(eParamUnitHdr));
    size += sizeof(eParamUnitHdr);

    IdxInfo = IdxMode & 0xF;
    ALOGD("%s(), add mode IdxInfo=0x%x", __FUNCTION__, IdxInfo);
    IdxTmp = IdxVolume << 4;
    IdxInfo += IdxTmp;
    ALOGD("%s(), add volume<<4 IdxInfo=0x%x, IdxTmp=0x%x", __FUNCTION__, IdxInfo, IdxTmp);

    memcpy(pPackedParamUnit + size, &IdxInfo, sizeof(IdxInfo));
    size += sizeof(IdxInfo);
    //NB
    DataHeader = 0x001F;
    memcpy(pPackedParamUnit + size, &DataHeader, sizeof(DataHeader));
    size += sizeof(DataHeader);

#ifdef APP_PARSER_SUPPORT
    AUDIO_TYPE_SPEECH_LAYERINFO_STRUCT pParamLayerInfo;

    pParamLayerInfo.audioTypeName = (char *) audioTypeNameList[AUDIO_TYPE_SPEECH];
    pParamLayerInfo.numCategoryType = MAX_NUM_CATEGORY_TYPE_SPEECH;//4
    pParamLayerInfo.numParam = MAX_NUM_PARAM_SPEECH;//4


    pParamLayerInfo.categoryType.assign(audioType_Speech_CategoryType, audioType_Speech_CategoryType + pParamLayerInfo.numCategoryType);
    pParamLayerInfo.paramName.assign(audioType_Speech_ParamName, audioType_Speech_ParamName + pParamLayerInfo.numParam);

    ALOGV("%s(), categoryType.size=%d, paramName.size=%d", __FUNCTION__, pParamLayerInfo.categoryType.size(), pParamLayerInfo.paramName.size());
    for (u4Index = 0; u4Index < pParamLayerInfo.categoryType.size() ; u4Index++)
    {
        ALOGV("%s(), categoryType[%d]= %s", __FUNCTION__, u4Index, pParamLayerInfo.categoryType.at(u4Index).string());
    }
    for (u4Index = 0; u4Index < pParamLayerInfo.paramName.size() ; u4Index++)
    {
        ALOGV("%s(), paramName[%d]= %s", __FUNCTION__, u4Index, pParamLayerInfo.paramName.at(u4Index).string());
    }

    //get only1 nb
    pParamLayerInfo.categoryName.push_back(String8(audioType_Speech_CategoryName1[0]));

    pParamLayerInfo.categoryName.push_back(String8(audioType_Speech_CategoryName2[IdxPfrfile]));

    if (IdxVolume > 6 || IdxVolume < 0)
    {
        pParamLayerInfo.categoryName.push_back(String8(audioType_Speech_CategoryName3[3]));
        ALOGE("%s(), Invalid IdxVolume=0x%x, use 3 !!!", __FUNCTION__, IdxVolume);
    }
    else
    {
        pParamLayerInfo.categoryName.push_back(String8(audioType_Speech_CategoryName3[IdxVolume]));
    }

    pParamLayerInfo.categoryName.push_back(String8(audioType_Speech_CategoryName4[0]));


    for (u4Index = 0; u4Index < pParamLayerInfo.categoryName.size() ; u4Index++)
    {
        ALOGV("%s(), categoryName[%d]= %s", __FUNCTION__, u4Index, pParamLayerInfo.categoryName.at(u4Index).string());
    }

    GetSpeechParamFromAppParser(AUDIO_TYPE_SPEECH, &pParamLayerInfo, pPackedParamUnitFromApp, &sizeByteFromApp);

    memcpy(pPackedParamUnit + size, pPackedParamUnitFromApp, sizeByteFromApp);
    size += sizeByteFromApp;

    //WB
    DataHeader = 0x002F;
    memcpy(pPackedParamUnit + size, &DataHeader, sizeof(DataHeader));
    size += sizeof(DataHeader);

    pParamLayerInfo.categoryName.at(0) = String8(audioType_Speech_CategoryName1[1]);
    sizeByteFromApp = 0;
    GetSpeechParamFromAppParser(AUDIO_TYPE_SPEECH, &pParamLayerInfo, pPackedParamUnitFromApp, &sizeByteFromApp);
    memcpy(pPackedParamUnit + size, pPackedParamUnitFromApp, sizeByteFromApp);
    size += sizeByteFromApp;


#else


    SPEECH_DYNAMIC_PARAM_NB_STRUCT eSphParamNB;

    memset(&eSphParamNB, 0, sizeof(eSphParamNB));

    unsigned short *nb_speech_mode_para, *wb_speech_mode_para;
    short *nb_sph_in_fir,  *wb_sph_in_fir;
    short *nb_sph_out_fir,  *wb_sph_out_fir;
    char byte_speech_mode_para = 96, byte_nb_sph_fir = 90, byte_wb_sph_fir = 180;

    switch (IdxMode)
    {
        case SPEECH_MODE_NORMAL:
            nb_speech_mode_para = (unsigned short *)nb_speech_mode_para_table[0];
            wb_speech_mode_para = (unsigned short *)wb_speech_mode_para_table[0];
            nb_sph_in_fir = (short *)nb_sph_in_fir_table[0];
            wb_sph_in_fir = (short *)wb_sph_in_fir_table[0];
            nb_sph_out_fir = (short *)nb_sph_out_fir_table[0];
            wb_sph_out_fir = (short *)wb_sph_out_fir_table[0];
            break;

        case SPEECH_MODE_EARPHONE:
            nb_speech_mode_para = (unsigned short *)nb_speech_mode_para_table[1];
            wb_speech_mode_para = (unsigned short *)wb_speech_mode_para_table[1];
            nb_sph_in_fir = (short *)nb_sph_in_fir_table[1];
            wb_sph_in_fir = (short *)wb_sph_in_fir_table[1];
            nb_sph_out_fir = (short *)nb_sph_out_fir_table[1];
            wb_sph_out_fir = (short *)wb_sph_out_fir_table[1];
            break;

        case SPEECH_MODE_LOUD_SPEAKER:
            nb_speech_mode_para = (unsigned short *)nb_speech_mode_para_table[2];
            wb_speech_mode_para = (unsigned short *)wb_speech_mode_para_table[2];
            nb_sph_in_fir = (short *)nb_sph_in_fir_table[2];
            wb_sph_in_fir = (short *)wb_sph_in_fir_table[2];
            nb_sph_out_fir = (short *)nb_sph_out_fir_table[2];
            wb_sph_out_fir = (short *)wb_sph_out_fir_table[2];
            break;

        case SPEECH_MODE_BT_EARPHONE:
        case SPEECH_MODE_BT_CARKIT:
            nb_speech_mode_para = (unsigned short *)nb_speech_mode_para_table[3];
            wb_speech_mode_para = (unsigned short *)wb_speech_mode_para_table[3];
            nb_sph_in_fir = (short *)nb_sph_in_fir_table[3];
            wb_sph_in_fir = (short *)wb_sph_in_fir_table[3];
            nb_sph_out_fir = (short *)nb_sph_out_fir_table[3];
            wb_sph_out_fir = (short *)wb_sph_out_fir_table[3];
            break;

        case SPEECH_MODE_MAGIC_CON_CALL:
            nb_speech_mode_para = (unsigned short *)nb_speech_mode_para_table[4];
            wb_speech_mode_para = (unsigned short *)wb_speech_mode_para_table[4];
            nb_sph_in_fir = (short *)nb_sph_in_fir_table[4];
            wb_sph_in_fir = (short *)wb_sph_in_fir_table[4];
            nb_sph_out_fir = (short *)nb_sph_out_fir_table[4];
            wb_sph_out_fir = (short *)wb_sph_out_fir_table[4];
            break;

        case SPEECH_MODE_HAC:
            nb_speech_mode_para = (unsigned short *)nb_speech_mode_para_table[5];
            wb_speech_mode_para = (unsigned short *)wb_speech_mode_para_table[5];
            nb_sph_in_fir = (short *)nb_sph_in_fir_table[5];
            wb_sph_in_fir = (short *)wb_sph_in_fir_table[5];
            nb_sph_out_fir = (short *)nb_sph_out_fir_table[5];
            wb_sph_out_fir = (short *)wb_sph_out_fir_table[5];
            break;

        default:
            nb_speech_mode_para = (unsigned short *)nb_speech_mode_para_table[0];
            wb_speech_mode_para = (unsigned short *)wb_speech_mode_para_table[0];
            nb_sph_in_fir = (short *)nb_sph_in_fir_table[0];
            wb_sph_in_fir = (short *)wb_sph_in_fir_table[0];
            nb_sph_out_fir = (short *)nb_sph_out_fir_table[0];
            wb_sph_out_fir = (short *)wb_sph_out_fir_table[0];
            break;
    }

#if 1
    memcpy(&eSphParamNB.speech_mode_para, nb_speech_mode_para, byte_speech_mode_para);
    memcpy(&eSphParamNB.sph_in_fir, nb_sph_in_fir, byte_nb_sph_fir);
    memcpy(&eSphParamNB.sph_out_fir, nb_sph_out_fir, byte_nb_sph_fir);

    //volume tag setting: 0x7F0X
    IdmVolumeFIR = IdxVolume & 0xF;
    IdmVolumeFIR += 0x7F00;
    ALOGD("%s(), update FIR for IdxVolume(0x%x), FIR value=0x%x", __FUNCTION__, IdxVolume, IdxTmp);

    memcpy(&eSphParamNB.sph_in_fir, &IdmVolumeFIR, sizeof(IdmVolumeFIR));
    memcpy(&eSphParamNB.sph_out_fir, &IdmVolumeFIR, sizeof(IdmVolumeFIR));



    memcpy(pPackedParamUnit + size, &eSphParamNB, sizeof(eSphParamNB));
    size += sizeof(eSphParamNB);

    //WB
    DataHeader = 0x002F;
    memcpy(pPackedParamUnit + size, &DataHeader, sizeof(DataHeader));
    size += sizeof(DataHeader);

    SPEECH_DYNAMIC_PARAM_WB_STRUCT eSphParamWB;

    memset(&eSphParamWB, 0, sizeof(eSphParamWB));
    memcpy(&eSphParamWB.speech_mode_para, wb_speech_mode_para, byte_speech_mode_para);
    memcpy(&eSphParamWB.sph_in_fir, wb_sph_in_fir, byte_wb_sph_fir);
    memcpy(&eSphParamWB.sph_out_fir, wb_sph_out_fir, byte_wb_sph_fir);


#else

    unsigned short speech_mode_para[48] = {96,   253,  16388,     31,   57351,     799,   400,     64,   80,  4325,      611,       0,   20488,      0,     0,  8192,
                                           0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                                           0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
                                          };

    short sph_in_fir[45] = {32767, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 0, 0, 0
                           };
    short sph_out_fir[45] = {32767, 0, 0, 0, 0, 0, 0, 0, 0,
                             0, 0, 0, 0, 0, 0, 0, 0, 0,
                             0, 0, 0, 0, 0, 0, 0, 0, 0,
                             0, 0, 0, 0, 0, 0, 0, 0, 0,
                             0, 0, 0, 0, 0, 0, 0, 0, 0
                            };
    memcpy(&eSphParamNB.speech_mode_para, &speech_mode_para, sizeof(speech_mode_para));
    memcpy(&eSphParamNB.sph_in_fir, &sph_in_fir, sizeof(sph_in_fir));
    memcpy(&eSphParamNB.sph_out_fir, &sph_out_fir, sizeof(sph_out_fir));

    memcpy(pPackedParamUnit + size, &eSphParamNB, sizeof(eSphParamNB));
    size += sizeof(eSphParamNB);
    ALOGD("%s(), After NB size=%d", __FUNCTION__, size);


    //WB
    DataHeader = 0x002F;
    memcpy(pPackedParamUnit + size, &DataHeader, sizeof(DataHeader));
    size += sizeof(DataHeader);

    ALOGD("%s(), After hdr size=%d", __FUNCTION__, size);
    SPEECH_DYNAMIC_PARAM_WB_STRUCT eSphParamWB;

    memset(&eSphParamWB, 0, sizeof(eSphParamWB));
    memcpy(&eSphParamWB.speech_mode_para, &speech_mode_para, sizeof(speech_mode_para));
    memcpy(&eSphParamWB.sph_in_fir, &sph_in_fir, sizeof(sph_in_fir));
    memcpy(&eSphParamWB.sph_out_fir, &sph_out_fir, sizeof(sph_out_fir));
#endif

    //volume tag setting: 0x7F0X
    memcpy(&eSphParamWB.sph_in_fir, &IdmVolumeFIR, sizeof(IdmVolumeFIR));
    memcpy(&eSphParamWB.sph_out_fir, &IdmVolumeFIR, sizeof(IdmVolumeFIR));

    memcpy(pPackedParamUnit + size, &eSphParamWB, sizeof(eSphParamWB));
    size += sizeof(eSphParamWB);
#endif

    if (pPackedParamUnitFromApp != NULL)
    {
        delete[] pPackedParamUnitFromApp;
    }
    ALOGD("-%s(), total size byte=%d", __FUNCTION__, size);
    return size;
}

status_t SpeechParamParser::SetParamInfo(const String8 &keyParamPairs)
{
    ALOGD("+%s(): %s", __FUNCTION__, keyParamPairs.string());
    AudioParameter param = AudioParameter(keyParamPairs);
    int value;
    if (param.getInt(String8("ParamSphLpbk"), value) == NO_ERROR)
    {
        param.remove(String8("ParamSphLpbk"));
#if defined(MTK_AUDIO_SPH_LPBK_PARAM)
        mSphParamInfo.bLPBK = (value == 1) ? true : false;
#else
        mSphParamInfo.bLPBK = false;

#endif
        ALOGD("%s(): mSphParamInfo.bLPBK = %d", __FUNCTION__, mSphParamInfo.bLPBK);
    }


    ALOGD("-%s(): %s", __FUNCTION__, keyParamPairs.string());
    return NO_ERROR;
}

int SpeechParamParser::GetMagiClarityParamUnit(char *pPackedParamUnit)
{
    ALOGD("+%s()", __FUNCTION__);
    uint16_t size = 0, u4Index = 0, u4Index2 = 0, sizeByteFromApp = 0;
    uint16_t DataHeader;
    SPEECH_DYNAMIC_PARAM_UNIT_HDR_STRUCT eParamUnitHdr;
    memset(&eParamUnitHdr, 0, sizeof(eParamUnitHdr));

    eParamUnitHdr.SphParserVer = 1;
    eParamUnitHdr.NumLayer = 0x1;
    eParamUnitHdr.NumEachLayer = 0x1;
    eParamUnitHdr.ParamHeader[0] = 0x1;//Common
    eParamUnitHdr.SphUnitMagiNum = 0xAA04;

    memcpy(pPackedParamUnit + size, &eParamUnitHdr, sizeof(eParamUnitHdr));
    size += sizeof(eParamUnitHdr);

    char *pPackedParamUnitFromApp = new char [MAX_BYTE_PARAM_SPEECH];
    memset(pPackedParamUnitFromApp, 0, MAX_BYTE_PARAM_SPEECH);
    AUDIO_TYPE_SPEECH_LAYERINFO_STRUCT pParamLayerInfo;

    pParamLayerInfo.audioTypeName = (char *) audioTypeNameList[AUDIO_TYPE_SPEECH_MAGICLARITY];
    pParamLayerInfo.numCategoryType = MAX_NUM_CATEGORY_TYPE_SPEECH_MAGICLARITY;//4
    pParamLayerInfo.numParam = MAX_NUM_PARAM_SPEECH_MAGICLARITY;//4

    pParamLayerInfo.categoryType.assign(audioType_SpeechMagiClarity_CategoryType, audioType_SpeechMagiClarity_CategoryType + pParamLayerInfo.numCategoryType);
    pParamLayerInfo.paramName.assign(audioType_SpeechMagiClarity_ParamName, audioType_SpeechMagiClarity_ParamName + pParamLayerInfo.numParam);

    ALOGV("%s(), categoryType.size=%d, paramName.size=%d", __FUNCTION__, pParamLayerInfo.categoryType.size(), pParamLayerInfo.paramName.size());
    for (u4Index = 0; u4Index < pParamLayerInfo.paramName.size() ; u4Index++)
    {
        ALOGV("%s(), paramName[%d]= %s", __FUNCTION__, u4Index, pParamLayerInfo.paramName.at(u4Index).string());
    }
    DataHeader = 0x000F;
    memcpy(pPackedParamUnit + size, &DataHeader, sizeof(DataHeader));
    size += sizeof(DataHeader);

    pParamLayerInfo.categoryName.push_back(String8(audioType_SpeechMagiClarity_CategoryName1[0]));

    GetSpeechParamFromAppParser(AUDIO_TYPE_SPEECH_MAGICLARITY, &pParamLayerInfo, pPackedParamUnitFromApp, &sizeByteFromApp);

    memcpy(pPackedParamUnit + size, pPackedParamUnitFromApp, sizeByteFromApp);
    size += sizeByteFromApp;

    ALOGD("-%s(), total size byte=%d", __FUNCTION__, size);

    if (pPackedParamUnitFromApp != NULL)
    {
        delete[] pPackedParamUnitFromApp;
    }
    return size;
}


}







//namespace android
