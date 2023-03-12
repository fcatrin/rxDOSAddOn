package retrobox.vinput;

import retrobox.vinput.GamepadMapping.Analog;
import retrobox.vinput.Mapper.ShortCut;
import retrobox.vinput.VirtualEvent.MouseButton;

public interface VirtualEventDispatcher {
	void sendKey(GamepadDevice gamepad, int keyCode, boolean down);
	void sendMouseButton(MouseButton button, boolean down);
	boolean handleShortcut(ShortCut shortcut, boolean down);
	void sendAnalog(GamepadDevice gamepad, Analog index, double x, double y, double hatx, double haty);
}
