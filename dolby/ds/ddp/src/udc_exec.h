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

#ifndef UDC_EXEC_H
#define UDC_EXEC_H

/* subroutine headers */
#include "udc_api.h"

/* executive headers */
#include "udc_user.h"
#include "udc_fio.h"

/*! \brief Buffer size used internally for file reading */
#define DDPIN_BUFFER_SIZE 2048

/*! \brief Manufacturer_ID used for checking license */
#define MANUFACTURER_ID (1)

/**** Module Structures ****/
typedef struct
{
    /* user_specified parameters */
    EXECPARAMS execparams;                              /* user-specified executive parameters     */
    SUBPARAMS subparams[DDPI_UDC_PCMOUT_COUNT];         /* user-specified subroutine parameters    */

    /* Output buffers */
    dlb_buffer a_outputbuf[DDPI_UDC_PCMOUT_COUNT];      /* array of output buffers for PCM data     */
	dlb_buffer jocd_outputbuf;                          /* output buffers for JOC PCM data     */
    dlb_buffer ddoutbuf;                                /* DD output buffer                         */

    /* subroutine handlers */
    void *p_dechdl;                         /* subroutine memory handle                */
    ddpi_udc_query_op      query_op;        /* data returned from subroutine query     */
    ddpi_udc_query_mem_op  query_mem_op;    /* data returned from subroutine query_mem */
    ddpi_udc_pt_op ptop;
	ddpi_udc_evolution_data evolution_mdat;     /* evolution data output buffer   */

    /* Local variables used in main */
    int nblkspcm;                           /* number of blocks of PCM to write out    */
    int samplerate;                         /* local variable in main: sample rate to write out */
    int streamcount;

} UDCEXEC;


#endif /* UDC_EXEC_H */
