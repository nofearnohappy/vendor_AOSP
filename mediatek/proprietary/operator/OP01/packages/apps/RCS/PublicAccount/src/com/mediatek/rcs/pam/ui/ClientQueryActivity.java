package com.mediatek.rcs.pam.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.mediatek.rcs.pam.Constants;
import com.mediatek.rcs.pam.IPAServiceCallback.Stub;
import com.mediatek.rcs.pam.PAService;
import com.mediatek.rcs.pam.R;
import com.mediatek.rcs.pam.SimpleServiceCallback;
import com.mediatek.rcs.pam.client.AsyncUIPAMClient;
import com.mediatek.rcs.pam.client.AsyncUIPAMClient.SimpleCallback;
import com.mediatek.rcs.pam.model.MessageContent;
import com.mediatek.rcs.pam.model.PublicAccount;
import com.mediatek.rcs.pam.model.ResultCode;

import java.util.List;

/**
 * This class is an abstract class, which implement a ListView to show search
 * results, a handler to enqueue search actions on a different thread and the
 * common actions to do search.
 */
public abstract class ClientQueryActivity extends Activity {

    protected String mTag;
    protected int mPageNum = 0;

    // delay to trigger search (set > 0 when debug)
    protected static final int DELAY_SEARCH = 0;

    // timeout to connect service: 10s
    private static final int CONNECTING_TIMEOUT = 10 * 1000;

    protected enum LoadState {
        FREE, ONFIRST, ONMORE
    };

    protected LoadState mLoadingState = LoadState.FREE;

    protected ProgressBar mOuterProgressBar;
    private RelativeLayout mFooterProgressView;

    protected Context mContext;
    protected ListView mSearchListView;

    private PAService mService;
    private long mToken = Constants.INVALID;

    protected AsyncUIPAMClient mAsyncUIPAMClient;
    protected int mExceptionCode = 0;

    // Variable to remember the round of search (only process current round's
    // request)
    protected int mUniqueSearchRound = 0;

