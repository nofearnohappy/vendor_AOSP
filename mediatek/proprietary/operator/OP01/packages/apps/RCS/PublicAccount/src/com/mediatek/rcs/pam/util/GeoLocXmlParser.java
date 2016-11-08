package com.mediatek.rcs.pam.util;

import java.util.StringTokenizer;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;

public class GeoLocXmlParser extends DefaultHandler {

    /* Geoloc-Info SAMPLE:
    <?xml version="1.0" encoding="UTF-8"?>
    <rcsenveloppe xmlns="urn:gsma:params:xml:ns:rcs:rcs:geolocation" xmlns:rpid="urn:ietf:params:
    xml:ns:pidf:rpid" xmlns:gp="urn:ietf:params:xml:ns:pidf:geopriv10" xmlns:gml="http://www.
    opengis.net/gml" xmlns:gs="http://www.opengis.net/pidflo/1.0" entity="tel:+12345678901">
    <rcspushlocation id="a123" label ="meeting location" >
    <rpid:place-type rpid:until="2012-03-15T21:00:00-05:00">
    </rpid:place-type>
    <rpid:time-offset rpid:until="2012-03-15T21:00:00-05:00"></rpid:time-offset>
    <gp:geopriv>
    <gp:location-info>
    <gs:Circle srsName="urn:ogc:def:crs:EPSG::4326">
    <gml:pos>48.731964 -3.45829</gml:pos>
    <gs:radius uom="urn:ogc:def:uom:EPSG::9001">10</gs:radius>
    </gs:Circle>
    </gp:location-info>
    <gp:usage-rules>
    <gp:retention-expiry>2012-03-15T21:00:00-05:00</gp:retention-expiry>
    </gp:usage-rules>
    </gp:geopriv>
    <timestamp>2012-03-15T16:09:44-05:00</timestamp>
    </rcspushlocation>
    </rcsenveloppe>
    */

    private static final String TAG = "GeoLocXmlParser";
    private StringBuffer accumulator;
    private double latitude;
    private double longitude;

    public GeoLocXmlParser(InputSource inputSource) throws Exception {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();
        parser.parse(inputSource, this);
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void startDocument() {
        Log.d(TAG, "startDocument");
        accumulator = new StringBuffer();
    }

    public void characters(char buffer[], int start, int length) {
        accumulator.append(buffer, start, length);
        Log.d(TAG, "characters, accumulator:" + accumulator.toString());
    }

    public void startElement(String namespaceURL, String localName, String qname, Attributes attr) {
        accumulator.setLength(0);
        Log.d(TAG, "startElement, localName=" + localName + ",qname=" + qname);
    }

    public void endElement(String namespaceURL, String localName, String qname) {
        Log.d(TAG, "endElement, localName=" + localName + ",qname=" + qname);

        if (localName.equals("pos")) {
            Log.d(TAG, "endElement, pos:" + accumulator.toString().trim());
            StringTokenizer st = new StringTokenizer(accumulator.toString().trim());
        try {
            if (st.hasMoreTokens()) {
            latitude = Double.parseDouble(st.nextToken());
            Log.d(TAG, "endElement:latitude=" + latitude + ",longitude=" + longitude);
            }
            if (st.hasMoreTokens()) {
            longitude = Double.parseDouble(st.nextToken());
            Log.d(TAG, "endElement:latitude=" + latitude + ",longitude=" + longitude);
            }
            Log.d(TAG, "endElement:latitude=" + latitude + ",longitude=" + longitude);
        } catch(Exception e) {
            Log.e(TAG, "Can't parse geoloc value, ", e);
        }
    }
}

    public void endDocument() {
        Log.d(TAG, "endDocument");
    }
}

