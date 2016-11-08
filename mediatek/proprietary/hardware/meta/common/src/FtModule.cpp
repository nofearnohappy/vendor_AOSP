#include <string.h>
#include <sys/reboot.h>
#include <cutils/properties.h>

#include <fcntl.h>

#include <cutils/sockets.h>
#include <sys/socket.h>

#include <sys/types.h>
#include <sys/stat.h>
#include <dirent.h>


#include "FtModule.h"
#include "LogDefine.h"
#include "Context.h"
#include "PortInterface.h"
#include "PortHandle.h"
#include "hardware/ccci_intf.h"
#include "ExternalFunction.h"

#define SIM_SWITCH_MODE_FILE  "/sys/mtk_ssw/mode"
#define SIM_SWITCH_MODE_STR_LEN 8

#ifndef FT_CNF_OK
#define FT_CNF_OK 0
#endif
#ifndef FT_CNF_FAIL
#define FT_CNF_FAIL 1
#endif

#ifdef FT_WIFI_FEATURE

FtModWifi::FtModWifi(void)
	:CmdTarget(FT_WIFI_REQ_ID)
{
}

FtModWifi::~FtModWifi(void)
{
}

void FtModWifi::exec(Frame *pFrm)
{
	CmdTarget::exec(pFrm);

	META_LOG("[Meta][FT] FT_WIFI_OP ");
    META_WIFI_OP((FT_WM_WIFI_REQ *)pFrm->localBuf(), (char *)pFrm->peerBuf(), pFrm->peerLen());
}
#endif

/////////////////////////////////////////////////////////////////////////////////////////
#ifdef FT_GPS_FEATURE

FtModGPS::FtModGPS(void)
	:CmdTarget(FT_GPS_REQ_ID)
{
}

FtModGPS::~FtModGPS(void)
{
}

void FtModGPS::exec(Frame *pFrm)
{
	CmdTarget::exec(pFrm);

	META_LOG("[Meta][FT] FT_GPS_OP ");


    META_GPS_OP((GPS_REQ *)pFrm->localBuf(), (char *)pFrm->peerBuf(), pFrm->peerLen());
}

#endif

/////////////////////////////////////////////////////////////////////////////////////////

#ifdef FT_NFC_FEATURE
FtModNFC::FtModNFC(void)
	:CmdTarget(FT_NFC_REQ_ID)
{
}

FtModNFC::~FtModNFC(void)
{
}

void FtModNFC::exec(Frame *pFrm)
{
	CmdTarget::exec(pFrm);

	META_LOG("[Meta][FT] FT_NFC_OP ");

	if(getInitState())
    META_NFC_OP((NFC_REQ *)pFrm->localBuf(), (char *)pFrm->peerBuf(), pFrm->peerLen());
}

int FtModNFC::init(Frame*)
{
	if (META_NFC_init() != 0)
	{	
		 META_NFC_deinit();
		 return false;
	}
	return true;
}
#endif

///////////////////////////////////////////////////////////////////////////////////////////////
#ifdef FT_FM_FEATURE

FtModFM::FtModFM(void)
	:CmdTarget(FT_FM_REQ_ID)
{
}

FtModFM::~FtModFM(void)
{

}

void FtModFM::exec(Frame *pFrm)
{
	CmdTarget::exec(pFrm);

	META_LOG("[Meta][FT] FT_FM_OP ");
    META_FM_OP((FM_REQ *)pFrm->localBuf(), (char *)pFrm->peerBuf(), pFrm->peerLen());
}

int FtModFM::init(Frame*)
{
	META_FM_init();
	return true;

}
#endif

///////////////////////////////////////////////////////////////////////////////////////////////
#ifdef FT_BT_FEATURE

FtModBT::FtModBT(void)
	:CmdTarget(FT_BT_REQ_ID)
{
}

FtModBT::~FtModBT(void)
{
}

void FtModBT::exec(Frame *pFrm)
{
	CmdTarget::exec(pFrm);

	META_LOG("[Meta][FT] FT_BT_OP ");

    META_BT_OP((BT_REQ *)pFrm->localBuf(), (char *)pFrm->peerBuf(), pFrm->peerLen());

}
#endif

///////////////////////////////////////////////////////////////////////////////////////////////
#ifdef FT_AUDIO_FEATURE

FtModAudio::FtModAudio(void)
	:CmdTarget(FT_L4AUD_REQ_ID)
{
}

FtModAudio::~FtModAudio(void)
{
}

void FtModAudio::exec(Frame *pFrm)
{
	CmdTarget::exec(pFrm);

	META_LOG("[Meta][FT] FT_Audio_OP ");

    META_Audio_OP((FT_L4AUD_REQ *)pFrm->localBuf(), (char *)pFrm->peerBuf(), pFrm->peerLen());
}

int FtModAudio::init(Frame*)
{
	META_Audio_init();
	return true;
}
#endif

///////////////////////////////////////////////////////////////////////////////////////////////
#ifdef FT_CCAP_FEATURE

FtModCCAP::FtModCCAP(void)
	:CmdTarget(FT_CCT_REQ_ID)
{
}

FtModCCAP::~FtModCCAP(void)
{
}

void FtModCCAP::exec(Frame *pFrm)
{
	CmdTarget::exec(pFrm);
	
	FT_CCT_CNF ft_cnf;
	memset(&ft_cnf, 0, sizeof(ft_cnf));
	FT_CCT_REQ *req = (FT_CCT_REQ *)pFrm->localBuf();
	
	if ((req->op != FT_CCT_OP_SUBPREVIEW_LCD_START) && (req->op !=FT_CCT_OP_SUBPREVIEW_LCD_STOP))
	{
		META_LOG("[Meta][FT] META_CCAP_init ");	
		if (!META_CCAP_init())
		{
			ft_cnf.header.id = req->header.id +1;
			ft_cnf.header.token = req->header.token;
			ft_cnf.op= req->op;
			ft_cnf.status = META_FAILED;
			WriteDataToPC(&ft_cnf, sizeof(ft_cnf),NULL, 0);
			META_LOG("[Meta][FT] FT_CCAP_OP META_CCT_init Fail ");
			return;
		}
	}
	else
    {
   		META_LOG("[Meta][FT] Now is sub Camera, init will be do later");	
   	}

	META_LOG("[Meta][FT] FT_CCAP_OP ");

    META_CCAP_OP((FT_CCT_REQ *)pFrm->localBuf(), (char *)pFrm->peerBuf());
}
#endif

///////////////////////////////////////////////////////////////////////////////////////////////
	
#ifdef FT_HDCP_FEATURE

FtModHDCP::FtModHDCP(void)
	:CmdTarget(FT_HDCP_REQ_ID)
{
}

FtModHDCP::~FtModHDCP(void)
{
}

void FtModHDCP::exec(Frame *pFrm)
{
	CmdTarget::exec(pFrm);	
  
	if(getInitState())
		META_HDCP_OP((HDCP_REQ *)pFrm->localBuf(), (char *)pFrm->peerBuf(), pFrm->peerLen());  
}

int FtModHDCP::init(Frame *pFrm)
{ 
	META_LOG("[Meta][FT] Ft_HDCP");
	HDCP_CNF ft_cnf;
	memset(&ft_cnf, 0, sizeof(ft_cnf));
	static int bInitFlagHDCP = false;
	HDCP_REQ *req = (HDCP_REQ *)pFrm->localBuf();

	if (false == bInitFlagHDCP)
	{
		// initial the HDCP module when it is called first time
		if (!META_HDCP_init())
		{
			ft_cnf.header.id = req->header.id +1;
			ft_cnf.header.token = req->header.token;
			ft_cnf.op= req->op;
			ft_cnf.status = META_FAILED;
			WriteDataToPC(&ft_cnf, sizeof(ft_cnf),NULL, 0);
			return false;
		}
		bInitFlagHDCP = true;
	}

	return true;
}
#endif

///////////////////////////////////////////////////////////////////////////////////////////////
#ifdef FT_DRM_KEY_MNG_FEATURE

FtModDRM::FtModDRM(void)
	:CmdTarget(FT_DRMKEY_REQ_ID)
{
}

FtModDRM::~FtModDRM(void)
{

}

void FtModDRM::exec(Frame *pFrm)
{
	CmdTarget::exec(pFrm);

	META_LOG("[Meta][FT] Ft_DRM");


	META_DRMKEY_INSTALL_OP((FT_DRMKEY_INSTALL_REQ *)pFrm->localBuf(), (char *)pFrm->peerBuf(), pFrm->peerLen());

}
#endif

///////////////////////////////////////////////////////////////////////////////////////////////

#define NVRAM_PEER_MAX_LEN 2000
#define BLK_CREATE	0x01
#define BLK_WRITE	0x02
#define BLK_EOF		0x04

#ifdef FT_NVRAM_FEATURE

FtModNvramBackup::FtModNvramBackup(void)
	:CmdTarget(FT_NVRAM_BACKUP_REQ_ID)
{
}

FtModNvramBackup::~FtModNvramBackup(void)
{
}

bool FtModNvramBackup::SendNVRAMFile(unsigned char file_ID, FT_NVRAM_BACKUP_CNF* pft_cnf)
{
	int backupFd;
	int peer_buff_size = 0;
    char* peer_buf = 0;
	bool return_value = FALSE;

	backupFd = open(makepath(file_ID), O_RDWR);

	if(backupFd >= 0)
	{
		META_LOG("[Meta][FT] File%d opens succeed ! ",file_ID);
		unsigned int fileLen = getFileSize(backupFd);

		peer_buf = (char*)malloc(NVRAM_PEER_MAX_LEN);
		memset(peer_buf, 0, NVRAM_PEER_MAX_LEN);

		pft_cnf->block.stage = BLK_CREATE;
		pft_cnf->block.file_ID = file_ID;

		while(!(pft_cnf->block.stage & BLK_EOF))
		{
			peer_buff_size = read(backupFd, peer_buf, NVRAM_PEER_MAX_LEN);

			if(peer_buff_size != -1)
			{
				pft_cnf->status = META_SUCCESS;
				if(peer_buff_size == 0)
				{
					pft_cnf->block.stage |= BLK_EOF;
					META_LOG("[Meta][FT] File%d backups succeed! ",file_ID);
					pft_cnf->block.file_size = fileLen;

					close(backupFd);

					free(peer_buf);

					if(remove(makepath(file_ID)) == 0)
					{
						META_LOG("[Meta][FT] File%d DeleteFile succeed! ",file_ID);
						return_value = TRUE;
						WriteDataToPC(pft_cnf, sizeof(FT_NVRAM_BACKUP_CNF),NULL, 0);
					}
					return return_value;
				}
				else
				{
					pft_cnf->block.stage |= BLK_WRITE;
					META_LOG("[Meta][FT] File%d backups %d data ! ",file_ID,peer_buff_size);
					WriteDataToPC(pft_cnf, sizeof(FT_NVRAM_BACKUP_CNF),peer_buf, peer_buff_size);
					memset(peer_buf,0,NVRAM_PEER_MAX_LEN);
					pft_cnf->block.stage &= ~BLK_CREATE;
				}

			}
			else
			{
				pft_cnf->block.stage |= BLK_EOF;
				META_LOG("[Meta][FT] File%d backups read failed ! ", file_ID);
			}

		}

		free(peer_buf);
		close(backupFd);

	}
	else
	{
		META_LOG("[Meta][FT] File%d backups open failed ! ", file_ID);
	}

	return return_value;

}


void FtModNvramBackup::exec(Frame *pFrm)
{
	CmdTarget::exec(pFrm);

	META_LOG("[Meta][FT] FT_NvramBackup_OP ");

	FT_NVRAM_BACKUP_CNF ft_cnf;
	int bFileOpResult = 0;
	memset(&ft_cnf, 0, sizeof(FT_NVRAM_BACKUP_CNF));
	//init the header
	ft_cnf.header.id = pFrm->getCmdTarget()->getId() + 1;
	ft_cnf.header.token = pFrm->getCmdTarget()->getToken();
	ft_cnf.status = META_FAILED;

	FT_NVRAM_BACKUP_REQ *req = (FT_NVRAM_BACKUP_REQ *)pFrm->localBuf();

	if (req->count > 0)
	{
		META_LOG("[Meta][FT] Count is %d, backup parts of NvRam!", req->count);
		bFileOpResult = FileOp_BackupData_Special(req->buffer, req->count, req->mode);
	}
	else
	{
		META_LOG("[Meta][FT] Count is %d, backup all NvRam!", req->count);
		bFileOpResult = FileOp_BackupAll_NvRam();
	}
	
	if(bFileOpResult)
	{
	    META_LOG("[Meta][FT] NVM_PcBackup_Get_Data Start ! ");
		if(SendNVRAMFile(0,&ft_cnf))
		{
			META_LOG("[Meta][FT] Send file 0 succeed! ! ");
			//init the header
			ft_cnf.header.id = req->header.id +1;
			ft_cnf.header.token = req->header.token;
			ft_cnf.status = META_FAILED;
			ft_cnf.block.file_size = 0;

			if(SendNVRAMFile(1,&ft_cnf))
			{
				META_LOG("[Meta][FT] Send file 1 succeed! ! ");
				return;
			}
		}
	}
	else
	{
		META_LOG("[Meta][FT] Failed to backup NvRam!");
	}

	WriteDataToPC(&ft_cnf, sizeof(FT_NVRAM_BACKUP_CNF),NULL, 0);

}

