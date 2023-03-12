package retrobox.vinput.overlay;

import java.util.ArrayList;
import java.util.List;

import retrobox.vinput.GamepadDevice;
import retrobox.vinput.KeyTranslator;
import retrobox.vinput.Mapper;
import retrobox.vinput.VirtualEvent;
import retrobox.vinput.VirtualEvent.MouseButton;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

public class OverlayExtra {
	private static final int EXTRA_BUTTONS_ALPHA = 0x80000000;
	private static final int EXTRA_BUTTONS_ALPHA_PRESSED = 0xE0000000;
	
	public static boolean requiresRedraw = false;
	public static List<ExtraButton> extraButtons = new ArrayList<ExtraButton>();
	public static float buttonTextSize;
	
	public static void addExtraButton(ExtraButton extraButton) {
		extraButtons.add(extraButton);
		ExtraButton.textSize = extraButtons.get(0).h / 2;
	}
	
	public static boolean hasExtraButtons() {
		return extraButtons.size()>0;
	}
	
	private static RectF mButtonRect = new RectF();

	public static void drawExtraButtons(Canvas canvas, Paint textPaint) {
		for(ExtraButton button : extraButtons) {
			if (button.event!=null && button.visible) {
				textPaint.setColor(button.pressed?button.colorPressed:button.color);
				mButtonRect.set(button.x, button.y, button.x+button.w, button.y + button.h);
				mButtonRect.inset(5, 5);
				canvas.drawRoundRect(mButtonRect, 5, 5, textPaint);
			}
		}
		
		textPaint.setColor(0x70000000);
		textPaint.setAntiAlias(true);

		float textSize = textPaint.getTextSize();
		
		textPaint.setTextSize(ExtraButton.textSize);
		float descent = textPaint.descent();
		for(ExtraButton button : extraButtons) {
			if (button.event!=null && button.visible) canvas.drawText(button.label, button.x + (button.w/2.0f), button.y + button.h/2.0f + (ExtraButton.textSize - descent)/2.0f , textPaint);
		}
		
		textPaint.setTextSize(textSize);
		
		textPaint.setAntiAlias(false);
	}

	public static boolean onExtraButtonPress(int pointerId, int x, int y) {
		GamepadDevice firstGamepad = Mapper.gamepadDevices[0];
		for(ExtraButton jb : extraButtons) {
    		if (jb.event!=null && inRect(jb.x, jb.y, jb.w, jb.h, x, y)) {
    			jb.pressed = true;
    			jb.pointerId = pointerId;
    			jb.event.sendEvent(firstGamepad, true);
    			requiresRedraw = true;
    			return true;
    		}
    	}
		return false;
	}
	
	public static boolean onExtraButtonRelease(int pointerId) {
		GamepadDevice firstGamepad = Mapper.gamepadDevices[0];
		for(ExtraButton jb : extraButtons) {
			if (jb.event!=null && jb.pointerId == pointerId) {
				jb.pressed = false;
				jb.pointerId = Overlay.POINTER_ID_NONE;
				jb.event.sendEvent(firstGamepad, false);
				requiresRedraw = true;
				return true;
			}
		}
		return false;
	}

	private static boolean inRect(int x, int y, int w, int h, float px, float py) {
		return px > x && px < x+w && py > y && py< y + h;
	}

	
	public static ExtraButton createMouseButton(String name, String key, int left, int margin, int h, int size) {
		boolean isLeftButton = key.equals("MOUSE_LEFT");
		ExtraButton mouseButton = new ExtraButton();
		mouseButton.w = size;
		mouseButton.h = (int)(size * 0.4);
		mouseButton.y = h - margin - mouseButton.h;
		
		mouseButton.x = isLeftButton?margin:left - margin - mouseButton.w;
		
		mouseButton.label = name;
		mouseButton.event = new VirtualEvent(isLeftButton?MouseButton.LEFT:MouseButton.RIGHT);
		mouseButton.color = EXTRA_BUTTONS_ALPHA | 0xFFFFFF;
		mouseButton.colorPressed = EXTRA_BUTTONS_ALPHA_PRESSED | 0xFFFFFF;
		return mouseButton;
	}
	
	public static ExtraButton createExtraButton(String name, String key, int left, int top, int size) {
		ExtraButton button = new ExtraButton();
		button.x = left;
		button.y = top;
		button.w = size;
		button.h = (int)(size * 0.4);
		button.label = name;
		button.event = KeyTranslator.translate(key);
		button.color = EXTRA_BUTTONS_ALPHA | 0xFFFFFF;
		button.colorPressed = EXTRA_BUTTONS_ALPHA_PRESSED | 0xFFFFFF;
		return button;
	}


}
