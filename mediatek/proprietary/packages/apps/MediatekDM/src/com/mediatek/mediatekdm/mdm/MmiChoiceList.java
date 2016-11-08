package com.mediatek.mediatekdm.mdm;

/**
 * Choice list MMI view
 */
public interface MmiChoiceList {
    /**
     * Prompt user to select one or more item from a list.
     *
     * @param context
     *        Context of the screen to be displayed.
     * @param items
     *        List items.
     * @param bitflags
     *        Each bit represents the a list item. If bit is set, item should be selected by
     *        default.
     * @param isMultipleSelection
     *        Whether more than one item may be selected.
     * @return whether the MMI has been displayed successfully.
     */
    MmiResult display(MmiViewContext context, String[] items, int bitflags,
            boolean isMultipleSelection);
}
