package htw.bui.openreskit.meter;

//import android.app.Fragment;
import htw.bui.openreskit.domain.meter.MeterReading;
import htw.bui.openreskit.meter.ReadingFragment.OnReadingSelectedListener;
import htw.bui.openreskit.odata.MeterRepository;

import javax.inject.Inject;

import roboguice.activity.RoboFragmentActivity;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class ReadingActivity extends RoboFragmentActivity implements OnReadingSelectedListener {
	@Inject
	FragmentManager mFragMan;
	
	@Inject
	MeterRepository mRepository;
	
	private ReadingFragment mReadingFragment;
	
	private long mSeriesId;
	
	private int mThemeId = -1;
	
	private Activity mContext;
		
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reading_fragment);
        mContext = this;        		
		if (Utils.isTablet(this)) 
		{
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
		}
		else
		{
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
		}
        
		//get bundled contents from launching activity and pass to reading fragment
        Intent launchingIntent = getIntent();
        mSeriesId = launchingIntent.getExtras().getLong("SeriesId");
        mReadingFragment = (ReadingFragment) mFragMan.findFragmentById(R.id.reading_fragment);
        mReadingFragment.updateReadings(mSeriesId);
        
        ActionBar bar = getActionBar();
		bar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_USE_LOGO | ActionBar.DISPLAY_SHOW_TITLE);
		bar.setDisplayShowHomeEnabled(true);
    }
	  
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.reading_menu, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case R.id.scanBarcode1:
			IntentIntegrator.initiateScan(mContext);
			return true;
		case R.id.showPreferences1:
			startPreferences();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	private void startPreferences()
	{
		Intent startPreferences = new Intent(this, Preferences.class);
		startPreferences.putExtra("theme", mThemeId);
		this.startActivity(startPreferences);
	}
	
	@Override
	public void onBackPressed() {
	           super.onBackPressed();
	            mReadingFragment.removeRepositoryListener();
	    }
	
	public void onReadingSelected(long readingId) 
	{
		//if ReadingFragment not present start new Activity (Phone)
		Intent showEnterValueForm = new Intent(getApplicationContext(), EnterValueActivity.class);
		showEnterValueForm.putExtra("ReadingId", readingId);
		startActivity(showEnterValueForm);	
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent intent)
	{
		IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
		String barcode = scanResult.getContents();

		// if QR Code Scan request
		if (requestCode == 64444)
		{
			if (resultCode == RESULT_OK)
			{
				MeterReading mr = mRepository.getLatestMeterReadingForSeriesByBarcode(barcode, mSeriesId);
				if (mr != null) 
				{
					onReadingSelected(mr.getId());
				}
				else
				{
					Toast.makeText(getApplicationContext(), "Der Zähler befindet sich nicht in der aktuellen Serie!", Toast.LENGTH_SHORT).show();
				}
			}
		}
	}
}
