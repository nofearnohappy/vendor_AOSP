
#include <tinyalsa/asoundlib.h>
#include "AudioALSADeviceConfigManager.h"
#include "AudioALSADriverUtility.h"

#define LOG_TAG "AudioALSADeviceConfigManager"

#define AUDIO_DEVICE_EXT_CONFIG_FILE         "/system/etc/audio_device.xml"

namespace android
{

DeviceCtlDescriptor::DeviceCtlDescriptor()
{
    DeviceStatusCounter = 0;
}


AudioALSADeviceConfigManager *AudioALSADeviceConfigManager::UniqueAlsaDeviceConfigParserInstance = NULL;

AudioALSADeviceConfigManager *AudioALSADeviceConfigManager::getInstance()
{
    if (UniqueAlsaDeviceConfigParserInstance == 0)
    {
        ALOGD("+UniqueAlsaDeviceConfigParserInstance\n");
        UniqueAlsaDeviceConfigParserInstance = new AudioALSADeviceConfigManager();
        ALOGD("-UniqueAlsaDeviceConfigParserInstance\n");
    }
    return UniqueAlsaDeviceConfigParserInstance;
}

AudioALSADeviceConfigManager::AudioALSADeviceConfigManager():
    mMixer(NULL),
    mConfigsupport(false),
    mInit(false)

{
    ALOGD("%s()", __FUNCTION__);

    int ret = LoadAudioConfig(AUDIO_DEVICE_EXT_CONFIG_FILE);
    if (ret != NO_ERROR)
    {
        mConfigsupport = false;
    }
    else
    {
        mConfigsupport = true;
    }

    if (mMixer == NULL)
    {
        mMixer = AudioALSADriverUtility::getInstance()->getMixer();
        ASSERT(mMixer != NULL);
    }

    mInit = true;

    dump();
}

status_t AudioALSADeviceConfigManager::GetVersion(TiXmlElement *root)
{
    const char *VersionNumber  = root->Attribute("value");  // get action
    VersionControl = String8(VersionNumber);
    ALOGD("GetVersion = %s", VersionNumber);
    return NO_ERROR;
}

bool AudioALSADeviceConfigManager::CheckDeviceExist(const char *devicename)
{
    int i = 0;
    for (i = 0; i < mDeviceVector.size(); i++)
    {
        DeviceCtlDescriptor *DeviceScriptor  = mDeviceVector.itemAt(i);
        if (strcmp(devicename, DeviceScriptor->mDevicename.string()) == 0)
        {
            ALOGD("CheckDeviceExist exist devicename = %s", devicename);
            return true;
        }
    }
    ALOGD("CheckDeviceExist not exist devicename = %s", devicename);
    return false;
}

DeviceCtlDescriptor *AudioALSADeviceConfigManager::GetDeviceDescriptorbyname(const char *devicename)
{
    ALOGD("%s", __FUNCTION__);
    int i = 0;
    for (i = 0; i < mDeviceVector.size(); i++)
    {
        DeviceCtlDescriptor *DeviceScriptor  = mDeviceVector.itemAt(i);
        if (strcmp(devicename, DeviceScriptor->mDevicename.string()) == 0)
        {
            ALOGD("CheckDeviceExist exist devicename = %s", devicename);
            return mDeviceVector.itemAt(i);
        }
    }
    return NULL;
}

status_t AudioALSADeviceConfigManager::ParseDeviceSequence(TiXmlElement *root)
{
    DeviceCtlDescriptor *TempDeviceDescriptor  = NULL;
    if (root != NULL)
    {
        const char *devicename  = root->Attribute("name");  // get path name
        const char *action  = root->Attribute("value");  // get action
        ALOGD("%s() devicename = %s action = %s", __FUNCTION__, devicename, action);
        if (CheckDeviceExist(devicename) == false) // this device has not exist , new object
        {
            TempDeviceDescriptor = new DeviceCtlDescriptor();
            TempDeviceDescriptor->mDevicename = String8(devicename);
            mDeviceVector.add(TempDeviceDescriptor);
        }
        else
        {
            TempDeviceDescriptor = GetDeviceDescriptorbyname(devicename);  // get instance in vector
        }

        if (TempDeviceDescriptor == NULL)
        {
            ALOGE("%s() is NULL pointer , return", __FUNCTION__);
            return INVALID_OPERATION;
        }

        if (strcmp(action, AUDIO_DEVICE_TURNON) == 0)
        {
            ALOGD("add turn on sequnce");
            TiXmlElement *child = root->FirstChildElement("kctl");
            while (child != NULL)
            {
                const char *valname  = child->Attribute("name");
                const char *valvalue  = child->Attribute("value");
                ALOGD("valname = %s  valvalue = %s  ", valname, valvalue);
                TempDeviceDescriptor->mDeviceCltonVector.add(String8(valname));
                TempDeviceDescriptor->mDeviceCltonVector.add(String8(valvalue));
                child = child->NextSiblingElement("kctl");
            }
        }
        else if (strcmp(action, AUDIO_DEVICE_TURNOFF) == 0)
        {
            ALOGD("add turn off sequnce");
            TiXmlElement *child = root->FirstChildElement("kctl");
            while (child != NULL)
            {
                const char *valname  = child->Attribute("name");
                const char *valvalue  = child->Attribute("value");
                ALOGD("valname = %s  valvalue = %s  ", valname, valvalue);
                TempDeviceDescriptor->mDeviceCltoffVector.add(String8(valname));
                TempDeviceDescriptor->mDeviceCltoffVector.add(String8(valvalue));
                child = child->NextSiblingElement("kctl");
            }
        }
        else if (strcmp(action, AUDIO_DEVICE_SETTING) == 0)
        {
            ALOGD("add AUDIO_DEVICE_SETTING");
            TiXmlElement *child = root->FirstChildElement("kctl");
            while (child != NULL)
            {
                const char *valname  = child->Attribute("name");
                const char *valvalue  = child->Attribute("value");
                ALOGD("valname = %s  valvalue = %s  ", valname, valvalue);
                TempDeviceDescriptor->mDeviceCltsettingVector.add(String8(valname));
                TempDeviceDescriptor->mDeviceCltsettingVector.add(String8(valvalue));
                child = child->NextSiblingElement("kctl");
            }
        }
        else
        {
            ALOGD("device sequnce either not turn on and turn off");
        }
    }
    return NO_ERROR;
}

status_t AudioALSADeviceConfigManager::ParseInitSequence(TiXmlElement *root)
{
    ALOGD("+%s()", __FUNCTION__);
    TiXmlElement *child = root->FirstChildElement("kctl");
    while (child != NULL)
    {
        const char *valname  = child->Attribute("name");
        const char *valvalue  = child->Attribute("value");
        ALOGD("valname = %s  valvalue = %s  ", valname, valvalue);
        mDeviceCtlSeq.mDeviceCltNameVector.add(String8(valname));
        mDeviceCtlSeq.mDeviceCltValueVector.add(String8(valvalue));
        child = child->NextSiblingElement("kctl");
    }
    ALOGD("-%s()", __FUNCTION__);
    return NO_ERROR;
}

bool  AudioALSADeviceConfigManager::SupportConfigFile()
{
    return mConfigsupport;
}

status_t AudioALSADeviceConfigManager::LoadAudioConfig(const char *path)
{
    if (mInit == true)
    {
        return ALREADY_EXISTS;
    }

    ALOGD("%s()", __FUNCTION__);
    TiXmlDocument doc(path);
    bool loadOkay = doc.LoadFile();
    if (loadOkay)
    {
        // document is loaded
        ALOGE("LoadAudioConfig success ");

        // here handle for xml version and other inforamtion
        TiXmlDeclaration *declaration = doc.FirstChild()->ToDeclaration();
        ALOGD("TiXmlDeclaration version = %s ", declaration->Version());
        ALOGD("TiXmlDeclaration Encoding = %s ", declaration->Encoding());
        ALOGD("TiXmlDeclaration Standalone = %s ", declaration->Standalone());

        TiXmlElement *root = doc.FirstChildElement("mixercontrol"); // find with mixer
        if(root != NULL)
        {
            TiXmlElement *Version = root->FirstChildElement("versioncontrol"); // find with version contol
            if(Version != NULL)
            {
                GetVersion(Version);
            }
        }

        if (root)
        {
            ALOGD("FirstChildElement can find mixer");
            ParseInitSequence(root);
            TiXmlElement *child = root->FirstChildElement("path");
            while (child)
            {
                ParseDeviceSequence(child);
                child = child->NextSiblingElement("path");
            }
        }
    }
    else
    {
        // load failed
        ALOGE("LoadAudioConfig fail ");
        return INVALID_OPERATION;
    }
    return NO_ERROR;
}

status_t AudioALSADeviceConfigManager::ApplyDeviceTurnonSequenceByName(const char *DeviceName)
{
    int count = 0;
    DeviceCtlDescriptor *descriptor = GetDeviceDescriptorbyname(DeviceName);
    if (descriptor == NULL)
    {
        ALOGE("%s  DeviceName = %s descriptor == NULL", __FUNCTION__, DeviceName);
        return INVALID_OPERATION;
    }
    ALOGD("%s() DeviceName = %s descriptor->DeviceStatusCounte = %d", __FUNCTION__, DeviceName, descriptor->DeviceStatusCounter);
    if (descriptor->DeviceStatusCounter == 0)
    {
        for (count = 0; count < descriptor->mDeviceCltonVector.size(); count += 2)
        {
            String8 cltname = descriptor->mDeviceCltonVector.itemAt(count);
            String8 cltvalue = descriptor->mDeviceCltonVector.itemAt(count + 1);
            ALOGD("cltname = %s cltvalue = %s", cltname.string(), cltvalue.string());
            if (mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, cltname.string()), cltvalue.string()))
            {
                ALOGE("Error: ApplyDeviceTurnonSequence  cltname.string () = %s cltvalue.string () = %s", cltname.string(), cltvalue.string());
                ASSERT(false);
            }
        }
    }
    descriptor->DeviceStatusCounter++;
    return NO_ERROR;
}

