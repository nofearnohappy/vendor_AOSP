package com.mediatek.keyguard.plugin;

import android.content.Context;
import android.util.Log;

import com.android.internal.telephony.PhoneConstants;

import com.mediatek.common.PluginImpl;
import com.mediatek.keyguard.ext.DefaultOperatorSIMString;
import com.mediatek.keyguard.ext.IOperatorSIMString.SIMChangedTag;

import java.util.Locale;

/**
 * Customize the SIM String for OP09.
 */
@PluginImpl(interfaceName = "com.mediatek.keyguard.ext.IOperatorSIMString")
public class OP09OperatorSIMStringImp extends DefaultOperatorSIMString {
    private static final String TAG = "OP09OperatorSIMStringImp";

    private static final String SIM = "SIM";
    private static final String UIMSIM = "UIM/SIM";
    private static final String UIM = "UIM";

    @Override
    public String getOperatorSIMString(String sourceStr, int slotId,
            SIMChangedTag simChangedTag, Context context) {
        String retStr = sourceStr;
        Log.d(TAG, "getOperatorSIMString, slotId = " + slotId
                + " simChangedTag = " + simChangedTag + " sourceStr = "
                + sourceStr);
        if (isNeedProcessByLanguage() && isOP09Card(slotId, context)) {
            if ((simChangedTag == SIMChangedTag.SIMTOUIM)) {
                retStr = retStr.replace(SIM, UIM);
            } else if (simChangedTag == SIMChangedTag.UIMSIM) {
                retStr = retStr.replace(SIM, UIMSIM);
            } else if (simChangedTag == SIMChangedTag.DELSIM) {
                retStr = delSim(retStr);
            }
        }

        Log.d(TAG, "getOperatorSIMString, processed string retStr = " + retStr);
        return retStr;
    }

    /**
     * Whether the card in the slotId is main card.
     * @param slotId the slot id.
     * @param context the context.
     * @return Whether it is Op09 sim card.
     */
    private boolean isOP09Card(int slotId, Context context) {
        boolean bOp09 = (slotId == PhoneConstants.SIM_ID_1);
        Log.d(TAG, "isOP09Card, slot=" + slotId + " bOp09=" + bOp09);
        return bOp09;
    }

    /**
     * Delete the substring Sim from the sourceStr, according to the
     * local(English and simplified Chinese and traditional Chinese).
     */
    private String delSim(String sourceStr) {
        String retStr = sourceStr;
        Locale locale = Locale.getDefault();
        String language = locale.getLanguage();
        Log.d(TAG, " delSim, Country = " + locale.getCountry()
                + " language=" + language + " locale=" + locale);
        if (isReplacedWithSpace(language)) {
            retStr = sourceStr.replaceAll(" *" + SIM + " *", " ");
            retStr = toUppercaseFirstLetter(retStr);
        } else if (isReplaceToEmpty(language)) {
            retStr = retStr.replaceAll(" *" + SIM + " *", "");
        }
        retStr = retStr.trim();
        return retStr;
    }

    /**
     * Whether replace the string with space.
     * @param countryStr the country code
     * @return true replace to space.
     */
    private boolean isReplacedWithSpace(String language) {
        return language.equalsIgnoreCase(Locale.ENGLISH.getLanguage());
    }

    /**
     * Whether replace the string to empty.
     * @param countryStr the country code
     * @return true replace to empty.
     */
    private boolean isReplaceToEmpty(String language) {
        return language.equalsIgnoreCase(Locale.CHINESE.getLanguage());
    }

    /**
     * Replace the first letter of the word to Upper case.
     * @param sourceStr the source string.
     * @return transfer to uppercase first letter.
     */
    private String toUppercaseFirstLetter(String sourceStr) {
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
    private boolean isNeedProcessByLanguage() {
        Locale locale = Locale.getDefault();
        String language = locale.getLanguage();
        Log.d(TAG, " isNeedProcessByLanguage, Country = " + locale.getCountry()
                + " language=" + language + " locale=" + locale);
        return language.equalsIgnoreCase(Locale.CHINESE.getLanguage())
                || language.equalsIgnoreCase(Locale.ENGLISH.getLanguage());
    }
}
