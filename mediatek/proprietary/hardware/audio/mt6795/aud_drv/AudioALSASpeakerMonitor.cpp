#if defined(MTK_SPEAKER_MONITOR_SUPPORT)
#define MTK_LOG_ENABLE 1
#include <unistd.h>
#include <sched.h>
#include <sys/prctl.h>
#include <stdint.h>
#include <sys/types.h>
#include <sys/resource.h>
#include <cutils/log.h>
#include <stdio.h>
#include "AudioALSAStreamManager.h"

#include "AudioALSAStreamOut.h"
#include "AudioALSAStreamIn.h"

#include "AudioALSACaptureHandlerBase.h"
#include "AudioALSACaptureHandlerNormal.h"

#include "AudioALSASpeechPhoneCallController.h"
#include "AudioALSAVolumeController.h"
#include "AudioALSADriverUtility.h"
#include "AudioALSAHardwareResourceManager.h" 
#include "AudioALSASpeakerMonitor.h"
#include "AudioCustParam.h"
#include "AudioUtility.h"
#include <hardware_legacy/power.h>

#ifdef __cplusplus
extern "C" {
#include "Audio_FFT_Types.h"
#include "Audio_FFT.h"
}
#endif

#define LOG_TAG "AudioALSASpeakerMonitor"

#include <tinyalsa/asoundlib.h> 
//#define DISCONTINUOUS_SAMPLE
#define USE_MULTI_CANDIDATE
//#define EXTRA_DUMP_I_V
//#define EXTRA_DUMP_Z
#define FFT_SIZE 4096
#define READ_STREAM_LENGTH 4096 
#if defined(DISCONTINUOUS_SAMPLE)
    #define DROP_SAMPLES (READ_STREAM_LENGTH*4)
#else
    #define DROP_SAMPLES (READ_STREAM_LENGTH*40)
#endif
#define INITIAL_CURRENT_SENSING_RESITOR (0.4f)
#define PHASE_INVERSE (1.0f)
#define R_INIT (8.26f)
#define T_INIT (25.0f)
#define MAG_VALIDE_LOWER 100
#define MAG_VALIDE_LOWER_AT_F0 100
#define TEMP_LOG_MAX 10
#define UNIT_GAIN 0x10000
#define DIGITAL_GAIN_POINT_THREE_TABLE_LENGTH 49
#define CADIDATE_NUMBER 5
#define FC_SHIFT_HIGH 300
#define FC_SHIFT_LOW 100

static const char SPEAKER_MONITOR_WAKELOCK_NAME[] = "SPEAKER_MONITOR_WAKELOCK_NAME";
static const char *streamInDumpName = "/sdcard/mtklog/StreamIn_SpkMonitor.pcm";    // ADC
static const char *ZLOG = "/sdcard/mtklog/Z_real.bin";    // ADC
static const char *VLOG = "/sdcard/mtklog/V_real.bin";    // ADC
static const char *ILOG = "/sdcard/mtklog/I_real.bin";    // ADC

float gain_control_table[TEMP_LOG_MAX];
short gain_control_idx = 0;
short gain_control_len = 0;
short gain_control_state = 0;
/*in size of bytes*/
const unsigned short digital_gain_point_three_db_table[DIGITAL_GAIN_POINT_THREE_TABLE_LENGTH]=
{
    65535,//0
    63311,// -0.3
    61161,// -0.6
    59085,// -0.9
    57079,// -1.2
    55141,// -1.5
    53269,// -1.8
    51461,// -2.1
    49714,// -2.4
    48026,// -2.7
    46395,// -3
    44820,// -3.3
    43299,// -3.6
    41829,// -3.9
    40409,// -4.2
    39037,// -4.5
    37712,// -4.8
    36431,// -5.1
    35194,// -5.4
    34000,// -5.7
    32845,// -6 
    31730,// -6.3
    30653,// -6.6
    29612,// -6.9
    28607,// -7.2
    27636,// -7.5
    26698,// -7.8
    25791,// -8.1
    24916,// -8.4
    24070,// -8.7
    23253,// -9.0
    22463,// -9.3
    21701,// -9.6
    20964,// -9.9
    20252,// -10.2
    19565,// -10.5
    18900,// -10.8
    18259,// -11.1
    17639,// -11.4
    17040,// -11.7
    16461,// -12.0
    15903,// -12.3
    15363,// -12.6
    14841,// -12.9
    14337,// -13.2
    13850,// -13.5
    13380,// -13.8
    12926,// -14.1
    12487// -14.4
};

