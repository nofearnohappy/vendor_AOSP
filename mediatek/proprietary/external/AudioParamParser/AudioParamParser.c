#include "AudioParamParserPriv.h"

#include <assert.h>
#include <stdio.h>
#include <string.h>
#include <sys/stat.h>

#ifdef __linux__
#include <dirent.h>
#include <unistd.h>
#else
#include <windows.h>
#pragma warning( disable : 4996 )
#endif

static AppHandle appHandleInst;
static int appHandleInited = 0;

#ifdef FORCE_DEBUG_LEVEL
int appDebugLevel = DEBUG_LEVEL;        /* Global debug level setting */
#else
int appDebugLevel = INFO_LEVEL;         /* Global debug level setting */
#endif

FILE *appLogFp = NULL;
int outputLogToStdout = 0;

#ifndef WIN32
static pthread_rwlock_t appHandleInstLock = PTHREAD_RWLOCK_INITIALIZER;
static const char *appHandleInstLockCallerFun = NULL;  /* Used to cache the lock holder */
#else
int __stdcall DllMain(HINSTANCE hInstance, DWORD dwReason, PVOID pvReserved)
{
    return TRUE;
}
#endif

EXPORT APP_STATUS appHandleInit(AppHandle *appHandle)
{
#ifdef _DEBUG
    /* Alwasy show the output console for debug build */
    appHandleRedirectIOToConsole();
#endif

    INFO_LOG("appHandle = 0x%p\n", appHandle);

    if (appHandle)
    {
        appHandle->xmlDir = NULL;
        appHandle->xmlCusDir = NULL;
        appHandle->audioTypeHash = NULL;
        appHandle->featureOptionsHash = NULL;
        appHandle->featureOptionsDoc = NULL;
        appHandle->noficyCbList = NULL;
#ifndef WIN32
        appHandle->lockCallerFun = NULL;
        appHandle->appThreadExit = 0;
        appHandle->inotifyFd = 0;

        pthread_rwlock_init(&appHandle->lock, NULL);
#endif
        appHandleShowAudioTypeSupportedVerInfo(appHandle);
        return APP_NO_ERROR;
    }
    else
    {
        WARN_LOG("AppHandle is NULL!\n");
        return APP_ERROR;
    }
}

EXPORT APP_STATUS appHandleUninit(AppHandle *appHandle)
{
    INFO_LOG("appHandle = 0x%p\n", appHandle);

    if (!appHandle)
    {
        WARN_LOG("AppHandle is NULL!\n");
        return APP_ERROR;
    }
    else
    {
        NotifyCb *notifyCb, *tmp;

        /* Lock */
        appHandleWriteLock(appHandle, __FUNCTION__);

        if (appHandle->xmlDir)
        {
            free(appHandle->xmlDir);
        }
        appHandle->xmlDir = NULL;

        if (appHandle->xmlCusDir)
        {
            free(appHandle->xmlCusDir);
        }
        appHandle->xmlCusDir = NULL;

        if (appHandle->featureOptionsDoc)
        {
            xmlFreeDoc(appHandle->featureOptionsDoc);
        }
        appHandle->featureOptionsDoc = NULL;

#ifndef WIN32
        if (appHandle->appThreadExit == 0)
        {
            void *status;
            appHandle->appThreadExit = 1;
            INFO_LOG("Send signal to appThread\n");
            pthread_kill(appHandle->appThread, SIGUSR1);

            /* TODO: Don't join the inotify thread, since the read function is block waiting */
            INFO_LOG("Waiting inotify thread join...\n");
            pthread_join(appHandle->appThread, &status);
            INFO_LOG("inotify thread joined\n");
            appHandle->inotifyFd = 0;
        }
#endif

        // release notify callback list
        LL_FOREACH_SAFE(appHandle->noficyCbList, notifyCb, tmp)
        {
            LL_DELETE(appHandle->noficyCbList, notifyCb);
            free(notifyCb);
        }
        appHandle->noficyCbList = NULL;

        /* If appHandle is singleton instance, reset the init info */
        if (appHandle == &appHandleInst)
        {
            appHandleInited = 0;
        }

        appHandleReleaseAudioTypeHash(appHandle);

        appHandleReleaseFeatureOptionsHash(appHandle);

        xmlCleanupParser();

        /* Flush app log */
        if (appLogFp)
        {
            fflush(appLogFp);
        }

        /* Unlock */
        appHandleUnlock(appHandle);

        return APP_NO_ERROR;
    }
}

EXPORT const char *appHandleGetBuildTimeStamp()
{
    return __DATE__" "__TIME__;
}

EXPORT int appHandleWriteLock(AppHandle *appHandle, const char *callerFun)
{
    int res = 0;

    if (!appHandle)
    {
        WARN_LOG("appHandle is NULL\n");
        return res;
    }

#ifndef WIN32
    while (1)
    {
        if (pthread_rwlock_trywrlock(&appHandle->lock) == 0)
        {
            appHandle->lockCallerFun = callerFun;
            DEBUG_LOG("AppHandle lock is locked by %s()\n", appHandle->lockCallerFun);
            break;
        }
        else
        {
            DEBUG_LOG("Cannot lock the AppHandle lock, delay some time. (the locker is %s())\n", appHandle->lockCallerFun);
            utilUsleep(1);
        }
    }
#else
    //DEBUG_LOG("Not support this function yet!\n");
#endif
    return res;
}

