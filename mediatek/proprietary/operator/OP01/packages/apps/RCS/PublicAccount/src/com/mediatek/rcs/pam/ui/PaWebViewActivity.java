package com.mediatek.rcs.pam.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.mediatek.rcs.pam.Constants;
import com.mediatek.rcs.pam.R;

public class PaWebViewActivity extends Activity {
    public static final String TAG = Constants.TAG_PREFIX + "PaWebViewActivity";
    public static final String KEY_WEB_LINK = "key_web_link";
    public static final String KEY_FORWARDABLE = "key_forwardable";

    private Menu mMenu;
    private String mLink;
    private WebView mWebView;
    private ProgressBar mProgressbar;
    private boolean mForwardable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pa_web_view);

        mLink = getIntent().getStringExtra(KEY_WEB_LINK);
        mForwardable = getIntent().getBooleanExtra(KEY_FORWARDABLE, true);
        Log.i(TAG, "Open link " + mLink);

        initWebView();

        if (savedInstanceState != null) {
            Log.i(TAG, "Restore saved instance to web view.");
            mWebView.restoreState(savedInstanceState);
        }

        // disable application icon from ActionBar
        getActionBar().setDisplayShowHomeEnabled(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.pa_web_view, menu);
        mMenu = menu;
        if (!mForwardable) {
            mMenu.removeItem(R.id.action_forward);
            Log.d(TAG, "remove forward option menu");
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_close) {
            this.finish();
            return true;
        } else if (id == R.id.action_copy_url) {
            pomptUser(getString(R.string.action_copy_url) + mWebView.getUrl());
            copyUrlToClipBoard();
            return true;
        } else if (id == R.id.action_forward) {
            String content = mWebView.getTitle() + mWebView.getUrl();
            pomptUser(getString(R.string.action_forward) + content);
            forwardMessage(content);
            return true;
        } else if (id == R.id.action_refresh) {
            pomptUser(getString(R.string.action_refresh));
            mWebView.reload();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.i(TAG, "[onSaveInstanceState]");
        mWebView.saveState(outState);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initWebView() {
        mProgressbar = (ProgressBar) findViewById(R.id.pb_webview_load);
        mWebView = (WebView) findViewById(R.id.main_webview);

        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        mWebView.setWebChromeClient(new WebChromeClient() {

            @Override
            public void onProgressChanged(WebView view, int progress) {
                Log.i(TAG,
                        "progress " + progress + ", title is "
                                + view.getTitle());
                updateTitle(view.getTitle());

                if (progress == 100) {
                    mProgressbar.setVisibility(View.GONE);
                } else {
                    if (mProgressbar.getVisibility() != View.VISIBLE) {
                        mProgressbar.setVisibility(View.VISIBLE);
                    }
                    mProgressbar.setProgress(progress);
                }
            }
        });

        // Force links and redirects to open in the WebView instead of in a
        // browser
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                mMenu.findItem(R.id.action_refresh).setVisible(false);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                mMenu.findItem(R.id.action_refresh).setVisible(true);
            }
        });

        mWebView.loadUrl(mLink);
    }

    private void copyUrlToClipBoard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData data = ClipData.newPlainText("Url link", mWebView.getUrl());
        clipboard.setPrimaryClip(data);
    }

    private void forwardMessage(String content) {
        Log.i(TAG, "forwardMessage " + content);
        Intent intent = new Intent();
        intent.putExtra("forwarded_message", true);
        intent.putExtra("forwarded_ip_message", true);
        intent.setAction("android.intent.action.ACTION_RCS_MESSAGING_SEND");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, content);
        startActivity(intent);
    }

    private void pomptUser(String text) {
        Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT)
                .show();
    }

    private void updateTitle(final String title) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                getActionBar().setTitle(title);
            }
        });
    }

    public static void openHyperLink(Context context, String link) {
        Intent intent = new Intent(context, PaWebViewActivity.class);
        intent.putExtra(KEY_WEB_LINK, link);
        context.startActivity(intent);
    }

    public static void openHyperLink(Context context, String link,
            boolean forwardable) {
        Intent intent = new Intent(context, PaWebViewActivity.class);
        intent.putExtra(KEY_WEB_LINK, link);
        intent.putExtra(KEY_FORWARDABLE, forwardable);
        context.startActivity(intent);
    }
}
