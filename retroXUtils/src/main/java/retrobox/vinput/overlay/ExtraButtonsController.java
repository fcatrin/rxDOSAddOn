package retrobox.vinput.overlay;

import android.view.MotionEvent;

public class ExtraButtonsController {
    public boolean onTouchEvent(MotionEvent event) {

        final int action = event.getAction();
		final int pointerId = ((event.getAction() & MotionEvent.ACTION_POINTER_ID_MASK) >> MotionEvent.ACTION_POINTER_ID_SHIFT);
		int xp = (int)event.getX(pointerId);
        int yp = (int)event.getY(pointerId);

        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: {
            	if (OverlayExtra.onExtraButtonPress(pointerId, xp, yp)) return true;
            	break;
            }
            case MotionEvent.ACTION_UP: {
            	if (OverlayExtra.onExtraButtonRelease(pointerId)) return true;
            	break;
            }
        }
        return false;
    }
}
