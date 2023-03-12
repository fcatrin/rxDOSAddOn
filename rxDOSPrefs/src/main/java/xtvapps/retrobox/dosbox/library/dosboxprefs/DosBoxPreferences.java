/*
 *  Copyright (C) 2012 Fishstix (Gene Ruebsamen - ruebsamen.gene@gmail.com)
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
package xtvapps.retrobox.dosbox.library.dosboxprefs;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import xtvapps.retrobox.v2.dosbox.library.dosboxprefs.R;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.widget.Toast;

public class DosBoxPreferences extends PreferenceActivity implements OnSharedPreferenceChangeListener, OnPreferenceClickListener {
	private Preference dosmanualconf_file = null;
	private Preference doseditconf_file = null;
	private Preference confmousetracking = null;
	private Preference confprofile_manager = null;
	
	public static final String CONFIG_FILE = "dosbox.conf";
	public static final String CONFIG_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()+"/";//"/sdcard/";
	public static final int NUM_USB_MAPPINGS = 1;
	public static final int XPERIA_BACK_BUTTON = 72617;
	private PreferenceCategory prefCatOther = null;
    
	private SharedPreferences prefs;
    
    private Context ctx = null;
    private boolean mProfileManagerInstalled = false;
    
    private static final int TOUCHSCREEN_MOUSE = 0;
    private static final int TOUCHSCREEN_JOY = 1;
    private static final int PHYSICAL_MOUSE = 2;
    private static final int PHYSICAL_JOY = 3;
    private static final int SCROLL_SCREEN = 4;
    
	  @SuppressWarnings("deprecation")
	  @Override
	  public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    ctx = this;
		try {
			Context otherAppsContext = createPackageContext("com.fishstix.dosboxlauncher", 0);
			mProfileManagerInstalled = true;
		} catch (NameNotFoundException e) {
			Log.i("DosBoxTurbo","Profile Manager not found");
		}

	    prefs = PreferenceManager.getDefaultSharedPreferences(this);

	    if (prefs.getString("dosautoexec", "-1").contentEquals("-1")) 
	    	prefs.edit().putString("dosautoexec","mount c: "+CONFIG_PATH+" \nc:").commit();
	    if (prefs.getString("dosmanualconf_file", "-1").contentEquals("-1")) 
	    	prefs.edit().putString("dosmanualconf_file",CONFIG_PATH+CONFIG_FILE).commit();
	    
	    addPreferencesFromResource(R.xml.preferences);
	    doseditconf_file = (Preference) findPreference("doseditconf_file");
	    confprofile_manager = (Preference) findPreference("confprofile_manager");
	    confprofile_manager.setOnPreferenceClickListener(this);
	    dosmanualconf_file = (Preference) findPreference("dosmanualconf_file");
	    confmousetracking = (Preference) findPreference("confmousetracking");
	    
	    prefCatOther = (PreferenceCategory) findPreference("prefCatOther");
	    InputFilter[] filterArray = new InputFilter[2];
	    filterArray[0] = new InputFilter() { 
	    	@Override
	        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) { 
	            	for (int i = start; i < end; i++) { 
	            		char c = source.charAt(i);
	            		if (!Character.isLetterOrDigit(c)) { 
	            			return ""; 
	                    }
	            		if (Character.isLetter(c)) {
	            			if (!Character.isLowerCase(c)) {
	            				return "";
	            			}
	            		}
	                } 
	            return null; 
	        }
	    };
	    filterArray[1] = new InputFilter.LengthFilter(1);
	    
	    // check for Xperia Play
	    Log.i("DosBoxTurbo", "Build.DEVICE: "+android.os.Build.DEVICE);
	}
	  	  
	static public void upgrade(final SharedPreferences prefs) {
		// TODO

	}
	  
	@Override
	public void onResume() {
		super.onResume();
		
		// No Physical Dpad
		//if (getResources().getConfiguration().navigation == Configuration.NAVIGATION_NONAV) {
			// no physical dpad available, hide dpad options
			//dpad_mappings.setEnabled(false);
			//confenabledpad.setEnabled(false);
			//confdpadsensitivity.setEnabled(false);
		    final int sdkVersion = Integer.parseInt(Build.VERSION.SDK);
		/*    if (sdkVersion < Build.VERSION_CODES.GINGERBREAD) {
		    	updateGameController(0);	// show dpad only
		    } else {
		    	updateGameController(Integer.valueOf(prefs.getString("confcontroller", "0")));
		    } */
		//}
		
		// enable/disable settings based upon input mode
		configureInputSettings(Integer.valueOf(prefs.getString("confinputmode", "0")));

	
	    // get the two custom preferences
	    Preference versionPref = (Preference) findPreference("version");
	    Preference helpPref = (Preference) findPreference("help");
	    doseditconf_file.setOnPreferenceClickListener(this);
	    //helpPref.setOnPreferenceClickListener(this);
	    String versionName="";
	    try {
			versionName = getPackageManager().getPackageInfo("com.fishstix.dosbox", 0).versionName;
		} catch (NameNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		if (mProfileManagerInstalled) {
			try {
				prefCatOther.removePreference(confprofile_manager);
			} catch (NullPointerException e) { }
		}
	    versionPref.setSummary(versionName);
	    prefs.registerOnSharedPreferenceChangeListener(this);
	}
	
	@Override
	public void onPause() {
		super.onPause();
	    prefs.unregisterOnSharedPreferenceChangeListener(this);
	}
	
	public void onSharedPreferenceChanged(SharedPreferences preference, String key) {
		if (key.contentEquals("dosmanualconf_file")) {
			dosmanualconf_file.setSummary(preference.getString("dosmanualconf_file",""));
			Toast.makeText(ctx, R.string.restart, Toast.LENGTH_SHORT).show();
		} else if (key.contentEquals("confinputmode")) {
			configureInputSettings(Integer.valueOf(preference.getString(key, "0")));
		} 
	}
	
	private void configureInputSettings(int input_mode) {
		switch (input_mode) {
		case TOUCHSCREEN_MOUSE:
			// enable tracking settings
			confmousetracking.setEnabled(true);
			break;
		case TOUCHSCREEN_JOY:
			confmousetracking.setEnabled(false);
			break;
		case PHYSICAL_MOUSE:
			confmousetracking.setEnabled(true);
			break;
		case PHYSICAL_JOY:
		case SCROLL_SCREEN:
			confmousetracking.setEnabled(false);
			break;		
		}		
	}
	
	private static final char DEFAULT_O_BUTTON_LABEL = 0x25CB;   //hex for WHITE_CIRCLE

	@Override
	public boolean onPreferenceClick(Preference preference) {
		if (preference == confprofile_manager) {
			Intent goToMarket = null;
			goToMarket = new Intent(Intent.ACTION_VIEW,Uri.parse("market://details?id=com.fishstix.dosboxlauncher"));
			try {
				startActivity(goToMarket);
				finish();
			} catch (ActivityNotFoundException e) {
				Toast.makeText(ctx, R.string.error, Toast.LENGTH_SHORT).show();
			}
			return true;
		} else if (preference == doseditconf_file) {
			// setup intent
			Intent intent = new Intent(Intent.ACTION_EDIT); 
			Uri uri = Uri.parse(prefs.getString("dosmanualconf_file","")); 
			intent.setDataAndType(uri, "text/plain"); 
			// Check if file exists, if not, copy template
			File f = new File(prefs.getString("dosmanualconf_file",""));
			if (!f.exists()) {
				try {
					InputStream in = getApplicationContext().getAssets().open("template.conf");
					FileOutputStream out = new FileOutputStream(f);
					byte[] buffer = new byte[1024];
					int len = in.read(buffer);
					while (len != -1) {
					    out.write(buffer, 0, len);
					    len = in.read(buffer);
					}
					in.close();
					out.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			// launch editor
			try {
				startActivity(intent);
			} catch (ActivityNotFoundException e) {
	    		Toast.makeText(this, R.string.noeditor,Toast.LENGTH_SHORT).show(); 
	    	}
		}
		return false;
	}
}
