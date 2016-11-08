package com.mediatek.voiceextension.cfg;

import android.content.Context;
import android.util.Log;

import com.mediatek.voiceextension.R;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;

/**
 * Read configuration file from voice service.
 *
 */
public class ConfigurationXml {

    static final String TAG = "ConfigurationXml";
    private final Context mContext;

    /**
     * ConfigurationXml constructor.
     *
     * @param context
     *            the Context in which this service is running
     */
    public ConfigurationXml(Context context) {
        mContext = context;
    }

    /**
     * read voice file path info from res.
     *
     * @param pathMap
     *            configuration file path map
     */
    public void readVoiceFilePathFromXml(HashMap<String, String> pathMap) {
        try {
            XmlPullParser parser = mContext.getResources().getXml(R.xml.viepath);

            int xmlEventType;
            String processName = null;
            String path = null;
            while ((xmlEventType = parser.next()) != XmlPullParser.END_DOCUMENT) {
                String name = parser.getName();
                if (xmlEventType == XmlPullParser.START_TAG && "Path".equals(name)) {
                    processName = parser.getAttributeValue(null, "Name");
                    path = parser.getAttributeValue(null, "Path");
                } else if (xmlEventType == XmlPullParser.END_TAG && "Path".equals(name)) {
                    if (processName != null & path != null) {
                        pathMap.put(processName, path);
                    } else {
                        Log.v(TAG, "the package has no voice command path ");
                    }
                }
            }
        } catch (FileNotFoundException e) {
            Log.v(TAG, "Got execption get xml.", e);
        } catch (XmlPullParserException e) {
            Log.v(TAG, "Got execption parsing paths.", e);
        } catch (IOException e) {
            Log.v(TAG, "Got execption parsing paths.", e);
        }
    }

}
