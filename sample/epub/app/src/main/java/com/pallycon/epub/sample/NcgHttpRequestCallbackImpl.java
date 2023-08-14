package com.pallycon.epub.sample;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.inka.ncg2.Ncg2Agent.HttpRequestCallback;
import com.inka.ncg2.Ncg2Agent.NcgHttpRequestException;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class NcgHttpRequestCallbackImpl implements HttpRequestCallback {

	static public String TAG = "NCG2SdkZipSample";
	static final int HTTP_OK = 200;
	private Context mContext;

	public NcgHttpRequestCallbackImpl(Context context) {
		mContext = context;
	}

	private boolean checkNetwordState() {
		ConnectivityManager connManager =(ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo state_3g = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
		NetworkInfo state_wifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		if( state_3g != null && state_3g.isConnected() ){
			return true;
		}

		if( state_wifi != null && state_wifi.isConnected() ){
			return true;
		}

		return false;
	}

	/**
	 * @brief	sends request to server and receives response data as string.
	 * 
	 * @param   url server URL
	 * @param   param get-type parameter
	 * @return	responsedata
	 */
	@Override
	public String sendRequest(String url, String param)
			throws NcgHttpRequestException {
		Log.d(TAG, "Called!");
		Log.d(TAG, "url : ["+ url +"]");
		if( checkNetwordState() == false ){
			throw new NcgHttpRequestException(0, "", "[sendRequest]Network Not Connected");
		}

		int responseCode = 0;
		String responseMsg = "";
		String fullURL;

		url = url.trim();
		param = param.trim();

		if( param.length() == 0 ) {
			fullURL = url;
		}
		else {
			if( url.indexOf('?') != -1 || param.indexOf('?' ) != -1) { // check whether '?' is included in URL or parameter.
				fullURL = url + param;	
			}else {
				fullURL = url + "?" + param;
			}
		}

		try {
			URL urlObj = new URL(fullURL);
			HttpURLConnection urlConn = (HttpURLConnection) urlObj.openConnection();
			urlConn.setConnectTimeout(5000);
			urlConn.setReadTimeout(5000);
//			urlConn.setRequestProperty("Cookie","company=eagletalk;");

			responseCode = urlConn.getResponseCode();
			responseMsg = urlConn.getResponseMessage();
			if (responseCode != HTTP_OK) {
				throw new NcgHttpRequestException(responseCode,
						responseMsg, "Error. Http response status code is "
								+ responseCode);
			}
			BufferedReader in = new BufferedReader(new InputStreamReader(
					urlConn.getInputStream()));
			StringBuffer buffer = new StringBuffer();
			int c;
			while ((c = in.read()) != -1) {
				buffer.append((char) c);
			}
			in.close();
			String responseData = buffer.toString();
			Log.d(TAG, "[responseData] : " + responseData);
			return responseData;
		} catch (MalformedURLException e) {
			e.printStackTrace();
			throw new NcgHttpRequestException(responseCode, responseMsg,
					"MalformedURLException Exception Occured!: "
							+ e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			throw new NcgHttpRequestException(responseCode, responseMsg,
					"IOException Exception Occured!: " + e.getMessage());
		}
	}

	/**
	 * @brief	sends request to server and receives response data as byte[].
	 * 
	 * @param   url server URL
	 * @param   param get-type parameter to be sent
	 * @return	responsedata
	 */
	@Override
	public byte[] sendRequestResponseBytes(String url, String param)
			throws NcgHttpRequestException {
		Log.d(TAG, "url : ["+ url +"]");
		Log.d(TAG, "param : ["+ param +"]");
		if( checkNetwordState() == false ){
			throw new NcgHttpRequestException(0, "", "[sendRequest]Netword Not Connected");
		}

		int responseCode = 0;
		String responseMsg = "";
		byte[] result;

		url = url.trim();
		param = param.trim();

		HttpURLConnection httpConn;
		int questionMarkIndex = url.indexOf('?');
		if(questionMarkIndex != -1 )  {
			param = url.substring(questionMarkIndex, url.length());
			url = url.substring(0, questionMarkIndex);
		}

		try {
			String fullURL;
			if( param.length() == 0 ) {
				fullURL = url;
			}
			else {
				if( url.indexOf('?') != -1 || param.indexOf('?' ) != -1) { // check whether '?' is included in URL or parameter.
					fullURL = url + param;	
				}else {
					fullURL = url + "?" + param;
				}
			}
			URL urlObj = new URL(fullURL);
			httpConn = (HttpURLConnection) urlObj.openConnection();							
			responseCode = httpConn.getResponseCode();
			responseMsg = httpConn.getResponseMessage();
			if (responseCode != 200) {					
				throw new NcgHttpRequestException(responseCode,
						responseMsg, "Error. Http response status code is "
								+ responseCode);
			}

			BufferedInputStream inputStream = (new BufferedInputStream(httpConn.getInputStream()));
			byte[] responseData = new byte[ 10240000 ];				
			int totalReadBytes = 0;
			while( true  ) {
				int readBytes = inputStream.read( responseData, totalReadBytes, 512 );
				if( readBytes == -1  ) {
					break;
				}
				totalReadBytes += readBytes;
			}
			Log.d("NCG_Agent", "sendRequestResponseBytes : totalReadyBytes -> " + totalReadBytes);
			inputStream.close();

			result = new byte[totalReadBytes];
			System.arraycopy(responseData, 0, result, 0, totalReadBytes);				
			return result;

		} catch (MalformedURLException e) {
			e.printStackTrace();
			throw new NcgHttpRequestException(responseCode, responseMsg,
					"MalformedURLException Exception Occured!: "
							+ e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			throw new NcgHttpRequestException(responseCode, responseMsg,
					"IOException Exception Occured!: " + e.getMessage());
		}
	}

	/**
	 * @brief	sends request to server and receives response data as byte[].
	 * <br> the difference between sendRequestResponseBytes method is that this method has range parameter. 
	 * 
	 * @param   url server URL
	 * @param   param get-type parameter to be sent
	 * @param   begin
	 * @param   end
	 * @return	responsedata
	 */
	@Override
	public byte[] sendRequest(String url, String param, int begin, int end)
			throws NcgHttpRequestException {			
		int responseCode = 0;
		String responseMsg = "";
		InputStream inputStream = null;
		if( checkNetwordState() == false ){
			throw new NcgHttpRequestException(0, "", "[sendRequest]Netword Not Connected");
		}

		url = url.trim();
		param = param.trim();

		Log.d(TAG, "Called!");
		Log.d(TAG, "url : ["+ url +"]");
		Log.d(TAG, "param : ["+ param +"]");

		String fullURL;
		if( param.length() == 0 ) {
			fullURL = url;
		}
		else {
			if( url.indexOf('?') != -1 || param.indexOf('?' ) != -1) { // check whether '?' is included in URL or parameter.
				fullURL = url + param;	
			}else {
				fullURL = url + "?" + param;
			}
		}
		try {
			int totalReadBytes = 0;
			byte[] buffer = new byte[ end ];

			URL urlObj = new URL(fullURL);				
			HttpURLConnection urlConnection = ( HttpURLConnection ) urlObj.openConnection();
			urlConnection.setRequestProperty("Range", String.format("bytes=%d-%d", begin, end) );					
			urlConnection.connect();
			responseCode = urlConnection.getResponseCode();
			responseMsg = urlConnection.getResponseMessage();
			inputStream = urlConnection.getInputStream();

			while(true) {
				int readBytes = inputStream.read(buffer, totalReadBytes, end-totalReadBytes);
				if( readBytes == -1  ) {
					break;
				}			
				totalReadBytes += readBytes;
				if( totalReadBytes >= end  ) {
					break;
				}
			}
			inputStream.close();
			return buffer;
		} catch (MalformedURLException e) {				
			e.printStackTrace();

			throw new NcgHttpRequestException(responseCode, responseMsg,
					"MalformedURLException Exception Occured!: "
							+ e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			throw new NcgHttpRequestException(responseCode, responseMsg,
					"IOException Exception Occured!: " + e.getMessage());
		}
	}
}
