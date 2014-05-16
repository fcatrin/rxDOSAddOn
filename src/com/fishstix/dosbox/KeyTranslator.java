package com.fishstix.dosbox;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import android.annotation.SuppressLint;
import android.util.Log;
import android.view.KeyEvent;

public class KeyTranslator {
	private static final String LOGTAG = KeyTranslator.class.getSimpleName();
	private static final String PREFIX = "KEYCODE_";
	static Map<String, Integer> keys = new HashMap<String, Integer>();
	
	
	@SuppressLint("InlinedApi")
	public static void init() {
		Field[] fields = KeyEvent.class.getDeclaredFields();
		for(Field field : fields) {
			String name = field.getName(); 
			if (name.startsWith(PREFIX)) {
				name = name.substring(PREFIX.length());
				try {
					int value = field.getInt(null);
					keys.put("KEY_" + name, value);
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
		
		keys.put("KEY_LEFTSHIFT", KeyEvent.KEYCODE_SHIFT_LEFT);
		keys.put("KEY_RIGHTSHIFT", KeyEvent.KEYCODE_SHIFT_RIGHT);
		keys.put("KEY_LEFTALT", KeyEvent.KEYCODE_ALT_LEFT);
		keys.put("KEY_RIGHTALT", KeyEvent.KEYCODE_ALT_RIGHT);
		keys.put("KEY_LEFTCTRL", KeyEvent.KEYCODE_CTRL_LEFT);
		keys.put("KEY_RIGHTCTRL", KeyEvent.KEYCODE_CTRL_RIGHT);
		keys.put("KEY_UP", KeyEvent.KEYCODE_DPAD_UP);
		keys.put("KEY_DOWN", KeyEvent.KEYCODE_DPAD_DOWN);
		keys.put("KEY_LEFT", KeyEvent.KEYCODE_DPAD_LEFT);
		keys.put("KEY_RIGHT", KeyEvent.KEYCODE_DPAD_RIGHT);

		keys.put("KEY_PAGEDOWN", KeyEvent.KEYCODE_PAGE_DOWN);
		keys.put("KEY_PAGEUP", KeyEvent.KEYCODE_PAGE_UP);
		keys.put("KEY_KPPLUS", KeyEvent.KEYCODE_PLUS);

		keys.put("KEY_BACKSPACE", KeyEvent.KEYCODE_BACK);
		keys.put("KEY_DOT", KeyEvent.KEYCODE_PERIOD);
		keys.put("KEY_ESC", KeyEvent.KEYCODE_ESCAPE);
		
		keys.put("KEY_MOUSE_TOGGLE", KeyEvent.KEYCODE_BUTTON_MODE);
		
		for(int i=0; i<10; i++)	keys.put("KEY_KP" + i, KeyEvent.KEYCODE_NUMPAD_0 + i);

	}
	
	public static VirtualKey translate(String name) {
		if (name == null || name.equals("NONE")) return null;
		
		VirtualKey vk = new VirtualKey();
		
		String parts[] = name.split("\\+");
		for (String part: parts) {
			if (parts.length>1) Log.d(LOGTAG, "Processing part [" + part + "]");
			if (part.equals("CTRL"))  vk.ctrl = true;
			if (part.equals("ALT"))   vk.alt = true;
			if (part.equals("SHIFT")) vk.shift = true;
			Integer keyCode = keys.get(part);
			if (keyCode != null) vk.keyCode = keyCode;
		}
		if (vk.keyCode!=0) return vk;
		
		Log.d(LOGTAG, "Can't translate key " + name);
		return null;
	}
}
