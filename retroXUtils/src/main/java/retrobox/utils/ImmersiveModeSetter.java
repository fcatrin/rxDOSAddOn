package retrobox.utils;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;

public abstract class ImmersiveModeSetter {
	private static final String LOGTAG = ImmersiveModeSetter.class.getSimpleName();
	
	public static ImmersiveModeSetter get() {
		if (android.os.Build.VERSION.SDK_INT>=android.os.Build.VERSION_CODES.KITKAT) {
			return ImmersiveModeSetterKitKat.instance;
		} else if (android.os.Build.VERSION.SDK_INT>=android.os.Build.VERSION_CODES.JELLY_BEAN) {
			return ImmersiveModeSetterJellyBean.instance;
		}
		return ImmersiveModeSetterLegacy.instance;
	}
	
	public abstract void setImmersiveMode(Window window, boolean stable);
	
	public static void postImmersiveMode(Handler handler, final Window window, final boolean stable) {
		handler.postDelayed(new Runnable(){
			@Override
			public void run() {
				get().setImmersiveMode(window, stable);
			}
		}, 1000);
	}
	
	private static boolean postPeriodic = false;
	
	public static void stop() {
		postPeriodic = false;
	}
	
	public static void postImmersiveModePeriodic(final Handler handler, final Window window, final boolean stable, final long period) {
		postPeriodic = true;
		final Runnable task = new Runnable(){
			@Override
			public void run() {
				get().setImmersiveMode(window, stable);
				if (postPeriodic) {
					postImmersiveModePeriodic(handler, window, stable, period);
				}
			}
		};
		
		handler.postDelayed(task, period);
	}
	
	private static class ImmersiveModeSetterLegacy extends ImmersiveModeSetter {
		private static final ImmersiveModeSetterLegacy instance = new ImmersiveModeSetterLegacy();
		@SuppressWarnings("deprecation")
		@Override
		public void setImmersiveMode(Window window, boolean stable) {
			window.getDecorView().setSystemUiVisibility(
		            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
		            | View.STATUS_BAR_HIDDEN
		            );
		}
	}
	private static class ImmersiveModeSetterJellyBean extends ImmersiveModeSetter {
		private static final ImmersiveModeSetterJellyBean instance = new ImmersiveModeSetterJellyBean();
		@Override
		@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
		public void setImmersiveMode(Window window, boolean stable) {
			window.getDecorView().setSystemUiVisibility(
		            (stable?View.SYSTEM_UI_FLAG_LAYOUT_STABLE
		            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
		            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN:0)
		            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
		            | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
		            );
		}
	}
	private static class ImmersiveModeSetterKitKat extends ImmersiveModeSetter {
		private static final ImmersiveModeSetterKitKat instance = new ImmersiveModeSetterKitKat();
		@Override
		@TargetApi(Build.VERSION_CODES.KITKAT)
		public void setImmersiveMode(Window window, boolean stable) {
			window.getDecorView().setSystemUiVisibility(
		            (stable?View.SYSTEM_UI_FLAG_LAYOUT_STABLE:0)
		            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
		            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
		            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
		            | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
		            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
		}
	}

}
