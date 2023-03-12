package retrobox.keyboard;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;

public class KeyboardLayout {
	private static final String LOGTAG = KeyboardLayout.class.getSimpleName();
	
	List<List<KeyDef>> keys = new ArrayList<List<KeyDef>>();

	public KeyboardLayout() {}
	
	public void addRow(String labels[], String codes[]) {
		List<KeyDef> row = new ArrayList<KeyDef>();
		for(int i=0; i<labels.length; i++) {
			boolean isMissingCode = i>=codes.length;
			String label = labels[i];
			String code = isMissingCode ? "MISSING" : codes[i];
			row.add(new KeyDef(label, code));
			
			if (isMissingCode) Log.e(LOGTAG, "Missing code for key " + label);
		}
		keys.add(row);
	}

	public List<List<KeyDef>> getKeys() {
		return keys;
	}

	public void setKeySize(String label, int size) {
		for(List<KeyDef> row : keys) {
			for(KeyDef keydef : row) {
				if (label.equals(keydef.getLabel())) {
					keydef.setSize(size);
				}
			}
		}
	}
	
	public void setKeyCode(String label, String code) {
		for(List<KeyDef> row : keys) {
			for(KeyDef keydef : row) {
				if (label.equals(keydef.getLabel())) {
					keydef.setValue(code);
				}
			}
		}
	}
}