EXPORT int appHandleReadLock(AppHandle *appHandle, const char *callerFun)
{
    int res = 0;

    if (!appHandle)
    {
        WARN_LOG("appHandle is NULL\n");
        return res;
    }

#ifndef WIN32
    while (1)
    {
        if (pthread_rwlock_tryrdlock(&appHandle->lock) == 0)
        {
            appHandle->lockCallerFun = callerFun;
            DEBUG_LOG("AppHandle lock is locked by %s()\n", appHandle->lockCallerFun);
            break;
        }
        else
        {
            DEBUG_LOG("Cannot lock the AppHandle lock, delay some time. (the locker is %s())\n", appHandle->lockCallerFun);
            utilUsleep(1);
        }
    }
#else
    //DEBUG_LOG("Not support this function yet!\n");
#endif
    return res;
}

EXPORT int appHandleUnlock(AppHandle *appHandle)
{
    int res = 0;

    if (!appHandle)
    {
        WARN_LOG("appHandle is NULL\n");
        return res;
    }

#ifndef WIN32
    DEBUG_LOG("Unlock appHandle lock\n");
    res = pthread_rwlock_unlock(&appHandle->lock);
#endif
    return res;
}

EXPORT int appHandleInstWriteLock(const char *callerFun)
{
    int res = 0;

#ifndef WIN32
    while (1)
    {
        if (pthread_rwlock_trywrlock(&appHandleInstLock) == 0)
        {
            appHandleInstLockCallerFun = callerFun;
            DEBUG_LOG("%s acquired the appHandleInstLock\n", callerFun);
            break;
        }
        else
        {
            DEBUG_LOG("Cannot lock the appHandleInstLock, delay some time. (the locker is %s)\n", callerFun);
            utilUsleep(1);
        }
    }
#else
    //DEBUG_LOG("Not support this function yet!\n");
#endif
    return res;
}

EXPORT int appHandleInstUnlock()
{
    int res = 0;
#ifndef WIN32
    DEBUG_LOG("Unlock appHandleInst lock\n");
    res = pthread_rwlock_unlock(&appHandleInstLock);
#endif
    return res;
}

EXPORT FeatureOption *featureOptionCreate(const char *name, const char *value)
{
    FeatureOption *featureOption = malloc(sizeof(FeatureOption));
    featureOption->name = strdup(name);
    featureOption->value = strdup(value);
    return featureOption;
}

EXPORT void featureOptionRelease(FeatureOption *featureOption)
{
    free(featureOption->name);
    free(featureOption->value);
    free(featureOption);
}

EXPORT void appHandleReleaseFeatureOptionsHash(AppHandle *appHandle)
{
    if (appHandle->featureOptionsHash)
    {
        FeatureOption *tmp, *item;
        HASH_ITER(hh, appHandle->featureOptionsHash, item, tmp)
        {
            HASH_DEL(appHandle->featureOptionsHash, item);
            featureOptionRelease(item);
        }
    }
    appHandle->featureOptionsHash = NULL;
}

EXPORT AppHandle *appHandleGetInstance()
{
    appHandleInstWriteLock(__FUNCTION__);

    INFO_LOG("");

    if (!appHandleInited)
    {
        appHandleInit(&appHandleInst);
#ifdef WIN32
        appHandleParseXml(&appHandleInst, XML_FOLDER_ON_TUNING_TOOL, XML_CUS_FOLDER_ON_TUNING_TOOL);
#else
        appHandleParseXml(&appHandleInst, XML_FOLDER_ON_DEVICE, XML_CUS_FOLDER_ON_DEVICE);
#endif
        appHandleInited = 1;
    }

    appHandleInstUnlock();

    return &appHandleInst;
}

EXPORT APP_STATUS appHandleParseXml(AppHandle *appHandle, const char *dir, const char *cusDir)
{
    INFO_LOG("appHandle = 0x%p, dir = %s, cusDir = %s\n", appHandle, dir, cusDir);

    if (!appHandle)
    {
        ERR_LOG("appHandle is NULL!\n");
        return APP_ERROR;
    }

    if (!dir)
    {
        ERR_LOG("dir is NULL\n");
        return APP_ERROR;
    }

    if (appHandle->xmlDir || appHandle->xmlCusDir)
    {
        ERR_LOG("XML already parsed, don't call the appHandleParseXml twice!\n");
        return APP_ERROR;
    }

    appHandleWriteLock(appHandle, __FUNCTION__);

    appHandle->xmlDir = strdup(dir);
#if defined(APP_FORCE_ENABLE_CUS_XML) || defined(WIN32) || defined(CONFIG_MT_ENG_BUILD)
    appHandle->xmlCusDir = strdup(cusDir);
#else
#if !defined(WIN32)
    if (isCustXmlEnable())
    {
        appHandle->xmlCusDir = strdup(cusDir);
    }
    else
    {
        INFO_LOG("Cust XML folder not enabled");
    }
#endif
#endif
    INFO_LOG("XmlDir = %s, XmlCusDir = %s\n", appHandle->xmlDir, appHandle->xmlCusDir);

    /* Load feature options information */
    appHandleLoadDirFeatureOptionsInfo(appHandle);

    /* Load audio type information */
    appHandleLoadDirAudioTypeInfo(appHandle);

    appHandleUnlock(appHandle);

#ifndef WIN32
    /* Setup file system monitor thread */
    if (appHandle->xmlCusDir)
    {
        if (pthread_create(&appHandle->appThread, NULL, appHandleThreadLoop, (void *)appHandle))
        {
            ERR_LOG("Create app thread fail!\n");
            return APP_ERROR;
        }
        else
        {
            INFO_LOG("Create app thread successfully\n");
        }
    }
    else
    {
        WARN_LOG("Cus folder is NULL, don't create FS monitor thread\n");
    }
#endif
    return APP_NO_ERROR;
}

