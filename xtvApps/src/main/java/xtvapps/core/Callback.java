package xtvapps.core;

public abstract class Callback<T> {
	public abstract void onResult(T result);
	public void onError(){}
	public void onFinally(){}
}
