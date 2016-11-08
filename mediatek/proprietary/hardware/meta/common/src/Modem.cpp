#include "Modem.h"
#include "Device.h"
#include "LogDefine.h"

Modem::Modem(const char *ccci, unsigned short id)
	: CmdTarget(id), m_pDev(new CCCI(ccci))
{
}

Modem::~Modem(void)
{
	delete m_pDev;
}

signed int Modem::pumpAsync(IDevWatcher *p)
{
	return m_pDev->pumpAsync(p);
}

void Modem::exec(Frame *pFrm)
{
	CmdTarget::exec(pFrm);
	m_pDev->write(pFrm->localBuf(), pFrm->localLen());
}

signed int Modem::getDevHandle()
{
	return m_pDev->getDevHandle();
}
	
int Modem::init()
{
	return 1;
}


void Modem::deinit()
{
}

