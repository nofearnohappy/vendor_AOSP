package com.mediatek.mms.plugin;

import android.content.Context;
import android.provider.Telephony.Mms;

import com.android.mtkex.chips.MTKRecipientEditTextView;
import com.mediatek.mms.ext.DefaultOpRecipientsEditorExt;

public class Op09RecipientsEditorExt extends DefaultOpRecipientsEditorExt {

    private static String TAG = "Op09RecipientsEditorExt";

    private MTKRecipientEditTextView mRecipientEditTextView;

    public Op09RecipientsEditorExt(Context context) {
        super(context);
    }

    @Override
    public void init(MTKRecipientEditTextView recipientEditTextView, boolean isWVGAScreen) {
        mRecipientEditTextView = recipientEditTextView;
        if (isWVGAScreen) {
            /// M: Modify for op09; @{
            if (MessageUtils.isShowPreviewForRecipient()) {
                mRecipientEditTextView.setMaxLines(3);
            }
            /// @}
        }
    }

    @Override
    public boolean isValidAddress(boolean commonValidValue, String number) {
        /// M: For OP09; Judge the address only can include the following characters :
        ///space,number and the first character can use +;@{
        if (commonValidValue && MessageUtils.isMoreStrictValidateForSmsAddr()) {
            if (!(MessageUtils.isWellFormedSmsAddress(number.replaceAll(" |-", ""))
                    || Mms.isEmailAddress(number))) {
                return true;
            }
        }
        /// @}
        return false;
    }

}
