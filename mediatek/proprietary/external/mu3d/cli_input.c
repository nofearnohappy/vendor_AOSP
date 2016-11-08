/*----------------------------------------------------------------------------*
 * No Warranty                                                                *
 * Except as may be otherwise agreed to in writing, no warranties of any      *
 * kind, whether express or implied, are given by MTK with respect to any MTK *
 * Deliverables or any use thereof, and MTK Deliverables are provided on an   *
 * "AS IS" basis.  MTK hereby expressly disclaims all such warranties,        *
 * including any implied warranties of merchantability, non-infringement and  *
 * fitness for a particular purpose and any warranties arising out of course  *
 * of performance, course of dealing or usage of trade.  Parties further      *
 * acknowledge that Company may, either presently and/or in the future,       *
 * instruct MTK to assist it in the development and the implementation, in    *
 * accordance with Company's designs, of certain softwares relating to        *
 * Company's product(s) (the "Services").  Except as may be otherwise agreed  *
 * to in writing, no warranties of any kind, whether express or implied, are  *
 * given by MTK with respect to the Services provided, and the Services are   *
 * provided on an "AS IS" basis.  Company further acknowledges that the       *
 * Services may contain errors, that testing is important and Company is      *
 * solely responsible for fully testing the Services and/or derivatives       *
 * thereof before they are used, sublicensed or distributed.  Should there be *
 * any third party action brought against MTK, arising out of or relating to  *
 * the Services, Company agree to fully indemnify and hold MTK harmless.      *
 * If the parties mutually agree to enter into or continue a business         *
 * relationship or other arrangement, the terms and conditions set forth      *
 * hereunder shall remain effective and, unless explicitly stated otherwise,  *
 * shall prevail in the event of a conflict in the terms in any agreements    *
 * entered into between the parties.                                          *
 *---------------------------------------------------------------------------*/
/*-----------------------------------------------------------------------------
 * Copyright (c) 2008, MediaTek, Inc.
 * All rights reserved.
 *
 * Unauthorized use, practice, perform, copy, distribution, reproduction,
 * or disclosure of this information in whole or in part is prohibited.
 *-----------------------------------------------------------------------------
 *
 *
 *---------------------------------------------------------------------------*/

/** @file cli_input.c
 *  Add your description here.
 */

//-----------------------------------------------------------------------------
// Include files
//-----------------------------------------------------------------------------
#include "cli.h"


//-----------------------------------------------------------------------------
// Configurations
//-----------------------------------------------------------------------------


//-----------------------------------------------------------------------------
// Constant definitions
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Macro definitions
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Static variables
//-----------------------------------------------------------------------------

// CLI Input Character Buffer
UINT8  _aszCliInputBuf[CLI_INPUT_BUF_ROW_NUM][CLI_INPUT_BUF_SIZE];
UINT32 _u4CliInputBufIdx = 0;
static UINT8 _szCliPromptStr[CLI_INPUT_BUF_SIZE];

//-----------------------------------------------------------------------------
// global variables
//-----------------------------------------------------------------------------

UINT8 u1CliUseUartPort=0;
UINT8 u1UserCLiExitFlag=0;

//-----------------------------------------------------------------------------
// Imported variables
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Imported functions
//-----------------------------------------------------------------------------

extern INT32 CLI_PromptParser(const char *szCmdBuf);
extern void CLI_CmdTblClear(void);
extern void CLI_ParserSetPromptStr(char* szPtr);
extern INT32 CLI_CmdTblAttach(CLI_EXEC_T* pTbl);
extern CLI_EXEC_T* CLI_GetDefaultCmdTbl(void);
extern void CLI_ModuleCmd_Install(void);

//-----------------------------------------------------------------------------
// Static functions
//-----------------------------------------------------------------------------
UINT32 _CliInputBuf_PreviousIdx(UINT32 u4Idx)
{
	return (u4Idx+CLI_INPUT_BUF_ROW_NUM-1) % CLI_INPUT_BUF_ROW_NUM;

	#if 0
	if( u4Idx > 0)
	{
		return (--u4Idx);
	}
	else
	{
		return CLI_INPUT_BUF_ROW_NUM;
	}
	#endif
}

