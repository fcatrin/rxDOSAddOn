package xtvapps.retrobox.dosbox.wrapper;

import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.MotionEvent;
import xtvapps.core.SimpleCallback;
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
	
	private enum Stage {Boot, Credits, Splash}
	private Stage stage = Stage.Boot;
	
	private static final long SPLASH_TIME_CREDITS = 12000;
	private static final long SPLASH_TIME_GAME = 5000;

	private File cDrive;

	private ScrollTextView txtCredits;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.credits);
		
		txtCredits = (ScrollTextView)findViewById(R.id.txtCredits);
		
		File rootDir = getFilesDir();
		cDrive = new File(rootDir, "game"); 
	}

	@Override
	protected void onStart() {
		super.onStart();
		
		new Handler().postDelayed(new Runnable(){
			@Override
			public void run() {
				startCreditsSplash();
			}
		}, 1000);
	}
	
	private void startCreditsSplash() {
		stage = Stage.Credits;
		
		txtCredits.setOnFinishedCallback(new SimpleCallback() {
			
			@Override
			public void onResult() {
				startGameSplash();
			}
		});
		txtCredits.setDuration((int)SPLASH_TIME_CREDITS);
		txtCredits.startScroll();
	}
	
	private void startGameSplash() {
		stage = Stage.Splash;

		setContentView(R.layout.splash);
		
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
	

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		skipCredits();
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		skipCredits();
		return super.onTouchEvent(event);
	}
	
	private void skipCredits() {
		if (stage != Stage.Credits) return;

		txtCredits.stopScroll();
		startGameSplash();
	}

	private File gamePrepare() throws IOException {
		AndroidUtils.unpackAssets(GameLauncher.this, "game", cDrive.getParentFile());
		
		String gameDir = getString(R.string.gamedir);
		
		File gameFolder = new File(cDrive, gameDir);
		File files[] = gameFolder.listFiles();
		for(File file : files) {
			if (file.getName().endsWith(".CAR")) file.delete();
		}

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
		
		long delta = SPLASH_TIME_GAME - (System.currentTimeMillis() - t0);
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

		
		dosboxIntent.putExtra("keepAspect", false);
		// dosboxIntent.putExtra(EXTRA_INVERT_RGB, RetroXCore.isRaspberryPiTillHertz());
		
		startActivity(dosboxIntent);
		finish();
	}

	private void addKeymapDefault(Intent intent, String src, String dst) {
		intent.putExtra("kmap1" + src, dst);
	}
}
