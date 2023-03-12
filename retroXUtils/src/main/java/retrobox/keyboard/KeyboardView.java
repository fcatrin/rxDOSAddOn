package retrobox.keyboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import retrobox.utils.R;
import retrobox.utils.RetroBoxUtils;
import xtvapps.core.AndroidFonts;
import xtvapps.core.SimpleCallback;
import xtvapps.core.Utils;

public class KeyboardView extends FrameLayout {
	
	public static final String SWITCH_LAYOUT = "SWITCH_LAYOUT_";
	public static final String TOGGLE_POSITION = "TOGGLE_POSITION";

	private List<KeyboardLayout> keylayouts = new ArrayList<KeyboardLayout>();
	private VirtualKeyListener onKeyListener;
	int activeLayout = 0;
	private SimpleCallback onTogglePositionCallback;

	public KeyboardView(Context context) {
		super(context);
	}

	public KeyboardView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public KeyboardView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public void init(Context ctx, KeyboardLayout keyboradLayout[]) {
		Log.d("KEYB", "init");
		keylayouts.addAll(Arrays.asList(keyboradLayout));
		if (keylayouts.size() > 0) switchLayout(ctx, 0);
	}
	
	private void switchLayout(final Context ctx, int index) {
		activeLayout = index;
		
		removeAllViews();
		
		OnClickListener listener = new OnClickListener() {
			@Override
			public void onClick(View view) {
				String keyCode = (String)view.getTag();
				Log.d("KEYB", "keyCode " + keyCode);
				if (keyCode.startsWith(SWITCH_LAYOUT)) {
					int layout = Utils.str2i(keyCode.substring(SWITCH_LAYOUT.length()));
					switchLayout(ctx, layout);
					getChildAt(0).requestFocus();
					return;
				}
				if (keyCode.equals(TOGGLE_POSITION)) {
					if (onTogglePositionCallback!=null)	onTogglePositionCallback.onResult();
					return;
				}
				if (onKeyListener!=null) {
					onKeyListener.onKeyPressed(keyCode);
				}
			}
		};
		
		KeyboardLayout keylayout = keylayouts.get(index);
		for(List<KeyDef> row : keylayout.getKeys()) {
			for(KeyDef keydef : row) {
				final Button view = new Button(new ContextThemeWrapper(ctx, R.style.KeyButton), null, 0);
				view.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));

				/*
				view.setBackgroundDrawable(null);
				view.setMinHeight(0);
				view.setMinWidth(0);s
				view.setPadding(0, 0, 0, 0);
				// view.setBackgroundResource(R.drawable.button_bar);
				view.setTextSize(TypedValue.COMPLEX_UNIT_PX, ctx.getResources().getDimensionPixelSize(R.dimen.text_normal));
				*/
				view.setGravity(Gravity.CENTER);
				view.setText(keydef.getLabel());
				view.setTag(keydef.getValue());
				view.setFocusable(true);
				// view.setFocusableInTouchMode(true);
				AndroidFonts.setViewFont(view, RetroBoxUtils.FONT_DEFAULT_R);
				keydef.setView(view);
				
				view.setOnClickListener(listener);
				
				addView(view);

			}
		}
	}
	
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		Log.d("KEYB", "onLayout "  + l + "," + t + " " + r + ", " + b);
		
		if (keylayouts.size() == 0) return;
		KeyboardLayout keylayout = keylayouts.get(activeLayout);
		
		int width  = r - l - getPaddingLeft() - getPaddingRight();
		int height = b - t - getPaddingTop() - getPaddingBottom();
		int top  = getPaddingTop();
		
		int rowHeight = height / keylayout.getKeys().size();
		
		for(List<KeyDef> row : keylayout.getKeys()) {
			int totalSize = 0;
			for(KeyDef keydef : row) {
				totalSize += keydef.getSize();
			}
			
			int keySize = width / totalSize;
			int left = getPaddingLeft();
			for(int k=0; k<row.size(); k++) {
				KeyDef keydef = row.get(k);
				int w = keySize*keydef.getSize();
				
				if (k+1 == row.size()) {
					// fill all remaining space
					w = width - left + getPaddingLeft();
				}
				
				Button view = (Button)keydef.getView();
				view.layout(left, top, left + w, top + rowHeight);

				// force relayout inside the button
				view.measure(
						MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY), 
						MeasureSpec.makeMeasureSpec(rowHeight, MeasureSpec.EXACTLY));
				
				Log.d("KEYB", "onLayout " + keydef.getLabel() + " " + left + "," + top + " " + w + "," + rowHeight);
				left += w;
			}
			top += rowHeight;
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		Log.d("KEYB", "onMeasure " + widthMeasureSpec + ", " + heightMeasureSpec);
		if (keylayouts.size() == 0) {
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
			return;
		}
		
		KeyboardLayout keylayout = keylayouts.get(activeLayout);
		
		int buttonHeight = getScreenHeight() / 18;
		
		int height = keylayout.getKeys().size() * buttonHeight + getPaddingTop() + getPaddingBottom();
		int width = MeasureSpec.getSize(widthMeasureSpec);
		setMeasuredDimension(width, height);
		Log.d("KEYB", "onMeasured " + width + "," + height);
	}
	
	public void setOnVirtualKeyListener(VirtualKeyListener listener) {
		this.onKeyListener = listener;
	}
	
	public void setOnTogglePositionCallback(SimpleCallback callback) {
		this.onTogglePositionCallback = callback;
	}
	
	private int getScreenHeight() {
		DisplayMetrics displayMetrics = new DisplayMetrics();
		
		((Activity) getContext()).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		
		return displayMetrics.heightPixels;
	}
	
}
