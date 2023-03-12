package retrobox.utils;

public interface ThreadedBackgroundTask {
	public void onBackground();
	public void onUIThread();
}
