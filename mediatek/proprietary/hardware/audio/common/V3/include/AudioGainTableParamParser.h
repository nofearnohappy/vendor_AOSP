#ifndef _AUDIO_GAIN_TABLE_PARAM_PARSER_H_
#define _AUDIO_GAIN_TABLE_PARAM_PARSER_H_

/*****************************************************************************
*                E X T E R N A L   R E F E R E N C E S
******************************************************************************
*/

#include <utils/Errors.h>
#include <vector>

//extern "C" {
#include "AudioParamParser.h"
//}

#include "AudioGainTableParam.h"

namespace android
{

class GainTableParamParser
{
    public:
        virtual ~GainTableParamParser();
        static GainTableParamParser *getInstance();

        status_t loadGainTableParam();
        status_t loadGainTableSpec();
        status_t loadGainTableMapDl();

        status_t getGainTableParam(GainTableParam *_gainTable);
        status_t getGainTableSpec(GainTableSpec *_gainTableSpec);

        status_t updatePlaybackDigitalGain(GainTableParam *_gainTable);
        status_t updatePlaybackAnalogGain(GainTableParam *_gainTable);
        status_t updateSpeechVol(GainTableParam *_gainTable);
        status_t updateRecordVol(GainTableParam *_gainTable);
        status_t updateVoIPVol(GainTableParam *_gainTable);

        /*
         * Utility functions
         */
        unsigned int bufferGainDb2Idx(int dB);
        unsigned int spkGainDb2Idx(int dB);

    private:
        GainTableParamParser();

        template<class T>
        status_t getParam(ParamUnit *_paramUnit, T *_param, const char *_paramName);
        status_t getParam(ParamUnit *_paramUnit, std::string *_param, const char *_paramName);

        template<class T>
        status_t getParamVector(ParamUnit *_paramUnit, std::vector<T> *_param, const char *_paramName);
        status_t getParamVector(ParamUnit *_paramUnit, std::vector<std::string> *_param, const char *_paramName);

        static GainTableParamParser *mGainTableParamParser;
        AppHandle *mAppHandle;

        GainTableSpec mSpec;

        // store mapping of DL total gain to seperate digital & analog gain
        std::vector<short> mMapDlDigital[NUM_GAIN_DEVICE];
        std::vector<short> mMapDlAnalog[NUM_GAIN_DEVICE];
        GAIN_ANA_TYPE      mMapDlAnalogType[NUM_GAIN_DEVICE];
};   //GainTableParamParser

}

#endif   //_AUDIO_GAIN_TABLE_PARAM_PARSER_H_