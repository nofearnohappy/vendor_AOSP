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


#ifndef ANDROID_AUDIO_CORE_H
#define ANDROID_AUDIO_CORE_H

enum {
    /* output channels */
    AUDIO_CHANNEL_OUT_FRONT_LEFT            = 0x1,
    AUDIO_CHANNEL_OUT_FRONT_RIGHT           = 0x2,
    AUDIO_CHANNEL_OUT_FRONT_CENTER          = 0x4,
    AUDIO_CHANNEL_OUT_LOW_FREQUENCY         = 0x8,
    AUDIO_CHANNEL_OUT_BACK_LEFT             = 0x10,
    AUDIO_CHANNEL_OUT_BACK_RIGHT            = 0x20,
    AUDIO_CHANNEL_OUT_FRONT_LEFT_OF_CENTER  = 0x40,
    AUDIO_CHANNEL_OUT_FRONT_RIGHT_OF_CENTER = 0x80,
    AUDIO_CHANNEL_OUT_BACK_CENTER           = 0x100,
    AUDIO_CHANNEL_OUT_SIDE_LEFT             = 0x200,
    AUDIO_CHANNEL_OUT_SIDE_RIGHT            = 0x400,
    AUDIO_CHANNEL_OUT_TOP_CENTER            = 0x800,
    AUDIO_CHANNEL_OUT_TOP_FRONT_LEFT        = 0x1000,
    AUDIO_CHANNEL_OUT_TOP_FRONT_CENTER      = 0x2000,
    AUDIO_CHANNEL_OUT_TOP_FRONT_RIGHT       = 0x4000,
    AUDIO_CHANNEL_OUT_TOP_BACK_LEFT         = 0x8000,
    AUDIO_CHANNEL_OUT_TOP_BACK_CENTER       = 0x10000,
    AUDIO_CHANNEL_OUT_TOP_BACK_RIGHT        = 0x20000,

    AUDIO_CHANNEL_OUT_MONO     = AUDIO_CHANNEL_OUT_FRONT_LEFT,
    AUDIO_CHANNEL_OUT_STEREO   = (AUDIO_CHANNEL_OUT_FRONT_LEFT |
                                  AUDIO_CHANNEL_OUT_FRONT_RIGHT),
    AUDIO_CHANNEL_OUT_QUAD     = (AUDIO_CHANNEL_OUT_FRONT_LEFT |
                                  AUDIO_CHANNEL_OUT_FRONT_RIGHT |
                                  AUDIO_CHANNEL_OUT_BACK_LEFT |
                                  AUDIO_CHANNEL_OUT_BACK_RIGHT),
    AUDIO_CHANNEL_OUT_SURROUND = (AUDIO_CHANNEL_OUT_FRONT_LEFT |
                                  AUDIO_CHANNEL_OUT_FRONT_RIGHT |
                                  AUDIO_CHANNEL_OUT_FRONT_CENTER |
                                  AUDIO_CHANNEL_OUT_BACK_CENTER),
    AUDIO_CHANNEL_OUT_5POINT1  = (AUDIO_CHANNEL_OUT_FRONT_LEFT |
                                  AUDIO_CHANNEL_OUT_FRONT_RIGHT |
                                  AUDIO_CHANNEL_OUT_FRONT_CENTER |
                                  AUDIO_CHANNEL_OUT_LOW_FREQUENCY |
                                  AUDIO_CHANNEL_OUT_BACK_LEFT |
                                  AUDIO_CHANNEL_OUT_BACK_RIGHT),
    // matches the correct AudioFormat.CHANNEL_OUT_7POINT1_SURROUND definition for 7.1
    AUDIO_CHANNEL_OUT_7POINT1  = (AUDIO_CHANNEL_OUT_FRONT_LEFT |
                                  AUDIO_CHANNEL_OUT_FRONT_RIGHT |
                                  AUDIO_CHANNEL_OUT_FRONT_CENTER |
                                  AUDIO_CHANNEL_OUT_LOW_FREQUENCY |
                                  AUDIO_CHANNEL_OUT_BACK_LEFT |
                                  AUDIO_CHANNEL_OUT_BACK_RIGHT |
                                  AUDIO_CHANNEL_OUT_SIDE_LEFT |
                                  AUDIO_CHANNEL_OUT_SIDE_RIGHT),
    AUDIO_CHANNEL_OUT_ALL      = (AUDIO_CHANNEL_OUT_FRONT_LEFT |
                                  AUDIO_CHANNEL_OUT_FRONT_RIGHT |
                                  AUDIO_CHANNEL_OUT_FRONT_CENTER |
                                  AUDIO_CHANNEL_OUT_LOW_FREQUENCY |
                                  AUDIO_CHANNEL_OUT_BACK_LEFT |
                                  AUDIO_CHANNEL_OUT_BACK_RIGHT |
                                  AUDIO_CHANNEL_OUT_FRONT_LEFT_OF_CENTER |
                                  AUDIO_CHANNEL_OUT_FRONT_RIGHT_OF_CENTER |
                                  AUDIO_CHANNEL_OUT_BACK_CENTER|
                                  AUDIO_CHANNEL_OUT_SIDE_LEFT|
                                  AUDIO_CHANNEL_OUT_SIDE_RIGHT|
                                  AUDIO_CHANNEL_OUT_TOP_CENTER|
                                  AUDIO_CHANNEL_OUT_TOP_FRONT_LEFT|
                                  AUDIO_CHANNEL_OUT_TOP_FRONT_CENTER|
                                  AUDIO_CHANNEL_OUT_TOP_FRONT_RIGHT|
                                  AUDIO_CHANNEL_OUT_TOP_BACK_LEFT|
                                  AUDIO_CHANNEL_OUT_TOP_BACK_CENTER|
                                  AUDIO_CHANNEL_OUT_TOP_BACK_RIGHT),

