#ifndef _MODEM_H_
#define _MODEM_H_

#include "CmdTarget.h"
#include "SerPort.h"

class IDevWatcher;

class Modem : public CmdTarget
{
public:
	Modem(const char*, unsigned short);
	virtual ~Modem(void);

public:
	virtual void exec(Frame*);

	signed int pumpAsync(IDevWatcher*);
	signed int getDevHandle();

protected:
	 int init();
	 void deinit();

private:
	CCCI *m_pDev;
};

#endif	// _MODEM_H_
