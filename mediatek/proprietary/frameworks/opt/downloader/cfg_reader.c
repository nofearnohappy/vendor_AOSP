/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>
#include "cfg_reader.h"
#include "interface.h"
#if defined(__GNUC__)
#include "errno.h"
#include "GCC_Utility.h"
#endif

static void discardComment(char *string, unsigned int length)
{
    unsigned int i = 0;

    //log_output("Debug : input string = (%s)\n", string);

    for(i = 0 ; i < length && i < strlen(string) ; i++)
    {
        if(string[i] == '#')
        {
            // discard all the string after '#'
            string[i] = '\0';
            break;
        }
    }

    //log_output("Debug : output string = (%s)\n", string);
}

static char *GetBaseDirectory(const char * src_path, char * des_path, unsigned int des_len)
{
    int i = 0 ;

    if(strlen(src_path) > 256 || strlen(src_path) > des_len)
    {   // length checking
        log_output("ERROR : GetBaseDirectory(), Wired path string length (%s)\n", strlen(src_path));
    }

    for(i = strlen(src_path); i >= 0 ; i--)
    {
        if(*(src_path + i) == '/' || *(src_path + i) == '\\' )
        {
            strncpy(des_path, src_path, i+1);
            //log_output("GetBaseDirectory(): Output path (%s)\n", des_path);
            return des_path;
        }
    }

    return NULL; // not path format string
}



int GetNormalRegionImagesNameFromCFG(FILE *cfg_fp, const char *cfg_directory_path, const char* p_region_name, RegionSetting *p_BootRegion)
{
    char line[256];
    char *line_ptr= NULL;
    char *cptr= NULL;
    int bInRegion = 0; // flag
    unsigned int strLen = 0;
    fseek(cfg_fp, 0L, SEEK_SET);

    while(!feof(cfg_fp))
    {
        // get line from file
        memset(line, '\0', sizeof(line));
        line_ptr = fgets(line, sizeof(line), cfg_fp);
        if(line_ptr == NULL)
        {
            // EOF
            log_output("GetBootRegionImagesNameFromCFG() EOF\n");
            break;
        }
        if(ferror(cfg_fp))
        {
            log_output("ERROR : GetBootRegionImagesNameFromCFG() read cfg file error\n");
            return S_FTHND_FILE_LOAD_FAIL;
        }

        //log_output("GetBootRegionImagesNameFromCFG() Line 1(len = %d)= [%s]\n",strlen(line_ptr),line_ptr);

        // discard the string after '#', it's comment.
        discardComment(line_ptr, sizeof(line));
        // discard '\n', '\r' at the end of the string
        strLen = strlen(line_ptr);
        if(strLen >= 2)
        {
            if(line_ptr[strLen-1] == '\n' || line_ptr[strLen-1] == '\r')
            {
                line_ptr[strLen-1] = '\0';
            }
            if(line_ptr[strLen-2] == '\n' || line_ptr[strLen-2] == '\r')
            {
                line_ptr[strLen-2] = '\0';
            }
        }

        //log_output("GetBootRegionImagesNameFromCFG() Line (len = %d)= [%s]\n",strlen(line_ptr),line_ptr);

        // search p_region_name string to enter boot region scope
        if(0 == strncmp(line_ptr, p_region_name, strlen(p_region_name)))
        {
            bInRegion = 1; // setup flag (in search scope)
            continue;
        }

        if(bInRegion == 1)
        {
            // search "- file:" to get Name
            cptr = strstr(line_ptr, "- file: ");
            if(cptr != NULL)
            {
                // get image name (the string after "- file: ")
                log_output("GetBootRegionImagesNameFromCFG() Get rom name : [%s] (len = %d)\n", cptr+strlen("- file: "), strlen(cptr+strlen("- file: ")));
                cptr += strlen("- file: ");

                // memory allocation
                //log_output("GetBootRegionImagesNameFromCFG() allocate memory size = %d.\n", strlen(cfg_directory_path) + strlen(cptr) + 1); // +1 : '\0'
                p_BootRegion->romFiles[p_BootRegion->romNumber] = malloc(strlen(cfg_directory_path) + strlen(cptr) + 1); // +1 : '\0'
                if(p_BootRegion->romFiles[p_BootRegion->romNumber] == NULL)
                {
                    log_output("ERROR : GetBootRegionImagesNameFromCFG() : Memory allocation failed.\n");
                }
                else
                {
                    memset(p_BootRegion->romFiles[p_BootRegion->romNumber], 0 , strlen(cfg_directory_path) + strlen(cptr) + 1);
                    strncpy(p_BootRegion->romFiles[p_BootRegion->romNumber], cfg_directory_path, strlen(cfg_directory_path));
                    strncat(p_BootRegion->romFiles[p_BootRegion->romNumber], cptr, strlen(cptr));
                    p_BootRegion->romNumber ++;
                }
            }
            else
            {
                // search "*_region" to exit parsing scope
                cptr = strstr(line_ptr, "_region");
                if(cptr != NULL)
                {
                    // end of parsing
                    //log_output("GetBootRegionImagesNameFromCFG() End of region scope.\n");
                    break;
                }
            }
        }
    }

    // error detect
    if(bInRegion != 1)
    {
        log_output("ERROR : GetBootRegionImagesNameFromCFG() : Region not found.\n");
        return S_FT_INVALID_CFG_FILE;
    }

    return S_DONE;
}


int GetLinuxRegionImagesNameFromCFG()
{
    return S_DONE;
}

//
// Ret = 1, means it found valid info in this line.
//
int GetFlashInfoByString(char *line_ptr, FlashInfo * flashInfo, int flashInfo_index)
{
    char *cptr= NULL;
    char *tokenptr= NULL;
    unsigned int i = 0;

    //
    // start to parse each items to flash info
    //

    // search "flash_type:" to setup flashInfo.flashType
    cptr = strstr(line_ptr, "flash_type: ");
    if(cptr != NULL)
    {
        //cptr += strlen("flash_type: ");
        if(NULL != strstr(cptr, "SF"))
        {
            //log_output("GetFlashInfoByString() Flash Type = SF\n");
            flashInfo->flashType = FLASHType_SERIAL_NOR_FLASH;
        }
        else if(NULL != strstr(cptr, "NAND"))
        {
            //log_output("GetFlashInfoByString() Flash Type = NAND\n");
            flashInfo->flashType = FLASHType_NAND;
        }
        else if(NULL != strstr(cptr, "NOR"))
        {
            //log_output("GetFlashInfoByString() Flash Type = NOR\n");
            flashInfo->flashType = FLASHType_NOR;
        }
        return 1; // found info
    }

    // search "id_length: " to setup flashInfo.u.v01[flashInfo_index].NOR_ID.idNumber
    cptr = strstr(line_ptr, "id_length: ");
    if(cptr != NULL)
    {
        //log_output("GetFlashInfoByString()  flash id length = %d \n", *(cptr+strlen("id_length: ")) - '0');
        flashInfo->u.v01[flashInfo_index].NOR_ID.idNumber = *(cptr+strlen("id_length: ")) - '0';
        // check number is valid
        if(flashInfo->u.v01[flashInfo_index].NOR_ID.idNumber <= 0 && flashInfo->u.v01[flashInfo_index].NOR_ID.idNumber > 8)
        {
            log_output("ERROR : GetFlashInfoByString()  flash id length is invalid \n");
            return 1;
        }
        return 1; // found info
    }

    // search "flash_id: " to setup flashInfo.u.v01[flashInfo_index].NOR_ID.id
    cptr = strstr(line_ptr, "flash_id: ");
    if(cptr != NULL)
    {
        cptr += strlen("flash_id: ");
        tokenptr = strtok(cptr, " [],");
        for(i = 0 ; i < flashInfo->u.v01[flashInfo_index].NOR_ID.idNumber ; i ++)
        {
            if(tokenptr != NULL)
            {
                flashInfo->u.v01[flashInfo_index].NOR_ID.id[i] = (unsigned char)strtoul(tokenptr, NULL, 16);
                //log_output("GetFlashInfoByString()  flash id %d = %d (0x%02x) \n", i, flashInfo->u.v01[flashInfo_index].NOR_ID.id[i], flashInfo->u.v01[flashInfo_index].NOR_ID.id[i]);
                tokenptr = strtok(NULL, " [],");
            }
        }
        return 1; // found info
    }

    return 0; // not found any flash info in this line string

}

