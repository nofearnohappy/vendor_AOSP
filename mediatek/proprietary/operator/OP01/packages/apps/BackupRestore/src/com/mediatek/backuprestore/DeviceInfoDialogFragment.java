package com.mediatek.backuprestore;

import com.mediatek.backuprestore.utils.Constants;
import com.mediatek.backuprestore.utils.MyLogger;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

public class DeviceInfoDialogFragment extends DialogFragment {
    private static final String CLASS_TAG = MyLogger.LOG_TAG + "/DeviceInfoDialogFragment";
    boolean mChecked;

    static DeviceInfoDialogFragment newInstance(int message, String key, int arrayRes) {
        DeviceInfoDialogFragment fragment = new DeviceInfoDialogFragment();
        Bundle args = new Bundle();
        args.putInt("message", message);
        args.putString("flag", key);
        args.putInt("array", arrayRes);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final int message = getArguments().getInt("message");
        final String key = getArguments().getString("flag");
        final int arrayRes = getArguments().getInt("array");
        LayoutInflater factory = LayoutInflater.from(getActivity());
        final View view = factory.inflate(R.layout.dialog_change_notice, null);
        TextView text = (TextView) view.findViewById(R.id.change_info);
        text.setText(message);
        CheckBox checkbox = (CheckBox) view.findViewById(R.id.checkbox);
        checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mChecked = isChecked;
            }
        });
        Dialog dialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.change_phone_notice)
                .setView(view)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Log.e(CLASS_TAG, "DLG_CHANGE_NOTICE: mChecked = " + mChecked + " key == "
                                + key);
                        StorageSettingsActivity.setNoticeStatus(getActivity(), mChecked, key);
                    }
                })
                .setNeutralButton(R.string.change_phone_summary,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                String[] mData = getActivity().getResources().getStringArray(
                                        arrayRes);
                                Intent intent = new Intent(getActivity(), DeviceChangedInfo.class);
                                intent.putExtra(Constants.ARRAYDATA, mData);
                                intent.putExtra(Constants.KEY_SAVED_DATA, key);
                                startActivity(intent);
                                dialog.dismiss();
                            }
                        }).create();

        return dialog;
    }
}
