package com.inka.ncgsample;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.IllegalFormatConversionException;
import java.util.IllegalFormatException;
import java.util.UUID;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.inka.ncg2.Ncg2Agent;
import com.inka.ncg2.Ncg2SdkFactory;


public class DemoLibrary {
	
	static public String TAG = "NCG2SdkSample";	
	
	private static final long SIZE_KB = 1024;
	private static final long SIZE_MB = (1024 * SIZE_KB);
	private static final long SIZE_GB = (1024 * SIZE_MB);
	
	
	static Ncg2Agent g_ncgAgent = Ncg2SdkFactory.getNcgAgentInstance();
	
	/**
	 * @brief   returns NCG2 Core object. <br>NCG2 Core is a singleton, so only one instance will be created while the application is running
	 * @return  NCG2 Core object
	 */
	final static public Ncg2Agent getNcgAgent() {		
		return g_ncgAgent;
	}

	/**
	 * @brief	returns OrderID
	 * <br>It returns blank string in this sample app.
	 * @return  orderID
	 */
	final static public String getOrderID() {
		return "";
	}
	
	/**
	 * @brief	returns unique device ID by UUID format string 
	 * <br>Order of getting unique ID : Phone Module ID -> Wifi module ID -> ANDROID_ID -> random-generated ID
	 * @param   context
	 * @return	 unique device ID string
	 * @throws UnsupportedEncodingException
	 */
	final static public String getDeviceID( Context context )  {
		
		final String deviceIdKey = "DeviceID";
		SharedPreferences prefs = context.getSharedPreferences("Ncg2PlayerDemo", 0);
		String savedDeviceID = prefs.getString(deviceIdKey, null);
		if( savedDeviceID != null ) {
			return savedDeviceID;
		}
		
		synchronized ( DemoLibrary.class ) {
			try {
				String strDeviceID;
				strDeviceID = getTelephonyDeviceID( context );
				if( strDeviceID == null ) {
					strDeviceID = getWifiDeviceID( context );
					if( strDeviceID == null ) {
						strDeviceID = getAndroidID( context );
						if( strDeviceID == null ) {
							strDeviceID = UUID.randomUUID().toString();
						}
					}
				}
				prefs.edit().putString(deviceIdKey, strDeviceID).commit();
				return strDeviceID;
			} catch (Exception e) {
				e.printStackTrace();
				return "Unknown_DeviceID";
			}
		}
	}
	
	/**
	 * @brief	get phone module's device ID and return it as UUID format string 
	 * 
	 * @param   context
	 * @return	phone module ID in UUID string, returns null on error
	 * @throws UnsupportedEncodingException
	 */
	final static public String getTelephonyDeviceID( Context context ) throws UnsupportedEncodingException {
		@SuppressLint("MissingPermission")
		final String deviceId = (( TelephonyManager ) context.getSystemService( Context.TELEPHONY_SERVICE )).getDeviceId();
		if( deviceId != null ) {
			return UUID.nameUUIDFromBytes(deviceId.getBytes("utf8")).toString();
		}
		return null;
	}
	
	/**
	 * @brief	get WIFI module's device ID and returns it as UUID format string 
	 * 
	 * @param   context
	 * @return	WIFI module ID in UUID string, returns null on error
	 * @throws UnsupportedEncodingException
	 */
	final static public String getWifiDeviceID( Context context ) throws UnsupportedEncodingException {
		WifiManager	wifiMan = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		if( wifiMan != null ) {
			WifiInfo	wifiInf = wifiMan.getConnectionInfo();
			StringBuilder strMacAddress	= new StringBuilder();
			strMacAddress.append(wifiInf.getMacAddress());
			try {
				strMacAddress.deleteCharAt(2);
				strMacAddress.deleteCharAt(4);
				strMacAddress.deleteCharAt(6);
				strMacAddress.deleteCharAt(8);
				strMacAddress.deleteCharAt(10);
			} catch( Exception e ) {
				e.printStackTrace();
			}
			
			return UUID.nameUUIDFromBytes(strMacAddress.toString().getBytes("utf8")).toString();
		}
		
		return null;
	}
	
