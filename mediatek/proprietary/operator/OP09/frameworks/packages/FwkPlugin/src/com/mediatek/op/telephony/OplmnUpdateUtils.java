package com.mediatek.op.telephony;

import android.content.Context;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;


//import org.apache.http.client.ClientProtocolException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Calendar;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * The utilities class of OPLMN updating.
 *
 */
class OplmnUpdateUtils {
    static final String LOG_TAG_PHONE = "PHONE";
    static final String LOG_TAG = "OplmnUpdater";

    private static final String CACHED_OPLMN_FILE_PATH = "/sdcard/oplmn.plmn";
    private static final String AUTHEN_FILE_PATH = "/sdcard/ca.crt";
    private static final String VERSION = "version";
    private static final String ADDR = "addr";
    private static final String MD5 = "md5";
    private static final String MD5_DIGET = "MD5";

    // Judge if the CA cert could trust to access the URL.
    static SSLContext getSslContext() {
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            InputStream caInput = new BufferedInputStream(new FileInputStream(AUTHEN_FILE_PATH));
            try {
                if (caInput != null) {
                    Certificate ca = cf.generateCertificate(caInput);

                    String keyStoreType = KeyStore.getDefaultType();
                    KeyStore keyStore = KeyStore.getInstance(keyStoreType);
                    keyStore.load(null, null);
                    keyStore.setCertificateEntry("ca", ca);

                    String tmfAlgorithm = TrustManagerFactory
                            .getDefaultAlgorithm();
                    TrustManagerFactory tmf = TrustManagerFactory
                            .getInstance(tmfAlgorithm);
                    tmf.init(keyStore);

                    SSLContext sSslContext = SSLContext.getInstance("TLS");
                    sSslContext.init(null, tmf.getTrustManagers(), null);
                    return sSslContext;
                }
            } finally {
                if (caInput != null) {
                    caInput.close();
                }
            }
        } catch (CertificateException e) {
            logd("get the trust to the url fail:" + e.getMessage());
        } catch (KeyStoreException e) {
            logd("get the trust to the url fail:" + e.getMessage());
        } catch (IOException e) {
            logd("get the trust to the url fail:" + e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            logd("get the trust to the url fail:" + e.getMessage());
        } catch (KeyManagementException e) {
            logd("get the trust to the url fail:" + e.getMessage());
        }
        return null;
    }

