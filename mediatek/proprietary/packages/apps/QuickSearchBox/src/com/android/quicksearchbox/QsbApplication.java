/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.quicksearchbox;

import com.android.quicksearchbox.google.GoogleSource;
import com.android.quicksearchbox.google.GoogleSuggestClient;
import com.android.quicksearchbox.google.SearchBaseUrlHelper;
import com.android.quicksearchbox.preferences.PreferenceControllerFactory;
import com.android.quicksearchbox.preferences.SearchEngineItemsController;
import com.android.quicksearchbox.ui.DefaultSuggestionViewFactory;
import com.android.quicksearchbox.ui.SearchActivityViewSinglePane;
import com.android.quicksearchbox.ui.SearchActivityViewTwoPane;
import com.android.quicksearchbox.ui.SuggestionViewFactory;
import com.android.quicksearchbox.util.Factory;
import com.android.quicksearchbox.util.HttpHelper;
import com.android.quicksearchbox.util.JavaNetHttpHelper;
import com.android.quicksearchbox.util.NamedTaskExecutor;
import com.android.quicksearchbox.util.PerNameExecutor;
import com.android.quicksearchbox.util.PriorityThreadFactory;
import com.android.quicksearchbox.util.SingleThreadNamedTaskExecutor;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mediatek.common.search.SearchEngine;
import com.mediatek.search.SearchEngineManager;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.SystemProperties;
import android.util.Log;
import android.view.ContextThemeWrapper;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.List;

public class QsbApplication {
    private final static String TAG = "QsbApplication";
    private final static boolean DBG = true;
    private final Context mContext;

    private int mVersionCode;
    private Handler mUiThreadHandler;
    private Config mConfig;
    private SearchSettings mSettings;
    private Sources mSources;
    private Corpora mCorpora;
    private CorpusRanker mCorpusRanker;
    private ShortcutRepository mShortcutRepository;
    private ShortcutRefresher mShortcutRefresher;
    private NamedTaskExecutor mSourceTaskExecutor;
    private ThreadFactory mQueryThreadFactory;
    private SuggestionsProvider mSuggestionsProvider;
    private SuggestionViewFactory mSuggestionViewFactory;
    private GoogleSource mGoogleSource;
    private VoiceSearch mVoiceSearch;
    private Logger mLogger;
    private SuggestionFormatter mSuggestionFormatter;
    private TextAppearanceFactory mTextAppearanceFactory;
    private NamedTaskExecutor mIconLoaderExecutor;
    private HttpHelper mHttpHelper;
    private SearchBaseUrlHelper mSearchBaseUrlHelper;
    private SearchEngine mSearchEngine;

    /// M: Do not change feature on device which is not default.
    private static boolean sIsDeviceDefault = "default".equals(
            SystemProperties.get("ro.build.characteristics"));
    /*
     * M: This variable to identify whether shortcuts and suggestions show together or not.
     * If device is default, mix mode is false, else mix mode is true.
     */
    private static boolean sIsMixMode = !sIsDeviceDefault;
    /*
     * M: Whether can remove single shortcut or not. If device is default, then can be removed,
     * else can not.
     */
    private static boolean sCanRemoveShortcut = !sIsDeviceDefault;
    /// M: Use Setting Fragment or not
    private static boolean sUseSettingFragment = !sIsDeviceDefault;

    /// M:SMB default false, so in phone default does NOT
    // update corpora
    private static boolean sIsTabletUsedInOneSession = false;

    /// M:SMB
    public static boolean getTabletUsedInOneSessionFlag() {
        return sIsTabletUsedInOneSession;
    }

    /// M:SMB
    public static void setTabletUsedInOneSessionFlag(Object who) {
        if (who instanceof SearchActivityViewTwoPane) {
            sIsTabletUsedInOneSession = true;
            Log.d(TAG + ".SMB", "setTabletUsedInOneSessionFlag(), set in tablet");

            return;
        }

        if (who instanceof SearchActivityViewSinglePane) {
            sIsTabletUsedInOneSession = false;
            Log.d(TAG + ".SMB", "setTabletUsedInOneSessionFlag(), set in phone");

            return;
        }

        Log.i(TAG + ".SMB", "setTabletUsedInOneSessionFlag(), A illegal call!");
    }

    public QsbApplication(Context context) {
        // the application context does not use the theme from the <application> tag
        mContext = new ContextThemeWrapper(context, R.style.Theme_QuickSearchBox);
    }