///////////////////////////////////////////////////////////////////////////////////////////////

FtModNvramRestore::FtModNvramRestore(void)
	:CmdTarget(FT_NVRAM_RESTORE_REQ_ID)
{
}

FtModNvramRestore::~FtModNvramRestore(void)
{
}


void FtModNvramRestore::exec(Frame *pFrm)
{
	CmdTarget::exec(pFrm);

	META_LOG("[Meta][FT] FT_NvramRestore_OP ");
	
	FT_NVRAM_RESTORE_CNF ft_cnf;
	memset(&ft_cnf, 0, sizeof(FT_NVRAM_RESTORE_CNF));

	FT_NVRAM_RESTORE_REQ *req = (FT_NVRAM_RESTORE_REQ *)pFrm->localBuf();
	
	//init the header
	ft_cnf.header.id = req->header.id +1;
	ft_cnf.header.token = req->header.token;
	ft_cnf.status = META_FAILED;
	
	int backupFd;
	unsigned int fileLen;
	META_LOG("[Meta][FT] FT_NVRAM_Restore_OP receive block stage %x  file id %d file size %d!",req->block.stage,req->block.file_ID,req->block.file_size);
	if(req->block.stage & BLK_CREATE)
	{
		backupFd = open(makepath(req->block.file_ID), O_RDWR | O_TRUNC | O_CREAT, 0777);
	}
	else
	{
		backupFd = open(makepath(req->block.file_ID), O_RDWR | O_APPEND);
	}
	
	if(backupFd >= 0)
	{
		META_LOG("[Meta][FT] FT_NVRAM_Restore_OP create or open file OK!");
		unsigned short sWriten = 0;
		sWriten = write(backupFd,pFrm->peerBuf(),pFrm->peerLen());

		if(sWriten)
		{
			ft_cnf.status = META_SUCCESS;
			META_LOG("[Meta][FT] FT_NVRAM_Restore_OP File%d write %d data total data %d!",req->block.file_ID,sWriten,pFrm->peerLen());
			if(req->block.stage & BLK_EOF)
			{
				fileLen = getFileSize(backupFd);
				if(req->block.file_size == fileLen)
				{
					META_LOG("[Meta][FT] FT_NVRAM_Restore_OP write file transfer success! ");
					close(backupFd);
					backupFd = -1;
	
					if(req->block.file_ID == 1)
					{
						if(!FileOp_RestoreAll_NvRam())
						{
							ft_cnf.status = META_FAILED;
							META_LOG("[Meta][FT] META_Editor_PcRestore_Set_Data failed! ");
	
						}
					}
				}
				else
				{
					ft_cnf.status = META_FAILED;
					META_LOG("[Meta][FT] FT_NVRAM_Restore_OP file %d size error! / %d ",req->block.file_ID,req->block.file_size);
				}
			}
		}
		else
		{
			META_LOG("[Meta][FT] FT_NVRAM_Restore_OP write file failed!");
		}
	
		if(backupFd != -1)
			close(backupFd);
	
	
	}
	else
	{
		META_LOG("[Meta][FT] FT_NVRAM_Restore_OP create or open file failed!");
	}
	
	WriteDataToPC(&ft_cnf, sizeof(FT_NVRAM_RESTORE_CNF),NULL, 0);

}



///////////////////////////////////////////////////////////////////////////////////////////////

FtModNvramReset::FtModNvramReset(void)
	:CmdTarget(FT_NVRAM_RESET_REQ_ID)
{
}

FtModNvramReset::~FtModNvramReset(void)
{
}

void FtModNvramReset::exec(Frame *pFrm)
{
	CmdTarget::exec(pFrm);

	META_LOG("[Meta][FT] FT_NvramReset_OP ");
	FT_AP_Editor_reset_cnf ft_cnf;
 
    memset(&ft_cnf, 0, sizeof(FT_AP_Editor_reset_cnf));

	FT_AP_Editor_reset_req *req = (FT_AP_Editor_reset_req *)pFrm->localBuf();

    //if the reset_category and file_idx is 0xfc and 0xfccf, we reset all nvram files.
    if ((req->reset_category == 0xfc )&& (req->file_idx ==0xfccf))
        ft_cnf = META_Editor_ResetAllFile_OP(req);	//reset all files
    else
        ft_cnf = META_Editor_ResetFile_OP(req);		//reset one nvram file

    //fill the ft module header
    ft_cnf.header.id = req->header.id +1;
    ft_cnf.header.token = req->header.token;
    ft_cnf.status = META_SUCCESS;

    WriteDataToPC(&ft_cnf, sizeof(FT_AP_Editor_reset_cnf),NULL, 0);

}

///////////////////////////////////////////////////////////////////////////////////////////////
FtModNvramRead::FtModNvramRead(void)
	:CmdTarget(FT_NVRAM_READ_REQ_ID)
{
}

FtModNvramRead::~FtModNvramRead(void)
{
}

void FtModNvramRead::exec(Frame *pFrm)
{
	CmdTarget::exec(pFrm);

	META_LOG("[Meta][FT] FT_NvramRead_OP ");
 	// just call the inferface of ap_editor lib which will reture the data after reading sucessfully
 	if (!META_Editor_ReadFile_OP((FT_AP_Editor_read_req *)pFrm->localBuf()))
	 	META_LOG("[Meta][FT] FT_APEditorR_OP META Test Fail");

}

///////////////////////////////////////////////////////////////////////////////////////////////

FtModNvramWrite::FtModNvramWrite(void)
	:CmdTarget(FT_NVRAM_WRITE_REQ_ID)
{
}

FtModNvramWrite::~FtModNvramWrite(void)
{
}

void FtModNvramWrite::exec(Frame *pFrm)
{
	CmdTarget::exec(pFrm);

	META_LOG("[Meta][FT] FT_NvramWrite_OP ");

    FT_AP_Editor_write_cnf ft_cnf;

    memset(&ft_cnf, 0, sizeof(FT_AP_Editor_write_cnf));
	FT_AP_Editor_write_req *req = (FT_AP_Editor_write_req *)pFrm->localBuf();

    //// just call the inferface of ap_editor lib
    ft_cnf = META_Editor_WriteFile_OP(req, (char *)pFrm->peerBuf(), pFrm->peerLen());



    //fill the ft module header
    ft_cnf.header.id = req->header.id +1;
    ft_cnf.header.token = req->header.token;
    ft_cnf.status = META_SUCCESS;

    WriteDataToPC(&ft_cnf, sizeof(FT_AP_Editor_write_cnf),NULL, 0);

}
#endif

///////////////////////////////////////////////////////////////////////////////////////////////


FtModTestAlive::FtModTestAlive(void)
	:CmdTarget(FT_IS_ALIVE_REQ_ID)
{
}

FtModTestAlive::~FtModTestAlive(void)
{
}

void FtModTestAlive::exec(Frame *pFrm)
{
	META_LOG("[Meta][FT] FT_TestAlive");
	FT_IS_ALIVE_CNF ft_cnf;
	memset(&ft_cnf, 0, sizeof(FT_IS_ALIVE_CNF));

	CmdTarget::exec(pFrm);

	
	//just give the respone.
	ft_cnf.header.id = pFrm->getCmdTarget()->getId()+1;
	ft_cnf.header.token = pFrm->getCmdTarget()->getToken();
	WriteDataToPC(&ft_cnf, sizeof(FT_IS_ALIVE_CNF),NULL, 0);
}

///////////////////////////////////////////////////////////////////////////////////////////////
//for normal version
#define VERSION_FILE_PATH           "/system/build.prop"

#define RELEASE_SW_TOKEN            "ro.mediatek.version.release"
#define RELEASE_PLATFORM_TOKEN      "ro.board.platform"
#define RELEASE_CHIP_TOKEN          "ro.mediatek.chip_ver"
#define RELEASE_PRODUCT_TOKEN       "ro.product.name"
#define RELEASE_BUILD_TIME_TOKEN    "ro.build.date"
#define RELEASE_BUILD_DISP_ID_TOKEN "ro.build.display.id"

FtModVersionInfo::FtModVersionInfo(void)
	:CmdTarget(FT_VER_INFO_REQ_ID)
{
}

FtModVersionInfo::~FtModVersionInfo(void)
{
}

void FtModVersionInfo::exec(Frame *pFrm)
{
	CmdTarget::exec(pFrm);
	META_LOG("[Meta][FT] FT_VersionInfo");
	FT_VER_INFO_CNF ft_cnf;
	unsigned int dwRc = 0;
	unsigned int dwValSize = 0;
	unsigned int dwValType = 0;
	char szBuffer[MAX_PATH] = {0};
	FILE *fd = 0;
	char str[256] = {0};
	char *loc = NULL;
	
	memset(&ft_cnf, 0, sizeof(ft_cnf));
	memset(szBuffer,0, sizeof(szBuffer));
	char* tmp = NULL;
	char platform[256] = {0};
	char chipVersion[256] = {0};
	
	//initail the value of ft header
	ft_cnf.header.id = pFrm->getCmdTarget()->getId()+1;
	ft_cnf.header.token = pFrm->getCmdTarget()->getToken();
	ft_cnf.status = META_FAILED;
	
	if((fd = fopen(VERSION_FILE_PATH,"r"))==NULL)
	{
		META_LOG("[Meta][FT] FT_GetVersionInfo Can't open file : %s\n", VERSION_FILE_PATH);
	}
	else
	{
	    while(!feof(fd))
	    {
		    if(fgets(str, 256, fd)!=NULL)
		    {
			    tmp = str;
			    loc = strsep(&tmp, "=");
			    if(!strcmp(loc, RELEASE_SW_TOKEN))
			    {
				    META_LOG("[Meta][FT] SW Version = %s\n", tmp);
				    strncpy((char*)ft_cnf.sw_ver, tmp, 63);
			    }
			    if(!strcmp(loc, RELEASE_PLATFORM_TOKEN))
			    {
				    META_LOG("[Meta][FT] Platform = %s\n", tmp);
				    strncpy(platform, tmp, 255);
			    }
			    if(!strcmp(loc, RELEASE_PRODUCT_TOKEN))
			    {
				    META_LOG("[Meta][FT] Product Name = %s\n", tmp);
			    }
			    if(!strcmp(loc, RELEASE_CHIP_TOKEN))
			    {
				    META_LOG("[Meta][FT] Chip Version = %s\n", tmp);
				    strncpy(chipVersion, tmp, 255);
                    strncpy((char*)ft_cnf.hw_ver, tmp, 63);
			    }
			    if(!strcmp(loc, RELEASE_BUILD_TIME_TOKEN))
			    {
				    META_LOG("[Meta][FT] Build Time = %s\n", tmp);
				    strncpy((char*)ft_cnf.sw_time, tmp, 63);
			    }
		    }
	    }
	}
	
	int i = 0;
	int k = 0;
	while (i < 256)
	{
		if (platform[i] != '\r' && platform[i] != '\n')
			szBuffer[k++] = platform[i++];				
		else
			break;
	}
	szBuffer[k++] = ',';
	i = 0;
	while (i < 256)
	{
		if (chipVersion[i] != '\r' && chipVersion[i] != '\n')
			szBuffer[k++] = chipVersion[i++];
		else
			break;
	}
	szBuffer[k++] = '\0';
	if (strlen(szBuffer) <= 64)
	{
		strncpy((char*)ft_cnf.bb_chip, szBuffer,strlen(szBuffer));
	}
	else
	{
		META_LOG("[Meta][FT] String is too long, length=%d ", strlen(szBuffer));
	}

	if(fd != NULL)
	{
	    fclose(fd);
	}
	
	/* Get Software version : ft_cnf.sw_ver */
	META_LOG("[Meta][FT] ft_cnf.sw_ver = %s ", ft_cnf.sw_ver);
	
	/* Get the build time : ft_cnf.sw_ver */
	META_LOG("[Meta][FT] ft_cnf.sw_time = %s ", ft_cnf.sw_time);
	
	/* Get the chip version : ft_cnf.sw_ver */
	META_LOG("[Meta][FT] ft_cnf.bb_chip = %s ", ft_cnf.bb_chip);
	META_LOG("[Meta][FT] ft_cnf.hw_ver = %s ", ft_cnf.hw_ver);
	
	ft_cnf.status = META_SUCCESS;
	
Ver_error:
	WriteDataToPC(&ft_cnf, sizeof(ft_cnf),NULL, 0);

}

