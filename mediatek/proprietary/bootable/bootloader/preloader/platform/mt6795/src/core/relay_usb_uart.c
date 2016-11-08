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

#include "typedefs.h"
#include "platform.h"
#include "download.h"
#include "meta.h"

#include "usbtty.h"

#if (CFG_DT_MD_DOWNLOAD && CFG_DT_MD_DOWNLOAD_BY_RELAY)

#define MOD                 "[TOOL]"

#define CDC_DTR_MASK         0x01
extern int g_usb_port_state;

/*============================================================================*/
/* INTERNAL FUNCTIONS                                                         */
/*============================================================================*/
int check_usb_port_close(void)
{
    return (!(g_usb_port_state & CDC_DTR_MASK));
}

int relay_uart_send(u8 *buf, u32 len)
{
    while (len--) {
        relay_PutUARTByte(*buf++);
    }
    return 0;
}

int relay_uart_recv(u8 *buf, u32 size, u32 tmo_ms)
{
    int ret;

    ret = relay_GetUARTBytes(buf, size, tmo_ms);

    return ret;
}

bool uart_handshake_6261_brom(void)
{
    u8 c = 0;
    int tmo_en = 1;
    ulong start_time;

    int i=0; //state machine index
	u8 req[] = {0xa0, 0x0a, 0x50, 0x05};
	u8 ack[] = {0x5f, 0xf5, 0xaf, 0xfa};
    uint32 tmo_ms = 5;
	ulong hsk_time = get_timer(0);

    mtk_serial_set_current_uart(UART1);
	
    while(get_timer(hsk_time) < 2500) {
        
		relay_PutUARTByte(req[i]);
		print("%s <UART> send 0x%x to 6261.\n",MOD, req[i]);
		
        start_time = get_timer(0);
        while(1) {
            if (tmo_en && (get_timer(start_time) > tmo_ms))
            {
                i = 0;
                break;
            }

            /* kick watchdog to avoid cpu reset */
            if (!tmo_en)
                platform_wdt_kick();

            GetUARTBytes(&c, 1, 10);
            print("%s <UART> Recv 0x%x from 6261, LSR = 0x%x.\n", MOD, c, UART_read_LSR());
			
            if(c == ack[i])
            {
                print("%s MATCH!\n", MOD);
                i++; //goto next state
				break;
            }
			else
			{
                i = 0; //reset state
                c = 0;
				break;
			}
        }

		if(i==4) //handshake complete
			break;
    }

    //mtk_serial_set_current_uart(CFG_UART_LOG);

	if (i==4){
        print("%s <UART> Handshake with 6261 BROM complete!\n",MOD);
		return TRUE;
	}
	else{
	    print("%s <UART> Handshake with 6261 BROM FAIL! TIMEOUT...\n",MOD);
		return FALSE;
	}
}

int relay_usb_send(u8 *buf, u32 len)
{
    mt_usbtty_putcn((int)len, (char*)buf, 0);
    mt_usbtty_flush();

    return 0;
}

int relay_usb_recv(u8 *buf, u32 size, u32 tmo_ms)
{
    ulong start_time = get_timer(0);
    u32 dsz;
    u32 tmo_en = (tmo_ms) ? 1 : 0;
    u8 *ptr = buf;
    int recv_cnt = 0;

    if (!size)
        return 0;

    while (1) {
        if (tmo_en && ((get_timer(start_time) > tmo_ms) || dsz == 0)){
            return recv_cnt;
        }

		dsz = mt_usbtty_query_data_size();
        if(dsz) {
            dsz = dsz < size ? dsz : size;
            mt_usbtty_getcn(dsz, (char*)ptr);
            ptr  += dsz;
            size -= dsz;
			recv_cnt += dsz;
        }
        if (size == 0)
            break;
    }

    return recv_cnt;
}

