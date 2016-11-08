#ifndef AUDIO_PARAM_PARSER_H
#define AUDIO_PARAM_PARSER_H

#include <libxml/parser.h>
#include <libxml/xmlreader.h>
#include <libxml/tree.h>

#ifdef WIN32
#pragma warning( disable : 4996 )
#ifdef __cplusplus
#define EXPORT extern "C" __declspec(dllexport)
#else
#define EXPORT __declspec(dllexport)
#endif
#else   /* WIN32*/
#define EXPORT
#ifdef __cplusplus
extern "C" {
#endif
#endif

#include "utstring.h"
#include "uthash.h"
#include "utlist.h"

#ifndef WIN32
#include <pthread.h>
#endif

/* Enable cus xml support */
//#define APP_FORCE_ENABLE_CUS_XML

/* Debugging Macro Definition */
//#define FORCE_DEBUG_LEVEL

#define XML_FOLDER_ON_TUNING_TOOL       ".\\preload_xml\\"
#define XML_CUS_FOLDER_ON_TUNING_TOOL   ".\\cus_xml\\"
#define XML_FOLDER_ON_DEVICE            "/system/etc/audio_param/"
#define XML_CUS_FOLDER_ON_DEVICE        "/sdcard/.audio_param/"

#define MAX_AUDIO_TYPE_LEN 50
#define INOTIFY_BUF_SIZE 512

#define AUDIO_PARAM_XML_POSFIX          "_AudioParam.xml"
#define PARAM_UNIT_DESC_XML_POSFIX      "_ParamUnitDesc.xml"
#define PARAM_TREE_VIEW_XML_POSFIX      "_ParamTreeView.xml"
#define FEATURE_OPTIONS_XML             "AudioParamOptions.xml"

/* XML element definition */
#define ELEM_AUDIO_FEATURE_OPTIONS      "AudioParamOptions"
#define ELEM_PARAM                  "Param"
#define ELEM_PARAM_UNIT_DESC            "ParamUnitDesc"
#define ELEM_CATEGORY_TYPE_LIST         "CategoryTypeList"
#define ELEM_CATEGORY_TYPE              "CategoryType"
#define ELEM_CATEGORY_GROUP             "CategoryGroup"
#define ELEM_CATEGORY                   "Category"

#define ELEM_AUDIO_PARAM                "AudioParam"
#define ELEM_PARAM_TREE                 "ParamTree"
#define ELEM_PARAM_UNIT_POOL            "ParamUnitPool"
#define ELEM_PARAM_UNIT                 "ParamUnit"
#define ELEM_PARAM                      "Param"
#define ELEM_FIELD                      "Field"

#define ELEM_PARAM_TREE_VIEW            "ParamTreeView"
#define ELEM_TREE_ROOT                  "TreeRoot"
#define ELEM_SHEET                      "Sheet"
#define ELEM_FEATURE                    "Feature"
#define ELEM_FIELD_LIST                 "FieldList"
#define ELEM_CATEGORY_PATH_LIST         "CategoryPathList"

/* XML attribute definition */
#define ATTRI_NAME                      "name"
#define ATTRI_TAB_NAME                  "tab_name"
#define ATTRI_VERSION                   "version"
#define ATTRI_WORDING                   "wording"
#define ATTRI_PARAM_ID                  "param_id"
#define ATTRI_PATH                      "path"
#define ATTRI_VALUE                     "value"
#define ATTRI_TYPE                      "type"
#define ATTRI_ARRAY_INDEX               "array_index"
#define ATTRI_BIT                       "bit"
#define ATTRI_CHECK_LIST                    "check_list"
#define ATTRI_ALIAS                     "alias"
#define ATTRI_FEATURE_OPTION                "feature_option"
#define ATTRI_SWITCH_AUDIO_TYPE         "switch_audio_type"
#define ATTRI_SWITCH_PARAM              "switch_param"
#define ATTRI_SWITCH_FIELD              "switch_field"
#define ATTRI_AUDIO_TYPE                    "audio_type"
#define ATTRI_PARAM                     "param"
#define ATTRI_VISIBLE                   "visible"

/* DATA_TYPE string */
#define DATA_TYPE_UNKNOWN_STRING        "unknown"
#define DATA_TYPE_STR_STRING            "string"
#define DATA_TYPE_INT_STRING            "int"
#define DATA_TYPE_UINT_STRING           "uint"
#define DATA_TYPE_FLOAT_STRING          "float"
#define DATA_TYPE_BYTE_ARRAY_STRING     "byte_array"
#define DATA_TYPE_UBYTE_ARRAY_STRING    "ubyte_array"
#define DATA_TYPE_SHORT_ARRAY_STRING    "short_array"
#define DATA_TYPE_USHORT_ARRAY_STRING   "ushort_array"
#define DATA_TYPE_INT_ARRAY_STRING      "int_array"
#define DATA_TYPE_UINT_ARRAY_STRING     "uint_array"
#define DATA_TYPE_DOUBLE_ARRAY_STRING   "double_array"
#define DATA_TYPE_FIELD_STRING          "Field"

#define ARRAY_SEPERATOR                 ","
#define ARRAY_SEPERATOR_CH              ','
#define PARAM_FIELD_NAME_SEPERATOR      "/"

#define AUDIO_TYPE_FMT_STR(STR_LEN) AUDIO_TYPE_FMT(STR_LEN)
#define AUDIO_TYPE_FMT(STR_LEN) "%"#STR_LEN"[^_]"

typedef struct _AppHandle               AppHandle;
typedef struct _AudioType               AudioType;
typedef struct _FieldInfo               FieldInfo;
typedef struct _Category                Category;
typedef struct _CategoryAlias           CategoryAlias;
typedef struct _CategoryGroup           CategoryGroup;
typedef struct _CategoryNameAlias       CategoryNameAlias;
typedef struct _CategoryPath            CategoryPath;
typedef struct _CategoryType            CategoryType;
typedef struct _Feature                 Feature;
typedef struct _FeatureField            FeatureField;
typedef struct _FeatureOption           FeatureOption;
typedef struct _Param                   Param;
typedef struct _ParamInfo               ParamInfo;
typedef struct _ParamTreeView           ParamTreeView;
typedef struct _ParamUnit               ParamUnit;
typedef struct _TreeRoot                TreeRoot;
typedef struct _NotifyCb                NotifyCb;

typedef void(*NOTIFY_CB_FUN)(AppHandle *appHandle, const char *audioType);

extern int appDebugLevel;

typedef enum
{
DEBUG_LEVEL = 0,
INFO_LEVEL,
WARN_LEVEL,
ERR_LEVEL,
} MSG_LEVEL;

typedef enum
{
    APP_ERROR = 0,
    APP_NO_ERROR = 1,
} APP_STATUS;

typedef enum
{
    PARENT_IS_CATEGORY_GROUP = 0,
    PARENT_IS_CATEGORY_TYPE = 1,
} CATEGORY_PARENT_TYPE;

/*
   Due to the system/media/camera/include/system/camera_metadata.h declare the same TYPE_FLOAT enum name,
   If module include the camera_metadata.h and AudioParamParser.h, AudioParamParser change the DATA_TYPE
   enum decleration to avoid conflict.
   User could using the APP_TYPE_FLOAT enum instead the TYPE_FLOAT.
*/
#ifndef SYSTEM_MEDIA_INCLUDE_ANDROID_CAMERA_METADATA_H
typedef enum
{
    TYPE_UNKNOWN = -1,
    TYPE_STR,
    TYPE_INT,
    TYPE_UINT,
    TYPE_FLOAT,
    TYPE_BYTE_ARRAY,
    TYPE_UBYTE_ARRAY,
    TYPE_SHORT_ARRAY,
    TYPE_USHORT_ARRAY,
    TYPE_INT_ARRAY,
    TYPE_UINT_ARRAY,
    TYPE_DOUBLE_ARRAY,
    TYPE_FIELD,
} DATA_TYPE;
#else
typedef enum
{
    APP_TYPE_UNKNOWN = -1,
    APP_TYPE_STR,
    APP_TYPE_INT,
    APP_TYPE_UINT,
    APP_TYPE_FLOAT,
    APP_TYPE_BYTE_ARRAY,
    APP_TYPE_UBYTE_ARRAY,
    APP_TYPE_SHORT_ARRAY,
    APP_TYPE_USHORT_ARRAY,
    APP_TYPE_INT_ARRAY,
    APP_TYPE_UINT_ARRAY,
    APP_TYPE_DOUBLE_ARRAY,
    APP_TYPE_FIELD,
} DATA_TYPE;
#endif

typedef union CategoryParent
{
    Category *category;                 /* Link to parent Category if it's not CategoryGroup */
    CategoryType *categoryType;         /* Link to parent CategoryType if it's CategoryGroup */
} CategoryParent;

/* UHash the parameter tree info from ParamTreeView.xml */
struct _CategoryPath
{
    char *path;
    Feature *feature;
    UT_hash_handle hh;
};

struct _FeatureField
{
    FieldInfo *fieldInfo;
    UT_hash_handle hh;
};

struct _Feature
{
    char *name;
    char *featureOption;
    FieldInfo *switchFieldInfo;
    CategoryPath *categoryPathHash;
    FeatureField *featureFieldHash;
    AudioType *audioType;
    UT_hash_handle hh;
};

struct _TreeRoot
{
    char *name;                 /* Key */
    FieldInfo *switchFieldInfo;
    xmlNode *treeRootNode;      /* Used to traversal tree */
    Feature *featureHash;       /* Used to opt feature information */
    ParamTreeView *paramTreeView;   /* Belong to which paramTreeView */
    UT_hash_handle hh;
};

struct _ParamTreeView
{
    int verMaj;
    int verMin;
    AudioType *audioType;
    TreeRoot *treeRootHash;
};

/* Hash the Param & Field info from ParamUnitDesc.xml */
struct _FieldInfo
{
    char *name;                         /* key */
    size_t arrayIndex;
    int startBit;
    int endBit;
    char *checkListStr;                 /* check list string array */
    struct _ParamInfo *paramInfo;       /* Link to parent ParamInfo */
    UT_hash_handle hh;                  /* hash handle */
};

struct _ParamInfo
{
    char *name;                         /* key */
    DATA_TYPE dataType;
    struct _FieldInfo *fieldInfoHash;
    AudioType *audioType;               /* Link to parent AudioType */
    UT_hash_handle hh;                  /* hash handle */
};

/* Hash the param name with value from AudioParam.xml */
struct _Param
{
    char *name;                         /* key */
    void *data;                         /* raw data */
    size_t arraySize;                       /* Array size if the data is the array pointer */
    ParamInfo *paramInfo;
    struct _ParamUnit *paramUnit;       /* Link to it's ParamUnit */
    UT_hash_handle hh;                  /* hash handle */
};

/* Hash the id with ParamUnit from AudioParam.xml */
struct _ParamUnit
{
    int paramId;                        /* key */
    int refCount;
    AudioType *audioType;               /* Link to it's AudioType */
    struct _Param *paramHash;           /* ParamUnit's params */
    UT_hash_handle hh;
};

/* Hash ParamTree info from AudioParam.xml */
typedef struct
{
    char *categoryPath;                 /* key */
    int paramId;                        /* Param id */
    UT_hash_handle hh;
} ParamTree;

struct _Category
{
    char *wording;                      /* key */
    char *name;
    int visible;
    CategoryParent parent;
    CATEGORY_PARENT_TYPE parentType;
    UT_hash_handle hh;
};

struct _CategoryAlias
{
    char *alias;                        /* key */
    Category *category;
    UT_hash_handle hh;
};

struct _CategoryGroup
{
    char *wording;                      /* key */
    char *name;
    int visible;
    Category *categoryHash;             /* Link to children */
    CategoryType *categoryType;         /* Link to parent */
    UT_hash_handle hh;
};

struct _CategoryType
{
    char *wording;                      /* key */
    char *name;
    int visible;
    CategoryGroup *categoryGroupHash;   /* Link to children */
    Category *categoryHash;             /* Link to children */
    CategoryAlias *categoryAliasHash;   /* Save category alias information */
    AudioType *audioType;               /* Link to parent */
    UT_hash_handle hh;
};

struct _AudioType
{
    char *name;
    char *tabName;
    int paramUnitDescVerMaj;            /* ParamUniDesc version */
    int paramUnitDescVerMin;
    int audioParamVerMaj;               /* AudioParam version */
    int audioParamVerMin;
    xmlDocPtr audioParamDoc;
    xmlDocPtr paramUnitDescDoc;
    xmlDocPtr paramTreeViewDoc;
    ParamTree *paramTreeHash;
    ParamUnit *paramUnitHash;
    ParamInfo *paramInfoHash;
    ParamTreeView *paramTreeView;
    int unusedParamId;
    int dirty;                          /* Indicate if the audio type modified without saveing*/
    int allowReload;                    /* Indicate the audio type can be reload since xml updated */
    CategoryType *categoryTypeHash;
#ifndef WIN32
    pthread_rwlock_t lock;
    const char *lockCallerFun;          /* Used to cache the lock holder */
#endif
    AppHandle *appHandle;               /* Link to it's appHandle parent */
    UT_hash_handle hh;
};

struct _FeatureOption
{
    char *name;
    char *value;
    UT_hash_handle hh;
};

struct _NotifyCb
{
    NOTIFY_CB_FUN cb;
    char test[512];
    struct _NotifyCb *next, *pre;
};

struct _AppHandle
{
    char *xmlDir;
    char *xmlCusDir;
    AudioType *audioTypeHash;
    FeatureOption *featureOptionsHash;
    xmlDocPtr featureOptionsDoc;
#ifndef WIN32
    pthread_t appThread;
    int appThreadExit;
    int inotifyFd;
    pthread_rwlock_t lock;
    const char *lockCallerFun;          /* Used to cache the lock holder */
#endif
    NotifyCb *noficyCbList;
};

typedef struct AudioTypeVerInfo{
    const char* audioTypeName;
    int paramUnitDescVerMaj;
    int paramUnitDescVerMin;
    int audioParamVerMaj;
    int audioParamVerMin;
} AudioTypeVerInfo;

static const AudioTypeVerInfo audioTypeSupportVerInfo [] =
{
    /* AudioType name, ParamUnitDescVer (maj, min), AudioParamVer (maj, min) */
    {"AudioCommonSetting",  1, 0, 1, 0},
    {"PlaybackACF",         1, 0, 1, 0},
    {"Playback",            1, 0, 1, 0},
    {"PlaybackDRC",         1, 0, 1, 0},
    {"PlaybackHCF",         1, 0, 1, 0},
    {"PlaybackVolAna",      1, 0, 1, 0},
    {"PlaybackVolDigi",     1, 0, 1, 0},
    {"PlaybackVolUI",       1, 0, 1, 0},
    {"Record",              1, 0, 1, 0},
    {"RecordDMNR",          1, 0, 1, 0},
    {"RecordFIR",           1, 0, 1, 0},
    {"RecordUI",            1, 0, 1, 0},
    {"RecordVol",           1, 0, 1, 0},
    {"RecordVolUI",         1, 0, 1, 0},
    {"Speech",              1, 0, 1, 0},
    {"SpeechDMNR",          1, 0, 1, 0},
    {"SpeechGeneral",       1, 0, 1, 0},
    {"SpeechMagiClarity",   1, 0, 1, 0},
    {"SpeechUI",            1, 0, 1, 0},
    {"SpeechVol",           1, 0, 1, 0},
    {"SpeechVolUI",         1, 0, 1, 0},
    {"VoIP",                1, 0, 1, 0},
    {"VoIPDMNR",            1, 0, 1, 0},
    {"VoIPGeneral",         1, 0, 1, 0},
    {"VoIPUI",              1, 0, 1, 0},
    {"VoIPVol",             1, 0, 1, 0},
    {"VoIPVolUI",           1, 0, 1, 0},
    {"Volume",              1, 0, 1, 0},
    {"VolumeGainMap",       1, 0, 1, 0},
    {NULL,                  0, 0, 0, 0}
};

/***********************
 * Public API
 **********************/
EXPORT void             appSetDebugLevel(MSG_LEVEL level);
EXPORT MSG_LEVEL        appGetDebugLevel(void);

/* appHandle API */
EXPORT APP_STATUS       appHandleInit(AppHandle *appHandle);
EXPORT APP_STATUS       appHandleUninit(AppHandle *appHandle);
EXPORT void             appHandleRedirectIOToConsole(void);
EXPORT AppHandle       *appHandleGetInstance(void);     /* Never uninit global instance */
EXPORT size_t           appHandleGetNumOfAudioType(AppHandle *appHandle);
EXPORT AudioType       *appHandleGetAudioTypeByIndex(AppHandle *appHandle, size_t index);
EXPORT AudioType       *appHandleGetAudioTypeByName(AppHandle *appHandle, const char *name);
EXPORT const char      *appHandleGetFeatureOptionValue(AppHandle *appHandle, const char *featureOptionName);
EXPORT int              appHandleIsFeatureOptionEnabled(AppHandle *appHandle, const char *featureOptionName);
EXPORT size_t           appHandleGetNumOfFeatureOption(AppHandle *appHandle);
EXPORT FeatureOption   *appHandleGetFeatureOptionByIndex(AppHandle *appHandle, size_t index);
EXPORT const char      *appHandleGetBuildTimeStamp();
EXPORT APP_STATUS       appHandleCompressFiles(const char* srcDir, const char* destFile);
EXPORT APP_STATUS       appHandleUncompressFile(const char* srcFile, const char* destDir);

/* Following 4 APIs will acquire app handle write lock automatically */
EXPORT APP_STATUS       appHandleParseXml(AppHandle *appHandle, const char *dir, const char *cusDir);
EXPORT APP_STATUS       appHandleReloadAudioType(AppHandle *appHandle, const char *audioTypeName);
EXPORT void             appHandleRegXmlChangedCb(AppHandle *appHandle, NOTIFY_CB_FUN nofiyCallback);
EXPORT void             appHandleUnregXmlChangedCb(AppHandle *appHandle, NOTIFY_CB_FUN nofiyCallback);

/* AudioType API */
EXPORT APP_STATUS       audioTypeIsTuningToolSupportedXmlVer(AudioType *audioType);
EXPORT APP_STATUS       audioTypeIsDeviceSupportedXmlVer(AudioType *audioType);
EXPORT size_t           audioTypeGetNumOfCategoryType(AudioType *audioType);
EXPORT CategoryType    *audioTypeGetCategoryTypeByIndex(AudioType *audioType, size_t idnex);
EXPORT CategoryType    *audioTypeGetCategoryTypeByName(AudioType *audioType, const char *categoryTypeName);
EXPORT CategoryType    *audioTypeGetCategoryTypeByWording(AudioType *audioType, const char *categoryTypeWording);
EXPORT xmlNode         *audioTypeGetCategoryTypeListNode(AudioType *audioType);
EXPORT ParamUnit       *audioTypeGetParamUnit(AudioType *audioType, const char *categoryPath);
EXPORT size_t           audioTypeGetNumOfParamInfo(AudioType *audioType);
EXPORT ParamInfo       *audioTypeGetParamInfoByIndex(AudioType *audioType, size_t index);
EXPORT ParamInfo       *audioTypeGetParamInfoByName(AudioType *audioType, const char *paramName);
EXPORT APP_STATUS       audioTypeSaveAudioParamXml(AudioType *audioType, const char *saveDir, int clearDirtyBit);
EXPORT int              audioTypeReadLock(AudioType *audioType, const char *callerFun);
EXPORT int              audioTypeWriteLock(AudioType *audioType, const char *callerFun);
EXPORT int              audioTypeUnlock(AudioType *audioType);
EXPORT TreeRoot        *audioTypeGetTreeRoot(AudioType *audioType, const char *treeRootName);

/* Following 3 write APIs will acquire write lock automatically */
EXPORT APP_STATUS       audioTypeSetParamData(AudioType *audioType, const char *categoryPath, ParamInfo *paramName, void *dataPtr, int arraySize);
EXPORT APP_STATUS       audioTypeSetFieldData(AudioType *audioType, const char *categoryPath, FieldInfo *fieldInfo, unsigned int val);
EXPORT APP_STATUS       audioTypeParamUnitCopy(AudioType *audioType, const char *srcCategoryPath, const char *dstCategoryPath);

/* CategoryType API */
EXPORT size_t           categoryTypeGetNumOfCategoryGroup(CategoryType *categoryType);
EXPORT CategoryGroup   *categoryTypeGetCategoryGroupByIndex(CategoryType *categoryType, size_t index);
EXPORT CategoryGroup   *categoryTypeGetCategoryGroupByWording(CategoryType *categoryType, const char *wording);
EXPORT size_t           categoryTypeGetNumOfCategory(CategoryType *categoryType);
EXPORT Category        *categoryTypeGetCategoryByIndex(CategoryType *categoryType, size_t index);
EXPORT Category        *categoryTypeGetCategoryByWording(CategoryType *categoryType, const char *wording);

/* CategoryGroup API */
EXPORT size_t           categoryGroupGetNumOfCategory(CategoryGroup *categoryGroup);
EXPORT Category        *categoryGroupGetCategoryByIndex(CategoryGroup *categoryGroup, size_t index);
EXPORT Category        *categoryGroupGetCategoryByWording(CategoryGroup *categoryGroup, const char *index);

/* CategoryAlias API */
EXPORT CategoryAlias   *categoryAliasCreate(const char *alias, Category *category);
EXPORT void             categoryAliasRelease(CategoryAlias *categoryAlias);

/* ParamInfo API */
EXPORT size_t           paramInfoGetNumOfFieldInfo(ParamInfo *paramInfo);
EXPORT FieldInfo       *paramInfoGetFieldInfoByIndex(ParamInfo *paramInfo, size_t index);
EXPORT FieldInfo       *paramInfoGetFieldInfoByName(ParamInfo *paramInfo, const char *fieldName);
EXPORT char            *paramNewDataStr(Param *param);

/* ParamUnit API */
EXPORT size_t           paramUnitGetNumOfParam(ParamUnit *paramUnit);
EXPORT Param           *paramUnitGetParamByIndex(ParamUnit *paramUnit, size_t index);
EXPORT Param           *paramUnitGetParamByName(ParamUnit *paramUnit, const char *paramName);
EXPORT ParamInfo       *paramUnitGetParamInfo(ParamUnit *paramUnit, const char *paramInfoName);
EXPORT FieldInfo       *paramUnitGetFieldInfo(ParamUnit *paramUnit, const char *paramName, const char *fieldName);
EXPORT APP_STATUS       paramUnitGetFieldVal(ParamUnit *paramUnit, const char *paramName, const char *fieldName, unsigned int *val);

/* Param API */
EXPORT size_t           paramGetArraySizeFromString(const char *str);
EXPORT APP_STATUS       paramGetFieldVal(Param *param, FieldInfo *fieldInfo, unsigned int *val);
EXPORT APP_STATUS       paramSetFieldVal(Param *param, FieldInfo *fieldInfo, unsigned int val);
EXPORT DATA_TYPE        paramDataTypeToEnum(const char *dataType);
EXPORT const char      *paramDataTypeToStr(DATA_TYPE dataType);

/* Field API */
EXPORT APP_STATUS       fieldInfoGetCheckListValue(FieldInfo *fieldInfo, const char *checkName, unsigned int *checkVal);

/* TreeRoot API */
EXPORT Feature         *treeRootGetFeatureByName(TreeRoot *treeRoot, const char *featureName);
EXPORT int              featureIsCategoryPathSupport(Feature *feature, const char *categoryPath);

/* Xml Node related APIs */
EXPORT xmlNode         *findXmlNodeByElemName(xmlNode *node, const char *elemName);
EXPORT xmlChar         *xmlNodeGetProp(xmlNode *node, const char *prop);
EXPORT xmlChar         *xmlNodeGetWording(xmlNode *node);

/* Utils APIs */
EXPORT APP_STATUS       utilConvDataStringToNative(DATA_TYPE dataType, const char *paramDataStr, void **paramData, size_t *arraySize);

/* Unit test */
EXPORT APP_STATUS       unitTest(AppHandle *appHandle);
EXPORT char            *utilGetStdin(char *buf, int bufSize);

/* Following APIs is designed for EM tool integration */
EXPORT APP_STATUS       utilNativeSetField(const char *audioTypeName, const char *categoryPath, const char *paramName, const char *fieldName, const char *fieldValueStr);
EXPORT APP_STATUS       utilNativeSetParam(const char *audioTypeName, const char *categoryPath, const char *paramName, const char *paramDataStr);
EXPORT char            *utilNativeGetCategory(const char *audioTypeName, const char *categoryTypeName);
EXPORT char            *utilNativeGetParam(const char *audioTypeName, const char *categoryPath, const char *paramName);
EXPORT unsigned int     utilNativeGetField(const char *audioTypeName, const char *categoryPath, const char *paramName, const char *fieldName);
EXPORT APP_STATUS       utilNativeSaveXml(const char *audioTypeName);
EXPORT char            *utilNativeGetChecklist(const char *audioTypeName, const char *paramName, const char *fieldName);

#ifndef WIN32
#ifdef __cplusplus
}
#endif
#endif

#endif
