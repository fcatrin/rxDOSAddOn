package retrobox.themes;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.Shape;

/*
 * From http://betaful.com/post/82668809883/programmatic-shapes-in-android
 */

public class CustomShapeDrawable extends ShapeDrawable {
	Paint fillPaint;
	Paint strokePaint;
	private int strokeWidth;

	public CustomShapeDrawable(Shape s, int fillColor, int strokeColor, int strokeWidth) {
		super(s);
        fillPaint = new Paint(this.getPaint());
        fillPaint.setColor(fillColor);
        if (strokeColor != fillColor) {
	        strokePaint = new Paint(fillPaint);
	        strokePaint.setStyle(Paint.Style.STROKE);
	        strokePaint.setStrokeWidth(strokeWidth);
	        strokePaint.setColor(strokeColor);
	        this.strokeWidth = strokeWidth;
        }
	}

	public CustomShapeDrawable(Shape s) {
		this(s, 0, 0, 0);
	}

	@Override
    protected void onDraw(Shape shape, Canvas canvas, Paint paint) {
		canvas.save();
		
		// shape.resize(canvas.getClipBounds().right, canvas.getClipBounds().bottom);
	    shape.draw(canvas, fillPaint);

	    Matrix matrix = new Matrix();
	    matrix.setRectToRect(new RectF(0, 0, 
	    		canvas.getClipBounds().right,
	            canvas.getClipBounds().bottom),
	            new RectF(strokeWidth/2, strokeWidth/2, canvas.getClipBounds().right - strokeWidth/2,
	                    canvas.getClipBounds().bottom - strokeWidth/2),
	            Matrix.ScaleToFit.FILL);
	    
	    canvas.concat(matrix);
	    if (strokePaint != null) {
	    	shape.draw(canvas, strokePaint);
	    }
	    
	    canvas.restore();
	}
}
