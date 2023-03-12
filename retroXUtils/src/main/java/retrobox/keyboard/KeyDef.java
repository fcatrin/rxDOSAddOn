package retrobox.keyboard;

import android.view.View;

public class KeyDef {

	private String label;
	private String value;
	private int size = 1;
	private View view;

	public KeyDef(String label, String value) {
		this.label = label;
		this.value = value;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public View getView() {
		return view;
	}

	public void setView(View view) {
		this.view = view;
	}

	
}
