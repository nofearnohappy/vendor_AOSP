#include "AudioParamParserPriv.h"

EXPORT CategoryType *categoryTypeCreate(const char *name, const char *wording, AudioType *audioType, int visible)
{
    CategoryType *categoryType = NULL;
    categoryType = (CategoryType *)malloc(sizeof(CategoryType));
    categoryType->name = strdup(name);
    categoryType->wording = strdup(wording);
    categoryType->audioType = audioType;
    categoryType->visible = visible;
    categoryType->categoryHash = NULL;
    categoryType->categoryAliasHash = NULL;
    categoryType->categoryGroupHash = NULL;
    return categoryType;
}

EXPORT void categoryTypeRelease(CategoryType *categoryType)
{
    free(categoryType->name);
    free(categoryType->wording);
    if (categoryType->categoryHash)
    {
        Category *tmp, *item;
        HASH_ITER(hh, categoryType->categoryHash, item, tmp)
        {
            HASH_DEL(categoryType->categoryHash, item);
            categoryRelease(item);
        }
    }

    if (categoryType->categoryAliasHash)
    {
        CategoryAlias *tmp, *item;
        HASH_ITER(hh, categoryType->categoryAliasHash, item, tmp)
        {
            HASH_DEL(categoryType->categoryAliasHash, item);
            categoryAliasRelease(item);
        }
    }
    free(categoryType);
}

EXPORT size_t categoryTypeGetNumOfCategory(CategoryType *categoryType)
{
    if (!categoryType)
    {
        ERR_LOG("categoryType is NULL!\n");
        return 0;
    }

    return HASH_COUNT(categoryType->categoryHash);
}

EXPORT size_t categoryTypeGetNumOfCategoryGroup(CategoryType *categoryType)
{
    if (!categoryType)
    {
        ERR_LOG("CategoryType is NULL\n");
        return 0;
    }

    return HASH_COUNT(categoryType->categoryGroupHash);
}

EXPORT Category *categoryTypeGetCategoryByIndex(CategoryType *categoryType, size_t index)
{
    Category *category = NULL;
    size_t i = 0;

    for (category = categoryType->categoryHash; category ; category = category->hh.next)
    {
        if (index == i++)
        {
            return category;
        }
    }

    return NULL;
}

EXPORT CategoryGroup *categoryTypeGetCategoryGroupByIndex(CategoryType *categoryType, size_t index)
{
    CategoryGroup *categoryGroup = NULL;
    size_t i = 0;

    if (!categoryType)
    {
        ERR_LOG("CategoryType is NULL\n");
        return NULL;
    }

    for (categoryGroup = categoryType->categoryGroupHash; categoryGroup ; categoryGroup = categoryGroup->hh.next)
    {
        if (index == i++)
        {
            return categoryGroup;
        }
    }

    return NULL;
}

EXPORT CategoryAlias *categoryTypeGetCategoryByAlias(CategoryType *categoryType, const char *alais)
{
    CategoryAlias *categoryAlias;

    if (!categoryType)
    {
        ERR_LOG("categoryType is NULL!\n");
        return NULL;
    }

    HASH_FIND_STR(categoryType->categoryAliasHash, alais, categoryAlias);

    return categoryAlias;
}

EXPORT Category *categoryTypeGetCategoryByName(CategoryType *categoryType, const char *name)
{
    CategoryGroup *categoryGroup;
    Category *category;

    if (!categoryType)
    {
        ERR_LOG("categoryType is NULL!\n");
        return NULL;
    }

    if (!name)
    {
        ERR_LOG("name is NULL\n");
        return NULL;
    }

    for (categoryGroup = categoryType->categoryGroupHash; categoryGroup; categoryGroup = categoryGroup->hh.next)
    {
        for (category = categoryGroup->categoryHash; category; category = category->hh.next)
        {
            if (!strcmp(category->name, name))
            {
                return category;
            }
        }
    }

    return NULL;
}

