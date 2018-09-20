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

package xtvapps.retrobox.dosbox;

import java.io.File;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;
import retrobox.utils.GamepadInfoDialog;
import retrobox.utils.ImmersiveModeSetter;
import retrobox.utils.ListOption;
import retrobox.utils.RetroBoxDialog;
import retrobox.utils.RetroBoxUtils;
import retrobox.vinput.GenericGamepad;
import retrobox.vinput.GenericGamepad.Analog;
import retrobox.vinput.Mapper;
import retrobox.vinput.Mapper.ShortCut;
import retrobox.vinput.QuitHandler;
import retrobox.vinput.QuitHandler.QuitHandlerCallback;
import retrobox.vinput.VirtualEvent.MouseButton;
import retrobox.vinput.VirtualEventDispatcher;
import retrobox.vinput.overlay.ExtraButtons;
import retrobox.vinput.overlay.ExtraButtonsController;
import retrobox.vinput.overlay.ExtraButtonsView;
import retrobox.vinput.overlay.GamepadController;
import retrobox.vinput.overlay.GamepadView;
import retrobox.vinput.overlay.Overlay;
import retrobox.vinput.overlay.OverlayExtra;
import xtvapps.core.AndroidFonts;
import xtvapps.core.Callback;
import xtvapps.core.SimpleCallback;
import xtvapps.core.Utils;
import xtvapps.core.content.KeyValue;
import xtvapps.retrobox.dosbox.library.dosboxprefs.DosBoxPreferences;
import xtvapps.retrobox.v2.dosbox.R;

  
public class DosBoxLauncher extends Activity {
	public static final String START_COMMAND_ID = "start_command";
	public String mConfFile = DosBoxPreferences.CONFIG_FILE;
	public String mConfPath = DosBoxPreferences.CONFIG_PATH;
	
	static { 
		System.loadLibrary("dosbox");
	}
	public native void nativeInit(Object ctx);
	public static native void nativeShutDown();
	public static native void nativeSetOption(int option, int value, String value2, boolean l);
	public native void nativeStart(Bitmap bitmap, int width, int height, String confPath, boolean invertRGB);
	public static native void nativePause(int state);
	public static native void nativeStop();
	public static native void nativePrefs();
	public static native boolean isARMv7();
	
	public DosBoxSurfaceView mSurfaceView = null;
	public DosBoxAudio mAudioDevice = null;
	public DosBoxThread mDosBoxThread = null;
	public SharedPreferences prefs;
	public static DosBoxLauncher mDosBoxLauncher = null;
	public boolean testingMode = true;
	
	// TODO manejar desde config de video en RetroBox
	public boolean mPrefScaleFilterOn = false;
	
	// TODO manejar desde config de videojuego (ej. Commander Keen)
	public boolean mPrefFullScreenUpdate = false;
	public boolean mPrefSoundModuleOn = true;
	public boolean mPrefAutoCPUOn = true;
	public boolean mTurboOn = false;
	
	static boolean turboCycles = true;
	static boolean turboVGA = true;
	static boolean turboAudio = true;
	static boolean turboCPU = false;

	
	public String mPID = null;
	//public String mPrefKeyMapping = "abc";
	public int mPrefCycles = 3000; 
	public int mPrefFrameskip = 2; 
	public int mPrefMemorySize = 4; 
	public int mPrefScaleFactor = 100;
	public boolean isMouseOnly = false;
	public boolean useRealJoystick = false;
	public boolean isRealMouse = false;
	
	static Mapper mapper;
	private VirtualEventDispatcher virtualEventDispatcher;
	
	static GamepadController gamepadController;
	static GamepadView gamepadView;
	static ExtraButtonsController extraButtonsController;
	static ExtraButtonsView extraButtonsView;
	public static final Overlay overlay = new Overlay();
	private GamepadInfoDialog gamepadInfoDialog;
	
	private static boolean useKeyTranslation = false;
    
    // gives the native activity a copy of this object so it can call OnNativeMotion
    //public native int RegisterThis();
    
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i("DosBoxTurbo", "onCreate()");
		
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setImmersiveMode();
        
