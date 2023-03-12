package retrobox.vinput;

public abstract class SwipeListener {
	public enum Swipe {Up, Down, Left, Right};
	
	public abstract void onSwipe(Swipe swipe);
}
