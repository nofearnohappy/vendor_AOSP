/******************************************************************************
 *  This program is protected under international and U.S. copyright laws as
 *  an unpublished work. This program is confidential and proprietary to the
 *  copyright owners. Reproduction or disclosure, in whole or in part, or the
 *  production of derivative works therefrom without the express permission of
 *  the copyright owners is prohibited.
 *
 *                 Copyright (C) 2011-2014 by Dolby Laboratories,
 *                             All rights reserved.
 ******************************************************************************/

#define LOG_TAG "ddpdec_client_joc"
#include "DlbLog.h"

/**** Module Dependencies ****/

/* subroutine headers */
#include "udc_api.h"

/* executive headers */
#include "udc_exec.h"
#include "udc_user.h"
#include "ddpdec_client.h"

#define true 1
#define false 0

typedef enum
{
    DDP_OUTPUT_NON_INTERLEAVED=0,
    DDP_OUTPUT_INTERLEAVED
} DDP_OUTPUT_INTERLEAVE_FLAG;

/*****************************************************************
* ddpdec_open: Initialize the UDC subroutine memory.
*****************************************************************/
void *ddpdec_open(tDdpDecoderExternal *pExt)
{
    int err;
    int sub_err;
    char *p_outbuf = NULL;
    char *p_outbuf_align = NULL;
    char *p_dynamic_buf = NULL;
    char *p_dynamic_buf_align = NULL;
    char *p_udcmem = NULL;
    char *udcmem = NULL;
    UDCEXEC *p_udcexec = NULL;
    ddpi_udc_query_ip queryip;
    int i;
    int is_interleaved_flag;
    int nstride;
    int num_out_chans;
    int out_data_type;

    p_udcexec = (UDCEXEC *)malloc(sizeof(UDCEXEC)) ;
    if (p_udcexec == NULL)
    {
        return NULL;
    }

    /* initialize executive memory to zero */
    memset(p_udcexec,0,sizeof(UDCEXEC));

    /* initialize executive parameters to defaults */
    err = initexecparams(&(p_udcexec->execparams));
    if (err)
    {
        goto BAIL;
    }

    /* initialize subroutine parameters to defaults */
    for (i = 0; i < DDPI_UDC_PCMOUT_COUNT; i++)
    {
        err = initsubparams(&((p_udcexec->subparams)[i]));
        if (err)
        {
            goto BAIL;
        }
    }

    /* initialize query input parameters to zero */
    memset(&queryip, 0, sizeof(queryip));

    /* query subroutine for configuration information */
    /* Disable DD Conversion */
    queryip.converter = 0;

    /* Disable mixer */
    queryip.mixer_mode = 0;

    queryip.num_outputs = p_udcexec->execparams.numofpcmout;
    queryip.num_main_outputs = p_udcexec->execparams.numofmainpcmout;

#ifdef DOLBY_UDC_VIRTUALIZE_AUDIO
    if (pExt->isJocDecodeMode)
#else
    ALOGI_IF(pExt->isJocDecodeMode, "%s() Outputting PCM instead of metadata in JOC mode.", __FUNCTION__);
    if (0)
#endif // DOLBY_END
    {
        ALOGV("%s in JOC mode", __FUNCTION__);
        //in case of JOC input decoder parameters are configured during init time and not in run time.
#ifdef DOLBY_UDC_OUTPUT_IN_QMF  
        p_udcexec->subparams[0].runningmode = DDPI_UDC_JOC_DECODE_QMF_OUT;
#else
        p_udcexec->subparams[0].runningmode = DDPI_UDC_JOC_DECODE_PCM_OUT;
#endif // DOLBY_END

        p_udcexec->subparams[0].joc_force_downmix = pExt->jocForceDownmixMode;
        switch (p_udcexec->subparams[0].runningmode)
        {
            case DDPI_UDC_JOC_DECODE_QMF_OUT:
                queryip.jocd_mode = DDPI_UDC_JOCD_QMF_OUT;
                break;
            case DDPI_UDC_JOC_DECODE_PCM_OUT:
                queryip.jocd_mode = DDPI_UDC_JOCD_PCM_OUT;
                break;
            case DDPI_UDC_LOW_COMPLEXITY_JOC_DECODE_QMF_OUT:
                queryip.jocd_mode = DDPI_UDC_JOCD_QMF_OUT_LC;
                break;
            case DDPI_UDC_LOW_COMPLEXITY_JOC_DECODE_PCM_OUT:
                queryip.jocd_mode = DDPI_UDC_JOCD_PCM_OUT_LC;
                break;
            default:
                ALOGE("%s runningmode not set", __FUNCTION__);
                goto BAIL;
        }
        p_udcexec->subparams[0].outlfe = DDPI_UDC_LFEOUTSINGLE;
        p_udcexec->subparams[0].compmode = DDPI_UDC_COMP_LINE;

        /* Set the ouput channel configuration to be raw output mode, i.e. no channel routing */
        p_udcexec->subparams[0].outchanconfig = DDPI_UDC_OUTMODE_RAW;

        if (p_udcexec->subparams[0].runningmode == DDPI_UDC_JOC_DECODE_QMF_OUT)
        {
            pExt->isDataInQMF = true;
        }
    }
    else
    {
        // by default running mode is set to DECODE_ONLY and not setting it explicitly here
        ALOGV("%s in non-JOC mode", __FUNCTION__);
        queryip.jocd_mode = DDPI_UDC_JOCD_DISABLE;
    }

    /* query the library to find out the static configuration */
    err = ddpi_udc_query(&(p_udcexec->query_op), queryip.jocd_mode);
    if (err)
    {
        ALOGE("%s ddpi_udc_query returs %d", __FUNCTION__, err);
        goto BAIL;
    }

#ifdef DOLBY_UDC_OUTPUT_TWO_BYTES_PER_SAMPLE
    out_data_type = DLB_BUFFER_SHORT_16;
#else
    out_data_type = DLB_BUFFER_INT_LEFT;
#endif

    is_interleaved_flag = DDP_OUTPUT_INTERLEAVED;
    /* Set maxnumchannels */
    for (i = 0; i < queryip.num_outputs; i++)
    {
        queryip.outputs[i].maxnumchannels = p_udcexec->execparams.outnchans[i];
    }

    sub_err = ddpi_udc_query_mem(&queryip, &p_udcexec->query_mem_op);
    if (sub_err)
    {
        err = ERR_QUERY_FAILED;
        ALOGE("%s ddpi_udc_query_mem returs %d", __FUNCTION__, err);
        goto BAIL;
    }

    ALOGV("UDC Memory requirements:\n\
        UDC Static: %d bytes\n\
        UDC Dynamic: %d bytes\n\
        Output Buffers: %d bytes\n\
        DD Output Buffers: %d bytes",
        p_udcexec->query_mem_op.udc_static_size,
        p_udcexec->query_mem_op.udc_dynamic_size,
        p_udcexec->query_mem_op.outputbuffersize,
        p_udcexec->query_mem_op.dd_buffersize);

    /* Allocate output buffers and take care of alignment; the original pointer must be used
       for freeing the memory */
    p_outbuf = (char *) calloc(1, p_udcexec->query_mem_op.outputbuffersize+(DDPI_UDC_MIN_MEMORY_ALIGNMENT-1));
    if (!p_outbuf)
    {
        err = ERR_MEMORY;
        goto BAIL;
    }
    p_outbuf_align = (char *) ((((long) p_outbuf) +(DDPI_UDC_MIN_MEMORY_ALIGNMENT-1)) & ~(DDPI_UDC_MIN_MEMORY_ALIGNMENT-1));

    /* allocate memory for subroutine and take care of alignment */
    udcmem = (char *)calloc(1, p_udcexec->query_mem_op.udc_static_size + (DDPI_UDC_MIN_MEMORY_ALIGNMENT-1));
    if (!udcmem)
    {
        err = ERR_MEMORY;
        goto BAIL;
    }
	p_udcexec->p_dechdl = (void *)((long)(udcmem+(DDPI_UDC_MIN_MEMORY_ALIGNMENT-1)) & ~(DDPI_UDC_MIN_MEMORY_ALIGNMENT-1));

    /* Allocate dynamic buffers and take care of alignment; the original pointer must be used
       for freeing the memory */
    p_dynamic_buf = (char *) calloc(1, p_udcexec->query_mem_op.udc_dynamic_size+(DDPI_UDC_MIN_MEMORY_ALIGNMENT-1));
    if (!p_dynamic_buf)
    {
        err = ERR_MEMORY;
        goto BAIL;
    }
    p_dynamic_buf_align = (char *) ((((long) p_dynamic_buf) + (DDPI_UDC_MIN_MEMORY_ALIGNMENT - 1)) & ~(DDPI_UDC_MIN_MEMORY_ALIGNMENT - 1));

    pExt->pOutMem = p_outbuf;
    pExt->pUdcMem = udcmem;
    pExt->pUdcDynamicMem = p_dynamic_buf;

    /* display banner info (regardless of verbose mode) */
    err = displaybanner(&(p_udcexec->query_op));
    if (err)
    {
        goto BAIL;
    }

    /* open (initialize) subroutine memory */
    sub_err = ddpi_udc_open(&queryip, p_udcexec->p_dechdl, p_dynamic_buf_align);
    if (sub_err)
    {
        err = ERR_OPEN_FAILED;
        ALOGE("%s ddpi_udc_open failed\n", __FUNCTION__);
        goto BAIL;
    }

    /* set subroutine parameters */
    sub_err = setsubparams(queryip.num_outputs, &(p_udcexec->subparams[0]), p_udcexec->p_dechdl);
    if (sub_err)
    {
        err = ERR_PROCESSING;
        ALOGE("%s setsubparams failed\n", __FUNCTION__);
        goto BAIL;
    }

    if(queryip.num_outputs > 0)
    {
        if (p_udcexec->subparams[0].runningmode == DDPI_UDC_JOC_DECODE_QMF_OUT || 
            p_udcexec->subparams[0].runningmode == DDPI_UDC_LOW_COMPLEXITY_JOC_DECODE_QMF_OUT)
        {
            ALOGI("%s JOCD_MODE Buffer assignment", __FUNCTION__);
            p_udcexec->ptop.joc_qmfbuf_out = (void **)p_outbuf_align;
        }
        else
        {
            ALOGI("%s NON-JOCD_MODE Buffer assignment", __FUNCTION__);
            /* If there's PCM output, set the output buffer descriptor, else initialized to NULL. */        
            p_udcexec->ptop.pcmoutbfds = p_udcexec->a_outputbuf;

            if(queryip.jocd_mode != DDPI_UDC_JOCD_DISABLE)
            {
                num_out_chans = DDPI_JOCD_MAX_CHANNELS;
            }
            else
            {
                num_out_chans = DDPI_UDC_MAXPCMOUTCHANS;
            }
            if(is_interleaved_flag)
            {
                nstride = num_out_chans;
            }
            else
            {
                nstride = 1;
            }
            for (i = 0; i < queryip.num_outputs; i++)
            {
                p_udcexec->ptop.pcmoutbfds[i].ppdata = (void **) malloc(sizeof(void *) * num_out_chans);
                p_udcexec->ptop.pcmoutbfds[i].data_type = out_data_type;
                p_udcexec->ptop.pcmoutbfds[i].nstride = nstride;
            }
            /* Set the output buffer pointer, this can be updated each time before calling ddpi_udc_processtime()  */
            p_udcexec->a_outputbuf[0].ppdata[0] = p_outbuf_align;
        }
    }
    else
    {
        p_udcexec->ptop.pcmoutbfds = NULL;
    }
    
    /* No converter buffers */
    p_udcexec->ptop.ddoutbfd = NULL;
    p_udcexec->ddoutbuf.ppdata = NULL;

    return (void *) p_udcexec;

BAIL:
    ddpdec_close(pExt, p_udcexec);
    return NULL;
}

