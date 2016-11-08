#include "AudioParamParserPriv.h"
#include <errno.h>

Param *paramCreate(const char *name, ParamInfo *paramInfo, const char *paramValue)
{
    Param *param = NULL;
    if (!paramInfo)
    {
        ERR_LOG("The paramInfo is NULL, cannot create %s param!\n", name);
        return NULL;
    }

    param = (Param *)malloc(sizeof(Param));
    param->data = NULL;
    param->name = strdup(name);
    param->paramInfo = paramInfo;
    paramSetupDataInfoByStr(param, paramValue);

    return param;
}

EXPORT void paramRelease(Param *param)
{
    if (param)
    {
        if (param->name)
        {
            free(param->name);
        }
        if (param->data)
        {
            free(param->data);
        }
        free(param);
    }
}

EXPORT Param *paramHashClone(Param *paramHash)
{
    Param *param = NULL, *item, *newItem;
    size_t i;
    if (!paramHash)
    {
        ERR_LOG("paramHash is NULL\n");
        return NULL;
    }

    for (item = paramHash; item != NULL; item = item->hh.next)
    {
        newItem = malloc(sizeof(Param));
        newItem->arraySize = item->arraySize;
        newItem->paramInfo = item->paramInfo;
        newItem->paramUnit = item->paramUnit;
        newItem->name = strdup(item->name);

        switch (item->paramInfo->dataType)
        {
            case TYPE_STR:
                newItem->data = strdup(item->data);
                break;
            case TYPE_INT:
                newItem->data = malloc(sizeof(int));
                *(int *)newItem->data = *(int *)item->data;
                break;
            case TYPE_UINT:
                newItem->data = malloc(sizeof(unsigned int));
                *(unsigned int *)newItem->data = *(unsigned int *)item->data;
                break;
            case TYPE_FLOAT:
                newItem->data = malloc(sizeof(float));
                *(float *)newItem->data = *(float *)item->data;
                break;
            case TYPE_BYTE_ARRAY:
                newItem->data = malloc(sizeof(char) * item->arraySize);
                for (i = 0; i < item->arraySize; i++)
                {
                    ((char *)newItem->data)[i] = ((char *)item->data)[i];
                }
                break;
            case TYPE_UBYTE_ARRAY:
                newItem->data = malloc(sizeof(unsigned char) * item->arraySize);
                for (i = 0; i < item->arraySize; i++)
                {
                    ((unsigned char *)newItem->data)[i] = ((unsigned char *)item->data)[i];
                }
                break;
            case TYPE_SHORT_ARRAY:
                newItem->data = malloc(sizeof(short) * item->arraySize);
                for (i = 0; i < item->arraySize; i++)
                {
                    ((short *)newItem->data)[i] = ((short *)item->data)[i];
                }
                break;
            case TYPE_USHORT_ARRAY:
                newItem->data = malloc(sizeof(unsigned short) * item->arraySize);
                for (i = 0; i < item->arraySize; i++)
                {
                    ((unsigned short *)newItem->data)[i] = ((unsigned short *)item->data)[i];
                }
                break;
            case TYPE_INT_ARRAY:
                newItem->data = malloc(sizeof(int) * item->arraySize);
                for (i = 0; i < item->arraySize; i++)
                {
                    ((int *)newItem->data)[i] = ((int *)item->data)[i];
                }
                break;
            case TYPE_UINT_ARRAY:
                newItem->data = malloc(sizeof(unsigned int) * item->arraySize);
                for (i = 0; i < item->arraySize; i++)
                {
                    ((unsigned int *)newItem->data)[i] = ((unsigned int *)item->data)[i];
                }
                break;
            case TYPE_DOUBLE_ARRAY:
                newItem->data = malloc(sizeof(double) * item->arraySize);
                for (i = 0; i < item->arraySize; i++)
                {
                    ((double *)newItem->data)[i] = ((double *)item->data)[i];
                }
                break;
            case TYPE_FIELD:
            case TYPE_UNKNOWN:
                break;
        }
        HASH_ADD_KEYPTR(hh, param, newItem->name, strlen(newItem->name), newItem);
    }

    return param;
}

