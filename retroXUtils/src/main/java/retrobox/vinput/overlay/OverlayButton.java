package retrobox.vinput.overlay;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Rect;

public class OverlayButton {
	public enum ButtonAction {TRIGGER, ANALOG}
	public enum ButtonType {RECT, CIRCLE}

	public int x;
	public int y;
	public ButtonType type;
	public ButtonAction action = ButtonAction.TRIGGER;
	
	public int width;  // radiusX if circle 
	public int height; // radiusY if circle
	
	public String label;

	private boolean pressed = false;
	public boolean visible = true;
	public int pointerId = Overlay.POINTER_ID_NONE;
	public int eventIndexes[];
	
	public Bitmap bitmap = null;
	
	private Rect src = new Rect();
	private Rect dst = new Rect();
	private Rect box = new Rect();
	
	private int rangeX = 0;
	private int rangeY = 0;

	private float radiusX = 0;
	private float radiusY = 0;

	public float analogX = 0;
	public float analogY = 0;
	
	static Paint textPaint = new Paint();
	float rangeMod = 1.0f;
	float pct = 1.0f;
	
	boolean displayTouchscreenAreas = false;
	
	int colors[] = {0x80FF00FF, 0x8000FFFF, 0x80FFFF00, 0x80FFFFFF, 0x8000FF00, 0x80A020A0,
			0x80A0A020, 0x80A02020, 0x8020A020, 0x802020A0, 0x80808080,
			0x80A0A0A0, 0x80202020
	};
	
	int colorIndex;
	static int colorIndexSeq = 0;
	
	public OverlayButton() {
		colorIndex = (colorIndexSeq++) % colors.length;
	}
	
	public static void setTextSize(int textSize) {
		textPaint.setTextSize(textSize);
		textPaint.setTextAlign(Align.CENTER);
		textPaint.setColor(0x80FFFFFF);
	}
	
	public void recalc() {
		if (bitmap!=null) {
			src.top = 0;
			src.left = 0;
			src.right = bitmap.getWidth();
			src.bottom =  bitmap.getHeight();
		}
		dst.left   = x - width;
		dst.top    = y - height;
		dst.right  = x + width;
		dst.bottom = y + height;
		recalcRangeMod(1.0f);
	}
	
	private void recalcRangeMod(float rangeMod) {
		rangeX = (int)(width  * rangeMod);
		rangeY = (int)(height * rangeMod);
		radiusX = (float)Math.pow(rangeX,2);
		radiusY = (float)Math.pow(rangeY,2);
		box.left   = x - rangeX;
		box.top    = y - rangeY;
		box.right  = x + rangeX;
		box.bottom = y + rangeY;
	}
	
	public void setPressed(boolean pressed) {
		float rm = pressed?rangeMod:1.0f;
		recalcRangeMod(rm);
		this.pressed = pressed;
	}
	
	public boolean isPressed() {
		return pressed;
	}

	public void draw(Canvas canvas, Paint p) {
		Paint pc = new Paint();
		pc.setColor(colors[colorIndex]);
		if (bitmap==null) {
			if (displayTouchscreenAreas) {
				if (type == ButtonType.CIRCLE) {
					canvas.drawCircle(x, y, rangeX, pc);
					canvas.drawCircle(x, y, rangeY, pc);
				} else {
					canvas.drawRect(box, pc);
					
				}
			}
			return;
		}
		canvas.drawBitmap(bitmap, src,  dst, p);
		/*
		if (label!=null && label.trim().length()>0) {
			canvas.drawText(label, x, y + textPaint.getTextSize()/4, textPaint);
		}
		*/
	}
	
	@Override
	public String toString() {
		return "{label:" + label + ", type:" + type.name() + ", action:" + action.name() + " (" + x + ", " + y + ")" + " w:" + width + ", h:" + height + " " +(bitmap!=null?"BITMAPEPD":"INVISIBLE") 
				+ (eventIndexes == null?" NO Events": " " + (eventIndexes.length) + " Events") + "}";
	}

	public boolean contains(int px, int py) {
		if (type == ButtonType.CIRCLE) {
			return inCircle(x, y, px, py);
		} else {
			return box.contains(px, py);
		}
	}
	
	private boolean inCircle(int center_x, int center_y, float x, float y) {
		double range = (Math.pow((center_x - x),2) / radiusX) + (Math.pow((center_y - y),2) / radiusY);
		return range <=1;
	}
	
	public void updateAnalog(int x, int y) {
		float dx = x-this.x;
		float dy = this.y - y; // y axis is inverted
		analogX = dx / (width*pct);
		analogY = dy / (height*pct);
		if (analogX> 1.0) analogX =  1.0f;
		if (analogX<-1.0) analogX = -1.0f;
		if (analogY> 1.0) analogY =  1.0f;
		if (analogY<-1.0) analogY = -1.0f;

		if (dx<-width) dx = -width;
		else if (dx>width) dx = width;
		
		if (dy<-height) dy = -height;
		else if (dy>height) dy = height;
		
		int centerX = (int)(this.x + dx);
		int centerY = (int)(this.y - dy);
		
		dst.left   = centerX - width;
		dst.top    = centerY - height;
		dst.right  = centerX + width;
		dst.bottom = centerY + height;

	}

}
