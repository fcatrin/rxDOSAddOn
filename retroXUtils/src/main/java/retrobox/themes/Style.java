package retrobox.themes;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class Style {
	
	public enum FontWeight {Undefined, Bold, Normal};
	private static Map<String, Style> namedStyles  = new LinkedHashMap<String, Style>();

	
	public String name;
	public String parentName;
	public String fontName;
	public float  fontSize;
	public String colorName;
	public String shadowName;
	public FontWeight fontWeight = FontWeight.Undefined;
	
	//int width = SIZE_UNDEFINED;
	//int height = SIZE_UNDEFINED;
	
	public static Style buildFrom(Style from) {
		Style style = new Style();
		style.name = from.name + "_from";
		style.fontName = from.fontName;
		style.fontSize = from.fontSize;
		style.colorName = from.colorName;
		style.fontWeight = from.fontWeight;
		style.shadowName = from.shadowName;
		
		return style;
	}
	
	public void merge(Style parent) {
		if (fontName == null) {
			fontName = parent.fontName;
		}
		
		if (fontSize == 0) {
			fontSize = parent.fontSize;
		}
		
		if (fontWeight == FontWeight.Undefined) {
			fontWeight = parent.fontWeight;
		}
		
		if (colorName == null) {
			colorName = parent.colorName;
		}
		
		if (shadowName == null) {
			shadowName = parent.shadowName;
		}
		/*
		if (width == SIZE_UNDEFINED) {
			width = parent.width;
		}
		
		if (height == SIZE_UNDEFINED) {
			height = parent.height;
		}
		*/
		
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public float getFontSize() {
		return fontSize;
	}

	public void setFontSize(float fontSize) {
		this.fontSize = fontSize;
	}

	public FontWeight getFontWeight() {
		return fontWeight;
	}

	public void setFontWeight(FontWeight fontWeight) {
		this.fontWeight = fontWeight;
	}

	public Color getFontColor() {
		return Color.build(colorName);
	}

	public void setColorName(String colorName) {
		this.colorName = colorName;
	}

	public String getParentName() {
		return parentName;
	}

	public void setParentName(String parentName) {
		this.parentName = parentName;
	}

	public String getFontName() {
		return fontName;
	}

	public void setFontName(String fontName) {
		this.fontName = fontName;
	}

	public static void clearNamedStyles() {
		namedStyles.clear();
	}
	
	public static void addNamedStyle(String name, Style style) {
		namedStyles.put(name, style);
	}
	
	public static Style getNamedStyle(String name) {
		return namedStyles.get(name);
	}

	public static Map<String, Style> getNamedStyles() {
		return namedStyles;
	}

	@Override
	public String toString() {
		return String.format(Locale.US, "Style {name:%s, parent:%s, font:%s, size:%f, color:%s}", name, parentName, fontName, fontSize, colorName);
	}
	
}
