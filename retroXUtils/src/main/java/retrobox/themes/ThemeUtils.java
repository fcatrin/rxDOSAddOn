package retrobox.themes;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.res.AssetManager;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.graphics.drawable.shapes.RoundRectShape;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.StateSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import retrobox.utils.R;
import xtvapps.core.AndroidCoreUtils;
import xtvapps.core.AndroidFonts;
import xtvapps.core.Utils;
import xtvapps.core.xml.SimpleXML;

public class ThemeUtils {
	private static final String LOGTAG = ThemeUtils.class.getSimpleName();

	public static ThemeResourceLocator resourceLocator = null;

	public static int width, height;
	private static int screenWidth;
	private static int screenHeight;
	
	private ThemeUtils() {}
	
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
	public static void initScreenSize(Activity activity) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
	        DisplayMetrics metrics = new DisplayMetrics();
	        activity.getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
	        screenWidth = metrics.widthPixels;
	        screenHeight = metrics.heightPixels;
	    } else {
			DisplayMetrics dm = activity.getApplicationContext().getResources().getDisplayMetrics();
			screenWidth = dm.widthPixels;
			screenHeight = dm.heightPixels;
	    }
		Log.d(LOGTAG, "screen " + screenWidth +"x" + screenHeight);
	}

	public static void unpackThemes(Activity activity, File baseDir) throws IOException {
		baseDir.mkdirs();
		
		String dir = "themes";
		AssetManager assets = activity.getAssets();
		String[] files = assets.list(dir);
		
		// always uncompress root files
		// save folder (themes) for later
		List<String> themeDirs = new ArrayList<String>();
		for(String file : files) {
			String fileName = dir + "/" + file;
			try {
				InputStream is = assets.open(fileName);
				File dstFile = new File(baseDir, file);
				Utils.copyFile(is, new FileOutputStream(dstFile));
			} catch (FileNotFoundException e) {
				themeDirs.add(file);
			}
		}
		
		// now for each theme, skip if versions are the same except in development mode 
		for(String themeDir : themeDirs) {
			File themeFile = new File(baseDir, themeDir +"/theme.xml");
			try {
				if (themeFile.exists()) {
					Document xmlAsset   = SimpleXML.parse(assets.open(dir + "/" + themeDir + "/theme.xml"));
					if (!SimpleXML.getBoolAttribute(xmlAsset.getDocumentElement(), "development", false)) {
						Document xmlCurrent = SimpleXML.parse(themeFile);
						int versionCurrent = SimpleXML.getIntAttribute(xmlCurrent.getDocumentElement(), "version", 1);
						int versionAsset   = SimpleXML.getIntAttribute(xmlAsset.getDocumentElement(), "version", 1);
						if (versionAsset == versionCurrent) continue;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			AndroidCoreUtils.unpackAssets(activity, dir + "/" + themeDir, baseDir.getParentFile());
		}
	}
	
	public static void applyDefaultColors(Activity activity) {
		int viewResourceIds[] = new int[] {
				R.id.modal_dialog_actions,
				R.id.modal_dialog_actions_background,
				R.id.modal_dialog_actions_border,
				R.id.txtDialogActionTitle,
				R.id.txtDialogAction,
				R.id.modal_dialog_actions_buttons,
				
				R.id.modal_dialog_list,
				R.id.modal_dialog_list_background,
				R.id.modal_dialog_list_border,
				R.id.txtDialogListTitle,
				R.id.txtDialogListInfo,
				
				R.id.modal_dialog_chooser,
				R.id.modal_dialog_chooser_background,
				R.id.modal_dialog_chooser_border,
				R.id.txtDialogChooserTitle,
				R.id.txtDialogChooserInfo,
				R.id.modal_dialog_chooser_device,
				R.id.modal_dialog_chooser_footer,
				
				// R.id.gamepadView,

				R.id.modal_dialog_gamepad,

				R.id.modal_dialog_custom,
				R.id.modal_dialog_custom_background,
				R.id.modal_dialog_custom_border,
				R.id.modal_dialog_custom_container,
				R.id.modal_dialog_custom_buttons,
				
				// R.id.modal_dialog_letters,
				// R.id.modal_dialog_letters_background,
				
				// R.id.progress_modal,
				// R.id.progress_modal_background,
				
				// R.id.login_window,
				// R.id.login_window_background,
				
				R.id.modal_dialog_login,
				R.id.modal_dialog_login_background,
				R.id.modal_dialog_login_border,
				R.id.txtDialogLogin,
				R.id.modal_dialog_login_buttons,
				
				// R.id.emumovies_window,
				// R.id.emumovies_background,
		};
		
		int roundedResourceIds[] = new int[] {
				R.id.modal_dialog_actions_background,
				R.id.modal_dialog_actions_border,
				R.id.modal_dialog_list_background,
				R.id.modal_dialog_list_border,
				R.id.modal_dialog_chooser_background,
				R.id.modal_dialog_chooser_border,
				R.id.modal_dialog_custom_background,
				R.id.modal_dialog_custom_border,
		};
		
		int roundedTopIds[] = new int[] {
				R.id.txtDialogActionTitle,
				R.id.txtDialogListTitle,
				R.id.txtDialogChooserTitle,
				R.id.txtDialogLogin,
		};

		
		String colorNames[] = new String[] {
			/* dialog actions */
			"rx_dialog_glass",
			"rx_dialog_background",
			"rx_dialog_border",
			"rx_dialog_background_title",
			"rx_dialog_background",
			"rx_dialog_background",
			
			/* dialog list */
			"rx_dialog_glass",
			"rx_dialog_background",
			"rx_dialog_border",
			"rx_dialog_background_title",
			"rx_dialog_background",

			/* dialog file chooser */
			"rx_dialog_glass",
			"rx_dialog_background",
			"rx_dialog_border",
			"rx_dialog_background_title",
			"rx_dialog_background",
			"rx_dialog_background",
			"rx_dialog_background",
			
			/* dialog gamepad view */
			// "rx_dialog_background",
			
			/* dialog gamepad info view */
			"rx_dialog_background",

			/* dialog custom */
			"rx_dialog_glass",
			"rx_dialog_background",
			"rx_dialog_border",
			"rx_dialog_background",
			"rx_dialog_background",

			/* dialog letters */
			// "rx_dialog_glass",
			// "rx_dialog_background",

			/* dialog progress */
			// "rx_dialog_glass",
			// "rx_dialog_background",

			/* dialog login */
			// "rx_dialog_login_background",
			// "rx_dialog_background",

			/* dialog login (LAN) */
			"rx_dialog_glass",
			"rx_dialog_background",
			"rx_dialog_border",
			"rx_dialog_background_title",
			"rx_dialog_background",
			
			/* EmuMovies sync */
			// "rx_dialog_glass",
			// "rx_dialog_background",
		};
		
		applyDialogBackgroundStyles(activity, viewResourceIds, colorNames, 
				roundedResourceIds, roundedTopIds);

		int textResourceIds[] = new int[] {
				R.id.txtDialogActionTitle,
				R.id.txtDialogAction,

				R.id.txtDialogListTitle,
				R.id.txtDialogListInfo,
				
				R.id.txtDialogChooserTitle,
				R.id.txtStorage,
				R.id.txtDialogChooserInfo,
				R.id.txtPanelStatus1,
				R.id.txtPanelStatus2,

				R.id.txtGamepadInfoTop,
				R.id.txtGamepadInfoBottom,

				R.id.txtDialogLogin,
				R.id.txtDialogLoginUserLabel,
				R.id.txtDialogLoginUser,
				R.id.txtDialogLoginPasswordLabel,
				R.id.txtDialogLoginPassword,

		};
		
		String styleNames[] = new String[] {
				/* dialog action */
				"rx_dialog_title",
				"rx_dialog_text",
				
				/* dialog list */
				"rx_dialog_title",
				"rx_dialog_text",
				
				/* dialog chooser */
				"rx_dialog_title",
				"rx_dialog_chooser_storage",
				"rx_dialog_chooser_info",
				"rx_dialog_chooser_status1",
				"rx_dialog_chooser_status2",

				/* dialog gamepad info */
				"rx_dialog_text",
				"rx_dialog_text",

				/* dialog login (LAN) */
				"rx_dialog_title",
				"rx_dialog_text",
				"rx_editor_text",
				"rx_dialog_text",
				"rx_editor_text",

		};
		
		applyTextStyles(activity, textResourceIds, styleNames);

		int textEditorResourceids[] = {
				R.id.txtDialogLoginUser, R.id.txtDialogLoginPassword
		};
		
		for(int textEditorResourceid : textEditorResourceids) {
			setTextEditorStyle((EditText)activity.findViewById(textEditorResourceid));
		}
		
		int bordersResourceIds[] = {
				R.id.modal_dialog_actions_border,
				R.id.modal_dialog_list_border,
				R.id.modal_dialog_chooser_border,
				R.id.modal_dialog_custom_border,
				R.id.modal_dialog_login_border,
		};
		
		// hide all dialog borders
		for(int bordersResourceId : bordersResourceIds) {
			activity.findViewById(bordersResourceId).setBackgroundColor(0x00000000);
		}
			
		
		int listViewResIds[] = {
				R.id.lstDialogList, R.id.lstDialogChooser, // R.id.login_method_list
		};
		
		for(int listViewResId : listViewResIds) {
			ListView listView = (ListView)activity.findViewById(listViewResId);
			applyDefaultListStyles(listView);
		}
		
		ImageView imgGamepadInfo  = (ImageView)activity.findViewById(R.id.gamepadDialogImage);
		imgGamepadInfo. setColorFilter(0xC0FFFFFF, PorterDuff.Mode.MULTIPLY);
	}
	
	public static void applyDefaultButtonStyles(Activity activity) {
		int buttonResourceIds[] = {
			R.id.btnDialogActionPositive, R.id.btnDialogActionNegative,
			R.id.btnDialogCustomPositive, R.id.btnDialogCustomNegative,
			R.id.btnDialogLoginPositive, R.id.btnDialogLoginNegative,
		};
		
		for(int resourceId : buttonResourceIds) {
			ThemeUtils.applyStyleButton((Button)activity.findViewById(resourceId));
		}
	}

	
	public static void applyDefaultListStyles(ListView list) {
		
		Color bgColorNormal   = Color.getNamedColor("rx_listitem_background");
		Color bgColorSelected = Color.getNamedColor("rx_listitem_background_selected");
		
		ColorDrawable bgDrawableNormal   = new ColorDrawable(bgColorNormal.asInt());
		ColorDrawable bgDrawableSelected = new ColorDrawable(bgColorSelected.asInt());
		
		StateListDrawable backgroundSelector = makeBackgroundSelector(
				bgDrawableNormal, 
				bgDrawableSelected);
		
		list.setSelector(backgroundSelector);
	}

	public static StateListDrawable makeBackgroundSelector(Drawable backgroundNormal, Drawable backgroundSelected) {
	    StateListDrawable res = new StateListDrawable();
	    res.addState(new int[]{android.R.attr.state_focused}, backgroundSelected);
	    res.addState(new int[]{android.R.attr.state_selected}, backgroundSelected);
	    res.addState(StateSet.WILD_CARD, backgroundNormal);
	    return res;
	}

	public static void applyTextStyles(Activity activity, int textResourceIds[], String styleNames[]) {
		for(int i=0; i<textResourceIds.length; i++) {
			int resourceId = textResourceIds[i];
			String styleName = styleNames[i];
			
			TextView textView = (TextView)activity.findViewById(resourceId);
			applyStyle(textView, styleName);
		}		
	}

	public static void applyStyle(TextView text, String styleName) {
		applyStyle(text, getTextStyle(styleName));
	}

	public static void applyStyle(TextView text, Style style) {
		if (style == null) return;
		
		applyFont(text, style);
		text.setTextColor(style.getFontColor().asInt());
		text.setTextSize(TypedValue.COMPLEX_UNIT_PX, scaleY(style.fontSize));
		if (style.shadowName!=null) {
			Shadow shadow = Shadow.get(style.shadowName);
			if (shadow!=null) {
				shadow.apply(text);
			} else {
				Log.e(LOGTAG, "Shadow name not found " + style.shadowName);
			}
		}
	}
	
	public static void applyDialogBackgroundStyles(Activity activity, int viewResourceIds[], String colorNames[], 
			int roundedResourceIds[], int[] roundedTopIds) {
		
		for(int i=0; i<viewResourceIds.length; i++) {
			int resourceId = viewResourceIds[i];
			String colorName = colorNames[i];
			
			boolean rounded = false;
			for(int r=0; roundedResourceIds!=null && r<roundedResourceIds.length && !rounded; r++) {
				rounded = resourceId == roundedResourceIds[r];
			}
			
			boolean roundedTop = false;
			for(int r=0; roundedTopIds!=null && r<roundedTopIds.length && !roundedTop; r++) {
				roundedTop = resourceId == roundedTopIds[r];
			}
			applyBackgroundColor(activity, resourceId, colorName, rounded, roundedTop);
		}		
	}

	public static void applyBackgroundColor(Activity activity, int resourceId, String colorName, boolean rounded, boolean roundedTop) {
		ShapeDrawable background = createBackground(colorName, colorName, rounded, roundedTop);
		View view = activity.findViewById(resourceId); 
		view.setBackgroundDrawable(background);	
	}

	public static ShapeDrawable createBackground(String fillColorName, String strokeColorName, boolean rounded) {
		return createBackground(fillColorName, strokeColorName, rounded, false);
	}

	public static ShapeDrawable createBackground(String fillColorName, String strokeColorName, boolean rounded, boolean roundedTop) {
		Color fillColor   = Color.getNamedColor(fillColorName);
		Color strokeColor = Color.getNamedColor(strokeColorName);

		RectShape rs = roundedTop ?
				new RoundRectShape(new float[] { 4, 4, 4, 4, 0, 0, 0, 0 }, null, null) :
				(rounded ?
						new RoundRectShape(new float[] { 4, 4, 4, 4, 4, 4, 4, 4 }, null, null) :
						new RectShape());
		
		return new CustomShapeDrawable(rs, fillColor.asInt(), strokeColor.asInt(), 2);
	}

	public static void applyProgressColors(ProgressBar progressBar, Color background, Color foreground) {
		
		if (background == null) background = Color.getNamedColor("rx_progress_background");
		if (foreground == null) foreground = Color.getNamedColor("rx_progress_foreground");
		
		ShapeDrawable barBackground = new ShapeDrawable();
		barBackground.getPaint().setColor(background.asInt());
		
		ShapeDrawable barForeground = new ShapeDrawable();
		barForeground.getPaint().setColor(foreground.asInt());
		ClipDrawable barBackgroundClip = new ClipDrawable(barForeground, Gravity.START, ClipDrawable.HORIZONTAL);
				
		LayerDrawable barDrawable = new LayerDrawable(new Drawable[] {barBackground, barBackgroundClip});
		barDrawable.setId(0, android.R.id.background);
		barDrawable.setId(1, android.R.id.progress);
		
		progressBar.setProgressDrawable(barDrawable);
	}

	public static void setTextEditorStyle(EditText textEditorView) {
		ShapeDrawable normalBackground  = createBackground("rx_editor_background", "rx_editor_stroke", false);
		ShapeDrawable focusedBackground = createBackground("rx_editor_background_focused", "rx_editor_stroke_focused", false);
		
		final StateListDrawable backgroundSelector = makeBackgroundSelector(normalBackground, focusedBackground);
		textEditorView.setBackgroundDrawable(backgroundSelector);
	}

	public static void applyFont(TextView textView, Style style) {
		if (style.fontName == null) return;
		
		String fontName = AndroidFonts.getNamedFont(style.fontName);
		if (fontName == null) fontName = style.fontName;
		
		File fontFile = resourceLocator.getFile("fonts/" + fontName);
		if (fontFile.exists()) {
			AndroidFonts.setViewFont(textView, fontFile);
		} else {
			AndroidFonts.setViewFont(textView, fontName);
		}
	}
	
	public static void applyStyleButton(TextView button) {
		
		applyStyle(button, "rx_button_text");
		
		RoundRectShape rs = new RoundRectShape(new float[] { 2, 2, 2, 2, 2, 2, 2, 2 }, null, null);
		
		Color buttonBackgroundColorNormal = Color.getNamedColor("rx_button_background");
		Color buttonStrokeColorNormal = Color.getNamedColor("rx_button_border");

		Color buttonBackgroundColorFocus = Color.getNamedColor("rx_button_background_focus");
		Color buttonStrokeColorFocus = Color.getNamedColor("rx_button_border_focus");

		ShapeDrawable backgroundNormal = new CustomShapeDrawable(rs, buttonBackgroundColorNormal.asInt(), buttonStrokeColorNormal.asInt(), 2);
		ShapeDrawable backgroundFocus  = new CustomShapeDrawable(rs, buttonBackgroundColorFocus.asInt(), buttonStrokeColorFocus.asInt(), 2);

		StateListDrawable background = makeBackgroundSelector(backgroundNormal, backgroundFocus);
		button.setBackgroundDrawable(background);
		
		Style normalStyle  = Style.getNamedStyle("rx_button_text");
		Style focusedStyle = Style.getNamedStyle("rx_button_text_focus");
		
		int colorNormal  = normalStyle.getFontColor().asInt();
		int colorFocused = focusedStyle.getFontColor().asInt();
		
		ColorStateList textColor = makeColorSelector(colorNormal, colorFocused);
		button.setTextColor(textColor);
	}
	
	public static ColorStateList makeColorSelector(int colorNormal, int colorSelected) {
		return new ColorStateList(
				new int[][]{
						new int[]{android.R.attr.state_selected},
						new int[] {android.R.attr.state_focused},
						StateSet.WILD_CARD
				},
				new int[] {
						colorSelected,
						colorSelected,
						colorNormal
				}
	    );
	}

	public static Style getTextStyle(String styleName) {
		if (styleName == null) styleName = "default";
		
		Style style = Style.getNamedStyle(styleName);
		if (style == null) {
			Log.w(LOGTAG, "Unknown style " + styleName);
			return null;
		}
		return style;
	}

	public static int getScreenWidth() {
		return screenWidth;
	}
	
	public static int getScreenHeight() {
		return screenHeight;
	}
	
	public static int getWidth() {
		return width;
	}

	public static int getHeight() {
		return height;
	}

	
	public static int scaleX(float x) {
		return (int)(x * screenWidth / width);
	}
	
	public static int scaleY(float y) {
		return (int)(y * screenHeight / height);
	}
	
	public static void clearNamedObjects() {
		Color.clearNamedColors();
		Shadow.clearNamedShadows();
		Style.clearNamedStyles();
		AndroidFonts.clearNamedFonts();
		AndroidFonts.clearKnownFonts();
	}
	
	public static void readNamedColors(Element root) {
		Element rootColors = SimpleXML.getElement(root, "colors");
		
		if (rootColors != null) {
			List<Element> namedColorElements = SimpleXML.getElements(rootColors, "color");
			for(Element namedColorElement : namedColorElements) {
				String name = namedColorElement.getAttribute("name");
				String spec = namedColorElement.getTextContent();
				Color.addNamedColor(name, spec);
			}
		}
	}
	
	public static void readNamedShadows(Element root) {
		Element rootColors = SimpleXML.getElement(root, "shadows");
		
		if (rootColors != null) {
			List<Element> namedShadowElements = SimpleXML.getElements(rootColors, "shadow");
			for(Element namedShadowElement : namedShadowElements) {
				Shadow shadow = buildShadow(namedShadowElement);
				Shadow.addNamedShadow(shadow.getName(), shadow);
			}
		}
	}
	
	public static void readNamedFonts(Element root) {
		Element rootFonts = SimpleXML.getElement(root, "fonts");
		
		if (rootFonts != null) {
			List<Element> rootFontElements = SimpleXML.getElements(rootFonts, "font");
			for(Element namedColorElement : rootFontElements) {
				String name = namedColorElement.getAttribute("name");
				String spec = namedColorElement.getTextContent();
				AndroidFonts.addNamedFont(name, spec);
			}
		}
	}

	public static Shadow buildShadow(Element node) {
		Shadow shadow = new Shadow();
		
		shadow.name = SimpleXML.getAttribute(node, "name");
		shadow.radius = SimpleXML.getFloat(node, "radius");
		shadow.dx  = SimpleXML.getFloat(node, "dx");
		shadow.dy  = SimpleXML.getFloat(node, "dy");
		shadow.colorName = SimpleXML.getText(node, "color");
		
		return shadow;
	}

	public static void readStyles(Element root) {
		Element styleElementContainer = SimpleXML.getElement(root, "styles");
		List<Element> styleElements = SimpleXML.getElements(styleElementContainer, "style");
		
		for(Element styleElement : styleElements) {
			Style style = buildStyle(styleElement);
			Style defaultStyle = Style.getNamedStyle(style.getName());
			
			if (defaultStyle!=null) {
				style.merge(defaultStyle);
			}
			Style.addNamedStyle(style.getName(), style);
		}
	}

	private static void mergeStyle(Style defaultStyle, Style style) {
		Style parentStyle = defaultStyle;
		String parentName = style.getParentName();
		if (parentName != null) {
			parentStyle = Style.getNamedStyle(parentName);
			if (parentStyle == null) {
				throw new RuntimeException("Unknown parent " + parentName + " for style " + style.getName());
			}
		}
		style.merge(parentStyle);
	}
	
	public static void mergeStyles() {
		// prepare default style
		Style defaultStyle = Style.getNamedStyle("default"); 
		if (defaultStyle == null) {
			defaultStyle =	buildStyleDefault();
		}
		
		Map<String, Style> namedStyles = Style.getNamedStyles();
		
		// inheritance is processed in declared order for now
		Log.d("STYLES", "non merged: " + namedStyles);

		for(Style style : namedStyles.values()) {
			mergeStyle(defaultStyle, style);
		}
		
		Log.d("STYLES", "merged: " + namedStyles);
	}

	private static Style buildStyleDefault() {
		Style style = new Style();
		style.fontSize = buildDimension("12px");
		style.colorName = "#FFFFFFFF";
		style.fontWeight = Style.FontWeight.Normal;
		//style.width  = SIZE_WRAP;
		//style.height = SIZE_WRAP;
		return style;
	}	
	
	private static Style buildStyle(Element element) {
		Style style = new Style();
		style.name = SimpleXML.getAttribute(element, "name");
		if (style.name == null) {
			throw new RuntimeException("There is a style element without a name " + SimpleXML.asString(element));
		}
		style.parentName = SimpleXML.getAttribute(element, "parent");
		style.fontName =  SimpleXML.getText(element, "font");
		style.fontSize = buildDimension(SimpleXML.getText(element, "size"));
		style.colorName = SimpleXML.getText(element, "color");
		style.shadowName = SimpleXML.getText(element, "shadow");
		// TODO font weight
		return style;
	}

	public static int buildDimension(String value) {
		if (value != null) {
			if (value.endsWith("px")) {
				value = value.replace("px", "");
				return Utils.str2i(value);
			} else {
				int n = Utils.str2i(value);
				if (String.valueOf(n).equals(value)) return n;
			}
			throw new RuntimeException("Invalid dimension " + value);
		}
		return 0;
	}


}
