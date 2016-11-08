package com.mediatek.rcs.pam.ui.conversation;

import com.mediatek.rcs.pam.R;
import com.mediatek.rcs.pam.ui.conversation.LevelControlLayout.OnScrollToScreenListener;

import android.content.Context;
import android.content.res.Configuration;
//import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

public class PaSharePanel extends LinearLayout {

    private static final String TAG = "PaSharePanel";

    private Handler mHandler;
    private Context mContext;
    private View mConvertView;
    private LevelControlLayout mScrollLayout;
    private LinearLayout mSharePanelMain;
    private RadioButton mDotFirst;
    private RadioButton mDotSec;

    private int mOrientation;
    private int[] mColumnArray;
    private int mScreenIndex;

    // TODO maybe use Common variable directly
    public static final int ACTION_SHARE = 0;
    public static final String SHARE_ACTION = "shareAction";
    private String[] mSource;

    public static final int IPMSG_TAKE_PHOTO = 100;
    public static final int IPMSG_RECORD_VIDEO = 101;
    public static final int IPMSG_RECORD_AUDIO = 102;
    public static final int IPMSG_CHOOSE_PHOTO = 104;
    public static final int IPMSG_CHOOSE_VIDEO = 105;
    public static final int IPMSG_CHOOSE_AUDIO = 106;
    public static final int IPMSG_SHARE_CONTACT = 108;
    public static final int IPMSG_SHARE_CALENDAR = 109;
    public static final int IPMSG_SHARE_POSITION = 110;

    private final int[] IP_MESSAGE_ACTIONS = { IPMSG_TAKE_PHOTO,
            IPMSG_RECORD_VIDEO, IPMSG_RECORD_AUDIO, IPMSG_SHARE_CONTACT,
            IPMSG_CHOOSE_PHOTO, IPMSG_CHOOSE_VIDEO, IPMSG_CHOOSE_AUDIO,
            IPMSG_SHARE_POSITION };

    private final int[] IP_MSG_SHARE_DRAWABLE_IDS = {
            R.drawable.ipmsg_take_a_photo, R.drawable.ipmsg_record_a_video,
            R.drawable.ipmsg_record_an_audio,

            R.drawable.ipmsg_share_contact, R.drawable.ipmsg_choose_a_photo,
            R.drawable.ipmsg_choose_a_video,

            R.drawable.ipmsg_choose_an_audio, R.drawable.ipmsg_share_location,
    // R.drawable.ipmsg_share_calendar
    };

    public PaSharePanel(Context context) {
        super(context);
        Log.d(TAG, "PaSharePanel construct");
        mContext = context;
    }