///////////////////////////////////////////////////////////////////////////////////////////////

FtModVersionInfo2::FtModVersionInfo2(void)
	:CmdTarget(FT_VER_INFO_V2_REQ_ID)
{
}

FtModVersionInfo2::~FtModVersionInfo2(void)
{
}

void FtModVersionInfo2::exec(Frame *pFrm)
{
	CmdTarget::exec(pFrm); 
	META_LOG("[Meta][FT] FT_VersionInfo2");
	FT_VER_INFO_V2_CNF ft_cnf;
    unsigned int dwRc = 0;
    unsigned int dwValSize = 0;
    unsigned int dwValType = 0;
    char szBuffer[MAX_PATH] = {0};
    FILE *fd = 0;
    char str[256] = {0};
    char *loc = NULL;

    memset(&ft_cnf, 0, sizeof(ft_cnf));
    memset(szBuffer,0, sizeof(szBuffer));
    char* tmp = NULL;
    char platform[256] = {0};
    char chipVersion[256] = {0};

    //initail the value of ft header
    ft_cnf.header.id = pFrm->getCmdTarget()->getId() +1;
    ft_cnf.header.token = pFrm->getCmdTarget()->getToken();
    ft_cnf.status = META_FAILED;

    META_LOG("[Meta][FT] FT_GetVersionInfoV2 ");

    if((fd = fopen(VERSION_FILE_PATH,"r"))==NULL)
    {
        META_LOG("[Meta][FT] FT_GetVersionInfo Can't open file : %s\n", VERSION_FILE_PATH);
    }
    else
    {
        while(!feof(fd))
        {
            if(fgets(str, 256, fd)!=NULL)
            {
                tmp = str;
                loc = strsep(&tmp, "=");
                if(!strcmp(loc, RELEASE_SW_TOKEN))
                {
                    META_LOG("[Meta][FT] SW Version = %s\n", tmp);
				    strncpy((char*)ft_cnf.sw_ver, tmp, 63);
                }
                if(!strcmp(loc, RELEASE_PLATFORM_TOKEN))
                {
                    META_LOG("[Meta][FT] Platform = %s\n", tmp);
				    strncpy(platform, tmp, 255);
                }
                if(!strcmp(loc, RELEASE_PRODUCT_TOKEN))
                {
                    META_LOG("[Meta][FT] Product Name = %s\n", tmp);
                }
                if(!strcmp(loc, RELEASE_CHIP_TOKEN))
                {
                    META_LOG("[Meta][FT] Chip Version = %s\n", tmp);
				    strncpy(chipVersion, tmp, 255);
                    strncpy((char*)ft_cnf.hw_ver, tmp, 63);
                }
                if(!strcmp(loc, RELEASE_BUILD_TIME_TOKEN))
                {
                    META_LOG("[Meta][FT] Build Time = %s\n", tmp);
                    strncpy((char*)ft_cnf.sw_time, tmp, 63);
                }
                if(!strcmp(loc, RELEASE_BUILD_DISP_ID_TOKEN))
                {
                    META_LOG("[Meta][FT] Build Display ID = %s\n", tmp);
                    strncpy((char*)ft_cnf.build_disp_id, tmp, 63);
                }
             }
         }
    }

    int i = 0;
    int k = 0;
	while (i < 256)
	{
		if (platform[i] != '\r' && platform[i] != '\n')
			szBuffer[k++] = platform[i++];
		else
			break;
	}
	szBuffer[k++] = ',';
	i = 0;
	while (i < 256)
	{
		if (chipVersion[i] != '\r' && chipVersion[i] != '\n')
			szBuffer[k++] = chipVersion[i++];
		else
			break;
	}
	szBuffer[k++] = '\0';
	if (strlen(szBuffer) <= 64)
	{
		strncpy((char*)ft_cnf.bb_chip, szBuffer,strlen(szBuffer));
	}
	else
	{
		META_LOG("[Meta][FT] String is too long, length=%d ", strlen(szBuffer));
	}
	if(fd != NULL)
    { 
        fclose(fd);
	}

	/* Get Software version : ft_cnf.sw_ver */
	META_LOG("[Meta][FT] ft_cnf.sw_ver = %s ", ft_cnf.sw_ver);

	/* Get the build time : ft_cnf.sw_ver */
	META_LOG("[Meta][FT] ft_cnf.sw_time = %s ", ft_cnf.sw_time);

	/* Get the chip version : ft_cnf.sw_ver */
	META_LOG("[Meta][FT] ft_cnf.bb_chip = %s ", ft_cnf.bb_chip);
	META_LOG("[Meta][FT] ft_cnf.hw_ver = %s ", ft_cnf.hw_ver);

    ft_cnf.status = META_SUCCESS;

Ver_error:
    WriteDataToPC(&ft_cnf, sizeof(ft_cnf),NULL, 0);


}

///////////////////////////////////////////////////////////////////////////////////////////////
//for chip version
#define CHIPINFO_CODE_FUNC          "/proc/chip/code_func"
#define CHIPINFO_CODE_PROJ          "/proc/chip/code_proj"
#define CHIPINFO_CODE_DATE          "/proc/chip/code_date"
#define CHIPINFO_CODE_FAB           "/proc/chip/code_fab"

FtModChipInfo::FtModChipInfo(void)
	:CmdTarget(FT_CHIP_INFO_REQ_ID)
{
}

FtModChipInfo::~FtModChipInfo(void)
{
}

void FtModChipInfo::exec(Frame *pFrm)
{
	CmdTarget::exec(pFrm);
	META_LOG("[Meta][FT] FT_ChipInfo");
	FT_CHIP_INFO_CNF ft_cnf;
	FILE *fd = 0;
	char szInfoPath[][32] = { 
			CHIPINFO_CODE_FUNC,
			CHIPINFO_CODE_PROJ,
		    CHIPINFO_CODE_DATE,
			CHIPINFO_CODE_FAB};

	char chip_info[4][64] = {0};
	memset(&ft_cnf, 0, sizeof(ft_cnf));
	
	//initail the value of ft header
	ft_cnf.header.id = pFrm->getCmdTarget()->getId()+1;
	ft_cnf.header.token = pFrm->getCmdTarget()->getToken();
	ft_cnf.status = META_FAILED;
	
	/* Get the chip info */
	int i = 0;
	for(i; i<4; i++)
	{
		if((fd = fopen(szInfoPath[i], "r")) != NULL)
		{
			if(fgets((char*)chip_info[i], 64, fd) != NULL)
			{
				META_LOG("[Meta][FT] %s = %s\n", szInfoPath[i], chip_info[i]);
			}
			else
			{
				META_LOG("[Meta][FT] failed to read <%s>\n", szInfoPath[i]);
			}

			fclose(fd);
		}
		else
		{
			META_LOG("[Meta][FT] Can't open file : %s\n", szInfoPath[i]);
			break;
		}
	}

	if(i == 4)
	{
		strncpy((char*)ft_cnf.code_func, chip_info[0], 63);
		strncpy((char*)ft_cnf.code_proj, chip_info[1], 63);
		strncpy((char*)ft_cnf.code_date, chip_info[2], 63);
		strncpy((char*)ft_cnf.code_fab, chip_info[3], 63);
		ft_cnf.status = META_SUCCESS;
	}
	
	WriteDataToPC(&ft_cnf, sizeof(ft_cnf),NULL, 0);

}
///////////////////////////////////////////////////////////////////////////////////////////////
FtModPowerOff::FtModPowerOff(void)
	:CmdTarget(FT_POWER_OFF_REQ_ID)
{
}

FtModPowerOff::~FtModPowerOff(void)
{
}

void FtModPowerOff::exec(Frame *pFrm)
{
	CmdTarget::exec(pFrm);

    META_LOG("[Meta][FT] FT_PowerOff");

	
	FT_POWER_OFF_REQ *req = (FT_POWER_OFF_REQ *)pFrm->localBuf();

	//sync();
    usleep(200*1000);

    int comPortType;
    comPortType = getComType();
    if(comPortType == META_USB_COM)
    {

		FILE *PUsbFile = NULL;
        PUsbFile = fopen("sys/devices/platform/mt_usb/cmode","w");
		if(PUsbFile == NULL)
		{
           	META_LOG("[Meta][FT] Could not open sys/devices/platform/mt_usb/cmode ");
			PUsbFile = fopen("/sys/devices/platform/musb-mtu3d/musb-hdrc/cmode","w");
			if(PUsbFile == NULL)
			{
			    META_LOG("[Meta][FT] Could not open /sys/devices/platform/musb-mtu3d/musb-hdrc/cmode ");
			}
			else
		    {
			    fputc('0',PUsbFile);
			    fclose(PUsbFile);
		    }
		}
		else
		{
			fputc('0',PUsbFile);
			fclose(PUsbFile);
		}
		
    }
    else
    {
        META_LOG("[Meta][FT] com port type is uart! ");
    }
    if(req->dummy == 0)
    {
        usleep(1000 * 1000);
		//reboot(RB_POWER_OFF);
		property_set("sys.powerctl","shutdown");
	}
    if(req->dummy == 2)
    {
		usleep(1000 * 1000);
		//reboot(RB_AUTOBOOT);
		property_set("sys.powerctl","reboot");
    }
}

///////////////////////////////////////////////////////////////////////////////////////////////
FtModReboot::FtModReboot(void)
	:CmdTarget(FT_REBOOT_REQ_ID)
{
}

FtModReboot::~FtModReboot(void)
{
}

void FtModReboot::exec(Frame *pFrm)
{
	CmdTarget::exec(pFrm);

    META_LOG("[Meta][FT] FT_Reboot");

	
	FT_META_REBOOT_REQ *req = (FT_META_REBOOT_REQ *)pFrm->localBuf();

	sleep(req->delay);

    //Reboot target side after finishing the meta
	//sync();
	//reboot(RB_AUTOBOOT);
	property_set("sys.powerctl","reboot");
}

///////////////////////////////////////////////////////////////////////////////////////////////
FtModBuildProp::FtModBuildProp(void)
	:CmdTarget(FT_BUILD_PROP_REQ_ID)
{
}

FtModBuildProp::~FtModBuildProp(void)
{
}

void FtModBuildProp::exec(Frame *pFrm)
{
	CmdTarget::exec(pFrm);

    META_LOG("[Meta][FT] FT_BuildProp");

	FT_BUILD_PROP_CNF ft_cnf;
	FT_BUILD_PROP_REQ *req = (FT_BUILD_PROP_REQ *)pFrm->localBuf();

	ft_cnf.header.id = req->header.id +1;
    ft_cnf.header.token = req->header.token;
	
    property_get((const char*)req->tag, (char *)ft_cnf.content, "unknown");	

	META_LOG("[Meta][FT] %s = %s ",req->tag,ft_cnf.content);

	ft_cnf.status = META_SUCCESS;

	WriteDataToPC(&ft_cnf, sizeof(ft_cnf),NULL, 0);	
}

///////////////////////////////////////////////////////////////////////////////////////////////
#ifdef FT_GSENSOR_FEATURE

FtModGSensor::FtModGSensor(void)
	:CmdTarget(FT_GSENSOR_REQ_ID)
{
}

FtModGSensor::~FtModGSensor(void)
{
}

void FtModGSensor::exec(Frame *pFrm)
{

	CmdTarget::exec(pFrm);

     //do the G-Sensor test by called the interface in meta G-Sensor lib
    if(getInitState())
	    Meta_GSensor_OP((GS_REQ *)pFrm->localBuf(), (char*)pFrm->peerBuf(), pFrm->peerLen());

}

int FtModGSensor::init(Frame*pFrm)
{
	META_LOG("[Meta][FT] FT_GSensor");	
	
  	GS_CNF ft_cnf;
    static int bInitFlag_GS = false;
	GS_REQ *req = (GS_REQ *)pFrm->localBuf();


    memset(&ft_cnf, 0, sizeof(GS_CNF));

    if (false == bInitFlag_GS)
    {
        // initial the G-Sensor module when it is called first time
        if (!Meta_GSensor_Open())
        {
            ft_cnf.header.id = req->header.id +1;
            ft_cnf.header.token = req->header.token;
            ft_cnf.status = META_FAILED;
		    ft_cnf.op = req->op;
			 
            META_LOG("[Meta][FT] FT_GSENSOR_OP Meta_GSensor_Open Fail ");
            WriteDataToPC(&ft_cnf, sizeof(ft_cnf),NULL, 0);
			return false;
        }
        bInitFlag_GS = true;
    }
	return true;
}

#endif


///////////////////////////////////////////////////////////////////////////////////////////////
#ifdef FT_MSENSOR_FEATURE

FtModMSensor::FtModMSensor(void)
	:CmdTarget(FT_MSENSOR_REQ_ID)
{
   memset(&m_ft_cnf, 0, sizeof(FT_MSENSOR_CNF));
}

