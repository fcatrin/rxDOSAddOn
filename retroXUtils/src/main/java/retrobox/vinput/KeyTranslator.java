package retrobox.vinput;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import retrobox.vinput.VirtualEvent.MouseButton;
import android.annotation.SuppressLint;
import android.util.Log;
import android.view.KeyEvent;

public class KeyTranslator {
	private static final String LOGTAG = KeyTranslator.class.getSimpleName();
	private static final String PREFIX = "KEYCODE_";
	static Map<String, Integer> keys = new HashMap<String, Integer>();
	private static boolean initialized = false;
	
	@SuppressLint("InlinedApi")
	public static void init() {
		if (initialized) return;
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
		keys.put("KEY_LEFTMETA", KeyEvent.KEYCODE_META_LEFT);
		keys.put("KEY_RIGHTMETA", KeyEvent.KEYCODE_META_RIGHT);

		keys.put("KEY_LEFTBRACKET", KeyEvent.KEYCODE_LEFT_BRACKET);
		keys.put("KEY_RIGHTBRACKET", KeyEvent.KEYCODE_RIGHT_BRACKET);

		
		keys.put("KEY_UP", KeyEvent.KEYCODE_DPAD_UP);
		keys.put("KEY_DOWN", KeyEvent.KEYCODE_DPAD_DOWN);
		keys.put("KEY_LEFT", KeyEvent.KEYCODE_DPAD_LEFT);
		keys.put("KEY_RIGHT", KeyEvent.KEYCODE_DPAD_RIGHT);
		
		keys.put("KEY_PAGEDOWN", KeyEvent.KEYCODE_PAGE_DOWN);
		keys.put("KEY_PAGEUP", KeyEvent.KEYCODE_PAGE_UP);
		keys.put("KEY_END", KeyEvent.KEYCODE_MOVE_END);
		keys.put("KEY_HOME", KeyEvent.KEYCODE_MOVE_HOME);
		
		keys.put("KEY_KPPLUS", KeyEvent.KEYCODE_PLUS);
		keys.put("KEY_KPDOT", KeyEvent.KEYCODE_NUMPAD_DOT);

		keys.put("KEY_BACKSPACE", KeyEvent.KEYCODE_DEL);
		keys.put("KEY_DOT", KeyEvent.KEYCODE_PERIOD);
		keys.put("KEY_ESC", KeyEvent.KEYCODE_ESCAPE);
		
		
		keys.put("KEY_MOUSE_TOGGLE", KeyEvent.KEYCODE_BUTTON_MODE);
		
		for(int i=0; i<10; i++)	keys.put("KEY_KP" + i, KeyEvent.KEYCODE_NUMPAD_0 + i);
		initialized = true;
	}
	
	public static void addTranslation(String name, int keyCode) {
		keys.put(name, keyCode);
	}
	
	public static VirtualEvent translate(String name) {
		if (name == null || name.equals("NONE")) return null;
		if (name.equals("MOUSE_LEFT")) {
			return new VirtualEvent(MouseButton.LEFT);
		}
		
		if (name.equals("MOUSE_RIGHT")) {
			return new VirtualEvent(MouseButton.RIGHT);
		}
		
		if (!name.contains("+") && 
			!name.startsWith("KEY_") &&
			!name.startsWith("ATR_")) {
			name = "KEY_" + name;
		}
		
		VirtualEvent ve = new VirtualEvent();
		
		String parts[] = name.split("\\+");
		for (String part: parts) {
			if (parts.length>1) Log.d(LOGTAG, "Processing part [" + part + "]");
			if (part.equals("CTRL"))  ve.ctrl = true;
			if (part.equals("ALT"))   ve.alt = true;
			if (part.equals("SHIFT")) ve.shift = true;
			Integer keyCode = keys.get(part);
			if (keyCode != null) ve.keyCode = keyCode;
		}
		if (ve.keyCode!=0) return ve;
		
		Log.d(LOGTAG, "Can't translate key " + name);
		return null;
	}
}
