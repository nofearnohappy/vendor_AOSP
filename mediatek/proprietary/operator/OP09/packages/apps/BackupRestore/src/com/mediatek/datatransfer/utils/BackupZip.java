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

package com.mediatek.datatransfer.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * @author
 *
 */
public class BackupZip {
    private static final String CLASS_TAG = MyLogger.LOG_TAG + "/BackupZip";

    private String mZipFile;

    /**
     * @param zipfile
     * @throws IOException.
     */
    public BackupZip(String zipfile) throws IOException {
        createZipFile(zipfile);
        mZipFile = zipfile;
    }

    public String getZipFileName() {
        return mZipFile;
    }

    // public static List<File> GetFileList(String zipFileString, boolean
    // bContainFolder, boolean bContainFile)throws IOException {
    // MyLogger.logI(CLASS_TAG, "GetFileList");

    // List<File> fileList = new ArrayList<File>();
    // ZipInputStream inZip = new ZipInputStream(new
    // FileInputStream(zipFileString));
    // ZipEntry zipEntry;
    // String szName = "";

    // while ((zipEntry = inZip.getNextEntry()) != null) {
    // szName = zipEntry.getName();
    // Log.e(CLASS_TAG, szName);

    // if (zipEntry.isDirectory()) {

    // // get the folder name of the widget
    // szName = szName.substring(0, szName.length() - 1);
    // File folder = new File(szName);
    // if (bContainFolder) {
    // fileList.add(folder);
    // }

    // } else {
    // File file = new File(szName);
    // if (bContainFile) {
    // fileList.add(file);
    // }
    // }
    // }//end of while

    // inZip.close();

    // return fileList;
    // }

    /**
     * @param zipFileString zipFileString.
     * @param bContainFolder bContainFolder.
     * @param bContainFile
     * @return List<String>.
     * @throws IOException.
     */
    public static List<String> getFileList(String zipFileString, boolean bContainFolder,
            boolean bContainFile) throws IOException {

        MyLogger.logI(CLASS_TAG, "GetFileList");

        List<String> fileList = new ArrayList<String>();
        ZipInputStream inZip = new ZipInputStream(new FileInputStream(zipFileString));
        ZipEntry zipEntry;
        String szName = "";

        while ((zipEntry = inZip.getNextEntry()) != null) {
            szName = zipEntry.getName();
            MyLogger.logD(CLASS_TAG, szName);

            if (zipEntry.isDirectory()) {

                // get the folder name of the widget
                szName = szName.substring(0, szName.length() - 1);
                if (bContainFolder) {
                    fileList.add(szName);
                }

            } else {
                if (bContainFile) {
                    fileList.add(szName);
                }
            }
        }

        inZip.close();
        if (fileList.size() > 0) {
            Collections.sort(fileList);
            Collections.reverse(fileList);
        }
        return fileList;
    }

    /**
     * @param zipFileString zipFileString
     * @param bContainFolder bContainFolder
     * @param bContainFile
     * @param tmpString
     * @return List<String>.
     * @throws IOException.
     */
    public static List<String> getFileList(String zipFileString, boolean bContainFolder,
            boolean bContainFile, String tmpString) throws IOException {

        MyLogger.logI(CLASS_TAG, "GetFileList");

        List<String> fileList = new ArrayList<String>();
        ZipInputStream inZip = new ZipInputStream(new FileInputStream(zipFileString));
        ZipEntry zipEntry;
        String szName = "";

        while ((zipEntry = inZip.getNextEntry()) != null) {
            szName = zipEntry.getName();
            MyLogger.logD(CLASS_TAG, szName);

            if (zipEntry.isDirectory()) {

                // get the folder name of the widget
                szName = szName.substring(0, szName.length() - 1);
                if (bContainFolder) {
                    if (tmpString == null) {
                        fileList.add(szName);
                    } else if (szName.matches(tmpString)) {
                        fileList.add(szName);
                    }
                }

            } else {
                if (bContainFile) {
                    if (tmpString == null) {
                        fileList.add(szName);
                    } else if (szName.matches(tmpString)) {
                        fileList.add(szName);
                    }
                }
            }
        }

        inZip.close();
        if (fileList.size() > 0) {
            Collections.sort(fileList);
            Collections.reverse(fileList);
        }
        return fileList;
    }

