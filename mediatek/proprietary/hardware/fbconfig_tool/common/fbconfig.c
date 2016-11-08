/* (C) Copyright 2009
 * MediaTek <www.MediaTek.com>
 * Xiaokuan Shi <Xiaokuan.Shi@MediaTek.com>
 *
 * FBCONFIG TOOL
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include "fbconfig.h"
#define CHAR_PER_LINE 680// Characters per record :680=128*5+30+x;
int tmp_val[128];
int fd_conf = -1;

int data_type = 0 ;
CONFIG_RECORD  record_cmd ;
MIPI_TIMING timing;

static int file_parse(char*path);

static char _help[] =
        "\n"
        "USAGE\n"
        "                Under /system/bin and run fbconfig"
        "    For Example:    ./fbconfig [PARAMETER] \n"
        "\n"
        "PARAMETER\n"
        "       set_dsi_id\n "
        "\n"
        "       test_lcm_type \n"
        "\n"
        "       driver_ic_config \n"
        "\n"
        "       lcm_get_id \n"
        "\n"
        "       lcm_get_esd \n"
        "\n"
        "       mipi_set_clock \n"
        "\n"
        "       get_dsi_clk \n"
        "\n"
        "       mipi_set_timing \n"
        "\n"
        "       get_dsi_timing \n"
        "\n"
        "       mipi_set_non_cc \n"
        "\n"
        "       get_dsi_continuous \n"
        "\n"
        "       mipi_set_ssc \n"
        "\n"
        "       get_dsi_ssc \n"
        "\n"
        "       te_set_enable \n"
        "\n"
        "       fb_layer_dump \n"
        "\n"
        "       get_dsi_lane_num \n"
        "\n"
        "       get_dsi_te_enable \n"
        "\n"
        "       get_misc \n"
        "\n"
        "       Reserved \n"
        ;

static char mipi_help[] =
        "\n"
        "\nUsage Example: ./fbconfig mipi_set_timing HS_ZERO 23 INDEX\n"
        "TIMCON0_REG:"  "LPX" "  " "HS_PRPR" "  "  "HS_ZERO" "  "  "HS_TRAIL\n"
        "\n"
        "TIMCON1_REG:" "TA_GO" "  " "TA_SURE" "  " "TA_GET" "  " "DA_HS_EXIT\n"
        "\n"
        "TIMCON2_REG:" "CLK_ZERO" "  " "CLK_TRAIL" "  " "CONT_DET\n"
        "\n"
        "TIMCON3_REG:" "CLK_HS_PRPR" "  " "CLK_HS_POST" "  " "CLK_HS_EXIT\n"
        "\n"
        "VDO MODE :" "HPW" "  " "HFP" "  " "HBP" "  " "VPW" "  " "VFP" "  " "VBP"
        "\n"
        ;
//---------------------Driver IC Config V2 START-----------------------
int parse_line2cmd(char*line,unsigned int*cmd_buf,int*ins_num)
{
        char*p,*digit,*rec;
        static int new_cmd=0;
        unsigned int idx=0;
        if(!line || !cmd_buf)
                return -1;
        if(((p=strchr(line,'{'))!=NULL))        //a line start with '{'
        {
                if(strlen(line)>3)
                        p=p+1;          //skip '{'
                new_cmd=1;
        }else
        {
                if(strlen(line)<3)
                        return -2;
                p=line;
        }
        if(!new_cmd)
                return -3;
        cmd_buf[idx]=(unsigned int)strtoul(p,NULL,16);
        if(cmd_buf[idx]==0)
                return -2;
        printf("====>the instruction is :0x%8x\n",cmd_buf[idx]);
        idx++;
        *ins_num=idx;
        rec=p;
        digit=strtok(p,",");
        do
        {
                digit=strtok(NULL,",");
                if(digit)
                {
                        cmd_buf[idx]=(unsigned int)strtoul(digit,NULL,16);
//                      if(cmd_buf[idx]==0)
//                              return -2;
                        rec=digit;
                        printf("====>the instruction is :0x%8x\n",cmd_buf[idx]);
                        idx++;
                }
        }while(digit);
        *ins_num=idx;
        //a '}' may be in the last string,it means a instruction end
        if(strchr(rec,'}'))
        {
                new_cmd=0;
                return 0;
        }
        return idx;
}
static int driver_ic_config(int fd,char*path)
{
        FILE * file;
        char*p;
        size_t len=0;
        unsigned int*ins_buf=NULL;
        int ret =0;
        int ins_num=0;
        char tmp[128];
        unsigned int cmd=DRIVER_IC_CONFIG;
        printf("driver ic config file is: %s\n",path);
        file = fopen(path,"r");
        if(file != NULL)
        {
                if(fgets(tmp,128,file))
                {
                        if(tmp[strlen(tmp)-1]!='\n')    // only one line?
                        {
                                fclose(file);
                                file = NULL;
                                return 0;
                        }
                        tmp[strlen(tmp)-1]='\0';
                        if(!strstr(tmp,"driver_ic_config_file:v2"))     //to support v1 file type
                        {
                                fclose(file);
                                file_parse(path);
                                return 0;
                        }
                }
                ins_buf=(unsigned int*)record_cmd.ins_array;
                record_cmd.ins_num =0;
                while(fgets(tmp,128,file)!=NULL)
                {
                        if(tmp[strlen(tmp)-1]=='\n')
                                tmp[strlen(tmp)-1]='\0';
                        p=strstr(tmp,"MS:");
                        if(p)
                        {
                                int ms;
                                sscanf(p+3,"0x%x",&ms);
                                record_cmd.ins_num =1;
                                record_cmd.type = RECORD_MS ;
                                record_cmd.ins_array[0] =ms;
                                printf("msleep :%d\n",ms);
                                ioctl(fd_conf, cmd, &record_cmd);
                                record_cmd.ins_num =0;
                                continue;
                        }
                        p=strstr(tmp,"PIN:");
                        if(p)
                        {
                                int enable;
                                sscanf(p+4,"0x%x",&enable);
                                record_cmd.ins_num =1;
                                record_cmd.type = RECORD_PIN_SET ;
                                record_cmd.ins_array[0] =enable;
                                printf("pin set :%d\n",enable);
                                ioctl(fd_conf, cmd, &record_cmd);
                                record_cmd.ins_num =0;
                                continue;
                        }
                        //a command line
                        ret=parse_line2cmd(tmp,ins_buf,&ins_num);
                        if(ret>0)
                        {
                                ins_buf=ins_buf+ins_num;
                                record_cmd.ins_num = record_cmd.ins_num+ins_num;
                        }else if(ret==0)        //a instruction parsed end
                        {
                                record_cmd.type = RECORD_CMD;
                                record_cmd.ins_num = record_cmd.ins_num+ins_num;
                                ioctl(fd, cmd, &record_cmd);
                                ins_buf=(unsigned int*)record_cmd.ins_array;
                                record_cmd.ins_num =0;
                                ins_num=0;
                        }
                }
                ioctl(fd,DRIVER_IC_CONFIG_DONE,0);
                fclose(file);

                return 0;
        }else
        {
                printf("open file %s failed\r\n",path);
                return -1;
        }
}

//---------------------Driver IC Config START-----------------------

static int  format_to_instrut(void)
{
        int base_data =0 ;
        /*
          0x29==>0x2902
          0x39==>0x3902
          0x15==>0x1500
          0x05==>0x0500
        */
        switch(data_type)
        {
        case 0x39:
                base_data = 0x3902;
                break;
        case 0x29:
                base_data = 0x2902;
                break;
        case 0x15:
                base_data = 0x1500;
                break;
        case 0x05:
                base_data = 0x0500;
                break;
        default :
                printf("No such data type ,error!!");
        }

        if((base_data == 0x3902)||(base_data==0x2902))
        {
                int ins_num = 1+((tmp_val[1]+1)/4) + (((tmp_val[1]+1)%4) ? 1:0) ;//base ins+parameter instruction;
                int * ins_array = (int*)malloc(sizeof(int)*ins_num);
                ins_array[0] = ((tmp_val[1]+1)<<16 )+ base_data;    // (1)the first one instruction
                int ins_index = 1;
                int tmp_vi =2;//tmp_val_index
                printf("ins_num is %d; tmp_val[1] is %d\n",ins_num,tmp_val[1]);
                if(ins_num >1)
                {
                        ins_array[ins_index] = tmp_val[0]+ (tmp_val[tmp_vi]<<8) +(tmp_val[tmp_vi+1]<<16)+(tmp_val[tmp_vi+2]<<24) ;
                        //(2) the first-2nd instruction
                        tmp_vi = 5 ;
                        for(ins_index=2;ins_index < ((tmp_val[1]+1)/4+1); ins_index++)
                        {
                                ins_array[ins_index]= tmp_val[tmp_vi]+(tmp_val[tmp_vi+1]<<8)+(tmp_val[tmp_vi+2]<<16)+(tmp_val[tmp_vi+3]<<24);
                                tmp_vi+=4;//(3)the middle instruction;
                        }
                        if(((tmp_val[1]+1)%4 != 0)&&(tmp_val[1] >3 ))//(4) the last instruction ;
                        {
                                ins_array[ins_index]= tmp_val[tmp_vi];
                                if(tmp_vi <= tmp_val[1] )
                                        ins_array[ins_index] +=         (tmp_val[tmp_vi+1]<<8) ;
                                if(tmp_vi+1 <= tmp_val[1] )
                                        ins_array[ins_index] +=         (tmp_val[tmp_vi+2]<<16) ;
                        }
                        //print to test *****************************
                        printf("\n the ins_index is %d\n",ins_index);
                        int z ;
                        for(z=0 ; z < ins_num;z++)
                                printf("====>the instruction is :0x%x\n",ins_array[z]);


                        /***now the cmd instructions are stored in ins_array[]****/
                        record_cmd.ins_num = ins_num;
                        record_cmd.type = RECORD_CMD;
                        //record_cmd.ins_array = ins_array ;
                        memcpy(record_cmd.ins_array,ins_array,sizeof(int)*ins_num);
                        free(ins_array);
                        ins_array = NULL;
                }
                else
                {
                        printf("only one instruction to apply!!\n");
                        free(ins_array);
                        ins_array = NULL;
                        return 0 ;
                }
        }//at least 2 instructions
        else if(base_data == 0x1500)
        {
                int tmp_inst=0;
                tmp_inst = base_data + (tmp_val[0]<<16)+(tmp_val[2]<<24);
                record_cmd.ins_num = 1;
                record_cmd.type = RECORD_CMD;
                printf("====>the instruction is :0x%x\n",tmp_inst);
                record_cmd.ins_array[0]= tmp_inst ;
        }
        else if(base_data == 0x0500)
        {
                int tmp_inst=0;
                tmp_inst = base_data + (tmp_val[0]<<16);
                record_cmd.ins_num = 1;
                record_cmd.type = RECORD_CMD;
                printf("====>the instruction is :0x%x\n",tmp_inst);
                record_cmd.ins_array[0]= tmp_inst ;
        }
        return 0 ;

}