namespace android
{
uint32_t AudioALSASpeakerMonitor::mDumpFileNum = 0;

static unsigned short CheckF0Change(Complex *ComData, uint32_t *magData, unsigned short len, unsigned short reso_fc)
{
    int i, r_max_idx = -1;
    unsigned short fc_shift_low, fc_shift_high, ori_fc_idx;
    float r_max = 0.0f;
    if(reso_fc > FC_SHIFT_LOW)
        fc_shift_low = reso_fc - FC_SHIFT_LOW;
    else
        fc_shift_low = 0;
    fc_shift_high = reso_fc + FC_SHIFT_HIGH;
    ori_fc_idx = (reso_fc * 4096 / 48000);
    fc_shift_low = (fc_shift_low * 4096 / 48000);
    fc_shift_high = (fc_shift_high * 4096 / 48000);
    for(i = fc_shift_low ; i < fc_shift_high ; i++)
    {
        if(magData[i] > MAG_VALIDE_LOWER_AT_F0 && ComData[i].real > r_max)
        {
            r_max = ComData[i].real;
            r_max_idx = i;
            ALOGD("SearchF0 %d %d, mag %d, R %f", r_max_idx, (r_max_idx * 48000/4096), magData[i], r_max);
        }
    }
    if(r_max_idx != -1)
    {
        return (r_max_idx * 48000/4096);
    }
    else
        return reso_fc;
}

static float averageCandidateTemp(float *Candidate, short cadidateFound)
{
    short minIdx = -1, maxIdx = -1, i;
    float minValue = 999.0f, maxValue = 0.0f, averageValue = 0.0f;
    
    if(cadidateFound >= 4)
    {
        for( i = 0; i < cadidateFound ; i++)
        {
            if(Candidate[i] > maxValue)
            {
                maxValue = Candidate[i];
                maxIdx = i;
            }
            if(Candidate[i] < minValue)
            {
                minValue = Candidate[i];
                minIdx = i;
            }
        }
        ALOGD("max = %d min = %d", maxIdx, minIdx);
        for( i = 0; i < cadidateFound ; i++)
        {
            if(i != maxIdx && i != minIdx)
            {
                averageValue += Candidate[i];
            }
        }
        averageValue = averageValue/(cadidateFound-2);
    }
    else if(cadidateFound > 0)
    {
        for( i = 0; i < cadidateFound ; i++)
        {
            averageValue += Candidate[i];
        }
        averageValue = averageValue/(cadidateFound);
        ALOGD("averageCandidateTemp not enough candidate");
    }
    return averageValue;
}
static float estimateTemperature(float r_init, float t_init, float r_now)
{
    return ((r_now / r_init) - 1) * 234.5f + (r_now / r_init) * t_init;
}
static int searchLowerGain(int gain_now)
{
    int i, gain_new = gain_now;
    for( i = 0 ; i < DIGITAL_GAIN_POINT_THREE_TABLE_LENGTH ; i++ )
    {
        if(gain_new > digital_gain_point_three_db_table[i])
        {
            gain_new = digital_gain_point_three_db_table[i]; 
            break;
        }
    }
    return gain_new;
}

static int searchUpperGain(int gain_now)
{
    int i, gain_new = gain_now;
    for( i = DIGITAL_GAIN_POINT_THREE_TABLE_LENGTH-1 ; i>=0 ; i-- )
    {
        if(gain_new < digital_gain_point_three_db_table[i])
        {
            gain_new = digital_gain_point_three_db_table[i]; 
            break;
        }
    }
    return gain_new;
}

static void CalSpkMntrGain(float lower_bound, float upper_bound, float *temp_log, float temp_now, short *temp_log_length, int *gain_now)
{
    short i;
    ALOGD("CalSpkMntrGain, gain = %d, t= %f, len %d", *gain_now, temp_now, *temp_log_length);
    if(temp_now < lower_bound)
    {
        for(i = 0 ; i < TEMP_LOG_MAX ; i++)
        {
            temp_log[i] = T_INIT;
        }
        *temp_log_length = 0;
        /*Check if gain should increase*/
        
        if(gain_control_state == 1 || gain_control_state == 2)
        {
            gain_control_state = 2;// gain graduate increase state;
            *gain_now = searchUpperGain(*gain_now);
        }
        if(*gain_now == UNIT_GAIN)
            gain_control_state = 0;
        gain_control_idx = 0;
        gain_control_len = 0;
        
        ALOGD("CalSpkMntrGain gain %d len %d", *gain_now, *temp_log_length);
    }
    else if(temp_now >= lower_bound )
    {//Keep monitoring or update gain?
        short log_length = *temp_log_length;
        int gain_new;
        if(log_length == TEMP_LOG_MAX)
        {
            for(i = 0 ; i < log_length-1; i++)
            {
                temp_log[i] = temp_log[i+1];
            }
            temp_log[log_length-1] = temp_now; //Latest log at tail
        }
        else
        {
            temp_log[log_length] = temp_now;
            log_length++;
            *temp_log_length = log_length;
        }
        if(temp_now >= upper_bound)
        { //Update Gain

            int gain_new;
            double x = (lower_bound/temp_log[0]);
#if 0
            gain_control_table[0] = float(x);
            for(i = 1 ; i < log_length ; i++)
            {
                x *= sqrt((temp_log[i-1]/temp_log[i]));
                gain_control_table[i] = x;
                ALOGD("x = %f %f %f", x, temp_log[i-1], temp_log[i]);
            }
#else
            if(gain_control_state == 1)// Already control attenuate state;
            {
                gain_new = searchLowerGain(*gain_now);
                ALOGD("gain_control_state = 1, gain_new = %d", gain_new);
            }
            else
            {
                x = (lower_bound/upper_bound);
                gain_new = (int)(x * (*gain_now));
                ALOGD("gain_control_state = %d, gain_new = %d", gain_control_state, gain_new);
            }
#endif
            gain_control_state = 1; // In control attenuate state;
            gain_control_len = log_length;
            gain_control_idx = 1;
            
            if(gain_new > UNIT_GAIN)
                gain_new = UNIT_GAIN;
            if(*gain_now == UNIT_GAIN || *gain_now > gain_new)
            { // Not in control gain mode or need more attenuate
                *gain_now = gain_new;
            }
            ALOGD("gain_new = %d", gain_new);
        }
        else if(gain_control_state == 1){ // gain attenuation mode
            *gain_now = searchLowerGain(*gain_now);
        }
        else if(gain_control_state == 2){ // gain increase mode
            *gain_now = searchUpperGain(*gain_now);
        }
    }
}
static void *SpeakerMonitorThread(void *arg)
{
    uint32_t device = AUDIO_DEVICE_IN_SPK_FEED;
    int format = AUDIO_FORMAT_PCM_16_BIT;
    uint32_t channel = AUDIO_CHANNEL_IN_STEREO;
    uint32_t sampleRate = 48000;
    status_t status = 0;
    int ret, gain_now = 0x10000;
    struct timeval now;
    struct timespec timeout;
    unsigned short new_F0, pre_F0;
    
#if defined(EXTRA_DUMP_Z)
    FILE *fp_z = NULL;
#endif
#if defined(EXTRA_DUMP_I_V)
    FILE *fp_v = NULL, *fp_i = NULL;
#endif
    int nRead, nNeed =0;
    
    android_audio_legacy::AudioStreamIn *streamInput = NULL;
    
	SLOGD("SpeakerMonitorThread in +");
    AudioALSASpeakerMonitor *pSpkMonitor = (AudioALSASpeakerMonitor*)arg;
    if(pSpkMonitor == NULL) {
        SLOGE("SpeakerMonitorThread pSpkMonitor = NULL arg = %x", arg);
        return 0;
    }
    
    pthread_mutex_lock(&pSpkMonitor->mSpkMonitorMutex);
    pSpkMonitor->m_bThreadExit = false;
    // Adjust thread priority
    prctl(PR_SET_NAME, (unsigned long)"SpeakerMonitorThread", 0, 0, 0);
    setpriority(PRIO_PROCESS, 0, ANDROID_PRIORITY_AUDIO);
	
    
    // ----start the loop --------
    pSpkMonitor->m_bThreadExit = false;

    SLOGD("pthread_cond_signal(&pSpkMonitor->mSpkMonitor_Cond)");
    pthread_cond_signal(&pSpkMonitor->mSpkMonitor_Cond); // wake all thread
    pthread_mutex_unlock(&pSpkMonitor->mSpkMonitorMutex);
    
    short readBuffer[(FFT_SIZE*2)] = {0};//for record
    kal_uint32 magData[FFT_SIZE/2], freqData[2], maxFreqIdx, maxFreq;
    short currentBuffer[FFT_SIZE], voltageBuffer[FFT_SIZE];
    Complex ComData_I[FFT_SIZE], ComData_V[FFT_SIZE];
    AUDIO_SPEAKER_MONITOR_PARAM_STRUCT SpkParam;
    GetSpeakerMonitorParamFromNVRam(&SpkParam);
    new_F0 = pre_F0 = SpkParam.reso_freq_center;
#if 1
    ALOGD("R0 = %f, T0= %f R = %f, timer = %d", 
        SpkParam.resistor[100],
        SpkParam.temp_initial,
        SpkParam.current_sensing_resistor,
        SpkParam.monitor_timer
        );
#endif
    char *pReadBuffer = (char *)readBuffer;
    short i, temp_log_idx =0;
    kal_uint32 tempMax = 0, tempMaxIdx;
    kal_uint32 tempCandidate[CADIDATE_NUMBER], tempCandidateMag[CADIDATE_NUMBER];
    short j, k, cadidateFound = 0;
    float r_initial = R_INIT, t_initial = T_INIT, t_now = T_INIT;
    float temp_log[TEMP_LOG_MAX], t_lower_bound, t_upper_bound;
    for(i = 0; i < TEMP_LOG_MAX ; i++)
        temp_log[i] = T_INIT;
    t_lower_bound = (float)SpkParam.temp_limit_low;
    t_upper_bound = (float)SpkParam.temp_limit_high;
    pSpkMonitor->SetTempLowerBound((short)SpkParam.temp_limit_low);
    pSpkMonitor->SetTempUpperBound((short)SpkParam.temp_limit_high);
#if defined(USE_MULTI_CANDIDATE)
    for(k = 0; k < CADIDATE_NUMBER ; k++)
    {
        tempCandidate[k] = 0;
        tempCandidateMag[k] = 0;
    }
#endif
    while(!pSpkMonitor->m_bThreadExit) {
        t_lower_bound = pSpkMonitor->GetTempLowerBound(); // Update boundary if tool or audio command update
        t_upper_bound = pSpkMonitor->GetTempUpperBound();
        if(pSpkMonitor->m_bActivated == true)
        {
            if(streamInput == NULL)
            {
                streamInput = pSpkMonitor->getStreamManager()->openInputStream(device, &format, &channel, &sampleRate, &status, (android_audio_legacy::AudioSystem::audio_in_acoustics)0);
                ASSERT(streamInput != NULL);
                pSpkMonitor->OpenPCMDump(LOG_TAG);
                acquire_wake_lock(PARTIAL_WAKE_LOCK, SPEAKER_MONITOR_WAKELOCK_NAME);
            }
            //memset(readBuffer, 0, sizeof(readBuffer));
            nRead = streamInput->read(pReadBuffer, READ_STREAM_LENGTH);
            nNeed += nRead;
#if 1
            if(nNeed > DROP_SAMPLES){ //Drop first samples
                pSpkMonitor->WritePcmDumpData((void *)pReadBuffer, nRead);
            }
#else
            if( fp == NULL )
            {
                fp = fopen(streamInDumpName, "wb");
            }
            if(fp != NULL)
            {
                if(nNeed > DROP_SAMPLES){ //Drop first samples
                    fwrite((void *)pReadBuffer, sizeof(char), nRead, fp);
                }
                //ALOGD("SpkMonitor fwrite %d nNeed %d", nRead, nNeed);
            }
#endif
            if(nNeed > DROP_SAMPLES){ //Drop first samples
                pReadBuffer += READ_STREAM_LENGTH;
            }
#if defined(EXTRA_DUMP_Z)
            if( fp_z == NULL )
            {
                fp_z = fopen(ZLOG, "w");
            }
#endif
#if defined(EXTRA_DUMP_I_V)
            if( fp_v == NULL )
            {
                fp_v = fopen(VLOG, "w");
                fp_i = fopen(ILOG, "w");
            }
#endif
#if 1
            if(nNeed >= (FFT_SIZE * 2 * sizeof(short) + DROP_SAMPLES))
            {
#if defined(DISCONTINUOUS_SAMPLE)
                streamInput->standby();
#endif
                //Do speaker monitor and control
                
                for(i = 0; i < FFT_SIZE ;i++)
                {
                    voltageBuffer[i] = readBuffer[i*2];
                    currentBuffer[i] = readBuffer[i*2+1];
                }
                ApplyFFT(48000, currentBuffer, 0, ComData_I, freqData, magData);
                ApplyFFT(48000, voltageBuffer, 0, ComData_V, freqData, magData);
                maxFreqIdx = freqData[0];
                maxFreq = maxFreqIdx * ((float)48000/4096);
#if defined(EXTRA_DUMP_Z)
                if(fp_z != NULL)
                {
                    ALOGD("maxFreqIdx = %d, freq = %d mag = %d", maxFreqIdx, maxFreq, magData[maxFreqIdx]);
                }
#endif
#if defined(EXTRA_DUMP_I_V)
                if(fp_v != NULL)
                {
                    fwrite((void *)ComData_V, sizeof(Complex), FFT_SIZE/2, fp_v);
                    fwrite((void *)ComData_I, sizeof(Complex), FFT_SIZE/2, fp_i);
                }
#endif
                for(i = 0; i < FFT_SIZE/2 ;i++)
                {
                    int result;
                    result = comp_divs(&ComData_V[i], ComData_I[i], 0.0000001);
#if 0
                    ComData_V[i].real *= (INITIAL_CURRENT_SENSING_RESITOR * PHASE_INVERSE);
                    ComData_V[i].image *= (INITIAL_CURRENT_SENSING_RESITOR * PHASE_INVERSE);
#else
                    ComData_V[i].real *= (SpkParam.current_sensing_resistor * PHASE_INVERSE);
                    ComData_V[i].image *= (SpkParam.current_sensing_resistor * PHASE_INVERSE);
#endif
                }
                // Check F0 change
                new_F0 = CheckF0Change(ComData_V, magData, FFT_SIZE/2, SpkParam.reso_freq_center);
                if( SpkParam.reso_freq_center != new_F0 && new_F0 != pre_F0)
                {
                    kal_uint32 center_freq, bw;
                    kal_int32 threshold;
                    pSpkMonitor->GetFilterParam(&center_freq, &bw, &threshold);
                    ALOGD("F0 %d change to %d bw %d, th %d", SpkParam.reso_freq_center, new_F0, bw, threshold);
                    pSpkMonitor->getStreamManager()->setSpkFilterParam(new_F0, bw, threshold);
                    pre_F0 = new_F0;
                }
                //Find suitable range
                tempMax = 0;
                tempMaxIdx = 0xFFFF;
#if 0
                SpkParam.prefer_band_lower = 35;
                SpkParam.prefer_band_upper = 90;
#endif
                ALOGD("looking %d %d %d %d", SpkParam.prefer_band_lower, SpkParam.prefer_band_upper, (SpkParam.prefer_band_lower<<2), (SpkParam.prefer_band_upper<<2));
#if defined(USE_MULTI_CANDIDATE)
                for(k = 0; k < CADIDATE_NUMBER ; k++)
                {
                    tempCandidate[k] = 0;
                    tempCandidateMag[k] = 0;
                }
#endif
                for(i = (SpkParam.prefer_band_lower << 2); i < 2048/*(SpkParam.prefer_band_upper << 2)*/;i++)
                {
#if defined(USE_MULTI_CANDIDATE)
                    if(magData[i] < MAG_VALIDE_LOWER || SpkParam.resistor[i>>2] < 4.0f)
                        continue;
                    for(j = 0; j < CADIDATE_NUMBER ; j++)
                    {
                        
                        if(magData[i] > tempCandidateMag[j])
                        {
                            for(k = CADIDATE_NUMBER - 1; k > j ; k--)
                            {
                                tempCandidate[k] = tempCandidate[k-1];
                                tempCandidateMag[k] = tempCandidateMag[k-1];
                            }
                            tempCandidate[j] = i;
                            tempCandidateMag[j] = magData[i];
                            break;
                        }
                    }
#else 
                    if(magData[i] > tempMax && magData[i] > MAG_VALIDE_LOWER && SpkParam.resistor[i>>2] >=4.0f /*a Check, no initial resistor should smaller than this*/)
                    {
                        tempMax = magData[i];
                        tempMaxIdx = i;
                    }
#endif
                }
#if defined(USE_MULTI_CANDIDATE)
                cadidateFound = 0;
                for(i = 0; i < CADIDATE_NUMBER ; i++)
                {
                    if(tempCandidateMag[i] != 0)
                        cadidateFound++;
                }
                if(cadidateFound < CADIDATE_NUMBER)
                {
                    //1. Find one candidate from lower band
                    for(i = 0; i < (SpkParam.prefer_band_lower << 2);i++)
                    {
                        if(magData[i] > tempMax && magData[i] > MAG_VALIDE_LOWER && SpkParam.resistor[i>>2] >=4.0f /*a Check, no initial resistor should smaller than this*/)
                        {
                            tempMax = magData[i];
                            tempMaxIdx = i;
                        }
                    }
                    // 2. If found, insert to candidate
                    if(tempMaxIdx != 0xFFFF)
                    {
                        for(j = 0; j < CADIDATE_NUMBER ; j++)
                        {
                            if(tempMax > tempCandidateMag[j])
                            {
                                for(k = CADIDATE_NUMBER - 1; k > j ; k--)
                                {
                                    tempCandidate[k] = tempCandidate[k-1];
                                    tempCandidateMag[k] = tempCandidateMag[k-1];
                                }
                                tempCandidate[j] = tempMaxIdx;
                                tempCandidateMag[j] = tempMax;
                                cadidateFound++;
                                break;
                            }
                        }
                    }
                }
                ALOGD("Candi %d %d %d %d %d", tempCandidate[0], tempCandidate[1], tempCandidate[2], tempCandidate[3], tempCandidate[4]);
#else
                if(tempMaxIdx != 0xFFFF)
                {
#if 0
                    r_initial = 8.26f;
                    t_initial = T_INIT;
#else
                    r_initial = SpkParam.resistor[(tempMaxIdx>>2)];
                    t_initial = SpkParam.temp_initial;
#endif
                    maxFreq = tempMaxIdx * ((float)48000/4096);
                    ALOGD("tempMaxIdx = %d, freq = %d mag = %d, r_initial = %f", tempMaxIdx, maxFreq, magData[tempMaxIdx], r_initial);
                }
#endif
#if defined(EXTRA_DUMP_Z)
                if(fp_z != NULL)
                {
                    fwrite((void *)ComData_V, sizeof(Complex), FFT_SIZE/2, fp_z);
                }
#endif
#if defined(USE_MULTI_CANDIDATE)
                float tempCandidateTemp[CADIDATE_NUMBER];
                for(i = 0; i < cadidateFound; i++)
                {
                    r_initial = SpkParam.resistor[(tempCandidate[i]>>2)];
                    t_initial = SpkParam.temp_initial;
                    tempCandidateTemp[i] = estimateTemperature(r_initial, t_initial, ComData_V[tempCandidate[i]].real);
                    ALOGD("candidate %d, degree %f R0 %f R %f", i, tempCandidateTemp[i], r_initial, ComData_V[tempCandidate[i]].real);
                }
                if(cadidateFound > 0)
                {
                    t_now = averageCandidateTemp(tempCandidateTemp, cadidateFound);
                    ALOGD("candidateTmpAvg, %f ", t_now);
                }
                else //low signal
                {
                    if(t_now > t_lower_bound)
                        t_now -= 10;
                }
#else
                if(tempMaxIdx != 0xFFFF)
                {
                    if(ComData_V[tempMaxIdx].real <= 0.0f)
                    {
                        ALOGD("ComData_Z Invalid %f", ComData_V[tempMaxIdx].real);
                        //t_now is not updated;
                    }
                    else
                    {
                        t_now = estimateTemperature( r_initial, t_initial, ComData_V[tempMaxIdx].real);
                        ALOGD("ComData_Z %f, T = %f", ComData_V[tempMaxIdx].real, t_now);
                    }
                }
                else
                {
                    ALOGD("ComData_Z Invalid(low_energy)");
                }
#endif
                //End of speaker monitor and control
                /* speaker control start*/
                CalSpkMntrGain(t_lower_bound, t_upper_bound, temp_log, t_now, &temp_log_idx, &gain_now);
                ALOGD("After CalSpkMntrGain gain = %d, t= %f, len %d", gain_now, t_now, temp_log_idx);
                pSpkMonitor->getStreamManager()->setSpkOutputGain(gain_now, 48000 * 11 /12);
                
                /* speaker control end */
                nNeed = 0;
                pReadBuffer = (char *)readBuffer;
#if defined(DISCONTINUOUS_SAMPLE)
                gettimeofday(&now, NULL);
                timeout.tv_sec = now.tv_sec + 1;
                timeout.tv_nsec = now.tv_usec * 800;
                pthread_mutex_lock(&pSpkMonitor->mSpkMonitorMutex);
                ALOGD("-mSpkMonitorActivate_Cond wait");
                ret = pthread_cond_timedwait(&pSpkMonitor->mSpkMonitorActivate_Cond, &pSpkMonitor->mSpkMonitorMutex, &timeout);
                ALOGD("-mSpkMonitorActivate_Cond receive ret=%d", ret);
                pthread_mutex_unlock(&pSpkMonitor->mSpkMonitorMutex);
#endif
            }
#endif
        }
		else
        {
            if(streamInput != NULL)
            {
                streamInput->standby();
                pSpkMonitor->getStreamManager()->closeInputStream(streamInput);
                release_wake_lock(SPEAKER_MONITOR_WAKELOCK_NAME);
                streamInput = NULL;
            }
            pSpkMonitor->ClosePCMDump();
#if defined(EXTRA_DUMP_Z)
            if( fp_z != NULL )
            {
                fclose(fp_z);
                fp_z = NULL;
            }
#endif
#if defined(EXTRA_DUMP_I_V)
            if(fp_v != NULL)
            {
                fclose(fp_v);
                fp_v = NULL;
                fclose(fp_i);
                fp_i = NULL;
            }
#endif
            nNeed = 0;
            pReadBuffer = (char *)readBuffer;
            for(i = 0; i < TEMP_LOG_MAX ; i++)
                temp_log[i] = T_INIT;
            temp_log_idx = 0;
            gain_now = UNIT_GAIN;
            pSpkMonitor->getStreamManager()->setSpkOutputGain(UNIT_GAIN, 48000);
            gettimeofday(&now, NULL);
            timeout.tv_sec = now.tv_sec + 60;
            timeout.tv_nsec = now.tv_usec * 1000;
            pthread_mutex_lock(&pSpkMonitor->mSpkMonitorMutex);
            pthread_cond_timedwait(&pSpkMonitor->mSpkMonitorActivate_Cond, &pSpkMonitor->mSpkMonitorMutex, &timeout);
            pthread_mutex_unlock(&pSpkMonitor->mSpkMonitorMutex);
        }
    }
    if(streamInput != NULL)
    {
        streamInput->standby();
        pSpkMonitor->getStreamManager()->closeInputStream(streamInput);
        release_wake_lock(SPEAKER_MONITOR_WAKELOCK_NAME);
    }
    
    //exit thread
    pthread_mutex_lock(&pSpkMonitor->mSpkMonitorMutex);
    SLOGD("pthread_cond_signal(&pSpkMonitor->mSpkMonitor_Cond)");
    pthread_cond_signal(&pSpkMonitor->mSpkMonitor_Cond); // wake all thread
    pthread_mutex_unlock(&pSpkMonitor->mSpkMonitorMutex);

    return 0;
}

AudioALSASpeakerMonitor *AudioALSASpeakerMonitor::UniqueInstance = NULL;
AudioALSASpeakerMonitor *AudioALSASpeakerMonitor::getInstance()
{
    static AudioLock mGetInstanceLock;
    AudioAutoTimeoutLock _l(mGetInstanceLock);

    if (UniqueInstance == NULL)
    {
        UniqueInstance = new AudioALSASpeakerMonitor();
    }
    ASSERT(UniqueInstance != NULL);
    return UniqueInstance;
}

AudioALSASpeakerMonitor::AudioALSASpeakerMonitor()
{
    int ret;
    ALOGD("%s()", __FUNCTION__);
    AUDIO_SPEAKER_MONITOR_PARAM_STRUCT SpkParam;
    m_bEnabled = false;
    m_bThreadExit = false;
    m_bActivated = false;
    mTempUpperBound = 120;
    mTempLowerBound = 100;
    GetSpeakerMonitorParamFromNVRam(&SpkParam);
    mNotchFC = (unsigned int)SpkParam.reso_freq_center;
    mNotchBW = (unsigned int)SpkParam.reso_freq_bw;
    //SpkParam.reso_freq_gain = 0xFFE0;
    mNotchTH = (int)((short)SpkParam.reso_freq_gain);
    mNotchTH = (mNotchTH << 8); // in Q24.8 format;
    mPCMDumpFile = NULL;
    mAudioMtkStreamManager = AudioALSAStreamManager::getInstance();
    
    ret = pthread_mutex_init(&mSpkMonitorMutex, NULL);
    if (ret != 0)
    {
        SLOGE("Failed to initialize mSpkMonitorMutex!");
    }

    ret = pthread_cond_init(&mSpkMonitor_Cond, NULL);
    if (ret != 0)
    {
        SLOGE("Failed to initialize mSpkMonitor_Cond!");
    }
    
    ret = pthread_cond_init(&mSpkMonitorActivate_Cond, NULL);
    if (ret != 0)
    {
        SLOGE("Failed to initialize mSpkMonitorActivate_Cond!");
    }
}


AudioALSASpeakerMonitor::~AudioALSASpeakerMonitor()
{
    ALOGD("%s()", __FUNCTION__);
    pthread_cond_destroy(&mSpkMonitorActivate_Cond);
    pthread_cond_destroy(&mSpkMonitor_Cond);
}

// gain: Q24.8
status_t AudioALSASpeakerMonitor::GetFilterParam(unsigned int *center_freq, unsigned int *bw, int *threshold)
{
    *center_freq = (unsigned int)mNotchFC;
    *bw = (unsigned int)mNotchBW;
    *threshold = mNotchTH;
    //ALOGD("%s(), mNotchFC %d mNotchBW %d, mNotchTH %d", __FUNCTION__, mNotchFC, mNotchBW, mNotchTH);
    return NO_ERROR;
}

status_t AudioALSASpeakerMonitor::SetTempLowerBound(short degree)
{
    ALOGD("%s(), %d degree", __FUNCTION__, degree);
    AUDIO_SPEAKER_MONITOR_PARAM_STRUCT SpkParam;
    GetSpeakerMonitorParamFromNVRam(&SpkParam);
    SpkParam.temp_limit_low = (unsigned short)degree;
    mTempLowerBound = degree;
    SetSpeakerMonitorParamToNVRam(&SpkParam);
    return NO_ERROR;
}
status_t AudioALSASpeakerMonitor::SetTempUpperBound(short degree)
{
    ALOGD("%s(), %d degree", __FUNCTION__, degree);
    AUDIO_SPEAKER_MONITOR_PARAM_STRUCT SpkParam;
    GetSpeakerMonitorParamFromNVRam(&SpkParam);
    SpkParam.temp_limit_high = (unsigned short)degree;
    mTempUpperBound = degree;
    SetSpeakerMonitorParamToNVRam(&SpkParam);
    return NO_ERROR;
}

short AudioALSASpeakerMonitor::GetTempLowerBound(void)
{
    return mTempLowerBound;
}

short AudioALSASpeakerMonitor::GetTempUpperBound(void)
{
    return mTempUpperBound;
}
status_t AudioALSASpeakerMonitor::Activate(void)
{
    ALOGD("%s()", __FUNCTION__);
    char value[PROPERTY_VALUE_MAX];
    int result = 0;
    if(m_bEnabled != true)
    {
        return NO_ERROR;
    }
    property_get("speakermonitor.bypass", value, "0");
    result = atoi(value);
    if(result == 1)
    {
        return NO_ERROR;
    }
    m_bActivated = true;
    pthread_cond_signal(&mSpkMonitorActivate_Cond); // wake all thread
    return NO_ERROR;
}

status_t AudioALSASpeakerMonitor::Deactivate(void)
{
    ALOGD("%s()", __FUNCTION__);
    char value[PROPERTY_VALUE_MAX];
    int result = 0;
    if(m_bEnabled != true)
        return NO_ERROR;
    property_get("speakermonitor.bypass", value, "0");
    result = atoi(value);
    if(result == 1)
    {
        return NO_ERROR;
    }
    m_bActivated = false;
    pthread_cond_signal(&mSpkMonitorActivate_Cond); // wake all thread
    return NO_ERROR;
}

status_t AudioALSASpeakerMonitor::EnableSpeakerMonitorThread(bool enable)
{
    struct timeval now;
    struct timespec timeout;
    gettimeofday(&now, NULL);
    timeout.tv_sec = now.tv_sec + 3;
    timeout.tv_nsec = now.tv_usec * 1000;
    int ret;
    ALOGD("%s()", __FUNCTION__);
    AudioAutoTimeoutLock _l(mLock);
    if(enable == true && m_bEnabled == false)
    {
        ALOGD("open SpeakerMonitorThread");
        //Echo reference path
        if (mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(AudioALSADriverUtility::getInstance()->getMixer(), "Audio_Speaker_Protection_SRAM_Switch"), "On"))
        {
            ALOGE("Error: Audio_Speaker_Protection_SRAM_Switch invalid value");
        }
        pthread_mutex_lock(&mSpkMonitorMutex);
        ret = pthread_create(&mSpeakerMonitorThreadID, NULL, SpeakerMonitorThread, (void *)this);
        if (ret != 0)
        {
            ALOGE("EnableSpeakerMonitorThread pthread_create error!!");
        }

        ALOGD("+mSpkMonitor_Cond wait");
        ret = pthread_cond_timedwait(&mSpkMonitor_Cond, &mSpkMonitorMutex, &timeout);
        ALOGD("-mSpkMonitor_Cond receive ret=%d", ret);
        m_bEnabled = true;
        pthread_mutex_unlock(&mSpkMonitorMutex);
    }
    else if(enable == false && m_bEnabled == true)
    { 
        //stop thread
        ALOGD("close EnableSpeakerMonitorThread");
        pthread_mutex_lock(&mSpkMonitorMutex);
        if (!m_bThreadExit)
        {
            m_bThreadExit = true;
            ALOGD("+mSpkMonitorActivate_Cond signal in disable %s()", __FUNCTION__);
            pthread_cond_signal(&mSpkMonitorActivate_Cond); // wake all thread
            ALOGD("+mSpkMonitor_Cond wait");
            ret = pthread_cond_timedwait(&mSpkMonitor_Cond, &mSpkMonitorMutex, &timeout);
            ALOGD("-mSpkMonitor_Cond receive ret=%d", ret);
        }
        m_bEnabled = false;
        pthread_mutex_unlock(&mSpkMonitorMutex);
        if (mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(AudioALSADriverUtility::getInstance()->getMixer(), "Audio_Speaker_Protection_SRAM_Switch"), "Off"))
        {
            ALOGE("Error: Audio_Speaker_Protection_SRAM_Switch invalid value");
        }
    }
    return NO_ERROR;
}

