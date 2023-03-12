package xtvapps.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

public final class AndroidCoreUtils {
	
	private static final String LOGTAG = AndroidCoreUtils.class.getSimpleName();
	
	private AndroidCoreUtils(){}
	
	public static void openGooglePlay(Context context, String packageName) {
		try {
			Log.d(LOGTAG, "opening market for " + packageName);
		    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName)));
		} catch (android.content.ActivityNotFoundException anfe) {
		    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + packageName)));
		}
	}

	public static void toast(Context context, String message) {
		Toast.makeText(context, message, Toast.LENGTH_LONG).show();
	}
	
	public static void createNoMediaFile(File folder) {
		File nomediaFile = new File(folder, ".nomedia");
		if (!nomediaFile.exists()) {
			try {
				nomediaFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void hideSoftKeyboard(Context context, View view) {
        InputMethodManager imm = (InputMethodManager) context
                .getSystemService(Context.INPUT_METHOD_SERVICE);

        if (imm != null && view!=null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
	}

	public static void unpackAssets(Context ctx, String dir, File dstDir) throws IOException {
		unpackAssets(ctx, dir, dstDir, null);
	}
	
	public static void unpackAssets(Context ctx, String dir, File dstDir, ProgressListener progressListener) throws IOException {
		File unpackDir = new File (dstDir, dir);
		if (unpackDir.exists()) Utils.delTree(unpackDir);
		unpackDir.mkdirs();

		AssetManager assets = ctx.getAssets();
		String[] files = assets.list(dir);
		
		int filesMax = files.length;
		int filesProgress = 0;
		
		for(String file : files) {
			String fileName = dir + "/" + file;
			if (progressListener!=null) progressListener.update(filesProgress++, filesMax);
			try {
				InputStream is = assets.open(fileName);
				File dstFile = new File(dstDir, fileName);
				Utils.copyFile(is, new FileOutputStream(dstFile));
			} catch (FileNotFoundException e) {
				// this is a folder
				unpackAssets(ctx, fileName, dstDir, progressListener);
			}
		}
	}

}
