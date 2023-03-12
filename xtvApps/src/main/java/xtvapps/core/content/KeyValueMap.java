package xtvapps.core.content;

import java.util.HashMap;
import java.util.Map;

public class KeyValueMap extends KeyValue {
	private Map<String, Object> map = new HashMap<String, Object>();
	
	public void put(String key, Object o) {
		map.put(key, o);
	}
	
	public Object get(String key) {
		return map.get(key);
	}
}
