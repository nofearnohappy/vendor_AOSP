/******************************************************************************
 *  This program is protected under international and U.S. copyright laws as
 *  an unpublished work. This program is confidential and proprietary to the
 *  copyright owners. Reproduction or disclosure, in whole or in part, or the
 *  production of derivative works therefrom without the express permission of
 *  the copyright owners is prohibited.
 *
 *                 Copyright (C) 2014 by Dolby Laboratories,
 *                             All rights reserved.
 ******************************************************************************/
#ifndef DOLBY_EFFECT_H_
#define DOLBY_EFFECT_H_

#include <utils/Errors.h>
#include <hardware/audio_effect.h>

/**
    This macro define the effect module interface symbol as expected by Android
    framework. This macro must be invoked in only one file in the implementation
    library. Typically this is done in the file containing IEffectDap
    implementation. Parameter \p effect_library_name is just a human readable
    name of the effect library and not the file name.
*/
#define DEFINE_DOLBY_EFFECT_LIBRARY_INFO(effect_library_name) \
    __attribute__ ((visibility ("default")))                  \
    audio_effect_library_t AUDIO_EFFECT_LIBRARY_INFO_SYM = {  \
        tag           : AUDIO_EFFECT_LIBRARY_TAG,             \
        version       : EFFECT_LIBRARY_API_VERSION,           \
        name          : effect_library_name,                  \
        implementor   : "Dolby Laboratories",                 \
        create_effect : dolby::DlbEffect::create,             \
        release_effect: dolby::DlbEffect::release,            \
        get_descriptor: dolby::DlbEffect::getDescriptor,      \
    }

/*
    If both software and hardware effects are defined, then they will get
    separate UUID. Otherwise only one of them will be defined and will get
    the DS effect UUID.
*/
#if defined(DOLBY_DAP_SW) && defined(DOLBY_DAP_HW)
#define SW_EFFECT_UUID EFFECT_UUID_DS_SW
#define HW_EFFECT_UUID EFFECT_UUID_DS_HW
#else
#define SW_EFFECT_UUID EFFECT_UUID_DS
#define HW_EFFECT_UUID EFFECT_UUID_DS
#endif

/**
    This macro defines the descriptors needed by Android framework.
    This macro must be invoked in only one file in the effect implementation
    library. Typically this is done in the file containing IEffectDap
    implementation. Parameter \p effect_library_name is just a human readable
    name of the effect library and not the file name.

    Note that \p cpuLoad and \p memoryUsage are set to 0 since the Android
    framework does not use them in any meaningful manner.
*/
#define DEFINE_DOLBY_EFFECT_DESCRIPTOR(effect_name, is_hw)            \
    const effect_descriptor_t dolby::DlbEffect::kDescriptor = {       \
        /* type        */ EFFECT_SL_IID_DS,                           \
        /* uuid        */ ((is_hw) ? HW_EFFECT_UUID                   \
                                   : SW_EFFECT_UUID),                 \
        /* apiVersion  */ EFFECT_CONTROL_API_VERSION,                 \
        /* flags       */ (                                           \
                            EFFECT_FLAG_TYPE_INSERT    |              \
                            EFFECT_FLAG_INSERT_FIRST   |              \
                            EFFECT_FLAG_VOLUME_IND     |              \
                            EFFECT_FLAG_DEVICE_IND     |              \
                            EFFECT_FLAG_AUDIO_MODE_IND |              \
                            ((is_hw) ? EFFECT_FLAG_HW_ACC_TUNNEL : 0) \
                          ),                                          \
        /* cpuLoad     */ 0,                                          \
        /* memoryUsage */ 0,                                          \
        /* name        */ effect_name,                                \
        /* implementor */ "Dolby Laboratories, Inc."                  \
    }

namespace dolby {
using namespace android;
// Forward declaration of classes referenced in this file
class EffectContext;
class ParserBuffer;

/**
    This class provides the effect interface expected by Android framework.

    Main purpose of this class is to group the effect interface functions. This
    class must not have any non-static members since Android effect interface is
    exposed as C functions.

    This class also handles packing/unpacking of data provided by effect interface.
    So the \p EffctContext interface is simplified. The \p handle_EFFECT_CMD_*()
    set of functions are mapped to each effect command. These are responsible
    for unpacking the data provided to \b effectCommand() function.
*/
class DlbEffect
{
public:
    static const effect_interface_s kInterface;
    static const effect_descriptor_t kDescriptor;

    static int create(const effect_uuid_t *uuid, int32_t sessionId, int32_t ioId, effect_handle_t *pHandle);
    static int release(effect_handle_t handle);
    static int getDescriptor(const effect_uuid_t *uuid, effect_descriptor_t *pDescriptor);

    static int effectProcess(effect_handle_t handle, audio_buffer_t *inBuffer, audio_buffer_t *outBuffer);
    static int effectCommand(effect_handle_t handle, uint32_t cmdCode, uint32_t cmdSize, void *pCmdData,
        uint32_t *pReplySize, void *pReplyData);
    static int effectGetDescriptor(effect_handle_t handle, effect_descriptor_t *pDescriptor);

private:
    static status_t handle_EFFECT_CMD_INIT(EffectContext &ctx, ParserBuffer &cmd, ParserBuffer &reply);
    static status_t handle_EFFECT_CMD_SET_CONFIG(EffectContext &ctx, ParserBuffer &cmd, ParserBuffer &reply);
    static status_t handle_EFFECT_CMD_RESET(EffectContext &ctx, ParserBuffer &cmd, ParserBuffer &reply);
    static status_t handle_EFFECT_CMD_ENABLE(EffectContext &ctx, ParserBuffer &cmd, ParserBuffer &reply);
    static status_t handle_EFFECT_CMD_DISABLE(EffectContext &ctx, ParserBuffer &cmd, ParserBuffer &reply);
    static status_t handle_EFFECT_CMD_SET_PARAM(EffectContext &ctx, ParserBuffer &cmd, ParserBuffer &reply);
    static status_t handle_EFFECT_CMD_GET_PARAM(EffectContext &ctx, ParserBuffer &cmd, ParserBuffer &reply);
    static status_t handle_EFFECT_CMD_SET_DEVICE(EffectContext &ctx, ParserBuffer &cmd, ParserBuffer &reply);
    static status_t handle_EFFECT_CMD_SET_VOLUME(EffectContext &ctx, ParserBuffer &cmd, ParserBuffer &reply);
    static status_t handle_EFFECT_CMD_SET_AUDIO_MODE(EffectContext &ctx, ParserBuffer &cmd, ParserBuffer &reply);
    static status_t handle_EFFECT_CMD_GET_CONFIG(EffectContext &ctx, ParserBuffer &cmd, ParserBuffer &reply);
    static status_t handle_EFFECT_CMD_OFFLOAD(EffectContext &ctx, ParserBuffer &cmd, ParserBuffer &reply);
};

} // namespace android

#endif//DOLBY_EFFECT_H_
