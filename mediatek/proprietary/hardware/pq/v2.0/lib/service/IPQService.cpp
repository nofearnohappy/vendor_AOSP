#define LOG_TAG "IPQService"
#define MTK_LOG_ENABLE 1
#include <cutils/log.h>
#include "IPQService.h"

namespace android {

// client : proxy PQ class
class BpPQService : public BpInterface<IPQService>
{
public:
    BpPQService(const sp<IBinder>& impl) : BpInterface<IPQService>(impl)
    {
    }
    virtual status_t getTDSHPFlag(int32_t *TDSHPFlag)
    {
        Parcel data, reply;

        if (remote()->transact(PQ_GET_TDSHP_FLAG, data, &reply) != NO_ERROR) {
            ALOGE("getParamters could not contact remote\n");
            return -1;
        }

        status_t ret = static_cast<status_t>(reply.readInt32());
        *TDSHPFlag = reply.readInt32();

        return ret;
    }
    virtual status_t setTDSHPFlag(int32_t TDSHPFlag)
    {
        Parcel data, reply;
        data.writeInt32(TDSHPFlag);
        if (remote()->transact(PQ_SET_TDSHP_FLAG, data, &reply) != NO_ERROR) {
            ALOGE("getParamters could not contact remote\n");
            return -1;
        }

        status_t ret = static_cast<status_t>(reply.readInt32());

        return ret;
    }
    virtual status_t getColorRegion(DISP_PQ_WIN_PARAM *win_param)
    {
        Parcel data, reply;

        if (remote()->transact(PQ_GET_COLOR_REGION, data, &reply) != NO_ERROR) {
            ALOGE("getParamters could not contact remote\n");
            return -1;
        }

        status_t ret = static_cast<status_t>(reply.readInt32());
        win_param->split_en= reply.readInt32();
        win_param->start_x= reply.readInt32();
        win_param->start_y= reply.readInt32();
        win_param->end_x= reply.readInt32();
        win_param->end_y= reply.readInt32();
        return ret;
    }

    virtual status_t setColorRegion(int32_t split_en,int32_t start_x,int32_t start_y,int32_t end_x,int32_t end_y)
    {
        Parcel data, reply;
        data.writeInt32(split_en);
        data.writeInt32(start_x);
        data.writeInt32(start_y);
        data.writeInt32(end_x);
        data.writeInt32(end_y);


        if (remote()->transact(PQ_SET_COLOR_REGION, data, &reply) != NO_ERROR) {
            ALOGE("getParamters could not contact remote\n");
            return -1;
        }

        status_t ret = static_cast<status_t>(reply.readInt32());

        return ret;
    }

    virtual status_t setPQMode(int32_t mode)
    {
        Parcel data, reply;
        data.writeInt32(mode);
        if (remote()->transact(PQ_SET_MODE, data, &reply) != NO_ERROR) {
            ALOGE("getParamters could not contact remote\n");
            return -1;
        }

        status_t ret = static_cast<status_t>(reply.readInt32());

        return ret;
    }

    virtual status_t setPQIndex(int32_t level, int32_t scenario, int32_t tuning_mode,int32_t index)
    {
        Parcel data, reply;
        data.writeInt32(level);
        data.writeInt32(scenario);
        data.writeInt32(tuning_mode);
        data.writeInt32(index);
        if (remote()->transact(PQ_SET_INDEX, data, &reply) != NO_ERROR) {
            ALOGE("getParamters could not contact remote\n");
            return -1;
        }

        status_t ret = static_cast<status_t>(reply.readInt32());

        return ret;
    }

