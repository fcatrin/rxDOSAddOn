package retrobox.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;
import retrobox.utils.GamepadLayoutManager.ButtonId;
import retrobox.utils.GamepadLayoutManager.ButtonLabelBox;
import xtvapps.core.Utils;

public class GamepadInfoDialog {
	
	private Activity activity;
	private GamepadLayoutManager gamepadLayoutManager;
	private int gamepadLabelResourceIds[] = {
			R.id.gamepadLabelL2, 
			R.id.gamepadLabelL1,
			R.id.gamepadLabelR2,
			R.id.gamepadLabelR1,
			R.id.gamepadLabelL3,
			R.id.gamepadLabelR3,
			R.id.gamepadLabelA,
			R.id.gamepadLabelB,
			R.id.gamepadLabelX,
			R.id.gamepadLabelY,
			R.id.gamepadLabelSELECT,
			R.id.gamepadLabelSTART 
	};
	
	public GamepadInfoDialog(Activity activity) {
		this.activity = activity;
		gamepadLayoutManager = new GamepadLayoutManager(activity, R.id.gamepadDialogImage, gamepadLabelResourceIds);
	}
	
	private String[] getLabelsFromIntent(Intent intent) {
		String labels[] = new String[ButtonId.values().length];
		for(int i=0; i<labels.length; i++) {
			String label = intent.getStringExtra("lbl" + ButtonId.values()[i]);
			labels[i] = label;
		}
		return labels;
	}
	
	public  void addLabelsToIntent(Intent intent, String[] buttonsLayout) {
		if (buttonsLayout == null) return;
		for(int i=0; i<buttonsLayout.length; i++) {
			String label = buttonsLayout[i]; 
			if (label!=null) intent.putExtra("lbl" + ButtonId.values()[i], label);
		}
	}
	
	public void addInfoToIntent(Intent intent, String infoTop, String infoBottom) {
		intent.putExtra("infoTop", infoTop);
		intent.putExtra("infoBottom", infoBottom);
	}
	
	public void loadFromIntent(Intent intent) {
		setLabels(getLabelsFromIntent(intent));
		setInfo(intent.getStringExtra("infoTop"), intent.getStringExtra("infoBottom"));
	}
	
	@SuppressLint("DefaultLocale")
	public void setLabels(String[] labels) {
		ButtonLabelBox[] labelBoxes = gamepadLayoutManager.getLabelBoxes();
		
		if (labels==null) labels = new String[labelBoxes.length];
		
		for(int i=0; i<labelBoxes.length; i++) {
			String label = labels[i];
			if (label == null) label = "";
			labelBoxes[i].setLabel(activity, label.toUpperCase());
		}
	}
	
	public boolean hasLabels(Activity activity) {
		ButtonLabelBox[] labelBoxes = gamepadLayoutManager.getLabelBoxes();
		for (ButtonLabelBox labelBox : labelBoxes) {
			if (!Utils.isEmptyString(labelBox.getLabel(activity))) return true;
		}
		return false;
	}
	
	public void updateGamepadVisible(Activity activity, boolean hasGamepad) {
		boolean hasLabels = hasLabels(activity) && hasGamepad;
		activity.findViewById(R.id.gamepadDialogFrame).setVisibility(hasLabels?View.VISIBLE:View.GONE);
	}
	
	public void setInfo(String textTop, String textBottom) {
		TextView vTop = (TextView)activity.findViewById(R.id.txtGamepadInfoTop);
		TextView vBottom = (TextView)activity.findViewById(R.id.txtGamepadInfoBottom);
		vTop.setText(textTop);
		vBottom.setText(textBottom);
	}

}
