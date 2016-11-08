package com.hesine.nmsg.ui;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.hesine.nmsg.R;
import com.hesine.nmsg.business.Pipe;
import com.hesine.nmsg.business.bean.ImageInfo;
import com.hesine.nmsg.business.bean.ServiceInfo;
import com.hesine.nmsg.business.bo.ImageWorker;
import com.hesine.nmsg.business.dao.DBUtils;
import com.hesine.nmsg.common.CommonUtils;
import com.hesine.nmsg.common.DeviceInfo;
import com.hesine.nmsg.common.EnumConstants;
import com.hesine.nmsg.common.FileEx;
import com.hesine.nmsg.common.MLog;

public class DetailActivity extends Activity implements Pipe {
    private Context mContext = null;
    private WebView mWebView = null;
    private String imageUrl = null;
    final Activity context = this;
    private View mProgressBar = null;
    private TextView mEmptyView = null;
    private WebChromeClientExt xwebchromeclient = new WebChromeClientExt();
    private View xCustomView;
    private HeaderView mHeader = null;
    private PopMenu popMenu;
    private ServiceInfo serviceInfo;
    private String msgId = null;
    private int msgSubId = -1;
    private String serviceInfoAccount;
    private long mThreadId = 0;
    private String mUrl = null;
    private String mShortUrl = null;
    private LinearLayout mLayout = null;
    private static final int SET_EMPTY_GONE = 0;
    private boolean setEmptyGone = true;
    private String mSubject = null;
    private String title;

    private ImageWorker workerForSave = null;
    private ImageWorker workerForShow = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        setOverflowShowingAlways();
        mContext = this;
        initViews();
        Intent intent = getIntent();
        serviceInfoAccount = intent.getExtras().getString("user_account");
        msgId = intent.getStringExtra("msgId");
        msgSubId = intent.getIntExtra("msgSubId", -1);
        mThreadId = intent.getLongExtra("thread_id", 0);
        mUrl = intent.getStringExtra("URL");
        mShortUrl = intent.getStringExtra("shortUrl");
        mSubject = intent.getStringExtra("subject");
        if (serviceInfoAccount == null) {
            MLog.error("serviceInfoAccount == null ");
            finish();
        }
        serviceInfo = DBUtils.getServiceInfo(serviceInfoAccount);
        if (serviceInfo == null) {
            MLog.error("serviceInfo == null ");
            finish();
        }

        title = intent.getStringExtra("Title");
        initHeader(title);
        initWebView(mUrl);

