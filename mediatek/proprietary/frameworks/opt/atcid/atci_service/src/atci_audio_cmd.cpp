/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 * 
 * MediaTek Inc. (C) 2010. All rights reserved.
 * 
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

#include "atci_audio_cmd.h"
#include "atci_service.h"
#include "pthread.h"
#include "stdio.h"

#include <utils/String8.h>
#include <media/AudioSystem.h>
#include <media/mediaplayer.h>
#include <system/audio.h>
//Ainge: Temporoary Modification for L PDK build pass
#include <media/IMediaHTTPService.h>
//Ainge: Temporoary Modification for L PDK build pass

#include <binder/IPCThreadState.h>
#include <binder/ProcessState.h>

using namespace android;

#if 0
extern "C"{
int enableTestMode(int enable, unsigned char *addr);
int queryTestMode();
}
#endif

char ACS_Ret_Str[8][64] = {
	"[0]ACOUSTIC MODE OFF\r\n\r\nOK\r\n",
	"[1]ACOUSTIC MODE ON\r\n\r\nOK\r\n",
	"[2]HEADSET PATH OPEN\r\n\r\nOK\r\n",
	"[3]HANDSET PATH OPEN\r\n\r\nOK\r\n",
	"[4]Slave ACOUSTIC MODE ON\r\n\r\nOK\r\n",
	"[5]Slave HEADSET PATH OPEN\r\n\r\nOK\r\n",
	"[6]Slave HANDSET PATH OPEN\r\n\r\nOK\r\n",
	"[7]ACOUSTIC DELAY MODE ON\r\n\r\nOK\r\n"};

typedef struct mp3_cmd_handle_type {
	char audioFileName[128];
	char responseStr[64];
}mp3_cmd_handle_type;

static mp3_cmd_handle_type mp3_cmd_handle_table[] = {
	{"/sdcard/No Signal.mp3","\r\n[1]NO SIGNAL\r\n\r\nOK\r\n"},
	{"/sdcard/1Khz_0dB_LR.mp3","\r\n[2]LR\r\n\r\nOK\r\n"},
	{"/sdcard/1Khz_0dB_L.mp3","\r\n[3]L\r\n\r\nOK\r\n"},
	{"/sdcard/1Khz_0dB_R.mp3","\r\n[4]R\r\n\r\nOK\r\n"},
	{"/sdcard/Multi Sine.mp3","\r\n[5]MULTI LR\r\n\r\nOK\r\n"},
	{"/sdcard/500hz_0dB_LR.mp3","\r\n[6]500LR\r\n\r\nOK\r\n"},
};

static bool Acoustic_Loopback_On = false;
static int ACS_Path = -1;

/*
* class ATAudioPlayer for audio file playback
*/
class ATAudioPlayer : public MediaPlayerListener
{
public:
	ATAudioPlayer();
	~ATAudioPlayer();
	int startAudioPlayer(char *filePath);
	int stopAudioPlayer();
	void notify(int msg, int ext1, int ext2, const android::Parcel* p);
private:
	sp <MediaPlayer> m_MediaPlayerClient;	
};

ATAudioPlayer::ATAudioPlayer()
{
	ALOGD("ATAudioPlayer constructor");
	
	// set up thread-pool
	sp<ProcessState> proc(ProcessState::self());
	ProcessState::self()->startThreadPool();
}

ATAudioPlayer::~ATAudioPlayer()
{
}