/*****************************************************************
* ddpdec_close: Perform all clean up necessary to close the UDC.
*****************************************************************/
void ddpdec_close(tDdpDecoderExternal *pExt, void *pMem)
{
    UDCEXEC   *p_udcexec = (UDCEXEC *) pMem;
    char *p_outbuf = pExt->pOutMem;
    char *udcmem = pExt->pUdcMem;
    char *p_dynamic_buf = pExt->pUdcDynamicMem;
    int i;

    if (p_udcexec == NULL)
    {
        return;
    }

    /* close subroutine memory */
    if (p_udcexec->p_dechdl != NULL)
    {
        ddpi_udc_close(p_udcexec->p_dechdl);
        p_udcexec->p_dechdl = NULL;
    }

    /* free subroutine memory */
    free(udcmem);
    udcmem = NULL;

    /* Free output buffers */
    free(p_outbuf);
    p_outbuf = NULL;

    /* Free dynamic buffers */
    free(p_dynamic_buf);
    p_dynamic_buf = NULL;

    /* Free data buffers */
    for (i = 0; i < DDPI_UDC_PCMOUT_COUNT; i++)
    {
        free(p_udcexec->a_outputbuf[i].ppdata);
        p_udcexec->a_outputbuf[i].ppdata = NULL;
    }

    free(p_udcexec);
    p_udcexec = NULL;

    return;
}

