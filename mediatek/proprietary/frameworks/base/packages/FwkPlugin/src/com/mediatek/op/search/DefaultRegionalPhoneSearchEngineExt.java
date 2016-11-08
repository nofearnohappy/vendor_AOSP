package com.mediatek.op.search;

import com.mediatek.common.PluginImpl;
import com.mediatek.common.search.IRegionalPhoneSearchEngineExt;
import com.mediatek.common.search.SearchEngine;
import android.content.Context;
import java.util.List;

@PluginImpl(interfaceName="com.mediatek.common.search.IRegionalPhoneSearchEngineExt")
public class DefaultRegionalPhoneSearchEngineExt implements IRegionalPhoneSearchEngineExt{

    public List<SearchEngine> initSearchEngineInfosFromRpm(Context context) {
        return null;
    }

}