    // Get basic information of OPLMN file.
    static OplmnInfo getOplmnInfo(SSLContext sslContext, URL url) {
        String content = getWebContent(sslContext, url);
        if (!TextUtils.isEmpty(content)) {
            InputStream is = null;
            try {
                is = new ByteArrayInputStream(content.getBytes());
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                Document document = null;
                try {
                    DocumentBuilder builder = factory.newDocumentBuilder();
                    document = builder.parse(is);
                } catch (ParserConfigurationException e) {
                    logd("failure to create documentBuilder: " + e.getMessage());
                } catch (SAXException e) {
                    logd("failure to parse document: " + e.getMessage());
                } catch (IOException e) {
                    logd("failure to parse document: " + e.getMessage());
                }
                if (document != null) {
                    document.getDocumentElement().normalize();
                    OplmnInfo oplmnInfo = new OplmnInfo();
                    oplmnInfo.mVersion = getInformation(document, VERSION);
                    oplmnInfo.mAddr = getInformation(document, ADDR);
                    oplmnInfo.mMd5 = getInformation(document, MD5);
                    logd("OPLMN info: " + oplmnInfo.toString());
                    return oplmnInfo;
                }
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        logd("close the stream fail: " + e);
                    }
                }
            }
        }

        return null;
    }

    // Compare the version between web server and local.
    static int compareVersion(String version1, String version2) {
        if (version1 == null || version1.length() == 0
                || version2 == null || version2.length() == 0) {
            logd("compareVersion, Invalid parameter!");
        }

        int index1 = 0;
        int index2 = 0;
        int result = 0;
        while (index1 < version1.length() && index2 < version2.length()) {
            int[] number1 = getValue(version1, index1);
            int[] number2 = getValue(version2, index2);

            if (number1[0] < number2[0]) {
                result = -1;
            } else if (number1[0] > number2[0]) {
                result = 1;
            } else {
                index1 = number1[1] + 1;
                index2 = number2[1] + 1;
            }
        }
        if (index1 == version1.length() && index2 == version2.length()) {
            result = 0;
        } else if (index1 < version1.length()) {
            result = 1;
        } else {
            result = -1;
        }
        return result;
    }

    // Download OPLMN file and do MD5 verification.
    static byte[] downloadOplmnFile(SSLContext sslContext, OplmnInfo oplmnInfo) {
        InputStream inputStream = null;
        OutputStream output = null;
        try {
            String oplmnAddr = oplmnInfo.getAddr();
            if (oplmnAddr != null) {
                URL url = new URL(oplmnAddr);
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                connection.setSSLSocketFactory(sslContext.getSocketFactory());
                inputStream = connection.getInputStream();
                File oplmnFile = new File(CACHED_OPLMN_FILE_PATH);
                oplmnFile.createNewFile();
                output = new FileOutputStream(oplmnFile);
                int length = (int) connection.getContentLength();
                byte[] oplmnBuffer = new byte[length];
                while (inputStream.read(oplmnBuffer) != -1) {
                    output.write(oplmnBuffer);
                }
                output.flush();

                String md5 = getMd5Digest(oplmnBuffer);
                if (!TextUtils.isEmpty(oplmnInfo.getMd5())
                        && !TextUtils.isEmpty(md5)
                        && oplmnInfo.getMd5().equals(md5)) {
                    return oplmnBuffer;
                }
            }
        } catch (MalformedURLException e) {
            logd("downloadOplmnFile ,initialize url fail: " + e.getMessage());
        } catch (IOException e) {
            logd("downloadOplmnFile , get inputstream fail: " + e.getMessage());
        } finally {
            try {
                if (output != null) {
                    output.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                logd("downloadOplmnFile , close the stream fail: " + e.getMessage());
            }
        }
        return null;
    }

    // Get the OPLMN file.
    static File getOplmnFile() {
        File oplmnFile = new File(CACHED_OPLMN_FILE_PATH);
        if (oplmnFile != null && oplmnFile.exists()) {
            return oplmnFile;
        }
        return null;
    }

    // Read the raw data of the OPLMN file.
    static byte[] readOplmnFile(File file) {
        ByteArrayOutputStream baos = null;
        BufferedInputStream bis = null;
        try {
            baos = new ByteArrayOutputStream(2000);
            bis = new BufferedInputStream(new FileInputStream(file));
            int ch = bis.read();
            while (ch != -1) {
                baos.write(ch);
                ch = bis.read();
            }
            return baos.toByteArray();
        } catch (IOException e) {
            logd("readOplmnFile, read file fail" + e.getMessage());
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                    logd("readOplmnFile, read file the inputstream close fail" + e.getMessage());
                }
            }
            if (baos != null) {
                try {
                    baos.close();
                } catch (IOException e) {
                    logd("readOplmnFile, read file the outputstream close fail " + e.getMessage());
                }
            }
        }
        return null;
    }

    // Parse the raw data and convert to string as modem format.
    static String parseOplmnAsModemFormat(String version, byte[] rawData) {
        StringBuilder oplmn = new StringBuilder();
        int count = 0;
        for (int i = 0; i + 5 <= rawData.length; i = i + 5) {
            byte temp1 = rawData[i];
            byte temp2 = rawData[i + 1];
            byte temp3 = rawData[i + 2];
            rawData[i] = (byte) (((temp1 << 4) & 0xF0) | (temp1 >> 4 & 0x0F));
            rawData[i + 1] = (byte) (((temp2 << 4) & 0xF0) | (temp3 & 0x0F));
            rawData[i + 2] = (byte) (((temp3 & 0xF0) | (temp2 >> 4 & 0x0F)));
            oplmn.append("\"");
            oplmn.append(String.format("%02x", rawData[i]));
            oplmn.append(String.format("%02x", rawData[i + 1]));
            oplmn.append(String.format("%02x", rawData[i + 2]));
            oplmn.append("\"").append(",");
            if ((rawData[i + 3] | 0x00) == 0) {
                oplmn.append("0").append(",");
            } else {
                oplmn.append("1").append(",");
            }
            oplmn.append("0");
            if ((rawData[i + 4] | 0x00) == 0) {
                oplmn.append("0").append(",");
            } else {
                oplmn.append("1").append(",");
            }
            if ((rawData[i + 2] & 0x0F) == 0x0F) {
                oplmn.deleteCharAt(oplmn.length() - 7);
            }
            count++;
        }
        oplmn.delete(oplmn.length() - 1, oplmn.length());
        StringBuilder oplmnInfo = new StringBuilder();
        oplmnInfo.append("\"").append(version).append("\"").append(",")
                .append(count).append(",").append(oplmn.toString());
        return oplmnInfo.toString();
    }

    // Post the result to web server.
    static void respondResult(Context context, SSLContext sslContext, URL url, String version,
            boolean result) {
        DataOutputStream out = null;
        try {
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setSSLSocketFactory(sslContext.getSocketFactory());
            if (conn != null) {
                try {
                    out = new DataOutputStream(conn.getOutputStream());
                    String postData = createXml(context, version, result);
                    byte[] xmlbyte = postData.getBytes("UTF-8");
                    conn.setConnectTimeout(6 * 1000);
                    conn.setDoOutput(true);
                    conn.setUseCaches(false);
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Charset", "UTF-8");
                    conn.setRequestProperty("Content-Length", String.valueOf(xmlbyte.length));
                    conn.setRequestProperty("Content-Type", "text/xml; charset=UTF-8");
                    out.write(xmlbyte);
                    out.flush();
                } catch (IOException e) {
                    logd("responseResult, post response fail: " + e.getMessage());
                }
            }
        /*}  catch (ClientProtocolException e) {
        //Todo: when android version is stable, then,
        // we will use this exception again.
            logd("responseResult, post response fail: " + e.getMessage());*/
        } catch (IOException e) {
            logd("responseResult, post response fail: " + e.getMessage());
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    logd("responseResult, close outpuststream fail: " + e.getMessage());
                }
            }
        }
    }

    // Get web site content by URL.
    private static String getWebContent(SSLContext sslContext, URL url) {
        try {
            StringBuffer buffer = new StringBuffer();
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setSSLSocketFactory(sslContext.getSocketFactory());
            BufferedReader br = null;
            if (conn != null) {
                try {
                    logd("The content of the URL is : ");
                    br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String outputContent;
                    while ((outputContent = br.readLine()) != null) {
                        buffer.append(outputContent);
                        logd("output :" + outputContent);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (br != null) {
                        br.close();
                    }
                }
            }
            return buffer.toString();
        } catch (MalformedURLException e) {
            logd("create the url fail: " + e.getMessage());
        } catch (IOException e) {
            logd("connect the url fail: " + e.getMessage());
        }
        return null;
    }

    // Get the MD5 diget for verification.
    private static String getMd5Digest(byte[] content) {
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance(MD5_DIGET);
        } catch (NoSuchAlgorithmException e) {
            logd("getMD5Digest, initialize MessageDigest fail" + e.getMessage());
        }

        byte[] diget = md5.digest(content);
        StringBuilder builder = new StringBuilder(diget.length * 2);
        for (byte b : diget) {
            builder.append(Integer.toHexString((b & 0xf0) >>> 4));
            builder.append(Integer.toHexString(b & 0x0f));
        }
        return builder.toString();
    }

    // Get the value of specified attribute in XML.
    private static String getInformation(Document document, String attribute) {
        NodeList list = document.getElementsByTagName(attribute);
        Node node = list.item(0);
        return node.getFirstChild().getNodeValue();
    }

    // Create the XML to respond to web server.
    private static String createXml(Context context, String version, boolean result) {
        StringWriter xmlWriter = new StringWriter();
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(
                Context.TELEPHONY_SERVICE);
        XmlSerializer xmlSerializer = null;
        try {
            XmlPullParserFactory pullParserFactory = XmlPullParserFactory.newInstance();
            xmlSerializer = pullParserFactory.newSerializer();
            xmlSerializer.setOutput(xmlWriter);
            xmlSerializer.startDocument("UTF-8", true);

            // Begin write xml
            xmlSerializer.startTag("", "xml");
            // the update time
            xmlSerializer.startTag("", "GTIME");
            xmlSerializer.text(getCurrentTime());
            xmlSerializer.endTag("", "GTIME");
            // the terminal version
            xmlSerializer.startTag("", "MODEL");
            xmlSerializer.text(Build.MODEL);
            xmlSerializer.endTag("", "MODEL");
            // the terminal IMSI
            xmlSerializer.startTag("", "GIMSI");
            xmlSerializer.text(telephonyManager.getSubscriberId());
            xmlSerializer.endTag("", "GIMSI");
            // the terminal MEID
            xmlSerializer.startTag("", "GMEID");
            xmlSerializer.text(telephonyManager.getDeviceId());
            xmlSerializer.endTag("", "GMEID");
            // the capability
            xmlSerializer.startTag("", "CAPABILITY");
            xmlSerializer.text("vodafone-plmn");
            xmlSerializer.endTag("", "CAPABILITY");
            // the oplmn version
            xmlSerializer.startTag("", "GVER");
            xmlSerializer.text(version);
            xmlSerializer.endTag("", "GVER");
            // the update state
            xmlSerializer.startTag("", "GSTATE");
            xmlSerializer.text(result ? "1" : "0");
            xmlSerializer.endTag("", "GSTATE");

            xmlSerializer.endTag("", "xml");
            xmlSerializer.endDocument();
            return xmlWriter.toString();
        } catch (XmlPullParserException e) {
            logd("Create XmlPullParserFactory fail:" + e.getMessage());
        } catch (IOException e) {
            logd("Can not output the xml Serialization: " + e.getMessage());
        }
        return null;
    }

    // Get the current date time.
    private static String getCurrentTime() {
        Calendar calendar = Calendar.getInstance();
        StringBuilder builder = new StringBuilder();
        builder.append(calendar.get(Calendar.YEAR));
        builder.append(calendar.get(Calendar.MONTH) + 1);
        builder.append(calendar.get(Calendar.DAY_OF_MONTH));
        builder.append(calendar.get(Calendar.HOUR_OF_DAY));
        builder.append(calendar.get(Calendar.MINUTE));
        builder.append(calendar.get(Calendar.SECOND));
        return builder.toString();
    }

    // Get the oplmn version value.
    private static int[] getValue(String version, int index) {
        int[] valueIndex = new int[2];
        StringBuilder sb = new StringBuilder();
        while (index < version.length() && version.charAt(index) != '.') {
            sb.append(version.charAt(index));
            index++;
        }
        valueIndex[0] = Integer.parseInt(sb.toString());
        valueIndex[1] = index;
        return valueIndex;
    }

    private static void logd(String msg) {
        Log.d("@M_" + LOG_TAG_PHONE, LOG_TAG + " " + msg);
    }

    /**
     * The internal class for the OPLMN information.
     *
     */
    static class OplmnInfo {
        private String mVersion;
        private String mMd5;
        private String mAddr;

        public String getVersion() {
            return mVersion;
        }

        public String getMd5() {
            return mMd5;
        }

        public String getAddr() {
            return mAddr;
        }

        public String toString() {
            return "mVersion = " + mVersion + "mAddr = " + mAddr + "mMd5 = " + mMd5;
        }
    }
}