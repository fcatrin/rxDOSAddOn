package xtvapps.retrobox.dosbox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import retrobox.keyboard.KeyboardView;
import retrobox.keyboard.VirtualKeyListener;
import retrobox.keyboard.layouts.PCKeyboardLayout;
import retrobox.vinput.KeyTranslator;
import retrobox.vinput.VirtualEvent;
import xtvapps.core.SimpleCallback;
import xtvapps.retrobox.v2.dosbox.R;

public class CustomKeyboard {
	
	// keys from retrobox.vinput.KeyTranslator
	// special keys can be added with retrobox.vinput.KeyTranslator.addTranslation
	
	List<Map<String, String>> keymaps = new ArrayList<Map<String, String>>();
	
	private KeyboardView kb;

	public CustomKeyboard(Activity activity) {
		kb = (KeyboardView)activity.findViewById(R.id.keyboard_view);

		PCKeyboardLayout kl = new PCKeyboardLayout();
		kb.init(activity, kl.getKeyboardLayout());
		
		kb.setOnVirtualKeyListener(new VirtualKeyListener(){

			@Override
			public void onKeyPressed(String code) {
				if (!code.contains("+") && !code.startsWith("KEY_")) {
					code = "KEY_" + code;
				}
				VirtualEvent event = KeyTranslator.translate(code);
				if (event!=null) {
					// DosBoxControl.sendNativeKeyPress(event.keyCode, event.ctrl, event.alt, event.shift);
				}
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
