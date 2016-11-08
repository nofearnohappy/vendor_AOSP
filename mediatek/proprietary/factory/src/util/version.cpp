/* Copyright Statement:
*                                                                                                            
* This software/firmware and related documentation ("MediaTek Software") are                                 
* protected under relevant copyright laws. The information contained herein                                  
* is confidential and proprietary to MediaTek Inc. and/or its licensors.                                     
* Without the prior written permission of MediaTek inc. and/or its licensors,                                
* any reproduction, modification, use or disclosure of MediaTek Software,                                    
* and information contained herein, in whole or in part, shall be strictly prohibited.                       
*/     
#include <ctype.h>                                                                                           
#include <errno.h>                                                                                           
#include <fcntl.h>                                                                                           
#include <getopt.h>                                                                                          
#include <limits.h>                                                                                          
#include <linux/input.h>                                                                                     
#include <stdio.h>                                                                                           
#include <stdlib.h>                                                                                          
#include <string.h>                                                                                          
#include <sys/reboot.h>                                                                                      
#include <sys/types.h>                                                                                       
#include <time.h>                                                                                            
#include <unistd.h>                                                                                                                                                                                                     
#include <cutils/properties.h>
#include "me_connection.h"
#include <pthread.h>
#include "common.h"
#include "ftm.h"
#include "miniui.h"
#include "utils.h"
#include "item.h"                                                                                         
#if defined(MTK_FTM_C2K_SUPPORT) && !defined(EVDO_FTM_DT_VIA_SUPPORT)                                                                                       
#include <c2kutils.h>
#endif
#include "at_command.h"
#define TAG        "[VERSION] "                                                                              
#define MAX_MODEM_INDEX 3  
int g_nr_lines; 
                                                                                             
extern sp_ata_data return_data;
extern char test_data[128];                                                                                  
                                                                                                          
extern int textview_key_handler(int key, void *priv);                                                        
extern int write_test_report(item_t *items, FILE *fp);                                                       
extern char SP_ATA_PASS[16];                                                                                 
//extern char SP_ATA_FAIL[16] = "fail\r\n";                                                                  
extern int get_barcode_from_nvram(char *barcode_result);                                
#define MAX_RETRY_COUNT 20


int getIMEI(int sim, Connection& modem ,char *result)
{
   char imei[128] = {0};
   int len = 0 ;
   if(sim==1) 
   {
       modem.QueryModemRevision(7, imei);	
   }
   else if(sim==2) 
   {
       modem.QueryModemRevision(10, imei);	
   }
  else if(sim==3) 
  {
      modem.QueryModemRevision(11, imei);		
  }
  else 
  {
      modem.QueryModemRevision(12, imei);	
  }  
  
  LOGD(TAG "get IMEI is %s",imei);
  
  strcpy(result,imei);
  
   if(strlen(result) <= 0)
   {
   	   LOGD(TAG "IMEI Can't read back");
       strcpy(result, "unknown");
   }
   
   LOGE("getIMEI %s",result);
   return 0 ;   
}

int getModemVersion(Connection& modem, char *result)
{
	  const int BUF_SIZE = 128;
	  char ver[BUF_SIZE] = {0};
    int len = 0 ;
    modem.QueryFWVersion(ver);
    LOGD("ver %s",ver);
    
    strcpy(result,ver);
    
    if(strlen(result) <= 0)
    {
   	    LOGD(TAG "Modemversion Can't read back");
        strcpy(result, "unknown");
    }
    
	  LOGD(TAG "getModemVersion result = %s\n", result);
	  return 0;
}

int getBarcode(Connection& modem, char *result)
{
	  const int BUF_SIZE = 128;
	  char barcode[BUF_SIZE]={0};
    int len = 0 ;
    
    if(ER_OK != modem.QueryModemRevision(5, barcode))
    return -1 ;
    
	  LOGD("Barcode %s",barcode);
    
    strcpy(result,barcode);
    
   if(strlen(result) <= 0)
   {
       strcpy(result, "unknown");
   }
   
    LOGE("getBarcode result = %s\n", result);
    return 0;    
}

