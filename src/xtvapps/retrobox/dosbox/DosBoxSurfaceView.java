/*
 *  Copyright (C) 2013 Fishstix (android.fishstix@gmail.com)
 *  
 *  Copyright (C) 2011 Locnet (android.locnet@gmail.com)
 *  
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package xtvapps.retrobox.dosbox;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.TextView;
import android.widget.Toast;
import retrobox.vinput.Mapper;
import xtvapps.dosbox.swos.R;
import xtvapps.retrobox.dosbox.library.dosboxprefs.DosBoxPreferences;
import xtvapps.retrobox.dosbox.touchevent.TouchEventWrapper;


class DosBoxSurfaceView extends SurfaceView implements SurfaceHolder.Callback {
	private final static int DEFAULT_WIDTH = 640;//800;
	private final static int DEFAULT_HEIGHT = 400;//600; 
	public int mJoyCenterX = 0;
	public int mJoyCenterY = 0;
	
	private final static int ONSCREEN_BUTTON_WIDTH = 4;
	private final static int ONSCREEN_BUTTON_HEIGHT = 5;
 
	private final static int BUTTON_TAP_TIME_MIN = 10; 
	private final static int BUTTON_TAP_TIME_MAX = 300;
	private final static int BUTTON_TAP_DELAY = 200;	
	private final static int BUTTON_REPEAT_DELAY = 100;	
	private final static int EVENT_THRESHOLD_DECAY = 100;
	private final static int EVENT_THRESHOLD_FINGER_MOVE = 200;
	
	public final static int INPUT_MODE_MOUSE = 0xf1;
	public final static int INPUT_MODE_SCROLL = 0xf2;
	public final static int INPUT_MODE_JOYSTICK = 0xf3;
	public final static int INPUT_MODE_REAL_MOUSE = 0xf4;
	public final static int INPUT_MODE_REAL_JOYSTICK = 0xf5;
	
	public final static int ACTION_DOWN = 0;
	public final static int ACTION_UP = 1;
	public final static int ACTION_MOVE = 2;

	//private static final int MOUSE_MOVE_THRESHOLD = 15;	// in pixels
	
	private static final int MAX_POINT_CNT = 3;
	
	private DosBoxLauncher mParent = null;	
	private boolean mSurfaceViewRunning = false;
	public DosBoxVideoThread mVideoThread = null;
	public KeyHandler mKeyHandler = null;
	public Buffer mVideoBuffer = null;
	private GestureDetector gestureScanner;
	
	boolean mScale = true;   
	int mInputMode = INPUT_MODE_MOUSE;
	boolean	mShowInfo = false;
	public boolean mInfoHide = false;
	//boolean mEmulateClick = false; 
	boolean mEnableDpad = false;
	boolean mAbsolute = true;
	boolean mInputLowLatency = false;
	boolean mUseLeftAltOn = false;
	public boolean mLongPress = true;
	public boolean mDebug = false;
	private static final int CLICK_DELAY = 125;	// in ms (170)
    private boolean mDoubleLong = false;
    public float mMouseSensitivity = 1.0f;
    public float warpX = 1;
    public float warpY = 1;
    public boolean mScreenTop = false;

	
	int mDpadRate = 7;
	private boolean mLongClick = false;
	//boolean mCalibrate = false;
	boolean mMaintainAspect = true;
	//private boolean mHasMoved = false;
	
	int	mContextMenu = 0;

	Bitmap mBitmap = null; 
	private Paint mBitmapPaint = null;
	private Paint mTextPaint = null;
	private Rect mSrcRect = new Rect();
	private Rect mDstRect = new Rect();
	private Rect mDirtyRect = new Rect();
	private Rect mScreenRect = new Rect();

	int mSrc_width = 0;
	int mSrc_height = 0;	
	int dst_width = 0;
	int dst_height = 0;
	int dst_left = 0;
	int dst_top = 0;
	int	screen_width=DEFAULT_WIDTH, screen_height=DEFAULT_HEIGHT;
	
	private int mDirtyCount = 0;
	private int mScroll_x = 0;
	private int mScroll_y = 0;
	
	Boolean mDirty = false;
	boolean isDirty = false;
	boolean isLandscape = false;
	int mStartLine = 0;
	int mEndLine = 0;
	private int bottomrow;
	private boolean mFilterLongClick = false;

	boolean mModifierCtrl = false;
	boolean mModifierAlt = false;
	boolean mModifierShift = false;
	//private int mKeyboardType = Configuration.KEYBOARD_NOKEYS;
	 
	boolean showFPS = false;
	
	class KeyHandler extends Handler {
		boolean mReCheck = false;
		
		@Override
		public void handleMessage (Message msg) {
			if (DosBoxControl.sendNativeKey(msg.what, false, mModifierCtrl, mModifierAlt, mModifierShift)) {
				mModifierCtrl = false;
				mModifierAlt = false;
				mModifierShift = false;					
			}
		}		
	}

	class DosBoxVideoThread extends Thread {
		
		private float targetFps = 60.0f;
		private float updateInterval;
		private float resetInterval;
		
		private static final float UPDATE_INTERVAL_MIN = 1000.0f / 60.0f;
		private final DecimalFormat fpsFormat = new DecimalFormat("#00");
		
		private boolean mVideoRunning = false;

		private long startTime = 0;
		private int frameCount = 0;
		private long curTime, nextUpdateTime, sleepTime;
		
		private long fpsLastTime = 0;
		private int fpsCount = 0;
		public int fpsCountInternal = 0;
		private Handler fpsHandler;
		private TextView txtFPS;
		
		public DosBoxVideoThread(Handler fpsHandler, TextView txtFPS) {
			this.fpsHandler = fpsHandler;
			this.txtFPS = txtFPS;
			
			setTargetFps(targetFps);
		}
		
		void setRunning(boolean running) {
			mVideoRunning = running;
		}
		
		public void setTargetFps(float fps) {
			synchronized (mDirty) {
				targetFps = fps;
				updateInterval = 1000.0f / targetFps;
				resetInterval = fps * 4;
			}
		}
		
		public void run() {
			mVideoRunning = true;
			while (mVideoRunning) {
				if (mSurfaceViewRunning) {

					curTime = System.currentTimeMillis();

					if (frameCount > resetInterval)
						frameCount = 0;					
					
					if (frameCount == 0) {
						startTime = curTime;
					}
					
					frameCount++;
					
					//if (mDebug) {
					//	Log.d("dosbox", "fps:" + 1000 * frameCount / (curTime - startTime));
					//}
					
					fpsCount++;
					if (showFPS && (curTime - fpsLastTime) > 1000) {
						final float fps = fpsCount > targetFps ? targetFps : fpsCount; // avoid problems for TOC people
						final float fpsInternal = fpsCountInternal;
						
						fpsCount = 0;
						fpsCountInternal = 0;
						
						fpsLastTime = curTime;
						fpsHandler.post(new Runnable() {
							@Override
							public void run() {
								String sFps = fpsFormat.format(fps);
								String sFpsInternal = fpsFormat.format(fpsInternal);
								txtFPS.setText("FPS Render " + sFps + " / Internal " + sFpsInternal);
							}
						});
					}
				
					synchronized (mDirty) {
						if (mDirty) {
							VideoRedraw(mBitmap, mSrc_width, mSrc_height, mStartLine, mEndLine);
							mDirty = false;				
						}
					}
			        
					try {
						long t0 = System.currentTimeMillis();
						nextUpdateTime = startTime + (long)(frameCount * updateInterval);
						sleepTime = nextUpdateTime - t0;
						// Log.d("SLEEP", "sleep: " + sleepTime + ", start:" + startTime + ", t0:" + t0 + ", next:" + nextUpdateTime + ", frame:" + frameCount + ", interval:" + UPDATE_INTERVAL);
						Thread.sleep((long)Math.max(sleepTime, UPDATE_INTERVAL_MIN));
					} catch (InterruptedException e) {
					}
				}
				else {
					try {
						frameCount = 0;
						Thread.sleep(1000);
					} catch (InterruptedException e) {
					}					
				}
			}
		}		
	}	

	@SuppressWarnings("deprecation")
	public DosBoxSurfaceView(DosBoxLauncher context) {
		super(context);
		mParent = context;
		gestureScanner = new GestureDetector(new MyGestureDetector());
		mBitmapPaint = new Paint();
		mBitmapPaint.setFilterBitmap(true);		
		mTextPaint = new Paint();
		mTextPaint.setTextSize(15 * getResources().getDisplayMetrics().density);
		mTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
		mTextPaint.setTextAlign(Paint.Align.CENTER);
		mTextPaint.setStyle(Paint.Style.FILL);
		mTextPaint.setSubpixelText(false); 
		
		mBitmap = Bitmap.createBitmap(DEFAULT_WIDTH, DEFAULT_HEIGHT, Bitmap.Config.RGB_565);
  
		//2011-04-28, support 2.1 or below
		mVideoBuffer = ByteBuffer.allocateDirect(DEFAULT_WIDTH * DEFAULT_HEIGHT * 2);

		showFPS = context.getIntent().getBooleanExtra("showFPS", false);
		TextView txtFPS = (TextView)context.findViewById(R.id.txtFPS);
		txtFPS.setVisibility(showFPS ? View.VISIBLE : View.GONE);
		txtFPS.setText("");
		
		mVideoThread = new DosBoxVideoThread(new Handler(), txtFPS);
		//mVideoThread.setPriority(5);
		if (mDebug)
			Log.i("DosBoxTurbo","Video Priority: " + mVideoThread.getPriority());
		mKeyHandler = new KeyHandler(); 				
	  
		// Receive keyboard events
		requestFocus();
		setFocusableInTouchMode(true);
		setFocusable(true);
		requestFocus(); 
		requestFocusFromTouch();
	
		getHolder().addCallback(this);
		getHolder().setFormat(PixelFormat.RGB_565);
		getHolder().setKeepScreenOn(true);
		//mKeyboardType = getResources().getConfiguration().keyboard;
		//setOnLongClickListener(this);
		
		
	}
	
	public void shutDown() {
		mBitmap = null;		
		mVideoThread = null;
		mKeyHandler = null;		
	}

	int tmp = 0;
	int tmp2 = 0;
	@TargetApi(26)
	public void VideoRedraw(Bitmap bitmap, int src_width, int src_height, int startLine, int endLine) {
		if (!mSurfaceViewRunning || (bitmap == null) || (src_width <= 0) || (src_height <= 0))
			return;
	
		SurfaceHolder surfaceHolder = getHolder();
		Canvas canvas = null;
	 
		try {
			synchronized (surfaceHolder)
			{
				dst_width = getWidth();
				dst_height = getHeight();
				isDirty = false;
				isLandscape = (dst_width > dst_height);
	
				//if (mShowInfo)
				if (mShowInfo || mParent.mPrefFullScreenUpdate) {
					mDirtyCount = 0;
				}
					
				if (mDirtyCount < 3) {
					mDirtyCount++;
					isDirty =  true;
					startLine = 0;
					endLine = src_height;
					// fishstix, update screendata for absolute mode
					mScreenRect.set(mDstRect);
					//if (mDebug)
					//	Log.d("DosBoxTurbo","DNAME="+DosBoxControl.nativeAbsoluteDName());
					//setWarpFactor(DosBoxControl.nativeAbsoluteDName());
				}
				
				if (mScale) {
					if (!mMaintainAspect && isLandscape) {
						tmp = 0;
					} else {
						tmp = src_width * dst_height /src_height;
						
						if (tmp < dst_width) {
							dst_width = tmp;
						}
						else if (tmp > dst_width) {
							dst_height = src_height * dst_width /src_width;
						}
						tmp = (getWidth() - dst_width)/2;
					}
					
					if (isLandscape) {
						dst_width *= (mParent.mPrefScaleFactor * 0.01f);
						dst_height *= (mParent.mPrefScaleFactor * 0.01f);
						tmp = (getWidth() - dst_width)/2;
						if (!mScreenTop)
							tmp2 = (getHeight() - dst_height)/2;
						else 
							tmp2 = 0;
					} else {
						tmp2 = 0;
					}
					
					mSrcRect.set(0, 0, src_width, src_height);
					mDstRect.set(0, 0, dst_width, dst_height);
					mDstRect.offset(tmp, tmp2);
					
					mDirtyRect.set(0, startLine * dst_height / src_height, dst_width, endLine * dst_height / src_height+1);
					
					//locnet, 2011-04-21, a strip on right side not updated
					mDirtyRect.offset(tmp, tmp2);
				} else {
					if ((mScroll_x + src_width) < dst_width)
						mScroll_x = dst_width - src_width;
	
					if ((mScroll_y + src_height) < dst_height)
						mScroll_y = dst_height - src_height;
	
					mScroll_x = Math.min(mScroll_x, 0);
					mScroll_y = Math.min(mScroll_y, 0);
					
					//mSrcRect.set(-offx, -offy, Math.min(dst_width - offx, src_width), Math.min(dst_height - offy, src_height));
					mSrcRect.set(-mScroll_x, Math.max(-mScroll_y, startLine), Math.min(dst_width - mScroll_x, src_width), Math.min(Math.min(dst_height - mScroll_y, src_height), endLine));
	
					dst_width = mSrcRect.width();					
					dst_height = mSrcRect.height();
					
					mDstRect.set(0, mSrcRect.top + mScroll_y, dst_width, mSrcRect.top + mScroll_y + dst_height);
	
					mDstRect.offset((getWidth() - dst_width)/2, 0);
					
					mDirtyRect.set(mDstRect);
				}						
				
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
					if (isDirty) {
						canvas = surfaceHolder.lockHardwareCanvas();
						canvas.drawColor(0xff000000);
					}
					else {
						canvas = surfaceHolder.lockHardwareCanvas();
					}
				} else { 
					if (isDirty) {
						canvas = surfaceHolder.lockCanvas(null);
						canvas.drawColor(0xff000000);
					}
					else {
						canvas = surfaceHolder.lockCanvas(mDirtyRect);
					}
				}
				
				synchronized (mBitmap) {
					if (mScale) {
						canvas.drawBitmap(bitmap, mSrcRect, mDstRect, (mParent.mPrefScaleFilterOn)?mBitmapPaint:null);
					} else {
						canvas.drawBitmap(bitmap, mSrcRect, mDstRect, null);					
					}
				}
				/*
				if (mShowInfo) {
					screen_width = getWidth();
					screen_height = getHeight();
					if (mInfoHide) {
						drawButton(canvas, ONSCREEN_BUTTON_WIDTH, screen_height-50, ONSCREEN_BUTTON_WIDTH*10, screen_height, "+");						
					} else {
						int but_height = (int) (screen_height*(ONSCREEN_BUTTON_HEIGHT-1)/ONSCREEN_BUTTON_HEIGHT);
						drawButton(canvas, 0, but_height, screen_width/ONSCREEN_BUTTON_WIDTH, screen_height, "Hide");
						drawButton(canvas, screen_width/ONSCREEN_BUTTON_WIDTH, but_height, screen_width * 2/ONSCREEN_BUTTON_WIDTH, screen_height, "Special");
						drawButton(canvas, screen_width * 2/ONSCREEN_BUTTON_WIDTH, but_height, screen_width*3/ONSCREEN_BUTTON_WIDTH, screen_height, "Btn 1");
						drawButton(canvas, screen_width * 3/ONSCREEN_BUTTON_WIDTH, but_height, screen_width, screen_height, "Btn 2");
					}
				}
				*/
				
				ensureMouseCalibration();
			}
		} finally {
			if (canvas != null) {
				surfaceHolder.unlockCanvasAndPost(canvas);
			}
		}
		
		surfaceHolder = null;
	}
	
	public void copyPixels() {
		//2011-04-28, support 2.1 or below
		if (mVideoBuffer != null) {
			mVideoBuffer.position(0);
			synchronized (mBitmap) {
				if (mBitmap.getWidth()*mBitmap.getHeight()*2 == mVideoBuffer.remaining())
					mBitmap.copyPixelsFromBuffer(mVideoBuffer);
			}
		}
	}
	
	private int[] mButtonDown = new int[MAX_POINT_CNT];

	private final static int ONSCREEN_BUTTON_SPECIAL_KEY = 33;
	private final static int ONSCREEN_BUTTON_HIDE = 34;
	public final static int BTN_A = 0;
	public final static int BTN_B = 1;
	
	float[] x = new float[MAX_POINT_CNT];
	float[] y = new float[MAX_POINT_CNT];
	//boolean[] isTouch = new boolean[MAX_POINT_CNT];
	   
	float[] x_last = new float[MAX_POINT_CNT];
	float[] y_last = new float[MAX_POINT_CNT];
	//boolean[] isTouch_last = new boolean[MAX_POINT_CNT];
	boolean[] virtButton = new boolean[MAX_POINT_CNT];
	//int pointerIndex,pointCnt,pointerId,source;
	//private int moveId = -1;

	private TouchEventWrapper mWrap = TouchEventWrapper.newInstance();
	//private volatile boolean mMouseBusy = false;
		
	@Override
	public boolean onGenericMotionEvent(MotionEvent event) {
		if (event.getEventTime()+EVENT_THRESHOLD_DECAY < SystemClock.uptimeMillis()) {
			Log.i("DosBoxTurbo","eventtime: "+event.getEventTime() + " systemtime: "+SystemClock.uptimeMillis());
			return true;	// get rid of old events
		}
		//final int pointerIndex = ((event.getAction() & MotionEvent.ACTION_POINTER_ID_MASK) >> MotionEvent.ACTION_POINTER_ID_SHIFT);
		//final int pointCnt = mWrap.getPointerCount(event);
		final int pointerId = mWrap.getPointerId(event, ((event.getAction() & MotionEvent.ACTION_POINTER_ID_MASK) >> MotionEvent.ACTION_POINTER_ID_SHIFT));
		//final int source = (mWrap.getSource(event) & TouchEventWrapper.SOURCE_CLASS_MASK);
		switch(mInputMode) {

			case INPUT_MODE_REAL_JOYSTICK:
				if ((event.getAction() == MotionEvent.ACTION_MOVE) &&  ((mWrap.getSource(event) & TouchEventWrapper.SOURCE_CLASS_MASK) == TouchEventWrapper.SOURCE_CLASS_JOYSTICK)) {
				    x[pointerId] = mWrap.getX(event, pointerId);
				    y[pointerId] = mWrap.getY(event, pointerId);
					DosBoxControl.nativeJoystick((int)((x[pointerId]*256f)+mJoyCenterX), (int)((y[pointerId]*256f)+mJoyCenterY-50), 2, -1);
					if (mDebug)
						Log.d("DosBoxTurbo","onGenericMotionEvent() INPUT_MODE_REAL_JOYSTICK x: " + (int)((x[pointerId]*256f)+mJoyCenterX) + "  y: " + (int)((y[pointerId]*256f)+mJoyCenterY) + "  |  xL: "+ x[pointerId] + "  yL: "+ y[pointerId]);
					//return true;
				}
				break;  
			case INPUT_MODE_REAL_MOUSE: 
				// pointer movement
				if ((event.getAction() == TouchEventWrapper.ACTION_HOVER_MOVE) && ((mWrap.getSource(event) & TouchEventWrapper.SOURCE_CLASS_MASK) == TouchEventWrapper.SOURCE_CLASS_POINTER) ) {
					/*if (mMouseBusy) {
						// fishstix, events coming too fast, consume extra events.
						return true;
					}*/
					//if (pointCnt <= MAX_POINT_CNT) {
						//if (pointerIndex <= MAX_POINT_CNT - 1){
						//mMouseBusy = true; 
							//for (int i = 0; i < pointCnt; i++) {
								//final int id = mWrap.getPointerId(event, );
							    x_last[pointerId] = x[pointerId];
							    y_last[pointerId] = y[pointerId];
							    //isTouch_last[id] = isTouch[id];
							    x[pointerId] = mWrap.getX(event, pointerId);
							    y[pointerId] = mWrap.getY(event, pointerId);
							    
							//} 
							if (mAbsolute) {
								ensureMouseCalibration();
								Log.d("MOUSE", "nativeMouseWarp " + 
										((x[pointerId] - mScreenRect.left) /  mScreenRect.width()) + ", " +
										((y[pointerId] - mScreenRect.top) /  mScreenRect.height()));
								DosBoxControl.nativeMouseWarp(x[pointerId], y[pointerId], mScreenRect.left, mScreenRect.top, (int)(mScreenRect.width() * warpX), (int)(mScreenRect.height() * warpY));
							} else {
								DosBoxControl.nativeMouse((int) (x[pointerId]*mMouseSensitivity), (int) (y[pointerId]*mMouseSensitivity), (int) (x_last[pointerId]*mMouseSensitivity), (int) (y_last[pointerId]*mMouseSensitivity), 2, -1);
							}
							if (mDebug)
								Log.d("DosBoxTurbo","onGenericMotionEvent() INPUT_MODE_REAL_MOUSE x: " + x[pointerId] + "  y: " + y[pointerId] + "  |  xL: "+ x_last[pointerId] + "  yL: "+ y_last[pointerId]);
							try {
						    	if (!mInputLowLatency) 
						    		Thread.sleep(95);
						    	else 
						    		Thread.sleep(65);
							} catch (InterruptedException e) {
							}
							//Thread.yield();
							//mMouseBusy = false;
							return true;
						//}
					//}
				}
				break;
			}
		//return super.onGenericMotionEvent(event);
		return false;
	}
	
	@Override
	public boolean onTouchEvent(final MotionEvent event) {
		
		final int pointerIndex = ((event.getAction() & MotionEvent.ACTION_POINTER_ID_MASK) >> MotionEvent.ACTION_POINTER_ID_SHIFT);
		final int pointCnt = mWrap.getPointerCount(event);
		final int pointerId = mWrap.getPointerId(event, pointerIndex);
		//Log.v("onTouch?", "yeah");
		if (pointCnt <= MAX_POINT_CNT){

			//if (pointerIndex <= MAX_POINT_CNT - 1){
			{
				for (int i = 0; i < pointCnt; i++) {
					int id = mWrap.getPointerId(event, i);
					if (id < MAX_POINT_CNT) {
						x_last[id] = x[id];
						y_last[id] = y[id];
						x[id] = mWrap.getX(event, i);
						y[id] = mWrap.getY(event, i);
					}
				}
	        	int xp = (int)x[pointerId];
	        	int yp = (int)y[pointerId];

				switch (event.getAction() & MotionEvent.ACTION_MASK) {
				case MotionEvent.ACTION_DOWN:
				case TouchEventWrapper.ACTION_POINTER_DOWN:

					int button = -1;
			        if (mInputMode == INPUT_MODE_MOUSE) {
			        	Log.d("MOUSE", "ACTION_POINTER_DOWN");
			        	if (mShowInfo) {
			        		bottomrow = (int)((float)getHeight() * 0.8);
			        		//int toprow = (int)((float)getHeight() * 0.2);

			        		if (y[pointerId] > bottomrow) {
			        			button = (int)(x[pointerId] * ONSCREEN_BUTTON_WIDTH / getWidth());
			        			// bottom row
			        			if (button == 0)
			        				button = ONSCREEN_BUTTON_HIDE;
			        			else if (button == 1)
			        				button = ONSCREEN_BUTTON_SPECIAL_KEY;
			        			else if (button >= 2) {
			        				button = button-2;
			        				if (mInputMode == INPUT_MODE_MOUSE) 
			        					DosBoxControl.nativeMouse(0, 0, 0, 0, ACTION_DOWN, button);
			        				else if (mInputMode == INPUT_MODE_JOYSTICK)
			        					DosBoxControl.nativeJoystick(0, 0, ACTION_DOWN, button);
			        				Log.v("Mouse","BUTTON DOWN: " + button);
			        			}
			        			virtButton[pointerIndex]= true;
			        			mFilterLongClick = true;
			        		} 
			        	}
			        } else if (mInputMode == INPUT_MODE_JOYSTICK) {

					} else if (mInputMode == INPUT_MODE_REAL_JOYSTICK) {
						button = mWrap.getButtonState(event);
						DosBoxControl.nativeJoystick(0, 0, ACTION_DOWN, button);
					} else if (mInputMode == INPUT_MODE_REAL_MOUSE) {
						button = mWrap.getButtonState(event) - 1;
						// handle trackpad presses as button clicks
						if (button == -1) {
							button = 0;		
						} 
						DosBoxControl.nativeMouse(0, 0, 0, 0, ACTION_DOWN, button);
						//Log.v ("Mouse", "BUTTON DOWN - x: " + x[pointerId] + "  y: "+ y[pointerId]);
						//Log.v("Mouse","BUTTON DOWN: " + (button-1));
					}
					mButtonDown[pointerId] = button;
				break;
				case MotionEvent.ACTION_UP: 
				case TouchEventWrapper.ACTION_POINTER_UP:
					long diff = event.getEventTime() - event.getDownTime();
					if (mInputMode == INPUT_MODE_JOYSTICK) {
						if (diff < BUTTON_TAP_DELAY) {
							try {
								Thread.sleep(BUTTON_TAP_DELAY - diff);
								//Thread.sleep(diff);
							} catch (InterruptedException e) {
							}
						}		
					} else
					if (mInputMode == INPUT_MODE_MOUSE){
						if (mLongClick) {
							// single tap long click release
							DosBoxControl.nativeMouse(0, 0, 0, 0, ACTION_UP, mGestureSingleClick-GESTURE_LEFT_CLICK);
							mLongClick = false;
							Log.i("DosBoxTurbo","SingleTap Long Click Release");
							return true;
						} else if (mDoubleLong) {
							// double tap long click release
							try {
								Thread.sleep(CLICK_DELAY);
							} catch (InterruptedException e) {
							}
							DosBoxControl.nativeMouse(0, 0, -1, -1, ACTION_UP, mGestureDoubleClick-GESTURE_LEFT_CLICK);
							Log.i("DosBoxTurbo","DoubleTap Long Click Release");
							mDoubleLong = false;
							//return true;
						} else if (pointCnt == 2) {
							// handle 2 finger tap gesture
							if (mLongPress) {
								if (!mTwoFingerAction) {
									// press button down
									Log.i("DosBoxTurbo","2-Finger Long Click Down");
									DosBoxControl.nativeMouse(0, 0, -1, -1, ACTION_DOWN, mGestureTwoFinger-GESTURE_LEFT_CLICK);
									mTwoFingerAction = true;
								} else {
									// already pressing button - release and press again
									Log.i("DosBoxTurbo","2-Finger Long Click - AGAIN");
									DosBoxControl.nativeMouse(0, 0, -1, -1, ACTION_UP, mGestureTwoFinger-GESTURE_LEFT_CLICK);
									try {
										Thread.sleep(CLICK_DELAY);
									} catch (InterruptedException e) {
									}
									DosBoxControl.nativeMouse(0, 0, -1, -1, ACTION_DOWN, mGestureTwoFinger-GESTURE_LEFT_CLICK);
								}
							} else {
								Log.i("DosBoxTurbo","2-Finger Long Click Down-UP");
								mouseClick(mGestureTwoFinger-GESTURE_LEFT_CLICK);
							}
							return true;
						} else if ((pointCnt == 1)&& mTwoFingerAction) {
			        		// release two finger gesture
							Log.i("DosBoxTurbo","2-Finger Long Click Release");
			        		DosBoxControl.nativeMouse(0, 0, -1, -1, ACTION_UP, mGestureTwoFinger-GESTURE_LEFT_CLICK);
			        		mTwoFingerAction = false;
			        		//return true;
						}
						if (mShowInfo) {
							virtButton[pointerId] = false;
							switch (mButtonDown[pointerId]) {
							case BTN_A:
								if (mInputMode == INPUT_MODE_MOUSE)
									DosBoxControl.nativeMouse(0, 0, 0, 0, ACTION_UP, BTN_A);
								else if (mInputMode == INPUT_MODE_JOYSTICK)
									DosBoxControl.nativeJoystick(0, 0, ACTION_UP, BTN_A);
								return true;
							case BTN_B:
								if (mInputMode == INPUT_MODE_MOUSE)
									DosBoxControl.nativeMouse(0, 0, 0, 0, ACTION_UP, BTN_B);
								else if (mInputMode == INPUT_MODE_JOYSTICK)
									DosBoxControl.nativeJoystick(0, 0, ACTION_UP, BTN_B);
								return true;
							case ONSCREEN_BUTTON_SPECIAL_KEY:
								if ((diff > BUTTON_TAP_TIME_MIN) && (diff < BUTTON_TAP_TIME_MAX)) {				
									mContextMenu = DosBoxMenuUtility.CONTEXT_MENU_SPECIAL_KEYS;
									mParent.openContextMenu(this);
								}
								return true;
							case ONSCREEN_BUTTON_HIDE:
								DosBoxMenuUtility.doShowHideInfo(mParent, !mInfoHide);
								return true;
							}
						} 
					}
					else if (mInputMode == INPUT_MODE_REAL_MOUSE) {
						//Log.v("Mouse","BUTTON UP: " + (mButtonDown[pointerId]));
						DosBoxControl.nativeMouse(0, 0, 0, 0, ACTION_UP, mButtonDown[pointerId]);
						return true;
					}
					else if (mInputMode == INPUT_MODE_REAL_JOYSTICK) {
						DosBoxControl.nativeJoystick(0, 0, ACTION_UP, (mButtonDown[pointerId]));
						return true;
					}
				break;
				case MotionEvent.ACTION_MOVE: 
					//isTouch[pointerId] = true;
					switch(mInputMode) {
						case INPUT_MODE_SCROLL:
							mScroll_x += (int)(x[pointerId] - x_last[pointerId]);
							mScroll_y += (int)(y[pointerId] - y_last[pointerId]);
							forceRedraw();
						break;
						case INPUT_MODE_JOYSTICK:
							int newPointerId;
							for(int i = 0; i < pointCnt; ++i) {
								newPointerId = mWrap.getPointerId(event,i);
								// if (Overlay.onJoystickOverlayPress(newPointerId, (int)x[pointerId], (int)y[pointerId])) return true;
							}
						break;
						case INPUT_MODE_MOUSE: 
						case INPUT_MODE_REAL_MOUSE:
							Log.d("MOUSE", "ACTION_MOVE");
							if (event.getEventTime()+EVENT_THRESHOLD_DECAY < SystemClock.uptimeMillis()) {
								Log.i("DosBoxTurbo","eventtime: "+event.getEventTime() + " systemtime: "+SystemClock.uptimeMillis());
								return true;	// get rid of old events
							}
							
							// ignore natural move triggered by finger when clicking
							if (event.getEventTime() - event.getDownTime() < EVENT_THRESHOLD_FINGER_MOVE) {
								Log.i("DosBoxTurbo", "Discarding move");
								return true;
							}
							
							int idx = (!virtButton[0]) ? 0:1;
							if (mAbsolute) {
								ensureMouseCalibration();
								// Log.d("MOUSE", "nativeMouseWarp " + x[idx] + ", " + y[idx] + " (" + mScreenRect.left + ", " + mScreenRect.top + ") - (" + mScreenRect.width() + "x" + mScreenRect.height() + ")");
								Log.d("MOUSE", "nativeMouseWarp " + 
										((x[pointerId] - mScreenRect.left) /  mScreenRect.width()) + ", " +
										((y[pointerId] - mScreenRect.top) /  mScreenRect.height()));

								DosBoxControl.nativeMouseWarp(x[idx], y[idx], mScreenRect.left, mScreenRect.top, (int)(mScreenRect.width()*warpX), (int)(mScreenRect.height()*warpY));
							} else {
								Log.d("MOUSE", "nativeMouse " + (int) (x[idx]*mMouseSensitivity) + ", " +  (int) (y[idx]*mMouseSensitivity) + ", " + (int) (x_last[idx]*mMouseSensitivity) + ", " +  (int) (y_last[idx]*mMouseSensitivity));
								DosBoxControl.nativeMouse((int) (x[idx]*mMouseSensitivity), (int) (y[idx]*mMouseSensitivity), (int) (x_last[idx]*mMouseSensitivity), (int) (y_last[idx]*mMouseSensitivity), ACTION_MOVE, -1);
							}
							try {
						    	if (!mInputLowLatency) 
						    		Thread.sleep(95);
						    	else
						    		Thread.sleep(65);  
							} catch (InterruptedException e) {
							}

						break;
						default:
					}
				break;
				}
			}
		}
	    try {
	    	Thread.sleep(15);
	    } catch (InterruptedException e) {
	        e.printStackTrace();
	    } 
	    //Thread.yield();
	    return gestureScanner.onTouchEvent(event);
	}

	private final static int MAP_EVENT_CONSUMED = -1;
	private final static int MAP_NONE = 0;
	private final static int MAP_LEFTCLICK 	= 20000;
	private final static int MAP_RIGHTCLICK 	= 20001;
	private final static int MAP_CYCLEUP 		= 20002;
	private final static int MAP_CYCLEDOWN	= 20003;
	private final static int MAP_SHOWKEYBOARD = 20004;
	private final static int MAP_SPECIALKEYS	= 20005;
	private final static int MAP_ADJUSTCYCLES	= 20006;
	private final static int MAP_ADJUSTFRAMES	= 20007;
	private final static int MAP_UNLOCK_SPEED = 20008;
	
	private boolean mMapCapture = false;
	
	// Map of Custom Maps
	public SparseIntArray customMap = new SparseIntArray(DosBoxPreferences.NUM_USB_MAPPINGS);

	private final int getMappedKeyCode(final int button, final KeyEvent event) {
		switch (button) {
		case MAP_LEFTCLICK:
			if ((mInputMode == INPUT_MODE_JOYSTICK)||(mInputMode == INPUT_MODE_REAL_JOYSTICK)) {
				DosBoxControl.nativeJoystick(0, 0, (event.getAction() == KeyEvent.ACTION_DOWN)?0:1, BTN_A);				
			}
			else if ((mInputMode == INPUT_MODE_MOUSE)||(mInputMode == INPUT_MODE_REAL_MOUSE)) {
				DosBoxControl.nativeMouse(0, 0, 0, 0, (event.getAction() == KeyEvent.ACTION_DOWN)?0:1, BTN_A);			
			}			
			return MAP_EVENT_CONSUMED;
		case MAP_RIGHTCLICK: 
			if ((mInputMode == INPUT_MODE_JOYSTICK)||(mInputMode == INPUT_MODE_REAL_JOYSTICK)) {
				DosBoxControl.nativeJoystick(0, 0, (event.getAction() == KeyEvent.ACTION_DOWN)?0:1, BTN_B);				
			}
			else if ((mInputMode == INPUT_MODE_MOUSE)||(mInputMode == INPUT_MODE_REAL_MOUSE)) {
				DosBoxControl.nativeMouse(0, 0, 0, 0, (event.getAction() == KeyEvent.ACTION_DOWN)?0:1, BTN_B);			
			}
			return MAP_EVENT_CONSUMED;
		case MAP_CYCLEUP: 
			if (event.getAction() == KeyEvent.ACTION_UP) {
				if (mParent.mTurboOn) { 
					mParent.mTurboOn = false;
					DosBoxLauncher.nativeSetOption(DosBoxMenuUtility.DOSBOX_OPTION_ID_TURBO_ON, mParent.mTurboOn?1:0, null,true);			
				} 
				DosBoxLauncher.nativeSetOption(DosBoxMenuUtility.DOSBOX_OPTION_ID_CYCLE_ADJUST, 1, null,true);
				if (DosBoxControl.nativeGetAutoAdjust()) {
					Toast.makeText(mParent, "Auto Cycles ["+DosBoxControl.nativeGetCycleCount() +"%]", Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(mParent, "DosBox Cycles: "+DosBoxControl.nativeGetCycleCount(), Toast.LENGTH_SHORT).show();
				}
			}
			return MAP_EVENT_CONSUMED;
		case MAP_CYCLEDOWN:
			if (event.getAction() == KeyEvent.ACTION_UP) {
				if (mParent.mTurboOn) { 
					mParent.mTurboOn = false;
					DosBoxLauncher.nativeSetOption(DosBoxMenuUtility.DOSBOX_OPTION_ID_TURBO_ON, mParent.mTurboOn?1:0, null,true);			
				} 
				DosBoxLauncher.nativeSetOption(DosBoxMenuUtility.DOSBOX_OPTION_ID_CYCLE_ADJUST, 0, null,true);
				if (DosBoxControl.nativeGetAutoAdjust()) {
					Toast.makeText(mParent, "Auto Cycles ["+DosBoxControl.nativeGetCycleCount() +"%]", Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(mParent, "DosBox Cycles: "+DosBoxControl.nativeGetCycleCount(), Toast.LENGTH_SHORT).show();
				}
			}
			return MAP_EVENT_CONSUMED;
		case MAP_SHOWKEYBOARD:
			if (event.getAction() == KeyEvent.ACTION_UP) {
				DosBoxMenuUtility.doShowKeyboard(mParent);
			}
			return MAP_EVENT_CONSUMED;
		case MAP_SPECIALKEYS:
			if (event.getAction() == KeyEvent.ACTION_UP) {
				mContextMenu = DosBoxMenuUtility.CONTEXT_MENU_SPECIAL_KEYS;
				mParent.openContextMenu(this);
			}
			return MAP_EVENT_CONSUMED;
		case MAP_ADJUSTCYCLES:
			if (event.getAction() == KeyEvent.ACTION_UP) {
				mContextMenu = DosBoxMenuUtility.CONTEXT_MENU_CYCLES;
				mParent.openContextMenu(this);
			}
			return MAP_EVENT_CONSUMED;
		case MAP_ADJUSTFRAMES:
			if (event.getAction() == KeyEvent.ACTION_UP) {
				mContextMenu = DosBoxMenuUtility.CONTEXT_MENU_FRAMESKIP;
				mParent.openContextMenu(this);
			}
			return MAP_EVENT_CONSUMED;
		case MAP_UNLOCK_SPEED:
			if (mParent.mTurboOn) {
				if (event.getAction() == KeyEvent.ACTION_UP) {
					DosBoxLauncher.nativeSetOption(DosBoxMenuUtility.DOSBOX_OPTION_ID_TURBO_ON, 0, null,true);	// turn off
					mParent.mTurboOn = false;
				}
			} else {
				if (event.getAction() == KeyEvent.ACTION_DOWN) {
					DosBoxLauncher.nativeSetOption(DosBoxMenuUtility.DOSBOX_OPTION_ID_TURBO_ON, 1, null,true);	// turn on
					mParent.mTurboOn = true;
				}
			}
			return MAP_EVENT_CONSUMED;
		default:
			return button;
		}
	}
	
	/*
	private boolean handleVirtualJoystick(int keyCode, boolean down) {
		if (mDebug) Log.d("JSTICK", "Handle keyCode " + keyCode);
		for(int i=0; i<DosBoxLauncher.virtualJoystick.length; i++) {
			VirtualJoystick vj = DosBoxLauncher.virtualJoystick[i];
			if (vj!=null && vj.keyCode == keyCode) {
				VirtualKey vk = DosBoxLauncher.keyValues[i];
				if (mDebug) Log.d("JSTICK", "keyCode " + keyCode + ", VK:" + vk);
				if (vk!=null) vk.sendToDosBox(down);
				return true;
			}
		}
		return false;
	}
	*/
	
	@Override
	public boolean onKeyDown(int keyCode, final KeyEvent event) {
		mMapCapture = false;
		if (true)
			Log.d("DosBoxTurbo", "onKeyDown keyCode="+keyCode + " mEnableDpad=" + mEnableDpad);
		
		if ((mInputMode == INPUT_MODE_REAL_JOYSTICK || mInputMode == INPUT_MODE_REAL_MOUSE) 
			&& Mapper.instance.handleKeyEvent(event, keyCode, true)) return true;

		if (mEnableDpad) {
			switch (keyCode) {
			// 	DPAD / TRACKBALL convert input to mouse/joy
			case KeyEvent.KEYCODE_DPAD_UP:
				if ((mInputMode == INPUT_MODE_MOUSE) || (mInputMode == INPUT_MODE_REAL_MOUSE)) {
					y[0] -= mDpadRate;
					DosBoxControl.nativeMouse((int)x[0], (int)y[0], (int)x[0], (int)y[0]+mDpadRate, 2, -1);
					return true;
				} else if ((mInputMode == INPUT_MODE_JOYSTICK) || (mInputMode == INPUT_MODE_REAL_JOYSTICK)) {
					DosBoxControl.nativeJoystick(0, -1024, 2, -1);
					return true;
				}
				break;
			case KeyEvent.KEYCODE_DPAD_DOWN:
				if ((mInputMode == INPUT_MODE_MOUSE) || (mInputMode == INPUT_MODE_REAL_MOUSE)) {
					y[0] += mDpadRate;
					DosBoxControl.nativeMouse((int)x[0], (int)y[0], (int)x[0], (int)y[0]-mDpadRate, 2, -1);
					return true;
				} else if ((mInputMode == INPUT_MODE_JOYSTICK) || (mInputMode == INPUT_MODE_REAL_JOYSTICK)) {
					DosBoxControl.nativeJoystick(0, 1024, 2, -1);
					return true;
				}
				break;
			case KeyEvent.KEYCODE_DPAD_LEFT:
				if ((mInputMode == INPUT_MODE_MOUSE) || (mInputMode == INPUT_MODE_REAL_MOUSE)) {
					x[0] -= mDpadRate;
					DosBoxControl.nativeMouse((int)x[0], (int)y[0], (int)x[0]+mDpadRate, (int)y[0], 2, -1);
					return true;
				} else if ((mInputMode == INPUT_MODE_JOYSTICK) || (mInputMode == INPUT_MODE_REAL_JOYSTICK)) {
					DosBoxControl.nativeJoystick(-1024, 0, 2, -1);
					return true;
				}
				break;
			case KeyEvent.KEYCODE_DPAD_RIGHT:
				if ((mInputMode == INPUT_MODE_MOUSE) || (mInputMode == INPUT_MODE_REAL_MOUSE)) {
					x[0] += mDpadRate;
					DosBoxControl.nativeMouse((int)x[0], (int)y[0], (int)x[0]-mDpadRate, (int)y[0], 2, -1);
					return true;
				} else if ((mInputMode == INPUT_MODE_JOYSTICK) || (mInputMode == INPUT_MODE_REAL_JOYSTICK)) {
					DosBoxControl.nativeJoystick(1024, 0, 2, -1);
					return true;
				}
				break;
			case KeyEvent.KEYCODE_DPAD_CENTER:	// button
				if ((mInputMode == INPUT_MODE_MOUSE) || (mInputMode == INPUT_MODE_REAL_MOUSE)) {
					DosBoxControl.nativeMouse(0, 0, 0, 0, 0, BTN_A);
					return true;
				} else if ((mInputMode == INPUT_MODE_JOYSTICK) || (mInputMode == INPUT_MODE_REAL_JOYSTICK)) {
					DosBoxControl.nativeJoystick(0, 0, 0, BTN_A);
					return true;
				}
				break;
			}
		}
		return handleKey(keyCode, event);			
	}
	
	@Override
	public boolean onKeyUp(int keyCode, final KeyEvent event) {
		if (mDebug)
			Log.d("DosBoxTurbo", "onKeyUp keyCode="+keyCode);

		if ((mInputMode == INPUT_MODE_REAL_JOYSTICK || mInputMode == INPUT_MODE_REAL_MOUSE) 
				&& Mapper.instance.handleKeyEvent(event, keyCode, false)) return true;
		
		if (mEnableDpad) {
			switch (keyCode) {
				// 	DPAD / TRACKBALL
				case KeyEvent.KEYCODE_DPAD_UP:
				case KeyEvent.KEYCODE_DPAD_DOWN:
				case KeyEvent.KEYCODE_DPAD_LEFT:
				case KeyEvent.KEYCODE_DPAD_RIGHT:
					if ((mInputMode == INPUT_MODE_JOYSTICK) || (mInputMode == INPUT_MODE_REAL_JOYSTICK)) {
						DosBoxControl.nativeJoystick(0, 0, 2, -1);
					}
				return true;
				case KeyEvent.KEYCODE_DPAD_CENTER:	// button
					if ((mInputMode == INPUT_MODE_MOUSE) || (mInputMode == INPUT_MODE_REAL_MOUSE)) {
						DosBoxControl.nativeMouse(0, 0, 0, 0, 1, BTN_A);
					} else if ((mInputMode == INPUT_MODE_JOYSTICK) || (mInputMode == INPUT_MODE_REAL_JOYSTICK)) {
						DosBoxControl.nativeJoystick(0, 0, 1, BTN_A);
					} 
				return true;
			}
		}
		return handleKey(keyCode, event);
	}
	 
	private boolean handleKey(int keyCode, final KeyEvent event) {
		if (true)
			Log.d("DosBoxTurbo", "handleKey keyCode="+keyCode);
		int tKeyCode = 0;

		if (keyCode == KeyEvent.KEYCODE_BACK) return false;

		// check for xperia play back case
		if (keyCode == KeyEvent.KEYCODE_BACK && event.isAltPressed()) {
			int backval = customMap.get(DosBoxPreferences.XPERIA_BACK_BUTTON);
			if (backval > 0) {
				// Special Sony XPeria Play case
				if (mEnableDpad) {
					// FIRE2
					if ((mInputMode == INPUT_MODE_MOUSE) || (mInputMode == INPUT_MODE_REAL_MOUSE)) {
						DosBoxControl.nativeMouse(0, 0, 0, 0, (event.getAction() == KeyEvent.ACTION_DOWN)?0:1, BTN_B);
					} else if ((mInputMode == INPUT_MODE_JOYSTICK) || (mInputMode == INPUT_MODE_REAL_JOYSTICK)) {
						DosBoxControl.nativeJoystick(0, 0, (event.getAction() == KeyEvent.ACTION_DOWN)?0:1, BTN_B);
					} 
				} else {
					// sony xperia play O (circle) button
					DosBoxControl.sendNativeKey(backval, (event.getAction() == KeyEvent.ACTION_DOWN), mModifierCtrl, mModifierAlt, mModifierShift);
					return true;	// consume event
				}
			}
			return true;	// consume event
		}
		
		// Handle all other keyevents
		int value = customMap.get(keyCode);
		
		if (value > 0) {
			// found a valid mapping
			tKeyCode = getMappedKeyCode(value,event);
			if (tKeyCode > MAP_NONE) {
				DosBoxControl.sendNativeKey(tKeyCode, (event.getAction() == KeyEvent.ACTION_DOWN), mModifierCtrl, mModifierAlt, mModifierShift);
				return true; // consume KeyEvent
			} else if (tKeyCode == MAP_EVENT_CONSUMED) {
				return true;
			}
		}
		

		switch (keyCode) {
		case KeyEvent.KEYCODE_UNKNOWN:
			break;
						
		default:
			boolean	down = (event.getAction() == KeyEvent.ACTION_DOWN);
			if (mDebug)
				Log.d("DosBoxTurbo", "handleKey (default) keyCode="+keyCode + " down="+down);
		
			if (!down || (event.getRepeatCount() == 0)) {
				int unicode = event.getUnicodeChar();
			
				// filter system generated keys, but not hardware keypresses
				if ((event.isAltPressed() || event.isShiftPressed()) && (unicode == 0) && ((event.getFlags() & KeyEvent.FLAG_FROM_SYSTEM) == 0))
					break;

				//fixed alt key problem for physical keyboard with only left alt
				if ((!mUseLeftAltOn) && (keyCode == KeyEvent.KEYCODE_ALT_LEFT)) {
					break;
				}
			
				if ((!mUseLeftAltOn) && (keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT)) {
					break;
				}
			
				if ((keyCode > 255) || (unicode > 255)) {
					//unknown keys
					break;
				}
							
				keyCode = keyCode | (unicode << 8);

				long diff = event.getEventTime() - event.getDownTime();
			
				if (!down && (diff < 50)) {
					//simulate as long press
					if (mDebug) 
						Log.d("DosBoxTurbo", "LongPress consumed keyCode="+keyCode + " down="+down);
					mKeyHandler.removeMessages(keyCode);
					mKeyHandler.sendEmptyMessageDelayed(keyCode, BUTTON_REPEAT_DELAY - diff);
				}
				else if (down && mKeyHandler.hasMessages(keyCode)) {
					if (mDebug)
						Log.d("DosBoxTurbo", "KeyUp consumed keyCode="+keyCode + " down="+down);
					//there is an key up in queue, should be repeated event
				}
				else if (DosBoxControl.sendNativeKey(keyCode, down, mModifierCtrl, mModifierAlt, mModifierShift)) {
					if (mDebug)
						Log.d("DosBoxTurbo", "sendNativeKey(true) keyCode="+keyCode + " down="+down + " mCtrl: "+ mModifierCtrl + " mAlt: " +mModifierAlt + " mShift: " + mModifierShift);
					mModifierCtrl = false; 
					mModifierAlt = false;  
					mModifierShift = false;
				}
			}
		}

		if (mMapCapture) {
			return true;
		}
		return false;
	}
	
	public void setDirty() {
		mDirtyCount = 0;		
	}
	
	public void resetScreen(boolean redraw) {
		setDirty();
		mScroll_x = 0;
		mScroll_y = 0;
		
		if (redraw)
			forceRedraw(); 	
	}
	
	public void forceRedraw() {
		setDirty();
		VideoRedraw(mBitmap, mSrc_width, mSrc_height, 0, mSrc_height);		
	}
	
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		resetScreen(true);
	}
	
	public void surfaceCreated(SurfaceHolder holder) {
		mSurfaceViewRunning = true;
	}
	
	public void surfaceDestroyed(SurfaceHolder holder) {
		mSurfaceViewRunning = false;
	}
	
	private final void toggleMouseJoystick() {
		switch (mInputMode) { 
		case DosBoxSurfaceView.INPUT_MODE_JOYSTICK:
			mInputMode = DosBoxSurfaceView.INPUT_MODE_MOUSE;
			DosBoxLauncher.nativeSetOption(DosBoxMenuUtility.DOSBOX_OPTION_ID_JOYSTICK_ENABLE, 0 ,null, true);
			break;
		case DosBoxSurfaceView.INPUT_MODE_MOUSE:
			mInputMode = DosBoxSurfaceView.INPUT_MODE_JOYSTICK;
			DosBoxLauncher.nativeSetOption(DosBoxMenuUtility.DOSBOX_OPTION_ID_JOYSTICK_ENABLE, 1 ,null, true);
			break;
		}
		forceRedraw();
	}
	
	private final void mouseClick(int button) {
 		DosBoxControl.nativeMouse(0, 0, -1, -1, ACTION_DOWN, button);
		try {
			Thread.sleep(CLICK_DELAY);
		} catch (InterruptedException e) {
		}
		DosBoxControl.nativeMouse(0, 0, -1, -1, ACTION_UP, button);
	}
	
	// Fix for Motorola Keyboards!!! - fishstix
	@Override
	public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
		return new BaseInputConnection(this, false) {
			@Override
			public boolean sendKeyEvent(KeyEvent event) {
				return super.sendKeyEvent(event);
			}
		};
	}
	
	// GESTURE MAP
	private final static int GESTURE_FLING_VELOCITY = 2000;
	private final static int GESTURE_NONE = 0;
	private final static int GESTURE_SHOW_KEYBOARD = 1;
	private final static int GESTURE_HIDE_KEYBOARD = 2;
	private final static int GESTURE_SHOW_MENU = 3;
	private final static int GESTURE_HIDE_MENU = 4;
	private final static int GESTURE_SHOW_SPECIALKEYS = 5;
	private final static int GESTURE_SHOW_CYCLES = 6;
	private final static int GESTURE_SHOW_FRAMESKIP = 7;
	
	public final static int GESTURE_LEFT_CLICK = 3;
	public final static int GESTURE_RIGHT_CLICK = 4;
	public final static int GESTURE_DOUBLE_CLICK = 5;
	public int mGestureUp = GESTURE_NONE;
	public int mGestureDown = GESTURE_NONE;
	public int mGestureSingleClick = GESTURE_SHOW_KEYBOARD;
	public int mGestureDoubleClick = GESTURE_NONE;
	public int mGestureTwoFinger = GESTURE_NONE;
	public boolean mTwoFingerAction = false;
	private DosBoxSurfaceView mSurfaceView = this;
	
	public int lastCalibrationOriginX = -1;
	public int lastCalibrationOriginY = -1;
	
	private void ensureMouseCalibration() {
		if (mScreenRect.left == lastCalibrationOriginX && mScreenRect.top == lastCalibrationOriginY) return;
		Log.i("DosBoxTurbo","nativeMouseWarp init");
		lastCalibrationOriginX = mScreenRect.left;
		lastCalibrationOriginY = mScreenRect.top;
		
		Thread t = new Thread() {
			@Override
			public void run() {
				try {Thread.sleep(400);} catch (InterruptedException e) {}
				int x = mScreenRect.left;
				int y = mScreenRect.top;
				DosBoxControl.nativeMouseWarp(x, y, x, y, (int)(mScreenRect.width() * warpX), (int)(mScreenRect.height() * warpY));

				try {Thread.sleep(400);} catch (InterruptedException e) {}
				x += mScreenRect.width();
				y += mScreenRect.height();
				DosBoxControl.nativeMouseWarp(x, y, x, y, (int)(mScreenRect.width() * warpX), (int)(mScreenRect.height() * warpY));
				
				try {Thread.sleep(400);} catch (InterruptedException e) {}
				x = mScreenRect.left + mScreenRect.width()/2;
				y = mScreenRect.top + mScreenRect.height()/2;
				DosBoxControl.nativeMouseWarp(x, y, x, y,(int)(mScreenRect.width() * warpX), (int)(mScreenRect.height() * warpY));
			}
		};
		
		t.start();
	}
	
    class MyGestureDetector extends SimpleOnGestureListener {
    	@Override
    	public boolean onDown(MotionEvent event) {
			Log.i("DosBoxTurbo","onDown()");
			if (mInputMode == INPUT_MODE_MOUSE) {
				if (mAbsolute) {
					ensureMouseCalibration();
   	       			final int pointerId = mWrap.getPointerId(event, ((event.getAction() & MotionEvent.ACTION_POINTER_ID_MASK) >> MotionEvent.ACTION_POINTER_ID_SHIFT));
   	       			DosBoxControl.nativeMouseWarp(x[pointerId], y[pointerId], mScreenRect.left, mScreenRect.top, (int)(mScreenRect.width() * warpX), (int)(mScreenRect.height() * warpY));
   	       			Log.i("DosBoxTurbo","nativeMouseWarp " + x[pointerId] + ", " +  y[pointerId]);
   	       			try {
   	       				Thread.sleep(85);
   	       			} catch (InterruptedException e) {
   	       			}
				}
			}
      		return true; 
    	}
    	
        @Override
    	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
    			float velocityY) {
    		// open keyboard
    		if (velocityY < -GESTURE_FLING_VELOCITY) {
    			// swipe up
    			switch (mGestureUp) {
    			case GESTURE_SHOW_KEYBOARD:
    				DosBoxMenuUtility.doShowKeyboard(mParent);
    				return true;
    			case GESTURE_HIDE_KEYBOARD:
    				DosBoxMenuUtility.doHideKeyboard(mParent);
    				return true;
    			case GESTURE_SHOW_MENU:
    				DosBoxMenuUtility.doShowMenu(mParent);
    				return true;
    			case GESTURE_HIDE_MENU:
    				DosBoxMenuUtility.doHideMenu(mParent);
    				return true;
    			case GESTURE_SHOW_SPECIALKEYS:
    				mContextMenu = DosBoxMenuUtility.CONTEXT_MENU_SPECIAL_KEYS;
					mParent.openContextMenu(mSurfaceView);
    				return true;
    			case GESTURE_SHOW_FRAMESKIP:
    				mContextMenu = DosBoxMenuUtility.CONTEXT_MENU_FRAMESKIP;
					mParent.openContextMenu(mSurfaceView);
    				return true;
    			case GESTURE_SHOW_CYCLES:
    				mContextMenu = DosBoxMenuUtility.CONTEXT_MENU_CYCLES;
					mParent.openContextMenu(mSurfaceView);
    				return true;
    			}
    		} else if (velocityY > GESTURE_FLING_VELOCITY) {
    			// swipe down
    			switch (mGestureDown) {
    			case GESTURE_SHOW_KEYBOARD:
    				DosBoxMenuUtility.doShowKeyboard(mParent);
    				return true;
    			case GESTURE_HIDE_KEYBOARD:
    				DosBoxMenuUtility.doHideKeyboard(mParent);
    				return true;
    			case GESTURE_SHOW_MENU:
    				DosBoxMenuUtility.doShowMenu(mParent);
    				return true;
    			case GESTURE_HIDE_MENU:
    				DosBoxMenuUtility.doHideMenu(mParent);
    				return true;
    			case GESTURE_SHOW_SPECIALKEYS:
    				mContextMenu = DosBoxMenuUtility.CONTEXT_MENU_SPECIAL_KEYS;
    				mParent.openContextMenu(mSurfaceView);
    				return true;
    			case GESTURE_SHOW_FRAMESKIP:
    				mContextMenu = DosBoxMenuUtility.CONTEXT_MENU_FRAMESKIP;
					mParent.openContextMenu(mSurfaceView);
    				return true;
    			case GESTURE_SHOW_CYCLES:
    				mContextMenu = DosBoxMenuUtility.CONTEXT_MENU_CYCLES;
					mParent.openContextMenu(mSurfaceView);
    				return true;    				
    			}
    		}
    		return false;
    	}
        
        @Override
    	public boolean onDoubleTap(MotionEvent event) {
			Log.i("DosBoxTurbo","onDoubleTap()");
			if (mInputMode == INPUT_MODE_MOUSE) {
        		switch (mGestureDoubleClick) {
        		case GESTURE_LEFT_CLICK:
        		case GESTURE_RIGHT_CLICK:
        			if (mLongPress) {
        				mDoubleLong = true;
        				DosBoxControl.nativeMouse(0, 0, -1, -1, ACTION_DOWN, mGestureDoubleClick-GESTURE_LEFT_CLICK);
        			} else 
        				mouseClick(mGestureDoubleClick-GESTURE_LEFT_CLICK);
        			return true;
        		case GESTURE_DOUBLE_CLICK:
        			mouseClick(BTN_A);
        			try{
        				Thread.sleep(CLICK_DELAY);
        			} catch (InterruptedException e) {
        			}
        			mouseClick(BTN_A);
        		}
        	}
    		return false;
    	}
        
        @Override
    	public boolean onSingleTapConfirmed(MotionEvent event) {
			Log.i("DosBoxTurbo","onSingleTapConfirmed()");
			
			if (mGestureSingleClick == GESTURE_SHOW_KEYBOARD) {
				Log.i("DosBoxTurbo","onShowKeyboard");
				showKeyboard();
				return true;
			}
			
        	if (mInputMode == INPUT_MODE_MOUSE) {
        		//pointerIndex = ((event.getAction() & MotionEvent.ACTION_POINTER_ID_MASK) >> MotionEvent.ACTION_POINTER_ID_SHIFT);
       			//final int pointerId = mWrap.getPointerId(event, ((event.getAction() & MotionEvent.ACTION_POINTER_ID_MASK) >> MotionEvent.ACTION_POINTER_ID_SHIFT));
        		if ((mGestureSingleClick != GESTURE_NONE)&&(mGestureDoubleClick != GESTURE_NONE)) {
        			mouseClick(mGestureSingleClick-GESTURE_LEFT_CLICK);
        			return true;
        		}
        	}
       		return false;
    	} 
        
        @Override
        public boolean onSingleTapUp(MotionEvent event) {
        	Log.i("DosBoxTurbo","onSingleTapUp()");
        	if (mInputMode == INPUT_MODE_MOUSE) {
        		if ((mGestureDoubleClick == GESTURE_NONE)&&(mGestureSingleClick != GESTURE_NONE)) {	// fishstix,fire only when doubleclick gesture is disabled
        			mouseClick(mGestureSingleClick-GESTURE_LEFT_CLICK);
        			return true;
        		} else {
        			showKeyboard();
        		}
        	}

      		return false;  
        } 
        
        @Override
        public void onLongPress(MotionEvent event) {
			Log.i("DosBoxTurbo","onLongPress()");
			if (mInputMode == INPUT_MODE_MOUSE)  {
				if (!mFilterLongClick && mLongPress && !mDoubleLong && !mTwoFingerAction) {
					mLongClick = true;
					if (mGestureSingleClick != GESTURE_NONE) {
						DosBoxControl.nativeMouse(0, 0, 0, 0, ACTION_DOWN, mGestureSingleClick-GESTURE_LEFT_CLICK);
					}
				}
				mFilterLongClick = false;
			}
        }
    }
    
    public void showKeyboard() {
    	// DosBoxMenuUtility.doShowKeyboard(mParent);
    	mParent.showKeyboard();
    }
    
    public void showMenu() {
    	DosBoxMenuUtility.doShowMenu(mParent);
    }
}

