#ifndef AUDIO_UTILITY_H
#define AUDIO_UTILITY_H


#include <stdlib.h>
#include <stdio.h>
#include "AudioDef.h"
#include "AudioType.h"
#include <sys/types.h>
#include <unistd.h>
#include <sched.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <utils/Log.h>
#include <utils/String8.h>
#include <stdint.h>
#include <sys/stat.h>
#include <utils/Vector.h>
#include <utils/threads.h>
#include <utils/SortedVector.h>
#include <time.h>


#if defined(PC_EMULATION)
#include "Windows.h"
#include "WinBase.h"
#else
#include <hardware_legacy/AudioSystemLegacy.h>
#include <cutils/log.h>
#include <cutils/properties.h>
#endif

#include "AudioLock.h"

/*
    this function should implement the basic function for debug information
    or basic function proviede to audio hardware modules
*/

namespace android
{
#define MAX_DUMP_NUM (1024)

enum
{
    AUDIO_LOG_HARDWARE = 1,
    AUDIO_LOG_A2DPHARDWARE,
    AUDIO_LOG_STREAMOUT,
    AUDIO_LOG_STREAMIN,
    AUDIO_LOG_I2SSTREAMIN,
    AUDIO_LOG_VOLUMECONTROLLER,
    AUDIO_LOG_RESOURCEMANAGER,
    AUDIO_LOG_AUDIODEVCE,
    AUDIO_LOG_ANALOG,
    AUDIO_LOG_DIGITAL,
    AUDIO_LOG_AUDIOFLINGER,
    AUDIO_LOG_SPEECHCONTROL,
    AUDIO_LOG_AUDIOPOLICYSERVICE,
    AUDIO_LOG_AUDIOPOLICYANAGER,
    AUDIO_LOG_AUDIOMIXER
};

enum
{
    AUDIO_SUPPORT_DMIC = 1,
    AUDIO_SUPPORT_2IN1_SPEAKER,
    AUDIO_SUPPORT_VIBRATION_SPEAKER,
    AUDIO_SUPPORT_EXTERNAL_BUILTIN_MIC,
    AUDIO_SUPPORT_EXTERNAL_ECHO_REFERENCE,
};

class AudioTimeStamp
{
    public:
        AudioTimeStamp();
        ~AudioTimeStamp();
        void SetSystemTimeValid(const char str);
    private:
        struct timespec systemtime[30];
        String8 TimeString[30];
        const int MaxRecord = 30;
        int index;
};

struct RingBuf
{
    char *pBufBase;
    char *pRead;
    char *pWrite;
    int   bufLen;
};

static const char *streamout_ori = "/sdcard/mtklog/audio_dump/streamoutori_dump.pcm";
static const char *streamout_ori_propty = "streamout_ori.pcm.dump";
static const char *streamout_dcr = "/sdcard/mtklog/audio_dump/streamoutdcr_dump.pcm";
static const char *streamout_dcr_propty = "streamout_dcr.pcm.dump";

static const char *streamout_s2m = "/sdcard/mtklog/audio_dump/streamouts2m_dump.pcm";
static const char *streamout_s2m_propty = "streamout_s2m.pcm.dump";
static const char *streamout_acf = "/sdcard/mtklog/audio_dump/streamoutacf_dump.pcm";
static const char *streamout_acf_propty = "streamout_acf.pcm.dump";
static const char *streamout_hcf = "/sdcard/mtklog/audio_dump/streamouthcf_dump.pcm";
static const char *streamout_hcf_propty = "streamout_hcf.pcm.dump";

static const char *streamout = "/sdcard/mtklog/audio_dump/streamout.pcm";
static const char *streamoutfinal = "/sdcard/mtklog/audio_dump/streamoutfinal.pcm";
static const char *streamout_propty = "streamout.pcm.dump";
static const char *aud_dumpftrace_dbg_propty = "dumpftrace_dbg";
//#if defined(MTK_VIBSPK_SUPPORT)
static const char *streamout_vibsignal = "/sdcard/mtklog/audio_dump/streamoutvib.pcm";
static const char *streamout_notch = "/sdcard/mtklog/audio_dump/streamoutnotch.pcm";
//#endif

static const char *streaminmanager = "/sdcard/mtklog/audio_dump/StreamInManager_Dump.pcm";    // ADC
static const char *streamin = "/sdcard/mtklog/audio_dump/StreamIn_Dump.pcm";    // ADC
static const char *streaminOri = "/sdcard/mtklog/audio_dump/StreamInOri_Dump.pcm";    // ADC
static const char *streaminI2S = "/sdcard/mtklog/audio_dump/StreamInI2S_Dump.pcm";  // I2S
static const char *streaminDAIBT = "/sdcard/mtklog/audio_dump/StreamInDAIBT_Dump.pcm";    // DAIBT
static const char *streaminSpk = "/sdcard/mtklog/audio_dump/streamin_spk.pcm";
static const char *streaminSpk_propty = "streamin.spkpcm.dump";
static const char *capture_data_provider = "/sdcard/mtklog/audio_dump/CaptureDataProvider";

static const char *streamin_propty = "streamin.pcm.dump";
static const char *streamin_epl_propty = "streamin.epl.dump";

static const char *allow_low_latency_propty = "streamout.lowlatency.allow";
static const char *streamhfp_propty = "streamhfp.pcm.dump";
extern bool bDumpStreamOutFlg;
extern bool bDumpStreamInFlg;

int AudiocheckAndCreateDirectory(const char *pC);
FILE *AudioOpendumpPCMFile(const char *filepath, const char *propty);
void AudioCloseDumpPCMFile(FILE  *file);
void AudioDumpPCMData(void *buffer , uint32_t bytes, FILE  *file);
timespec GetSystemTime(bool print =0);
unsigned long long TimeDifference(struct timespec time1, struct timespec time2);

//-------ring buffer operation
int RingBuf_getDataCount(const RingBuf *RingBuf1);
int RingBuf_getFreeSpace(const RingBuf *RingBuf1);
void RingBuf_copyToLinear(char *buf, RingBuf *RingBuf1, int count);
void RingBuf_copyFromLinear(RingBuf *RingBuf1, const char *buf, int count);
void RingBuf_fillZero(RingBuf *RingBuf1, int count);
void RingBuf_copyEmpty(RingBuf *RingBuft, RingBuf *RingBufs);
int RingBuf_copyFromRingBuf(RingBuf *RingBuft, RingBuf *RingBufs, int count);
void RingBuf_writeDataValue(RingBuf *RingBuf1, const int value, const int count);

void RingBuf_copyFromLinearSRC(void *pSrcHdl, RingBuf *RingBuft, char *buf, int num, int srt, int srs);
void RingBuf_copyEmptySRC(void *pSrcHdl, RingBuf *RingBuft, const RingBuf *RingBufs, int srt, int srs);

void CVSDLoopbackGetWriteBuffer(uint8_t **buffer, uint32_t *buf_len);
void CVSDLoopbackGetReadBuffer(uint8_t **buffer, uint32_t *buf_len);
void CVSDLoopbackReadDataDone(uint32_t len);
void CVSDLoopbackWriteDataDone(uint32_t len);
void CVSDLoopbackResetBuffer(void);
int32_t CVSDLoopbackGetFreeSpace(void);
int32_t CVSDLoopbackGetDataCount(void);

bool IsAudioSupportFeature(int dFeatureOption);


uint32_t GetMicDeviceMode(uint32_t mic_category);

}

#endif
