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

#ifndef UDC_FIO_H
#define UDC_FIO_H

/**** Module Defines ****/
#define LFE2_IDX_UNDEF      (-1)
#define ROUTING_LFE         5
#define ROUTING_X1          6
#define OUTPUTMAP_RSVD      13
#define OUTPUTMAP_LFE2      14
#define OUTPUTMAP_LFE       15
#define NO_CHAN             16
#define MAXNUMOUTPUTCONFIGS 17
#define MAXNUMSTRINGS       2
#define MAXOUTPUTCHARS      16
#define MAXOUTPUTSTRCHARS   1024
#define FIO_MAXFCHANS       5
#define TEXTBUFLEN			256

#define FIO_MAXPCMCHANS			(16)

#define FIO_BLKSIZE             256

/* define input/output word size types */
typedef enum
{
	FIO_PCMINT16 = 0,
	FIO_PCMINT24 = 24,
    FIO_PCMINT32 = 32
} FIO_PCMWORDTYPE;


#endif /* UDC_FIO_H */
