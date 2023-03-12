package retrobox.vinput;

import java.io.File;

import android.util.SparseBooleanArray;
import android.view.KeyEvent;

public class GamepadDevice {
	
	SparseBooleanArray triggerState = new SparseBooleanArray();

	private String deviceName;
	private int deviceId;
	public int player;
	public long lastSeen = 0;
	
	public boolean isOverlay = false;
	public boolean is8bitdoAuto = false;
	public boolean isPlayerKnown = false;
	
	public File keymapFile;
	
	private GamepadMapping gamepadMapping;
	
	public GamepadDevice() {}
	
	public String getDeviceName() {
		return deviceName;
	}

	public void setDeviceName(String deviceName) {
		this.deviceName = deviceName;
	}

	public int getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(int deviceId) {
		this.deviceId = deviceId;
	}

	@Override
	public String toString() {
		return "GenericGamepad {device:" + deviceName + ", deviceId:" + deviceId + ", player:" + player + "}";
	}

	public void setTriggerState(int code, boolean down) {
		triggerState.put(code, down);
	}
	
	public boolean getTriggerState(int code) {
		Boolean down = triggerState.get(code);
		return down!=null && down.booleanValue();
	}
	
	public void setDpad(boolean left, boolean right, boolean up, boolean down) {
		boolean leftChanged  = getTriggerState(KeyEvent.KEYCODE_DPAD_LEFT)  != left;
		boolean rightChanged = getTriggerState(KeyEvent.KEYCODE_DPAD_RIGHT) != right;
		boolean upChanged    = getTriggerState(KeyEvent.KEYCODE_DPAD_UP)    != up;
		boolean downChanged  = getTriggerState(KeyEvent.KEYCODE_DPAD_DOWN)  != down;
		
		setTriggerState(KeyEvent.KEYCODE_DPAD_LEFT , left);
		setTriggerState(KeyEvent.KEYCODE_DPAD_RIGHT, right);
		setTriggerState(KeyEvent.KEYCODE_DPAD_UP   , up);
		setTriggerState(KeyEvent.KEYCODE_DPAD_DOWN , down);
		
		GamepadKeyMapping gamepadKeyMapping = Mapper.knownKeyMappings[player];
		
		sendDpadEvent(gamepadKeyMapping, upChanged,    0, up);
		sendDpadEvent(gamepadKeyMapping, downChanged,  1, down);
		sendDpadEvent(gamepadKeyMapping, leftChanged,  2, left);
		sendDpadEvent(gamepadKeyMapping, rightChanged, 3, right);
	}
	
	private void sendDpadEvent(GamepadKeyMapping gamepadKeyMapping, boolean changed, int eventIndex, boolean down) {
		if (!changed) return;
		
		VirtualEvent virtualEvent = gamepadKeyMapping.virtualEvents[eventIndex];
		if (virtualEvent!=null) {
			virtualEvent.sendEvent(this, down);
		}
	}

	public GamepadMapping getGamepadMapping() {
		return gamepadMapping;
	}

	public void setGamepadMapping(GamepadMapping gamepadMapping) {
		this.gamepadMapping = gamepadMapping;
	}
	
	
}
