package xtvapps.core;

public abstract class SimpleCallback extends Callback<Void> {

	public abstract void onResult();
	
	@Override
	public void onResult(Void result) {
		onResult();
	}
	
}