int getMEID(Connection& modem, char *result)
{   
	  const int BUF_SIZE = 10000;
	  char meid[BUF_SIZE] = {0};
    char *p = NULL ;
    char *ptr = NULL ;
    
    if(ER_OK != modem.ResetConfig())
    return -1 ;
    
    if(ER_OK != modem.SetModemFunc(1))
    return -1 ;
    
    modem.QueryMEID(meid);
    LOGD("MEID: %s",meid);
    p = strchr(meid, 'x');
    if(NULL == p)
	  {                                                                                                        
	      LOGE("get MEID error,can't find 'x'");
	      return -1 ;
	  }                                                                                                        
    strcpy(result, ++p);
    ptr = strchr(result, '\n');
    if (ptr != NULL)
    {                                                                                                        
        *ptr = 0;                                                                                            
    }                                                                                                        
    else                                                                                                     
    {                                                                                                        
        LOGE("get MEID error");                                          
        return -1 ;	                                                                                         
    }                                                                                                        
    if(strlen(result) <= 0)                                                                                  
    {                                                                                                        
        strncpy(result, "unknown", strlen("unknown"));                                                       
    }                                                                                                        
    else                                                                                                     
    {                                                                                                        
        if(result[strlen(result)-1] == '\r')                                                                 
        {                                                                                                    
            result[strlen(result)-1] = 0;                                                                    
        }                                                                                                    
    }                                                                                                        
                                                                                                             
	  LOGD(TAG "getMEID result = %s\n", result);                                                       
	  return 0;                                                                                                                                                                         	
}

int getRFID(Connection& modem, char *result)
{   
	  const int BUF_SIZE = 10000;
	  char rfid[BUF_SIZE];
   
    if(ER_OK != modem.ResetConfig())
    return -1 ;
    
    if(ER_OK != modem.SetModemFunc(1))
    return -1 ;
    
    strcpy(result,rfid);
  
    if(strlen(result) <= 0)
    {
   	    LOGD(TAG "rfid Can't read back");
        strcpy(result, "unknown");
    }
                                                                                                             
	  LOGD(TAG "getRF Chip ID result = %s\n", result);                                                       
	  return 0;                                                                                                                                                                         	
}

#if 0                                                                                                                                                                                                             
int write_barcode(int fd, char* barcode)                                                                     
{                                                                                                            
    const int BUF_SIZE = 128;                                                                                
    char buf[BUF_SIZE];                                                                                      
    int result;                                                                                              
    if((fd == -1) || (barcode == NULL))                                                                      
    {                                                                                                        
        return 0;                                                                                            
    }                                                                                                        
                                                                                                             
    memset(buf, 0, BUF_SIZE);                                                                                
    sprintf(buf, "AT+EGMR=1,5,\"%s\"\r\n", barcode);                                                         
    send_at(fd, buf);                                                                                        
    LOGD(TAG "before memset\n");                                                                             
    memset(buf, 0, BUF_SIZE);                                                                                
    LOGD(TAG "after memset\n");                                                                              
//    read_ack(fd, buf, BUFSZ);                                                                              
    result = wait4_ack (fd, NULL, 3000);                                                                     
                                                                                                             
    return result;                                                                                           
}                                                                                                            
#endif
                                                                                               