FtModMSensor::~FtModMSensor(void)
{
}

void FtModMSensor::exec(Frame *pFrm)
{

  META_LOG("[Meta][FT] FT_MSensor");	
    
	int res = -1;
  	memset(&m_ft_cnf, 0, sizeof(FT_MSENSOR_CNF));
	
	FT_MSENSOR_REQ *req = (FT_MSENSOR_REQ *)pFrm->localBuf();
	
	m_ft_cnf.header.id = req->header.id + 1;
	m_ft_cnf.header.token = req->header.token;
	m_ft_cnf.status = META_SUCCESS;

  	CmdTarget::exec(pFrm);

	if(getInitState())
	{
		res = Meta_MSensor_OP();
		if (0 == res)
		{
		    META_LOG("[Meta][FT] FT_MSENSOR_OP Meta_MSensor_OP success!");
		    m_ft_cnf.status = META_SUCCESS;
		}
		else
		{
		    META_LOG("[Meta][FT] FT_MSENSOR_OP Meta_MSensor_OP failed!");
		    m_ft_cnf.status = META_FAILED;
		}

	  WriteDataToPC(&m_ft_cnf, sizeof(m_ft_cnf),NULL, 0);
	}

}

int FtModMSensor::init(Frame*)
{
	static int bInitFlag_MS = false;
	

	if (false == bInitFlag_MS)
    {
        // initial the M-Sensor module when it is called first time
        if (!Meta_MSensor_Open())
        {
            META_LOG("[Meta][FT] FT_MSENSOR_OP Meta_MSensor_Open failed!");
			m_ft_cnf.status = META_FAILED;
            WriteDataToPC(&m_ft_cnf, sizeof(m_ft_cnf),NULL, 0);
			return false;
        }
        bInitFlag_MS = TRUE;
    }

	return true;
}
#endif


///////////////////////////////////////////////////////////////////////////////////////////////

#ifdef FT_ALSPS_FEATURE

FtModALSPS::FtModALSPS(void)
	:CmdTarget(FT_ALSPS_REQ_ID)
{
    memset(&m_ft_cnf, 0, sizeof(FT_ALSPS_CNF));
}

FtModALSPS::~FtModALSPS(void)
{
}

void FtModALSPS::exec(Frame *pFrm)
{
	META_LOG("[Meta][FT] FT_ALSPS");	
	
	int res = -1;
  	memset(&m_ft_cnf, 0, sizeof(FT_ALSPS_CNF));
	
	FT_ALSPS_REQ *req = (FT_ALSPS_REQ *)pFrm->localBuf();
		
	m_ft_cnf.header.id = req->header.id + 1;
	m_ft_cnf.header.token = req->header.token;
	m_ft_cnf.status = META_SUCCESS;

	CmdTarget::exec(pFrm);

	if(getInitState())
	{		
	    res = Meta_ALSPS_OP();
	    if (0 == res)
	    {
	        META_LOG("[Meta][FT] FT_ALSPS_OP Meta_ALSPS_OP success!");
	        m_ft_cnf.status = META_SUCCESS;
	    }
		else
		{
		    META_LOG("[Meta][FT] FT_ALSPS_OP Meta_ALSPS_OP failed!");
		    m_ft_cnf.status = META_FAILED;
		}
	    WriteDataToPC(&m_ft_cnf, sizeof(m_ft_cnf),NULL, 0);
	}

}

int FtModALSPS::init(Frame*)
{
	static int bInitFlag_ALSPS = false;

	if (false == bInitFlag_ALSPS)
    {
        // initial the M-Sensor module when it is called first time
        if (!Meta_ALSPS_Open())
        {
            META_LOG("[Meta][FT] FT_ALSPS_OP Meta_ALSPS_Open failed!");
			m_ft_cnf.status = META_FAILED;
            WriteDataToPC(&m_ft_cnf, sizeof(m_ft_cnf),NULL, 0);
			return false;
        }
        bInitFlag_ALSPS = true;
    }
	return true;
}
#endif

///////////////////////////////////////////////////////////////////////////////////////////////
#ifdef FT_GYROSCOPE_FEATURE

FtModGyroSensor::FtModGyroSensor(void)
	:CmdTarget(FT_GYROSCOPE_REQ_ID)
{
}

FtModGyroSensor::~FtModGyroSensor(void)
{
}

void FtModGyroSensor::exec(Frame *pFrm)
{

	CmdTarget::exec(pFrm);

    //do the Gyroscope-Sensor test by called the interface in meta Gyroscope-Sensor lib  
	if(getInitState())
		Meta_Gyroscope_OP((GYRO_REQ *)pFrm->localBuf(), (char *)pFrm->peerBuf(), pFrm->peerLen());

}

int FtModGyroSensor::init(Frame* pFrm)
{
 	META_LOG("[Meta][FT] FT_GyroSensor");
	
	GYRO_REQ *req = (GYRO_REQ *)pFrm->localBuf();
	
	
    static int bInitFlag_GYRO = false;
	GYRO_CNF ft_cnf;
	memset(&ft_cnf, 0, sizeof(GYRO_CNF));
	
    if (false == bInitFlag_GYRO)
    {
        // initial the Gyroscope-Sensor module when it is called first time
        if (!Meta_Gyroscope_Open())
        {
            ft_cnf.header.id = req->header.id +1;
            ft_cnf.header.token = req->header.token;
            ft_cnf.status = META_FAILED;
	    	ft_cnf.op = req->op;

            META_LOG("[Meta][FT] FT_GYROSENSOR_OP Meta_GYROSensor_Open Fail ");
            WriteDataToPC(&ft_cnf, sizeof(ft_cnf),NULL, 0);
			return false;
        }
        bInitFlag_GYRO = true;
    }

	return true;
}
#endif

///////////////////////////////////////////////////////////////////////////////////////////////

FtModModemInfo::FtModModemInfo(void)
	:CmdTarget(FT_MODEM_REQ_ID)
{
}

FtModModemInfo::~FtModModemInfo(void)
{
}

int FtModModemInfo::getModemCapability(MODEM_CAPABILITY_LIST_CNF* modem_capa)
{
	int modem_type = 0;	
	modem_type = getMdmType();

	if((modem_type & MD1_INDEX) == MD1_INDEX)
	{
	    if(getModemProtocol(0, (void*)modem_capa) == 0)
	    {
	        META_LOG("[Meta][FT] MD1 getModemProtocol fail");
	        return 0;
	    }
	}

	if((modem_type & MD2_INDEX) == MD2_INDEX)
	{
	    if(getModemProtocol(1, (void*)modem_capa) == 0)
	    {
	        META_LOG("[Meta][FT] MD2 getModemProtocol fail");
	        return 0;
	    }
	}

	if((modem_type & MD3_INDEX) == MD3_INDEX)
	{
	    if(getModemProtocol(2, (void*)modem_capa) == 0)
	    {
	        META_LOG("[Meta][FT] MD3 getModemProtocol fail");
	        return 0;
	    }
	}

	if((modem_type & MD5_INDEX) == MD5_INDEX)
	{	
	    if(getModemProtocol(4, (void*)modem_capa) == 0)
	    {
	        META_LOG("[Meta][FT] MD5 getModemProtocol fail");
	        return 0;
	    }
	}

	return 1;
}


void FtModModemInfo::exec(Frame *pFrm)
{
	CmdTarget::exec(pFrm);

    META_LOG("[Meta][FT] FT_ModemInfo");	

	FT_MODEM_CNF ft_cnf;
	int modem_req=0;
	int fd = -1;
	int type_define=0;
	char dev_node[32] = {0};
	static int modemCreate = 0;
	int bDataDevice = FALSE;
	int nModemIndex = 0;

	memset(&ft_cnf, 0, sizeof(FT_MODEM_CNF));
	ft_cnf.status = META_FAILED;

	
	FT_MODEM_REQ *req = (FT_MODEM_REQ *)pFrm->localBuf();
    META_LOG("[Meta][FT] FT_MODEM_INFO_OP, req type:%d ",req->type);

	if(req->type == FT_MODEM_OP_QUERY_INFO)
	{
		type_define = 1;	
		ft_cnf.result.query_modem_info_cnf.modem_number = getMdmNumber();
		ft_cnf.result.query_modem_info_cnf.modem_id = getActiveMdmId();
		ft_cnf.status = META_SUCCESS;
	}
	else if(req->type == FT_MODEM_OP_CAPABILITY_LIST)
	{
		type_define = 1;
		int nRet = 0;
		MODEM_CAPABILITY_LIST_CNF modem_capa;
		memset(&modem_capa, 0, sizeof(MODEM_CAPABILITY_LIST_CNF));
		nRet = getModemCapability(&modem_capa);
		memcpy(&ft_cnf.result.query_modem_cap_cnf,&modem_capa,sizeof(MODEM_CAPABILITY_LIST_CNF));
		if (nRet == 1)
		{
		    ft_cnf.status = META_SUCCESS;
		}
		else
		{
		    ft_cnf.status = META_FAILED;
		}
	}
	else if(req->type == FT_MODEM_OP_QUERY_MDDOWNLOADSTATUS)
	{
		type_define = 1;
		char temp[128]={0};
		int percentage = 0;
		int status_code = 0;
		property_get("persist.sys.extmddlprogress",temp,NULL);
		META_LOG("[Meta][FT] persist.sys.extmddlprogress = %s",temp);
		sscanf(temp,"%03d_%04d",&percentage,&status_code);
		META_LOG("[Meta][FT] FT_MODEM_INFO_OP FT_MODEM_OP_QUERY_MDDOWNLOADSTATUS percentage = %d,status_code = %d",percentage,status_code);
		ft_cnf.result.query_modem_download_status_cnf.percentage = percentage;
		ft_cnf.result.query_modem_download_status_cnf.status_code = status_code;
		
		if(percentage == 110 && status_code ==0 && modemCreate == 0)
		{
			unsigned int modemType = getMdmType();
			if((modemType & MD5_INDEX) == MD5_INDEX)
			{
				createModemThread(4,1);
			}
			modemCreate = 1;
		}
		ft_cnf.status = META_SUCCESS;
	}

    if(req->type == FT_MODEM_OP_SET_MODEMTYPE)
    {
        nModemIndex = req->cmd.set_modem_type_req.modem_id;
    }
	else if(req->type == FT_MODEM_OP_GET_CURENTMODEMTYPE)
	{
	    nModemIndex = req->cmd.get_currentmodem_type_req.modem_id;
	}
	else if(req->type == FT_MODEM_OP_QUERY_MDIMGTYPE)
	{
	    nModemIndex = req->cmd.query_modem_imgtype_req.modem_id;
	}
    else
    {
        META_LOG("[Meta][FT] FtModModemInfo unknown request type:%d",req->type);
    }
	if(nModemIndex > 0 && nModemIndex < 6)
	{
	    nModemIndex = nModemIndex - 1;
	}
	else
	{
	    nModemIndex = 0;
	}	

	if((nModemIndex == 0) || (nModemIndex == 1) || (nModemIndex == 4 && ccci_get_version() == EDSDA))
	{
        snprintf(dev_node, 32, "%s", ccci_get_node_name(USR_META_IOCTL,(CCCI_MD)nModemIndex));
	    fd = open(dev_node, O_RDWR|O_NOCTTY|O_NDELAY );
		bDataDevice = FALSE;
	}
	else
	{
	    unsigned short id = getActiveMdmId() - 1; 
		fd = getModemHandle(id);
		bDataDevice = TRUE;
	}

	if(fd < 0 && (req->type == FT_MODEM_OP_SET_MODEMTYPE || req->type == FT_MODEM_OP_GET_CURENTMODEMTYPE|| req->type == FT_MODEM_OP_QUERY_MDIMGTYPE))
	{
		ft_cnf.status = META_FAILED;
		META_LOG("[Meta][FT] FtModModemInfo Open MD%d device note %s fail ",(nModemIndex+1),dev_node);
		ft_cnf.header.id = req->header.id +1;
    	ft_cnf.header.token = req->header.token;
		ft_cnf.type = req->type;	
	
		WriteDataToPC(&ft_cnf, sizeof(FT_MODEM_CNF),NULL, 0);
		return;
	}
	

	if(req->type == FT_MODEM_OP_SET_MODEMTYPE)
	{
		type_define = 1;
		unsigned int modem_type = req->cmd.set_modem_type_req.modem_type;
		 
	
		if (0 == ioctl(fd, CCCI_IOC_RELOAD_MD_TYPE, &modem_type))
		{
			if (0 == ioctl(fd, CCCI_IOC_MD_RESET))
			{
				ft_cnf.status = META_SUCCESS;		
			}
			else
			{
				ft_cnf.status = META_FAILED;
				META_LOG("[Meta][FT] ioctl CCCI_IOC_MD_RESET fail " );	
			}
		}
		else
		{
			ft_cnf.status = META_FAILED;
			META_LOG("[Meta][FT] ioctl CCCI_IOC_RELOAD_MD_TYPE fail modem_type = %d", modem_type);	
		}
	}
	else if(req->type == FT_MODEM_OP_GET_CURENTMODEMTYPE)
	{
		unsigned int modem_type=0;
		type_define = 1;
		
		if (0 == ioctl(fd, CCCI_IOC_GET_MD_TYPE, &modem_type))
		{
			ft_cnf.status = META_SUCCESS;
			ft_cnf.result.get_currentmodem_type_cnf.current_modem_type = modem_type;	
			META_LOG("[Meta][FT] ioctl CCCI_IOC_GET_MD_TYPE success modem_type = %d", modem_type);
		}
		else
		{
			ft_cnf.status = META_FAILED;
			META_LOG("[Meta][FT] ioctl CCCI_IOC_GET_MD_TYPE fail");	
		}	
		
	}
	else if(req->type == FT_MODEM_OP_QUERY_MDIMGTYPE)
	{
		unsigned int mdimg_type[16]={0};
		type_define = 1;

		if (0 == ioctl(fd, CCCI_IOC_GET_MD_IMG_EXIST, &mdimg_type))
		{
			ft_cnf.status = META_SUCCESS;
			memcpy(ft_cnf.result.query_modem_imgtype_cnf.mdimg_type,mdimg_type,16*sizeof(unsigned int));

			for(int i = 0;i<16;i++)
			{
				META_LOG("[Meta][FT] mdimg_type[%d] %d",i, mdimg_type[i]);	
			}
				
		}
		else
		{
			ft_cnf.status = META_FAILED;
			META_LOG("[Meta][FT] ioctl CCCI_IOC_GET_MD_IMG_EXIST fail");	
		}	
	}

	if(bDataDevice == FALSE)
	{
	     if(fd != -1)
		 {
		     close(fd);
	     }
	}

	if(type_define == 0)
	{
		META_LOG("[Meta][FT] FT_MODEM_REQ have no this type %d",req ->type );	
	}
	
	ft_cnf.header.id = req ->header.id +1;
    ft_cnf.header.token = req ->header.token;
	ft_cnf.type = req ->type;	
	
	WriteDataToPC(&ft_cnf, sizeof(FT_MODEM_CNF),NULL, 0);	
}