status_t AudioALSADeviceConfigManager::ApplyDeviceTurnoffSequenceByName(const char *DeviceName)
{
    int count = 0;
    DeviceCtlDescriptor *descriptor = GetDeviceDescriptorbyname(DeviceName);
    if (descriptor == NULL)
    {
        ALOGE("%s  DeviceName = %s descriptor == NULL", __FUNCTION__, DeviceName);
        return INVALID_OPERATION;
    }
    ALOGD("%s() DeviceName = %s descriptor->DeviceStatusCounte = %d", __FUNCTION__, DeviceName, descriptor->DeviceStatusCounter);

    descriptor->DeviceStatusCounter--;
    if (descriptor->DeviceStatusCounter == 0)
    {
        for (count = 0; count < descriptor->mDeviceCltoffVector.size(); count += 2)
        {
            String8 cltname = descriptor->mDeviceCltoffVector.itemAt(count);
            String8 cltvalue = descriptor->mDeviceCltoffVector.itemAt(count + 1);
            ALOGD("cltname = %s cltvalue = %s", cltname.string(), cltvalue.string());
            if (mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, cltname.string()), cltvalue.string()))
            {
                ALOGE("Error: ApplyDeviceTurnoffSequenceByName devicename = %s", descriptor->mDevicename.string());
                ASSERT(false);
            }
        }
    }
    return NO_ERROR;
}


