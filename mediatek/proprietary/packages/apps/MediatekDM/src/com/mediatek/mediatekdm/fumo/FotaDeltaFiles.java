package com.mediatek.mediatekdm.fumo;

import android.os.Build;
import android.os.Environment;
import android.os.RecoverySystem;
import android.os.StatFs;
import android.util.Log;

import com.mediatek.mediatekdm.DmApplication;
import com.mediatek.mediatekdm.DmConst.TAG;
import com.mediatek.mediatekdm.PlatformManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Enumeration;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class FotaDeltaFiles {
    public static final int DELTA_VERIFY_OK = 0;
    public static final int DELTA_NO_STORAGE = 1;
    public static final int DELTA_INVALID_ZIP = 2;
    public static final int DELTA_CHECKSUM_ERR = 3;

    private static final String FUMO_PKG_FINGERPRINT_INI = "fumofingerprint.ini";
    private static final String SRC_FINGERPRINT = "src_fp";
    private static final String DST_FINGERPRINT = "des_fp";

    private static final int FP_GROUP_ID = 2;
    private static final String FP_REGEX_PATTERN = "^(assert\\()?"
            + " *file_getprop\\(\"/system/build.prop\", "
            + "\"ro.build.fingerprint\"\\) == \"(.*)\"[ \\|\\||\\);]";
    private static final String FUMO_PKG_SCRIPT_PATH = "META-INF/com/google/android/updater-script";

    private static String getPackageFingerprintPath() {
        return DmApplication.getInstance().getFilesDir() + File.separator
                + FUMO_PKG_FINGERPRINT_INI;
    }

    private static String getDeltaPath() {
        return PlatformManager.getInstance().getPathInData(DmApplication.getInstance(),
                FumoComponent.DELTA_FILE);
    }

    public static int unpackAndVerify(String updateFile) {
        Log.d(TAG.FUMO, "[parsing delta] + unpackAndVerify");

        // 1. check remained data storage
        if (!hasEnoughSpace(updateFile)) {
            Log.e(TAG.FUMO, "[parsing delta] storage not enough");
            return DELTA_NO_STORAGE;
        }

        // 2. check files are correct
        boolean verificationPassed = true;
        File pkg = new File(updateFile);
        try {
            Log.d(TAG.FUMO, "[parsing delta] + begin system verify");
            RecoverySystem.verifyPackage(pkg, null, null);
        } catch (GeneralSecurityException e) {
            Log.e(TAG.FUMO, "System verify: find GeneralSecurityException");
            verificationPassed = false;
        } catch (IOException e) {
            Log.e(TAG.FUMO, "System verify: find IOException");
            verificationPassed = false;
        }

        if (!verificationPassed) {
            // verify failed, Tip to user
            Log.e(TAG.FUMO, "System verify failed, later return");
            delFingerprintFile();
            return DELTA_INVALID_ZIP;
        }
        Log.d(TAG.FUMO, "[parsing delta] - begin system verify");

        // 3. compare src fingerprint with OS's fingerprint
        if (verifyFumoPkgFP()) {
            Log.d(TAG.FUMO, "[parsing delta] verifyFumoPkgFP success");
        } else {
            Log.d(TAG.FUMO, "[parsing delta] verifyFumoPkgFP fail");
            delFingerprintFile();
            return DELTA_CHECKSUM_ERR;
        }

        Log.d(TAG.FUMO, "[parsing delta] - unpackAndVerify");
        return DELTA_VERIFY_OK;
    }

    public static boolean verifyUpdateStatus() {
        boolean result = false;
        String fingerPrint = null;
        try {
            fingerPrint = getDestinationFingerprint();
        } catch (IOException e) {
            Log.d(TAG.FUMO, "[] Exception to get fingerprint of des load ");
            e.printStackTrace();
        }
        if (null != fingerPrint) {
            result = Build.FINGERPRINT.equals(fingerPrint);
            Log.d(TAG.FUMO, "[] Compare fingerprint with des load ");
        }
        return result;
    }

    public static String getSourceFingerprint() throws IOException {
        FileInputStream fis = null;
        Properties paramTable = null;
        String source = null;

        try {
            fis = new FileInputStream(getPackageFingerprintPath());
            paramTable = new Properties();

            paramTable.load(fis);

            if (paramTable.containsKey(SRC_FINGERPRINT)) {
                source = paramTable.getProperty(SRC_FINGERPRINT);
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG.FUMO, "getSourceFingerprint fail.");
        } finally {
            try {
                if (null != fis) {
                    fis.close();
                    fis = null;
                }
            } catch (IOException e) {
                Log.e(TAG.FUMO, e.getMessage());
            }
        }
        return source;
    }

    public static String getDestinationFingerprint() throws IOException {
        FileInputStream fis = null;
        Properties paramTable = null;
        String destination = null;

        try {
            fis = new FileInputStream(getPackageFingerprintPath());
            paramTable = new Properties();

            paramTable.load(fis);

            if (paramTable.containsKey(DST_FINGERPRINT)) {
                destination = paramTable.getProperty(DST_FINGERPRINT);
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG.FUMO, "getDestinationFingerprint fail.");
        } finally {
            try {
                if (null != fis) {
                    fis.close();
                    fis = null;
                }
            } catch (IOException e) {
                Log.e(TAG.FUMO, e.getMessage());
            }
        }
        return destination;
    }

    private static void saveFPPairToFile(FPPair fpPair) throws IOException {
        if ((null == fpPair) || (null == fpPair.srcFP) || (null == fpPair.dstFP)) {
            return;
        }

        FileOutputStream fos = null;
        Properties paramTable = null;

        try {
            File file = new File(getPackageFingerprintPath());

            if (!file.exists()) {
                file.createNewFile();
            }

            fos = new FileOutputStream(getPackageFingerprintPath());
            paramTable = new Properties();

            paramTable.clear();
            paramTable.setProperty(SRC_FINGERPRINT, fpPair.srcFP);
            paramTable.setProperty(DST_FINGERPRINT, fpPair.dstFP);

            paramTable.store(fos, "SRC AND DST FINGERPRINT");
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG.FUMO, "saveFPPairToFile fail.");
        } finally {
            try {
                if (null != fos) {
                    fos.close();
                    fos = null;
                }
            } catch (IOException e) {
                Log.e(TAG.FUMO, e.getMessage());
            }
        }
    }

    public static void delFingerprintFile() {
        File file = new File(getPackageFingerprintPath());

        if (file.exists()) {
            Log.d(TAG.FUMO, "Delete existed FingerprintFile ");
            if (!file.delete()) {
                Log.e(TAG.FUMO, "Fail to delete FingerprintFile");
            }
        }
    }

    private static boolean verifyFumoPkgFP() {
        FPPair fingeprints = null;
        boolean matched = false;

        try {
            fingeprints = fetchFPPair(getDeltaPath());
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG.FUMO, "get finger prints failed");
            return matched;
        }

        if (null != fingeprints) {
            matched = Build.FINGERPRINT.equals(fingeprints.srcFP);
            Log.d(TAG.FUMO, "fingeprints of OS is " + Build.FINGERPRINT);
            Log.d(TAG.FUMO, "fingeprints not null, match is " + matched);

            if (matched) {
                try {
                    Log.d(TAG.FUMO, "save the fingeprints to file");
                    saveFPPairToFile(fingeprints);
                } catch (IOException e) {
                    e.printStackTrace();
                    matched = false;
                    Log.d(TAG.FUMO, "Fail to save the fingeprints to file");
                }
            }
        }

        return matched;
    }

    public static class FPPair {
        public String srcFP;
        public String dstFP;

        public FPPair() {
            srcFP = null;
            srcFP = null;
        }
    }

    public static FPPair fetchFPPair(String packageName) throws IOException {
        ZipFile zip = new ZipFile(packageName);
        InputStream is = null;
        InputStreamReader isr = null;
        BufferedReader br = null;
        FPPair fingerprint = null;
        try {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            ZipEntry entry;
            while (entries.hasMoreElements()) {
                entry = entries.nextElement();

                if (FUMO_PKG_SCRIPT_PATH.equals(entry.getName())) {
                    is = zip.getInputStream(entry);
                    break;
                }
            }

            if (null != is) {
                isr = new InputStreamReader(is);
                if (null != isr) {
                    br = new BufferedReader(isr);
                    fingerprint = new FPPair();
                    String srcLine = null;
                    String dstLine = null;

                    br.readLine();
                    srcLine = br.readLine();
                    dstLine = br.readLine();

                    fingerprint.srcFP = parseFP(srcLine);
                    Log.d(TAG.FUMO, "SRC fingerprint is " + fingerprint.srcFP);
                    fingerprint.dstFP = parseFP(dstLine);
                    Log.d(TAG.FUMO, "DES fingerprint is " + fingerprint.dstFP);

                    br.close();
                    isr.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG.FUMO, "fetchFPPair fail");
        } finally {
            if (null != br) {
                br.close();
            }
            if (null != isr) {
                isr.close();
            }
            if (null != is) {
                is.close();
            }
            if (null != zip) {
                zip.close();
            }
        }
        return fingerprint;
    }

    private static String parseFP(String line) {
        Pattern pattern = Pattern.compile(FP_REGEX_PATTERN);
        Matcher matcher = pattern.matcher(line);
        String fingerprint = null;

        if (matcher.find()) {
            fingerprint = matcher.group(FP_GROUP_ID);
        }
        return fingerprint;
    }

    private static boolean hasEnoughSpace(String zipFile) {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        final long freeSpace = stat.getBlockSize() * stat.getAvailableBlocks();

        File file = new File(zipFile);
        final long fileSize = file.length();
        final long rawFileSize = (long) (fileSize * 1.5);

        return freeSpace > rawFileSize;
    }
}
