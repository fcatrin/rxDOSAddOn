package xtvapps.retrobox.dosbox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.FrameLayout;
import retrobox.keyboard.KeyboardLayout;
import retrobox.keyboard.KeyboardView;
import xtvapps.core.SimpleCallback;
import xtvapps.dosbox.swos.R;

public class CustomKeyboard {
	
	// keys from retrobox.vinput.KeyTranslator
	// special keys can be added with retrobox.vinput.KeyTranslator.addTranslation
	
	List<Map<String, String>> keymaps = new ArrayList<Map<String, String>>();
	
	String labels_1[] = {"Esc", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "Back"};
	
	String labels_2[] = {"Tab", "Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P", "\\", "*" };
	String labels_3[] = {"Up/Down", "A", "S", "D", "F", "G", "H", "J", "K", "L", "Enter"};
	String labels_4[] = {"Shift", "Z", "X", "C", "V", "B", "N", "M", ",", ".", "-", "Shift"};
	String labels_5[] = {"[Fn]", "Ctrl", "Win", "Alt", "Space", "AltGr", "Menu", "Ctrl"};
	
	String labels_6[] = {"Esc","F1", "F2", "F3", "F4", "F5", "F6", "F7", "F8", "F9", "F10", "F11", "F12"};
	String labels_7[] = {"|", ":", ";", "@", "!", "\"", "#", "$", "%", "&", "/", "(", ")", "="};
	String labels_8[] = {"<", ">", "*", "+", "\\", "Ins", "Del", "Home", "End", "Pg Up", "Pg Down"};
	String labels_9[] = {"[abc]", "KP .", "KP0", "KP1", "KP2", "KP3", "KP4", "KP5", "KP6", "KP7", "KP8", "KP9"};
				
	String codes_1[] = {"ESCAPE", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "BACKSPACE"};
	String codes_2[] = {"TAB", "Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P", "BACKSLASH", "STAR"};
	String codes_3[] = {"", "A", "S", "D", "F", "G", "H", "J", "K", "L", "ENTER"};
	String codes_4[] = {"LEFTSHIFT", "Z", "X", "C", "V", "B", "N", "M", "COMMA", "DOT", "MINUS", "RIGHTSHIFT"};
	String codes_5[] = {"", "LEFTCTRL", "DOS_WIN", "LEFTALT", "SPACE", "RIGHTALT", "DOS_MENU", "RIGHTCTRL"};
	String codes_6[] = {"ESC","F1", "F2", "F3", "F4", "F5", "F6", "F7", "F8", "F9", "F10", "F11", "F12"};
	String codes_7[] = {"UNK_PIPE", "SHIFT+KEY_SEMICOLON", "SEMICOLON", "AT", "SHIFT+KEY1", "BACKSLASH", 
			"POUND", "SHIFT+KEY_4", "SHIFT+KEY_5", "SHIFT+KEY_6", "SLASH", "SHIFT+KEY_8", "SHIFT+KEY_9", "EQUALS"};
	String codes_8[] = {"SHIFT+KEY_COMMA", "SHIFT+KEY_PERIOD", "STAR", "PLUS", "BACKSLASH", 
			"INSERT", "DEL", "HOME", "END", "PAGEUP", "PAGEDOWN"};
	String codes_9[] = {"", "KPDOT", "KP0", "KP1", "KP2", "KP3", "KP4", "KP5", "KP6", "KP7", "KP8", "KP9"};

	private KeyboardView kb;

	public CustomKeyboard(Activity activity) {
		kb = (KeyboardView)activity.findViewById(R.id.keyboard_view);

		KeyboardLayout kl = new KeyboardLayout();
		kl.addRow(labels_1, codes_1);
		kl.addRow(labels_2, codes_2);
		kl.addRow(labels_3, codes_3);
		kl.addRow(labels_4, codes_4);
		kl.addRow(labels_5, codes_5);

		kl.setKeySize("Space", 4);
		kl.setKeyCode("[Fn]", KeyboardView.SWITCH_LAYOUT + 1);
		kl.setKeyCode("Up/Down", KeyboardView.TOGGLE_POSITION);
		kl.setKeySize("Up/Down", 2);
		kl.setKeySize("Tab", 2);
		kl.setKeySize("Enter", 2);
		kl.setKeySize("Back", 2);
		kl.setKeySize("Shift", 2);
		kb.init(activity, kl);

		kl = new KeyboardLayout();
		kl.addRow(labels_6, codes_6);
		kl.addRow(labels_7, codes_7);
		kl.addRow(labels_8, codes_8);
		kl.addRow(labels_9, codes_9);
		kl.setKeySize("[abc]", 2);
		kl.setKeyCode("[abc]", KeyboardView.SWITCH_LAYOUT + 2);
		kb.init(activity, kl);
		
		kb.setOnKeyListener(new OnKeyListener() {
			
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				Log.d("KEYB", "pressed " + keyCode);
				return true;
			}
		});
		
		kb.setOnTogglePositionCallback(new SimpleCallback() {
			
			@Override
			public void onResult() {
				FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams)kb.getLayoutParams();
				layoutParams.gravity = layoutParams.gravity == Gravity.TOP ? Gravity.BOTTOM : Gravity.TOP;
				kb.requestLayout();
			}
		});
	}
	
	public void open() {
		kb.setVisibility(View.VISIBLE);
		kb.requestFocus();
	}
	
	public void close() {
		kb.setVisibility(View.GONE);
	}
	
	public boolean isVisible() {
		return kb.getVisibility() == View.VISIBLE;
	}
}
