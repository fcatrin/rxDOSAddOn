package retrobox.vinput.overlay;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;
import android.view.KeyEvent;
import retrobox.vinput.GamepadDevice;
import retrobox.vinput.GamepadMapping;
import retrobox.vinput.GamepadMapping.Analog;
import retrobox.vinput.Mapper;
import retrobox.vinput.VirtualEvent;
import retrobox.vinput.overlay.OverlayButton.ButtonAction;
import retrobox.vinput.overlay.OverlayButton.ButtonType;

public class Overlay {
	private static final int BUF_SIZE = 256*1024;
	public static final int POINTER_ID_NONE = -1;
	
	public enum OverlayControlsMode {Auto, On, Off}

	static List<OverlayButton> buttons = new ArrayList<OverlayButton>();
	static Map<String, OverlayButton> knownButtons = new HashMap<String, OverlayButton>();
	
	private static GamepadDevice overlayGamepadDevice;
	
	public static boolean requiresRedraw = false;
	
	Paint paintPressed = new Paint();
	Paint paintNormal = new Paint();
	
	public static String overlayEventNames[] = { 
		"up", "down", "left", "right", 
		"a", "b", "x", "y", 
		"l", "r", "l2", "r2",
		"l3", "r3", "select", "start", "analog_left", "analog_right"
	};

	//private static final int ANALOG_LEFT  = overlayEventNames.length-2;
	private static final int ANALOG_RIGHT = overlayEventNames.length-1;
	
	private Properties loadConfig(String filename) {
		Properties p = new Properties();
		InputStream is = null;
		try {
			is = new FileInputStream(new File(filename));
			p.load(is);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (is!=null) try {is.close();} catch (Exception e){}
		}
		return p;
	}
	
	public void init(String filename, int width, int height, float alpha) {
		OverlayButton.setTextSize(height / 40);
		buttons.clear();
		knownButtons.clear();
		
		overlayGamepadDevice = new GamepadDevice();
		overlayGamepadDevice.setDeviceName("_overlay_");
		overlayGamepadDevice.setGamepadMapping(new GamepadMapping(overlayGamepadDevice.getDeviceName()));
		overlayGamepadDevice.isOverlay = true;
		overlayGamepadDevice.player = 0;
		overlayGamepadDevice.setDeviceId(0);
		
		Properties config = loadConfig(filename);
		Log.d("OVERLAY", "Using config " + filename + " = " + config);
		int overlays = 1; // TODO handle multiple overlays getConfigInt(config, "overlays");
		String prefix = "overlay0";
		for(int i = 0; i< overlays; i++) {
			if (config.get("overlay" + i + "_name").equals("landscape")) {
				prefix = "overlay" + i;
				break;
			}
		}
		
		Log.d("OVERLAY", "Using overlay " + prefix);
		
		String baseDir = new File(filename).getParent();
		
		boolean normalized = getConfigBool(config, prefix + "_normalized");
		boolean fullScreen = getConfigBool(config, prefix + "_full_screen");
		float rangeMod = getConfigFloat(config, prefix + "_range_mod", 1.0f);

		float alphaMod = getConfigFloat(config, prefix + "_alpha_mod", 1.0f);
		
		int alphaNormal = (int)(255.0f * alpha);
		int alphaPressed = (int)(255.0f * alpha * alphaMod);
		if (alphaNormal>255) alphaNormal = 255;
		if (alphaPressed>255) alphaPressed = 255;
		paintNormal.setAlpha(alphaNormal);
		paintPressed.setAlpha(alphaPressed);
		
		int descs = getConfigInt(config, prefix + "_descs");
		for(int i=0; i<descs; i++) {
			processDesc(baseDir, config, prefix, normalized, fullScreen, rangeMod, width, height, i);
		}
	}
	
