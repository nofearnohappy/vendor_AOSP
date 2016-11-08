package com.mtk.offlinek;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

public class FileHandler {
	Context mContext;
	static FileHandler thisPtr = null;
	public static FileHandler getInstance(Context context){
		if(thisPtr == null){
			thisPtr = new FileHandler();
			thisPtr.mContext = context;
		}		
		return thisPtr;
	}
	
	public String copyAssets(String src){
		String dest;
		File outFile = new File(mContext.getExternalFilesDir(null), src);
		dest = copyAssets(src, outFile.getAbsolutePath());
		outFile = null;
		return dest;
	}
	
	public String copyAssets(String src, String dest) {
	    AssetManager assetManager = mContext.getAssets();
	    InputStream in = null;
	    OutputStream out = null;
	    File outFile = null;
        try {
          in = assetManager.open(src);
          outFile = new File(dest);
          out = new FileOutputStream(outFile);
          copyFile(in, out);
	      in.close();
	      out.flush();
	      out.close();
	    } catch(IOException e) {
	    	Log.e("tag", "Failed to copy asset file: " + src, e);
	    	return null;
	    } finally{
	    	outFile = null;
	    	out = null;
	    	in = null;
	    }
        return dest;
	    
	}
	private static void copyFile(InputStream in, OutputStream out) throws IOException {
	    byte[] buffer = new byte[1024];
	    int read;
	    while((read = in.read(buffer)) != -1){
	    	out.write(buffer, 0, read);
	    }
	    buffer = null;
	}
	
	public static final int BUFFER_SIZE = 256;
	public static void zip(String[] files, String zipFile) throws IOException {
	    BufferedInputStream origin = null;
	    ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)));
	    byte data[] = null;
	    FileInputStream fi = null;
	    ZipEntry entry = null;
	    try { 
	        data = new byte[BUFFER_SIZE];

	        for (int i = 0; i < files.length; i++) {
	            fi = new FileInputStream(files[i]);    
	            origin = new BufferedInputStream(fi, BUFFER_SIZE);
	            try {
	                entry = new ZipEntry(files[i].substring(files[i].lastIndexOf("/") + 1));
	                out.putNextEntry(entry);
	                int count;
	                while ((count = origin.read(data, 0, BUFFER_SIZE)) != -1) {
	                    out.write(data, 0, count);
	                }
	            }
	            finally {
	            	entry = null;
	                origin.close();
	                origin = null;
	            }
	        }
	    } finally {
	    	fi = null;
	    	data = null; 
	        out.close();
	        out = null;
	    }
	}
	
	public static void setDevNode(String path, String echoStr) {
		File file = new File(path);
		FileWriter fr = null;  
        try {  
        	fr = new FileWriter(file);  
            fr.write(echoStr);   
            fr.close();  
        }  
        catch (IOException e) {  
        	e.printStackTrace();  
        } finally{
        	if(fr != null)
				try {
					fr.close();
				} catch (IOException e) {}
        	fr = null;
        	file = null;
        }
	}
	
	public static String getFileText(String filePath) {
		String lineStr;
		String output = null;
		FileReader fr;
		BufferedReader br = null;
		StringBuffer sb;
		try {
			fr = new FileReader(filePath);
			br = new BufferedReader(fr);
			sb = new StringBuffer();
			while ((lineStr=br.readLine())!=null){
				sb.append(lineStr);
			}
			output = sb.toString();
			if(br != null)
				br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally{
			if(br != null){
				try {
					br.close();
				} catch (IOException e) {}
			}
			sb = null;
			br = null;
			fr = null;
		}
		return output;
	}
	
	public static boolean isKeywordInFile(String filePath, String keyword){
		 String data = getFileText(filePath);
		 if(data!=null){
			 return data.contains(keyword);
		 }
		 return false;
	}
	
	public static boolean deleteFile(String filePath){
		File file = null;
		boolean deleted = false;
		if(filePath != null && new File(filePath).exists()){
			file = new File(filePath);
			deleted = file.delete();
			file = null;
		}
		return deleted;
	}
}
