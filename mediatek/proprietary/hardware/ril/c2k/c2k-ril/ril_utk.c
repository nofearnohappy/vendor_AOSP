/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

/* //hardware/viatelecom/ril/viatelecom-withuim-ril/viatelecom-withuim-ril.c
**
** Copyright 2009, Viatelecom Inc.
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/

#include <telephony/ril.h>
#include <stdio.h>
#include <assert.h>
#include <string.h>
#include <errno.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <pthread.h>
#include <alloca.h>
#include <getopt.h>
#include <sys/socket.h>
#include <cutils/sockets.h>
#include <termios.h>
#include "atchannel.h"
#include "oem-support.h"
#include "ril_callbacks.h"
#include <utils/Log.h>

#include <cutils/properties.h>
#include <netdb.h>
#include <sys/param.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <linux/if.h>
#include <linux/sockios.h>
#include <linux/route.h>

#define STK_CHANNEL_CTX getRILChannelCtxFromToken(t)

static void requestUTKProfileDownload(void *data, size_t datalen, RIL_Token t);
static void requestUTKCallConfirmed(void *data, size_t datalen, RIL_Token t);
static void requestUTKSendEnvelopeCommand(void *data, size_t datalen, RIL_Token t);
static void requestUTKSendTerminalResponse(void *data, size_t datalen, RIL_Token t);
static void requestUTKGetLocalInfo(void *data, size_t datalen, RIL_Token t);
static void requestUTKRefresh(void *data, size_t datalen, RIL_Token t);
static void requestUTKSetupMenu(void *data, size_t datalen, RIL_Token t);
static void requestSTKQueryMenu(void *data, size_t datalen, RIL_Token t);

int rilUtkMain(int request, void *data, size_t datalen, RIL_Token t)
{
    switch (request) {
        case RIL_REQUEST_STK_SET_PROFILE:
            requestUTKProfileDownload(data, datalen, t);
            break;
        case RIL_REQUEST_STK_HANDLE_CALL_SETUP_REQUESTED_FROM_SIM:
            requestUTKCallConfirmed(data, datalen, t);
            break;
        case RIL_REQUEST_STK_SEND_ENVELOPE_COMMAND:
            requestUTKSendEnvelopeCommand(data, datalen, t);
            break;
        case RIL_REQUEST_STK_SEND_TERMINAL_RESPONSE:
            requestUTKSendTerminalResponse(data, datalen, t);
            break;
        case RIL_REQUEST_GET_LOCAL_INFO:
            requestUTKGetLocalInfo(data, datalen, t);
            break;
        case RIL_REQUEST_UTK_REFRESH:
            requestUTKRefresh(data, datalen, t);
            break;
        case RIL_REQUEST_QUERY_UTK_MENU_FROM_MD:
            requestUTKSetupMenu(data, datalen, t);
            break;
        case RIL_REQUEST_QUERY_STK_MENU_FROM_MD:
            requestSTKQueryMenu(data, datalen, t);
            break;
        default:
            return 0; /* no matched request */
    }
    return 1;
}

int rilUtkUnsolicited(const char *s, const char *sms_pdu)
{
    if (strStartsWith(s, "+CRSM:")) {
        char* proactive_cmd = NULL;

        ParseAtcmdCRSM(s, &proactive_cmd);

        if (!proactive_cmd) {
            RIL_onUnsolicitedResponse(RIL_UNSOL_UTK_SESSION_END, NULL, 0);
        } else {
            RIL_onUnsolicitedResponse(RIL_UNSOL_UTK_PROACTIVE_COMMAND,
                    proactive_cmd, strlen(proactive_cmd));
            free(proactive_cmd);
        }
        return 1;
    } else if (strStartsWith(s, "+UTKURC:")) {
        char* proactive_command = NULL;
        ParseUtkProcmdStr(s, &proactive_command);

        int str_len = strlen(proactive_command);
        char result[5] = {0};
        memcpy(result, proactive_command + (str_len - 4), 4);
        memset(proactive_command + (str_len - 4), 0, 4);

        RIL_onUnsolicitedResponse (
            RIL_UNSOL_UTK_PROACTIVE_COMMAND,
            proactive_command, strlen(proactive_command));
        if (!proactive_command) {
            free(proactive_command);
        }
        return 1;
    } else if (strStartsWith(s, "+UTKIND:")) {
        char* proactive_cmd = NULL;
        ParseUtkRawData(s, &proactive_cmd);
        if ( !proactive_cmd ){
            RIL_onUnsolicitedResponse(RIL_UNSOL_UTK_SESSION_END, NULL, 0);
        }else{
            RIL_onUnsolicitedResponse(RIL_UNSOL_UTK_PROACTIVE_COMMAND, proactive_cmd, strlen(proactive_cmd));
            free(proactive_cmd);
        }
        return 1;
    } else if (strStartsWith(s, "+UTKNOTIFY:")) {
        char* proactive_cmd = NULL;
        ParseUtkRawData(s, &proactive_cmd);
        if ( proactive_cmd ){
            if (strStartsWith(proactive_cmd, "81")){
                //ignore
                LOGD("ignore +UTKNOTIFY = %s", proactive_cmd);
            }else{
                RIL_onUnsolicitedResponse(RIL_UNSOL_UTK_PROACTIVE_COMMAND, proactive_cmd, strlen(proactive_cmd));
            }

            free(proactive_cmd);
        }
        return 1;
    }
    else if (strStartsWith(s, "+UTKCALL:")) {
        //not used
        return 1;
    }
    return 0;
}

