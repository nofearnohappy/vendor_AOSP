package com.mediatek.nfc.dta;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Locale;
import android.app.Activity;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.nfc.tech.NfcF;
import android.nfc.tech.NfcV;
import android.nfc.tech.MifareUltralight;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.widget.Toast;
import android.widget.Button;
import android.widget.TextView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.KeyEvent;
public class NdefReadWrite extends Activity implements OnClickListener {

    private static final String TAG = "DTA";
    private static final String SUB_TAG = "[Ndef-R/W]";
    private static final boolean DBG = true;

    private NfcAdapter mAdapter;
    private PendingIntent mPendingIntent;
    private IntentFilter[] mFilters;
    private String[][] mTechLists;
    private NdefMessage mNdefMessage;

    private Button mBackButton;
    private TextView mPatternNumberText;
    private TextView mResultText;

    private int mPatternNumber;
    private ProgressDialog mProgressDialog;
    private IntentFilter mIntentFilter;

    private Tag mCurrentTag;
    private NativeDtaManager mDtaManager;

    private byte [][] T2T_NDA_BV_6_0 = {
        { (byte)0xa2, (byte)0x03, (byte)0xe1, (byte)0x10, (byte)0x06, (byte)0x0f },
        { (byte)0xa2, (byte)0x02, (byte)0x79, (byte)0xc8, (byte)0xff, (byte)0xff }
    };
    private byte [][] T2T_NDA_BV_6_1 = {
        { (byte)0xa2, (byte)0x03, (byte)0xe1, (byte)0x10, (byte)0xfe, (byte)0x0f },
        { (byte)0xa2, (byte)0x02, (byte)0x79, (byte)0xc8, (byte)0xff, (byte)0xff },
        { (byte)0xa2, (byte)0x00, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff },
        { (byte)0xa2, (byte)0x01, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff },
        { (byte)0xa2, (byte)0x02, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff },
        { (byte)0xa2, (byte)0x03, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff },
        { (byte)0xa2, (byte)0x04, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff },
        { (byte)0xa2, (byte)0x05, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff },
        { (byte)0xa2, (byte)0x06, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff },
        { (byte)0xa2, (byte)0x07, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff },
    };

    private byte [][] T2T_NDA_BV_6_2 = {
        { (byte)0xa2, (byte)0x03, (byte)0xe1, (byte)0x10, (byte)0x12, (byte)0x0f },
        { (byte)0xa2, (byte)0x02, (byte)0xA9, (byte)0x48, (byte)0xff, (byte)0xff },
        { (byte)0xa2,( byte)0x28, (byte)0xff, (byte)0xff, (byte)0x00, (byte)0x00 }
    };

    private byte [] T2T_SECTOR_SELECT_CMD_1 = {(byte)0xc2, (byte)0xff };
    //private byte [] T2T_SECTOR_SELECT_CMD_2 = {(byte)0x02, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00 };
    private byte [] T2T_SECTOR_SELECT_CMD_2 = {(byte)0x02, (byte)0x00, (byte)0x00, (byte)0x00 };