//
// Ret = 1, means it found valid info in this line.
//
int GetRAMSettingByString(char *line_ptr, ExternalRAMSetting * ramSetting, int ramSetting_index, CFG_Type_Version CFGVersion)
{
    char *cptr= NULL;

    // search "memory_type: "
    cptr = strstr(line_ptr, "memory_type: ");
    if(cptr != NULL)
    {
        if(NULL != strstr(cptr, "DDR_166"))
        {
            //log_output("GetRAMSettingByString() Mem Type = DDR_166MHZ\n");
            ramSetting->ramType = RAMType_DDR_166M;
            if(CFGVersion == CFGType_V1)
                ramSetting->u.v05[ramSetting_index].ramType = DRAMType_DDR_166M;
            else if(CFGVersion == CFGType_V2)
                ramSetting->u.v06[ramSetting_index].ramType = DRAMType_DDR_166M;
        }
        else if(NULL != strstr(cptr, "DDR_200"))
        {
            //log_output("GetRAMSettingByString() Mem Type = DDR_200MHZ\n");
            ramSetting->ramType = RAMType_DDR_200M;
            if(CFGVersion == CFGType_V1)
                ramSetting->u.v05[ramSetting_index].ramType = DRAMType_DDR_200M;
            else if(CFGVersion == CFGType_V2)
                ramSetting->u.v06[ramSetting_index].ramType = DRAMType_DDR_200M;
        }
        else if(NULL != strstr(cptr, "DDR2_166"))
        {
            //log_output("GetRAMSettingByString() Mem Type = DDR2_166MHZ\n");
            ramSetting->ramType = RAMType_DDR2_166M;
            if(CFGVersion == CFGType_V1)
                ramSetting->u.v05[ramSetting_index].ramType = DRAMType_DDR2_166M;
            else if(CFGVersion == CFGType_V2)
                ramSetting->u.v06[ramSetting_index].ramType = DRAMType_DDR2_166M;
        }
        else if(NULL != strstr(cptr, "DDR2_200"))
        {
            //log_output("GetRAMSettingByString() Mem Type = DDR2_200MHZ\n");
            ramSetting->ramType = RAMType_DDR2_200M;
            if(CFGVersion == CFGType_V1)
                ramSetting->u.v05[ramSetting_index].ramType = DRAMType_DDR2_200M;
            else if(CFGVersion == CFGType_V2)
                ramSetting->u.v06[ramSetting_index].ramType = DRAMType_DDR2_200M;
        }
        else if(NULL != strstr(cptr, "PSRAM"))
        {
            //log_output("GetRAMSettingByString() Mem Type = PSRAM\n");
            ramSetting->ramType = RAMType_SRAM_Normal;
            if(CFGVersion == CFGType_V1)
                ramSetting->u.v05[ramSetting_index].ramType = DRAMType_Invalid;
            else if(CFGVersion == CFGType_V2)
                ramSetting->u.v06[ramSetting_index].ramType = DRAMType_Invalid;
        }
        return 1; // found info
    }

    //
    // Search EMI setting (support DRAMSetting_v05, DRAMSetting_v06)
    //

    // search "EMI_CONI: "
    cptr = strstr(line_ptr, "EMI_CONI: ");
    if(cptr != NULL)
    {
        cptr += strlen("EMI_CONI: ");
        //log_output("GetRAMSettingByString() EMI_CONI = 0x%08x\n", (unsigned int)strtoul(cptr, NULL, 16));
        if(CFGVersion == CFGType_V1)
            ramSetting->u.v05[ramSetting_index].EMI_CONI_Value = (unsigned int)strtoul(cptr, NULL, 16);
        else if(CFGVersion == CFGType_V2)
            ramSetting->u.v06[ramSetting_index].EMI_CONI_Value = (unsigned int)strtoul(cptr, NULL, 16);
        else
        {
            // unkonwn EMI item for this cfg version, bypass it
        }
        return 1; // found info
    }

    // search "EMI_CONJ: "
    cptr = strstr(line_ptr, "EMI_CONJ: ");
    if(cptr != NULL)
    {
        cptr += strlen("EMI_CONJ: ");
        //log_output("GetRAMSettingByString() EMI_CONJ = 0x%08x\n", (unsigned int)strtoul(cptr, NULL, 16));
        if(CFGVersion == CFGType_V1)
            ramSetting->u.v05[ramSetting_index].EMI_CONJ_Value = (unsigned int)strtoul(cptr, NULL, 16);
        else if(CFGVersion == CFGType_V2)
            ramSetting->u.v06[ramSetting_index].EMI_CONJ_Value = (unsigned int)strtoul(cptr, NULL, 16);
        else
        {
            // unkonwn EMI item for this cfg version, bypass it
        }
        return 1; // found info
    }

    // search "EMI_CONK: "
    cptr = strstr(line_ptr, "EMI_CONK: ");
    if(cptr != NULL)
    {
        cptr += strlen("EMI_CONK: ");
        //log_output("GetRAMSettingByString() EMI_CONK = 0x%08x\n", (unsigned int)strtoul(cptr, NULL, 16));
        if(CFGVersion == CFGType_V1)
            ramSetting->u.v05[ramSetting_index].EMI_CONK_Value = (unsigned int)strtoul(cptr, NULL, 16);
        else if(CFGVersion == CFGType_V2)
            ramSetting->u.v06[ramSetting_index].EMI_CONK_Value = (unsigned int)strtoul(cptr, NULL, 16);
        else
        {
            // unkonwn EMI item for this cfg version, bypass it
        }
        return 1; // found info
    }

    // search "EMI_CONL: "
    cptr = strstr(line_ptr, "EMI_CONL: ");
    if(cptr != NULL)
    {
        cptr += strlen("EMI_CONL: ");
        //log_output("GetRAMSettingByString() EMI_CONL = 0x%08x\n", (unsigned int)strtoul(cptr, NULL, 16));
        if(CFGVersion == CFGType_V1)
            ramSetting->u.v05[ramSetting_index].EMI_CONL_Value = (unsigned int)strtoul(cptr, NULL, 16);
        else if(CFGVersion == CFGType_V2)
            ramSetting->u.v06[ramSetting_index].EMI_CONL_Value = (unsigned int)strtoul(cptr, NULL, 16);
        else
        {
            // unkonwn EMI item for this cfg version, bypass it
        }
        return 1; // found info
    }

    // search "EMI_CONN: "
    cptr = strstr(line_ptr, "EMI_CONN: ");
    if(cptr != NULL)
    {
        cptr += strlen("EMI_CONN: ");
        //log_output("GetRAMSettingByString() EMI_CONN = 0x%08x\n", (unsigned int)strtoul(cptr, NULL, 16));
        if(CFGVersion == CFGType_V1)
            ramSetting->u.v05[ramSetting_index].EMI_CONN_Value = (unsigned int)strtoul(cptr, NULL, 16);
        else if(CFGVersion == CFGType_V2)
            ramSetting->u.v06[ramSetting_index].EMI_CONN_Value = (unsigned int)strtoul(cptr, NULL, 16);
        else
        {
            // unkonwn EMI item for this cfg version, bypass it
        }
        return 1; // found info
    }


    // search "EMI_DRVA: "
    cptr = strstr(line_ptr, "EMI_DRVA: ");
    if(cptr != NULL)
    {
        cptr += strlen("EMI_DRVA: ");
        //log_output("GetRAMSettingByString() EMI_DRVA = 0x%08x\n", (unsigned int)strtoul(cptr, NULL, 16));
        if(CFGVersion == CFGType_V1)
            ramSetting->u.v05[ramSetting_index].EMI_DRVA_Value = (unsigned int)strtoul(cptr, NULL, 16);
        else if(CFGVersion == CFGType_V2)
            ramSetting->u.v06[ramSetting_index].EMI_DRVA_Value = (unsigned int)strtoul(cptr, NULL, 16);
        else
        {
            // unkonwn EMI item for this cfg version, bypass it
        }
        return 1; // found info
    }


    // search "EMI_DRVB: "
    cptr = strstr(line_ptr, "EMI_DRVB: ");
    if(cptr != NULL)
    {
        cptr += strlen("EMI_DRVB: ");
        //log_output("GetRAMSettingByString() EMI_DRVB = 0x%08x\n", (unsigned int)strtoul(cptr, NULL, 16));
        if(CFGVersion == CFGType_V1)
            ramSetting->u.v05[ramSetting_index].EMI_DRVB_Value = (unsigned int)strtoul(cptr, NULL, 16);
        else if(CFGVersion == CFGType_V2)
            ramSetting->u.v06[ramSetting_index].EMI_DRVB_Value = (unsigned int)strtoul(cptr, NULL, 16);
        else
        {
            // unkonwn EMI item for this cfg version, bypass it
        }
        return 1; // found info
    }

    // search "EMI_ODLA: "
    cptr = strstr(line_ptr, "EMI_ODLA: ");
    if(cptr != NULL)
    {
        cptr += strlen("EMI_ODLA: ");
        //log_output("GetRAMSettingByString() EMI_ODLA = 0x%08x\n", (unsigned int)strtoul(cptr, NULL, 16));
        if(CFGVersion == CFGType_V1)
            ramSetting->u.v05[ramSetting_index].EMI_ODLA_Value = (unsigned int)strtoul(cptr, NULL, 16);
        else
        {
            // unkonwn EMI item for this cfg version, bypass it
        }
        return 1; // found info
    }

    // search "EMI_ODLB: "
    cptr = strstr(line_ptr, "EMI_ODLB: ");
    if(cptr != NULL)
    {
        cptr += strlen("EMI_ODLB: ");
        //log_output("GetRAMSettingByString() EMI_ODLB = 0x%08x\n", (unsigned int)strtoul(cptr, NULL, 16));
        if(CFGVersion == CFGType_V1)
            ramSetting->u.v05[ramSetting_index].EMI_ODLB_Value = (unsigned int)strtoul(cptr, NULL, 16);
        else
        {
            // unkonwn EMI item for this cfg version, bypass it
        }
        return 1; // found info
    }

    // search "EMI_ODLC: "
    cptr = strstr(line_ptr, "EMI_ODLC: ");
    if(cptr != NULL)
    {
        cptr += strlen("EMI_ODLC: ");
        //log_output("GetRAMSettingByString() EMI_ODLC = 0x%08x\n", (unsigned int)strtoul(cptr, NULL, 16));
        if(CFGVersion == CFGType_V1)
            ramSetting->u.v05[ramSetting_index].EMI_ODLC_Value = (unsigned int)strtoul(cptr, NULL, 16);
        else
        {
            // unkonwn EMI item for this cfg version, bypass it
        }
        return 1; // found info
    }

    // search "EMI_ODLD: "
    cptr = strstr(line_ptr, "EMI_ODLD: ");
    if(cptr != NULL)
    {
        cptr += strlen("EMI_ODLD: ");
        //log_output("GetRAMSettingByString() EMI_ODLD = 0x%08x\n", (unsigned int)strtoul(cptr, NULL, 16));
        if(CFGVersion == CFGType_V1)
            ramSetting->u.v05[ramSetting_index].EMI_ODLD_Value = (unsigned int)strtoul(cptr, NULL, 16);
        else
        {
            // unkonwn EMI item for this cfg version, bypass it
        }
        return 1; // found info
    }


    // search "EMI_ODLE: "
    cptr = strstr(line_ptr, "EMI_ODLE: ");
    if(cptr != NULL)
    {
        cptr += strlen("EMI_ODLE: ");
        //log_output("GetRAMSettingByString() EMI_ODLE = 0x%08x\n", (unsigned int)strtoul(cptr, NULL, 16));
        if(CFGVersion == CFGType_V1)
            ramSetting->u.v05[ramSetting_index].EMI_ODLE_Value = (unsigned int)strtoul(cptr, NULL, 16);
        else if(CFGVersion == CFGType_V2)
            ramSetting->u.v06[ramSetting_index].EMI_ODLE_Value = (unsigned int)strtoul(cptr, NULL, 16);
        else
        {
            // unkonwn EMI item for this cfg version, bypass it
        }
        return 1; // found info
    }


    // search "EMI_ODLF: "
    cptr = strstr(line_ptr, "EMI_ODLF: ");
    if(cptr != NULL)
    {
        cptr += strlen("EMI_ODLF: ");
        //log_output("GetRAMSettingByString() EMI_ODLF = 0x%08x\n", (unsigned int)strtoul(cptr, NULL, 16));
        if(CFGVersion == CFGType_V1)
            ramSetting->u.v05[ramSetting_index].EMI_ODLF_Value = (unsigned int)strtoul(cptr, NULL, 16);
        else if(CFGVersion == CFGType_V2)
            ramSetting->u.v06[ramSetting_index].EMI_ODLF_Value = (unsigned int)strtoul(cptr, NULL, 16);
        else
        {
            // unkonwn EMI item for this cfg version, bypass it
        }
        return 1; // found info
    }


    // search "EMI_ODLG: "
    cptr = strstr(line_ptr, "EMI_ODLG: ");
    if(cptr != NULL)
    {
        cptr += strlen("EMI_ODLG: ");
        //log_output("GetRAMSettingByString() EMI_ODLG = 0x%08x\n", (unsigned int)strtoul(cptr, NULL, 16));
        if(CFGVersion == CFGType_V1)
            ramSetting->u.v05[ramSetting_index].EMI_ODLG_Value = (unsigned int)strtoul(cptr, NULL, 16);
        else if(CFGVersion == CFGType_V2)
            ramSetting->u.v06[ramSetting_index].EMI_ODLG_Value = (unsigned int)strtoul(cptr, NULL, 16);
        else
        {
            // unkonwn EMI item for this cfg version, bypass it
        }
        return 1; // found info
    }


    // search "EMI_ODLH: "
    cptr = strstr(line_ptr, "EMI_ODLH: ");
    if(cptr != NULL)
    {
        cptr += strlen("EMI_ODLH: ");
        //log_output("GetRAMSettingByString() EMI_ODLH = 0x%08x\n", (unsigned int)strtoul(cptr, NULL, 16));
        if(CFGVersion == CFGType_V2)
            ramSetting->u.v06[ramSetting_index].EMI_ODLH_Value = (unsigned int)strtoul(cptr, NULL, 16);
        else
        {
            // unkonwn EMI item for this cfg version, bypass it
        }
        return 1; // found info
    }


    // search "EMI_ODLI: "
    cptr = strstr(line_ptr, "EMI_ODLI: ");
    if(cptr != NULL)
    {
        cptr += strlen("EMI_ODLI: ");
        //log_output("GetRAMSettingByString() EMI_ODLI = 0x%08x\n", (unsigned int)strtoul(cptr, NULL, 16));
        if(CFGVersion == CFGType_V2)
            ramSetting->u.v06[ramSetting_index].EMI_ODLI_Value = (unsigned int)strtoul(cptr, NULL, 16);
        else
        {
            // unkonwn EMI item for this cfg version, bypass it
        }
        return 1; // found info
    }


    // search "EMI_ODLJ: "
    cptr = strstr(line_ptr, "EMI_ODLJ: ");
    if(cptr != NULL)
    {
        cptr += strlen("EMI_ODLJ: ");
        //log_output("GetRAMSettingByString() EMI_ODLJ = 0x%08x\n", (unsigned int)strtoul(cptr, NULL, 16));
        if(CFGVersion == CFGType_V1)
            ramSetting->u.v05[ramSetting_index].EMI_CONJ_Value = (unsigned int)strtoul(cptr, NULL, 16);
        else if(CFGVersion == CFGType_V2)
            ramSetting->u.v06[ramSetting_index].EMI_CONJ_Value = (unsigned int)strtoul(cptr, NULL, 16);
        else
        {
            // unkonwn EMI item for this cfg version, bypass it
        }
        return 1; // found info
    }


    // search "EMI_ODLK: "
    cptr = strstr(line_ptr, "EMI_ODLK: ");
    if(cptr != NULL)
    {
        cptr += strlen("EMI_ODLK: ");
        //log_output("GetRAMSettingByString() EMI_ODLK = 0x%08x\n", (unsigned int)strtoul(cptr, NULL, 16));
        if(CFGVersion == CFGType_V2)
            ramSetting->u.v06[ramSetting_index].EMI_ODLK_Value = (unsigned int)strtoul(cptr, NULL, 16);
        else
        {
            // unkonwn EMI item for this cfg version, bypass it
        }
        return 1; // found info
    }


    // search "EMI_ODLL: "
    cptr = strstr(line_ptr, "EMI_ODLL: ");
    if(cptr != NULL)
    {
        cptr += strlen("EMI_ODLL: ");
        //log_output("GetRAMSettingByString() EMI_ODLL = 0x%08x\n", (unsigned int)strtoul(cptr, NULL, 16));
        if(CFGVersion == CFGType_V2)
            ramSetting->u.v06[ramSetting_index].EMI_ODLL_Value = (unsigned int)strtoul(cptr, NULL, 16);
        else
        {
            // unkonwn EMI item for this cfg version, bypass it
        }
        return 1; // found info
    }


    // search "EMI_ODLM: "
    cptr = strstr(line_ptr, "EMI_ODLM: ");
    if(cptr != NULL)
    {
        cptr += strlen("EMI_ODLM: ");
        //log_output("GetRAMSettingByString() EMI_ODLM = 0x%08x\n", (unsigned int)strtoul(cptr, NULL, 16));
        if(CFGVersion == CFGType_V2)
            ramSetting->u.v06[ramSetting_index].EMI_ODLM_Value = (unsigned int)strtoul(cptr, NULL, 16);
        else
        {
            // unkonwn EMI item for this cfg version, bypass it
        }
        return 1; // found info
    }


    // search "EMI_ODLN: "
    cptr = strstr(line_ptr, "EMI_ODLN: ");
    if(cptr != NULL)
    {
        cptr += strlen("EMI_ODLN: ");
        //log_output("GetRAMSettingByString() EMI_ODLN = 0x%08x\n", (unsigned int)strtoul(cptr, NULL, 16));
        if(CFGVersion == CFGType_V2)
            ramSetting->u.v06[ramSetting_index].EMI_ODLN_Value = (unsigned int)strtoul(cptr, NULL, 16);
        else
        {
            // unkonwn EMI item for this cfg version, bypass it
        }
        return 1; // found info
    }


    // search "EMI_DUTA: "
    cptr = strstr(line_ptr, "EMI_DUTA: ");
    if(cptr != NULL)
    {
        cptr += strlen("EMI_DUTA: ");
        //log_output("GetRAMSettingByString() EMI_DUTA = 0x%08x\n", (unsigned int)strtoul(cptr, NULL, 16));
        if(CFGVersion == CFGType_V1)
            ramSetting->u.v05[ramSetting_index].EMI_DUTA_Value = (unsigned int)strtoul(cptr, NULL, 16);
        else if(CFGVersion == CFGType_V2)
            ramSetting->u.v06[ramSetting_index].EMI_DUTA_Value = (unsigned int)strtoul(cptr, NULL, 16);
        else
        {
            // unkonwn EMI item for this cfg version, bypass it
        }
        return 1; // found info
    }


    // search "EMI_DUTB: "
    cptr = strstr(line_ptr, "EMI_DUTB: ");
    if(cptr != NULL)
    {
        cptr += strlen("EMI_DUTB: ");
        //log_output("GetRAMSettingByString() EMI_DUTB = 0x%08x\n", (unsigned int)strtoul(cptr, NULL, 16));
        if(CFGVersion == CFGType_V1)
            ramSetting->u.v05[ramSetting_index].EMI_DUTB_Value = (unsigned int)strtoul(cptr, NULL, 16);
        else if(CFGVersion == CFGType_V2)
            ramSetting->u.v06[ramSetting_index].EMI_DUTB_Value = (unsigned int)strtoul(cptr, NULL, 16);
        else
        {
            // unkonwn EMI item for this cfg version, bypass it
        }
        return 1; // found info
    }


    // search "EMI_DUTC: "
    cptr = strstr(line_ptr, "EMI_DUTC: ");
    if(cptr != NULL)
    {
        cptr += strlen("EMI_DUTC: ");
        //log_output("GetRAMSettingByString() EMI_DUTC = 0x%08x\n", (unsigned int)strtoul(cptr, NULL, 16));
        if(CFGVersion == CFGType_V1)
            ramSetting->u.v05[ramSetting_index].EMI_DUTC_Value = (unsigned int)strtoul(cptr, NULL, 16);
        else if(CFGVersion == CFGType_V2)
            ramSetting->u.v06[ramSetting_index].EMI_DUTC_Value = (unsigned int)strtoul(cptr, NULL, 16);
        else
        {
            // unkonwn EMI item for this cfg version, bypass it
        }
        return 1; // found info
    }


    // search "EMI_DUCA: "
    cptr = strstr(line_ptr, "EMI_DUCA: ");
    if(cptr != NULL)
    {
        cptr += strlen("EMI_DUCA: ");
        //log_output("GetRAMSettingByString() EMI_DUCA = 0x%08x\n", (unsigned int)strtoul(cptr, NULL, 16));
        if(CFGVersion == CFGType_V1)
            ramSetting->u.v05[ramSetting_index].EMI_DUCA_Value = (unsigned int)strtoul(cptr, NULL, 16);
        else if(CFGVersion == CFGType_V2)
            ramSetting->u.v06[ramSetting_index].EMI_DUCA_Value = (unsigned int)strtoul(cptr, NULL, 16);
        else
        {
            // unkonwn EMI item for this cfg version, bypass it
        }
        return 1; // found info
    }


    // search "EMI_DUCB: "
    cptr = strstr(line_ptr, "EMI_DUCB: ");
    if(cptr != NULL)
    {
        cptr += strlen("EMI_DUCB: ");
        //log_output("GetRAMSettingByString() EMI_DUCB = 0x%08x\n", (unsigned int)strtoul(cptr, NULL, 16));
        if(CFGVersion == CFGType_V1)
            ramSetting->u.v05[ramSetting_index].EMI_DUCB_Value = (unsigned int)strtoul(cptr, NULL, 16);
        else if(CFGVersion == CFGType_V2)
            ramSetting->u.v06[ramSetting_index].EMI_DUCB_Value = (unsigned int)strtoul(cptr, NULL, 16);
        else
        {
            // unkonwn EMI item for this cfg version, bypass it
        }
        return 1; // found info
    }


    // search "EMI_DUCE: "
    cptr = strstr(line_ptr, "EMI_DUCE: ");
    if(cptr != NULL)
    {
        cptr += strlen("EMI_DUCE: ");
        //log_output("GetRAMSettingByString() EMI_DUCE = 0x%08x\n", (unsigned int)strtoul(cptr, NULL, 16));
        if(CFGVersion == CFGType_V1)
            ramSetting->u.v05[ramSetting_index].EMI_DUCE_Value = (unsigned int)strtoul(cptr, NULL, 16);
        else if(CFGVersion == CFGType_V2)
            ramSetting->u.v06[ramSetting_index].EMI_DUCE_Value = (unsigned int)strtoul(cptr, NULL, 16);
        else
        {
            // unkonwn EMI item for this cfg version, bypass it
        }
        return 1; // found info
    }

    // search "EMI_IOCL: "
    cptr = strstr(line_ptr, "EMI_IOCL: ");
    if(cptr != NULL)
    {
        cptr += strlen("EMI_IOCL: ");
        //log_output("GetRAMSettingByString() EMI_IOCL = 0x%08x\n", (unsigned int)strtoul(cptr, NULL, 16));
        if(CFGVersion == CFGType_V1)
            ramSetting->u.v05[ramSetting_index].EMI_IOCL_Value = (unsigned int)strtoul(cptr, NULL, 16);
        else if(CFGVersion == CFGType_V2)
            ramSetting->u.v06[ramSetting_index].EMI_IOCL_Value = (unsigned int)strtoul(cptr, NULL, 16);
        else
        {
            // unkonwn EMI item for this cfg version, bypass it
        }
        return 1; // found info
    }

    return 0; // not found any ram setting in this line string
}


