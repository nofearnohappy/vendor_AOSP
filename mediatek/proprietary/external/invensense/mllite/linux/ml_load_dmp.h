/*
 $License:
    Copyright (C) 2014 InvenSense Corporation, All Rights Reserved.
 $
 */

/*******************************************************************************
 *
 * $Id:$
 *
 ******************************************************************************/

#ifndef INV_LOAD_DMP_H
#define INV_LOAD_DMP_H

#ifdef __cplusplus
extern "C" {
#endif

/*
    Includes.
*/
#include "mltypes.h"

/*
    APIs
*/
int inv_write_dmp(FILE *fd, const unsigned char *dmp, size_t len);
int inv_load_dmp_lib(FILE *fd);
int inv_load_dmp(char *dmp_syfs_path, const char *dmp_firmware_path);
void read_dmp_img(char *dmp_path, char *out_file);

#ifdef __cplusplus
}
#endif
#endif  /* INV_LOAD_DMP_H */