    public static boolean isFroyoOrLater() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO;
    }

    public static boolean isHoneycombOrLater() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }

    public static QsbApplication get(Context context) {
        return ((QsbApplicationWrapper) context.getApplicationContext()).getApp();
    }

    protected Context getContext() {
        return mContext;
    }

    public int getVersionCode() {
        if (mVersionCode == 0) {
            try {
                PackageManager pm = getContext().getPackageManager();
                PackageInfo pkgInfo = pm.getPackageInfo(getContext().getPackageName(), 0);
                mVersionCode = pkgInfo.versionCode;
            } catch (PackageManager.NameNotFoundException ex) {
                // The current package should always exist, how else could we
                // run code from it?
                throw new RuntimeException(ex);
            }
        }
        return mVersionCode;
    }

    protected void checkThread() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalStateException("Accessed Application object from thread "
                    + Thread.currentThread().getName());
        }
    }

    protected void close() {
        checkThread();
        if (mConfig != null) {
            mConfig.close();
            mConfig = null;
        }
        if (mShortcutRepository != null) {
            mShortcutRepository.close();
            mShortcutRepository = null;
        }
        if (mSourceTaskExecutor != null) {
            mSourceTaskExecutor.close();
            mSourceTaskExecutor = null;
        }
        if (mSuggestionsProvider != null) {
            mSuggestionsProvider.close();
            mSuggestionsProvider = null;
        }
    }

    public synchronized Handler getMainThreadHandler() {
        if (mUiThreadHandler == null) {
            mUiThreadHandler = new Handler(Looper.getMainLooper());
        }
        return mUiThreadHandler;
    }

    public void runOnUiThread(Runnable action) {
        getMainThreadHandler().post(action);
    }

    public synchronized NamedTaskExecutor getIconLoaderExecutor() {
        if (mIconLoaderExecutor == null) {
            mIconLoaderExecutor = createIconLoaderExecutor();
        }
        return mIconLoaderExecutor;
    }

    protected NamedTaskExecutor createIconLoaderExecutor() {
        ThreadFactory iconThreadFactory = new PriorityThreadFactory(
                    Process.THREAD_PRIORITY_BACKGROUND);
        return new PerNameExecutor(SingleThreadNamedTaskExecutor.factory(iconThreadFactory));
    }

    /**
     * Indicates that construction of the QSB UI is now complete.
     */
    public void onStartupComplete() {
    }

    /**
     * Gets the QSB configuration object.
     * May be called from any thread.
     */
    public synchronized Config getConfig() {
        if (mConfig == null) {
            mConfig = createConfig();
        }
        return mConfig;
    }

    protected Config createConfig() {
        return new Config(getContext());
    }

    public synchronized SearchSettings getSettings() {
        if (mSettings == null) {
            mSettings = createSettings();
            mSettings.upgradeSettingsIfNeeded();
        }
        return mSettings;
    }

    protected SearchSettings createSettings() {
        return new SearchSettingsImpl(getContext(), getConfig());
    }

    /**
     * M: Set SearchEngine according favicon.
     * @param favicon the SearchEnginInfo favicon
     */
    public void setSearchEngine(String favicon) {
        try {
            String oldFavicon = "";
            if (mSearchEngine != null) {
                oldFavicon = mSearchEngine.getFaviconUri();
            }

            SearchEngineManager searchEngineManager = (SearchEngineManager) mContext
                    .getSystemService(Context.SEARCH_ENGINE_SERVICE);
            if (searchEngineManager != null) {
                mSearchEngine = searchEngineManager
                        .getBestMatch("", favicon) != null ? searchEngineManager
                        .getBestMatch("", favicon)
                        : searchEngineManager.getDefault();
            } else {
                Log.d(TAG, "setSearchEngine(), get SEARCH_ENGINE_SERVICE failed.");
            }

            // update SharedPreferences
            String currentfavicon = "";
            if (mSearchEngine != null) {
                currentfavicon = mSearchEngine.getFaviconUri();
            }

            if (!oldFavicon.equals(currentfavicon) && !"".equals(currentfavicon)) {
                SharedPreferences p = mContext.getSharedPreferences(
                        SearchSettingsImpl.PREFERENCES_NAME,
                        Context.MODE_PRIVATE);

                SharedPreferences.Editor editor = p.edit();
                editor.putString(SearchEngineItemsController.SEARCH_ENGINE_PREF,
                        currentfavicon);
                editor.commit();
            }
        } catch (IllegalArgumentException exception) {
            Log.e(TAG, "Cannot load search engine " + favicon, exception);
        }
    }

    /**
     * M: Get SearchEngine object.
     * @return the SearchEngine object
     */
    public SearchEngine getSearchEngine() {
        if (mSearchEngine == null) {
            createSearchEngine();
        }
        return mSearchEngine;
    }

    /**
     * M: Create SearchEngine and save info into SharedPreferences.
     */
    public void createSearchEngine() {
        SharedPreferences p =
                //PreferenceManager.getDefaultSharedPreferences(mContext);
        mContext.getSharedPreferences(SearchSettingsImpl.PREFERENCES_NAME, Context.MODE_PRIVATE);
        updateSearchEngine(p);
    }

    /**
     * M: Get search engines's info frome search manager service.
     * @param context the context to get SearchEngine
     * @return        the SearchEngine list of context
     */
    public static List<SearchEngine> getSearchEngines(Context context) {
        SearchEngineManager searchEngineManager = (SearchEngineManager) context
                .getSystemService(Context.SEARCH_ENGINE_SERVICE);
        return searchEngineManager.getAvailables();
    }

    /**
     * M: Update SearchEngine according new SharedPreference.
     * @param p new SharedPreferences object to update SearchEngine
     */
    public void updateSearchEngine(SharedPreferences p) {
        /// M: using getDefaultSearchEngineName for flexibility @{
        String searchEngineFavicon = p.getString(SearchEngineItemsController.SEARCH_ENGINE_PREF,
                getSettings().getDefaultSearchEngineFavicon());
        /// @}

        if (mSearchEngine == null
                || !mSearchEngine.getFaviconUri().equals(searchEngineFavicon)) {
            setSearchEngine(searchEngineFavicon);
        }
        if (DBG) {
            Log.i(TAG, "Selected search engine: " + mSearchEngine.getFaviconUri());
            Log.i(TAG, getSearchEngine().getFaviconUri());
        }
        //broadcastSearchEngineChanged();
    }

    /**
     * M: Update external SearchEngine.
     * @param p new SharedPreferences object to update SearchEngine
     * @param newSearchEngine the new SearchEngine name
     */
    public void updateSearchEngineExternal(SharedPreferences p, String newSearchEngine) {
        if (newSearchEngine == null) {
            /// M: The broadcast is from framework, Maybe the locale is changed.
            /// It should be to update the search engine, maybe in this language has
            /// not the before engine, then set the default search engine in this locale @{
            setSearchEngine(getSettings().getSavedSearchEngineFavicon());
            /// @}
            return;
        }

        if (mSearchEngine == null || !mSearchEngine.getName().equals(newSearchEngine)) {
            // newSearchEngine is search engine name, but save in preference use favicon
            String newFavicon = getSettings().getSearchEngineFaviconByName(newSearchEngine);
            setSearchEngine(newFavicon);
        }
        if (DBG) {
            Log.i(TAG, "Selected search engine: " + mSearchEngine.getFaviconUri());
            Log.i(TAG, getSearchEngine().getFaviconUri());
        }
        //broadcastSearchEngineChanged();
    }

    /**
     * Gets all corpora.
     *
     * May only be called from the main thread.
     */
    public Corpora getCorpora() {
        checkThread();
        if (mCorpora == null) {
            mCorpora = createCorpora(getSources());
        }
        return mCorpora;
    }

    protected Corpora createCorpora(Sources sources) {
        SearchableCorpora corpora = new SearchableCorpora(getContext(), getSettings(), sources,
                createCorpusFactory());
        corpora.update();
        return corpora;
    }

    /**
     * Updates the corpora, if they are loaded.
     * May only be called from the main thread.
     */
    public void updateCorpora() {
        checkThread();
        if (mCorpora != null) {
            mCorpora.update();
        }
    }

    protected Sources getSources() {
        checkThread();
        if (mSources == null) {
            mSources = createSources();
        }
        return mSources;
    }

    protected Sources createSources() {
        return new SearchableSources(getContext(), getMainThreadHandler(),
                getIconLoaderExecutor(), getConfig());
    }

    protected CorpusFactory createCorpusFactory() {
        int numWebCorpusThreads = getConfig().getNumWebCorpusThreads();
        return new SearchableCorpusFactory(getContext(), getConfig(), getSettings(),
                createExecutorFactory(numWebCorpusThreads));
    }

    protected Factory<Executor> createExecutorFactory(final int numThreads) {
        final ThreadFactory threadFactory = getQueryThreadFactory();
        return new Factory<Executor>() {
            public Executor create() {
                return Executors.newFixedThreadPool(numThreads, threadFactory);
            }
        };
    }

    /**
     * Gets the corpus ranker.
     * May only be called from the main thread.
     */
    public CorpusRanker getCorpusRanker() {
        checkThread();
        if (mCorpusRanker == null) {
            mCorpusRanker = createCorpusRanker();
        }
        return mCorpusRanker;
    }

    protected CorpusRanker createCorpusRanker() {
        return new DefaultCorpusRanker(getCorpora(), getShortcutRepository());
    }

    /**
     * Gets the shortcut repository.
     * May only be called from the main thread.
     */
    public ShortcutRepository getShortcutRepository() {
        checkThread();
        if (mShortcutRepository == null) {
            mShortcutRepository = createShortcutRepository();
        }
        return mShortcutRepository;
    }

    protected ShortcutRepository createShortcutRepository() {
        ThreadFactory logThreadFactory =
                new ThreadFactoryBuilder()
                .setNameFormat("ShortcutRepository #%d")
                .setThreadFactory(new PriorityThreadFactory(
                        Process.THREAD_PRIORITY_BACKGROUND))
                .build();
        Executor logExecutor = Executors.newSingleThreadExecutor(logThreadFactory);
        return ShortcutRepositoryImplLog.create(getContext(), getConfig(), getCorpora(),
            getShortcutRefresher(), getMainThreadHandler(), logExecutor);
    }

    /**
     * Gets the shortcut refresher.
     * May only be called from the main thread.
     */
    public ShortcutRefresher getShortcutRefresher() {
        checkThread();
        if (mShortcutRefresher == null) {
            mShortcutRefresher = createShortcutRefresher();
        }
        return mShortcutRefresher;
    }

    protected ShortcutRefresher createShortcutRefresher() {
        // For now, ShortcutRefresher gets its own SourceTaskExecutor
        return new SourceShortcutRefresher(createSourceTaskExecutor());
    }

    /**
     * Gets the source task executor.
     * May only be called from the main thread.
     */
    public NamedTaskExecutor getSourceTaskExecutor() {
        checkThread();
        if (mSourceTaskExecutor == null) {
            mSourceTaskExecutor = createSourceTaskExecutor();
        }
        return mSourceTaskExecutor;
    }

    protected NamedTaskExecutor createSourceTaskExecutor() {
        ThreadFactory queryThreadFactory = getQueryThreadFactory();
        return new PerNameExecutor(SingleThreadNamedTaskExecutor.factory(queryThreadFactory));
    }

    /**
     * Gets the query thread factory.
     * May only be called from the main thread.
     */
    protected ThreadFactory getQueryThreadFactory() {
        checkThread();
        if (mQueryThreadFactory == null) {
            mQueryThreadFactory = createQueryThreadFactory();
        }
        return mQueryThreadFactory;
    }

    protected ThreadFactory createQueryThreadFactory() {
        String nameFormat = "QSB #%d";
        int priority = getConfig().getQueryThreadPriority();
        return new ThreadFactoryBuilder()
                .setNameFormat(nameFormat)
                .setThreadFactory(new PriorityThreadFactory(priority))
                .build();
    }

    /**
     * Gets the suggestion provider.
     *
     * May only be called from the main thread.
     */
    protected SuggestionsProvider getSuggestionsProvider() {
        checkThread();
        if (mSuggestionsProvider == null) {
            mSuggestionsProvider = createSuggestionsProvider();
        }
        return mSuggestionsProvider;
    }

    protected SuggestionsProvider createSuggestionsProvider() {
        return new SuggestionsProviderImpl(getConfig(),
              getSourceTaskExecutor(),
              getMainThreadHandler(),
              getLogger());
    }

    /**
     * Gets the default suggestion view factory.
     * May only be called from the main thread.
     */
    public SuggestionViewFactory getSuggestionViewFactory() {
        checkThread();
        if (mSuggestionViewFactory == null) {
            mSuggestionViewFactory = createSuggestionViewFactory();
        }
        return mSuggestionViewFactory;
    }

    protected SuggestionViewFactory createSuggestionViewFactory() {
        return new DefaultSuggestionViewFactory(getContext());
    }

    public Promoter createBlendingPromoter() {
        return new ShortcutPromoter(getConfig(),
                new RankAwarePromoter(getConfig(), null, null), null);
    }

    public Promoter createSingleCorpusPromoter(Corpus corpus) {
        /// M: If the mix mode is false, then limit shortcut number @{
        if (sIsMixMode) {
            return new SingleCorpusPromoter(corpus, Integer.MAX_VALUE);
        } else {
            return new SingleCorpusPromoter(corpus, getConfig().getShortcutsOnlyLimitedCount());
        }
        /// @}
    }

    public Promoter createSingleCorpusResultsPromoter(Corpus corpus) {
        return new SingleCorpusResultsPromoter(corpus, Integer.MAX_VALUE);
    }

    public Promoter createWebPromoter() {
        return new WebPromoter(getConfig().getMaxShortcutsPerWebSource());
    }

    public Promoter createResultsPromoter() {
        SuggestionFilter resultFilter = new ResultFilter();
        return new ShortcutPromoter(getConfig(), null, resultFilter);
    }

    /**
     * Gets the Google source.
     * May only be called from the main thread.
     */
    public GoogleSource getGoogleSource() {
        checkThread();
        if (mGoogleSource == null) {
            mGoogleSource = createGoogleSource();
        }
        return mGoogleSource;
    }

    protected GoogleSource createGoogleSource() {
        return new GoogleSuggestClient(getContext(), getMainThreadHandler(),
                getIconLoaderExecutor(), getConfig());
    }

    /**
     * Gets Voice Search utilities.
     */
    public VoiceSearch getVoiceSearch() {
        checkThread();
        if (mVoiceSearch == null) {
            mVoiceSearch = createVoiceSearch();
        }
        return mVoiceSearch;
    }

    protected VoiceSearch createVoiceSearch() {
        return new VoiceSearch(getContext());
    }

    /**
     * Gets the event logger.
     * May only be called from the main thread.
     */
    public Logger getLogger() {
        checkThread();
        if (mLogger == null) {
            mLogger = createLogger();
        }
        return mLogger;
    }

    protected Logger createLogger() {
        return new EventLogLogger(getContext(), getConfig());
    }

    public SuggestionFormatter getSuggestionFormatter() {
        if (mSuggestionFormatter == null) {
            mSuggestionFormatter = createSuggestionFormatter();
        }
        return mSuggestionFormatter;
    }

    protected SuggestionFormatter createSuggestionFormatter() {
        return new LevenshteinSuggestionFormatter(getTextAppearanceFactory());
    }

    public TextAppearanceFactory getTextAppearanceFactory() {
        if (mTextAppearanceFactory == null) {
            mTextAppearanceFactory = createTextAppearanceFactory();
        }
        return mTextAppearanceFactory;
    }

    protected TextAppearanceFactory createTextAppearanceFactory() {
        return new TextAppearanceFactory(getContext());
    }

    public PreferenceControllerFactory createPreferenceControllerFactory(Activity activity) {
        return new PreferenceControllerFactory(getSettings(), activity);
    }

    public synchronized HttpHelper getHttpHelper() {
        if (mHttpHelper == null) {
            mHttpHelper = createHttpHelper();
        }
        return mHttpHelper;
    }

    protected HttpHelper createHttpHelper() {
        return new JavaNetHttpHelper(
                new JavaNetHttpHelper.PassThroughRewriter(),
                getConfig().getUserAgent());
    }

    public synchronized SearchBaseUrlHelper getSearchBaseUrlHelper() {
        if (mSearchBaseUrlHelper == null) {
            mSearchBaseUrlHelper = createSearchBaseUrlHelper();
        }

        return mSearchBaseUrlHelper;
    }

    protected SearchBaseUrlHelper createSearchBaseUrlHelper() {
        // This cast to "SearchSettingsImpl" is somewhat ugly.
        return new SearchBaseUrlHelper(getContext(), getHttpHelper(),
                getSettings(), ((SearchSettingsImpl)getSettings()).getSearchPreferences());
    }

    public Help getHelp() {
        // No point caching this, it's super cheap.
        return new Help(getContext(), getConfig());
    }

    /**
     * M: Whether the Corpus is enabled or not.
     * @param name the Corpus name
     * @return     if the Corpus is enable return true, else return false
     */
    public boolean isCorpusEnabled(String name) {
        List<Corpus> corpora = mCorpora.getEnabledCorpora();
        for (Corpus corpus : corpora) {
            if (corpus.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * M: Get the mode to identify how to show shortcuts and suggestions.
     * @return if true then show together, else show separately.
     */
    public static boolean isMixMode() {
        return sIsMixMode;
    }

    /**
     * M: Get the variable whether can remove single shortcut or not.
     * @return if true then can remove single shortcut, else not
     */
    public static boolean canRemoveShortcut() {
        return sCanRemoveShortcut;
    }

    /**
     * M: When show search setting activity, use fragment or activity.
     * @return if use fragment return true, else return false. Default return true.
     */
    public static boolean useSettingFragment() {
        return sUseSettingFragment;
    }

}