int GetExternalMemorySettingFromCFG(FILE *cfg_fp, ExternalMemorySetting *externalMemorySetting)
{
    char line[256];
    char *line_ptr= NULL;
    char *cptr= NULL;
    char *tokenptr= NULL;
    int bInRegion = 0; // flag
    int flashInfo_index = -1;  // begin from 0
    unsigned int i = 0;
    CFG_Type_Version cfg_version = CFGType_Invalid;

    fseek(cfg_fp, 0L, SEEK_SET);

    while(!feof(cfg_fp))
    {
        // get line
        memset(line, 0, sizeof(line));
        line_ptr = fgets(line, sizeof(line), cfg_fp);
        if(line_ptr == NULL)
        {
            // EOF
            log_output("GetExternalMemorySettingFromCFG() EOF\n");
            break;
        }
        if(ferror(cfg_fp))
        {
            log_output("GetExternalMemorySettingFromCFG() read cfg file error\n");
            return S_FTHND_FILE_LOAD_FAIL;
        }

        //log_output("GetExternalMemorySettingFromCFG() Line = %s\n",line_ptr);

        // discard the string after '#', it's comment.
        discardComment(line_ptr, sizeof(line));

        // Get version first
        cptr = strstr(line_ptr, "parameters_version: v");
        if(cptr == NULL)
        {
            // error return
        }
        else
        {
            cptr += strlen("parameters_version: v");
            if(cptr[0] == '1')
            {
                cfg_version = CFGType_V1;
                externalMemorySetting->CFGVersion = CFGType_V1;
                externalMemorySetting->ramSetting.version = 5;
            }
            else if(cptr[0] == '2')
            {
                cfg_version = CFGType_V2;
                externalMemorySetting->CFGVersion = CFGType_V2;
                externalMemorySetting->ramSetting.version = 6;
            }
            else if(cptr[0] == '3' && cptr[1] == '.' && cptr[2] == '1')
            {
                cfg_version = CFGType_V3_1;
                externalMemorySetting->CFGVersion = CFGType_V3_1;
                externalMemorySetting->ramSetting.version = 6;
            }
            else if(cptr[0] == '3')
            {
                cfg_version = CFGType_V3;
                externalMemorySetting->CFGVersion = CFGType_V3;
                externalMemorySetting->ramSetting.version = 7;
            }
            else
            {
                // not support CFG version (maybe it's new version)
                log_output("ERROR : GetExternalMemorySettingFromCFG() Unkown cfg version = %s\n", line_ptr);
                return S_FT_INVALID_CFG_FILE;
            }
            externalMemorySetting->flashInfo.version = 1; // current only version 1.

            bInRegion = 1; // after get cfg version, we can start parsing other items.

            continue;
        }

        // search "PMIC: " to setup externalMemorySetting->PMICController
        cptr = strstr(line_ptr, "PMIC: ");
        if(cptr != NULL)
        {
            //cptr += strlen("PMIC: ");
            if(NULL != strstr(cptr, "MT6321"))
            {
                //log_output("GetExternalMemorySettingFromCFG() PMIC Type = PMIC_MT6321\n");
                externalMemorySetting->PMICController = PMIC_MT6321;
            }
            else if(NULL != strstr(cptr, "MT6327"))
            {
                //log_output("GetExternalMemorySettingFromCFG() PMIC Type = PMIC_MT6327\n");
                externalMemorySetting->PMICController = PMIC_MT6327;
            }
            else if(NULL != strstr(cptr, "MT6329"))
            {
                //log_output("GetExternalMemorySettingFromCFG() PMIC Type = PMIC_MT6329\n");
                externalMemorySetting->PMICController = PMIC_MT6329;
            }
            continue;
        }

        if(bInRegion == 1)
        {
            // search "- flash_info:" to count flashInfo_index number
            cptr = strstr(line_ptr, "- flash_info:");
            if(cptr != NULL)
            {
                flashInfo_index ++;
                externalMemorySetting->flashInfo.numValidEntries++;
                externalMemorySetting->ramSetting.numValidEntries++;
                continue;
            }

            if(flashInfo_index >= 0)
            {
                // Parse flash info
                if(1 == GetFlashInfoByString(line_ptr, &(externalMemorySetting->flashInfo), flashInfo_index))
                {
                    // if return == 1, it found info in this line, contine parse next line.
                    continue;
                }

                // Parse  EMI setting
                if(1 == GetRAMSettingByString(line_ptr, &(externalMemorySetting->ramSetting), flashInfo_index, externalMemorySetting->CFGVersion))
                {
                    // if return == 1, it found info in this line, contine parse next line.
                    continue;
                }
            }
        }

    }

    // error detect
    if(bInRegion != 1)
    {
        log_output("ERROR : GetExternalMemorySettingFromCFG() : Region not found.\n");
        return S_FT_INVALID_CFG_FILE;
    }
    else
    {
        externalMemorySetting->valid = _TRUE; // setup this externalMemorySetting is valid
    }

    return S_DONE;
}