EXPORT APP_STATUS appHandleLoadDirFeatureOptionsInfo(AppHandle *appHandle)
{
    struct stat fileStat;
    int strLen;
    char *featureOptionsFile = NULL;
    xmlNode *node = NULL;
    xmlNode *root = NULL;
    xmlChar *name = NULL;
    xmlChar *value = NULL;

    if (!appHandle)
    {
        ERR_LOG("appHandle is NULL!\n");
        return APP_ERROR;
    }

    if (!appHandle->xmlDir)
    {
        ERR_LOG("xmlDir is NULL!\n");
        return APP_ERROR;
    }

    if (appHandle->featureOptionsHash)
    {
        WARN_LOG("Feature options already loaded, don't reload it!\n");
        return APP_NO_ERROR;
    }

    /* Check cus folder xml first */
    strLen = strlen(appHandle->xmlDir) + strlen(FEATURE_OPTIONS_XML) + 2;
    featureOptionsFile = (char *)malloc(strLen);
    sprintf(featureOptionsFile, "%s%s%s", appHandle->xmlDir, FOLDER, FEATURE_OPTIONS_XML);

    if (stat(featureOptionsFile, &fileStat) == -1)
    {
        ERR_LOG("No %s file\n", featureOptionsFile);
        free(featureOptionsFile);
        return APP_ERROR;
    }

    appHandle->featureOptionsDoc = xmlParseFile(featureOptionsFile);
    if (appHandle->featureOptionsDoc == NULL)
    {
        ERR_LOG("Failed to parse %s\n", featureOptionsFile);
        free(featureOptionsFile);
        return APP_ERROR;
    }
    else
    {
        INFO_LOG("Load xml file successfully. (%s)\n", featureOptionsFile);
    }
    free(featureOptionsFile);

    /* Parse informatino to feature options hash */
    root = xmlDocGetRootElement(appHandle->featureOptionsDoc);
    if (!root)
    {
        ERR_LOG("Root element is NULL\n");
        return APP_ERROR;
    }

    node = findXmlNodeByElemName(root, ELEM_AUDIO_FEATURE_OPTIONS);
    if (node && node->children)
    {
        node = node->children;
    }
    else
    {
        ERR_LOG("No feature options found!\n");
        return APP_ERROR;
    }

    while ((node = findXmlNodeByElemName(node->next, ELEM_PARAM)))
    {
        FeatureOption *featureOption;
        name = xmlGetProp(node, (const xmlChar *)ATTRI_NAME);
        value = xmlGetProp(node, (const xmlChar *)ATTRI_VALUE);

        featureOption = featureOptionCreate((const char *)name, (const char *)value);
        HASH_ADD_KEYPTR(hh, appHandle->featureOptionsHash, featureOption->name, strlen(featureOption->name), featureOption);

        if (name)
        {
            xmlFree(name);
        }

        if (value)
        {
            xmlFree(value);
        }
    }

    return APP_NO_ERROR;
}

EXPORT APP_STATUS appHandleLoadDirAudioTypeInfo(AppHandle *appHandle)
{
    size_t numOfAudioParamXml = 0;
    char audioType[MAX_AUDIO_TYPE_LEN];

#ifdef __linux__
    struct dirent **namelist;
    int i;
    int total;

    if (!appHandle)
    {
        ERR_LOG("appHandle is NULL!\n");
        return APP_ERROR;
    }

    total = scandir(appHandle->xmlDir, &namelist, 0, alphasort);

    /* Release old audio type first */
    appHandleReleaseAudioTypeHash(appHandle);

    if (total < 0)
    {
        ERR_LOG("Scandir error\n");
    }
    else
    {
        for (i = 0; i < total; i++)
        {
            if (strstr(namelist[i]->d_name, AUDIO_PARAM_XML_POSFIX) == NULL)
            {
                DEBUG_LOG("File name's posfix is not AudioParam.xml (%s)\n", namelist[i]->d_name);
                continue;
            }

            sscanf(namelist[i]->d_name, AUDIO_TYPE_FMT_STR(MAX_AUDIO_TYPE_LEN), audioType);
            if (appHandleIsValidAudioType(appHandle, audioType))
            {
                appHandleAddAudioType(appHandle, audioType);
            }
            else
            {
                WARN_LOG("Invalid audio param xml = %s\n", namelist[i]->d_name);
            }
        }
    }
#else
    WIN32_FIND_DATA FindFileData;
    HANDLE hFind;
    UT_string *path = NULL;

    if (!appHandle)
    {
        ERR_LOG("appHandle is NULL!\n");
        return APP_ERROR;
    }

    /* Release old audio type first */
    appHandleReleaseAudioTypeHash(appHandle);

    /* Check preload xml folder */
    utstring_new(path);
    utstring_printf(path, "%s"FOLDER"*"AUDIO_PARAM_XML_POSFIX, appHandle->xmlDir);
    hFind = FindFirstFile(utstring_body(path), &FindFileData);
    utstring_free(path);

    if (hFind == INVALID_HANDLE_VALUE)
    {
        WARN_LOG("No xml found!\n");
        return APP_ERROR;
    }

    do
    {
        sscanf(FindFileData.cFileName, AUDIO_TYPE_FMT_STR(MAX_AUDIO_TYPE_LEN), audioType);

        if (appHandleIsValidAudioType(appHandle, audioType))
        {
            appHandleAddAudioType(appHandle, audioType);
        }
        else
        {
            INFO_LOG("Invalid audio param xml = %s\n", FindFileData.cFileName);
        }
    }
    while (FindNextFile(hFind, &FindFileData));
#endif

    /* Load all XMLs */
    appHandleLoadAllAudioTypeXml(appHandle);
    INFO_LOG("Load all audio type XML - ok\n");

    /* Modify data depends on feature options */
    appHandleReviseXmlDocByFeatureOptions(appHandle);

    /* Load hash info from XML */
    appHandleLoadAllAudioTypeHash(appHandle);
    INFO_LOG("Load all audio Hash - ok\n");

    if (appDebugLevel == DEBUG_LEVEL)
    {
        appHandleDumpAudioTypeList(appHandle);
    }

    return APP_NO_ERROR;
}

