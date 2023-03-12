package retrobox.vinput;

import retrobox.vinput.SwipeListener.Swipe;
import android.view.GestureDetector;
import android.view.MotionEvent;

class SwipeDetector extends GestureDetector.SimpleOnGestureListener {
    private static final String DEBUG_TAG = SwipeDetector.class.getSimpleName();
    
	float minDistanceX = 0.25f;
	float minDistanceY = 0.25f;
	float minVelocityX = 1.0f;
	float minVelocityY = 1.0f;
	float screenWidth = 0;
	float screenHeight = 0;
    
    private SwipeListener listener;
    
    public SwipeDetector(SwipeListener listener, int screenWidth, int screenHeight) {
    	this.listener = listener;
    	this.screenWidth = screenWidth;
    	this.screenHeight = screenHeight;
    }
    
    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
    	if (e1 == null || e2 == null) return false;
    	
    	final float distanceX = Math.abs(e1.getX() - e2.getX()) / screenWidth;
    	final float distanceY = Math.abs(e1.getY() - e2.getY()) / screenHeight;
    	
    	velocityX = velocityX / screenWidth;
    	velocityY = velocityY / screenHeight;
    	
    	//Log.d(DEBUG_TAG, "onFlingX dst:" + distanceX + ", velocity:" + Math.abs(velocityX));
    	//Log.d(DEBUG_TAG, "onFlingY dst:" + distanceY + ", velocity:" + Math.abs(velocityY));
    	if (Math.abs(velocityX)>minVelocityX && distanceX>minDistanceX) {
    		if (velocityX<0) listener.onSwipe(Swipe.Left);
    		else listener.onSwipe(Swipe.Right);
    		return true;
    	}
    	if (Math.abs(velocityY)>minVelocityY && distanceY>minDistanceY) {
    		if (velocityY<0) listener.onSwipe(Swipe.Up);
    		else listener.onSwipe(Swipe.Down);
    		return true;
    	}
    	return true;

    }
}