	/**
	 * @brief	get ANDROID_ID value provided by Android OS and returns it as UUID format string 
	 * 
	 * @param   context
	 * @return	ANDROID_ID in UUID string, returns null on error
	 * @throws UnsupportedEncodingException
	 */
	final static public String getAndroidID( Context context ) throws UnsupportedEncodingException {
		final String androidId = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
		if( androidId == null ) return null;
		
		if (!"9774d56d682e549c".equals(androidId)) {   
			return UUID.nameUUIDFromBytes(androidId.getBytes("utf8")).toString();   
		}
		
		return null;
	}
	
	
	/**
	 * @brief	check if the file exists on the path
	 * 
	 * @param   filePath path of file
	 * @return	true: exist, false: not exist
	 *
	 */
	public final static boolean checkIfFileExists(String filePath) {
	    boolean ret = false;
		if (filePath != null) {
	        File file = new File (filePath);
	        ret = file.exists();
	        file = null;
	    }
	    return ret;
	} 
	
	/**
	 * @brief	make new folder in path
	 * 
	 * @param   path
	 * @return	void
	 *
	 */
	@SuppressLint("WorldWriteableFiles")
	final static public void mkdir( String path ) {
		File newPath = new File( path );
		if(! newPath.isDirectory()) {
			newPath.mkdirs();
		}
		newPath = null;
	}
	
	
	final static public String getFilenameFromFilePath( String filePath ) {
		filePath = filePath.replace( '\\', '/' );
		String[] token = filePath.split( "/" );
		if( token == null ){
			return null;
		}	
		if( token.length < 1 ){
			return filePath;
		}	
		
		String ret = token[token.length - 1];
		return ret;
	}

	
	