void print_verinfo(char *info, int *len, char *tag, char *msg)                                               
{                                                                                                            
	char buf[256] = {0};                                                                                       
	int _len = 0;                                                                                              
	int tag_len = 0;                                                                                           
	int max_len=0;
    if((info == NULL) || (len == NULL) || (tag == NULL) || (msg == NULL))                                    
    {                                                                                                        
        return;                                                                                              
    }                                                                                                        
                                                                                                             
    _len = *len;                                                                                             
    tag_len = strlen(tag);                                                                                   
	//int max_len = gr_fb_width() / CHAR_WIDTH *2;
	#if defined SUPPORT_GB2312
	    max_len = gr_fb_width() / CHAR_WIDTH*2;
	#else
	    max_len = gr_fb_width() / CHAR_WIDTH;
	#endif

	int msg_len = strlen(msg);                                                                                 
                                                                                                             
	int buf_len = gr_fb_width() / CHAR_WIDTH;                                                                  
                                                                                                             
	_len += sprintf(info + _len, "%s", tag);                                                                   
	_len += sprintf(info + _len, ": ");                                                                        
                                                                                                             
	if(msg_len>max_len-tag_len-2)                                                                              
    {                                                                                                        
		_len += sprintf(info+_len,"\n    ");                                                                     
		g_nr_lines++;                                                                                            
	}                                                                                                          
                                                                                                             
	while(msg_len>0)                                                                                           
    {                                                                                                        
		buf_len = max_len - 4;                                                                                   
		buf_len = (msg_len > buf_len ? buf_len : msg_len);                                                       
		strncpy(buf, msg, 256);                                                                              
		buf[buf_len] = 0;                                                                                        
                                                                                                             
		_len += sprintf(info + _len, "%s", buf);                                                                 
		_len += sprintf(info + _len, "\n");                                                                      
		g_nr_lines++;                                                                                            
		msg_len-=buf_len;                                                                                        
		msg = &(msg[buf_len]);                                                                                   
		while(msg_len>0 && msg[0]==' ')                                                                          
        {                                                                                                    
			msg_len--;                                                                                             
			msg = &(msg[1]);                                                                                       
		}                                                                                                        
                                                                                                             
		if(msg_len>0)                                                                                            
        {                                                                                                    
			for(buf_len=0; buf_len < 4; buf_len++) buf[buf_len]=' ';                                               
			buf[buf_len]=0;                                                                                        
			//_len += sprintf(info+_len, buf);                                                                     
			// Fix Anroid 2.3 build error                                                                          
			_len += sprintf(info + _len, "%s", buf);                                                               
		}                                                                                                        
                                                                                                             
	}                                                                                                          
	*len = _len;                                                                                               
	//LOGD(TAG "In factory mode: g_nr_lines = %d\n", g_nr_lines);                                              
}                                                                                                                                                                                                                   
                                                                                     
