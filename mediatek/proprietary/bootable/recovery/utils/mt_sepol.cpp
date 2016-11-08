/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
#include <stdio.h>
#include <string.h>
#include <errno.h>
#include <selinux/selinux.h>

bool recovery_is_permissive(void)
{
    int rc;
    bool result = false;

    rc = is_selinux_enabled();
    if (rc < 0) {
        printf("%s is_selinux_enabled() failed (%s)\n", __FUNCTION__, strerror(errno));
        return false;
    }
    if (rc == 1) {
        rc = security_getenforce();
        if (rc < 0) {
            printf("%s getenforce fail (%s)\n", __FUNCTION__, strerror(errno));
            return false;
        }

        if (rc == 0)
            result = true;
    } else {
        printf("recovery selinux is disabled\n");
        result = true;
    }

    if (result)
        printf("recovery is permissive mode\n");
    else
        printf("recovery is enforcing mode\n");

    return result;
}
