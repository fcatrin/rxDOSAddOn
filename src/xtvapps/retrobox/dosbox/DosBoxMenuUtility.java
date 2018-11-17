/*
 *  Copyright (C) 2013 Fishstix (android.fishstix@gmail.com)
 *  
 *  Copyright (C) 2011 Locnet (android.locnet@gmail.com)
 *  
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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Scanner;
import xtvapps.res.dosbox.R;
import xtvapps.retrobox.dosbox.library.dosboxprefs.DosBoxPreferences;
import xtvapps.retrobox.dosbox.library.dosboxprefs.preference.GamePreference;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

public class DosBoxMenuUtility {
	
	/* settings from dosbox.conf
	 * 
	 * frameskip=0
	 * cycles=auto
	 * 
	 * 
	 * 
	 */
	
	public static String mPrefCycleString = "max";	// default slow system
	private static final Uri CONTENT_URI=Uri.parse("content://com.fishstix.dosboxlauncher.files/");
	private final static int JOYSTICK_CENTER_X = 0;
	private final static int JOYSTICK_CENTER_Y = 0;

	public static final int KEYCODE_ESCAPE = 111;
	public static final int KEYCODE_F1 = 131;
	//public static final int KEYCODE_F12 = 142;

	public final static int CONTEXT_MENU_SPECIAL_KEYS = 1;
	public final static int CONTEXT_MENU_CYCLES = 2;
	public final static int CONTEXT_MENU_FRAMESKIP = 3;
	public final static int CONTEXT_MENU_MEMORY_SIZE = 4;
	 
	//private final static int MENU_SETTINGS = 1;
	private final static int MENU_QUIT = 2;
	private final static int MENU_KEYBOARD_SPECIAL = 6;
	private final static int MENU_KEYBOARD = 7;
	//private final static int MENU_NUMPAD = 8;
	//private final static int MENU_BUTTON = 9;

	private final static int MENU_SETTINGS_SCALE = 24;
	
	private final static int MENU_INPUT_INPUT_METHOD = 45;
	
	private final static int MENU_KEYBOARD_CTRL = 61;
	private final static int MENU_KEYBOARD_ALT = 62;
	private final static int MENU_KEYBOARD_SHIFT = 63;
	
	private final static int MENU_KEYBOARD_ESC = 65;
	private final static int MENU_KEYBOARD_TAB = 66;
	
	private final static int MENU_KEYBOARD_F1 = 70;
	private final static int MENU_KEYBOARD_F12 = 81;
	private final static int MENU_KEYBOARD_SWAP_MEDIA = 91;
	private final static int MENU_KEYBOARD_TURBO = 92;

	private final static int MENU_PREFS = 96;
	
	private final static int MENU_CYCLE_1000 = 151;
	private final static int MENU_CYCLE_30000 = 180;

	private final static int MENU_FRAMESKIP_0 = 171;
	private final static int MENU_FRAMESKIP_10 = 181;

	private final static String PREF_KEY_FRAMESKIP = "dosframeskip";
	private final static String PREF_KEY_CYCLES = "doscycles";
	//private final static String PREF_KEY_KEY_MAPPING = "pref_key_key_mapping"; 
	
	public final static int INPUT_MOUSE = 0;
	public final static int INPUT_JOYSTICK = 1;
	public final static int INPUT_REAL_MOUSE = 2;
	public final static int INPUT_REAL_JOYSTICK = 3;
	public final static int INPUT_SCROLL = 4;
	
	//following must sync with AndroidOSfunc.cpp
	public final static int DOSBOX_OPTION_ID_SOUND_MODULE_ON = 1;
	public final static int DOSBOX_OPTION_ID_MEMORY_SIZE = 2;
	public final static int DOSBOX_OPTION_ID_CYCLES = 10;
	public final static int DOSBOX_OPTION_ID_FRAMESKIP = 11;
	public final static int DOSBOX_OPTION_ID_REFRESH_HACK_ON = 12;
	public final static int DOSBOX_OPTION_ID_CYCLE_HACK_ON = 13;
	public final static int DOSBOX_OPTION_ID_MIXER_HACK_ON = 14;
	public final static int DOSBOX_OPTION_ID_AUTO_CPU_ON = 15;
	public final static int DOSBOX_OPTION_ID_TURBO_ON = 16;
	public final static int DOSBOX_OPTION_ID_CYCLE_ADJUST = 17;
	public final static int DOSBOX_OPTION_ID_JOYSTICK_ENABLE = 18;
	public final static int DOSBOX_OPTION_ID_SWAP_MEDIA = 21;
	public final static int DOSBOX_OPTION_ID_START_COMMAND = 50;
	public final static int DOSBOX_OPTION_ID_DOSBOX_DEFAULT = 51;
	public final static int DOSBOX_OPTION_ID_DOSBOX_USER = 52;
	
	public final static int DOSBOX_OPTION_ID_CPU_AUTO  = 60;
	public final static int DOSBOX_OPTION_ID_CPU_MAX   = 61;
	public final static int DOSBOX_OPTION_ID_CPU_FIXED = 62;
	
	private static boolean initialized = false;
	
	static public void loadPreference(DosBoxLauncher context, final SharedPreferences prefs) {	
		if (initialized) return;
		initialized = true;
		
		// gracefully handle upgrade from previous versions, fishstix
		if (Integer.valueOf(prefs.getString("confcontroller", "-1")) >= 0) {
			DosBoxPreferences.upgrade(prefs);
		}
		
		if (!prefs.getBoolean("dosmanualconf", false)) {  // only write conf if not in manual config mode
			// Build DosBox config
			// Set Application Prefs
			PrintStream out;
			InputStream myInput; 
			try {
				myInput = context.getAssets().open(DosBoxPreferences.CONFIG_FILE);
				Scanner scanner = new Scanner(myInput);
				out = new PrintStream(new FileOutputStream(context.mConfPath+context.mConfFile));
				// Write text to file
				out.println("[dosbox]");
				out.println("memsize="+prefs.getString("dosmemsize", "4"));
				out.println("machine=svga_s3");
				out.println();
				out.println("[render]");
				out.println("frameskip="+prefs.getString("dosframeskip","2"));
				out.println();
				out.println("[cpu]");
				out.println("core="+prefs.getString("doscpu", "dynamic"));
				out.println("cputype=auto");
				if (prefs.getString("doscycles", "auto").contentEquals("auto")) {
					out.println("cycles="+mPrefCycleString);	// auto performance
				} else {
					out.println("cycles="+prefs.getString("doscycles", "3000"));
				}
				out.println("cycleup=500");
				out.println("cycledown=500");
				out.println();
				out.println("[sblaster]");
				out.println("sbtype=" + prefs.getString("dossbtype","sb16"));
				out.println("mixer=true");
				out.println("oplmode=auto");
				out.println("oplemu=fast");
				out.println("oplrate=" + prefs.getString("dossbrate", "22050"));
				out.println();
				out.println("[mixer]");
				try {
					out.println("prebuffer=" + prefs.getInt("dosmixerprebuffer", 15));
				} catch (Exception e) {
					out.println("prebuffer=15");
				}
				out.println("rate=" + prefs.getString("dossbrate", "22050"));
				out.println("blocksize=1024");
				out.println();
				out.println("[dos]");
				out.print("xms=");
				if (prefs.getBoolean("dosxms", true)) {
					out.println("true");
				} else {
					out.println("false");
				}
				out.print("ems=");
				if (prefs.getBoolean("dosems", true)) {
					out.println("true");
				} else {
					out.println("false");
				}
				out.print("umb=");
				if (prefs.getBoolean("dosumb", true)) {
					out.println("true");
				} else {
					out.println("false");
				}
				out.println("keyboardlayout="+prefs.getString("doskblayout", "auto"));
				out.println();
				out.println("[speaker]");
				out.print("pcspeaker=");
				if (prefs.getBoolean("dospcspeaker", false)) {
					out.println("true");
				} else {
					out.println("false");
				}

				// concat dosbox conf
				while (scanner.hasNextLine()){
					out.println(scanner.nextLine());
				}
				// handle autoexec
				out.println(prefs.getString("dosautoexec", "mount c: "+DosBoxPreferences.CONFIG_PATH+" \nc:"));
				out.flush();
				out.close();
				myInput.close();
				scanner.close();
				Log.i("DosBoxTurbo","finished writing: "+ context.mConfPath+context.mConfFile);
			} catch (IOException e) {
				e.printStackTrace();
			} 
		}
		// SCALE SCREEN
		context.mSurfaceView.mScale = prefs.getBoolean("confscale", true);
		
		if (Integer.valueOf(prefs.getString("confscalelocation", "0")) == 0)
			context.mSurfaceView.mScreenTop = false;
		else 
			context.mSurfaceView.mScreenTop = true;
		
		
		// INPUT MODE
		
		int inputMode = INPUT_JOYSTICK; // Integer.valueOf(prefs.getString("confinputmode", "0"));
		boolean isMouseOnly = DosBoxLauncher.mDosBoxLauncher.isMouseOnly;
		boolean isRealMouse = DosBoxLauncher.mDosBoxLauncher.isRealMouse;
		boolean useRealJoystick = DosBoxLauncher.mDosBoxLauncher.useRealJoystick;
		if (isMouseOnly) inputMode = INPUT_MOUSE;
		if (useRealJoystick) inputMode = INPUT_REAL_JOYSTICK;
		if (isRealMouse) inputMode = INPUT_REAL_MOUSE;
		
		Log.d("JSTICK", "setting Input Mode " + inputMode);
		Log.d("JSTICK", "isMouseOnly " + isMouseOnly);
		Log.d("JSTICK", "useRealJoystick " + useRealJoystick);

		switch (inputMode) { 
		case INPUT_MOUSE:
			context.mSurfaceView.mInputMode = DosBoxSurfaceView.INPUT_MODE_MOUSE;
			DosBoxLauncher.nativeSetOption(DosBoxMenuUtility.DOSBOX_OPTION_ID_JOYSTICK_ENABLE, 0 ,null, true);
			break;
		case INPUT_JOYSTICK:
			context.mSurfaceView.mInputMode = DosBoxSurfaceView.INPUT_MODE_JOYSTICK;
			DosBoxLauncher.nativeSetOption(DosBoxMenuUtility.DOSBOX_OPTION_ID_JOYSTICK_ENABLE, 1 ,null, true);
			break;
		case INPUT_REAL_MOUSE:
			context.mSurfaceView.mInputMode = DosBoxSurfaceView.INPUT_MODE_REAL_MOUSE;
			DosBoxLauncher.nativeSetOption(DosBoxMenuUtility.DOSBOX_OPTION_ID_JOYSTICK_ENABLE, 0 ,null, true);
			break;
		case INPUT_REAL_JOYSTICK:
			context.mSurfaceView.mInputMode = DosBoxSurfaceView.INPUT_MODE_REAL_JOYSTICK;
			DosBoxLauncher.nativeSetOption(DosBoxMenuUtility.DOSBOX_OPTION_ID_JOYSTICK_ENABLE, 0 ,null, true);
			break;
		case INPUT_SCROLL:
			context.mSurfaceView.mInputMode = DosBoxSurfaceView.INPUT_MODE_SCROLL;
			DosBoxLauncher.nativeSetOption(DosBoxMenuUtility.DOSBOX_OPTION_ID_JOYSTICK_ENABLE, 0 ,null, true);
			break;
		}
		// Mouse Tracking Mode
		if (Integer.valueOf(prefs.getString("confmousetracking", "0")) == 0) {
			// absolute tracking
			context.mSurfaceView.mAbsolute = true;
		} else {
			context.mSurfaceView.mAbsolute = true;
		}
		// Joystick Center
		context.mSurfaceView.mJoyCenterX = (prefs.getInt("confjoyx", 100)-100)+JOYSTICK_CENTER_X;
		context.mSurfaceView.mJoyCenterY = (prefs.getInt("confjoyy", 100)-100)+JOYSTICK_CENTER_Y;
		
		// mouse sensitivity
		context.mSurfaceView.mMouseSensitivity = ((float)prefs.getInt("confmousesensitivity", 50)/100f)+0.5f;
		
		// Absolute Tracking Calibration function
		/*if (prefs.getBoolean("conf_doReset",false)) {
			// reset calibration data
			context.mSurfaceView.mWarpX = 0f;
			context.mSurfaceView.mWarpY = 0f;
			prefs.edit().putBoolean("conf_doReset", false);
			prefs.edit().putBoolean("conf_doCalibrate", false).commit();
		} else if (prefs.getBoolean("conf_doCalibrate", false)) {
			context.mSurfaceView.mCalibrate = true;
			Toast.makeText(context, R.string.abscalibrate, Toast.LENGTH_SHORT).show();
			prefs.edit().putBoolean("conf_doReset", false);
			prefs.edit().putBoolean("conf_doCalibrate", false).commit();
		}*/
		
		//context.mSurfaceView.mWarpX = Float.valueOf(prefs.getString("confwarpX", "0"));
		//context.mSurfaceView.mWarpY = Float.valueOf(prefs.getString("confwarpY", "0"));
		
		// Input Resolution
		if (Integer.valueOf(prefs.getString("confinputlatency", "0")) == 0) {
			// absolute tracking
			context.mSurfaceView.mInputLowLatency = false;
		} else {
			context.mSurfaceView.mInputLowLatency = true;
		}
		
		// Emulate Mouse Click
		//context.mSurfaceView.mEmulateClick = prefs.getBoolean("confmousetapclick", false);
		// VOL BUTTONS
		//context.mPrefHardkeyOn = prefs.getBoolean("confvolbuttons", true);

		if (prefs.getBoolean("confjoyoverlay", true) && !isMouseOnly) {
			//context.mSurfaceView.mInputMode = DosBoxSurfaceView.INPUT_MODE_JOYSTICK;	// switch to joystick mode
			SharedPreferences.Editor editor = prefs.edit();
			editor.putString("confinputmode", String.valueOf(INPUT_JOYSTICK));
			editor.commit();
		}
		
		if (prefs.getBoolean("confbuttonoverlay", false)) {
			context.mSurfaceView.mShowInfo = true;
		} else {
			context.mSurfaceView.mShowInfo = false;
		}
		// dpad / trackpad emulation
		context.mSurfaceView.mEnableDpad = prefs.getBoolean("confenabledpad", true);
		try {
			int tmp = Integer.valueOf(prefs.getString("confdpadsensitivity", "7").trim());
			if ((tmp >= 1) && (tmp <= 25)) {
				context.mSurfaceView.mDpadRate = tmp;
			} else {
				context.mSurfaceView.mDpadRate = 7;
			}
		} catch (NumberFormatException e) {
			context.mSurfaceView.mDpadRate = 7;
		}
		
		// OS 2.1 - 2.3 < > key fix
		//context.mSurfaceView.mEnableLTKeyFix = prefs.getBoolean("conffixgingerkey", false);
		
		
		// Add custom mappings to ArrayList 
		//context.mSurfaceView.customMapList.clear();
		context.mSurfaceView.customMap.clear();
		for (short i=0;i<DosBoxPreferences.NUM_USB_MAPPINGS;i++) {
			int hardkey = Integer.valueOf(prefs.getString("confmap_custom"+String.valueOf(i)+GamePreference.HARDCODE_KEY, "-1"));
			if ( hardkey > 0) {
				int doskey = Integer.valueOf(prefs.getString("confmap_custom"+String.valueOf(i)+GamePreference.DOSCODE_KEY, "-1"));
				if (doskey > 0) {
					context.mSurfaceView.customMap.put(hardkey,doskey);
				}
			}
		}
		Log.i("DosBoxTurbo","Found " + context.mSurfaceView.customMap.size() + " custom mappings.");
		
		// GESTURES
		context.mSurfaceView.mGestureUp = Short.valueOf(prefs.getString("confgesture_swipeup", "0"));
		context.mSurfaceView.mGestureDown = Short.valueOf(prefs.getString("confgesture_swipedown", "0"));
		
		// TOUCHSCREEN MOUSE
		context.mSurfaceView.mGestureSingleClick = Short.valueOf(prefs.getString("confgesture_singletap", "3"));
		context.mSurfaceView.mGestureDoubleClick = Short.valueOf(prefs.getString("confgesture_doubletap", "5"));
		context.mSurfaceView.mGestureTwoFinger = Short.valueOf(prefs.getString("confgesture_twofinger", "0"));
		context.mSurfaceView.mLongPress = prefs.getBoolean("confgesture_longpress", true);
		
		// FORCE Physical LEFT ALT
		context.mSurfaceView.mUseLeftAltOn = prefs.getBoolean("confaltfix", false);
		
		// SOUND
		DosBoxLauncher.nativeSetOption(DosBoxMenuUtility.DOSBOX_OPTION_ID_SOUND_MODULE_ON, (prefs.getBoolean("confsound", true))?1:0,null,true);
		// DEBUG
		context.mSurfaceView.mDebug = prefs.getBoolean("confdebug", false);
		
		if (context.mSurfaceView.mDebug) {
			// debug mode enabled, show warning
			Toast.makeText(context, R.string.debug, Toast.LENGTH_LONG).show();
		}
		
		context.mSurfaceView.forceRedraw();
	}
	


	static public void copyConfigFile(DosBoxLauncher context) {
		copyFile(context, context.mConfFile);
	}
	
	static public void copyFile(DosBoxLauncher context, String file) {
		try {
		      
			InputStream myInput = new FileInputStream(context.mConfPath + file);
			myInput.close();
			myInput = null;
		}
		catch (FileNotFoundException f) {
			try {
		    	InputStream myInput = context.getAssets().open(file);
		    	OutputStream myOutput = new FileOutputStream(context.mConfPath + file);
		    	byte[] buffer = new byte[1024];
		    	int length;
		    	while ((length = myInput.read(buffer))>0){
		    		myOutput.write(buffer, 0, length);
		    	}
		    	myOutput.flush();
		    	myOutput.close();
		    	myInput.close();
			} catch (IOException e) {
			}
		} catch (IOException e) {
		}
    }	
	
	
	static public void savePreference(DosBoxLauncher context, String key) {
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		if (sharedPrefs != null) {
			SharedPreferences.Editor editor = sharedPrefs.edit();			
			if (editor != null) {		
				//if (PREF_KEY_REFRESH_HACK_ON.equals(key)) {		
				//	editor.putBoolean(PREF_KEY_REFRESH_HACK_ON, context.mPrefRefreshHackOn);
				//}

				editor.commit();
			}
		}		
	} 
	
	static public void saveBooleanPreference(DosBoxLauncher context, String key, boolean value) {
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		if (sharedPrefs != null) {
			SharedPreferences.Editor editor = sharedPrefs.edit();
		
			if (editor != null) {
				editor.putBoolean(key, value);
				editor.commit();
			}
		}				
	}
	
	static public boolean doCreateOptionsMenu(Menu menu) {
		
		menu.add(Menu.NONE, MENU_KEYBOARD, 0, "Keyboard").setIcon(R.drawable.ic_menu_keyboard);
		menu.add(Menu.NONE, MENU_INPUT_INPUT_METHOD, 0, "Input Method").setIcon(R.drawable.ic_menu_flag);
		menu.add(Menu.NONE, MENU_KEYBOARD_SPECIAL, 0, "Special Keys").setIcon(R.drawable.ic_menu_flash);
		menu.add(Menu.NONE, MENU_SETTINGS_SCALE, 0, "Scale: Off").setIcon(R.drawable.ic_menu_resize);
		menu.add(Menu.NONE, MENU_PREFS,Menu.NONE,"Config");
		menu.add(Menu.NONE, MENU_QUIT, 0, "Exit").setIcon(R.drawable.ic_menu_close_clear_cancel);
		return true;		
	}
	
	static public boolean doPrepareOptionsMenu(DosBoxLauncher context, Menu menu) {
		menu.findItem(MENU_SETTINGS_SCALE).setTitle((context.mSurfaceView.mScale)?"Scale: On":"Scale: Off");
		return true;
	}
	
	static public void doShowMenu(DosBoxLauncher context) {
		context.openOptionsMenu();
	}

	static public void doHideMenu(DosBoxLauncher context) {
		context.closeOptionsMenu();
	}
	
	static public void doShowKeyboard(DosBoxLauncher context) {
		InputMethodManager imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imm != null) {
			if (!context.mSurfaceView.hasFocus()){ 
		        context.mSurfaceView.requestFocus();
			}
			imm.showSoftInput(context.mSurfaceView, 0);
		}
	}

	static public void doHideKeyboard(DosBoxLauncher context) {
		InputMethodManager imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imm != null)
			imm.hideSoftInputFromWindow(context.mSurfaceView.getWindowToken(),0);
	}
	
	static public void doShowTextDialog(final DosBoxLauncher context, String message) {
    	AlertDialog.Builder builder = new AlertDialog.Builder(context);
    	builder.setTitle(R.string.app_name);
		builder.setMessage(message);
		
		builder.setPositiveButton("OK", null);

		builder.create().show();		
	}
	
	static public void doShowHideInfo(DosBoxLauncher context, boolean show) {
		context.mSurfaceView.mInfoHide = show;
		context.mSurfaceView.forceRedraw();
	}
	
	static private boolean doOptionsItemSelected(DosBoxLauncher context, MenuItem item)
	{
		switch(item.getItemId()){
			case MENU_QUIT:
				//doConfirmQuit(context);
			    break;
			case MENU_INPUT_INPUT_METHOD:
			{
				InputMethodManager imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
				if (imm != null)
					imm.showInputMethodPicker();
			}
				break;

			case MENU_KEYBOARD_SPECIAL:
				//toggleExtraButtons(context);
				//context.mSurfaceView.mContextMenu = CONTEXT_MENU_SPECIAL_KEYS;				
				//context.openContextMenu(context.mSurfaceView);
				break;
			case MENU_KEYBOARD:
				doShowKeyboard(context);
				break;
			case MENU_SETTINGS_SCALE: 
				context.mSurfaceView.mScale = !context.mSurfaceView.mScale;
				saveBooleanPreference(context, "confscale",context.mSurfaceView.mScale);
				context.mSurfaceView.forceRedraw();
				break;
			case MENU_PREFS:
				/*if (Build.VERSION.SDK_INT<Build.VERSION_CODES.HONEYCOMB) {
			          //startActivity(new Intent(this, EditPreferences.class));
					context.startActivity(new Intent(context, DosBoxPreferences.class));  
				} else {
			          //startActivity(new Intent(this, EditPreferencesHC.class));
			       	context.startActivity(new Intent(context, DosBoxPreferencesHC.class));
			    }*/
				if (context.mPID != null) {
					Intent i = new Intent(context, DosBoxPreferences.class);
					Bundle b = new Bundle();
					b.putString("com.fishstix.dosboxlauncher.pid", context.mPID);
					i.putExtras(b);
					context.startActivity(i);
				} else {
					context.startActivity(new Intent(context, DosBoxPreferences.class));
				}
				break;
			default:
				break;
		  }
		  return true;
	}
	
	static public void doCreateContextMenu(DosBoxLauncher context, ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		switch (context.mSurfaceView.mContextMenu) {
			case CONTEXT_MENU_SPECIAL_KEYS:
			{
				MenuItem	item;
				item = menu.add(0, MENU_KEYBOARD_CTRL, 0, "Ctrl");
				item.setCheckable(true);
				item.setChecked(context.mSurfaceView.mModifierCtrl);

				item = menu.add(0, MENU_KEYBOARD_ALT, 0, "Alt");
				item.setCheckable(true);
				item.setChecked(context.mSurfaceView.mModifierAlt);
						
				item = menu.add(0, MENU_KEYBOARD_SHIFT, 0, "Shift");
				item.setCheckable(true);
				item.setChecked(context.mSurfaceView.mModifierShift);

				menu.add(0, MENU_KEYBOARD_ESC, 0, "ESC");
				menu.add(0, MENU_KEYBOARD_TAB, 0, "Tab");
				
				for(int i = MENU_KEYBOARD_F1; i <= MENU_KEYBOARD_F12; i++)
					menu.add(0, i, 0, "F"+(i-MENU_KEYBOARD_F1+1));	

				menu.add(0, MENU_KEYBOARD_SWAP_MEDIA, 0, "Swap Media");
				context.mSurfaceView.mContextMenu = -1;

				item = menu.add(0, MENU_KEYBOARD_TURBO, 0, "Fast Forward");
				item.setCheckable(true);
				item.setChecked(context.mTurboOn);
			}
				break;
			case CONTEXT_MENU_CYCLES:
			{
				for(int i = MENU_CYCLE_1000; i <= MENU_CYCLE_30000; i++) {
					int value = (i-MENU_CYCLE_1000+1) * 1000;
					MenuItem item = menu.add(1, i, 0, ""+value);
					
					if (value == context.mPrefCycles) {
						item.setChecked(true);
					}
				}
				
				menu.setGroupCheckable(1, true, true);
			}
				break;
			case CONTEXT_MENU_FRAMESKIP:
			{
				for(int i = MENU_FRAMESKIP_0; i <= MENU_FRAMESKIP_10; i++) {
					int value = (i-MENU_FRAMESKIP_0);
					MenuItem item = menu.add(2, i, 0, ""+value);

					if (value == context.mPrefFrameskip) {
						item.setChecked(true);
					}
				}
				
				menu.setGroupCheckable(2, true, true);
			}
				break;
		}
	}
	
	static public void doSendDownUpKey(DosBoxLauncher context, int keyCode) {
		DosBoxControl.sendNativeKey(keyCode , true, context.mSurfaceView.mModifierCtrl, context.mSurfaceView.mModifierAlt, context.mSurfaceView.mModifierShift);
		DosBoxControl.sendNativeKey(keyCode , false, context.mSurfaceView.mModifierCtrl, context.mSurfaceView.mModifierAlt, context.mSurfaceView.mModifierShift);
		context.mSurfaceView.mModifierCtrl = false;
		context.mSurfaceView.mModifierAlt = false;
		context.mSurfaceView.mModifierShift = false;
	}
	
	static public boolean doContextItemSelected(DosBoxLauncher context, MenuItem item) {
		int itemID = item.getItemId();
		
		switch(itemID) {
		case MENU_KEYBOARD_CTRL:
			context.mSurfaceView.mModifierCtrl = !context.mSurfaceView.mModifierCtrl; 
			break;
		case MENU_KEYBOARD_ALT:
			context.mSurfaceView.mModifierAlt = !context.mSurfaceView.mModifierAlt; 
			break;		
		case MENU_KEYBOARD_SHIFT:
			context.mSurfaceView.mModifierShift = !context.mSurfaceView.mModifierShift; 
			break;		
		case MENU_KEYBOARD_TAB:
			doSendDownUpKey(context, KeyEvent.KEYCODE_TAB);
			break;
		case MENU_KEYBOARD_ESC:
			doSendDownUpKey(context, KEYCODE_ESCAPE);
			break;
		case MENU_KEYBOARD_TURBO:
			context.mTurboOn = !context.mTurboOn;
			DosBoxLauncher.nativeSetOption(DOSBOX_OPTION_ID_TURBO_ON, context.mTurboOn?1:0, null,true);			
		case MENU_KEYBOARD_SWAP_MEDIA:
			DosBoxLauncher.nativeSetOption(DOSBOX_OPTION_ID_SWAP_MEDIA, 1,null,true);
			break;
		default:
			if ((itemID >= MENU_KEYBOARD_F1) && (itemID <= MENU_KEYBOARD_F12)) {
				doSendDownUpKey(context, KEYCODE_F1 + (itemID - MENU_KEYBOARD_F1));
			}
			else if ((itemID >= MENU_CYCLE_1000) && (itemID <= MENU_CYCLE_30000)) {
				if (context.mTurboOn) { 
					context.mTurboOn = false;
					DosBoxLauncher.nativeSetOption(DosBoxMenuUtility.DOSBOX_OPTION_ID_TURBO_ON, context.mTurboOn?1:0, null,true);			
				} 
				context.mPrefCycles = (itemID - MENU_CYCLE_1000 + 1) * 1000; 
				savePreference(context, PREF_KEY_CYCLES);
				DosBoxLauncher.nativeSetOption(DOSBOX_OPTION_ID_CYCLES, context.mPrefCycles,null,true);
				Toast.makeText(context, "DosBox Cycles: "+DosBoxControl.nativeGetCycleCount(), Toast.LENGTH_SHORT).show();
			}
			else if ((itemID >= MENU_FRAMESKIP_0) && (itemID <= MENU_FRAMESKIP_10)) {
				context.mPrefFrameskip = (itemID - MENU_FRAMESKIP_0); 
				savePreference(context, PREF_KEY_FRAMESKIP);
				DosBoxLauncher.nativeSetOption(DOSBOX_OPTION_ID_FRAMESKIP, context.mPrefFrameskip,null,true);
			}
			break;
		}
		
		return true;
	}	
	
	static public boolean isExternalStorageWritable() {
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
		    // We can read and write the media
		    return true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
		    // We can only read the media
		    return false;
		} else {
		    // Something else is wrong. It may be one of many other states, but all we need
		    //  to know is we can neither read nor write
		    return false;
		}
	}
	
	public static void getData(DosBoxLauncher context, String pid) {
		try {
			 InputStream is = context.getContentResolver().openInputStream(Uri.parse(CONTENT_URI + pid + ".xml"));
			 FileOutputStream fostream;
			 // Samsung workaround:
			 File file = new File("/dbdata/databases/xtvapps.retrobox.dosbox/shared_prefs/");
			 if (file.isDirectory() && file.exists()) {
				 // samsung
				 fostream = new FileOutputStream("/dbdata/databases/xtvapps.retrobox.dosbox/shared_prefs/"+pid+".xml");
			 } else {
				 // every one else.
				 fostream = new FileOutputStream(context.getFilesDir()+"/../shared_prefs/"+pid+".xml");
			 }

			 PrintStream out = new PrintStream(fostream);
			 Scanner scanner = new Scanner(is);
			 while (scanner.hasNextLine()){
				out.println(scanner.nextLine());
			 }
			 out.flush();
			 is.close();
			 out.close();
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}