EXPORT size_t appHandleGetNumOfAudioType(AppHandle *appHandle)
{
    if (!appHandle)
    {
        ERR_LOG("appHandle is NULL!\n");
        return APP_ERROR;
    }

    return HASH_COUNT(appHandle->audioTypeHash);
}

EXPORT APP_STATUS appHandleLoadAllAudioTypeXml(AppHandle *appHandle)
{
    size_t i;
    size_t count = appHandleGetNumOfAudioType(appHandle);

    for (i = 0; i < count; i++)
    {
        AudioType *audioType = appHandleGetAudioTypeByIndex(appHandle, i);

        /* Load xml struct */
        if (appHandleLoadAudioTypeXml(appHandle, audioType) == APP_ERROR)
        {
            WARN_LOG("Load audio type XML failed. (%s)\n", audioType->name);
        }
    }

    return APP_NO_ERROR;
}

EXPORT APP_STATUS appHandleLoadAudioTypeXml(AppHandle *appHandle, AudioType *audioType)
{
    char *audioTypeFile;

    INFO_LOG("audioType = %s\n", audioType->name);

    // Load AudioParamXml
    audioTypeFile = appHandleGetAudioTypeFilePath(appHandle, audioType->name, AUDIO_PARAM_XML_POSFIX);
    if (audioTypeFile == NULL)
    {
        WARN_LOG("The AudioTypeFile(%s%s) doesn't exist.\n", audioType->name, AUDIO_PARAM_XML_POSFIX);
        return APP_ERROR;
    }

    audioType->audioParamDoc = xmlParseFile(audioTypeFile);

    if (audioType->audioParamDoc == NULL)
    {
        ERR_LOG("Failed to parse %s\n", audioTypeFile);
        free(audioTypeFile);

        // Audio param file broken, load preload xml file instead
        audioTypeFile = appHandleGetPreloadAudioTypeFilePath(appHandle, audioType->name, AUDIO_PARAM_XML_POSFIX);
        if (audioTypeFile == NULL)
        {
            WARN_LOG("The AudioTypeFile(%s%s) doesn't exist.\n", audioType->name, AUDIO_PARAM_XML_POSFIX);
            return APP_ERROR;
        }

        WARN_LOG("Trying to load preload %s file instead of broken XML file!\n", audioTypeFile);
        audioType->audioParamDoc = xmlParseFile(audioTypeFile);
        if (audioType->audioParamDoc == NULL)
        {
            ERR_LOG("Failed to parse %s\n", audioTypeFile);
            free(audioTypeFile);
            return APP_ERROR;
        }
        else
        {
            INFO_LOG("Load xml file successfully. (%s)\n", audioTypeFile);
        }
    }
    else
    {
        INFO_LOG("Load xml file successfully. (%s)\n", audioTypeFile);
    }

    free(audioTypeFile);

    // Load ParamUnitDescXml
    audioTypeFile = appHandleGetAudioTypeFilePath(appHandle, audioType->name, PARAM_UNIT_DESC_XML_POSFIX);
    if (audioTypeFile == NULL)
    {
        WARN_LOG("The AudioTypeFile(%s%s) doesn't exist.\n", audioType->name, PARAM_UNIT_DESC_XML_POSFIX);
        return APP_ERROR;
    }

    audioType->paramUnitDescDoc = xmlParseFile(audioTypeFile);
    if (audioType->paramUnitDescDoc == NULL)
    {
        ERR_LOG("Failed to parse %s%s\n", audioTypeFile, PARAM_UNIT_DESC_XML_POSFIX);
        free(audioTypeFile);
        return APP_ERROR;
    }
    else
    {
        INFO_LOG("Load xml file successfully. (%s)\n", audioTypeFile);
    }
    free(audioTypeFile);

#ifdef WIN32
    // Load ParamTreeViewXml only for tuning tool
    audioTypeFile = appHandleGetAudioTypeFilePath(appHandle, audioType->name, PARAM_TREE_VIEW_XML_POSFIX);
    if (audioTypeFile == NULL)
    {
        INFO_LOG("The AudioTypeFile(%s%s) doesn't exist.\n", audioType->name, PARAM_TREE_VIEW_XML_POSFIX);
        free(audioTypeFile);
    }
    else
    {
        audioType->paramTreeViewDoc = xmlParseFile(audioTypeFile);
        if (audioType->audioParamDoc == NULL)
        {
            DEBUG_LOG("Failed to parse %s%s\n", audioTypeFile, PARAM_TREE_VIEW_XML_POSFIX);
        }
        else
        {
            INFO_LOG("Load xml file successfully. (%s)\n", audioTypeFile);
        }
        free(audioTypeFile);
    }
#endif

    /* Get tab name info */
    audioTypeParseTabName(audioType);

    /* Get version info */
    if (audioTypeParseXmlVer(audioType) == APP_ERROR)
    {
        ERR_LOG("Cannot parse xml version info. (%s)\n", audioType->name);
        return APP_ERROR;
    }

#ifndef WIN32
    /* XML Version check for device driver or HAL */
    if (!audioTypeIsDeviceSupportedXmlVer(audioType))
    {
        abort();
    }
#endif

    return APP_NO_ERROR;
}