    virtual status_t getMappedColorIndex(DISP_PQ_PARAM *index, int32_t scenario, int32_t mode)
    {
        Parcel data, reply;
        data.writeInt32(scenario);
        data.writeInt32(mode);
        if (remote()->transact(PQ_GET_MAPPED_COLOR_INDEX, data, &reply) != NO_ERROR) {
            ALOGE("getParamters could not contact remote\n");
            return -1;
        }

        status_t ret = static_cast<status_t>(reply.readInt32());
        index->u4SatGain = reply.readInt32();
        index->u4PartialY = reply.readInt32();
        index->u4HueAdj[0] = reply.readInt32();
        index->u4HueAdj[1] = reply.readInt32();
        index->u4HueAdj[2] = reply.readInt32();
        index->u4HueAdj[3] = reply.readInt32();
        index->u4SatAdj[0] = reply.readInt32();
        index->u4SatAdj[1] = reply.readInt32();
        index->u4SatAdj[2] = reply.readInt32();
        index->u4SatAdj[3] = reply.readInt32();
        index->u4Contrast = reply.readInt32();
        index->u4Brightness = reply.readInt32();
        return ret;
    }

    virtual status_t getMappedTDSHPIndex(DISP_PQ_PARAM *index, int32_t scenario, int32_t mode)
    {
        Parcel data, reply;
        data.writeInt32(scenario);
        data.writeInt32(mode);
        if (remote()->transact(PQ_GET_MAPPED_TDSHP_INDEX, data, &reply) != NO_ERROR) {
            ALOGE("getParamters could not contact remote\n");
            return -1;
        }

        status_t ret = static_cast<status_t>(reply.readInt32());
        index->u4SHPGain = reply.readInt32();

        return ret;
    }

    virtual status_t setPQDCIndex(int32_t level, int32_t index)
    {
        Parcel data, reply;
        data.writeInt32(level);
        data.writeInt32(index);
        if (remote()->transact(PQ_SET_PQDC_INDEX, data, &reply) != NO_ERROR) {
            ALOGE("getParamters could not contact remote\n");
            return -1;
        }

        status_t ret = static_cast<status_t>(reply.readInt32());

        return ret;
    }

    virtual status_t getPQDCIndex(DISP_PQ_DC_PARAM *dcparam, int32_t index)
    {
        Parcel data, reply;
        int32_t i;
        data.writeInt32(index);
        if (remote()->transact(PQ_GET_PQDC_INDEX, data, &reply) != NO_ERROR) {
            ALOGE("getParamters could not contact remote\n");
            return -1;
        }

        status_t ret = static_cast<status_t>(reply.readInt32());

        for(i = 0; i < 40; i++){
            dcparam->param[i] = reply.readInt32();
        }
        return ret;
    }

    virtual status_t getColorCapInfo(MDP_COLOR_CAP *param)
    {
        Parcel data, reply;
        int32_t i;
        if (remote()->transact(PQ_GET_COLOR_CAP, data, &reply) != NO_ERROR) {
            ALOGE("getParamters could not contact remote\n");
            return -1;
        }

        status_t ret = static_cast<status_t>(reply.readInt32());
        param->en    = reply.readInt32();
        param->pos_x = reply.readInt32();
        param->pos_y = reply.readInt32();
        return ret;
    }

    virtual status_t getTDSHPReg(MDP_TDSHP_REG *param)
    {
        Parcel data, reply;
        int32_t i;
        if (remote()->transact(PQ_GET_TDSHP_REG, data, &reply) != NO_ERROR) {
            ALOGE("getParamters could not contact remote\n");
            return -1;
        }

        status_t ret = static_cast<status_t>(reply.readInt32());
        param->TDS_GAIN_MID  = reply.readInt32();
        param->TDS_GAIN_HIGH = reply.readInt32();
        param->TDS_COR_GAIN  = reply.readInt32();
        param->TDS_COR_THR   = reply.readInt32();
        param->TDS_COR_ZERO  = reply.readInt32();
        param->TDS_GAIN      = reply.readInt32();
        param->TDS_COR_VALUE = reply.readInt32();
        return ret;
    }

