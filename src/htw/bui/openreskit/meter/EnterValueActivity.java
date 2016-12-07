package htw.bui.openreskit.meter;

import htw.bui.openreskit.meter.EnterValueFragment.OnValueEnteredListener;
import htw.bui.openreskit.odata.MeterRepository;

import javax.inject.Inject;

import roboguice.activity.RoboFragmentActivity;
import android.app.ActionBar;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;

public class EnterValueActivity extends RoboFragmentActivity implements OnValueEnteredListener {
	@Inject
	FragmentManager mfragMan;
	
	@Inject
	MeterRepository mRepository;
	

	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.enter_value_fragment);

		if (Utils.isTablet(this)) 
		{
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
		}
		else
		{
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
		}
        
        Intent launchingIntent = getIntent();
        long readingId = launchingIntent.getExtras().getLong("ReadingId");
        EnterValueFragment enterValueFragment = (EnterValueFragment) mfragMan.findFragmentById(R.id.enter_value_fragment);
        enterValueFragment.getReadingDetails(readingId);
        
		ActionBar bar = getActionBar();
		bar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_USE_LOGO | ActionBar.DISPLAY_SHOW_TITLE);
		bar.setDisplayShowHomeEnabled(true);
    }

	public void onValueEntered(long readingId, double fillLevel) 
	{
		mRepository.updateValueForReading(readingId, fillLevel);
		finish();
		
	}
}