		// load real joystick translation
		virtualEventDispatcher = new VirtualInputDispatcher();
		mapper = new Mapper(getIntent(), virtualEventDispatcher);
		Mapper.initGestureDetector(this);
		Mapper.joinPorts = getIntent().getBooleanExtra("joinPorts", false);
		
        for(int i=0; i<2; i++) {
        	String prefix = "j" + (i+1);
        	String deviceDescriptor = getIntent().getStringExtra(prefix + "DESCRIPTOR");
        	Mapper.registerGamepad(i, deviceDescriptor);
        }
        
		useRealJoystick = Mapper.hasGamepads();
		Log.d("JSTICK", "useRealJoystick is " + useRealJoystick);
		isMouseOnly  = getIntent().getBooleanExtra("mouseOnly", false);
		//testingMode = getIntent().getBooleanExtra("testingMode", false);
		
		setContentView(R.layout.main);
		
		AndroidFonts.setViewFontRecursive(findViewById(R.id.rootContainer), RetroBoxUtils.FONT_DEFAULT_M);

        gamepadInfoDialog = new GamepadInfoDialog(this);
        gamepadInfoDialog.loadFromIntent(getIntent());

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        Log.d("VIDEO", "metrics " + metrics.widthPixels + "x" + metrics.heightPixels);

		ViewGroup root = (ViewGroup)findViewById(R.id.root);
        Log.d("VIDEO", "root " + root.getMeasuredWidth() + "x" + root.getMeasuredHeight());
        
        View decor = getWindow().getDecorView();
        Log.d("VIDEO", "decor " + decor.getMeasuredWidth() + "x" + decor.getMeasuredHeight());
        
		mSurfaceView = new DosBoxSurfaceView(this);
		root.addView(mSurfaceView);
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

		gamepadController = new GamepadController();
		gamepadView = new GamepadView(this, overlay);
		extraButtonsController = new ExtraButtonsController();
		extraButtonsView = new ExtraButtonsView(this);
		
		int mouseWarpX = getIntent().getIntExtra("warpX", 100);
		int mouseWarpY = getIntent().getIntExtra("warpY", 100);
		if (mouseWarpX>0) mSurfaceView.warpX = mouseWarpX / 100.0f;
		if (mouseWarpY>0) mSurfaceView.warpY = mouseWarpY / 100.0f;
		
		DosBoxMenuUtility.loadPreference(this,prefs);	

		setupGamepadOverlay(root);

