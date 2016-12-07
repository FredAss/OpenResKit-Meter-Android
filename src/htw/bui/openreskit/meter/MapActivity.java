package htw.bui.openreskit.meter;

import htw.bui.openreskit.odata.MeterRepository;
import roboguice.activity.RoboFragmentActivity;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.inject.Inject;

public class MapActivity extends RoboFragmentActivity 
{
	@Inject
	private MeterRepository mRepository;

	MapCollectionPagerAdapter mMapCollectionPagerAdapter;
	ViewPager mViewPager;

	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.map_pager);

		mMapCollectionPagerAdapter =
				new MapCollectionPagerAdapter(getSupportFragmentManager(), mRepository.mMaps);
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mMapCollectionPagerAdapter);


		ActionBar bar = getActionBar();
		bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		bar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_USE_LOGO | ActionBar.DISPLAY_SHOW_TITLE);
		bar.setDisplayShowHomeEnabled(true);

		ActionBar.TabListener tabListener = new ActionBar.TabListener() {


			public void onTabReselected(Tab tab,
					android.app.FragmentTransaction ft) {
				// TODO Auto-generated method stub

			}

			public void onTabSelected(Tab tab,
					android.app.FragmentTransaction ft) {
				mViewPager.setCurrentItem(tab.getPosition());

			}

			public void onTabUnselected(Tab tab,
					android.app.FragmentTransaction ft) {
				// TODO Auto-generated method stub

			}
		};

		mViewPager.setOnPageChangeListener(
				new ViewPager.SimpleOnPageChangeListener() {
					@Override
					public void onPageSelected(int position) {
						// When swiping between pages, select the
						// corresponding tab.
						getActionBar().setSelectedNavigationItem(position);
					}
				});

		// Add 3 tabs, specifying the tab's text and TabListener
		for (int i = 0; i < mRepository.mMaps.size(); i++) {
			bar.addTab(
					bar.newTab()
					.setText(mRepository.mMaps.get(i).getName())
					.setTabListener(tabListener));
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.map_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case R.id.closeMap:
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

}
