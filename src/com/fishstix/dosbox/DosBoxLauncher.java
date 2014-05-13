/*
 *  Copyright (C) 2013 Fishstix - Based upon DosBox & anDosBox by Locnet
 *  
 *  Copyright (C) 2011 Locnet (android.locnet@gmail.com)
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package com.fishstix.dosbox;

import java.io.File;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.fishstix.dosbox.Joystick.Position;
import com.fishstix.dosbox.VirtualKey.MouseButton;
import com.fishstix.dosbox.library.dosboxprefs.DosBoxPreferences;

  
public class DosBoxLauncher extends Activity {
	public static final int SPLASH_TIMEOUT_MESSAGE = -1;
	public static final String START_COMMAND_ID = "start_command";
	public String mConfFile = DosBoxPreferences.CONFIG_FILE;
	public String mConfPath = DosBoxPreferences.CONFIG_PATH;
	
	static { 
		System.loadLibrary("dosbox");
	}
	public native void nativeInit(Object ctx);
	public static native void nativeShutDown();
	public static native void nativeSetOption(int option, int value, String value2, boolean l);
	public native void nativeStart(Bitmap bitmap, int width, int height, String confPath);
	public static native void nativePause(int state);
	public static native void nativeStop();
	public static native void nativePrefs();
	public static native boolean isARMv7();
	
	public DosBoxSurfaceView mSurfaceView = null;
	public DosBoxAudio mAudioDevice = null;
	public DosBoxThread mDosBoxThread = null;
	public SharedPreferences prefs;
	private static DosBoxLauncher mDosBoxLauncher = null;
	
	public boolean mPrefRefreshHackOn = false;
	public boolean mPrefCycleHackOn = true;
	public boolean mPrefScaleFilterOn = false;
	public boolean mPrefSoundModuleOn = true;
	//public boolean mPrefAutoCPUOn = true;
	public boolean mPrefMixerHackOn = true;
	public boolean mTurboOn = false;
	public String mPID = null;
	//public String mPrefKeyMapping = "abc";
	public int mPrefCycles = 3000; 
	public int mPrefFrameskip = 2; 
	public int mPrefMemorySize = 4; 
	public int mPrefScaleFactor = 100;
	private boolean isMouseOnly = false;
	
	private static String keyNames[] = { 
		"UP", "DOWN", "LEFT", "RIGHT", "BTN_A", "BTN_B", "BTN_X", "BTN_Y", "TL", "TR",
		"SELECT", "START", "EXIT"
	};

	private static VirtualKey keyValues[] = new VirtualKey[keyNames.length];
	private static boolean useKeyTranslation = false;
    
    // gives the native activity a copy of this object so it can call OnNativeMotion
    //public native int RegisterThis();
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i("DosBoxTurbo", "onCreate()");
		// Touchpad stuff
		//getWindow().takeSurface(null);
		//RegisterThis();
		
		mSurfaceView = new DosBoxSurfaceView(this);
		setContentView(mSurfaceView);
		registerForContextMenu(mSurfaceView); 
		
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		mDosBoxLauncher = this;
		//PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		Context otherAppsContext = null;
		//getData("Default.xml");
		Bundle b = getIntent().getExtras();
		if (b != null) {
			mPID = b.getString("com.fishstix.dosboxlauncher.pid");
		}
		try {
			otherAppsContext = createPackageContext("com.fishstix.dosboxlauncher", 0);
		} catch (NameNotFoundException e) {
			Log.i("DosBoxTurbo","Profile Manager not found");
		}
		
		if ((otherAppsContext != null)&&(mPID != null)) {
			try {
				DosBoxMenuUtility.getData(this,mPID);
				mConfPath = DosBoxPreferences.CONFIG_PATH + "dosbox/"; 
				mConfFile = mPID+".conf";
				if (DosBoxMenuUtility.isExternalStorageWritable()) {
					File file = new File(mConfPath);
					if (!file.exists())
						file.mkdirs();
				}
				prefs = getSharedPreferences(mPID, MODE_PRIVATE);
			} catch (Exception e) {
				prefs = PreferenceManager.getDefaultSharedPreferences(this);
			}
		} else {
			prefs = PreferenceManager.getDefaultSharedPreferences(this);
		}
			
		if (prefs.getBoolean("dosmanualconf", false)) {
			File f = new File(prefs.getString("dosmanualconf_file", DosBoxPreferences.CONFIG_PATH+DosBoxPreferences.CONFIG_FILE));
			mConfPath = f.getParent()+"/";
			mConfFile = f.getName();
			if (!f.exists()) {
				Log.i("DosBoxTurbo","Config file not found: "+f.getAbsolutePath());
			}
		} else {
			// if not manual mode
			if (prefs.getString("doscycles", "auto").contentEquals("auto")) {	
				// quick system
				DosBoxMenuUtility.mPrefCycleString = "max";			
			}
		}
		
		isMouseOnly  = getIntent().getBooleanExtra("mouseOnly", false);
		
	    // load key translations from retrobox (linux) to sdl
		KeyTranslator.init();
	    for(int i=0; i<keyNames.length; i++) {
	    	String keyNameLinux = getIntent().getStringExtra("kmap1" + keyNames[i]); // only 1 player in andriod touchscreen
	    	if (keyNameLinux!=null) {
	    		Log.d("REMAP", "Key for " + keyNames[i] + " is " + keyNameLinux);
	    		useKeyTranslation = true;
	    		VirtualKey key = KeyTranslator.translate(keyNameLinux);;
	    		keyValues[i] = key;
	    		Log.d("REMAP", "Linux key " + keyNameLinux + " mapped to key " + key);
	    	} else keyValues[i] = null;
	    }
		
	    
		DosBoxMenuUtility.loadPreference(this,prefs);	

		BitmapDrawable splash = (BitmapDrawable) getResources().getDrawable(R.drawable.splash);
		splash.setTargetDensity(120);
		splash.setGravity(Gravity.CENTER);		
		mSurfaceView.setBackgroundDrawable(splash);

		initDosBox();
		startDosBox();
		Log.i("DosBoxTurbo","onCreate");
		// don't know whether one more handler will hurt, so abuse key handler
		mSurfaceView.mKeyHandler.sendEmptyMessageDelayed(SPLASH_TIMEOUT_MESSAGE, 1000);
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// calculate joystick constants
		ViewTreeObserver observer = mSurfaceView.getViewTreeObserver();
		observer.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

	        public void onGlobalLayout() {
	    		extraButtons.clear();
	    		recalculateJoystickOverlay();
	    	    initExtraButtons(getIntent().getStringExtra("buttons"));
	    		
	            Log.v("DosBoxTurbo",
	                    String.format("new width=%d; new height=%d", mSurfaceView.getWidth(),
	                            mSurfaceView.getHeight()));
	        }
	    });

	}
	// splash can go here

	public static final int EXTRA_BUTTONS_ALPHA = 0x80000000;
	public static final int EXTRA_BUTTONS_ALPHA_PRESSED = 0xE0000000;

	private JoystickButtonExtra createMouseButton(String name, String key, int left, int margin, int h, int size) {
		boolean isLeftButton = key.equals("MOUSE_LEFT");
		JoystickButtonExtra mouseButton = new JoystickButtonExtra();
		mouseButton.w = size;
		mouseButton.h = (int)(size * 0.4);
		mouseButton.y = h - margin - mouseButton.h;
		
		mouseButton.x = isLeftButton?margin:left - margin - mouseButton.w;
		
		mouseButton.label = name;
		mouseButton.key = new VirtualKey(isLeftButton?MouseButton.LEFT:MouseButton.RIGHT);
		mouseButton.color = EXTRA_BUTTONS_ALPHA | 0xFFFFFF;
		mouseButton.colorPressed = EXTRA_BUTTONS_ALPHA_PRESSED | 0xFFFFFF;
		return mouseButton;
	}
	
	private JoystickButtonExtra createExtraButton(String name, String key, int left, int top, int size) {
		JoystickButtonExtra button = new JoystickButtonExtra();
		button.x = left;
		button.y = top;
		button.w = size;
		button.h = (int)(size * 0.4);
		button.label = name;
		button.key = KeyTranslator.translate(key);
		button.color = EXTRA_BUTTONS_ALPHA | 0xFFFFFF;
		button.colorPressed = EXTRA_BUTTONS_ALPHA_PRESSED | 0xFFFFFF;
		return button;
	}
	
	private void initExtraButtons(String json){
		int w = mSurfaceView.getWidth();
		int h = mSurfaceView.getHeight();
		int maxButtons = 10;
		
		final float scale = getResources().getDisplayMetrics().density;
		int margin = (int)(10 * scale);
		int gap = (int)(4 * scale);
		int size = (w - (maxButtons-1)*gap + margin*2 ) / maxButtons;

		int left = (w - (2*size) - gap) / 2;
		int top = h - margin - size;

		JoystickButtonExtra select = null;
		JoystickButtonExtra start = null;
		
		if (!isMouseOnly) {
			select = createExtraButton("SELECT", "NONE", left, top, size);
			left += size + gap;
			start  = createExtraButton("START" , "NONE", left, top, size);
			select.key = keyValues[10];
			start.key  = keyValues[11];
			
			extraButtons.add(select);
			extraButtons.add(start);
			Log.d("EXTRA", "Added select and start");
		}

		
		if (json != null && json.trim().length() >= 0) {
			try {
				int nTopButtons = 0;
				JSONArray a = new JSONArray(json);
				for(int i=0; i<a.length(); i++) {
					JSONObject o = a.getJSONObject(i);
					String key = o.getString("key");
					if (!key.startsWith("MOUSE_") && !key.startsWith("BTN_")) nTopButtons++;
				}
				
				left = nTopButtons == 0 ? 0 : (w - (nTopButtons*size) - (nTopButtons-1) * gap) / 2;
				top = margin;
				for(int i=0; i<a.length(); i++) {
					JSONObject o = a.getJSONObject(i);
					String name = o.getString("name");
					String key = o.getString("key");
					if (key.startsWith("MOUSE_")) {
						if (isMouseOnly) extraButtons.add(createMouseButton(name, key, w, top, h, size));
					} else if (key.startsWith("BTN_")) {
						int index = -1;
						if (key.equals("BTN_A")) index = 0;
						if (key.equals("BTN_B")) index = 1;
						if (key.equals("BTN_C")) index = 2;
						if (key.equals("BTN_D")) index = 3;
						if (index>=0) mSurfaceView.joystickButtonsOverlay[index].label = name;
						
						if (key.equals("BTN_SELECT") && select!=null) {
							if (name.equals("_hide_")) select.key = null;
							else select.label = name;
						}
						if (key.equals("BTN_START") && start!=null) {
							if (name.equals("_hide_")) start.key = null;
							else start.label = name;
						}
					} else {
						extraButtons.add(createExtraButton(name, key, left, top, size));
						left += size + gap;
					}
				}
			} catch (JSONException je) {
				je.printStackTrace();
			}
		}
		Log.d("EXTRA", extraButtons.size() + " buttons");
		mSurfaceView.joystickExtraButtonsOverlay = extraButtons.toArray(new JoystickButtonExtra[0]);
	}
	
	public static final int MAX_BUTTONS = 4;
	public static final int BUTTONS_ALPHA = 0x70000000;
	public static final int BUTTONS_ALPHA_PRESSED = 0xC0000000;
	public static final int BUTTONS_ALPHA_BALL = 0xA0000000;
	String buttonNames[] = {"A", "B", "X", "Y"};
	int buttonColors[] = {0xFF0000, 0xFFFF00, 0x0000FF, 0x00FF00};
	
	List<JoystickButtonExtra> extraButtons = new ArrayList<JoystickButtonExtra>();
	
	private void recalculateJoystickOverlay() {
		JoystickButton buttons[] = new JoystickButton[MAX_BUTTONS];

		int w = mSurfaceView.getWidth();
		int h = mSurfaceView.getHeight();
		
		int margin =  w / 8;
		int marginButton = w / 10;
		int radius = w / 24;
		int gap = (int)(radius * 1.5);
		
		JoystickButton button = new JoystickButton();
		button.x = w - marginButton;
		button.y = h - margin;
		buttons[0] = button;
		
		button = new JoystickButton();
		button.x = w - marginButton - gap;
		button.y = h - margin + gap;
		buttons[1] = button;
		
		button = new JoystickButton();
		button.x = w - marginButton - gap;
		button.y = h - margin - gap;
		buttons[2] = button;

		button = new JoystickButton();
		button.x = w - marginButton - gap*2;
		button.y = h - margin;
		buttons[3] = button;

		for(int i=0; i<buttonNames.length; i++) {
			buttons[i].label = buttonNames[i];
			buttons[i].color = buttonColors[i] | BUTTONS_ALPHA;
			buttons[i].colorPressed = buttonColors[i] | BUTTONS_ALPHA_PRESSED;
			buttons[i].key = keyValues[4+i];
			buttons[i].radius = radius;
		}
		
		mSurfaceView.joystickButtonsOverlay = buttons ;

		Joystick joystick = new Joystick();
		joystick.x = margin;
		joystick.y = h - margin;
		joystick.radius = (int)(margin * 0.8);
		joystick.radiusBall = (int)(joystick.radius * 0.6);
		joystick.color = BUTTONS_ALPHA | 0x777777;
		joystick.colorBall = BUTTONS_ALPHA_BALL | 0x777777;
		joystick.keyUp    = keyValues[0];
		joystick.keyDown  = keyValues[1];
		joystick.keyLeft  = keyValues[2];
		joystick.keyRight = keyValues[3];
		joystick.axisX = Position.CENTER;
		joystick.axisY = Position.CENTER;
		joystick.positionX = 0;
		joystick.positionY = 0;
		joystick.threshold = 0.2f;
		
		joystick.hasValidKeys = joystick.keyUp!=null && joystick.keyDown!=null && joystick.keyLeft != null && joystick.keyRight != null;
		
		mSurfaceView.joystickOverlay = joystick;
		
	}
	
	@Override
	protected void onDestroy() {
		Log.i("DosBoxTurbo", "onDestroy()");

		shutDownDosBox();
		mSurfaceView.shutDown();
		mSurfaceView = null;
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		Log.i("DosBoxTurbo","onPause()");
		pauseDosBox(true);
		super.onPause();
	}
	
	@Override
	protected void onStop() {
		Log.i("DosBoxTurbo","onStop()");
		super.onStop();
	}

	@Override
	protected void onResume() {
		Log.i("DosBoxTurbo","onResume()");
		super.onResume();
		pauseDosBox(false);
		
		DosBoxMenuUtility.loadPreference(this,prefs);
		  
		// set rotation
		if (Integer.valueOf(prefs.getString("confrotation", "0"))==0) {
			// auto
	        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
		} else if (Integer.valueOf(prefs.getString("confrotation", "0"))==1) {
			// portrait
	        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		} else {
	        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);			
		}

		if (mTurboOn) {
			Toast.makeText(this, "Fast Forward", Toast.LENGTH_SHORT).show();
		} else {			
			if (DosBoxControl.nativeGetAutoAdjust()) { 
				Toast.makeText(this, "Auto Cycles ["+DosBoxControl.nativeGetCycleCount() +"%]", Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(this, "DosBox Cycles: "+DosBoxControl.nativeGetCycleCount(), Toast.LENGTH_SHORT).show();
			}
		}
		
		Log.i("DosBoxTurbo","onResume");
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);		
		return DosBoxMenuUtility.doCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onPrepareOptionsMenu (Menu menu) {
		super.onPrepareOptionsMenu(menu);
		return DosBoxMenuUtility.doPrepareOptionsMenu(this, menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)	{
		if (DosBoxMenuUtility.doOptionsItemSelected(this, item))
			return true;
	    return super.onOptionsItemSelected(item);	    
	}	

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		  super.onCreateContextMenu(menu, v, menuInfo);
		  DosBoxMenuUtility.doCreateContextMenu(this, menu, v, menuInfo);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if (DosBoxMenuUtility.doContextItemSelected(this, item))
			return true;
	    return super.onOptionsItemSelected(item);	    
	}	

	void pauseDosBox(boolean pause) {
		if (pause) {
			mDosBoxThread.mDosBoxRunning = false;
			nativePause(1);
			if (mAudioDevice != null)
				mAudioDevice.pause();			
		}
		else {
			nativePause(0);
			mDosBoxThread.mDosBoxRunning = true;
			//will auto play audio when have data
			//if (mAudioDevice != null)
			//	mAudioDevice.play();		
		}
	}
	
	void initDosBox() {
		mAudioDevice = new DosBoxAudio(this);

		nativeInit(mDosBoxLauncher); 

		/*nativeSetOption(DosBoxMenuUtility.DOSBOX_OPTION_ID_CYCLES, mPrefCycles);
		nativeSetOption(DosBoxMenuUtility.DOSBOX_OPTION_ID_FRAMESKIP, mPrefFrameskip);
		nativeSetOption(DosBoxMenuUtility.DOSBOX_OPTION_ID_MEMORY_SIZE, mPrefMemorySize);
		nativeSetOption(DosBoxMenuUtility.DOSBOX_OPTION_ID_SOUND_MODULE_ON, (mPrefSoundModuleOn)?1:0);
		nativeSetOption(DosBoxMenuUtility.DOSBOX_OPTION_ID_REFRESH_HACK_ON, (mPrefRefreshHackOn)?1:0);
		nativeSetOption(DosBoxMenuUtility.DOSBOX_OPTION_ID_CYCLE_HACK_ON, (mPrefCycleHackOn)?1:0); */
		
		
	    Intent intent = getIntent();
	    String dosBoxConfigFile = intent.getStringExtra("conf");
	    String dosBoxConfigFileUser =intent.getStringExtra("userconf");
	    //String userCommandLine = intent.getStringExtra("cmdline");

		
		String argStartCommand = getIntent().getStringExtra(START_COMMAND_ID);
		
		if (argStartCommand == null) {
			argStartCommand = ""; 
		}

		nativeSetOption(DosBoxMenuUtility.DOSBOX_OPTION_ID_START_COMMAND, 0, argStartCommand, true);
		if (dosBoxConfigFile!=null) nativeSetOption(DosBoxMenuUtility.DOSBOX_OPTION_ID_DOSBOX_DEFAULT, 0, dosBoxConfigFile, true);
		if (dosBoxConfigFileUser!=null) nativeSetOption(DosBoxMenuUtility.DOSBOX_OPTION_ID_DOSBOX_USER, 0, dosBoxConfigFileUser, true);
		
		
		nativeSetOption(DosBoxMenuUtility.DOSBOX_OPTION_ID_MIXER_HACK_ON, (mPrefMixerHackOn)?1:0,null, true);
		nativeSetOption(DosBoxMenuUtility.DOSBOX_OPTION_ID_SOUND_MODULE_ON, (mPrefSoundModuleOn)?1:0,null, true);
		nativeSetOption(DosBoxMenuUtility.DOSBOX_OPTION_ID_AUTO_CPU_ON, 0,null, true);
		

		mDosBoxThread = new DosBoxThread(this);
	}
	
	void shutDownDosBox() {
		boolean retry;
		retry = true;
		while (retry) {
			try {
				mDosBoxThread.join();
				retry =	false;
			}
			catch (InterruptedException e) { // try again shutting down the thread
			}
		}		
		nativeShutDown();

		if (mAudioDevice != null) {
			mAudioDevice.shutDownAudio();
			mAudioDevice = null;
		}
		mDosBoxThread = null;
	}	

	void startDosBox() {
		if (mDosBoxThread != null)
			mDosBoxThread.start();		
		
		if ((mSurfaceView != null) && (mSurfaceView.mVideoThread != null))
			mSurfaceView.mVideoThread.start();
	}
	
	void stopDosBox() {
		nativePause(0);//it won't die if not running
		
		//stop audio AFTER above
		if (mAudioDevice != null)
			mAudioDevice.pause();
		
		mSurfaceView.mVideoThread.setRunning(false);
		
		nativeStop();		
	}
	
	public void callbackExit() {
		if (mDosBoxThread != null)
			mDosBoxThread.doExit();
	}

	public void callbackVideoRedraw( int w, int h, int s, int e) {
		mSurfaceView.mSrc_width = w;
		mSurfaceView.mSrc_height = h;
		synchronized (mSurfaceView.mDirty) {
			if (mSurfaceView.mDirty) {
				mSurfaceView.mStartLine = Math.min(mSurfaceView.mStartLine, s);
				mSurfaceView.mEndLine = Math.max(mSurfaceView.mEndLine, e);				
			}
			else {
				mSurfaceView.mStartLine = s;
				mSurfaceView.mEndLine = e;
			}
			mSurfaceView.mDirty = true;
		}
	}

	public Bitmap callbackVideoSetMode( int w, int h) {
		mSurfaceView.mSrc_width = w;
		mSurfaceView.mSrc_height = h;
		mSurfaceView.resetScreen(false);
		Bitmap newBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
		if (newBitmap != null) {
			mSurfaceView.mBitmap = null;
			mSurfaceView.mBitmap = newBitmap;
			
			//locnet, 2011-04-28, support 2.1 or below
			mSurfaceView.mVideoBuffer = null;
			mSurfaceView.mVideoBuffer = ByteBuffer.allocateDirect(w * h * 2);
			
			return mSurfaceView.mBitmap;
		}
		return null;
	}
	
	//locnet, 2011-04-28, support 2.1 or below
	public Buffer callbackVideoGetBuffer() {
		if (mSurfaceView != null)  {
			//if (mSurfaceView.mVideoBuffer != null)
			//	mSurfaceView.mVideoBuffer.position(0);
			return mSurfaceView.mVideoBuffer;
		}
		else
			return null;
	} 
	
	public int callbackAudioInit(int rate, int channels, int encoding, int bufSize) {
		if (mAudioDevice != null)
			return mAudioDevice.initAudio(rate, channels, encoding, bufSize);
		else
			return 0;
	}
	
	public void callbackAudioWriteBuffer(int size) {
		if (mAudioDevice != null)
			mAudioDevice.AudioWriteBuffer(size);		
	} 

	public short[] callbackAudioGetBuffer() {
		if (mAudioDevice != null)
			return mAudioDevice.mAudioBuffer;
		else
			return null;
	}
	
	class DosBoxThread extends Thread {
		DosBoxLauncher mParent;
		public boolean	mDosBoxRunning = false;

		DosBoxThread(DosBoxLauncher parent) {
			mParent =  parent;
		}
		
		public void run() {
			mDosBoxRunning = true;
			Log.i("DosBoxTurbo", "Using DosBox Config: "+mConfPath+mConfFile);
			nativeStart(mSurfaceView.mBitmap, mSurfaceView.mBitmap.getWidth(), mSurfaceView.mBitmap.getHeight(), mConfPath+mConfFile);
			//will never return to here;
		}
		
		public void doExit() {			
			if (mSurfaceView != null) {
				InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
				if (imm != null) {
					imm.hideSoftInputFromWindow(mSurfaceView.getWindowToken(), 0);
				}
			}
			
			mDosBoxRunning = false;
			mParent.finish();						
		}		
	}
	
	public static Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Toast.makeText(mDosBoxLauncher,msg.getData().getString("msg") , Toast.LENGTH_LONG).show();
		    //do something in the user interface to display data from message
	  	}
	};
	/*
	//called by the native activity when ever touch input is found
	public void OnNativeMotion(int action, int x, int y, int source, int device_id) {
		if(source == 1048584){	//touchpad
			// Obtain MotionEvent object
			long downTime = SystemClock.uptimeMillis();
			long eventTime = SystemClock.uptimeMillis() + 100;
			// 	List of meta states found here: developer.android.com/reference/android/view/KeyEvent.html#getMetaState()
			int metaState = 0;
			MotionEvent motionEvent = MotionEvent.obtain( downTime, eventTime, action, x, (366-y), metaState);
			mSurfaceView.onTouchPadEvent(motionEvent);	//custom made method for dealing with touch input
		}
		/*else{
			// Obtain MotionEvent object
			long downTime = SystemClock.uptimeMillis();
			long eventTime = SystemClock.uptimeMillis() + 100;
			// List of meta states found here: developer.android.com/reference/android/view/KeyEvent.html#getMetaState()
			int metaState = 0;
			MotionEvent motionEvent = MotionEvent.obtain(downTime, eventTime, action,x, y, metaState);
			// Dispatch touch event to view
			mGLSurfaceView.dispatchTouchEvent(motionEvent);
		}*/
	//}
	
}


