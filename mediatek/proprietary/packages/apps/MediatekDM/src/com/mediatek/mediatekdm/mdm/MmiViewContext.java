package com.mediatek.mediatekdm.mdm;

/**
 * MMI View context. A structure holding data relevant to most MMI views.
 */
public class MmiViewContext {
    /** Text to be displayed on screen. */
    public final String displayText;
    /** Minimum display time for a screen in seconds. */
    public final int minDT;
    /** Minimum display time for a screen in seconds. */
    public final int maxDT;

    public MmiViewContext(String text, int min, int max) {
        displayText = text;
        minDT = min;
        maxDT = max;
    }
}
