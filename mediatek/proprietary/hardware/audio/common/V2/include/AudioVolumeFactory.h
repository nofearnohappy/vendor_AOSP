#ifndef AUDIO_VOLUME_FACTORY_H
#define AUDIO_VOLUME_FACTORY_H

#ifdef MTK_AUDIO_GAIN_TABLE
#include "AudioMTKGainController.h"
#else
#include "AudioMTKVolumeController.h"
#endif
#include "AudioMTKVolumeInterface.h"
class AudioVolumeFactory
{
    public:
        // here to implement create and
        static AudioMTKVolumeInterface *CreateAudioVolumeController();
        static void DestroyAudioVolumeController();
    private:
};

#endif
