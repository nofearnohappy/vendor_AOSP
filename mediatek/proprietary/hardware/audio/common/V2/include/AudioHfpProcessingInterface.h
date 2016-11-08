#ifndef _AUDIO_HFP_PROCESSING_INTERFACE_H
#define _AUDIO_HFP_PROCESSING_INTERFACE_H

#include "AudioStreamAttribute.h"
#include "AudioDigitalType.h"
#include "AudioType.h"

//!  Audio HFP Processing interface
/*!
  this class is define Audio Hfp Processing interface
*/
namespace android
{

class AudioHfpProcessingInterface
{
    public:

        /**
        * a destuctor for AudioHfpProcessingInterface
        */

        virtual ~AudioHfpProcessingInterface() {};

        /**
        * initital check for resource allocate
        * @return status_t
        */
        virtual status_t InitCheck() = 0;

};

}
#endif