int ParseConfigFile(const char* p_cfgpath, CFG_Images_Name *cfgImagesName, ExternalMemorySetting *externalMemorySetting)
{
    FILE *cfg_fp;
    int ret = 0;
    char cfg_folder[256];
    memset(cfg_folder, 0 , sizeof(cfg_folder));

    // check CFG path
    if(p_cfgpath == NULL)
    {
        log_output("Error : CFG path is NULL. Please assign CFG path for parsing.\n");
        return S_INVALID_ARGUMENTS;
    }

    // open CFG file
    cfg_fp = fopen(p_cfgpath, "rb");
    if(cfg_fp == NULL)
    {
        log_output("Error : Open CFG file is failed. Please check CFG path is correct or CFG file exist. errno=%d.\n", errno);
        return S_FT_FILE_NOT_EXIST;
    }

    // Get CFG folder path string
    GetBaseDirectory(p_cfgpath, cfg_folder, sizeof(cfg_folder));

    // parse boot region images name
    ret = GetNormalRegionImagesNameFromCFG(cfg_fp, cfg_folder, "boot_region:", &(cfgImagesName->BootRegion));
    if(ret != S_DONE)
    {
        log_output("Error : Failed to parse boot region images name, GetBootRegionImagesNameFromCFG() ret = %d\n", ret);
        goto error;
    }

    // parse CBR region images name
    ret = GetNormalRegionImagesNameFromCFG(cfg_fp, cfg_folder, "control_block_region:", &(cfgImagesName->ControlBlockRegion));
    if(ret != S_DONE)
    {
        log_output("Error : Failed to parse CBR region images name, GetCBRImagesNameFromCFG() ret = %d\n", ret);
        goto error;
    }

    // parse main region images name
    ret = GetNormalRegionImagesNameFromCFG(cfg_fp, cfg_folder, "main_region:", &(cfgImagesName->MainRegion));
    if(ret != S_DONE)
    {
        log_output("Error : Failed to parse main region images name, GetMainRegionImagesNameFromCFG() ret = %d\n", ret);
        goto error;
    }

    // parse linux parition images name
    /*ret = GetLinuxRegionImagesNameFromCFG();
    if(ret != 0)
    {
        log_output("Error : Failed to parse linux partition images name, GetLinuxParitionImagesNameFromCFG() ret = %d\n", ret);
        goto error;
    }*/

    // parse ExternalMemorySetting
    ret = GetExternalMemorySettingFromCFG(cfg_fp, externalMemorySetting);
    if(ret != S_DONE)
    {
        log_output("Error : Failed to parse ExternalMemorySetting, GetExternalMemorySettingFromCFG() ret = %d\n", ret);
        goto error;
    }

    fclose(cfg_fp);
    log_output("ParseConfigFile():  Done \n");
    return S_DONE;

error:
    fclose(cfg_fp);
    log_output("ParseConfigFile():  Failed, ret = %d \n",ret);
    return ret;
}

