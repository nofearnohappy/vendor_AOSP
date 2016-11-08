package com.mediatek.rcs.common.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.util.Log;

import com.android.vcard.exception.VCardException;
import com.android.vcard.exception.VCardVersionException;
import com.android.vcard.exception.VCardNotSupportedException;
import com.android.vcard.VCardEntry;
import com.android.vcard.VCardEntryCounter;
import com.android.vcard.VCardEntryConstructor;
import com.android.vcard.VCardEntryHandler;
import com.android.vcard.VCardParser;
import com.android.vcard.VCardParser_V21;
import com.android.vcard.VCardParser_V30;
import com.android.vcard.VCardSourceDetector;
import com.mediatek.rcs.common.utils.RcsVcardData;

import java.io.IOException;
import java.io.InputStream;
import java.io.FileNotFoundException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.List;
import java.util.ArrayList;


/**
* It is a vcard utils class, it is for get vcard entry count,parse vcard...
*/
public class RcsVcardUtils {

    private static final String TAG = "RcsVcardUtils";

    private static final int MAX_PHONE_SIZE = 3;

    public static final int VCARD_NO_ERROR = 0;
    public static final int VCARD_IO_ERROR = -1;
    public static final int VCARD_VERSION_ERROR = -2;
    public static final int VCARD_PARSE_ERROR = -3;
    public static final int VCARD_COUNT_ERROR = -4;
    public static final int VCARD_NO_SUPPORT_ERROR = -5;
    public static final int VCARD_FILE_FORMAT_ERROR = -6;
    public static final int VCARD_NO_FILE_ERROR = -7;

    final static int VCARD_VERSION_V21 = 1;
    final static int VCARD_VERSION_V30 = 2;

    final static int LABEL_PHONE = 0;
    final static int LABEL_EMAIL = 1;

