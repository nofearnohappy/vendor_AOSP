#include <fcntl.h>
#include <string.h>
#include <errno.h>



#include "ExternalFunction.h"
#include "LogDefine.h"

/* include linux framebuffer header */
#include <sys/ioctl.h>
#include <linux/fb.h>
#include <linux/mtkfb.h>

//#ifdef _META_LV_AUTOK_   //Owner:WCD/OSS10/SS7 Juju Sung

#include <sys/stat.h>
#include<arpa/inet.h>
#include <poll.h> 
#include <dirent.h>
#define AUTOK_BUF_LEN 512
#define AUTOK_PORT 28794
#define AUTOK_LV_TIMEOUT 12000
#define AUTOK_RES_PATH   "/data"
#define AUTOK_S_BUF_LEN 256

static int is_file_valid(const char *fname)
{
    struct stat fstat;
    if (lstat(fname,&fstat)==-1)
        return -ENFILE;
    if (!S_ISREG(fstat.st_mode))
        return -ENOENT;
    if(!fstat.st_size)
        return -ENOSPC;
    return fstat.st_size;
}

static int is_lv_done_before(const char * pVol)
{
    DIR *autok_data_dir;
    struct dirent *autok_file;
    char *filename;
    static char sname[AUTOK_S_BUF_LEN] = "";
    
    if(strlen(sname) > 0){
        if(is_file_valid(sname) > 0){
            META_LOG("[AUTOK]Cache LV Done");
            return true;
        }
        else {
            META_LOG("[AUTOK]Cache LV not done");
            memset(sname, 0, AUTOK_S_BUF_LEN);
            return false;
        }
    }
    autok_data_dir = opendir(AUTOK_RES_PATH);
    while((autok_file = readdir(autok_data_dir)) != NULL) {
        filename = autok_file->d_name;
        if(strstr(filename, "autok")!= NULL &&
           strstr(filename, pVol)!=NULL && 
            strstr(filename, "_log")==NULL){
                memset(sname, 0, AUTOK_S_BUF_LEN);
                snprintf(sname, AUTOK_S_BUF_LEN, "%s/%s", AUTOK_RES_PATH, filename);
                META_LOG("[AUTOK]Non-Cached LV check exist");
                closedir(autok_data_dir);
                return true;
            }
    }
    closedir(autok_data_dir);
    return false;
}

static int should_do_dvfs_autok()
{
    int fd = 0 ;
    char buf[AUTOK_BUF_LEN] = {0};
    int ret = -1 ;
    int i = 0;
    char * p = NULL;
    fd = open("/sys/power/vcorefs/pwr_ctrl",O_RDONLY);
    if(fd < 0){
        META_LOG("[AUTOK]Open /sys/power/vcorefs/pwr_ctrl error");
        return 0;
    }
    ret = read(fd, buf, (AUTOK_BUF_LEN - 1));
    close(fd);
    if(ret < 0){
        META_LOG("[AUTOK]Read suggest_vol file error !");
        return 0;
    }     
    p = strstr(buf, "lv_autok_trig = 1");
    if(p!=NULL)
        return 1;  
    else
        return 0;
}

static int turn_off_6630()
{
    char *off_cmd[5] = {"7 0 0", "7 1 0", "7 2 0", "7 3 0", "7 5 0"};
    const char *wmt_node = "/proc/driver/wmt_dbg";
    int fd_wmt = -1;
    char cmd_line[AUTOK_BUF_LEN] = "";
    int i;
    int ret;
    
    ret = 0;
    fd_wmt = open(wmt_node, O_RDWR, 0);
    if(fd_wmt < 0){
        META_LOG("[AUTOK]Open %s fail", wmt_node);
        return -1;
    }
    for(i=0; i<sizeof(off_cmd)/sizeof(off_cmd[0]); i++){
        ret = write(fd_wmt, off_cmd[i], strlen(off_cmd[i]));
        if(ret < 0){
            META_LOG("[AUTOK]Write %s to %s error[%d]", off_cmd[i], wmt_node, ret);
            goto EXIT_TURN_OFF_6630;
        }
        ret = 0;
    }
EXIT_TURN_OFF_6630:
    if(fd_wmt > -1)
        close(fd_wmt);
    return ret;
}

static int restore_meta_screen()
{
#ifdef MTKFB_META_SHOW_BOOTLOGO
    int fb_fd = open("/dev/graphics/fb0", O_RDWR);
    if(fb_fd >= 0){
        ioctl(fb_fd, MTKFB_META_SHOW_BOOTLOGO, 0);
        close(fb_fd);
    }else{
        META_LOG("fail to open display driver error:%d, errorstr:%s",
        errno, strerror(errno));
    }
#endif
    return 0;  
}

