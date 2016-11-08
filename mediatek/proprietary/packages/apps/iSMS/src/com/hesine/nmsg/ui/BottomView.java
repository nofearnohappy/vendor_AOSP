package com.hesine.nmsg.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.hesine.hstat.util.DevInfo;
import com.hesine.nmsg.R;
import com.hesine.nmsg.business.bean.LocationInfo;
import com.hesine.nmsg.business.bo.SendSystemInfo;
import com.hesine.nmsg.business.dao.Config;
import com.hesine.nmsg.common.DeviceInfo;
import com.hesine.nmsg.common.GlobalData;
import com.hesine.nmsg.common.MLog;

public class BottomView extends LinearLayout {
    public static final class MsgType {
        public static final int MORE_ACTION = 0;
        public static final int SEND_MSG = 1;
        public static final int SCROLL_TO_BOTTOM = 2;
        public static final int HIDE_MORE_ACTION = 3;
    }

    // private Context mContext;
    private View convertView;
    private ImageView mMore;
    private EditText mEdit;
    private ImageView mSend;
    private ActionListener mListener;
    private Context mContext;

    public BottomView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        convertView = LayoutInflater.from(context).inflate(R.layout.bottom_view, this, true);
        mMore = (ImageView) convertView.findViewById(R.id.more);
        mMore.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showKeyBoard(false);
                mListener.doAction(MsgType.MORE_ACTION);
            }
        });

        mEdit = (EditText) convertView.findViewById(R.id.edit);
        mEdit.clearFocus();
        mEdit.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mEdit.onTouchEvent(event);
                // showKeyBoard(true);
                mListener.doAction(MsgType.SCROLL_TO_BOTTOM);
                mListener.doAction(MsgType.HIDE_MORE_ACTION);
                return true;
            }
        });
        mEdit.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
                if (arg0.length() > 0) {
                    mSend.setImageResource(R.drawable.send_btn_selector);
                    String value = arg0.toString();
                    if (value.equalsIgnoreCase("*#NMSGDEBUG#")) {
                        nmsgInfoDialog();
                    } else if (value.equalsIgnoreCase("*#NMSGLOCATION#")) {
                        nmsgLocationDialog();
                    }else if (value.equalsIgnoreCase("*#NMSGLOGINFO#")) {
                        MLog.setLogPriority(MLog.LOG_INFO);
                        Toast.makeText(mContext, "log changed to info", Toast.LENGTH_SHORT).show();;
                    }else if (value.equalsIgnoreCase("*#NMSGLOGERROR#")) {
                        MLog.setLogPriority(MLog.LOG_ERROR);
                        Toast.makeText(mContext, "log changed to error", Toast.LENGTH_SHORT).show();;
                    }
                } else {
                    mSend.setImageResource(R.drawable.ic_send_disabled);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {

            }

            @Override
            public void afterTextChanged(Editable arg0) {

            }
        });
        mSend = (ImageView) convertView.findViewById(R.id.send);
        mSend.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (!TextUtils.isEmpty(getContent())) {
                    showKeyBoard(false);
                    mListener.doAction(MsgType.SEND_MSG);
                }
            }
        });
    }

    public void showKeyBoard(boolean flag) {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (flag) {
            mEdit.requestFocus();
            imm.showSoftInput(mEdit, 0);
        } else {
            imm.hideSoftInputFromWindow(mEdit.getWindowToken(), 0);
        }
    }

    public void addActionListener(ActionListener listener) {
        mListener = listener;
    }

    public String getContent() {
        return mEdit.getEditableText().toString();
    }

    public String getContentForSend() {
        String content = mEdit.getEditableText().toString();
        mEdit.setText("");
        return content;
    }

    private void nmsgInfoDialog() {
        String appVersion = null;
        PackageManager manager = mContext.getPackageManager();
        try {
            PackageInfo info = manager.getPackageInfo(mContext.getPackageName(), 0);
            appVersion = info.versionName; // 版本名
        } catch (NameNotFoundException e) {
            MLog.error(MLog.getStactTrace(e));
        }

        new AlertDialog.Builder(mContext)
                .setItems(
                        new String[] { "version:" + appVersion, "pn:" + Config.getPnToken(),
                                "uuid:" + Config.getUuid(), "imsi:" + Config.getImsi(),
                                "model:" + DeviceInfo.getDeviceModel(), "brand:" + DeviceInfo.getDeviceBrand(),
                                "Hstat Id:" + DevInfo.getDeviceId(mContext) }, null)
                .setNegativeButton(R.string.btn_ok, null).show();
    }
    
    private void nmsgLocationDialog() {
        final String[] str = new String[] { "河北省", "河南省", "山东省", "天津市", "海南省", "西藏自治区", "广西壮族自治区",
                "新疆维吾尔自治区" };
        new AlertDialog.Builder(mContext).setItems(str, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                LocationInfo loc = new LocationInfo();
                loc.setProvince(str[which]);
                GlobalData.instance().getSystemInfo().setLocation(loc);
                SendSystemInfo.updateSystemInfo(null);
            }
        }).show();
    }
}
