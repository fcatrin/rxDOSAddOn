package retrobox.utils;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import xtvapps.core.AndroidFonts;

public class GamepadView extends FrameLayout {
	private static final int REF_GAMEPAD_WIDTH = 458;
	private static final int REF_GAMEPAD_HEIGHT = 300;

	public enum EventId {
		UP, DOWN, LEFT, RIGHT,
		BTN_A, BTN_B, BTN_X, BTN_Y,
		BTN_L1, BTN_R1, BTN_L2, BTN_R2,
		BTN_L3, BTN_R3, BTN_SELECT, BTN_START,
		RX, RY
	}
	
	public static String eventNames[] = { 
			"UP", "DOWN", "LEFT", "RIGHT", 
			"BTN_A", "BTN_B", "BTN_X", "BTN_Y", 
			"TL", "TR", "TL2", "TR2",
			"TL3", "TR3", "SELECT", "START",
			"RX", "RY"
		};
	
	public static String eventLabels[] = { 
			"UP", "DOWN", "LEFT", "RIGHT", 
			"A", "B", "X", "Y", 
			"L", "R", "L2", "R2",
			"L3", "R3", "SELECT", "START",
			"RX", "RY"
		};
	
	ButtonLabelBox labelBoxes[] = new ButtonLabelBox[eventNames.length];
	private int textSize;
	private int textColor; 

	public GamepadView(Context context) {
		super(context);
	}

	public GamepadView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public GamepadView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	public void init() {

		removeAllViews();
		
		textSize = getResources().getDimensionPixelSize(R.dimen.text_small);
		textColor = getResources().getColor(R.color.pal_text_hl);
		
		new ButtonLabelBox(this, EventId.UP, 95, 90);
		new ButtonLabelBox(this, EventId.DOWN, 95, 146);
		new ButtonLabelBox(this, EventId.LEFT, 65, 118);
		new ButtonLabelBox(this, EventId.RIGHT, 124, 118);
		
		new ButtonLabelBox(this, EventId.BTN_L2, 90, 15);
		new ButtonLabelBox(this, EventId.BTN_L1, 90, 45);
		new ButtonLabelBox(this, EventId.BTN_R2, 368, 15);
		new ButtonLabelBox(this, EventId.BTN_R1, 368, 45);
		new ButtonLabelBox(this, EventId.BTN_L3, 161, 183);
		new ButtonLabelBox(this, EventId.BTN_R3, 296, 183);

		new ButtonLabelBox(this, EventId.RX, 339, 183);
		new ButtonLabelBox(this, EventId.RY, 296, 227);
		
		new ButtonLabelBox(this, EventId.BTN_A, 394, 118);
		new ButtonLabelBox(this, EventId.BTN_B, 364, 150);
		new ButtonLabelBox(this, EventId.BTN_X, 333, 118);
		new ButtonLabelBox(this, EventId.BTN_Y, 364, 87);
		new ButtonLabelBox(this, EventId.BTN_SELECT, 203, 116);
		new ButtonLabelBox(this, EventId.BTN_START, 252, 116);
		
		setBackgroundResource(R.drawable.gamepad);
		
		this.post(new Runnable(){

			@Override
			public void run() {
				layout();
			}
		});
	}

	public ButtonLabelBox getButton(int index) {
		return labelBoxes[index];
	}

	public void highLightButton(ButtonLabelBox button) {
		for(ButtonLabelBox labelBox : labelBoxes) {
			if (labelBox.eventId == button.eventId) {
				labelBox.hightlight();
			} else {
				labelBox.dim();
			}
		}
		
	}
	

	public void layout() {
		int gamepadWidth  = getMeasuredWidth();
		int gamepadHeight = getMeasuredHeight();
		
		for(ButtonLabelBox labelBox : labelBoxes) {
			if (labelBox!=null) labelBox.layout(gamepadWidth, gamepadHeight);
		}
	}
	
	public class ButtonLabelBox {
		EventId eventId;
		float x;
		float y;
		String label;
		TextView  textView;
		ImageView imageView;
		
		public ButtonLabelBox(ViewGroup parent, EventId eventId, float x, float y) {
			this.eventId = eventId;
			labelBoxes[eventId.ordinal()] = this;
			
			this.x = x / REF_GAMEPAD_WIDTH;
			this.y = y / REF_GAMEPAD_HEIGHT;
			
			textView = new TextView(parent.getContext());
			imageView = new ImageView(parent.getContext());
			imageView.setAlpha(0.1f);
			
			parent.addView(imageView);
			parent.addView(textView);

			AndroidFonts.setViewFont(textView, RetroBoxUtils.FONT_DEFAULT_R);
			textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
			textView.setTextColor(textColor);
			
			int buttonResourceId = eventId == EventId.RX || eventId == EventId.RY ?
					R.drawable.gamepad_press_analog : R.drawable.gamepad_press;
			
			imageView.setImageResource(buttonResourceId);
			
			setLabel(eventLabels[eventId.ordinal()]);
		}
		
		private void dim() {
			setButtonAlpha(0.1f);
		}

		private void hightlight() {
			setButtonAlpha(0.8f);
		}

		private void setButtonAlpha(float alpha) {
			imageView.setAlpha(alpha);
		}
		
		public void setLabel(String label) {
			this.label = label;
			textView.setText(label);
		}
		
		public String getLabel() {
			return label;
		}
		
		public void layout(int gamepadWidth, int gamepadHeight) {
			int buttonWidth = (int)(gamepadWidth / (18 ));
			int buttonHeight = (int)(gamepadHeight / (12 ));

			int textWidth  = buttonWidth * 4;
			int textHeight = buttonHeight;
			
			FrameLayout.LayoutParams textLayoutParams = (FrameLayout.LayoutParams)textView.getLayoutParams();
			textLayoutParams.width = textWidth;
			textLayoutParams.height = textHeight;
			textLayoutParams.leftMargin = (int) (x * gamepadWidth - textWidth / 2);
			textLayoutParams.topMargin  = (int) (y * gamepadHeight - textHeight / 2); 
			textView.setGravity(Gravity.CENTER);
			
			
			FrameLayout.LayoutParams imageLayoutParams = (FrameLayout.LayoutParams)imageView.getLayoutParams();
			imageLayoutParams.width = buttonWidth;
			imageLayoutParams.height = buttonHeight;
			imageLayoutParams.leftMargin = (int) (x * gamepadWidth - buttonWidth / 2);
			imageLayoutParams.topMargin  = (int) (y * gamepadHeight - buttonHeight /2);

		}
	}
	
}
