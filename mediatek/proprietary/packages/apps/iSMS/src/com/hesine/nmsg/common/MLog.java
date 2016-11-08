package com.hesine.nmsg.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Date;

import android.os.Environment;
import android.util.Log;

public final class MLog {
    public static final int LOG_DEBUG = 0;
    public static final int LOG_INFO = 1;
    public static final int LOG_ERROR = 2;
    public static final int LOG_NONE = 3;
    private static int logLevel = LOG_ERROR;
    public static File file = null;
    public static RandomAccessFile fWriter = null;
    public static final String LOG_PATH = File.separator + EnumConstants.ROOT_DIR + "/Log/";

    private static final int MAX_LOG_SIZE = 10 * 1024 * 1024;

    public static synchronized void init() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            try {
                String newLog = FileEx.getSDCardPath() + LOG_PATH + "nmsg.log";
                String bakLog = FileEx.getSDCardPath() + LOG_PATH + "nmsg-bak.log";
                file = new File(newLog);
                if (!file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }

                boolean isFileExist = file.exists();
                if (isFileExist && file.length() >= MAX_LOG_SIZE) {
                    File fileBak = new File(bakLog);
                    if (fileBak.exists()) {
                        fileBak.delete();
                    }
                    file.renameTo(fileBak);
                    file = null;
                    file = new File(newLog);
                    isFileExist = false;
                }

                if (fWriter != null) {
                    fWriter.close();
                }

                fWriter = new RandomAccessFile(file, "rws");
                if (isFileExist) {
                    fWriter.seek(file.length());
                }

                error("java file logger is inited");
            } catch (IOException e) {
                MLog.error(e.toString());
            }
        }
    }

    public static void destroy() {
        if (fWriter != null) {
            try {
                fWriter.close();
            } catch (IOException e) {
                MLog.error(e.toString());
            } finally {
                fWriter = null;
            }
        }
        file = null;
    }

    public static String getStactTrace(Exception e) {
        if (null == e) {
            return null;
        }
        StringBuffer ret = new StringBuffer(e.toString());
        StackTraceElement[] stack = e.getStackTrace();
        for (int i = 0; stack != null && i < stack.length; ++i) {
            ret = ret.append("\n" + stack[i].toString());
        }
        return ret.toString();
    }
//
//    public static void printStackTrace(Exception e) {
//        if (null == e && (logLevel > LOG_ERROR)) {
//            return;
//        }
//
//        error("global Exception: " + e.toString());
//        StackTraceElement[] stack = e.getStackTrace();
//        for (int i = 0; stack != null && i < stack.length; ++i) {
//            error("global "+stack[i].toString());
//        }
//    }

    public static void error( String msg) {
        if (logLevel <= LOG_ERROR) {
            StackTraceElement element =Thread.currentThread().getStackTrace()[3];
            if(element != null){
                Log.e(element.getFileName()," ERROR " + "Line: " + element.getLineNumber() + "  " + msg);
                appendLog(file," ERROR "+  element.getFileName() + "\t" + "Line: " 
                        +element.getLineNumber() + "  "
                        + msg); 
            }else{
                Log.e(null," ERROR " + "Line: " + 0 + "  " + msg);
                appendLog(file," ERROR "+  null + "\t" + "Line: " 
                        +0 + "  "
                        + msg);  
            }
        }
    }

    public static void info( String msg) {
        if (logLevel <= LOG_INFO) {
            StackTraceElement element =Thread.currentThread().getStackTrace()[3];
            if(element != null){
                Log.i(element.getFileName(), " INFO " +"Line:" + element.getLineNumber() + "  " + msg);
                appendLog(file," INFO " + element.getFileName() + "\t" + "Line: " 
                         + element.getLineNumber() + "  "
                        + msg);
            }else{
                Log.i(null, " INFO " +"Line:" + 0 + "  " + msg);
                appendLog(file," INFO " + null + "\t" + "Line: " 
                         + 0 + "  "
                        + msg);  
            }
          
        }
    }

    public static void debug( String msg) {
        if (logLevel <= LOG_DEBUG) {
            StackTraceElement element =Thread.currentThread().getStackTrace()[3];
            if(element != null){
                Log.w(element.getFileName(), " DEBUG "+"Line:" + element.getLineNumber() + "  " + msg);
                appendLog(file, " DEBUG " +element.getFileName() + "\t" + "Line " 
                        + element.getLineNumber() + "  "
                        + msg);
            }else{
                Log.w(null, " DEBUG "+"Line:" + 0 + "  " + msg);
                appendLog(file, " DEBUG " +null + "\t" + "Line " 
                        + 0 + "  "
                        + msg);
            }
        }
    }

    public static void appendLog(File file, String content) {
        try {
            if (file == null || !file.exists()) {
                init();
                return;
            }
            StringBuffer sb = new StringBuffer();
            sb.append(EnumConstants.SDF2.format(new Date()));
            sb.append("\t ");
            sb.append("\t");
            sb.append(content);
            sb.append("\r\n");
            fWriter.write(sb.toString().getBytes());
        } catch (IOException  e) {
            Log.e("global","global log output exception,maybe the log file is not exists," + e.toString());
        } finally {

            if (file != null && file.length() >= MAX_LOG_SIZE) {
                init();
                return;
            }
        }
    }

    public static void setLogPriority(int priority) {
        logLevel = priority;
    }

    public static int getLogPriority() {
        return logLevel;
    }

    public static void readIniFile() {
        String filePath = FileEx.getSDCardPath() + File.separator + "nmsg.ini";
        File file = new File(filePath);
        String logLevelString = null;
        if (!file.exists()) {
            return;
        }
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String tempString = null;
            while ((tempString = reader.readLine()) != null) {
                if (tempString.indexOf("LOG_LEVEL") != -1) {
                    String[] str = tempString.split("=");
                    logLevelString = str[1];
                    logLevelString = logLevelString.replaceAll("\\s", "");
                    logLevelString = logLevelString.replaceAll(";", "");
                    int logLevel = Integer.parseInt(logLevelString);
                    MLog.error("log was changed before loglevel is:" + getLogPriority());
                    setLogPriority(logLevel);
                    MLog.error("log was changed current loglevel is:" + logLevel);
                    break;
                }
            }
            if (null != reader) {
                reader.close();
            }
        } catch (NumberFormatException  e) {
            MLog.error(e.toString());
        } catch (IOException e) {          
            MLog.error(e.toString());
        }
    }
}
