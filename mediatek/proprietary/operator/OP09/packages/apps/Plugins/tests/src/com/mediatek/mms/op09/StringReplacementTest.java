package com.mediatek.mms.op09;

import com.mediatek.common.MPlugin;
import com.mediatek.mms.ext.IStringReplacementExt;
import com.mediatek.mms.op09.Op09MmsPluginTestRunner.BasicCase; 

public class StringReplacementTest extends BasicCase {
    private IStringReplacementExt mStringReplacement;
    private static final String[] mExpectedArray = new String[] {
            "Phone", "UIM Card"};
    private static final String[] mExpectedStrings = new String[] {
            "Save message to UIM/SIM card",
            "Select UIM/SIM",
            "Manage UIM/SIM card messages",
            "Manage messages stored on your UIM/SIM card",
            "Text messages on UIM card",
            "No messages on the UIM card.",
            "Get UIM capacity failed.",
            "This message on the UIM will be deleted.",
            "UIM card full",
            "It only allows to be read due to limitation of current mobile network type.",
            "SMS camps on current network will be deleted.",
            "Current capacity changes with the network which UIM camps on."
            };

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mStringReplacement = MPlugin.createInstance("com.mediatek.mms.ext.IStringReplacement" , mContext);
    }

    /// M: This case need to set the language in English.
    public void test001GetStringAPI() {
        String[] actualArray = mStringReplacement.getSaveLocationString();
        assertEquals(mExpectedArray[0], actualArray[0]);
        assertEquals(mExpectedArray[1], actualArray[1]);

        assertEquals(mExpectedStrings[IStringReplacementExt.SAVE_MSG_TO_CARD - 1],
                mStringReplacement.getStrings(IStringReplacementExt.SAVE_MSG_TO_CARD));
        assertEquals(mExpectedStrings[IStringReplacementExt.SELECT_CARD - 1],
                mStringReplacement.getStrings(IStringReplacementExt.SELECT_CARD));
        assertEquals(mExpectedStrings[IStringReplacementExt.MANAGE_CARD_MSG_TITLE - 1],
                mStringReplacement.getStrings(IStringReplacementExt.MANAGE_CARD_MSG_TITLE));
        assertEquals(mExpectedStrings[IStringReplacementExt.MANAGE_CARD_MSG_SUMMARY - 1],
                mStringReplacement.getStrings(IStringReplacementExt.MANAGE_CARD_MSG_SUMMARY));
        assertEquals(mExpectedStrings[IStringReplacementExt.MANAGE_UIM_MESSAGE - 1],
                mStringReplacement.getStrings(IStringReplacementExt.MANAGE_UIM_MESSAGE));
        assertEquals(mExpectedStrings[IStringReplacementExt.UIM_EMPTY - 1],
                mStringReplacement.getStrings(IStringReplacementExt.UIM_EMPTY));
        assertEquals(mExpectedStrings[IStringReplacementExt.GET_CAPACITY_FAILED - 1],
                mStringReplacement.getStrings(IStringReplacementExt.GET_CAPACITY_FAILED));
        assertEquals(mExpectedStrings[IStringReplacementExt.CONFIRM_DELETE_MSG - 1],
                mStringReplacement.getStrings(IStringReplacementExt.CONFIRM_DELETE_MSG));
        assertEquals(mExpectedStrings[IStringReplacementExt.UIM_FULL_TITLE - 1],
                mStringReplacement.getStrings(IStringReplacementExt.UIM_FULL_TITLE));
        assertEquals(mExpectedStrings[IStringReplacementExt.MESSAGE_CANNOT_BE_OPERATED - 1],
                mStringReplacement.getStrings(IStringReplacementExt.MESSAGE_CANNOT_BE_OPERATED));
        assertEquals(mExpectedStrings[IStringReplacementExt.CONFIRM_DELETE_SELECTED_MESSAGES - 1],
                mStringReplacement.getStrings(IStringReplacementExt.CONFIRM_DELETE_SELECTED_MESSAGES));
    }

}
