package retrobox.vinput;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import retrobox.utils.RetroBoxUtils;
import retrobox.vinput.overlay.Overlay.OverlayControlsMode;
import xtvapps.core.AndroidCoreUtils;

public class Mapper {
	private static final String LOGTAG = "vinput.Mapper"; 
	
	public static final int MAX_PLAYERS = 4;
	private static final int MAX_MAPPINGS = 100;
	
	public enum ShortCut {NONE, LOAD_STATE, SAVE_STATE, SWAP_DISK, MENU, EXIT, SCREENSHOT};
	private static int keyShortCuts[] = {0, KeyEvent.KEYCODE_BUTTON_L2, KeyEvent.KEYCODE_BUTTON_R2, KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.KEYCODE_BUTTON_L1};
	
	private static final long LAST_SEEN_TIMEOUT = 60 * 1000;
	
	private boolean inShortcutSequence = false;
	private boolean wasShortcutSent = false;
	public static VirtualEventDispatcher listener;
	public static AnalogGamepadListener analogListener;
	public static Mapper instance;
	private static GestureDetector mDetector;
	private static boolean joinPorts = false;

	private static GamepadMapping defaultGamepadMapping;

	public static GamepadDevice[] gamepadDevices = new GamepadDevice[MAX_PLAYERS];
	public static GamepadKeyMapping[] knownKeyMappings = new GamepadKeyMapping[MAX_PLAYERS];
	private static Map<String, GamepadMapping> knownGamepadMappings = new HashMap<String, GamepadMapping>();
	
	private static int registeredGamepadDevices = 0;
	
	private static OverlayControlsMode overlayControlsMode = OverlayControlsMode.Auto;
	
	private static boolean is8bitdoAuto = true;
	
	static {
		for(int i=0; i<MAX_PLAYERS; i++) {
			gamepadDevices[i] = new GamepadDevice();
			gamepadDevices[i].player = i;
			
			knownKeyMappings[i] = new GamepadKeyMapping();
		}
	}