UINT32 _CliInputBuf_NextIdx(UINT32 u4Idx)
{
	return (u4Idx+CLI_INPUT_BUF_ROW_NUM+1) % CLI_INPUT_BUF_ROW_NUM;

	#if 0
	if( u4Idx < CLI_INPUT_BUF_ROW_NUM)
	{
		return (++u4Idx);
	}
	else
	{
		return 0;
	}
	#endif
}
/*
#if 0 //jackson remove
#include "stdarg.h"
#include "stdio.h"
#include "string.h"
//UINT8 u1CliUseUartModID=0;
#define I1BUFLEN  128
static char i1Buf[I1BUFLEN];
int cli_print(char *fmt,...)
{
	UINT16 len;
	va_list ap;
    char   *u1p;
	va_start (ap, fmt);
	len=vsnprintf(i1Buf,I1BUFLEN,fmt,ap);
	va_end (ap);

	while (UART_GetTxRoomLeft(u1CliUseUartPort)<len) kal_sleep_task(2);
//	UART_PutBytes(u1CliUseUartPort,(kal_uint8 *)i1Buf, len, MOD_UART1_HISR);

    u1p = &i1Buf[0];//todo: UART_PutBytes(...MOD_UART1_HISR) can't work.
    while(*u1p!=NULL){
        PutUARTByte(u1CliUseUartPort,*u1p);
        u1p++;
    }

	return len;
		}
#endif //jackson remove
*/

#define GetUARTByte(x)      getchar()
#define PutUARTByte(x, y)   putchar(y)
//#define cli_print printf
//#define cli_print(char *fmt,...) printf(char *fmt,...)
UINT32 getOneLine(UINT32 u4Len)
{
	UINT8 u1Ch, u1Cnt=0,u1i=0;

	//while ((u1Ch = GetUARTByte(u1CliUseUartPort)) != ASCII_KEY_CR) {
	while (((u1Ch = GetUARTByte(u1CliUseUartPort)) != ASCII_KEY_CR) ) {
        //cli_print("1.1:u1Ch=%x\r\n",u1Ch);
        if (u1Ch == ASCII_KEY_BS) {
            if (u1Cnt > 0) {
				cli_print("\b \b");
				u1Cnt--;
			}
		}
        else if ( u4Len && (u1Cnt >= u4Len-1) ) {
            //cli_print("out 0x7\r\n");
			PutUARTByte(u1CliUseUartPort, 0x07);
		}
        else if(u1Ch == ASCII_KEY_ESC){ //arrow case
            u1Ch = GetUARTByte(u1CliUseUartPort);
            //cli_print("2:u1Ch=%x\r\n",u1Ch);
            if(u1Ch == ASCII_KEY_ARROW){
                u1Ch = GetUARTByte(u1CliUseUartPort);
                //cli_print("3:u1Ch=%x\r\n",u1Ch);
                switch(u1Ch){
                case ASCII_KEY_UP:
                   // cli_print("ASCII_KEY_UP\r\n");
                   // eddie                   
                   // clear previous character                   
                   while(u1Cnt > 0)                   
				   {                      
						cli_print("\b \b");                      
						u1Cnt--;                   
					}
                    _u4CliInputBufIdx = _CliInputBuf_PreviousIdx(_u4CliInputBufIdx);
                    u1i =0;
                    //cli_print("\r\n");
                    while(_aszCliInputBuf[_u4CliInputBufIdx][u1i] != '\0'){
                        PutUARTByte(u1CliUseUartPort, _aszCliInputBuf[_u4CliInputBufIdx][u1i]);
                        u1i++;
                    }
                    u1Cnt = u1i;
                    break;
                case ASCII_KEY_DOWN:
                    //cli_print("ASCII_KEY_DOWN\r\n");
                    // eddie
                    // clear previous character
                    while(u1Cnt > 0)
					{
						cli_print("\b \b");
						u1Cnt--;
                    }
                    _u4CliInputBufIdx = _CliInputBuf_NextIdx(_u4CliInputBufIdx);
                    u1i =0;
                    //cli_print("\r\n");
                    while(_aszCliInputBuf[_u4CliInputBufIdx][u1i] != '\0'){
                        PutUARTByte(u1CliUseUartPort, _aszCliInputBuf[_u4CliInputBufIdx][u1i]);
                        u1i++;
                    }
                    u1Cnt = u1i;
                    break;
                case ASCII_KEY_LEFT:
                    //cli_print("ASCII_KEY_LEFT\r\n");
                    if (u1Cnt > 0) {
                        cli_print("\b \b");
                        u1Cnt--;
                    }
                    break;
                case ASCII_KEY_RIGHT:
                    //cli_print("ASCII_KEY_RIGHT, do nothing\r\n");
                    break;
                default:
                    cli_print("unknown byte: %x\r\n",u1Ch);
                    break;
                }
            }
            //else if(u1Ch == ASCII_KEY_CR){
            else if(u1Ch == ASCII_KEY_CR || u1Ch ==ASCII_KEY_NL){
                //cli_print("1 u1Ch=CR or NL,break\r\n");
                break;
            }
            else{
                //u1buf[u1Cnt++] = u1Ch;
                _aszCliInputBuf[_u4CliInputBufIdx][u1Cnt++] = u1Ch;
            }
        }
//        else if (u1Ch < 0x20) {
//			continue;
//		}
        else if(u1Ch == ASCII_KEY_CR || u1Ch ==ASCII_KEY_NL){
            //cli_print("2 u1Ch=CR or NL,break\r\n");
            break;
        }
		else {
			//u1buf[u1Cnt++] = u1Ch;
            _aszCliInputBuf[_u4CliInputBufIdx][u1Cnt++] = u1Ch;
			PutUARTByte(u1CliUseUartPort, u1Ch);
		}
	}

	//u1buf[u1Cnt] = '\0';
    _aszCliInputBuf[_u4CliInputBufIdx][u1Cnt] = '\0';
	cli_print("\r\n");
	return u1Cnt;
}

