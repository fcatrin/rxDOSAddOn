package retrobox.vinput;

import retrobox.vinput.AnalogGamepad.Axis;

public interface AnalogGamepadListener {
	public void onAxisChange(GamepadDevice gamepad, float axisx, float axisy, float hatX, float hatY, float raxisx, float raxisy);
	public void onMouseMove(int mousex, int mousey);
	public void onMouseMoveRelative(float mousex, float mousey);
	public void onDigitalX(GamepadDevice gamepad, Axis axis, boolean on);
	public void onDigitalY(GamepadDevice gamepad, Axis axis, boolean on);
	public void onTriggers(String deviceName, int deviceId, boolean left, boolean right);
	public void onTriggersAnalog(GamepadDevice gamepad, int deviceId, float left, float right);
}
