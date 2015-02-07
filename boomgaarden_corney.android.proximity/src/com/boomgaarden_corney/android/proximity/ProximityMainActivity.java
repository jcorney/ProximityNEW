package com.boomgaarden_corney.android.proximity;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class ProximityMainActivity extends Activity implements
		SensorEventListener {

	private final String DEBUG_TAG = "DEBUG_PROXIMITY";
	private final String SERVER_URL = "http://54.86.68.241/proximity/test.php";

	private TextView txtResults;
	private SensorManager sensorManager;

	private String errorMsg;

	private float proximityAccuracy;
	private float proximityValue0;
	private float proximityValue1;
	private float proximityValue2;
	private float proximityMaxRange = 0;
	private float proximityPower = 0;
	private float proximityResolution = 0;
	private int  proximitySensorType;
	private int numProximityChanges = 0;
	private int proximityVersion = 0;
	private int proximityHashCode = 0;
	private Sensor mProximity;
	private long proximityTimeStamp;
	private String proximityVendor;

	private List<NameValuePair> paramsDevice = new ArrayList<NameValuePair>();
	private List<NameValuePair> paramsErrorMsg = new ArrayList<NameValuePair>();
	private List<NameValuePair> paramsProximity = new ArrayList<NameValuePair>();
	private List<NameValuePair> paramsSensor = new ArrayList<NameValuePair>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		txtResults = (TextView) this.findViewById(R.id.txtResults);

		// Setup Proximity Manager and Provider
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		mProximity = sensorManager
				.getDefaultSensor(Sensor.TYPE_PROXIMITY);

		setDeviceData();
		showDeviceData();
		sendDeviceData();

		if (mProximity == null){
			setErrorMsg("No Proximity Detected");
			showErrorMsg();
			sendErrorMsg();
		} else{
			setSensorData();
			showSensorData();
			sendSensorData();
		}

	}

	/* Request Proximity updates at startup */
	@Override
	protected void onResume() {
		super.onResume();
		sensorManager.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL);

	}

	/* Remove the Proximitylistener updates when Activity is paused */
	@Override
	protected void onPause() {
		super.onPause();
		sensorManager.unregisterListener(this);
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (mProximity != null) {
			if ((event.sensor.getType() == mProximity.getType()) && numProximityChanges < 10) {
				
				numProximityChanges++;
				setProximityData(event);
				showProximityData();
				sendProximityData();
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private String buildPostRequest(List<NameValuePair> params)
			throws UnsupportedEncodingException {
		StringBuilder result = new StringBuilder();
		boolean first = true;

		for (NameValuePair pair : params) {
			if (first)
				first = false;
			else
				result.append("&");

			result.append(URLEncoder.encode(pair.getName(), "UTF-8"));
			result.append("=");
			result.append(URLEncoder.encode(pair.getValue(), "UTF-8"));
		}

		return result.toString();
	}

	private String sendHttpRequest(String myURL, String postParameters)
			throws IOException {

		URL url = new URL(myURL);

		// Setup Connection
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setReadTimeout(10000); /* in milliseconds */
		conn.setConnectTimeout(15000); /* in milliseconds */
		conn.setRequestMethod("POST");
		conn.setDoOutput(true);

		// Setup POST query params and write to stream
		OutputStream ostream = conn.getOutputStream();
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
				ostream, "UTF-8"));

		if (postParameters.equals("DEVICE")) {
			writer.write(buildPostRequest(paramsDevice));
		} else if (postParameters.equals("PROXIMITY")) {
			writer.write(buildPostRequest(paramsProximity));
			paramsProximity = new ArrayList<NameValuePair>();
		} else if (postParameters.equals("ERROR_MSG")) {
			writer.write(buildPostRequest(paramsErrorMsg));
			paramsErrorMsg = new ArrayList<NameValuePair>();
		} else if (postParameters.equals("SENSOR")) {
			writer.write(buildPostRequest(paramsSensor));
			paramsSensor = new ArrayList<NameValuePair>();
		}

		writer.flush();
		writer.close();
		ostream.close();

		// Connect and Log response
		conn.connect();
		int response = conn.getResponseCode();
		Log.d(DEBUG_TAG, "The response is: " + response);

		conn.disconnect();
		
		return String.valueOf(response);

	}

	private class SendHttpRequestTask extends AsyncTask<String, Void, String> {

		// @params come from SendHttpRequestTask.execute() call
		@Override
		protected String doInBackground(String... params) {
			// params comes from the execute() call: params[0] is the url,
			// params[1] is type POST
			// request to send - i.e. whether to send Device or Proximity
			// parameters.
			try {
				return sendHttpRequest(params[0], params[1]);
			} catch (IOException e) {
				setErrorMsg("Unable to retrieve web page. URL may be invalid.");
				showErrorMsg();
				return errorMsg;
			}
		}

	}

	private void setDeviceData() {
		paramsDevice.add(new BasicNameValuePair("Device", Build.DEVICE));
		paramsDevice.add(new BasicNameValuePair("Brand", Build.BRAND));
		paramsDevice.add(new BasicNameValuePair("Manufacturer",
				Build.MANUFACTURER));
		paramsDevice.add(new BasicNameValuePair("Model", Build.MODEL));
		paramsDevice.add(new BasicNameValuePair("Product", Build.PRODUCT));
		paramsDevice.add(new BasicNameValuePair("Board", Build.BOARD));
		paramsDevice.add(new BasicNameValuePair("Android API", String
				.valueOf(Build.VERSION.SDK_INT)));
	}

	private void setErrorMsg(String error) {
		errorMsg = error;
		paramsErrorMsg.add(new BasicNameValuePair("Error", errorMsg));
	}

	private void setProximityData(SensorEvent proximity) {
		
		
		proximityAccuracy = proximity.accuracy;
		proximitySensorType = proximity.sensor.getType();
		proximityTimeStamp = proximity.timestamp;
		proximityValue0 = proximity.values[0];
		proximityValue1 = proximity.values[1];
		proximityValue2 = proximity.values[2];
		proximityHashCode = proximity.hashCode();
		
		paramsProximity.add(new BasicNameValuePair("Proximity Update Count",
				String.valueOf(numProximityChanges)));
		paramsProximity.add(new BasicNameValuePair("Accuracy", String
				.valueOf(proximityAccuracy)));
		paramsProximity.add(new BasicNameValuePair("Sensor Type", String
				.valueOf(proximitySensorType)));
		paramsProximity.add(new BasicNameValuePair("Time Stamp", String
				.valueOf(proximityTimeStamp)));
		paramsProximity.add(new BasicNameValuePair(
				"Value 0 proximity minus Gx on the x axis", String
						.valueOf(proximityValue0)));
		paramsProximity.add(new BasicNameValuePair(
				"Value 1 Proximity minus Gy on the y axis", String
						.valueOf(proximityValue1)));
		paramsProximity.add(new BasicNameValuePair(
				"Value 2 Proximity minus Gz on the z axis", String
						.valueOf(proximityValue2)));
		paramsProximity.add(new BasicNameValuePair(
				"Hash Code Value", String
						.valueOf(proximityHashCode)));
	}
	
	private void setSensorData() {
		proximityMaxRange = mProximity.getMaximumRange();
		proximityPower = mProximity.getPower();
		proximityResolution = mProximity.getResolution();
		proximityVendor = mProximity.getVendor();
		proximityVersion = mProximity.getVersion();		
		
		paramsSensor.add(new BasicNameValuePair("Max Range", String
						.valueOf(proximityMaxRange)));
		paramsSensor.add(new BasicNameValuePair("Power", String
				.valueOf(proximityPower)));
		paramsSensor.add(new BasicNameValuePair("Resolution", String
				.valueOf(proximityResolution)));
		paramsSensor.add(new BasicNameValuePair("Vendor", String
				.valueOf(proximityVendor)));
		paramsSensor.add(new BasicNameValuePair("Version", String
				.valueOf(proximityVersion)));
	}


	private void showDeviceData() {
		// Display and store (for sending via HTTP POST query) device
		// information
		txtResults.append("Device: " + Build.DEVICE + "\n");
		txtResults.append("Brand: " + Build.BRAND + "\n");
		txtResults.append("Manufacturer: " + Build.MANUFACTURER + "\n");
		txtResults.append("Model: " + Build.MODEL + "\n");
		txtResults.append("Product: " + Build.PRODUCT + "\n");
		txtResults.append("Board: " + Build.BOARD + "\n");
		txtResults.append("Android API: "
				+ String.valueOf(Build.VERSION.SDK_INT) + "\n");

		txtResults.append("\n");

	}

	private void showErrorMsg() {
		Log.d(DEBUG_TAG, errorMsg);
		txtResults.append(errorMsg + "\n");
	}

	private void showProximityData() {
		StringBuilder results = new StringBuilder();

		results.append("Proximity Update Count: "
				+ String.valueOf(numProximityChanges) + "\n");
		results.append("Proximity Accuracy: " + String.valueOf(proximityAccuracy) + "\n");
		results.append("Proximity Sensor Type: " + String.valueOf(proximitySensorType) + "\n");
		results.append("Proximity Time Stamp: " + String.valueOf(proximityTimeStamp) + "\n");
		results.append("Proximity Vaule 0 (X axis): " + String.valueOf(proximityValue0) + "\n");
		results.append("Proximity Vaule 1 (Y axis): " + String.valueOf(proximityValue1) + "\n");
		results.append("Proximity Vaule 2 (Z axis): " + String.valueOf(proximityValue2) + "\n");
		results.append("Proximity Hash Code " + String.valueOf(proximityHashCode) + "\n");
		
		txtResults.append(new String(results));
		txtResults.append("\n");
	}
	
	private void showSensorData() {
		StringBuilder results = new StringBuilder();
		
		results.append("Max Range: " + String.valueOf(proximityMaxRange) + "\n");
		results.append("Power: " + String.valueOf(proximityPower) + "\n");
		results.append("Resolution: " + String.valueOf(proximityResolution) + "\n");
		results.append("Vendor: " + String.valueOf(proximityVendor) + "\n");
		results.append("Version: " + String.valueOf(proximityVersion) + "\n");
		
		txtResults.append(new String(results));
		txtResults.append("\n");
	}


	private void sendDeviceData() {
		ConnectivityManager connectMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectMgr.getActiveNetworkInfo();

		// Verify network connectivity is working; if not add note to TextView
		// and Logcat file
		if (networkInfo != null && networkInfo.isConnected()) {
			// Send HTTP POST request to server which will include POST
			// parameters with Proximity info
			new SendHttpRequestTask().execute(SERVER_URL, "DEVICE");
		} else {
			setErrorMsg("No Network Connectivity");
			showErrorMsg();
		}
	}

	private void sendErrorMsg() {
		ConnectivityManager connectMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectMgr.getActiveNetworkInfo();

		// Verify network connectivity is working; if not add note to TextView
		// and Logcat file
		if (networkInfo != null && networkInfo.isConnected()) {
			// Send HTTP POST request to server which will include POST
			// parameters with Proximity info
			new SendHttpRequestTask().execute(SERVER_URL, "ERROR_MSG");
		} else {
			setErrorMsg("No Network Connectivity");
			showErrorMsg();
		}
	}

	private void sendProximityData() {
		ConnectivityManager connectMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectMgr.getActiveNetworkInfo();

		// Verify network connectivity is working; if not add note to TextView
		// and Logcat file
		if (networkInfo != null && networkInfo.isConnected()) {
			// Send HTTP POST request to server which will include POST
			// parameters with Proximity info
			new SendHttpRequestTask().execute(SERVER_URL, "PROXIMITY");
		} else {
			setErrorMsg("No Network Connectivity");
			showErrorMsg();
		}
	}
	
	private void sendSensorData() {
		ConnectivityManager connectMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectMgr.getActiveNetworkInfo();

		// Verify network connectivity is working; if not add note to TextView
		// and Logcat file
		if (networkInfo != null && networkInfo.isConnected()) {
			// Send HTTP POST request to server which will include POST
			// parameters with Proximity info
			new SendHttpRequestTask().execute(SERVER_URL, "SENSOR");
		} else {
			setErrorMsg("No Network Connectivity");
			showErrorMsg();
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

}