        popMenu = new PopMenu(this);
        popMenu.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0: // share
                        showShareDialog();
                        break;
                    case 1:
                        viewContact();
                        break;
                    case 2:
                        addContact(serviceInfo);
                        break;
                    default:
                        break;
                }
                popMenu.dismiss();
            }
        });

        workerForSave = new ImageWorker();
        workerForSave.setListener(this);
        workerForShow = new ImageWorker();
        workerForShow.setListener(this);
    }

    private void initViews() {
        mProgressBar = (View) findViewById(R.id.progress_bar);
        mEmptyView = (TextView) findViewById(R.id.empty_view);
        mEmptyView.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                mWebView.reload();
            }
        });
    }

    private void checkNetwork() {
        if (!DeviceInfo.isNetworkReady(this)) {
            Toast.makeText(this, R.string.network_unavailable, Toast.LENGTH_SHORT).show();
            mProgressBar.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.VISIBLE);
        } else {
            mWebView.reload();
        }
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("SetJavaScriptEnabled")
    private void initWebView(String url) {
        mLayout = (LinearLayout) findViewById(R.id.activity_detail);
        mWebView = (WebView) findViewById(R.id.wv_main);
        WebSettings settings = mWebView.getSettings();
        settings.setUseWideViewPort(true);
        settings.setAllowFileAccess(true);
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSavePassword(true);
        settings.setSaveFormData(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setJavaScriptEnabled(true);
        mWebView.setInitialScale(0);
        mWebView.setWebViewClient(new MyWebViewClient());
        mWebView.addJavascriptInterface(new MyJavascriptInterface(), "imagelistner");
        mWebView.setWebChromeClient(xwebchromeclient);
        mWebView.setDownloadListener(new WebViewDownLoadListener());
        mWebView.loadUrl(url);
        ((Activity) mContext).registerForContextMenu(mWebView);
        this.registerForContextMenu(mWebView);
    }

    private void initHeader(String title) {
        mHeader = (HeaderView) findViewById(R.id.header);
        mHeader.setTitle(title);
        mHeader.setMoreView(R.drawable.actionbar_more_icon);
        mHeader.setBackRsc(R.drawable.browser_back);
        mHeader.getMoreView().setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onKeyDown(KeyEvent.KEYCODE_MENU, null);
                if (popMenu != null) {
                    popMenu.showAsDropDown(mHeader.getMoreView());
                }
            }
        });
        mHeader.getBackView().setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View view) {
                if (mWebView.canGoBack()) {
                    mWebView.goBack();
                } else {
                    finish();
                }
            }
        });
    }

    private void startLoading() {
        mProgressBar.setVisibility(View.VISIBLE);
    }

    private void stopLoading() {
        mProgressBar.setVisibility(View.GONE);
        LayoutParams lp = mProgressBar.getLayoutParams();
        lp.width = 4;
        mProgressBar.setLayoutParams(lp);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.browser_nemu, menu);
        return super.onPrepareOptionsMenu(menu);
    }

    private void addContact(final ServiceInfo si) {
        new Thread(new Runnable() {

            @Override
            public void run() {
                if (CommonUtils.isExistSystemContactViaAccount(si, true)) {
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            Toast.makeText(DetailActivity.this, R.string.add_contact_exist,
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    if (CommonUtils.addContactInPhonebook(si)) {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(DetailActivity.this, R.string.add_contact_success,
                                        Toast.LENGTH_LONG).show();
                            }
                        });
                    } else {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(DetailActivity.this, R.string.add_contact_fail,
                                        Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLayout.removeView(mWebView);
        mWebView.removeAllViews();
        mWebView.destroy();
        mHandler.removeMessages(SET_EMPTY_GONE);
        System.exit(0);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.menu_share:
                showShareDialog();
                break;

            case R.id.menu_add_contact:
                addContact(serviceInfo);
                break;

            case R.id.menu_view_contact:
                viewContact();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        WebView.HitTestResult result = ((WebView) v).getHitTestResult();

        if (result != null) {
            int type = result.getType();
            // Confirm type is an image
            if (type == WebView.HitTestResult.IMAGE_TYPE
                    || type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                imageUrl = result.getExtra();
                showDialog(imageUrl);
            }
        }
    }

    private void showDialog(final String imgUrl) {

        new AlertDialog.Builder(DetailActivity.this).setTitle(R.string.save_title)
                .setMessage(R.string.save_image)
                .setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String iconName = imgUrl.substring(imgUrl.lastIndexOf('/') + 1);
                        String imgPath = getImgPath(iconName);
                        if(FileEx.isFileExisted(imgPath)) {
                            Toast.makeText(getApplicationContext(),
                                    getString(R.string.saved) + imgPath,
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            mkImgDirs();
                            ImageInfo ii = new ImageInfo();
                            ii.setUrl(imgUrl);
                            ii.setPath(imgPath);
                            workerForSave.setImageInfo(ii);
                            workerForSave.request();
                        }
                    }
                }).setNegativeButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                }).create().show();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (inCustomView()) {
            hideCustomView();
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_MENU) {
            if (popMenu.isShowing()) {
                popMenu.dismiss();
            } else {
                popMenu.showAsDropDown(mHeader.getMoreView());
            }

            return true;
        } else if (keyCode == KeyEvent.KEYCODE_BACK && mWebView.canGoBack()) {
            mWebView.goBack();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_BACK && popMenu.isShowing()) {
            popMenu.dismiss();
        }

        return super.onKeyDown(keyCode, event);
    }

    public class MyJavascriptInterface {
        @JavascriptInterface
        public void openImage(String imgUrl) {
            String iconName = imgUrl.substring(imgUrl.lastIndexOf('/') + 1);
            String imgPath = getImgPath(iconName);
            if(FileEx.isFileExisted(imgPath)) {
                openLocalImage(imgPath);
            } else {
                mkImgDirs();
                ImageInfo ii = new ImageInfo();
                ii.setUrl(imgUrl);
                ii.setPath(imgPath);
                workerForShow.setImageInfo(ii);
                workerForShow.request();
            }
        }
    }

    private static class MyHandler extends Handler {
        private final WeakReference<DetailActivity> mActivity;

        public MyHandler(DetailActivity activity) {
            mActivity = new WeakReference<DetailActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            DetailActivity activity = mActivity.get();
            if (activity != null) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case SET_EMPTY_GONE:
                        activity.mEmptyView.setVisibility(View.GONE);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    public final Handler mHandler = new MyHandler(this);

    private class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return super.shouldOverrideUrlLoading(view, url);
        }

        @SuppressLint("SetJavaScriptEnabled")
        @Override
        public void onPageFinished(WebView view, String url) {
            view.getSettings().setJavaScriptEnabled(true);
            stopLoading();
            String title = view.getTitle();
            if (!TextUtils.isEmpty(title)) {
                mHeader.setTitle(title);
            }
            if (url.contains(EnumConstants.DOMAIN_NAME)) {
                addImageClickListner();
            }
            super.onPageFinished(view, url);
            if (setEmptyGone) {
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            Thread.sleep(100);
                            Message msg = new Message();
                            msg.what = SET_EMPTY_GONE;
                            mHandler.sendMessage(msg);
                        } catch (InterruptedException e) {
                            MLog.error(MLog.getStactTrace(e));
                        }
                    }
                }).start();
            }
        }

        @SuppressLint("SetJavaScriptEnabled")
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            setEmptyGone = true;
            view.getSettings().setJavaScriptEnabled(true);
            startLoading();
            MLog.info("onPageStarted   url: "+url);
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description,
                String failingUrl) {
            setEmptyGone = false;
            stopLoading();
            checkNetwork();
            MLog.info("onReceivedError   errorCode: "+errorCode);
            super.onReceivedError(view, errorCode, description, failingUrl);
        }
    }

    private void showShareDialog() {
        ShareDialog shareDialog = new ShareDialog(this).setTitle(R.string.save_image_title);
        shareDialog.setmMsgId(msgId);
        shareDialog.setmSubMsgId(msgSubId);
        shareDialog.setmUrl(mShortUrl);
        shareDialog.setmSubject(mSubject);
        shareDialog.show();
    }

    public class WebViewDownLoadListener implements DownloadListener {

        @Override
        public void onDownloadStart(String url, String userAgent, String contentDisposition,
                String mimetype, long contentLength) {
            Uri uri = Uri.parse(url);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        }

    }

    public class WebChromeClientExt extends WebChromeClient {

        public void onProgressChanged(WebView view, int progress) {
            if (progress > 0) {
                int totalWidth = mHeader.getWidth();
                LayoutParams lp = mProgressBar.getLayoutParams();
                lp.width = totalWidth * progress / 100;
                mProgressBar.setLayoutParams(lp);
                mProgressBar.forceLayout();
            }
        }

        @Override
        public void onReceivedTitle(WebView view, String title) {
            super.onReceivedTitle(view, title);
            mHeader.setTitle(title);
        }
    }

    public boolean inCustomView() {
        return xCustomView != null;
    }

    public void hideCustomView() {
        xwebchromeclient.onHideCustomView();
    }

    public void viewContact() {
        Intent intent = new Intent(mContext, VendorAccountSetting.class);
        intent.putExtra(EnumConstants.NMSG_INTENT_EXTRA_ACCOUNT, serviceInfoAccount);
        intent.putExtra(EnumConstants.NMSG_INTENT_EXTRA_THREADID, mThreadId);
        mContext.startActivity(intent);
    }

    private void addImageClickListner() {
        mWebView.loadUrl("javascript:(function(){"
                + "var objs = document.getElementsByTagName(\"img\"); "
                + "for(var i=0;i<objs.length;i++)  " + "{" + "var parent = objs[i].parentNode;"
                + "var str = parent.nodeName;" + "if (str !='A' && str != 'a') {"
                + "    objs[i].onclick=function()  " + "    {  "
                + "        window.imagelistner.openImage(this.src);  " + "    }  " + "}" + "}"
                + "})()");
    }

    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        if (featureId == Window.FEATURE_ACTION_BAR && menu != null) {
            if (menu.getClass().getSimpleName().equals("MenuBuilder")) {
                try {
                    Method m = menu.getClass().getDeclaredMethod("setOptionalIconsVisible",
                            Boolean.TYPE);
                    m.setAccessible(true);
                    m.invoke(menu, true);
                } catch (NoSuchMethodException e) {
                    MLog.error(MLog.getStactTrace(e));
                } catch (IllegalAccessException e) {
                    MLog.error(MLog.getStactTrace(e));
                } catch (IllegalArgumentException e) {
                    MLog.error(MLog.getStactTrace(e));
                } catch (InvocationTargetException e) {
                    MLog.error(MLog.getStactTrace(e));
                } catch (NullPointerException e) {
                    MLog.error(MLog.getStactTrace(e));
                }
            }
        }
        return super.onMenuOpened(featureId, menu);
    }

    private void setOverflowShowingAlways() {
        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            menuKeyField.setAccessible(true);
            menuKeyField.setBoolean(config, false);
        } catch (NoSuchFieldException e) {
            MLog.error(MLog.getStactTrace(e));
        } catch (IllegalAccessException e) {
            MLog.error(MLog.getStactTrace(e));
        } catch (IllegalArgumentException e) {
            MLog.error(MLog.getStactTrace(e));
        } catch (NullPointerException e) {
            MLog.error(MLog.getStactTrace(e));
        }
    }

    private static String getImgDirPath() {
        String filePath = null;
        if (FileEx.getSDCardStatus()) {
            filePath = FileEx.getSDCardPath() + File.separator + EnumConstants.ROOT_DIR
                    + File.separator + "image" + File.separator;
        } else {
            filePath = FileEx.getSDCardPath() + File.separator + "image" + File.separator;
        }
        MLog.info("getImgDirPath   filePath:"+filePath);
        return filePath;
    }

    private static String getImgPath(String iconName) {
        return getImgDirPath() + iconName;
    }

    private static void mkImgDirs() {
        File dir = new File(getImgDirPath());
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    private void openLocalImage(String imgPath) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.addCategory("android.intent.category.DEFAULT");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        Uri uri = Uri.fromFile(new File(imgPath));
        intent.setDataAndType(uri, "image/*");
        startActivity(intent);
    }

    @Override
    public void complete(Object owner, Object data, int success) {
        if (owner == workerForSave) {
            if (success == Pipe.NET_SUCCESS) {
                ImageInfo ii = (ImageInfo) data;
                Toast.makeText(this,
                        getString(R.string.saved) + ii.getPath(),
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this,
                        getString(R.string.save_image_fail),
                        Toast.LENGTH_SHORT).show();
            }
        } else if(owner == workerForShow) {
            if (success == Pipe.NET_SUCCESS) {
                ImageInfo ii = (ImageInfo) data;
                openLocalImage(ii.getPath());
            } else {
                Toast.makeText(this,
                        getString(R.string.get_image_fail),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
}
