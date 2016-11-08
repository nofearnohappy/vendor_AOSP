package com.mediatek.voiceextension.swip;

/**
 * The SwipAdapter callback interface.
 *
 */
public interface ISwipCallback {

    /**
     * Swip callback method.
     *
     * @param swipSetName
     *            swip set name
     * @param apiType
     *            feature api type
     * @param result
     *            swip result
     * @param extraMsg
     *            recognition command id
     * @param extraObj
     *            recognition command string list
     */
    public void onSwipMessageNotify(String swipSetName, int apiType,
            int result, int extraMsg, Object extraObj);

}
