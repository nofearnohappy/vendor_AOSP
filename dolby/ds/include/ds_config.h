/******************************************************************************
 *  This program is protected under international and U.S. copyright laws as
 *  an unpublished work. This program is confidential and proprietary to the
 *  copyright owners. Reproduction or disclosure, in whole or in part, or the
 *  production of derivative works therefrom without the express permission of
 *  the copyright owners is prohibited.
 *
 *               Copyright (C) 2014 by Dolby Laboratories,
 *                             All rights reserved.
 ******************************************************************************/

#ifndef DOLBY_DS_CONFIG_H_
#define DOLBY_DS_CONFIG_H_

#include <system/audio.h>
#include <hardware/audio_effect.h>

// @@DOLBY_DAP_HW
#ifdef DOLBY_DAP_HW
/*
    Devices that require software DAP on platform that has DSP implementation.
*/
#define NO_OFFLOAD_DEVICES ( AUDIO_DEVICE_OUT_BLUETOOTH_SCO             \
                           | AUDIO_DEVICE_OUT_BLUETOOTH_SCO_HEADSET     \
                           | AUDIO_DEVICE_OUT_BLUETOOTH_SCO_CARKIT      \
                           | AUDIO_DEVICE_OUT_BLUETOOTH_A2DP            \
                           | AUDIO_DEVICE_OUT_BLUETOOTH_A2DP_HEADPHONES \
                           | AUDIO_DEVICE_OUT_BLUETOOTH_A2DP_SPEAKER    \
                           | AUDIO_DEVICE_OUT_USB_ACCESSORY             \
                           | AUDIO_DEVICE_OUT_USB_DEVICE                \
                           )
#endif // DOLBY_END
// @@DOLBY_DAP_HW_END

/*
    Start offset for Dolby OMX extensions. Typically OMX Vendor extensions start at 0x??000000.
    So adding this constant to OMX vendor extension constant yields the base offset for all
    Dolby constants.
*/
#define OMX_DolbyVendorOffset 0x00D1B000

/*
    Files using following constant must include OMX_Core.h for definition of OMX_EventVendorStartUnused.
*/
#define OMX_EventDolbyProcessedAudioString "omx.event.dolby.processed_audio"
#define OMX_EventDolbyProcessedAudio (OMX_EventVendorStartUnused + OMX_DolbyVendorOffset + 1)

/*
    Files using following constant must include OMX_Index.h for definition of OMX_IndexVendorStartUnused.
*/
#define OMX_IndexDolbyReconfigOnEndpChangeString "omx.index.dolby.reconfig_on_endp_change"
#define OMX_IndexDolbyReconfigOnEndpChange (OMX_IndexVendorStartUnused + OMX_DolbyVendorOffset + 1)

/*
    Audio track parameter for indicating that Dolby decoder is producing processed audio
*/
#define DOLBY_PARAM_PROCESSED_AUDIO "dolby_processed_audio"

/*
    Signal constant used to inform the AudioFlinger to use moveDolbyEffects instead of moveEffects
*/
#define DOLBY_MOVE_EFFECT_SIGNAL -13

/*
    Type-UUID and UUID for DS Effect.
    IMPORTANT NOTES: Do not change these numbers without updating their counterparts in DsEffect.java
*/
static const effect_uuid_t EFFECT_SL_IID_DS = // 46d279d9-9be7-453d-9d7c-ef937f675587
{ 0x46d279d9, 0x9be7, 0x453d, 0x9d7c, {0xef, 0x93, 0x7f, 0x67, 0x55, 0x87} };
static const effect_uuid_t EFFECT_UUID_DS = // 9d4921da-8225-4f29-aefa-39537a04bcaa
{ 0x9d4921da, 0x8225, 0x4f29, 0xaefa, {0x39, 0x53, 0x7a, 0x04, 0xbc, 0xaa} };
static const effect_uuid_t EFFECT_UUID_DS_SW = // 6ab06da4-c516-4611-8166-452799218539
{ 0x6ab06da4, 0xc516, 0x4611, 0x8166, {0x45, 0x27, 0x99, 0x21, 0x85, 0x39} };
static const effect_uuid_t EFFECT_UUID_DS_HW = // a0c30891-8246-4aef-b8ad-d53e26da0253
{ 0xa0c30891, 0x8246, 0x4aef, 0xb8ad, {0xd5, 0x3e, 0x26, 0xda, 0x02, 0x53} };

/**
    DS effect parameter identifiers
*/
enum DolbyEffectParams {
    EFFECT_PARAM_SET_VALUES = 0,
    EFFECT_PARAM_VISUALIZER_ENABLE = 1,
    EFFECT_PARAM_VISUALIZER_DATA = 2,
    EFFECT_PARAM_VERSION = 3,
    EFFECT_PARAM_OFF_TYPE = 4,
    EFFECT_PARAM_DEFINE_PROFILE = 5,
    // Here internal effect params start.
    EFFECT_PARAM_SET_PREGAIN = 0x10,
    EFFECT_PARAM_SET_POSTGAIN = 0x11,
    EFFECT_PARAM_SET_BYPASS = 0x12
};

#endif//DOLBY_DS_CONFIG_H_