static int check_upper_case(char *tmp)
{
        int ret =0 ;
        while(*tmp != '\0')
        {
                if(*tmp=='X')
                {
                        ret = -1 ;
                        printf("\nnow tmp is %c\n",*tmp);
                        break;
                }
                tmp++;
        }
        return ret ;
}


static int convert_to_int(char *tmp)
{
        unsigned int cmd ;
        cmd = DRIVER_IC_CONFIG;
        printf("\ni want to know the record ^0^:\n%s",tmp);
        if(strncmp(tmp,"TYPE",4)==0)
        {
                // it's cmd line ,parse it !!
                //format : CMD:ADDR:NUM:{PAR,PAR,.....}
                int addr;
                int num_par;
                sscanf(tmp+5,"0x%x",&data_type);
                memset(tmp_val,0x00,sizeof(int)*128);
                sscanf(tmp+14,"0x%x",&addr);
                sscanf(tmp+19,"0x%x",&num_par);
                tmp_val[0] = addr ;
                tmp_val[1] = num_par ;
                printf("\nAddr is 0x%x\nNum_par is 0x%x\n",addr,num_par);
                tmp=tmp+23;//here is ":{"
                if(strncmp(tmp,":{",2)==0)//check !!
                        printf("till now all is right ! and next is cmd value!\n");
                else
                        printf("something is wrong before cmd value!! check please\n");

                int size = strlen(tmp);
                printf("the size of tmp[] is %d\n",size);
                int j=0;
                int n=2;
                tmp=tmp+2;
                while((strncmp(tmp,"}",1)!=0))//&&(j<size))
                {
                        sscanf(tmp,"0x%x",&tmp_val[n]);
                        printf("data value is 0x%x\n",tmp_val[n]);
                        n++;
                        j+=5;
                        tmp+=5;
                }

                format_to_instrut();
                ioctl(fd_conf, cmd, &record_cmd);

        }
        else if(strncmp(tmp,"MS",2)==0)
        {
                // not cmd line ;
                int ms;
                sscanf(tmp+3,"0x%x",&ms);
                record_cmd.ins_num =1;
                record_cmd.type = RECORD_MS ;
                record_cmd.ins_array[0] =ms;
                printf("run here -->msleep :%d\n",ms);
                ioctl(fd_conf, cmd, &record_cmd);
        }
        else if(strncmp(tmp,"PIN",3)==0)
        {
                int enable;
                sscanf(tmp+4,"0x%x",&enable);
                record_cmd.ins_num =1;
                record_cmd.type = RECORD_PIN_SET ;
                record_cmd.ins_array[0] =enable;
                printf("run here -->pin set :%d\n",enable);
                ioctl(fd_conf, cmd, &record_cmd);
        }
        return 0;
}