EXPORT DATA_TYPE paramDataTypeToEnum(const char *dataType)
{
    if (!dataType)
    {
        ERR_LOG("dataType is NULL\n");
        return TYPE_UNKNOWN;
    }

    if (!strcmp(DATA_TYPE_STR_STRING, dataType))
    {
        return TYPE_STR;
    }
    else if (!strcmp(DATA_TYPE_INT_STRING, dataType))
    {
        return TYPE_INT;
    }
    else if (!strcmp(DATA_TYPE_UINT_STRING, dataType))
    {
        return TYPE_UINT;
    }
    else if (!strcmp(DATA_TYPE_FLOAT_STRING, dataType))
    {
        return TYPE_FLOAT;
    }
    else if (!strcmp(DATA_TYPE_BYTE_ARRAY_STRING, dataType))
    {
        return TYPE_BYTE_ARRAY;
    }
    else if (!strcmp(DATA_TYPE_UBYTE_ARRAY_STRING, dataType))
    {
        return TYPE_UBYTE_ARRAY;
    }
    else if (!strcmp(DATA_TYPE_SHORT_ARRAY_STRING, dataType))
    {
        return TYPE_SHORT_ARRAY;
    }
    else if (!strcmp(DATA_TYPE_USHORT_ARRAY_STRING, dataType))
    {
        return TYPE_USHORT_ARRAY;
    }
    else if (!strcmp(DATA_TYPE_INT_ARRAY_STRING, dataType))
    {
        return TYPE_INT_ARRAY;
    }
    else if (!strcmp(DATA_TYPE_UINT_ARRAY_STRING, dataType))
    {
        return TYPE_UINT_ARRAY;
    }
    else if (!strcmp(DATA_TYPE_DOUBLE_ARRAY_STRING, dataType))
    {
        return TYPE_DOUBLE_ARRAY;
    }
    else
    {
        return TYPE_UNKNOWN;
    }
}

EXPORT const char  *paramDataTypeToStr(DATA_TYPE dataType)
{
    switch (dataType)
    {
        case TYPE_STR:
            return DATA_TYPE_STR_STRING;
        case TYPE_INT:
            return DATA_TYPE_INT_STRING;
        case TYPE_UINT:
            return DATA_TYPE_UINT_STRING;
        case TYPE_FLOAT:
            return DATA_TYPE_FLOAT_STRING;
        case TYPE_BYTE_ARRAY:
            return DATA_TYPE_BYTE_ARRAY_STRING;
        case TYPE_UBYTE_ARRAY:
            return DATA_TYPE_UBYTE_ARRAY_STRING;
        case TYPE_SHORT_ARRAY:
            return DATA_TYPE_SHORT_ARRAY_STRING;
        case TYPE_USHORT_ARRAY:
            return DATA_TYPE_USHORT_ARRAY_STRING;
        case TYPE_INT_ARRAY:
            return DATA_TYPE_INT_ARRAY_STRING;
        case TYPE_UINT_ARRAY:
            return DATA_TYPE_UINT_ARRAY_STRING;
        case TYPE_DOUBLE_ARRAY:
            return DATA_TYPE_DOUBLE_ARRAY_STRING;
        case TYPE_UNKNOWN:
            return DATA_TYPE_UNKNOWN_STRING;
        case TYPE_FIELD:
            return DATA_TYPE_FIELD_STRING;
    }
    return DATA_TYPE_UNKNOWN_STRING;
}

EXPORT APP_STATUS paramSetupDataInfoByStr(Param *param, const char *str)
{
    return utilConvDataStringToNative(param->paramInfo->dataType, str, &param->data, &param->arraySize);
}

EXPORT size_t paramGetArraySizeFromString(const char *str)
{
    size_t numOfSeperator = 0;

    if (!str)
    {
        ERR_LOG("str is NULL!\n");
        return 0;
    }

    while ((str = strstr(str, ARRAY_SEPERATOR)) != NULL)
    {
        numOfSeperator++;
        str++;
    }

    return numOfSeperator + 1;
}

