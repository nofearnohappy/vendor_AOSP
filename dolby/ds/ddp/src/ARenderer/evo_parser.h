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


#ifndef _EVO_PARSER_H_
#define _EVO_PARSER_H_


#ifdef __cplusplus
extern "C"
{
#endif

typedef struct evo_parser_handle
{
	unsigned int mdOffset;
	unsigned int oamdPdSize;
	unsigned char *oamdPdData;
}evo_handle;


evo_handle* evo_parser_init();
void evo_parser_close(evo_handle *hndl);

int get_oamd_pd_from_evo(evo_handle *hndl, void *evo_frame_data, unsigned int evo_frame_size,
						unsigned int *mdOffset);


#ifdef __cplusplus
}
#endif
#endif // _EVO_PARSER_H_