int release_cfg_images_name(CFG_Images_Name *cfgImagesName)
{
    unsigned int i = 0;

    for (i = 0 ; i < cfgImagesName->BootRegion.romNumber ; i++)
    {
        // free memory
        log_output("release_cfg_images_name():  free boot region entry %d \n", i);
        if(cfgImagesName->BootRegion.romFiles[i] != NULL)
        {
            log_output("release_cfg_images_name():  free entry %d [%s](%d)\n", i, cfgImagesName->BootRegion.romFiles[i], strlen(cfgImagesName->BootRegion.romFiles[i]));
            free(cfgImagesName->BootRegion.romFiles[i]);
            cfgImagesName->BootRegion.romFiles[i] = NULL;
        }
    }

    for (i = 0 ; i < cfgImagesName->ControlBlockRegion.romNumber ; i++)
    {
        // free memory
        log_output("release_cfg_images_name():  free CBR region entry %d \n", i);
        if(cfgImagesName->ControlBlockRegion.romFiles[i] != NULL)
        {
            log_output("release_cfg_images_name():  free entry %d \n", i);
            free(cfgImagesName->ControlBlockRegion.romFiles[i]);
            cfgImagesName->ControlBlockRegion.romFiles[i] = NULL;
        }
    }

    for (i = 0 ; i < cfgImagesName->MainRegion.romNumber ; i++)
    {
        // free memory
        log_output("release_cfg_images_name():  free main region entry %d \n", i);
        if(cfgImagesName->MainRegion.romFiles[i] != NULL)
        {
            log_output("release_cfg_images_name():  free entry %d [%s](%d)\n", i, cfgImagesName->MainRegion.romFiles[i], strlen(cfgImagesName->MainRegion.romFiles[i]));
            free(cfgImagesName->MainRegion.romFiles[i]);
            cfgImagesName->MainRegion.romFiles[i] = NULL;
        }
    }

    // Todo : add LinuxRegion free flow.

    return 0;
}