	public Mapper(Intent intent, VirtualEventDispatcher listener) {
		Mapper.instance = this;
		Mapper.listener = listener;
		
		defaultGamepadMapping = GamepadMapping.buildDefaultMapping();
		
		KeyTranslator.init();
		initVirtualEvents(intent);
		initGamepadMappings(intent);
		
		String sOverlayControlsMode = intent.getStringExtra("overlayControlsMode");
		if (sOverlayControlsMode!=null) {
			try {
				overlayControlsMode = OverlayControlsMode.valueOf(sOverlayControlsMode);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		is8bitdoAuto = intent.getBooleanExtra("8bitdoAutoMap", true);
		
		String defaultDeviceName = intent.getStringExtra("gamepadDeviceName");
		int    defaultDeviceId   = intent.getIntExtra("gamepadDeviceId", 0);
		
		if (defaultDeviceName!=null) {
			registerGamepad(defaultDeviceName, defaultDeviceId);
		}
	}
	
	public static void initGestureDetector(Activity activity) {
		SwipeListener listener = new SwipeListener() {

			@Override
			public void onSwipe(Swipe swipe) {
				if (swipe == Swipe.Left) sendShortcutMenu();
			}
		};
		
		DisplayMetrics metrics = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
		mDetector = new GestureDetector(activity, new SwipeDetector(listener, metrics.widthPixels, metrics.heightPixels));
	}
	
	public static void setJoystickAnalogListener(AnalogGamepadListener analogListener) {
		Mapper.analogListener = analogListener;
	}
	
	private void initVirtualEvents(Intent intent) {
		Log.d("REMAP", "Intent " + intent.getExtras());
		for(int player = 0; player<MAX_PLAYERS; player++) {
			
			// first try to load from file
			String keymapFileKey = "KEYMAP_" + (player+1);
			String keymapFileName = intent.getStringExtra(keymapFileKey);
			if (keymapFileName!=null) {
				try {
					loadVirtualEvents(player, new File(keymapFileName));
					continue;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			// if anything fails, read the old way
	    	String prefix = "kmap" + (player+1);
		    for(int i=0; i<GamepadMapping.eventNames.length; i++) {
		    	String keyName = GamepadMapping.eventNames[i];
		    	String keyNameLinux = intent.getStringExtra(prefix + keyName); 
		    	Log.d("REMAP", "Keyname Linux  " + prefix + keyName + "=" + keyNameLinux);
		    	
		    	if (keyNameLinux!=null) {
		    		Log.d("REMAP", "Key for " + keyName + " is " + keyNameLinux);
		    		VirtualEvent event = KeyTranslator.translate(keyNameLinux);
		    		knownKeyMappings[player].virtualEvents[i] = event;
		    		Log.d("REMAP", "Linux key " + keyNameLinux + " mapped to event " + event);
		    	} else knownKeyMappings[player].virtualEvents[i] = null;
		    }
		}
	}
	
	public static void loadVirtualEvents(int player, File keymapFile) throws IOException {
		Map<String, String> mapping = RetroBoxUtils.loadMapping(keymapFile);
		Log.d("KEYMAPFILE", "load " + keymapFile.getAbsolutePath() + " as " + mapping);
		for(int i=0; i<GamepadMapping.eventNames.length; i++) {
	    	String keyName = GamepadMapping.eventNames[i];
	    	String keyNameLinux = mapping.get(keyName);
	    	
	    	VirtualEvent event = null;
	    	
	    	if (keyNameLinux!=null) {
	    		event = KeyTranslator.translate(keyNameLinux);
		    	Log.d("KEYMAPFILE", "Linux key " + keyNameLinux + " mapped to event " + event);
	    	}
	    	knownKeyMappings[player].virtualEvents[i] = event;
		}
		gamepadDevices[player].keymapFile = keymapFile;
	}
	
	public static File getKeymapFile(int player) {
		return gamepadDevices[player-1].keymapFile;
	}

	private void initGamepadMappings(Intent intent) {
		joinPorts = intent.getBooleanExtra("joinPorts", false);
		
		for(int mapping = 0; mapping < MAX_MAPPINGS; mapping++) {
			String prefix = "gmap_" + mapping;
			String deviceName = intent.getStringExtra(prefix);
			if (deviceName == null) return;
			
			deviceName = deviceName.toLowerCase(Locale.US).trim();

			GamepadMapping gamepadMapping = new GamepadMapping(deviceName);
			for(int i=0; i<GamepadMapping.eventNames.length; i++) {
				String eventName = GamepadMapping.eventNames[i];
				Integer keyCode = intent.getIntExtra(prefix + eventName, 0);
				if (keyCode>0) {
					Log.d(LOGTAG, "keyCode " +  prefix + " for " + eventName + ":" + keyCode);
					gamepadMapping.translatedCodes[i] = keyCode;
				}
			}
			
			gamepadMapping.axisRx = intent.getIntExtra(prefix + "RX", 0) / 1000;
			gamepadMapping.axisRy = intent.getIntExtra(prefix + "RY", 0) / 1000;
			
			knownGamepadMappings.put(deviceName, gamepadMapping);
		}

	}
	
	public static int getTranslatedVirtualEvent(GamepadDevice gamepad, int genericCode) {
		GamepadMapping gamepadMapping = gamepad.getGamepadMapping();
		return gamepadMapping.getTranslatedVirtualEvent(genericCode);
	}
	
	public static VirtualEvent getVirtualEvent(GamepadDevice gamepad, int translatedCode) {
		GamepadMapping gamepadMapping = gamepad.getGamepadMapping();
		for(int i=0; i<gamepadMapping.translatedCodes.length; i++) {
			if (gamepadMapping.translatedCodes[i] == translatedCode) {
				VirtualEvent ev = knownKeyMappings[gamepad.player].virtualEvents[i];
				return ev;
			}
		}
		return null;
	}
	
	public static VirtualEvent getTargetEventIndex(GamepadDevice gamepad, int index) {
		return knownKeyMappings[gamepad.player].virtualEvents[index];
	}
	
	public static VirtualEvent getTargetEvent(GamepadDevice gamepad, int genericCode) {
		for(int i=0; i<GamepadMapping.originCodes.length; i++) {
			if (GamepadMapping.originCodes[i] == genericCode) {
				return knownKeyMappings[gamepad.player].virtualEvents[i];
			}
		}
		return null;
	}
	
	public static void setTargetEvent(GamepadDevice gamepad, int genericCode, VirtualEvent ev) {
		for(int i=0; i<GamepadMapping.originCodes.length; i++) {
			if (GamepadMapping.originCodes[i] == genericCode) {
				knownKeyMappings[gamepad.player].virtualEvents[i] = ev;
				return;
			}
		}
	}
	
	private void sendKeyPress(GamepadDevice gamepad, int keyCode) {
		listener.sendKey(gamepad, keyCode, true);
		try {
			Thread.sleep(100);
		} catch (Exception e) {}
		listener.sendKey(gamepad, keyCode, false);
	}
	
	public boolean handleKeyEvent(Activity activity, KeyEvent event, int keyCode, boolean down) {
		GamepadDevice gamepad = resolveGamepadByName(event.getDevice().getName(), event.getDeviceId());
		if (gamepad == null) return false;
		
		if (!gamepad.isPlayerKnown) {
			announceGamepad(activity, event.getDevice().getName(), gamepad);
			gamepad.isPlayerKnown = true;
		}

		return handleKeyEvent(gamepad, keyCode, down);
	}
	
	public boolean handleTriggerEventByDeviceName(String deviceName, int deviceId, boolean left, boolean right) {
		GamepadDevice gamepad = resolveGamepadByName(deviceName, deviceId);
		if (gamepad == null) return false;
		
		boolean leftChanged  = gamepad.getTriggerState(MotionEvent.AXIS_LTRIGGER) != left;
		boolean rightChanged = gamepad.getTriggerState(MotionEvent.AXIS_RTRIGGER) != right;
		if (leftChanged) {
			gamepad.setTriggerState(MotionEvent.AXIS_LTRIGGER, left);
			VirtualEvent ev = getTargetEvent(gamepad, KeyEvent.KEYCODE_BUTTON_L2);
			if (ev!=null) ev.sendEvent(gamepad, left);
		}
		if (rightChanged) {
			gamepad.setTriggerState(MotionEvent.AXIS_RTRIGGER, right);
			VirtualEvent ev = getTargetEvent(gamepad, KeyEvent.KEYCODE_BUTTON_R2);
			if (ev!=null) ev.sendEvent(gamepad, right);
		}
		return true;
	}
	
	private void announceGamepad(final Activity activity, String deviceName, GamepadDevice gamepad) {
		// gamepad device name is normalized, so we need the original device name here
		final String msg = "Controller " + deviceName + " is Player " + (gamepad.player + 1);
		activity.runOnUiThread(new Runnable(){

			@Override
			public void run() {
				AndroidCoreUtils.toast(activity, msg);
			}
		});
	}
	
	public boolean handleKeyEvent(GamepadDevice gamepad, int keyCode, boolean down) {
		if (gamepad == null) return false;
		
		if (handleShortcut(gamepad, keyCode, down)) return true;
		
		VirtualEvent ev = getVirtualEvent(gamepad, keyCode);
		if (ev != null) {
			ev.sendEvent(gamepad, down);
			return true;
		}
		
		if (keyCode == KeyEvent.KEYCODE_SEARCH || keyCode == KeyEvent.KEYCODE_MENU) {
			if (!down) {
				sendShortcutMenu();
			}
			return true;
		}

		return false;
	}
	
	protected boolean handleDetectedShortcut(int keyCode, boolean down) {
		ShortCut shortcut = ShortCut.NONE;
		for(int i=1; i<keyShortCuts.length; i++) {
			if (keyShortCuts[i] == keyCode) {
				shortcut = ShortCut.values()[i];
				break;
			}
		}
		if (shortcut!=ShortCut.NONE) {
			Log.d(LOGTAG, "Shortcut sent: " + shortcut + " pressed:" + down);
			return listener.handleShortcut(shortcut, down);
		}
		return false;
	}
	
	public static void sendShortcutMenu() {
		listener.handleShortcut(ShortCut.MENU, false);
	}
	
	protected void sendStartKeyPress(GamepadDevice gamepad) {
		VirtualEvent ve = getTargetEvent(gamepad, KeyEvent.KEYCODE_BUTTON_START);
		if (ve!=null) sendKeyPress(gamepad, ve.keyCode);
	}
	
	protected int getOriginCode(GamepadDevice gamepad, int keyCode) {
		return gamepad.getGamepadMapping().getOriginCode(keyCode);
	}
	
	public static int getOriginCodeByIndex(int index) {
		return GamepadMapping.originCodes[index];
	}

	protected boolean isStartButton(GamepadDevice gamepad, int keyCode) {
		return getOriginCode(gamepad, keyCode) == KeyEvent.KEYCODE_BUTTON_START;
	}
	
	boolean isL3down = false;
	boolean isR3down = false;
	
	public boolean handleShortcut(GamepadDevice gamepad, int keyCode, boolean down) {
		if (gamepad == null) return false;
		
		boolean wasScreenshotComboDown = isL3down && isR3down;

		if (getOriginCode(gamepad, keyCode) == KeyEvent.KEYCODE_BUTTON_THUMBL) isL3down = down;
		if (getOriginCode(gamepad, keyCode) == KeyEvent.KEYCODE_BUTTON_THUMBR) isR3down = down;
		
		if (!wasScreenshotComboDown) {
			if (isL3down && isR3down) {
				boolean handled = listener.handleShortcut(ShortCut.SCREENSHOT, down);
				if (handled) return true;
			}
		}
		
		if (isStartButton(gamepad, keyCode)) {
			if (down) {
				inShortcutSequence = true;
				return true;
			} else {
				inShortcutSequence = false; 
				if (!wasShortcutSent) sendStartKeyPress(gamepad);  // send pending Start if no shortcut was generated
				wasShortcutSent = false;
				return true;
			}
		}
		if (inShortcutSequence) {
			wasShortcutSent = handleDetectedShortcut(getOriginCode(gamepad, keyCode), down);
			if (wasShortcutSent && !down) inShortcutSequence = false; // make sure that we clean this. Another window could receive the keyUp
			return wasShortcutSent;
		}
		return false;
	}
	
	public boolean isSystemKey(KeyEvent event, int keyCode) {
		GamepadDevice gamepad = event==null ? null : resolveGamepadByName(event.getDevice().getName(), event.getDeviceId());
		if (gamepad!=null && gamepad.getGamepadMapping().getOriginCode(keyCode)!=0) return false;
		
		return 
			keyCode == KeyEvent.KEYCODE_BACK || 
			keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || 
			keyCode == KeyEvent.KEYCODE_VOLUME_UP || 
			keyCode == KeyEvent.KEYCODE_VOLUME_MUTE;
	}

	public static void sendJoystickAnalogMove(GamepadDevice gamepad, float xval, float yval) {
		if (analogListener!=null) analogListener.onAxisChange(gamepad, xval,  yval, xval, yval, 0, 0);
	}
	
	public void onTouchEvent(MotionEvent me) {
		if (mDetector!=null) mDetector.onTouchEvent(me);
	}
	
	public static GamepadDevice resolveGamepadByName(String deviceName, int deviceId) {
		deviceName = deviceName.toLowerCase(Locale.US).trim();
		
		long t0 = System.currentTimeMillis();
		for(int retry = 0; retry < 2; retry++) {
			for(int i=0; i<MAX_PLAYERS; i++) {
				GamepadDevice gamepad = gamepadDevices[i];
				String gamepadDeviceName = gamepad.getDeviceName();
				if (gamepadDeviceName == null) continue;
				
				gamepadDeviceName = gamepadDeviceName.toLowerCase(Locale.US).trim();
				if (deviceName.equals(gamepadDeviceName) && (joinPorts || gamepad.getDeviceId() == 0 || gamepad.getDeviceId() == deviceId)) {
					gamepad.lastSeen = t0; 
					gamepad.setDeviceId(deviceId);
					return gamepad;  
				}
			}
			
			// if not found, reset deviceId on probably disconnected devices
			for(int i=0; i<MAX_PLAYERS; i++) {
				GamepadDevice gamepad = gamepadDevices[i];
				if (gamepad.lastSeen < t0 - LAST_SEEN_TIMEOUT && gamepad.getDeviceName()!=null) {
					gamepad.setDeviceId(0);
					Log.d(LOGTAG, "GamepadDevice " + gamepad.getDeviceName() + ", id:" + gamepad.getDeviceId() + " has been reset");
				}
			}			
		}
		return registerGamepad(deviceName, deviceId);
	}
	
	private static boolean is8bitdo(String deviceName) {
		return deviceName!=null && deviceName.toLowerCase(Locale.US).startsWith("8bitdo n64");
	}
	
	private static GamepadDevice registerGamepad(String deviceName, int deviceId) {
		if (registeredGamepadDevices == MAX_PLAYERS) return null;

		if (deviceName == null) return null;
		deviceName = deviceName.toLowerCase(Locale.US).trim();
		
		GamepadDevice gamepad = gamepadDevices[registeredGamepadDevices];
		gamepad.setDeviceName(deviceName);
		gamepad.setDeviceId(deviceId);
		gamepad.is8bitdoAuto = is8bitdoAuto && is8bitdo(deviceName);
		
		GamepadMapping gamepadMapping = knownGamepadMappings.get(deviceName);
		if (gamepadMapping == null || gamepad.is8bitdoAuto) gamepadMapping = defaultGamepadMapping;
		
		gamepad.setGamepadMapping(gamepadMapping);
		
		Log.d(LOGTAG, "Register gamepad for player " + (registeredGamepadDevices) + " device:" + deviceName + " deviceId:" + deviceId + " mapper:" + gamepadMapping.getDeviceName() + " mappings:" + knownGamepadMappings);
		registeredGamepadDevices++;
		
		return gamepad;
	}

	public static boolean mustDisplayOverlayControllers() {
		return (!hasGamepads() && overlayControlsMode == OverlayControlsMode.Auto)
			|| overlayControlsMode == OverlayControlsMode.On;
	}
	
	public static boolean hasGamepads() {
		return hasGamepad(0) || hasGamepad(1);  // check player one or two
	}

	private static boolean hasGamepad(int player) {
		return gamepadDevices[player].getDeviceName()!=null;
	}
}
