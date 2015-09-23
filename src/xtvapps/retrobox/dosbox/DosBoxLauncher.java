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
import xtvapps.retrobox.dosbox.library.dosboxprefs.DosBoxPreferences;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.Menu;
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

	
	public String mPID = null;
	//public String mPrefKeyMapping = "abc";
	public int mPrefCycles = 3000; 
	public int mPrefFrameskip = 2; 
	public int mPrefMemorySize = 4; 
	public int mPrefScaleFactor = 100;
	public boolean isMouseOnly = false;
	public boolean useRealJoystick = false;
	
	static Mapper mapper;
	private VirtualEventDispatcher virtualEventDispatcher;
	
	static GamepadController gamepadController;
	static GamepadView gamepadView;
	static ExtraButtonsController extraButtonsController;
	static ExtraButtonsView extraButtonsView;
	public static final Overlay overlay = new Overlay();
	
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
        for(int i=0; i<2; i++) {
        	String prefix = "j" + (i+1);
        	String deviceDescriptor = getIntent().getStringExtra(prefix + "DESCRIPTOR");
        	Mapper.registerGamepad(i, deviceDescriptor);
        }
        
		useRealJoystick = Mapper.hasGamepads();
		Log.d("JSTICK", "useRealJoystick is " + useRealJoystick);
		isMouseOnly  = getIntent().getBooleanExtra("mouseOnly", false);
		//testingMode = getIntent().getBooleanExtra("testingMode", false);
		
		View main = getLayoutInflater().inflate(R.layout.main, null);
		setContentView(main);

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

		BitmapDrawable splash = (BitmapDrawable) getResources().getDrawable(R.drawable.splash);
		splash.setTargetDensity(120);
		splash.setGravity(Gravity.CENTER);		
		mSurfaceView.setBackgroundDrawable(splash);

		setupGamepadOverlay(root);

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
		
		
		
	}
	
	public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) new Handler().postDelayed(new Runnable(){
			@Override
			public void run() {
				setImmersiveMode();
			}
		}, 5000);
	}

	@TargetApi(Build.VERSION_CODES.KITKAT)
	private void setImmersiveMode() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			getWindow().getDecorView().setSystemUiVisibility(
		            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
		            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
		            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
		            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
		            | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
		            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
		} else {
			
		}
	}
	
	private boolean needsOverlay() {
		return !Mapper.hasGamepads();
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
		testingMode = DosBoxCustomConfig.getBoolean("testmode", false);
		
		// Full Screen update
		mPrefFullScreenUpdate = DosBoxCustomConfig.getBoolean("fsupdate", mPrefFullScreenUpdate);
		
		// SCALE MODE
		mPrefScaleFilterOn = DosBoxCustomConfig.getBoolean("videofilter", mPrefScaleFilterOn);
		 
		// ASPECT Ratio 
		mSurfaceView.mMaintainAspect = getIntent().getBooleanExtra("keepAspect",  true);
		
		mPrefFrameskip = DosBoxCustomConfig.getInt("frameskip", mPrefFrameskip);
		turboCycles = DosBoxCustomConfig.getBoolean("turboCycle", turboCycles);
		turboVGA    = DosBoxCustomConfig.getBoolean("turboVGA", turboCycles);
		turboAudio  = DosBoxCustomConfig.getBoolean("turboAudio", turboCycles);
		mPrefAutoCPUOn = DosBoxCustomConfig.getBoolean("autocpu", mPrefAutoCPUOn);
		
		DosBoxLauncher.nativeSetOption(DosBoxMenuUtility.DOSBOX_OPTION_ID_CYCLES, mPrefCycles, null, true);
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
	
	protected void toastMessage(String message) {
    	Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
	@Override
	public void onBackPressed() {
		openOptionsMenu();
	}
	
    static final private int TOGGLE_BUTTONS_ID = Menu.FIRST +1;
    static final private int TOGGLE_FILTER_ID = Menu.FIRST +2;
    static final private int MORE_CYCLES_ID = Menu.FIRST +3;
    static final private int AUTO_CYCLES_ID = Menu.FIRST +4;
    static final private int LESS_CYCLES_ID = Menu.FIRST +5;
    static final private int MORE_FRAMESKIP_ID = Menu.FIRST +6;
    static final private int LESS_FRAMESKIP_ID = Menu.FIRST +7;
    static final private int QUIT_ID = Menu.FIRST +8;
    static final private int TURBO_VGA_ID = Menu.FIRST +9;
    static final private int TURBO_CYCLES_ID = Menu.FIRST +10;
    static final private int TURBO_AUDIO_ID = Menu.FIRST +11;
    static final private int FULLSCREEN_UPDATE_ID = Menu.FIRST +12;
    static final private int OVERLAY_ID = Menu.FIRST + 13;
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        if (OverlayExtra.hasExtraButtons()) {
        	menu.add(0, TOGGLE_BUTTONS_ID, 0, "Extra Buttons");
        }
        
        if (needsOverlay()) {
        	menu.add(0, OVERLAY_ID, 0, "Overlay ON/OFF");
        }
        	
        if (testingMode) {
	        menu.add(0, TOGGLE_FILTER_ID, 0, "Toggle Video Filter");
	        menu.add(0, MORE_FRAMESKIP_ID, 0, "More Frameskip");
	        menu.add(0, LESS_FRAMESKIP_ID, 0, "Less Frameskip");
	        menu.add(0, MORE_CYCLES_ID, 0, "More Cycles");
	        menu.add(0, AUTO_CYCLES_ID, 0, "Auto Cycles");
	        menu.add(0, LESS_CYCLES_ID, 0, "Less Cycles");
	        menu.add(0, TURBO_VGA_ID, 0, "Toggle Turbo VGA");
	        menu.add(0, TURBO_CYCLES_ID, 0, "Toggle Turbo Cycles");
	        menu.add(0, TURBO_AUDIO_ID, 0, "Toggle Turbo Audio");
	        menu.add(0, FULLSCREEN_UPDATE_ID, 0, "Toggle FullScreen Update");
        }
        menu.add(0, QUIT_ID, 0, "Quit");
        
        return true;
    }
    
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
    	if (item != null) {
	        switch (item.getItemId()) {
	        case TOGGLE_BUTTONS_ID : uiToggleButtons(); return true;
	        case TOGGLE_FILTER_ID : uiToggleFilter(); return true;
	        case OVERLAY_ID : uiToggleOverlay(); return true;
	        case MORE_FRAMESKIP_ID : uiFrameskipMore(); return true;
	        case LESS_FRAMESKIP_ID : uiFrameskipLess(); return true;
	        case MORE_CYCLES_ID : uiCyclesMore(); return true;
	        case AUTO_CYCLES_ID : uiCyclesAuto(); return true;
	        case LESS_CYCLES_ID : uiCyclesLess(); return true;
	        case TURBO_VGA_ID : uiTurboVGA(); return true;
	        case TURBO_CYCLES_ID : uiTurboCycles(); return true;
	        case TURBO_AUDIO_ID : uiTurboAudio(); return true;
	        case FULLSCREEN_UPDATE_ID : uiToggleFullScreenUpdate(); return true;
	        case QUIT_ID : uiQuit(); return true;
	        }
    	}
        return super.onMenuItemSelected(featureId, item);
    }
    
    @Override
	public boolean onMenuOpened(int featureId, Menu menu) {
		onPause();
		return super.onMenuOpened(featureId, menu);
	}

	@Override
	public void onOptionsMenuClosed(Menu menu) {
		onResume();
		super.onOptionsMenuClosed(menu);
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

	protected void uiCyclesMore() {
		mPrefCycles += 1000; 
		uiUpdateCycles();
	}

	protected void uiCyclesAuto() {
		mPrefCycles = 3000; 
		uiUpdateCycles();
	}

	protected void uiCyclesLess() {
		if (mPrefCycles<=1000) return;
		
		mPrefCycles -= 1000; 
		uiUpdateCycles();
	}
	
	protected void uiUpdateCycles() {
		DosBoxLauncher.nativeSetOption(DosBoxMenuUtility.DOSBOX_OPTION_ID_CYCLES, mPrefCycles,null,true);
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
		public void sendAnalog(Analog index, double x, double y) {}

		@Override
		public void sendKey(int keyCode, boolean down) {
			DosBoxControl.sendNativeKey(keyCode, down, false, false, false);
		}

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
			case MENU : if (!down) openOptionsMenu(); return true;
			default:
				return false;
			}
		}

	
	}
	
}


