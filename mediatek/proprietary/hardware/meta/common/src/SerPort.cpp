#include <stdlib.h>
#include <unistd.h>
#include <termios.h> 
#include <fcntl.h>
#include <errno.h>
#include <string.h>



#include "SerPort.h"
#include "LogDefine.h"

//////////////////////////////////////////////////////////////////////////
CCCI::CCCI(const char *path)
{
	m_fd = open(path);
}

signed int CCCI::open(const char *path)
{
	int retry = 100;
	signed int fd = NULL_FILE_DESCRIPTOR;
	
	while(fd == NULL_FILE_DESCRIPTOR && retry != 0)
	{
		fd = ::open(path, O_RDWR|O_NOCTTY);
	    META_LOG("[Meta]Open modem. m_fd = %d", fd);
	    if (fd != NULL_FILE_DESCRIPTOR)
	    {
		    META_LOG("[Meta] Open modem port:(%s) success.", path);
			break;
	    }
	    else
	    {
		    META_LOG("[Meta] Open modem port:(%s) fail.", path);
			usleep(100*1000);
			retry--;
	    }
	}
	
	return fd;
}

//////////////////////////////////////////////////////////////////////////

SerPort::SerPort(const char *path)
{
	m_fd = open(path);	
}

signed int SerPort::open(const char *path)
{
	signed int fd = ::open(path, O_RDWR|O_NOCTTY);

	META_LOG("[Meta] Open serPort. m_fd = %d", fd);

	if (fd != NULL_FILE_DESCRIPTOR)
	{
		META_LOG("[Meta] Open serport:(%s) success.", path);
		initTermIO(fd);
	}
	else
	{
		META_LOG("[Meta] Open serport:(%s) fail, error code = %d", path, errno);
	}
	
	return fd;
}

void SerPort::initTermIO(int portFd)
{
	struct termios termOptions;
	if (fcntl(portFd, F_SETFL, 0) == -1)
	{
	    META_LOG("[Meta] initTermIO call fcntl fail");
	}
	// Get the current options:
	tcgetattr(portFd, &termOptions);

	// Set 8bit data, No parity, stop 1 bit (8N1):
	termOptions.c_cflag &= ~PARENB;
	termOptions.c_cflag &= ~CSTOPB;
	termOptions.c_cflag &= ~CSIZE;
	termOptions.c_cflag |= CS8 | CLOCAL | CREAD;

	// Raw mode
	termOptions.c_iflag &= ~(INLCR | ICRNL | IXON | IXOFF | IXANY);
	termOptions.c_lflag &= ~(ICANON | ECHO | ECHOE | ISIG);  /*raw input*/
	termOptions.c_oflag &= ~OPOST;  /*raw output*/


	tcflush(portFd,TCIFLUSH);//clear input buffer
	termOptions.c_cc[VTIME] = 100; /* inter-character timer unused */
	termOptions.c_cc[VMIN] = 0; /* blocking read until 0 character arrives */


	cfsetispeed(&termOptions, B921600);
    cfsetospeed(&termOptions, B921600);
	/*
	* Set the new options for the port...
	*/
	tcsetattr(portFd, TCSANOW, &termOptions);
}


//////////////////////////////////////////////////////////////////////////

UartPort::UartPort(const char *path)
	: SerPort(path)
{
}

//////////////////////////////////////////////////////////////////////////

UsbPort::UsbPort(const char *path)
	: SerPort(path)
{
	m_devPath = strdup(path);
	m_usbFlag = 1;
}

UsbPort::~UsbPort()
{
	// it'll never get here
	// so it doesn't make much sense...
	free((char*)m_devPath);
}

signed int UsbPort::read(unsigned char *buf, unsigned int len)
{
	// try to reopen USB if it was unplugged
/*	if (NULL_FILE_DESCRIPTOR == m_fd && !update())
	{
		return -1;
	}
*/
	
	signed int ret = SerPort::read(buf, len);

	// in case of error, see if USB is unplugged


	// it doesn't make sense to do PnP check if 'read' succeeds

	return ret;
}

signed int UsbPort::write(const unsigned char *buf, unsigned int len)
{
	// try to reopen USB if it was unplugged
	if (NULL_FILE_DESCRIPTOR == m_fd)	//&& !update())
	{
		return -1;
	}
	signed int ret = SerPort::write(buf, len);

	// it doesn't make sense to do PnP check if 'write' succeeds

	return ret;
}

void UsbPort::close()
{
	if (m_fd != NULL_FILE_DESCRIPTOR)
	{
		::close(m_fd);
		m_fd = NULL_FILE_DESCRIPTOR;
	}
}

int UsbPort::isReady() const
{
	int type = 0;
    char buf[11];
    int bytes_read = 0;
    int res = 0;
    int fd = ::open("/sys/class/android_usb/android0/state", O_RDONLY);
    if (fd != -1)
    {
        memset(buf, 0, 11);
        while (bytes_read < 10)
        {
            res = ::read(fd, buf + bytes_read, 10);
            if (res > 0)
                bytes_read += res;
            else
                break;
        }
        ::close(fd);
        type = strcmp(buf,"CONFIGURED");

        META_LOG("[Meta]Query usb state OK.");
    }
    else
    {
        META_LOG("[Meta]Failed to open:/sys/class/android_usb/android0/state");
    }
         
	return (type == 0);  
}

pthread_mutex_t META_USBPort_Mutex = PTHREAD_MUTEX_INITIALIZER;
pthread_mutex_t META_ComPortMD_Mutex = PTHREAD_MUTEX_INITIALIZER;

void UsbPort::update()
{
	if (!isReady())
	{
		if(m_usbFlag)
		{
			close();	
		}
		m_usbFlag = 0;
		META_LOG("[Meta]USB cable plus out!");
	}
	else
	{
		if(!m_usbFlag)
		{
			sleep(1);
			if (pthread_mutex_lock (&META_USBPort_Mutex))
			{
				META_LOG( "[Meta]META_MAIN META_USBPort_Mutex lock error!\n"); 
			}
			m_fd = open(m_devPath);

			if(m_fd != NULL_FILE_DESCRIPTOR)
			{
				m_usbFlag = 1;
			}

			if (pthread_mutex_unlock (&META_USBPort_Mutex))
			{
				META_LOG( "[Meta]META_Main META_USBPort_Mutex unlock error!\n"); 
			}
		}
	}

}