#if 0
UINT32 getOneLine(UINT8 *u1buf, UINT32 u4Len)
{
	UINT8 u1Ch, u1Cnt=0;

	while ((u1Ch = GetUARTByte(u1CliUseUartPort)) != ASCII_KEY_CR) {
        //cli_print("1:u1Ch=%x\r\n",u1Ch);
        if (u1Ch == ASCII_KEY_BS) {
            if (u1Cnt > 0) {
				cli_print("\b \b");
				u1Cnt--;
			}
		}
        else if ( u4Len && (u1Cnt >= u4Len-1) ) {
			PutUARTByte(u1CliUseUartPort, 0x07);
		}
        else if(u1Ch == ASCII_KEY_ESC){ //arrow case
            u1Ch = GetUARTByte(u1CliUseUartPort);
            //cli_print("2:u1Ch=%x\r\n",u1Ch);
            if(u1Ch == ASCII_KEY_ARROW){
                u1Ch = GetUARTByte(u1CliUseUartPort);
                //cli_print("3:u1Ch=%x\r\n",u1Ch);
                switch(u1Ch){
                case ASCII_KEY_UP:
                    cli_print("ASCII_KEY_UP\r\n");
                    break;
                case ASCII_KEY_DOWN:
                    cli_print("ASCII_KEY_DOWN\r\n");
                    break;
                case ASCII_KEY_LEFT:
                    //cli_print("ASCII_KEY_LEFT\r\n");
                    if (u1Cnt > 0) {
                        cli_print("\b \b");
                        u1Cnt--;
                    }
                    break;
                case ASCII_KEY_RIGHT:
                    cli_print("ASCII_KEY_RIGHT\r\n");
                    break;
                default:
                    cli_print("unknown byte: %x\r\n",u1Ch);
                    break;
                }
            }
            else if(u1Ch == ASCII_KEY_CR){
                break;
            }
            else{
                u1buf[u1Cnt++] = u1Ch;
            }
        }
        else if (u1Ch < 0x20) {
			continue;
		}
		else {
			u1buf[u1Cnt++] = u1Ch;
			PutUARTByte(u1CliUseUartPort, u1Ch);
		}
	}
	u1buf[u1Cnt] = '\0';
	cli_print("\r\n");
	return u1Cnt;
}
#endif