		initDosBox();
		startDosBox();
		Log.i("DosBoxTurbo","onCreate ends");
	}
	
	public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) ImmersiveModeSetter.postImmersiveMode(new Handler(), getWindow(), isLayoutStable());
	}

	private void setImmersiveMode() {
		ImmersiveModeSetter.get().setImmersiveMode(getWindow(), isLayoutStable());
	}
	
	private boolean needsOverlay() {
		return !Mapper.hasGamepads();
	}
	
	private boolean isLayoutStable() {
		return Mapper.hasGamepads();
	}
	
	private void setupGamepadOverlay(ViewGroup root) {
		ViewTreeObserver observer = root.getViewTreeObserver();
		observer.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				int w = mSurfaceView.getWidth();
				int h = mSurfaceView.getHeight();
				Log.d("VIDEO", "global layout " + w + "x" + h);
				if (needsOverlay()) {
					String overlayConfig = getIntent().getStringExtra("OVERLAY");
					float alpha = getIntent().getFloatExtra("OVERLAY_ALPHA", 0.8f);
					if (overlayConfig!=null) overlay.init(overlayConfig, w, h, alpha);
				}
				Log.d("REMAP", "addExtraButtons : " + getIntent().getStringExtra("buttons"));
				ExtraButtons.initExtraButtons(DosBoxLauncher.this, getIntent().getStringExtra("buttons"), mSurfaceView.getWidth(), mSurfaceView.getHeight(), isMouseOnly);
				}
			});
		
		Log.d("OVERLAY", "setupGamepadOverlay");
		if (needsOverlay()) {
			Log.d("OVERLAY", "has Overlay");
			gamepadView.addToLayout(root);
			gamepadView.showPanel();
		}
		Log.d("OVERLAY", "extraButtonsView.addToLayout");
		extraButtonsView.addToLayout(root);
		boolean hideExtraButtons = !getIntent().getBooleanExtra("showExtraButtons", false);
		if (hideExtraButtons) extraButtonsView.hidePanel();
		else extraButtonsView.showPanel();
	}
	
	
	// splash can go here

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
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        Log.d("VIDEO", "metrics2 " + metrics.widthPixels + "x" + metrics.heightPixels);

		ViewGroup root = (ViewGroup)findViewById(R.id.root);
        Log.d("VIDEO", "root2 " + root.getMeasuredWidth() + "x" + root.getMeasuredHeight());
        
        View decor = getWindow().getDecorView();
        Log.d("VIDEO", "decor2 " + decor.getMeasuredWidth() + "x" + decor.getMeasuredHeight());

        Log.d("VIDEO", "mSurfaceView " + mSurfaceView.getMeasuredWidth() + "x" + mSurfaceView.getMeasuredHeight());

        

		super.onResume();
		ImmersiveModeSetter.postImmersiveMode(new Handler(), getWindow(), isLayoutStable());
		
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

		/*
		if (mTurboOn) {
			Toast.makeText(this, "Fast Forward", Toast.LENGTH_SHORT).show();
		} else {			
			if (DosBoxControl.nativeGetAutoAdjust()) { 
				Toast.makeText(this, "Auto Cycles ["+DosBoxControl.nativeGetCycleCount() +"%]", Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(this, "DosBox Cycles: "+DosBoxControl.nativeGetCycleCount(), Toast.LENGTH_SHORT).show();
			}
		}
		*/
		
		Log.i("DosBoxTurbo","onResume");
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
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
		
		DosBoxCustomConfig.init(dosBoxConfigFile);
		DosBoxCustomConfig.init(dosBoxConfigFileUser);
		loadCustomPreferences();

		mDosBoxThread = new DosBoxThread(this);
	}
	
	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		if (gamepadView.isVisible() && gamepadController.onTouchEvent(ev)) {
			Log.d("TOUCH", "dispatched to gamepadController");
			if (Overlay.requiresRedraw) {
				Overlay.requiresRedraw = false;
				gamepadView.invalidate();
			}
			return true;
		}
		
		if (extraButtonsView.isVisible() && extraButtonsController.onTouchEvent(ev)) {
			if (OverlayExtra.requiresRedraw) {
				OverlayExtra.requiresRedraw = false;
				extraButtonsView.invalidate();
			}
			return true;
		}
		
		mapper.onTouchEvent(ev);
		
		return super.dispatchTouchEvent(ev);
	}
	void loadCustomPreferences() {
		// SCREEN SCALE FACTOR
		mPrefScaleFactor = 100;
		
		// Test mode
		testingMode = getIntent().getBooleanExtra("testingMode", false);
		
		// Full Screen update
		mPrefFullScreenUpdate = DosBoxCustomConfig.getBoolean("fsupdate", mPrefFullScreenUpdate);
		
		mPrefScaleFilterOn = false; // TOO SLOW when active :-( getIntent().getBooleanExtra("linearFilter", true);
		 
		// ASPECT Ratio 
		mSurfaceView.mMaintainAspect = getIntent().getBooleanExtra("keepAspect",  true);
		
		mPrefFrameskip = DosBoxCustomConfig.getInt("frameskip", mPrefFrameskip);
		turboCycles = DosBoxCustomConfig.getBoolean("turboCycle", turboCycles);
		turboVGA    = DosBoxCustomConfig.getBoolean("turboVGA", turboCycles);
		turboAudio  = DosBoxCustomConfig.getBoolean("turboAudio", turboCycles);
		mPrefAutoCPUOn = DosBoxCustomConfig.getBoolean("autocpu", mPrefAutoCPUOn);
		
		// this now come from dosbox.conf or rbx menu
		// DosBoxLauncher.nativeSetOption(DosBoxMenuUtility.DOSBOX_OPTION_ID_CYCLES, mPrefCycles, null, true);
		
		DosBoxLauncher.nativeSetOption(DosBoxMenuUtility.DOSBOX_OPTION_ID_FRAMESKIP, mPrefFrameskip ,null, true);		
		
		// TURBO CYCLE
		DosBoxLauncher.nativeSetOption(DosBoxMenuUtility.DOSBOX_OPTION_ID_CYCLE_HACK_ON, turboCycles?1:0,null,true);
		// TURBO VGA
		DosBoxLauncher.nativeSetOption(DosBoxMenuUtility.DOSBOX_OPTION_ID_REFRESH_HACK_ON, turboVGA?1:0,null,true);
		// TURBO AUDIO
		DosBoxLauncher.nativeSetOption(DosBoxMenuUtility.DOSBOX_OPTION_ID_MIXER_HACK_ON, turboAudio?1:0,null,true);

		DosBoxLauncher.nativeSetOption(DosBoxMenuUtility.DOSBOX_OPTION_ID_SOUND_MODULE_ON, mPrefSoundModuleOn?1:0,null,true);
		// AUTO CPU 
		DosBoxLauncher.nativeSetOption(DosBoxMenuUtility.DOSBOX_OPTION_ID_AUTO_CPU_ON, mPrefAutoCPUOn?1:0,null,true);

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
			
			boolean invertRGB = getIntent().getBooleanExtra("invertRGB", false);
			nativeStart(mSurfaceView.mBitmap, mSurfaceView.mBitmap.getWidth(), mSurfaceView.mBitmap.getHeight(), mConfPath+mConfFile, invertRGB);
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
	
	protected void toastMessage(String message) {
    	Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
	@Override
	public void onBackPressed() {
		if (RetroBoxDialog.cancelDialog(this)) return;
		
		openRetroBoxMenu();
	}
	
	Map<Integer, String> cpuCycles = new LinkedHashMap<Integer, String>();
	private void setupCpuCycles() {
		if (!cpuCycles.isEmpty()) return;
		
		cpuCycles.put(0, "Auto");
		cpuCycles.put(-1, "Max");
		cpuCycles.put(3000, "Slow 386 (3000 cycles)");
		cpuCycles.put(6000, "Fast 386 (6000 cycles)");
		cpuCycles.put(9000, "Slow 486 (9000 cycles)");
		cpuCycles.put(15000, "Fast 486 (15000 cycles)");
		cpuCycles.put(20000, "Faster 486 (20000 cycles)");
		cpuCycles.put(25000, "Pentium (25000 cycles)");
		
	}
	
	private String getCpuCyclesName() {
		setupCpuCycles();
		int cycles = DosBoxControl.nativeGetCPUCycles();
		String cpuCyclesName = cpuCycles.get(cycles);
		if (cpuCyclesName == null) {
			cpuCyclesName = cycles + " cycles";
		}
		return cpuCyclesName;
	}
	
	private void uiCPUSettings() {
		List<ListOption> options = new ArrayList<ListOption>();
        // options.add(new ListOption("cycles+", "More Cycles"));
        // options.add(new ListOption("cycles-", "Less Cycles"));
        
        boolean isOneActive = false;
        final int cycles = DosBoxControl.nativeGetCPUCycles();
        for(Entry<Integer, String> entry : cpuCycles.entrySet()) {
        	boolean selected = entry.getKey() == cycles;
        	isOneActive |= selected;
        	options.add(new ListOption(String.valueOf(entry.getKey()), entry.getValue(), selected?"Active":""));
        }
        
        String title = "CPU Settings" + (isOneActive?"":" (" + cycles + " cycles)");
        
        RetroBoxDialog.showListDialog(this, title, options, new Callback<KeyValue>() {
			
			@Override
			public void onResult(KeyValue result) {
				String key = result.getKey();
				if (key.equals("cycles+")) {
					uiCyclesMore(cycles);
				} else if (key.equals("cycles-")) {
					uiCyclesLess(cycles);
				} else {
					int newCycles = Utils.str2i(key);
					if (newCycles < 0) {
						uiCPUMaxCycles();
					} else if (newCycles == 0) {
						uiAutoCycles();
					} else {
						mPrefCycles = newCycles;
						uiUpdateCycles();
					}
				}
				onResume();
			}

			@Override
			public void onError() {
				super.onError();
				onResume();
			}
		});
	}
	
	private void openRetroBoxMenu() {
		onPause();
		
		List<ListOption> options = new ArrayList<ListOption>();
		
        options.add(new ListOption("", "Cancel"));
        
        if (OverlayExtra.hasExtraButtons()) {
            options.add(new ListOption("extra", "Extra Buttons"));
        }
        
        if (testingMode) {
            options.add(new ListOption("fullscreenUpdate", "DEVEL - Toggle FullScreen Update"));
        }
        options.add(new ListOption("cpu", "CPU settings", getCpuCyclesName()));
        options.add(new ListOption("help", "Help"));
        options.add(new ListOption("quit", "Quit"));
        
        RetroBoxDialog.showListDialog(this, getString(R.string.emu_opt_title), options, new Callback<KeyValue>() {
			
			@Override
			public void onResult(KeyValue result) {
				String key = result.getKey();
				if (key.equals("quit")) {
					uiQuit();
				} else if (key.equals("extra")) {
					uiToggleButtons();
				} else if (key.equals("filter")) {
					uiToggleFilter();
				} else if (key.equals("overlay")) {
					uiToggleOverlay();
				} else if (key.equals("frame+")) {
					uiFrameskipMore();
				} else if (key.equals("frame-")) {
					uiFrameskipLess();
				} else if (key.equals("turboVGA")) {
					uiTurboVGA();
				} else if (key.equals("turboCPU")) {
					uiTurboCycles();
				} else if (key.equals("turboAudio")) {
					uiTurboAudio();
				} else if (key.equals("unlockCPU")) {
					uiUnlockSpeed();
				} else if (key.equals("autoCPU")) {
					uiAutoCycles();
				} else if (key.equals("fullscreenUpdate")) {
					uiToggleFullScreenUpdate();
				} else if (key.equals("cpu")) {
					uiCPUSettings();
					return;
				} else if (key.equals("help")) {
					uiHelp();
					return;
				}
				onResume();
			}

			@Override
			public void onError() {
				super.onError();
				onResume();
			}
			
			
		});

	}
	
    protected void uiHelp() {
		RetroBoxDialog.showGamepadDialogIngame(this, gamepadInfoDialog, new SimpleCallback() {
			@Override
			public void onResult() {
				onResume();
			}
		});
    }
	
	protected void uiToggleButtons() {
		extraButtonsView.toggleView();
	}
	
	private void uiToggleOverlay() {
		gamepadView.toggleView();
	}
	
	protected void uiToggleFilter() {
		mPrefScaleFilterOn = !mPrefScaleFilterOn;
		mSurfaceView.forceRedraw();
	}

	protected void uiToggleFullScreenUpdate() {
		mPrefFullScreenUpdate = !mPrefFullScreenUpdate;
		mSurfaceView.forceRedraw();
		toastMessage("Fullscreen update is " + (mPrefFullScreenUpdate?"on":"off"));
	}
	
	protected void uiFrameskipMore() {
		mPrefFrameskip++;
		uiUpdateFrameskip();
	}

	protected void uiFrameskipLess() {
		if (mPrefFrameskip == 0) return;
		
		mPrefFrameskip--;
		uiUpdateFrameskip();
	}
	
	protected void uiUpdateFrameskip() {
		DosBoxLauncher.nativeSetOption(DosBoxMenuUtility.DOSBOX_OPTION_ID_FRAMESKIP, mPrefFrameskip ,null, true);
		toastMessage("Frameskip: " + mPrefFrameskip);
	}

	protected void uiCyclesMore(int current) {
		mPrefCycles = (current>0?current:3000) + 1000; 
		uiUpdateCycles();
	}

	protected void uiCyclesLess(int current) {
		mPrefCycles = (current>0?current:3000) - 1000;
		if (mPrefCycles<1000) mPrefCycles = 1000;
		uiUpdateCycles();
	}
	
	protected void uiAutoCycles() {
		DosBoxLauncher.nativeSetOption(DosBoxMenuUtility.DOSBOX_OPTION_ID_CPU_AUTO, 1,null,true);
		toastMessage("Auto CPU is ON");
	}

	protected void uiCPUMaxCycles() {
		DosBoxLauncher.nativeSetOption(DosBoxMenuUtility.DOSBOX_OPTION_ID_CPU_MAX, 1,null,true);
		toastMessage("CPU set as max cycles");
	}

	protected void uiUpdateCycles() {
		DosBoxLauncher.nativeSetOption(DosBoxMenuUtility.DOSBOX_OPTION_ID_CPU_FIXED, mPrefCycles,null,true);
		toastMessage("CPU Cycles: " + mPrefCycles);
	}
	
	protected void uiTurboCycles() {
		turboCycles = !turboCycles;
		DosBoxLauncher.nativeSetOption(DosBoxMenuUtility.DOSBOX_OPTION_ID_CYCLE_HACK_ON, turboCycles?1:0,null,true);
		toastMessage("Turbo Cycles is " + (turboCycles?"on":"off"));
	}
	protected void uiTurboVGA() {
		turboVGA = !turboVGA;
		DosBoxLauncher.nativeSetOption(DosBoxMenuUtility.DOSBOX_OPTION_ID_REFRESH_HACK_ON, turboVGA?1:0,null,true);
		toastMessage("Turbo VGA is " + (turboVGA?"on":"off"));
	}
	protected void uiTurboAudio() {
		turboAudio = !turboAudio;
		DosBoxLauncher.nativeSetOption(DosBoxMenuUtility.DOSBOX_OPTION_ID_MIXER_HACK_ON, turboAudio?1:0,null,true);
		toastMessage("Turbo Audio is " + (turboAudio?"on":"off"));
	}
	
	protected void uiUnlockSpeed() {
		turboCPU = !turboCPU;
		DosBoxLauncher.nativeSetOption(DosBoxMenuUtility.DOSBOX_OPTION_ID_TURBO_ON, turboCPU?1:0, null,true);
		toastMessage("Turbo CPU is " + (turboCPU?"on":"off"));
	}
	
    protected void uiQuit() {
    	stopDosBox();
    }
    
	public void uiQuitConfirm() {
		QuitHandler.askForQuit(this, new QuitHandlerCallback() {
			@Override
			public void onQuit() {
				uiQuit();
			}
		});
	}

	
	class VirtualInputDispatcher implements VirtualEventDispatcher {

		@Override
		public void sendMouseButton(MouseButton button, boolean down) {
			int action = down?DosBoxSurfaceView.ACTION_DOWN:DosBoxSurfaceView.ACTION_UP;
			int btn = button == MouseButton.LEFT?DosBoxSurfaceView.BTN_A:DosBoxSurfaceView.BTN_B;
			DosBoxControl.nativeMouse(0, 0, 0, 0, action, btn);
		}

		@Override
		public boolean handleShortcut(ShortCut shortcut, boolean down) {
			switch(shortcut) {
			case EXIT: if (!down) uiQuitConfirm(); return true;
			case MENU : if (!down) openRetroBoxMenu(); return true;
			default:
				return false;
			}
		}

		@Override
		public void sendKey(GenericGamepad gamepad, int keyCode, boolean down) {
			DosBoxControl.sendNativeKey(keyCode, down, false, false, false);
		}

		@Override
		public void sendAnalog(GenericGamepad gamepad, Analog index, double x, double y, double hatx, double haty) {
		}
	
	}
	
}