int DumpExternalMemorySetting(ExternalMemorySetting *externalMemorySetting)
{
    unsigned int i = 0;
    unsigned int j = 0;

    log_output("DumpExternalMemorySetting(): Dump ExternalMemorySetting structure\n");

    log_output("DumpExternalMemorySetting(): Valid   (0x%08x)\n", externalMemorySetting->valid);
    log_output("DumpExternalMemorySetting(): BB Type (0x%08x)\n", externalMemorySetting->bbchipType);
    log_output("DumpExternalMemorySetting(): CFG Ver (0x%08x)\n", externalMemorySetting->CFGVersion);
    log_output("DumpExternalMemorySetting(): PMIC    (0x%08x)\n", externalMemorySetting->PMICController);

    log_output("DumpExternalMemorySetting(): ExternalRAMSetting.ver     (0x%08x)\n", externalMemorySetting->ramSetting.version);
    log_output("DumpExternalMemorySetting(): ExternalRAMSetting.number  (0x%08x)\n", externalMemorySetting->ramSetting.numValidEntries);
    log_output("DumpExternalMemorySetting(): ExternalRAMSetting.ramType (0x%08x)\n", externalMemorySetting->ramSetting.ramType);
    //for(i = 0 ; i < MAX_NUM_EXTERNAL_RAM_SETTING_ENTRIES; i++)
    for(i = 0 ; i < externalMemorySetting->ramSetting.numValidEntries; i++)
    {
        switch(externalMemorySetting->CFGVersion)
        {
            case CFGType_V1:
                log_output("DumpExternalMemorySetting(): DRAMSetting_v05[%d].ramType = 0x%08x\n", i, externalMemorySetting->ramSetting.u.v05[i].ramType);
                log_output("DumpExternalMemorySetting(): DRAMSetting_v05[%d].EMICONI = 0x%08x\n", i, externalMemorySetting->ramSetting.u.v05[i].EMI_CONI_Value);
                log_output("DumpExternalMemorySetting(): DRAMSetting_v05[%d].EMICONJ = 0x%08x\n", i, externalMemorySetting->ramSetting.u.v05[i].EMI_CONJ_Value);
                log_output("DumpExternalMemorySetting(): DRAMSetting_v05[%d].EMICONK = 0x%08x\n", i, externalMemorySetting->ramSetting.u.v05[i].EMI_CONK_Value);
                log_output("DumpExternalMemorySetting(): DRAMSetting_v05[%d].EMICONL = 0x%08x\n", i, externalMemorySetting->ramSetting.u.v05[i].EMI_CONL_Value);
                log_output("DumpExternalMemorySetting(): DRAMSetting_v05[%d].EMICONN = 0x%08x\n", i, externalMemorySetting->ramSetting.u.v05[i].EMI_CONN_Value);
                log_output("DumpExternalMemorySetting(): DRAMSetting_v05[%d].EMIDRVA = 0x%08x\n", i, externalMemorySetting->ramSetting.u.v05[i].EMI_DRVA_Value);
                log_output("DumpExternalMemorySetting(): DRAMSetting_v05[%d].EMIDRVB = 0x%08x\n", i, externalMemorySetting->ramSetting.u.v05[i].EMI_DRVB_Value);
                log_output("DumpExternalMemorySetting(): DRAMSetting_v05[%d].EMIODLA = 0x%08x\n", i, externalMemorySetting->ramSetting.u.v05[i].EMI_ODLA_Value);
                log_output("DumpExternalMemorySetting(): DRAMSetting_v05[%d].EMIODLB = 0x%08x\n", i, externalMemorySetting->ramSetting.u.v05[i].EMI_ODLB_Value);
                log_output("DumpExternalMemorySetting(): DRAMSetting_v05[%d].EMIODLC = 0x%08x\n", i, externalMemorySetting->ramSetting.u.v05[i].EMI_ODLC_Value);
                log_output("DumpExternalMemorySetting(): DRAMSetting_v05[%d].EMIODLD = 0x%08x\n", i, externalMemorySetting->ramSetting.u.v05[i].EMI_ODLD_Value);
                log_output("DumpExternalMemorySetting(): DRAMSetting_v05[%d].EMIODLE = 0x%08x\n", i, externalMemorySetting->ramSetting.u.v05[i].EMI_ODLE_Value);
                log_output("DumpExternalMemorySetting(): DRAMSetting_v05[%d].EMIODLF = 0x%08x\n", i, externalMemorySetting->ramSetting.u.v05[i].EMI_ODLF_Value);
                log_output("DumpExternalMemorySetting(): DRAMSetting_v05[%d].EMIODLG = 0x%08x\n", i, externalMemorySetting->ramSetting.u.v05[i].EMI_ODLG_Value);
                log_output("DumpExternalMemorySetting(): DRAMSetting_v05[%d].EMIDUTA = 0x%08x\n", i, externalMemorySetting->ramSetting.u.v05[i].EMI_DUTA_Value);
                log_output("DumpExternalMemorySetting(): DRAMSetting_v05[%d].EMIDUTB = 0x%08x\n", i, externalMemorySetting->ramSetting.u.v05[i].EMI_DUTB_Value);
                log_output("DumpExternalMemorySetting(): DRAMSetting_v05[%d].EMIDUTC = 0x%08x\n", i, externalMemorySetting->ramSetting.u.v05[i].EMI_DUTC_Value);
                log_output("DumpExternalMemorySetting(): DRAMSetting_v05[%d].EMIDUCA = 0x%08x\n", i, externalMemorySetting->ramSetting.u.v05[i].EMI_DUCA_Value);
                log_output("DumpExternalMemorySetting(): DRAMSetting_v05[%d].EMIDUCB = 0x%08x\n", i, externalMemorySetting->ramSetting.u.v05[i].EMI_DUCB_Value);
                log_output("DumpExternalMemorySetting(): DRAMSetting_v05[%d].EMIDUCE = 0x%08x\n", i, externalMemorySetting->ramSetting.u.v05[i].EMI_DUCE_Value);
                log_output("DumpExternalMemorySetting(): DRAMSetting_v05[%d].EMIIOCL = 0x%08x\n", i, externalMemorySetting->ramSetting.u.v05[i].EMI_IOCL_Value);
                break;
            case CFGType_V2:
                log_output("DumpExternalMemorySetting(): DRAMSetting_v06[%d].ramType = 0x%08x\n", i, externalMemorySetting->ramSetting.u.v06[i].ramType);
                log_output("DumpExternalMemorySetting(): DRAMSetting_v06[%d].EMICONI = 0x%08x\n", i, externalMemorySetting->ramSetting.u.v06[i].EMI_CONI_Value);
                log_output("DumpExternalMemorySetting(): DRAMSetting_v06[%d].EMICONJ = 0x%08x\n", i, externalMemorySetting->ramSetting.u.v06[i].EMI_CONJ_Value);
                log_output("DumpExternalMemorySetting(): DRAMSetting_v06[%d].EMICONK = 0x%08x\n", i, externalMemorySetting->ramSetting.u.v06[i].EMI_CONK_Value);
                log_output("DumpExternalMemorySetting(): DRAMSetting_v06[%d].EMICONL = 0x%08x\n", i, externalMemorySetting->ramSetting.u.v06[i].EMI_CONL_Value);
                log_output("DumpExternalMemorySetting(): DRAMSetting_v06[%d].EMICONN = 0x%08x\n", i, externalMemorySetting->ramSetting.u.v06[i].EMI_CONN_Value);
                log_output("DumpExternalMemorySetting(): DRAMSetting_v06[%d].EMIDRVA = 0x%08x\n", i, externalMemorySetting->ramSetting.u.v06[i].EMI_DRVA_Value);
                log_output("DumpExternalMemorySetting(): DRAMSetting_v06[%d].EMIDRVB = 0x%08x\n", i, externalMemorySetting->ramSetting.u.v06[i].EMI_DRVB_Value);
                log_output("DumpExternalMemorySetting(): DRAMSetting_v06[%d].EMIODLE = 0x%08x\n", i, externalMemorySetting->ramSetting.u.v06[i].EMI_ODLE_Value);
                log_output("DumpExternalMemorySetting(): DRAMSetting_v06[%d].EMIODLF = 0x%08x\n", i, externalMemorySetting->ramSetting.u.v06[i].EMI_ODLF_Value);
                log_output("DumpExternalMemorySetting(): DRAMSetting_v06[%d].EMIODLG = 0x%08x\n", i, externalMemorySetting->ramSetting.u.v06[i].EMI_ODLG_Value);
                log_output("DumpExternalMemorySetting(): DRAMSetting_v06[%d].EMIODLH = 0x%08x\n", i, externalMemorySetting->ramSetting.u.v06[i].EMI_ODLH_Value);
                log_output("DumpExternalMemorySetting(): DRAMSetting_v06[%d].EMIODLI = 0x%08x\n", i, externalMemorySetting->ramSetting.u.v06[i].EMI_ODLI_Value);
                log_output("DumpExternalMemorySetting(): DRAMSetting_v06[%d].EMIODLJ = 0x%08x\n", i, externalMemorySetting->ramSetting.u.v06[i].EMI_ODLJ_Value);
                log_output("DumpExternalMemorySetting(): DRAMSetting_v06[%d].EMIODLK = 0x%08x\n", i, externalMemorySetting->ramSetting.u.v06[i].EMI_ODLK_Value);
                log_output("DumpExternalMemorySetting(): DRAMSetting_v06[%d].EMIODLL = 0x%08x\n", i, externalMemorySetting->ramSetting.u.v06[i].EMI_ODLL_Value);
                log_output("DumpExternalMemorySetting(): DRAMSetting_v06[%d].EMIODLM = 0x%08x\n", i, externalMemorySetting->ramSetting.u.v06[i].EMI_ODLM_Value);
                log_output("DumpExternalMemorySetting(): DRAMSetting_v06[%d].EMIODLN = 0x%08x\n", i, externalMemorySetting->ramSetting.u.v06[i].EMI_ODLN_Value);
                log_output("DumpExternalMemorySetting(): DRAMSetting_v06[%d].EMIDUTA = 0x%08x\n", i, externalMemorySetting->ramSetting.u.v06[i].EMI_DUTA_Value);
                log_output("DumpExternalMemorySetting(): DRAMSetting_v06[%d].EMIDUTB = 0x%08x\n", i, externalMemorySetting->ramSetting.u.v06[i].EMI_DUTB_Value);
                log_output("DumpExternalMemorySetting(): DRAMSetting_v06[%d].EMIDUTC = 0x%08x\n", i, externalMemorySetting->ramSetting.u.v06[i].EMI_DUTC_Value);
                log_output("DumpExternalMemorySetting(): DRAMSetting_v06[%d].EMIDUCA = 0x%08x\n", i, externalMemorySetting->ramSetting.u.v06[i].EMI_DUCA_Value);
                log_output("DumpExternalMemorySetting(): DRAMSetting_v06[%d].EMIDUCB = 0x%08x\n", i, externalMemorySetting->ramSetting.u.v06[i].EMI_DUCB_Value);
                log_output("DumpExternalMemorySetting(): DRAMSetting_v06[%d].EMIDUCE = 0x%08x\n", i, externalMemorySetting->ramSetting.u.v06[i].EMI_DUCE_Value);
                log_output("DumpExternalMemorySetting(): DRAMSetting_v06[%d].EMIIOCL = 0x%08x\n", i, externalMemorySetting->ramSetting.u.v06[i].EMI_IOCL_Value);
                break;
            default:
                ;// not support now.
        }
    }

    log_output("DumpExternalMemorySetting(): flashInfo.ver      (0x%08x)\n", externalMemorySetting->flashInfo.version);
    log_output("DumpExternalMemorySetting(): flashInfo.number   (0x%08x)\n", externalMemorySetting->flashInfo.numValidEntries);
    log_output("DumpExternalMemorySetting(): flashInfo.flashType(0x%08x)\n", externalMemorySetting->flashInfo.flashType);
    //for(i = 0 ; i < MAX_NUM_EXTERNAL_RAM_SETTING_ENTRIES; i++)
    for(i = 0 ; i < externalMemorySetting->flashInfo.numValidEntries; i++)
    {
        switch(externalMemorySetting->flashInfo.flashType)
        {
            case FLASHType_NOR:
            case FLASHType_SERIAL_NOR_FLASH:
                log_output("DumpExternalMemorySetting(): flashInfo[%d].idNumber = 0x%08x\n", i, externalMemorySetting->flashInfo.u.v01[i].NOR_ID.idNumber);
                log_output("DumpExternalMemorySetting(): flashInfo[%d].id[0] = 0x%02x\n", i, externalMemorySetting->flashInfo.u.v01[i].NOR_ID.id[0]);
                log_output("DumpExternalMemorySetting(): flashInfo[%d].id[1] = 0x%02x\n", i, externalMemorySetting->flashInfo.u.v01[i].NOR_ID.id[1]);
                log_output("DumpExternalMemorySetting(): flashInfo[%d].id[2] = 0x%02x\n", i, externalMemorySetting->flashInfo.u.v01[i].NOR_ID.id[2]);
                log_output("DumpExternalMemorySetting(): flashInfo[%d].id[3] = 0x%02x\n", i, externalMemorySetting->flashInfo.u.v01[i].NOR_ID.id[3]);
                log_output("DumpExternalMemorySetting(): flashInfo[%d].id[4] = 0x%02x\n", i, externalMemorySetting->flashInfo.u.v01[i].NOR_ID.id[4]);
                log_output("DumpExternalMemorySetting(): flashInfo[%d].id[5] = 0x%02x\n", i, externalMemorySetting->flashInfo.u.v01[i].NOR_ID.id[5]);
                log_output("DumpExternalMemorySetting(): flashInfo[%d].id[6] = 0x%02x\n", i, externalMemorySetting->flashInfo.u.v01[i].NOR_ID.id[6]);
                log_output("DumpExternalMemorySetting(): flashInfo[%d].id[7] = 0x%02x\n", i, externalMemorySetting->flashInfo.u.v01[i].NOR_ID.id[7]);

                break;
            case FLASHType_NAND:
                log_output("DumpExternalMemorySetting(): flashInfo[%d].idNumber = 0x%08x\n", i, externalMemorySetting->flashInfo.u.v02[i].NAND_ID.idNumber);
                log_output("DumpExternalMemorySetting(): flashInfo[%d].id[0] = 0x%02x\n", i, externalMemorySetting->flashInfo.u.v02[i].NAND_ID.id[0]);
                log_output("DumpExternalMemorySetting(): flashInfo[%d].id[1] = 0x%02x\n", i, externalMemorySetting->flashInfo.u.v02[i].NAND_ID.id[1]);
                log_output("DumpExternalMemorySetting(): flashInfo[%d].id[2] = 0x%02x\n", i, externalMemorySetting->flashInfo.u.v02[i].NAND_ID.id[2]);
                log_output("DumpExternalMemorySetting(): flashInfo[%d].id[3] = 0x%02x\n", i, externalMemorySetting->flashInfo.u.v02[i].NAND_ID.id[3]);
                log_output("DumpExternalMemorySetting(): flashInfo[%d].id[4] = 0x%02x\n", i, externalMemorySetting->flashInfo.u.v02[i].NAND_ID.id[4]);
                log_output("DumpExternalMemorySetting(): flashInfo[%d].id[5] = 0x%02x\n", i, externalMemorySetting->flashInfo.u.v02[i].NAND_ID.id[5]);
                log_output("DumpExternalMemorySetting(): flashInfo[%d].id[6] = 0x%02x\n", i, externalMemorySetting->flashInfo.u.v02[i].NAND_ID.id[6]);
                log_output("DumpExternalMemorySetting(): flashInfo[%d].id[7] = 0x%02x\n", i, externalMemorySetting->flashInfo.u.v02[i].NAND_ID.id[7]);
                break;
            default:
                ;// not support now.
        }
    }

    return 0;
}
