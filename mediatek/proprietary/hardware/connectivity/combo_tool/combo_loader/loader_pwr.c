#include "../src/wmt_ioctl.h"
#include <stdio.h>
#include <errno.h>
#include <fcntl.h>
#include <unistd.h>
#include <stdlib.h>
int loader_wmt_pwr_ctrl(int on)
{
    int i_ret = -1;
    int wmt_fd;
    int para = (on == 0) ? 0 : 1;
    /* open wmt dev */
    wmt_fd = open("/dev/stpwmt", O_RDWR | O_NOCTTY);
    if (wmt_fd < 0) {
        printf("[%s] Can't open stpwmt \n", __FUNCTION__);
        return -1;
    }
    if(ioctl(wmt_fd, WMT_IOCTL_LPBK_POWER_CTRL, para) != 0)
    {
        printf("[%s] power on combo chip failed\n", __FUNCTION__);
        i_ret = -2;
    } else {
        printf("[power %s combo chip ok!]\n", para == 0 ? "off" : "on");
        i_ret = 0;
    }
    close(wmt_fd);
    wmt_fd = -1;
    
    return i_ret;
}