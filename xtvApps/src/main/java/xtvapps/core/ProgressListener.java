package xtvapps.core;

public abstract class ProgressListener {
	public abstract boolean update(int progress, int max);
	public void onStart() {}
	public void onEnd() {}
	public int getBufferSize(int reference) {
		return 0;
	}
}