static int file_parse(char*path)
{
        FILE * filed;

        char tmp[CHAR_PER_LINE];
        int test =0;
        int ret =0;
        printf("config file is: %s\n",path);
        filed = fopen(path,"r");
        if(filed != NULL)
        {
                while(fgets(tmp,CHAR_PER_LINE,filed)!=NULL)
                {
                        ret=check_upper_case(tmp);
                        if(ret !=0)
                        {
                                printf("Error!!!there is a upper case 'X' in config file \nLine: %s\n",tmp);
                                break;
                        }else
                                convert_to_int(tmp);//parsing the record to tmp_val[128];
                }
                fclose(filed);

                return 0;

        }else
        {
                printf("can not open file in:%s",path);
                return 0 ;
        }
}

//---------------------Driver IC Config END-----------------------

static void check_mipi_type(char * type)
{
        if(!strcmp(type,"HS_PRPR"))
                timing.type = HS_PRPR;
        else if(!strcmp(type,"HS_ZERO"))
                timing.type = HS_ZERO;
        else if(!strcmp(type,"HS_TRAIL"))
                timing.type = HS_TRAIL;
        else if(!strcmp(type,"TA_GO"))
                timing.type = TA_GO;
        else if(!strcmp(type,"TA_SURE"))
                timing.type = TA_SURE;
        else if(!strcmp(type,"TA_GET"))
                timing.type = TA_GET;
        else if(!strcmp(type,"DA_HS_EXIT"))
                timing.type = DA_HS_EXIT;
        else if(!strcmp(type,"CLK_ZERO"))
                timing.type = CLK_ZERO;
        else if(!strcmp(type,"CLK_TRAIL"))
                timing.type = CLK_TRAIL;
        else if(!strcmp(type,"CONT_DET"))
                timing.type = CONT_DET;
        else if(!strcmp(type,"CLK_HS_PRPR"))
                timing.type = CLK_HS_PRPR;
        else if(!strcmp(type,"CLK_HS_POST"))
                timing.type = CLK_HS_POST;
        else if(!strcmp(type,"CLK_HS_EXIT"))
                timing.type = CLK_HS_EXIT;
        else if(!strcmp(type,"HPW"))
                timing.type = HPW;
        else if(!strcmp(type,"HFP"))
                timing.type = HFP;
        else if(!strcmp(type,"HBP"))
                timing.type = HBP;
        else if(!strcmp(type,"VPW"))
                timing.type = VPW;
        else if(!strcmp(type,"VFP"))
                timing.type = VFP;
        else if(!strcmp(type,"VBP"))
                timing.type = VBP;
        else if(!strcmp(type,"LPX"))
                timing.type = LPX;
        else if(!strcmp(type,"SSC_EN"))
                timing.type = SSC_EN;
        else
                printf("No such mipi timing control option!!\n");

}

