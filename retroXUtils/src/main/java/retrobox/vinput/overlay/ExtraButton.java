package retrobox.vinput.overlay;

import retrobox.vinput.VirtualEvent;

public class ExtraButton {
	public static float textSize;
	public int x;
	public int y;
	public int w;
	public int h;
	public String label;
	public int color;
	public int colorPressed;
	public boolean pressed;
	public int pointerId = Overlay.POINTER_ID_NONE;
	
	public boolean visible = true;
	
	public VirtualEvent event;
}