EXPORT char *appHandleGetAudioTypeFilePath(AppHandle *appHandle, const char *audioType, const char *posfix)
{
    /* Check cus folder xml first */
    struct stat fileStat;
    int strLen;
    char *path;

    if (appHandle->xmlCusDir && !strcmp(posfix, AUDIO_PARAM_XML_POSFIX))
    {
        strLen = strlen(appHandle->xmlCusDir) + strlen(audioType) + strlen(posfix) + 2;
        path = (char *)malloc(strLen);
        sprintf(path, "%s%s%s%s", appHandle->xmlCusDir, FOLDER, audioType, posfix);

        if (stat(path, &fileStat) != -1)
        {
            return path;
        }
        else
        {
            free(path);
        }
    }

    /* Check default folder */
    strLen = strlen(appHandle->xmlDir) + strlen(audioType) + strlen(posfix) + 2;
    path = (char *)malloc(strLen);
    sprintf(path, "%s%s%s%s", appHandle->xmlDir, FOLDER, audioType, posfix);

    if (stat(path, &fileStat) != -1)
    {
        return path;
    }

    free(path);
    return NULL;
}

EXPORT char *appHandleGetPreloadAudioTypeFilePath(AppHandle *appHandle, const char *audioType, const char *posfix)
{
    /* Check cus folder xml first */
    struct stat fileStat;
    int strLen;
    char *path;

    /* Check default folder */
    strLen = strlen(appHandle->xmlDir) + strlen(audioType) + strlen(posfix) + 2;
    path = (char *)malloc(strLen);
    sprintf(path, "%s%s%s%s", appHandle->xmlDir, FOLDER, audioType, posfix);

    if (stat(path, &fileStat) != -1)
    {
        return path;
    }

    free(path);
    return NULL;
}

EXPORT int appHandleIsValidAudioType(AppHandle *appHandle, const char *audioType)
{
    char *filePath;

    assert(appHandle != NULL);
    filePath = appHandleGetAudioTypeFilePath(appHandle, audioType, PARAM_UNIT_DESC_XML_POSFIX);
    if (filePath == NULL)
    {
        ERR_LOG("%s audio type is not valid! (%s is not exist)\n", audioType, filePath);
        free(filePath);
        return 0;
    }

    free(filePath);
    return 1;
}

EXPORT AudioType *appHandleAddAudioType(AppHandle *appHandle, const char *audioTypeName)
{
    AudioType *audioType;

    if (!appHandle)
    {
        ERR_LOG("The appHandle is NULL\n");
        return NULL;
    }

    if (!audioTypeName)
    {
        ERR_LOG("The audioTypeName is NULL\n");
        return NULL;
    }

    audioType = audioTypeCreate(appHandle, audioTypeName);

    /* Add audio type to hash */
    HASH_ADD_KEYPTR(hh, appHandle->audioTypeHash, audioType->name, strlen(audioType->name), audioType);

    return audioType;
}

EXPORT AudioType *appHandleGetAudioTypeByIndex(AppHandle *appHandle, size_t index)
{
    AudioType *audioType = NULL;
    size_t i = 0;

    DEBUG_LOG("appHandle = 0x%p, index = %lu\n", appHandle, index);

    if (!appHandle)
    {
        ERR_LOG("appHandle is NULL!\n");
        return NULL;
    }

    for (audioType = appHandle->audioTypeHash; audioType ; audioType = audioType->hh.next)
    {
        if (index == i++)
        {
            return audioType;
        }
    }

    return NULL;
}

EXPORT AudioType *appHandleGetAudioTypeByName(AppHandle *appHandle, const char *name)
{
    AudioType *audioType = NULL;

    INFO_LOG("appHandle = 0x%p, name = %s\n", appHandle, name);

    if (!appHandle)
    {
        ERR_LOG("appHandle is NULL!\n");
        return NULL;
    }

    HASH_FIND_STR(appHandle->audioTypeHash, name, audioType);

    return audioType;
}

EXPORT void appHandleReleaseAudioTypeHash(AppHandle *appHandle)
{
    if (appHandle->audioTypeHash)
    {
        AudioType *tmp, *item;
        HASH_ITER(hh, appHandle->audioTypeHash, item, tmp)
        {
            HASH_DEL(appHandle->audioTypeHash, item);
            audioTypeRelease(item);
        }
    }
    appHandle->audioTypeHash = NULL;
}

EXPORT void appHandleDumpAudioTypeList(AppHandle *appHandle)
{
    size_t index = 0;
    size_t numOfAudioType = appHandleGetNumOfAudioType(appHandle);
    INFO_LOG("=================================\n");
    INFO_LOG("Totoal num of Audio Type List = %lu\n", numOfAudioType);
    for (index = 0; index < numOfAudioType; index++)
    {
        AudioType *audioType = appHandleGetAudioTypeByIndex(appHandle, index);
        INFO_LOG("AudioType[%lu] = %s\n", index, audioType->name);
        audioTypeDump(audioType);
    }
}

EXPORT APP_STATUS appHandleLoadAllAudioTypeHash(AppHandle *appHandle)
{
    size_t index = 0;
    size_t numOfAudioType = appHandleGetNumOfAudioType(appHandle);
    /* Load stage1 information */
    for (index = 0; index < numOfAudioType; index++)
    {
        AudioType *audioType = appHandleGetAudioTypeByIndex(appHandle, index);
        audioTypeLoadStage1Hash(audioType);
    }

    /* Load stage2 information (ex: ParamTreeView's switch object)*/
    for (index = 0; index < numOfAudioType; index++)
    {
        AudioType *audioType = appHandleGetAudioTypeByIndex(appHandle, index);
        audioTypeLoadStage2Hash(audioType);
    }

    return APP_NO_ERROR;
}

EXPORT void appHandleRegXmlChangedCb(AppHandle *appHandle, NOTIFY_CB_FUN callbackFun)
{
    INFO_LOG("appHandle = 0x%p, callbackFun = 0x%p\n", appHandle, callbackFun);

    appHandleWriteLock(appHandle, __FUNCTION__);

    if (appHandle && callbackFun)
    {
        /* Checking the duplicated callback function registration */
        NotifyCb *notifyCb;
        LL_FOREACH(appHandle->noficyCbList, notifyCb)
        {
            if (notifyCb->cb == callbackFun)
            {
                INFO_LOG("Same callback function found. ignore it\n");
                appHandleUnlock(appHandle);
                return;
            }
        }

        notifyCb = malloc(sizeof(NotifyCb));
        notifyCb->cb = callbackFun;
        LL_APPEND(appHandle->noficyCbList, notifyCb);
    }
    else
    {
        WARN_LOG("Cannot register xml callback! (AppHandle = 0x%p, callbackFun = 0x%p)\n", appHandle, callbackFun);
    }

    appHandleUnlock(appHandle);
}

