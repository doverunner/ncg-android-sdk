package com.inka.ncgsample;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;


/**
 * @class  	DemoApplication
 * @brief 	DemoApplication class is launched before NCG2SDKSAMPLE class.
 * <br> It initializes NCG library and registers HttpRequestCallback.
 *
 */
@SuppressLint("DefaultLocale") public class DemoApplication extends Application {

	
	//Ncg2SdkWrapperListenerImpl mNcg2SdkListener;


	/**
	 * @class  	mHttpRequestCallback
	 * @brief 	implementation of callback interface for HTTP request inside SDK.
	 * <br> HTTP communication will be processed inside SDK if Ncg2Agent.HttpRequestCallback is not registered. 
	 * If the callback is registered, it will be called whenever HTTP communication is needed.
	 */
	
	
	@Override
	public void onCreate() {
		super.onCreate();
		//initializeNcgAgent();
	}

	
	
	/**
	 * @class  	CurrentTimeChecker
	 * @brief 	utility class for checking current time
	 */
	@SuppressLint("SimpleDateFormat") class CurrentTimeChecker {
		
		/**
		 * @brief	get information of currently connected network
		 *
		 * @return	type of network, if it is -1, no network is connected
		 *
		 */
		private  int getConnectedNetwork() {
			ConnectivityManager cm = (ConnectivityManager) getSystemService (Context.CONNECTIVITY_SERVICE);
	    	
	    	NetworkInfo networkInofs[] = cm.getAllNetworkInfo();
	    	for( NetworkInfo networkInof : networkInofs ){
	    		if( networkInof.isConnectedOrConnecting()) {
	    			return networkInof.getType();
	    		}
	    	}
	    	return -1;
		}
		
		/**
		 * @brief	check whether the device is on-line or not
		 *
		 * @return	false: off-line, true: on-line
		 *
		 */
		private boolean isConnectedNetwork() {
			if( getConnectedNetwork() == -1 ) {
				return false;
			}
			else {
				return true;
			}
		}
		
		/**
		 * @brief	get time info from a well-known time server.<br> 
		 * it can be checked on http://www.epochconverter.com site

		 * @return current time value(second unit)
		 * @throws IOException 		when it failed to get network time
		 *
		 */
		private long getNetworkTimeFromTimeServer() throws IOException {
			String[] machines = new String[]{ "time.bora.net", "time.ewha.net", "time.korserve.net", "time.nuri.net"  };
			
		    final int daytimeport = 37;
		    byte[] buff = new byte[8];
		    
			for( int i = 0; i < machines.length; i++ ) {
			    try {			    	
			    	SocketAddress addr = new InetSocketAddress(machines[i], daytimeport);		    	
			    	Socket socket = new Socket();			    	
				   	socket.setSoTimeout(1000);		// read timeout
				   	socket.connect( addr, 1000);	// connect timeout
				   	
				    BufferedInputStream bis = new BufferedInputStream( socket.getInputStream());
				    int nLen = bis.read( buff );
				    bis.close();
				    socket.close();
				    
			    	// Convert the byte array to an long value
				    long ret = 0;
				    for ( int j = 0; j < nLen; j++ ) {
				    	int shift = (nLen - 1 - j) * 8;
			            ret += ((long) buff[j] & 0x000000FF) << shift;
			        }
				    
				    return ret - 2208988800L;				
				} catch (UnknownHostException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}			
			throw new IOException("Cannot get network time.");		
		}
		
		/**
		 * @brief	returns time info from getNetworkTimeFromTimeServer method after transform it to yyyy-MM-ddTHH:mm:ss format.

		 * @return yyyy-MM-ddTHH:mm:ss
		 * @throws IOException 		when it failed to check network time
		 *
		 */
		private String	getNetworkTime() throws IOException {
			long t_value = getNetworkTimeFromTimeServer();

	        Calendar cal = Calendar.getInstance();
	        cal.setTimeInMillis( t_value * 1000 );
	        cal.setTimeZone( TimeZone.getTimeZone("GMT") );
	        
	        return String.format( "%04d-%02d-%02dT%02d:%02d:%02dZ", 
	        		cal.get( Calendar.YEAR ), cal.get( Calendar.MONTH) + 1,
	        		cal.get( Calendar.DAY_OF_MONTH), cal.get( Calendar.HOUR_OF_DAY),
	        		cal.get( Calendar.MINUTE ), cal.get( Calendar.SECOND));
		}
		
		/**
		 * @brief	returns time info from device system clock after transform it to yyyy-MM-ddTHH:mm:ss form.

		 * @return yyyy-MM-ddTHH:mm:ss
		 *
		 */
		private String	getDeviceGMTTime() {
	        SimpleDateFormat	sdf	= new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	        sdf.setTimeZone(TimeZone.getTimeZone("gmt"));
	        return	sdf.format(new Date());
		}	
		
		/**
		 * @brief	get network time if the device is on-line, 
	     * and get device time if it is off-line.

		 * @return GMT time
		 *
		 */
		public String getCurrentTimeInGMT() {
			String gmtTime = "";
			gmtTime = getDeviceGMTTime();			
			if ( isConnectedNetwork() ) {
				try {
					gmtTime = getNetworkTime();
				} catch (IOException e1) {
					e1.printStackTrace();
					gmtTime = getDeviceGMTTime();
				}
			} else {
				gmtTime = getDeviceGMTTime();
			}
			if (gmtTime.length() == 0) {
				throw new RuntimeException("Failed to check current time.");
			}			
			return gmtTime;
		}
	}
	
	
	public boolean hasTelephony()
	{
	    TelephonyManager tm = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
	    if (tm == null)
	        return false;


	    PackageManager pm = getPackageManager();

	    if (pm == null)
	        return false;

	    boolean retval = false;
	    try
	    {
	        Class<?> [] parameters = new Class[1];
	        parameters[0] = String.class;
	        Method method = pm.getClass().getMethod("hasSystemFeature", parameters);
	        Object [] parm = new Object[1];
	        parm[0] = "android.hardware.telephony";
	        Object retValue = method.invoke(pm, parm);
	        if (retValue instanceof Boolean)
	            retval = ((Boolean) retValue).booleanValue();
	        else
	            retval = false;
	    }
	    catch (Exception e)
	    {
	        retval = false;
	    }

	    return retval;
	}
	
//	/**
//	 * @brief	initialize NCGSDK and register httpRequestCallback.
//
//	 * @return void
//	 *
//	 */
//	public void initializeNcgAgent() {
//		Ncg2ExceptionlEventListenerImpl.getInstance().setApiKey("Your API Key");
//		Ncg2ExceptionlEventListenerImpl.getInstance().initialize(this);
//		
//		mNcg2SdkListener = new Ncg2SdkWrapperListenerImpl(this, MediaPlayerActivity.class);
//		Ncg2SdkWrapper.getInstance().getNcgAgent().setExceptionalEventListener(Ncg2ExceptionlEventListenerImpl.getInstance());
//		Ncg2SdkWrapper.getInstance().init(this, mNcg2SdkListener);
//	}
	

	@Override
	public void onTerminate() {
		Ncg2SdkWrapper.getInstance().release();
		super.onTerminate();
	}
}
