
#include <string.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/ioctl.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <errno.h>
#include <linux/if.h>
#include <linux/if_tun.h>

#include <android/log.h>

#define LLV_ERROR   0
#define LLV_WARNING 1
#define LLV_NOTIFY  2
#define LLV_INFO    3
#define LLV_DEBUG   4
#define LLV_DEBUG2  5

extern void plog_android(int level, char *format, ...);
extern int setkey_main(int argc, char ** argv);