EXPORT void appHandleUnregXmlChangedCb(AppHandle *appHandle, NOTIFY_CB_FUN callbackFun)
{
    INFO_LOG("appHandle = 0x%p, callbackFun = 0x%p\n", appHandle, callbackFun);

    appHandleWriteLock(appHandle, __FUNCTION__);

    if (appHandle && callbackFun)
    {
        NotifyCb *notifyCb, *tmp;
        LL_FOREACH_SAFE(appHandle->noficyCbList, notifyCb, tmp)
        {
            if (notifyCb->cb == callbackFun)
            {
                LL_DELETE(appHandle->noficyCbList, notifyCb);
                free(notifyCb);
                INFO_LOG("Callback function unregistered. (0x%p, 0x%p)\n", callbackFun, callbackFun);
                break;
            }
        }
    }
    else
    {
        WARN_LOG("Cannot unregister xml callback! (AppHandle = 0x%p, callbackFun = %p)\n", appHandle, callbackFun);
    }

    appHandleUnlock(appHandle);
}

EXPORT void *appHandleThreadLoop(void *arg)
{
#if defined(WIN32)
    /* Always disable on WIN32 */
    return NULL;
#else
    /* This thread only work on linux platform */
    /* Only eng load could monitor custom folder */
    AppHandle *appHandle = (AppHandle *)arg;
    ssize_t len;
    char buf[INOTIFY_BUF_SIZE];
    char *ptr;
    const struct inotify_event *event;

#if !defined(APP_FORCE_ENABLE_CUS_XML) && !defined(CONFIG_MT_ENG_BUILD)
    /* User load, check NVRam status */
    if (isCustXmlEnable())
    {
        INFO_LOG("User load, cust xml enabled\n");
    }
    else
    {
        INFO_LOG("User load but cust xml disabled\n");
        return NULL;
    }
#endif

    if (!appHandle->xmlCusDir)
    {
        WARN_LOG("xmlCusDir is NULL, don't run the appHandleThreadLoop !!!");
        exit(1);
    }

    /* Create folder first to make inotify work */
    utilMkdir(appHandle->xmlCusDir);

    /* Register signal handler */
    struct sigaction sa;
    sa.sa_handler = NULL;
    sa.sa_sigaction = &signalHandler;
    sa.sa_flags = SA_SIGINFO;
    sigemptyset(&sa.sa_mask);

    if (sigaction(SIGUSR1, &sa, NULL) < 0)
    {
        ERR_LOG("sigaction fail");
        exit(1);
    }

    /* inotify registration */
    appHandle->inotifyFd = inotify_init();
    if (appHandle->inotifyFd < 0)
    {
        ERR_LOG("inotify_init failed !!!");
        exit(1);
    }

    INFO_LOG("Add inotify monitor path = %s, fd = %d\n", appHandle->xmlCusDir, appHandle->inotifyFd);

    while (1)
    {
        if (inotify_add_watch(appHandle->inotifyFd, appHandle->xmlCusDir, IN_CLOSE_WRITE) < 0)
        {
            ERR_LOG("inotify_add_watch failed !!! try again...");
            utilMkdir(appHandle->xmlCusDir);
            utilUsleep(1000000);
        } else {
            break;
        }
    }

    while (!appHandle->appThreadExit)
    {
        INFO_LOG("inotify read waiting... (fd = %d)\n", appHandle->inotifyFd);
        len = read(appHandle->inotifyFd, buf, sizeof(buf));

        if (len < 0)
        {
            if (appHandle->appThreadExit)
            {
                break;
            }

            ERR_LOG("inotify read error!\n");
            pthread_exit(NULL);
        }

        /* Loop over all events in the buffer */
        for (ptr = buf; ptr < buf + len; ptr += sizeof(struct inotify_event) + event->len)
        {
            event = (const struct inotify_event *) ptr;
            if (event->len)
            {
                NotifyCb *notifyCb;
                char audioTypeName[MAX_AUDIO_TYPE_LEN];
                AudioType *audioType;

                if (strstr(event->name, AUDIO_PARAM_XML_POSFIX) == NULL)
                {
                    INFO_LOG("File name's posfix is not AudioParam.xml (%s)\n", event->name);
                    continue;
                }

                sscanf(event->name, AUDIO_TYPE_FMT_STR(MAX_AUDIO_TYPE_LEN), audioTypeName);
                INFO_LOG("XML File chanegd (%s)\n", event->name);

                audioType = appHandleGetAudioTypeByName(appHandle, audioTypeName);
                if (audioType)
                {
                    audioType->allowReload = 1;
                }

                /* notify users */
                INFO_LOG("Notify all callback function.\n");
                LL_FOREACH(appHandle->noficyCbList, notifyCb)
                {
                    INFO_LOG("Notify callback function. (0x%p, %pf)\n", notifyCb->cb, notifyCb->cb);
                    (*notifyCb->cb)(appHandle, audioTypeName);
                }
            }
        }
    }

    inotify_rm_watch(appHandle->inotifyFd, IN_CLOSE_NOWRITE);

    if (appHandle->inotifyFd)
    {
        INFO_LOG("close inotify handle %d\n", appHandle->inotifyFd);
        close(appHandle->inotifyFd);
    }

    INFO_LOG("appHandleThreadLoop exit\n");
    return NULL;
#endif
}

