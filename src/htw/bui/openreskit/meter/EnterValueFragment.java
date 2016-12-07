package htw.bui.openreskit.meter;

import htw.bui.openreskit.domain.meter.MeterReading;
import htw.bui.openreskit.odata.MeterRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import roboguice.fragment.RoboFragment;
import roboguice.inject.InjectView;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.echo.holographlibrary.Bar;
import com.echo.holographlibrary.BarGraph;

public class EnterValueFragment extends RoboFragment {

	@Inject
	private MeterRepository mRepository;
	
	@InjectView
	(R.id.meterName) TextView mMeterNameView;
	
	@InjectView
	(R.id.meterDescription) TextView mMeterDescriptionView;

	@InjectView
	(R.id.unit) TextView mUnitView;
	
	@InjectView
	(R.id.valueBox) EditText mValueBox;
	
	@InjectView
	(R.id.saveButton) Button mSaveButton;
	
	@InjectView
	(R.id.graph) BarGraph mBarGraph;
	
	private OnValueEnteredListener mListener;
	
	private OnClickListener mButtonListener = new OnClickListener() 
	{
		public void onClick(View v) 
		{
			String responsibleSubjectId = mPrefs.getString("default_responsibleSubject", "none");
			if (responsibleSubjectId != "none") 
			{
				if (mValueBox.getText() != null) {
					double value = Double.valueOf(mValueBox.getText().toString());
					mListener.onValueEntered(mReadingId, value);
				}
				else
				{
					Toast.makeText(mContext, "Bitte geben sie einen Wert ein.", Toast.LENGTH_SHORT).show();
				}
				
			}
			else
			{
				Toast.makeText(mContext, "Bitte wählen Sie zuerst in den Einstellungen einen Mitarbeiter aus.", Toast.LENGTH_SHORT).show();
			}
		}
	};
	
	private long mReadingId;
	private SharedPreferences mPrefs;
	private Context mContext;
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) 
	{
		mContext = getActivity().getApplicationContext();
		mPrefs= PreferenceManager.getDefaultSharedPreferences(mContext);
		// Inflate the layout for this fragment
		return inflater.inflate(R.layout.enter_value_form, container, false);
	}
	
	public void getReadingDetails(long readingId) 
	{
		MeterReading fr = mRepository.getMeterReadingById(readingId);
		mReadingId = readingId;
		SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy");
		
		mMeterNameView.setText(fr.getReadingMeter().getNumber());
		mMeterDescriptionView.setText("Abzulesen bis " + formatter.format(fr.getDueDate().getBegin()) + System.getProperty ("line.separator"));
		mValueBox.setText(String.valueOf(fr.getCounterReading()));
		mUnitView.setText(fr.getReadingMeter().getUnit());
		mSaveButton.setOnClickListener(mButtonListener);
		
		SimpleDateFormat dateFormatter = new SimpleDateFormat("dd.MM.yyyy");
		ArrayList<Bar> points = new ArrayList<Bar>();
		int barCount = 1;
		double tempValue=0;
		List<MeterReading> history = mRepository.getHistoryForMeter(fr.getReadingMeter().getId());
		if (history != null) 
		{
		
			for (MeterReading r : history) 
			{
				if (barCount == 6) 
				{
					break;
				}
				
				if (barCount > 1) 
				{
					float diff = (float)r.getCounterReading()- (float)tempValue;
					Bar d = new Bar();
					d.setColor(Color.parseColor("#0099CC"));
					d.setName(dateFormatter.format(r.getEntryDate().getBegin()));
					d.setValue(diff);
					d.setStringValue(diff + " (" + r.getCounterReading() + ") " + fr.getReadingMeter().getUnit());
					d.setShowStringValue(true);
					if (barCount == history.size()) 
					{
						d.setColor(Color.parseColor("#669900"));
					}
					points.add(d);	
				}
				tempValue = r.getCounterReading();
				barCount++;
			}
			mBarGraph.setBars(points);
			mBarGraph.setUnit("none");
			mBarGraph.update();
		}
	}
	
	//Container Activity must implement this interface
    public interface OnValueEnteredListener 
    {
        public void onValueEntered(long readingId, double value);
    }
    
    //Throw if interface not implemented
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnValueEnteredListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnValueEnteredListener");
        }
    }
}