static void requestUTKProfileDownload(void *data, size_t datalen, RIL_Token t)
{
  int err = at_send_command("AT+UTKPD", NULL, STK_CHANNEL_CTX);
  LOGD("requestUTKProfileDownload err=%d", err);

  if(err < 0)
  {
    RIL_onRequestComplete(t, RIL_E_GENERIC_FAILURE, NULL, 0);
  }
  else
  {
    RIL_onRequestComplete(t, RIL_E_SUCCESS, NULL, 0);
  }
}

static void requestUTKCallConfirmed(void *data, size_t datalen, RIL_Token t)
{
  char *cmd = NULL;
  int err = 0;

  asprintf(&cmd, "AT+UTKCONF=%d", ((int *)data)[0]);
  err = at_send_command(cmd, NULL, STK_CHANNEL_CTX);
  free(cmd);

  if(err < 0)
  {
    RIL_onRequestComplete(t, RIL_E_GENERIC_FAILURE, NULL, 0);
  }
  else
  {
    RIL_onRequestComplete(t, RIL_E_SUCCESS, NULL, 0);
  }
}

static void requestUTKSendEnvelopeCommand(void *data, size_t datalen, RIL_Token t)
{
    ATResponse *p_response = NULL;
    int err;
    char *cmd = NULL;
    char* cmd_data = (char*)data;

    //asprintf(&cmd, "at+crsm=194,0,0,0,0,%d,\"%s\"", strlen(cmd_data) / 2, cmd_data);
    asprintf(&cmd, "AT+UTKENV=%s", cmd_data);
    err = at_send_command(cmd, NULL, STK_CHANNEL_CTX);
    free(cmd);

    if ( err == 0 )
        RIL_onRequestComplete(t, RIL_E_SUCCESS, NULL, 0);
    else
        RIL_onRequestComplete(t, RIL_E_GENERIC_FAILURE, NULL, 0);
}

static void requestUTKSendTerminalResponse(void *data, size_t datalen, RIL_Token t)
{
    ATResponse *p_response = NULL;
    int err;
    char *cmd = NULL;
    char* cmd_data = (char*)data;

    //asprintf(&cmd, "at+crsm=20,0,0,0,0,%d,\"%s\"", strlen(cmd_data) / 2, cmd_data);
    asprintf(&cmd, "AT+UTKTERM=%s", cmd_data);
    err = at_send_command(cmd, NULL, STK_CHANNEL_CTX);
    free(cmd);

    if ( err == 0 )
        RIL_onRequestComplete(t, RIL_E_SUCCESS, NULL, 0);
    else
        RIL_onRequestComplete(t, RIL_E_GENERIC_FAILURE, NULL, 0);
}

static void requestUTKGetLocalInfo(void *data, size_t datalen, RIL_Token t)
{
    int i;
    int err = 0;
    ATResponse *p_response = NULL;


    int response[8] = {0};
    int size = sizeof(response) / sizeof(response[0]);
    char *line = NULL;

    err = at_send_command_singleline("AT+VLOCINFO?", "+VLOCINFO:", &p_response, STK_CHANNEL_CTX);

    if (err < 0 || p_response->success == 0) {
        LOGE("requestGetLocalInfo at_send_command_singleline err  = %d", err );
        goto error;
    }

    line = p_response->p_intermediates->line;

    err = at_tok_start(&line);

    if (err < 0) {
        LOGE("requestGetLocalInfo at_tok_start err");
        goto error;
    }
    for (i = 0; i < size; i++) {
        err = at_tok_nextint(&line, &response[i]);

        if (err < 0) {
            LOGE("requestGetLocalInfo at_tok_nextint err i = %d", i);
            goto error;
        }
    }
    RIL_onRequestComplete(t, RIL_E_SUCCESS, response, sizeof(response));
    at_response_free(p_response);
    return;
error:
    at_response_free(p_response);
    LOGE("requestGetLocalInfo return error = %d", err);
    RIL_onRequestComplete(t, RIL_E_GENERIC_FAILURE, NULL, 0);
}

