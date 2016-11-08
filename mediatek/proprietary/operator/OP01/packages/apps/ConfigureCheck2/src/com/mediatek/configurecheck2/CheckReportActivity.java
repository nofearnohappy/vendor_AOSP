/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.mediatek.configurecheck2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.text.SimpleDateFormat;

import com.mediatek.configurecheck2.CheckResultActivity.ListViewItem;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemProperties;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

//IMEI
import android.telephony.TelephonyManager;
import com.android.internal.telephony.PhoneConstants;
import android.content.Context;

public class CheckReportActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "CheckReportActivity";

    private String mCheckCategory;
    private ArrayList<ListViewItem> mAllItemsArray;
    private ArrayList<ListViewItem> mWrongItemArray;
    private ArrayList<ListViewItem> mUnknownItemArray;
    private ArrayList<ListViewItem> mRightItemArray;
    private String mReportString;
    private String mCsvReportString;
    private TextView mReportTextView;
    private String mExportFileName;
    private String mExportDirStr;
    private File mExportDir;
    private File mExportFile;
    private String mNextLine = System.getProperty("line.separator");
    private Button mExportBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        CTSCLog.i(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.check_report);

        Intent intent = getIntent();
        mCheckCategory = intent.getStringExtra("Category");

            setTitle(mCheckCategory);

        mAllItemsArray = new ArrayList<ListViewItem>(CheckResultActivity.getAutoItemArray());
        mAllItemsArray.addAll(CheckResultActivity.getManualItemArray());

        mReportString = generateReport();
        mReportTextView = (TextView) findViewById(R.id.report_text);
        mReportTextView.setMovementMethod(ScrollingMovementMethod.getInstance());
        mReportTextView.setText(mReportString);

        mExportBtn = (Button) findViewById(R.id.export_btn);
        mExportBtn.setOnClickListener(this);
        if (!mCheckCategory.equals(getString(R.string.one_button_check))) {
            mExportBtn.setVisibility(View.GONE);
        }
    }

    private void appendReportString(StringBuilder sb, int index, ListViewItem item) {

        sb.append(" ").append(index).append(".")
            .append(item.mCheckItem.getTitle()).append(mNextLine);

        String value = item.mCheckItem.getValue();
        if (value != null && !value.isEmpty()) {
            if (value.contains(mNextLine)) {
                value = value.replace(mNextLine, mNextLine + "     ");
            }
            sb.append("     ").append(value).append(mNextLine);
        }
    }

    private String generateReport() {
        myAssert(mAllItemsArray != null, "Provider is null");

        int wrongNum = 0;
        int unknownNum = 0;
        int rightNum = 0;
        StringBuilder wrongItems = new StringBuilder("");
        StringBuilder unknownItems = new StringBuilder("");
        StringBuilder rightItems = new StringBuilder("");

        ListViewItem item;
        int count = mAllItemsArray.size();
        for (int i = 0; i < count; i++) {
            item = mAllItemsArray.get(i);
            switch (item.mResultIconId) {
            case R.drawable.ic_wrong:
                wrongNum++;
                appendReportString(wrongItems, wrongNum, item);
                break;
            case R.drawable.ic_unknown:
                unknownNum++;
                appendReportString(unknownItems, unknownNum, item);
                break;
            case R.drawable.ic_right:
                rightNum++;
                appendReportString(rightItems, rightNum, item);
                break;
            default:
                break;
            }
        }

        String wrongTitle = getString(R.string.wrong_tile);
        String unknownTitle = getString(R.string.unknown_title);
        String rightTitle = getString(R.string.right_title);

        StringBuilder report = new StringBuilder("");
        report.append(mCheckCategory).append(mNextLine);

        if (wrongNum != 0) {
            wrongTitle = String.format(wrongTitle, wrongNum);
            report.append(wrongTitle).append(mNextLine).append(wrongItems);
        }
        if (unknownNum != 0) {
            unknownTitle = String.format(unknownTitle, unknownNum);
            report.append(unknownTitle).append(mNextLine).append(unknownItems);
        }
        if (rightNum != 0) {
            rightTitle = String.format(rightTitle, rightNum);
            report.append(rightTitle).append(mNextLine).append(rightItems);
        }

        return report.toString();
    }

    private void myAssert(boolean noException, String log) {
        if (!noException) {
            throw new RuntimeException(TAG + "Assertion failed:" + log);
        }
    }

    private void getItemsByResult() {
        mWrongItemArray = new ArrayList<ListViewItem>();
        mUnknownItemArray = new ArrayList<ListViewItem>();
        mRightItemArray = new ArrayList<ListViewItem>();

        ListViewItem item;
        int count = mAllItemsArray.size();
        for (int i = 0; i < count; i++) {
            item = mAllItemsArray.get(i);
            switch (item.mResultIconId) {
            case R.drawable.ic_wrong:
                mWrongItemArray.add(item);
                break;
            case R.drawable.ic_unknown:
                mUnknownItemArray.add(item);
                break;
            case R.drawable.ic_right:
                mRightItemArray.add(item);
                break;
            default:
                break;
            }
        }
    }

    private int appendItemsStr_CSV(StringBuilder sb, ArrayList<ListViewItem> items, int index) {
        ListViewItem item;
        int count = items.size();
        for (int i = 0; i < count; i++) {
            item = items.get(i);
            index++;
            appendOneItemStr_CSV(sb, index, item);
        }
        return index;
    }

    private String getIMEIStr() {
        TelephonyManager tm = (TelephonyManager)getSystemService(
                Context.TELEPHONY_SERVICE);
        TelephonyManager tmEx = TelephonyManager.getDefault();

        if (Utils.MTK_GEMINI_SUPPORT == true) {
            String sim1IMEI = tmEx.getDeviceId(PhoneConstants.SIM_ID_1);
            String sim2IMEI = tmEx.getDeviceId(PhoneConstants.SIM_ID_2);

            if (null == sim1IMEI) sim1IMEI = "null";
            if (null == sim2IMEI) sim2IMEI = "null";
            
            StringBuilder sb = new StringBuilder();
            sb.append("SIM1: ")
              .append(sim1IMEI)
              .append(",")
              .append("SIM2: ")
              .append(sim2IMEI);

            return sb.toString();
        }

        if (null == tm.getDeviceId()) {
            return "null";
        } else {
            return tm.getDeviceId();
        }
    }

    private String getFileNameIMEI() {
        String result = getIMEIStr();

        if (Utils.MTK_GEMINI_SUPPORT == true) {
            result = result.replace("SIM1: ", "");
            result = result.replace("SIM2: ", "");
            result = result.replace(",", "_");
        }

        return result;
    }

    private String getTimeStr() {
        SimpleDateFormat formatTime = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        return formatTime.format(new java.util.Date());
    }

    private String getFileNameTime() {
        SimpleDateFormat formatTime = new SimpleDateFormat("yyyyMMddhhmmss");
        return formatTime.format(new java.util.Date());
    }

    private String generateReport_CSV() {
        myAssert(mAllItemsArray != null, "Provider is null");
        getItemsByResult();

        StringBuilder csvReport = new StringBuilder("");
        csvReport.append(mCheckCategory).append(mNextLine);

        csvReport.append(getString(R.string.export_time))
                 .append(",")
                 .append(getTimeStr())
                 .append(mNextLine);

        csvReport.append(getString(R.string.export_phone_info))
                 .append(",")
                 .append(getIMEIStr())
                 .append(mNextLine);

        csvReport.append(getString(R.string.csv_report_title)).append(mNextLine);

        int index = 0;
        if (!mWrongItemArray.isEmpty()) {
            index = appendItemsStr_CSV(csvReport, mWrongItemArray, index);
        }
        if (!mUnknownItemArray.isEmpty()) {
            index = appendItemsStr_CSV(csvReport, mUnknownItemArray, index);
        }
        if (!mRightItemArray.isEmpty()) {
            index = appendItemsStr_CSV(csvReport, mRightItemArray, index);
        }

        myAssert(index == mAllItemsArray.size(), "wrong index when export report");
        return csvReport.toString();
    }

    private void appendOneItemStr_CSV(StringBuilder sb, int index, ListViewItem item) {

        sb.append(index).append(",")
            .append(item.mCheckItem.getTitle()).append(",");

        String value = item.mCheckItem.getValue();
        if (value != null && !value.isEmpty()) {
            if (value.contains(mNextLine)) {
                value = value.replace(mNextLine, " ");
            }
            sb.append(value);
        } else {
            sb.append("\"\"");
        }
        sb.append(",");

        switch (item.mResultIconId) {
        case R.drawable.ic_wrong:
            sb.append(getString(R.string.wrong_judge));
            break;
        case R.drawable.ic_unknown:
            sb.append(getString(R.string.unknown_judge));
            break;
        case R.drawable.ic_right:
            sb.append(getString(R.string.right_judge));
            break;
        default:
            break;
        }

        sb.append(",");
        String note = item.mCheckItem.getNote();
        if (note != null && !note.isEmpty()) {
            if (note.contains(mNextLine)) {
                note = note.replace(mNextLine, " ");
            }
            sb.append(note);
        } else {
            sb.append("\"\"");
        }

        sb.append(mNextLine);

    }

    private boolean delOldReports() {
        if (!mExportDir.exists()) {
            return false;
        }

        File[] files = mExportDir.listFiles();
        if (null == files) {
            myAssert(false, "mExportDir is not a directory!");
        }
        for (int i = 0; i < files.length; i++) {
            if (files[i].isFile() && files[i].getName().startsWith("CheckReport_")) {
                files[i].delete();
            }
        }

        return true;
    }

    private void exportCheckReport() {

        boolean exported = false;
        if (!isExternalStorageWritable()) {
            CTSCLog.i(TAG, "External storage is not available!");
        }
        if (!createExportDir()) {
            CTSCLog.i(TAG, "Create check report directory failed!");
        }

        delOldReports();

        //create file and write
        mCsvReportString = generateReport_CSV();
        byte[] cvsHead = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        mExportFileName = String.format(getString(R.string.export_file_name), getFileNameTime(), getFileNameIMEI());
        mExportFile = new File(mExportDir, mExportFileName);
        try {
            FileOutputStream fout = new FileOutputStream(mExportFile);
            fout.write(cvsHead);
            fout.write(mCsvReportString.getBytes());
            fout.close();
            exported = true;
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            CTSCLog.i(TAG, " File Not Found Export check report file failed!");
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            CTSCLog.i(TAG, "IO Exception Export check report file failed!");
            e.printStackTrace();
        }
        showAlertDlg(exported);
    }

    private void showAlertDlg(boolean exported) {
        String text;
        if (exported) {
            text = String.format(getString(R.string.export_file_succeed),
                    mExportFileName, mExportDirStr);
        } else {
            text = getString(R.string.export_file_failed);
        }
        final Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(text);
        builder.setPositiveButton("OK",  null);
        builder.create().show();
    }

    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    private boolean createExportDir() {
        mExportDirStr = getString(R.string.export_dir);
        mExportDirStr = String.format(mExportDirStr, Environment.getExternalStorageDirectory().getAbsolutePath());
        mExportDir = new File(mExportDirStr);
        if (!mExportDir.exists()) {
            if (!mExportDir.mkdirs()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        CTSCLog.d(TAG, "onClick View = " + v);
        switch (v.getId()) {
        case R.id.export_btn:
            exportCheckReport();
            break;

        default:
            break;
        }
    }
}
