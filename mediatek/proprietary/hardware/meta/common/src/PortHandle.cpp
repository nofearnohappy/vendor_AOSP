#include <assert.h>
#include <stdlib.h>
#include <fcntl.h>
#include <string.h>


#include "PortHandle.h"
#include "PortInterface.h"
#include "LogDefine.h"
#include "SerPort.h"

#if defined(MTK_TC1_FEATURE)
	#define DEV_USB_PATH	"/dev/ttyGS4"
#else
    #define DEV_USB_PATH    "/dev/ttyGS0"
#endif

#define COM_PORT_TYPE_FILE "/sys/bus/platform/drivers/meta_com_type_info/meta_com_type_info"
#define COM_PORT_TYPE_STR_LEN 1

#define UART_PORT_INFO_FILE "/sys/bus/platform/drivers/meta_uart_port_info/meta_uart_port_info"
#define UART_PORT_INFO_STR_LEN 1



class PortHandle
{
private:
	PortHandle(void);
public:
	~PortHandle(void);

	static PortHandle *instance();
	SerPort * createPort();
	SerPort * getPort() const;
	META_COM_TYPE getComType();
	void querySerPortStatus();
	void FTMuxPrimitiveData(META_RX_DATA *pMuxBuf);
	int WriteDataToPC(void *Local_buf,unsigned short Local_len,void *Peer_buf,unsigned short Peer_len);
	int getMetaUartPort(void);
	void destroy();

private:
	SerPort *			m_serPort;
	META_COM_TYPE		m_comType;
private:
	static PortHandle *	m_myInst;

};

PortHandle *PortHandle::m_myInst = NULL;

PortHandle::PortHandle(void)
	: m_comType(META_UNKNOWN_COM),
	  m_serPort(NULL)
{

}

PortHandle::~PortHandle(void)
{
	if (m_serPort != NULL)
	{
		delete m_serPort;
	}
}


PortHandle *PortHandle::instance()
{
	return (m_myInst==NULL) ? ((m_myInst=new PortHandle)) : m_myInst;
}

void PortHandle::destroy()
{
	delete m_myInst;
	m_myInst = NULL;
}

SerPort * PortHandle::createPort()
{
	if (m_serPort != NULL)
	{
		assert(false);	// repeated create
	}
	else if (getComType() == META_USB_COM)
	{
		m_serPort = new UsbPort(DEV_USB_PATH);
	}
	else if (getComType() == META_UART_COM)
	{
        char szDevUartPath[256] = {0};
		switch(getMetaUartPort())
		{
		    case 1:      //UART1
		        strcpy(szDevUartPath, "/dev/ttyMT0");
				break;
			case 2:      //UART2
			    strcpy(szDevUartPath, "/dev/ttyMT1");
				break;
			case 3:      //UART3
			    strcpy(szDevUartPath, "/dev/ttyMT2");
				break;
			case 4:      //UART4
			    strcpy(szDevUartPath, "/dev/ttyMT3");
				break;
			default:     //default use UART1
				strcpy(szDevUartPath, "/dev/ttyMT0");
				break;
		}
		META_LOG("[Meta] uart port path: %s", szDevUartPath);
		m_serPort = new UartPort(szDevUartPath);
	}

	assert(m_serPort != NULL);

	return m_serPort;
}

SerPort * PortHandle::getPort() const
{
	return m_serPort;
}

META_COM_TYPE PortHandle::getComType()
{
	if (m_comType == META_UNKNOWN_COM)
	{
		char buf[COM_PORT_TYPE_STR_LEN + 1];
		int bytes_read = 0;
		int res = 0;
		int fd = open(COM_PORT_TYPE_FILE, O_RDONLY);
		if (fd != -1)
		{
			memset(buf, 0, COM_PORT_TYPE_STR_LEN + 1);
			while (bytes_read < COM_PORT_TYPE_STR_LEN)
			{
				res = read(fd, buf + bytes_read, COM_PORT_TYPE_STR_LEN);
				if (res > 0)
					bytes_read += res;
				else
					break;
			}
			close(fd);
			m_comType = (META_COM_TYPE)atoi(buf);
		}
		else
		{
			META_LOG("[Meta] Failed to open com port type file %s", COM_PORT_TYPE_FILE);
		}
		META_LOG("[Meta] com port type: %d", m_comType);
	}

	return m_comType;
}

void PortHandle::querySerPortStatus()
{
     if (m_comType == META_USB_COM)
     {
           SerPort * pPort = getSerPort();
		   if (pPort != NULL)
		   {
		       pPort->update();
		   }
     }
}

