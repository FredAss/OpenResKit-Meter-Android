package htw.bui.openreskit.meter;

import htw.bui.openreskit.domain.organisation.Series;

import java.text.SimpleDateFormat;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class SeriesAdapter extends BaseAdapter {

	private final Activity mContext;
	private final List<Series> mSeries;
	private final int mRowResID;
	private final LayoutInflater mLayoutInflater;
	
	public SeriesAdapter(final Activity context, final int rowResID, final List<Series> series) 
	{
		mContext = context;
		mRowResID = rowResID;
		mSeries = series;
		mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}
	
	public int getCount() 
	{
		return mSeries.size();
	}
	
	//returns position in List
	public Object getItem(int position) 
	{
		return mSeries.get(position);
	}

	//returns the Database id of the item
	public long getItemId(int position) {
		return mSeries.get(position).getId();
	}
	
	// connects Unit members to be displayed with (text)views in a layout
	// per item
	public View getView(int position, View convertView, ViewGroup parent) {

		View rowView = convertView;
		if (rowView == null) 
		{
			rowView = mLayoutInflater.inflate(mRowResID, null);
					
			SeriesViewHolder viewHolder = new SeriesViewHolder();
			viewHolder.name = (TextView) rowView.findViewById(R.id.text1);
			viewHolder.coloredBox = (ImageView) rowView.findViewById(R.id.ImageView01);
			viewHolder.dateInfo = (TextView) rowView.findViewById(R.id.text2);
			rowView.setTag(viewHolder);
		}
		
		final Series singleSeries = mSeries.get(position);
		SeriesViewHolder holder = (SeriesViewHolder) rowView.getTag();
		holder.name.setText(singleSeries.getName());
		
		int r = singleSeries.getSeriesColor().getR() & 0xff;
		int g = singleSeries.getSeriesColor().getG() & 0xff;
		int b = singleSeries.getSeriesColor().getB() & 0xff;
		holder.coloredBox.setBackgroundColor(Color.argb(255, r, g, b));
		
		if(singleSeries.getRepeat()) 
		{
			SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy");
	        String formattedStartDate = formatter.format(singleSeries.getBegin());
	        String cycle = "";
	        switch (singleSeries.getCycle()) 
	        {
	        case 0:
	        	cycle = "täglich";
	        	break;
	        case 1:
	        	cycle = "wöchentlich";
	        	break;
	        case 2:
	        	cycle = "monatlich";
	        	break;
	        case 3:
	        	cycle = "jährlich";
	        	break;
	        default:
	        	break;
	        }
	       
	        if (singleSeries.getRepeatUntilDate() != null) 
	        {
	        	String formattedEndDate = formatter.format(singleSeries.getBegin());
	        	holder.dateInfo.setText(formattedStartDate + " - " + formattedEndDate + " ("+cycle+")");
	        }
	        else 
	        {
	        	holder.dateInfo.setText("vom " + formattedStartDate + " - " + singleSeries.getNumberOfRecurrences() + " mal ("+cycle+")");
	        }
	        
		}
		else
		{
			holder.dateInfo.setText("einmalig");
		}
		return rowView;
	}

	 static class SeriesViewHolder 
	{
		public TextView name;
		public ImageView coloredBox;
		public TextView dateInfo;
	}
	
}

	

