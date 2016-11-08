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

#define LOG_TAG "evo_parser"
#include "DlbLog.h"
#include "evo_parser.h"
#include "dlb_bitbuf/include/dlb_bitbuf_read.h"

#define OAMD_PAYLOAD_ID             (11)

int variable_bitbuf_safe_read(dlb_bitbuf *bitbuf, unsigned int n, unsigned int *pData)
{
    unsigned int err        = -1;
    unsigned int data       = 0;
    unsigned int value      = 0;
    unsigned int read_more  = 0;

    do
    {
        if (dlb_bitbuf_safe_read(bitbuf, n, &data))
        {
            return err;
        }
        value += data;
        /* read more */
        if (dlb_bitbuf_safe_read(bitbuf, 1, &read_more))
        {
            return err;
        }        
        if (read_more)
        {
            value <<= n;
            value += (1 << n);
        }
    } while (read_more);

    *pData = value;
    return 0;
}

int parse_payload_config(dlb_bitbuf *bitbuf, unsigned int *offsetValue)
{
    int     err                = -1;
    unsigned data                = 0;
    unsigned timestamp_present    = 0;
    unsigned now_or_never        = 0;

    /* timestamp_present */
    if (dlb_bitbuf_safe_read(bitbuf, 1, &timestamp_present))
    {
        return err;
    }
    if (timestamp_present)
    {
        if (variable_bitbuf_safe_read(bitbuf, 11, &data))
        {
            return err;
        }
        *offsetValue = data;
    }

    /* duration_present */
    if (dlb_bitbuf_safe_read(bitbuf, 1, &data))
    {
        return err;
    }
    if (data)
    {
        if (variable_bitbuf_safe_read(bitbuf, 11, &data))
        {
            return err;
        }
    }

    /* group_id_present */
    if (dlb_bitbuf_safe_read(bitbuf, 1, &data))
    {
        return err;
    }
    if (data)
    {
        if (variable_bitbuf_safe_read(bitbuf, 2, &data))
        {
            return err;
        }
    }

    /* codec_specific_id_present */
    if (dlb_bitbuf_safe_read(bitbuf, 1, &data))
    {
        return err;
    }
    if (data)
    {
        if (dlb_bitbuf_safe_read(bitbuf, 8, &data))
        {
            return err;
        }
    }

    /* dont_transcode */
    if (dlb_bitbuf_safe_read(bitbuf, 1, &data))
    {
        return err;
    }
    if (!data)
    {
        if (!timestamp_present)
        {
            if (dlb_bitbuf_safe_read(bitbuf, 1, &now_or_never))
            {
                return err;
            }
            if (now_or_never)
            {
                /* create_duplicate */
                if (dlb_bitbuf_safe_read(bitbuf, 1, &data))
                {
                    return err;
                }
                /* remove_duplicate */
                if (dlb_bitbuf_safe_read(bitbuf, 1, &data))
                {
                    return err;
                }
            }
        }
        if (timestamp_present || now_or_never)
        {
            /* priority */
            if (dlb_bitbuf_safe_read(bitbuf, 5, &data))
            {
                return err;
            }
            /* tight_coupling */
            if (dlb_bitbuf_safe_read(bitbuf, 2, &data))
            {
                return err;
            }
        }
    }
    /* payload config END */
    return 0;

}

evo_handle* evo_parser_init()
{
    ALOGI("%s", __FUNCTION__);
    evo_handle *hndl = (evo_handle *) malloc (sizeof(evo_handle));
    if (hndl != NULL)
    {
        hndl->oamdPdSize = 0;
        hndl->oamdPdData = NULL;
    }
    return hndl;
}

void evo_parser_close(evo_handle *hndl)
{
    ALOGI("%s", __FUNCTION__);
    if (hndl != NULL)
    {
        if (hndl->oamdPdData != NULL)
        {
            free (hndl->oamdPdData);
        }
        free (hndl);
    }   
}

int get_oamd_pd_from_evo(evo_handle *hndl, void *evo_frame_data, unsigned int evo_frame_size, 
                            unsigned int *mdOffset)