EXPORT APP_STATUS appHandleReloadAudioType(AppHandle *appHandle, const char *audioTypeName)
{
    /* Release old audioType */
    char *audioTypeFile;
    AudioType *audioType;

    INFO_LOG("appHandle = 0x%p, audioTypeName = %s\n", appHandle, audioTypeName);

    audioType = appHandleGetAudioTypeByName(appHandle, audioTypeName);
    if (!audioType)
    {
        ERR_LOG("Invalid AudioType name = %s\n", audioTypeName);
        return APP_ERROR;
    }

    /* Write lock */
    audioTypeWriteLock(audioType, __FUNCTION__);

    /* Checking if the audioType reloaded */
    if (!audioType->allowReload)
    {
        INFO_LOG("AudioType is already reloaded!\n");
        audioTypeUnlock(audioType);
        return APP_NO_ERROR;
    }

    /* Release audio param data */
    audioTypeReleaseAudioParam(audioType);
    /* Release audio param xml */
    if (audioType->audioParamDoc)
    {
        xmlFreeDoc(audioType->audioParamDoc);
    }

    /* Load AudioParam XML */
    audioTypeFile = appHandleGetAudioTypeFilePath(appHandle, audioType->name, AUDIO_PARAM_XML_POSFIX);
    if (audioTypeFile == NULL)
    {
        WARN_LOG("The AudioTypeFile(%s%s) doesn't exist.\n", audioType->name, AUDIO_PARAM_XML_POSFIX);
        audioTypeUnlock(audioType);
        return APP_ERROR;
    }

    audioType->audioParamDoc = xmlParseFile(audioTypeFile);
    if (audioType->audioParamDoc == NULL)
    {
        ERR_LOG("Failed to parse %s\n", audioTypeFile);
        free(audioTypeFile);
        audioTypeUnlock(audioType);
        return APP_ERROR;
    }
    else
    {
        INFO_LOG("Load xml file successfully. (%s)\n", audioTypeFile);
    }

    free(audioTypeFile);

    /* Load AudioParam hash */
    if (audioTypeLoadParamUnitHash(audioType) == APP_ERROR)
    {
        audioTypeUnlock(audioType);
        return APP_ERROR;
    }

    if (audioTypeLoadParamTreeHash(audioType) == APP_ERROR)
    {
        audioTypeUnlock(audioType);
        return APP_ERROR;
    }

    /* AudioType reloaded */
    audioType->allowReload = 0;

    audioTypeUnlock(audioType);
    return APP_NO_ERROR;
}

EXPORT const char *appHandleGetFeatureOptionValue(AppHandle *appHandle, const char *featureOptionName)
{
    FeatureOption *featureOption = NULL;

    if (!appHandle)
    {
        ERR_LOG("appHandle is NULL\n");
        return NULL;
    }

    if (!featureOptionName)
    {
        DEBUG_LOG("featureOptionName is NULL\n");
        return NULL;
    }

    HASH_FIND_STR(appHandle->featureOptionsHash, featureOptionName, featureOption);
    if (featureOption)
    {
        return featureOption->value;
    }

    return NULL;
}

EXPORT int appHandleIsFeatureOptionEnabled(AppHandle *appHandle, const char *featureOptionName)
{
    const char *featureOptionValueStr;
    if (!appHandle)
    {
        WARN_LOG("appHandle is NULL\n");
        return 0;
    }

    if (!featureOptionName)
    {
        WARN_LOG("featureOptionName is NULL\n");
        return 0;
    }

    featureOptionValueStr = appHandleGetFeatureOptionValue(appHandle, featureOptionName);
    if (featureOptionValueStr)
    {
        return !strcmp(featureOptionValueStr, "yes");
    }
    else
    {
        DEBUG_LOG("No %s such feature option\n", featureOptionName);
        return 0;
    }
}

EXPORT size_t appHandleGetNumOfFeatureOption(AppHandle *appHandle)
{
    if (!appHandle)
    {
        ERR_LOG("appHandle is NULL!\n");
        return APP_ERROR;
    }

    return HASH_COUNT(appHandle->featureOptionsHash);
}

EXPORT FeatureOption *appHandleGetFeatureOptionByIndex(AppHandle *appHandle, size_t index)
{
    FeatureOption *featureOption = NULL;
    size_t i = 0;

    if (!appHandle)
    {
        ERR_LOG("appHandle is NULL\n");
        return NULL;
    }

    for (featureOption = appHandle->featureOptionsHash; featureOption ; featureOption = featureOption->hh.next)
    {
        if (index == i++)
        {
            return featureOption;
        }
    }

    return NULL;
}

/* This function is only work for windows */
EXPORT void appHandleRedirectIOToConsole()
{
    INFO_LOG("");
#ifdef WIN32
    outputLogToStdout = 1;
    redirectIOToConsole();
#endif
}

int removeNodeByFeatureOption(AppHandle *appHandle, xmlNode *categoryNode)
{
    /* Process Category of CategoryTpe Node */
    xmlChar *featureOption = xmlNodeGetProp(categoryNode, ATTRI_FEATURE_OPTION);
    if (featureOption)
    {
        int not = 0;
        if (featureOption[0] == '!')
        {
            not = 1;
            featureOption++;
        }

        if (!(not ^ appHandleIsFeatureOptionEnabled(appHandle, (char *)featureOption)))
        {
            xmlNode *deleteNode = categoryNode;
            categoryNode = categoryNode->next;

            INFO_LOG("Remove %s category (%s feature option is disabled)\n", xmlNodeGetProp(deleteNode, ATTRI_NAME), featureOption);
            xmlUnlinkNode(deleteNode);
            xmlFreeNode(deleteNode);
            return 1;
        }
    }

    return 0;
}

