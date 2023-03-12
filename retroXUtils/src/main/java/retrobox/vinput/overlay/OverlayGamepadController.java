package retrobox.vinput.overlay;

import android.view.MotionEvent;

public class OverlayGamepadController  {

    public boolean onTouchEvent(MotionEvent event) {

        final int action = event.getActionMasked();
		final int pointerIndex = event.getActionIndex();
		int xp = (int)event.getX(pointerIndex);
        int yp = (int)event.getY(pointerIndex);
        int pointerId = event.getPointerId(pointerIndex);

        switch (action) {
        	case MotionEvent.ACTION_MOVE:
        		boolean handled = false;
        		for(int i = 0; i<event.getPointerCount(); i++) {
        			pointerId = event.getPointerId(i);
        			xp = (int)event.getX(i);
        			yp = (int)event.getY(i);
            		if (Overlay.onPointerMove(pointerId, xp, yp)) handled = true;
        		}
        		return handled;
        	case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_DOWN: {
            	if (Overlay.onPointerDown(pointerId, xp, yp)) return true;
            	break;
            }
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_UP: {
            	if (Overlay.onPointerUp(pointerId)) return true;
            	break;
            }
        }

        return false;
    }

}