///////////////////////////////////////////////////////////////////////////////////////////////

FtModSIMNum::FtModSIMNum(void)
	:CmdTarget(FT_SIM_NUM_REQ_ID)
{
}

FtModSIMNum::~FtModSIMNum(void)
{
}

void FtModSIMNum::exec(Frame *pFrm)
{
	CmdTarget::exec(pFrm);

    META_LOG("[Meta][FT] Ft_SIMNum");	

	FT_GET_SIM_CNF ft_cnf;

	FT_GET_SIM_REQ *req = (FT_GET_SIM_REQ *)pFrm->localBuf();

	ft_cnf.header.id = req->header.id +1;
    ft_cnf.header.token = req->header.token;
    ft_cnf.type = req->type;
    ft_cnf.status = META_SUCCESS;

    ft_cnf.number = 0;
    META_LOG("[Meta][FT] The sim card number is zero");
#ifdef GEMINI
	ft_cnf.number = 2;
    META_LOG("[Meta][FT] The sim card number is two");  
	
#ifdef MTK_GEMINI_3SIM_SUPPORT	
	ft_cnf.number = 3;
	META_LOG("[Meta][FT] The sim card number is three");
#endif

#ifdef MTK_GEMINI_4SIM_SUPPORT
	ft_cnf.number = 4;
	META_LOG("[Meta][FT] The sim card number is four");
#endif		   	   
#else	  
    ft_cnf.number = 1;
	META_LOG("[Meta][FT] The sim card number is one");	    
#endif
   	
   	WriteDataToPC(&ft_cnf, sizeof(ft_cnf),NULL, 0);

}

///////////////////////////////////////////////////////////////////////////////////////////////
#ifdef FT_SDCARD_FEATURE

FtModSDcard::FtModSDcard(void)
	:CmdTarget(FT_SDCARD_REQ_ID)
{
}

FtModSDcard::~FtModSDcard(void)
{
}

void FtModSDcard::exec(Frame *pFrm)
{

	CmdTarget::exec(pFrm);
	
	if(getInitState())   
	    Meta_SDcard_OP((SDCARD_REQ *)pFrm->localBuf(), (char *)pFrm->peerBuf(), pFrm->peerLen());

}

int FtModSDcard::init(Frame* pFrm)
{
 	META_LOG("[Meta][FT] Ft_SDcard");

    SDCARD_CNF ft_cnf;	
	memset(&ft_cnf, 0, sizeof(SDCARD_CNF));
    static int bInitFlag_SDcard = FALSE;
	
	SDCARD_REQ *req = (SDCARD_REQ *)pFrm->localBuf();

	META_LOG("[Meta][FT] FT_SDcard_OP META Test req: %d , %d ",
        sizeof(SDCARD_REQ), sizeof(SDCARD_CNF));
		
    if (FALSE == bInitFlag_SDcard)
    {
        // initial the DVB module when it is called first time
        if (!Meta_SDcard_Init(req))
        {
            ft_cnf.header.id = req->header.id +1;
            ft_cnf.header.token = req->header.token;
            ft_cnf.status = META_FAILED;

            META_LOG("[Meta][FT] FT_SDcard_OP Meta_SDcard_Init Fail ");
    		WriteDataToPC(&ft_cnf, sizeof(ft_cnf),NULL, 0);
			return false;
        }
        bInitFlag_SDcard = TRUE;
    }
	return true;
}
#endif


///////////////////////////////////////////////////////////////////////////////////////////////
#ifdef FT_EMMC_FEATURE

FtModEMMC::FtModEMMC(void)
	:CmdTarget(FT_EMMC_REQ_ID)
{
}

FtModEMMC::~FtModEMMC(void)
{
}

void FtModEMMC::exec(Frame *pFrm)
{
	CmdTarget::exec(pFrm);

    META_LOG("[Meta][FT] Ft_EMMC");

    META_CLR_EMMC_OP((FT_EMMC_REQ *)pFrm->localBuf());

}
#endif

///////////////////////////////////////////////////////////////////////////////////////////////
#ifdef FT_CRYPTFS_FEATURE

FtModCRYPTFS::FtModCRYPTFS(void)
	:CmdTarget(FT_CRYPTFS_REQ_ID)
{
}

FtModCRYPTFS::~FtModCRYPTFS(void)
{
}

void FtModCRYPTFS::exec(Frame *pFrm)
{
	CmdTarget::exec(pFrm);

    META_LOG("[Meta][FT] Ft_CRYPTFS");
	
    META_CRYPTFS_OP((FT_CRYPTFS_REQ *)pFrm->localBuf());    

}

#endif

///////////////////////////////////////////////////////////////////////////////////////////////
#ifdef FT_ADC_FEATURE

FtModADC::FtModADC(void)
	:CmdTarget(FT_ADC_REQ_ID)
{
}

FtModADC::~FtModADC(void)
{
}

void FtModADC::exec(Frame *pFrm)
{
	CmdTarget::exec(pFrm);

    META_LOG("[Meta][FT] Ft_ADC");

	Meta_ADC_OP((ADC_REQ *)pFrm->localBuf(), (char *)pFrm->peerBuf(),  pFrm->peerLen());  
}
#endif


///////////////////////////////////////////////////////////////////////////////////////////////

FtModCustomer::FtModCustomer(void)
	:CmdTarget(FT_CUSTOMER_REQ_ID)
{
}

FtModCustomer::~FtModCustomer(void)
{
}

void FtModCustomer::exec(Frame *pFrm)
{
	CmdTarget::exec(pFrm);

    META_LOG("[Meta][FT] Ft_Customer");

    FT_CUSTOMER_CNF ft_cnf;
    memset(&ft_cnf, 0, sizeof(FT_CUSTOMER_CNF));
	FT_CUSTOMER_REQ *req = (FT_CUSTOMER_REQ *)pFrm->localBuf();
	
	int peer_buff_size = 0;
    char* peer_buf = NULL;
	
	// Implement custom API logic here. The following is a sample code for testing.
    ft_cnf.header.id = req->header.id +1;
    ft_cnf.header.token = req->header.token;
	ft_cnf.type = req->type;
    ft_cnf.status = META_SUCCESS;
	
	peer_buf = (char*)malloc(pFrm->peerLen());
	if (peer_buf == NULL)
	{
	    META_LOG("[Meta][FT] Ft_Customer malloc fail");
		return;
	}
	memcpy(peer_buf, pFrm->peerBuf(), pFrm->peerLen());
	peer_buff_size = pFrm->peerLen();
	
	META_LOG("[Meta][FT] FT_CUSTOMER_OP successful, OP type is %d!", req->type);
	
    WriteDataToPC(&ft_cnf, sizeof(ft_cnf), peer_buf, peer_buff_size);
	free(peer_buf);
}

///////////////////////////////////////////////////////////////////////////////////////////////

FtModSpecialTest::FtModSpecialTest(void)
	:CmdTarget(FT_SPECIALTEST_REQ_ID)
{
}

FtModSpecialTest::~FtModSpecialTest(void)
{
}

void FtModSpecialTest::exec(Frame *pFrm)
{
	CmdTarget::exec(pFrm);

    META_LOG("[Meta][FT] Ft_SpecialTest");
	
	FT_SPECIALTEST_REQ *req = (FT_SPECIALTEST_REQ *)pFrm->localBuf();
	
    FT_SPECIALTEST_CNF SpecialTestCnf;
    memset(&SpecialTestCnf, 0, sizeof(FT_SPECIALTEST_CNF));
    SpecialTestCnf.header.id = req->header.id +1;
    SpecialTestCnf.header.token = req->header.token;
    SpecialTestCnf.type = req->type;
    SpecialTestCnf.status= META_FAILED;

    switch (req->type)
    {
          case FT_SPECIALTEST_OP_HUGEDATA:		//query the supported modules
                  META_LOG("[Meta][FT] pFTReq->type is FT_SPECIALTEST_OP_HUGEDATA ");                 
                  SpecialTestCnf.status= META_SUCCESS;
                  break;
          default :
                  break;
    }

    WriteDataToPC(&SpecialTestCnf, sizeof(FT_SPECIALTEST_CNF),(char *)pFrm->peerBuf(),  pFrm->peerLen());   
}


///////////////////////////////////////////////////////////////////////////////////////////////

#define CHIP_RID_PATH	"/proc/rid"
#define CHIP_RID_LEN 	16 


FtModChipID::FtModChipID(void)
	:CmdTarget(FT_GET_CHIPID_REQ_ID)
{
}

FtModChipID::~FtModChipID(void)
{
}

void FtModChipID::exec(Frame *pFrm)
{
	CmdTarget::exec(pFrm);

    META_LOG("[Meta][FT] Ft_ChipID");

	FT_GET_CHIPID_CNF ft_cnf;
	int bytes_read = 0;
	int res = 0;
		
	memset(&ft_cnf, 0, sizeof(FT_GET_CHIPID_CNF));

	
	FT_GET_CHIPID_REQ *req = (FT_GET_CHIPID_REQ *)pFrm->localBuf();
		
	ft_cnf.header.id = req->header.id +1;
	ft_cnf.header.token = req->header.token;
	ft_cnf.status = META_FAILED;
		
	int fd = open(CHIP_RID_PATH, O_RDONLY);
	if (fd != -1)
	{
		while (bytes_read < CHIP_RID_LEN)
		{
			res = read(fd, ft_cnf.chipId + bytes_read, CHIP_RID_LEN);
			if (res > 0)
				bytes_read += res;
			else
				break;
		}
		close(fd);
		ft_cnf.status = META_SUCCESS;
		META_LOG("[Meta][FT] Chip rid=%s", ft_cnf.chipId);
	}
	else
	{
		if (errno == ENOENT)
		{
			ft_cnf.status = META_FAILED;
		}
		META_LOG("[Meta][FT] Failed to open chip rid file %s, errno=%d", CHIP_RID_PATH, errno);
	}
		
	WriteDataToPC(&ft_cnf, sizeof(ft_cnf), NULL, 0);

}

///////////////////////////////////////////////////////////////////////////////////////////////
#ifdef FT_TOUCH_FEATURE

FtModCTP::FtModCTP(void)
	:CmdTarget(FT_CTP_REQ_ID)
{
	memset(&m_ft_cnf, 0, sizeof(Touch_CNF));
}

FtModCTP::~FtModCTP(void)
{
}

void FtModCTP::exec(Frame *pFrm)
{
	CmdTarget::exec(pFrm);
	
	memset(&m_ft_cnf, 0, sizeof(Touch_CNF));
	Touch_REQ *req = (Touch_REQ *)pFrm->localBuf();
	
	m_ft_cnf.header.id = req->header.id + 1;
	m_ft_cnf.header.token = req->header.token;
	m_ft_cnf.status = META_SUCCESS;
	m_ft_cnf.tpd_type = req->tpd_type;
	
	if(getInitState())
		Meta_Touch_OP(req,(char *)pFrm->peerBuf(),  pFrm->peerLen());	

}