int configOutChannel(UDCEXEC *p_udcexec, tDdpDecoderExternal *pExt)
{
    int err = ERR_NO_ERROR;
    int instance = 0;
    int acmod, lfeOn;
    int depFrmFlag;
    int channelMap;
    int outChanNum;

    // API for extract timeslice metadata
    ddpi_udc_timeslice_mdat timeslice_mdat;
    err = ddpi_udc_gettimeslice_mdat(p_udcexec->p_dechdl, DDPI_UDC_DDPIN_0, &timeslice_mdat);

    // bit stream related information
    ALOGD("pgm_count=%d\n", timeslice_mdat.pgm_count);
    ALOGD("dep_substream_count = %d", timeslice_mdat.pgm_mdat[0].dep_substream_count);
    ALOGD("aggregate channel_map = %d", timeslice_mdat.pgm_mdat[0].channel_map);
    ALOGD("I0: frm_present=%d\n", timeslice_mdat.pgm_mdat[0].ind_frm_mdat.frm_present);
    ALOGD("I0: frm_id=%d\n", timeslice_mdat.pgm_mdat[0].ind_frm_mdat.frm_id);
    ALOGD("I0: minbsi_valid=%d\n", timeslice_mdat.pgm_mdat[0].ind_frm_mdat.minbsi_valid);
    ALOGD("I0: acmod=%d\n", timeslice_mdat.pgm_mdat[0].ind_frm_mdat.minbsi_mdat.acmod);
    ALOGD("I0: lfeon=%d\n", timeslice_mdat.pgm_mdat[0].ind_frm_mdat.minbsi_mdat.lfeon);
    ALOGD("I0: chanmap=%d\n", timeslice_mdat.pgm_mdat[0].ind_frm_mdat.minbsi_mdat.chanmap);
    ALOGD("D0: frm_present=%d\n", timeslice_mdat.pgm_mdat[0].dep_frm_mdat[0].frm_present);
    ALOGD("D0: frm_id=%d\n", timeslice_mdat.pgm_mdat[0].dep_frm_mdat[0].frm_id);
    ALOGD("D0: minbsi_valid=%d\n", timeslice_mdat.pgm_mdat[0].dep_frm_mdat[0].minbsi_valid);
    ALOGD("D0: acmod=%d\n", timeslice_mdat.pgm_mdat[0].dep_frm_mdat[0].minbsi_mdat.acmod);
    ALOGD("D0: lfeon=%d\n", timeslice_mdat.pgm_mdat[0].dep_frm_mdat[0].minbsi_mdat.lfeon);
    ALOGD("D0: chanmap=%d\n", timeslice_mdat.pgm_mdat[0].dep_frm_mdat[0].minbsi_mdat.chanmap);


    if (timeslice_mdat.pgm_mdat[0].ind_frm_mdat.frm_present)
    {
        acmod = timeslice_mdat.pgm_mdat[0].ind_frm_mdat.minbsi_mdat.acmod;
        lfeOn = timeslice_mdat.pgm_mdat[0].ind_frm_mdat.minbsi_mdat.lfeon;
    }
    else
    {
        acmod = BSI_ACMOD_20;
        lfeOn = 0;
    }
    depFrmFlag = timeslice_mdat.pgm_mdat[0].dep_substream_count;
    channelMap = timeslice_mdat.pgm_mdat[0].channel_map;

    /* The DD+ decoder shall use the following mapping of input channel configrations to output channel configurations
        1.  1.0 , 2.0, 2.1 shall be mapped to 2.0
        2.  3.0, ..., 5.1 shall be mapped to 5.1
        3.  6.0, ..., 7.1 shall be mapped to 7.1
    */
    switch (pExt->nPCMOutMaxChannels)
    {
        case 2:
        {
            acmod=BSI_ACMOD_20;
            lfeOn=0;
            outChanNum = 2;
            break;
        }
        case 6:
        {
            if (acmod > BSI_ACMOD_20)
            {
                acmod = BSI_ACMOD_32;
                lfeOn=1;
                outChanNum = 6;
            }
            else
            {
                acmod = BSI_ACMOD_20;
                lfeOn=0;
                outChanNum = 2;
            }
            break;
        }
        case 8:
        {
            if (depFrmFlag)
            {
                if ((channelMap & 0x300) != 0)
                {
                    acmod = BSI_ACMOD_322;
                    lfeOn = 1;
                    outChanNum = 8;
                }
                else
                {
                    acmod = BSI_ACMOD_32;
                    lfeOn=1;
                    outChanNum = 6;
                }
            }
            else
            {
                if (acmod > BSI_ACMOD_20)
                {
                    acmod = BSI_ACMOD_32;
                    lfeOn=1;
                    outChanNum = 6;
                }
                else
                {
                    acmod = BSI_ACMOD_20;
                    lfeOn=0;
                    outChanNum = 2;
                }
            }
            break;
        }
        default:
        {
            acmod=BSI_ACMOD_20;
            lfeOn=0;
            outChanNum = 2;
        }
    }

    p_udcexec->execparams.outnchans[DDPI_UDC_PCMOUT_MAIN] = outChanNum;
    if (acmod != p_udcexec->subparams[0].outchanconfig)
    {
        p_udcexec->subparams[0].outchanconfig = acmod;
        err = ddpi_udc_setoutparam(
            p_udcexec->p_dechdl,
            instance,
            DDPI_UDC_OUTCTL_OUTMODE_ID,
            &p_udcexec->subparams[0].outchanconfig,
            sizeof(p_udcexec->subparams[0].outchanconfig));
        if (err != 0)
        {
            ALOGE("%s acmod %d DDPI_UDC_OUTCTL_OUTMODE_ID FAILED with err %d", __FUNCTION__, acmod, err);
            return err;
        }
    }

    if (lfeOn != p_udcexec->subparams[0].outlfe)
    {
        p_udcexec->subparams[0].outlfe = lfeOn;
        err = ddpi_udc_setoutparam(
            p_udcexec->p_dechdl,
            instance,
            DDPI_UDC_OUTCTL_OUTLFEON_ID,
            &p_udcexec->subparams[0].outlfe,
            sizeof(p_udcexec->subparams[0].outlfe));
        if (err != 0)
        {
            ALOGE("%s lfeOn %d DDPI_UDC_OUTCTL_OUTLFEON_ID FAILED with err %d", __FUNCTION__, lfeOn, err);
            return err;
        }
    }
    return err;
}

