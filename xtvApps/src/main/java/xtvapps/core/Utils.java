package xtvapps.core;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import android.content.Context;

public final class Utils {
	private static final String LOGTAG = Utils.class.getSimpleName();
	private static final int BUF_SIZE = 65536;
	private static final int HEX_BASE = 16;
	private static final int BYTE_MASK = 0xFF;

	private static final long SIZE_GIGABYTE = 1024*1024*1024;
	private static final long SIZE_MEGABYTE = 1024*1024;
	private static final long SIZE_KILOBYTE = 1024;

	// less ugly, otherwise it would require a change on all apps
	public static Context context;

	private Utils() {}
	
	public static String size2human(long size) {
		if (size > SIZE_GIGABYTE) {
			DecimalFormat df = new DecimalFormat("#.#");
			return df.format((float)size/ SIZE_GIGABYTE) + "G";
		}
		if (size > SIZE_MEGABYTE) {
			DecimalFormat df = new DecimalFormat("#.#");
			return df.format((float)size / SIZE_MEGABYTE) + "M";
		}
		return ((size + SIZE_KILOBYTE/2) / SIZE_KILOBYTE) + "K";
	}
	
	public static String size2humanDetailed(long size) {
		if (size > SIZE_GIGABYTE) {
			DecimalFormat df = new DecimalFormat("#.00");
			return df.format((float)size / SIZE_GIGABYTE) + " GB";
		}
		if (size > SIZE_MEGABYTE) {
			DecimalFormat df = new DecimalFormat("#.00");
			return df.format((float)size / SIZE_MEGABYTE) + " MB";
		}
		return (size / SIZE_KILOBYTE) + " KB";
	}


	/*
*/
	
	public static String loadString(File f) throws IOException  {
		return loadString(f, "UTF-8");
	}
	
	public static List<String> loadLines(File f) throws IOException  {
		return loadLines(f, "UTF-8");
	}
	
	public static String loadString(File f, String encoding) throws IOException {
		return loadString(new FileInputStream(f), encoding);
	}
	
	public static List<String> loadLines(File f, String encoding) throws IOException {
		return loadLines(new FileInputStream(f), encoding);
	}
	
	public static String loadString(InputStream is, String encoding) throws IOException {
		List<String> lines = loadLines(is, encoding);
		StringBuffer s = new StringBuffer();
		for(String line : lines) {
			s.append(line).append("\n");
		}
		return s.toString();
	}
	
	public static List<String> loadLines(InputStream is, String encoding) throws IOException {
		List<String> lines = new ArrayList<String>();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(is, encoding));
			
