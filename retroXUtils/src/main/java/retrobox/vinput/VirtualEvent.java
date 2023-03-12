package retrobox.vinput;

import android.view.KeyEvent;

public class VirtualEvent {
	public enum MouseButton {LEFT, CENTER, RIGHT};
	
	private static final String LOGTAG = VirtualEvent.class.getSimpleName();
	public int keyCode;
	public boolean alt;
	public boolean ctrl;
	public boolean shift;
	public MouseButton mouseButton;
	
	public VirtualEvent(int keyCode, boolean alt, boolean ctrl, boolean shift) {
		this.keyCode = keyCode;
		this.alt = alt;
		this.ctrl = ctrl;
		this.shift = shift;
	}
	
	public VirtualEvent(int keyCode) {
		this(keyCode, false, false, false);
	}
	
	public VirtualEvent(MouseButton mouseButton) {
		this.mouseButton = mouseButton;
	}
	
	public VirtualEvent() {
		this(0);
	}
	
	public void sendEvent(GamepadDevice gamepad, boolean down) {
		if (mouseButton==null) {
			Mapper.listener.sendKey(gamepad, keyCode, down);
		} else {
			Mapper.listener.sendMouseButton(mouseButton, down);
		}
	}

	public boolean isMouseButton() {
		return mouseButton!=null;
	}
	
	public boolean isKeyboardMouseToggle() {
		return keyCode == KeyEvent.KEYCODE_BUTTON_MODE;
	}

	@Override
	public String toString() {
		StringBuffer s = new StringBuffer();
		s.append("VK ");
		if (mouseButton!=null) {
			s.append("MOUSE BUTTON ").append(mouseButton.name());
		} else if (isKeyboardMouseToggle()) {
			s.append("KEYB/MOUSE TOGGLE");
		} else {
			if (ctrl)  s.append("CTRL+");
			if (alt)   s.append("ALT+");
			if (shift) s.append("SHIFT+");
			s.append(keyCode);
		}
		return s.toString();
	}

}
