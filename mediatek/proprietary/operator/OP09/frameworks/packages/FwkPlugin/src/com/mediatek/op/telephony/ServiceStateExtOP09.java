package com.mediatek.op.telephony;

import android.content.Context;
import android.os.Environment;
import android.telephony.Rlog;
import android.telephony.SignalStrength;
import android.util.Log;
import android.util.Xml;

import com.android.internal.util.XmlUtils;

import com.mediatek.common.PluginImpl;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


@PluginImpl(interfaceName="com.mediatek.common.telephony.IServiceStateExt")
public class ServiceStateExtOP09 extends DefaultServiceStateExt {
    private static final String TAG = "CDMA";
    private static final String SPN_OVERRIDE_PATH = "etc/spn-conf-op09.xml";

    @Override
    public Map<String, String> loadSpnOverrides() {
        Map<String, String> items = new HashMap<String, String>();

        final File spnFile = new File(Environment.getRootDirectory(), SPN_OVERRIDE_PATH);
        Log.d(TAG, "load files: " + Environment.getRootDirectory() + "/" + SPN_OVERRIDE_PATH);
        FileReader spnReader = null;
        try {
            spnReader = new FileReader(spnFile);

            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(spnReader);

            XmlUtils.beginDocument(parser, "spnOverrides");
            while (true) {
                XmlUtils.nextElement(parser);
                String name = parser.getName();
                if (!"spnOverride".equals(name)) {
                    break;
                }
                String numeric = parser.getAttributeValue(null, "numeric");
                String data = parser.getAttributeValue(null, "spn");
                items.put(numeric, data);
            }

            Log.d(TAG, "load spn overrides ok.");
            return items;
        } catch (FileNotFoundException e) {
            Log.d(TAG, "Exception in spn-conf parser " + e);
            return null;
        } catch (XmlPullParserException e) {
            Log.d(TAG, "Exception in spn-conf parser " + e);
            return null;
        } catch (IOException e) {
            Log.d(TAG, "Exception in spn-conf parser " + e);
            return null;
        } finally {
            if (spnReader != null) {
                try {
                    spnReader.close();
                } catch (IOException e) {
                    Log.d(TAG, "Exception in spn-conf parser " + e);
                }
            }
        }
    }

    @Override
    public boolean needSpnRuleShowPlmnOnly() {
        return true;
    }

    @Override
    public boolean allowSpnDisplayed() {
        return false;
    }

    @Override
    public boolean supportEccForEachSIM() {
        return true;
    }

    @Override
    public void updateOplmn(Context context, Object ci) {
        new OplmnUpdateCenter().startOplmnUpdater(context, ci);
        Rlog.d(TAG, "custom for oplmn update");
    }

    @Override
    public int mapLteSignalLevel(int rsrp , int rssnr, int lteSignalStrength) {
        int rsrpIconLevel = SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN;
        int snrIconLevel = SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN;

        if (rsrp >= -105) {
            rsrpIconLevel = SignalStrength.SIGNAL_STRENGTH_GREAT;
        } else if (rsrp >= -114) {
            rsrpIconLevel = SignalStrength.SIGNAL_STRENGTH_GOOD;
        } else if (rsrp >= -118) {
            rsrpIconLevel = SignalStrength.SIGNAL_STRENGTH_MODERATE;
        } else if (rsrp >= -123) {
            rsrpIconLevel = SignalStrength.SIGNAL_STRENGTH_POOR;
        } else {
            rsrpIconLevel = SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN;
        }

        if (rssnr >= 9) {
            snrIconLevel = SignalStrength.SIGNAL_STRENGTH_GREAT;
        } else if (rssnr >= 1) {
            snrIconLevel = SignalStrength.SIGNAL_STRENGTH_GOOD;
        } else if (rssnr >= -3) {
            snrIconLevel = SignalStrength.SIGNAL_STRENGTH_MODERATE;
        } else if (rssnr >= -5) {
            snrIconLevel = SignalStrength.SIGNAL_STRENGTH_POOR;
        } else {
            snrIconLevel = SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN;
        }

        Rlog.d(TAG, "OP09_getLTELevel - rsrp:" + rsrp + " snr:" + rssnr + " rsrpIconLevel:"
                + rsrpIconLevel + " snrIconLevel:" + snrIconLevel);

        /* Choose a measurement type to use for notification */
        if (rsrpIconLevel != snrIconLevel) {
            /*
             * Use the lower one as the singal strength.
             */
            return (rsrpIconLevel < snrIconLevel ? rsrpIconLevel : snrIconLevel);
        }
        return rsrpIconLevel;
    }

    @Override
    public boolean isRoamingForSpecialSIM(String strServingPlmn, String strHomePlmn) {
        if (strServingPlmn != null && !strServingPlmn.startsWith("460")) {
            if ("45403".equals(strHomePlmn) || "45404".equals(strHomePlmn)) {
                Rlog.d(TAG, "special SIM, force roaming. IMSI:" + strHomePlmn);
                return true;
            }
        }
        return false;
    }
}