    //TC_T3T_NDA_BV_4
    //08 02 FE 00 01 02 03 04 05 01 09 00 01 80 00 10 0F 0C 00 93 00 00 00 00 0F 01 00 00 F0 01 BD
    //08 02 FE 00 01 02 03 04 05 01 09 00 0C 80 01 80 02 80 03 80 04 80 05 80 06 80 07 80 08 80 09 80 0A 80 0B 80 0C XX XX (192 Byte of NDEF Data)
    //08 02 FE 00 01 02 03 04 05 01 09 00 01 80 00 10 0F 0C 00 93 00 00 00 00 00 01 00 00 C0 01 7F
    private byte [] TC_T3T_NDA_BV_4_1 = {
        (byte)0x08,
        (byte)0x02, (byte)0xFE, (byte)0x00, (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04,
        (byte)0x05, (byte)0x01, (byte)0x09, (byte)0x00, (byte)0x01, (byte)0x80, (byte)0x00,
        (byte)0x10, (byte)0x0F, (byte)0x0C, (byte)0x00, (byte)0x93, (byte)0x00, (byte)0x00,
        (byte)0x00, (byte)0x00, (byte)0x0F, (byte)0x01, (byte)0x00, (byte)0x00, (byte)0xF0,
        (byte)0x01, (byte)0xBE
    };

    private byte [] TC_T3T_NDA_BV_4_2 = {
        (byte)0x08,
        (byte)0x02, (byte)0xFE, (byte)0x00, (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04,
        (byte)0x05, (byte)0x01, (byte)0x09, (byte)0x00, (byte)0x0C, (byte)0x80, (byte)0x01,
        (byte)0x80, (byte)0x02, (byte)0x80, (byte)0x03, (byte)0x80, (byte)0x04, (byte)0x80,
        (byte)0x05, (byte)0x80, (byte)0x06, (byte)0x80, (byte)0x07, (byte)0x80, (byte)0x08,
        (byte)0x80, (byte)0x09, (byte)0x80, (byte)0x0A, (byte)0x80, (byte)0x0B, (byte)0x80,
        (byte)0x0C
    };

    private byte [] TC_T3T_NDA_BV_4_3 = {
        (byte)0x08,
        (byte)0x02, (byte)0xFE, (byte)0x00, (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04,
        (byte)0x05, (byte)0x01, (byte)0x09, (byte)0x00, (byte)0x01, (byte)0x80, (byte)0x00,
        (byte)0x10, (byte)0x0F, (byte)0x0C, (byte)0x00, (byte)0x93, (byte)0x00, (byte)0x00,
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x00, (byte)0xC0,
        (byte)0x01, (byte)0x7F
    };

    // 08 02 FE 00 01 02 03 04 05 01 09 00 01 80 01 XX (16 byte)
    // 06 02 FE 00 01 02 03 04 05 01 09 00 01 80 01
    // 08 02 FE 00 01 02 03 04 05 01 09 00 01 80 01 0F 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
    private byte [] TC_T3T_MEM_BV_1_1 = {
        (byte)0x08,
        (byte)0x02, (byte)0xFE, (byte)0x00, (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04,
        (byte)0x05, (byte)0x01, (byte)0x09, (byte)0x00, (byte)0x01, (byte)0x80, (byte)0x01
    };

    private byte [] TC_T3T_MEM_BV_1_2 = {
        (byte)0x06,
        (byte)0x02, (byte)0xFE, (byte)0x00, (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04,
        (byte)0x05, (byte)0x01, (byte)0x09, (byte)0x00, (byte)0x01, (byte)0x80, (byte)0x01
    };

    private byte [] TC_T3T_MEM_BV_1_3 = {
        (byte)0x08,
        (byte)0x02, (byte)0xFE, (byte)0x00, (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04,
        (byte)0x05, (byte)0x01, (byte)0x09, (byte)0x00, (byte)0x01, (byte)0x80, (byte)0x01,
        (byte)0x0F, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
        (byte)0x00, (byte)0x00
    };

    //08 02 FE 00 01 02 03 04 05 0C 09 00 49 11 89 22 C9 33 09 44 49 55 89 66 C9 77 09 88 49 99 89 AA C9 BB 0C 80 00 81 00 82 00 83 00 84 00 85 00 86 00 87 00 88 00 89 00 8A 00 8B 00 XX XX  (192 byte)
    //06 02 FE 00 01 02 03 04 05 0F 0B 00 4B 11 8B 22 CB 33 0B 44 4B 55 8B 66 CB 77 0B 88 4B 99 8B AA CB BB 0B CC 4B DD 8B EE 0F 80 00 81 00 82 00 83 00 84 00 85 00 86 00 87 00 88 00 89 00 8A 00 8B 00 8C 00 8D 00 8E 00
    //08 02 FE 00 01 02 03 04 05 01 09 00 01 80 01 0F 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
    private byte [] TC_T3T_CSE_BV_1_1 = {
        (byte)0x08,
        (byte)0x02, (byte)0xFE, (byte)0x00, (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04,
        (byte)0x05, (byte)0x0C, (byte)0x09, (byte)0x00, (byte)0x49, (byte)0x11, (byte)0x89,
        (byte)0x22, (byte)0xC9, (byte)0x33, (byte)0x09, (byte)0x44, (byte)0x49, (byte)0x55,
        (byte)0x89, (byte)0x66, (byte)0xC9, (byte)0x77, (byte)0x09, (byte)0x88, (byte)0x49,
        (byte)0x99, (byte)0x89, (byte)0xAA, (byte)0xC9, (byte)0xBB, (byte)0x0C, (byte)0x80,
        (byte)0x00, (byte)0x81, (byte)0x00, (byte)0x82, (byte)0x00, (byte)0x83, (byte)0x00,
        (byte)0x84, (byte)0x00, (byte)0x85, (byte)0x00, (byte)0x86, (byte)0x00, (byte)0x87,
        (byte)0x00, (byte)0x88, (byte)0x00, (byte)0x89, (byte)0x00, (byte)0x8A, (byte)0x00,
        (byte)0x8B, (byte)0x00
    };

    private byte [] TC_T3T_CSE_BV_1_2 = {
        (byte)0x06,
        (byte)0x02, (byte)0xFE, (byte)0x00, (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04,
        (byte)0x05, (byte)0x0F, (byte)0x0B, (byte)0x00, (byte)0x4B, (byte)0x11, (byte)0x8B,
        (byte)0x22, (byte)0xCB, (byte)0x33, (byte)0x0B, (byte)0x44, (byte)0x4B, (byte)0x55,
        (byte)0x8B, (byte)0x66, (byte)0xCB, (byte)0x77, (byte)0x0B, (byte)0x88, (byte)0x4B,
        (byte)0x99, (byte)0x8B, (byte)0xAA, (byte)0xCB, (byte)0xBB, (byte)0x0B, (byte)0xCC,
        (byte)0x4B, (byte)0xDD, (byte)0x8B, (byte)0xEE, (byte)0x0F, (byte)0x80, (byte)0x00,
        (byte)0x81, (byte)0x00, (byte)0x82, (byte)0x00, (byte)0x83, (byte)0x00, (byte)0x84,
        (byte)0x00, (byte)0x85, (byte)0x00, (byte)0x86, (byte)0x00, (byte)0x87, (byte)0x00,
        (byte)0x88, (byte)0x00, (byte)0x89, (byte)0x00, (byte)0x8A, (byte)0x00, (byte)0x8B,
        (byte)0x00, (byte)0x8C, (byte)0x00, (byte)0x8D, (byte)0x00, (byte)0x8E, (byte)0x00
    };

    private byte [] TC_T3T_CSE_BV_1_3 = {
        (byte)0x08,
        (byte)0x02, (byte)0xFE, (byte)0x00, (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04,
        (byte)0x05, (byte)0x01, (byte)0x09, (byte)0x00, (byte)0x01, (byte)0x80, (byte)0x01,
        (byte)0x0F, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
        (byte)0x00, (byte)0x00
    };

    // 08 02 FE 00 01 02 03 04 05 04 09 00 49 11 89 22 C9 33 0D 80 00 80 01 80 02 81 00 81 01 81 02 82 00 82 01 82 02 83 00 83 01 83 02 83 03 XX XX  (208 byte)

    // 06 02 FE 00 01 02 03 04 05 05 0B 00 4B 11 8B 22 CB 33 0B 44 0F 80 00 80 01 80 02 81 00 81 01 81 02 82 00 82 01 82 02 83 00 83 01 83 02 84 00 84 01 84 02

    // 08 02 FE 00 01 02 03 04 05 07 09 00 49 11 89 22 C9 33 09 44 49 55 89 66 0C 00 00 00 00 01 00 01 00 00 01 01 00 02 00 00 02 01 00 03 00 00 03 01 00 04 00 00 04 01 00 05 00 00 06 00 00 XX XX (192 byte)

    // 06 02 FE 00 01 02 03 04 05 05 0B 00 4B 11 8B 22 CB 33 0B 44 0F 00 00 00 00 01 00 00 02 00 01 00 00 01 01 00 01 02 00 02 00 00 02 01 00 02 02 00 03 00 00 03 01 00 03 02 00 04 00 00 04 01 00 04 02 00

    // 08 02 FE 00 01 02 03 04 05 01 09 00 01 80 01 0F 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00

    private byte [] TC_T3T_CSE_BV_2_1 = {
        (byte)0x08,
        (byte)0x02, (byte)0xFE, (byte)0x00, (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04,
        (byte)0x05, (byte)0x04, (byte)0x09, (byte)0x00, (byte)0x49, (byte)0x11, (byte)0x89,
        (byte)0x22, (byte)0xC9, (byte)0x33, (byte)0x0D, (byte)0x80, (byte)0x00, (byte)0x80,
        (byte)0x01, (byte)0x80, (byte)0x02, (byte)0x81, (byte)0x00, (byte)0x81, (byte)0x01,
        (byte)0x81, (byte)0x02, (byte)0x82, (byte)0x00, (byte)0x82, (byte)0x01, (byte)0x82,
        (byte)0x02, (byte)0x83, (byte)0x00, (byte)0x83, (byte)0x01, (byte)0x83, (byte)0x02,
        (byte)0x83, (byte)0x03
    };

    private byte [] TC_T3T_CSE_BV_2_2 = {
        (byte)0x06,
        (byte)0x02, (byte)0xFE, (byte)0x00, (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04,
        (byte)0x05, (byte)0x05, (byte)0x0B, (byte)0x00, (byte)0x4B, (byte)0x11, (byte)0x8B,
        (byte)0x22, (byte)0xCB, (byte)0x33, (byte)0x0B, (byte)0x44, (byte)0x0F, (byte)0x80,
        (byte)0x00, (byte)0x80, (byte)0x01, (byte)0x80, (byte)0x02, (byte)0x81, (byte)0x00,
        (byte)0x81, (byte)0x01, (byte)0x81, (byte)0x02, (byte)0x82, (byte)0x00, (byte)0x82,
        (byte)0x01, (byte)0x82, (byte)0x02, (byte)0x83, (byte)0x00, (byte)0x83, (byte)0x01,
        (byte)0x83, (byte)0x02, (byte)0x84, (byte)0x00, (byte)0x84, (byte)0x01, (byte)0x84,
        (byte)0x02
    };

    private byte [] TC_T3T_CSE_BV_2_3 = {
        (byte)0x08,
        (byte)0x02, (byte)0xFE, (byte)0x00, (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04,
        (byte)0x05, (byte)0x07, (byte)0x09, (byte)0x00, (byte)0x49, (byte)0x11, (byte)0x89,
        (byte)0x22, (byte)0xC9, (byte)0x33, (byte)0x09, (byte)0x44, (byte)0x49, (byte)0x55,
        (byte)0x89, (byte)0x66, (byte)0x0C, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
        (byte)0x01, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x01, (byte)0x01,
        (byte)0x00, (byte)0x02, (byte)0x00, (byte)0x00, (byte)0x02, (byte)0x01, (byte)0x00,
        (byte)0x03, (byte)0x00, (byte)0x00, (byte)0x03, (byte)0x01, (byte)0x00, (byte)0x04,
        (byte)0x00, (byte)0x00, (byte)0x04, (byte)0x01, (byte)0x00, (byte)0x05, (byte)0x00,
        (byte)0x00, (byte)0x06, (byte)0x00, (byte)0x00
    };

    private byte [] TC_T3T_CSE_BV_2_4 = {
        (byte)0x06, (byte)0x02, (byte)0xFE, (byte)0x00, (byte)0x01, (byte)0x02, (byte)0x03,
        (byte)0x04, (byte)0x05, (byte)0x05, (byte)0x0B, (byte)0x00, (byte)0x4B, (byte)0x11,
        (byte)0x8B, (byte)0x22, (byte)0xCB, (byte)0x33, (byte)0x0B, (byte)0x44, (byte)0x0F,
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x00,
        (byte)0x02, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x01, (byte)0x01,
        (byte)0x00, (byte)0x01, (byte)0x02, (byte)0x00, (byte)0x02, (byte)0x00, (byte)0x00,
        (byte)0x02, (byte)0x01, (byte)0x00, (byte)0x02, (byte)0x02, (byte)0x00, (byte)0x03,
        (byte)0x00, (byte)0x00, (byte)0x03, (byte)0x01, (byte)0x00, (byte)0x03, (byte)0x02,
        (byte)0x00, (byte)0x04, (byte)0x00, (byte)0x00, (byte)0x04, (byte)0x01, (byte)0x00,
        (byte)0x04, (byte)0x02, (byte)0x00
    };

    private byte [] TC_T3T_CSE_BV_2_5 = {
        (byte)0x08,
        (byte)0x02, (byte)0xFE, (byte)0x00, (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04,
        (byte)0x05, (byte)0x01, (byte)0x09, (byte)0x00, (byte)0x01, (byte)0x80, (byte)0x01,
        (byte)0x0F, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
        (byte)0x00, (byte)0x00
    };


    // TC_T4T_BV4

    // 00 A4 04 00 07 d2 76 00 00 85 01 01 00
    // 00 A4 00 0C 02 E1 03
    // 00 B0 00 00 0F
    // 00 A4 00 0C 02 E1 04
    // 00 B0 00 00 12

    // 00 D6 00 00 02 00 00
    // 00 D6 00 02 0A 11 22 33 44 55 66 77 88 99 AA
    // 00 d6 00 00 02 00 0A
    // 00 d6 00 00 02

    // or  5 byte
    // 00 D6 00 00 02 00 00
    // 00 D6 00 02 05 11 22 33 44 55
    // 00 d6 00 00 02 00 05
    // 00 d6 00 00 02
    // 00 d6 00 00 02 00 05


    private byte [] TC_T4T_BV4_1 = {
        (byte)0x00, (byte)0xA4, (byte)0x04, (byte)0x00, (byte)0x07, (byte)0xd2, (byte)0x76,
        (byte)0x00, (byte)0x00, (byte)0x85, (byte)0x01, (byte)0x01, (byte)0x00
    };

    private byte [] TC_T4T_BV4_2 = {
        (byte)0x00, (byte)0xA4, (byte)0x00, (byte)0x0C, (byte)0x02, (byte)0xE1, (byte)0x03
    };

    private byte [] TC_T4T_BV4_3 = {
        (byte)0x00, (byte)0xB0, (byte)0x00, (byte)0x00, (byte)0x0F
    };

    private byte [] TC_T4T_BV4_4 = {
        (byte)0x00, (byte)0xA4, (byte)0x00, (byte)0x0C, (byte)0x02, (byte)0xE1, (byte)0x04
    };

    private byte [] TC_T4T_BV4_5 = {
        (byte)0x00, (byte)0xB0, (byte)0x00, (byte)0x00, (byte)0x12
    };

    /*   10 byte
         private byte [] TC_T4T_BV4_6 = {
         (byte)0x00, (byte)0xD6, (byte)0x00, (byte)0x00, (byte)0x02, (byte)0x00, (byte)0x00
         };

         private byte [] TC_T4T_BV4_7 = {
         (byte)0x00, (byte)0xD6, (byte)0x00, (byte)0x02, (byte)0x0A, (byte)0x11, (byte)0x22, (byte)0x33, (byte)0x44, (byte)0x55, (byte)0x66, (byte)0x77, (byte)0x88, (byte)0x99, (byte)0xAA
         };

         private byte [] TC_T4T_BV4_8 = {
         (byte)0x00, (byte)0xD6, (byte)0x00, (byte)0x00, (byte)0x02, (byte)0x00, (byte)0x0A
         };

         private byte [] TC_T4T_BV4_9 = {
         (byte)0x00, (byte)0xD6, (byte)0x00, (byte)0x00, (byte)0x02
         };
     */

    private byte [] TC_T4T_BV4_6 = {
        (byte)0x00, (byte)0xD6, (byte)0x00, (byte)0x00, (byte)0x02, (byte)0x00, (byte)0x00
    };

    private byte [] TC_T4T_BV4_7 = {
        (byte)0x00, (byte)0xD6, (byte)0x00, (byte)0x02, (byte)0x05, (byte)0x11, (byte)0x22,
        (byte)0x33, (byte)0x44, (byte)0x55
    };

    private byte [] TC_T4T_BV4_8 = {
        (byte)0x00, (byte)0xD6, (byte)0x00, (byte)0x00, (byte)0x02, (byte)0x00, (byte)0x05
    };

    private byte [] TC_T4T_BV4_9 = {
        (byte)0x00, (byte)0xD6, (byte)0x00, (byte)0x00, (byte)0x02
    };



    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (NfcAdapter.ACTION_ADAPTER_STATE_CHANGED.equals(action)) {
                    handleNfcStateChanged(intent.getIntExtra(NfcAdapter.EXTRA_ADAPTER_STATE,
                                NfcAdapter.STATE_OFF));
                }
            }
    };


    @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.ndef_read_write);
            mAdapter = NfcAdapter.getDefaultAdapter(this);
            mDtaManager = new NativeDtaManager();
            mDtaManager.setDtaMode(1);
            initUI();

            //parse intent
            Intent intent = getIntent();
            if (intent != null) {
                String action = intent.getAction();
                if (action != null && action.equals(DeviceTestAppConstants.ACTION_OPERATION_TEST_START)) {
                    Bundle bundle = intent.getExtras();
                    if(bundle != null){
                        mPatternNumber = bundle.getInt(DeviceTestAppConstants.DATA_PATTERN_NUMBER, 0x01);
                        mPatternNumberText.setText("" + mPatternNumber);
                        enableNfc();
                        initNdef();
                    }else{
                        if (DBG) Log.d(TAG, "Bundle is null.");
                    }
                }
            } else {
                if (DBG) Log.d(TAG, "intent is null.");
            }

        }

    @Override
        protected void onDestroy() {
            if (mDtaManager != null) {
                mDtaManager.setDtaMode(0);
            }
            super.onDestroy();
        }


    private void initUI() {
        mPatternNumberText = (TextView) findViewById(R.id.pattern_number);
        mResultText = (TextView) findViewById(R.id.test_result);
        mBackButton = (Button) findViewById(R.id.back);
        mBackButton.setOnClickListener(this);
    }

    private void initNdef() {

        if(mAdapter == null) {
            mResultText.setText(mResultText.getText() + "\n NfcAdapter is null");
        }
        mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
                                         getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        IntentFilter tech = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        IntentFilter tag = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);

        try {
            ndef.addDataType("*/*");
        } catch (MalformedMimeTypeException e) {
            throw new RuntimeException("[Fail] : ", e);
        }

        mFilters = new IntentFilter[] { ndef, tech, tag };
        //mFilters = new IntentFilter[] { ndef, tech };
        // Setup a tech list for all NfcF tags
        mTechLists = new String[][] { new String[] {
            Ndef.class.getName(),
                NfcA.class.getName(),
                NfcB.class.getName(),
                NfcF.class.getName()
        }};
    }

    @Override
        public void onResume() {
            super.onResume();
            if (mAdapter != null) {
                Log.d(TAG, SUB_TAG + " mAdapter != null");
                mAdapter.enableForegroundDispatch(this, mPendingIntent, mFilters, mTechLists);
            }
        }

    @Override
        public void onNewIntent(Intent intent) {
            if(DBG) Log.d(TAG, SUB_TAG + "onNewIntent : " + intent);
            try {
                String action = intent.getAction();
                Log.d(TAG, action);
                mResultText.setText(action);

                mCurrentTag = (Tag) intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                if (mCurrentTag == null){
                    mResultText.setText( mResultText.getText() + "\n mCurrentTag is null");
                }else{
                    mResultText.setText( mResultText.getText() + "\n" + mCurrentTag);

                    NfcF f = NfcF.get(mCurrentTag);
                    if (f != null) {
                        mResultText.setText( mResultText.getText() + "\n TypeF");
                        if(DBG) Log.e(TAG, SUB_TAG + " TypeF : \n");
                        runT3TTest(mCurrentTag, intent);

                    } else {
                        mResultText.setText( mResultText.getText() + "\n TypeA B");
                        if(DBG) Log.e(TAG, SUB_TAG + " TypeA B : \n");

                        Ndef ndefTag = Ndef.get(mCurrentTag);
                        if (ndefTag == null){
                            mResultText.setText( mResultText.getText() + "\n ndefTag is null");
                        }else{
                            //NfcA nfcATag = NfcA.get(mCurrentTag);
                            //if (ndefTag == null) mResultText.setText( mResultText.getText() + "\n nfcATag is null");

                            String ndefType = ndefTag.getType();
                            if (ndefType == null) {
                                mResultText.setText( mResultText.getText() + "\n ndefType is null");
                            }else{
                                mResultText.setText( mResultText.getText() + "\n" + ndefType);

                                if(ndefType.equals(Ndef.NFC_FORUM_TYPE_1)) {
                                    runT1TTest(mCurrentTag, intent);

                                } else if (ndefType.equals(Ndef.NFC_FORUM_TYPE_2)) {
                                    runT2TTest(mCurrentTag, intent);

                                } else if (ndefType.equals(Ndef.NFC_FORUM_TYPE_3)) {
                                    runT3TTest(mCurrentTag, intent);

                                } else if (ndefType.equals(Ndef.NFC_FORUM_TYPE_4)) {
                                    runT4TTest(mCurrentTag, intent);

                                } else {
                                    mResultText.setText("Unknown type!");
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, SUB_TAG + " error :" + e);
                mResultText.setText( mResultText.getText() + "\n" + e);
            }
        }

    @Override
        public void onPause() {
            super.onPause();
            if (mAdapter != null) mAdapter.disableForegroundDispatch(this);
        }

    @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.back:
                    if (DBG) Log.d(TAG, "Back");
                    runBack();
                    break;

                default:
                    Log.d(TAG, "Ghost button.");
                    break;
            }
        }
/**
 * 2015/06/08 add kill nfcstackp flow
 */
    private void runBack(){
        Intent intent = new Intent();
        intent.setClass(NdefReadWrite.this, DeviceTestApp.class);
        startActivity(intent);
        disableNfc();
        finish();
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (DBG) Log.d(TAG, "onKeyDown " + keyCode);

            switch (keyCode) {
                case KeyEvent.KEYCODE_MENU:
                    if (DBG) Log.d(TAG, "KEYCODE_MENU");
                    return true;

                case KeyEvent.KEYCODE_BACK:
                    if (DBG) Log.d(TAG, "KEYCODE_BACK");
                    runBack();
                    return true;

                case KeyEvent.KEYCODE_HOME:
                    if (DBG) Log.d(TAG, "KEYCODE_HOME");
                    return true;

                default:
                    //return true;
                    break;
        }
        return super.onKeyDown(keyCode, event);
    }
    private void disableNfc(){
        if (DBG) Log.d(TAG, "SWPTest disable Nfc");
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
        if(adapter != null){
          boolean  mNfcEnabled = adapter.isEnabled();
            if (mNfcEnabled) {
                if (DBG) Log.d(TAG, "Nfc is on");
                new NativeDtaManager().setDtaQuickMode(0);
                if (DBG) Log.d(TAG, "[QE]Nfc is on,Now to disable NFC");
                if (DBG) Log.d(TAG, "[QE]change Nfc Adapter to state_changed");
                mIntentFilter = new IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED);
                if (DBG) Log.d(TAG, "[QE]Start  DTA-JNI de-init");
                registerReceiver(mReceiver, mIntentFilter);
                if (DBG) Log.d(TAG, "[QE]setDtaQuickMode =1 ");
                new NativeDtaManager().setDtaQuickMode(1);
                if (adapter.disable()) {
                    mProgressDialog = ProgressDialog.show(NdefReadWrite.this,
                                                          "Disable Nfc", "Please wait ...", true);
                    mProgressDialog.show();
                }
            }
            if (DBG) Log.d(TAG, "Nfc is off");
        }else{
            if (DBG) Log.d(TAG, "Device isn't support nfc");
        }
    }
    //kill nfcstackp end
    private void enableNfc() {
        if (DBG) Log.d(TAG, SUB_TAG + "enable Nfc");
        if ( mAdapter == null) {
            mAdapter = NfcAdapter.getDefaultAdapter(this);
        }
        if ((mAdapter!=null) & !mAdapter.isEnabled()) {
            mIntentFilter = new IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED);
            registerReceiver(mReceiver, mIntentFilter);

            mAdapter.enable();
            mProgressDialog = ProgressDialog.show(NdefReadWrite.this,
                                                  "Enable Nfc", "Please wait ...", true);
            mProgressDialog.show();
        }
    }

    private void runT1TTest(Tag tag, Intent intent) {
        if(DBG) Log.d(TAG, "T1T Test");

        NdefMessage [] msgs;
        byte[] ndefRaw = {0x00, 0x00};
        NdefMessage writeMessage = null;
        String uri = "";
        //Ndef Message
        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        String testCase = "";
        if (rawMsgs != null) {
            msgs = new NdefMessage[rawMsgs.length];
            for (int i = 0; i < rawMsgs.length; i++) {
                msgs[i] = (NdefMessage) rawMsgs[i];
                ndefRaw = msgs[i].toByteArray();
                String ndefString = DeviceTestAppConstants.binaryToHex(ndefRaw, ndefRaw.length);
                mResultText.setText( mResultText.getText() + "\n" + ndefString);
            }
        } else {
            mResultText.setText( mResultText.getText() + "\n rawMsgs == null");
            ndefRaw = new byte[3];
            ndefRaw[2] = 0x00;
        }

        // NdefFormatable formatTag = NdefFormatable.get(mCurrentTag);

        Ndef ndef = Ndef.get(tag);
        int maxsize = 0;
        boolean isWritable = false;
        int LTLayout = 0;
        if(ndef != null){
            maxsize = ndef.getMaxSize();
            isWritable = ndef.isWritable();
            LTLayout = checkT1TLayout(maxsize, isWritable, ndefRaw);
            mResultText.setText(mResultText.getText() + "\n" + maxsize + ", layout = " + LTLayout);
        }else{
            mResultText.setText( mResultText.getText() + "\n ndef == null");
        }
        NfcA nfca = NfcA.get(tag);
        if (nfca == null){
            mResultText.setText( mResultText.getText() + "\n nfca == null");
        }else{
            if (mPatternNumber == 0x01) {
                mResultText.setText(mResultText.getText() + "\n[Read Test case]");
                switch(LTLayout){
                    case 1:
                        testCase = "TC_T1T_READ_BV_1";
                        break;
                    case 2:
                        break;
                    case 3:
                        testCase = "TC_T1T_READ_BV_2";
                        break;
                    case 4:
                        testCase = "TC_T1T_READ_BV_4";
                        break;
                    case 5:
                        break;
                    case 6:
                        testCase = "TC_T1T_READ_BV_5";
                        break;
                    case 7:
                        break;
                    default:
                        break;
                }
            } else if (mPatternNumber == 0x02) {
                mResultText.setText(mResultText.getText() + "\n[Write Test case]");
                try {
                    switch(LTLayout){
                        case 1:
                            testCase = "TC_T1T_WRITE_BV_2";
                            //uri = "abcdefghijklmnopqrstuvwxyzabcdefg.com";
                            uri = "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabc.com";
                            break;
                        case 2:
                            testCase = "TC_T1T_WRITE_BV_1";
                            //uri = "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabc.com";
                            uri = "abcdefghijklmnopqrstuvwxyzabcdefg.com";
                            break;
                        case 3:
                            break;
                        case 4:
                            testCase = "TC_T1T_WRITE_BV_5";
                            //if (isMsr3110) {
                            uri = "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdef" +
                                "ghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmn" +
                                "opqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuv" +
                                "wxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcd" +
                                "efghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijkl" +
                                "mnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrst" +
                                "uvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzab" +
                                "cdefghijklmnopqrstuvwxyzabcdefg.com";
                            //} else {
                            //    uri = "abc";
                            //}
                            break;
                        case 5:
                            testCase = "TC_T1T_WRITE_BV_4";
                            //if (isMsr3110) {
                            uri = "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcde" +
                                "fghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijkl" +
                                "mnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrs" +
                                "tuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz" +
                                "abcdefghijk.com";
                            //} else {
                            //    uri = "abc";
                            //}
                            break;
                        case 6:
                            break;
                        case 7:
                            testCase = "TC_T1T_WRITE_BV_3";
                            uri = "abcdefghijklmnopqrstuvwxyzabcdefg.com";
                            break;
                        default:
                            break;
                    }

                    NdefRecord uriRecord = createRtdUriRecord( uri, new Integer(0x01).byteValue());

                    writeMessage = new NdefMessage(new NdefRecord[]{uriRecord});

                    {
                        if ( T1TWrite(ndef, writeMessage) ) {
                            mResultText.setText(mResultText.getText() + "\n Write Success");
                        } else {
                            mResultText.setText(mResultText.getText() + "\n Write Fail");
                        }
                    }

                } catch(Exception e) {
                    Log.d(TAG, SUB_TAG + "error :" +e );
                    mResultText.setText(mResultText.getText() + "\n error: " +e);

                }
            } else if ( mPatternNumber == 0x03 ) {
                mResultText.setText(mResultText.getText() + "\n[Trans]");
                try {
                    //NfcA nfca = NfcA.get(tag);
                    nfca.connect();
                    byte[] cmd_0 = {(byte)0x53,(byte)0x0B,(byte)0x0F,(byte)0x00,(byte)0x11,
                                    (byte)0x22,(byte)0x33};
                    nfca.transceive(cmd_0);
                    byte[] cmd_1 = {(byte)0x53,(byte)0x70,(byte)0xFF,(byte)0x00,(byte)0x11,
                                    (byte)0x22,(byte)0x33};
                    nfca.transceive(cmd_1);
                    byte[] cmd_2 = {(byte)0x53,(byte)0x71,(byte)0xFF,(byte)0x00,(byte)0x11,
                                    (byte)0x22,(byte)0x33};
                    nfca.transceive(cmd_2);
                    nfca.close();

                } catch (Exception e) {
                    mResultText.setText(mResultText.getText() + "\n error: " +e);
                }
            }
        }
        mResultText.setText(mResultText.getText() + "\n" + testCase);
    }


    private void msrWriteNdef(NfcA tagA, int layout, NdefMessage message) {

        byte commandCode = 0x00;
        byte length = 0x00;
        byte [] command ;
        byte [] messageBytes  = message.toByteArray();
        int messageLength = messageBytes.length;
        int i,j;
        String tmp = DeviceTestAppConstants.binaryToHex(messageBytes, messageLength);
        mResultText.setText(mResultText.getText() + "\n" + tmp);
        if ( tagA == null) {
            mResultText.setText(mResultText.getText() + "\n msrWriteNdef , NfcA is null");
            return;
        }

        try {

            //connect
            tagA.connect();
            //send command
            switch(layout) {
                case 1:  // TC_T1T_WRITE_BV_2
                    //init
                    command = new byte[7];
                    command[3] = 0x00;
                    command[4] = 0x11;
                    command[5] = 0x22;
                    command[6] = 0x33;

                    command[0] = (byte)0x53;
                    //T
                    command[1] = (byte)0x08;
                    command[2] = (byte)0x00;
                    tagA.transceive(command);
                    //L
                    command[1] = (byte)0x0C;
                    command[2] = (byte)0x03;
                    tagA.transceive(command);
                    command[1] = (byte)0x0D;
                    command[2] = (byte)0x5A;
                    tagA.transceive(command);
                    //V
                    for(i=0; i < messageLength; i++) {
                        command[1]++;
                        command[2] = messageBytes[i];
                        tagA.transceive(command);
                    }

                    //FE
                    //command[1]++;
                    //command[2] = (byte)0xFE;
                    //tagA.transceive(command);

                    //update E1
                    command[1] = (byte)0x08;
                    command[2] = (byte)0xE1;
                    tagA.transceive(command);

                    break;
                case 2:  // TC_T1T_WRITE_BV_1
                    //init
                    command = new byte[7];
                    command[3] = 0x00;
                    command[4] = 0x11;
                    command[5] = 0x22;
                    command[6] = 0x33;


                    command[0] = (byte)0x53;
                    //T
                    command[1] = (byte)0x08;
                    command[2] = (byte)0x00;
                    tagA.transceive(command);
                    //L
                    command[1] = (byte)0x0C;
                    command[2] = (byte)0x03;
                    tagA.transceive(command);
                    command[1] = (byte)0x0D;
                    command[2] = (byte)0x2A;
                    tagA.transceive(command);
                    //V
                    for(i=0; i < messageLength; i++) {
                        command[1]++;
                        command[2] = messageBytes[i];
                        tagA.transceive(command);
                    }

                    //FE
                    command[1]++;
                    command[2] = (byte)0xFE;
                    tagA.transceive(command);

                    //update E1
                    command[1] = (byte)0x08;
                    command[2] = (byte)0xE1;
                    tagA.transceive(command);

                    break;
                case 4:  // TC_T1T_WRITE_BV_5

                    command = new byte[14];
                    command[10] = (byte)0x00;
                    command[11] = (byte)0x11;
                    command[12] = (byte)0x22;
                    command[13] = (byte)0x33;

                    command[0] = (byte)0x54;
                    //01
                    command[1] = (byte)0x01;
                    command[2] = (byte)0x00;
                    command[3] = (byte)0x10;
                    command[4] = (byte)0x3F;
                    command[5] = (byte)0x00;
                    command[6] = (byte)0x01;
                    command[7] = (byte)0x03;
                    command[8] = (byte)0xF2;
                    command[9] = (byte)0x30;
                    tagA.transceive(command);

                    //02
                    command[1] = (byte)0x02;
                    command[2] = (byte)0x33;
                    command[3] = (byte)0x02;
                    command[4] = (byte)0x03;
                    command[5] = (byte)0xF0;
                    command[6] = (byte)0x02;
                    command[7] = (byte)0x03;
                    command[8] = (byte)0x03;
                    command[9] = (byte)0xFF;
                    tagA.transceive(command);

                    //03
                    command[1] = (byte)0x03;
                    command[2] = (byte)0x01;
                    command[3] = (byte)0xCD;
                    command[4] = (byte)0xC1;
                    command[5] = (byte)0x01;
                    command[6] = (byte)0x00;
                    command[7] = (byte)0x00;
                    command[8] = (byte)0x01;
                    command[9] = (byte)0xC5;
                    tagA.transceive(command);

                    //04

                    for(i=4, j=6; i < 63; i++) {
                        command[1]++;
                        if (command[1] == 0x0D ||
                                command[1] == 0x0E ||
                                command[1] == 0x0F ) {
                            continue;
                        }
                        command[2] = messageBytes[j++];
                        command[3] = messageBytes[j++];
                        command[4] = messageBytes[j++];
                        command[5] = messageBytes[j++];
                        command[6] = messageBytes[j++];
                        command[7] = messageBytes[j++];
                        command[8] = messageBytes[j++];
                        command[9] = messageBytes[j++];

                        tagA.transceive(command);
                    }

                    //end-1
                    command[1]++;
                    command[2] = messageBytes[j++];
                    command[3] = messageBytes[j++];
                    command[4] = messageBytes[j++];
                    command[5] = messageBytes[j++];
                    command[6] = messageBytes[j++];
                    command[7] = messageBytes[j++];
                    command[8] = messageBytes[j++];
                    command[9] = (byte)0xFE;
                    tagA.transceive(command);

                    //end
                    command[1] = (byte)0x01;
                    command[2] = (byte)0xE1;
                    command[3] = (byte)0x10;
                    command[4] = (byte)0x3F;
                    command[5] = (byte)0x00;
                    command[6] = (byte)0x01;
                    command[7] = (byte)0x03;
                    command[8] = (byte)0xF2;
                    command[9] = (byte)0x30;
                    tagA.transceive(command);



                    break;
                case 5:  // TC_T1T_WRITE_BV_4

                    command = new byte[14];
                    command[10] = (byte)0x00;
                    command[11] = (byte)0x11;
                    command[12] = (byte)0x22;
                    command[13] = (byte)0x33;

                    command[0] = (byte)0x54;
                    //01
                    command[1] = (byte)0x01;
                    command[2] = (byte)0x00;
                    command[3] = (byte)0x10;
                    command[4] = (byte)0x3F;
                    command[5] = (byte)0x00;
                    command[6] = (byte)0x01;
                    command[7] = (byte)0x03;
                    command[8] = (byte)0xF2;
                    command[9] = (byte)0x30;
                    tagA.transceive(command);

                    //02
                    command[1] = (byte)0x02;
                    command[2] = (byte)0x33;
                    command[3] = (byte)0x02;
                    command[4] = (byte)0x03;
                    command[5] = (byte)0xF0;
                    command[6] = (byte)0x02;
                    command[7] = (byte)0x03;
                    command[8] = (byte)0x03;
                    command[9] = (byte)0xFE;
                    tagA.transceive(command);

                    //ndef message
                    //write block from 3~end
                    //byte  = (byte)0x03;

                    //int totalBlock = messageLength / 8;
                    //int leave = messageLength % 8;

                    //mResultText.setText(mResultText.getText() + "\n totalBlock =" + totalBlock + ",leave = " + leave);

                    for(i=3, j=0; i < 37; i++) {
                        command[1]++;
                        if (command[1] == 0x0D || command[1] == 0x0E || command[1] == 0x0F) {
                            continue;
                        }
                        command[2] = messageBytes[j++];
                        command[3] = messageBytes[j++];
                        command[4] = messageBytes[j++];
                        command[5] = messageBytes[j++];
                        command[6] = messageBytes[j++];
                        command[7] = messageBytes[j++];
                        command[8] = messageBytes[j++];
                        command[9] = messageBytes[j++];

                        tagA.transceive(command);
                    }

                    //mResultText.setText(mResultText.getText() + "\n ---- ");

                    //end-1
                    command[1]++;
                    command[2] = messageBytes[j++];
                    command[3] = messageBytes[j++];
                    command[4] = messageBytes[j++];
                    command[5] = messageBytes[j++];
                    command[6] = messageBytes[j++];
                    command[7] = messageBytes[j++];
                    command[8] = (byte)0xFE;
                    command[9] = (byte)0x00;
                    tagA.transceive(command);

                    //end
                    command[1] = (byte)0x01;
                    command[2] = (byte)0xE1;
                    command[3] = (byte)0x10;
                    command[4] = (byte)0x3F;
                    command[5] = (byte)0x00;
                    command[6] = (byte)0x01;
                    command[7] = (byte)0x03;
                    command[8] = (byte)0xF2;
                    command[9] = (byte)0x30;
                    tagA.transceive(command);


                    break;
                case 7:  // TC_T1T_WRITE_BV_3

                    break;
                default:
                    break;
            }

            //close
            tagA.close();

        } catch(Exception e) {
            mResultText.setText( mResultText.getText() + "\n" + e);
        }
    }

    private void runT2TTest(Tag tag, Intent intent) {
        if(DBG) Log.d(TAG, "T2T Test");
        NdefMessage [] msgs;
        byte[] ndefRaw = {0x00, 0x00};
        NdefMessage writeMessage = null;
        String uri = "";
        boolean makeReadOnly = false ;
        //Ndef Message
        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        String testCase = "";
        if (rawMsgs != null) {
            msgs = new NdefMessage[rawMsgs.length];
            for (int i = 0; i < rawMsgs.length; i++) {
                msgs[i] = (NdefMessage) rawMsgs[i];
                ndefRaw = msgs[i].toByteArray();
                String ndefString = DeviceTestAppConstants.binaryToHex(ndefRaw, ndefRaw.length);
                mResultText.setText( mResultText.getText() + "\n" + ndefString);
            }
        } else {
            mResultText.setText( mResultText.getText() + "\n rawMsgs == null");
        }

        Ndef ndef = Ndef.get(tag);
        if( ndef != null){
            int maxsize = ndef.getMaxSize();
            boolean isWritable = ndef.isWritable();
            boolean isEmptyNdef = true;

            if (mPatternNumber == 0x01) {
                mResultText.setText(mResultText.getText() + "\n[Read Test case]");

            } else if (mPatternNumber == 0x02) {
                mResultText.setText(mResultText.getText() + "\n[Write Test case]");
                mResultText.setText(mResultText.getText() + "\n maxsize " + maxsize);
                //if (rawMsgs == null && mifareData == null) {
                /// ---- workaround for NFC ---- {
                if( (maxsize == 2028) ) {
                    try {

                        NfcA typeA = NfcA.get(tag);
                        byte [] command = {0x30, 0x04};
                        byte [] receive;

                        typeA.connect();
                        receive = typeA.transceive(command);
                        typeA.close();

                        if (receive != null && receive[15] != 0x00) {
                            isEmptyNdef = false;
                            mResultText.setText(mResultText.getText() + "\n Ndef is not empty.");
                        } else {
                            isEmptyNdef = true;
                            mResultText.setText(mResultText.getText() + "\n Ndef is empty.");
                        }

                    } catch (Exception e) {
                        Log.d(TAG, SUB_TAG + "error :" + e );
                        mResultText.setText(mResultText.getText() + "\n error: " + e);
                    }
                }
                ///---- workaround for NFC ---- }

                if ( ( (rawMsgs == null) && (maxsize != 2028) ) ||
                    (isEmptyNdef  && (maxsize == 2028))) {
                    makeReadOnly = false;
                    if (maxsize == 46) {

                        uri = "n.com";
                        testCase = "TC_T2T_NDA_BV_4_0";

                    } else if (maxsize == 137) {
                        uri = "nfccccccccccc" +
                            "cccccccccccccccccccc" ;
                        testCase = "TC_T2T_NDA_BV_4_2";

                    } else if ( maxsize == 2028) {
                        uri = "n.commmmm" +
                            "commmmmm" +
                            "mmmmmmmm";

                        testCase = "TC_T2T_NDA_BV_4_1";
                    }

                    mResultText.setText(mResultText.getText() + "\n" + testCase);

                    try {
                        NdefRecord uriRecord = createRtdUriRecord( uri, new Integer(0x01).byteValue());

                        writeMessage = new NdefMessage(new NdefRecord[]{uriRecord});

                        if ( T2TWrite(ndef, writeMessage) ) {
                            mResultText.setText(mResultText.getText() + "\n Write Success");
                        } else {
                            mResultText.setText(mResultText.getText() + "\n Write Fail");
                        }

                    } catch (Exception e) {
                        Log.d(TAG, SUB_TAG + "error :" +e );
                        mResultText.setText(mResultText.getText() + "\n error: " +e);
                    }


                } else {
                    makeReadOnly = true;
                    NfcA nfca = NfcA.get(tag);
                    if(nfca != null){
                        try {
                            nfca.connect();
                            if (maxsize == 46) {
                                testCase = "TC_T2T_NDA_BV_6_0";
                                mResultText.setText(mResultText.getText() + "\n" + testCase);

                                nfca.transceive(T2T_NDA_BV_6_0[0]);

                                nfca.transceive(T2T_NDA_BV_6_0[1]);

                            } else if (maxsize == 2028) {
                                testCase = "TC_T2T_NDA_BV_6_1";
                                mResultText.setText(mResultText.getText() + "\n" + testCase);


                                nfca.transceive(T2T_NDA_BV_6_1[0]);

                                nfca.transceive(T2T_NDA_BV_6_1[1]);

                                nfca.transceive(T2T_SECTOR_SELECT_CMD_1);

                                nfca.transceive(T2T_SECTOR_SELECT_CMD_2);

                                nfca.transceive(T2T_NDA_BV_6_1[2]);

                                nfca.transceive(T2T_NDA_BV_6_1[3]);

                                nfca.transceive(T2T_NDA_BV_6_1[4]);

                                nfca.transceive(T2T_NDA_BV_6_1[5]);

                                nfca.transceive(T2T_NDA_BV_6_1[6]);

                                nfca.transceive(T2T_NDA_BV_6_1[7]);

                                nfca.transceive(T2T_NDA_BV_6_1[8]);

                                nfca.transceive(T2T_NDA_BV_6_1[9]);


                            } else if ( maxsize == 137) {
                                testCase = "TC_T2T_NDA_BV_6_2";

                                mResultText.setText(mResultText.getText() + "\n" + testCase);

                                nfca.transceive(T2T_NDA_BV_6_2[0]);

                                nfca.transceive(T2T_NDA_BV_6_2[1]);

                                nfca.transceive(T2T_NDA_BV_6_2[2]);

                            }

                            nfca.close();

                        } catch (Exception e) {
                            Log.d(TAG, SUB_TAG + "error :" +e );
                            mResultText.setText(mResultText.getText() + "\n error: " +e);
                        }
                    }else{
                        mResultText.setText(mResultText.getText() + "nfa = null");
                    }

                }

            }
            }else{
                mResultText.setText( mResultText.getText() + "\n ndef == null");
            }

        }

        private void runT3TTest(Tag tag, Intent intent) {
            if(DBG) Log.d(TAG, "T3T Test");

            String testCase = "";
            NfcF nfcF = NfcF.get(tag);
            if(nfcF != null){
                if(DBG) Log.d(TAG, "runT3TTest, mPatternNumber = " + mPatternNumber);
                switch (mPatternNumber) {
                    case 0x01:  // NDEF READ
                    case 0x06:
                        if(DBG) Log.d(TAG, " NDEF READ");
                        break;

                    case 0x02:  //NDEF WRITE
                    case 0x07:
                        if(DBG) Log.d(TAG, " NDEF WRITE");
                        testCase = "TC_T3T_NDA_BV_4";
                        try {

                            int cmdSize = TC_T3T_NDA_BV_4_2.length;
                            byte command [] = new byte[cmdSize + 1] ;

                            for (int i=0; i < cmdSize + 1; i++) {
                                command[i] = (byte)0x00;
                            }

                            for (int i=0; i < cmdSize; i++) {
                                command[i] = TC_T3T_NDA_BV_4_2[i];
                            }

                            nfcF.connect();

                            nfcF.transceive(TC_T3T_NDA_BV_4_1);

                            nfcF.transceive(command);

                            nfcF.transceive(TC_T3T_NDA_BV_4_3);

                            nfcF.close();


                        } catch (Exception e) {
                            Log.d(TAG, SUB_TAG + "runT3TTest, TC_T3T_NDA_BV_4, Error :" + e );
                            mResultText.setText(mResultText.getText() + "\n runT3TTest, TC_T3T_NDA_BV_4, Error :" + e);
                        }

                        break;

                    case 0x03:  // MEM_BV_1 , FTH_BV_1
                        if(DBG) Log.d(TAG, " TC_T3T_MEM_BV_1 , TC_T3T_FTH_BV_1");
                        testCase = "TC_T3T_MEM_BV_1 or TC_T3T_FTH_BV_1";
                        try {

                            int cmdSize = TC_T3T_MEM_BV_1_1.length;
                            byte command [] = new byte[cmdSize + 1] ;

                            for (int i=0; i < cmdSize + 1; i++) {
                                command[i] = (byte)0x00;
                            }

                            for (int i=0; i < cmdSize; i++) {
                                command[i] = TC_T3T_MEM_BV_1_1[i];
                            }

                            nfcF.connect();

                            nfcF.transceive(command);

                            nfcF.transceive(TC_T3T_MEM_BV_1_2);

                            nfcF.transceive(TC_T3T_MEM_BV_1_3);

                            nfcF.close();


                        } catch (Exception e) {
                            Log.d(TAG, SUB_TAG + "runT3TTest, TC_T3T_CSE_BV_1, Error :" + e );
                            mResultText.setText(mResultText.getText() + "\n runT3TTest, TC_T3T_CSE_BV_1, Error :" + e);
                        }

                        break;

                    case 0x04:  // CSE_BV_1
                        if(DBG) Log.d(TAG, "TC_T3T_CSE_BV_1");
                        testCase = "TC_T3T_CSE_BV_1";
                        try {

                            int cmdSize = TC_T3T_CSE_BV_1_1.length;
                            byte command [] = new byte[cmdSize + 1] ;

                            for (int i=0; i < cmdSize + 1; i++) {
                                command[i] = (byte)0x00;
                            }

                            for (int i=0; i < cmdSize; i++) {
                                command[i] = TC_T3T_CSE_BV_1_1[i];
                            }

                            nfcF.connect();

                            int maxTransceiveLength = nfcF.getMaxTransceiveLength();

                            mResultText.setText(mResultText.getText() + "\n maxTransceiveLength = " + maxTransceiveLength);

                            nfcF.transceive(command);

                            nfcF.transceive(TC_T3T_CSE_BV_1_2);

                            nfcF.transceive(TC_T3T_CSE_BV_1_3);

                            nfcF.close();


                        } catch (Exception e) {
                            Log.d(TAG, SUB_TAG + "runT3TTest, TC_T3T_CSE_BV_1, Error :" + e );
                            mResultText.setText(mResultText.getText() + "\n runT3TTest, TC_T3T_CSE_BV_1, Error :" + e);
                        }

                        break;

                    case 0x05:  //CSE_BV_2
                        if(DBG) Log.d(TAG, " TC_T3T_CSE_BV_2");
                        testCase = "TC_T3T_CSE_BV_2";
                        try {

                            /// command 1
                            int cmdSize_1 = TC_T3T_CSE_BV_2_1.length;
                            mResultText.setText(mResultText.getText() + "\n command 1 = " + (cmdSize_1 + 208));

                            byte command_1 [] = new byte[cmdSize_1 + 1] ;

                            for (int i=0; i < ( cmdSize_1 + 1 ); i++) {
                                command_1[i] = (byte)0x00;
                            }

                            for (int i=0; i < cmdSize_1; i++) {
                                command_1[i] = TC_T3T_CSE_BV_2_1[i];
                            }
                            ///

                            /// command 3
                            int cmdSize_3 = TC_T3T_CSE_BV_2_3.length;
                            byte command_3 [] = new byte[cmdSize_3 + 1] ;

                            for (int i=0; i < cmdSize_3 + 1; i++) {
                                command_3[i] = (byte)0x00;
                            }

                            for (int i=0; i < cmdSize_3; i++) {
                                command_3[i] = TC_T3T_CSE_BV_2_3[i];
                            }
                            ///

                            nfcF.connect();

                            int maxTransceiveLength = nfcF.getMaxTransceiveLength();

                            mResultText.setText(mResultText.getText() + "\n maxTransceiveLength = " + maxTransceiveLength);

                            nfcF.transceive(command_1);

                            nfcF.transceive(TC_T3T_CSE_BV_2_2);

                            nfcF.transceive(command_3);

                            nfcF.transceive(TC_T3T_CSE_BV_2_4);

                            nfcF.transceive(TC_T3T_CSE_BV_2_5);

                            nfcF.close();

                        } catch (Exception e) {
                            Log.d(TAG, SUB_TAG + "runT3TTest, TC_T3T_CSE_BV_2, Error :" + e );
                            mResultText.setText(mResultText.getText() + "\n runT3TTest, TC_T3T_CSE_BV_2, Error :" + e);
                        }
                        break;

                    default:
                        if(DBG) Log.d(TAG, "default");
                        break;
                }
            }else{
                mResultText.setText(mResultText.getText() + "\n nfcF is null ");
            }

        }

        private void runT4TTest(Tag tag, Intent intent) {
            if(DBG) Log.d(TAG, "T4T Test");

            NdefMessage [] msgs;
            byte[] ndefRaw = {0x00, 0x00};
            NdefMessage writeMessage = null;
            String uri = "";
            String testCase = "";
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

            if (rawMsgs != null) {
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                    ndefRaw = msgs[i].toByteArray();
                    String ndefString = DeviceTestAppConstants.binaryToHex(ndefRaw, ndefRaw.length);
                    mResultText.setText( mResultText.getText() + "\n" + ndefString);
                }
            } else {
                mResultText.setText( mResultText.getText() + "\n rawMsgs == null");
                ndefRaw = new byte[3];
                ndefRaw[2] = 0x00;
            }

            Ndef ndef = Ndef.get(tag);
            if(ndef != null){
                int maxsize = ndef.getMaxSize();
                boolean isWritable = ndef.isWritable();

                if (mPatternNumber == 0x01) {
                    mResultText.setText(mResultText.getText() + "\n[Read Test case]");

                } else if (mPatternNumber == 0x02) {
                    mResultText.setText(mResultText.getText() + "\n[Write Test case]");
                    //if (isWritable) {
                    testCase = "TC_T4T_NDA_BV_4";

                    uri = "nfcccom.com";
                    NdefRecord uriRecord = createRtdUriRecord( uri, new Integer(0x01).byteValue());
                    writeMessage = new NdefMessage(new NdefRecord[]{uriRecord});

                    {

                        NfcA nfcA = NfcA.get(tag);

                        try {

                            nfcA.connect();

                            nfcA.transceive(TC_T4T_BV4_1);

                            nfcA.transceive(TC_T4T_BV4_2);

                            nfcA.transceive(TC_T4T_BV4_3);

                            nfcA.transceive(TC_T4T_BV4_4);

                            nfcA.transceive(TC_T4T_BV4_5);

                            nfcA.transceive(TC_T4T_BV4_6);

                            nfcA.transceive(TC_T4T_BV4_7);

                            nfcA.transceive(TC_T4T_BV4_8);

                            //nfcA.transceive(TC_T4T_BV4_9);

                            nfcA.transceive(TC_T4T_BV4_8);

                            nfcA.close();


                        } catch (Exception e) {
                            Log.d(TAG, SUB_TAG + "runT4TTest, TC_T4T_NDA_BV_4, Error :" + e );
                            mResultText.setText(mResultText.getText() + "\n runT4TTest, TC_T4T_NDA_BV_4, Error :" + e);
                        }

                    }
                    //} else {
                    //    testCase = "TC_T4T_NDA_BV_5";
                    //}
                }
                mResultText.setText(mResultText.getText() + "\n" + testCase);
            }else{
                mResultText.setText(mResultText.getText() + "\n nfcA is null ");
            }

        }

        private int checkT1TLayout(int maxsize, boolean isWritable, byte[] ndefRaw) {

            int layout = 0;
            mResultText.setText( mResultText.getText() +
                    "\n maxsize :" + maxsize +
                    "\nisWritable :" + isWritable +
                    "\nndefRaw[2] :" + (byte)ndefRaw[2] );
            if(maxsize <= 0x5A) { //static
                if (ndefRaw[2] == 0x26) {
                    if (isWritable) {
                        layout = 1;
                    } else {
                        layout = 7;
                    }
                } else if (ndefRaw[2] == 0x00) {
                    layout = 2;
                } else if (ndefRaw[2] == 0x56) {
                    layout = 3;
                }

            } else {  //dynamic
                if (-6 == ndefRaw[2]){
                    layout = 4;
                } else if (ndefRaw[2] == 0x00) {
                    layout = 5;
                }
            }

            return layout;
        }

        private boolean T1TWrite(Ndef ndef, NdefMessage message){
            int size = message.toByteArray().length;
            mResultText.setText( mResultText.getText() + "\n T1TWrite");
            String ndefString = DeviceTestAppConstants.binaryToHex(message.toByteArray(), message.toByteArray().length);
            mResultText.setText( mResultText.getText() + "\n" + ndefString);
            try {
                ndef.connect();
                if(!ndef.isWritable()){
                    return false;
                }
                if (ndef.getMaxSize() < size){
                    return false;
                }
                ndef.writeNdefMessage(message);
                return true;
            } catch(Exception e) {
                if (DBG)Log.e(TAG, SUB_TAG + "Exception" + e);
                return false;
            } finally {
                if(ndef != null){
                    try {
                        ndef.close();
                    }catch (IOException ioe){
                        if (DBG)Log.e(TAG, SUB_TAG + "Error closing tag ..." + ioe);
                    }
                }
            }
        }


        private boolean T2TWrite(Ndef ndef, NdefMessage message){
            int size = message.toByteArray().length;
            mResultText.setText( mResultText.getText() + "\n T2TWrite");
            String ndefString = DeviceTestAppConstants.binaryToHex(message.toByteArray(), message.toByteArray().length);
            mResultText.setText( mResultText.getText() + "\n" + ndefString);
            try {
                ndef.connect();
                if(!ndef.isWritable()){
                    return false;
                }
                if (ndef.getMaxSize() < size){
                    return false;
                }
                ndef.writeNdefMessage(message);
                return true;
            } catch(Exception e) {
                if (DBG)Log.e(TAG, SUB_TAG + "Exception" + e);
                return false;
            } finally {
                if(ndef != null){
                    try {
                        ndef.close();
                    }catch (IOException ioe){
                        if (DBG)Log.e(TAG, SUB_TAG + "Error closing tag ..." + ioe);
                    }
                }
            }
        }


        private boolean T4TWrite(Ndef ndef, NdefMessage message){
            int size = message.toByteArray().length;
            mResultText.setText( mResultText.getText() + "\n T4TWrite");
            String ndefString = DeviceTestAppConstants.binaryToHex(message.toByteArray(), message.toByteArray().length);
            mResultText.setText( mResultText.getText() + "\n" + ndefString);
            try {
                ndef.connect();
                if(!ndef.isWritable()){
                    return false;
                }
                if (ndef.getMaxSize() < size){
                    return false;
                }
                ndef.writeNdefMessage(message);
                return true;
            } catch(Exception e) {
                if (DBG)Log.e(TAG, SUB_TAG + "Exception" + e);
                return false;
            } finally {
                if(ndef != null){
                    try {
                        ndef.close();
                    }catch (IOException ioe){
                        if (DBG)Log.e(TAG, SUB_TAG + "Error closing tag ..." + ioe);
                    }
                }
            }
        }


        //RTD_URI
        public NdefRecord createRtdUriRecord(String uri, byte prefix){
            byte[] uriField = uri.getBytes(Charset.forName("US-ASCII"));
            byte[] payload = new byte[uriField.length + 1];
            payload[0] = prefix;
            System.arraycopy(uriField, 0, payload, 1, uriField.length);
            NdefRecord uriRecord = new NdefRecord (
                    NdefRecord.TNF_WELL_KNOWN,
                    NdefRecord.RTD_URI,
                    new byte[0],
                    payload
                    );
            return uriRecord;
        }


        private void handleNfcStateChanged(int newState) {
            switch (newState) {
                case NfcAdapter.STATE_OFF:
                    break;

                case NfcAdapter.STATE_ON:
                    if(mProgressDialog.isShowing()) {
                        mProgressDialog.dismiss();
                    }
                    unregisterReceiver(mReceiver);
                    break;

                case NfcAdapter.STATE_TURNING_ON:
                    break;

                case NfcAdapter.STATE_TURNING_OFF:
                    break;
            }
        }

    }