EXPORT Category *categoryTypeGetCategoryByWording(CategoryType *categoryType, const char *wording)
{
    Category *category;

    if (!categoryType)
    {
        ERR_LOG("categoryType is NULL!\n");
        return NULL;
    }

    HASH_FIND_STR(categoryType->categoryHash, wording, category);

    return category;
}

EXPORT CategoryGroup *categoryTypeGetCategoryGroupByWording(CategoryType *categoryType, const char *wording)
{
    CategoryGroup *categoryGroup;

    if (!categoryType)
    {
        ERR_LOG("categoryType is NULL!\n");
        return NULL;
    }

    HASH_FIND_STR(categoryType->categoryGroupHash, wording, categoryGroup);

    return categoryGroup;
}

EXPORT CategoryGroup *categoryGroupCreate(const char *name, const char *wording, CategoryType *categoryType, int visible)
{
    CategoryGroup *categoryGroup = NULL;
    categoryGroup = (CategoryGroup *)malloc(sizeof(CategoryGroup));
    categoryGroup->name = strdup(name);
    categoryGroup->wording = strdup(wording);
    categoryGroup->categoryType = categoryType;
    categoryGroup->visible = visible;
    categoryGroup->categoryHash = NULL;
    return categoryGroup;
}

EXPORT void categoryGroupRelease(CategoryGroup *categoryGroup)
{
    free(categoryGroup->name);
    free(categoryGroup->wording);
    if (categoryGroup->categoryHash)
    {
        Category *tmp, *item;
        HASH_ITER(hh, categoryGroup->categoryHash, item, tmp)
        {
            HASH_DEL(categoryGroup->categoryHash, item);
            categoryRelease(item);
        }
    }
    free(categoryGroup);
}

EXPORT size_t categoryGroupGetNumOfCategory(CategoryGroup *categoryGroup)
{
    if (!categoryGroup)
    {
        ERR_LOG("categoryGroup is NULL!\n");
        return 0;
    }

    return HASH_COUNT(categoryGroup->categoryHash);
}

EXPORT Category *categoryCreate(const char *name, const char *wording, CATEGORY_PARENT_TYPE isCategoryGroup, void *parent, int visible)
{
    Category *category = NULL;
    category = (Category *)malloc(sizeof(Category));
    category->name = strdup(name);
    category->wording = strdup(wording);
    category->parentType = isCategoryGroup;
    if (isCategoryGroup)
    {
        category->parent.categoryType = (CategoryType *)parent;
    }
    else
    {
        category->parent.category = (Category *)parent;
    }
    category->visible = visible;

    return category;
}

EXPORT void categoryRelease(Category *category)
{
    free(category->name);
    free(category->wording);
    free(category);
}

EXPORT CategoryAlias *categoryAliasCreate(const char *alias, Category *category)
{
    CategoryAlias *categoryAlias = NULL;
    categoryAlias = (CategoryAlias *)malloc(sizeof(CategoryAlias));
    categoryAlias->alias = strdup(alias);
    categoryAlias->category = category;

    return categoryAlias;
}

EXPORT void categoryAliasRelease(CategoryAlias *categoryAlias)
{
    free(categoryAlias->alias);
    free(categoryAlias);
}

EXPORT Category *categoryGroupGetCategoryByIndex(CategoryGroup *categoryGroup, size_t index)
{
    Category *category = NULL;
    size_t i = 0;

    if (!categoryGroup)
    {
        ERR_LOG("categoryGroup is NULL!\n");
        return NULL;
    }

    for (category = categoryGroup->categoryHash; category ; category = category->hh.next)
    {
        if (index == i++)
        {
            return category;
        }
    }

    return NULL;
}

EXPORT Category *categoryGroupGetCategoryByWording(CategoryGroup *categoryGroup, const char *wording)
{
    Category *category;

    if (!categoryGroup)
    {
        ERR_LOG("categoryGroup is NULL!\n");
        return 0;
    }

    HASH_FIND_STR(categoryGroup->categoryHash, wording, category);

    return category;
}