int configStereoMode(UDCEXEC *p_udcexec, tDdpDecoderExternal *pExt)
{
    int err = ERR_NO_ERROR;
    int instance = 0;

    if (pExt->downmixMode != p_udcexec->subparams[0].stereomode)
    {
        p_udcexec->subparams[0].stereomode = pExt->downmixMode;
        err = ddpi_udc_setoutparam(
            p_udcexec->p_dechdl,
            instance,
            DDPI_UDC_OUTCTL_STEREOMODE_ID,
            &p_udcexec->subparams[0].stereomode,
            sizeof(p_udcexec->subparams[0].stereomode));
        if (err != 0)
        {
            ALOGE("%s failed with error %d", __FUNCTION__, err);
            return err;
        }
    }
    return err;
}

int configDRCMode(UDCEXEC *p_udcexec, tDdpDecoderExternal *pExt)
{
    int err = ERR_NO_ERROR;
    int instance = 0;

    if (pExt->drcMode != p_udcexec->subparams[0].compmode)
    {
        p_udcexec->subparams[0].compmode = pExt->drcMode;
        err = ddpi_udc_setoutparam(
            p_udcexec->p_dechdl,
            instance,
            DDPI_UDC_OUTCTL_COMPMODE_ID,
            &p_udcexec->subparams[0].compmode,
            sizeof(p_udcexec->subparams[0].compmode));
        if (err != 0)
        {
            ALOGE("%s failed with error %d", __FUNCTION__, err);
            return err;
        }
    }
    return err;
}