    public PaSharePanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        Log.d(TAG, "PaSharePanel construct 2");
        LayoutInflater inflater = LayoutInflater.from(context);
        mConvertView = inflater
                .inflate(R.layout.share_common_panel, this, true);
        mContext = context;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mScrollLayout = (LevelControlLayout) mConvertView
                .findViewById(R.id.share_panel_zone);
        mSharePanelMain = (LinearLayout) mConvertView
                .findViewById(R.id.share_panel_main);
        mDotFirst = (RadioButton) mConvertView.findViewById(R.id.rb_dot_first);
        mDotSec = (RadioButton) mConvertView.findViewById(R.id.rb_dot_sec);
        resetShareItem();
    }

    public void resetShareItem() {
        mOrientation = getResources().getConfiguration().orientation;
        if (mScrollLayout.getChildCount() != 0) {
            mScrollLayout.removeAllViews();
        }
        addSharePage(0);
        mDotFirst.setVisibility(View.VISIBLE);
        mDotFirst.setChecked(true);
        int pageNumber = computePageNumber();
        if (pageNumber == 1) {
            mDotFirst.setVisibility(View.GONE);
            mDotSec.setVisibility(View.GONE);
        }
        if (pageNumber == 2) {
            // total two pages
            addSharePage(1);
            mDotSec.setVisibility(View.VISIBLE);
        } else if (pageNumber > 2) {
            Log.d(TAG, "Wrong page number:" + pageNumber);
        }
        mScrollLayout.setOnScrollToScreen(new OnScrollToScreenListener() {
            @Override
            public void doAction(int whichScreen) {
                mScreenIndex = whichScreen;
                if (whichScreen == 0) {
                    mDotFirst.setChecked(true);
                } else {
                    mDotSec.setChecked(true);
                }
            }
        });
        if (mScreenIndex >= pageNumber) {
            Log.d(TAG, "ScreenIndex lager than total page !!! :" + mScreenIndex);
            mScreenIndex = 0;
        }
        mScrollLayout.setDefaultScreen(mScreenIndex);
        mScrollLayout.autoRecovery();
    }

    private void addSharePage(int index) {
        mColumnArray = getResources().getIntArray(R.array.share_column);
        View v = LayoutInflater.from(mContext).inflate(R.layout.share_flipper,
                mScrollLayout, false);
        GridView gridView = (GridView) v.findViewById(R.id.gv_share_gridview);
        android.view.ViewGroup.LayoutParams params = mSharePanelMain
                .getLayoutParams();
        if (mOrientation != Configuration.ORIENTATION_LANDSCAPE) {
            params.height = getResources().getDimensionPixelOffset(
                    R.dimen.share_panel_port_height);
        } else {
            params.height = getResources().getDimensionPixelOffset(
                    R.dimen.share_panel_lan_height);
        }
        mSharePanelMain.setLayoutParams(params);
        if (mOrientation == Configuration.ORIENTATION_PORTRAIT) {
            gridView.setNumColumns(mColumnArray[0]);
        } else {
            gridView.setNumColumns(mColumnArray[1]);
        }
        ShareAdapter adapter = new ShareAdapter(getLableArray(index),
                getIconArray(index));
        adapter.setIndex(index);
        gridView.setAdapter(adapter);
        gridView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                Message msg = mHandler.obtainMessage();
                Bundle bundle = new Bundle();
                int actionPosition = getActionId(position);
                Log.d(TAG, "onItemClick. position=" + actionPosition);
                if (actionPosition < 0
                        || actionPosition >= IP_MESSAGE_ACTIONS.length) {
                    return;
                }
                bundle.putInt(SHARE_ACTION, IP_MESSAGE_ACTIONS[actionPosition]);
                msg.setData(bundle);
                msg.what = ACTION_SHARE;
                mHandler.sendMessage(msg);
            }
        });
        mScrollLayout.addView(v);
    }

    private String[] getLableArray(int index) {
        mSource = getResources().getStringArray(R.array.share_string_array);

        int onePage;
        if (mOrientation == Configuration.ORIENTATION_PORTRAIT) {
            onePage = mColumnArray[0] * 2;
        } else {
            onePage = mColumnArray[1];
        }
        if (index == 0) {
            String[] index0 = new String[onePage];
            int count = onePage > mSource.length ? mSource.length : onePage;
            for (int i = 0; i < count; i++) {
                index0[i] = mSource[i];
            }
            return index0;
        } else {
            int count = mSource.length - onePage;
            String[] index1 = new String[count];
            for (int i = 0; i < count; i++) {
                index1[i] = mSource[onePage + i];
            }
            return index1;
        }
    }

    private int[] getIconArray(int index) {
        // add for ipmessage
        int[] source = IP_MSG_SHARE_DRAWABLE_IDS;

        int onePage;
        if (mOrientation == Configuration.ORIENTATION_PORTRAIT) {
            onePage = mColumnArray[0] * 2;
        } else {
            onePage = mColumnArray[1];
        }
        if (index == 0) {
            int[] index0 = new int[onePage];
            int count = onePage > source.length ? source.length : onePage;
            for (int i = 0; i < count; i++) {
                index0[i] = source[i];
            }
            return index0;
        } else {
            int count = source.length - onePage;
            int[] index1 = new int[count];
            for (int i = 0; i < count; i++) {
                index1[i] = source[onePage + i];
            }
            return index1;
        }
    }

    private int getActionId(int position) {
        int onePage;
        if (mOrientation == Configuration.ORIENTATION_PORTRAIT) {
            onePage = mColumnArray[0] * 2;
        } else {
            onePage = mColumnArray[1];
        }
        if (mScreenIndex == 0) {
            return position;
        } else {
            return onePage + position;
        }
    }

    public void setHandler(Handler handler) {
        mHandler = handler;
    }

    public void recycleView() {
        if (mScrollLayout != null && mScrollLayout.getChildCount() != 0) {
            mScrollLayout.removeAllViews();
        }
    }

    private int computePageNumber() {
        int numberArray[] = getResources().getIntArray(R.array.share_column);
        int onePage;
        if (mOrientation == Configuration.ORIENTATION_PORTRAIT) {
            onePage = numberArray[0] * 2;
        } else {
            onePage = numberArray[1];
        }
        int pages = mSource.length / onePage
                + (mSource.length % onePage == 0 ? 0 : 1);
        return pages;
    }

    private class ShareAdapter extends BaseAdapter {

        private String[] mStringArray;
        private int[] mIconArray;
        private int mIndex;

        public ShareAdapter(String[] stringArray, int[] iconArray) {
            mStringArray = stringArray;
            mIconArray = iconArray;
        }

        public void setIndex(int index) {
            mIndex = index;
        }

        @Override
        public int getCount() {
            int count = 0;
            if (mOrientation == Configuration.ORIENTATION_PORTRAIT) {
                count = mColumnArray[0] * 2;
            } else {
                count = mColumnArray[1];
            }
            return count;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(final int position, View convertView,
                ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(
                        R.layout.share_grid_common_item, null);
                convertView.setTag(convertView);
            } else {
                convertView = (View) convertView.getTag();
            }

            TextView text = (TextView) convertView
                    .findViewById(R.id.tv_share_name);
            ImageView img = (ImageView) convertView
                    .findViewById(R.id.iv_share_icon);
            int actionPosition = mIndex * getCount() + position;
            // Resources resource = getResources();
            if (position < mIconArray.length) {
                text.setText(mStringArray[position]);
                img.setImageResource(mIconArray[position]);
            } else {
                Log.d(TAG, "getView error position=" + actionPosition);
            }
            return convertView;
        }
    }

}
