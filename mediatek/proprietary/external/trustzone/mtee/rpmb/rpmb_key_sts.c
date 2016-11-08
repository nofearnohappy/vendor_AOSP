#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <string.h>
#include <stdio.h>
#include <unistd.h>
#include <errno.h>
#include "tz_cross/ta_rpmb.h"
#include "rpmb.h"

#define RPMB_DEV_NODE "/dev/block/mmcblk0rpmb"

int main(int argc, char *argv[])
{
    int fd;
    int ret;
    rpmb_pkt_t pkt;
    char *result[8] = {
     "Operation OK",
     "General failure",
     "Authentication failure (MAC comparison not matching)",
     "Counter failure (counter not matching in comparison)",
     "Address failure (address out of range, wrong address alignment)",
     "Write failure (data/counter/result write failure)",
     "Read failure (data/counter/result read failure)",
     "Authentication key not yet programmed"
    };

    fd = open(RPMB_DEV_NODE, O_RDWR);
    if (fd <= 0) {
        fprintf(stderr, "open %s fail!, errno=%d (%s)\n", RPMB_DEV_NODE,
                errno, strerror(errno));
        return -1;
    }

    memset(&pkt, 0, sizeof(pkt));
    pkt.u2ReqResp = 0x2;
    ret = EmmcRpmbReadCounter(fd, &pkt);
    if (0 != ret)
    {
        fprintf(stderr, "ReadCounter return failure (%d)!, errno=%d (%s)\n",
                ret, errno, strerror(errno));
        return -2;
    }

    printf("RPMB status:\n%s\n", result[(pkt.u2Result>7)?(1):pkt.u2Result]);
    close(fd);
    return 0;
}
