#include "AudioVolumeFactory.h"

AudioMTKVolumeInterface *AudioVolumeFactory::CreateAudioVolumeController()
{
    // here can create diffeerent volumecontroller base on differemt platform or policy
    AudioMTKVolumeInterface *mInstance = NULL;
    if(!mInstance)
    {
    #ifdef MTK_AUDIO_GAIN_TABLE
        mInstance = android::AudioMTKGainController::getInstance();
    #else
        mInstance = android::AudioMTKVolumeController::getInstance();
    #endif
    }
    return mInstance;
}

void DestroyAudioVolumeController(AudioMTKVolumeInterface *mInstance)
{
    //here to destroy
}