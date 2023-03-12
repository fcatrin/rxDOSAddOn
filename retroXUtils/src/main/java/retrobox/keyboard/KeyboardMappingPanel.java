package retrobox.keyboard;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import retrobox.keyboard.layouts.PCKeyboardLayout;
import retrobox.utils.GamepadView;
import retrobox.utils.GamepadView.ButtonLabelBox;
import retrobox.utils.GamepadView.EventId;
import retrobox.utils.R;
import retrobox.utils.RetroBoxDialog;
import retrobox.utils.RetroBoxUtils;
import xtvapps.core.AndroidFonts;
import xtvapps.core.SimpleCallback;
import xtvapps.core.Utils;

public class KeyboardMappingPanel {
	
	// keys from retrobox.vinput.KeyTranslator
	// special keys can be added with retrobox.vinput.KeyTranslator.addTranslation
	
	Map<String, String> keymap = new HashMap<String, String>();
	
	private int currentButtonIndex = 0;
	
	private Activity activity;
	private GamepadView gamepadView;
	
	private EventId orderedEvents[] = {
			EventId.LEFT, EventId.UP, 
			EventId.DOWN, EventId.RIGHT,
			EventId.BTN_SELECT,EventId.BTN_START,
			EventId.BTN_X, EventId.BTN_Y, 
			EventId.BTN_B, EventId.BTN_A,
			EventId.BTN_L2, EventId.BTN_L1, 
			EventId.BTN_R2, EventId.BTN_R1,
			EventId.BTN_L3, EventId.BTN_R3,
			EventId.RX, EventId.RY
	};

	private File keymapFile;
	private KeyboardView kbView;
	private SimpleCallback callback;

	public KeyboardMappingPanel(final Activity activity, File keymapFile, KeyboardLayout keyboardLayout[], final SimpleCallback callback) {
		this.activity = activity; 
		this.keymapFile = keymapFile;
		this.callback = callback;

		load();
		
		TextView txtMapperLeft   = (TextView)activity.findViewById(R.id.txtGamepadMappingMessage);
		TextView txtMapperButton = (TextView)activity.findViewById(R.id.txtGamepadButtonName);

		AndroidFonts.setViewFont(txtMapperLeft,   RetroBoxUtils.FONT_DEFAULT_R);
		AndroidFonts.setViewFont(txtMapperButton, RetroBoxUtils.FONT_DEFAULT_R);

		kbView = (KeyboardView)activity.findViewById(R.id.keyboard_map_view);

		kbView.init(activity, keyboardLayout);
		
		kbView.setOnVirtualKeyListener(new VirtualKeyListener() {
			
			@Override
			public void onKeyPressed(String code) {
				setKeyMap(code);
			}
		});
		
		Button btnButtonPrev = (Button)activity.findViewById(R.id.btnGamepadButtonPrev);
		Button btnButtonNext = (Button)activity.findViewById(R.id.btnGamepadButtonNext);
		
		btnButtonPrev.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				switchToButton(-1);
			}
		});
		btnButtonNext.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				switchToButton(1);
			}
		});
		
		gamepadView = (GamepadView)activity.findViewById(R.id.keyboardMapGamepad);
		gamepadView.init();
		gamepadView.layout();
		
		switchToButton(0);

		updateButtonLabels();
		
		Button btnSave = (Button)activity.findViewById(R.id.btnKeymapSave);
		Button btnClose = (Button)activity.findViewById(R.id.btnKeymapClose);
		
		btnSave.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				try {
					save();
					close();
					callback.onResult();
				} catch(Exception e) {
					String msg = "There was an error trying to save the keymap: " + e.getMessage();
					RetroBoxDialog.showAlert(activity, msg);
				}
			}
		});
		
		btnClose.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});
	}
	
	public boolean isVisible() {
		return activity.findViewById(R.id.keyboard_mapping_panel).getVisibility() == View.VISIBLE;	
	}

	public void dismiss() {
		close();
		callback.onError();
	}
	
	protected void setKeyMap(String code) {
		int eventIndex = orderedEvents[currentButtonIndex].ordinal();
		keymap.put(GamepadView.eventNames[eventIndex], code);
		
		ButtonLabelBox button = gamepadView.getButton(eventIndex); 
		button.setLabel(event2human(code));
		
		updateButtonDisplay();
	}

	public void open() {
		setVisible(true);
		
		kbView.post(new Runnable() {

			@Override
			public void run() {
				kbView.getChildAt(0).requestFocus();
			}
		});
	}

	private void setVisible(boolean visible) {
		int visibility = visible ? View.VISIBLE : View.GONE;
		activity.findViewById(R.id.keyboard_mapping_panel).setVisibility(visibility);
	}
	
	private void close() {
		setVisible(false);
	}

	private void switchToButton(int delta) {
		currentButtonIndex += delta;
		if (currentButtonIndex<0) currentButtonIndex = orderedEvents.length - 1;
		if (currentButtonIndex>= orderedEvents.length) currentButtonIndex = 0;
		
		updateButtonDisplay();
	}
	
	private void updateButtonDisplay() {
		int eventIndex = orderedEvents[currentButtonIndex].ordinal();
		ButtonLabelBox button = gamepadView.getButton(eventIndex); 
		gamepadView.highLightButton(button);

		String buttonName = event2human(GamepadView.eventNames[eventIndex]);
		TextView buttonNameView = (TextView)activity.findViewById(R.id.txtGamepadButtonName);

		String buttonMapName = button.getLabel();
		if (Utils.isEmptyString(buttonMapName)) buttonMapName = " is not mapped";
		else buttonMapName = " is mapped to " + buttonMapName;

		buttonNameView.setText(buttonName + buttonMapName);
		
	}
	
	private void updateButtonLabels() {
		for(int i=0; i<GamepadView.eventNames.length; i++) {
			String label = keymap.get(GamepadView.eventNames[i]);
			if (label == null) label = "";
			
			ButtonLabelBox button = gamepadView.getButton(i);
			button.setLabel(event2human(label));
		}
	}
	
	private String event2human(String event) {
		if (event.startsWith("KEY_")) return event.substring("KEY_".length());
		if (event.startsWith("ATR_")) return event.substring("ATR_".length());
		if (event.startsWith("BTN_")) return "Button " + event.substring("BTN_".length());
		if (event.equals("RX")) return "Analog Right X";
		if (event.equals("RY")) return "Analog Right Y";
		if (event.equals("TL")) return "Thumb Left";
		if (event.equals("TR")) return "Thumb Right";
		if (event.equals("TL2")) return "Thumb Left 2";
		if (event.equals("TR2")) return "Thumb Right 2";
		if (event.equals("TL3")) return "Thumb Left 3";
		if (event.equals("TR3")) return "Thumb Right 3";
		return event;
	}
	
	private void save() throws IOException {
		RetroBoxUtils.saveMapping(keymapFile, keymap);
	}
	
	private void load() {
		try {
			keymap.clear();
			keymap.putAll(RetroBoxUtils.loadMapping(keymapFile));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