int configJocForceDownmixMode(UDCEXEC *p_udcexec, tDdpDecoderExternal *pExt)
{
    int err = ERR_NO_ERROR;
    int instance = 0;    

    if (pExt->jocForceDownmixMode != p_udcexec->subparams[0].joc_force_downmix)
    {
        ALOGI("%s setting joc_force_downmix %d", __FUNCTION__, pExt->jocForceDownmixMode);
        p_udcexec->subparams[0].joc_force_downmix = pExt->jocForceDownmixMode;
        err = ddpi_udc_setprocessparam(
            p_udcexec->p_dechdl,
            DDPI_UDC_CTL_FORCE_JOC_OUPUT_DMX_ID,
            &p_udcexec->subparams[0].joc_force_downmix,
            sizeof(p_udcexec->subparams[0].joc_force_downmix));
        if (err != 0)
        {
            ALOGE("%s failed with error %d", __FUNCTION__, err);
            return err;
        }
    }
    return err;
}

/*****************************************************************
* configDecoder:
*****************************************************************/
int configDecoder(UDCEXEC *p_udcexec, tDdpDecoderExternal *pExt)
{
    int err = ERR_NO_ERROR;
    ALOGD("ddpdec_client.c|int configDecoder()");
    
    if (pExt->isJocDecodeMode)
    {
        configJocForceDownmixMode(p_udcexec, pExt);
    }
    else
    {
        err = configOutChannel(p_udcexec, pExt);
        if (err != ERR_NO_ERROR)
        {
            ALOGE("%s configOutChannel failed with err %d", __FUNCTION__, err);
            return err;
        }

        err = configStereoMode(p_udcexec, pExt);
        if (err != ERR_NO_ERROR)
        {
            ALOGE("%s configStereoMode failed with err %d", __FUNCTION__, err);
            return err;
        }
       
        err = configDRCMode(p_udcexec, pExt);
        if (err != ERR_NO_ERROR)
        {
            ALOGE("%s configDRCMode failed with err %d", __FUNCTION__, err);
            return err;
        }
    }
    return err;
}