static PM_LAYER_INFO get_layer_size(int layer_id)
{
//int size =0 ;
        int cmd = FB_LAYER_GET_INFO ;
        PM_LAYER_INFO tmp;
        tmp.layer_size=0;
        tmp.index= layer_id ;
        tmp.fmt = 0 ;
        printf("[LAYER_DUMP]layer_id is %d\n",layer_id);

        ioctl(fd_conf, cmd, &tmp);
        return tmp ;
}

static int fbconfig_layer_dump(int layer_id)
{
        void* base = NULL;
        void * tmp_base = NULL;
        BMF_HEADER bmp_file ;
        int fd_store= -1 ;
        int cmd = FB_LAYER_DUMP ;
        char store_path[30];
        PM_LAYER_INFO layer_info;
        int i ;
        sprintf(store_path,"%s%d%s", "/data/layer",layer_id,"_dump.bmp");
        fd_store = open(store_path,O_WRONLY | O_CREAT,0644);
        if(fd_store <0)
        {
                printf("[LAYER_DUMP]create /data/lay_dump fail !!\n");
                return -1 ;
        }
        memset(&layer_info,0,sizeof(PM_LAYER_INFO));
        memset(&bmp_file,0,sizeof(bmp_file));

        layer_info= get_layer_size(layer_id);
        printf("[LAYER_DUMP]layer%d size is %dbyte\n",layer_id ,layer_info.layer_size);

        if(layer_info.layer_size >0)
        {
                bmp_file.type = 0x4D42 ; //'BM'
                bmp_file.fsize= layer_info.layer_size + 54;
                bmp_file.res1 = 0;
                bmp_file.res2 = 0;
                bmp_file.offset = 54;//40+14 bytes
                //below 40 bytes are for BMP INFO HEADER
                bmp_file.this_struct_size = 0x28;
                bmp_file.width= (layer_info.layer_size)/(layer_info.height)/(layer_info.fmt/8);
                bmp_file.height = layer_info.height ;
                bmp_file.planes = 0x01;
                bmp_file.bpp = layer_info.fmt ;//32
                bmp_file.compression = 0x0;
                bmp_file.raw_size = layer_info.layer_size;
                bmp_file.x_per_meter = 0x0ec4;
                bmp_file.y_per_meter = 0x0ec4;
                bmp_file.color_used = 0x0;
                bmp_file.color_important = 0x0;

                printf("[LAYER_DUMP]size of bmp_file is %lu\n",sizeof(bmp_file));
                printf("[LAYER_DUMP]file size  is 0x%x\n",(layer_info.layer_size+54));
                printf("[LAYER_DUMP]raw size  is 0x%x\n",layer_info.layer_size);
                printf("[LAYER_DUMP]height  is %d\n",layer_info.height);
                printf("[LAYER_DUMP]bpp  is %d\n",layer_info.fmt);

                base = malloc(layer_info.layer_size);
                if(base == NULL)
                {
                        printf("[LAYER_DUMP]malloc for layer dump fail !!!\n");
                        close(fd_store);
                        return -1 ;
                }
                tmp_base = base ;
                //memcpy(base,&bmp_file,sizeof(bmp_file));
                printf("[LAYER_DUMP]malloc :0x%lx \n",(unsigned long)base);
                if(ioctl(fd_conf, cmd, base)!=0)
                {
                        printf("[LAYER_DUMP]ioctl memcpy fail !!!\n");
                        close(fd_store);
                        free(base);
                        tmp_base = base = NULL;
                        return -2 ;
                }
                write(fd_store, &bmp_file, sizeof(bmp_file));//write BMF header to bmp file ;
                base = base + layer_info.layer_size; // reposition pointer ;
                for(i=0;i<=layer_info.height;i++)//  write raw data to bmp file;
                {
                        base =  base -  bmp_file.width*(layer_info.fmt/8);
                        write(fd_store, base, bmp_file.width*(layer_info.fmt/8));
                }
                free(tmp_base);
                tmp_base = base = NULL ;
                close(fd_store);
                return 0 ;
        }else
        {
                close(fd_store);
                return -2;
        }
}
#define FBCONFIG_FILE_PATH      "/sys/kernel/debug/fbconfig"
int main (int argc, char **argv)
{
        unsigned int cmd ;
        int ret=0;
        int i=0;
        char * tmp ;
        if (argc <2)
        {
                printf("%s",_help);
                return -1;
        }
/*dump command list*/
        for(i=0;i<argc;i++)
                printf("%s ",argv[i]);
        printf("\r\n");

        fd_conf = open(FBCONFIG_FILE_PATH, O_RDWR);
        if(fd_conf <=0)
        {
                printf(" \n***open %s Fail!!*** \n",FBCONFIG_FILE_PATH);
                return -1;
        }
        if(!strcmp(argv[1],"get_dsi_id"))
        {
                int dsi_id=-1;
                ret= ioctl(fd_conf, GET_DSI_ID,&dsi_id);
                printf("get_dsi_id:%d",dsi_id);
        }else if(!strcmp(argv[1],"set_dsi_id"))
        {

                ret= ioctl(fd_conf, SET_DSI_ID, atoi(argv[2]));
        }else if(!strcmp(argv[1],"lcm_get_id"))
        {
                int id_num = 0 ;
                cmd = LCM_GET_ID;
                ret=ioctl(fd_conf, cmd, &id_num);
                printf("lcm_get_id :%d",id_num);
                if(id_num == 0)
                        printf("\n====please make sure you have implemented get_lcm_id() in lcm driver==");
        }else if(!strcmp(argv[1],"driver_ic_config"))
        {
                //lcm driver IC config ,this will parse config file and process in lcm driver .
                if(argc !=3)
                {
                        printf("Usage:fbconfig driver_ic_config [file path]\n");
                        close(fd_conf);
                        return 0 ;
                }
                else
                {
                        char path[128] ={0};
                        int x ;
                        if(strlen(argv[2])<120)
                        {
                                sprintf(path,"/data/%s",argv[2]);
                                memset(&record_cmd,0x00,sizeof(CONFIG_RECORD));
//                              file_parse(path);
                                driver_ic_config(fd_conf,path);
                        }
                        else
                        {
                                printf("\nThe name of config file is too long !!\n");
                                return 0;
                        }
                }
        }else if (!strcmp(argv[1],"mipi_set_clock"))
        {
                if(argc !=3)
                        printf("\nUsage: <./fbconfig mipi_set_clock CLOCK>\n");
                else
                {
                        unsigned int clock = atoi(argv[2]);
                        cmd = MIPI_SET_CLK ;
                        printf("mipi_set_clock :%d",clock);
                        ioctl(fd_conf, cmd, &clock);
                }
        }else if (!strcmp(argv[1],"mipi_set_ssc"))
        {
                if(argc !=3)
                        printf("\nUsage: <./fbconfig mipi_set_ssc SSC_RANGE>\n");
                else
                {
                        unsigned int ssc = atoi(argv[2]);
                        //maybe need deal with INDEX(dsi0,dsi1....)later,but not now
                        printf("mipi_set_ssc=>dsi:%d",ssc);
                        ret=ioctl(fd_conf, MIPI_SET_SSC, &ssc);
                }
        }else if (!strcmp(argv[1],"mipi_set_timing"))
        {
                if(argc !=4)
                {
                        printf("%s\n",mipi_help);
                        goto end;
                }
                timing.value= atoi(argv[3]);
                check_mipi_type(argv[2]);
                printf("mipi_set_timing :type is %d;value is %d\n",timing.type,timing.value);
                if(ioctl(fd_conf, MIPI_SET_TIMING, &timing)!= 0)
                {
                        printf("==Error !! Do you have ever put your phone in suspend mode ?==\n");
                        printf("==Please make sure your phone NOT in suspend mode!!");
                }
        }else if (!strcmp(argv[1],"mipi_set_non_cc"))
        {
                if(argc==3)
                {
                        unsigned int cc_en = atoi(argv[2]);
                        printf("\nmipi_set_non_cc =>dsi:%d",cc_en);
                        ret=ioctl(fd_conf,MIPI_SET_CC, &cc_en);
                }else
                        printf("\nUsage: <./fbconfig mipi_set_non_cc [value]\n");
        }else if (!strcmp(argv[1],"fb_layer_dump"))
        {
                PM_LAYER_EN layers;
                int get_info_ok = 0;
                misc_property misc;

                if(ioctl(fd_conf,FB_LAYER_GET_EN, &layers) == 0)
                {
                        if (ioctl(fd_conf,FB_GET_MISC, &misc) == 0)
                        {
                                printf("The current layer enable/disable info is :\n");
                                int i;
                                for (i = 0; i < misc.overall_layer_num; ++i)
                                        printf("layer_%d:%s\n", i, (layers.layer_en[i]==1)?"Enable":"Disable");
                                get_info_ok = 1;
                        } else
                        {
                                printf("get layer info failed.\n");
                        }
                }

                if (get_info_ok && argc >= 3)
                {
                        int layer_id= atoi(argv[2]);
                        if(layer_id >= misc.overall_layer_num)
                        {
                                printf("Currently we only have totally %d layer to be dumped !!", misc.overall_layer_num);
                        } else
                        {
                                if (layers.layer_en[layer_id])
                                {
                                        int ret = fbconfig_layer_dump(layer_id);
                                        if(ret ==0)
                                                printf("Layer dump Correctly!!");
                                        else
                                                printf("Layer dump Fail!!");
                                } else
                                {
                                        printf("Layer %d currently is not available.\n", layer_id);
                                }
                        }
                } else
                {
                        printf("\nUsage: <./fbconfig fb_layer_dump LAYER_ID> to dump LAYER_ID\n");
                }
        }else if (!strcmp(argv[1],"get_dsi_continuous"))
        {
                unsigned int continue_clk=0;
                ioctl(fd_conf, LCM_GET_DSI_CONTINU, &continue_clk);
                printf("get_dsi_continuous=>dsi:%d\n",continue_clk);
        }else if (!strcmp(argv[1],"get_dsi_clk"))
        {
                unsigned int dsi_clk=0;
                ret=ioctl(fd_conf, LCM_GET_DSI_CLK, &dsi_clk);
                printf("get_dsi_clk=>dsi:%d\n",dsi_clk);
        }else if (!strcmp(argv[1],"test_lcm_type"))
        {
                LCM_TYPE_FB lcm_fb ;
                lcm_fb.clock =0;
                lcm_fb.lcm_type =0;
                ret=ioctl(fd_conf, LCM_TEST_DSI_CLK, &lcm_fb);
                printf("get_dsi_type ==>clk:%d \n",lcm_fb.clock);
                /*
                  {
                  CMD_MODE = 0,
                  SYNC_PULSE_VDO_MODE = 1,
                  SYNC_EVENT_VDO_MODE = 2,
                  BURST_VDO_MODE = 3

                */
                switch(lcm_fb.lcm_type)
                {
                case 0:
                        printf("get_dsi_type ==> CMD_MODE\n");
                        break;
                case 1:
                        printf("get_dsi_type ==> SYNC_PULSE_VDO_MODE \n");
                        break;
                case 2:
                        printf("get_dsi_type ==> SYNC_EVENT_VDO_MODE\n");
                        break;
                case 3:
                        printf("get_dsi_type ==> BURST_VDO_MODE\n");
                        break;
                default :
                        printf("get_dsi_type ==> Error: no such type!!\n");
                        break;
                }
        }
        else if (!strcmp(argv[1],"get_dsi_ssc"))
        {
                unsigned int ssc=0;
                ret=ioctl(fd_conf, LCM_GET_DSI_SSC, &ssc);
                printf("get_dsi_ssc=> ssc:%d\n",ssc);
        }else if (!strcmp(argv[1],"get_dsi_lane_num"))
        {
                unsigned int dsi_lane_num =0 ;
                ret=ioctl(fd_conf, LCM_GET_DSI_LANE_NUM, &dsi_lane_num);
                printf("get_dsi_lane_num=>dsi:%d\n",dsi_lane_num);
        }else if (!strcmp(argv[1],"get_dsi_timing"))
        {
                if(argc !=3)
                        printf("\nUsage: <./fbconfig get_dsi_timing VBP>\n");
                else
                {
                        check_mipi_type(argv[2]);
                        timing.value=0;
                        ret=ioctl(fd_conf, LCM_GET_DSI_TIMING, &timing);
                        printf("get_dsi_timing==>%s:%d\n",argv[2],timing.value);
                }
        }else if (!strcmp(argv[1],"get_dsi_te_enable"))
        {
                int dsi_te_enable =0 ;
                ret=ioctl(fd_conf, LCM_GET_DSI_TE, &dsi_te_enable);
                printf("get_dsi_te_enable:%d\n",dsi_te_enable);
        }else if(!strcmp(argv[1],"te_set_enable"))
        {
                int dsi_te_enable=atoi(argv[2]);
                ret=ioctl(fd_conf, TE_SET_ENABLE, &dsi_te_enable);
                printf("get_dsi_te_enable:%d\n",dsi_te_enable);
        }else if(!strcmp(argv[1],"lcm_get_esd"))
        {
                unsigned int addr,para_num,type;
                ESD_PARA esd_para;
                if(argc!=5)
                {
                        printf("./fbconfig lcm_get_esd [address] [type] [paramter num]\r\n");
                        return -1;
                }
                printf("%s %s %s %s %s\r\n",argv[0],argv[1],argv[2],argv[3],argv[4]);
                addr=(unsigned int)strtoul(argv[2],NULL,16);
                type = strtoul(argv[3],NULL,16);
                para_num = strtoul(argv[4],NULL,16);
                if(para_num>4)
                {
                        printf("the para_num must less than 4!!\n");
                        return 0;
                }
                printf("lcm_get_esd:addr=0x%x type=%d para_num=%d\n",addr,type,para_num);
                printf("lcm_get_esd:type==0 means:DCS Read;  type==1 means GERNERIC READ\n");

                esd_para.addr = addr ;
                esd_para.type= type ;
                esd_para.para_num = para_num ;
                esd_para.esd_ret_buffer =malloc(sizeof(char)*(para_num+6));
                if(esd_para.esd_ret_buffer == NULL)
                        goto end;
                memset(esd_para.esd_ret_buffer,0,(para_num+6));
                if(ioctl(fd_conf,LCM_GET_ESD, &esd_para)==0)
                {
                        int i ;
                        for(i=0;i<(para_num+6);i++)
                                printf("\nLCM_GET_ESD:esd_get[%d]==>0x%x\n",i,esd_para.esd_ret_buffer[i]);
                }else
                        printf("Something WRONG in LCM_GET_ESD\n");
                free(esd_para.esd_ret_buffer);
        }else if(!strcmp(argv[1],"driver_ic_reset"))
        {
                if(argc != 2)
                {
                        printf("\nUsage: <./fbconfig driver_ic_reset > \n");
                        close(fd_conf);
                        return 0 ;
                }else
                {
                        printf("\nIn order to Reset Driver IC config to lcm_init setting\n");
                        ioctl(fd_conf, DRIVER_IC_RESET, NULL);
                }
        }else if (!strcmp(argv[1],"get_misc"))
        {
                misc_property misc;
                ioctl(fd_conf, FB_GET_MISC, &misc);
                printf("get_misc: 0x%08x\n", *(unsigned int *)&misc);
                printf("reserve : %08x\n", misc.reserved);
                printf("dual_port: %d\n", misc.dual_port);
        }else
        {
                printf("parameter is not correct !!%s",_help);
        }
end:
        close(fd_conf);
        printf(" \n***finish for this query or setting ret=%d*** \n",ret);

        return 0;
}

