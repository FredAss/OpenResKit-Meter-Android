package htw.bui.openreskit.odata;

import htw.bui.openreskit.domain.meter.Meter;
import htw.bui.openreskit.domain.meter.MeterReading;
import htw.bui.openreskit.domain.organisation.Appointment;
import htw.bui.openreskit.domain.organisation.Map;
import htw.bui.openreskit.domain.organisation.ResponsibleSubject;
import htw.bui.openreskit.domain.organisation.Series;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class MeterRepository
{
	private Activity mContext;
	public List<ResponsibleSubject> mResponsibleSubjects;
	public List<MeterReading> mMeterReadings;
	public List<Series> mDistinctSeries;
	public List<Meter> mDistinctMeters;
	public List<Map> mMaps;
	private ProgressDialog mProgressDialog;
	private List<RepositoryChangedListener> mListeners = new ArrayList<RepositoryChangedListener>();
	private SharedPreferences mPrefs;
	private static ObjectMapper objectMapper;
	@Inject
	public MeterRepository(Activity ctx)
	{
		objectMapper = new ObjectMapper();
		mContext = ctx;
		mMeterReadings = getMeterReadingsFromFile();
		mResponsibleSubjects = getResponsibleSubjectsFromFile();
		mDistinctSeries = getDistinctSeries();
		mDistinctMeters = getDistinctMeters();
		mMaps = getMapsFromFile();
		mPrefs = PreferenceManager.getDefaultSharedPreferences(ctx);

	}

	public MeterReading getMeterReadingById(long readingId) 
	{
		for (MeterReading fr : mMeterReadings) 
		{
			if (fr.getId() == readingId) 
			{
				return fr;
			}
		}
		return null;
	}

	public Map getMapById(long mapId) 
	{
		for (Map m : mMaps) 
		{
			if (m.getId() == mapId) 
			{
				return m;
			}
		}
		return null;
	}
	
	public List<Meter> getMetersByMapId(long mapId) 
	{
		List<Meter> meters = new ArrayList<Meter>();
		for (Meter m : mDistinctMeters) 
		{
			if (m.getMapPosition().getMap().getId() == mapId) 
			{
				meters.add(m);
			}
		}
		return meters;
	}

	public MeterReading getLatestMeterReadingForSeriesByBarcode(String barcode, long selectedSeriesId) 
	{

		List<MeterReading> readingsForContainer = new ArrayList<MeterReading>();

		for (MeterReading mr : mMeterReadings) 
		{
			if (mr.getReadingMeter().getBarcode().equals(barcode) && (mr.getRelatedSeries().getId() == selectedSeriesId)) 
			{
				readingsForContainer.add(mr);
			}
		}
		if (!readingsForContainer.isEmpty()) 
		{
			//Collections.sort(readingsForContainer);
			//return readingsForContainer.get(0);
			long diffCache = 0;
			long diff = 0;
			int counter = 0;
			MeterReading resultReading = null;
			Date now = new Date();
			long nowTimestamp = now.getTime();
			for (MeterReading r : readingsForContainer) 
			{
				diff = Math.abs(nowTimestamp-r.getDueDate().getBegin().getTime());
				if (counter == 0) 
				{
					diffCache = diff;
					resultReading = r;
				}
				else 
				{
					if (diff < diffCache) 
					{
						diffCache = diff;
						resultReading = r;
					}
				}
				counter++;
			}
			return resultReading;
		}
		return null;
	}

	public List<MeterReading> getHistoryForMeter(long meterId) 
	{
		List<MeterReading> historyForMeter = new ArrayList<MeterReading>();
		Date now = new Date();
		for (MeterReading mr : mMeterReadings) 
		{
			if ((mr.getReadingMeter().getId() == meterId) && (mr.getEntryDate() != null) && (mr.getEntryDate().getBegin().before(now))) 
			{
				historyForMeter.add(mr);
			}
		}
		if (!historyForMeter.isEmpty()) 
		{
			Collections.sort(historyForMeter, new Comparator<MeterReading>(){
				public int compare(MeterReading one, MeterReading two) {
					if (one.getEntryDate().getBegin().before(two.getEntryDate().getBegin())) 
					{
						return -1;
					}
					else if (one.getEntryDate().getBegin().after(two.getEntryDate().getBegin())) 
					{
						return 1;
					}
					else
					{
						return 0;
					}
				}
			});

			return historyForMeter;
		}
		return null;
	}


	public ResponsibleSubject getResponsibleSubjectById(long responsibleSubjectId) 
	{
		for (ResponsibleSubject rs : mResponsibleSubjects) 
		{
			if (rs.getId() == responsibleSubjectId) {
				return rs;
			}
		}
		return null;
	}

	public List<MeterReading> getMeterReadingsForSeries(long seriesId) 
	{
		List<MeterReading> matchingReadings = new ArrayList<MeterReading>();

		for (MeterReading mr : mMeterReadings) 
		{
			if (mr.getRelatedSeries().getId() == seriesId) 
			{
				matchingReadings.add(mr);
			}
		}
		return matchingReadings;
	}

	private List<Series> getDistinctSeries() 
	{
		List<Series> distinctSeries = new ArrayList<Series>();
		Set<Integer> ids = new HashSet<Integer>();
		
		for (MeterReading mr : mMeterReadings) 
		{
			if (!ids.contains(mr.getRelatedSeries().getId())) 
			{ 
				ids.add(mr.getRelatedSeries().getId());
				distinctSeries.add(mr.getRelatedSeries());
			}
		}
		return distinctSeries;
	}
	
	private List<Meter> getDistinctMeters() 
	{
		List<Meter> distinctMeters = new ArrayList<Meter>();
		Set<Integer> ids = new HashSet<Integer>();
		
		for (MeterReading mr : mMeterReadings) 
		{
			if (!ids.contains(mr.getReadingMeter().getId())) 
			{ 
				ids.add(mr.getReadingMeter().getId());
				distinctMeters.add(mr.getReadingMeter());
			}
		}
		return distinctMeters;
	}

	public void updateValueForReading (long readingId, double value) 
	{
		MeterReading mr = getMeterReadingById(readingId);
		mr.setCounterReading(value);
		if (mr.getEntryDate() != null) 
		{
			mr.getEntryDate().setBegin(new Date());
		}
		else
		{
			Appointment a = new Appointment();
			a.setBegin(new Date());
			mr.setEntryDate(a);
		}
		saveMeterReadingsToFile(mMeterReadings);

		String responsibleSubjectId = mPrefs.getString("default_responsibleSubject", "none");
		long id = Long.parseLong(responsibleSubjectId);
		ResponsibleSubject defaultRs = getResponsibleSubjectById(id);
		mr.setEntryResponsibleSubject(defaultRs);
		mr.setManipulated(true);

		fireRepositoryUpdate();
	}

	public synchronized void addEventListener(RepositoryChangedListener listener)  
	{
		mListeners.add(listener);
	}

	public synchronized void removeEventListener(RepositoryChangedListener listener)   
	{
		mListeners.remove(listener);
	}

	private synchronized void fireRepositoryUpdate() 
	{
		RepositoryChangedEvent event = new RepositoryChangedEvent(this);
		Iterator<RepositoryChangedListener> i = mListeners.iterator();
		while(i.hasNext())  
		{
			((RepositoryChangedListener) i.next()).handleRepositoryChange(event);
		}
	}

	private List<MeterReading> getMeterReadingsFromFile() {

		ArrayList<MeterReading> meterReadings = new ArrayList<MeterReading>();
		String meterReadingsJSON = loadFromExternal("meterReadings.json");

		if (meterReadingsJSON != null) 
		{
			try {
				JSONArray meterReadingsJSONArray = new JSONArray(meterReadingsJSON);

				for (int i = 0; i < meterReadingsJSONArray.length(); i++) 
				{
					JSONObject scheduledTaskJSON = meterReadingsJSONArray.getJSONObject(i);
					MeterReading mr = objectMapper.readValue(scheduledTaskJSON.toString(), MeterReading.class);
					meterReadings.add(mr);

				}
			} 
			catch (Exception e) 
			{
				e.printStackTrace();
			}
		}
		return meterReadings;
	}

	private List<ResponsibleSubject> getResponsibleSubjectsFromFile() {

		ArrayList<ResponsibleSubject> responsibleSubjects = new ArrayList<ResponsibleSubject>();
		String responsibleSubjectsJSON = loadFromExternal("responsibleSubjects.json");

		if (responsibleSubjectsJSON != null) 
		{
			try {
				JSONArray responsibleSubjectsJSONArray = new JSONArray(responsibleSubjectsJSON);

				for (int i = 0; i < responsibleSubjectsJSONArray.length(); i++) 
				{
					JSONObject responsibleSubjectJSON = responsibleSubjectsJSONArray.getJSONObject(i);
					ResponsibleSubject rs = objectMapper.readValue(responsibleSubjectJSON.toString(), ResponsibleSubject.class);
					responsibleSubjects.add(rs);
				}
			} 
			catch (Exception e) 
			{
				e.printStackTrace();
			}
		}
		return responsibleSubjects;
	}

	private List<Map> getMapsFromFile() {

		ArrayList<Map> maps = new ArrayList<Map>();
		String mapsJSON = loadFromExternal("maps.json");

		if (mapsJSON != null) 
		{
			try {
				JSONArray mapsJSONArray = new JSONArray(mapsJSON);

				for (int i = 0; i < mapsJSONArray.length(); i++) 
				{
					JSONObject mapJSON = mapsJSONArray.getJSONObject(i);
					Map m = objectMapper.readValue(mapJSON.toString(), Map.class);
					maps.add(m);

				}
			} 
			catch (Exception e) 
			{
				e.printStackTrace();
			}
		}
		return maps;
	}

	private void saveResponsibleSubjectToFile(List<ResponsibleSubject> responsibleSubjects) 
	{
		saveToExternal(serializeResponsibleSubjects(responsibleSubjects), "responsibleSubjects.json");
	}

	private void saveMeterReadingsToFile(List<MeterReading> meterReadings) 
	{
		saveToExternal(serializeMeterReadings(meterReadings), "meterReadings.json");
	}

	private void saveMapsToFile(List<Map> maps) 
	{
		saveToExternal(serializeMaps(maps), "maps.json");
	}


	public void getDataFromOdataService(Activity _start)
	{
		if (isOnline())
		{
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
			String defaultIP = prefs.getString("default_url", "none");
			String port = prefs.getString("default_port", "none");
			String username = prefs.getString("auth_user", "none");
			String password = prefs.getString("auth_password", "none");
			if (defaultIP == "none" || port == "none" || username == "none" || password == "none") 
			{
				Toast.makeText(mContext, "Bitte geben sie in den Einstellungen zuerst die Verbingungsparamenter an", Toast.LENGTH_SHORT).show();
			}
			else
			{
				new GetData().execute((Void[]) null);	
			}
		} 
		else
		{
			Toast.makeText(mContext, "Keine Verbindung!", Toast.LENGTH_SHORT).show();
		}

	}

	public void writeDataToOdataService(Activity _start)
	{
		if (isOnline())
		{
			new WriteData().execute((Void[]) null);
		} 
		else
		{
			Toast.makeText(mContext, "Keine Verbindung!", Toast.LENGTH_SHORT).show();
		}

	}
	private boolean isOnline()
	{
		ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		if (netInfo != null && netInfo.isConnectedOrConnecting())
		{
			return true;
		}
		return false;
	}

	private class GetData extends AsyncTask<Void, Void, Integer>
	{
		protected Integer doInBackground(Void... params)
		{
			mMeterReadings = new ArrayList<MeterReading>();
			mResponsibleSubjects = new ArrayList<ResponsibleSubject>();
			mMaps = new ArrayList<Map>();

			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
			String defaultIP = prefs.getString("default_url", "none");
			String port = prefs.getString("default_port", "none");
			String username = prefs.getString("auth_user", "none");
			String password = prefs.getString("auth_password", "none");


			int counter = 0;
			try 
			{
				//MeterReadings
				String expandMeterReadings = "RelatedSeries,RelatedSeries/SeriesColor,AppointmentResponsibleSubject,DueDate,EntryDate,OpenResKit.DomainModel.MeterReading/ReadingMeter/MapPosition/Map";
				JSONArray scheduledTaskJSONArray = getJSONArrayFromOdata(defaultIP, port, username, password, "OpenResKitHub", "ScheduledTasks/OpenResKit.DomainModel.MeterReading", expandMeterReadings, null);
				for (int i = 0; i < scheduledTaskJSONArray.length(); i++) 
				{
					JSONObject scheduledTaskJSON = scheduledTaskJSONArray.getJSONObject(i);
					MeterReading mr = objectMapper.readValue(scheduledTaskJSON.toString(), MeterReading.class);
					mMeterReadings.add(mr);
					counter++;

				}
			} 
			catch (final Exception e) 
			{
				mContext.runOnUiThread(new Runnable() {

					public void run() {
						Toast.makeText(mContext, "Es ist ein Fehler aufgetreten. " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();

					}
				});
				e.printStackTrace();
			}

			try 
			{
				//ResponsibleSubjects
				String expandResponsibleSubjects = "OpenResKit.DomainModel.Employee/Groups";
				JSONArray responsibleSubjectsJSONArray = getJSONArrayFromOdata(defaultIP, port, username, password, "OpenResKitHub", "ResponsibleSubjects", expandResponsibleSubjects, null);
				for (int i = 0; i < responsibleSubjectsJSONArray.length(); i++) 
				{
					JSONObject responsibleSubjectsJSON = responsibleSubjectsJSONArray.getJSONObject(i);
					ResponsibleSubject rs = objectMapper.readValue(responsibleSubjectsJSON.toString(), ResponsibleSubject.class);
					mResponsibleSubjects.add(rs);

				}
			} 
			catch (Exception e) 
			{
				e.printStackTrace();
			}

			try 
			{
				//Maps
				String expandMaps = "MapSource";
				JSONArray mapsJSONArray = getJSONArrayFromOdata(defaultIP, port, username, password, "OpenResKitHub", "Maps", expandMaps, null);
				for (int i = 0; i < mapsJSONArray.length(); i++) 
				{
					JSONObject mapsJSON = mapsJSONArray.getJSONObject(i);
					Map m = objectMapper.readValue(mapsJSON.toString(), Map.class);
					mMaps.add(m);

				}
			} 
			catch (Exception e) 
			{
				e.printStackTrace();
			}

			return counter;

		}

		protected void onPreExecute()
		{
			super.onPreExecute();
			mProgressDialog = new ProgressDialog(mContext);
			mProgressDialog.setMessage("Aktualisiere Daten");
			mProgressDialog.show();
		}

		@Override
		protected void onPostExecute(Integer result)
		{
			saveMeterReadingsToFile(mMeterReadings);
			saveResponsibleSubjectToFile(mResponsibleSubjects);
			saveMapsToFile(mMaps);
			mDistinctSeries = getDistinctSeries();
			mDistinctMeters = getDistinctMeters();
			mProgressDialog.dismiss();
			Toast.makeText(mContext, "Es wurden " + result + " Datensätze vom Server geladen.", Toast.LENGTH_SHORT).show();
			fireRepositoryUpdate();

		}
	}

	private class WriteData extends AsyncTask<Void, Void, Integer>
	{
		protected Integer doInBackground(Void... params)
		{
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
			String defaultIP = prefs.getString("default_url", "none");
			String port = prefs.getString("default_port", "none");
			String username = prefs.getString("auth_user", "none");
			String password = prefs.getString("auth_password", "none");

			int counter = 0;
			for (MeterReading mr : mMeterReadings) 
			{
				if (mr.isManipulated()) 
				{
					try {
						writeChangesInReadingToOdata(defaultIP, port, username, password, "OpenResKitHub", "ScheduledTasks", mr);
					} catch (final Exception e) {
						mContext.runOnUiThread(new Runnable() {

							public void run() {
								Toast.makeText(mContext, "Es ist ein Fehler aufgetreten. " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
							}
						});
						e.printStackTrace();
					}
					mr.setManipulated(false);
					counter++;
				}
			}
			return counter;

		}

		protected void onPreExecute()
		{
			super.onPreExecute();
			mProgressDialog = new ProgressDialog(mContext);
			mProgressDialog.setMessage("Schreibe Daten");
			mProgressDialog.show();
		}

		@Override
		protected void onPostExecute(Integer result)
		{
			mProgressDialog.dismiss();
			Toast.makeText(mContext, "Es wurden " + result + "Ablesungen zum Server übermittelt!", Toast.LENGTH_SHORT).show();
		}
	}

	private static String serializeMeterReadings(List<MeterReading> meterReadings) 
	{
		String str = null;
		try 
		{
			str = objectMapper.writerWithType(new TypeReference<List<MeterReading>>(){}).writeValueAsString(meterReadings);
		} 
		catch (JsonProcessingException e) 
		{
			e.printStackTrace();
		}
		return str;
	}

	private static String serializeResponsibleSubjects(List<ResponsibleSubject> responsibleSubjects) 
	{
		String str = null;
		try 
		{
			str = objectMapper.writerWithType(new TypeReference<List<ResponsibleSubject>>(){}).writeValueAsString(responsibleSubjects);
		} 
		catch (JsonProcessingException e) 
		{
			e.printStackTrace();
		}
		return str;
	}

	private static String serializeMaps(List<Map> maps) 
	{
		String str = null;
		try 
		{
			str = objectMapper.writerWithType(new TypeReference<List<Map>>(){}).writeValueAsString(maps);
		} 
		catch (JsonProcessingException e) 
		{
			e.printStackTrace();
		}
		return str;
	}

	private static JSONArray getJSONArrayFromOdata(String ip, String port, String username, String password, String endpoint, String collection, String expand, String filter) throws Exception 
	{
		JSONArray returnJSONArray = null;
		String jsonText = null;
		String uriString = null;
		try {
			HttpParams httpParams = new BasicHttpParams();
			HttpProtocolParams.setVersion(httpParams, HttpVersion.HTTP_1_1);
			HttpProtocolParams.setContentCharset(httpParams, HTTP.UTF_8);
			httpParams.setBooleanParameter("http.protocol.expect-continue", false);
			HttpConnectionParams.setConnectionTimeout(httpParams, 10000);
			if (filter == null) 
			{
				if (expand == null) 
				{
					uriString = "http://"+ip+":"+ port +"/" + endpoint +"/"+ collection +"/?$format=json";
				}
				else 
				{
					uriString = "http://"+ip+":"+ port +"/" + endpoint +"/"+ collection +"/?$format=json&$expand=" + expand;
				}
			} 
			else
			{
				if (expand == null) 
				{
					uriString = "http://"+ ip +":"+ port +"/" + endpoint +"/"+ collection +"/?$format=json&$filter="+ filter;
				}
				else
				{
					uriString = "http://"+ ip +":"+ port +"/" + endpoint +"/"+ collection +"/?$format=json&$expand=" + expand + "&$filter="+ filter;
				}
			}
			HttpGet request = new HttpGet(uriString);
			request.setHeader("Accept", "application/json");
			request.setHeader("Content-type", "application/json");
			request.addHeader(BasicScheme.authenticate(new UsernamePasswordCredentials(username, password), "UTF-8", false));
			HttpClient httpClient = new DefaultHttpClient(httpParams);

			HttpResponse response = httpClient.execute(request);
			if(response.getStatusLine().getStatusCode() == 200){
				HttpEntity entity = response.getEntity();
				if (entity != null) {
					InputStream instream = entity.getContent();
					jsonText = convertStreamToString(instream);
					instream.close();
					returnJSONArray  = new JSONObject(jsonText).getJSONArray("value");
				}
			}
			else if (response.getStatusLine().getStatusCode() == 403) 
			{
				Exception e1 = new AuthenticationException("Der Benutzername oder das Passwort für die Authentifizierung am OData Service sind nicht korrekt");
				throw e1; 
			}

		}
		catch (Exception e) 
		{
			throw e;
		}
		return returnJSONArray;
	}

	private static void writeChangesInReadingToOdata(String ip, String port, String username, String password, String endpoint, String collection, MeterReading meterReading) throws Exception 
	{
		try 
		{
			HttpResponse response;
			HttpParams httpParams = new BasicHttpParams();
			HttpProtocolParams.setVersion(httpParams, HttpVersion.HTTP_1_1);
			HttpProtocolParams.setContentCharset(httpParams, HTTP.UTF_8);
			httpParams.setBooleanParameter("http.protocol.expect-continue", false);
			DefaultHttpClient httpClient = new DefaultHttpClient(httpParams);

			final DateFormat dfs = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
			Date now = new Date();

			ObjectMapper mapper = new ObjectMapper();
			HttpPost dateRequest = null;
			if (meterReading.getEntryDate().getId() > 0) 
			{				
				dateRequest = new HttpPost("http://" + ip + ":" + port + "/" + endpoint + "/" + "Appointments(" + meterReading.getEntryDate().getId() +")");
				dateRequest.setHeader("X-HTTP-Method", "MERGE");
				dateRequest.setHeader("Accept", "application/json");
				dateRequest.setHeader("Content-type", "application/json;odata=verbose");
				dateRequest.addHeader(BasicScheme.authenticate(new UsernamePasswordCredentials(username, password), "UTF-8", false));


				String payloadDateJSON = "{\"odata.type\":\"OpenResKit.DomainModel.Appointment\"," +
						"\"Id\":"+ meterReading.getEntryDate().getId()+"," +	
						"\"Begin\":\"" + dfs.format(now) + "\"," +
						"\"End\":\"" + dfs.format(now) + "\"}";

				dateRequest.setEntity(new StringEntity(payloadDateJSON,HTTP.UTF_8));

			}
			else 
			{
				dateRequest = new HttpPost("http://" + ip + ":" + port + "/" + endpoint + "/" + "Appointments");
				dateRequest.setHeader("X-HTTP-Method-Override", "PUT");
				dateRequest.setHeader("Accept", "application/json");
				dateRequest.setHeader("Content-type", "application/json;odata=verbose");
				dateRequest.addHeader(BasicScheme.authenticate(new UsernamePasswordCredentials(username, password), "UTF-8", false));

				String payloadDateJSON = "{\"odata.type\":\"OpenResKit.DomainModel.Appointment\"," +
						"\"Begin\":\"" + dfs.format(now) + "\"," +
						"\"End\":\"" + dfs.format(now) + "\"," +
						"\"IsAllDay\":\"false\"}"; 

				dateRequest.setEntity(new StringEntity(payloadDateJSON,HTTP.UTF_8));


			}

			response = httpClient.execute(dateRequest);
			HttpEntity dateResponseEntity = response.getEntity();
			Appointment newAppointment = null;

			if(dateResponseEntity != null) 
			{
				char[] buffer = new char[(int)dateResponseEntity.getContentLength()];
				InputStream stream = dateResponseEntity.getContent();
				InputStreamReader reader = new InputStreamReader(stream);
				reader.read(buffer);
				stream.close();

				JSONObject answer = new JSONObject(new String(buffer));
				System.out.print(answer);
				newAppointment = mapper.readValue(answer.toString(), Appointment.class);	    	
			}

			int appointmentId;
			if (meterReading.getEntryDate().getId() > 0) 
			{
				appointmentId = meterReading.getEntryDate().getId();
			}
			else
			{
				appointmentId = newAppointment.getId();
			}

			String payloadJSON = "{\"odata.type\":\"OpenResKit.DomainModel.MeterReading\"," +
					"\"Id\":"+ meterReading.getId()+"," +
					"\"EntryResponsibleSubject\":{\"__metadata\":{\"uri\": \"http://" + ip + ":" + port + "/" + endpoint + "/ResponsibleSubjects("+ meterReading.getEntryResponsibleSubject().getId() +")\"}},"+ 
					"\"EntryDate\":{\"__metadata\":{\"uri\": \"http://" + ip + ":" + port + "/" + endpoint + "/Appointments("+ appointmentId +")\"}},"+
					"\"Progress\":\"1\","+
					"\"CounterReading\":" + meterReading.getCounterReading() + "}";

			StringEntity stringEntity = new StringEntity(payloadJSON,HTTP.UTF_8);
			stringEntity.setContentType("application/json");

			HttpPost request = new HttpPost("http://" + ip + ":" + port + "/" + endpoint + "/" + collection +"("+ meterReading.getId()+")/OpenResKit.DomainModel.MeterReading/");
			request.setHeader("X-HTTP-Method", "MERGE");
			request.setHeader("Accept", "application/json");
			request.setHeader("Content-type", "application/json;odata=verbose");
			request.addHeader(BasicScheme.authenticate(new UsernamePasswordCredentials(username, password), "UTF-8", false));
			request.setEntity(stringEntity);

			response = httpClient.execute(request);
			HttpEntity responseEntity = response.getEntity();
			if(responseEntity != null) 
			{
				char[] buffer = new char[(int)responseEntity.getContentLength()];
				InputStream stream = responseEntity.getContent();
				InputStreamReader reader = new InputStreamReader(stream);
				reader.read(buffer);
				stream.close();

				JSONObject answer = new JSONObject(new String(buffer));
				System.out.print(answer);
			}
		}
		catch (Exception e)
		{
			throw e;
		}
	}

	public void deleteLocalData()
	{
		mMeterReadings = new ArrayList<MeterReading>();
		saveMeterReadingsToFile(mMeterReadings);
		mResponsibleSubjects = new ArrayList<ResponsibleSubject>();
		saveResponsibleSubjectToFile(mResponsibleSubjects);
		mMaps = new ArrayList<Map>();
		saveMapsToFile(mMaps);
		mDistinctSeries = new ArrayList<Series>();
		mDistinctMeters = new ArrayList<Meter>();

		fireRepositoryUpdate();
	}

	private void saveToExternal(String content, String fileName) {
		FileOutputStream fos = null;
		Writer out = null;
		try {
			File file = new File(getAppRootDir(), fileName);
			fos = new FileOutputStream(file);
			out = new OutputStreamWriter(fos, "UTF-8");

			out.write(content);
			out.flush();
		} catch (Throwable e){
			e.printStackTrace();
		} finally {
			if(fos!=null){
				try {
					fos.close();
				} catch (IOException ignored) {}
			}
			if(out!= null){
				try {
					out.close();
				} catch (IOException ignored) {}
			}
		}
	}

	private String loadFromExternal(String fileName) {
		String res = null;
		File file = new File(getAppRootDir(), fileName);
		if(!file.exists()){
			Log.e("", "file " +file.getAbsolutePath()+ " not found");
			return null;
		}
		FileInputStream fis = null;
		BufferedReader inputReader = null;
		try {
			fis = new FileInputStream(file);
			inputReader = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
			StringBuilder strBuilder = new StringBuilder();
			String line;
			while ((line = inputReader.readLine()) != null) {
				strBuilder.append(line + "\n");
			}
			res = strBuilder.toString();
		} catch(Throwable e){
			if(fis!=null){
				try {
					fis.close();
				} catch (IOException ignored) {}
			}
			if(inputReader!= null){
				try {
					inputReader.close();
				} catch (IOException ignored) {}
			}
		}
		return res;
	}

	public File getAppRootDir() {
		File appRootDir;
		boolean externalStorageAvailable;
		boolean externalStorageWriteable;
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			externalStorageAvailable = externalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			externalStorageAvailable = true;
			externalStorageWriteable = false;
		} else {
			externalStorageAvailable = externalStorageWriteable = false;
		}
		if (externalStorageAvailable && externalStorageWriteable) {

			appRootDir = mContext.getExternalFilesDir(null);
		} else {
			appRootDir = mContext.getDir("appRootDir", Context.MODE_PRIVATE);
		}
		if (!appRootDir.exists()) {
			appRootDir.mkdir();
		}
		return appRootDir;
	}

	private static String convertStreamToString(InputStream is) {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		StringBuilder sb = new StringBuilder();

		String line;
		try {
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return sb.toString();
	}
}
