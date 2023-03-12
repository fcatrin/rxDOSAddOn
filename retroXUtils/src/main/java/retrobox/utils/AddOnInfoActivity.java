package retrobox.utils;

import xtvapps.core.AndroidCoreUtils;
import xtvapps.core.AndroidFonts;
import xtvapps.core.SimpleCallback;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public abstract class AddOnInfoActivity extends Activity {

	private static final String RETROBOX_APKID = "xtvapps.retrobox.v2";
	private static final String RETROBOX_ACTION = "xtvapps.retroboxtv";
	private static final String FONT_LOGO = "edunline.ttf";
	private static final String FONT_NORMAL = "ubuntu/ubuntu-m.ttf";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_view_add_on);
		
		String info = getInfo();
		String msg = getString(R.string.addon_installed).replace("{addon}", info);
		TextView txtInfo = (TextView)findViewById(R.id.txtAddOnInfo);
		txtInfo.setText(msg);
		
		Button btnOpenRetroBox = (Button)findViewById(R.id.btnOpenRetroBox);
		btnOpenRetroBox.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				intent.setAction(RETROBOX_ACTION);
				if (intent.resolveActivity(getPackageManager())!=null) {
					startActivity(intent);
					finish();
				} else {
					String msg = getString(R.string.addon_rbx_missing_msg);
					String optYes = getString(R.string.addon_rbx_missing_yes);
					String optNo  = getString(R.string.addon_rbx_missing_no);
					RetroBoxDialog.showAlertAsk(AddOnInfoActivity.this, msg, optYes, optNo, new SimpleCallback() {
						
						@Override
						public void onResult() {
							AndroidCoreUtils.openGooglePlay(AddOnInfoActivity.this, RETROBOX_APKID);
						}
					});
				}
			}
		});
		
		View viewLogo = findViewById(R.id.txtRetroBoxLogo);
		AndroidFonts.setViewFont(viewLogo, FONT_LOGO);
		AndroidFonts.setViewFont(txtInfo, FONT_NORMAL);
		AndroidFonts.setViewFont(btnOpenRetroBox, FONT_NORMAL);
	}
	
	protected abstract String getInfo();

}
