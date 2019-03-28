package xtvapps.retrobox.dosbox.wrapper;

import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import xtvapps.core.Utils;
import xtvapps.retrobox.dosbox.DosBoxLauncher;
import xtvapps.retrobox.v2.dosbox.R;

public class GameLauncher extends Activity {
	private static final String DOS_ENCODING = "ISO-8859-1";

	private static final String DOSBOX_CONFIG = "dosboxconf";
	private static final String DOSBOX_CMD = "cmdline";
	private static final String DOSBOX_MOUSEONLY = "mouseOnly";
	private static final String DOSBOX_REALMOUSE = "useRealMouse";
	private static final String DOSBOX_TESTING = "testingMode";
	private static final String DOSBOX_SHOW_EXTRA_BUTTONS = "showExtraButtons";
	private static final String DOSBOX_WARP_X = "warpX";
	private static final String DOSBOX_WARP_Y = "warpY";
	private static final String DOSBOX_GAMEPAD = "gaemepad";
	private static final String DOSBOX_APK_ID = "xtvapps.retrobox.v2.dosbox";
	private static final String DOSBOX_SHOW_FPS = "showFPS";

	
	private static final long SPLASH_TIME = 3000;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.boot);
	}

	@Override
	protected void onStart() {
		super.onStart();
		
		final long t0 = System.currentTimeMillis();
		
		BackgroundTask<File> task = new BackgroundTask<File>() {

			@Override
			public File onBackground() throws Exception {
				return gamePrepare();
			}

			@Override
			public void onSuccess(File config) {
				runEmulator(t0, config);
			}
		};
		task.execute();
	}


	private File gamePrepare() throws IOException {
		File rootDir = getFilesDir();
		
		AndroidUtils.unpackAssets(GameLauncher.this, "game", rootDir);
		
		File cDrive = new File(rootDir, "game"); 
		String gameDir = getString(R.string.gamedir);

		// read dosbox.conf template
		File dosboxTemplateFile = new File(cDrive, "dosbox.template.conf");
		String dosboxconf = Utils.loadString(dosboxTemplateFile, DOS_ENCODING);
		
		// apply settings for this game
		dosboxconf = dosboxconf.replace("{cdrive}", cDrive.getAbsolutePath());
		dosboxconf = dosboxconf.replace("{gamedir}", gameDir);
		
		// write final dosbox file
		File dosboxFile = new File(cDrive, "dosbox.conf");
		Utils.saveBytes(dosboxFile, dosboxconf.getBytes(DOS_ENCODING));
		
		return dosboxFile;
	}
	
	private void runEmulator(long t0, final File dosboxConfigFile) {
		Runnable task = new Runnable() {
			@Override
			public void run() {
				runEmulator(dosboxConfigFile);
			}
		};
		
		long delta = SPLASH_TIME - (System.currentTimeMillis() - t0);
		if (delta < 0) delta = 0;
		
		new Handler().postDelayed(task, delta);
		
	}
	
	private void runEmulator(File dosboxConfigFile) {
		Intent dosboxIntent = new Intent(this, DosBoxLauncher.class);
		dosboxIntent.putExtra(DOSBOX_CONFIG, dosboxConfigFile.getAbsolutePath());
		dosboxIntent.putExtra(DOSBOX_MOUSEONLY, false);
		dosboxIntent.putExtra(DOSBOX_WARP_X, 0);
		dosboxIntent.putExtra(DOSBOX_WARP_Y, 0);
		dosboxIntent.putExtra(DOSBOX_CMD, "-exit");

		// dosboxIntent.putExtra(EXTRA_KEEP_ASPECT, hasKeepAspectRatio(game));
		// dosboxIntent.putExtra(EXTRA_INVERT_RGB, RetroXCore.isRaspberryPiTillHertz());
		
		startActivity(dosboxIntent);
		finish();
	}
}
