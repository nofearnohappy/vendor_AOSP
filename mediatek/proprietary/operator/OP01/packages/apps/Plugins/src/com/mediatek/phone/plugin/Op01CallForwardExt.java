package com.mediatek.phone.plugin;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncResult;
import android.os.Message;
import android.os.Handler;
import android.preference.EditTextPreference;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.CallForwardInfo;
import com.android.internal.telephony.CallForwardInfoEx;

import com.mediatek.common.PluginImpl;
import com.mediatek.op01.plugin.R;
import com.mediatek.phone.callforward.CallForwardTimeActivity;
import com.mediatek.phone.callforward.CallForwardUtils;
import com.mediatek.phone.ext.DefaultCallForwardExt;
import com.mediatek.phone.ext.ICallForwardExt.ICfiAction;

@PluginImpl(interfaceName="com.mediatek.phone.ext.ICallForwardExt")
public class Op01CallForwardExt extends DefaultCallForwardExt {
    private static final String LOG_TAG = "Op01CallForwardExt";
    private static final boolean DBG = true;

    private static final String BUTTON_CFU_KEY = "button_cfu_key";
    private static final int SET_TIME_REQUEST = 1260;
    private Context mPluginContext = null;
    private Activity mActivity = null;

    private static final int MESSAGE_GET_CF = 0;
    private static final int MESSAGE_SET_CF = 1;
    
    private static final int NO_BUTTON = -1;
    private static final int FROM_TIME_BUTTON = 0;
    private static final int TO_TIME_BUTTON = 1;
    private int setFromToTimeButtonLabel = NO_BUTTON;

    private static final int FROM_TIME = 0;
    private static final int TO_TIME = 1;

    private long[] mTimeSlot = null;
    private long tempFromTime = Long.MAX_VALUE;
    private long tempToTime = Long.MAX_VALUE;
    private boolean  supportVoLTETimeSlot = false;
    private static final long CLICK_BUTTON_INTERVAL = 500L;
    private long fromTimeButtonLastClickTime = 0L;
    private long toTimeButtonLastClickTime = 0L;

    private CheckBox mTimeSlotCheckBox = null;
    private Button mFromTimeButton = null;
    private Button mToTimeButton = null;

    private static final String TIMER_ACTION = "android.intent.action.FROM_TO_TIMER";
    private static PendingIntent mTimerSender = null;
    private static ICfiAction mCfiAction = null;
    private static AlarmManager mAlarmMg = null;
    private Phone mPhone = null;
    private Handler mHandler = null;

