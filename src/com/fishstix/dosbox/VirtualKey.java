package com.fishstix.dosbox;

import android.util.Log;

public class VirtualKey {
	enum MouseButton {LEFT, RIGHT};
	
	private static final String LOGTAG = VirtualKey.class.getSimpleName();
	public int keyCode;
	public boolean alt;
	public boolean ctrl;
	public boolean shift;
	public MouseButton mouseButton;
	
	public VirtualKey(int keyCode, boolean alt, boolean ctrl, boolean shift) {
		this.keyCode = keyCode;
		this.alt = alt;
		this.ctrl = ctrl;
		this.shift = shift;
	}
	
	public VirtualKey(int keyCode) {
		this(keyCode, false, false, false);
	}
	
	public VirtualKey(MouseButton mouseButton) {
		this.mouseButton = mouseButton;
	}
	
	public VirtualKey() {
		this(0);
	}
	
	public void sendToDosBox(boolean down) {
		if (mouseButton==null) {
			DosBoxControl.nativeKey(keyCode, down?1:0, ctrl?1:0, alt?1:0, shift?1:0);
		}
		Log.d(LOGTAG, "Send " + this);
	}

	public void sendToDosBox(int x, int y, int action) {
		if (mouseButton!=null) {
			DosBoxControl.nativeMouse(x, y, x, y, action, mouseButton.ordinal());
		}
		Log.d(LOGTAG, "Send " + this);
	}
	
	public boolean isMouseButton() {
		return mouseButton!=null;
	}

	@Override
	public String toString() {
		StringBuffer s = new StringBuffer();
		s.append("VK ");
		if (mouseButton!=null) {
			s.append("MOUSE BUTTON ").append(mouseButton.name());
		} else {
			if (ctrl)  s.append("CTRL+");
			if (alt)   s.append("ALT+");
			if (shift) s.append("SHIFT+");
			s.append(keyCode);
		}
		return s.toString();
	}
}