EXPORT APP_STATUS paramGetFieldVal(Param *param, FieldInfo *fieldInfo, unsigned int *val)
{
    unsigned char uchar;
    unsigned short ushort;
    unsigned int uint;

    if (!param)
    {
        WARN_LOG("param is NULL\n");
        return APP_ERROR;
    }

    if (!fieldInfo)
    {
        WARN_LOG("param is NULL\n");
        return APP_ERROR;
    }

    if (fieldInfo->arrayIndex >= param->arraySize)
    {
        WARN_LOG("Param's array size is less than field index. (param=%s, field index=%lu >= array size=%lu)\n", param->name, fieldInfo->arrayIndex, param->arraySize);
        return APP_ERROR;
    }

    if (fieldInfo->startBit < 0)
    {
        WARN_LOG("The startBit < 0 (param=%s, startBit=%d)\n", param->name, fieldInfo->startBit);
        return APP_ERROR;
    }

    if (fieldInfo->endBit >= 32)
    {
        WARN_LOG("The startBit >= 32 (param=%s, endBit=%d)\n", param->name, fieldInfo->endBit);
        return APP_ERROR;
    }

    switch (param->paramInfo->dataType)
    {
        case TYPE_BYTE_ARRAY:
        case TYPE_UBYTE_ARRAY:
            if (fieldInfo->startBit > fieldInfo->endBit || fieldInfo->endBit >= 8)
            {
                WARN_LOG("Field's bit information is not match param type. (param=%s, start bit=%d, end bit=%d)\n", param->name, fieldInfo->startBit, fieldInfo->endBit);
                return APP_ERROR;
            }
            uchar = ((unsigned char *)param->data)[fieldInfo->arrayIndex];
            *val = ((uchar >> (fieldInfo->startBit)) & ((1 << (fieldInfo->endBit - fieldInfo->startBit + 1)) - 1));
            return APP_NO_ERROR;
        case TYPE_SHORT_ARRAY:
        case TYPE_USHORT_ARRAY:
            if (fieldInfo->startBit > fieldInfo->endBit || fieldInfo->endBit >= 16)
            {
                WARN_LOG("Field's bit information is not match param type. (param=%s, start bit=%d, end bit=%d)\n", param->name, fieldInfo->startBit, fieldInfo->endBit);
                return APP_ERROR;
            }
            ushort = ((unsigned short *)param->data)[fieldInfo->arrayIndex];
            *val = ((ushort >> (fieldInfo->startBit)) & ((1 << (fieldInfo->endBit - fieldInfo->startBit + 1)) - 1));
            return APP_NO_ERROR;
        case TYPE_INT_ARRAY:
        case TYPE_UINT_ARRAY:
            if (fieldInfo->startBit > fieldInfo->endBit || fieldInfo->endBit >= 32)
            {
                WARN_LOG("Field's bit information is not match param type. (param=%s, start bit=%d, end bit=%d)\n", param->name, fieldInfo->startBit, fieldInfo->endBit);
                return APP_ERROR;
            }
            uint = ((unsigned int *)param->data)[fieldInfo->arrayIndex];
            *val = ((uint >> (fieldInfo->startBit)) & ((1 << (fieldInfo->endBit - fieldInfo->startBit + 1)) - 1));
            return APP_NO_ERROR;
        default:
            WARN_LOG("Param didn't support field info (param=%s, type=%s)\n", param->name, paramDataTypeToStr(param->paramInfo->dataType));
            return APP_ERROR;
    }

    return APP_ERROR;
}

EXPORT char *paramNewDataStr(Param *param)
{
    return utilConvDataToString(param->paramInfo->dataType, param->data, param->arraySize);
}

EXPORT APP_STATUS paramSetupDataInfoByVal(Param *param, void *data, int arraySize)
{
    int i;

    if (!param)
    {
        ERR_LOG("Param is NULL\n");
        return APP_ERROR;
    }

    switch (param->paramInfo->dataType)
    {
        case TYPE_BYTE_ARRAY:
            param->data = malloc(sizeof(char) * arraySize);
            for (i = 0; i < arraySize; i++)
            {
                ((char *)param->data)[i] = ((char *)data)[i];
            }
            param->arraySize = arraySize;
            break;
        case TYPE_UBYTE_ARRAY:
            param->data = malloc(sizeof(unsigned char) * arraySize);
            for (i = 0; i < arraySize; i++)
            {
                ((unsigned char *)param->data)[i] = ((unsigned char *)data)[i];
            }
            param->arraySize = arraySize;
            break;
        case TYPE_SHORT_ARRAY:
            param->data = malloc(sizeof(short) * arraySize);
            for (i = 0; i < arraySize; i++)
            {
                ((short *)param->data)[i] = ((short *)data)[i];
            }
            param->arraySize = arraySize;
            break;
        case TYPE_USHORT_ARRAY:
            param->data = malloc(sizeof(unsigned short) * arraySize);
            for (i = 0; i < arraySize; i++)
            {
                ((unsigned short *)param->data)[i] = ((unsigned short *)data)[i];
            }
            param->arraySize = arraySize;
            break;
        case TYPE_INT_ARRAY:
            param->data = malloc(sizeof(int) * arraySize);
            for (i = 0; i < arraySize; i++)
            {
                ((int *)param->data)[i] = ((int *)data)[i];
            }
            param->arraySize = arraySize;
            break;
        case TYPE_UINT_ARRAY:
            param->data = malloc(sizeof(unsigned int) * arraySize);
            for (i = 0; i < arraySize; i++)
            {
                ((unsigned int *)param->data)[i] = ((unsigned int *)data)[i];
            }
            param->arraySize = arraySize;
            break;
        case TYPE_DOUBLE_ARRAY:
            param->data = malloc(sizeof(double) * arraySize);
            for (i = 0; i < arraySize; i++)
            {
                ((double *)param->data)[i] = ((double *)data)[i];
            }
            param->arraySize = arraySize;
            break;
        case TYPE_STR:
            param->data = strdup((const char *)data);
            break;
        case TYPE_INT:
            param->data = malloc(sizeof(int));
            *(int *)param->data = *(int *)data;
            break;
        case TYPE_UINT:
            param->data = malloc(sizeof(unsigned int));
            *(unsigned int *)param->data = *(unsigned int *)data;
            break;
        case TYPE_FLOAT:
            param->data = malloc(sizeof(float));
            *(float *)param->data = *(float *)data;
            break;
        default:
            return APP_ERROR;
    }

    return APP_NO_ERROR;
}

