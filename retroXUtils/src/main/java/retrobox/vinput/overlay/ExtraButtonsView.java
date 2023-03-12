package retrobox.vinput.overlay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;

public class ExtraButtonsView extends View {

	Paint mTextPaint;
	private ViewGroup _viewGroup;
	
	public ExtraButtonsView(Context context) {
		super(context);
		mTextPaint = new Paint();
		mTextPaint.setTextSize(15 * getResources().getDisplayMetrics().density);
		mTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
		mTextPaint.setTextAlign(Paint.Align.CENTER);
		mTextPaint.setStyle(Paint.Style.FILL);
		mTextPaint.setSubpixelText(false); 
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		OverlayExtra.drawExtraButtons(canvas, mTextPaint);
	}
	
    public void addToLayout(ViewGroup viewGroup) {
        if (_viewGroup == null) {
            _viewGroup = viewGroup;
        }
    }
    
    public void hidePanel() {
        if (_viewGroup != null) {
            _viewGroup.removeView(this);
        }
    }
    public void showPanel() {
        if (_viewGroup != null) {
            if (getParent() != null) _viewGroup.removeView(this);
            _viewGroup.addView(this);
        }
    }

    public boolean isVisible() {
        return (null != getParent());
    }
    
    public void toggleView() {
    	if (isVisible()) hidePanel();
    	else showPanel();
    }


}