void AudioALSASpeakerMonitor::OpenPCMDump(const char *class_name)
{
    ALOGV("%s()", __FUNCTION__);
    char mDumpFileName[128];
    sprintf(mDumpFileName, "%s.%d.%s.pcm", streaminSpk, mDumpFileNum, class_name);

    //property_set(streaminSpk_propty, "1");
    mPCMDumpFile = NULL;
    mPCMDumpFile = AudioOpendumpPCMFile(mDumpFileName, streaminSpk_propty);

    if (mPCMDumpFile != NULL)
    {
        ALOGD("%s DumpFileName = %s", __FUNCTION__, mDumpFileName);

        mDumpFileNum++;
        mDumpFileNum %= MAX_DUMP_NUM;
    }
}

void AudioALSASpeakerMonitor::ClosePCMDump()
{
    ALOGV("%s()", __FUNCTION__);
    if (mPCMDumpFile)
    {
        AudioCloseDumpPCMFile(mPCMDumpFile);
        ALOGD("%s(), close it", __FUNCTION__);
    }
}

void AudioALSASpeakerMonitor::WritePcmDumpData(const void *buffer, ssize_t bytes)
{
    if (mPCMDumpFile)
    {
        //ALOGD("%s()", __FUNCTION__);
        AudioDumpPCMData((void *)buffer , bytes, mPCMDumpFile);
    }
}

} // end of namespace android
#endif //end of defined(MTK_SPEAKER_MONITOR_SUPPORT)
    
