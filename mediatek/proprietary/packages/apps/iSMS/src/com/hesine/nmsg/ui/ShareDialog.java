package com.hesine.nmsg.ui;

import java.util.HashMap;
import java.util.List;

import com.hesine.nmsg.R;
import com.hesine.nmsg.common.CommonUtils;
import com.hesine.nmsg.thirdparty.Statistics;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ImageView.ScaleType;

public class ShareDialog implements View.OnClickListener {
    public static class ActionType {
        public static final int ACTION_LEFT = 0;
        public static final int ACTION_RIGHT = 1;
    }

    private Context mContext;
    private TextView mTitle;
    private GridView mLogosGridView;
    private Dialog mDialog;
    private String mMsgId = null;
    private int mMsgSubId = -1;
    private String mUrl = null;
    private String mSubject = null;

    private int[] shareLogosLight = new int[] { R.drawable.logo_facebook, R.drawable.logo_twitter,
            R.drawable.logo_wechat, R.drawable.logo_sinaweibo };// ,
                                                                // R.drawable.logo_wechatmoments
    private int[] shareLogos = new int[] { R.drawable.logo_facebook_grey,
            R.drawable.logo_twitter_grey, R.drawable.logo_wechat_grey,
            R.drawable.logo_sinaweibo_grey };// ,
                                             // R.drawable.logo_wechatmoments_grey

    private HashMap<String, ResolveInfo> mSharePackage = new HashMap<String, ResolveInfo>(5);

    private void getSharePackages() {
        mSharePackage.clear();
        Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
        shareIntent.setType("text/plain");

        PackageManager pm = mContext.getApplicationContext().getPackageManager();
        List<ResolveInfo> activityList = pm.queryIntentActivities(shareIntent, 0);

        for (final ResolveInfo app : activityList) {
            if ("com.twitter.android.composer.ComposerActivity".equals(app.activityInfo.name)) {// twitter
                mSharePackage.put("twitter", app);
                shareLogos[1] = shareLogosLight[1];
            } else if ("com.sina.weibo.EditActivity".equals(app.activityInfo.name)
                    || "com.sina.weibo.ComposerDispatchActivity".equals(app.activityInfo.name)) {// weibo
                mSharePackage.put("weibo", app);
                shareLogos[3] = shareLogosLight[3];
            } else if ("com.tencent.mm.ui.tools.ShareImgUI".equals(app.activityInfo.name)) {// weixin
                mSharePackage.put("weixin", app);
                shareLogos[2] = shareLogosLight[2];
            } else if ((app.activityInfo.name).contains("facebook")) {// facebook
                mSharePackage.put("facebook", app);
                shareLogos[0] = shareLogosLight[0];
            } 
        }
    };

    public ShareDialog(Context context) {
        mContext = context;
        mDialog = new Dialog(mContext, R.style.Theme_pop_dialog);
        mDialog.setContentView(R.layout.share_dialog);
        mTitle = (TextView) mDialog.findViewById(R.id.title);
        mLogosGridView = (GridView) mDialog.findViewById(R.id.share_gridview);

        getSharePackages();

        BaseAdapter imageAdapter = new BaseAdapter() {
            public int getCount() {
                return shareLogos.length;
            }

            public Object getItem(int position) {
                return null;
            }

            public long getItemId(int position) {
                return position;
            }

            public View getView(int position, View converToView, ViewGroup parent) {
                ImageView imageView = null;
                if (converToView == null) {
                    imageView = new ImageView(mContext);
                    imageView.setImageResource(shareLogos[position]);
                    imageView.setScaleType(ScaleType.CENTER);
                    int value = CommonUtils.dp2px(mContext, 46);
                    imageView.setLayoutParams(new GridView.LayoutParams(value, value));
                } else {
                    imageView = (ImageView) converToView;
                }
                imageView.setImageResource(shareLogos[position]);
                return imageView;
            }
        };

        mLogosGridView.setAdapter(imageAdapter);

        mLogosGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "title!!!");
                shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, mSubject + mUrl);

                switch (position) {
                    case 0: // facebook
                        Statistics.getInstance().msgShare(mMsgId, mMsgSubId,
                                Statistics.ShareChannel.FACEBOOK);
                        ResolveInfo appFacebook = mSharePackage.get("facebook");
                        if (appFacebook != null) {
                            final ActivityInfo activity = appFacebook.activityInfo;
                            final ComponentName name = new ComponentName(
                                    activity.applicationInfo.packageName, activity.name);
                            shareIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                            shareIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                    | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                            shareIntent.setComponent(name);
                            mContext.startActivity(shareIntent);

                        }
                        break;
                    case 1:// twitter
                        Statistics.getInstance().msgShare(mMsgId, mMsgSubId,
                                Statistics.ShareChannel.TWITTER);
                        ResolveInfo appTwitter = mSharePackage.get("twitter");
                        if (appTwitter != null) {
                            final ActivityInfo activity = appTwitter.activityInfo;
                            final ComponentName name = new ComponentName(
                                    activity.applicationInfo.packageName, activity.name);
                            shareIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                            shareIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                    | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                            shareIntent.setComponent(name);
                            mContext.startActivity(shareIntent);
                        }
                        break;
                    case 2:// weixin
                        Statistics.getInstance().msgShare(mMsgId, mMsgSubId,
                                Statistics.ShareChannel.WEIXIN);
                        ResolveInfo appWeixin = mSharePackage.get("weixin");
                        if (appWeixin != null) {
                            final ActivityInfo activity = appWeixin.activityInfo;
                            final ComponentName name = new ComponentName(
                                    activity.applicationInfo.packageName, activity.name);
                            shareIntent.setComponent(name);
                            mContext.startActivity(shareIntent);
                        }
                        break;
                    case 3:// weibo
                        Statistics.getInstance().msgShare(mMsgId, mMsgSubId,
                                Statistics.ShareChannel.WEIBO);
                        ResolveInfo appWeibo = mSharePackage.get("weibo");
                        if (appWeibo != null) {
                            final ActivityInfo activity = appWeibo.activityInfo;
                            final ComponentName name = new ComponentName(
                                    activity.applicationInfo.packageName, activity.name);
                            shareIntent.setComponent(name);
                            mContext.startActivity(shareIntent);
                        }
                        break;
                    default:
                        break;
                }

            }
        });

    }

    public void setmMsgId(String msgId) {
        mMsgId = msgId;
    }

    public void setmSubMsgId(int subMsgId) {
        mMsgSubId = subMsgId;
    }

    public void setmUrl(String url) {
        mUrl = url;
    }

    public void setmSubject(String subject) {
        mSubject = subject;
    }

    public ShareDialog show() {
        mDialog.show();
        return this;
    }

    public ShareDialog setCancelable(boolean cancelable) {
        mDialog.setCancelable(cancelable);
        return this;
    }

    public ShareDialog setTitle(int resId) {
        setTitle(mContext.getText(resId).toString());
        return this;
    }

    public ShareDialog setTitle(String title) {
        mTitle.setText(title);
        return this;
    }

    public void dismiss() {
        mDialog.dismiss();
    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub

    }
}
