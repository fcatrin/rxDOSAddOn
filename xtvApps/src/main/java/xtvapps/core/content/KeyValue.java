package xtvapps.core.content;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;


public class KeyValue implements Persistent {
	private String key;
	private String value;
	
	public KeyValue() {
	}
	
	public KeyValue(String key, String value) {
		this.key = key;
		this.value = value;
	}
	
	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public JSONObject toJSON() throws JSONException {
		JSONObject o = new JSONObject();
		o.put("key", key);
		o.put("value", value);
		return o;
	}
	public void fromJSON(JSONObject o) throws JSONException {
		key = o.getString("key");
		value = o.getString("value");
	}

	@Override
	public String toString() {
		return value;
	}

	@Override
	public int hashCode() {
		if (key == null) return 0;
		return key.hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null) return false;
		if (!(o instanceof KeyValue)) return false;
		return hashCode() == o.hashCode();
	}

	public static void sort(List<KeyValue> list) {
		Collections.sort(list, new Comparator<KeyValue>(){
			@Override
			public int compare(KeyValue ls, KeyValue rs) {
				return ls.getValue().compareTo(rs.getValue());
			}
		});
	}
}
