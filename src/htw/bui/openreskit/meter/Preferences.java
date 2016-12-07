package htw.bui.openreskit.meter;


import htw.bui.openreskit.domain.organisation.Employee;
import htw.bui.openreskit.domain.organisation.EmployeeGroup;
import htw.bui.openreskit.domain.organisation.ResponsibleSubject;
import htw.bui.openreskit.odata.MeterRepository;

import java.util.ArrayList;
import java.util.List;

import roboguice.activity.RoboPreferenceActivity;
import android.app.ActionBar;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;

import com.google.inject.Inject;

public class Preferences extends RoboPreferenceActivity {

	@Inject 
	private MeterRepository mWasteRepository;
	private CharSequence[] mEmployee_names;
	private CharSequence[] mEmployee_ids;
		
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ActionBar actionBar = getActionBar();
		actionBar.hide();
		int themeId = this.getIntent().getExtras().getInt("theme");
        this.setTheme(themeId);		
        
        //get Employees
		List<String> emp_names = new ArrayList<String>();
		List<String> emp_ids = new ArrayList<String>();

		if (mWasteRepository.mResponsibleSubjects != null) {
			
			for (ResponsibleSubject rs : mWasteRepository.mResponsibleSubjects) 
			{
				if (rs.getClass().getName() == Employee.class.getName()) 
				{
					Employee e = (Employee) rs;
					emp_names.add(e.getFirstName() + ", " + e.getLastName());
					emp_ids.add(String.valueOf(e.getId()));
				} 
				else if (rs.getClass().getName() == EmployeeGroup.class.getName()) 
				{
					EmployeeGroup g = (EmployeeGroup) rs;
					emp_names.add(g.getName() + " (Gruppe)");
					emp_ids.add(String.valueOf(g.getId()));
				}
			}
			   
			mEmployee_names = emp_names.toArray(new CharSequence[emp_names.size()]);
			mEmployee_ids = emp_ids.toArray(new CharSequence[emp_ids.size()]);
		}
		setPreferenceScreen(createPreferenceHierarchy());
	}
	
	private PreferenceScreen createPreferenceHierarchy() {
		  
		// Root
		@SuppressWarnings("deprecation")
		PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);
	    PreferenceCategory dialogBasedPrefCat = new PreferenceCategory(getApplicationContext());
	    dialogBasedPrefCat.setTitle(R.string.pref_cat_settings);
	    root.addPreference(dialogBasedPrefCat); //Adding a category
    	    
	    // List preferences under the category
	    //default employee
	    ListPreference listPref = new UpdatingListPref(this);
	    listPref.setKey("default_responsibleSubject"); //Refer to get the pref value
	    listPref.setEntries(mEmployee_names);
	    listPref.setEntryValues(mEmployee_ids);
	    listPref.setDialogTitle(R.string.pref_default_employee_header); 
	    listPref.setTitle(R.string.pref_default_employee_header);
	    listPref.setSummary("%s");
	    dialogBasedPrefCat.addPreference(listPref); 

	    //server
	    EditTextPreference urlPref = new EditTextPreference(this);
	    urlPref.setKey("default_url");
	    EditTextPreference portPref = new EditTextPreference(this);
	    portPref.setKey("default_port");
	    EditTextPreference userPref = new EditTextPreference(this);
	    userPref.setKey("auth_user");
	    EditTextPreference passwordPref = new EditTextPreference(this);
	    passwordPref.setKey("auth_password");
	    	    
	    String serviceURI = "141.45.165.40";
	    String servicePort = "7000";
	    String serviceUser = "root";
	    String servicePass = "ork123";
	    
	    urlPref.setText(serviceURI);
	    urlPref.setDialogTitle(R.string.pref_url_header);
	    urlPref.setTitle(R.string.pref_url_header);
	    urlPref.setSummary(R.string.pref_url_summary);
	    dialogBasedPrefCat.addPreference(urlPref);
	    
	    portPref.setText(servicePort);
	    portPref.setDialogTitle(R.string.pref_port_header);
	    portPref.setTitle(R.string.pref_port_header);
	    portPref.setSummary(R.string.pref_port_summary);
	    dialogBasedPrefCat.addPreference(portPref);
	    
	    userPref.setText(serviceUser);
	    userPref.setDialogTitle(R.string.pref_user_header);
	    userPref.setTitle(R.string.pref_user_header);
	    userPref.setSummary(R.string.pref_user_summary);
	    dialogBasedPrefCat.addPreference(userPref);
	    
	    passwordPref.setText(servicePass);
	    passwordPref.setDialogTitle(R.string.pref_pass_header);
	    passwordPref.setTitle(R.string.pref_pass_header);
	    passwordPref.setSummary(R.string.pref_pass_summary);
	    dialogBasedPrefCat.addPreference(passwordPref);
	    
	    
	    return root;
	}	

}