int FtModCTP::init(Frame*)
{
	META_LOG("[Meta][FT] Ft_CTP");
	
	static int bInitFlag_CTP = false;
	if (false == bInitFlag_CTP)
	{
		// initial the touch panel module when it is called first time
		if (!Meta_Touch_Init())
		{
			META_LOG("[Meta][FT] FT_CTP_OP Meta_Touch_Init failed!");
			m_ft_cnf.status = META_FAILED;
			WriteDataToPC(&m_ft_cnf, sizeof(m_ft_cnf),NULL, 0);
			return false;
		}
		bInitFlag_CTP = TRUE;
	}

	return true;
}
#endif

///////////////////////////////////////////////////////////////////////////////////////////////

#ifdef FT_GPIO_FEATURE

FtModGPIO::FtModGPIO(void)
	:CmdTarget(FT_GPIO_REQ_ID)
{
	
}

FtModGPIO::~FtModGPIO(void)
{
}

void FtModGPIO::exec(Frame *pFrm)
{
	GPIO_CNF ft_cnf;
	CmdTarget::exec(pFrm);

    META_LOG("[Meta][FT] Ft_GPIO");
	memset(&ft_cnf, 0, sizeof(GPIO_CNF));

	GPIO_REQ *req = (GPIO_REQ *)pFrm->localBuf();

	if(getInitState())
	{	//do the bat test by called the interface in meta bat lib
    	ft_cnf = Meta_GPIO_OP(*req,(unsigned char *)pFrm->peerBuf(),  pFrm->peerLen());
    	WriteDataToPC(&ft_cnf, sizeof(ft_cnf),NULL, 0);
	}
}

int FtModGPIO::init(Frame*pFrm)
{
	GPIO_CNF ft_cnf;
    static int bInitFlag_GPIO = false;

    META_LOG("[Meta][FT] FT_GPIO_OP META Test ");
    memset(&ft_cnf, 0, sizeof(GPIO_CNF));
	GPIO_REQ *req = (GPIO_REQ *)pFrm->localBuf();

    if (FALSE == bInitFlag_GPIO)
    {
        // initial the bat module when it is called first time
        if (!Meta_GPIO_Init())
        {
            ft_cnf.header.id = req->header.id +1;
            ft_cnf.header.token = req->header.token;
            ft_cnf.status = META_FAILED;

            META_LOG("[Meta][FT] FT_GPIO_OP Meta_GPIO_Init Fail ");
            WriteDataToPC(&ft_cnf, sizeof(ft_cnf),NULL, 0);

			return false;
        }
        bInitFlag_GPIO = true;
    }

	return true;
}

#endif

///////////////////////////////////////////////////////////////////////////////////////////////
//for SIM detect
FtModSIMDetect::FtModSIMDetect(void)
	:CmdTarget(FT_SIM_DETECT_REQ_ID)
{
	
}

FtModSIMDetect::~FtModSIMDetect(void)
{
}

void FtModSIMDetect::exec(Frame *pFrm)
{
	META_LOG("[Meta][SIM DETECT] FtModSIMDetect");	

	FT_SIM_DETECT_CNF  ft_cnf;

	memset(&ft_cnf, 0, sizeof(FT_SIM_DETECT_CNF));
	ft_cnf.status = META_FAILED;

	
	FT_SIM_DETECT_REQ *req = (FT_SIM_DETECT_REQ *)pFrm->localBuf();

	ft_cnf.header.id = req->header.id +1;
    ft_cnf.header.token = req->header.token;
    ft_cnf.type= req->type;

	META_LOG("[Meta][SIM DETECT] FtModSIMDetect req->type = %d", req->type);
	switch(req->type)
	{
		case FT_SIM_DETECT_OP_EXTMOD:
			{
				ft_cnf.result.sim_query_mdtype_cnf.md_type = CheckExtMd();
				ft_cnf.status = META_SUCCESS;
			}
			break;
		case FT_SIM_DETECT_OP_PRJTYPE:
			{
				unsigned int type;
				if(GetPrjType(type))
				{
					ft_cnf.result.sim_query_prjtype_cnf.prj_type = type;
					ft_cnf.status = META_SUCCESS;
				}
				else
				{
					META_LOG("[Meta][SIM DETECT] Failed to get project type");
				}
			}
			break;
		case FT_SIM_DETECT_OP_MDIDXSET:
			{
				ft_cnf.result.sim_query_mdidxset_cnf.md_idxset = getMdmType();
				ft_cnf.status = META_SUCCESS;
			}
			break;
		case FT_SIM_DETECT_OP_SWITCH:
			{
				if(SetSIMSwitcher(req->cmd.sim_set_switcher_req.mode_cmd))
				{
					META_LOG("[Meta][SIM DETECT] SIM Switch cmd  = 0x%08x", req->cmd.sim_set_switcher_req.mode_cmd );
					ft_cnf.status = META_SUCCESS;
				}
				else
				{
					META_LOG("[Meta][SIM DETECT] Failed to switch SIM");
				}
			}
			break;
		case FT_SIM_DETECT_OP_GETSSW:
			{
				int ssw_val = GetSSWValue();
				if(ssw_val > 0)
				{
					ft_cnf.result.sim_query_ssw_cnf.ssw_val = ssw_val;
					ft_cnf.status = META_SUCCESS;
				}					
			}
			break;
	}

	WriteDataToPC(&ft_cnf, sizeof(FT_SIM_DETECT_CNF),NULL, 0);
}

int FtModSIMDetect::GetDevNode()
{
	char dev_node[32] = {0};
	int fd = -1;
	
	snprintf(dev_node, 32, "%s", ccci_get_node_name(USR_META_IOCTL,MD_SYS5));
	fd = open(dev_node, O_RDWR|O_NOCTTY|O_NDELAY );	
	
	if(fd < 0)
	{
	    META_LOG("[Meta][SIM DETECT] Failed to open CCCI port: MD_SYS5, %s", dev_node );
	}
    else
	{
	    META_LOG("[Meta][SIM DETECT] Open CCCI success: path = %s, handle = %d", dev_node, fd);
    }
	
	return fd;
		
}

unsigned int FtModSIMDetect::SetSIMSwitcher(unsigned int &mode)
{
	int fd = -1;

	META_LOG("[Meta][SIM DETECT] SetSIMSwitcher mode = 0x%08x", mode);
	
	if((fd = GetDevNode()) < 0)
		return 0;

	if (0 == ioctl(fd, CCCI_IOC_SIM_SWITCH, &mode))
	{
		close(fd);
		META_LOG("[Meta][SIM DETECT] Close ccci  node: handle = %d", fd);
		return 1;
	}

	META_LOG("[Meta][SIM DETECT] IOCTL CCCI_IOC_SIM_SWITCH failed");

    close(fd);
	return 0;	
}

unsigned int  FtModSIMDetect::GetPrjType(unsigned int &type)
{
	int fd = -1;
	
	if((fd = GetDevNode()) < 0)
		return 0;

	if (0 == ioctl(fd, CCCI_IOC_SIM_SWITCH_TYPE, &type))
	{
		close(fd);
		return 1;
	}

	META_LOG("[Meta][SIM DETECT] IOCTL CCCI_IOC_SIM_SWITCH_TYPE failed");

    close(fd);
	return 0;	
}

unsigned int  FtModSIMDetect::CheckExtMd()
{
	unsigned int ret = 0;

//MTK_DT_SUPPORT=yes  && MTK_EXTERNAL_MODEM_SLOT > 0 && MTK_C2K_SUPPORT=no

#if (!defined(MTK_C2K_SUPPORT) && defined(MTK_DT_SUPPORT) && defined(MTK_EXTERNAL_MODEM) )

	ret = 1;

#endif
	META_LOG("[Meta][SIM DETECT] CheckExtMd ret = %d", ret);
	return ret;
}

unsigned int FtModSIMDetect::GetSSWValue()
{
	char buf[SIM_SWITCH_MODE_STR_LEN];
	int res = 0;
	unsigned int val = 0;
	int fd = open(SIM_SWITCH_MODE_FILE, O_RDONLY);

	META_LOG("[Meta][SIM DETECT] To open ssw file, fd = %d", fd);
	if (fd != -1)
	{
		memset(buf, 0, SIM_SWITCH_MODE_STR_LEN);
		res = read(fd, buf, SIM_SWITCH_MODE_STR_LEN);
		META_LOG("[Meta][SIM DETECT] To read ssw value, buf = %s", buf);
		if(res >= 3) //0x1, 0x2
			sscanf(buf, "0x%d", &val);
		close(fd);
	}

	META_LOG("[Meta][SIM DETECT] reture ssw value = %d", val);
	return val;	
}

///////////////////////////////////////////////////////////////////////////////////////////////
FtModFileOperation::FtModFileOperation(void)
	:CmdTarget(FT_FILE_OPERATION_REQ_ID)
{
	m_nFileCount = 0;
}

FtModFileOperation::~FtModFileOperation(void)
{    
	ClearFileInfoList();
}

unsigned int FtModFileOperation::GetFileLen(char *pFilePath)
{
    struct stat st;
	if(stat((const char*)pFilePath, &st) < 0)
	{
	    META_LOG("[Meta][FT] GetFileLen pFilePath(%s) fail, errno=%d",pFilePath,errno);
	    return 0;
	}
	else
	{
	    return (unsigned int)st.st_size;
	}
}

void FtModFileOperation::ClearFileInfoList(void)
{
    mlist<FT_FILE_INFO*>::iterator it1 = m_fileInfoList.begin();	
	while (it1 != m_fileInfoList.end())
	{
		delete (*it1);
		++ it1;
	}
}


int FtModFileOperation::ListPath(unsigned char *pPath,unsigned char *pFileNameSubStr) 
{
	if(pPath == NULL)
	{
	    META_LOG("[Meta][FT] ListPath path is NULL");
		return 1;
	}

	if(pFileNameSubStr == NULL || pFileNameSubStr[0]  == '\0')
	{
	    META_LOG("[Meta][FT] ListPath file name substr is NULL or 0");
		return 1;
	}

	struct stat s;
	if(stat((const char*)pPath, &s) < 0)
	{
	    META_LOG("[Meta][FT] ListPath call stat fail");
		return 1;
	}

	if(!S_ISDIR(s.st_mode))
	{
	    META_LOG("[Meta][FT] ListPath path(%s) is not a folder name",pPath);
		return 1;
	}

    char currfile[1024] = {0};
    DIR *dir = NULL;
    struct dirent *ptr = NULL;
    dir = opendir((const char*)pPath);
	if(dir == NULL)
    {
        META_LOG("[Meta][FT] ListPath opendir(%s) fail, errno=%d",pPath,errno);
        return 1;
    }
    
    m_nFileCount = 0;
	ClearFileInfoList();
    while((ptr = readdir(dir)) != NULL)
    {
        if(strcmp(ptr->d_name,".") == 0
          || strcmp(ptr->d_name,"..") ==0)
            continue;
		if(strlen(ptr->d_name) > 256)
		{
		    META_LOG("[Meta][FT] ListPath file name(%s) length is too large,just skip this file!!!!",ptr->d_name);
		    continue;
		}

        if(strcmp((const char*)pFileNameSubStr,"*") == 0
			|| strstr(ptr->d_name,(const char*)pFileNameSubStr) != NULL)
        {
		     FT_FILE_INFO *pFileInfo = new FT_FILE_INFO;
	         if(pFileInfo==NULL)
	         {		    
		         META_LOG("[Meta][FT] ListPath new FT_FILE_INFO fail,errno=%d",errno);
			     closedir(dir);
		         return 1;
	         }
		     memset(pFileInfo,0,sizeof(FT_FILE_INFO));		
             memcpy(pFileInfo->file_name, ptr->d_name, strlen(ptr->d_name));
			 if(pPath[strlen((const char*)pPath)-1] != '/')
			 {
			     sprintf(currfile,"%s/%s",(char*)pPath,ptr->d_name);
			 }
			 else
		     {
		         sprintf(currfile,"%s%s",(char*)pPath,ptr->d_name);
			 }
		
             if(ptr->d_type == DT_REG)  //file
             {
                 pFileInfo->file_type = FT_FILE_TYPE_FILE;
			     pFileInfo->file_size = GetFileLen(currfile);
             }
             else if(ptr->d_type == DT_DIR) //directory
             {
                 pFileInfo->file_type = FT_FILE_TYPE_FOLDER;
			     pFileInfo->file_size = 0;			
             }
			 else
			 {
			     pFileInfo->file_type = FT_FILE_TYPE_INVALID;
			     pFileInfo->file_size = 0;
			 }
		     m_nFileCount++;
		     META_LOG("[Meta][FT] ListPath find one file or folder,file_name=%s,file_type:%d,file_size:%d,m_nFileCount:%d",pFileInfo->file_name,pFileInfo->file_type,pFileInfo->file_size,m_nFileCount);
		     m_fileInfoList.push_back(pFileInfo);
        }

    }

	closedir(dir);
	return 0;
}

