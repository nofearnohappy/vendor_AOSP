#include <cstdio>
#include <fcntl.h>
#include <unistd.h>
#include <sys/ioctl.h>
#include <sys/socket.h>
#include <arpa/inet.h>   //inet_addr
#include "autok.h"
#define BUF_LEN     1024

int get_node_data(const char *filename, char **data, int *len)
{
    int fd_rd = 0;
    int result = -1;
    int length=0;
    char rBuf[PAGE_SIZE] = "";
    *len = 0;
    *data = NULL;
    fd_rd = open(filename, O_RDONLY, 0000);
    if(fd_rd == -1){
        LOGD("Can't open %s\n", filename);
        return result;
    }
    if ((length = read(fd_rd, rBuf, PAGE_SIZE)) == -1) {
        LOGD("Can't read %s\n", filename);
        goto EXIT_GET_NODE_DATA;
    }
    //LOGD("GETNODE_LENGTH:%d\n", length);
    *len = length;
    *data = (char*)malloc(sizeof(char)*length+1);
    if(NULL == *data)
        goto EXIT_GET_NODE_DATA;
    memset((*data)+length, 0, 1);
    //goto EXIT_GET_NODE_DATA;
    memcpy(*data, rBuf, length);
    
    result = 0;
EXIT_GET_NODE_DATA:
    if(fd_rd)
        close(fd_rd);
    return result;
}

int set_node_data(const char *filename, char *data, int len)
{
    int fd_wr = 0;
    int result = -1;
    fd_wr = open(filename, O_WRONLY, 0000);
    if(fd_wr == -1)
        return result;
    if(write(fd_wr, data, len) == -1){
        LOGE("Can't write %s\n", filename);
        goto EXIT_SET_NODE_DATA;
    }
    result = 0;
EXIT_SET_NODE_DATA:
    if(fd_wr)
        close(fd_wr);
    return result;
}

int from_dev_to_data(char *from, char *to)
{
    int data_count;
    char *data_buf = NULL;
    FILE * pFile = NULL;
    get_node_data(from, &data_buf, &data_count);
    pFile = fopen (to, "wb");
    if(pFile == NULL)
        return 0;
    fwrite (data_buf , sizeof(char), data_count, pFile);
    if(pFile != NULL)
        fclose(pFile);
    AUTOK_FREE(data_buf);
    return 0;
}

int data_copy(char *from, char *to)
{
    FILE * inFile;
    FILE * outFile;
    char *data_buf = NULL;
    long lSize = -1;
    
    inFile = fopen (from, "rb");
    if (inFile==NULL) {
        LOGE("[%s]Open file error", from); 
        return -1;
    }
    outFile = fopen (to, "wb");
    if (outFile==NULL) {
        LOGE("[%s]Open file error", to); 
        if(inFile!=NULL)
            fclose (inFile); 
        return -1;
    }    
    fseek (inFile , 0 , SEEK_END);
    lSize = ftell (inFile);
    rewind (inFile);
    data_buf = (char*) malloc (sizeof(char)*lSize);
    fread (data_buf, 1, lSize, inFile);    
    fwrite (data_buf , sizeof(char), lSize, outFile);
    fclose (inFile); 
    fclose (outFile);
    AUTOK_FREE(data_buf);
    return lSize;
}

int write_to_file(char *filename, char *data_buf, int length)
{
    FILE * outFile;
    outFile = fopen(filename, "ab+");
    if (outFile==NULL) {
        LOGE("[%s]Open file error", filename); 
        return -1;
    }
    if(data_buf == NULL)
        goto exit_write_to_file;
	
    fwrite (data_buf , sizeof(char), length, outFile);
exit_write_to_file:	
    if(outFile != NULL)
        fclose(outFile);   
    return 0;
}

int read_from_file(const char *filename, char **data, int *len)
{
    FILE * inFile;
    size_t lSize = 0;
    *len = 0;
    *data = NULL;
    inFile = fopen(filename, "rb");
    if (inFile==NULL) {
        LOGE("Open File error[%s]", filename); 
        return -1;
    }    
    fseek (inFile , 0 , SEEK_END);
    lSize = ftell(inFile);
    lSize = lSize > 0?lSize:0;
    rewind (inFile);
    LOGI("Get File Size[%u]\n", lSize); 
    *data = (char*) malloc (sizeof(char)*lSize+1);
    if(NULL == *data){
        goto exit_read_file;
    }
    
    memset((*data), 0, lSize+1);
    if(lSize != fread (*data, 1, lSize, inFile)){
        LOGE("Read File Size Error[%s]", filename); 
        *data = NULL;
        goto exit_read_file;
    }
    *len = lSize;
exit_read_file: 
    if(inFile != NULL)
        fclose(inFile); 
    return 0;
}

int notify_autok_done()
{
    int sock;
    int ret = 0;
    struct sockaddr_in server = { 0 };
    
    //Create socket
    sock = socket(AF_INET , SOCK_STREAM , 0);
    if (sock == -1){
        LOGE("Could not create socket");
        ret = -1;
        goto exit_notify_autok_done;
    }
    LOGD("Socket created\n");

    memset(&server, 0, sizeof(struct sockaddr_in));
    
    server.sin_addr.s_addr = inet_addr("127.0.0.1");
    server.sin_family = AF_INET;
    server.sin_port = htons( 28794 );

    //Connect to remote server
    if (connect(sock, (struct sockaddr *)&server , sizeof(server)) < 0) {
        LOGE("Socket connect failed. Error\n");
        ret = -2;
        goto exit_notify_autok_done;
    }
   
    LOGD("Socket Connected Done\n");
exit_notify_autok_done:
    if(sock > -1)
        close(sock);
    return 0;
}  