static void requestUTKRefresh(void *data, size_t datalen, RIL_Token t)
{
    int err = 0;
    ATResponse *p_response = NULL;
    char *line = NULL;
    int type = ((int*)data)[0];
    char * cmd = NULL;

    LOGD("requestUtkRefresh type=%d", type);
    if(type == 2)
        property_set("net.cdma.boottimes","0");  //Fix CRTS#19231
    asprintf(&cmd, "AT+UTRFSH=%d", type);
    err = at_send_command(cmd, &p_response, STK_CHANNEL_CTX);
    free(cmd);
    if ((err < 0) || p_response==NULL || (p_response->success == 0))
    {
        goto error;
    }

    RIL_onRequestComplete(t, RIL_E_SUCCESS, NULL, 0);
    at_response_free(p_response);

    return;

error:
    LOGE("requestUtkRefresh  ERROR");
    RIL_onRequestComplete(t, RIL_E_GENERIC_FAILURE, NULL, 0);
    at_response_free(p_response);
}

void requestUTKSetupMenu(void *data, size_t datalen, RIL_Token t)
{
    int err = 0;
    ATResponse *p_response = NULL;
    char *line = NULL;
    char * responseStr = NULL;

    err = at_send_command_singleline ("AT+UTKMENU", "+UTKMENU:", &p_response, STK_CHANNEL_CTX);
    if ((err < 0) || (p_response->success == 0))
    {
        goto error;
    }

    line = p_response->p_intermediates->line;
    LOGD("requestUTKSetupMenu:  before at_tok_start line = %s",line);
    err = at_tok_start(&line);
    if (err < 0)
    {
        goto error;
    }

    err = at_tok_nextstr(&line, &responseStr);
    LOGD("requestUTKSetupMenu: responseStr = %s",responseStr);
    RIL_onRequestComplete(t, RIL_E_SUCCESS, responseStr, sizeof(char *));
    at_response_free(p_response);
    return;
error:
    RIL_onRequestComplete(t, RIL_E_GENERIC_FAILURE, NULL, 0);
    at_response_free(p_response);
}
void requestSTKQueryMenu(void *data, size_t datalen, RIL_Token t) {
    int err = 0;
    ATResponse *p_response = NULL;
    char *line = NULL;
    char * responseStr = NULL;
    int p1 = 0;

    err = at_send_command_singleline ("AT+STKMENU", "+STKMENU:", &p_response, STK_CHANNEL_CTX);
    if ((err < 0) || (p_response->success == 0))
    {
        goto error;
    }

    line = p_response->p_intermediates->line;
    LOGD("requestSTKQueryMenu:  before at_tok_start line = %s",line);
    err = at_tok_start(&line);
    if (err < 0) {
        goto error;
    }

    err = at_tok_nextint(&line, &p1);
    if(err < 0) {
    	   goto error;
    }

    err = at_tok_nextstr(&line, &responseStr);
    if(err < 0) {
    	   goto error;
    }

    LOGD("requestSTKQueryMenu: responseStr = %s",responseStr);
    RIL_onRequestComplete(t, RIL_E_SUCCESS, responseStr, sizeof(char *));
    at_response_free(p_response);
    return;
error:
    LOGE("There is something wrong with the +STKMENU");
    RIL_onRequestComplete(t, RIL_E_GENERIC_FAILURE, NULL, 0);
    at_response_free(p_response);
}

/// M: [C2K][IR] Support CT 3g dual Mode card IR feature. @{
/* Switch UTK/STK mode. */
int switchStkUtkMode(int mode, RIL_Token t) {
    int err = 0;
    ATResponse *p_response = NULL;
    LOGD("switchStkUtkMode(), mode=%d.", mode);
    // AT+EUTK
    char* cmd;
    err = asprintf(&cmd, "AT+EUTK=%d", mode);
    LOGD("switchStkUtkMode(), send command %s.", cmd);
    err = at_send_command(cmd, &p_response, STK_CHANNEL_CTX);
    free(cmd);
    at_response_free(p_response);
    p_response = NULL;
    return err;
}
/// M: [C2K][IR] Support CT 3g dual Mode card IR feature. @}

