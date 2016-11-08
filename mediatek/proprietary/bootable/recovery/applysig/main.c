#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <errno.h>
#include "applysig.h"

int main(int argc, char** argv) {
    char *part_name = NULL;
    if (argc != 3) {
        printf("usage: %s <sig-file> <partition name>\n", argv[0]);
        return -3;
    }
    if (access(argv[1], F_OK) != 0){
      printf("Fail to access %s, %s", argv[1], strerror(errno));
      return errno;
    }
    part_name = strrchr(argv[2], '/');
    if (part_name == NULL)
        part_name = argv[2];
    else
        part_name = part_name + 1; // ignore '/'
    /* So far only support bootimg and recovery partition */
    if (!(!strcmp(part_name, "boot") || !strcmp(part_name, "bootimg") || !strcmp(part_name, "recovery"))){
      printf("%s partition is not supported\n", part_name);
      return -1;
    }
    return applysignature(argv[1], part_name);
}
