package retrobox.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import xtvapps.core.Utils;
import android.annotation.SuppressLint;
import android.os.Environment;
import android.util.Log;

public class DriveUtils {
	private static final String LOGTAG = DriveUtils.class.getSimpleName();

	private static final String validFileSystems[] = {"vfat", "sdcardfs", "fuse", "fuseblk", "ntfs", "esdfs", "smb", "cifs", "tntfs"};
	
	private static List<MountPoint> cachedMounts = null;
	private static long cacheExpiration = 0;
	private static long CACHE_TTL = 30 * 1000;
	
	private static boolean isValidFileSystem(String fileSystem) {
		for(String validFileSystem : validFileSystems) {
			if (validFileSystem.equals(fileSystem)) return true;
		}
		return false;
	}

	@SuppressLint("SdCardPath")
	public static List<MountPoint> findMounts() {
		long now = System.currentTimeMillis();
		if (cachedMounts != null && cacheExpiration > now) {
			// make this result valid for the next 30 seconds
			cacheExpiration = now + CACHE_TTL;
			return cachedMounts;
		}
		
		Set<String> paths = new HashSet<String>();
		Set<String> signatures = new HashSet<String>();
		List<MountPoint> mounts = new ArrayList<MountPoint>();
		
		Set<MountPoint> extraMountPoints = new LinkedHashSet<MountPoint>();
		extraMountPoints.add(new MountPoint("/sdcard"));
		extraMountPoints.add(new MountPoint(Environment.getExternalStorageDirectory().getPath()));
		
		extraMountPoints.add(new MountPoint("/usbdisk"));
		extraMountPoints.add(new MountPoint("/usbdisk1"));
		extraMountPoints.add(new MountPoint("/usbdisk2"));
		extraMountPoints.add(new MountPoint("/usbdisk3"));
		
		List<MountPoint> rootMountPoints = listRoots();
		if (rootMountPoints!=null) {
			for (MountPoint rootMountPoint : rootMountPoints) {
				// add storage mount points first
				if (rootMountPoint.isValid() && rootMountPoint.getDir().getAbsolutePath().startsWith("/storage/")) extraMountPoints.add(rootMountPoint);
			}
			for (MountPoint rootMountPoint : rootMountPoints) {
				if (rootMountPoint.isValid() && !rootMountPoint.getDir().getAbsolutePath().equals("/")) extraMountPoints.add(rootMountPoint);
			}
			for (MountPoint rootMountPoint : rootMountPoints) {
				if (!rootMountPoint.isValid()) extraMountPoints.add(rootMountPoint);
			}
		}
		
		for(MountPoint extraMountPoint : extraMountPoints) {
			if (!extraMountPoint.isValid()) {
				mounts.add(extraMountPoint);
				continue;
			}
			String unreadableReason = getUnreadableReason(extraMountPoint.getDir());
			if (unreadableReason!=null) {
				MountPoint mountPoint = new MountPoint();
				String name = extraMountPoint.getDir()!=null?extraMountPoint.getDir().getAbsolutePath():null;
				mountPoint.setDescription((name!=null? name : mountPoint.getDescription()) + " " + unreadableReason);
				mounts.add(mountPoint);
				Log.d(LOGTAG, mountPoint.getDescription());
				continue;
			}
			String signature = createSignature(extraMountPoint.getDir());
			if (paths.contains(extraMountPoint.getDir().getAbsolutePath()) || signatures.contains(signature)) {
				MountPoint mountPoint = new MountPoint();
				mountPoint.setDescription("Already added" + extraMountPoint.getDir().getAbsolutePath());
				mounts.add(mountPoint);
				Log.d(LOGTAG, mountPoint.getDescription());
			} else {
				signatures.add(signature);
				MountPoint localMountPoint = new MountPoint(extraMountPoint.getDir());
				if (extraMountPoint.getFilesystem().equals("unknown")) {
					localMountPoint.setFilesystem(extraMountPoint.getDir().getAbsolutePath().equals("/sdcard")?"Legacy sdcard":"Storage");
				} else {
					localMountPoint.setFilesystem(extraMountPoint.getFilesystem());
				}
				mounts.add(localMountPoint);
			}
		}
		
		cachedMounts    = mounts;
		cacheExpiration = now + CACHE_TTL;
		return mounts;
	}
	
	private static String getUnreadableReason(File dir) {
		if (!dir.exists()) return "doesn't exist";
		if (!dir.isDirectory()) return "not a directory";
		if (dir.canRead() || dir.canExecute()) return null;
		
		File[] listFiles = dir.listFiles();
		if (listFiles == null) return "cannot list files";
		if (listFiles.length == 0) return "no files found";
		return null;
	}

	private static List<MountPoint> listRoots() {
		List<MountPoint> mounts = new ArrayList<MountPoint>();

		String mountFile;
		try {
			mountFile = Utils.loadString(new File("/proc/mounts"));
		} catch (IOException e) {
			MountPoint mountPoint = new MountPoint();
			mountPoint.setDescription("/proc/mount unavailable");
			mounts.add(mountPoint);
			return mounts;
		}
		String lines[] = mountFile.split("\n");
		for(String line : lines) {
			String parts[] = line.split(" ");
			Log.d(LOGTAG, "Mount line parts " + Arrays.toString(parts));
			if (parts.length>=3 && isValidFileSystem(parts[2]) && isValidPath(parts[1])) {
				String path = parts[1];
				Log.d(LOGTAG, "Adding mount root " + path);
				MountPoint localMountPoint = new MountPoint(path);
				localMountPoint.setFilesystem(parts[2]);
				mounts.add(localMountPoint);
			} else {
				String path = parts.length>=2?parts[1]:"";
				if (!path.startsWith("/dev") && !path.startsWith("/sys") && !path.startsWith("/proc")) {
					MountPoint mountPoint = new MountPoint();
					mountPoint.setDescription("skip " + line);
					mounts.add(mountPoint);
				}
			}
		}
		return mounts;
	}
	
	private static boolean isValidPath(String path) {
		return !path.startsWith("/mnt/secure") 
				&& !path.contains("/obb") 
				&& !path.contains("/firmware");
	}
	
	private static String createSignature(File dir) {
		List<String> fileNames = new ArrayList<String>();
		File files[] = dir.listFiles();
		if (files!=null) {
			for(File file : files) fileNames.add(file.getName());
		}

		Collections.sort(fileNames);
		StringBuffer fileNamesText = new StringBuffer();
		for(String fileName : fileNames) {
			fileNamesText.append(fileName);
		}

		Log.d(LOGTAG, "signature for root " + dir.getAbsolutePath() + " input " + fileNamesText.toString());
		String signature = Utils.md5(fileNamesText.toString()) + "." + dir.getTotalSpace();
		Log.d(LOGTAG, "signature for root " + dir.getAbsolutePath() + " = " + signature);
		return signature;
	}
	
	public static Map<File, MountPoint> getMountsMap() {
		List<MountPoint> mountList = findMounts();
		Map<File, MountPoint> mounts = new LinkedHashMap<File, MountPoint>();
		for(MountPoint mount : mountList) {
			mounts.put(mount.getDir(), mount);
		}
		return mounts;
	}
}
