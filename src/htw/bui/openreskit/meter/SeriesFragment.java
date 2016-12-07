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

import htw.bui.openreskit.odata.MeterRepository;
import htw.bui.openreskit.odata.RepositoryChangedListener;

import java.util.EventObject;

import roboguice.fragment.RoboFragment;
import roboguice.inject.InjectView;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.google.inject.Inject;

public class SeriesFragment extends RoboFragment {
    
    
	@Inject
    private MeterRepository mRepository;
	
	@InjectView
	(R.id.seriesListView) ListView mListView;
	
	
	private Parcelable mListState;
    private Activity mContext;
    OnSeriesSelectedListener mListener;
    boolean mDualPane;
    int mListPosition = 0;
    
	private RepositoryChangedListener mRepositoryChangedListener = new RepositoryChangedListener() 
	{
		public void handleRepositoryChange(EventObject e) 
		{
			populateSeries();
		}
	};
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) 
    {
        super.onActivityCreated(savedInstanceState);
        mContext = getActivity();
        
        mRepository.addEventListener(mRepositoryChangedListener);
        
        if (savedInstanceState != null) 
        {
        	mListState = savedInstanceState.getParcelable("listState");
        	mListPosition = savedInstanceState.getInt("listPosition");
        }
        mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mListView.setCacheColorHint(Color.TRANSPARENT);
        mListView.setOnItemClickListener(mListItemClickListener);
        
        TextView emptyView = new TextView(mContext);
        emptyView.setText("Es sind keine Daten vorhanden");
        mListView.setEmptyView(emptyView);

        populateSeries();
    }

	@Override
	public void onResume() {
	    super.onResume();
	    if(mListState!=null){
	        mListView.onRestoreInstanceState(mListState);
	    } 
	    mListState = null;
	 
	}
    
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) 
    {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.series_list, container, false);
    }
    
    @Override
    public void onSaveInstanceState (Bundle outState) 
    {
        super.onSaveInstanceState(outState);
        Parcelable state = mListView.onSaveInstanceState();
        outState.putParcelable("listState", state);
        int listPosition = mListView.getSelectedItemPosition();
        outState.putInt("listPosition", listPosition);
    }
 
    public void populateSeries() 
    {
    	SeriesAdapter adapter = new SeriesAdapter(mContext, R.layout.series_list_row, mRepository.mDistinctSeries);
    	mListView.setAdapter(adapter);
    }

    public long getSelectedSeriesId() 
    {
    	return mListView.getItemIdAtPosition(mListPosition);
    }
    
    private OnItemClickListener mListItemClickListener = new OnItemClickListener() 
    {
		public void onItemClick(AdapterView<?> arg0, View view, int position, long id) 
		{
			mListener.onSeriesSelected(id);
		}
	};

    // Container Activity must implement this interface
    public interface OnSeriesSelectedListener 
    {
        public void onSeriesSelected(long id);
    }
    
    //Throw if interface not implemented
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnSeriesSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnSeriesSelectedListener");
        }
    }
	

}