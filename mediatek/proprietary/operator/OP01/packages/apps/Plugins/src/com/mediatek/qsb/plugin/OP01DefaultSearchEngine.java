package com.mediatek.qsb.plugin;

import android.content.Context;

import com.mediatek.common.PluginImpl;
import com.mediatek.common.search.SearchEngine;
import com.mediatek.search.SearchEngineManager;

import com.mediatek.qsb.ext.IPreferenceSetting;

@PluginImpl(interfaceName="com.mediatek.qsb.ext.IPreferenceSetting")
public class OP01DefaultSearchEngine implements IPreferenceSetting {
    private static final String TAG = "OP01DefaultSearchEngine";

    public static final String DEFAULT_SEARCH_ENGINE = "baidu";

    /// M: Return the default search engine info. @{
    public SearchEngine getDefaultSearchEngine(Context context) {
        SearchEngineManager searchEngineManager = (SearchEngineManager) context
                .getSystemService(Context.SEARCH_ENGINE_SERVICE);
        return searchEngineManager.getByName(DEFAULT_SEARCH_ENGINE);
    }
    /// @}
}
