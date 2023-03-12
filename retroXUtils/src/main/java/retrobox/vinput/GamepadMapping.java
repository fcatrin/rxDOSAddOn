package retrobox.vinput;

import android.view.KeyEvent;
import retrobox.utils.R;

public class GamepadMapping {
	public enum Analog {LEFT, RIGHT};

	public static String eventNames[] = { 
			"UP", "DOWN", "LEFT", "RIGHT", 
			"BTN_A", "BTN_B", "BTN_X", "BTN_Y", 
			"TL", "TR", "TL2", "TR2",
			"TL3", "TR3", "SELECT", "START"
	};
	
	public static int eventLabelResourceIds[] = {
			R.string.gamepad_label_up,
			R.string.gamepad_label_down,
			R.string.gamepad_label_left,
			R.string.gamepad_label_right,
			R.string.gamepad_label_a,
			R.string.gamepad_label_b,
			R.string.gamepad_label_x,
			R.string.gamepad_label_y,
			R.string.gamepad_label_tl,
			R.string.gamepad_label_tr,
			R.string.gamepad_label_tl2,
			R.string.gamepad_label_tr2,
			R.string.gamepad_label_tl3,
			R.string.gamepad_label_tr3,
			R.string.gamepad_label_select,
			R.string.gamepad_label_start,
	};

	public static final int originCodes[] = {
			KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
			KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_BUTTON_X, KeyEvent.KEYCODE_BUTTON_Y,
			KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_R1, KeyEvent.KEYCODE_BUTTON_L2, KeyEvent.KEYCODE_BUTTON_R2,
			KeyEvent.KEYCODE_BUTTON_THUMBL, KeyEvent.KEYCODE_BUTTON_THUMBR, KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.KEYCODE_BUTTON_START
	};
	
	public String deviceName;
	public int translatedCodes[] = new int[originCodes.length];

	public int axisRx = 0;
	public int axisRy = 0;

	public GamepadMapping(String deviceName) {
		this.deviceName = deviceName;
		for(int i=0; i<originCodes.length; i++) {
			translatedCodes[i] = originCodes[i];
		}
	}

	public static GamepadMapping buildDefaultMapping() {
		return new GamepadMapping("rxdefault");
	}
	
	public int getOriginCode(int keyCode) {
		for(int i=0; i<translatedCodes.length; i++) {
			if (translatedCodes[i] == keyCode) {
				return originCodes[i];
			}
		}
		return 0;
	}
	
	public static int getOriginIndex(int keyCode) {
		for(int i=0; i<originCodes.length; i++) {
			if (originCodes[i] == keyCode) return i; 
		}
		return -1;
	}
	
	public int getTranslatedVirtualEvent(int genericCode) {
		for(int i=0; i<originCodes.length; i++) {
			if (originCodes[i] == genericCode) {
				return translatedCodes[i];
			}
		}
		return 0;
	}

	public String getDeviceName() {
		return deviceName;
	}
	
	public static boolean isGamepadEvent(int keyCode) {
		for(int i=4; i<originCodes.length; i++) {  // skip dpad to avoid remotes, consider only buttons
			if (originCodes[i] == keyCode) return true;
		}
		return false;
	}
}
