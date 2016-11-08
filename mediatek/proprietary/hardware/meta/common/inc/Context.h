#ifndef _CONETXT_H_
#define _CONETXT_H_

#include "MetaPub.h"


#define MD1_INDEX 0x01
#define MD2_INDEX 0x02
#define MD3_INDEX 0x04
#define MD5_INDEX 0x10


class Modem;
class CmdTarget;

Modem * createModem(const char *ccci, unsigned short id);
Modem * getModem(unsigned short id);
CmdTarget * getModule(unsigned short id);
unsigned int getMdmType();
unsigned int getActiveMdmId();
unsigned int getMdmNumber();
signed int getModemHandle(unsigned short id); 

unsigned int dumpData(const unsigned char* con, int length);
unsigned int dumpDataInHexString(const unsigned char* con, int length, unsigned int bytesPerRow=16);
unsigned int getFileSize(int fd);
const char* makepath(unsigned char file_ID);
void createModemThread(unsigned short modemIndex,int usbUsb);

void setLogLevel(unsigned int level);
unsigned int getLogLevel();

int getModemProtocol(unsigned short modemIndex, void* modem_capa);
int getMDChType(unsigned short modemIndex);


void destroyContext();

#endif	// _CONETXT_H_
