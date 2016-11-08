#pragma once
#include "Device.h"


class CCCI : public Device
{
public:
	CCCI(const char*);

private:
	static signed int open(const char*);
};


class SerPort : public Device
{
public:
	SerPort(const char*);


protected:
	static signed int open(const char*);
	static void initTermIO(int portFd);
};


class UartPort : public SerPort
{
public:
	UartPort(const char*);
};

class UsbPort : public SerPort
{
public:
	UsbPort(const char*);
	~UsbPort();

public:
	virtual signed int read(unsigned char*, unsigned int);
	virtual signed int write(const unsigned char*, unsigned int);
	virtual void update();
	

private:
	void close();
	int isReady() const;

private:
	const char *m_devPath;
	int m_usbFlag;
};
