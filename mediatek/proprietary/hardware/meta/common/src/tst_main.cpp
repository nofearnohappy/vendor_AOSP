#include <assert.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <stdlib.h>
#include <cutils/properties.h>

#include "Modem.h"
#include "SerPort.h"

#include "UsbRxWatcher.h"
#include "Context.h"
#include "PortInterface.h"
#include "LogDefine.h"

int main(int argc, char** argv)
{
	META_LOG("[Meta] Enter meta_tst init flow!");

	umask(007);

	UsbRxWatcher hostRx;

	SerPort *pPort = createSerPort();

	unsigned int modemType = getMdmType();
	
	if((modemType & MD1_INDEX) == MD1_INDEX)
	{
		createModemThread(0,0);
	}

	if((modemType & MD2_INDEX) == MD2_INDEX)
	{
		createModemThread(1,0);
	}

#ifdef TST_C2K_SUPPORT
	if((modemType & MD3_INDEX) == MD3_INDEX)
	{
		META_LOG("[Meta] To Create C2K modem");
		createModemThread(2,0);
	}
#endif

	if((modemType & MD5_INDEX) == MD5_INDEX)
	{
#ifndef MTK_EXTMD_NATIVE_DOWNLOAD_SUPPORT
		createModemThread(4,0);
#else
		META_LOG("MTK_EXTMD_NATIVE_DOWNLOAD_SUPPORT creat modem tread after native download complete");	
#endif
	}
	
	if (pPort != NULL)
	{
		pPort->pumpAsync(&hostRx);
	}
	else
	{
		META_LOG("[Meta] Enter meta_tst init fail");
	}

	while (1)
    {	
		sleep(5);
		querySerPortStatus();		
    }

	// infinite loop above; it'll never get here...



	destroyContext();

	return 0;
}
