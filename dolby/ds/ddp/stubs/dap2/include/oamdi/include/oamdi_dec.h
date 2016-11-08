/******************************************************************************
 * This program is protected under international and U.S. copyright laws as
 * an unpublished work. This program is confidential and proprietary to the
 * copyright owners. Reproduction or disclosure, in whole or in part, or the
 * production of derivative works therefrom without the express permission of
 * the copyright owners is prohibited.
 *
 *                Copyright (C) 2013-2014 by Dolby International AB.
 *                            All rights reserved.
 *
 *    Module:     Object Audio Metadata Interface (OAMDI)
 *
 *    File:       oamdi_dec.h
 ******************************************************************************/

/** @addtogroup OAMDI_API */
/*@{*/
/**
 * @file   oamdi_dec.h
 * @brief  Object Audio Metadata Interface (OAMDI) API
 */

#ifndef OAMDI_DEC_H
#define OAMDI_DEC_H

#include <stddef.h> /* size_t */
#include "oamdi/include/oamdi.h"

#ifdef __cplusplus
extern "C"
{
#endif

/*********************************/
/* Deserialization */
/*********************************/

/** 
 * @brief Get minimum OAMDI init configuration needed to deserialize given bitstream. 
 * @return 0 if successful, otherwise the bitstream couldn't be parsed.
 */
int
oamdi_get_init_info_from_bitstream
    (const unsigned char *p_bs                /**< [in]  Valid OAMDI bitstream. */
    ,      size_t         bs_size             /**< [in]  Size of the bitstream. */
    ,      unsigned      *p_num_objs          /**< [out] Number of objects in the bitstream. */ 
    ,      unsigned      *p_num_obj_info_blks /**< [out] Number of object info blocks per object. */
    );

/** 
 * @brief Get version of the OAMD in the bitstream. 
 * @return 0 if successful, otherwise the bitstream couldn't be parsed.
 */
int
oamdi_get_oamd_ver_from_bitstream
    (const unsigned char *p_bs        /**< [in]  Valid OAMDI bitstream. */
    ,      size_t         bs_size     /**< [in]  Size of the bitstream. */
    ,      unsigned      *p_oamd_ver  /**< [out] OAMD version. */ 
    );

/** 
 * @brief Deserialize metadata from a bitstream. 
 * @return 0 if successful, otherwise the bitstream couldn't be parsed.
 */
int
oamdi_from_bitstream
    (      oamdi         *p_oamdi  /**< [out] Must be already instantiated with large enough maximums */
    ,      size_t         bs_size  /**< [in]  Size of the passed bitstream buffer (in bytes). */ 
    ,const unsigned char *p_bs     /**< [in]  Valid OAMDI bitstream. */
    );

#ifdef __cplusplus
}
#endif
#endif /* OAMDI_DEC_H */
/*@}*/
