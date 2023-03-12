package xtvapps.core;

public abstract class NetworkProgressListener {
	public abstract boolean updateProgress(int progress, int max);
	public void onDownloadStart() {}
	public void onDownloadEnd() {}
	public int getBufferSize(int max) {return 0;}
}
