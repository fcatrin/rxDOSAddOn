package retrobox.utils;

import java.util.List;

import xtvapps.core.AndroidFonts;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ListOptionAdapter extends BaseAdapter {
	
	boolean hasIcons = false;
	List<ListOption>options;

	static ViewCustomizer viewCustomizer = null;
	
	public static String fontName = RetroBoxUtils.FONT_DEFAULT_R;
	
	public ListOptionAdapter(List<ListOption> options) {
		this.options = options;
		for(ListOption option : options) {
			hasIcons = option.hasIcon();
			if (hasIcons) break;
		}
	}
	
	@Override
	public int getCount() {
		return options.size();
	}

	@Override
	public Object getItem(int position) {
		return options.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) parent.getContext()
		        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View optionView = convertView!=null?convertView:inflater.inflate(R.layout.listitem_options, parent, false);
		
		TextView txtName = (TextView)optionView.findViewById(R.id.optionName);
		TextView txtValue = (TextView)optionView.findViewById(R.id.optionValue);
		
		AndroidFonts.setViewFont(txtName, fontName);
		AndroidFonts.setViewFont(txtValue, fontName);
		
		ListOption kv = options.get(position);
		String name = kv.getValue();
		String preset = kv.getPreset();
		
		txtName.setText(name);
		if (preset!=null) {
			txtValue.setText(preset);
			txtValue.setVisibility(View.VISIBLE);
		} else {
			txtValue.setVisibility(View.GONE);
		}
		
		ImageView icon = (ImageView)optionView.findViewById(R.id.optionIcon);
		if (hasIcons) {
			if (kv.hasIcon()) {
				icon.setImageResource(kv.iconResourceId);
				icon.setVisibility(View.VISIBLE);
			} else {
				icon.setVisibility(View.INVISIBLE);
			}
		} else {
			icon.setVisibility(View.GONE);
		}
		
		if (viewCustomizer!=null) {
			viewCustomizer.customize(icon, txtName, txtValue);
		}
		
		return optionView;
	}

	public static void setViewCustomizer(ViewCustomizer viewCustomizer) {
		ListOptionAdapter.viewCustomizer = viewCustomizer;
	}

	public interface ViewCustomizer {
		public void customize(ImageView icon, TextView txtName, TextView txtValue);
	}
	
}
