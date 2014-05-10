package com.fishstix.dosbox;

import android.util.Log;

public class VirtualKey {
	private static final String LOGTAG = VirtualKey.class.getSimpleName();
	public int keyCode;
	public boolean alt;
	public boolean ctrl;
	public boolean shift;
	
	public VirtualKey(int keyCode, boolean alt, boolean ctrl, boolean shift) {
		this.keyCode = keyCode;
		this.alt = alt;
		this.ctrl = ctrl;
		this.shift = shift;
	}
	
	public VirtualKey(int keyCode) {
		this(keyCode, false, false, false);
	}
	
	public VirtualKey() {
		this(0);
	}
	
	public void sendToDosBox(boolean down) {
		DosBoxControl.nativeKey(keyCode, down?1:0, ctrl?1:0, alt?1:0, shift?1:0);
		Log.d(LOGTAG, "Send " + this);
	}

	@Override
	public String toString() {
		StringBuffer s = new StringBuffer();
		s.append("VK ");
		if (ctrl)  s.append("CTRL+");
		if (alt)   s.append("ALT+");
		if (shift) s.append("SHIFT+");
		s.append(keyCode);
		return s.toString();
	}
}