    virtual status_t getPQDSIndex(DISP_PQ_DS_PARAM *dsparam)
    {
        Parcel data, reply;
        int32_t i;
        if (remote()->transact(PQ_GET_PQDS_INDEX, data, &reply) != NO_ERROR) {
            ALOGE("getParamters could not contact remote\n");
            return -1;
        }

        status_t ret = static_cast<status_t>(reply.readInt32());

        for(i = 0; i < PQ_DS_INDEX_MAX; i++){
            dsparam->param[i] = reply.readInt32();
        }
        return ret;
    }

    virtual status_t getColorIndex(DISP_PQ_PARAM *index, int32_t scenario, int32_t mode)
    {
        Parcel data, reply;
        data.writeInt32(scenario);
        data.writeInt32(mode);
        if (remote()->transact(PQ_GET_COLOR_INDEX, data, &reply) != NO_ERROR) {
            ALOGE("getParamters could not contact remote\n");
            return -1;
        }

        status_t ret = static_cast<status_t>(reply.readInt32());
        index->u4SatGain = reply.readInt32();
        index->u4PartialY= reply.readInt32();
        index->u4HueAdj[0] = reply.readInt32();
        index->u4HueAdj[1] = reply.readInt32();
        index->u4HueAdj[2] = reply.readInt32();
        index->u4HueAdj[3] = reply.readInt32();
        index->u4SatAdj[0] = reply.readInt32();
        index->u4SatAdj[1] = reply.readInt32();
        index->u4SatAdj[2] = reply.readInt32();
        index->u4SatAdj[3] = reply.readInt32();;
        index->u4Contrast = reply.readInt32();
        index->u4Brightness = reply.readInt32();
        return ret;
    }

    virtual status_t getTDSHPIndex(DISP_PQ_PARAM *index, int32_t scenario, int32_t mode)
    {
        Parcel data, reply;
        data.writeInt32(scenario);
        data.writeInt32(mode);
        if (remote()->transact(PQ_GET_TDSHP_INDEX, data, &reply) != NO_ERROR) {
            ALOGE("getParamters could not contact remote\n");
            return -1;
        }

        status_t ret = static_cast<status_t>(reply.readInt32());
        index->u4SHPGain = reply.readInt32();

        return ret;
    }
    virtual status_t setDISPScenario(int32_t scenario)
    {
        Parcel data, reply;
        data.writeInt32(scenario);

        if (remote()->transact(PQ_SET_SCENARIO, data, &reply) != NO_ERROR) {
            ALOGE("getParamters could not contact remote\n");
            return -1;
        }

        status_t ret = static_cast<status_t>(reply.readInt32());

        return ret;
    }

    virtual status_t setFeatureSwitch(PQFeatureID id, uint32_t value)
    {
        Parcel data, reply;

        data.writeInt32(id);
        data.writeInt32(value);
        if (remote()->transact(PQ_SET_FEATURE_SWITCH, data, &reply) != NO_ERROR) {
            ALOGE("PQ_SET_FEATURE_SWITCH() could not contact remote\n");
            return -1;
        }

        status_t ret = static_cast<status_t>(reply.readInt32());

        return ret;
    }

