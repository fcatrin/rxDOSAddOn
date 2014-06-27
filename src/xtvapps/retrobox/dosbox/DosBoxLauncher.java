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

import retrobox.vinput.Mapper;
import retrobox.vinput.Mapper.ShortCut;
import retrobox.vinput.VirtualEvent.MouseButton;
import retrobox.vinput.VirtualEventDispatcher;
import retrobox.vinput.overlay.ExtraButtons;
import retrobox.vinput.overlay.Overlay;
import xtvapps.retrobox.dosbox.library.dosboxprefs.DosBoxPreferences;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
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
	
	public boolean mPrefRefreshHackOn = true;
	public boolean mPrefCycleHackOn = true;
	// TODO manejar desde config de video en RetroBox
	public boolean mPrefScaleFilterOn = false;
	
	// TODO manejar desde config de videojuego (ej. Commander Keen)
	public boolean mPrefFullScreenUpdate = false;
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
	public boolean isMouseOnly = false;
	public boolean useRealJoystick = false;
	
	static Mapper mapper;
	private VirtualEventDispatcher virtualEventDispatcher;
	
	
	private static boolean useKeyTranslation = false;
    
    // gives the native activity a copy of this object so it can call OnNativeMotion
    //public native int RegisterThis();
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i("DosBoxTurbo", "onCreate()");
		
		useRealJoystick = getIntent().getBooleanExtra("realJoystick", false);
		isMouseOnly  = getIntent().getBooleanExtra("mouseOnly", false);

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

		// load real joystick translation
		Log.d("JSTICK", "useRealJoystick is " + useRealJoystick);
		virtualEventDispatcher = new VirtualInputDispatcher();
		mapper = new Mapper(getIntent(), virtualEventDispatcher);
		
		int mouseWarpX = getIntent().getIntExtra("warpX", 100);
		int mouseWarpY = getIntent().getIntExtra("warpY", 100);
		if (mouseWarpX>0) mSurfaceView.warpX = mouseWarpX / 100.0f;
		if (mouseWarpY>0) mSurfaceView.warpY = mouseWarpY / 100.0f;
		
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
	        	int w = mSurfaceView.getWidth();
	        	int h = mSurfaceView.getHeight();
	        	Overlay.initJoystickOverlay(w, h);
	    	    ExtraButtons.initExtraButtons(DosBoxLauncher.this, getIntent().getStringExtra("buttons"), mSurfaceView.getWidth(), mSurfaceView.getHeight(), isMouseOnly);
	    		
	            Log.v("DosBoxTurbo",
	                    String.format("new width=%d; new height=%d", mSurfaceView.getWidth(),
	                            mSurfaceView.getHeight()));
	        }
	    });

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
	
	
	class VirtualInputDispatcher implements VirtualEventDispatcher {

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
			// TODO Auto-generated method stub
			return false;
		}
		
	}
	
}