    /* input channels */
    AUDIO_CHANNEL_IN_LEFT            = 0x4,
    AUDIO_CHANNEL_IN_RIGHT           = 0x8,
    AUDIO_CHANNEL_IN_FRONT           = 0x10,
    AUDIO_CHANNEL_IN_BACK            = 0x20,
    AUDIO_CHANNEL_IN_LEFT_PROCESSED  = 0x40,
    AUDIO_CHANNEL_IN_RIGHT_PROCESSED = 0x80,
    AUDIO_CHANNEL_IN_FRONT_PROCESSED = 0x100,
    AUDIO_CHANNEL_IN_BACK_PROCESSED  = 0x200,
    AUDIO_CHANNEL_IN_PRESSURE        = 0x400,
    AUDIO_CHANNEL_IN_X_AXIS          = 0x800,
    AUDIO_CHANNEL_IN_Y_AXIS          = 0x1000,
    AUDIO_CHANNEL_IN_Z_AXIS          = 0x2000,
    AUDIO_CHANNEL_IN_VOICE_UPLINK    = 0x4000,
    AUDIO_CHANNEL_IN_VOICE_DNLINK    = 0x8000,

    AUDIO_CHANNEL_IN_MONO   = AUDIO_CHANNEL_IN_FRONT,
    AUDIO_CHANNEL_IN_STEREO = (AUDIO_CHANNEL_IN_LEFT | AUDIO_CHANNEL_IN_RIGHT),
    AUDIO_CHANNEL_IN_ALL    = (AUDIO_CHANNEL_IN_LEFT |
                               AUDIO_CHANNEL_IN_RIGHT |
                               AUDIO_CHANNEL_IN_FRONT |
                               AUDIO_CHANNEL_IN_BACK|
                               AUDIO_CHANNEL_IN_LEFT_PROCESSED |
                               AUDIO_CHANNEL_IN_RIGHT_PROCESSED |
                               AUDIO_CHANNEL_IN_FRONT_PROCESSED |
                               AUDIO_CHANNEL_IN_BACK_PROCESSED|
                               AUDIO_CHANNEL_IN_PRESSURE |
                               AUDIO_CHANNEL_IN_X_AXIS |
                               AUDIO_CHANNEL_IN_Y_AXIS |
                               AUDIO_CHANNEL_IN_Z_AXIS |
                               AUDIO_CHANNEL_IN_VOICE_UPLINK |
                               AUDIO_CHANNEL_IN_VOICE_DNLINK),
};

#endif  // ANDROID_AUDIO_CORE_H

