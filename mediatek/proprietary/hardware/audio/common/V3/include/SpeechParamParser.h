#ifndef _SPEECH_PARAM_PARSER_H_
#define _SPEECH_PARAM_PARSER_H_

/*****************************************************************************
*                E X T E R N A L   R E F E R E N C E S
******************************************************************************
*/
#include "AudioType.h"
#include "SpeechType.h"
#include <vector>
//extern "C" {
#include "AudioParamParser.h"
//}

namespace android
{

#define APP_PARSER_SUPPORT

/*****************************************************************************
*                         I O C T R L  M E S S A G E S
******************************************************************************
*/
typedef struct _SPEECH_DYNAMIC_PARAM_NB_STRUCT
{
    /* speech enhancement */
    unsigned short speech_mode_para[48];
    /* speech input FIR */
    short          sph_in_fir[45];
    /* speech output FIR */
    short          sph_out_fir[45];

} SPEECH_DYNAMIC_PARAM_NB_STRUCT;

typedef struct _SPEECH_DYNAMIC_PARAM_WB_STRUCT
{
    /* speech enhancement */
    unsigned short speech_mode_para[48];
    /* speech input FIR */
    short          sph_in_fir[90];
    /* speech output FIR */
    short          sph_out_fir[90];

} SPEECH_DYNAMIC_PARAM_WB_STRUCT;

typedef struct _SPEECH_DYNAMIC_PARAM_UNIT_HDR
{
    uint16_t SphParserVer;
    uint16_t NumLayer ;
    uint16_t NumEachLayer ;
    uint16_t ParamHeader[4] ;//Network, VoiceBand, Reserved, Reserved
    uint16_t SphUnitMagiNum;

} SPEECH_DYNAMIC_PARAM_UNIT_HDR_STRUCT;

typedef struct _SPEECH_GENERAL_PARAM_STRUCT
{
    /* speech common parameters */
    unsigned short speech_common_para[12] ;
    unsigned short debug_info[16] ;
} SPEECH_GENERAL_PARAM_STRUCT;

typedef struct _AUDIO_TYPE_SPEECH_LAYERINFO_STRUCT
{
    char *audioTypeName;
    char numCategoryType;//4
    std::vector<String8> categoryType;
    std::vector<String8> categoryName;
    char numParam;//4
    std::vector<String8> paramName;

} AUDIO_TYPE_SPEECH_LAYERINFO_STRUCT;

enum speech_profile_t
{
    SPEECH_PROFILE_HANDSET          = 0,
    SPEECH_PROFILE_4_POLE_HEADSET        = 1,
    SPEECH_PROFILE_HANDSFREE    = 2,
    SPEECH_PROFILE_BT_EARPHONE     = 3,
    SPEECH_PROFILE_BT_NREC_OFF     = 4,
    SPEECH_PROFILE_MAGICONFERENCE  = 5,
    SPEECH_PROFILE_HAC      = 6,
    SPEECH_PROFILE_LPBK_HANDSET          = 7,
    SPEECH_PROFILE_LPBK_HEADSET        = 8,
    SPEECH_PROFILE_LPBK_HANDSFREE    = 9,

    SPEECH_PROFILE_MAX_NUM      = 10
};

typedef struct _SPEECH_PARAM_INFO_STRUCT
{
    speech_mode_t SpeechMode;
    unsigned int u4VolumeIndex;
    bool bBtHeadsetNrecOn;
    bool bLPBK;
} SPEECH_PARAM_INFO_STRUCT;


/*****************************************************************************
*                         F U N C T I O N S
******************************************************************************
*/

class SpeechParamParser
{
    public:
        virtual ~SpeechParamParser();
        static SpeechParamParser *getInstance();
        void Init();
        bool GetSpeechParamSupport(void);
        int GetSpeechParamUnit(char *pPackedParamUnit, int *p4ParamArg);
        int GetGeneralParamUnit(char *pPackedParamUnit);
        int GetDmnrParamUnit(char *pPackedParamUnit);
        int GetMagiClarityParamUnit(char *pPackedParamUnit);
        status_t SetParamInfo(const String8 &keyParamPairs);

    protected:


    private:
        SpeechParamParser();
        static SpeechParamParser *UniqueSpeechParamParser;
#if defined(APP_PARSER_SUPPORT)
        AppHandle *mAppHandle;
#endif
        speech_profile_t GetSpeechProfile(const speech_mode_t IdxMode, bool bBtHeadsetNrecOn);

        void InitAppParser();
        void Deinit();
        status_t GetSpeechParamFromAppParser(uint16_t uSpeechTypeIndex, AUDIO_TYPE_SPEECH_LAYERINFO_STRUCT *pParamLayerInfo, char *pPackedParamUnit, uint16_t *sizeByteTotal);
        uint16_t sizeByteParaData(DATA_TYPE dataType, uint16_t arraySize);
        SPEECH_PARAM_INFO_STRUCT mSphParamInfo;


};   //SpeechParamParser

}   //namespace android

#endif   //_SPEECH_ANC_CONTROL_H_