int FT_UtilLVAutok()
{
    char *pvol = "1000000";
    char *s_backlight_on = "200";
    char *s_backlight_off = "0";
    char *s_state_mem = "mem";
    char *s_state_on = "on";
    int ret = 0;
    int result = 0;
    int fd_backlight = -1;
    int fd_suspend = -1;
    int socket_desc = -1;
    int client_sock = 0;
    struct pollfd pfd[1];
    int c = 0 ;
    int poll_res = 0;
    int read_size = 0 ;
    int sock_opt = 1;
    struct sockaddr_in server = {0} ;
    struct sockaddr_in client = {0} ;
    bool is_cali_done_before = false ;
    bool is_suspend_before = false;
    bool is_backlight_off_before = false;
        
    // Backward compatible:[95:yes][82/92:no]
    if(should_do_dvfs_autok()== 0){
        META_LOG("[AUTOK]No need to do LV autok");
        result = 0;
        goto EXIT_FT_UtilLVAutok;
    }      
    is_cali_done_before = is_lv_done_before(pvol);
    if(is_cali_done_before){
        META_LOG("[AUTOK]LV is calibrated before !");
        result = 0;
        goto EXIT_FT_UtilLVAutok;
    }
    result = turn_off_6630();
    if(result < 0){
        META_LOG("[AUTOK]Turn off 6630 Fail!");
        goto EXIT_FT_UtilLVAutok;
    }
    META_LOG("[AUTOK]Turn off 6630 Done!");
    
    //Create socket
    socket_desc = socket(AF_INET , SOCK_STREAM , 0);
    if (socket_desc == -1){
        META_LOG("[AUTOK]Could not create socket");
        result = -2;
        goto EXIT_FT_UtilLVAutok;
    }
    META_LOG("[AUTOK]Socket created");      
    //Prepare the sockaddr_in structure
    server.sin_family = AF_INET;
    server.sin_addr.s_addr = INADDR_ANY;
    server.sin_port = htons(AUTOK_PORT);   
        
    // SET SOCKET REUSE Address
    if(setsockopt(socket_desc, SOL_SOCKET, SO_REUSEADDR, (void*)&sock_opt, sizeof(sock_opt) ) < 0){
        result = -3;
        goto EXIT_FT_UtilLVAutok;
    }
       
    //Bind
    if((ret = bind(socket_desc,(struct sockaddr *)&server , sizeof(server))) < 0){
        META_LOG("[AUTOK]bind failed. Error[%d]", ret);
        result = -4;
        goto EXIT_FT_UtilLVAutok;
    }
    META_LOG("[AUTOK]bind done");          
    //Listen
    listen(socket_desc , 3);      
    //Accept and incoming connection
    META_LOG("[AUTOK]Waiting for incoming connections...");
    c = sizeof(struct sockaddr_in);    
    fd_backlight = open("/sys/class/leds/lcd-backlight/brightness", O_RDWR, 0);
    if(fd_backlight < 0){
        META_LOG("[AUTOK]Open fd_backlight file error : %d",fd_backlight);
        result = -5;
        goto EXIT_FT_UtilLVAutok;
    }    
    fd_suspend = open("/sys/power/state", O_RDWR, 0);
    if(fd_suspend < 0){
        META_LOG("[AUTOK]Open fd_suspend file error : %d",fd_suspend);
        result = -6;
        goto EXIT_FT_UtilLVAutok;
    }    
    META_LOG("[AUTOK]Open backlight and suspend file done !");
    if(!is_cali_done_before){
        META_LOG("[AUTOK]1.0v isn't calibration before!");
        ret = write(fd_backlight, s_backlight_off, strlen(s_backlight_off));
        if(ret < 0){
            META_LOG("[AUTOK]write fd_backlight file off error : %d",ret);
            result = -7;
            goto EXIT_FT_UtilLVAutok;
        } 
        is_backlight_off_before = true;
        META_LOG("[AUTOK]Backlight off done !");
        ret = write(fd_suspend, s_state_mem, strlen(s_state_mem));
        if(ret < 0){
            META_LOG("[AUTOK]write fd_suspend file off error : %d",ret);
            result = -8;
            goto EXIT_FT_UtilLVAutok;
        }
        is_suspend_before = true;
        META_LOG("[AUTOK]Power state to off done !");
        pfd[0].fd = socket_desc;
        pfd[0].events = POLLIN;
        poll_res = poll(pfd, (nfds_t)1, AUTOK_LV_TIMEOUT); // Wait autok LV done for 15s
        switch(poll_res){
            case 0:
                META_LOG("[AUTOK]Wait autok lv done timeout");
                result = -9;
                break;
            case -1:
                META_LOG("[AUTOK]Poll Error");
                result = -10;
                break;
            default:
                client_sock = accept(socket_desc, (struct sockaddr *)&client, (socklen_t*)&c);
                if(client_sock > -1){
                    META_LOG("[AUTOK]Connection accepted");
                    is_cali_done_before = is_lv_done_before(pvol);
                    if(!is_cali_done_before){
                        META_LOG("[AUTOK]Calibrate Autok LV Fail!");
                        result = -11;
                    } else {
                        META_LOG("[AUTOK]LV is calibrated done");
                        result = 0;
                    }                      
                }else {
                    META_LOG("[AUTOK]accept failed");
                    result = -11;
                }
                break;
        }
    } else {
        result = 0;
    }
    
EXIT_FT_UtilLVAutok:
    if(is_suspend_before){
    	if(!(fd_suspend < 0)){
	        ret = write(fd_suspend, s_state_on, strlen(s_state_on));
	        if(ret < 0){
	            META_LOG("[AUTOK]write fd_suspend file on error : %d",ret);
	            result = -12;
	        } else
	            META_LOG("[AUTOK]Power state to on done !");
	}
    }
    
    if(is_backlight_off_before){
    	if(!(fd_backlight < 0)){
	        ret = write(fd_backlight, s_backlight_on, strlen(s_backlight_on));
	        if(ret < 0){
	          META_LOG("[AUTOK]write fd_backlight file on error : %d",ret);
	          result = -13;
	        } else
	            META_LOG("[AUTOK]Backlight on done !");
	}
    }
    
    if(is_suspend_before || is_backlight_off_before){
        if((ret = restore_meta_screen())<0){
            META_LOG("[AUTOK]restore to meta background error : %d", ret);
            result = -14;
        } else
            META_LOG("[AUTOK]Restore meta background done !");
    }
    
    if(fd_suspend > -1)
        close(fd_suspend);
    if(fd_backlight > -1)
        close(fd_backlight);
    if(socket_desc > -1)
        close(socket_desc);
    if(client_sock > -1)
        close(client_sock);
    return result;
}
//#endif
/////////////////////////////////////////////////////////////////////////////////////////