	private void processDesc(String baseDir, Properties config, String prefix, boolean normalized, boolean fullscreen, float rangeMod, int width, int height, int index) {
		String descriptorKey = prefix + "_desc" + index;
		String descriptor = getConfigString(config, descriptorKey);
		if (descriptor.isEmpty()) return;
		
		descriptor = descriptor.replace("\"", "");
		
		String parts[] = descriptor.split(",");
		if (parts.length!=6) return;
		
		String event = parts[0];
		float x = str2f(parts[1]);
		float y = str2f(parts[2]);
		ButtonType buttonType = str2ButtonType(parts[3]);
		float w = str2f(parts[4]);
		float h = str2f(parts[5]);
		
		OverlayButton button = new OverlayButton();
		button.x = (int)(x *  width);
		button.y = (int)(y *  height);
		button.type = buttonType;
		button.action = isAnalog(event)?ButtonAction.ANALOG:ButtonAction.TRIGGER;
		button.width = (int)(w * width);
		button.height = (int)(h * height);
		button.eventIndexes = parseEvents(event);
		knownButtons.put(event, button);
		
		button.rangeMod = getConfigFloat(config, descriptorKey + "_range_mod", rangeMod);
		button.pct = getConfigFloat(config, descriptorKey + "_pct", 1.0f);
		
		button.bitmap = loadBitmap(baseDir + "/" + getConfigString(config, descriptorKey + "_overlay"));
		button.recalc();
		
		Log.d("ANALOG", "add button " + button);
		
		buttons.add(button);
	}
	
	private boolean isAnalog(String event) {
		return event!=null && event.startsWith("analog");
	}
	
	private static int[] parseEvents(String eventDescriptor) {
		List<Integer> events = new ArrayList<Integer>();
		String parts[] = eventDescriptor.split("[|]");
		for(String part : parts) {
			Log.d("OVERLAY", "match event " + part);
			for(int i=0; i<overlayEventNames.length; i++) {
				if (part.equals(overlayEventNames[i])) {
					Log.d("OVERLAY", "matched event " + part + " with " + overlayEventNames[i]);
					events.add(i);
				}
			}
		}
		Log.d("OVERLAY", "events matched " + events);
		if (events.size() == 0) return null;
		
		int result[] = new int[events.size()];
		for(int i=0; i<events.size(); i++) result[i] = events.get(i);
		return result;
	}
	
	private static Bitmap loadBitmap(String filename) {
		Log.d("OVERLAY", "loading bitmap " + filename);
		if (filename.isEmpty()) return null;
		File f = new File(filename);
		if (!f.exists()) return null;
		
		try {
			byte raw[] = loadBytes(f);
			
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inDither = false;
			return BitmapFactory.decodeByteArray(raw , 0, raw .length, options);
		} catch (Exception e) {
			return null;
		}
	}
	
	public void draw(Canvas canvas) {
		for(OverlayButton button : buttons) {
			button.draw(canvas, button.isPressed()?paintPressed:paintNormal);
		}
	}
	
	private static int fixupRetroArchSnesStyleButtons(int eventIndex) {
		int originalGamepadCode = GamepadMapping.originCodes[eventIndex];
		int fixedGamepadCode = -1;
		
		switch (originalGamepadCode) {
		case KeyEvent.KEYCODE_BUTTON_A: fixedGamepadCode = KeyEvent.KEYCODE_BUTTON_B; break;
		case KeyEvent.KEYCODE_BUTTON_B: fixedGamepadCode = KeyEvent.KEYCODE_BUTTON_A; break;
		case KeyEvent.KEYCODE_BUTTON_X: fixedGamepadCode = KeyEvent.KEYCODE_BUTTON_Y; break;
		case KeyEvent.KEYCODE_BUTTON_Y: fixedGamepadCode = KeyEvent.KEYCODE_BUTTON_X; break;
		}
		
		int newEventIndex = -1;
		if (fixedGamepadCode>=0) newEventIndex = GamepadMapping.getOriginIndex(fixedGamepadCode);
		return newEventIndex>=0 ? newEventIndex : eventIndex;
	}
	
	private static void pressButton(OverlayButton button, int pointerId, int x, int y) {
		button.setPressed(true);
		button.pointerId = pointerId;
		
		if (button.eventIndexes!=null) {
			if (button.action == ButtonAction.ANALOG) {
				button.updateAnalog(x, y);
				Analog analogControl = button.eventIndexes[0] == ANALOG_RIGHT?Analog.RIGHT:Analog.LEFT;
				Mapper.listener.sendAnalog(overlayGamepadDevice, analogControl, button.analogX, button.analogY, 0, 0);
			} else {
				for(int event : button.eventIndexes) {
					event = fixupRetroArchSnesStyleButtons(event);
					VirtualEvent ev = Mapper.getTargetEventIndex(overlayGamepadDevice, event);
					if (ev!=null) {
						ev.sendEvent(overlayGamepadDevice, true);
					}
				}
			}
		}
		requiresRedraw = true;
	}
	
