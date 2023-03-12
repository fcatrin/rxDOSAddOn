package retrobox.themes;

import java.util.HashMap;
import java.util.Map;

import android.widget.TextView;

public class Shadow {
	public String name;
	public float  radius;
	public float  dx;
	public float  dy;
	public String colorName;
	
	private static Map<String, Shadow> namedShadows = new HashMap<String, Shadow>();

	public static void clearNamedShadows() {
		namedShadows.clear();
	}
	
	public static void addNamedShadow(String name, Shadow shadow) {
		if (shadow!=null) namedShadows.put(name, shadow);
	}
	
	public void apply(TextView textView) {
		textView.setShadowLayer(radius, dx, dy, Color.build(colorName).asInt());
	}
	
	public static Shadow get(String spec) {
		Shadow namedShadow = namedShadows.get(spec);
		if (namedShadow!=null) {
			return clone(namedShadow);
		}
		return null;
	}
	
	private static Shadow clone(Shadow base) {
		Shadow shadow = new Shadow();
		
		shadow.name = base.name;
		shadow.radius = base.radius;
		shadow.dx  = base.dx;
		shadow.dy  = base.dy;
		shadow.colorName = base.colorName;
		
		return shadow;
	}

	public String getName() {
		return name;
	}
	
}