{
    dlb_bitbuf bitbuf;
    unsigned err                = -1;
    unsigned data               = 0;
    unsigned evo_ver            = 0;
    unsigned ext_bits           = 0;
    unsigned key_id             = 0;
    unsigned payload_id         = 0;
    unsigned payload_size       = 0;
    unsigned offsetValue        = 0;

    if (hndl == NULL)
    {
        ALOGE("%s  line = %d", __FUNCTION__, __LINE__);
        return err;
    }
    
    /* Initialize bit buffer */
    dlb_bitbuf_init(&bitbuf, (unsigned char *)evo_frame_data, (unsigned long)(evo_frame_size << 3));

    /* Read md version */
    if (dlb_bitbuf_safe_read(&bitbuf, 2, &data))
    {
        ALOGE("%s line = %d evo_frame_size %d", __FUNCTION__, __LINE__, evo_frame_size);
        return err;
    }
    if (data == 0x3)
    {
        if (variable_bitbuf_safe_read(&bitbuf, 2, &ext_bits))
        {
            ALOGE("%s  line = %d", __FUNCTION__, __LINE__);
            return err;
        }
        
        data += ext_bits;
    }
    evo_ver = data;
    ALOGV("%s evo_version %d", __FUNCTION__, evo_ver);

    /* Read key id */
    if (dlb_bitbuf_safe_read(&bitbuf, 3, &data))
    {
        ALOGE("%s  line = %d", __FUNCTION__, __LINE__);
        return err;
    }
    if (data == 0x7)
    {
        if (variable_bitbuf_safe_read(&bitbuf, 3, &ext_bits))
        {
            ALOGE("%s  line = %d", __FUNCTION__, __LINE__);
            return err;
        }
        data += ext_bits;
    }
    key_id = data;
    ALOGV("%s key_id %d", __FUNCTION__, key_id);

    if (dlb_bitbuf_safe_read(&bitbuf, 5, &payload_id))
    {
        ALOGE("%s  line = %d", __FUNCTION__, __LINE__);
        return err;
    }

    ALOGV("%s payload_id %d", __FUNCTION__, payload_id);
    while (payload_id != 0)
    {
        if (payload_id == 31)
        {
            if (variable_bitbuf_safe_read(&bitbuf, 5, &data))
            {
                ALOGE("%s  line = %d", __FUNCTION__, __LINE__);
                return err;
            }
            payload_id += data;
        }
        ALOGV("%s payload_id %d", __FUNCTION__, payload_id);

        /* payload config */
        offsetValue = 0;
        if (parse_payload_config(&bitbuf, &offsetValue) != 0)
        {
            ALOGE("%s  line = %d", __FUNCTION__, __LINE__);
            return err;
        }

        /* payload_size */
        if (variable_bitbuf_safe_read(&bitbuf, 8, &payload_size))
        {
            ALOGE("%s  line = %d", __FUNCTION__, __LINE__);
            return err;
        }
        ALOGV("%s payload_size %d", __FUNCTION__, payload_size);

        if (payload_id == OAMD_PAYLOAD_ID)
        {
            //sanity check
            if (payload_size > hndl->oamdPdSize)
            {
                // re-allocate memory
                ALOGI("%s reallocating mem %d", __FUNCTION__, payload_size);
                unsigned char *allocPtr =  (unsigned char *) realloc (hndl->oamdPdData, payload_size);
                if (allocPtr == NULL)
                {
                    ALOGE("%s error in re-allocating memory", __FUNCTION__);
                    free (hndl->oamdPdData);
                    hndl->oamdPdSize = 0;
                    ALOGE("%s  line = %d", __FUNCTION__, __LINE__);
                    return err;
                }
                hndl->oamdPdData = allocPtr;
                hndl->oamdPdSize = payload_size;
            }

            unsigned int i = 0;
            for (i = 0; i < payload_size; ++i)
            {
                if (dlb_bitbuf_safe_read(&bitbuf, 8, &data))
                {
                    ALOGE("%s  line = %d", __FUNCTION__, __LINE__);
                    return err;
                }
                hndl->oamdPdData[i] = data;
            }
            *mdOffset = offsetValue;
            return 0;
        }
        else
        {

            if (dlb_bitbuf_skip(&bitbuf, payload_size * DLB_BITBUF_WIDTH))
            {
                ALOGE("%s  line = %d", __FUNCTION__, __LINE__);
                return err;
            }
        }

        if (dlb_bitbuf_safe_read(&bitbuf, 5, &payload_id))
        {
            ALOGE("%s  line = %d", __FUNCTION__, __LINE__);
            return err;
        }
    }
    ALOGI("%s payload parsing ended and OAMD not found", __FUNCTION__);
    return err; // OAMD payload not in evo_frame
}

