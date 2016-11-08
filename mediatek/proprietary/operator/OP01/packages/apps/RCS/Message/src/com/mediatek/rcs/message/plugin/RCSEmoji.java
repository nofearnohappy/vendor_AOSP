package com.mediatek.rcs.message.plugin;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import cn.com.em.sdk.EmShopSDK;
import cn.com.em.sdk.utils.UninitializedException;
import cn.com.em.sdk.utils.AppNotInstalledException;
import com.mediatek.rcs.common.binder.RCSServiceManager;
import com.mediatek.rcs.common.RCSMessageManager;
import com.mediatek.rcs.common.utils.ContextCacher;
import com.mediatek.rcs.common.utils.EmojiShop;
import com.mediatek.rcs.common.utils.EmojiShop.EmojiPackage;
import com.mediatek.rcs.message.R;

/**
 * RCSEmoji.
 *
 */
public class RCSEmoji implements EmojiShop.OnEmDataChangedListener{

    private static String TAG = "RCSEmoji";

    private Context mPluginContext;
    private Resources mPluginResource;
    private Activity mHostActivity;

    private ViewParent mViewParent;
    private EditText mTextEditor;
    private RcsComposeActivity mComposer;

    private boolean isPanelShow = false;
    private ImageButton mEmojiBtn;
    private View mEmojiPanel;
    private LinearLayout mPageIndicator;
    private ArrayList<GridView> mGridViews;
    // view group contain em package shortcut icons
    private LinearLayout mEmPkgIconsContainer;
    private EmojiPagerAdapter mPagerAdapter;
    private ViewPager mEmojiViewPager;
    private int mCurrentPage;
    private EmojiImpl mEmojiImpl;
    private EmojiShop mEmojiShop;

    private boolean mIsGroupChat;
    private String mGroupChatId;

    // show 8 emoticon shop icons in each page
    private static int EM_PER_PAGE = 8;
    // unicode emoticon icons set as 5 columns, 4 lines
    private static int ITEMS_COLUMN = 5;
    // show 20 unicode emoticon icons in each page
    private static int ITEMS_PER_PAGE = 20;
    // default package ID of unicode emotions: 0x1f600 - 0x1f64f
    private static String UNICODE_PACKAGE_ID = "1f600";

    // HashMap<pkgId, HashMap<pageIndex, List<HashMap<EmId, EmResPath>>>>
    private final HashMap<String, HashMap<Integer, List<HashMap<String, Object>>>> mPackageInfos =
            new HashMap<String, HashMap<Integer, List<HashMap<String, Object>>>>();


    /**
     * Construction.
     * @param hostActivity Activity
     * @param viewParent viewParent
     * @param RcsComposeActivity composer
     */
    public RCSEmoji(Activity hostActivity, ViewParent viewParent,
                                            RcsComposeActivity composer) {
        mHostActivity = hostActivity;
        mPluginContext = ContextCacher.getPluginContext();
        mPluginResource = mPluginContext.getResources();
        mViewParent = viewParent;
        mComposer = composer;
        mEmojiImpl = EmojiImpl.getInstance(mPluginContext);
        initEmojiButton();
    }

    public void unInit() {
        Log.d(TAG, "unInit");
        EmojiShop.removeOnEmDataChangedListener(this);
    }

    public void setEmojiButtonVisible(boolean visible) {
        Log.d(TAG, "setEmojiButtonVisible, visible = " + visible);
        if (mEmojiBtn != null) {
            if (visible) {
                mEmojiBtn.setVisibility(View.VISIBLE);
            } else {
                mEmojiBtn.setVisibility(View.GONE);
            }
        }
    }

    public void setGroupInfo(boolean isGroupChat, String chatId) {
        mIsGroupChat = isGroupChat;
        mGroupChatId = chatId;
    }

