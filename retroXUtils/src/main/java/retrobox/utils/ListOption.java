package retrobox.utils;

import xtvapps.core.content.KeyValue;

public class ListOption extends KeyValue {
	String preset = null;
	int iconResourceId = 0;
	
	public ListOption(String key, String value) {
		this(key, value, null, 0);
	}
	
	public ListOption(String key, String value, String preset) {
		this(key, value, preset, 0);
	}

	public ListOption(String key, String value, int iconResourceId) {
		this(key, value, null, iconResourceId);
	}
	
	public ListOption(String key, String value, String preset, int iconResourceId) {
		super(key, value);
		this.preset = preset;
		this.iconResourceId = iconResourceId;
	}

	public String getPreset() {
		return preset;
	}

	public int getIconResourceId() {
		return iconResourceId;
	}
	
	public boolean hasIcon() {
		return iconResourceId!=0;
	}
}