EXPORT APP_STATUS paramSetFieldVal(Param *param, FieldInfo *fieldInfo, unsigned int val)
{
    unsigned long mask;

    if (!param)
    {
        ERR_LOG("param is NULL\n");
        return APP_ERROR;
    }

    if (!fieldInfo)
    {
        ERR_LOG("param is NULL\n");
        return APP_ERROR;
    }

    if (fieldInfo->arrayIndex >= param->arraySize)
    {
        ERR_LOG("Param's array size is less than field index. (param=%s, field index=%lu >= array size=%lu)\n", param->name, fieldInfo->arrayIndex, param->arraySize);
        return APP_ERROR;
    }

    if (fieldInfo->startBit < 0)
    {
        ERR_LOG("The startBit < 0 (param=%s, startBit=%d)\n", param->name, fieldInfo->startBit);
        return APP_ERROR;
    }

    if (fieldInfo->endBit >= 32)
    {
        ERR_LOG("The startBit >= 32 (param=%s, endBit=%d)\n", param->name, fieldInfo->endBit);
        return APP_ERROR;
    }

    if (fieldInfo->endBit == 31 && fieldInfo->startBit == 0)
    {
        mask = 0xFFFFFFFF;
    }
    else
    {
        mask = ((1 << (fieldInfo->endBit - fieldInfo->startBit + 1)) - 1);
    }

    switch (param->paramInfo->dataType)
    {
        case TYPE_BYTE_ARRAY:
        case TYPE_UBYTE_ARRAY:
            if (fieldInfo->startBit > fieldInfo->endBit || fieldInfo->endBit >= 8)
            {
                ERR_LOG("Field's bit information is not match param type. (param=%s, start bit=%d, end bit=%d)\n", param->name, fieldInfo->startBit, fieldInfo->endBit);
                return APP_ERROR;
            }

            ((unsigned char *)param->data)[fieldInfo->arrayIndex] =
                ((((unsigned char *)param->data)[fieldInfo->arrayIndex] & ~(mask << fieldInfo->startBit)) | (unsigned char)(val & mask) << fieldInfo->startBit);
            return APP_NO_ERROR;
        case TYPE_SHORT_ARRAY:
        case TYPE_USHORT_ARRAY:
            if (fieldInfo->startBit > fieldInfo->endBit || fieldInfo->endBit >= 16)
            {
                ERR_LOG("Field's bit information is not match param type. (param=%s, start bit=%d, end bit=%d)\n", param->name, fieldInfo->startBit, fieldInfo->endBit);
                return APP_ERROR;
            }

            ((unsigned short *)param->data)[fieldInfo->arrayIndex] =
                ((((unsigned short *)param->data)[fieldInfo->arrayIndex] & ~(mask << fieldInfo->startBit)) | (unsigned short)(val & mask) << fieldInfo->startBit);
            return APP_NO_ERROR;
        case TYPE_INT_ARRAY:
        case TYPE_UINT_ARRAY:
            if (fieldInfo->startBit > fieldInfo->endBit || fieldInfo->endBit >= 32)
            {
                ERR_LOG("Field's bit information is not match param type. (param=%s, start bit=%d, end bit=%d)\n", param->name, fieldInfo->startBit, fieldInfo->endBit);
                return APP_ERROR;
            }

            ((unsigned int *)param->data)[fieldInfo->arrayIndex] =
                ((((unsigned int *)param->data)[fieldInfo->arrayIndex] & ~(mask << fieldInfo->startBit)) | (val & mask) << fieldInfo->startBit);
            return APP_NO_ERROR;
        default:
            ERR_LOG("Param is not array type, cannot query field value (param=%s, type=%s)\n", param->name, paramDataTypeToStr(param->paramInfo->dataType));
            return APP_ERROR;
    }

    return APP_ERROR;
}

