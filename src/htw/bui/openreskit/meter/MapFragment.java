package htw.bui.openreskit.meter;

import htw.bui.openreskit.domain.meter.Meter;
import htw.bui.openreskit.domain.organisation.Map;
import htw.bui.openreskit.odata.MeterRepository;

import java.util.List;

import roboguice.fragment.RoboFragment;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

import com.google.inject.Inject;

public class MapFragment extends RoboFragment {

	@Inject
	private MeterRepository mRepository;

	private TouchImageView mTouchImageView;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) 
	{
		// The last two arguments ensure LayoutParams are inflated
		// properly.
		View rootView = inflater.inflate(R.layout.map_fragment, container, false);
		ViewGroup layout = (ViewGroup) rootView.findViewById(R.id.mapContainer);

		Bundle args = getArguments();
		int mapId = args.getInt("MapId");

		Map map = mRepository.getMapById(mapId);
		List<Meter> metersOnMap = mRepository.getMetersByMapId(mapId);

		Resources resources = getActivity().getResources();
		float scale = resources.getDisplayMetrics().density;

		Bitmap mapBitmap =  BitmapFactory.decodeByteArray(map.getMapSource().getBinarySource(), 0, map.getMapSource().getBinarySource().length);

		//set default bitmap config if none
		android.graphics.Bitmap.Config bitmapConfig = mapBitmap.getConfig();

		if(bitmapConfig == null) 
		{
			bitmapConfig = android.graphics.Bitmap.Config.ARGB_8888;
		}
		mapBitmap = mapBitmap.copy(bitmapConfig, true);

		Canvas canvas = new Canvas(mapBitmap);

		// new antialised Paint
		Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		// text color - #3D3D3D
		textPaint.setColor(Color.rgb(61, 61, 61));
		// text size in pixels
		textPaint.setTextSize((int) (14 * scale));
		// text shadow
		textPaint.setShadowLayer(1f, 0f, 1f, Color.WHITE);

		
		Paint rectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		rectPaint.setStyle(Paint.Style.FILL);
		rectPaint.setColor(Color.rgb(210, 210, 210));
		
		Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		borderPaint.setStyle(Paint.Style.STROKE);
		borderPaint.setColor(Color.rgb(20, 20, 20));
		
		// new antialised Paint
		Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		circlePaint.setStyle(Paint.Style.FILL);
		circlePaint.setColor(Color.rgb(200, 0, 30));
	
		// new antialised Paint
		Paint circleBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		circleBorderPaint.setStyle(Paint.Style.STROKE);
		circleBorderPaint.setColor(Color.rgb(20, 20, 20));
		
		
		for (Meter meter : metersOnMap) 
		{
			Rect bounds = new Rect();
			textPaint.getTextBounds(meter.getNumber(), 0, meter.getNumber().length(), bounds);
			
			int x = (int)meter.getMapPosition().getXposition();
			int y = (int)meter.getMapPosition().getYposition();
			canvas.drawCircle(x, y, 12, circlePaint);
			canvas.drawCircle(x, y, 12, circleBorderPaint);
			canvas.drawRect(x+(25*scale)+bounds.left-(10*scale), y+10+bounds.top-(10*scale), x+(25*scale)+bounds.right+(10*scale), y+10+bounds.bottom+(10*scale), rectPaint);
			canvas.drawRect(x+(25*scale)+bounds.left-(10*scale), y+10+bounds.top-(10*scale), x+(25*scale)+bounds.right+(10*scale), y+10+bounds.bottom+(10*scale), borderPaint);
			canvas.drawText(meter.getNumber(), x+(25*scale), y+10, textPaint);
		}

		mTouchImageView = new TouchImageView(getActivity());
		mTouchImageView.setImageBitmap(mapBitmap);
		mTouchImageView.setMaxZoom(4f); //change the max level of zoom, default is 3f
		mTouchImageView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		layout.addView(mTouchImageView);

		return rootView;


	}
}