FT_FILE_INFO* FtModFileOperation::GetFileInfo(unsigned int id)
{
    mlist<FT_FILE_INFO*>::iterator it = m_fileInfoList.begin();
	int i = 0;

	while (it != m_fileInfoList.end())
	{
		
		if (i == id)
		{
			return (*it);
		}
		i++;
		++ it;
	}
	return NULL;
}



int FtModFileOperation::SaveSendData(FILE_OPERATION_SENDFILE_REQ *req, char *peer_buff, unsigned short peer_len)
{
    int nRet = 1;
    if(req == NULL || peer_buff == NULL)
    {
        return nRet;
    }
    if(req->dest_file_name[0] == '\0')
    {
      	return nRet;
    }
	  
    int SendFileFd = -1;
    unsigned int fileLen;
	META_LOG("[Meta][FT] SaveSendData receive block stage %x, file size %d!",req->stream_block.stage,req->stream_block.file_size);
	if(req->stream_block.stage & BLK_CREATE)
	{
		SendFileFd = open((const char*)req->dest_file_name, O_RDWR | O_TRUNC | O_CREAT, 0777);
	}
	else
	{
		SendFileFd = open((const char*)req->dest_file_name, O_RDWR | O_APPEND);
	}
	
	if(SendFileFd >= 0)
	{
		META_LOG("[Meta][FT] SaveSendData create or open file OK!");
		unsigned short sWriten = 0;
		sWriten = write(SendFileFd, peer_buff, peer_len);

		if(sWriten)
		{
			META_LOG("[Meta][FT] SaveSendData  write %d data total data %d!",sWriten,peer_len);
			if(req->stream_block.stage & BLK_EOF)
			{
				fileLen = getFileSize(SendFileFd);
				if(req->stream_block.file_size == fileLen)
				{
					META_LOG("[Meta][FT] SaveSendData write file BLK_EOF success! ");
					close(SendFileFd);
					SendFileFd = -1;
					nRet = 0;
				}
				else
				{
					META_LOG("[Meta][FT] SaveSendData file size(%d) error! ",req->stream_block.file_size);
				}
			}
			else
			{
			    META_LOG("[Meta][FT] SaveSendData write file BLK_WRITE success! ");
			    nRet = 0;
			}
		}
		else
		{
			META_LOG("[Meta][FT] SaveSendData write file failed!");
		}
	
		if(SendFileFd != -1)
			close(SendFileFd);
	
	}
	else
	{
		META_LOG("[Meta][FT] SaveSendData create or open file failed!");
	}
	return nRet;
	
}

#define RECEIVE_PEER_MAX_LEN 20*1024

int FtModFileOperation::SetReceiveData(FILE_OPERATION_RECEIVEFILE_REQ *req, FT_FILE_OPERATION_CNF* pft_cnf)
{
    int nRet = 1;
	if(req == NULL || pft_cnf == NULL)
	{
		return nRet;
	}
	if(req->source_file_name[0]  == '\0')
	{
		return nRet;
	}

    int nReceiveDataSize = 0;
    int ReceiveFileFd = -1;
	int nPeerBuffSize = 0;
    char* pPeerBuf = NULL;

	ReceiveFileFd = open((const char*)req->source_file_name, O_RDONLY);

	if(ReceiveFileFd >= 0)
	{
		unsigned int nFileLen = getFileSize(ReceiveFileFd);
        META_LOG("[Meta][FT] SetReceiveData open file %s succeed, fileSize %d ! ",req->source_file_name,nFileLen);

		pPeerBuf = (char*)malloc(RECEIVE_PEER_MAX_LEN);
		memset(pPeerBuf, 0, RECEIVE_PEER_MAX_LEN);

		pft_cnf->result.receivefile_cnf.stream_block.stage = BLK_CREATE;

		while(!(pft_cnf->result.receivefile_cnf.stream_block.stage & BLK_EOF))
		{
			nPeerBuffSize = read(ReceiveFileFd, pPeerBuf, RECEIVE_PEER_MAX_LEN);

            META_LOG("[Meta][FT] SetReceiveData nPeerBuffSize:%d,nReceiveDataSize:%d,",nPeerBuffSize,nReceiveDataSize);
			if(nPeerBuffSize != -1)
			{
				pft_cnf->status = META_SUCCESS;
				pft_cnf->result.receivefile_cnf.receive_result = 0;
				if(nReceiveDataSize == nFileLen)
				{
					pft_cnf->result.receivefile_cnf.stream_block.stage |= BLK_EOF;
					pft_cnf->result.receivefile_cnf.stream_block.file_size = nFileLen;
					WriteDataToPC(pft_cnf, sizeof(FT_FILE_OPERATION_CNF),NULL, 0);
					META_LOG("[Meta][FT] SetReceiveData file end, set BLK_EOF! ");
					nRet = 0;
					break;
				}
				else
				{
					pft_cnf->result.receivefile_cnf.stream_block.stage |= BLK_WRITE;
					pft_cnf->result.receivefile_cnf.stream_block.file_size = nFileLen;
					META_LOG("[Meta][FT] SetReceiveData File set %d data ! ",nPeerBuffSize);
					WriteDataToPC(pft_cnf, sizeof(FT_FILE_OPERATION_CNF),pPeerBuf, nPeerBuffSize);
					memset(pPeerBuf,0,RECEIVE_PEER_MAX_LEN);
					pft_cnf->result.receivefile_cnf.stream_block.stage &= ~BLK_CREATE;
					META_LOG("[Meta][FT] SetReceiveData set BLK_WRITE! ");
				}
				nReceiveDataSize = nReceiveDataSize + nPeerBuffSize;

			}
			else
			{
				pft_cnf->result.receivefile_cnf.stream_block.stage |= BLK_EOF;
				META_LOG("[Meta][FT] SetReceiveData read file fail, set BLK_EOF! ");
			}

		}

		free(pPeerBuf);
		close(ReceiveFileFd);

	}
	else
	{
	    META_LOG("[Meta][FT] SetReceiveData open File %s failed, errno=%d",req->source_file_name,errno);
	}

	return nRet;

}
void FtModFileOperation::exec(Frame *pFrm)
{
	META_LOG("[Meta][FILE OPERATION] FtModFileOperation");	

	FT_FILE_OPERATION_CNF  ft_cnf;

	memset(&ft_cnf, 0, sizeof(FT_FILE_OPERATION_CNF));
	ft_cnf.status = META_FAILED;

	
	FT_FILE_OPERATION_REQ *req = (FT_FILE_OPERATION_REQ *)pFrm->localBuf();

	ft_cnf.header.id = req->header.id +1;
    ft_cnf.header.token = req->header.token;
    ft_cnf.type= req->type;

	META_LOG("[Meta][FILE OPERATION] FtModFileOperation req->type = %d", req->type);
	switch(req->type)
	{
		case FT_FILE_OP_PARSE:
			{
				if(ListPath(req->cmd.parse_req.path_name,req->cmd.parse_req.filename_substr) == 0)
				{
				    ft_cnf.result.parse_cnf.file_count = m_nFileCount;
				    ft_cnf.status = META_SUCCESS;
				    META_LOG("[Meta][FILE OPERATION] FtModFileOperation parse folder success!");
				}
				else
				{
				    META_LOG("[Meta][FILE OPERATION] FtModFileOperation parse folder fail!");
				}
			}
			break;
		case FT_FILE_OP_GETFILEINFO:
			{
				if(req->cmd.getfileinfo_req.index < 0 || req->cmd.getfileinfo_req.index > m_nFileCount)
				{
				    META_LOG("[Meta][FILE OPERATION] FtModFileOperation invalid file index!");
				}
				else
				{
				    FT_FILE_INFO* pFileInfo = GetFileInfo(req->cmd.getfileinfo_req.index);
				    if(pFileInfo != NULL)
				    {				  
				        memcpy(&(ft_cnf.result.getfileinfo_cnf.file_info),pFileInfo,sizeof(FT_FILE_INFO));
						META_LOG("[Meta][FILE OPERATION] file_name=%s,file_type:%d,file_size:%d",pFileInfo->file_name,pFileInfo->file_type,pFileInfo->file_size);
				        ft_cnf.status = META_SUCCESS;
				    }
				}
			}
			break;
		case FT_FILE_OP_SENDFILE:
			{
				if(SaveSendData(&(req->cmd.sendfile_req),(char *)pFrm->peerBuf(), pFrm->peerLen()) == 0)
				{
				    ft_cnf.result.sendfile_cnf.send_result = 0;
				    ft_cnf.status = META_SUCCESS;					   
				}
			}
			break;
		case FT_FILE_OP_RECEIVEFILE:
			{
				if(SetReceiveData(&(req->cmd.receivefile_req),&ft_cnf) == 0)
		        {
			        META_LOG("[Meta][FILE OPERATION] FtModFileOperation success to set receive data!");
					return;
		        }
	            else
	            {
		            META_LOG("[Meta][FILE OPERATION] FtModFileOperation Failed to set receive data!");
	            }
			}
			break;
		default:
			break;
	}

	WriteDataToPC(&ft_cnf, sizeof(FT_FILE_OPERATION_CNF),NULL, 0);
}


///////////////////////////////////////////////////////////////////////////////////////////////

BOOL Meta_Mobile_Log()
{        
    int fd = 0;
	int len = 0;
	BOOL ret = FALSE;
	char tempstr[5]={0};
	META_LOG("[Meta][FT] Meta_Mobile_Log ");

    //support end load and user load,send stop command to mobilelog 

	fd = socket_local_client("mobilelogd", ANDROID_SOCKET_NAMESPACE_ABSTRACT, SOCK_STREAM);
	if (fd < 0) 
	{
		META_LOG("[Meta][FT] socket fd <0 ");
		return FALSE;
	}
	META_LOG("[Meta][FT] socket ok\n");
	if((len = write(fd, "stop", sizeof("stop"))) < 0)
	{
		META_LOG("[Meta][FT] socket write error!");
		ret = FALSE;
	}
	else
	{
		META_LOG("[Meta][FT] write %d Bytes.", len);
		ret = TRUE;
	}
	close(fd);
	sleep(4);	      
	return ret;
	
}

void FT_UtilCheckIfFuncExist(FT_UTILITY_COMMAND_REQ  *req, FT_UTILITY_COMMAND_CNF  *cnf)
{

    unsigned int	query_ft_msg_id = req->cmd.CheckIfFuncExist.query_ft_msg_id;
    unsigned int	query_op_code = req->cmd.CheckIfFuncExist.query_op_code;
    META_LOG("[Meta][FT] FT_UtilCheckIfFuncExist META Test ");
    cnf->status = FT_CNF_FAIL;

	META_LOG("[Meta][FT] request id = %d op = %d",query_ft_msg_id,query_op_code);

	
    switch (query_ft_msg_id)
    {

#ifdef FT_FM_FEATURE 
    	case FT_FM_REQ_ID:
			if(query_op_code == 0)//FT_FM_OP_READ_CHIP_ID
			{
				cnf->status = FT_CNF_OK; 
			}        	
        	break;
#endif

#ifdef FT_EMMC_FEATURE
		case FT_CRYPTFS_REQ_ID:
			if(query_op_code == 0)//FT_CRYPTFS_OP_QUERYSUPPORT
			{
				cnf->status = FT_CNF_OK;
			}
			else if(query_op_code == 1)//FT_CRYPTFS_OP_VERITIFY
			{
				cnf->status = FT_CNF_OK;
			}
        	break;
#endif
			
		case FT_MODEM_REQ_ID:
			if(query_op_code == FT_MODEM_OP_QUERY_INFO )
			{
				cnf->status = FT_CNF_OK;
			}
			else if(query_op_code == FT_MODEM_OP_CAPABILITY_LIST)
			{
				cnf->status = FT_CNF_OK;
			}
#ifdef MTK_EXTMD_NATIVE_DOWNLOAD_SUPPORT
			else if(query_op_code == FT_MODEM_OP_QUERY_MDDOWNLOADSTATUS)
			{
				cnf->status = FT_CNF_OK;
			}
#endif
			else if(query_op_code == FT_MODEM_OP_SET_MODEMTYPE)
			{
				cnf->status = FT_CNF_OK;
			}
			else if(query_op_code == FT_MODEM_OP_GET_CURENTMODEMTYPE)
			{
				cnf->status = FT_CNF_OK;
			}
			else if(query_op_code == FT_MODEM_OP_QUERY_MDIMGTYPE )
			{
				cnf->status = FT_CNF_OK;
			}
			break;

		case FT_L4AUD_REQ_ID:
			if(query_op_code == 59)//FT_L4AUD_OP_SPEAKER_CALIBRATION_SUPPORT
			{
#ifdef MTK_SPEAKER_MONITOR_SUPPORT
				cnf->status = FT_CNF_OK;
				META_LOG("[Meta][FT] MTK_SPEAKER_MONITOR_SUPPORT = yes");
#else
				cnf->status = FT_CNF_FAIL;
				META_LOG("[Meta][FT] MTK_SPEAKER_MONITOR_SUPPORT = no");
#endif
			}

			break;
		case FT_SIM_DETECT_REQ_ID:
			if(query_op_code == FT_SIM_DETECT_OP_EXTMOD)
			{
				cnf->status = FT_CNF_OK;
				META_LOG("[Meta][FT] FT_SIM_DETECT_OP_EXTMOD = yes");
			}
			break;
			
		case FT_UTILITY_COMMAND_REQ_ID:
			if(query_op_code == FT_UTILCMD_QUERY_WCNDRIVER_READY)
			{
			    cnf->status = FT_CNF_OK;
				META_LOG("[Meta][FT] FT_UTILCMD_QUERY_WCNDRIVER_READY = yes");
			}
			break;
		case FT_FILE_OPERATION_REQ_ID:
			if(query_op_code == FT_FILE_OP_PARSE )
			{
				cnf->status = FT_CNF_OK;
			}
			else if(query_op_code == FT_FILE_OP_GETFILEINFO)
			{
			    cnf->status = FT_CNF_OK;
			}
			else if(query_op_code == FT_FILE_OP_SENDFILE)
			{
			    cnf->status = FT_CNF_OK;
			}
			else if(query_op_code == FT_FILE_OP_RECEIVEFILE)
			{
				cnf->status = FT_CNF_OK;
			}
            break;			
    	default:
			META_LOG("[Meta][FT] NOT FOUND THE PRIMITIVE_ID");
        	cnf->status = FT_CNF_FAIL;		
        	break;
    }
    

    // assign return structure
    cnf->result.CheckIfFuncExist.query_ft_msg_id = query_ft_msg_id;
    cnf->result.CheckIfFuncExist.query_op_code = query_op_code;
}

