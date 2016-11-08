package com.mediatek.rcs.pam.util;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

//import com.mediatek.mms.ipmessage.IpComposeActivityCallback;
import com.mediatek.rcs.pam.ui.conversation.PaComposeActivity;
import com.mediatek.rcs.pam.util.PageAdapter;
import com.mediatek.rcs.pam.R;
import com.mediatek.rcs.pam.util.ContextCacher;


public class RCSEmoji implements PageAdapter.OnEmotionItemSelectedListener{
    
    private static String TAG = "RCSEmoji";
    
    private Context mPluginContext;
    private Context mHostContext;
    private Activity mHostActivity;
    private Resources mPluginResource;
    private ViewParent mViewParent;
    
    private LinearLayout mEmojiPanel;
    private ImageButton mEmojiBtn;
    private static boolean isEmojiPanelShow = false;
    private View mEmojiView;   
    private ViewPager mEmojiViewPager;
    private ArrayList<GridView> mGridViews;
    private LinearLayout mPageIndicator; 

    private int mPageCount;    
    private static int mCurrentPage;
    private static int ITEMS_COLUMN = 5;
    private static int ITEMS_PER_PAGE = 19;
    static final HashMap<Integer, List<Integer>> mResIdsMap = new HashMap<Integer, List<Integer>>();
    private EditText mTextEditor;
    private PaComposeActivity mCallback;
    private int mOrientation;
    private PageAdapter pageAdapter;
    private EmojiImpl mEmojiImpl;
    
    public RCSEmoji(Activity hostActivity, ViewParent viewParent, PaComposeActivity callback) {
        mHostActivity = hostActivity;
        mPluginContext = ContextCacher.getPluginContext();
        mPluginResource = mPluginContext.getResources();
        mViewParent = viewParent;
        mCallback = callback;
        initEmojiButton();
        mEmojiImpl = EmojiImpl.getInstance(ContextCacher.getPluginContext());
    }

