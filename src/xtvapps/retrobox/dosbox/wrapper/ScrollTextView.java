package xtvapps.retrobox.dosbox.wrapper;

import android.content.Context;
import android.graphics.Rect;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.animation.LinearInterpolator;
import android.widget.Scroller;
import android.widget.TextView;
import xtvapps.core.SimpleCallback;

public class ScrollTextView extends TextView {

    // scrolling feature
    private Scroller mSlr;

    // milliseconds for a round of scrolling
    private int mDuration = 1000;

    // whether it's being paused
    private boolean mRunning = false;

    private SimpleCallback onFinishedCallback;
    
    /*
    * constructor
    */
    public ScrollTextView(Context context) {
        this(context, null);
        // customize the TextView
        setSingleLine();
        setEllipsize(null);
        setVisibility(INVISIBLE);
    }

    /*
    * constructor
    */
    public ScrollTextView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.textViewStyle);
        // customize the TextView
        setSingleLine();
        setEllipsize(null);
        setVisibility(INVISIBLE);
    }

    /*
    * constructor
    */
    public ScrollTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        // customize the TextView
        setSingleLine();
        setEllipsize(null);
        setVisibility(INVISIBLE);
    }

    public void startScroll() {
    	mRunning = false;
    	resumeScroll();
    }
    
    /**
    * resume the scroll from the pausing point
    */
    public void resumeScroll() {

        if (mRunning) return;

        // Do not know why it would not scroll sometimes
        // if setHorizontallyScrolling is called in constructor.
        setHorizontallyScrolling(true);

        // use LinearInterpolator for steady scrolling
        mSlr = new Scroller(this.getContext(), new LinearInterpolator());
        setScroller(mSlr);

        int width = (int)(getMeasuredWidth()*1.25f);
        
        int scrollingLen = calculateScrollingLen();
        int distance = scrollingLen + width*2;

        setVisibility(VISIBLE);
        mSlr.startScroll(-width, 0, distance, 0, mDuration);
        invalidate();
        mRunning = true;
        
    }

    /**
    * calculate the scrolling length of the text in pixel
    *
    * @return the scrolling length in pixels
    */
    private int calculateScrollingLen() {
        TextPaint tp = getPaint();
        Rect rect = new Rect();
        String strTxt = getText().toString();
        tp.getTextBounds(strTxt, 0, strTxt.length(), rect);
        return rect.width();
    }

    @Override
    /*
    * override the computeScroll to restart scrolling when finished so as that
    * the text is scrolled forever
    */
    public void computeScroll() {
        super.computeScroll();

        if (null == mSlr) return;

        if (mSlr.isFinished() && (mRunning)) {
        	if (onFinishedCallback!=null) onFinishedCallback.onResult();
        	this.startScroll();
        }
    }
    
    public void stopScroll() {
    	mRunning = false;
    	mSlr.abortAnimation();
    }

    public int getDuration() {
      return mDuration;
    }

    public void setDuration(int duration) {
      this.mDuration = duration;
    }

	public void setOnFinishedCallback(SimpleCallback onFinishedCallback) {
		this.onFinishedCallback = onFinishedCallback;
	}

}