/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package htw.bui.openreskit.meter;

import htw.bui.openreskit.domain.meter.MeterReading;
import htw.bui.openreskit.meter.EnterValueFragment.OnValueEnteredListener;
import htw.bui.openreskit.meter.ReadingFragment.OnReadingSelectedListener;
import htw.bui.openreskit.meter.SeriesFragment.OnSeriesSelectedListener;
import htw.bui.openreskit.odata.MeterRepository;
import roboguice.activity.RoboFragmentActivity;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.inject.Inject;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class SeriesActivity extends RoboFragmentActivity implements OnSeriesSelectedListener, OnReadingSelectedListener, OnValueEnteredListener
{
	@Inject
	private MeterRepository mRepository;
	
	@Inject
	private FragmentManager mFragMan;
	
	private int mThemeId = -1;
	private Activity mContext;
	private long mSeriesId;
	private boolean mScanningEnabled = false;
	
	private FrameLayout mOverlayFramelayout;
	private View mHelpView;
	
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		mOverlayFramelayout = new FrameLayout(this);
		
		setContentView(mOverlayFramelayout);
		View view = getLayoutInflater().inflate(R.layout.series_fragment, mOverlayFramelayout, false);
		mOverlayFramelayout.addView(view);
		mContext = this;
		mHelpView = getLayoutInflater().inflate(R.layout.help_overlay, mOverlayFramelayout,false);

				
		if (Utils.isTablet(this)) 
		{
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
		}
		else
		{
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
		}
		
		ActionBar bar = getActionBar();
		bar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_USE_LOGO | ActionBar.DISPLAY_SHOW_TITLE);
		bar.setDisplayShowHomeEnabled(true);
		
		EnterValueFragment enterValueFrag = (EnterValueFragment) mFragMan.findFragmentById(R.id.enter_value_fragment);
		if (enterValueFrag != null)
		{
			enterValueFrag.getView().setVisibility(View.INVISIBLE);
		}
		
	}
	
	private OnClickListener mButtonListener = new OnClickListener() 
	{
		public void onClick(View v) 
		{
			startPreferences();
		}
	};
	
	@Override
	public void onResume() 
	{
		super.onResume();
		
		Button helpView = (Button) mOverlayFramelayout.findViewById(R.id.openPreferences);
		if (mRepository.mMeterReadings.isEmpty()) 
		{
			if (helpView == null)
			{
				mOverlayFramelayout.addView(mHelpView);
				Button helpButton = (Button) mHelpView.findViewById(R.id.openPreferences); 
				helpButton.setOnClickListener(mButtonListener);
			}
		}
		else
		{
			if (helpView != null)
			{
				helpView.setOnClickListener(null);
				mOverlayFramelayout.removeView(mHelpView);
			}
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case R.id.scanBarcode:
			IntentIntegrator.initiateScan(mContext);
			return true;
		case R.id.startSync:
			startSync();
			return true;
		case R.id.writeData:
			mRepository.writeDataToOdataService(mContext);
			return true;
		case R.id.showPreferences:
			startPreferences();
			return true;
		case R.id.showMaps:
			showMapActivity();
			return true;
		case R.id.deleteLocalData:
			mRepository.deleteLocalData();
			EnterValueFragment enterValueFrag = (EnterValueFragment) mFragMan.findFragmentById(R.id.enter_value_fragment);
			if (enterValueFrag != null)
			{
				enterValueFrag.getView().setVisibility(View.INVISIBLE);
			}
			onResume();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	private void showMapActivity() 
	{
		Intent showMaps = new Intent(mContext, MapActivity.class);
		startActivity(showMaps);
	}

	private void startSync() 
	{
		boolean unsavedData = false;
		if ( mRepository.mMeterReadings != null) 
		{
			for (MeterReading fr : mRepository.mMeterReadings) 
			{
				if (fr.isManipulated()) 
				{
					unsavedData = true;
					break;
				}
			}
		}
		
		if (unsavedData) 
		{
			// 1. Instantiate an AlertDialog.Builder with its constructor
			AlertDialog.Builder builder = new AlertDialog.Builder(mContext);

			// 2. Chain together various setter methods to set the dialog characteristics
			builder.setMessage("Es gibt ungespeicherte Ablesungen. Durch ein erneutes Abrufen gehen diese verloren! Möchten sie fortfahren?")
			       .setTitle("Ungespeicherte Daten");
			builder.setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   
		           }
		       });
			
			builder.setPositiveButton("Fortfahren", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		       		mRepository.getDataFromOdataService(mContext);
		           }
		       });

			// 3. Get the AlertDialog from create()
			AlertDialog dialog = builder.create();
			dialog.show();
		}
		else
		{
			mRepository.getDataFromOdataService(mContext);
    		mOverlayFramelayout.removeView(mHelpView);
		}
	}
	
	private void startPreferences()
	{
		Intent startPreferences = new Intent(this, Preferences.class);
		startPreferences.putExtra("theme", mThemeId);
		this.startActivity(startPreferences);
	}
	
	public void onSeriesSelected(long seriesId) 
	{
		ReadingFragment readingFrag = (ReadingFragment) mFragMan.findFragmentById(R.id.reading_fragment);

		mSeriesId = seriesId;
		if (readingFrag == null || !readingFrag.isInLayout()) 
		{
			//if ReadingFragment not present start new Activity (Phone)
			Intent showReadings = new Intent(mContext, ReadingActivity.class);
			showReadings.putExtra("SeriesId", mSeriesId);
			startActivity(showReadings);
		}
		else
		{
			//if ReadingFragment is present update (Tablet)
			readingFrag.updateReadings(mSeriesId);
		}
		
		mScanningEnabled = true;
		invalidateOptionsMenu();
	}
	
	public void onReadingSelected(long readingId) 
	{
		EnterValueFragment enterValueFrag = (EnterValueFragment) mFragMan.findFragmentById(R.id.enter_value_fragment);
		enterValueFrag.getView().setVisibility(View.VISIBLE);
		
		if (enterValueFrag == null || !enterValueFrag.isInLayout()) 
		{
			//if ReadingFragment not present start new Activity (Phone)
			Intent showEnterValueForm = new Intent(mContext, EnterValueActivity.class);
			showEnterValueForm.putExtra("ReadingId", readingId);
			startActivity(showEnterValueForm);
		}
		else
		{
			//if ReadingFragment is present update (Tablet)
			enterValueFrag.getReadingDetails(readingId);
		}
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent intent)
	{
		if (mSeriesId > 0) {
			EnterValueFragment enterValueFrag = (EnterValueFragment) mFragMan.findFragmentById(R.id.enter_value_fragment);
			ReadingFragment readingFrag = (ReadingFragment) mFragMan.findFragmentById(R.id.reading_fragment);
			
			IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
		
			// if QR Code Scan request
			if (requestCode == 64444)
			{
				if (resultCode == RESULT_OK)
				{
					MeterReading mr = mRepository.getLatestMeterReadingForSeriesByBarcode(scanResult.getContents(), mSeriesId);
					if (mr != null) 
					{
						enterValueFrag.getReadingDetails(mr.getId());
						readingFrag.hilightReading(mr.getId());
						if (enterValueFrag.getView().getVisibility() == View.INVISIBLE) 
						{
							enterValueFrag.getView().setVisibility(View.VISIBLE);
						}
						
					}
					else
					{
						Toast.makeText(mContext, "Der Zähler befindet sich nicht in der aktuellen Serie!", Toast.LENGTH_SHORT).show();
					}
				}
			}
		}
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		MenuItem item = menu.findItem(R.id.scanBarcode);
		if (mScanningEnabled)
		{
			item.setVisible(true);
		} else
		{
			item.setVisible(false);
		}
		return true;
	}

	public void onValueEntered(long readingId, double fillLevel) 
	{
		mRepository.updateValueForReading(readingId, fillLevel);
	}
}