    virtual status_t getFeatureSwitch(PQFeatureID id, uint32_t *value)
    {
        Parcel data, reply;

        data.writeInt32(id);
        if (remote()->transact(PQ_GET_FEATURE_SWITCH, data, &reply) != NO_ERROR) {
            ALOGE("PQ_GET_FEATURE_SWITCH() could not contact remote\n");
            return -1;
        }

        status_t ret = static_cast<status_t>(reply.readInt32());
        *value = reply.readInt32();

        return ret;
    }

};


IMPLEMENT_META_INTERFACE(PQService, "PQService");

status_t BnPQService::onTransact(uint32_t code, const Parcel& data, Parcel* reply, uint32_t flags)
{
    status_t ret = 0;
    //ALOGD("receieve the command code %d", code);

    switch(code)
    {
        case PQ_SET_INDEX:
            {
                int32_t level;
                int32_t scenario;
                int32_t tuning_mode;
                int32_t index;

                data.readInt32(&level);
                data.readInt32(&scenario);
                data.readInt32(&tuning_mode);
                data.readInt32(&index);
                ret = setPQIndex(level,scenario,tuning_mode,index);

                reply->writeInt32(ret);

            }
            break;
        case PQ_GET_MAPPED_COLOR_INDEX:
            {
                DISP_PQ_PARAM  param;
                int32_t scenario;
                int32_t mode;
                data.readInt32(&scenario);
                data.readInt32(&mode);

                ret = getMappedColorIndex(&param,scenario,mode);

                reply->writeInt32(ret);
                reply->writeInt32(param.u4SatGain);
                reply->writeInt32(param.u4PartialY);
                reply->writeInt32(param.u4HueAdj[0]);
                reply->writeInt32(param.u4HueAdj[1]);
                reply->writeInt32(param.u4HueAdj[2]);
                reply->writeInt32(param.u4HueAdj[3]);
                reply->writeInt32(param.u4SatAdj[0]);
                reply->writeInt32(param.u4SatAdj[1]);
                reply->writeInt32(param.u4SatAdj[2]);
                reply->writeInt32(param.u4SatAdj[3]);
                reply->writeInt32(param.u4Contrast);
                reply->writeInt32(param.u4Brightness);
            }
            break;
        case PQ_GET_MAPPED_TDSHP_INDEX:
            {
                DISP_PQ_PARAM  param;
                int32_t scenario;
                int32_t mode;
                data.readInt32(&scenario);
                data.readInt32(&mode);

                ret = getMappedTDSHPIndex(&param,scenario,mode);

                reply->writeInt32(ret);
                reply->writeInt32(param.u4SHPGain);

            }
            break;
        case PQ_GET_COLOR_INDEX:
            {
                DISP_PQ_PARAM  param;
                int32_t scenario;
                int32_t mode;
                data.readInt32(&scenario);
                data.readInt32(&mode);

                ret = getColorIndex(&param,scenario,mode);

                reply->writeInt32(ret);
                reply->writeInt32(param.u4SatGain);
                reply->writeInt32(param.u4PartialY);
                reply->writeInt32(param.u4HueAdj[0]);
                reply->writeInt32(param.u4HueAdj[1]);
                reply->writeInt32(param.u4HueAdj[2]);
                reply->writeInt32(param.u4HueAdj[3]);
                reply->writeInt32(param.u4SatAdj[0]);
                reply->writeInt32(param.u4SatAdj[1]);
                reply->writeInt32(param.u4SatAdj[2]);
                reply->writeInt32(param.u4SatAdj[3]);
                reply->writeInt32(param.u4Contrast);
                reply->writeInt32(param.u4Brightness);
            }
            break;
        case PQ_SET_PQDC_INDEX:
            {
                int32_t level;
                int32_t index;

                data.readInt32(&level);
                data.readInt32(&index);

                ret = setPQDCIndex(level,index);

                reply->writeInt32(ret);

                // LumaAdj should be added  later

            }
            break;
        case PQ_GET_PQDC_INDEX:
            {
                DISP_PQ_DC_PARAM dcparam;
                int32_t index;
                int32_t i;
                data.readInt32(&index);

                ret = getPQDCIndex(&dcparam,index);

                reply->writeInt32(ret);

                for(i = 0; i < 40; i++){
                    reply->writeInt32(dcparam.param[i]);
                }

                // LumaAdj should be added  later

            }
            break;
        case PQ_GET_PQDS_INDEX:
            {
                DISP_PQ_DS_PARAM dsparam;
                int32_t i;

                ret = getPQDSIndex(&dsparam);

                reply->writeInt32(ret);

                for(i = 0; i < PQ_DS_INDEX_MAX; i++){
                    reply->writeInt32(dsparam.param[i]);
                }

            }
            break;
        case PQ_GET_TDSHP_INDEX:
            {
                DISP_PQ_PARAM  param;
                int32_t scenario;
                int32_t mode;
                data.readInt32(&scenario);
                data.readInt32(&mode);

                ret = getTDSHPIndex(&param,scenario,mode);

                reply->writeInt32(ret);
                reply->writeInt32(param.u4SHPGain);

            }
            break;
        case PQ_GET_COLOR_CAP:
            {
                MDP_COLOR_CAP param;

                ret = getColorCapInfo(&param);

                reply->writeInt32(ret);
                reply->writeInt32(param.en);
                reply->writeInt32(param.pos_x);
                reply->writeInt32(param.pos_y);
            }
            break;
        case PQ_GET_TDSHP_REG:
            {
                MDP_TDSHP_REG param;

                ret = getTDSHPReg(&param);

                reply->writeInt32(ret);
                reply->writeInt32(param.TDS_GAIN_MID);
                reply->writeInt32(param.TDS_GAIN_HIGH);
                reply->writeInt32(param.TDS_COR_GAIN);
                reply->writeInt32(param.TDS_COR_THR);
                reply->writeInt32(param.TDS_COR_ZERO);
                reply->writeInt32(param.TDS_GAIN);
                reply->writeInt32(param.TDS_COR_VALUE);
            }
            break;
        case PQ_SET_MODE:
            {
                int32_t mode;
                data.readInt32(&mode);

                ret = setPQMode(mode);

                reply->writeInt32(ret);

            }
            break;
        case PQ_SET_COLOR_REGION:
            {
                int32_t split_en;
                int32_t start_x;
                int32_t start_y;
                int32_t end_x;
                int32_t end_y;
                data.readInt32(&split_en);
                data.readInt32(&start_x);
                data.readInt32(&start_y);
                data.readInt32(&end_x);
                data.readInt32(&end_y);
                ret = setColorRegion(split_en,start_x,start_y,end_x,end_y);

                reply->writeInt32(ret);

            }
            break;
        case PQ_GET_COLOR_REGION:
            {
                DISP_PQ_WIN_PARAM param;
                ret = getColorRegion(&param);

                reply->writeInt32(ret);
                reply->writeInt32(param.split_en);
                reply->writeInt32(param.start_x);
                reply->writeInt32(param.start_y);
                reply->writeInt32(param.end_x);
                reply->writeInt32(param.end_y);
            }
            break;
        case PQ_GET_TDSHP_FLAG:
            {
                int32_t tdshp_flag;
                ret = getTDSHPFlag(&tdshp_flag);

                reply->writeInt32(ret);
                reply->writeInt32(tdshp_flag);
            }
            break;
        case PQ_SET_TDSHP_FLAG:
            {
                int32_t tdshp_flag;
                 data.readInt32(&tdshp_flag);
                ret = setTDSHPFlag(tdshp_flag);

                reply->writeInt32(ret);
            }
            break;
        case PQ_SET_SCENARIO:
            {
                int32_t scenario;
                data.readInt32(&scenario);
                ret = setDISPScenario(scenario);

                reply->writeInt32(ret);
            }
            break;

        case PQ_SET_FEATURE_SWITCH:
            {

                int32_t id;
                int32_t value;
                data.readInt32(&id);
                data.readInt32(&value);

                ret = setFeatureSwitch(static_cast<PQFeatureID>(id), value);

                reply->writeInt32(ret);
            }
            break;

        case PQ_GET_FEATURE_SWITCH:
            {
                int32_t id;
                uint32_t value;
                data.readInt32(&id);

                ret = getFeatureSwitch(static_cast<PQFeatureID>(id), &value);

                reply->writeInt32(ret);
                reply->writeInt32(value);
            }
            break;


        default:
            return BBinder::onTransact(code, data, reply, flags);
    }
    return ret;
}
};

