package com.mediatek.rcs.pam.ui.accountlist;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.TextView;
import android.widget.Toast;

import com.mediatek.rcs.pam.Constants;
import com.mediatek.rcs.pam.R;
import com.mediatek.rcs.pam.model.PublicAccount;
import com.mediatek.rcs.pam.ui.ClientQueryActivity;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@SuppressLint("InflateParams")
public class AccountSearchActivity extends ClientQueryActivity {

    // default size per request (in [0,50])
    private static final int DEFAULT_PAGESIZE = 10;

    private String mKey;

    // 0: by followed time in reverse order (default) 1: by account name
    private int mOrder = Constants.ORDER_BY_REVERSED_TIMESTAMP;
    private int mPageSize = DEFAULT_PAGESIZE;

    private SearchView mSearchView;
    private TextView mHintTextView;
    private TextView mNoResultTextView;

    private AccountSearchAdapter mSearchAdapter;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.account_search, menu);
        MenuItem menuItem = menu.findItem(R.id.search_button_item);

        mSearchView = (SearchView) menuItem.getActionView();
        mSearchView
                .setQueryHint(getString(R.string.hint_search_public_accounts));
        mSearchView.setIconifiedByDefault(false);
        mSearchView.requestFocus();

        mSearchView.setOnQueryTextListener(new OnQueryTextListener() {
            private static final int MAX_LENGTH = 30;

            @Override
            public boolean onQueryTextSubmit(String query) {
                startFirstQuery();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mKey = newText;
                Log.i(mTag, "length of text is " + mKey.length());

                if (mKey.length() > 0 && mKey.length() <= MAX_LENGTH) {
                    mSearchView.setSubmitButtonEnabled(true);
                } else if (mKey.length() > MAX_LENGTH) {
                    String hint = getResources().getString(
                            R.string.hint_max_length, MAX_LENGTH);
                    Toast.makeText(mContext, hint, Toast.LENGTH_SHORT).show();
                    mSearchView.setQuery(mKey.substring(0, MAX_LENGTH), false);
                } else {
                    mSearchView.setSubmitButtonEnabled(false);
                    backToInit();
                }
                return false;
            }
        });

        showKeyboard();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            hideKeyboard();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {
        hideKeyboard();
        super.onStop();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_account_search;
    }

    @Override
    protected String getLogTag() {
        return Constants.TAG_PREFIX + "AccountSearchActivity";
    }

    @Override
    protected void initBasicViews() {
        mHintTextView = (TextView) findViewById(R.id.tv_hint_to_search);

        mNoResultTextView = (TextView) findViewById(R.id.tv_no_accounts_found);
        mNoResultTextView.setVisibility(View.GONE);

        mOuterProgressBar = (ProgressBar) findViewById(R.id.pb_account_search);
        mOuterProgressBar.setVisibility(View.GONE);

        mSearchListView = (ListView) findViewById(R.id.lv_search_result);
        mSearchListView.setVisibility(View.GONE);

        mSearchAdapter = new AccountSearchAdapter(mContext,
                new ArrayList<PublicAccount>());
        mSearchListView.setAdapter(mSearchAdapter);
        super.initBasicViews();
    }

    @Override
    protected void searchDataViaClient() {
        mAsyncUIPAMClient.search(mKey, mOrder, mPageSize, mPageNum);
    }

    @Override
    protected void doWhenResultNormal(List<?> list) {
        super.doWhenResultNormal(list);

        // must ensure callback of searchDataViaClient() return the right type
        @SuppressWarnings("unchecked")
        List<PublicAccount> searchList = (List<PublicAccount>) list;
        sortAccountViaTitle(searchList);
    }

    // sort search result via title
    private void sortAccountViaTitle(List<PublicAccount> list) {
        Comparator<PublicAccount> accountComparator = new Comparator<PublicAccount>() {
            Collator collator = Collator.getInstance(Locale.CHINESE);

            @Override
            public int compare(PublicAccount lhs, PublicAccount rhs) {
                return collator.compare(lhs.name, rhs.name);
            }
        };
        Collections.sort(list, accountComparator);
    }

    @Override
    protected void startFirstQuery() {
        // Not trigger search until service is connected
        if (!isServiceConnected()) {
            String text = getResources().getString(
                    R.string.text_service_not_connect);
            Toast.makeText(mContext, text, Toast.LENGTH_SHORT).show();
            return;
        }

        if (mLoadingState == LoadState.FREE) {
            hideKeyboard();
        }

        // super method modify mIsLoading, invoke below
        super.startFirstQuery();
    }

    @Override
    protected void startMoreQuery() {
        if (mLoadingState == LoadState.FREE) {
            hideKeyboard();
        }
        // super method modify mIsLoading, invoke below
        super.startMoreQuery();
    }

    private void backToInit() {
        mUniqueSearchRound++;
        Log.i(mTag, "backToInit, swith round to " + mUniqueSearchRound);

        mLoadingState = LoadState.FREE;
        hideOuterProgressBar();
        hideFooterProgressBar();
        showSearchHintView();
    }

    @Override
    protected void updateListonUI(List<?> listSearch) {
        final List<PublicAccount> dataList;
        // must ensure callback of searchDataViaClient() return the right type
        @SuppressWarnings("unchecked")
        List<PublicAccount> searchList = (List<PublicAccount>) listSearch;
        if (mLoadingState == LoadState.ONMORE) {
            dataList = mSearchAdapter.getAccountList();
            Log.i(mTag, "[updateListonUI] old size is " + dataList.size());
            dataList.addAll(searchList);
        } else {
            dataList = searchList;
        }

        mSearchListView.setVisibility(View.VISIBLE);
        mSearchListView.requestLayout();
        mSearchAdapter.setAccountList(dataList);
        Log.i(mTag, "Refresh list, new size " + dataList.size());

        if (mLoadingState == LoadState.ONFIRST) {
            Log.i(mTag, "Scroll to top of listview");
            mSearchListView.smoothScrollToPosition(0);
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mSearchView.getWindowToken(), 0);
    }

    private void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,
                InputMethodManager.HIDE_IMPLICIT_ONLY);
    }

    private void showSearchHintView() {
        mHintTextView.setVisibility(View.VISIBLE);
        mSearchListView.setVisibility(View.GONE);
        mNoResultTextView.setVisibility(View.GONE);
    }

    @Override
    protected void hideSearchHintView() {
        mHintTextView.setVisibility(View.GONE);
    }

    @Override
    protected void showNoSearchResult() {
        mNoResultTextView.setVisibility(View.VISIBLE);
        mSearchListView.setVisibility(View.GONE);
    }

    @Override
    protected void hideNoSearchResult() {
    }

    @Override
    protected void doWhenConnected() {
        super.doWhenConnected();
    }
}
