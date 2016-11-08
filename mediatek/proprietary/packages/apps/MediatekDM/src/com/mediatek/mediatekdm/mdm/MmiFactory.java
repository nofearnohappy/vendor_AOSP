package com.mediatek.mediatekdm.mdm;

/**
 * MMI View factory. MdmEngine will invoke methods in this interface to create MMI view if needed.
 */
public interface MmiFactory {
    /**
     * Create a {@link MmiInfoMsg} instance.
     *
     * @param observer
     *        observer to be notified upon MMI events.
     * @return
     */
    MmiInfoMsg createInfoMsgDlg(MmiObserver observer);

    /**
     * Create a {@link MmiConfirmation} instance.
     *
     * @param observer
     *        observer to be notified upon MMI events.
     * @return
     */
    MmiConfirmation createConfirmationDlg(MmiObserver observer);

    /**
     * Create a {@link MmiInputQuery} instance.
     *
     * @param observer
     *        observer to be notified upon MMI events.
     * @return
     */
    MmiInputQuery createInputQueryDlg(MmiObserver observer);

    /**
     * Create a {@link MmiChoiceList} instance.
     *
     * @param observer
     *        observer to be notified upon MMI events.
     * @return
     */
    MmiChoiceList createChoiceListDlg(MmiObserver observer);

    /**
     * Create a {@link MmiProgress} instance.
     *
     * @param observer
     *        observer to be notified upon MMI events.
     * @return
     */
    MmiProgress createProgress(int total);
}