static int create_md_verinfo(char *info, int *len)                                                          
{
    char ccci_path[MAX_MODEM_INDEX][32] = {0};
    char temp_ccci_path [MAX_MODEM_INDEX][32] = {0};                                                                                                        
    char imei1[64]="unknown";                                                                                      
    char imei2[64]="unknown";                                                                                      
    char imei3[128]="unknown";                                                                                     
    char imei4[128]="unknown";
    char meid[128]="unknown";
    char rfid[128]="unknown";                                                                                     
    char modem_ver[128] = "unknown";                                                                         
    char modem_ver2[128] = "unknown";                                                                        
    char barcode[128] = "unknown";                                                                           
    char barcode2[128] = "unknown";                                                                          
    Connection modem[5];                                                                                                        
    int modem_number = 0;                                                                                    
    int ccci_status = 0;                                                                                     
    int sdio_status = 0;
    int i=0;
    int j=0;  
    char * asciDevice = NULL;                                                                                                  
    modem_number = get_md_count();
    g_Flag_EIND = 0;
    g_Flag_VPUP = 0;
    
    for(i = 0; i < MAX_MODEM_INDEX; i++)
    {
        if(1 == get_ccci_path(i,temp_ccci_path[i]))
        {
            strcpy(ccci_path[j],temp_ccci_path[i]);
            j++ ;
        }
    }
    
    if(modem_number == 1)
    {   
        if(0 == modem[0].Conn_Init(ccci_path[0],1,g_SIGNAL_Callback[0]))
	      {
	          LOGD(TAG "modem 1 open failed");
	      }
	      else
	      {
	          LOGD(TAG "modem 1 open OK");	           
	          if(g_Flag_EIND != 1)
	          {
	          	   if(ER_OK!= modem[0].QueryModemStatus())
                 {
                     g_Flag_EIND = 0 ;
                     wait_URC(ID_EIND);
                 }
                 else
                 {
                 	  g_Flag_EIND = 1 ;
                 }
	          }
            usleep(50000);                                
            getIMEI(1, modem[0], imei1);                                                               
            #ifdef GEMINI                                                                                    
                getIMEI(2, modem[0], imei2);                                                           
                #if defined(MTK_GEMINI_3SIM_SUPPORT)                                                         
                    getIMEI(3,modem[0], imei3);                                                        
                #elif defined(MTK_GEMINI_4SIM_SUPPORT)                                                       
                    getIMEI(3,modem[0], imei3);                                                        
                    getIMEI(4,modem[0], imei4);                                                        
                #endif                                                                                       
            #endif                                                                                           
            getModemVersion(modem[0], modem_ver);                                                      
            getBarcode(modem[0],barcode);                                                              
  	      }                                                                     
    }                                                                                                        
    else if(modem_number == 2)                                            
    {                                                                                
        if(1 == modem[0].Conn_Init(ccci_path[0],1,g_SIGNAL_Callback[0]))
        {                                                                                                    
            LOGD(TAG "modem 1 open OK");
	          if(g_Flag_EIND != 1)
	          {
	          	  if(ER_OK!= modem[0].QueryModemStatus())
                {
                    g_Flag_EIND = 0;
	                  wait_URC(ID_EIND);
                }
                else
                {
                	  g_Flag_EIND = 1;
                }
	          }
            usleep(50000);                                                         
            getIMEI(1, modem[0], imei1);                                                               
            getModemVersion(modem[0], modem_ver);                                                      
            getBarcode(modem[0],barcode);
            g_Flag_EIND = 0 ;
        }                                                                                                    
        if(1 == modem[1].Conn_Init(ccci_path[1],2,g_SIGNAL_Callback[1]))
        {
            LOGD(TAG "modem 2 open OK");
	          if(g_Flag_EIND != 1)
	          {
	          	  if(ER_OK!= modem[1].QueryModemStatus())
                {
                    g_Flag_EIND = 0;
	                  wait_URC(ID_EIND);
                }
                else
                {
                	  g_Flag_EIND = 1;
                }
	          }
            getIMEI(1, modem[1], imei2);
            getModemVersion(modem[1], modem_ver2);
            getBarcode(modem[1],barcode2);
        }                                                                                                    
    }                                                                                                        
    else if(modem_number == 0)                                                                               
    {                                                                                                        
        LOGD(TAG "modem_number == 0\n");                                                                     
    }    
                                                                                               
    #if defined(FEATURE_FTM_WIFI_ONLY)
        get_barcode_from_nvram(barcode);                                                                     
    #endif                                                                                                   
    
   #if defined(MTK_FTM_C2K_SUPPORT) && !defined(EVDO_FTM_DT_VIA_SUPPORT)    
   asciDevice = viatelAdjustDevicePathFromProperty(VIATEL_CHANNEL_AT);
   if(1 == modem[modem_number].Conn_Init(asciDevice,4,g_SIGNAL_Callback[3]))
   {
	     if(g_Flag_VPUP != 1)
	     {
	         if(ER_OK!= modem[modem_number].QueryModemStatus())
           {
               g_Flag_VPUP = 0 ;
               wait_URC(ID_VPUP);
           }
           else
           {
           	  g_Flag_VPUP = 1 ;
           }
	     }	
       LOGD("modem c2k open successfully");
       getMEID(modem[modem_number], meid);
	     getRFID(modem[modem_number],rfid);
   }
    #endif 
    
    LOGD(TAG "[AT]CCCI port close\n");
    for(i=0;i<modem_number;i++)
    {
        if(1 == modem[i].Conn_DeInit())
        {
        	 LOGD(TAG "Deinit the port successfully\n");
        }
        else
        {
           LOGD(TAG "Deinit the port failed \n");
        }
    }
    #if defined(MTK_FTM_C2K_SUPPORT) && !defined(EVDO_FTM_DT_VIA_SUPPORT)
      LOGD(TAG "[AT]modem_number = %d,SDIO port close\n",modem_number);
      modem[modem_number].Conn_DeInit();
      LOGD(TAG "[AT]SDIO port close done \n");
    #endif
                                                                                                            
    #ifdef FEATURE_FTM_3GDATA_SMS                                                                            
    #elif defined FEATURE_FTM_3GDATA_ONLY                                                                    
    #elif defined FEATURE_FTM_WIFI_ONLY                                                                      
    #elif defined GEMINI                                                                                     
        #ifndef EVDO_FTM_DT_VIA_SUPPORT                                                                               
            print_verinfo(info, len,  "IMEI1       ", imei1);                                                
            print_verinfo(info, len,  "IMEI2       ", imei2);                                                
            #if defined(MTK_GEMINI_3SIM_SUPPORT)                                                             
                print_verinfo(info, len,  "IMEI3       ", imei3);                                            
	        #elif defined(MTK_GEMINI_4SIM_SUPPORT)                                                             
                print_verinfo(info, len,  "IMEI3       ", imei3);                                            
                print_verinfo(info, len,  "IMEI4       ", imei4);                                            
	        #endif                                                                                             
         #else                                                                                               
            print_verinfo(info, len, "IMEI        ", imei1);                                                 
         #endif                                                                                              
    #else                                                                                                    
        print_verinfo(info, len,  "IMEI        ", imei1);                                                    
    #endif  
    
    #if defined(MTK_FTM_C2K_SUPPORT) && !defined(EVDO_FTM_DT_VIA_SUPPORT)
       print_verinfo(info, len,  "MEID        ", meid);
       print_verinfo(info, len, "RFID        ", rfid);
    #endif
                                                                                                     
    if(modem_number == 1)                                                                                    
    {                                                                                                        
        print_verinfo(info, len,  "Modem Ver.  ", modem_ver);                                                
        sprintf(return_data.version.modem_ver,"%s", modem_ver);                                              
        print_verinfo(info, len,  "Bar code    ", barcode);                                                  
    }                                                                                                        
    else if(modem_number == 2)                                                                               
    {                                                                                                        
        print_verinfo(info, len,  "Modem Ver.  ", modem_ver);                                                
        sprintf(return_data.version.modem_ver,"%s", modem_ver);                                              
        print_verinfo(info, len,  "Modem Ver2.  ", modem_ver2);                                              
        print_verinfo(info, len,  "Bar code    ", barcode);                                                  
        print_verinfo(info, len,  "Bar code2    ", barcode2);                                                
    }                                                                                                        
                                                                                                             
    #if defined(FEATURE_FTM_3GDATA_SMS) || defined(FEATURE_FTM_3GDATA_ONLY) || defined(FEATURE_FTM_WIFI_ONLY)
        print_verinfo(info, len,  "Bar code    ", barcode);                                                  
    #endif                                                                                                   
    return 0;                                                                                                
}                                                                                                            

                                                                                                           
static int create_ap_verinfo(char *info, int *len)                                                           
{                                                                                                            
    char val[128] = {0};                                                                                     
    char kernel_ver[256] = "unknown";                                                                        
    char uboot_build_ver[128]  = "unknown";                                                                  
    int kernel_ver_fd = -1;                                                                                  
    int kernel_cli_fd = -1;                                                                                  
    char buffer[1024] = {0};                                                                                 
    char *ptr= NULL, *pstr = NULL;                                                                           
    int i = 0;                                                                                               
                                                                                                             
    kernel_ver_fd = open("/proc/version",O_RDONLY);                                                          
    if(kernel_ver_fd!=-1)                                                                                    
    {                                                                                                        
        read(kernel_ver_fd, kernel_ver, 256);                                                                
        close(kernel_ver_fd);                                                                                
    }                                                                                                        
                                                                                                             
    kernel_cli_fd = open("/proc/cmdline",O_RDONLY);                                                          
    if(kernel_cli_fd!=-1)                                                                                    
    {                                                                                                        
        read(kernel_cli_fd,buffer,128);                                                                      
        ptr = buffer;                                                                                        
        pstr = strtok(ptr, ", =");                                                                           
        while(pstr != NULL)                                                                                  
        {                                                                                                    
            if(!strcmp(pstr, "uboot_build_ver"))                                                             
            {                                                                                                
                pstr = strtok(NULL, ", =");                                                                  
                strcpy(uboot_build_ver, pstr);                                                               
            }                                                                                                
            pstr = strtok(NULL, ", =");                                                                      
        }                                                                                                    
        close(kernel_cli_fd);                                                                                
    }                                                                                                        
                                                                                                             
    if(uboot_build_ver[strlen(uboot_build_ver)-1]=='\n') uboot_build_ver[strlen(uboot_build_ver)-1]=0;       
    if(kernel_ver[strlen(kernel_ver)-1]=='\n') kernel_ver[strlen(kernel_ver)-1]=0;                           
                                                                                                             
    property_get("ro.mediatek.platform", val, "unknown");                                                    
    print_verinfo(info, len,  "BB Chip     ", val);                                                          
    property_get("ro.product.device", val, "unknown");                                                       
    print_verinfo(info, len,  "MS Board.   ", val);                                                          
                                                                                                             
    property_get("ro.build.date", val, "TBD");                                                               
    print_verinfo(info, len,  "Build Time  ", val);                                                          
                                                                                                             
    ptr = &(kernel_ver[0]);                                                                                  
    for(i=0;i<strlen(kernel_ver);i++)                                                                        
    {                                                                                                        
        if(kernel_ver[i]>='0' && kernel_ver[i]<='9')                                                         
        {                                                                                                    
            ptr = &(kernel_ver[i]);                                                                          
            break;                                                                                           
        }                                                                                                    
    }                                                                                                        
    print_verinfo(info, len,  "Kernel Ver. ", ptr);                                                          
    property_get("ro.build.version.release", val, "unknown");                                                
    print_verinfo(info, len,  "Android Ver.", val);                                                          
    property_get("ro.mediatek.version.release", val, "unknown");                                             
    print_verinfo(info, len,  "SW Ver.     ", val);                                                          
	sprintf(return_data.version.sw_ver,"%s", val);                                                             
	property_get("ro.custom.build.version",val,"unknown");                                                     
	print_verinfo(info, len,  "Custom Build Verno.", val);                                                     
                                                                                                             
    return *len;                                                                                              
}                                                                                                            

                                                                                                             
int create_verinfo(char *info, int size)                                                              
{                                                                                                            
                                                                                                             
    int len = 0;                                                                                             
	g_nr_lines = 0;                                                                                            
                                                                                                             
    create_ap_verinfo(info, &len);                                                                           
    create_md_verinfo(info, &len);                                                                           
                                                                                                             
    return 0;                                                                                                
}                                                                                                            
 
                                                                                                            
char ** trans_verinfo(const char *str, int *line)                                                            
{                                                                                                            
	char **pstrs = NULL;                                                                                       
	int  len     = 0;                                                                                          
	int  row     = 0;                                                                                          
	const char *start;                                                                                         
	const char *end;                                                                                           
                                                                                                             
    if((str == NULL) || (line == NULL))                                                                      
    {                                                                                                        
        return NULL;                                                                                         
    }                                                                                                        
                                                                                                             
    len = strlen(str) + 1;                                                                                   
    start  = str;                                                                                            
    end    = str;                                                                                            
    pstrs = (char**)malloc(g_nr_lines * sizeof(char*));                                                      
                                                                                                             
	if (!pstrs)                                                                                                
    {                                                                                                        
		LOGE("In factory mode: malloc failed\n");                                                                
		return NULL;                                                                                             
	}                                                                                                          
                                                                                                             
	while (len--)                                                                                              
    {                                                                                                        
		if ('\n' == *end)                                                                                        
        {                                                                                                    
			pstrs[row] = (char*)malloc((end - start + 1) * sizeof(char));                                          
                                                                                                             
			if (!pstrs[row])                                                                                       
            {                                                                                                
				LOGE("In factory mode: malloc failed\n");                                                            
				return NULL;                                                                                         
			}                                                                                                      
                                                                                                             
			strncpy(pstrs[row], start, end - start);                                                               
			pstrs[row][end - start] = '\0';                                                                        
			start = end + 1;                                                                                       
			row++;                                                                                                 
		}                                                                                                        
		end++;                                                                                                   
	}                                                                                                          
                                                                                                             
	*line = row;                                                                                               
	return pstrs;                                                                                              
}                                                                                                            
                                                                                                             
