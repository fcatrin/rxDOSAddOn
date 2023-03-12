package retrobox.content;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import xtvapps.core.Utils;
import android.graphics.Bitmap;

public class SaveStateInfo {
	File file;
	File shotFile;
	long ts;
	int order;
	private String infoText;
	private String slotInfo;
	private Bitmap bitmap;
	private boolean isSelected;
	private int imageResourceId;
	
	public SaveStateInfo(File file) {
		this(file, new File(file.getAbsolutePath() + ".png"), 0);
	}

	public SaveStateInfo(File file, int imageResourceId) {
		this(file, null, imageResourceId);
	}
	
	public SaveStateInfo(File file, File shotFile) {
		this(file, shotFile, 0);
	}

	private SaveStateInfo(File file, File shotFile, int imageResourceId) {
		this.file = file;
		this.shotFile = shotFile;
		this.imageResourceId = imageResourceId;
		
		File tsFile = new File(file.getAbsolutePath() + ",ts");
		if (tsFile.exists()) {
			ts = ts2time(tsFile);
		}
		
		if (ts == 0) {
			ts = file.lastModified();
		}
	}
	
	private long ts2time(File tsFile) {
		String sts;
		try {
			sts = Utils.loadString(tsFile);
		} catch (IOException e) {
			e.printStackTrace();
			return 0;
		}
		
		if (sts == null || sts.length() != 14) {
			return 0;
		}
		
		int year  = Utils.str2i(sts.substring(0,  3));
		int month = Utils.str2i(sts.substring(4,  5));
		int day   = Utils.str2i(sts.substring(6,  7));
		int hour  = Utils.str2i(sts.substring(8,  9));
		int min   = Utils.str2i(sts.substring(10, 11));
		int sec   = Utils.str2i(sts.substring(12, 13));
		
		Date date = new Date(year-1900, month-1, day, hour, min, sec);
		return date.getTime();
	}
	
	public int getImageResourceId() {
		return imageResourceId;
	}
	
	public boolean exists() {
		return file.exists();
	}
	
	public long getTimestamp() {
		return ts;
	}
	
	public File getScreenshot() {
		return shotFile;
	}

	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	public void setInfo(String infoText) {
		this.infoText = infoText;
	}
	
	public String getInfo() {
		return infoText;
	}
	
	public String getSlotInfo() {
		return slotInfo;
	}

	public void setSlotInfo(String slotInfo) {
		this.slotInfo = slotInfo;
	}

	public void setBitmap(Bitmap bitmap) {
		this.bitmap = bitmap;
	}
	
	public Bitmap getBitmap() {
		return bitmap;
	}

	public boolean isSelected() {
		return isSelected;
	}

	public void setSelected(boolean isSelected) {
		this.isSelected = isSelected;
	}
	
	
}
