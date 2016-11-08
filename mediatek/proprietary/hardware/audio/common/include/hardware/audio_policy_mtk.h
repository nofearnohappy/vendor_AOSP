/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


#ifndef AUDIO_POLICY_INTERFACE_MTK_H
#define AUDIO_POLICY_INTERFACE_MTK_H

#include <hardware/audio_policy.h>

struct audio_policy_mtk: audio_policy{
//    struct audio_policy legacy_audio_policy;
    int (*set_policy_parameters)(struct audio_policy *pol, int par1, int par2, int par3, int par4);

    /* indicates to the audio policy manager that the output starts being used
        * by corresponding stream and samplerate. */
    int (*start_output_samplerate)(struct audio_policy *pol,
                        audio_io_handle_t output,
                        audio_stream_type_t stream,
                        int session, int samplerate);
    /* indicates to the audio policy manager that the output starts being used
        * by corresponding stream and samplerate. */
    int (*stop_output_samplerate)(struct audio_policy *pol,
                        audio_io_handle_t output,
                        audio_stream_type_t stream,
                        int session, int samplerate);
};

#endif  // AUDIO_POLICY_INTERFACE_MTK_H