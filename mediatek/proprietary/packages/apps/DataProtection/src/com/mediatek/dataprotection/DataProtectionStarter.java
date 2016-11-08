package com.mediatek.dataprotection;

import java.util.List;

import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternView;
import com.android.internal.widget.LockPatternView.Cell;
import com.mediatek.dataprotection.AlertDialogFragment.AlertDialogFragmentBuilder;
import com.mediatek.dataprotection.AlertDialogFragment.PatternInputDialogFragment;
import com.mediatek.dataprotection.AlertDialogFragment.PatternInputDialogFragment.PasswordInputListener;
import com.mediatek.dataprotection.AlertDialogFragment.PatternInputDialogFragmentBuilder;
import com.mediatek.dataprotection.ConfirmLockPatternFragment.Stage;
import com.mediatek.dataprotection.utils.DataProtectionLockPatternUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class DataProtectionStarter extends Activity {

    private static final String KEY_PATTERN_REQUEST_DIALOG = "request_pattern";

    private static final int WRONG_PATTERN_CLEAR_TIMEOUT_MS = 2000;

    private static final String TAG = "DataProtectionStarter";

    private static final String KEY_CANCEL_DIALOG = null;

    private LockPatternView mLockPatternView;
    private LockPatternUtils mLockPatternUtils;
    private DataProtectionLockPatternUtils mLocalPatternUtils;

    private static final int FAILED_ATTEMPTS_BEFORE_TIMEOUT = 5;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mLockPatternUtils = new LockPatternUtils(this);
        mLocalPatternUtils = new DataProtectionLockPatternUtils(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        DataProtectionApplication.setPriviledge(this, false);
        showPatternVerifyDialog();
    }

    @Override
    public void onPause() {
        super.onPause();
        FragmentManager fragmentManager = getFragmentManager();
        PatternConfirmFragment requestDialog = (PatternConfirmFragment) fragmentManager
                .findFragmentByTag(KEY_PATTERN_REQUEST_DIALOG);
        if (requestDialog != null) {
            requestDialog.cancelCountdown();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
        return;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        showPatternVerifyDialog();
    }

    class PatternConfirmFragment extends AlertDialogFragment {

        private Bundle mBundle = new Bundle();

        public PatternConfirmFragment() {
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            this.setOnDoneListener(this);
            this.setOnDialogDismissListener(new OnDialogDismissListener() {

                @Override
                public void onDialogDismiss() {
                    Log.d(TAG, "onDialogDismiss finish activity");
                    finish();
                }
            });
            AlertDialog.Builder builder = createAlertDialogBuilder(savedInstanceState);
            Bundle args = null;
            if (savedInstanceState == null) {
                args = getArguments();
            } else {
                args = savedInstanceState;
            }
            if (args != null) {
                // String fileName = args.getString(KEY_UNLOCK_FILE, "");
                String titleText = getString(R.string.pattern_input_title);

                // builder.setTitle(titleText);
                /* builder.setTitle(titleText + " <" + fileName + ">"); */
                View view = getActivity().getLayoutInflater().inflate(
                        R.layout.pattern_verify_dialog, null);
                // mHeaderText = (TextView) view.findViewById(R.id.headerText);
                mLockPatternView = (LockPatternView) view
                        .findViewById(R.id.lockPattern);
                //mFooterTextView = (TextView) view.findViewById(R.id.footerText);
                mLockPatternView.setOnPatternListener(mPatternListener);
                mHeaderTextView = (TextView) view.findViewById(R.id.headerText);
                mHeaderTextView
                        .setText(getString(R.string.unlock_pattern_header));
                builder.setCancelable(true);
                builder.setNegativeButton(R.string.btn_cancel,
                        new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                                if (mCountdownTimer != null) {
                                    mCountdownTimer.cancel();
                                    mCountdownTimer = null;
                                }
                            }
                        });
                // mBtnCancel = (Button)
                // view.findViewById(R.id.footerLeftButton);
                builder.setView(view);
            }
            // View view = getView();
            /*
             * if (null != view) { mHeaderText = (TextView)
             * view.findViewById(R.id.headerText); mPatternView =
             * (LockPatternView) view.findViewById(R.id.lockPattern);
             * mPatternView.setOnPatternListener(mPatternListener); }
             */
            return builder.create();
        }

        private LockPatternView.OnPatternListener mPatternListener = new LockPatternView.OnPatternListener() {

            public void onPatternStart() {
                mLockPatternView.removeCallbacks(mClearPatternRunnable);
            }

            public void onPatternCleared() {
                mLockPatternView.removeCallbacks(mClearPatternRunnable);
            }

            public void onPatternCellAdded(List<Cell> pattern) {

            }

            public void onPatternDetected(List<LockPatternView.Cell> pattern) {
                if (mLocalPatternUtils.checkPattern(mLockPatternUtils
                        .patternToString(pattern))) {
                    FragmentTransaction transaction = getFragmentManager()
                            .beginTransaction();
                    transaction.remove(PatternConfirmFragment.this);
                    transaction.commitAllowingStateLoss();

                    Intent intent = new Intent(getActivity(),
                            ViewLockedFilesActivity.class);
                    Bundle bundle = getArguments();
                    String action = bundle != null ? bundle.getString("ACTION", null) : null;
                    if (action != null && action.equals("cancel")) {
                        DataProtectionApplication.setNeedShowCancel(getActivity(), true);
                    } else if (action != null && action.equals("view_decrypt_fail_files")) {
                        DataProtectionApplication.setNeedShowDecryptFail(getActivity(), true);
                    }
                    bundle.putBoolean(DataProtectionService.KEY_INVOKE, true);
                    DataProtectionApplication.setPriviledge(getActivity(), true);
                    intent.putExtras(bundle);
                    startActivity(intent);
                    getActivity().finish();

                    /*
                     * Intent intent = getActivity().getIntent(); if
                     * (intent.getIntExtra(REQUEST_TYPE, 0) ==
                     * ACTION_CHANGE_PATTERN) { startFragment(new
                     * ChooseLockPatternFragment(), false); } else if
                     * (intent.getIntExtra(REQUEST_TYPE, 0) ==
                     * ACTION_VERIFY_PATTERN) { intent.putExtra(KEY_INVOKE,
                     * true); getActivity().setResult(Activity.RESULT_OK,
                     * intent); getActivity().finish(); }
                     */
                } else {
                    if (pattern.size() >= LockPatternUtils.MIN_PATTERN_REGISTER_FAIL
                            && ++mNumWrongConfirmAttempts >= FAILED_ATTEMPTS_BEFORE_TIMEOUT) {
                        long deadline = DataProtectionApplication
                                .setLockoutAttemptDeadline(getActivity());
                        handleAttemptLockout(deadline);
                    } else {
                        updateStage(Stage.NeedToUnlockWrong);
                        postClearPatternRunnable();
                    }
                }
            }
        };
        private CountDownTimer mCountdownTimer;
        private TextView mHeaderTextView;
        //private TextView mFooterTextView;
        private int mNumWrongConfirmAttempts;
        private CharSequence mHeaderWrongText = null;
 /*       private CharSequence mFooterWrongText = null;*/
        private CharSequence mHeaderText;

        protected void handleAttemptLockout(long elapsedRealtimeDeadline) {
            updateStage(Stage.LockedOut);
            long elapsedRealtime = SystemClock.elapsedRealtime();
            mCountdownTimer = new CountDownTimer(elapsedRealtimeDeadline
                    - elapsedRealtime,
                    LockPatternUtils.FAILED_ATTEMPT_COUNTDOWN_INTERVAL_MS) {

                @Override
                public void onTick(long millisUntilFinished) {
                    Activity activity = getActivity();
                    if (activity != null && activity.isResumed()) {
                        mHeaderTextView
                                .setText(getString(R.string.lockpattern_too_many_failed_confirmation_attempts_header));
                    }
                }

                @Override
                public void onFinish() {
                    Activity activity = getActivity();
                    if (activity != null && activity.isResumed()) {
                        mNumWrongConfirmAttempts = 0;
                        updateStage(Stage.NeedToUnlock);
                    }
                }
            } .start();
        }

        protected void cancelCountdown() {
            if (mCountdownTimer != null) {
                mCountdownTimer.cancel();
                mCountdownTimer = null;
            }
        }

        private void updateStage(int stage) {
            switch (stage) {
            case Stage.NeedToUnlock:
                mHeaderTextView
                        .setText(getString(R.string.unlock_pattern_header));

/*                  if (mFooterText != null) {
                 mFooterTextView.setText(mFooterText); } else {
                  mFooterTextView
                  .setText(R.string.lockpattern_need_to_unlock_footer); }*/

                mLockPatternView.setEnabled(true);
                mLockPatternView.enableInput();
                break;
            case Stage.NeedToUnlockWrong:
                if (mHeaderWrongText != null) {
                    mHeaderTextView.setText(mHeaderWrongText);
                } else {
                    mHeaderTextView
                            .setText(R.string.lockpattern_need_to_unlock_wrong);
                }
/*                if (mFooterWrongText != null) {
                    mFooterTextView.setText(mFooterWrongText);
                } else {
                    mFooterTextView
                            .setText(R.string.lockpattern_need_to_unlock_wrong_footer);
                }*/

                mLockPatternView
                        .setDisplayMode(LockPatternView.DisplayMode.Wrong);
                mLockPatternView.setEnabled(true);
                mLockPatternView.enableInput();
                break;
            case Stage.LockedOut:
                mLockPatternView.clearPattern();
                // enabled = false means: disable input, and have the
                // appearance of being disabled.
                mLockPatternView.setEnabled(false); // appearance of being
                                                    // disabled
                break;
            }

            // Always announce the header for accessibility. This is a no-op
            // when accessibility is disabled.
            mHeaderTextView.announceForAccessibility(mHeaderTextView.getText());
        }

        private Runnable mClearPatternRunnable = new Runnable() {
            public void run() {
                mLockPatternView.clearPattern();
            }
        };

        // clear the wrong pattern unless they have started a new one
        // already
        private void postClearPatternRunnable() {
            mLockPatternView.removeCallbacks(mClearPatternRunnable);
            mLockPatternView.postDelayed(mClearPatternRunnable,
                    WRONG_PATTERN_CLEAR_TIMEOUT_MS);
        }
    }

    private void showCancelDialog(final long taskId) {
        // Log.d(TAG, "showCancelDialog " + mNeedShowCancel + " " +
        // mCancelTitle);
        AlertDialogFragment listDialogFragment = (AlertDialogFragment) getFragmentManager()
                .findFragmentByTag(KEY_CANCEL_DIALOG);
        final DialogInterface.OnClickListener cListener = new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                /*
                 * if (mService != null) { mService.cancelTask(taskId); }
                 */
                dialog.dismiss();
            }
        };
        if (DataProtectionStarter.this.isResumed()) {
            if (listDialogFragment == null) {
                AlertDialogFragmentBuilder builder = new AlertDialogFragmentBuilder();
                listDialogFragment = builder.setDoneTitle(R.string.btn_ok)
                        .setCancelTitle(R.string.btn_cancel).create();
                listDialogFragment.setOnDoneListener(cListener);
                // listDialogFragment.setOnDialogDismissListener(clickListener);

                listDialogFragment
                        .show(getFragmentManager(), KEY_CANCEL_DIALOG);
                boolean ret = getFragmentManager().executePendingTransactions();

            }
        }
    }

    private void showPatternVerifyDialog() {
        Bundle data = getIntent() != null ? getIntent().getExtras() : null;
        FragmentManager fragmentManager = getFragmentManager();
        PatternConfirmFragment requestDialog = (PatternConfirmFragment) fragmentManager
                .findFragmentByTag(KEY_PATTERN_REQUEST_DIALOG);
        if (data != null && requestDialog == null) {
            requestDialog = new PatternConfirmFragment();
            requestDialog.setArguments(data);
            requestDialog
                    .show(getFragmentManager(), KEY_PATTERN_REQUEST_DIALOG);
            boolean ret = getFragmentManager().executePendingTransactions();
        } else {
            Log.e(TAG, "showPatternVerifyDialog data is " + data);
        }
        long deadline = DataProtectionApplication
                .getLockoutAttemptDeadline(this);
        if (deadline != 0 && requestDialog != null) {
            requestDialog.handleAttemptLockout(deadline);
        } else {
            Log.d(TAG, "showPatternVerifyDialog " + deadline + " "
                    + requestDialog);
        }
    }

    private void showPatternRequestDialog(/* final long taskId */String failName) {
        PatternInputDialogFragment listDialogFragment = (PatternInputDialogFragment) getFragmentManager()
                .findFragmentByTag(KEY_PATTERN_REQUEST_DIALOG);
        if (DataProtectionStarter.this.isResumed()) {
            if (listDialogFragment == null) {
                PatternInputDialogFragmentBuilder builder = new PatternInputDialogFragmentBuilder();
                builder.setCancelable(true);
                builder.setCancelTitle(R.string.btn_cancel);
                // builder.set
                // builder.setUnlockFileName(mDecryptFailFileName);
                // builder.setUnlockFileName(failName);
                listDialogFragment = builder.create();
                listDialogFragment
                        .setOnPatternInput(new PasswordInputListener() {

                            @Override
                            public void onPatternDetect(String password) {
                                /*
                                 * if (password != null && mService != null) {
                                 * Log.d(TAG,
                                 * "onPatternDetect user has input key");
                                 * tryToDecrypt(password); if (mActionMode !=
                                 * null) { mActionMode.finish(); }
                                 * mNeedRestorePatternInput = false; }
                                 */
                            }

                        });

                listDialogFragment.show(getFragmentManager(),
                        KEY_PATTERN_REQUEST_DIALOG);
                // listDialogFragment.startCounter();
                boolean ret = getFragmentManager().executePendingTransactions();
            }
        }
    }
}
