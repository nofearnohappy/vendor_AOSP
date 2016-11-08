#include "AudioParamParser.h"
#include <math.h>

#ifdef WIN32
#ifdef __cplusplus
#define EXPORT extern "C" __declspec(dllexport)
#else
#define EXPORT __declspec(dllexport)
#endif
#include <process.h>
#include <Windows.h>
#include <direct.h>
#else   /* WIN32*/
#define EXPORT
#ifdef __cplusplus
extern "C" {
#endif
#include <errno.h>
#include <sys/stat.h>
#include <sys/inotify.h>
#include <unistd.h>
#include <utils/Timers.h>
#endif

#ifdef __linux__
#define FOLDER "/"
#else
#define FOLDER "\\"
#endif

extern FILE *appLogFp;
extern int outputLogToStdout;

#ifdef WIN32
#define ERR_LOG(format, ...) \
    if(appDebugLevel <= ERR_LEVEL) \
    utilLog("ERROR[%d,%d](): %s(), "format"\n^^^^\n", _getpid(), GetCurrentThreadId(), __FUNCTION__, __VA_ARGS__)

#define WARN_LOG(format, ...) \
    if(appDebugLevel <= WARN_LEVEL) \
    utilLog("WARNING[%d,%d]: %s(), "format, _getpid(), GetCurrentThreadId(), __FUNCTION__, __VA_ARGS__)

#define INFO_LOG(format, ...) \
    if(appDebugLevel <= INFO_LEVEL) \
    utilLog("INFO[%d,%d]: %s(), "format, _getpid(), GetCurrentThreadId(), __FUNCTION__, __VA_ARGS__)

#define DEBUG_LOG(format, ...) \
    if(appDebugLevel <= DEBUG_LEVEL) \
    utilLog("DEBUG[%d,%d]: %s(), "format, _getpid(), GetCurrentThreadId(), __FUNCTION__, __VA_ARGS__)

#else   /* WIN32 */
#define LOG_TAG "AudioParamParser"
#include <utils/Log.h>

#define ERR_LOG(format, args...) \
    if(appDebugLevel <= ERR_LEVEL) \
    ALOGE("%s(), "format, __FUNCTION__, ##args)

#define WARN_LOG(format, args...) \
    if(appDebugLevel <= WARN_LEVEL) \
    ALOGW("%s(), "format, __FUNCTION__, ##args)

#define INFO_LOG(format, args...) \
    if(appDebugLevel <= INFO_LEVEL) \
    ALOGI("%s(), "format, __FUNCTION__, ##args)

#define DEBUG_LOG(format, args...) \
    if(appDebugLevel <= DEBUG_LEVEL) \
    ALOGD("%s(), "format, __FUNCTION__, ##args)
#endif

/* Force adding following category group info and bypass categoryGroup path checking */
static const char *HARD_CATEGORY_GROUP[][3] =
{
/* {AudioTypeName, CategoryTypeName, CategoryGroupName} */
{"Speech", "Band", "NB"},
{"Speech", "Band", "WB"},
{NULL, NULL, NULL}
};

/***********************
 * Private APIs
 **********************/
/* appHandle API */
EXPORT APP_STATUS       appHandleLoadDirAudioTypeInfo(AppHandle *appHandle);
EXPORT APP_STATUS       appHandleLoadAllAudioTypeXml(AppHandle *appHandle);
EXPORT APP_STATUS       appHandleLoadAudioTypeXml(AppHandle *appHandle, AudioType *audioType);
EXPORT int              appHandleIsValidAudioType(AppHandle *appHandle, const char *audioType);
EXPORT AudioType       *appHandleAddAudioType(AppHandle *appHandle, const char *audioType);
EXPORT AudioType       *appHandleGetAudioType(AppHandle *appHandle, size_t index);
EXPORT void             appHandleReleaseAudioTypeHash(AppHandle *appHandle);
EXPORT APP_STATUS       appHandleLoadDirFeatureOptionsInfo(AppHandle *appHandle);
EXPORT void             appHandleReleaseFeatureOptionsHash(AppHandle *appHandle);
EXPORT void             appHandleDumpAudioTypeList(AppHandle *appHandle);
EXPORT char            *appHandleGetAudioTypeFilePath(AppHandle *appHandle, const char *audioType, const char *posfix);
EXPORT char            *appHandleGetPreloadAudioTypeFilePath(AppHandle *appHandle, const char *audioType, const char *posfix);
EXPORT APP_STATUS       appHandleLoadAllAudioTypeHash(AppHandle *appHandle);
EXPORT void            *appHandleThreadLoop(void *arg);
EXPORT int              appHandleWriteLock(AppHandle *appHandle, const char *callerFun);
EXPORT int              appHandleReadLock(AppHandle *appHandle, const char *callerFun);
EXPORT int              appHandleUnlock(AppHandle *appHandle);
EXPORT int              appHandleInstWriteLock(const char *callerFun);
EXPORT int              appHandleInstUnlock(void);
EXPORT void             appHandleReviseXmlDocByFeatureOptions(AppHandle *appHandle);
EXPORT APP_STATUS       appHandleGetAudioTypeSupportedVerInfo(const char* audioTypeName, int* paramUnitDescVerMaj, int* paramUnitDescVerMin, int* audioParamVerMaj, int* audioParamVerMin);
EXPORT void             appHandleShowAudioTypeSupportedVerInfo(AppHandle *appHandle);

/* AudioType API */
EXPORT AudioType       *audioTypeCreate(AppHandle *appHandle, const char *audioTypeName);
EXPORT void             audioTypeRelease(AudioType *audioType);
EXPORT void             audioTypeReleaseAudioParam(AudioType *audioType);
EXPORT void             audioTypeDump(AudioType *audioType);
EXPORT APP_STATUS       audioTypeParseTabName(AudioType *audioType);
EXPORT APP_STATUS       audioTypeLoadStage1Hash(AudioType *audioType);
EXPORT APP_STATUS       audioTypeLoadStage2Hash(AudioType *audioType);
EXPORT APP_STATUS       audioTypeLoadParamTreeHash(AudioType *audioType);
EXPORT APP_STATUS       audioTypeLoadParamTreeView(AudioType *audioType);
EXPORT APP_STATUS       audioTypeLoadParamUnitHash(AudioType *audioType);
EXPORT Param           *audioTypeGetParamHash(AudioType *audioType, xmlNode *paramUnitNode);
EXPORT APP_STATUS       audioTypeParseXmlVer(AudioType *audioType);
EXPORT APP_STATUS       audioTypeLoadParamFieldInfoHash(AudioType *audioType);
EXPORT APP_STATUS       audioTypeLoadCategoryTypeHash(AudioType *audioType);
EXPORT size_t           audioTypeGetNumOfParamTree(AudioType *audioType);
EXPORT APP_STATUS       audioTypeValidCategoryGroupName(AudioType *audioType, const char *name);
EXPORT int              audioTypeIsHardCategoryGroup(AudioType *audioType, const char *categoryName);

/* CategoryType API */
EXPORT CategoryType    *categoryTypeCreate(const char *name, const char *wording, AudioType *audioType, int visible);
EXPORT void             categoryTypeRelease(CategoryType *categoryType);
EXPORT CategoryAlias   *categoryTypeGetCategoryByAlias(CategoryType *categoryType, const char *alias);
EXPORT Category        *categoryTypeGetCategoryByName(CategoryType *categoryType, const char *name);

/* CategoryGroup API */
EXPORT CategoryGroup   *categoryGroupCreate(const char *categoryGroupName, const char *categoryGroupWording, CategoryType *categoryType, int visible);
EXPORT void             categoryGroupRelease(CategoryGroup *categoryGroup);

/* Category API */
EXPORT Category        *categoryCreate(const char *name, const char *wording, CATEGORY_PARENT_TYPE parentTypeIsCategoryType, void *parent, int visible);
EXPORT void             categoryRelease(Category *category);

/* ParamTree API */
EXPORT ParamTree       *paramTreeCreate(int paramId, const char *categoryPath);
EXPORT void             paramTreeRelease(ParamTree *paramTree);
EXPORT size_t           paramTreeGetNumOfParam(ParamTree *paramTree);

/* ParamUnit API */
EXPORT ParamUnit       *paramUnitCreate(AudioType *audioType, int id, Param *param);
EXPORT ParamUnit       *paramUnitClone(ParamUnit *paramUnit);
EXPORT void             paramUnitRelease(ParamUnit *paramUnit);

/* ParamInfo API */
EXPORT ParamInfo       *paramInfoCreate(const char *name, DATA_TYPE dataType, AudioType *audioType);
EXPORT void             paramInfoRelease(ParamInfo *paramInfo);

/* FieldInfo API */
EXPORT FieldInfo       *fieldInfoCreate(const char *fieldName, unsigned int arrayIndex, int startBit, int endBit, const char *checkList, ParamInfo *paramInfo);
EXPORT void             fieldInfoRelease(FieldInfo *paramInfo);

/* Param API */
EXPORT Param           *paramCreate(const char *paramName, ParamInfo *paramInfo, const char *paramValue);
EXPORT void             paramRelease(Param *param);
EXPORT APP_STATUS       paramSetupDataInfoByStr(Param *param, const char *str);
EXPORT APP_STATUS       paramSetupDataInfoByVal(Param *param, void *data, int arraySize);
EXPORT Param           *paramHashClone(Param *paramHash);

/* ParamTreeView API */
EXPORT ParamTreeView   *paramTreeViewCreate(AudioType *audioType, int verMaj, int verMin);
EXPORT void             paramTreeViewRelease(ParamTreeView *paramTreeView);
EXPORT TreeRoot        *treeRootCreate(const char *name, xmlNode *treeRootNode, ParamTreeView *paramTreeView);
EXPORT void             treeRootRelease(TreeRoot *treeRoot);
EXPORT Feature         *featureCreate(const char *name, AudioType *audioType, FieldInfo *switchFieldInfo, const char *featureOption);
EXPORT void             featureRelease(Feature *feature);
EXPORT CategoryPath    *categoryPathCreate(Feature *feature, const char *path);
EXPORT void             categoryPathRelease(CategoryPath *categoryPath);
EXPORT APP_STATUS       categoryPathValidation(CategoryPath *categoryPath);
EXPORT FeatureField    *featureFieldCreate(FieldInfo *fieldInfo);
EXPORT void             featureFieldRelease(FeatureField *featureField);

/* Feature Options API */
EXPORT FeatureOption   *featureOptionCreate(const char *name, const char *value);
EXPORT void             featureOptionRelease(FeatureOption *featureOption);

/* Utils API */
EXPORT char            *utilConvDataToString(DATA_TYPE dataType, void *data, int arraySize);
EXPORT UT_string       *utilNormalizeCategoryPathForAudioType(const char *categoryPath, AudioType *audioType);
EXPORT UT_string       *utilNormalizeCategoryGroupPathForAudioType(const char *categoryPath, AudioType *audioType);
EXPORT int              utilFindUnusedParamId(AudioType *audioType);
EXPORT void             utilUsleep(unsigned int usec);
EXPORT void             utilLog(char *str, ...);
EXPORT void             utilLogClose(void);
EXPORT FieldInfo       *utilXmlNodeGetFieldInfo(AppHandle *appHandle, xmlNode *node, const char *audioTypeAttrName, const char *paramAttrName, const char *fieldAttrName);
EXPORT void             appDumpXmlDoc(xmlDoc *doc);
EXPORT void             redirectIOToConsole(void);
EXPORT void             utilMkdir(const char *dir);
EXPORT void             utilShowParamValue(Param *param);
EXPORT char            *utilGenCheckList(int bits);
EXPORT int              utilCompNormalizeCategoryPath(AudioType *audioType, const char *srcCategoryPath, const char *dstCategoryPath);
EXPORT int              isCustXmlEnable(void);
EXPORT void             utilShellExecute(const char* prog, const char* params);
#ifndef WIN32
EXPORT void             signalHandler(int sig, siginfo_t *info, void *ucontext);
#endif

/* Unit Test */
EXPORT void             testDebugLevel(void);
EXPORT void             testHashParamTree(void);
EXPORT void             testHashParamUnit(void);
EXPORT void             testHashParam(void);
EXPORT APP_STATUS       testReadWriteParam(AppHandle *appHandle);
EXPORT APP_STATUS       testMemoryLeak(AppHandle *appHandle);
EXPORT APP_STATUS       testAudioTypeLock(AppHandle *appHandle);
EXPORT APP_STATUS       testAppHandleInitUninit(void);
EXPORT void             inotifyTest(const char *path);
EXPORT void             notifyCbTest(AppHandle *appHandle);

#ifndef WIN32
#ifdef __cplusplus
}
#endif
#endif