	private static void releaseButton(OverlayButton button) {
		button.setPressed(false);
		button.pointerId = POINTER_ID_NONE;

		if (button.eventIndexes!=null) {
			if (button.action == ButtonAction.ANALOG) {
				button.updateAnalog(button.x, button.y);
				Analog analogControl = button.eventIndexes[0] == ANALOG_RIGHT?Analog.RIGHT:Analog.LEFT;
				Mapper.listener.sendAnalog(overlayGamepadDevice, analogControl, 0, 0, 0, 0);
			} else {
				for(int event : button.eventIndexes) {
					event = fixupRetroArchSnesStyleButtons(event);

					if (isPressedInAnotherButton(button, event)) continue;
					VirtualEvent ev = Mapper.getTargetEventIndex(overlayGamepadDevice, event);
					if (ev!=null) {
						ev.sendEvent(overlayGamepadDevice, false);
					}
				}
			}
		}
		requiresRedraw = true;
	}
	
	private static boolean isPressedInAnotherButton(OverlayButton button, int event) {
		for(OverlayButton otherButton : buttons) {
			if (otherButton == button || !otherButton.isPressed()) continue;
			
			for(int otherEvent : otherButton.eventIndexes) {
				if (otherEvent == event) return true;
			}
		}
		return false;
	}
	
	public static boolean onPointerMove(int pointerId, int x, int y) {
		Log.d("sendKey", "onPointerMove start");
		boolean handled = false;
		for(OverlayButton button : buttons) {
			if (button.eventIndexes == null) continue;
			if (button.pointerId == pointerId) {
				if (!button.contains(x,  y)) {
					releaseButton(button);
				} else if (button.action == ButtonAction.ANALOG) {
					pressButton(button, pointerId, x, y);
					requiresRedraw = false; // Avoid redrawing too often
				}
				handled = true;
			} else {
				if (!button.isPressed() && button.contains(x,  y)) {
					pressButton(button, pointerId, x, y);
					handled = true;
				}
			}
		}
		Log.d("sendKey", "onPointerMove end");
		return handled;
	}
	
	public static boolean onPointerDown(int pointerId, int x, int y) {
		boolean handled = false;
    	for(OverlayButton button : buttons) {
    		if (button.eventIndexes == null) continue;
    		if (button.contains(x, y)) {
    			pressButton(button, pointerId, x, y);
    			handled = true;
    		}
    	}
    	return handled;
	}
	
	public static boolean onPointerUp(int pointerId) {
		boolean handled = false;
		for(OverlayButton button : buttons) {
			if (button.pointerId == pointerId) {
				releaseButton(button);
				handled = true;
			}
		}
		return handled;
	}

	private static ButtonType str2ButtonType(String s) {
		return s.equals("radial")?ButtonType.CIRCLE:ButtonType.RECT;
	}
	
	private static int getConfigInt(Properties config, String key) {
		try {
			String s = getConfigString(config, key);
			return Integer.parseInt(s);
		} catch (Exception e) {
			return 0;
		}
	}
	
	private static float getConfigFloat(Properties config, String key, float defaultValue) {
		String s = getConfigString(config, key);
		return str2f(s, defaultValue);
	}


	private static float str2f(String s) {
		return str2f(s, 0);
	}
	
	private static float str2f(String s, float defaultValue) {
		try {
			return Float.parseFloat(s);
		} catch (Exception e) {
			return defaultValue;
		}
	}
	
	private static String getConfigString(Properties config, String key) {
		return config.getProperty(key, "");
	}
	
	private static boolean getConfigBool(Properties config, String key) {
		String s = getConfigString(config, key);
		return s.equals("true");
	}
	
	public static byte[] loadBytes(File f) throws IOException {
		FileInputStream is = null;
		try {
			is = new FileInputStream(f);
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			byte[] buf = new byte[BUF_SIZE];
			while (true) {
				int rc = is.read(buf);
				if (rc <= 0)
					break;
				else
					bout.write(buf, 0, rc);
			}
			return bout.toByteArray();
		} finally {
			if (is!=null) is.close();
		}
	}

	public static void setTriggerLabel(String trigger, String name) {
		OverlayButton button = knownButtons.get(trigger);
		if (button!=null) button.label = name;
	}
	
}
