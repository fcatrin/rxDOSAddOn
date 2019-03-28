package xtvapps.retrobox.dosbox.wrapper;

import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import xtvapps.core.Utils;
import xtvapps.retrobox.dosbox.DosBoxLauncher;
import xtvapps.dosbox.swos.R;

public class GameLauncher extends Activity {
	private static final String DOS_ENCODING = "ISO-8859-1";

	private static final String DOSBOX_CONFIG = "dosboxconf";
	private static final String DOSBOX_CMD = "cmdline";
	private static final String DOSBOX_MOUSEONLY = "mouseOnly";
	private static final String DOSBOX_REALMOUSE = "useRealMouse";
	private static final String DOSBOX_STANDALONE = "standalone";
	private static final String DOSBOX_SHOW_EXTRA_BUTTONS = "showExtraButtons";
	private static final String DOSBOX_WARP_X = "warpX";
	private static final String DOSBOX_WARP_Y = "warpY";
	private static final String DOSBOX_GAMEPAD = "gaemepad";
	private static final String DOSBOX_APK_ID = "xtvapps.retrobox.v2.dosbox";
	private static final String DOSBOX_SHOW_FPS = "showFPS";
	private static final String GAMEPAD_OVERLAY = "OVERLAY";;
	
	private static final long SPLASH_TIME = 3000;

	private File cDrive;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.boot);
		
		File rootDir = getFilesDir();
		cDrive = new File(rootDir, "game"); 
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
		AndroidUtils.unpackAssets(GameLauncher.this, "game", cDrive.getParentFile());
		
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
		
		dosboxIntent.putExtra(DOSBOX_STANDALONE, true);
		
		File overlayDir = new File(cDrive, "overlay/" + getString(R.string.overlay));
		dosboxIntent.putExtra(GAMEPAD_OVERLAY, overlayDir.getAbsolutePath());

		// TODO make it more configurable
		
		addKeymapDefault(dosboxIntent, "UP", "KEY_UP");
		addKeymapDefault(dosboxIntent, "DOWN", "KEY_DOWN");
		addKeymapDefault(dosboxIntent, "LEFT", "KEY_LEFT");
		addKeymapDefault(dosboxIntent, "RIGHT", "KEY_RIGHT");
		addKeymapDefault(dosboxIntent, "BTN_A", "KEY_SPACE");

		
		// dosboxIntent.putExtra(EXTRA_KEEP_ASPECT, hasKeepAspectRatio(game));
		// dosboxIntent.putExtra(EXTRA_INVERT_RGB, RetroXCore.isRaspberryPiTillHertz());
		
		startActivity(dosboxIntent);
		finish();
	}

	private void addKeymapDefault(Intent intent, String src, String dst) {
		intent.putExtra("kmap1" + src, dst);
	}
}