int PortHandle::getMetaUartPort(void)
{
    int nPort = 1;
    if (m_comType == META_UART_COM)
    {
	    char buf[UART_PORT_INFO_STR_LEN + 1] = {0};
	    int fd = open(UART_PORT_INFO_FILE, O_RDONLY);
	    if (fd != -1)
	    {
			if (read(fd, buf, sizeof(char)*COM_PORT_TYPE_STR_LEN) <= 0)
			{
			    META_LOG("[Meta] ERROR can not read meta uart port ");
		    }
			else
			{
			    nPort = atoi(buf);
			}
		    close(fd);

	    }
	    else
	    {
		    META_LOG("[Meta] Failed to open meta uart port file %s", UART_PORT_INFO_FILE);
	    }
	    META_LOG("[Meta] uart com port: %d", nPort);
    }
	else
	{
	    META_LOG("[Meta] com port type is not uart");
	}
	return nPort;
}


void PortHandle::FTMuxPrimitiveData(META_RX_DATA *pMuxBuf)
{
    /* This primitive is logged by TST */
    unsigned char *pTempBuf = NULL;
    unsigned char *pTempDstBuf = NULL;
    unsigned char *pMamptrBase = NULL;
    unsigned char *pDestptrBase = NULL;
    int iCheckNum = 0;
    int dest_index=0;
    unsigned char cCheckSum = 0;
    int cbWriten = 0;
    int cbTxBuffer = 0;
    int i=0;
	SerPort * pPort = getSerPort();

    if(pMuxBuf == NULL)
    {
        META_LOG("[Meta] (FTMuxPrimitiveData) Err: pMuxBuf is NULL");
        return;
    }

    cbTxBuffer = pMuxBuf->LocalLen + pMuxBuf->PeerLen + 9;
    if (cbTxBuffer>FRAME_MAX_LEN)
    {
        META_LOG("[Meta] (FTMuxPrimitiveData) error frame size is too big!! ");
        return;
    }
    else
        META_LOG("[Meta] (FTMuxPrimitiveData) Type = %d Local_len = %d, Peer_len = %d", pMuxBuf->eFrameType, pMuxBuf->LocalLen, pMuxBuf->PeerLen);

    META_LOG("[Meta] (FTMuxPrimitiveData) total size = %d", cbTxBuffer);
    pMamptrBase = (unsigned char *)malloc(cbTxBuffer);

    if(pMamptrBase == NULL)
    {
        META_LOG("[Meta] (FTMuxPrimitiveData) Err: malloc pMamptrBase Fail");
        return;
    }
    pDestptrBase = (unsigned char *)malloc(FRAME_MAX_LEN);
    if(pDestptrBase == NULL)
    {
        META_LOG("[Meta] (FTMuxPrimitiveData) Err: malloc pDestptrBase Fail");
        free(pMamptrBase);
        return;
    }


    pTempDstBuf = pDestptrBase;
    pTempBuf = pMamptrBase;

    /* fill the frameheader */
    *pTempBuf++ = 0x55;
    *pTempBuf++=((pMuxBuf->LocalLen + pMuxBuf->PeerLen +5)&0xff00)>>8;
    *pTempBuf++= (pMuxBuf->LocalLen + pMuxBuf->PeerLen +5)&0xff;
    *pTempBuf++ = 0x60;

    /*fill the local and peer data u16Length and its data */
    *pTempBuf++ = ((pMuxBuf->LocalLen)&0xff); /// pMuxBuf->LocalLen ;
    *pTempBuf++ = ((pMuxBuf->LocalLen)&0xff00)>>8;
    *pTempBuf++ = (pMuxBuf->PeerLen )&0xff;   ///pMuxBuf->PeerLen ;
    *pTempBuf++ = ((pMuxBuf->PeerLen)&0xff00)>>8;

    memcpy((pTempBuf), pMuxBuf->pData, pMuxBuf->LocalLen + pMuxBuf->PeerLen);

    pTempBuf = pMamptrBase;

    /* 0x5a is start data, so we use 0x5a and 0x01 inidcate 0xa5, use 0x5a and 0x5a indicate 0x5a
    the escape is just for campatiable with feature phone */
    while (iCheckNum != (cbTxBuffer-1))
    {
        cCheckSum ^= *pTempBuf;
        *pTempDstBuf = *pTempBuf;
        iCheckNum++;

        if (*pTempBuf ==0xA5 )
        {
            *pTempDstBuf++ = 0x5A;
            *pTempDstBuf++ = 0x01;
            dest_index++;		//do the escape, dest_index should add for write to uart or usb
        }
        else if (*pTempBuf ==0x5A )
        {
            *pTempDstBuf++ = 0x5A;
            *pTempDstBuf++ = 0x5A;
            dest_index++;		//do the escape, dest_index should add for write to uart or usb
        }
        else
            pTempDstBuf++;

        dest_index++;
        pTempBuf++;
    }

    /* 0x5a is start data, so we use 0x5a and 0x01 inidcate 0xa5 for check sum, use 0x5a and 0x5a indicate 0x5a
    the escape is just for campatiable with feature phone */
    if ( cCheckSum ==0xA5 )
    {
        dest_index++;		//do the escape, dest_index should add for write to uart or usb
        //Wayne replace 2048 with MAX_TST_RECEIVE_BUFFER_LENGTH
        if ((dest_index) > FRAME_MAX_LEN)//2048)
        {
            META_LOG("[Meta] (FTMuxPrimitiveData) Data is too big: index = %d cbTxBuffer = %d ",dest_index, cbTxBuffer);
            goto TSTMuxError;
        }

        *pTempDstBuf++= 0x5A;
        *pTempDstBuf = 0x01;
    }
    else if ( cCheckSum ==0x5A )
    {
        dest_index++;		//do the escape, dest_index should add for write to uart or usb
        if ((dest_index) > FRAME_MAX_LEN)
        {
            META_LOG("[Meta] (FTMuxPrimitiveData) Data is too big: index = %d cbTxBuffer = %d ",dest_index, cbTxBuffer);
            goto TSTMuxError;
        }
        *pTempDstBuf++= 0x5A;
        *pTempDstBuf = 0x5A;
    }
    else
        *pTempDstBuf =(char )cCheckSum;

    dest_index++;

    //write to PC
    //cbWriten = write(getPort(), (void *)pDestptrBase, dest_index);

	pPort->write(pDestptrBase, dest_index);
    pTempDstBuf = pDestptrBase;

    META_LOG("[Meta] FTMuxPrimitiveData: %d  %d %d  cChecksum: %d ",cbWriten, cbTxBuffer, dest_index,cCheckSum);

    TSTMuxError:

    free(pMamptrBase);
    free(pDestptrBase);
}


