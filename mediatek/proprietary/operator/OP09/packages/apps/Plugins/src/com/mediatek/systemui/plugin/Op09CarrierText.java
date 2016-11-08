/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mediatek.systemui.plugin;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.internal.telephony.PhoneConstants;
import com.mediatek.op09.plugin.R;
import com.mediatek.systemui.statusbar.util.SIMHelper;

import java.util.Locale;

/**
 * Op09 CarrierText implements for SystemUI.
 */
public class Op09CarrierText {
    private static final String TAG = "Op09CarrierText";
    private static final boolean DEBUG = true;
    private static final int MAX_CARRIER_TEXT_NUM = 4;

    private static final String COUNTRY_REGION_CN = "CN";
    private static final String COUNTRY_REGION_TW = "TW";
    private static final String COUNTRY_REGION_HK = "HK";
    private static final String COUNTRY_REGION_UK = "UK";
    private static final String COUNTRY_REGION_US = "US";

    private static final String SIM = "SIM";
    private static final String UIMSIM = "UIM/SIM";
    private static final String UIM = "UIM";
    private static final String CT_PLMN = "china telecom";

    private Context mContext;
    protected int mNumOfSub;
    protected String mNetworkNameDefault;
    protected String mSimMissingDefault;

    protected LinearLayout mCarrierLayout;
    protected TextView mCarrierView[];
    protected TextView mCarrierDivider[];

    private int mCarrierLabelHeight;
    private int mNavigationBarSize;

    /**
     * Constructs a new Op09CarrierText instance with Context.
     * @param context A Context object.
     */
    public Op09CarrierText(Context context) {
        this.mContext = context;

        mNumOfSub = SIMHelper.getSlotCount();

        final Resources res = mContext.getResources();

        mNetworkNameDefault = res.getString(R.string.lockscreen_carrier_default);
        mSimMissingDefault = res.getString(R.string.lockscreen_missing_sim_message_short);

        mCarrierLabelHeight = res.getDimensionPixelSize(R.dimen.carrier_label_height);
        mNavigationBarSize = res.getDimensionPixelSize(R.dimen.navigation_bar_size);
        final FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, mCarrierLabelHeight, Gravity.BOTTOM);
        params.bottomMargin = mNavigationBarSize;

        mCarrierLayout = (LinearLayout) View.inflate(mContext, R.layout.sys_carrier_label, null);
        mCarrierLayout.setLayoutParams(params);

        // the carrier and carrier divider number is defined in layout: carrier_label.xml
        mCarrierView = new TextView[MAX_CARRIER_TEXT_NUM];
        mCarrierDivider = new TextView[MAX_CARRIER_TEXT_NUM - 1];

        mCarrierView[0] = (TextView) mCarrierLayout.findViewById(R.id.carrier_1);
        mCarrierView[1] = (TextView) mCarrierLayout.findViewById(R.id.carrier_2);
        mCarrierView[2] = (TextView) mCarrierLayout.findViewById(R.id.carrier_3);
        mCarrierView[3] = (TextView) mCarrierLayout.findViewById(R.id.carrier_4);
        mCarrierDivider[0] = (TextView) mCarrierLayout.findViewById(R.id.carrier_divider_1);
        mCarrierDivider[1] = (TextView) mCarrierLayout.findViewById(R.id.carrier_divider_2);
        mCarrierDivider[2] = (TextView) mCarrierLayout.findViewById(R.id.carrier_divider_3);

        for (int i = 0; i < mNumOfSub; i++) {
            mCarrierView[i].setText(mNetworkNameDefault);
            if (i < mNumOfSub - 1) {
                mCarrierDivider[i].setText("|");
            }
        }

        if (mNumOfSub == 2) {
            mCarrierView[0].setGravity(Gravity.END);
            mCarrierView[1].setGravity(Gravity.START);
        }

