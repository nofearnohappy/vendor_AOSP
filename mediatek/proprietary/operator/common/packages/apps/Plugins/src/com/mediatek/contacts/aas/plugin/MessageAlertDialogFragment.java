package com.mediatek.contacts.aas.plugin;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

public class MessageAlertDialogFragment extends DialogFragment implements
        DialogInterface.OnClickListener {

    public static final String TITLE = "title";
    public static final String MESSAGE = "message";
    public static final String NEGATIVE_TITLE = "negativeTitle";
    public static final String POSITIVE_TITLE = "positiveTitle";
    public static final String IS_OWN_CANCEL = "is_own_cancle";
    private DoneListener mDoneListener;
    public static final int INVIND_RES_ID = -1;
    private static String sText = "";

    public static MessageAlertDialogFragment newInstance(int title,
            int message, boolean isOwnCancel, String text) {
        MessageAlertDialogFragment frag = new MessageAlertDialogFragment();
        Bundle args = new Bundle();
        args.putInt(TITLE, title);
        args.putInt(MESSAGE, message);
        args.putBoolean(IS_OWN_CANCEL, isOwnCancel);
        frag.setArguments(args);
        sText = text;
        return frag;
    }

    public interface DoneListener {
        void onClick(String text);
    }

    public void setDeleteDoneListener(DoneListener listener) {
        mDoneListener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        int title = getArguments().getInt(TITLE);
        int message = getArguments().getInt(MESSAGE);
        boolean isOwnCancel = getArguments().getBoolean(IS_OWN_CANCEL);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setIconAttribute(android.R.attr.alertDialogIcon)
                .setTitle(title).setMessage(message)
                .setPositiveButton(android.R.string.ok, this);
        if (isOwnCancel) {
            builder.setNegativeButton(android.R.string.cancel,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                                int whichButton) {
                        }
                    });
        }
        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (mDoneListener != null) {
            mDoneListener.onClick(sText);
        }
    }

}
