package com.hesine.nmsg.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
//import android.widget.RadioGroup;
import android.widget.TextView;

import com.hesine.nmsg.R;
import com.hesine.nmsg.business.bean.UserInfo;
import com.hesine.nmsg.business.dao.Config;
import com.hesine.nmsg.business.dao.DBUtils;
import com.hesine.nmsg.common.EnumConstants;
import com.hesine.nmsg.common.MLog;
import com.hesine.nmsg.ui.SexDialog.SexActionListener;

public class UserInfoActivity extends Activity implements AdapterView.OnItemClickListener,
        View.OnClickListener {
    private HeaderView mHeader;
    private TextView accountName;
    private TextView accountTel;
    // private TextView nmsgSetting;
    private LinearLayout nmsgSettingLayout;
    private View nmsgSettingLine;
    private CircularImage accountAvadar;
    // private RadioGroup sexCheckButton;
    // private RadioButton maleButton;
    // private RadioButton femaleButton;
    private UserInfo mUserInfo;
    private String mUserAccount;
    // private String mFromScreen;
    private TextView accountSex;
    // private static final int IMAGE_REQUEST_CODE = 0;
    // private static final int RESULT_REQUEST_CODE = 1;
    private AlertDialog chooseAvadarDialog;
    private int[] userAvadars = new int[] { R.drawable.avadar1, R.drawable.avadar2,
            R.drawable.avadar3, R.drawable.avadar4, R.drawable.avadar5, R.drawable.avadar6,
            R.drawable.avadar7, R.drawable.avadar8, R.drawable.avadar9 };// ,

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_info_layout);
        mUserAccount = getIntent().getStringExtra(EnumConstants.NMSG_INTENT_EXTRA_ACCOUNT);

        mUserInfo = DBUtils.getUser(mUserAccount);
        if (mUserInfo == null) {
            mUserInfo = new UserInfo();
            mUserInfo.setName("");
            mUserInfo.setSex(0);
            mUserInfo.setPhoneNum("");
            mUserInfo.setIcon(0);
            mUserInfo.setAccount(mUserAccount);
            MLog.info("userInfo == null");
        }
        initView();
        setUser();

    }

    private void initView() {
        mHeader = (HeaderView) findViewById(R.id.header);
        mHeader.getMoreView().setVisibility(View.GONE);
        mHeader.setTitle(this.getResources().getString(R.string.personal_setting));
        accountName = (TextView) findViewById(R.id.account_name);
        accountName.setOnClickListener(this);
        accountTel = (TextView) findViewById(R.id.account_tel);
        accountTel.setOnClickListener(this);
        accountAvadar = (CircularImage) findViewById(R.id.account_avatar);

        nmsgSettingLayout = (LinearLayout) findViewById(R.id.nmsg_setting_layout);
        nmsgSettingLayout.setVisibility(View.VISIBLE);
        if (!EnumConstants.NETWORK_SETTING_SWITCH){
            TextView nmsgSetting = (TextView) findViewById(R.id.nmsg_setting);
            nmsgSetting.setText(getResources().getString(R.string.nmsg_receive_setting));
        }
        nmsgSettingLine = (View) findViewById(R.id.nmsg_setting_line_view);
        nmsgSettingLine.setVisibility(View.VISIBLE);
        // nmsgSetting = (TextView) findViewById(R.id.nmsg_setting);
        nmsgSettingLayout.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(UserInfoActivity.this, NmsgSettingActivity.class);
                intent.putExtra(EnumConstants.NMSG_INTENT_EXTRA_ACCOUNT, Config.getUuid());
                UserInfoActivity.this.startActivity(intent);
            }
        });

        accountSex = (TextView) findViewById(R.id.account_sex);
        accountSex.setOnClickListener(this);
        // maleButton = (RadioButton) findViewById(R.id.radio_male);
        // femaleButton = (RadioButton) findViewById(R.id.radio_female);
        // sexCheckButton.setOnCheckedChangeListener(new
        // RadioGroup.OnCheckedChangeListener() {
        // @Override
        // public void onCheckedChanged(RadioGroup group, int checkedId) {
        // switch (checkedId) {
        // case R.id.radio_male:
        // mUserInfo.setSex(0);// male
        // break;
        // case R.id.radio_female:
        // mUserInfo.setSex(1);// female
        // break;
        // }
        // }
        // });
        accountAvadar.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                chooseAvadarDialog();
            }
        });
    }

    private void setUser() {
        if (!mUserInfo.getName().isEmpty()) {
            accountName.setText(mUserInfo.getName());
        }
        if (!mUserInfo.getPhoneNum().isEmpty()) {
            accountTel.setText(mUserInfo.getPhoneNum());
        }
        accountAvadar.setImageResource(userAvadars[mUserInfo.getIcon()]);
        if (mUserInfo.getSex() == 0) {
            accountSex.setText(getResources().getString(R.string.account_sex_male));
        } else {
            accountSex.setText(getResources().getString(R.string.account_sex_female));
        }
    }

    protected void onPause() {
        super.onPause();
        if (mUserInfo != null) {
            mUserInfo.setName((String) accountName.getText());
            mUserInfo.setPhoneNum((String) accountTel.getText());
            DBUtils.updateUser(mUserInfo);
        }
    }

    BaseAdapter imageAdapter;

    private void chooseAvadarDialog() {
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.choose_avadar_gridview, null);
        GridView logosGridView = (GridView) layout.findViewById(R.id.share_gridview);

        imageAdapter = new BaseAdapter() {
            public int getCount() {
                return userAvadars.length;
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
                    imageView = new ImageView(UserInfoActivity.this);
                    imageView.setScaleType(ScaleType.FIT_XY);
                    imageView.setLayoutParams(new GridView.LayoutParams(getResources()
                            .getDimensionPixelSize(R.dimen.list_avatar_width), getResources()
                            .getDimensionPixelSize(R.dimen.list_avatar_width)));
                } else {
                    imageView = (ImageView) converToView;
                }
                imageView.setImageBitmap(null);
                imageView.setBackgroundResource(userAvadars[position]);
                if (mUserInfo.getIcon() == position) {
                    imageView.setImageResource(R.drawable.avadar_bg);
                }
                return imageView;
            }
        };

        logosGridView.setAdapter(imageAdapter);
        logosGridView.setOnItemClickListener(this);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(layout);
        chooseAvadarDialog = builder.create();
        chooseAvadarDialog.show();
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {

        accountAvadar.setImageResource(userAvadars[arg2]);
        chooseAvadarDialog.dismiss();
        mUserInfo.setIcon(arg2);
        // imageAdapter.notifyDataSetChanged();
        // ((ImageView)arg1).setImageResource(R.drawable.avadar_bg);
    }

    @Override
    public void onClick(View v) {
        if (v == accountName) {
            setNameorTelNumber(0, accountName.getText().toString());
        } else if (v == accountTel) {
            setNameorTelNumber(1, accountTel.getText().toString());
        } else if (v == accountSex) {
            final SexDialog sexDialog = new SexDialog(this).show();
            if (mUserInfo.getSex() == 0) {
                sexDialog.setMaleChecked(true);
            } else if (mUserInfo.getSex() == 1) {
                sexDialog.setFemaleChecked(true);
            }
            sexDialog.setListener(new SexActionListener() {

                @Override
                public void doAction(View v) {
                    switch (v.getId()) {
                        case R.id.check_male:
                            sexDialog.setFemaleChecked(false);
                            sexDialog.setMaleChecked(true);
                            mUserInfo.setSex(0);
                            accountSex.setText(getResources().getString(R.string.account_sex_male));
                            break;
                        case R.id.check_female:
                            sexDialog.setMaleChecked(false);
                            sexDialog.setFemaleChecked(true);
                            mUserInfo.setSex(1);
                            accountSex.setText(getResources()
                                    .getString(R.string.account_sex_female));
                            break;
                        default:
                            break;
                    }
                    sexDialog.dismiss();
                }
            });
        }

    }

    private void setNameorTelNumber(final int position, String value) {

        final EditAccoutDialog editAccountDialog = new EditAccoutDialog(this)
                .setInfo(position == 0 ? R.string.account_set_name : R.string.account_set_number)
                .setLeft(R.string.btn_cancel).setRight(R.string.btn_ok);
        if (position == 0) {
            editAccountDialog.setEditTextInputType(InputType.TYPE_CLASS_TEXT);
            editAccountDialog.setFilters(new InputFilter[] { new InputFilter.LengthFilter(32) });
        } else {
            editAccountDialog.setEditTextInputType(InputType.TYPE_CLASS_NUMBER);
            editAccountDialog.setFilters(new InputFilter[] { new InputFilter.LengthFilter(20) });
        }

        editAccountDialog.setText("");
        editAccountDialog.append(value);

        editAccountDialog.setListener(new ActionListener() {

            @Override
            public void doAction(int type) {
                if (type == EditAccoutDialog.ActionType.ACTION_RIGHT) {

                    String editTextString = editAccountDialog.getText().toString();
                    if (!TextUtils.isEmpty(editTextString)) {
                        if (position == 0) {
                            accountName.setText(editTextString);
                        } else {
                            accountTel.setText(editTextString);
                        }
                    }
                }
                editAccountDialog.dismiss();
            }
        });
        editAccountDialog.show();
    }

}