/*****************************************************************
* processudcoutput:
*****************************************************************/
int processudcoutput(
    UDCEXEC *p_udcexec, char *p_buf)      /* modify */
{
    /* declare local variables */
    int err = DDPI_UDC_ERR_NO_ERROR;
    int pcmout_index;
#ifdef DOLBY_UDC_OUTPUT_TWO_BYTES_PER_SAMPLE
    short *p_outbuf = (short *) p_buf;
    short *p_samp[FIO_MAXPCMCHANS];
#else
    int *p_outbuf = (int *) p_buf;
    int *p_samp[FIO_MAXPCMCHANS];
#endif
    int i;

    /* handle any errors reported in the process timeslice output params */
    for (pcmout_index = DDPI_UDC_PCMOUT_MAIN; pcmout_index < p_udcexec->execparams.numofpcmout; pcmout_index++)
    {
        switch (p_udcexec->ptop.errflag[pcmout_index])
        {
            /* some errors do not affect the output and processing can continue */
            case DDPI_UDC_ERR_NO_ERROR:
            {
                break;
            }
            case DDPI_UDC_ERR_INVALID_TIMESLICE:
            {
                ALOGE("Invalid timeslice in audio program %d", pcmout_index);
                break;
            }
            case DDPI_UDC_ERR_PROG_MISSING:
            {
                ALOGE("Audio program %d missing", pcmout_index);
                break;
            }
            case DDPI_UDC_ERR_PROG_INCONSISTENT:
            {
                ALOGE("Main and associated audio programs (%d) are inconsistent", pcmout_index);
                break;
            }
            default:
            /* unknown or unexpected error code */
            {
                ALOGE("Error code %d returned in audio program %d", p_udcexec->ptop.errflag[pcmout_index], pcmout_index);
                break;
            }
        }

        /* Write out the PCM data if required */
        if (p_udcexec->ptop.pcmdatavalid[pcmout_index])
        {
            int ch;
            int s;
            int samp;
            int jocd_mode = ( (p_udcexec->subparams[0].runningmode == DDPI_UDC_JOC_DECODE_PCM_OUT) || (p_udcexec->subparams[0].runningmode == DDPI_UDC_JOC_DECODE_QMF_OUT) );

            if(jocd_mode)
            {
                /* JOC decoding mode*/
                /* Assign the output buffers */
                for(ch = 0; ch < (int) p_udcexec->ptop.jocd_active_channels; ch++)
                {
                    p_samp[ch] = p_udcexec->a_outputbuf[pcmout_index].ppdata[p_udcexec->execparams.chanrouting[pcmout_index][ch]];
                }

                /* The samples are already interleaved */
                for (s = 0; s < (int)(p_udcexec->ptop.nblkspcm  * FIO_BLKSIZE); s++)
                {
                    for (ch = 0; ch < (int) p_udcexec->ptop.jocd_active_channels; ch++)
                    {
                        samp = p_samp[ch][s * p_udcexec->a_outputbuf[pcmout_index].nstride];
                        *p_outbuf++ = samp;
                    }
                }
                if (p_udcexec->ptop.oamd_status == DDPI_UDC_OAMD_STATUS_UNMATCH)
                {
                    ALOGE("Warning: OAMD doesn't match the JOC output in timeslice");
                }
                else if (p_udcexec->ptop.oamd_status != DDPI_UDC_OAMD_STATUS_VALID)
                {
                    ALOGE("Warning: No valid oamd data associated with JOC decoding in timeslice");
                }
            }

            else
            {
                /* Assign the output buffers */
                for (ch = 0; ch < p_udcexec->execparams.outnchans[pcmout_index]; ch++)
                {
                    p_samp[ch] = p_udcexec->a_outputbuf[pcmout_index].ppdata[p_udcexec->execparams.chanrouting[pcmout_index][ch]];
                }

                /* The samples are already interleaved */
                for (s = 0; s < (int)(p_udcexec->ptop.nblkspcm  * FIO_BLKSIZE); s++)
                {
                    for (ch = 0; ch < p_udcexec->execparams.outnchans[pcmout_index]; ch++)
                    {
                        samp = p_samp[ch][s * p_udcexec->a_outputbuf[pcmout_index].nstride];
                        *p_outbuf++ = samp;
                    }
                }
            }
        }
    }

error:
    return err ? ERR_PROCESS_OUTPUT : 0;
}