    /**
     * @param zipFileString
     * @param fileString
     * @return String.
     */
    public static String readFile(String zipFileString, String fileString) {
        MyLogger.logI(CLASS_TAG, "getFile");
        ByteArrayOutputStream baos = null;
        String content = null;
        try {
            ZipFile zipFile = new ZipFile(zipFileString);
            ZipEntry zipEntry = zipFile.getEntry(fileString);
            if (zipEntry != null) {
                InputStream is = zipFile.getInputStream(zipEntry);
                baos = new ByteArrayOutputStream();
                int len = -1;
                byte[] buffer = new byte[512];
                while ((len = is.read(buffer, 0, 512)) != -1) {
                    baos.write(buffer, 0, len);
                }
                content = baos.toString();
                is.close();
            }

            zipFile.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return content;
    }

    /**
     * @param zipFileString
     * @param fileString
     * @return byte[].
     */
    public static byte[] readFileContent(String zipFileString, String fileString) {
        MyLogger.logI(CLASS_TAG, "getFile");
        ByteArrayOutputStream baos = null;
        try {
            ZipFile zipFile = new ZipFile(zipFileString);
            ZipEntry zipEntry = zipFile.getEntry(fileString);
            if (zipEntry != null) {
                InputStream is = zipFile.getInputStream(zipEntry);
                baos = new ByteArrayOutputStream();
                int len = -1;
                byte[] buffer = new byte[512];
                while ((len = is.read(buffer, 0, 512)) != -1) {
                    baos.write(buffer, 0, len);
                }
                is.close();
            }

            zipFile.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        if (baos != null) {
            return baos.toByteArray();
        }
        return null;
    }

    /**
     * @param zipFileName
     * @return ZipFile.
     * @throws IOException.
     */
    public static ZipFile getZipFileFromFileName(String zipFileName) throws IOException {
        return new ZipFile(zipFileName);
    }

    /**
     * @param zipFile
     * @param fileString
     * @return String.
     */
    public static String readFile(ZipFile zipFile, String fileString) {
        MyLogger.logI(CLASS_TAG, "getFile");
        ByteArrayOutputStream baos = null;
        String content = null;
        try {
            ZipEntry zipEntry = zipFile.getEntry(fileString);
            if (zipEntry != null) {
                InputStream is = zipFile.getInputStream(zipEntry);
                baos = new ByteArrayOutputStream();
                int len = -1;
                byte[] buffer = new byte[512];
                while ((len = is.read(buffer, 0, 512)) != -1) {
                    baos.write(buffer, 0, len);
                }
                content = baos.toString();
                is.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return content;
    }

    /**
     * @param zipFile
     * @param fileString
     * @return byte[].
     */
    public static byte[] readFileContent(ZipFile zipFile, String fileString) {
        MyLogger.logI(CLASS_TAG, "getFile");
        ByteArrayOutputStream baos = null;
        try {
            ZipEntry zipEntry = zipFile.getEntry(fileString);
            if (zipEntry != null) {
                InputStream is = zipFile.getInputStream(zipEntry);
                baos = new ByteArrayOutputStream();
                int len = -1;
                byte[] buffer = new byte[512];
                while ((len = is.read(buffer, 0, 512)) != -1) {
                    baos.write(buffer, 0, len);
                }
                is.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return baos.toByteArray();
    }

    /**
     * @param zipFileName
     * @param srcFileName
     * @param destFileName
     * @throws IOException.
     */
    public static void unZipFile(String zipFileName, String srcFileName, String destFileName)
            throws IOException {
        File destFile = new File(destFileName);
        if (!destFile.exists()) {
            File tmpDir = destFile.getParentFile();
            if (!tmpDir.exists()) {
                tmpDir.mkdirs();
            }

            destFile.createNewFile();
        }

        FileOutputStream out = new FileOutputStream(destFile);
        try {
            ZipFile zipFile = new ZipFile(zipFileName);
            ZipEntry zipEntry = zipFile.getEntry(srcFileName);
            if (zipEntry != null) {
                InputStream is = zipFile.getInputStream(zipEntry);
                int len = -1;
                byte[] buffer = new byte[512];
                while ((len = is.read(buffer, 0, 512)) != -1) {
                    out.write(buffer, 0, len);
                    out.flush();
                }

                is.close();
            }
            zipFile.close();
        } catch (IOException e) {
            // e.printStackTrace();
            throw e;
        } finally {
            out.close();
        }
    }

    /**
     * @param zipFileString zipFileString.
     * @param outPathString outPathString.
     * @throws IOException exception
     */
    public static void unZipFolder(String zipFileString, String outPathString)
            throws IOException {
        // MyLogger.logI(CLASS_TAG, "UnZipFolder(String, String)");
        ZipInputStream inZip = new ZipInputStream(new FileInputStream(zipFileString));
        ZipEntry zipEntry;
        String szName = "";

        while ((zipEntry = inZip.getNextEntry()) != null) {
            szName = zipEntry.getName();

            if (zipEntry.isDirectory()) {

                // get the folder name of the widget
                szName = szName.substring(0, szName.length() - 1);
                File folder = new File(outPathString + File.separator + szName);
                folder.mkdirs();

            } else {

                File file = new File(outPathString + File.separator + szName);
                file.createNewFile();
                // get the output stream of the file
                FileOutputStream out = new FileOutputStream(file);
                int len;
                byte[] buffer = new byte[512];
                // read (len) bytes into buffer
                while ((len = inZip.read(buffer)) != -1) {
                    // write (len) byte from buffer at the position 0
                    out.write(buffer, 0, len);
                    out.flush();
                }
                out.close();
            }
        } // end of while

        inZip.close();

    } // end of func

    /**
     * @param srcFileString
     * @param zipFileString
     * @throws IOException.
     */
    public static void zipFolder(String srcFileString, String zipFileString) throws IOException {
        MyLogger.logI(CLASS_TAG, "zipFolder(String, String)");

        ZipOutputStream outZip = new ZipOutputStream(new FileOutputStream(zipFileString));

        File file = new File(srcFileString);

        zipFiles(file.getParent() + File.separator, file.getName(), outZip);

        outZip.finish();
        outZip.close();

    } // end of func

    private static void zipFiles(String folderString, String fileString,
            ZipOutputStream zipOutputSteam) throws IOException {
        MyLogger.logI(CLASS_TAG, "zipFiles(String, String, ZipOutputStream)");

        if (zipOutputSteam == null) {
            return;
        }

        File file = new File(folderString + fileString);

        if (file.isFile()) {

            ZipEntry zipEntry = new ZipEntry(fileString);
            FileInputStream inputStream = new FileInputStream(file);
            zipOutputSteam.putNextEntry(zipEntry);

            int len;
            byte[] buffer = new byte[1024];

            while ((len = inputStream.read(buffer)) != -1) {
                zipOutputSteam.write(buffer, 0, len);
            }

            zipOutputSteam.closeEntry();
            inputStream.close();
        } else {

            String fileList[] = file.list();

            if (fileList.length <= 0) {
                ZipEntry zipEntry = new ZipEntry(fileString + File.separator);
                zipOutputSteam.putNextEntry(zipEntry);
                zipOutputSteam.closeEntry();
            }

            for (int i = 0; i < fileList.length; i++) {
                zipFiles(folderString, fileString + File.separator + fileList[i], zipOutputSteam);
            } // end of for

        } // end of if

    } // end of func

    /**
     * @param srcFileString
     * @param zipFileString
     * @throws IOException.
     */
    public static void zipOneFile(String srcFileString, String zipFileString) throws IOException {
        MyLogger.logI(CLASS_TAG, "zipFolder(String, String)");

        ZipOutputStream outZip = new ZipOutputStream(new FileOutputStream(zipFileString));

        File file = new File(srcFileString);

        zipFiles(file.getParent() + File.separator, file.getName(), outZip);

        outZip.finish();
        outZip.close();
    }

    ZipOutputStream mOutZip;

    /**
     * @param zipFileString
     * @throws IOException.
     */
    public void createZipFile(String zipFileString) throws IOException {
        mOutZip = new ZipOutputStream(new FileOutputStream(zipFileString));
    }

    /**
     * @param fileName
     * @param fileContent
     * @throws IOException.
     */
    public void addFile(String fileName, String fileContent) throws IOException {
        if (fileContent != null && fileContent.length() > 0) {
            addFile(fileName, fileContent.getBytes());
        }
    }

    /**
     * @param fileName
     * @param fileContent
     * @throws IOException.
     */
    public void addFile(String fileName, byte[] fileContent) throws IOException {
        if (fileContent != null && fileContent.length > 0) {
            ZipEntry zipEntry = new ZipEntry(fileName);
            mOutZip.putNextEntry(zipEntry);
            mOutZip.write(fileContent, 0, fileContent.length);
            mOutZip.closeEntry();
        }
    }

    /**
     *
     * @param srcFileName
     * @param desFileName
     * @throws IOException.
     */
    public void addFileByFileName(String srcFileName, String desFileName) throws IOException {
        MyLogger.logD("BACKUP", "addFileByFileName:" + "srcFile:" + srcFileName + ",desFile:"
                + desFileName);

        ZipEntry zipEntry = new ZipEntry(desFileName);
        File file = new File(srcFileName);
        FileInputStream inputStream = new FileInputStream(file);
        mOutZip.putNextEntry(zipEntry);

        byte[] buffer = new byte[1024];
        int len = 0;
        while ((len = inputStream.read(buffer)) != -1) {
            mOutZip.write(buffer, 0, len);
        }

        inputStream.close();
    }

    /**
     * @param srcPath
     * @param desPath
     * @throws IOException.
     */
    public void addFolder(String srcPath, String desPath) throws IOException {
        File dir = new File(srcPath);

        // Log.d("BACKUP", "addFolder," +
        // "srcFile:" + srcPath +
        // ",desFile:" + desPath +
        // ",dir.isDirectory():" + dir.isDirectory());

        if (dir.isDirectory()) {
            File[] fileArray = dir.listFiles();
            if (fileArray != null) {
                for (File file : fileArray) {
                    addFolder(srcPath + File.separator + file.getName(), desPath + File.separator
                            + file.getName());
                }
            } else {
                // Log.d("BACKUP", "addFolder, empty folder:" + srcPath);
            }

        } else {
            try {
                addFileByFileName(srcPath, desPath);
            } catch (IOException e) {
                MyLogger.logD("BACKUP", "IOException");
            }
        }
    }

    /**
     * @throws IOException.
     */
    public void finish() throws IOException {
        mOutZip.finish();
        mOutZip.close();
    }
}