    /**
    * this method is for get Vcard Entry Count.
    * @param file path.
    * @return vcard entry count.
    */
    public static int getVcardEntryCount(final String filePath) {
        VCardEntryCounter counter = null;
        VCardSourceDetector detector = null;
        VCardParser parser;
        int vcardVersion = VCARD_VERSION_V21;
        InputStream is;
        int vcardType = 0;

        try {
            is = new FileInputStream(filePath);
            parser = new VCardParser_V21();
            try {
                Log.d(TAG, "parse vcard according to V20");
                counter = new VCardEntryCounter();
                detector = new VCardSourceDetector();
                parser.addInterpreter(counter);
                parser.addInterpreter(detector);
                parser.parse(is);
            } catch (VCardVersionException e1) {
                try {
                    if (is != null) {
                        is.close();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "detect close IOException", e);
                }
                Log.d(TAG, "parse vcard according to V30");
                is = new FileInputStream(filePath);
                vcardVersion = VCARD_VERSION_V30;
                parser = new VCardParser_V30();
                try {
                    counter = new VCardEntryCounter();
                    detector = new VCardSourceDetector();
                    parser.addInterpreter(counter);
                    parser.addInterpreter(detector);
                    parser.parse(is);
                } catch (VCardVersionException e2) {
                    Log.e(TAG, "vcard version Exception", e2);
                    return VCARD_VERSION_ERROR;
                }
            } finally {
                try {
                    if (is != null) is.close();
                } catch (IOException e) {
                    Log.e(TAG, "detect close IOException", e);
                }
            }
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "detect wrong Format", e);
            return VCARD_FILE_FORMAT_ERROR;
        } catch (FileNotFoundException e) {
            Log.e(TAG, "detect no File", e);
            return VCARD_NO_FILE_ERROR;
        } catch (IOException e) {
            Log.e(TAG, "detect IOException", e);
            return VCARD_IO_ERROR;
        } catch (VCardException e) {
            Log.e(TAG, "detect vcard Exception", e);
            return VCARD_PARSE_ERROR;
        }
        Log.d(TAG, "vcard count = " + counter.getCount());
        return counter.getCount();
    }

    /**
    * this method is for parse vcard.
    * @param file path, VCardEntryHandler handler.
    * @return   error code.
    */
    public static int parseVcard(final String filePath, VCardEntryHandler handler) {
        VCardParser parser;
        int vcardVersion = VCARD_VERSION_V21;
        InputStream is = null;
        int vcardType = 0;
        VCardSourceDetector detector = null;
        detector = new VCardSourceDetector();

         try {
            is = new FileInputStream(filePath);
            parser = new VCardParser_V21();
            try {
                detector = new VCardSourceDetector();
                parser.addInterpreter(detector);
                parser.parse(is);
            } catch (VCardVersionException e1) {
                try {
                    if (is != null) is.close();
                } catch (IOException e) {
                    Log.e(TAG, "detect close IOException", e);
                }
                is = new FileInputStream(filePath);
                vcardVersion = VCARD_VERSION_V30;
                parser = new VCardParser_V30();
                try {
                    detector = new VCardSourceDetector();
                    parser.addInterpreter(detector);
                    parser.parse(is);
                } catch (VCardVersionException e2) {
                    Log.e(TAG, "vcard version Exception", e2);
                    return VCARD_VERSION_ERROR;
                }
            } finally {
                try {
                    if (is != null) is.close();
                } catch (IOException e) {
                    Log.e(TAG, "detect close IOException", e);
                }
            }
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "detect wrong Format", e);
            return VCARD_FILE_FORMAT_ERROR;
        } catch (FileNotFoundException e) {
            Log.e(TAG, "detect no File", e);
            return VCARD_NO_FILE_ERROR;
        } catch (IOException e) {
            Log.e(TAG, "detect IOException", e);
            return VCARD_IO_ERROR;
        } catch (VCardException e) {
            Log.e(TAG, "detect vcard Exception", e);
            return VCARD_PARSE_ERROR;
        }

        vcardType = detector.getEstimatedType();

        final VCardEntryConstructor constructor = new VCardEntryConstructor(vcardType);
        constructor.addEntryHandler(handler);
        try {
            is = new FileInputStream(filePath);
            Log.d(TAG, "vcard version: " + vcardVersion);
            parser = (vcardVersion == VCARD_VERSION_V30 ? new VCardParser_V30(vcardType) :
                    new VCardParser_V21(vcardType));
            parser.parse(is, constructor);
        } catch (IOException e) {
            Log.e(TAG, "IOException", e);
            return VCARD_IO_ERROR;
        } catch (VCardNotSupportedException e) {
            Log.e(TAG, "VCardNotSupportedException", e);
            return VCARD_NO_SUPPORT_ERROR;
        } catch (VCardVersionException e) {
            Log.e(TAG, "VCardVersionException", e);
            return VCARD_VERSION_ERROR;
        } catch (VCardException e) {
            Log.e(TAG, "VCardException", e);
            return VCARD_PARSE_ERROR;
        } finally {
            try {
                if (is != null) is.close();
            } catch (IOException e) {
                Log.e(TAG, "detect close IOException", e);
            }
        }
        return VCARD_NO_ERROR;
    }

    /**
    * this method is for parse vcard entry.
    * @param  vcard entry, context.
    * @return   RcsVcardParserResult.
    */
    public static RcsVcardParserResult ParseRcsVcardEntry(VCardEntry entry, Context context) {
        String name = null;
        String organization = null;
        String title = null;
        List<RcsVcardData> number = null;
        List<RcsVcardData> email = null;
        byte[] photo = null;
        String photoFormat = null;
        int size = 0;
        int i = 0;
        String type = null;

        name = entry.getDisplayName();
        Log.d(TAG, "ParseVCardEntry name: " + name);

        if (entry.getOrganizationList() != null) {
            organization = entry.getOrganizationList().get(0).getOrganizationName();
            Log.d(TAG, "ParseVCardEntry organization: " + organization);

            title = entry.getOrganizationList().get(0).getTitle();
            Log.d(TAG, "ParseVCardEntry title: " + title);
        }

        if (entry.getEmailList() != null) {
            email = new ArrayList<RcsVcardData>();
            size = entry.getEmailList().size();
            Log.d(TAG, "ParseVCardEntry email size: " + size);
            for (i = 0; i < size; i++) {
                type =  getLabelType(LABEL_EMAIL, entry.getEmailList().get(i).getType(), context);
                if (type == null) {
                    type = entry.getEmailList().get(i).getLabel();
                }
                email.add(new RcsVcardData(entry.getEmailList().get(i).getAddress(), type));
                Log.d(TAG, "ParseVCardEntry email: " + email.get(email.size() - 1).toString());
            }
        }

        if (entry.getPhotoList() != null) {
            photo = entry.getPhotoList().get(0).getBytes();
            photoFormat = entry.getPhotoList().get(0).getFormat();
            Log.d(TAG, "ParseVCardEntry photo: " + photoFormat);
        }

        if (entry.getPhoneList() != null) {
            number = new ArrayList<RcsVcardData>();
            size = entry.getPhoneList().size();
            Log.d(TAG, "ParseVCardEntry number size: " + size);
            for (i = 0; i < size; i++) {
                type =  getLabelType(LABEL_PHONE, entry.getPhoneList().get(i).getType(), context);
                if (type == null) {
                    type = entry.getPhoneList().get(i).getLabel();
                }
                number.add(new RcsVcardData(entry.getPhoneList().get(i).getNumber(), type));
                Log.d(TAG, "ParseVCardEntry number: " + number.get(number.size() - 1).toString());
            }
        }
        return new RcsVcardParserResult(name, organization, title, number,
                email, photo, photoFormat);
    }

    /**
    * this method is for get label type.
    * @param  vcard entry, context.
    * @return   Lable type.
    */
    public static String getLabelType(int element, int type, Context context) {
        String result = null;
        int res = 0;
        if (type != 0) {
            switch (element) {
                case LABEL_PHONE:
                    res = Phone.getTypeLabelResource(type);
                    break;
                case LABEL_EMAIL:
                    res = Email.getTypeLabelResource(type);
                    break;
                default:
                    Log.e(TAG, "getEmailType error: " + element);
                    break;
            }
            result = context.getString(res);
        }
        Log.d(TAG, "getType: " + result);
        return result;
    }
}