int handshake_usb_recv(u8 *buf, u32 size, u32 tmo_ms)
{
    ulong start_time = get_timer(0);
    u32 dsz;
    u32 tmo_en = (tmo_ms) ? 1 : 0;
    u8 *ptr = buf;

    if (!size)
        return 0;

    while (1) {
        if (tmo_en && (get_timer(start_time) > tmo_ms)){
            //print("%s : usb receive timeout\n", MOD);
            return -1;
        }

        dsz = mt_usbtty_query_data_size();
        if (dsz) {
            dsz = dsz < size ? dsz : size;
            mt_usbtty_getcn(dsz, (char*)ptr);
            ptr  += dsz;
            size -= dsz;
        }
        if (size == 0)
            break;
    }

    return 0;
}

bool usb_handshake_6261_flashtool(void)
{
    u8 startcmd[] = {0xa0, 0x0a, 0x50, 0x05};
    ulong start = 0;
    u32 i = 0, j = 5;
    u8 cmd = 0, rsp;
    u32 tmo = 5000;
    ulong start_time = get_timer(0);
	
    while(1) {
		if(get_timer(start_time) > 5000){
            print("%s Wait over 5s to listen flashtool.\n", MOD);
			break;
		}
		handshake_usb_recv(&cmd, 1, 10);
		if(cmd == startcmd[0]) {
			print("%s Flashtool handshake cmd matched!\n", MOD);
            break;
		}
    }

	start = get_timer(0);
    do {
        if (get_timer(start) > tmo)
            return FALSE;

        /* timeout 1 ms */
        if (0 != handshake_usb_recv(&cmd, 1, 10)) {
            continue;
        }

        if (cmd == startcmd[i]) {
            rsp = ~cmd;
            i++;
        } else {
            rsp = cmd + 1;
            i = 0;
        }
        print("%s TGT<-(0x%x)--TOOL\n", MOD, cmd);
        print("%s TGT--(0x%x)->TOOL\n", MOD, rsp);
        relay_usb_send(&rsp, 1);
    } while(i < sizeof(startcmd));
    return TRUE;
}

int usb_uart_relay(void)
{
    uint8 usb_data[256] = {'\0'};
	uint8 uart_data[256] = {'\0'};
	int i = 0;
    int cnt0 = 0, cnt1 = 0;
    ulong start_time = 0;
	bool check_relay_finish = FALSE;
	
	while(1){
        platform_wdt_kick();

        usb_data[0] = '\0';
	    uart_data[0] = '\0';
#if 0		
        cnt0 = relay_usb_recv(usb_data, 256, 1); //Received from USB
        //print("----- USB Get: %d Bytes. -----\n", cnt0);
        if(cnt0 > 0){
			//if(cnt0 <= 10){
				//print("Dump Bytes: ");
				//for(i=0;i<cnt0;i++)
			        //print("0x%x ", usb_data[i]);
				//print("\n");
			//}
            relay_uart_send(usb_data, cnt0);
        }
	    
		cnt1 = relay_uart_recv(uart_data, 256, 1); //Received from UART
		//print("----- UART Get: %d Bytes. LSR = 0x%x -----\n", cnt1, UART_read_LSR());
        if(cnt1 > 0) {
			//if(cnt1 <= 10){
				//print("Dump Bytes: ");
				//for(i=0;i<cnt1;i++)
			        //print("0x%x ", uart_data[i]);
				//print("\n");
			//}
		    relay_usb_send(uart_data, cnt1);
        }
#else
        cnt0 = relay_usb_recv(usb_data, 256, 1); //Received from USB
        if(cnt0 > 0)
            relay_uart_send(usb_data, cnt0);
	    
		cnt1 = relay_uart_recv(uart_data, 256, 1); //Received from UART
        if(cnt1 > 0)
		    relay_usb_send(uart_data, cnt1);

#endif
        if(cnt0 == 0 && cnt1 == 0) {
            check_relay_finish = TRUE;
            if(!start_time)
			    start_time = get_timer(0); //should only do once
        }
		else {
            check_relay_finish = FALSE;
            start_time = 0;
        }
		
		if(check_relay_finish) {
            if(get_timer(start_time) > 5000 && check_usb_port_close()) {
                print("%s No Data Transfering. Stop Relay. usb_port_close = %d\n", MOD, check_usb_port_close());
                //mtk_arch_reset(1);
                pl_power_off();  //shut down
            }
        }
	}
    return TRUE;
}

#endif

