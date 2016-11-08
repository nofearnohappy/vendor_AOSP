package com.mediatek.rcs.message.location;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.util.TimeZone;

import android.location.Location;
import android.text.format.Time;
import android.util.Log;

import org.xml.sax.InputSource;

public class GeoLocUtils {

    private static final String TAG = "GeoLocUtils";
    private static final String CRLF = "\r\n";

    /**
    * generator a geoloc document
    *
    * @param geoloc
    * @param number sender phone number
    * @param msgId
    * @param fullPath XML document full path, not include file name
    * @return fullPath XML document full path and full name
    */
    public static String buildGeoLocXml(Location geoloc, String number,
                            String msgId, String fullPath) {

        String expire = encodeDate(geoloc.getTime());
        String str= "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + CRLF +
            "<rcsenveloppe xmlns=\"urn:gsma:params:xml:ns:rcs:rcs:geolocation\"" +
            " xmlns:rpid=\"urn:ietf:params:xml:ns:pidf:rpid\"" +
            " xmlns:gp=\"urn:ietf:params:xml:ns:pidf:geopriv10\"" +
            " xmlns:gml=\"http://www.opengis.net/gml\"" +
            " xmlns:gs=\"http://www.opengis.net/pidflo/1.0\"" +
            " entity=\"tel: "+ number +"\">" + CRLF +
            "<rcspushlocation id=\""+ "a1233" +"\" label=\"Location\">" + CRLF +
            "<rpid:place-type rpid:until=\""+ expire +"\">" + CRLF +
            "<rpid:other>Location</rpid:other>" + CRLF +
            "</rpid:place-type>" + CRLF +
            "<rpid:time-offset rpid:until=\""+ expire +"\"></rpid:time-offset>" + CRLF +
            "<gp:geopriv>" + CRLF +
            "<gp:location-info>" + CRLF +
            "<gs:Circle srsName=\"urn:ogc:def:crs:EPSG::4326\">" + CRLF +
            "<gs:radius uom=\"urn:ogc:def:uom:EPSG::9001\">" + geoloc.getAccuracy() +
            "</gs:radius>" + CRLF +
            "<gml:pos>"+ geoloc.getLatitude()+" "+geoloc.getLongitude() +"</gml:pos>" + CRLF +
            "</gs:Circle>" + CRLF +
            "</gp:location-info>" + CRLF +
            "<gp:usage-rules>" + CRLF +
            "<gp:retention-expiry>"+ expire +"</gp:retention-expiry>" + CRLF +
            "</gp:usage-rules>" + CRLF +
            "</gp:geopriv>" + CRLF +
            "<timestamp>"+ encodeDate(System.currentTimeMillis()) +"</timestamp>" + CRLF +
            "</rcspushlocation>" + CRLF +
            "</rcsenveloppe>";

        File file = new File(fullPath);
        if (!file.exists()) {
            file.mkdir();
        }
        String name = fullPath + "geoloc_" + System.currentTimeMillis() + ".xml";
        try {
            FileWriter fw = new FileWriter(name, false);
            fw.flush();
            fw.write(str);
            fw.close();
        } catch (IOException e) {
            Log.e(TAG, "buildGeoLocXml IOException,", e);
        }
        /*
        try {
            FileOutputStream outputStream = new FileOutputStream(file);
            outputStream.write(str.getBytes());
            outputStream.close();
        } catch (FileNotFoundException e) {
            Log.e(TAG, "buildGeoLocXml FileNotFoundException,", e);
        } catch (IOException e) {
            Log.e(TAG, "buildGeoLocXml IOException,", e);
        }*/

        Log.d(TAG, "buildGeoLocXml:" + str);
        Log.d(TAG, "buildGeoLocXml, file name is " + name);
        return name;
    }

    /**
    * Parse a geoloc document
    *
    * @param fullPath XML document full path and full name
    * @return GeoLocXmlParser
    */
    public static GeoLocXmlParser parseGeoLocXml(String fullPath) {
        Log.d(TAG, "parseGeoLocXml, file name is " + fullPath);

        File file = new File(fullPath);
        try {
            FileReader reader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(reader);
            String line = "";
            String xml = "";
            while ((line = bufferedReader.readLine()) != null) {
                xml += line + "\n";
            }
            reader.close();
            Log.d(TAG, "parseGeoLocXml:" + xml);

            InputSource geolocInput = new InputSource(new ByteArrayInputStream(xml.getBytes()));
            GeoLocXmlParser geolocParser = new GeoLocXmlParser(geolocInput);
            return geolocParser;
        } catch (FileNotFoundException e) {
            Log.e(TAG, "parseGeolocXml FileNotFoundException,", e);
            return null;
        } catch (IOException e) {
            Log.e(TAG, "parseGeolocXml IOException,", e);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "parseGeolocXml Exception,", e);
            return null;
        }
    }

    /**
    * Encode a long date to string value in Z format (see RFC 3339)
    *
    * @param date Date in milliseconds
    * @return String
    */
    public static String encodeDate(long date) {
        Time t = new Time(TimeZone.getTimeZone("GMT-08:00").getID());
        t.set(date);
        return t.format3339(false);
    }

}