FtModUtility::FtModUtility(void)
	:CmdTarget(FT_UTILITY_COMMAND_REQ_ID)
{
}

FtModUtility::~FtModUtility(void)
{
}

void FtModUtility::exec(Frame *pFrm)
{
	CmdTarget::exec(pFrm);  
	
	FT_UTILITY_COMMAND_CNF UtilityCnf;
    //PROCESS_INFORMATION cleanBootProcInfo;
    static META_BOOL bLCDBKInitFlag_Peri = FALSE;
    static META_BOOL bLCDFtInitFlag_Peri = FALSE;
    static META_BOOL bVibratorInitFlag_Peri = FALSE;
	int nNVRAMFlag = 0;
	unsigned int level = 0;
	int ret = 0;
	char tempstr[128]={0};

    //cleanBootProcInfo.hProcess = NULL;
    //cleanBootProcInfo.hThread = NULL;

    META_LOG("[Meta][FT] FT_Peripheral_OP META Test ");

    memset(&UtilityCnf, 0, sizeof(FT_UTILITY_COMMAND_CNF));
	FT_UTILITY_COMMAND_REQ *req = (FT_UTILITY_COMMAND_REQ *)pFrm->localBuf();

    UtilityCnf.header.id = req->header.id +1;
    UtilityCnf.header.token = req->header.token;
    UtilityCnf.type = req->type;
    UtilityCnf.status= META_FAILED;

    //do the related test.
    switch (req->type)
    {
    case FT_UTILCMD_CHECK_IF_FUNC_EXIST:		//query the supported modules
        META_LOG("[Meta][FT] FT_Peripheral_OP pFTReq->type is FT_UTILCMD_CHECK_IF_FUNC_EXIST ");
        FT_UtilCheckIfFuncExist(req, &UtilityCnf);
        break;
    //delete since no use.
    //case FT_UTILCMD_QUERY_LOCAL_TIME:			//query RTC from meta cpu lib
    //    META_LOG("[Meta][FT] FT_Peripheral_OP pFTReq->type is FT_UTILCMD_QUERY_LOCAL_TIME ");
    //    UtilityCnf.result.m_WatchDogCnf= META_RTCRead_OP(req->cmd.m_WatchDogReq);
    //    UtilityCnf.status= META_SUCCESS;
    //    break;

#ifdef FT_LCDBK_FEATURE
    case FT_UTILCMD_MAIN_SUB_LCD_LIGHT_LEVEL:	//test lcd backlight from meta lcd backlight lig
        META_LOG("[Meta][FT] FT_Peripheral_OP pFTReq->type is FT_UTILCMD_MAIN_SUB_LCD_LIGHT_LEVEL ");
	    if(bLCDBKInitFlag_Peri==FALSE)
     	{   		
			if (!Meta_LCDBK_Init())
			{
				META_LOG("[Meta][FT] FT_Peripheral_OP Meta_LCDBK_Init Fail ");
				goto Per_Exit;
			}   		
		    bLCDBKInitFlag_Peri = TRUE;
    	}        
        UtilityCnf.result.m_LCDCnf = Meta_LCDBK_OP(req->cmd.m_LCDReq);
        UtilityCnf.status= META_SUCCESS;
        break;
#endif

#ifdef FT_LCD_FEATURE
	case FT_UTILCMD_LCD_COLOR_TEST:
        META_LOG("[Meta][FT] FT_Peripheral_OP pFTReq->type is FT_UTILCMD_LCD_COLOR_TEST ");
	    if(bLCDFtInitFlag_Peri==FALSE)
     	{   					
			if (!Meta_LCDFt_Init())
			{
			    META_LOG("[Meta][FT]] FT_Peripheral_OP Meta_LCDFt_Init Fail ");
			    goto Per_Exit;
			}			
		    bLCDFtInitFlag_Peri = TRUE;
    	}        
        UtilityCnf.result.m_LCDColorTestCNF = Meta_LCDFt_OP(req->cmd.m_LCDColorTestReq);
        UtilityCnf.status= META_SUCCESS;
        break;
#endif

#ifdef FT_VIBRATOR_FEATURE
    case FT_UTILCMD_SIGNAL_INDICATOR_ONOFF:
        META_LOG("[Meta][FT] FT_Peripheral_OP pFTReq->type is FT_UTILCMD_SIGNAL_INDICATOR_ONOFF ");
        UtilityCnf.result.m_NLEDCnf = Meta_Vibrator_OP(req->cmd.m_NLEDReq);
        UtilityCnf.status= META_SUCCESS;
        break;
#endif

		
#ifdef FT_VIBRATOR_FEATURE
    case FT_UTILCMD_VIBRATOR_ONOFF:				//test vibrate and indicator from meta nled lib
        META_LOG("[Meta][FT] FT_Peripheral_OP pFTReq->type is FT_UTILCMD_VIBRATOR_ONOFF ");
	    if(bVibratorInitFlag_Peri==FALSE)
     	{
    		if (!Meta_Vibrator_Init())
        	{
            	META_LOG("[Meta][FT] FT_Peripheral_OP Meta_Vibrator_Init Fail ");
            	goto Per_Exit;
        	}	
		    bVibratorInitFlag_Peri = TRUE;
    	}         
        UtilityCnf.result.m_NLEDCnf = Meta_Vibrator_OP(req->cmd.m_NLEDReq);
        UtilityCnf.status= META_SUCCESS;
        break;
#endif

#ifdef FT_VIBRATOR_FEATURE
    case FT_UTILCMD_KEYPAD_LED_ONOFF:	
        META_LOG("[Meta][FT] FT_Peripheral_OP pFTReq->type is FT_UTILCMD_KEYPAD_LED_ONOFF ");
        UtilityCnf.result.m_NLEDCnf = Meta_Vibrator_OP(req->cmd.m_NLEDReq);
        UtilityCnf.status= META_SUCCESS;
        break;
#endif

    case FT_UTILCMD_SET_CLEAN_BOOT_FLAG:		
        nNVRAMFlag = req->cmd.m_SetCleanBootFlagReq.Notused;		
        META_LOG("[Meta][FT] FT_Peripheral_OP pFTReq->type is FT_UTILCMD_SET_CLEAN_BOOT_FLAG, nNVRAMFlag =%d",nNVRAMFlag);
        META_LOG("[Meta][FT] FT_UTILCMD_SET_CLEAN_BOOT_FLAG META Test %s,%d,%s",__FILE__,__LINE__,__FUNCTION__);	
		if ( nNVRAMFlag == 1 || nNVRAMFlag == 2 || nNVRAMFlag == 3 ) //For NVRAM to record write barcode(1) and IMEI(2) and both barcode and IMEI(3) history
        {
		    UtilityCnf.result.m_SetCleanBootFlagCnf.drv_statsu = FileOp_BackupToBinRegion_All_Ex(nNVRAMFlag);  
		}
		else //default
        {
            UtilityCnf.result.m_SetCleanBootFlagCnf.drv_statsu = FileOp_BackupToBinRegion_All(); 
        }
		UtilityCnf.status=META_SUCCESS;
        break;

    case FT_UTILCMD_CHECK_IF_LOW_COST_SINGLE_BANK_FLASH:	//query the single flash feature, we now just return.
        META_LOG("[Meta][FT] FT_Peripheral_OP pFTReq->type is FT_UTILCMD_CHECK_IF_LOW_COST_SINGLE_BANK_FLASH ");
        UtilityCnf.status=META_SUCCESS;
        break;

    case FT_UTILCMD_SAVE_MOBILE_LOG:                           //save mobile log
        META_LOG("[Meta][FT] FT_Peripheral_OP pFTReq->type is FT_UTILCMD_SAVE_MOBILE_LOG ");
        META_LOG("[Meta][FT] FT_UTILCMD_SAVE_MOBILE_LOG META Test %s,%d,%s",__FILE__,__LINE__,__FUNCTION__);
        UtilityCnf.result.m_SaveMobileLogCnf.drv_status = Meta_Mobile_Log();
        UtilityCnf.status = META_SUCCESS;
		break;
	case FT_UTILCMD_SET_LOG_LEVEL:	  
		level = req->cmd.m_SetLogLevelReq.level;
		META_LOG("[FTT_Drv:] FT_Peripheral_OP pFTReq->type is FT_UTILCMD_SET_LOG_LEVEL ");
		META_LOG("[FTT_Drv:] FT_UTILCMD_SET_LOG_LEVEL META Test %s,%d,%s,level = %d",__FILE__,__LINE__,__FUNCTION__,level);
		setLogLevel(level);
		UtilityCnf.status = META_SUCCESS;
		break;
	case FT_UTILCMD_SDIO_AUTO_CALIBRATION:	
		META_LOG("[FTT_Drv:] FT_Peripheral_OP pFTReq->type is FT_UTILCMD_SDIO_AUTO_CALIBRATION ");
		META_LOG("[FTT_Drv:] FT_UTILCMD_SDIO_AUTO_CALIBRATION META Test %s,%d,%s",__FILE__,__LINE__,__FUNCTION__);
		ret = FT_UtilLVAutok();
		if(ret != 0)
		{
			META_LOG("[FTT_Drv:] FT_UtilLVAutok() return fail! ret = %d ",ret);	
			UtilityCnf.status= META_FAILED;
		}
		else
		{
			META_LOG("[FTT_Drv:] FT_UtilLVAutok() return success!");
			UtilityCnf.status = META_SUCCESS;
		}
		break;
	case FT_UTILCMD_QUERY_WCNDRIVER_READY:
		META_LOG("[FTT_Drv:] FT_Peripheral_OP pFTReq->type is FT_UTILCMD_QUERY_WCNDRIVER_READY ");
		property_get("service.wcn.driver.ready",tempstr,"no");
		if(strcmp(tempstr,"yes")==0)
		{
		    UtilityCnf.result.m_QueryWCNDriverReadyCnf.result = 1; //has ready
		    META_LOG("[FTT_Drv:] FT_UTILCMD_QUERY_WCNDRIVER_READY() wcn driver ready");
		}
		else
		{
		    UtilityCnf.result.m_QueryWCNDriverReadyCnf.result = 0;  //not ready
		    META_LOG("[FTT_Drv:] FT_UTILCMD_QUERY_WCNDRIVER_READY() wcn driver not ready");
		}
		UtilityCnf.status = META_SUCCESS;
		break;
    default:
        META_LOG("[Meta][FT] FT_Peripheral_OP pFTReq->type error ");
        UtilityCnf.status= META_FAILED;
        break;

    }

Per_Exit:
    WriteDataToPC(&UtilityCnf, sizeof(FT_UTILITY_COMMAND_CNF),NULL, 0);

}

/////////////////////////////////////////////////////////////////////////////////////////