    private void initEmojiButton() {
        Log.d(TAG, "initEmojiButton");
        LinearLayout bottomPanel = (LinearLayout)mViewParent;

        // init emoji button
        XmlResourceParser xrp = mPluginResource.getLayout(R.layout.emoji_button);
        LayoutInflater inflater = (LayoutInflater) mHostActivity.getApplicationContext().
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mEmojiBtn = (ImageButton)inflater.inflate(xrp, null);
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        mEmojiBtn.setLayoutParams(lp);
        mEmojiBtn.setImageDrawable(
                mPluginResource.getDrawable(R.drawable.emoji_btn_normal));
//        mEmojiBtn.getBackground().setAlpha(0);
        mEmojiBtn.setVisibility(View.VISIBLE);
        mEmojiBtn.setTag(R.drawable.emoji_btn_normal);
        bottomPanel.addView(mEmojiBtn, 1);

        mEmojiBtn.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isPanelShow) {
                    //hide imm and sharepanel
                    mComposer.showSharePanelOrKeyboard(false, false);
                    mEmojiBtn.setImageDrawable(
                            mPluginResource.getDrawable(R.drawable.emoji_btn_focused));
                    mEmojiBtn.setTag(R.drawable.emoji_btn_focused);
                    showEmojiPanel(true);
                } else {
                    mEmojiBtn.setImageDrawable(
                            mPluginResource.getDrawable(R.drawable.emoji_btn_normal));
                    mEmojiBtn.setTag(R.drawable.emoji_btn_normal);
                    showEmojiPanel(false);
                }
            }
        });
    }

   /**
    * Hide or show Emoticon panel
    *
    * @param isShow: true if show, else hide
    */
    public void showEmojiPanel(boolean isShow) {
        Log.d(TAG, "showEmojiPanel() isPanelShow=" + isPanelShow + ", isShow=" + isShow);
        if (isPanelShow == isShow) {
            return;
        }
        if (isShow) {
            if (mEmojiPanel == null) {
                initEmojiPanel();
                updateEmojiViews();
            }
            mEmojiPanel.setVisibility(View.VISIBLE);
            isPanelShow = true;
        } else {
            mEmojiPanel.setVisibility(View.GONE);
            isPanelShow = false;
        }
    }

    private void initEmojiPanel() {
        Log.d(TAG, "initEmojiPanel");
        LinearLayout bottomPanel = (LinearLayout)mViewParent;
        LinearLayout bottomLinear = (LinearLayout) bottomPanel.getParent();
        int index = bottomLinear.indexOfChild(bottomPanel);
        if (mEmojiPanel != null) {
            bottomLinear.removeViews(index+1, 1);
        }
        // init emoji panel
        XmlResourceParser xrp = mPluginResource.getLayout(R.layout.emoji_viewpager);
        LayoutInflater inflater = (LayoutInflater) mHostActivity.getApplicationContext().
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mEmojiPanel =  inflater.inflate(xrp, null);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.FILL_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
        mEmojiPanel.setLayoutParams(params);
        bottomLinear.addView(mEmojiPanel, index+1);
    }

    private void updateEmojiViews() {
        Log.d(TAG, "updateEmojiViews");
        updateEmPkgIcons();
        initEmojiViewPager();
        initPageIndicator();
        showEmojiPanel(isPanelShow);
        EmojiShop.addOnEmDataChangedListener(this);
    }

    // called in onIpComposeActivityResume()
    public void setEmojiEditor(EditText textEditor) {
        Log.d(TAG, "setEmojiEditor: textEditor = " + textEditor);
        if (textEditor == null) {
            if (mTextEditor != null) {
                mTextEditor.setOnFocusChangeListener(null);
            }
            return;
        }

        mTextEditor = textEditor;
        String text = mTextEditor.getText().toString();
        float textSize = mTextEditor.getTextSize();

        if (!TextUtils.isEmpty(text)) {
            CharSequence charSequence = mEmojiImpl.getEmojiExpression(text, true, (int)textSize);
            mTextEditor.setTextKeepState(charSequence);
        }

        if (mTextEditor.hasFocus()) {
            mEmojiBtn.setEnabled(true);
            mEmojiBtn.setImageDrawable(mPluginResource.getDrawable(R.drawable.emoji_btn_focused));
        } else {
            mEmojiBtn.setEnabled(false);
            mEmojiBtn.setImageDrawable(mPluginResource.getDrawable(R.drawable.emoji_btn_normal));
        }


        mTextEditor.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    showEmojiPanel(false);
                    mEmojiBtn.setEnabled(false);
                    mEmojiBtn.setImageDrawable(
                        mPluginResource.getDrawable(R.drawable.emoji_btn_normal));
                } else {
                    mEmojiBtn.setEnabled(true);
                    mEmojiBtn.setImageDrawable(
                        mPluginResource.getDrawable(R.drawable.emoji_btn_focused));
                }
            }
        });
    }

    private ArrayList<List<HashMap<String, Object>>> sliptIntoPages(
                                          List<HashMap<String, Object>> list, int pageSize) {
        ArrayList<List<HashMap<String, Object>>> arrayList =
                                          new ArrayList<List<HashMap<String, Object>>>();
        int mPageCount = list.size() / pageSize;
        boolean hasMod = false;
        if (list.size() % pageSize != 0) {
            hasMod = true;
            mPageCount += 1;
        }

        for (int pageNo = 0; pageNo < mPageCount; pageNo++) {
            int pageStart = pageNo * pageSize;
            int pageStop = 0;
            if (pageNo == mPageCount - 1) {
                if (hasMod) {
                    pageStop = list.size();
                } else {
                    pageStop = (pageNo + 1) * pageSize;
                }
            } else {
                pageStop = (pageNo + 1) * pageSize;
            }
            arrayList.add(list.subList(pageStart, pageStop));
        }
        return arrayList;
    }

    private HashMap<Integer, List<HashMap<String, Object>>> loadUnicodeEmojiIcon() {
        int emojiCount = EmojiConstants.emojiUnicodes.length;
        List<HashMap<String, Object>> listItems = new ArrayList<HashMap<String, Object>>();
        for (int i = 0; i < emojiCount; i++) {
            HashMap<String, Object> map = new HashMap<String, Object>();
            map.put(EmojiConstants.emojiUnicodes[i], EmojiConstants.emojiImags[i]);
            listItems.add(map);
            // add a delete icon at the end of each page
            if (i % (ITEMS_PER_PAGE-1) == (ITEMS_PER_PAGE-2)) {
                HashMap<String, Object> delete = new HashMap<String, Object>();
                // ASCII BS(backspace) 0x08
                delete.put("0x08", R.drawable.emoji_delete);
                listItems.add(delete);
            }
        }

        HashMap<Integer, List<HashMap<String, Object>>> unicodeResMap =
            new HashMap<Integer, List<HashMap<String, Object>>> ();

        ArrayList<List<HashMap<String, Object>>> arrayList =
                                             sliptIntoPages(listItems, ITEMS_PER_PAGE);
        for (int i = 0; i < arrayList.size(); i++) {
            unicodeResMap.put(i, arrayList.get(i));
        }
        return unicodeResMap;
    }

    // load all em data and
    public HashMap<String, HashMap<Integer, List<HashMap<String, Object>>>>
                                                              loadAllEmShopIcons() {
        Log.d(TAG, "loadEmShopEmIcon()");
        HashMap<String, HashMap<Integer, List<HashMap<String, Object>>>> mEmShopEmIcons =
            new HashMap<String, HashMap<Integer, List<HashMap<String, Object>>>>();

        List<EmojiPackage> pkgInfos = EmojiShop.getAllPackageInfo();

        // init data
        for (int i = 0; i < pkgInfos.size(); i++) {
            EmojiPackage pkgInfo = pkgInfos.get(i);

            HashMap<Integer, List<HashMap<String, Object>>> packageIcons =
                new HashMap<Integer, List<HashMap<String, Object>>>();

            String pkgId = pkgInfo.getPkgId();
            HashMap<String, String> pkgIcons = pkgInfo.getEmIcons();
            List<HashMap<String, Object>> emIcons = new ArrayList<HashMap<String, Object>>();

            Iterator iterator = pkgIcons.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry)iterator.next();
                String emId = entry.getKey().toString();
                String emRes = entry.getValue().toString();
                HashMap<String, Object> map = new HashMap<String, Object>();
                map.put(emId, emRes);
                emIcons.add(map);
            }

            ArrayList<List<HashMap<String, Object>>> arrayList =
                            sliptIntoPages(emIcons, EM_PER_PAGE);
            for (int pageNo = 0; pageNo < arrayList.size(); pageNo++) {
                packageIcons.put(pageNo, arrayList.get(pageNo));
            }
            mEmShopEmIcons.put(pkgId, packageIcons);
        }
        return mEmShopEmIcons;
    }

    private void addGridViewById(String emPkgId) {
        Log.d(TAG, "addGridViewById res="+ emPkgId);
        HashMap<Integer, List<HashMap<String, Object>>> res = mPackageInfos.get(emPkgId);
        for(int i = 0; i < res.size(); i++) {
            XmlResourceParser xrp = mPluginResource.getLayout(R.layout.share_flipper);
            LayoutInflater inflater = (LayoutInflater) mHostActivity.getApplicationContext().
                    getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            GridView gridView =  (GridView)inflater.inflate(xrp, null);
            gridView.setGravity(Gravity.CENTER);
            if (emPkgId.equals(UNICODE_PACKAGE_ID)) {
                gridView.setNumColumns(5);
            } else {
                gridView.setNumColumns(4);
            }
            gridView.setTag(emPkgId);

            GridViewAdapter gridAdapter = new GridViewAdapter(res.get(i), i, emPkgId);
            gridView.setAdapter(gridAdapter);
            mGridViews.add(gridView);
        }
    }

    public void initEmojiViewPager() {
        Log.d(TAG, "initEmojiViewPager");

        // init ViewPager
        mEmojiViewPager = (ViewPager) mEmojiPanel.findViewById(R.id.emoji_view);
        int orientation = mPluginResource.getConfiguration().orientation;
        Log.d(TAG, "orientation = " + orientation);
        ViewGroup.LayoutParams params = mEmojiViewPager.getLayoutParams();
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            params.height = dip2px(108);
            ITEMS_COLUMN = 10;
        } else {
            params.height = dip2px(216);
            ITEMS_COLUMN = 5;
        }
        mEmojiViewPager.setLayoutParams(params);
        initEmShopData();
        mPagerAdapter = new EmojiPagerAdapter();
        mEmojiViewPager.setAdapter(mPagerAdapter);
        mEmojiViewPager.setCurrentItem(0);
        mEmojiViewPager.setOnPageChangeListener(new PageChangedListener());
    }

    private class EmojiPagerAdapter extends PagerAdapter {
        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
          return arg0 == ((View)arg1);
        }

        @Override
        public int getCount() {
          return mGridViews.size();
        }

        @Override
        public void destroyItem(View container, int position, Object object) {
           ((ViewPager) container).removeView((View)object);
        }

        @Override
        public Object instantiateItem(View container, int position) {
          ((ViewPager) container).addView(mGridViews.get(position));
          return mGridViews.get(position);
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }
    }

    private void initEmShopData() {
        // load em data
        mPackageInfos.clear();
        mPackageInfos.put(UNICODE_PACKAGE_ID, loadUnicodeEmojiIcon());
        mPackageInfos.putAll(loadAllEmShopIcons());

        // add GridViews
        mGridViews = new ArrayList<GridView>();
        addGridViewById(UNICODE_PACKAGE_ID);
        for(int index = 1; index < mPackageInfos.size(); index++) {
            String packageId = EmojiShop.getEmPackageId(index-1);
            addGridViewById(packageId);
        }
    }

    private void initPageIndicator() {
        Log.d(TAG, "initPageIndicator()");
        // int page indicator
        mPageIndicator = (LinearLayout) mEmojiPanel.findViewById(R.id.page_indicator);
        mCurrentPage = 0;
        for (int pageNo = 0; pageNo < mPackageInfos.get(UNICODE_PACKAGE_ID).size(); pageNo++) {
            ImageView indicator = new ImageView(mHostActivity);
            indicator.setPadding(dip2px(5), dip2px(5), dip2px(5), dip2px(5));
            if (pageNo == 0) {
                indicator.setImageDrawable(
                    mPluginResource.getDrawable(R.drawable.emoji_page_focused));
            } else {
                indicator.setImageDrawable(
                    mPluginResource.getDrawable(R.drawable.emoji_page_unfocused));
            }
            mPageIndicator.addView(indicator);
        }
    }

    private int dip2px(float dpValue) {
        float scale = mPluginResource.getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    private void updatePageIndicator(int pageIndex) {
        int iconIndex = pageIndex;
        String emPkgId = (String)mGridViews.get(pageIndex).getTag();
        Log.d(TAG, "updatePageIndicator() pageIndex = " + pageIndex + ", emPkgId=" + emPkgId);
        for(int i=0; i < mGridViews.size(); i++) {
            if (!emPkgId.equals((String)mGridViews.get(i).getTag())) {
                iconIndex--;
            } else {
                break;
            }
        }
        mCurrentPage = pageIndex;
        Log.d(TAG, "updatePageIndicator() iconIndex = " + iconIndex);
        int iconNum = mPackageInfos.get(emPkgId).size();
        mPageIndicator.removeAllViews();
        for (int i = 0; i < iconNum; i++) {
            ImageView indicator = new ImageView(mHostActivity);
            indicator.setPadding(dip2px(5), dip2px(5), dip2px(5), dip2px(5));
            if (i == iconIndex) {
                indicator.setImageDrawable(
                    mPluginResource.getDrawable(R.drawable.emoji_page_focused));
            } else {
                indicator.setImageDrawable(
                    mPluginResource.getDrawable(R.drawable.emoji_page_unfocused));
            }
            mPageIndicator.addView(indicator);
        }
        updateEmPkgIconsBg(emPkgId);
    }

    public class PageChangedListener implements OnPageChangeListener {
        @Override
        public void onPageScrollStateChanged(int arg0) {
            Log.d(TAG, "onPageScrollStateChanged = " + arg0);
        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {
            Log.d(TAG, "onPageScrolled = " + arg0);
        }

        @Override
        public void onPageSelected(int arg0) {
            updatePageIndicator(arg0);
        }
     }

    private void insertEmoji(SpannableString spannableString) {
        mTextEditor.requestFocus();
        int index = mTextEditor.getSelectionStart();
        Editable edit = mTextEditor.getEditableText();
        if (index < 0 || index >= edit.length()) {
            edit.append(spannableString);
        } else {
            edit.insert(index, spannableString);
        }
    }

    private void deleteEmoji() {
        mTextEditor.requestFocus();
        mTextEditor.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DEL));
    }

    public void editEmoticonString(String emID, String emPkgId) {
        Log.d(TAG, "editEmoticonString emID=" + emID + ", emPkgId=" + emPkgId);
        if (emPkgId.equals(UNICODE_PACKAGE_ID)) {
            // delete icon
            if (emID.toUpperCase().equals("0X08")) {
                deleteEmoji();
                return;
            }
            try {
                int emCode = Integer.parseInt(emID, 16);
                String emoji = new String(Character.toChars(Integer.parseInt(emID, 16)));
                SpannableString spannableString = new SpannableString(emoji);
                insertEmoji(spannableString);
            } catch (NumberFormatException e) {
                Log.d(TAG, "NumberFormatException");
                return;
            }
        }
    }

    public void sendEmoticonMessage(final String emID, final String emPkgId) {
        Log.d(TAG, "sendEmoticonMessage emID=" + emID + ", emPkgId=" + emPkgId);
        if (emPkgId.equals(UNICODE_PACKAGE_ID)) {
            return;
        } else {
            if (!RCSServiceManager.getInstance().serviceIsReady()) {
                Toast.makeText(mHostActivity,
                    mPluginContext.getString(R.string.rcs_not_availble),
                    Toast.LENGTH_SHORT).show();
                return;
            }
            // create em xml string
            String emXml = EmojiShop.createEmXml(emPkgId, emID);
            Log.d(TAG, "EmojiShop, createEmXml emXml = " + emXml);
            RcsComposeActivity.getRcsComposer().sendIpEmoticonMessage(emXml);
        }
    }

    @Override
    public void onEmDataChanged() {
        initEmShopData();
        updateEmPkgIcons();
        mPagerAdapter.notifyDataSetChanged();
        mEmojiViewPager.setCurrentItem(0);
        mPagerAdapter.notifyDataSetChanged();
    }

    public boolean isEmojiPanelShow() {
        return isPanelShow;
    }

    public void resetEmojiPanel(Configuration newConfig) {
        Log.d(TAG, "resetEmojiPanel(), orientation = " + newConfig.orientation);
        initEmojiPanel();
    }

    public void updateEmPkgIcons() {
        Log.d(TAG, "updateEmPkgIcons");
        // Emoticon Shop main activity
        ImageView emShopMain = (ImageView) mEmojiPanel.findViewById(R.id.emoji_shop);
        emShopMain.setImageDrawable(mPluginResource.getDrawable(R.drawable.emoji_shop));
        emShopMain.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO: if Emoticon apk not install, or RCS not ready, give toast.
                if (!RCSServiceManager.getInstance().serviceIsReady()) {
                    Toast.makeText(mHostActivity,
                            mPluginContext.getString(R.string.rcs_not_availble),
                            Toast.LENGTH_SHORT).show();
                    return;
                } else if (!EmojiShop.isApkAvailable()) {
                    Toast.makeText(mHostActivity,
                            mPluginContext.getString(R.string.emo_shop_not_availble),
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                EmojiShop.getInstance().startMain();
            }
        });

        // Emoticon Shop Setting activity
        ImageView emShopSetting = (ImageView) mEmojiPanel.findViewById(R.id.emoji_setting);
        emShopSetting.setImageDrawable(mPluginResource.getDrawable(R.drawable.emoji_setting));
        emShopSetting.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO: if Emoticon apk not install, or RCS not ready, give toast.
                if (!RCSServiceManager.getInstance().serviceIsReady()) {
                    Toast.makeText(mHostActivity,
                            mPluginContext.getString(R.string.rcs_not_availble),
                            Toast.LENGTH_SHORT).show();
                    return;
                } else if (!EmojiShop.isApkAvailable()) {
                    Toast.makeText(mHostActivity,
                            mPluginContext.getString(R.string.emo_shop_not_availble),
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                EmojiShop.getInstance().startManager();
            }
        });

        //add all em shop package icons to the container
        mEmPkgIconsContainer = (LinearLayout) mEmojiPanel.findViewById(
                                                       R.id.emoji_index_container);
        mEmPkgIconsContainer.removeAllViews();

        //  unicode emoticon package icon
        ImageView unicodePkgIcon = new ImageView(mHostActivity);
      //  ImageView unicodePkgIcon = (ImageView) mEmojiPanel.findViewById(R.id.emoji_icon);
        unicodePkgIcon.setImageDrawable(mPluginResource.getDrawable(R.drawable.emoji_1f60a));
        unicodePkgIcon.setTag(UNICODE_PACKAGE_ID);
        mEmPkgIconsContainer.addView(unicodePkgIcon);

        List<EmojiPackage> pkgInfo = EmojiShop.getAllPackageInfo();
        for (int i = 0; i < pkgInfo.size(); i++) {
            String pkgId = pkgInfo.get(i).getPkgId();
            String iconPath = pkgInfo.get(i).getPkgIcon();
            Drawable iconRes = Drawable.createFromPath(iconPath);
            ImageView emIcon = new ImageView(mPluginContext);
            RelativeLayout.LayoutParams params =
                        new RelativeLayout.LayoutParams(dip2px(40), dip2px(40));
            emIcon.setLayoutParams(params);
            emIcon.setImageDrawable(iconRes);
            emIcon.setTag(pkgId);
            mEmPkgIconsContainer.addView(emIcon);
        }
        // set package icons click listener
        setEmPkgIconsClickListener();
        // update package icon background color
        updateEmPkgIconsBg(UNICODE_PACKAGE_ID);
    }

    public void updateEmPkgIconsBg(String emPkgId) {
        if (mEmPkgIconsContainer.getChildCount() == 1) {
            mEmPkgIconsContainer.getChildAt(0).setBackgroundColor(0);
            return;
        }
        for(int i = 0; i < mEmPkgIconsContainer.getChildCount(); i++) {
            if (mEmPkgIconsContainer.getChildAt(i).getTag().equals(emPkgId)) {
                mEmPkgIconsContainer.getChildAt(i).setBackgroundColor(
                                                  Color.parseColor("#FF0101"));
            } else {
                mEmPkgIconsContainer.getChildAt(i).setBackgroundColor(0);
            }
        }
    }

    private void setEmPkgIconsClickListener() {
        // only has unicode emoticon package
        if (mEmPkgIconsContainer.getChildCount() <= 1) {
            return;
        }
        for(int i = 0; i < mEmPkgIconsContainer.getChildCount(); i++) {
            final ImageView child = (ImageView)mEmPkgIconsContainer.getChildAt(i);
            child.setOnClickListener(new android.view.View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String emPkgId = (String)child.getTag();
                    Log.d(TAG, "EmPackageIconClicked, emPkgId=" + emPkgId);
                    for (int i = 0; i < mGridViews.size(); i++) {
                        if (emPkgId.equals((String)mGridViews.get(i).getTag())) {
                            mCurrentPage = i;
                            mEmojiViewPager.setCurrentItem(mCurrentPage);
                            break;
                        }
                    }
                    mPagerAdapter.notifyDataSetChanged();
                }
            });
        }
    }

    public class GridViewAdapter extends BaseAdapter {
        private final List<HashMap<String, Object>> mResIds;
        private final int mPage;
        private final String mPkgId;

        public GridViewAdapter(List<HashMap<String, Object>> list, int page, String pkgId) {
            Log.d(TAG, "GridViewAdapter() pkgId=" + pkgId +
                        ", page=" + page + ", list.size()=" + list.size());
            mResIds = list;
            mPage = page;
            mPkgId = pkgId;
        }

        @Override
        public int getCount() {
            return mResIds.size();
        }

        @Override
        public Object getItem(int position) {
            return mResIds.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            Log.d(TAG, "getView() position=" + position);
            View currentView = null;
            if (convertView == null) {
                XmlResourceParser xrp = mPluginResource.getLayout(R.layout.emoji_button);
                LayoutInflater inflater = (LayoutInflater) mHostActivity.getApplicationContext().
                        getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                ImageButton image = (ImageButton)inflater.inflate(xrp, null);
                currentView = image;
            } else {
                currentView = convertView;
            }

            ImageView imageView = (ImageView) currentView;
            imageView.setLayoutParams(new GridView.LayoutParams(
                GridView.LayoutParams.WRAP_CONTENT,
                GridView.LayoutParams.WRAP_CONTENT));
            imageView.setAdjustViewBounds(false);

            boolean isEmShop = false;
            String emId = "0";
            String emRes = "0";
            int emResInt = 0;
            Iterator iterator = mResIds.get(position).entrySet().iterator();
            if (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry)iterator.next();
                emId = entry.getKey().toString();
                emRes = entry.getValue().toString();
                Log.d(TAG, "getView() emId=" + emId + ", emRes=" + emRes);
            }
            try {
                emResInt = Integer.parseInt(emRes);
            } catch (NumberFormatException e) {
                Log.d(TAG, "getView() NumberFormatException");
                // not in 0x1f600  -- 0x1f64f, 0X08
                isEmShop = true;
            }
            final String mEmId = emId;
            if (!isEmShop) {
                imageView.setImageDrawable(mPluginResource.getDrawable(emResInt));
                imageView.setOnClickListener(new android.view.View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        editEmoticonString(mEmId, mPkgId);
                    }
                });
            } else {
                imageView = new ImageView(mPluginContext);
                Drawable res = Drawable.createFromPath(emRes);
                RelativeLayout.LayoutParams params =
                        new RelativeLayout.LayoutParams(dip2px(80), dip2px(80));
                imageView.setLayoutParams(params);
                imageView.setImageDrawable(res);
                imageView.setOnClickListener(new android.view.View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        sendEmoticonMessage(mEmId, mPkgId);
                    }
                });
            }
            return imageView;
        }
    }
}