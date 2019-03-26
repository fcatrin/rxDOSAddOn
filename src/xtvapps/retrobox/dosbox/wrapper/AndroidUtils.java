package xtvapps.retrobox.dosbox.wrapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.content.res.AssetManager;

public class AndroidUtils {
	private static final int BUF_SIZE = 256 * 1024;

	public static void unpackAssets(Context ctx, String dir, File dstDir) throws IOException {
		File unpackDir = new File (dstDir, dir);
		if (unpackDir.exists()) delTree(unpackDir);
		unpackDir.mkdirs();

		AssetManager assets = ctx.getAssets();
		String[] files = assets.list(dir);
		for(String file : files) {
			String fileName = dir + "/" + file;
			try {
				InputStream is = assets.open(fileName);
				File dstFile = new File(dstDir, fileName);
				copyFile(is, new FileOutputStream(dstFile));
			} catch (FileNotFoundException e) {
				// this is a folder
				unpackAssets(ctx, fileName, dstDir);
			}
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
		int customBufferSize = 0;
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
	
	public abstract class ProgressListener {
		public abstract boolean update(int progress, int max);
		public void onStart() {}
		public void onEnd() {}
	}
}
