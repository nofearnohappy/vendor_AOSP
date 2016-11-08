package com.mediatek.rcs.pam.model;

import android.text.TextUtils;

import com.mediatek.rcs.pam.Constants;
import com.mediatek.rcs.pam.PAMException;
import com.mediatek.rcs.pam.util.Utils;

public class ComplainInfo implements SanityCheck {
    public String uuid;
    public int result;

    @Override
    public void checkSanity() throws PAMException {
        Utils.throwIf(ResultCode.PARAM_ERROR_MANDATORY_MISSING, TextUtils.isEmpty(uuid));
        Utils.throwIf(ResultCode.PARAM_ERROR_INVALID_FORMAT,
                (result != Constants.COMPLAIN_RESULT_SUCCESS
                && result != Constants.COMPLAIN_RESULT_FAILED));

    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{_class:\"ComplainInfo\", uuid:\"").append(uuid)
            .append("\", result:\"").append(result).append("\"}");
        return sb.toString();

    }
}