			String line = null;
			while ((line = reader.readLine())!=null) {
				lines.add(line);
			}
			return lines;
		} finally {
			if (reader!=null) reader.close();
		}
	}
	
	public static byte[] loadBytes(File f) throws IOException {
		FileInputStream is = null;
		try {
			is = new FileInputStream(f);
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			byte[] buf = new byte[BUF_SIZE];
			while (true) {
				int rc = is.read(buf);
				if (rc <= 0)
					break;
				else
					bout.write(buf, 0, rc);
			}
			return bout.toByteArray();
		} finally {
			if (is!=null) is.close();
		}
	}
	
	public static void saveString(File f, String s) throws IOException {
		saveBytes(f, s.getBytes());
	}
	
	public static void saveBytes(File f, byte[] content) throws IOException {
		FileOutputStream os = null;
		ByteArrayInputStream bais = null;
		try {
			os = new FileOutputStream(f);
			bais = new ByteArrayInputStream(content);
			byte[] buf = new byte[BUF_SIZE];
			while (true) {
				int rc = bais.read(buf);
				if (rc <= 0)
					break;
				else
					os.write(buf, 0, rc);
			}
		} finally {
			if (os!=null) os.close();
			if (bais!=null) bais.close();
		}
	}

	public static int str2i(String value) {
		return str2i(value, 0);
	}

	public static int str2i(String value, int defaultValue) {
		try {
			if (value == null) return defaultValue;
			return Integer.parseInt(value.trim());
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	public static long str2l(String value) {
		return str2l(value, 0);
	}
	
	public static long str2l(String value, long defaultValue) {
		try {
			if (value == null) return defaultValue;
			return Long.parseLong(value.trim());
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	
	public static float str2f(String value) {
		return str2f(value, 0);
	}

	public static float str2f(String value, float defaultValue) {
		try {
			if (value == null) return defaultValue;
			return Float.parseFloat(value.trim());
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	public static int strHex2i(String value, int defaultValue) {
		try {
			if (value == null) return defaultValue;
			return (int) Long.parseLong(value.trim(), HEX_BASE);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	public static String md5(String s) {
		try {
			return md5(s.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return "";
		}
	}

	public static String sha1(String s) {
		try {
			return sha1(s.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return "";
		}
	}

	public static String md5(byte[] data) {
		return digest("MD5", data);
	}
	
	public static String sha1(byte[] data) {
		return digest("SHA-1", data);
	}
	
	private static String digest(String method, byte[] data) {
		try {
			MessageDigest digest = java.security.MessageDigest.getInstance(method);
			digest.update(data);
			return hexString(digest.digest());
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return "";
	}
	
	public static String hexString(byte[] data) {
		// Create Hex String
		StringBuffer hexString = new StringBuffer();
		for (int i = 0; i < data.length; i++) {
			String s = Integer.toHexString(BYTE_MASK & data[i]);
			if (s.length()<2) s = "0" + s;
			hexString.append(s);
		}
		return hexString.toString();
	}

	public static boolean isEmptyString(String s) {
		return s == null || s.trim().length() == 0;
	}
	
	public static void copyFile(File src, File dst) throws IOException {
		copyFile(src, dst, null);
	}
	
	public static void copyFile(File src, File dst, ProgressListener progressListener) throws IOException {
		FileInputStream is = new FileInputStream(src);
		FileOutputStream os = new FileOutputStream(dst);
		copyFile(is, os, progressListener, src.length());
	}
	
	public static void copyFile(InputStream is, OutputStream os) throws IOException {
		copyFile(is, os, null, 0);
	}
	
	public static void copyFile(InputStream is, OutputStream os, ProgressListener progressListener, long max) throws IOException {
        byte buffer[] = new byte[BUF_SIZE];
		int bufferLength = 0;

		if (progressListener!=null) progressListener.update(0, (int)max);
		try {
			int pos = 0;
			while ((bufferLength = is.read(buffer)) > 0) {
				os.write(buffer, 0, bufferLength);
				pos += bufferLength;
				if (progressListener!=null) progressListener.update(pos, (int)max);
			}
		} finally {
			is.close();
			os.close();
		}
	}
	
	public static void delTree(File dir) {
		if (!dir.exists() || !dir.isDirectory()) return;
		if (dir.listFiles() != null) {
			for(File f : dir.listFiles()) {
				if (f.isDirectory()) delTree(f);
				else f.delete();
			}
		}
		dir.delete();
	}
	public static boolean unzip(File file, File dstPath) throws IOException {
		return unzip(file, dstPath, null);
	}
	
	public static boolean unzip(File file, File dstPath, ProgressListener listener) throws IOException {
		
		ZipFile zipFile = null;
		long max = 0;
		long pos = 0;
		
		boolean hasSizeInfo = true;
		if (listener!=null) {
			try {
				zipFile = new ZipFile(file);
				Enumeration<? extends ZipEntry> e = zipFile.entries();
		        while (e.hasMoreElements()) {
		        	ZipEntry entry = e.nextElement();
		        	long size = entry.getSize();
		        	max += size>0?size:0;
		        }
	        
		        if (max == 0) {
		        	hasSizeInfo = false;
		        	max = zipFile.size();
		        }
			} finally {
				if (zipFile!=null)	zipFile.close();
				zipFile = null;
			}
	        
			listener.update(0, (int)max);
		}
		
		dstPath.mkdirs();
		
		long maxFileSize = dstPath.getFreeSpace();
		if (max > maxFileSize) throwNotEnoughStorageException(R.string.xtv_not_enough_space_uncompress, maxFileSize, max);
		
		try {
	        zipFile = new ZipFile(file);
			int customBufferSize = 0;
			if (listener!=null) {
				customBufferSize = listener.getBufferSize((int)max);
			}
	        byte buffer[] = new byte[customBufferSize>0?customBufferSize:BUF_SIZE];
	        boolean cancel = false;
			Enumeration<? extends ZipEntry> e = zipFile.entries();
	          while (e.hasMoreElements()) {
	              ZipEntry entry = e.nextElement();
	              if (entry.getName().startsWith("._")) continue;
	              
	              File destinationPath = new File(dstPath, entry.getName());
	              destinationPath.getParentFile().mkdirs();
	              if (entry.isDirectory()) continue;
	              
	              BufferedInputStream bis = new BufferedInputStream(zipFile.getInputStream(entry));
	              int b;
	              FileOutputStream fos = new FileOutputStream(destinationPath);
	              BufferedOutputStream bos = new BufferedOutputStream(fos, buffer.length);
	              bos.flush();
	              while ((b = bis.read(buffer, 0, buffer.length)) != -1) {
	                  bos.write(buffer, 0, b);
	                  
	                  if (hasSizeInfo && listener!=null) {
	                      pos+=b;
	                	  cancel = listener.update((int)pos, (int)max);
	                	  if (cancel) break;
	                  }
	              }
	              bos.close();
	              bis.close();
	              
	              if (listener!=null && !hasSizeInfo) {
	            	  pos++;
	            	  cancel = listener.update((int)pos, (int)max);
	              }
	              if (cancel) return false;
	          }
	          return true;
		} finally {
			if (zipFile!=null) zipFile.close();
		}
	}

	public static void throwNotEnoughStorageException(int textResourceId, long available, long required) throws IOException {
		String msg = context.getString(textResourceId)
				.replace("{available}", Utils.size2humanDetailed(available))
				.replace("{required}",  Utils.size2humanDetailed(required));
		throw new IOException(msg);
	}

}