int ATAudioPlayer::startAudioPlayer(char *filePath)
{
	ALOGD("ATAudioPlayer::startAudioPlayer in +");


	if (m_MediaPlayerClient.get()==NULL) {
		m_MediaPlayerClient = new MediaPlayer();
		m_MediaPlayerClient->setListener(this);
	}else if (m_MediaPlayerClient->isPlaying()){
			m_MediaPlayerClient->stop();
			m_MediaPlayerClient->reset();
	}

//Ainge: Temporoary Modification for L PDK build pass
	if (m_MediaPlayerClient->setDataSource(NULL, filePath, NULL)!=NO_ERROR) {
	//if (m_MediaPlayerClient->setDataSource(filePath, NULL)!=NO_ERROR) {
//Ainge: Temporoary Modification for L PDK build pass
		m_MediaPlayerClient->reset();
		ALOGE("Fail to load the audio file");
		return false;
	}

    m_MediaPlayerClient->setAudioStreamType(AUDIO_STREAM_MUSIC);
	if (m_MediaPlayerClient->prepare()!=NO_ERROR) {
		m_MediaPlayerClient->reset();
		ALOGE("Fail to play the audio file, prepare failed");
		return false;
	}

	if (m_MediaPlayerClient->start()!=NO_ERROR) {
		m_MediaPlayerClient->reset();
		ALOGE("Fail to play the audio file, start failed");
		return false;
	}

	return true;
}

int ATAudioPlayer::stopAudioPlayer()
{
	ALOGD("ATAudioPlayer::stopAudioPlayer in +");
	if (m_MediaPlayerClient.get()!=NULL && m_MediaPlayerClient->isPlaying()) {
		if (m_MediaPlayerClient->stop()!=NO_ERROR) {
			ALOGE("Fail to stop playing the audio file");
			return false;
		}
		m_MediaPlayerClient->reset();
	}

	return true;
}

void ATAudioPlayer::notify(int msg,int ext1,int ext2, const android::Parcel* p)
{
	ALOGD("ATAudioPlayer received message: msg=%d, ext1=%d, ext2=%d", msg, ext1, ext2);
	switch(msg)
	{
	  case MEDIA_PLAYBACK_COMPLETE:
	  	if (m_MediaPlayerClient.get()!=NULL) {
			m_MediaPlayerClient->stop();
	  		m_MediaPlayerClient->reset();
	  	}
		
		ALOGD("ATAudioPlayer::notify -- audio playback complete");
	  	break;
	  case MEDIA_ERROR:
	  	if (m_MediaPlayerClient.get()!=NULL) {
	  		m_MediaPlayerClient->reset();
	  	}
		
		ALOGE("AudioCmdHandler::notify -- audio playback error, exit");
	  	break;
	  default:
	  	break;
	}
}

ATAudioPlayer mp3Player;