/*****************************************************************
* ddpdec_process: Add a certain amount of bytes to the input of the decoder.
* and Processe a DD/DD+ timeslice.
*****************************************************************/
int ddpdec_process(tDdpDecoderExternal *pExt, void *pMem)
{
    UDCEXEC   *p_udcexec = (UDCEXEC *) pMem;
    unsigned int bytesconsumed;
    unsigned int avail;
    int timeslicecomplete;
    char *p_buf;
    char *p_dst;
    char *p_outbuf_align = NULL;
    char *p_outbuf;
    int err = ERR_NO_ERROR;
    int sub_err = DDPI_UDC_ERR_NO_ERROR;

    if (pExt == NULL)
    {
        ALOGE("ddpdec_client.c|  > DD+ DECODER : ddpdec_process FAILED! pExt == NULL");
        return DDPAUDEC_INIT_FAILURE;
    }
    if (pMem == NULL)
    {
        ALOGE("ddpdec_client.c|  > DD+ DECODER : ddpdec_process FAILED! p_udcexec == NULL");
        return DDPAUDEC_INIT_FAILURE;
    }

    p_dst = pExt->pOutputBuffer;
    p_outbuf = pExt->pOutMem;
    p_buf = pExt->pInputBuffer;
    avail = pExt->inputBufferCurrentLength;

    sub_err = ddpi_udc_addbytes(p_udcexec->p_dechdl, p_buf, avail, DDPI_UDC_DDPIN_0, &bytesconsumed, &timeslicecomplete);

    ALOGV("ddpi_udc_addbytes: mem=%x, avail=%d, bytesconsumed=%d, timeslice=%d, err=%d\n", (unsigned int)(p_udcexec->p_dechdl), avail, bytesconsumed, timeslicecomplete, err);

    /* in case of error, just exit */
    if (sub_err)
    {
        ALOGI("ddpi_udc_addbytes returned %d\n", sub_err);
    }

    pExt->inputBufferUsedLength += bytesconsumed;

    if (timeslicecomplete)
    {
         int jocd_mode = ( (p_udcexec->subparams[0].runningmode == DDPI_UDC_JOC_DECODE_PCM_OUT) || (p_udcexec->subparams[0].runningmode == DDPI_UDC_JOC_DECODE_QMF_OUT) ||
                            (p_udcexec->subparams[0].runningmode == DDPI_UDC_JOCD_QMF_OUT_LC));
        
        if (pExt->updatedChannelRouting == 1)
        {
            // up/downmix and downmix mode controlled from stagefright layer. Clear the changed flag if it was set.
            pExt->updatedChannelRouting = 0;
            if (configDecoder(p_udcexec, pExt) != ERR_NO_ERROR)
                ALOGE("%s setup channel routing failed", __FUNCTION__);
        }

        /*********************************************\
        *  Call subroutine function to process frame  *
        \*********************************************/
        sub_err = ddpi_udc_processtimeslice(
                p_udcexec->p_dechdl,
                &(p_udcexec->ptop));
        if (sub_err)
        {   
            ALOGI("ddpi_udc_processtimeslice returned %d\n", sub_err);   
        }

        pExt->frameLengthSamplesPerChannel = p_udcexec->ptop.nblkspcm * FIO_BLKSIZE;
        pExt->samplingRate = p_udcexec->ptop.pcmsamplerate;
        pExt->pEvoFrameData = NULL;
        pExt->evoFrameSize = 0;
        pExt->outChannelMap = p_udcexec->ptop.outputmap[0];
        pExt->isJocOutput = (p_udcexec->ptop.jocd_out_mode == DDPI_UDC_JOCD_OBJ_OUT);
        ALOGV("%s outchanmap %d isJocOutput %d", __FUNCTION__, p_udcexec->ptop.outputmap[0], pExt->isJocOutput);

        if(jocd_mode)
        {
            pExt->nrChans = p_udcexec->ptop.jocd_active_channels;
            
            if (p_udcexec->ptop.jocd_out_mode == DDPI_UDC_JOCD_OBJ_OUT)
            {
                int evo_err = DDPI_UDC_ERR_NO_ERROR;
                
                if (p_udcexec->ptop.evo_status_substream[DDPI_UDC_EVOLUTION_OUT_MAIN_INDEP] == DDPI_UDC_EVO_STATUS_VALID)
                {
                    evo_err = ddpi_udc_get_evolution_mdat(p_udcexec->p_dechdl, DDPI_UDC_EVOLUTION_OUT_MAIN_INDEP, &p_udcexec->evolution_mdat);
                }
                else if (p_udcexec->ptop.evo_status_substream[DDPI_UDC_EVOLUTION_OUT_MAIN_DEP] == DDPI_UDC_EVO_STATUS_VALID)
                {
                    evo_err = ddpi_udc_get_evolution_mdat(p_udcexec->p_dechdl, DDPI_UDC_EVOLUTION_OUT_MAIN_DEP, &p_udcexec->evolution_mdat);
                }

                if( evo_err == DDPI_UDC_ERR_NO_ERROR)
                {
                    pExt->pEvoFrameData = p_udcexec->evolution_mdat.evo_serialized_frame.data;
                    pExt->evoFrameSize = p_udcexec->evolution_mdat.evo_serialized_frame.size;
                    ALOGV("%s evoFrameSize %d", __FUNCTION__, pExt->evoFrameSize);
                }                
            }
        }
        else
        {
            /* Do additional processing and write output buffer */
            err = processudcoutput(p_udcexec, p_dst);

            /* Clear the PCM output buffer */
            p_outbuf_align = (char *) ((((long) p_outbuf) +(DDPI_UDC_MIN_MEMORY_ALIGNMENT-1)) & ~(DDPI_UDC_MIN_MEMORY_ALIGNMENT-1));
            memset(p_outbuf_align, 0, p_udcexec->query_mem_op.outputbuffersize);

            pExt->nrChans = p_udcexec->execparams.outnchans[DDPI_UDC_PCMOUT_MAIN];

            if (err)
            {
                memset(p_dst, 0, pExt->nrChans * pExt->frameLengthSamplesPerChannel * GBL_BYTESPERWRD);
            }
        }

        ALOGV("ddpdec_process: one frame processed");
        return DDPAUDEC_SUCCESS;
    }
    else
    {
        ALOGV("ddpdec_process: incomplete frame");
        return DDPAUDEC_INCOMPLETE_FRAME;
    }
}