    public void setEmojiEditor(EditText textEditor) { 
        mTextEditor = textEditor;
        String text = mTextEditor.getText().toString();
        Log.d(TAG, "setEmojiEditor: text = " + text);

        CharSequence charSequence = mEmojiImpl.getEmojiExpression(text, true);
        mTextEditor.setText(charSequence);

        if (mTextEditor.hasFocus()) {
            mEmojiBtn.setEnabled(true);
            mEmojiBtn.setImageDrawable(mPluginResource.getDrawable(R.drawable.emoji_btn_focused));
        } else {
            mEmojiBtn.setEnabled(false);
            mEmojiBtn.setImageDrawable(mPluginResource.getDrawable(R.drawable.emoji_btn_normal));
        }
        
        //mTextEditor.setOnTouchListener(new OnTouchListener() {
        //    @Override
        //    public boolean onTouch(View v, MotionEvent event) {
        //        showEmojiPanel(false);
        //        return false;
       //     }
        //});

        mTextEditor.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    showEmojiPanel(false);
                    mEmojiBtn.setEnabled(false);
                    mEmojiBtn.setImageDrawable(mPluginResource.getDrawable(R.drawable.emoji_btn_normal));
                } else {
                    mEmojiBtn.setEnabled(true);
                    mEmojiBtn.setImageDrawable(mPluginResource.getDrawable(R.drawable.emoji_btn_focused));
                }
            }
        });

       if (pageAdapter != null) {
           pageAdapter.notifyDataSetChanged();
       }
    }

    private void initEmojiButton() {
        Log.d(TAG, "initEmojiButton");
        LinearLayout bottomPanel = (LinearLayout)mViewParent;
        
        // init emoji button
        XmlResourceParser xrp = mPluginResource.getLayout(R.layout.emoji_button);
        LayoutInflater inflater = (LayoutInflater) mHostActivity.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        mEmojiBtn = (ImageButton)inflater.inflate(xrp, null);
//        mEmojiBtn = new ImageButton(mHostActivity);

        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        mEmojiBtn.setLayoutParams(lp);
        mEmojiBtn.setImageDrawable(mPluginResource.getDrawable(R.drawable.emoji_btn_normal));
//        mEmojiBtn.getBackground().setAlpha(0);
        mEmojiBtn.setVisibility(View.VISIBLE);
        mEmojiBtn.setTag(R.drawable.emoji_btn_normal);
        bottomPanel.addView(mEmojiBtn, 2);

        mEmojiBtn.setOnClickListener(new android.view.View.OnClickListener() {
             @Override
             public void onClick(View v) {
            //    if(mEmojiBtn.getTag() == R.drawable.emoji_btn_normal){
             if (!isEmojiPanelShow) {
                    mCallback.hideSharePanel();//hideIpSharePanel();
                    mEmojiBtn.setImageDrawable(mPluginResource.getDrawable(R.drawable.emoji_btn_focused));
                    mEmojiBtn.setTag(R.drawable.emoji_btn_focused);
                    showEmojiPanel(true);
                } else {
                    mEmojiBtn.setImageDrawable(mPluginResource.getDrawable(R.drawable.emoji_btn_normal));
                    mEmojiBtn.setTag(R.drawable.emoji_btn_normal);
                    showEmojiPanel(false);
                }
             }
         });

    }

    // true: show panel
    // false: hide panel
    public void showEmojiPanel(boolean isShow) {
        Log.d(TAG, "showEmojiPanel");
        if (isEmojiPanelShow == isShow) {
  //          return;
        }
        isEmojiPanelShow = isShow;
        if (mEmojiView == null) {
            initEmojiPanel();
        }
        //call host API to hide imm and sharepanel
    //    getHost().hideKeyboard();
    //    getHost().hideSharePanel();          
        if (isShow) {
            mEmojiView.setVisibility(View.VISIBLE);
        } else {
            mEmojiView.setVisibility(View.GONE);    
        }  
    }
    
    private void initEmojiPanel() {
        Log.d(TAG, "initEmojiPanel");
      
        initEmojiView();
        initEmojiViewPager();
        initPageIndicator();
        showEmojiPanel(isEmojiPanelShow);
        
    }

    private void initEmojiView() {
        Log.d(TAG, "initEmojiView");
        LinearLayout bottomPanel = (LinearLayout)mViewParent;
        LinearLayout bottomLinear = (LinearLayout) bottomPanel.getParent();
        int index = bottomLinear.indexOfChild(bottomPanel);
        if (mEmojiView != null) {
            bottomLinear.removeViews(index + 1, 1);
        }
        // init emoji panel
        XmlResourceParser xrp = mPluginResource.getLayout(R.layout.emoji_viewpager);
        LayoutInflater inflater = (LayoutInflater) mHostActivity.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        mEmojiView =  inflater.inflate(xrp, null);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT); 
        mEmojiView.setLayoutParams(params);
        mEmojiView.setTag(mEmojiView);
        bottomLinear.addView(mEmojiView, index + 1);
    }

    
    public void initEmojiViewPager() {
        Log.d(TAG, "initEmojiViewPager");

        mEmojiViewPager = (ViewPager) mEmojiView.findViewById(R.id.emoji_view);
        mOrientation = mPluginResource.getConfiguration().orientation;
        Log.d(TAG, "mOrientation = " + mOrientation);
        ViewGroup.LayoutParams params = mEmojiViewPager.getLayoutParams();
        if (mOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            params.height = dip2px(108);
            ITEMS_COLUMN = 10;
        } else {
            params.height = dip2px(216);
            ITEMS_COLUMN = 5;
        }
        mEmojiViewPager.setLayoutParams(params);
        int emojiCount = EmojiConstants.emojiUnicodes.length;
        List<Integer> listItems = new ArrayList<Integer>();
        for (int i = 0; i < emojiCount; i++) {
            
            listItems.add(EmojiConstants.emojiImags[i]);  
            //add delete icon on the last of every page
            if (i % ITEMS_PER_PAGE == (ITEMS_PER_PAGE - 1)) {
                listItems.add(R.drawable.emoji_delete);
            }
        }

        mPageCount = listItems.size() / (ITEMS_PER_PAGE + 1);
        boolean hasMod = false;
        if (listItems.size() % (ITEMS_PER_PAGE + 1) != 0) {
            hasMod = true;
            mPageCount += 1;
        }
        mResIdsMap.clear();
        for (int pageNo = 0; pageNo < mPageCount; pageNo++) {
            int pageStart = pageNo * (ITEMS_PER_PAGE + 1);
            int pageStop = 0;
            if (pageNo == mPageCount - 1) {
                if (hasMod) {
                    pageStop = listItems.size();
                } else {
                    pageStop = (pageNo + 1) * (ITEMS_PER_PAGE + 1);
                }
            } else {
                pageStop = (pageNo + 1) * (ITEMS_PER_PAGE + 1);
            }
            mResIdsMap.put(pageNo, listItems.subList(pageStart, pageStop));
        }
        
        mGridViews = new ArrayList<GridView>();
        for (int i = 0; i < mPageCount; i++) {
            
            XmlResourceParser xrp = mPluginResource.getLayout(R.layout.share_flipper);
            LayoutInflater inflater = (LayoutInflater) mHostActivity.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            //LinearLayout v = (LinearLayout)inflater.inflate(xrp, null);
            GridView gridView =  (GridView)inflater.inflate(xrp, null);
//            GridView gridView = (GridView)v.findViewById(R.id.gv_share_gridview);

            
//            GridView gridView = new GridView(mHostActivity);
//            gridView.setVerticalScrollBarEnabled(false);
            gridView.setGravity(Gravity.CENTER); 
            gridView.setNumColumns(ITEMS_COLUMN);
       //    gridView.setVerticalSpacing(dip2px(8));
       //     gridView.setBackgroundDrawable(mPluginResource.getDrawable(R.drawable.smiley_panel_bg));
      //      gridView.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
            
//            Drawable drawable = mPluginContext.getResources().getDrawable(R.drawable.smiley_item_bg);
//            gridView.setSelector(drawable);
            pageAdapter = new PageAdapter(mHostActivity, mResIdsMap.get(i), i);
            pageAdapter.unregisterListener();
            pageAdapter.registerListener(this);
            gridView.setAdapter(pageAdapter);
            mGridViews.add(gridView);
        }

        PagerAdapter mPagerAdapter = new PagerAdapter() {
        
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
              ((ViewPager) container).removeView(mGridViews.get(position));
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
        };

        mEmojiViewPager.setAdapter(mPagerAdapter);  
        mEmojiViewPager.setCurrentItem(0);
        mEmojiViewPager.setOnPageChangeListener(new GuidePageChangeListener());

    }


    private void initPageIndicator() {
        // int page indicator
        mPageIndicator = (LinearLayout) mEmojiView.findViewById(R.id.page_indicator);
        mCurrentPage = 0;
        for (int pageNo = 0; pageNo < mPageCount; pageNo++) {            
            ImageView indicator = new ImageView(mHostActivity);
            indicator.setPadding(dip2px(5), dip2px(5), dip2px(5), dip2px(5));
            if (pageNo == 0) {
                indicator.setImageDrawable(mPluginResource.getDrawable(R.drawable.emoji_page_focused));
            } else {
                indicator.setImageDrawable(mPluginResource.getDrawable(R.drawable.emoji_page_unfocused));
            }
            mPageIndicator.addView(indicator);
        }
    }

    private int dip2px(float dpValue) {
        float scale = mPluginResource.getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    private void updatePageIndicator(int page) {
        Log.d(TAG, "updatePageIndicator() page = " + page);
        mCurrentPage = page;
        for (int i = 0; i < mPageCount; i++) {
            if (i == mCurrentPage) {
                ((ImageView)mPageIndicator.getChildAt(i)).setImageDrawable(mPluginResource.getDrawable(
                    R.drawable.emoji_page_focused));
            } else {
                 ((ImageView)mPageIndicator.getChildAt(i)).setImageDrawable(mPluginResource.getDrawable(
                    R.drawable.emoji_page_unfocused));
            }
        }
    }

    class GuidePageChangeListener implements OnPageChangeListener {
    
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

    public void onEmotionItemSelectedListener(PageAdapter adapter, int position) {
        Log.d(TAG, "onEmotionItemSelectedListener position=" + position);
        
        if (position >= 0 && position < ITEMS_PER_PAGE) {
            int index = mCurrentPage * ITEMS_PER_PAGE + position;
            Log.d(TAG, "onEmotionItemSelectedListener index=" + index);
            Log.d(TAG, "onEmotionItemSelectedListener emojiUnicode=" + EmojiConstants.emojiUnicodes[index]);
            //Bitmap bitmap = BitmapFactory.decodeResource(mPluginResource,
            //        EmojiConstants.emojiImags[index]);
            //ImageSpan imageSpan = new ImageSpan(mHostActivity, bitmap);
            
            String emoji = new String(Character.toChars(Integer.parseInt(EmojiConstants.emojiUnicodes[index], 16)));       
            SpannableString spannableString = new SpannableString(emoji);
            //spannableString.setSpan(imageSpan, 0, emoji.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); 
            insertEmoji(spannableString);
        } else {
           // emiji delete button pressed
           deleteEmoji();
        
        }
    }

    public boolean isEmojiPanelShow() { 
        return isEmojiPanelShow;
    }

    public void resetEmojiPanel(Configuration newConfig) {
        Log.d(TAG, "resetEmojiPanel(), orientation = " + newConfig.orientation);
        initEmojiPanel();
    }
    
}