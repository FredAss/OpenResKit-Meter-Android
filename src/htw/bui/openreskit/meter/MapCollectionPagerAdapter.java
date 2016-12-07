package htw.bui.openreskit.meter;

import htw.bui.openreskit.domain.organisation.Map;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

public class MapCollectionPagerAdapter extends FragmentStatePagerAdapter{

	private ArrayList<Map> mMap;

	public MapCollectionPagerAdapter(FragmentManager fm, List<Map> maps) 
	{
		super(fm);
		mMap = new ArrayList<Map>(maps);
	}

	@Override
	public Fragment getItem(int i) 
	{
		Fragment fragment = new MapFragment();
		Bundle args = new Bundle();
		args.putInt("MapId", mMap.get(i).getId());
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public int getCount() 
	{
		return mMap.size();
	}

	@Override
	public CharSequence getPageTitle(int position) 
	{
		Map m = mMap.get(position);
		return m.getName();
	}
}
