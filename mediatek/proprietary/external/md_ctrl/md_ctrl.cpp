#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <logwrap/logwrap.h>
#include <cutils/properties.h>

#define LOG_TAG "md_ctrl"
#include <cutils/log.h>

static void
usage(void)
{
    fprintf(stderr, "%s\n%s\n",
        "usage: md_ctrl 0,   stop modem",
        "       md_ctrl 1,   start modem");
    exit(1);
}

void stop_modem(){
    int fd;
    const char *args[3];
    int rc;
    int status;

    SLOGD("Use muxreport to stop modem\n");

    args[0] = "/system/bin/muxreport";
    args[1] = "3";
    rc = android_fork_execvp(2, (char **)args, &status, false,
            true);
    if (rc != 0) {
        SLOGE("stop md1 failed due to logwrap error");
    }

    args[1] = "7";
    rc = android_fork_execvp(2, (char **)args, &status, false,
            true);
    if (rc != 0) {
        SLOGE("stop md2 failed due to logwrap error");
    }
}

void start_modem(){
    int fd;
    const char *args[3];
    int rc;
    int status;
    SLOGD("Just use muxreport to start modem\n");

    args[0] = "/system/bin/muxreport";
    args[1] = "4";
    rc = android_fork_execvp(2, (char **)args, &status, false,
            true);
    if (rc != 0) {
        SLOGE("start md1 failed due to logwrap error");
    }

    args[1] = "8";
    rc = android_fork_execvp(2, (char **)args, &status, false,
            true);
    if (rc != 0) {
        SLOGE("start md2 failed due to logwrap error");
    }
}

int
main(int argc, char **argv)
{
    int ret = 0, erg;
    int ch;

    if (argc!=2) {
        usage();
    }
    if (*argv[1] !='0' && *argv[1] !='1' ) {
        usage();
    }

    char state[PROPERTY_VALUE_MAX];
    char decrypt[PROPERTY_VALUE_MAX];
    char encryption_type [PROPERTY_VALUE_MAX];

    property_get("ro.crypto.state", state, "");
    property_get("vold.decrypt", decrypt, "");
    property_get("vold.encryption.type", encryption_type, "");

    SLOGI("ro.crypto.state=%s, vold.decrypt=%s, vold.encryption.type=%s, start/stop=%c", state, decrypt, encryption_type, *argv[1]);
    if(!strcmp(state, "")) {   /* first boot and to encrypt-ing */
       SLOGI("this is first boot and will to encrypt. set vold.encryption.type to default");
       property_set("vold.encryption.type", "default");
       SLOGE("ccci is waiting. No need to start/stop modem");
       return -1;
    }
    else if(!strcmp(state, "encrypted")){
       // set property, vold.encryption.type
       if(!strcmp(decrypt, "trigger_restart_min_framework")) {
         property_set("vold.encryption.type", "not_default");
         property_get("vold.encryption.type", encryption_type, "");
       }
       else {
           if(strcmp(encryption_type, "not_default") && !strcmp(decrypt, "trigger_restart_framework")) {
              property_set("vold.encryption.type", "default");
           }
       }

       if(!strcmp(encryption_type, "not_default")) {
         /* pin, password, pattern type */
         if(!strcmp(decrypt, "trigger_restart_min_framework")) {
            SLOGE("encryption.type is NOT default. ccci is waiting. No need to start/stop modem");
            return -1;
         }
       }
       else if(!strcmp(encryption_type, "default")) { /* default type */
          SLOGE("encryption.type is default. ccci is waiting. No need to start/stop modem");
          return -1;
       }
    }

    if(*argv[1] =='0') {
        stop_modem();
    }

    if(*argv[1] =='1') {
        start_modem();
    }

    return 0;
}


