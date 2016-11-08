package com.android.internal.tedongle;

import android.os.Messenger;
import android.tedongle.SignalStrength;
import com.android.internal.tedongle.ITedongleStateListener;
import android.tedongle.ServiceState;

interface ITedongle {


    /**
     * Check to see if the radio is on or not.
     * @return returns true if the radio is on.
     */
    boolean isRadioOn();

    /**
     * Toggles the radio on or off.
     */
    void toggleRadioOnOff();

    /**
     * Set the radio to on or off
     */
    boolean setRadio(boolean turnOn);


    /**
     * Enable a specific APN type.
     *
    int enableApnType(String type);
    */

    /**
     * Disable a specific APN type.
     *
    int disableApnType(String type);
	*/

	int getDataState();

    int getDataActivity();

	int getActivePhoneType();

	int getNetworkType();

	boolean isDonglePluged();
	 /**
     * Get a reference to handler. This is used by a client to establish
     * an AsyncChannel communication with TedongleService
     */
	 Messenger getTedongleServiceMessenger();


	boolean isAirplaneMode();

	/*int getSimStat();*/
	void listen(ITedongleStateListener callback, int events);

	void NotifySignalStrength(in SignalStrength SS);

	ServiceState getServiceState() ;

	SignalStrength getSignalStrength() ;

	boolean isSimReady() ;

	    /**
     * Supply a pin to unlock the SIM.  Blocks until a result is determined.
     * @param pin The pin to check.
     * @return whether the operation was a success.
     */
    boolean supplyPin(String pin);

    /**
     * Supply puk to unlock the SIM and set SIM pin to new pin.
     *  Blocks until a result is determined.
     * @param puk The puk to check.
     *        pin The new pin to be set in SIM
     * @return whether the operation was a success.
     */
    boolean supplyPuk(String puk, String pin);

    /**
     * Handles PIN MMI commands (PIN/PIN2/PUK/PUK2), which are initiated
     * without SEND (so <code>dial</code> is not appropriate).
     *
     * @param dialString the MMI command to be executed.
     * @return true if MMI command is executed.

    boolean handlePinMmi(String dialString);
	*/
	boolean getIccLockEnabled() ;

	String getLine1Number() ;

    String getSubscriberId() ;
}
