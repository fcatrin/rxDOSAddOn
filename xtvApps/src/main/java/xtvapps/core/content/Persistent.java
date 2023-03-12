package xtvapps.core.content;

import org.json.JSONException;
import org.json.JSONObject;

public interface Persistent {
	JSONObject toJSON() throws JSONException;
	void fromJSON(JSONObject o) throws JSONException;
}
