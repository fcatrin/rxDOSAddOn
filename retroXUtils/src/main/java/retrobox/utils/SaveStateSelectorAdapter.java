package retrobox.utils;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import retrobox.content.SaveStateInfo;
import xtvapps.core.AndroidFonts;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class SaveStateSelectorAdapter extends BaseAdapter {
	private static final String LOGTAG = SaveStateSelectorAdapter.class.getSimpleName();
	List<SaveStateInfo> content = new ArrayList<SaveStateInfo>();
	private Context context;
	
	public SaveStateSelectorAdapter(Context context, List<SaveStateInfo> content, int selected) {
		this.content = content;
		this.context = context;
		
		List<SaveStateInfo> ordered = new ArrayList<SaveStateInfo>();
		ordered.addAll(content);
		
		Collections.sort(ordered, new Comparator<SaveStateInfo>() {
			@Override
			public int compare(SaveStateInfo lhs, SaveStateInfo rhs) {
				long lts = lhs.getTimestamp();
				long rts = rhs.getTimestamp();
				return lts < rts?1:
					(lts > rts?-1:0);
			}
		});
		
		for(int i=0; i<ordered.size(); i++) {
			ordered.get(i).setOrder(i);
		}
		
		for(int i=0; i<content.size(); i++) {
			SaveStateInfo info = content.get(i);
			
			info.setSelected(i==selected);
			info.setSlotInfo("Slot " + (i+1) + ":");
			
			String infoText = context.getString(R.string.slot_empty);
			long ts = info.getTimestamp();
			if (ts!=0) {
				DateFormat df = DateFormat.getDateTimeInstance();
				infoText = df.format(new Date(ts));
				if (info.getOrder() == 0) {
					infoText += " (" + 
							context.getString(R.string.slot_latest) 
							+  ")";
				} else {
					infoText += " (" + 
							context.getString(R.string.slot_latest_n).replace("{n}", String.valueOf(info.getOrder()))
							+ ")";
				}
			}
			info.setInfo(infoText);

		}
		
	}
	
	public void loadImages() {
		for(SaveStateInfo info : content) {
			if (info.getImageResourceId()!=0) return;

			File file = info.getScreenshot();
			Bitmap bitmap = null;
			if (file != null && file.exists()) {
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inDither = false;
				System.out.println("loading shot " + file.getAbsolutePath());
				bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
			}
			info.setBitmap(bitmap);
		}
	}
	
	public void releaseImages() {
		for(SaveStateInfo info : content) {
			info.setBitmap(null);
		}
	}
	
	@Override
	public Object getItem(int index) {
		return content.get(index);
	}
	
	@Override
	public int getCount() {
		return content.size();
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		View view = convertView;
		if(view == null) {
			LayoutInflater layout =  LayoutInflater.from(parent.getContext());
			view = layout.inflate(R.layout.savestate_cell, parent, false);
			AndroidFonts.setViewFont(view.findViewById(R.id.savestate_label), RetroBoxUtils.FONT_DEFAULT_M);
		}
		
		ImageView imageScreenshot = (ImageView)view.findViewById(R.id.savestate_image);
		TextView textView = (TextView)view.findViewById(R.id.savestate_label);

		final SaveStateInfo info = (SaveStateInfo)getItem(position);
		
		imageScreenshot.setImageBitmap(null);
		
		int imageResourceId = info.getImageResourceId();
		if (imageResourceId!=0) {
			imageScreenshot.setImageResource(info.exists()?imageResourceId:0);
		} else {
			imageScreenshot.setImageBitmap(info.getBitmap());
		}
		
		if (!info.exists()) {
			textView.setText(context.getString(R.string.slot_v_empty));
		} else if (info.getOrder() == 0) {
			textView.setText(context.getString(R.string.slot_v_latest));
		} else {
			String chars = context.getString(R.string.slot_v_latest_n);
			textView.setText(chars.substring(0,  (info.getOrder())*2));
		}

		return view;
	}

}