int PortHandle::WriteDataToPC(void *Local_buf,unsigned short Local_len,void *Peer_buf,unsigned short Peer_len)
{
	META_RX_DATA metaRxData;
	memset(&metaRxData,0, sizeof(META_RX_DATA));
	unsigned int dataLen = Local_len+Peer_len+8+1;
	unsigned char *metaRxbuf = (unsigned char *)malloc(dataLen);
	memset(metaRxbuf,0, dataLen);
	unsigned char *cPeerbuf = &metaRxbuf[Local_len+8];

	metaRxData.eFrameType = AP_FRAME;
	metaRxData.pData = metaRxbuf;
	metaRxData.LocalLen = Local_len;
	metaRxData.PeerLen = Peer_len >0 ? Peer_len+8 : Peer_len;

    if (((Local_len + Peer_len)> FT_MAX_LEN)||(Peer_len >PEER_BUF_MAX_LEN))
    {
        META_LOG("[Meta] (WriteDataToPC) Err: Local_len = %hu, Peer_len = %hu", Local_len,Peer_len);
        return 0;
    }

    if ((Local_len == 0) && (Local_buf == NULL))
    {
        META_LOG("[Meta] (WriteDataToPC) Err: Local_len = %hu, Peer_len = %hu", Local_len,Peer_len);
        return 0;
    }

    // copy to the temp buffer, and send it to the tst task.
    memcpy(metaRxbuf, Local_buf, Local_len);
    if ((Peer_len >0)&&(Peer_buf !=NULL))
        memcpy(cPeerbuf, Peer_buf, Peer_len);

    FTMuxPrimitiveData(&metaRxData);

    return 1;
}


/////////////////////////////////////////////////////////////////////////////////

void destroyPortHandle()
{
	return PortHandle::instance()->destroy();
}

META_COM_TYPE getComType()
{
	return PortHandle::instance()->getComType();
}

SerPort * createSerPort()
{
	return PortHandle::instance()->createPort();
}

SerPort * getSerPort()
{
	return PortHandle::instance()->getPort();
}


void querySerPortStatus()
{
     return PortHandle::instance()->querySerPortStatus();
}

int WriteDataToPC(void *Local_buf,unsigned short Local_len,void *Peer_buf,unsigned short Peer_len)
{
	return PortHandle::instance()->WriteDataToPC(Local_buf,Local_len,Peer_buf,Peer_len);
}








