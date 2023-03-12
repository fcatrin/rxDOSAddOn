package retrobox.vinput;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import retrobox.utils.R;

public class QuitHandler {
	public static abstract class QuitHandlerCallback {
		public abstract void onQuit();
		public void onCancel() {};
	}
	
	public static void askForQuit(Context context, final QuitHandlerCallback callback) {
    	AlertDialog.Builder builder = new AlertDialog.Builder(context);
    	builder.setTitle(context.getString(R.string.quit_title));
		builder.setMessage(context.getString(R.string.quit_msg));
		
		builder.setPositiveButton(context.getString(R.string.quit_yes), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface arg0, int arg1) {
				if (callback!=null) callback.onQuit();
			}
		});
		builder.setNegativeButton(context.getString(R.string.quit_no), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface arg0, int arg1) {
				if (callback!=null) callback.onCancel();
			}
		});				
		builder.create().show();		
	}
}