#ifdef __cplusplus
extern "C" {
#endif

#include <at_tok.h>

/*
* Following function are atci service callback, need to be called in *.c file
* audio_mp3_handler            for AT%MPT
* audio_circuit_handler        for AT%ACS
* audio_speaker_phone_handler  for AT%SPM
* audio_volume_control_handler for AT%VLC
* audio_fm_radio_control_handler for AT%FMR
*/

#define AUDIO_FM_RADIO_TEST  0x92

int audio_fm_radio_control_handler(char* cmdline, ATOP_t at_op, char* response)
{
    ALOGD("audio cmd handler handles cmdline:%s", cmdline);

    int actinoID = 0, err = 0;

    switch(at_op) {
        case AT_ACTION_OP:
        case AT_READ_OP:
        case AT_TEST_OP:
            sprintf(response,"\r\nOK\r\n");
            break;
        case AT_SET_OP:
            at_tok_nextint(&cmdline, &actinoID);
            ALOGD("audio_mp3_handler action ID:%d", actinoID);
            if (actinoID<0 || actinoID>2) {
                sprintf(response,"\r\nFMRX ERROR\r\n");
                break;
            }
    
            if(actinoID == 0) {
                sprintf(response,"\r\n[0]FMR OFF\r\n\r\nOK\r\n");
            } 
            else if (actinoID == 1) {
                sprintf(response,"\r\n[1]FMR 90.1\r\n\r\nOK\r\n");
            }
            else if (actinoID == 2) {
                sprintf(response,"\r\n[2]FMR 106.3\r\n\r\nOK\r\n");
            }
//Ainge: Temporoary Modification for L PDK build pass
			#if 0
//Ainge: Temporoary Modification for L PDK build pass
            AudioSystem::SetAudioCommand(AUDIO_FM_RADIO_TEST, actinoID);
//Ainge: Temporoary Modification for L PDK build pass
			#endif
//Ainge: Temporoary Modification for L PDK build pass
//            sprintf(response,"\r\nOK\r\n");
            break;
        default:
            break;
    }

    return 0;
}


int
bt_bttm_cmd_handler(
    char* cmdline,
    ATOP_t at_op,
    char* response
    )
{
    int ret = -1;
    int query = -1;
    char sKeyParams[128];
    unsigned char addr[6] = {0x00, 0x05, 0xC9,0x00, 0x00, 0x00};

    if (at_op == AT_SET_OP)
    {
        int actionID = 0;
        at_tok_nextint(&cmdline, &actionID);

        if (actionID == 3)
        {
            //ret = enableTestMode(0, NULL);

            //4 Add exit BT PCM Loopback
            sprintf(sKeyParams, "SET_LOOPBACK_TYPE=%d", 0);
            AudioSystem::setParameters(0, String8(sKeyParams));
            sprintf(response, "\r\n%cBTTM OK%c\r\n\r\nOK\r\n", STX, ETX);
        }
        else
        {
            if ((actionID == 2)|| (actionID >= 11 && actionID <= 42))
            {
                if ( actionID == 2)
                {
                    addr[5] = 0x00;
                }
                else if (actionID >= 11 && actionID <= 19)
                {
                    addr[5] = actionID - 10;
                }
                else if (actionID >= 20 && actionID <= 29)
                {
                    addr[5] = actionID - 4;
                }
                else if (actionID >= 30 && actionID <= 39)
                {
                    addr[5] = actionID + 2;
                }
                else if (actionID >= 40 && actionID <= 42)
                {
                    addr[5] = actionID + 8;
                }

                //ret = enableTestMode(1, addr);

                //4 Add enter BT PCM Loopback
                sprintf(sKeyParams, "SET_LOOPBACK_TYPE=%d", 32);
                AudioSystem::setParameters(0, String8(sKeyParams));

                sprintf(response, "\r\n%cBTTM OK%c\r\n\r\nOK\r\n", STX, ETX);
            }
            else
            {
                sprintf(response, "\r\n%cBTTM ERROR%c\r\n\r\nERROR\r\n", STX, ETX);
            }
        }
    }
    else if(at_op == AT_READ_OP || at_op == AT_ACTION_OP || at_op == AT_TEST_OP)
    {
        //query = queryTestMode() ? 1 : 2;
        sprintf(response, "\r\n%c%d%c\r\n\r\nOK\r\n", STX, query, ETX );
        ret = 1;
    }

    return ret;
}


/*
* measuring of MP3 output level and quality
*/
int audio_mp3_handler(char* cmdline, ATOP_t at_op, char* response)
{
    ALOGD("audio cmd handler handles cmdline:%s", cmdline);

    int actinoID = 0, err = 0;
    char fileName[256];

    switch(at_op) {
        case AT_ACTION_OP:
        case AT_READ_OP:
        case AT_TEST_OP:
            sprintf(response,"\r\nOK\r\n");
            break;
        case AT_SET_OP:
            at_tok_nextint(&cmdline, &actinoID);
            ALOGD("audio_mp3_handler action ID:%d", actinoID);
            if (actinoID<0 || actinoID>6) {
                sprintf(response,"\r\nMP3 ERROR\r\n");
                break;
            }
            if(actinoID == 0){
                err = mp3Player.stopAudioPlayer();
                AudioSystem::setForceUse(AUDIO_POLICY_FORCE_FOR_MEDIA, AUDIO_POLICY_FORCE_NONE);
                if(!err)
                    sprintf(response,"\r\nMP3 ERROR\r\n");
                else
                    sprintf(response,"\r\n[0]MP3 OFF\r\nOK\r\n\r\n");
            }else {
                ALOGD("audio_mp3_handler before start audio player");

                /* set playback path */
                if (ACS_Path==2) {
                    AudioSystem::setForceUse(AUDIO_POLICY_FORCE_FOR_MEDIA, AUDIO_POLICY_FORCE_HEADPHONES);
                }
                else if (ACS_Path==3) {
                    AudioSystem::setForceUse(AUDIO_POLICY_FORCE_FOR_MEDIA, AUDIO_POLICY_FORCE_SPEAKER);
                }

                err = mp3Player.startAudioPlayer(mp3_cmd_handle_table[actinoID-1].audioFileName);
                if(!err) {
                    sprintf(response,"\r\nMP3 ERROR\r\n");
                }else {
                    sprintf(response,"\r\n%s\r\n", mp3_cmd_handle_table[actinoID-1].responseStr);
                }
            }
            break;
        default:
            break;
    }

    return 0;
}

/*
* Audio circuit test
* Audio circuit inspection ( MIC<->SPK, EarMic<->EarPhone)
* Packet Loop Back ON implementation
* when inserting Ear Jack, it becomes loopback through ear-mic, ear-phone.
* when not inserting Ear jack, it becomes loopback through mic and receiver
*/
int audio_circuit_handler(char* cmdline, ATOP_t at_op, char* response)
{
    ALOGD("audio cmd handler handles cmdline:%s", cmdline);

    int actinoID = 0,  internal_actinoID = 0;
    int mLoopbackMode = 0;
    char sKeyParams[128];
    char *pParam = sKeyParams;
    String8 keyValuePairs;

    switch(at_op) {
        case AT_ACTION_OP:
        case AT_READ_OP:
        case AT_TEST_OP:
            keyValuePairs = AudioSystem::getParameters(0,String8("AT_GetAcousticLoopback"));
            ALOGD("audio cmd handler get loopback mode:%s", keyValuePairs.string());
            if (keyValuePairs==String8("")) {
                sprintf(response,"\r\nACS ERROR\r\n");
                break;
            }
            strcpy(sKeyParams, keyValuePairs.string());
            at_tok_start_flag(&pParam, '=');
            at_tok_nextint(&pParam, &mLoopbackMode);
            sprintf(response,"\r\n%d\r\n\r\nOK\r\n", mLoopbackMode);
            break;
        case AT_SET_OP:
            at_tok_nextint(&cmdline, &actinoID);
            if (actinoID<0 || actinoID>7) {
                sprintf(response,"\r\nACS ERROR\r\n");
                break;
            }else if(actinoID>3 && actinoID!=7){
                sprintf(response,"\r\nNot Implement\r\n");
                break;
            }
            /*
            * Internal useing command
            * 0 : turn off loopback
            * 1 : turn on loopback with the current path
            * 2 : turn on loopback with headset
            * 3 : turn on loopback with receiver
            * 5 : set headset path
            * 6 : set receiver path
            * 7 : set speaker path
            */

            if (actinoID == 0) {
                internal_actinoID = 0;
            }
            else if (actinoID == 1) {
                if (Acoustic_Loopback_On) {
                    sprintf(response,"\r\nACS ERROR\r\n");
                    break;
                }
                if (ACS_Path ==2) {
                    internal_actinoID = 2;
                }else if (ACS_Path ==3) {
                    internal_actinoID = 3;
                }else {
                    internal_actinoID = 1;
                }
            }else if (actinoID == 2) {
                // turn off loopback
                if (Acoustic_Loopback_On) {
                    sprintf(sKeyParams, "SET_AT_ACS=%d", 0);
                    AudioSystem::setParameters(0,String8(sKeyParams));
                    usleep(50000);

                    if (Acoustic_Loopback_On==7) {
                        sprintf(sKeyParams, "SET_LOOPBACK_MODEM_DELAY_FRAMES=%d", 32);
                        AudioSystem::setParameters(0,String8(sKeyParams));
                    }
                    internal_actinoID = 2;
                }
                else {
                    internal_actinoID = 5;
                }
            }else if (actinoID == 3) {
                // turn off loopback
                if (Acoustic_Loopback_On) {
                    sprintf(sKeyParams, "SET_AT_ACS=%d", 0);
                    AudioSystem::setParameters(0,String8(sKeyParams));
                    usleep(50000);

                    if (Acoustic_Loopback_On==7) {
                        sprintf(sKeyParams, "SET_LOOPBACK_MODEM_DELAY_FRAMES=%d", 32);
                        AudioSystem::setParameters(0,String8(sKeyParams));
                    }
                    internal_actinoID = 3;
                } else {
                    internal_actinoID = 7;
                }
            }else if (actinoID == 7) {
                if (Acoustic_Loopback_On) {
                    sprintf(response,"\r\nACS ERROR\r\n");
                    break;
                }
                sprintf(sKeyParams, "SET_LOOPBACK_MODEM_DELAY_FRAMES=%d", 32);
                AudioSystem::setParameters(0,String8(sKeyParams));

                internal_actinoID = 3;
            }

            sprintf(sKeyParams, "SET_AT_ACS=%d", internal_actinoID);
            AudioSystem::setParameters(0,String8(sKeyParams));

            /* Store the ordered command */
            if (actinoID==0 || actinoID==1 || actinoID==7) {
                Acoustic_Loopback_On = actinoID;

                if (actinoID==0) {
                sprintf(sKeyParams, "SET_LOOPBACK_MODEM_DELAY_FRAMES=%d", 12);
                AudioSystem::setParameters(0,String8(sKeyParams));
                usleep(5000);
                }
            }

            if (actinoID==2 || actinoID==3)
            {
                ACS_Path = actinoID;
            }

            sprintf(response, "\r\n%s\r\n", &ACS_Ret_Str[actinoID][0]);
            break;
        default:
            break;
    }

    return 0;
}

/*
*
*/
int audio_pure_loopback(char* cmdline, ATOP_t at_op, char* response)
{
    ALOGD("audio cmd handler handles cmdline:%s", cmdline);

    int actinoID = 0;
    int mLoopbackMode = 0;
    char sKeyParams[128];
    char *pParam = sKeyParams;
    String8 keyValuePairs;

    switch(at_op) {
        case AT_ACTION_OP:
        case AT_READ_OP:
        case AT_TEST_OP:
            sprintf(response,"\r\nNot Implement\r\n\r\nOK\r\n");
            break;
        case AT_SET_OP:
            sprintf(response,"\r\nNot Implement\r\n\r\nOK\r\n");
            break;
        default:
            break;
    }

    return 0;
}

/*
*After call connected, speaker phone mode on/off
*/
int audio_speaker_phone_handler(char* cmdline, ATOP_t at_op, char* response)
{
    ALOGD("audio cmd handler handles cmdline:%s", cmdline);
    int actinoID = 0;
    int SpeakerPhoneMode_On = 0;

    switch(at_op) {
    case AT_ACTION_OP:
    case AT_READ_OP:
    case AT_TEST_OP:
        SpeakerPhoneMode_On = (int)AudioSystem::getForceUse(AUDIO_POLICY_FORCE_FOR_COMMUNICATION);
        if (SpeakerPhoneMode_On == AUDIO_POLICY_FORCE_SPEAKER)
            sprintf(response,"\r\n1\r\n\r\nOK\r\n");
        else
            sprintf(response,"\r\n0\r\n\r\nOK\r\n");
            break;
    case AT_SET_OP:
        at_tok_nextint(&cmdline, &actinoID);
        if (actinoID==0) {
            AudioSystem::setForceUse(AUDIO_POLICY_FORCE_FOR_COMMUNICATION, AUDIO_POLICY_FORCE_NONE);
            sprintf(response, "\r\n[0]SPM OFF\r\n\r\nOK\r\n");
        }else if (actinoID==1) {
            AudioSystem::setForceUse(AUDIO_POLICY_FORCE_FOR_COMMUNICATION, AUDIO_POLICY_FORCE_SPEAKER);
            sprintf(response, "\r\n[1]SPM ON\r\n\r\nOK\r\n", actinoID);
        }else {
            sprintf(response, "\r\nSPM ERROR\r\n");
        }
        break;
    default:
        break;
    }

    return 0;
}

/*
* Control all audio volumes of cellular phone's call, FMR, acoustic and MP3
*/

int audio_volume_control_handler(char* cmdline, ATOP_t at_op, char* response) 
{
    ALOGD("audio cmd handler handles cmdline:%s", cmdline);

    int actinoID = 0;
    int mStreamVolumeIndex = 0;
    int mPhoneMode = 0;
    float mVoiceVolume = 0.0;
    bool bMusic_FM_Mute = false;
    char sKeyParams[64];
    char *pParam = sKeyParams;
    String8 keyValuePairs;

    int mStreamVolumeValue[6][3] = {{0,0,0},{1,1,1},{4,8,4},{6,13,13},{4,8,4},{5,12,12}};
    float mVoiceVolumeValue[6] = {0.0,0.1,0.45,1.0,0.45,0.9};
    int mVoiceVolumeLevelValue[6] = {0,0,3,6,4,5};

    keyValuePairs = AudioSystem::getParameters(0, String8("GetPhoneMode"));
    ALOGD("audio cmd handler get phone mode:%s", keyValuePairs.string());
    strcpy(sKeyParams, keyValuePairs.string());
    at_tok_start_flag(&pParam, '=');
    at_tok_nextint(&pParam, &mPhoneMode);

    switch(at_op) {
        case AT_ACTION_OP:
        case AT_READ_OP:
        case AT_TEST_OP:
            if (mPhoneMode==AUDIO_MODE_IN_CALL || mPhoneMode==AUDIO_MODE_IN_COMMUNICATION || Acoustic_Loopback_On){
                keyValuePairs = AudioSystem::getParameters(0, String8("AT_GetVolumeMute"));
                ALOGD("audio cmd handler get voice mute:%s", keyValuePairs.string());
                strcpy(sKeyParams, keyValuePairs.string());
                pParam = sKeyParams;
                at_tok_start_flag(&pParam, '=');
                bMusic_FM_Mute = strcmp(pParam,"true")==0?true:false;
                if (!bMusic_FM_Mute) {
                    keyValuePairs = AudioSystem::getParameters(0, String8("GetVoiceVolume"));
                    ALOGD("audio cmd handler get voice volume:%s", keyValuePairs.string());
                    sscanf(keyValuePairs.string(),"%[a-zA-Z=]%f", sKeyParams, &mVoiceVolume);
                    ALOGD("audio cmd handler get voice volume:%f", mVoiceVolume);
                    if ((mVoiceVolume-0.10<=0.001) && (mVoiceVolume-0.1>=-0.001)) {
                        sprintf(response,"\r\n1\r\n\r\nOK\r\n");
                    }else if ((mVoiceVolume-0.45<=0.001) && (mVoiceVolume-0.45>=-0.001)) {
                        sprintf(response,"\r\n2\r\n\r\nOK\r\n");
                    }else if ((mVoiceVolume-1.0<=0.001) && (mVoiceVolume-1.0>=-0.001)) {
                        sprintf(response,"\r\n3\r\n\r\nOK\r\n");
                    }else {
                        sprintf(response,"\r\nNot Min/Default/Max Volume\r\n\r\nOK\r\n");
                    }
                }else {
                    sprintf(response,"\r\n0\r\n\r\nOK\r\n");
                }
            }else { 
                AudioSystem::getStreamMute(AUDIO_STREAM_MUSIC, &bMusic_FM_Mute);
                AudioSystem::getStreamVolumeIndex(AUDIO_STREAM_MUSIC, &mStreamVolumeIndex, AUDIO_DEVICE_OUT_ALL);
                if(bMusic_FM_Mute) {
                    sprintf(response,"\r\n0\r\n\r\nOK\r\n");
                }else if (mStreamVolumeIndex==1) {
                    sprintf(response,"\r\n1\r\n\r\nOK\r\n");
                }else if (mStreamVolumeIndex==8) {
                    sprintf(response,"\r\n2\r\n\r\nOK\r\n");
                }else if (mStreamVolumeIndex==13) {
                    sprintf(response,"\r\n3\r\n\r\nOK\r\n");
                } else {
                    sprintf(response,"\r\nNot Min/Default/Max Volume\r\n\r\nOK\r\n");
                }
            }
            break; 
        case AT_SET_OP:
            at_tok_nextint(&cmdline, &actinoID);
            if (actinoID<0 || actinoID>5) {
                sprintf(response,"\r\nVLC ERROR\r\n");
                break;
            }

            if (mPhoneMode==AUDIO_MODE_IN_CALL || mPhoneMode==AUDIO_MODE_IN_COMMUNICATION || Acoustic_Loopback_On) {
                #if 0
                bMusic_FM_Mute = actinoID==0?true:false;
                if (bMusic_FM_Mute) {
                AudioSystem::setParameters(0, String8("AT_SetVolumeMute=1"));
                }else{
                AudioSystem::setParameters(0, String8("AT_SetVolumeMute=0"));
                AudioSystem::setVoiceVolume(mVoiceVolumeValue[actinoID]);
                }
                #endif
                sprintf(sKeyParams, "SET_AT_ACSVolume=%d", mVoiceVolumeLevelValue[actinoID]);
                AudioSystem::setParameters(0, String8(sKeyParams));
            }else if(actinoID==0) {
                AudioSystem::setStreamMute(AUDIO_STREAM_MUSIC, true);
                //AudioSystem::setStreamMute(AUDIO_STREAM_FM, true);
            }else {
                // unmute music stream, and set music volume
                AudioSystem::setStreamMute(AUDIO_STREAM_MUSIC, false);
                AudioSystem::setStreamVolumeIndex(AUDIO_STREAM_MUSIC, mStreamVolumeValue[actinoID][1],AUDIO_DEVICE_OUT_SPEAKER);
                AudioSystem::setStreamVolumeIndex(AUDIO_STREAM_MUSIC, mStreamVolumeValue[actinoID][1],AUDIO_DEVICE_OUT_WIRED_HEADSET);
                AudioSystem::setStreamVolumeIndex(AUDIO_STREAM_MUSIC, mStreamVolumeValue[actinoID][1],AUDIO_DEVICE_OUT_WIRED_HEADPHONE);
                //unmute FM stream, and set FM volume
                //AudioSystem::setStreamMute(AUDIO_STREAM_FM, false);
                //AudioSystem::setStreamVolumeIndex(AUDIO_STREAM_FM, mStreamVolumeValue[actinoID][2],AUDIO_DEVICE_OUT_SPEAKER);
                //AudioSystem::setStreamVolumeIndex(AUDIO_STREAM_FM, mStreamVolumeValue[actinoID][2],AUDIO_DEVICE_OUT_WIRED_HEADSET);
                //AudioSystem::setStreamVolumeIndex(AUDIO_STREAM_FM, mStreamVolumeValue[actinoID][2],AUDIO_DEVICE_OUT_WIRED_HEADPHONE);
            }
        #if 0
            if (actinoID != 0)
            {
            sprintf(sKeyParams, "SET_AT_ACSVolume=%d", mVoiceVolumeLevelValue[actinoID]);
            AudioSystem::setParameters(0, String8(sKeyParams));
            }
        #endif
            sprintf(response, "\r\n[%d]VOLUME\r\n\r\nOK\r\n",actinoID);
            break;
        default:
            break;
    }

    return 0;
}

#ifdef __cplusplus
}
#endif