class SexDialog implements OnClickListener {
    // private View pickSex;
    private CheckBox checkMale;
    private CheckBox checkFemale;

    public static class ActionType {
        public static final int ACTION_MALE = 0;
        public static final int ACTION_FEMALE = 1;
    }

    private Context mContext;
    private SexActionListener mListener;
    private Dialog mDialog;
    private Object mStore;

    public SexDialog(Context context) {
        mContext = context;
        mDialog = new Dialog(mContext, R.style.Theme_pop_dialog);
        mDialog.setContentView(R.layout.pick_sex);
        checkMale = (CheckBox) mDialog.findViewById(R.id.check_male);
        checkFemale = (CheckBox) mDialog.findViewById(R.id.check_female);
        checkMale.setOnClickListener(this);
        checkFemale.setOnClickListener(this);
    }

    public SexDialog show() {
        mDialog.show();
        return this;
    }

    public SexDialog setCancelable(boolean cancelable) {
        mDialog.setCancelable(cancelable);
        return this;
    }

    public SexDialog setListener(SexActionListener listener) {
        mListener = listener;
        return this;
    }

    public void setTag(Object obj) {
        mStore = obj;
    }

    public Object getTag() {
        return mStore;
    }

    public void dismiss() {
        mDialog.dismiss();
    }

    public void setMaleChecked(boolean b) {
        checkMale.setChecked(b);
    }

    public void setFemaleChecked(boolean b) {
        checkFemale.setChecked(b);
    }

    public boolean isMaleChecked() {
        return checkMale.isChecked();
    }

    public boolean isFemaleChecked() {
        return checkFemale.isChecked();
    }

    interface SexActionListener {
        void doAction(View v);
    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        if (mListener == null) {
            return;
        }
        mListener.doAction(v);
    }
}
