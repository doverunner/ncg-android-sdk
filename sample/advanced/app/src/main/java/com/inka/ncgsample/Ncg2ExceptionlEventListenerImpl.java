package com.inka.ncgsample;

import android.content.Context;
import android.util.Log;

import com.inka.ncg2.Ncg2Agent;

import java.util.logging.Logger;

public class Ncg2ExceptionlEventListenerImpl implements Ncg2Agent.NcgExceptionalEventListener {

	private static final String TAG = "Ncg2Exception";
	private static Ncg2ExceptionlEventListenerImpl mInstance;
	private Context mContext;
	private String mApiKey = "Your API Key";
	
	private Ncg2ExceptionlEventListenerImpl() {
		
	}
	
	public static Ncg2ExceptionlEventListenerImpl getInstance() {
		if( mInstance == null ) {
			mInstance = new Ncg2ExceptionlEventListenerImpl();
		}
		
		return mInstance;
	}
	
	public void setApiKey(String apiKey) {
		mApiKey = apiKey;
	}
	
	
	public void initialize(Context context) {
		mContext = context;
		/*
		Mint.disableNetworkMonitoring();
		Mint.setUserIdentifier(mContext.getPackageName());
		Mint.initAndStartSession(context, mApiKey);
		Mint.enableLogging(true);
		Mint.setLogging(5000);
		*/
	}
	
	@Override
	public void log(String message) {
		//Mint.leaveBreadcrumb(message);
		Log.e(TAG, message);
	}
	
	@Override
	public void logException(Exception ex) {
		//Mint.logException(ex);
		Log.e(TAG, ex.getMessage());
	}
}