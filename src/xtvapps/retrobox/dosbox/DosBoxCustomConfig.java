package xtvapps.retrobox.dosbox;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class DosBoxCustomConfig {
	
	private static Map<String, String> properties = new HashMap<String, String>();
	
	public static void init(String filename) {
		BufferedReader reader = null;
		try {
			InputStream is = new FileInputStream(new File(filename));
			reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			
			String line = null;
			while ((line = reader.readLine())!=null) {
				parseLine(line);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (reader!=null) try {reader.close();} catch (Exception e){}
		}
	}
	
	private static void parseLine(String line) {
		if (line.startsWith("#") || line.startsWith("[") || line.indexOf("=")<0) return;
		String parts[] = line.split("=");
		if (parts.length!=2) return;
		
		String key = parts[0].trim();
		String value = parts[1].trim();
		properties.put(key, value);
	}
	
	public static String getString(String key, String defaultValue) {
		String value = properties.get(key);
		return value!=null?value:defaultValue;
		
	}
	
	public static int getInt(String key, int defaultValue) {
		String value = getString(key, defaultValue + "");
		try {
			return Integer.parseInt(value);
		} catch (Exception e) {
			return defaultValue;
		}
	}
	
	public static boolean getBoolean(String key, boolean defaultValue) {
		String value = getString(key, defaultValue + "");
		return value.equals("true");
	}
}