    /**
     * get activity and subId
     * @param activity
     * @param subId
     */
    @Override
    public void onCreate(Activity activity, int subId) {
        if (!CallForwardUtils.isSupportVolteIms()) {
            log("not support volte or ims");
            return;
        }

        log("onCreate(), subId: " + subId);
        if (activity == null || !CallForwardUtils.isValidSubId(activity, subId)) {
            log("onCreate(), activity is null or subId is not valid" );
            return;
        }
        mActivity = activity;
        mPhone = CallForwardUtils.getPhoneUsingSubId(subId);
        log("onCreate(), mPhone: " + mPhone + "mActivity: " + mActivity);

        try {
            mPluginContext = activity.createPackageContext("com.mediatek.op01.plugin", 
                    Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
        } catch (NameNotFoundException e) {
            log("catch no found com.mediatek.op01.plugin");
        }

        tempFromTime = Long.MAX_VALUE;
        tempToTime = Long.MAX_VALUE;
        setFromToTimeButtonLabel = NO_BUTTON;
    }

    /**
     * custom number editor dialog
     * @param context
     * @param preference
     * @param view
     */
    @Override
    public void onBindDialogView(EditTextPreference preference, View view) {
        if (!CallForwardUtils.isSupportVolteIms()) {
            log("not support volte or ims");
            return;
        }

        log("onBindDialogView");
        if (mActivity == null || mPluginContext == null || preference == null || view == null) {
            log("mActivity is null or mPluginContext or preference or view is null");
            return;
        }
        if (!preference.getKey().equals(BUTTON_CFU_KEY)) {
            log("not BUTTON_CFU_KEY, so return");
            return;
        }

        tempFromTime = Long.MAX_VALUE;
        tempToTime = Long.MAX_VALUE;
        setFromToTimeButtonLabel = NO_BUTTON;
        /*try {
            mPluginContext = context.createPackageContext("com.mediatek.op01.plugin", 
                    Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
        } catch (NameNotFoundException e) {
            log("catch no found com.mediatek.op01.plugin");
        }*/

        Resources resource = mActivity.getResources();
        String packageName = mActivity.getPackageName();
        ViewGroup callForwardViewGroup = (ViewGroup) view;
        int numberIndex = callForwardViewGroup.indexOfChild((View)view.findViewById(resource.
                getIdentifier("number_field", "id", packageName)));

        if (supportVoLTETimeSlot) {
            String timeSlot = mPluginContext.getString(R.string.time_slot);
            LinearLayout timeSlotLayout = newTimeSlotLayoutView(timeSlot, mActivity);
            callForwardViewGroup.addView(timeSlotLayout, numberIndex+1);

            RelativeLayout timelayout = newTimeLayoutView(mActivity);
            callForwardViewGroup.addView(timelayout, numberIndex+2);

            long time[] = getTimeSlot();
            if(time != null) {
                setTimeSlot(time);
            } else {
                String timeFrom =  mPluginContext.getString(R.string.time_from);
                String timeTo =  mPluginContext.getString(R.string.time_to); 
                long nowTime = CallForwardUtils.getNowTime();
                timeFrom = timeFrom + "    " +
                        CallForwardUtils.getHourMinute(nowTime, mPluginContext);
                timeTo =  timeTo + "    " + CallForwardUtils.getHourMinute(nowTime, mPluginContext);
                if (mFromTimeButton != null) {
                    mFromTimeButton.setText(timeFrom);
                    tempFromTime = nowTime;
                }
                if (mToTimeButton != null) {
                    mToTimeButton.setText(timeTo);
                    tempToTime = nowTime;
                }
            }
        } else {
            if (mToTimeButton != null && mFromTimeButton != null) {
                if (callForwardViewGroup.getChildAt(numberIndex+2) != null) {
                    callForwardViewGroup.removeViewAt(numberIndex+2);
                }
                mToTimeButton = null;
                mFromTimeButton = null;
            }
            if (mTimeSlotCheckBox != null) {
                if (callForwardViewGroup.getChildAt(numberIndex+1) != null) {
                    callForwardViewGroup.removeViewAt(numberIndex+1);
                }
                mTimeSlotCheckBox = null;
            }
        }
    }

    /**
     * onDialogClosed
     * @param preference Edit Text Preference.
     * @param action Commands Interface Action.
     * @return true when time slot is the same.
     */
    @Override
    public boolean onDialogClosed(EditTextPreference preference, int action) {
        if (!CallForwardUtils.isSupportVolteIms()) {
            log("onDialogClosed(),not support volte or ims");
            return false;
        }

        if (mActivity == null || mPluginContext == null) {
            log("mActivity is null or mPluginContext is null");
            return false;
        }

        if (!preference.getKey().equals(BUTTON_CFU_KEY)) {
            log("onDialogClosed(), not BUTTON_CFU_KEY, so return");
            return false;
        }

        if(action != CommandsInterface.CF_ACTION_REGISTRATION) {
            log("onDialogClosed(), not CF_ACTION_REGISTRATION , so return");
            return false;
        }

        if (mTimeSlotCheckBox == null || !mTimeSlotCheckBox.isChecked() || !supportVoLTETimeSlot) {
            log("onDialogClosed(),mTimeSlotCheckBox is null," +
                    "or isChecked(): false, or not support volte time slot");
            return false;
        }

        long timeSlot[] = getTimeSlot();
        if (tempFromTime != Long.MAX_VALUE && tempToTime != Long.MAX_VALUE) {
            long tempTimeSlot[] = {tempFromTime, tempToTime};
            if (CallForwardUtils.compareTime(tempTimeSlot, mPluginContext)) {
                Toast.makeText(mActivity, mPluginContext.getString(R.string.time_slot_hint),
                        Toast.LENGTH_LONG).show();
                tempFromTime = Long.MAX_VALUE;
                tempToTime = Long.MAX_VALUE;
                log("onDialogClosed(), time slot is same");
                return true;
            }
        } else if (timeSlot != null && timeSlot.length == 2) {
            long tempTimeSlot[] = {0, 0};
            if (tempFromTime != Long.MAX_VALUE) {
                tempTimeSlot[0] = tempFromTime;
                tempTimeSlot[1] = timeSlot[1];
            } else if (tempToTime != Long.MAX_VALUE) {
                tempTimeSlot[0] = timeSlot[0];
                tempTimeSlot[1] = tempToTime;
            } else {
                tempTimeSlot[0] = timeSlot[0];
                tempTimeSlot[1] = timeSlot[1];
            }

           if (CallForwardUtils.compareTime(tempTimeSlot, mPluginContext)) {
                Toast.makeText(mActivity, mPluginContext.getString(R.string.time_slot_hint),
                        Toast.LENGTH_LONG).show();
                tempFromTime = Long.MAX_VALUE;
                tempToTime = Long.MAX_VALUE;
                log("onDialogClosed(), time slot is same");
                return true;
            }
        }
        return false;
    }

    /**
     * get result
     * @param requestCode
     * @param resultCode
     * @param data
     * @return true when get CallForwardTimeActivity result
     */
    @Override
    public boolean onCallForwardActivityResult(int requestCode, int resultCode, Intent data) {
        if (!CallForwardUtils.isSupportVolteIms()) {
            log("not support volte or ims");
            return false;
        }

        log("onCallForwardActivityResult");
        if (resultCode != Activity.RESULT_OK) {
            return false;
        }

        if (mPluginContext == null) {
            log("onCallForwardActivityResult(), mPluginContext is null");
            return false;
        }

        Intent resultIntent = data;
        boolean timeLabel = resultIntent.getBooleanExtra("timeLabel", false);
        if (timeLabel) {
            long time = resultIntent.getLongExtra("time", Long.MAX_VALUE);
            log("onCallForwardActivityResult time: " + time);
            if (time != Long.MAX_VALUE) {
                if (setFromToTimeButtonLabel == FROM_TIME_BUTTON) {
                    tempFromTime = time;
                    String timeFrom =  mPluginContext.getString(R.string.time_from);
                    timeFrom = timeFrom + "    " +
                            CallForwardUtils.getHourMinute(time, mPluginContext);
                    if (mFromTimeButton != null) {
                        mFromTimeButton.setText(timeFrom);
                    }
                } else if (setFromToTimeButtonLabel == TO_TIME_BUTTON) {
                    tempToTime = time;
                    String timeTo =  mPluginContext.getString(R.string.time_to);
                    timeTo = timeTo + "    " +
                            CallForwardUtils.getHourMinute(time, mPluginContext);
                    if (mToTimeButton != null) {
                        mToTimeButton.setText(timeTo);
                    }
                }
            }
            setFromToTimeButtonLabel = NO_BUTTON;
            return true;
        }
        return false;
    }

    /**
     * get Call Forward Time Slot
     * @param preference
     * @param message
     * @param handler
     * @return true when CFU and support volte ims
     */
    @Override
    public boolean getCallForwardInTimeSlot(EditTextPreference preference, Message message,
            Handler handler) {
        if (!CallForwardUtils.isSupportVolteIms()) {
            log("not support volte or ims");
            return false;
        }

        log("getCallForwardInTimeSlot()");
        if (preference != null && !preference.getKey().equals(BUTTON_CFU_KEY)) {
            log("getCallForwardInTimeSlot(), not BUTTON_CFU_KEY, so return");
            return false;
        }

        if(mPhone == null || handler == null) {
            log("getCallForwardInTimeSlot(), mPhone or handler is null, so return");
            return false;
        }
        mHandler = handler;

        Message getCFMessage = null;
        if (message != null && message.what == MESSAGE_SET_CF) {
            if (!supportVoLTETimeSlot) {
                return false;
            }
            AsyncResult ar = (AsyncResult) message.obj;
            getCFMessage = handler.obtainMessage(MESSAGE_GET_CF, message.arg1, 
                    MESSAGE_SET_CF, ar.exception);
        } else {
            getCFMessage = handler.obtainMessage(MESSAGE_GET_CF,
                    // unused in this case
                    CommandsInterface.CF_ACTION_DISABLE,
                    MESSAGE_GET_CF, null);
        }

        mPhone.getCallForwardInTimeSlot(CommandsInterface.CF_REASON_UNCONDITIONAL, getCFMessage);
        return true;
    }

    /**
     * set CallForward for time slot
     * @param preference preference
     * @param action action
     * @param number number
     * @param time time
     * @param handler handler
     * @return true when CFU and support volte ims
     */
    @Override
    public boolean setCallForwardInTimeSlot(EditTextPreference preference, int action,
            String number, int time, Handler handler) {
        if (!CallForwardUtils.isSupportVolteIms()) {
            log("not support volte or ims");
            return false;
        }

        log("setCallForwardInTimeSlot()");
        if (preference != null && !preference.getKey().equals(BUTTON_CFU_KEY)) {
            log("setCallForwardInTimeSlot(), not BUTTON_CFU_KEY, so return");
            return false;
        }

        if(mPhone == null || mHandler == null) {
            log("setCallForwardInTimeSlot(), mPhone or handler is null, so return");
            return false;
        }

        if (!supportVoLTETimeSlot) {
            log("not support VoLTE Time Slot");
            return false;
        }

        if (mHandler != handler) {
            mHandler = handler;
        }

        boolean timeSlotCheckBox = false;
        if (mTimeSlotCheckBox != null && mTimeSlotCheckBox.isChecked()) {
            timeSlotCheckBox = true;
            log("setCallForwardInTimeSlot(), timeSlotCheckBox: " + timeSlotCheckBox);
        }

        long timeSlot[] = getTimeSlot();
        if (timeSlotCheckBox && tempFromTime != Long.MAX_VALUE && tempToTime != Long.MAX_VALUE) {
            long tempTimeSlot[] = {tempFromTime, tempToTime};
            Message setCFMessage = mHandler.obtainMessage(MESSAGE_SET_CF, action, MESSAGE_SET_CF);
            mPhone.setCallForwardInTimeSlot(action, CommandsInterface.CF_REASON_UNCONDITIONAL,
                    number, time, tempTimeSlot, setCFMessage);
            log("setCallForwardInTimeSlot(), tempTimeSlot1: " + tempTimeSlot);
            tempFromTime = Long.MAX_VALUE;
            tempToTime = Long.MAX_VALUE;
        } else if (timeSlotCheckBox && timeSlot != null && timeSlot.length == 2) {
            long tempTimeSlot[] = {0,0};
            if (tempFromTime != Long.MAX_VALUE) {
                tempTimeSlot[0] = tempFromTime;
                tempTimeSlot[1] = timeSlot[1];
            } else if (tempToTime != Long.MAX_VALUE) {
                tempTimeSlot[0] = timeSlot[0];
                tempTimeSlot[1] = tempToTime;
            } else {
                tempTimeSlot[0] = timeSlot[0];
                tempTimeSlot[1] = timeSlot[1];
            }

            Message setCFMessage = mHandler.obtainMessage(MESSAGE_SET_CF, action, MESSAGE_SET_CF);
            mPhone.setCallForwardInTimeSlot(action, CommandsInterface.CF_REASON_UNCONDITIONAL,
                    number, time, tempTimeSlot, setCFMessage);
            log("setCallForwardInTimeSlot(), tempTimeSlot2: " + tempTimeSlot);
            tempFromTime = Long.MAX_VALUE;
            tempToTime = Long.MAX_VALUE;
        } else {
            Message setCFMessage = mHandler.obtainMessage(MESSAGE_SET_CF, action, MESSAGE_SET_CF);
            mPhone.setCallForwardInTimeSlot(action, CommandsInterface.CF_REASON_UNCONDITIONAL,
                    number, time, null, setCFMessage);
            log("setCallForwardInTimeSlot(), tempFromTime: " + tempFromTime
                    + " tempToTime: " + tempToTime);
        }
        return true;
    }

    /**
     * handle Get CF Time Slot Response
     * @param preference
     * @param msg
     * @return true when not support cfu volte time slot set
     */
    @Override
    public boolean handleGetCFInTimeSlotResponse(EditTextPreference preference, Message msg) {
        if (!CallForwardUtils.isSupportVolteIms()) {
            log("not support volte or ims");
            return false;
        }

        log("handleGetCFInTimeSlotResponse()");
        if (preference != null && !preference.getKey().equals(BUTTON_CFU_KEY)) {
            log("handleGetCFInTimeSlotResponse(), not BUTTON_CFU_KEY, so return");
            return false;
        }

        if(mPhone == null || msg == null || mHandler == null) {
            log("handleGetCFInTimeSlotResponse(), phone or msg or handler is null, so return");
            return false;
        }

        AsyncResult ar = (AsyncResult) msg.obj;
        CommandException exception = null;
        if (ar.exception instanceof CommandException) {
            exception = (CommandException)ar.exception;
        }
        if (exception != null && exception.getCommandError() ==
                CommandException.Error.SPECAIL_UT_COMMAND_NOT_SUPPORTED) {
            log("handleGetCFInTimeSlotResponse: ar.exception=" + ar.exception);
            Message getCFMessage;
            if (msg != null && msg.arg2 == MESSAGE_SET_CF ) {
                getCFMessage = mHandler.obtainMessage(MESSAGE_GET_CF, msg.arg1,
                        MESSAGE_SET_CF, null);
            } else {
                getCFMessage = mHandler.obtainMessage(MESSAGE_GET_CF,
                        CommandsInterface.CF_ACTION_DISABLE, MESSAGE_GET_CF, null);
            }
            mPhone.getCallForwardingOption(CommandsInterface.CF_REASON_UNCONDITIONAL, getCFMessage);
            supportVoLTETimeSlot = false;
            setTimeSlot(null);
            return true;
        } else if (ar.result instanceof CallForwardInfoEx[]){
            supportVoLTETimeSlot = true;
        }

        if(ar.result instanceof CallForwardInfoEx[]) {
            CallForwardInfoEx cfInfoExArray[] = (CallForwardInfoEx[]) ar.result;
            CallForwardInfo cfInfoArray[] = new CallForwardInfo[cfInfoExArray.length];
            boolean timeSlotFlag = false;
            for (int i = 0, length = cfInfoExArray.length; i < length; i++) {
                cfInfoArray[i] = new CallForwardInfo();
                cfInfoArray[i].status = cfInfoExArray[i].status;
                cfInfoArray[i].reason = cfInfoExArray[i].reason;
                cfInfoArray[i].serviceClass = cfInfoExArray[i].serviceClass;
                cfInfoArray[i].toa = cfInfoExArray[i].toa;
                cfInfoArray[i].number = cfInfoExArray[i].number;
                cfInfoArray[i].timeSeconds = cfInfoExArray[i].timeSeconds;
                log("handleGetCFInTimeSlotResponse(), cfInfoExArray[i].timeSlot: "
                        + cfInfoExArray[i].timeSlot);
                if (cfInfoExArray[i].timeSlot != null) {
                     setTimeSlot(cfInfoExArray[i].timeSlot);
                     timeSlotFlag = true;
                } else if ((i == cfInfoExArray.length -1) && !timeSlotFlag) {
                     setTimeSlot(null);
                }
            }

            ar.result = (Object) cfInfoArray;
            msg.obj = (Object) ar;
        }
        return false;
    }

    /**
     * add time slot for summary text
     * @param values
     */
    @Override
    public void updateSummaryTimeSlotText(EditTextPreference preference, String values[]) {
        if (!CallForwardUtils.isSupportVolteIms()) {
            log("not support volte or ims");
            return;
        }

        log("updateSummaryTimeSlotText()");
        if (preference != null && !preference.getKey().equals(BUTTON_CFU_KEY)) {
            log("updateSummaryTimeSlotText(), not BUTTON_CFU_KEY, so return");
            return;
        }

        //String numberAndTimeSlot = values[0] + "\n From 12:00 To 12:00";
        long timeSlot[] = getTimeSlot();
        if (timeSlot != null) {
            String timeFrom = mPluginContext.getString(R.string.time_from);
            String timeTo = mPluginContext.getString(R.string.time_to);
            String strFromTime = timeFrom + "  " + CallForwardUtils.getHourMinute(timeSlot[0], mPluginContext);
            String strToTime = timeTo + "  " + CallForwardUtils.getHourMinute(timeSlot[1], mPluginContext);
            if(values != null && values.length > 0) {
                String numberAndTimeSlot = values[0] + "\n" + strFromTime + "    " + strToTime;
                values[0] = numberAndTimeSlot;
            }
        }
    }

    /**
     * update time slot cfu icon
     * @param subId
     * @param context
     * @param cfiAction
     */
    @Override
    public void updateCfiIcon(int subId, Context context, ICfiAction cfiAction) {
        if (!CallForwardUtils.isSupportVolteIms()) {
            log("not support volte or ims");
            return;
        }

        log("updateCfiIcon() subId: " + subId + " cfiAction:" + cfiAction);
        if(context == null || cfiAction == null) {
            log("updateCfiIcon(), context or cfiAction is null ");
            return;
        }
        mCfiAction = cfiAction;
        mAlarmMg = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

        if (!registFlag) {
            IntentFilter intentFilter = new IntentFilter(TIMER_ACTION);
            intentFilter.setPriority(1000);
            context.registerReceiver(mReceiver, intentFilter);
            registFlag = true;
            log("updateCfiIcon registerReceiver");

            Intent timerIntent = new Intent();
            timerIntent.setAction(TIMER_ACTION);
            timerIntent.putExtra("subId", subId);
            mTimerSender = PendingIntent.getBroadcast(context, 1, timerIntent, 0);
        } 
        Phone phone = CallForwardUtils.getPhoneUsingSubId(subId);
        long timeSlot[] = phone.getTimeSlot();
        if (registFlag && (timeSlot != null && timeSlot.length == 2)) {
            log("updateCfiIcon() timeSlot[0]: " + timeSlot[0] + " timeSlot[1]: " + timeSlot[1]);
            cancelAlarmTime();
            setAlarmTime(context, subId, timeSlot);
        } else {
            cancelAlarmTime();
            if (registFlag) {
                context.unregisterReceiver(mReceiver);
                registFlag = false;
                log("updateCfiIcon unregisterReceiver");
            }
        }
        //cfiAction.updateCfiEx(visible, subId);
    }

    /**
     * from time button OnClickListener
     * @return OnClickListener
     */
    public OnCheckedChangeListener onTimeSlotCheckedChangeListener() {
        return new CompoundButton.OnCheckedChangeListener(){ 
            @Override 
            public void onCheckedChanged(CompoundButton buttonView, 
                    boolean isChecked) { 
                // TODO Auto-generated method stub 
                if(isChecked){ 
                    mFromTimeButton.setEnabled(true);
                    mToTimeButton.setEnabled(true);
                }else{ 
                    mFromTimeButton.setEnabled(false);
                    mToTimeButton.setEnabled(false);
                } 
            } 
        };
    }

    /**
     * from time button OnClickListener
     * @return OnClickListener
     */
    public OnClickListener onFromTimeButtonClickListener() {
        return new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mActivity == null) {
                    log("onClick(), mActivity is null");
                    return;
                }
                Intent intent = new Intent(mPluginContext, CallForwardTimeActivity.class);
                long timeSlot[] = getTimeSlot();
                if (tempFromTime != Long.MAX_VALUE) {
                    intent.putExtra("time", tempFromTime);
                } else if (timeSlot != null && timeSlot.length == 2) {
                    intent.putExtra("time", timeSlot[0]);
                } else {
                    long nowTime = CallForwardUtils.getNowTime();
                    intent.putExtra("time", nowTime);
                }
                try {
                    long currentTime = System.currentTimeMillis();
                    if (((currentTime - fromTimeButtonLastClickTime) > CLICK_BUTTON_INTERVAL) ||
                            (fromTimeButtonLastClickTime == 0)) {
                        fromTimeButtonLastClickTime = currentTime;
                        log("startActivity -> CallForwardTimeActivity");
                        setFromToTimeButtonLabel = FROM_TIME_BUTTON;
                        mActivity.startActivityForResult(intent, SET_TIME_REQUEST);
                    }
                    //mCallForwardContext.startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    log(e.toString());
                }
                return;
            }
        };
    }

    /**
     * to time button OnClickListener
     * @return OnClickListener
     */
    public OnClickListener onToTimeButtonClickListener() {
        return new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mActivity == null) {
                    log("onClick(), mActivity is null");
                    return;
                }
                Intent intent = new Intent(mPluginContext, CallForwardTimeActivity.class);
                long timeSlot[] = getTimeSlot();
                if (tempToTime != Long.MAX_VALUE) {
                    intent.putExtra("time", tempToTime);
                } else if (timeSlot != null && timeSlot.length == 2) {
                    intent.putExtra("time", timeSlot[1]);
                } else {
                    long nowTime = CallForwardUtils.getNowTime();
                    intent.putExtra("time", nowTime);
                }
                try {
                    long currentTime = System.currentTimeMillis();
                    if (((currentTime - toTimeButtonLastClickTime) > CLICK_BUTTON_INTERVAL) ||
                            (toTimeButtonLastClickTime == 0)) {
                        toTimeButtonLastClickTime = currentTime;
                        log("startActivity -> CallForwardTimeActivity");
                        setFromToTimeButtonLabel = TO_TIME_BUTTON;
                        mActivity.startActivityForResult(intent, SET_TIME_REQUEST);
                    }
                    //mCallForwardContext.startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    log(e.toString());
                }
                return;
            }
        };
    }

    /**
     * Construct time slot checkbox and textview
     * @param text
     * @param context
     * @return new item LinearLayout
     */
    public LinearLayout newTimeSlotLayoutView(String text, Context context) {
        log("newTimeSlotLayoutView");
        if (context == null || mPluginContext == null) {
            log("newTimeSlotLayoutView(), context or mPluginContext is null");
            return null;
        }
        Resources r = mPluginContext.getResources(); 
        int margin_bottom = r.getDimensionPixelSize(R.dimen.margin_bottom);
        int checkbox_margin_left = r.getDimensionPixelSize(R.dimen.checkbox_margin_left);
        float text_size = CallForwardUtils.px2sp(context, r.getDimension(R.dimen.text_size));

        //new linelayout
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        LinearLayout newTimeSlotLayoutView = new LinearLayout(context);
        params.setMargins(0, 0, 0, margin_bottom);
        newTimeSlotLayoutView.setLayoutParams(params);
        newTimeSlotLayoutView.setOrientation(LinearLayout.HORIZONTAL);
        newTimeSlotLayoutView.setBaselineAligned(false);

        //check box
        mTimeSlotCheckBox = new CheckBox(context);
        LinearLayout.LayoutParams checkBoxparams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        checkBoxparams.setMargins(checkbox_margin_left, 0, 0, 0);
        mTimeSlotCheckBox.setLayoutParams(checkBoxparams);
        mTimeSlotCheckBox.setVerticalFadingEdgeEnabled(true);
        mTimeSlotCheckBox.setFocusable(false);
        long timeSlot[] = getTimeSlot();
        if (timeSlot != null) {
            mTimeSlotCheckBox.setChecked(true);
        } else {
            mTimeSlotCheckBox.setChecked(false);
        }
        mTimeSlotCheckBox.setOnCheckedChangeListener(onTimeSlotCheckedChangeListener());
        newTimeSlotLayoutView.addView(mTimeSlotCheckBox);

        //text view
        LinearLayout.LayoutParams textViewparams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        textViewparams.gravity = Gravity.CENTER_VERTICAL;
        TextView textView = new TextView(context);
        textView.setTextAppearance(context, android.R.attr.textAppearanceMedium);
        textView.setLayoutParams(textViewparams);
        //textView.setGravity(Gravity.CENTER_VERTICAL);
        textView.setText(text);
        textView.setTextSize(text_size);
        newTimeSlotLayoutView.addView(textView);

        return newTimeSlotLayoutView;
    }

    /**
     * Construct fromtime and totime button
     * @param context
     * @return new item RelativeLayout
     */
    public RelativeLayout newTimeLayoutView(Context context) {
        log("newTimeLayoutView");
        if (context == null || mPluginContext == null) {
            log("newTimeLayoutView(), context or mPluginContext is null");
            return null;
        }
        Resources r = mPluginContext.getResources();
        int margin_left = r.getDimensionPixelSize(R.dimen.margin_left);
        int margin_right = r.getDimensionPixelSize(R.dimen.margin_right);
        int button_margin_height = r.getDimensionPixelSize(R.dimen.button_margin_height);
        float text_size = CallForwardUtils.px2sp(context, r.getDimension(R.dimen.text_size));
        Drawable dropdown = r.getDrawable(R.drawable.dropdown_normal_holo_dark);

        //new linelayout
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        RelativeLayout newTimeLayoutView = new RelativeLayout(context);
        newTimeLayoutView.setLayoutParams(params);
        newTimeLayoutView.setHorizontalGravity(RelativeLayout.CENTER_HORIZONTAL);

        //from time button
        mFromTimeButton = new Button(context);
        RelativeLayout.LayoutParams fromTimeButtonparams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, button_margin_height);
        fromTimeButtonparams.setMargins(margin_left, 0, 0, 0);
        mFromTimeButton.setLayoutParams(fromTimeButtonparams);
        mFromTimeButton.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, dropdown, null);
        mFromTimeButton.setEllipsize(null);
        mFromTimeButton.setGravity(Gravity.LEFT|Gravity.CENTER);
        mFromTimeButton.setHorizontalFadingEdgeEnabled(true);
        mFromTimeButton.setFocusable(false);
        mFromTimeButton.setSingleLine();
        mFromTimeButton.setTextAppearance(context, android.R.attr.textAppearanceMedium);
        //mFromTimeButton.setTextAppearance(context, R.style.TextAppearance_EditEvent_SpinnerButton);
        mFromTimeButton.setBackgroundColor(Color.TRANSPARENT);
        //mFromTimeButton.setText(fromText);
        mFromTimeButton.setTextSize(text_size);
        mFromTimeButton.setOnClickListener(onFromTimeButtonClickListener());
        newTimeLayoutView.addView(mFromTimeButton);

        //to time button
        mToTimeButton = new Button(context);
        RelativeLayout.LayoutParams toTimeButtonparams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, button_margin_height);
        toTimeButtonparams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        toTimeButtonparams.setMargins(0, 0, margin_right, 0);
        mToTimeButton.setLayoutParams(toTimeButtonparams);
        mToTimeButton.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, dropdown, null);
        mToTimeButton.setEllipsize(null);
        mToTimeButton.setGravity(Gravity.LEFT|Gravity.CENTER);
        mToTimeButton.setHorizontalFadingEdgeEnabled(true);
        mToTimeButton.setFocusable(false);
        mToTimeButton.setSingleLine();
        mToTimeButton.setTextAppearance(context, android.R.attr.textAppearanceMedium);
        //mToTimeButton.setTextAppearance(context, R.style.TextAppearance_EditEvent_SpinnerButton);
        mToTimeButton.setBackgroundColor(Color.TRANSPARENT);
        //mToTimeButton.setText(toText);
        mToTimeButton.setTextSize(text_size);
        mToTimeButton.setOnClickListener(onToTimeButtonClickListener());
        newTimeLayoutView.addView(mToTimeButton);

        if (mTimeSlotCheckBox.isChecked()) {
            mFromTimeButton.setEnabled(true);
            mToTimeButton.setEnabled(true);
        } else {
            mFromTimeButton.setEnabled(false);
            mToTimeButton.setEnabled(false);
        }

        return newTimeLayoutView;
    }

    /**
     * set time slot
     * @param hour
     * @param minute
     */
    private void setTimeSlot(long[] timeSlot) {
        if (mPluginContext == null) {
            log("setTimeSlot(), mPluginContext is null");
            return;
        }
        String timeFrom =  mPluginContext.getString(R.string.time_from);
        String timeTo =  mPluginContext.getString(R.string.time_to);
        mTimeSlot = timeSlot;
        log("setTimeSlot(), timeSlot: " + timeSlot);
        if(mTimeSlot != null && mTimeSlot.length == 2) {
            timeFrom = timeFrom + "    " +
                    CallForwardUtils.getHourMinute(mTimeSlot[FROM_TIME], mPluginContext);
            timeTo = timeTo + "    " +
                    CallForwardUtils.getHourMinute(mTimeSlot[TO_TIME], mPluginContext);
        } else if(mTimeSlot != null && mTimeSlot.length == 1) {
            timeFrom = timeFrom + "    " +
                    CallForwardUtils.getHourMinute(mTimeSlot[FROM_TIME], mPluginContext);
            long nowTime = CallForwardUtils.getNowTime();
            timeTo =  timeTo + "    " + CallForwardUtils.getHourMinute(nowTime, mPluginContext);
        } else {
            long nowTime = CallForwardUtils.getNowTime();
            timeFrom =  timeFrom + "    " + CallForwardUtils.getHourMinute(nowTime, mPluginContext);
            timeTo =  timeTo + "    " + CallForwardUtils.getHourMinute(nowTime, mPluginContext);
        }

        if (mToTimeButton != null) {
            mToTimeButton.setText(timeTo);
        }
        if (mFromTimeButton != null) {
            mFromTimeButton.setText(timeFrom);
        }
    }

    /**
     * get time slot
     * @return long[] time slot, hour and minute
     */
    private long[] getTimeSlot(){
        return mTimeSlot;
    }

    /**
     * set Alarm Time
     * @param context
     * @param timeSlot
     */
    public void setAlarmTime(Context context, int subId, long[] timeSlot) {
        if ((timeSlot == null) || (timeSlot != null && timeSlot.length != 2)) {
            log("setAlarmTime(), timeSlot is null");
            return;
        }

        log("setAlarmTime()");
        long triggerTime = getTriggerTime(timeSlot, subId);
        if (triggerTime == Long.MAX_VALUE) {
            log("wrong triggerTime");
            return;
        }
        log("triggerTime: " + CallForwardUtils.getHourMinute(triggerTime, context));

        if(mAlarmMg != null && mTimerSender != null) {
            mAlarmMg.setExact(AlarmManager.RTC_WAKEUP, triggerTime, mTimerSender);
        }
    }

    /**
     * cancel Alarm Time
     */
    public void cancelAlarmTime(){
        log("cancelAlarmTime()");
        if(mTimerSender != null && mAlarmMg != null) {
            mAlarmMg.cancel(mTimerSender);
        }
    }

    private boolean registFlag = false;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            log("onReceive action: " + intent.getAction());
            int subId = intent.getIntExtra("subId", -1);
            log("onReceive action subId: " + subId);
            if (intent.getAction().equals(TIMER_ACTION)) {
                if(mCfiAction == null || subId == -1) {
                    log("onReceive mCfiAction is null or subId is -1");
                    return;
                }

                Phone phone = CallForwardUtils.getPhoneUsingSubId(subId);
                long timeSlot[] = phone.getTimeSlot();

                long triggerTime = getTriggerTime(timeSlot, subId);
                if (triggerTime == Long.MAX_VALUE) {
                    log("wrong triggerTime");
                    cancelAlarmTime();
                    return;
                }
                if (context != null) {
                    log("triggerTime: " + CallForwardUtils.getHourMinute(triggerTime, context));
                } else {
                    log("triggerTime: " + triggerTime);
                }

                cancelAlarmTime();
                if(mAlarmMg != null && mTimerSender != null) {
                    mAlarmMg.setExact(AlarmManager.RTC_WAKEUP, triggerTime, mTimerSender);
                }
            } 
        }
    };

    /**
     * get volte time slot trigger time.
     * @param timeSlot network time slot.
     * @param subId sim subId.
     * @return trigger time.
     */
    public long getTriggerTime(long[] timeSlot, int subId) {
        if ((timeSlot == null) || (timeSlot != null && timeSlot.length != 2)
                || mCfiAction == null ) {
            return Long.MAX_VALUE;
        }

        long nowTime = CallForwardUtils.getNowTime();
        long triggerTime= nowTime;
        long fromTime = timeSlot[0];
        long toTime = timeSlot[1];
        log("getTriggerTime()1, fromTime:" + fromTime + " toTime: " + toTime +
                " nowTime:" + nowTime);

        fromTime = CallForwardUtils.getTime(CallForwardUtils.getHour(fromTime),
                CallForwardUtils.getMinute(fromTime), 0);
        toTime = CallForwardUtils.getTime(CallForwardUtils.getHour(toTime),
                CallForwardUtils.getMinute(toTime), 59);
        log("getTriggerTime() 2, fromTime:" + fromTime + " toTime: " + toTime +
                " nowTime:" + nowTime);

        if (toTime > fromTime && 
                (nowTime > fromTime && nowTime < toTime)){
            triggerTime = toTime;
            mCfiAction.updateCfiEx(subId, true);
            log("toTime > fromTime && (nowTime > fromTime && nowTime < toTime)");
        } else if(toTime > fromTime && 
                (nowTime > fromTime && nowTime > toTime)) {
            triggerTime = fromTime + 24*60*60*1000;
            mCfiAction.updateCfiEx(subId, false);
            log("toTime > fromTime && (nowTime > fromTime && nowTime > toTime)");
        } else if(toTime > fromTime && 
                (nowTime < fromTime && nowTime < toTime)) {
            triggerTime = fromTime;
            mCfiAction.updateCfiEx(subId, false);
            log("toTime > fromTime && (nowTime < fromTime && nowTime < toTime)");
        }else if (toTime < fromTime && 
                (nowTime > fromTime && nowTime > toTime)) {
            triggerTime = toTime + 24*60*60*1000;
            mCfiAction.updateCfiEx(subId, true);
            log("toTime < fromTime && (nowTime > fromTime && nowTime > toTime)");
        } else if (toTime < fromTime && 
                (nowTime < fromTime && nowTime < toTime)) {
            triggerTime = toTime;
            mCfiAction.updateCfiEx(subId, true);
            log("toTime < fromTime && (nowTime < fromTime && nowTime < toTime)");
        } else if (toTime < fromTime && 
                (nowTime < fromTime && nowTime > toTime)) {
            triggerTime = fromTime;
            mCfiAction.updateCfiEx(subId, false);
            log("toTime < fromTime && (nowTime < fromTime && nowTime > toTime)");
        }
        return triggerTime;
    }

    /**
     * Log the message
     * @param msg the message will be printed
     */
    private void log(String msg) {
        Log.d(LOG_TAG, msg);
    }
}