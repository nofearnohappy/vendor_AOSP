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

#define LOG_TAG "DlbDlbEffect"

#include "DlbLog.h"
#include "DlbEffect.h"
#include "EffectContext.h"
#include "EffectParamParser.h"

namespace dolby {

using namespace android;

/**
    Holds effect context in format required by effect framework.

    The android effect framework requires effect handle with \p effect_interface_s*
    pointer as the first member. Since this structure is not needed anywhere
    else, it is defined in this source file.
*/
struct dlb_effect_module_s {
    const effect_interface_s *itfe;
    EffectContext ctx;
};

/**
    This is the interface for every effect instance.

    This structure is used by android framework as effect API. Since we shall
    never change, these methods, the structure is defined as constant.
*/
const effect_interface_s DlbEffect::kInterface = {
    process         : DlbEffect::effectProcess,
    command         : DlbEffect::effectCommand,
    get_descriptor  : DlbEffect::effectGetDescriptor,
    process_reverse : NULL,
};

/**
    Creates a new instance of effect and returns it in pHandle parameter.
*/
int DlbEffect::create(const effect_uuid_t *uuid, int32_t sessionId, int32_t ioId, effect_handle_t *pHandle)
{
    if (pHandle == NULL)
    {
        ALOGE("%s() Called with NULL pointer", __FUNCTION__);
        return BAD_VALUE;
    }

    if (uuid == NULL || memcmp(&kDescriptor.uuid, uuid, sizeof(effect_uuid_t)) != 0) {
        ALOGE("%s() invalid UUID specified", __FUNCTION__);
        return NAME_NOT_FOUND;
    }

    ALOGI("%s(uuid=%08X-%04X-%04X-%04X-%02X%02X%02X%02X%02X%02X, sessionId=%i, ioId=%i, pHandle=%p)",
        __FUNCTION__, uuid->timeLow, uuid->timeMid, uuid->timeHiAndVersion, uuid->clockSeq,
        uuid->node[0], uuid->node[1], uuid->node[2], uuid->node[3], uuid->node[4], uuid->node[5],
        sessionId, ioId, pHandle);

    // Create a new instance and return it.
    dlb_effect_module_s *self = new dlb_effect_module_s();
    self->itfe = &kInterface;
    *pHandle = reinterpret_cast<effect_handle_t>(self);
    ALOGI("%s() Created effect %s", __FUNCTION__, kDescriptor.name);
    return 0;
}

/**
    Releases an effect instance previously allocated by create() function.
*/
int DlbEffect::release(effect_handle_t handle)
{
    ALOGI("%s(handle=%p)", __FUNCTION__, handle);
    delete reinterpret_cast<dlb_effect_module_s *>(handle);
    return 0;
}

/**
    Returns the descriptor for the effect specified by UUID.
*/
int DlbEffect::getDescriptor(const effect_uuid_t *uuid, effect_descriptor_t *pDescriptor)
{
    if (pDescriptor == NULL || uuid == NULL)
    {
        ALOGE("%s() called with NULL pointers", __FUNCTION__);
        return BAD_VALUE;
    }

    ALOGI("%s(uuid=%08X-%04X-%04X-%04X-%02X%02X%02X%02X%02X%02X, pDescriptor=%p)",
        __FUNCTION__, uuid->timeLow, uuid->timeMid, uuid->timeHiAndVersion, uuid->clockSeq,
        uuid->node[0], uuid->node[1], uuid->node[2], uuid->node[3], uuid->node[4], uuid->node[5],
        pDescriptor);

   if (memcmp(&kDescriptor.uuid, uuid, sizeof(effect_uuid_t)) != 0) {
        ALOGE("%s() UUID not found", __FUNCTION__);
        return NAME_NOT_FOUND;
    }

    *pDescriptor = kDescriptor;
    ALOGI("%s() Returning descriptor for %s", __FUNCTION__, kDescriptor.name);
    return 0;
}

/**
    Called by Android framework to perform audio processing with the given effect instance.
*/
int DlbEffect::effectProcess(effect_handle_t handle, audio_buffer_t *inBuffer, audio_buffer_t *outBuffer)
{
    ALOGVV("%s(handle=%p, inBuffer=%p, outBuffer=%p)", __FUNCTION__, handle, inBuffer, outBuffer);

    if (handle == NULL || inBuffer == NULL || outBuffer == NULL)
    {
        ALOGE("%s() called with NULL parameters", __FUNCTION__);
        return BAD_VALUE;
    }
    dlb_effect_module_s *self = reinterpret_cast<dlb_effect_module_s*>(handle);
    return self->ctx.process(inBuffer, outBuffer);
}

/**
    Called by Android framework to get descriptor of the given effect instance.
*/
int DlbEffect::effectGetDescriptor(effect_handle_t handle, effect_descriptor_t *pDescriptor)
{
    ALOGD("%s(handle=%p)", __FUNCTION__, handle);
    if (handle == NULL || pDescriptor == NULL)
    {
        ALOGE("%s() called with invalid parameters", __FUNCTION__);
        return BAD_VALUE;
    }
    *pDescriptor = kDescriptor;
    return 0;
}

/**
    Called by Android framework to send commands to the given effect instance.

    This function dispatches received command to appropriate \p handle_EFFECT_CMD function.
*/
int DlbEffect::effectCommand(effect_handle_t handle, uint32_t cmdCode, uint32_t cmdSize, void *pCmdData, uint32_t *pReplySize, void *pReplyData)
{
    uint32_t replySize = (pReplySize == NULL) ? 0 : (*pReplySize);

    ALOGVV("%s(handle=%p, cmdCode=%u, cmdSize=%u, pCmdData=%p, replySize=%u, pReplyData=%p)",
        __FUNCTION__, handle, cmdCode, cmdSize, pCmdData, replySize, pReplyData);

    dlb_effect_module_s *self = reinterpret_cast<dlb_effect_module_s*>(handle);
    if (self == NULL)
    {
        ALOGE("%s() called with NULL handle", __FUNCTION__);
        return BAD_VALUE;
    }

    // Create parser buffer objects to extract command data
    ParserBuffer pbCmd(pCmdData, static_cast<int>(cmdSize));
    ParserBuffer pbReply(pReplyData, static_cast<int>(replySize));
    status_t status;

    switch (cmdCode)
    {
    case EFFECT_CMD_INIT:
        status = handle_EFFECT_CMD_INIT(self->ctx, pbCmd, pbReply);
        break;
    case EFFECT_CMD_SET_CONFIG:
        status = handle_EFFECT_CMD_SET_CONFIG(self->ctx, pbCmd, pbReply);
        break;
    case EFFECT_CMD_RESET:
        status = handle_EFFECT_CMD_RESET(self->ctx, pbCmd, pbReply);
        break;
    case EFFECT_CMD_ENABLE:
        status = handle_EFFECT_CMD_ENABLE(self->ctx, pbCmd, pbReply);
        break;
    case EFFECT_CMD_DISABLE:
        status = handle_EFFECT_CMD_DISABLE(self->ctx, pbCmd, pbReply);
        break;
    case EFFECT_CMD_SET_PARAM:
        status = handle_EFFECT_CMD_SET_PARAM(self->ctx, pbCmd, pbReply);
        break;
    case EFFECT_CMD_GET_PARAM:
        status = handle_EFFECT_CMD_GET_PARAM(self->ctx, pbCmd, pbReply);
        break;
    case EFFECT_CMD_SET_DEVICE:
        status = handle_EFFECT_CMD_SET_DEVICE(self->ctx, pbCmd, pbReply);
        break;
    case EFFECT_CMD_SET_VOLUME:
        status = handle_EFFECT_CMD_SET_VOLUME(self->ctx, pbCmd, pbReply);
        break;
    case EFFECT_CMD_SET_AUDIO_MODE:
        status = handle_EFFECT_CMD_SET_AUDIO_MODE(self->ctx, pbCmd, pbReply);
        break;
    case EFFECT_CMD_GET_CONFIG:
        status = handle_EFFECT_CMD_GET_CONFIG(self->ctx, pbCmd, pbReply);
        break;
    case EFFECT_CMD_OFFLOAD:
        status = handle_EFFECT_CMD_OFFLOAD(self->ctx, pbCmd, pbReply);
        break;
    default:
        ALOGV("%s() Unknown command code: %d", __FUNCTION__, cmdCode);
        status = BAD_VALUE;
    }
    // Adjust reply size by removing number of bytes not used up by reply parser
    if (status == NO_ERROR && pReplySize != NULL)
    {
        *pReplySize -= pbReply.remaining();
    }
    return status;
}

status_t DlbEffect::handle_EFFECT_CMD_INIT(EffectContext &ctx, ParserBuffer &cmd, ParserBuffer &reply)
{
    ALOGD("%s()", __FUNCTION__);
    int *status;
    if (!reply.consume(&status))
    {
        ALOGE("%s() Invalid command data", __FUNCTION__);
        return BAD_VALUE;
    }
    *status = ctx.init();
    return NO_ERROR;
}

status_t DlbEffect::handle_EFFECT_CMD_SET_CONFIG(EffectContext &ctx, ParserBuffer &cmd, ParserBuffer &reply)
{
    ALOGD("%s()", __FUNCTION__);
    effect_config_t *pConfig;
    int *status;
    if (!(cmd.consume(&pConfig) && reply.consume(&status)))
    {
        ALOGE("%s() Invalid command data", __FUNCTION__);
        return BAD_VALUE;
    }
    *status = ctx.setConfig(pConfig);
    return NO_ERROR;
}

status_t DlbEffect::handle_EFFECT_CMD_RESET(EffectContext &ctx, ParserBuffer &cmd, ParserBuffer &reply)
{
    ALOGD("%s()", __FUNCTION__);
    ctx.reset();
    return NO_ERROR;
}

status_t DlbEffect::handle_EFFECT_CMD_ENABLE(EffectContext &ctx, ParserBuffer &cmd, ParserBuffer &reply)
{
    ALOGD("%s()", __FUNCTION__);
    int *status;
    if (!reply.consume(&status))
    {
        ALOGE("%s() Invalid command data", __FUNCTION__);
        return BAD_VALUE;
    }
    *status = ctx.enable();
    return NO_ERROR;
}

status_t DlbEffect::handle_EFFECT_CMD_DISABLE(EffectContext &ctx, ParserBuffer &cmd, ParserBuffer &reply)
{
    ALOGD("%s()", __FUNCTION__);
    int *status;
    if (!reply.consume(&status))
    {
        ALOGE("%s() Invalid command data", __FUNCTION__);
        return BAD_VALUE;
    }
    *status = ctx.disable();
    return NO_ERROR;
}

status_t DlbEffect::handle_EFFECT_CMD_SET_PARAM(EffectContext &ctx, ParserBuffer &cmd, ParserBuffer &reply)
{
    ALOGD("%s()", __FUNCTION__);
    effect_param_t *param;
    int *status;
    // The last member of effect_param_t structure (data) extends the size of
    // structure to include parameter name & parameter value. So ensure that
    // sufficient data is provided after reading the structure.
    if (!(reply.consume(&status) && cmd.consume(&param)
        && (param->psize == sizeof(int32_t)) && cmd.skip(param->psize + param->vsize)))
    {
        ALOGE("%s() Invalid command data", __FUNCTION__);
        return BAD_VALUE;
    }
    // We only expect integer parameter ids
    int32_t *data = reinterpret_cast<int32_t*>(param->data);
    int paramId = *data;
    void *values = data + 1;
    *status = ctx.setParam(paramId, param->vsize, values);
    return NO_ERROR;
}

status_t DlbEffect::handle_EFFECT_CMD_GET_PARAM(EffectContext &ctx, ParserBuffer &cmd, ParserBuffer &reply)
{
    ALOGVV("%s()", __FUNCTION__);
    effect_param_t *req_param;
    effect_param_t *ret_param;
    // Make sure that request parameter is correctly specified and the reply parameter
    // has at least the space to include parameter id.
    if (!(cmd.consume(&req_param) && (req_param->psize == sizeof(int32_t))
        && cmd.skip(req_param->psize) && reply.consume(&ret_param)
        && reply.skip(req_param->psize)))
    {
        ALOGE("%s() Invalid command data", __FUNCTION__);
        return BAD_VALUE;
    }
    // Initialize reply structure by copying parameter id from command structure.
    ret_param->psize = req_param->psize;
    int32_t *ret_data = reinterpret_cast<int32_t*>(ret_param->data);
    *ret_data = *reinterpret_cast<int*>(req_param->data);
    // Values start after the parameter id
    void *values = ret_data + 1;
    ret_param->vsize = reply.remaining();
    // Call EffectContext::getParam() and return status.
    ret_param->status = ctx.getParam(*ret_data, &ret_param->vsize, values);
    // Mark bytes used by value as used.
    reply.skip(ret_param->vsize);
    return NO_ERROR;
}

status_t DlbEffect::handle_EFFECT_CMD_SET_DEVICE(EffectContext &ctx, ParserBuffer &cmd, ParserBuffer &reply)
{
    uint32_t device;
    if (!cmd.extract(&device))
    {
        ALOGE("%s() Invalid command data", __FUNCTION__);
        return BAD_VALUE;
    }
    ALOGV("%s(device=0x%08x)", __FUNCTION__, device);
    ctx.setDevice(device);
    return NO_ERROR;
}

status_t DlbEffect::handle_EFFECT_CMD_SET_VOLUME(EffectContext &ctx, ParserBuffer &cmd, ParserBuffer &reply)
{
    uint32_t *volumes;
    int num_chans = cmd.size() / sizeof(*volumes);
    if (num_chans == 0 || !cmd.consume(&volumes, num_chans))
    {
        ALOGE("%s() Invalid command data", __FUNCTION__);
        return BAD_VALUE;
    }
    ALOGV_IF(num_chans > 1, "%s(vl=%d, vr=%d)", __FUNCTION__, volumes[0], volumes[1]);
    ctx.setVolume(num_chans, volumes);
    return NO_ERROR;
}

status_t DlbEffect::handle_EFFECT_CMD_SET_AUDIO_MODE(EffectContext &ctx, ParserBuffer &cmd, ParserBuffer &reply)
{
    audio_mode_t mode;
    if (!cmd.extract(&mode))
    {
        ALOGE("%s() Invalid command data", __FUNCTION__);
        return BAD_VALUE;
    }
    ALOGV("%s(mode=0x%08x)", __FUNCTION__, mode);
    ctx.setAudioMode(mode);
    return NO_ERROR;
}

status_t DlbEffect::handle_EFFECT_CMD_GET_CONFIG(EffectContext &ctx, ParserBuffer &cmd, ParserBuffer &reply)
{
    ALOGV("%s()", __FUNCTION__);
    effect_config_t *pConfig;
    if (!reply.consume(&pConfig))
    {
        ALOGE("%s() Invalid command data", __FUNCTION__);
        return BAD_VALUE;
    }
    return ctx.getConfig(pConfig);
}

status_t DlbEffect::handle_EFFECT_CMD_OFFLOAD(EffectContext &ctx, ParserBuffer &cmd, ParserBuffer &reply)
{
    effect_offload_param_t *params;
    uint32_t *status;
    if (!(cmd.consume(&params) && reply.consume(&status)))
    {
        ALOGE("%s() Invalid command data", __FUNCTION__);
        return BAD_VALUE;
    }
    ALOGD("%s(offload=%d, ioHandle=%d)", __FUNCTION__, params->isOffload, params->ioHandle);
    *status = ctx.offload(params->isOffload, params->ioHandle);
    return NO_ERROR;
}

} // namespace dolby