status_t AudioALSADeviceConfigManager::ApplyDeviceSettingByName(const char *DeviceName)
{
    int count = 0;
    DeviceCtlDescriptor *descriptor = GetDeviceDescriptorbyname(DeviceName);
    if (descriptor == NULL)
    {
        ALOGE("%s  DeviceName = %s descriptor == NULL", __FUNCTION__, DeviceName);
        return INVALID_OPERATION;
    }
    ALOGD("%s() DeviceName = %s descriptor->DeviceStatusCounte = %d", __FUNCTION__, DeviceName, descriptor->DeviceStatusCounter);

    for (count = 0; count < descriptor->mDeviceCltsettingVector.size(); count += 2)
    {
        String8 cltname = descriptor->mDeviceCltsettingVector.itemAt(count);
        String8 cltvalue = descriptor->mDeviceCltsettingVector.itemAt(count + 1);
        ALOGD("cltname = %s cltvalue = %s", cltname.string(), cltvalue.string());
        if (mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, cltname.string()), cltvalue.string()))
        {
            ALOGE("Error: ApplyDeviceTurnoffSequenceByName devicename = %s", descriptor->mDevicename.string());
            ASSERT(false);
        }
    }
    return NO_ERROR;
}

void AudioALSADeviceConfigManager::dump()
{
    ALOGD("AudioALSADeviceConfigManager dump");
    int count = 0, sequence = 0;
    DeviceCtlDescriptor *descriptor = NULL;

    ALOGD("AudioALSADeviceConfigManager dump init sequence");
    for (count  = 0; count < mDeviceCtlSeq.mDeviceCltNameVector.size() ; count++)
    {
        String8 temp = mDeviceCtlSeq.mDeviceCltNameVector.itemAt(count);
        String8 temp1 = mDeviceCtlSeq.mDeviceCltValueVector.itemAt(count);
        ALOGD("init sequnce kclt = %s value = %s", temp.string(), temp1.string());
    }

    for (count  = 0; count < mDeviceVector.size() ; count++)
    {
        descriptor = mDeviceVector.itemAt(count);
        ALOGD("mDescritor->name = %s", descriptor->mDevicename.string());
        for (sequence  = 0; sequence < descriptor->mDeviceCltonVector.size() ; sequence += 2)
        {
            String8 temp = descriptor->mDeviceCltonVector.itemAt(sequence);
            String8 temp1 = descriptor->mDeviceCltonVector.itemAt(sequence + 1);
            ALOGD("turn on name = %s value = %s ", temp.string(), temp1.string());
        }
        for (sequence  = 0; sequence < descriptor->mDeviceCltoffVector.size() ; sequence += 2)
        {
            String8 temp = descriptor->mDeviceCltoffVector.itemAt(sequence);
            String8 temp1 = descriptor->mDeviceCltoffVector.itemAt(sequence + 1);
            ALOGD("turn off name = %s value = %s ", temp.string(), temp1.string());
        }
        for (sequence  = 0; sequence < descriptor->mDeviceCltsettingVector.size() ; sequence += 2)
        {
            String8 temp = descriptor->mDeviceCltsettingVector.itemAt(sequence);
            String8 temp1 = descriptor->mDeviceCltsettingVector.itemAt(sequence + 1);
            ALOGD("mDeviceCltsettingVector  name = %s value = %s ", temp.string(), temp1.string());
        }
    }
    ALOGD("dump done");
}


}