	/**
	 * @brief send parameter to the URL and receive response data
	 * 
	 * @param httpClient
	 * @param url
	 * @param params
	 * @param values
	 * @return reponseData
	 */
	static final public String getUrlResult( HttpClient httpClient, String url, String [] params, String [] values ) {
		
		int nParamLen = Math.min( params.length, values.length );
		for( int i = 0; i < nParamLen; i++ ) {
			if( i == 0 ) {
				url = url + "?" + params[i] + "=" + values[i];
			} else {
				url = url + "&" + params[i] + "=" + values[i];
			}
		}
		
		Log.d(TAG, url);
		HttpGet method = new HttpGet(url);
		HttpResponse response = null;
		BasicResponseHandler myHandler = new BasicResponseHandler();
		String endResult = null;
		// set timeout (5 seconds)
		HttpParams httpParams = httpClient.getParams();
		HttpConnectionParams.setConnectionTimeout(httpParams, 5000);
		HttpConnectionParams.setSoTimeout(httpParams, 5000);
		try {
			response = httpClient.execute(method);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		try {
			endResult = myHandler.handleResponse(response);
		} catch (HttpResponseException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		return endResult;
	}
	
	
	final static public String getFileSizeString(long nSize) throws IllegalFormatConversionException {
		String fileSize = "";

		try {
			if(nSize <= 0)
				fileSize = String.format("0");
			else if(nSize > SIZE_GB)
				fileSize = String.format("%.2f GB", (float)nSize/(float)SIZE_GB);
			else if(nSize > SIZE_MB)
				fileSize = String.format("%.2f MB", (float)nSize/(float)SIZE_MB);
			else if(nSize > SIZE_KB)
				fileSize = String.format("%.2f KB", (float)nSize/(float)SIZE_KB);
			else 
				fileSize = String.format("%d byte", nSize);
		} catch( IllegalFormatException e1 ) {
			e1.printStackTrace();
		} catch( NullPointerException e2 ) {
			e2.printStackTrace();
		}
		
		return fileSize;
	}
	

	private static final String ALLOW_URI = ":/._?&=";
	/**
	 * @brief	convert Url to UTF-8 string
	 * 
	 * @param   strInput Url
	 * @return	converted Url
	 */
	final static public	String	safeUrlEncoder(String strInput){
		String	strResult = Uri.encode(strInput, ALLOW_URI);
		String	strLower = strResult.toLowerCase();
		if( strLower.startsWith("http://") || strLower.startsWith("https://") ) {
			return	strResult;
		} else {
			return	"http://" + strResult;
		}
	}
	
	
	/**
	 * @brief	returns path of external storage in Android device
	 *
	 * @return	path of external storage
	 *
	 */
	final static public String getBasePath(Context context) {
		String path = null;
		File file = null;
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
			file = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
			path = file.getAbsolutePath();
		} else {
			path = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
		}
		
		return path;
	}
	
	/**
	 * @brief	returns path of temp files for download
	 * 
	 * @param   filename 
	 * @return	external storage path + /ncg/download/ + filename
	 *
	 */
	final static public String getDonwloadPath(Context context, String filename ) {
		String filepath = getBasePath(context) + "/ncg/download";
		if( !DemoLibrary.checkIfFileExists( filepath )) {
			DemoLibrary.mkdir( filepath );
		}
		return filepath + "/" + DemoLibrary.getFilenameFromFilePath( filename );
	}
	
	
	/**
	 * @brief	returns full path of file
	 * 
	 * @param   filename 
	 * @return	external storage path + /ncg/ + filename
	 *
	 */
	final static public String getItemPath(Context context, String filename ) {
		String filepath = getBasePath(context);
		return filepath + "/ncg/" + DemoLibrary.getFilenameFromFilePath( filename );
	}
	
	
	/**
	 * @brief	returns list of files in path as ArrayList<File>
	 * 
	 * @param   path file path
	 * @return	file list
	 *
	 */
	final static public ArrayList<File> getFileList( final String path ) {
		ArrayList<File> ret = new ArrayList<File>();
		
		File f = new File( path );
		File[] files = f.listFiles();
		
		if( files != null ) {
			for( File file : files ) {
				if( file.isFile()) ret.add( file );
			}
		}
		return ret;
	}
	
	
	/**
	 * @class  	DownloadNotifyHelper
	 * @brief 	a class for download event notification function between Activities
	 *
	 */
	static class DownloadNotifyHelper {
		
		public static interface OnDownloadEvent {
			void onDownloadComplete();
			void onProgress(int percent);
		}
		
		ArrayList<OnDownloadEvent> mDownloadCompleteEvent = new ArrayList<OnDownloadEvent>();
		
		void registerDownloadCompleteEvent(OnDownloadEvent obj) {
			mDownloadCompleteEvent.add(obj);
		}
		
		void ungisterDownloadCompleteEvent(OnDownloadEvent obj) {
			mDownloadCompleteEvent.remove(obj);
		}
		
		void fireOnDownloadCompleted() {
			for( OnDownloadEvent downloadCompletedEvent : mDownloadCompleteEvent ) {
				downloadCompletedEvent.onDownloadComplete();
			}
		}
		
		void fireOnProgress(int percent) {
			for( OnDownloadEvent downloadCompletedEvent : mDownloadCompleteEvent ) {
				downloadCompletedEvent.onProgress(percent);
			}
		}
	}
	
	
	/** 
	 * @brief	DownloadNotifyHelper object, it is returned when getDownloadNotifier is called by other object.
	 */
	private static DownloadNotifyHelper mDownloadNotifier = new DownloadNotifyHelper();
	
	
	/**
	 * @brief	DownloadNotifyHelper object.
	 *
	 * @return	DownloadNotifyHelper object
	 */
	public static DownloadNotifyHelper getDownloadNotifier() {
		return mDownloadNotifier;
	}
}
