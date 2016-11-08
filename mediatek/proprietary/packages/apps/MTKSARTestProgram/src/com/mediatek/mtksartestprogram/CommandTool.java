package com.mediatek.mtksartestprogram;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.IPowerManager;
import android.os.Message;
import android.os.PowerManager;
import android.provider.Settings;
import android.telephony.ServiceState;
import android.util.Log;
import android.util.TypedValue;

import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.DriverCall;
import com.android.internal.telephony.MmiCode;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants.State;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.test.SimulatedCommands;
import com.android.internal.telephony.test.SimulatedRadioControl;
import com.android.internal.telephony.uicc.UiccController;

public class CommandTool {

    private SimulatedRadioControl mRadioControl;
    private Phone phone;
    private GSMTestHandler mGSMTestHandler;
    private Handler mHandler;

    private static final int EVENT_OEM_RIL_MESSAGE = 13;
    private static final int EVENT_RADIOPOWER_MESSAGE = 12;
    static final int ANY_MESSAGE = -1;

    private static final String resetCommand = "AT+ERFTX=4";
    private static final String reductionByRAT = "AT+ERFTX=1,";
    private static final String reductionByRATByBand2g = "AT+ERFTX=5,1,";
    private static final String reductionByRATByBand3g = "AT+ERFTX=5,2,";
    private static final String reductionByRATByBand4g = "AT+ERFTX=3,3,";
    private static final String checkHave4g = "AT+CEREG";

    public static String[] modem = { "2g", "3g", "4g" };
    public static final String[] band_2g = { "FrequencyBand850", "FrequencyBand900",
            "FrequencyBand1800", "FrequencyBand1900" };
    public static final String[] band_3g = { "WCDMA_IMT_2000", "WCDMA_PCS_1900", "WCDMA_DCS_1800",
            "WCDMA_AWS_1700", "WCDMA_CLR_850", "WCDMA_800", "WCDMA_IMT_E_2600", "WCDMA_GSM_900",
            "WCDMA_1800", "WCDMA_1700" };
    public static final String[] band_4g = new String[64];
    public static String[][] band = { band_2g, band_3g, band_4g };

    static {
        for (int i = 0; i < 64; i++) {
            band_4g[i] = "" + (i + 1);
        }
    }

    public CommandTool() {

        mGSMTestHandler = new GSMTestHandler();

        mGSMTestHandler.start();
        synchronized (mGSMTestHandler) {
            do {
                try {
                    mGSMTestHandler.wait();
                } catch (Exception e) {

                }
            } while (mGSMTestHandler.getGSMPhone() == null);
        }

        phone = mGSMTestHandler.getGSMPhone();
        mRadioControl = mGSMTestHandler.getSimulatedCommands();

        mHandler = mGSMTestHandler.getHandler();

        if (mRadioControl instanceof SimulatedCommands) {
            CommandsInterface ci = (CommandsInterface) mRadioControl;
            ci.getBasebandVersion(null);

            try {
                ci.setRadioPower(true, mHandler.obtainMessage(EVENT_RADIOPOWER_MESSAGE));
                Message msg = mGSMTestHandler.waitForMessage(EVENT_RADIOPOWER_MESSAGE);
                if (msg != null) {
                    AsyncResult ar = (AsyncResult) msg.obj;
                    String str = new String(((byte[]) (ar.result)), "utf_8");
                }
                phone.setRadioPower(true);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("xxx", "ci.setRadioPower  Exception");
            }

        }
        checkHave4g();
    }