void tear_down(char **pstr, int row)                                                                         
{                                                                                                            
    int i;                                                                                                   
    if(pstr == NULL)                                                                                         
    {                                                                                                        
        return;                                                                                              
    }                                                                                                        
    for (i = 0; i < row; i++)                                                                                
    {                                                                                                        
        if (pstr[i])                                                                                         
        {                                                                                                    
            free(pstr[i]);                                                                                   
            pstr[i] = NULL;                                                                                  
        }                                                                                                    
    }                                                                                                        
	                                                                                                           
    if (pstr)                                                                                                
    {                                                                                                        
        free(pstr);                                                                                          
        pstr = NULL;                                                                                         
    }                                                                                                        
}                                                                                                            
                                                                                                             
                                                                                                             
/*                                                                                                           
    autoreturn:  if the function called by ata, then true;                                                   
    if called by main, then false;                                                                           
*/                                                                                                           
char* display_version_ata(int index, char* result)                                                           
{                                                                                                            
    if(result == NULL)                                                                                       
    {                                                                                                        
        return NULL;                                                                                         
    }                                                                                                        
    return display_version(index, result, true);	                                                           
}                                                                                                            


                                                                                                          
char* display_version(int index, char* result, bool autoreturn)                                              
{                                                                                                            
	char *buf = NULL;                                                                                          
	struct textview vi;	 /* version info */                                                                    
	text_t vi_title;                                                                                           
	int nr_line;                                                                                               
	text_t info;                                                                                               
	int avail_lines = 0;                                                                                       
	text_t rbtn;                                                                                               
                                                                                                             
	buf = (char *)malloc(BUFSZ);
                                                                                                             
	init_text(&vi_title, uistr_version, COLOR_YELLOW);                                                         
	init_text(&info, buf, COLOR_YELLOW);                                                                       
	init_text(&info, buf, COLOR_YELLOW);                                                                       
                                                                                                             
	avail_lines = get_avail_textline();                                                                        
	init_text(&rbtn, uistr_key_back, COLOR_YELLOW);                                                            
	ui_init_textview(&vi, textview_key_handler, &vi);                                                          
	vi.set_btn(&vi, NULL, NULL, &rbtn);                                                                        
  create_verinfo(buf, BUFSZ);                                                                                
	LOGD(TAG "after create_verinfo");                                                                          
	vi.set_title(&vi, &vi_title);                                                                              
	vi.set_text(&vi, &info);                                                                                   
	vi.m_pstr = trans_verinfo(info.string, &nr_line);                                                          
	vi.m_nr_lines = g_nr_lines;                                                                                
	LOGD(TAG "g_nr_lines is %d, avail_lines is %d\n", g_nr_lines, avail_lines);                                
	vi.m_start = 0;                                                                                            
	vi.m_end = (nr_line < avail_lines ? nr_line : avail_lines);                                                
	LOGD(TAG "vi.m_end is %d\n", vi.m_end);                                                                    
                                                                                                             
    if(autoreturn)                                                                                           
    {                                                                                                        
    	vi.redraw(&vi);                                                                                        
        strcpy(result, SP_ATA_PASS);                                                                         
    }                                                                                                        
    else                                                                                                     
    {                                                                                                        
        vi.run(&vi);                                                                                         
    }                                                                                                        
                                                                                                             
	LOGD(TAG "Before tear_down\n");                                                                            
	tear_down(vi.m_pstr, nr_line);                                                                             
	if (buf)                                                                                                   
    {                                                                                                        
		free(buf);                                                                                               
        buf = NULL;                                                                                          
    }                                                                                                        
    LOGD(TAG "End of %s\n", __FUNCTION__);                                                                   
                                                                                                             
    return SP_ATA_PASS;                                                                                      
}                                                                                                            
                                                                                                             

