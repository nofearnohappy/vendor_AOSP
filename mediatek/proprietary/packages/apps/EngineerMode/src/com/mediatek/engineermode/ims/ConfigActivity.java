package com.mediatek.engineermode.ims;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.XmlResourceParser;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;

import com.mediatek.engineermode.R;
import com.mediatek.engineermode.Elog;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public class ConfigActivity extends Activity {
    private static final String TAG = "EM/ImsConfig";
    private static final int TYPE_NUMBER = 0;
    private static final int TYPE_TEXT = 1;
    private static final int TYPE_SINGLE = 2;
    private static final int TYPE_MULTI = 3;
    private static final int MSG_QUERY = 0;
    private static final int MSG_SET = 1;
    private static final int DIALOG_WAIT = 0;
    private static final int DIALOG_MANUAL = 1;

    private String mCategory = null;
    private ArrayList<Setting> mSettings = new ArrayList<Setting>();
    private ArrayList<SettingView> mSettingViews = new ArrayList<SettingView>();
    private ViewGroup mList;
    private ProgressDialog mProgressDialog;
    private Toast mToast;
    private Phone mPhone = null;
    private int mQuerying = 0;
    private String mSettingRule =
        "Setting Rule:<digit of list num><list num><mnc_len><MNC><mcc_len><MCC>...";

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_QUERY) {
                AsyncResult ar = (AsyncResult) msg.obj;
                if (ar.exception == null) {
                    String[] data = (String[]) ar.result;
                    if ((data != null) && (data.length > 0) && (data[0] != null)) {
                        ((SettingView) ar.userObj).setValue(parseCommandResponse(data[0]));
                        if (((SettingView)ar.userObj).setting.label.equals("mncmcc check")){
                            if (parseCommandResponse(data[0]).equals("1")){
                                sendCommand("mncmcc_whitelist", getSettingView("white list"));
                            }
                        }
                        if (((SettingView)ar.userObj).setting.label.equals("white list")){
                            ((SettingView)ar.userObj).label.setText("white list: \n"
                                + parseCommandResponse(data[0]));
                        }
                    }
                } else {
                    Toast.makeText(ConfigActivity.this,
                            "Query failed for " + ((SettingView) ar.userObj).setting.label,
                            Toast.LENGTH_SHORT).show();
                }
                mQuerying--;
                if (mQuerying == 0) {
                    mProgressDialog.dismiss();
                }
            } else if (msg.what == MSG_SET) {
                AsyncResult ar = (AsyncResult) msg.obj;
                if (ar.exception == null) {
                    showToast("Set successful.");
                } else {
                    showToast("Set failed.");
                }
            }
        }
    };

    /**
     * Setting class.
     */
    private class Setting {
        public String label;
        public int type = 0;
        public ArrayList<String> entries = new ArrayList<String>();
        public ArrayList<Integer> values = new ArrayList<Integer>();
        public String defaultValue;
        public String suffix = "";
    }

    /**
     * SettingView class.
     */
    private abstract class SettingView extends FrameLayout {
        public Setting setting;
        public Button button;
        public TextView label;

        public SettingView(Context context, Setting setting) {
            super(context);
            LayoutInflater inflater = getLayoutInflater();
            View convertView = inflater.inflate(R.layout.ims_config_view, null);
            addView(convertView);
            this.setting = setting;
            label = (TextView) findViewById(R.id.ims_config_label);
            TextView suffix = (TextView) findViewById(R.id.ims_config_suffix);
            button = (Button) findViewById(R.id.ims_config_set);
            label.setText(setting.label + ":");
            suffix.setText(setting.suffix);

            if (setting.label.equals("white list")) {
                Elog.d(TAG, "setting.label" + setting.label);
                button.setVisibility(View.GONE);
            }
            button.setOnClickListener(new OnClickListener() {
                public void onClick(View view) {
                    send();
                }
            });
            if (setting.label.equals("Authentication")) {
                sendCommand("UA_net_ipsec", this);
            } else if (setting.label.equals("mncmcc check")) {
                sendCommand("mncmcc_pass_flag", this);
            } else if (setting.label.equals("white list")) {
                Elog.d(TAG, "Don't do anything!");
                mQuerying--;
            } else if (setting.label.equals(mSettingRule)){
                Elog.d(TAG, "Don't do anything!");
                mQuerying--;
            } else {
                sendCommand(setting.label, this);
            }
            mQuerying++;
        }

        public void send() {
            if (setting.label.equals("ims_roaming_mode")) {
                SystemProperties.set("sys.ims.roaming", getValue());
            }
            if (setting.label.equals("Authentication")) {
                sendCommand("UA_net_ipsec", getValue());
                if (getValue().equals("1")) {
                    sendCommand("UA_reg_http_digest", "0");
                } else {
                    sendCommand("UA_reg_http_digest", "1");
                }
            } else if (setting.label.equals("force_user_account_by_manual")
                    && getValue().equals("1")) {
                showDialog(DIALOG_MANUAL);
            } else if (setting.label.equals("mncmcc check")) {
                Elog.d(TAG, "button.getText().toString()" + button.getText().toString());
                sendCommand("mncmcc_pass_flag", getValue());
            } else if (setting.label.equals("white list")){
                sendCommand("mncmcc_whitelist", this);
            } else if (setting.label.equals(mSettingRule)){
                sendCommand("mncmcc_whitelist", getValue());
            } else {
                sendCommand(setting.label, getValue());
            }
        }

        abstract protected String getValue();
        abstract protected void setValue(String value);
    }

    /**
     * SettingEditTextView class.
     */
    private class SettingEditTextView extends SettingView {
        public EditText mEditText;

        public SettingEditTextView(Context context, Setting setting, int type) {
            super(context, setting);
            mEditText = (EditText) findViewById(R.id.ims_config_edit_text);
            if (type == TYPE_NUMBER) {
                mEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
            }
            findViewById(R.id.ims_config_edit_layout).setVisibility(View.VISIBLE);
            mEditText.setText(setting.defaultValue);
            if (setting.label.equals("white list"))
            {
                mEditText.setVisibility(View.GONE);
            }
        }

        protected String getValue() {
            return mEditText.getText().toString();
        }

        protected void setValue(String value) {
            mEditText.setText(value);
        }
    }

    /**
     * SettingSingleSelectView class.
     */
    private class SettingSingleSelectView extends SettingView {
        private RadioButton[] mRadios;
        private RadioGroup mRadioGroup;

        public SettingSingleSelectView(Context context, Setting setting) {
            super(context, setting);
            mRadioGroup = (RadioGroup) findViewById(R.id.ims_config_radio_group);
            mRadioGroup.setVisibility(View.VISIBLE);
            mRadioGroup.removeAllViews();
            mRadios = new RadioButton[setting.entries.size()];
            for (int i = 0; i < setting.entries.size(); i++) {
                RadioButton radio = new RadioButton(ConfigActivity.this);
                radio.setText(setting.entries.get(i));
                radio.setTag(setting.values.get(i));
                mRadioGroup.addView(radio);
                mRadios[i] = radio;
                if (setting.values.get(i) == parseInt(setting.defaultValue)) {
                    mRadioGroup.check(radio.getId());
                }
            }
        }

        protected String getValue() {
            for (int i = 0; i < mRadios.length; i++) {
                if (mRadios[i].isChecked()) {
                    return String.valueOf((Integer) mRadios[i].getTag());
                }
            }
            return "";
        }

        protected void setValue(String value) {
            Log.d("@M_" + TAG, "setValue " + setting.label + ", " + value);
            int integerValue = parseInt(value);
            for (int i = 0; i < mRadios.length; i++) {
                if (integerValue == (Integer) mRadios[i].getTag()) {
                    mRadioGroup.check(mRadios[i].getId());
                }
            }
        }
    }

    /**
     * SettingMultiSelectView class.
     */
    private class SettingMultiSelectView extends SettingView {
        private CheckBox[] mCheckboxes;

        public SettingMultiSelectView(Context context, Setting setting) {
            super(context, setting);
            ViewGroup checkboxList = (ViewGroup) findViewById(R.id.ims_config_checkbox_list);
            checkboxList.setVisibility(View.VISIBLE);
            checkboxList.removeAllViews();
            mCheckboxes = new CheckBox[setting.entries.size()];
            for (int i = 0; i < setting.entries.size(); i++) {
                CheckBox checkbox = new CheckBox(ConfigActivity.this);
                checkbox.setText(setting.entries.get(i));
                checkbox.setTag(setting.values.get(i));
                if ((setting.values.get(i) & parseInt(setting.defaultValue)) > 0) {
                    checkbox.setChecked(true);
                }
                checkboxList.addView(checkbox);
                mCheckboxes[i] = checkbox;
            }
        }

        protected String getValue() {
            int checked = 0;
            for (int i = 0; i < mCheckboxes.length; i++) {
                if (mCheckboxes[i].isChecked()) {
                    checked |= (Integer) mCheckboxes[i].getTag();
                }
            }
            return String.valueOf(checked);
        }

        protected void setValue(String value) {
            int integerValue = parseInt(value);
            for (int i = 0; i < mCheckboxes.length; i++) {
                if (integerValue > 0 && (integerValue & ((Integer) mCheckboxes[i].getTag())) > 0) {
                    mCheckboxes[i].setChecked(true);
                } else {
                    mCheckboxes[i].setChecked(false);
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ims_config);
        mCategory = getIntent().getStringExtra("category");
        setTitle(mCategory);
        mList = (ViewGroup) findViewById(R.id.list);

        getXMLContent(getResources().getXml(R.xml.ims_config));
        int subId = SubscriptionManager.getDefaultDataSubId();
        Log.i("@M_" + TAG, "sub id " + subId);
        int phoneId = SubscriptionManager.getPhoneId(subId);
        Log.i("@M_" + TAG, "phone id " + phoneId);
        int phoneCount = TelephonyManager.getDefault().getPhoneCount();
        Log.i("@M_" + TAG, "phone count " + phoneCount);
        mPhone = PhoneFactory.getPhone(phoneId >= 0 && phoneId < phoneCount ? phoneId : 0);
        showDialog(DIALOG_WAIT);
        initializeViews();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        switch (id) {
        case DIALOG_WAIT:
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setTitle("Querying");
            mProgressDialog.setMessage("Querying");
            mProgressDialog.setCancelable(false);
            mProgressDialog.setIndeterminate(true);
            return mProgressDialog;
        case DIALOG_MANUAL:
            LayoutInflater inflater = getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.ims_config, null);
            ViewGroup list = (ViewGroup) dialogView.findViewById(R.id.list);
            final ArrayList<SettingView> originalViews = new ArrayList<SettingView>();
            final ArrayList<SettingView> views = new ArrayList<SettingView>();
            for (SettingView s : mSettingViews) {
                if (s.setting.label.equals("manual_impi") || s.setting.label.equals("manual_impu")
                        || s.setting.label.equals("manual_domain_name")) {
                    SettingView v = new SettingEditTextView(this, s.setting, TYPE_TEXT);
                    v.setValue(s.getValue());
                    originalViews.add(s);
                    views.add(v);
                    v.findViewById(R.id.ims_config_set).setVisibility(View.GONE);
                    list.addView(v);
                }
            }
            return new AlertDialog.Builder(this)
                    .setTitle("Manual Settings")
                    .setView(dialogView)
                    .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                for (int i = 0; i < views.size(); i++) {
                                    views.get(i).send();
                                    originalViews.get(i).setValue(views.get(i).getValue());
                                }
                                originalViews.get(0).requestFocus();
                                sendCommand("force_user_account_by_manual", "1");
                            }
                        })
                    .setNegativeButton("Cancel", null).create();
        default:
            return super.onCreateDialog(id);
        }
    }

    private void showToast(String msg) {
        if (mToast != null) {
            mToast.cancel();
        }
        mToast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        mToast.show();
    }

    private void sendCommand(String name, String value) {
        Message msg = mHandler.obtainMessage(MSG_SET);
        mPhone.invokeOemRilRequestStrings(
                new String[] {"AT+ECFGSET=\"" + name + "\",\"" + value + "\"", ""}, msg);
    }

    private void sendCommand(String name, View obj) {
        Message msg = mHandler.obtainMessage(MSG_QUERY, obj);
        mPhone.invokeOemRilRequestStrings(
                new String[] {"AT+ECFGGET=\"" + name + "\"", "+ECFGGET:"}, msg);
    }

    private String parseCommandResponse(String data) {
        Log.d("@M_" + TAG, "raw data: " + data);
        Pattern p = Pattern.compile("\\+ECFGGET:\\s*\".*\"\\s*,\\s*\"(.*)\"");
        Matcher m = p.matcher(data);
        while (m.find()) {
            String value = m.group(1);
            Log.d("@M_" + TAG, "value: " + value);
            return value;
        }
        Log.e("@M_" + TAG, "wrong format: " + data);
        showToast("wrong format: " + data);
        return "";
    }

    private int parseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            showToast("Wrong integer format: " + s);
            return -1;
        }
    }

    private void getXMLContent(XmlResourceParser parser) {
        Setting setting = new Setting();
        String text = "";
        String category = "";
        try {
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                case XmlPullParser.START_TAG:
                    if ("setting".equals(parser.getName())) {
                        setting = new Setting();
                        category = "";
                    } else if ("option".equals(parser.getName())) {
                        setting.entries.add(parser.getAttributeValue(null, "name"));
                        setting.values.add(parseInt(parser.getAttributeValue(null, "value")));
                    }
                    text = "";
                    break;
                case XmlPullParser.END_TAG:
                    String name = parser.getName();
                    if ("label".equals(name)) {
                        setting.label = text;
                    } else if ("suffix".equals(name)) {
                        setting.suffix = text;
                    } else if ("category".equals(name)) {
                        category = text;
                    } else if ("type".equals(name)) {
                        setting.type = parseInt(text);
                    } else if ("default".equals(name)) {
                        setting.defaultValue = text;
                    } else if ("setting".equals(name)) {
                        if (mCategory.equals(category)) {
                            mSettings.add(setting);
                        }
                    }
                    break;
                case XmlPullParser.TEXT:
                    text = parser.getText();
                    break;
                default:
                    break;
                }
                eventType = parser.next();
            }
        } catch (IOException e) {
            Log.e("@M_" + TAG, "");
        } catch (XmlPullParserException e) {
            Log.e("@M_" + TAG, "");
        }
    }

    private void initializeViews() {
        mList.removeAllViews();
        for (Setting setting : mSettings) {
            SettingView view = null;
            switch (setting.type) {
            case TYPE_NUMBER:
            case TYPE_TEXT:
                view = new SettingEditTextView(this, setting, setting.type);
                break;
            case TYPE_SINGLE:
                view = new SettingSingleSelectView(this, setting);
                break;
            case TYPE_MULTI:
                view = new SettingMultiSelectView(this, setting);
                break;
            default:
                break;
            }
            if (view != null) {
                mList.addView(view);
                mSettingViews.add(view);
            }
        }
    }
    private SettingView getSettingView (String label)
    {
        for (SettingView settingView : mSettingViews){
            if (settingView.setting.label.equals(label)){
                return settingView;
            }
        }
        return null;
    }
}
