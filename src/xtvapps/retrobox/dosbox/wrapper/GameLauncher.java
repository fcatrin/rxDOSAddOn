package xtvapps.retrobox.dosbox.wrapper;

import android.app.Activity;
import android.os.Bundle;

public class GameLauncher extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onStart() {
		super.onStart();
		
		SimpleBackgroundTask task = new SimpleBackgroundTask() {
			
			@Override
			public void onBackgroundTask() throws Exception {
				AndroidUtils.unpackAssets(GameLauncher.this, "game", getFilesDir());
			}
		};
		task.execute();
	}




}
