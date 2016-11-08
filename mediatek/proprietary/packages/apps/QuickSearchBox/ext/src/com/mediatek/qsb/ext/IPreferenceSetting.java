package com.mediatek.qsb.ext;

import android.content.Context;
import com.mediatek.common.search.SearchEngine;

public interface IPreferenceSetting {
    /**
     * API for getting the default search engine set in QSB.
     *
     * @param context
     * @return SearchEngine object based on current setting.
     * @internal
     */
    SearchEngine getDefaultSearchEngine(Context context);
}