    protected boolean mIsActivityForeground = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutId());
        mFooterProgressView = (RelativeLayout) getLayoutInflater().inflate(
                R.layout.item_progress_load, null);

        mContext = this;
        mAsyncUIPAMClient = new AsyncUIPAMClient(mContext, mPAClientCallBack);
        mTag = getLogTag();

        initPAService();
        initBasicViews();

        // disable application icon from ActionBar
        getActionBar().setDisplayShowHomeEnabled(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsActivityForeground = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        mIsActivityForeground = false;
    }

    /*
     * Disconnect the service when quit
     */
    @Override
    protected void onDestroy() {
        Log.i(mTag, "onDestroy");
        mUniqueSearchRound = 0;
        if (mService != null) {
            mService.unregisterCallback(mToken);
        }
        super.onDestroy();
    }

    /*
     * Whether activity is running in foreground
     */
    protected boolean isActivityForeground() {
        return mIsActivityForeground;
    };

    /*
     * Get the UI layout id
     */
    protected abstract int getLayoutId();

    /*
     * Get the log tag for subclass
     */
    protected abstract String getLogTag();

    /*
     * Init service
     */
    protected void initPAService() {
        final Stub callback = new SimpleServiceCallback() {

            @Override
            public void onServiceConnected() throws RemoteException {
                Log.i(mTag, "onServiceConnected");
                runOnUiThread(new Runnable() {
                    public void run() {
                        doWhenConnected();
                    }
                });
            }

            @Override
            public void onServiceDisconnected(final int reason)
                    throws RemoteException {
                Log.i(mTag, "onServiceDisconnected");
                runOnUiThread(new Runnable() {
                    public void run() {
                        doWhenDisConnected(reason);
                    }
                });
            }
        };

        PAService.init(mContext, new PAService.ServiceConnectNotify() {

            @Override
            public void onServiceConnected() {
                Log.i(mTag, "onServiceConnectedsss");
                mService = PAService.getInstance();
                mToken = mService.registerCallback(callback, false);
                mService.registerAck(mToken);
            }
        });
    }

    /*
     * Whether service is connected
     */
    protected boolean isServiceConnected() {
        if (mService != null) {
            return mService.isServiceConnected(mToken);
        } else {
            return false;
        }

    }

    /*
     * Hide outer progress bar when service is connected
     */
    protected void doWhenConnected() {
        hideOuterProgressBar();
    }

    /*
     * Finish activity when service is disconnected
     */
    protected void doWhenDisConnected(int reason) {
        Log.i(mTag, "doWhenDisConnected, reason is " + reason);
        if (reason == PAService.INTERNAL_ERROR) {
            finish();
        } else {
            Toast.makeText(mContext, R.string.text_service_not_connect,
                    Toast.LENGTH_SHORT).show();
            showOuterProgressBar();
        }
    }

    /*
     * Init basic views in onCreate(Bundle)
     */
    protected void initBasicViews() {
        mSearchListView.setOnScrollListener(new OnScrollListener() {
            private Boolean mIsDividePage = false;

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                // if scroll to end and release, load more data
                if (mIsDividePage
                        && scrollState == OnScrollListener.SCROLL_STATE_IDLE) {
                    startMoreQuery();
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem,
                    int visibleItemCount, int totalItemCount) {
                mIsDividePage = (firstVisibleItem + visibleItemCount == totalItemCount);
            }
        });
    }

    private SimpleCallback mPAClientCallBack = new SimpleCallback() {
        @Override
        public void reportSearchResult(long requestId, int resultCode,
                List<PublicAccount> results) {
            doWhenComplete(requestId, resultCode, results);
        }

        @Override
        public void reportGetRecommendsResult(long requestId, int resultCode,
                List<PublicAccount> results) {
            doWhenComplete(requestId, resultCode, results);
        }

        @Override
        public void reportGetMessageHistoryResult(long requestId,
                int resultCode, List<MessageContent> results) {
            doWhenComplete(requestId, resultCode, results);
        }

        public void doWhenComplete(long requestId, int resultCode,
                List<?> results) {
            Log.i(mTag,
                    "[doWhenComplete], resultCode = "
                            + resultCode
                            + ", List size = "
                            + (results == null ? "null" : Integer
                                    .toString(results.size())));
            if (resultCode != ResultCode.SUCCESS) {
                mPageNum--;
                if (resultCode == ResultCode.SYSTEM_ERROR_NETWORK) {
                    showToastIfForeground(R.string.text_network_error);
                } else {
                    showToastIfForeground(R.string.text_exception_happend);
                }
            }

            if (mLoadingState == LoadState.ONFIRST) {
                hideOuterProgressBar();
                hideSearchHintView();
                if (resultCode == ResultCode.SUCCESS) {
                    mPageNum++;
                    if (results.size() < 1) {
                        showNoSearchResult();
                    } else {
                        hideNoSearchResult();
                        updateListonUI(results);
                    }
                }
            } else if (mLoadingState == LoadState.ONMORE) {
                hideFooterProgressBar();
                if (resultCode == ResultCode.SUCCESS) {
                    mPageNum++;
                    if (results.size() < 1) {
                        showToastIfForeground(R.string.text_no_more_result);
                    } else {
                        updateListonUI(results);
                    }
                }
            } else {
                throw new IllegalStateException();
            }

            mLoadingState = LoadState.FREE;
        }
    };

    /*
     * Trigger the first query & update state (loading, progress bar)
     */
    protected void startFirstQuery() {
        Log.i(mTag, "[startFirstQuery], LoadingState " + mLoadingState);
        if (mLoadingState == LoadState.FREE) {
            mPageNum = 0;
            mLoadingState = LoadState.ONFIRST;
            showOuterProgressBar();
            mPageNum++;
            searchDataViaClient();
        }
    }

    /*
     * Trigger more query & update state (loading, progress bar)
     */
    protected void startMoreQuery() {
        Log.i(mTag, "[startMoreQuery], LoadingState " + mLoadingState);
        if (mLoadingState == LoadState.FREE) {
            mLoadingState = LoadState.ONMORE;
            showFooterProgressBar();
            mPageNum++;
            searchDataViaClient();
        }
    }

    /*
     * Update the search result list view
     */
    protected abstract void updateListonUI(List<?> listSearch);

    /*
     * Search data via client
     */
    protected abstract void searchDataViaClient();

    protected void doWhenResultNormal(List<?> list) {
        Log.i(mTag, "size is " + list.size());
    }

    /*
     * Show no search result view
     */
    protected abstract void hideNoSearchResult();

    /*
     * hide no search result view
     */
    protected abstract void showNoSearchResult();

    /*
     * hide the search hint view (optional)
     */
    protected void hideSearchHintView() {
        // do nothing here
    }

    /*
     * Show the outer progress bar view
     */
    protected void showOuterProgressBar() {
        mOuterProgressBar.setVisibility(View.VISIBLE);
        mOuterProgressBar.bringToFront();
    }

    /*
     * hide the outer progress bar view
     */
    protected void hideOuterProgressBar() {
        mOuterProgressBar.setVisibility(View.GONE);
    }

    /*
     * Show the footer progress bar view
     */
    @SuppressLint("InflateParams")
    private void showFooterProgressBar() {
        Log.i(mTag, "showFooterProgressBar");
        // inflate() each time, or else progress display still when
        // 1. backToInit when load more -> 2. startFirstQuery ->
        // 3.startMoreQuery
        mSearchListView.addFooterView(mFooterProgressView);
    }

    /*
     * Hide the footer progress bar view
     */
    protected void hideFooterProgressBar() {
        mSearchListView.removeFooterView(mFooterProgressView);
    }

    /*
     * Show the search result list view
     */
    protected void showSearchListView() {
        mSearchListView.setVisibility(View.VISIBLE);
    }

    /*
     * Hide the search result list view
     */
    protected void hideSearchListView() {
        mSearchListView.setVisibility(View.GONE);
    }

    protected void showToastIfForeground(int resId) {
        String text = mContext.getString(resId);
        showToastIfForeground(text);
    }

    protected void showToastIfForeground(String text) {
        if (isActivityForeground()) {
            Log.i(mTag, "Show toast " + text);
            Toast.makeText(mContext, text, Toast.LENGTH_SHORT).show();
        } else {
            Log.i(mTag, "Not foreground, hide toast " + text);
        }
    }
}
