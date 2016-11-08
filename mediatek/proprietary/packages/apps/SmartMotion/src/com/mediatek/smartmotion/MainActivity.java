package com.mediatek.smartmotion;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceActivity;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.Switch;
import android.widget.TextView;

import com.mediatek.smartmotion.enabler.EasyRejectEnabler;
import com.mediatek.smartmotion.enabler.IncomingCallListener;
import com.mediatek.smartmotion.enabler.Preferences;
import com.mediatek.smartmotion.enabler.QuickAnswerEnabler;
import com.mediatek.smartmotion.enabler.SmartMotionEnabler;
import com.mediatek.smartmotion.enabler.SmartSilentEnabler;
import com.mediatek.smartmotion.enabler.InPocketEnabler;
import com.mediatek.smartmotion.enabler.PedometerEnabler;
import com.mediatek.smartmotion.enabler.UserActivityEnabler;
import com.mediatek.smartmotion.enabler.SignificantMotionEnabler;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends PreferenceActivity {
    private final static String TAG = "MainActivity";
    private final static String BUNDLE_HEADERS = "bundle_headers";
    private List<Header> mHeaders;
    public static boolean sDemoMode;
    private boolean mAttachFragment;

    private static final String[] ENTRY_FRAGMENTS = {
        QuickAnswerFragment.class.getName(),
        EasyRejectFragment.class.getName(),
        SmartSilentFragment.class.getName()
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            Parcelable[] parcelables = savedInstanceState.getParcelableArray(BUNDLE_HEADERS);
            if (parcelables != null) {
                mHeaders = new ArrayList<PreferenceActivity.Header>();
                for (int i = 0; i < parcelables.length; i++) {
                    mHeaders.add((Header)parcelables[i]);
                }
                Log.d(TAG, "size=" + mHeaders.size());
            }
        }

        TelephonyManager tpm = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        tpm.listen(IncomingCallListener.getInstance(), PhoneStateListener.LISTEN_CALL_STATE);

        sDemoMode = Preferences.getPreferences(this).getDemoMode();
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mHeaders != null) {
            Header[] array = mHeaders.toArray(new Header[]{});
            Log.d(TAG, "before save size:" + array.length);
            outState.putParcelableArray(BUNDLE_HEADERS, (Parcelable[])array);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem menuItem = menu.findItem(R.id.action_settings);
        menuItem.setVisible(mAttachFragment ? false : true);
        if (sDemoMode) {
            menuItem.setTitle(getResources().getString(
                    R.string.change_to_powerconsumption_mode));
        } else {
            menuItem.setTitle(getResources().getString(
                    R.string.change_to_demo_mode));
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main, menu);
        return true;
    }

    public void attachFragment(boolean attached) {
        mAttachFragment = attached;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.action_settings:
            sDemoMode = !sDemoMode;
            SmartMotionEnabler.resetAllEnabler();
            invalidateHeaders();
            Preferences.getPreferences(this).setDemoMode(sDemoMode);
            return true;
        case R.id.exit_application:
            SmartMotionEnabler.disableAllSensors();
            System.exit(0);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.smartmotion_headers, target);
        if (sDemoMode) {
            int count = HeaderAdapter.POSITION_SIGNIFICANT_MOTION
                    - HeaderAdapter.POSITION_IN_POCKET + 1;
            for (int i = 0; i < count; i++) {
                target.remove(target.size() - 1);
            }
        }
        Log.d(TAG, "onBuildHeaders");
        mHeaders= target;
    }

    @Override
    public void setListAdapter(ListAdapter adapter) {
        super.setListAdapter(new HeaderAdapter(this, mHeaders));
    }

    private static class HeaderAdapter extends ArrayAdapter<Header> {
        private LayoutInflater mInflater;
        private Context mContext;

        private static final int POSITION_QUICK_ANSWER = 0;
        private static final int POSITION_EASY_REJECT = 1;
        private static final int POSITION_SMART_SILENT = 2;
        private static final int POSITION_IN_POCKET = 3;
        private static final int POSITION_PEDOMETER = 4;
        private static final int POSITION_USER_ACTIVITY = 5;
        private static final int POSITION_SIGNIFICANT_MOTION = 6;

        private static class HeaderViewHolder {
            TextView title;
            TextView summary;
            Switch switch_;
        }

        public HeaderAdapter(Context context, List<Header> objects) {
            super(context, 0, objects);
            mContext = context;
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            QuickAnswerEnabler.unregisterAllSwitches();
            EasyRejectEnabler.unregisterAllSwitches();
            InPocketEnabler.unregisterAllSwitches();
            SmartSilentEnabler.unregisterAllSwitches();
            PedometerEnabler.unregisterAllSwitches();
            UserActivityEnabler.unregisterAllSwitches();
            SignificantMotionEnabler.unregisterAllSwitches();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Log.d(TAG, "getview, pos=" + position);
            HeaderViewHolder holder = new HeaderViewHolder();
            Header header = getItem(position);
            View view = mInflater.inflate(R.layout.preference_header_switch_item, parent,
                    false);

            holder.title = (TextView)
                    view.findViewById(R.id.title);
            holder.summary = (TextView)
                    view.findViewById(R.id.summary);
            holder.switch_ = (Switch) view.findViewById(R.id.switchWidget);

            switch (position) {
                case POSITION_QUICK_ANSWER:
                    QuickAnswerEnabler.registerSwitch(mContext, holder.switch_);
                    break;
                case POSITION_EASY_REJECT:
                    EasyRejectEnabler.registerSwitch(mContext, holder.switch_);
                    break;
                case POSITION_SMART_SILENT:
                    SmartSilentEnabler.registerSwitch(mContext, holder.switch_);
                    break;
                case POSITION_IN_POCKET:
                    InPocketEnabler.registerSwitch(mContext, holder.switch_);
                    break;
                case POSITION_PEDOMETER:
                    PedometerEnabler.registerSwitch(mContext, holder.switch_);
                    break;
                case POSITION_USER_ACTIVITY:
                    UserActivityEnabler.registerSwitch(mContext, holder.switch_);
                    break;
                case POSITION_SIGNIFICANT_MOTION:
                    SignificantMotionEnabler.registerSwitch(mContext, holder.switch_);
                    break;
            }

            CharSequence title = header.getTitle(getContext().getResources());
            holder.title.setText(title);
            CharSequence summary = header.getSummary(getContext().getResources());
            holder.summary.setText(summary);

            return view;
        }
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        for (int i = 0; i < ENTRY_FRAGMENTS.length; i++) {
            if (ENTRY_FRAGMENTS[i].equals(fragmentName)) return true;
        }

        return false;
    }
}