        if (DEBUG) {
            Log.d(TAG, "Op09CarrierText, mNumOfSub=" + mNumOfSub
                    + " , mNetworkNameDefault=" + mNetworkNameDefault
                    + " , mSimMissingDefault=" + mSimMissingDefault);
        }
    }

    protected void updateCarrierText(int slotId, boolean isSimInserted, boolean isHasSimService,
            boolean isLockedCard, String[] networkNames) {
        if (DEBUG) {
            Log.d(TAG, "updateCarrierText, slotId=" + slotId + ", networkNames="
                    + networkNames[slotId]);
        }

        mNetworkNameDefault = mContext.getString(R.string.lockscreen_carrier_default);
        mSimMissingDefault = mContext.getString(R.string.lockscreen_missing_sim_message_short);

        CharSequence text = networkNames[slotId];
        if (slotId == PhoneConstants.SIM_ID_1 && isLockedCard) {
            text = mNetworkNameDefault;
        } else if (!isSimInserted) {
            text = mSimMissingDefault;
        } else {
            text = getOperatorSIMString(text);
        }
        ///M: add to fix 1993574 @{
        CharSequence tempText = null;
        if (text != null) {
            if (text.toString().equalsIgnoreCase(CT_PLMN)) {
                tempText = text;
            } else {
                tempText = text.toString().toUpperCase();
            }
        }
        /// @}
        mCarrierView[slotId].setText(tempText != null ? tempText.toString() : null);

        showOrHideCarrier(networkNames);
    }

    private void showOrHideCarrier(CharSequence[] networkNames) {
        int mNumOfSIM = 0;
        TextView mCarrierLeft = null;
        TextView mCarrierRight = null;

        for (int i = 0; i < mNumOfSub - 1; i++) {
            if (mCarrierDivider[i] != null) {
                mCarrierDivider[i].setVisibility(View.GONE);
            }
        }

        for (int i = 0; i < mNumOfSub; i++) {
            if (mCarrierView[i] != null) {
                mCarrierView[i].setVisibility(View.VISIBLE);
            }
            mNumOfSIM++;
            if (mNumOfSIM == 1) {
                mCarrierLeft = mCarrierView[i];
            } else if (mNumOfSIM == 2) {
                mCarrierRight = mCarrierView[i];
            }
            if (mNumOfSIM >= 2 && ((i - 1) >= 0) && (mCarrierDivider[i - 1] != null)) {
                if (mCarrierView[i - 1] != null
                        && !TextUtils.isEmpty(mCarrierView[i - 1].getText())
                        && mCarrierView[i] != null
                        && !TextUtils.isEmpty(mCarrierView[i].getText())) {
                    mCarrierDivider[i - 1].setVisibility(View.VISIBLE);
                }
            }

            if (mCarrierView[i] != null) {
                mCarrierView[i].setGravity(Gravity.CENTER);
            }
        }

        if (mNumOfSIM == 2) {
            if (mCarrierLeft != null) {
                mCarrierLeft.setGravity(Gravity.END);
            }
            if (mCarrierRight != null) {
                mCarrierRight.setGravity(Gravity.START);
            }
        } else if (mNumOfSIM == 0) {
            final String defaultPlmn = mNetworkNameDefault;
            int index = 0;
            for (int i = 0; i < mNumOfSub; i++) {
                CharSequence plmn = getOperatorSIMString(networkNames[i]);
                if (plmn != null && defaultPlmn.contentEquals(plmn) == false) {
                    index = i;
                    break;
                }
            }
            if (mCarrierView[index] != null) {
                mCarrierView[index].setVisibility(View.VISIBLE);
            }
            Log.d(TAG, "updateOperatorInfo, No SIM cards, force slotId " + index + " to visible.");
        }
    }

    /**
     * Get the string with SIM or UIM according to Operator.
     *
     * @param sourceStr the source string.
     * @return the operator sim string.
     */
    private static final String getOperatorSIMString(CharSequence sourceStr) {
        String retStr = "";
        if (!TextUtils.isEmpty(sourceStr)) {
            retStr = sourceStr.toString();
            if (isNeedProcessByLanguage()) {
                retStr = delSim(retStr);
            }
        }

        return retStr;
    }

    /**
     * Delete the substring Sim from the sourceStr, according to the
     * local(English and simplified Chinese and traditional Chinese).
     */
    private static final String delSim(String sourceStr) {
        String retStr = sourceStr;
        final String countryStr = Locale.getDefault().getCountry();
        if (isReplacedWithSpace(countryStr)) {
            retStr = sourceStr.replaceAll(" *" + SIM + " *", " ");
            retStr = toUppercaseFirstLetter(retStr);
        } else if (isReplaceToEmpty(countryStr)) {
            retStr = retStr.replaceAll(" *" + SIM + " *", "");
        }
        retStr = retStr.trim();

        Log.d(TAG, "delSim, sourceStr=" + sourceStr + ", retStr=" + retStr);
        return retStr;
    }

    /**
     * Whether replace the string with space.
     * @param countryStr the country code
     * @return true replace to space.
     */
    private static final boolean isReplacedWithSpace(String countryStr) {
        return (COUNTRY_REGION_US.equals(countryStr)
                || COUNTRY_REGION_UK.equals(countryStr));
    }

    /**
     * Whether replace the string to empty.
     * @param countryStr the country code
     * @return true replace to empty.
     */
    private static final boolean isReplaceToEmpty(String countryStr) {
        return (COUNTRY_REGION_CN.equals(countryStr)
                || COUNTRY_REGION_TW.equals(countryStr)
                || COUNTRY_REGION_HK.equals(countryStr));
    }

    /**
     * Replace the first letter of the word to Upper case.
     * @param sourceStr the source string.
     * @return transfer to uppercase first letter.
     */
    private static final String toUppercaseFirstLetter(String sourceStr) {
        String retStr = sourceStr.trim();
        if (retStr.length() > 1) {
            retStr = retStr.substring(0, 1).toUpperCase() + retStr.substring(1);
        } else if (retStr.length() == 1) {
            retStr = retStr.substring(0, 1).toUpperCase();
        }
        return retStr;
    }

    /**
     * Whether the language need to process.
     * @return true need process.
     */
    private static final boolean isNeedProcessByLanguage() {
        final String countryStr = Locale.getDefault().getCountry();
        if (DEBUG) {
            Log.d(TAG, " isNeedProcessByLanguage, County = " + countryStr);
        }

        return (COUNTRY_REGION_CN.equals(countryStr)
                || COUNTRY_REGION_TW.equals(countryStr)
                || COUNTRY_REGION_HK.equals(countryStr)
                || COUNTRY_REGION_US.equals(countryStr)
                || COUNTRY_REGION_UK.equals(countryStr));
    }
}