//-----------------------------------------------------------------------------
/**
 *
 *  @param
 *  @retval
 */
//-----------------------------------------------------------------------------



//-----------------------------------------------------------------------------
// public functions
//-----------------------------------------------------------------------------
/******************************************************************************
* Function		: CLI_Input(void)
* Description	: cli input fucntion
* Parameter		: None
* Return		: None
******************************************************************************/
void CLI_Input()
{
	INT32 i4ReturnValue;
	UINT32 u4TempIdx = 0;
	//CHAR cChar = 0;

    //
	for (u4TempIdx = 0; u4TempIdx < CLI_INPUT_BUF_ROW_NUM; u4TempIdx++)
	{
		_aszCliInputBuf[u4TempIdx][0] = ASCII_NULL;
	}

//	cli_print("%s%s>", CLI_PROMPT_STR, _szCliPromptStr);

	while ( 1 ) {
		//cli_print("\n%s idx=%d\r\n", CLI_PROMPT_STR,_u4CliInputBufIdx);
		cli_print("\n%s", CLI_PROMPT_STR);
		//cli_print("cli_print\r\n");
		if ( getOneLine(CLI_INPUT_BUF_SIZE) ){
            cli_print("input=%s\r\n",_aszCliInputBuf[_u4CliInputBufIdx]);
			//ParseCmd(_au1InputStr);
			i4ReturnValue = CLI_PromptParser((const char *)_aszCliInputBuf[_u4CliInputBufIdx]);
			if ((i4ReturnValue < CLI_COMMAND_OK) && (i4ReturnValue != CLI_UNKNOWN_CMD))
			{
				cli_print("CLI Command Return Value (%d)\n", (int)i4ReturnValue);
			}
		}
        _u4CliInputBufIdx = _CliInputBuf_NextIdx(_u4CliInputBufIdx);
        if(u1UserCLiExitFlag==1){
            break;
        }
	}
    u1UserCLiExitFlag=0;
    cli_print("CLI_Input return\r\n");
    return;
}
#if 0
void CLI_Input()
{
	INT32 i4ReturnValue;
	UINT32 u4TempIdx = 0;
	CHAR cChar = 0;

    //??????
	for (u4TempIdx = 0; u4TempIdx < CLI_INPUT_BUF_ROW_NUM; u4TempIdx++)
	{
		_aszCliInputBuf[u4TempIdx][0] = ASCII_NULL;
	}

//	cli_print("%s%s>", CLI_PROMPT_STR, _szCliPromptStr);

	while ( 1 ) {
		cli_print("\n%s ", CLI_PROMPT_STR);
		if ( getOneLine(_aszCliInputBuf[0], CLI_INPUT_BUF_SIZE) ){
            //cli_print("input=%s\r\n",_aszCliInputBuf[0]);
			//ParseCmd(_au1InputStr);
			i4ReturnValue = CLI_PromptParser(_aszCliInputBuf[0]);
			if ((i4ReturnValue < CLI_COMMAND_OK) && (i4ReturnValue != CLI_UNKNOWN_CMD))
			{
				cli_print("CLI Command Return Value (%d)\n", i4ReturnValue);
			}
		}
	}
}
#endif
//-----------------------------------------------------------------------------
/**
 *
 *  @param
 *  @param
 *  @param
 *  @retval
 *  @retval
 */
//-----------------------------------------------------------------------------
/******************************************************************************
* Function		: CLI_Init(void)
* Description	: cli input initialization
* Parameter		: None
* Return		: None
******************************************************************************/
void CLI_Init(void)
{
    _szCliPromptStr[0] = ASCII_NULL;

	_u4CliInputBufIdx = 0;
	CLI_CmdTblClear();	// Initialize Command Table

    //CLI_AliasInit();		// Initialize Alias Table

    CLI_ParserSetPromptStr((char*)_szCliPromptStr);

	CLI_CmdTblAttach(CLI_GetDefaultCmdTbl());

    CLI_ModuleCmd_Install(); //jackson add this API

    //cli_print("", _arNullCmdTbl, _arDefaultCmdTbl);
    CLI_Input();
}
