package com.mediatek.qsb.ext;

import android.content.Context;

public interface IWebSearchHandler {
    /**
     * Handle search using the input search URI and engine name.
     *
     * @param context
     * @param searchEngineName Name of search engine to be used for search.
     * @param searchUri Search URI on which search is to be performed.
     * @return false in case search fails (incorrect input), else return true.
     * @internal
     */
    boolean handleSearchInternal(Context context, String searchEngineName, String searchUri);
}