EXPORT void appHandleReviseXmlDocByFeatureOptions(AppHandle *appHandle)
{
    // Travel all audioType's category & category group node
    size_t i;
    size_t numOfAppHandle = appHandleGetNumOfAudioType(appHandle);
    for (i = 0; i < numOfAppHandle; i++)
    {
        xmlNode *categoryTypeListNode, *categoryTypeNode, *categoryGroupNode, *categoryNode, *prevCategoryGroupNode, *prevCategoryNode, *prevCategoryTypeNode;
        AudioType *audioType = appHandleGetAudioTypeByIndex(appHandle, i);
        categoryTypeListNode = audioTypeGetCategoryTypeListNode(audioType);
        if (!categoryTypeListNode)
        {
            continue;
        }

        categoryTypeNode = categoryTypeListNode->children;
        while ((categoryTypeNode = findXmlNodeByElemName(categoryTypeNode->next, ELEM_CATEGORY_TYPE)))
        {
            prevCategoryTypeNode = categoryTypeNode->prev;
            if (removeNodeByFeatureOption(appHandle, categoryTypeNode))
            {
                categoryTypeNode = prevCategoryTypeNode;
                continue;
            }

            /* Process CategoryType node */
            categoryGroupNode = categoryTypeNode->children;
            while ((categoryGroupNode = findXmlNodeByElemName(categoryGroupNode->next, ELEM_CATEGORY_GROUP)))
            {
                /* Process CategoryGroup of CategoryType Node */
                prevCategoryGroupNode = categoryGroupNode->prev;
                if (removeNodeByFeatureOption(appHandle, categoryGroupNode))
                {
                    categoryGroupNode = prevCategoryGroupNode;
                    continue;
                }

                categoryNode = categoryGroupNode->children;
                while ((categoryNode = findXmlNodeByElemName(categoryNode->next, ELEM_CATEGORY)))
                {
                    /* Process Category of CategoryGroup Node */
                    prevCategoryNode = categoryNode->prev;
                    if (removeNodeByFeatureOption(appHandle, categoryNode))
                    {
                        categoryNode = prevCategoryNode;
                    }
                }
            }

            categoryNode = categoryTypeNode->children;
            while ((categoryNode = findXmlNodeByElemName(categoryNode->next, ELEM_CATEGORY)))
            {
                prevCategoryNode = categoryNode->prev;
                if (removeNodeByFeatureOption(appHandle, categoryNode))
                {
                    categoryNode = prevCategoryNode;
                }
            }
        }
    }
}

EXPORT APP_STATUS appHandleCompressFiles(const char* srcDir, const char* destFile)
{
#ifdef WIN32
    INFO_LOG("%s(), src = %s, dest = %s\n", __FUNCTION__, srcDir, destFile);
    if (!srcDir || !destFile)
    {
        ERR_LOG("%s(), srcDir or destFile is NULL\n", __FUNCTION__);
        return APP_ERROR;
    } else {
        UT_string *path = NULL;
        utstring_new(path);
        utstring_printf(path, "a -tzip %s %s\\*", destFile, srcDir);
        utilShellExecute("7za.exe", utstring_body(path));
        utstring_free(path);
    }
#else
    ERR_LOG("Not support on linux\n");
#endif
    return APP_NO_ERROR;
}

EXPORT APP_STATUS appHandleUncompressFile(const char* srcFile, const char* destDir)
{
#ifdef WIN32
    INFO_LOG("%s(), src = %s, dest = %s\n", __FUNCTION__, srcFile, destDir);
    if (!srcFile || !destDir)
    {
        ERR_LOG("%s(), srcFile or destDir is NULL\n", __FUNCTION__);
        return APP_ERROR;
    } else {
        UT_string *path = NULL;
        utstring_new(path);
        utstring_printf(path, "x %s -y -o%s\\", srcFile, destDir);
        utilShellExecute("7za.exe", utstring_body(path));
        utstring_free(path);
    }
#else
    ERR_LOG("Not support on linux\n");
#endif
    return APP_NO_ERROR;
}

EXPORT APP_STATUS appHandleGetAudioTypeSupportedVerInfo(const char* audioTypeName, int* paramUnitDescVerMaj, int* paramUnitDescVerMin, int* audioParamVerMaj, int* audioParamVerMin)
{
    int i = 0;
    while(audioTypeSupportVerInfo[i].audioTypeName != NULL)
    {
        if (!strcmp(audioTypeName, audioTypeSupportVerInfo[i].audioTypeName))
        {
            *paramUnitDescVerMaj = audioTypeSupportVerInfo[i].paramUnitDescVerMaj;
            *paramUnitDescVerMin = audioTypeSupportVerInfo[i].paramUnitDescVerMin;
            *audioParamVerMaj = audioTypeSupportVerInfo[i].audioParamVerMaj;
            *audioParamVerMin = audioTypeSupportVerInfo[i].audioParamVerMin;
            return APP_NO_ERROR;
        }
        i++;
    }

    ERR_LOG("%s AudioType version support info not found!\n", audioTypeName);
    return APP_ERROR;
}

EXPORT void appHandleShowAudioTypeSupportedVerInfo(AppHandle* appHandle)
{
    int i = 0;
    INFO_LOG("=======================\n");
    while(audioTypeSupportVerInfo[i].audioTypeName != NULL)
    {
        INFO_LOG("[%d] %s, ParamUnitDesc ver(%d.%d), AudioParam ver(%d.%d)\n",
            i,
            audioTypeSupportVerInfo[i].audioTypeName, audioTypeSupportVerInfo[i].paramUnitDescVerMaj,
            audioTypeSupportVerInfo[i].paramUnitDescVerMin, audioTypeSupportVerInfo[i].audioParamVerMaj, audioTypeSupportVerInfo[i].audioParamVerMin);
        i++;
    }
}