    private String[] atCommand(String command) {
        String[] back = { command, "fail" };
        try {
            phone.invokeOemRilRequestRaw(command.getBytes("utf_8"),
                    mHandler.obtainMessage(EVENT_OEM_RIL_MESSAGE));
            Message msg = mGSMTestHandler.waitForMessage(EVENT_OEM_RIL_MESSAGE);
            if (msg != null) {
                AsyncResult ar = (AsyncResult) msg.obj;
                back[0] = command;
                back[1] = new String(((byte[]) (ar.result)), "utf_8");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("xxx", "atCommand  Exception");
        }
        return back;
    }

    /**
     * reset 2g/3g/4g
     * */
    public String[] reset() {
        return atCommand(resetCommand);
    }

    /**
     * Reduction by RAT
     * */
    public String[] reductionRat(String db_2g, String db_3g, String db_4g) {
        String command = reductionByRAT + db2level(db_2g) + "," + db2level(db_3g) + ","
                + db2level(db_4g);
        return atCommand(command);
    }

    /**
     * command
     * */
    public String[] command(String command) {
        return atCommand(command);
    }

    public String[] reductionRatByBand(int modemIndex, int bandIndex, String dbValue) {
        switch (modemIndex) {
        case 0:
            return reductionRatByBand2G(bandIndex, dbValue);
        case 1:
            return reductionRatByBand3G(bandIndex, dbValue);
        case 2:
            return reductionRatByBand4G(bandIndex, dbValue);
        }
        return new String[] { "", "fail" };
    }

    /**
     * unit 1/8db 2g
     * 
     * @param bandIndex
     * */
    public String[] reductionRatByBand2G(int bandIndex, String dbValue) {
        // FrequencyBand850, FrequencyBand900,
        // FrequencyBand1800,FrequencyBand1900
        // GMSK, 8PSK
        int level[] = { 255, 255, 255, 255 };
        level[bandIndex] = db2level(dbValue);
        String command = reductionByRATByBand2g + getDBString(level[0], 8) + ","
                + getDBString(level[1], 8) + "," + getDBString(level[2], 8) + ","
                + getDBString(level[3], 8);
        return atCommand(command);
    }

    /**
     * unit 1/8db 3g
     * 
     * @param bandIndex
     * */
    public String[] reductionRatByBand3G(int bandIndex, String dbValue) {
        // (UMTS BAND I : WCDMA_IMT_2000=0 UMTS BAND II : WCDMA_PCS_1900
        // UMTS BAND III : WCDMA_DCS_1800 UMTS BAND IV : WCDMA_AWS_1700
        // UMTS BAND V : WCDMA_CLR_850 UMTS BAND VI : WCDMA_800
        // UMTS BAND VII : WCDMA_IMT_E_2600 UMTS BAND VIII : WCDMA_GSM_900
        // UMTS BAND IX : WCDMA_1800 UMTS BAND X : WCDMA_1700,)
        // GMSK, 8PSK
        int level[] = { 255, 255, 255, 255, 255, 255, 255, 255, 255, 255 };
        level[bandIndex] = db2level(dbValue);
        String command = reductionByRATByBand3g + getDBString(level[0], 10) + ","
                + getDBString(level[1], 10) + "," + getDBString(level[2], 10) + ","
                + getDBString(level[3], 10) + "," + getDBString(level[4], 10) + ","
                + getDBString(level[5], 10) + "," + getDBString(level[6], 10) + ","
                + getDBString(level[7], 10) + "," + getDBString(level[8], 10) + ","
                + getDBString(level[9], 10);
        return atCommand(command);
    }

    /**
     * unit 1/8db 4g
     * 
     * @param bandIndex
     * */
    public String[] reductionRatByBand4G(int bandIndex, String dbValue) {
        // GMSK, 8PSK
        int level = db2level(dbValue);
        String command = reductionByRATByBand4g + (bandIndex + 1) + "," + level;
        return atCommand(command);
    }

    public void checkHave4g() {
        String result = atCommand(checkHave4g)[1];
        if (result.contains("ok") || result.contains("OK")) {

        } else {
            modem = new String[] { "2g", "3g" };
            band = new String[][] { band_2g, band_3g };
        }
    }

    private int db2level(String db) {
        return Integer.parseInt(db) * 8;
    }

    private String getDBString(int db, int num) {
        String dbString = "";
        for (int i = 0; i < num; i++) {
            if (i == 0) {
                dbString = db + "";
            } else {
                dbString = dbString + "," + db;
            }
        }
        return dbString;
    }

}
