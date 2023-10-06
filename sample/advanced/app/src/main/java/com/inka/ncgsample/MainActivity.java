package com.inka.ncgsample;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.inka.ncg.jni.Ncg2CoreErrorCode;
import com.inka.ncg.jni.Util;
import com.inka.ncg2.DeviceManager;
import com.inka.ncg2.Ncg2Agent;
import com.inka.ncg2.Ncg2Agent.LicenseValidation;
import com.inka.ncg2.Ncg2Exception;
import com.inka.ncg2.Ncg2HttpException;
import com.inka.ncg2.Ncg2LocalWebServer;
import com.inka.ncg2.Ncg2ServerResponseErrorException;
import com.inka.ncgsample.MainActivity.CustomAdapter.ListViewItem;

import static com.inka.ncg2.Ncg2Agent.LicenseValidation.ExpiredLicense;
import static com.inka.ncg2.Ncg2Agent.LicenseValidation.NotExistLicense;
import static com.inka.ncg2.Ncg2Agent.LicenseValidation.ValidLicense;


@SuppressLint("NewApi")
public class MainActivity extends Activity {

	/**
	 * @brief	variables for showDialogEx method
	 *
	 */
	final static public int DIALOG_CONFIRM_DOWNLOAD			= 1;



	final static public int DIALOG_ALERTE					= 9000;
	final static public int DIALOG_EXIT						= 9999;
	final static public String TAG							= "MainActivity";

	private String mNcgFileURL;
	private String mNcgFileToken;
	private ListView mListView;
	private String mNcgFilesDir;
	private boolean mCheckButtonFlags[] = null;

	private int		m_dialog_id = -1;
	private String	m_alertMessage = null;
	//private LicenseProcessor licenseProcessor;

	private Global mGlobal = Global.getInstance();
	private Ncg2SdkWrapper mNcgSdkWrapper;
	private Ncg2SdkWrapperListenerImpl mNcg2SdkListener;

	@Override
	public void onBackPressed() {
		showDialogEx( DIALOG_EXIT );
	}

	@Override
	public void onPause() {
    	Log.d(DemoLibrary.TAG, "MainActivity.onPause ++");

    	mGlobal.mIsActivityPaused = true;
		super.onPause();
	}

    @Override
    public void onResume() {
    	Log.d(DemoLibrary.TAG, "MainActivity.onResume ++");
		super.onResume();

		mGlobal.mIsActivityPaused = false;


		if( mGlobal.mDownloadCompleted == true ) {
			moveTempFileToDownloadDir();
			Log.d(DemoLibrary.TAG, "[MainActivity.onActivityResult] moveTempFileToDownloadDir() Called");
			AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
			builder.setTitle(getString(R.string.notify));
			builder.setMessage(getString(R.string.download_completed));
    		builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
    			@Override
    			public void onClick(DialogInterface dialog, int which) {
    	    		dialog.dismiss();
    			}
    		});
    		builder.show();
    		refreshListViewData();
		}

		mGlobal.reset();

		if( mNcgSdkWrapper.getNcgAgent().isInitialized() ) {
			refreshListViewData();
		}
	}

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(DemoLibrary.TAG, "[MainActivity.onActivityResult] ++");

		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
		recreate();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.layout_main);
		mNcgSdkWrapper = Ncg2SdkWrapper.getInstance();

		if (Build.VERSION.SDK_INT >= 23) {
			if( checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_DENIED ) {
				requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE}, 1);
				return;
			}
//			if( checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED ) {
//				requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
//				return;
//			}
		}

		if(mNcgSdkWrapper.getNcgAgent().isInitialized() == false) {
			initializeNcgAgent();
		}

		mNcgFilesDir = DemoLibrary.getBasePath(this);
		mListView = (ListView)findViewById(R.id.listView);
		mListView.setOnItemClickListener(mOnItemClickListener);
		mListView.setOnItemLongClickListener(mOnItemLongClickListener);

		//Spinner spinner = (Spinner)findViewById(R.id.playerMode);
		//spinner.setOnItemSelectedListener(mOnItemSelectedListener);
		// check whether the Activity object is released before finishing download
		if( mGlobal.mDownloadTask != null &&
			mGlobal.mDownloadCompleted == false ) {
			mGlobal.mDownloadTask.recreated();
		}

	}

	/**
	 * @brief	initialize NCGSDK and register httpRequestCallback.
	 * @return void
	 *
	 */
	void initializeNcgAgent() {
		Ncg2ExceptionlEventListenerImpl.getInstance().initialize(this);
		Ncg2SdkWrapper.getInstance().getNcgAgent().setExceptionalEventListener(Ncg2ExceptionlEventListenerImpl.getInstance());

		mNcg2SdkListener = new Ncg2SdkWrapperListenerImpl(MainActivity.this, ExoPlayerActivity.class, this);
		Ncg2SdkWrapper.getInstance().init(this, mNcg2SdkListener);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
    	boolean ret = super.onCreateOptionsMenu( menu );
    	menu.add( 0, 0, 0, getString(R.string.menu_delete_file));
    	menu.add( 0, 1, 0, getString(R.string.menu_decrypt_license_only));
    	menu.add( 0, 2, 0, getString(R.string.menu_show_application_info));
    	menu.add( 0, 3, 0, "Unpack Ncg Files");
    	menu.add( 0, 4, 0, "Force S/W Codec(NexPlayer only)");
    	menu.add( 0, 5, 0, "open source information");
		return ret;
	}

	 private String readText(String file) throws IOException {
		  InputStream is = getAssets().open(file);

		  int size = is.available();
		  byte[] buffer = new byte[size];
		  is.read(buffer);
		  is.close();

		  String text = new String(buffer);

		  return text;
	 }

	@SuppressLint({"DefaultLocale", "MissingPermission"})
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch( item.getItemId()) {

		case 0 : // remove licenses of selected files
			removeLicenseForFiles(true);
			break;

		case 1 : // remove selected files and their licenses
			removeLicenseForFiles(false);
			break;

		case 2 :
			String versinInfo = "No Information";
    		String memory = "No Information";

    		String currentTime = mNcgSdkWrapper.getNcgAgent().getCurrentSecureTime() + "_GMT";
    		PackageManager pm = getPackageManager();
    		try {
    			memory = ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024)) + "MB";
    			versinInfo = pm.getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES).versionName;
    		} catch (NameNotFoundException e) {
    			e.printStackTrace();
    		}
    		boolean hasTelephony = hasTelephony();
    		boolean isInvalidTelephonyDeviceId = isInvalidTelephonyDeviceId();
    		String telephonyID = "";
    		String phoneNumber = "";
    		int phoneType = 0;
    		TelephonyManager telephonyMgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
			if( telephonyMgr != null ) {
				// 전화기능이 없지만 모듈이 감지된 경우.
	            phoneNumber = telephonyMgr.getLine1Number();
	            Log.d(TAG, "PhoneNumber : " + phoneNumber);
	            telephonyID = telephonyMgr.getDeviceId();
	            phoneType = telephonyMgr.getPhoneType();
			}
			
    		String deviceIdInfo = String.format("\n\t Invalid-TelphonyDeviceID ? [%s]\n\t hasTelephony ? [%s]\n\t " + 
    						"TelephonyID : [%s]\n\t PhoneNumber : [%s]\n\t PhoneType : [%d]\n", 
    						isInvalidTelephonyDeviceId ? "true" : "false",
    						hasTelephony ? "true" : "false",
    						telephonyID, phoneNumber, phoneType
    								);
    		
    		String message = String.format(
    				"Version : %s(%s)\nDeveloper : %s \nMemory : %s\nCurrentTime : %s\n " +
    				"DeviceID Info: [%s]",
    				versinInfo, 
    				getString(R.string.dateInfo), 
    				getString(R.string.developerInfo), 
    				memory, 
    				currentTime,
    				deviceIdInfo);
    		AlertDialog.Builder builder = new AlertDialog.Builder( this ); 
    		builder.setTitle("NCG2SDK Sample");
    		builder.setMessage( message );
    		builder.setNegativeButton( R.string.confirm, null );
    		builder.show();
			break;
			
		case 3 : // Unpack
			unpackNcgFiles();
			break;
			
		case 4:
			Global.getInstance().mIsSwCodecForced = !Global.getInstance().mIsSwCodecForced;
			if( Global.getInstance().mIsSwCodecForced ) {
				Toast.makeText(this, "S/W Codec Enabled", Toast.LENGTH_LONG).show();
			}
			else {
				Toast.makeText(this, "S/W Codec Disabled", Toast.LENGTH_LONG).show();
			}
			break;
			
		case 5:
			String licenseString = "";
			try {
				licenseString = readText("opensourcelicense.txt");
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			AlertDialog.Builder openSourceBuilder = new AlertDialog.Builder( this ); 
			openSourceBuilder.setTitle("NCG2SDK Sample");
			openSourceBuilder.setMessage( licenseString );
			openSourceBuilder.setNegativeButton( R.string.confirm, null );
    		openSourceBuilder.show();
		}
		return super.onOptionsItemSelected(item);
	}
	
	public boolean hasTelephony()
    {
        TelephonyManager tm = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        if (tm == null) {        	
        	return false;
        }
 
        PackageManager pm = getPackageManager();
        if (pm == null) {
            return false;
        }
        
        int phoneType = tm.getPhoneType();
        if( phoneType != TelephonyManager.PHONE_TYPE_NONE && pm.hasSystemFeature("android.hardware.telephony")) {
        	return true;
        }
        
        return false;
    }
	
	@SuppressLint("MissingPermission")
	private boolean isInvalidTelephonyDeviceId() {
		
		TelephonyManager telephonyMgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		if( telephonyMgr == null ) {
			return false;
		}
		
		String phoneNumber = "";
        phoneNumber = telephonyMgr.getLine1Number();
        Log.d(TAG, "PhoneNumber : " + phoneNumber);
        
        String telephonyID = telephonyMgr.getDeviceId();
        Log.d(TAG, "Telephony DeviceID: " + telephonyID);
        
        if( hasTelephony() == false && 
        		telephonyID != null && 
        		telephonyID.length() > 0 ) {
        	return true;
        }
	
		return false;
	}

	private void unpackNcgFiles() {
		if( mCheckButtonFlags == null ) {
			Toast.makeText(this, "Select the ncg files first.", Toast.LENGTH_SHORT).show();
			return;
		}
		ArrayList<File> files = new ArrayList<File>();
		int nLen = mCheckButtonFlags.length;
		for( int i = 0; i < nLen; i++ ) {
			if( mCheckButtonFlags[i] ) {
				CustomAdapter.ListViewItem lvi = (ListViewItem) mListView.getItemAtPosition(i);
				if( lvi.itemType != ListItemType.LocalDownloaded ) {
					continue;
				}
				
				if( mNcgSdkWrapper.isNcgContent(lvi.path) ) {
					files.add(new File(lvi.path));
				}
			}
		}
		
		UnpackNcgFileTask task = new UnpackNcgFileTask(this, files);
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		} else {
			task.execute();
		}
	}

	@Override 
    protected Dialog onCreateDialog(int id) {
		
		Dialog dialog = null;
    	AlertDialog.Builder builder = new AlertDialog.Builder( this );
    	String item_info_msg = null;
    	
    	switch( id ) {
    	
    	// closing application
    	case DIALOG_EXIT :
    		builder.setMessage( getString( R.string.confirm_end ));
    		builder.setPositiveButton( getString( R.string.yes ),	new DialogInterface.OnClickListener() {
    			@Override public void onClick(DialogInterface dialog, int which) {    				
    				moveTaskToBack(true);
    				finish();
    				mNcgSdkWrapper.release();
    				//android.os.Process.killProcess(android.os.Process.myPid());
    				
    			}
    		});
    		builder.setNegativeButton( R.string.no, null );
    		dialog = builder.create();
    		break; 
    		
    	// dialog for showing message only
    	case DIALOG_ALERTE :
    		if( m_alertMessage != null ) {
    			item_info_msg = m_alertMessage; 
    			builder.setMessage( item_info_msg );
    		}
			builder.setPositiveButton( getString(R.string.ok), null );
			dialog = builder.create();
    		break;
    	
    		
    	// dialog for file download confirmation
    	case DIALOG_CONFIRM_DOWNLOAD :
    		builder.setMessage( getString( R.string.confirm_dnp ));
    		builder.setPositiveButton( getString( R.string.yes ),	new DialogInterface.OnClickListener() {
    			@Override public void onClick(DialogInterface dialog, int which) {
    				dialog.dismiss();
    				mGlobal.mDownloadTask = new DownloadNcgFileTask(
    						MainActivity.this, 
    						mNcgFileURL, mGlobal.mNcgFileDownloadPath, mGlobal.mToken);
    				
    				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
    					mGlobal.mDownloadTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    				} else {
    					mGlobal.mDownloadTask.execute();
    				}
    			}
    		});    		
    		builder.setNeutralButton( R.string.no, null );
    		dialog = builder.create();
    		break; 
    	}    	
    	
    	return dialog;
	}
	
	/**
	 * @class  	mOnItemClickListener
	 * @brief	register listener on ListView item click event
	 * 
	 * @warning  This ItemClickListener is hard-coded for sample application. It should be re-written by application developer for service.
	 * If the path is a URL, it should start with "http:/".
	 */
	private OnItemClickListener mOnItemClickListener = new OnItemClickListener(){
		
		@SuppressWarnings("deprecation")
		@Override
		public void onItemClick(AdapterView<?> adapterView, View view, int position,
				long arg3) {
			 
			CustomAdapter.ListViewItem lvi = ((CustomAdapter.ListViewItem)mListView.getItemAtPosition(position)); 
			String absoultePath = lvi.path;
			String token = lvi.token;
			
			// when selecting category
			if( lvi.itemType == ListItemType.Title ){
				return;
			}
			
			// reset filesize for DnP Size. 
			mGlobal.mNcgFileSizeForDnp = 0;
			if(lvi.description.contains("ads") == true) {
				mGlobal.mIsAds = true;
			} else {
				mGlobal.mIsAds = false;
			}

			if(lvi.description.contains("live") == true) {
				mGlobal.mIsLive = true;
			} else {
				mGlobal.mIsLive = false;
			}

			switch(lvi.itemType) {
			case Download:
				//when selecting DownLoad category
				mNcgFileURL = URLDecoder.decode(absoultePath);
				mGlobal.mToken = token;
				// downloaded NCG file path
				final String downloadedNcgPath = DemoLibrary.getItemPath(MainActivity.this, mNcgFileURL );
				String downloadNcgPath = DemoLibrary.getDonwloadPath(MainActivity.this, mNcgFileURL );
				mNcgFileURL = DemoLibrary.safeUrlEncoder(mNcgFileURL);
				
				//  check if there is already downloaded file
				if( DemoLibrary.checkIfFileExists( downloadedNcgPath )) {
					// if there is already downloaded file, start playback of the file
					AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
	            	builder.setCancelable(true);		            	
	            	builder.setMessage(getString(R.string.confirm_play_already_exist));
	            	builder.setPositiveButton( getString(R.string.ok), new DialogInterface.OnClickListener() {
		        		public void onClick(DialogInterface dialog, int id) {
		        			dialog.dismiss();
		        			
		        			// local playback			
		        			mGlobal.mNcgFilePath = downloadedNcgPath;
		        			if(mGlobal.mNcgFilePath.toLowerCase().contains("jpg") || mGlobal.mNcgFilePath.toLowerCase().contains("png")){
								startImageActivity();
							} else {
								startPlayerActivityIfPossible();
							}
		        		}
		        	});
	            	
	            	builder.setNegativeButton( getString(R.string.cancel), new DialogInterface.OnClickListener() {
		        		public void onClick(DialogInterface dialog, int id) {
		        			dialog.dismiss();
		        		}
		        	});
	            	builder.show();
	            							
				} else {
					// if there is no file, show download dialog
					mGlobal.mNcgFileDownloadPath = downloadNcgPath;
					showDialogEx( DIALOG_CONFIRM_DOWNLOAD );
				}
				break;

			case PD:
				mNcgFileURL = URLDecoder.decode(absoultePath);
				mNcgFileURL = DemoLibrary.safeUrlEncoder(mNcgFileURL);
				mGlobal.mNcgFilePath = mNcgFileURL;
				mGlobal.mToken = token;
				startPlayerActivityIfPossible();
				break;
				
			case HLS:
				mNcgFileURL = URLDecoder.decode(absoultePath);
				mNcgFileURL = DemoLibrary.safeUrlEncoder(mNcgFileURL);
				mGlobal.mNcgFilePath = mNcgFileURL;
				mGlobal.mToken = token;

				// for highly response speed, you have to check license validation
				startHlsPlayerActivity(absoultePath);

				// basic response speed
				//startPlayerActivityIfPossible();
				break;

			case CENC:
				mGlobal.mNcgFilePath = absoultePath;
				checkCENC();
				break;

			case LocalDownloaded:
				// when selecting Local category
				mGlobal.mNcgFilePath = absoultePath;
				mGlobal.mToken = token;
				if(mGlobal.mNcgFilePath.toLowerCase().contains("jpg") || mGlobal.mNcgFilePath.toLowerCase().contains("png")){
					startImageActivity();
				} else {
					startPlayerActivityIfPossible();
				}
				break;
			
			default:
				throw new RuntimeException("Unrecognized ListViewItem type.");
			}
			
		}
	};
	

	private OnItemSelectedListener mOnItemSelectedListener = new OnItemSelectedListener(){

		@Override
		public void onItemSelected(AdapterView<?> view, View arg1, int arg2,
				long arg3) {
			mGlobal.mPlayerMode = view.getItemAtPosition(arg2).toString();
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) {
			// If nothing was selected, set player mode with 'NexPlayer'
			mGlobal.mPlayerMode = "NexPlayer";
		}
		
	};
	/**
	 * @class  	mOnItemLongClickListener
	 * @brief	register LongClick event on ListView item
	 * When LongClick occurs on an item, showLicenseInfo method is called for the item
	 */
	private OnItemLongClickListener mOnItemLongClickListener = new OnItemLongClickListener(){

		@Override
		public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
				int position, long arg3) {
			CustomAdapter.ListViewItem lvi = ((CustomAdapter.ListViewItem)mListView.getItemAtPosition(position));
			if( lvi.itemType == ListItemType.Title ) {
				return true;
			}
			
			String absoultePath = lvi.path;
			if(absoultePath.contains(mNcgFilesDir)){
				showLicenseInfo( absoultePath);
			}
			
			return true;
		}
		
	};

	
	protected void refreshListViewData(){
	    CustomAdapter customAdapter = new CustomAdapter();
		mListView.setAdapter(customAdapter);
	}
	
	/**
	 * @brief	remove license for ncg file with optional removal of the file itself
	 * 
	 * @param   fileDelete remove license and file if true, remove only license if false
	 * @return	void
	 *
	 */
	private void removeLicenseForFiles(boolean fileDelete) {

		if( mCheckButtonFlags == null ) {
			Toast.makeText(this, "Select the ncg files first.", Toast.LENGTH_SHORT).show();
			return;
		}

		int nLen = mCheckButtonFlags.length;
		for( int i = 0; i < nLen; i++ ) {
			if( mCheckButtonFlags[i] ) {
				CustomAdapter.ListViewItem lvi = (ListViewItem) mListView.getItemAtPosition(i);
				if( lvi.itemType != ListItemType.LocalDownloaded ) {
					continue;
				}
				
				try {
					if( mNcgSdkWrapper.isNcgContent(lvi.path) ) {
						mNcgSdkWrapper.getNcgAgent().removeLicenseByPath(lvi.path);
					}
				} catch (Ncg2Exception e) {						
					e.printStackTrace();
				}
				
				if(fileDelete){
					new File(lvi.path).delete();
				}
			}
		}
		refreshListViewData();
	}
	
	
	
	/**
	 * @brief	show AlertDialog with license information of NCG file
	 * 
	 * @param   filePath file path
	 * @return	void
	 *
	 */
	private void showLicenseInfo( String filePath ) {		
		String strPlayStartDate = "(unlimited)";
		String strPlayEndDate = "(unlimited)";
		String strPlayTotalCount = "(unlimited)";
		String strPlayRemainCount = "(unlimited)";
		
		String strExternalDisplayAllow = "Not Detected";
		String strRootingDeviceAllow = "Not Detected";
		
		if(mNcgSdkWrapper.isNcgContent(filePath) == false ) {
			Toast.makeText(this, "Non-DRM Content", Toast.LENGTH_LONG).show();
			return;
		}

		try {
			LicenseValidation lv = mNcgSdkWrapper.getNcgAgent().checkLicenseValid(filePath);
			Toast.makeText(this, lv.toString(), Toast.LENGTH_LONG).show();
		} catch (Ncg2HttpException e) {
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
		} catch (Ncg2Exception e) {
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
		}
		
		Ncg2Agent.HeaderInformation headerInfo = mNcgSdkWrapper.getHeaderInfo(filePath);
		Ncg2Agent.LicenseInformation licenseInfo = mNcgSdkWrapper.getLicenseInfo(filePath);
		if( headerInfo == null || licenseInfo == null ) {
			return;
		}
		
		LayoutInflater inflater = (LayoutInflater)getSystemService(android.content.Context.LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.license_info,
			                               (ViewGroup)findViewById(R.id.LLayout_LicenseInfo));
		
		// File header information
		TextView	textViewCID	= (TextView)layout.findViewById(R.id.TextView_ContentsID);
		textViewCID.setText(headerInfo.contentID);
			
		TextView	textViewSID	= (TextView)layout.findViewById(R.id.TextView_SiteID);
		textViewSID.setText(headerInfo.siteID);

		TextView	textViewAcqURL	= (TextView)layout.findViewById(R.id.TextView_AcqusitionURL);
		textViewAcqURL.setText(headerInfo.acquisitionURL);
			
		TextView	textViewSrc		= (TextView)layout.findViewById(R.id.TextView_Source);
		textViewSrc.setText(headerInfo.serviceProvider);
			
		TextView	textViewPack	= (TextView)layout.findViewById(R.id.TextView_PackDate);
		textViewPack.setText(headerInfo.packageDate);
		
		TextView	textViewPlayStart	= (TextView)layout.findViewById(R.id.TextView_PlayStartDate);
		TextView	textViewPlayEnd	= (TextView)layout.findViewById(R.id.TextView_PlayEndDate);	
		TextView	textViewPlayVerMethod	= (TextView)layout.findViewById(R.id.TextView_PlayVerificationMethod);	
		TextView	textViewPlayDuration	= (TextView)layout.findViewById(R.id.TextView_PlayDurationHour);	
		TextView	textViewPlayTotalCount	= (TextView)layout.findViewById(R.id.TextView_PlayTotalCount);	
		TextView	textViewPlayRemainCount	= (TextView)layout.findViewById(R.id.TextView_PlayRemainCount);	
		
		TextView	textViewExternalDisplay	= (TextView)layout.findViewById(R.id.TextView_ExternalDisplay);	
		TextView	textViewRootedDevice	= (TextView)layout.findViewById(R.id.TextView_RootedDevice);
		TextView	textViewDeviceModel	= (TextView)layout.findViewById(R.id.TextView_DeviceModel);
		
		ViewGroup vg_binding = ( ViewGroup ) layout.findViewById(R.id.license_binding );
		if( vg_binding != null ) {
			if( headerInfo.binding != null ) {
				if( headerInfo.binding.length() < 1 ) {
					vg_binding.setVisibility( View.GONE );
				} else {
					vg_binding.setVisibility( View.VISIBLE );
					TextView	textBinding	= (TextView)layout.findViewById(R.id.TextView_Binding );
					textBinding.setText(headerInfo.binding);
				}
			} else {
				vg_binding.setVisibility( View.GONE );
			}
		}
		
		// show only header information if there is no license
		if(licenseInfo.licenseValidation == NotExistLicense){
			textViewPlayStart.setText("No License");
			textViewPlayEnd.setText("No License");
			textViewPlayVerMethod.setText("No License");
			textViewPlayDuration.setText("No License");
			textViewPlayTotalCount.setText("No License");
			textViewPlayRemainCount.setText("No License");
			textViewExternalDisplay.setText("No License");
			textViewRootedDevice.setText("No License");
			textViewDeviceModel.setText("");
			AlertDialog.Builder	builder = new AlertDialog.Builder( MainActivity.this );
			builder.setView(layout);
			builder.setTitle( DemoLibrary.getFilenameFromFilePath( filePath ))
					.setCancelable(true)
					.setPositiveButton( R.string.ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.cancel();
						}
					});
				
			AlertDialog	alert = builder.create();			
			alert.show();
			return;
		}
		
		if( licenseInfo.playTotalCount != -1 ) {
			strPlayTotalCount	= String.valueOf(licenseInfo.playTotalCount);
		}	
		
		if( licenseInfo.playRemainCount != -1 ) {
			strPlayRemainCount	= String.valueOf(licenseInfo.playRemainCount);
		}
					
		if( !licenseInfo.playStartDate.equals("") ) {					
			strPlayStartDate = licenseInfo.playStartDate;
		}
		
		if( !licenseInfo.playEndDate.equals("") ) {
			strPlayEndDate = licenseInfo.playEndDate; 
		}
		
		if( licenseInfo.abnormal_device == 'a' ) {
			strRootingDeviceAllow = "allow";
		}
		else {
			strRootingDeviceAllow = "disallow";
		}
		
		if( licenseInfo.opt_externalDisplay == 'a' ) {
			strExternalDisplayAllow = "allow";
		}
		else {
			strExternalDisplayAllow = "disallow";
		}
		
		// license information
		textViewPlayStart.setText(strPlayStartDate);
		textViewPlayEnd.setText(strPlayEndDate);
		textViewPlayVerMethod.setText(licenseInfo.playVerificationMethod);
		textViewPlayDuration.setText(String.valueOf(licenseInfo.playDurationHour));
		textViewPlayTotalCount.setText(strPlayTotalCount);
		textViewPlayRemainCount.setText(strPlayRemainCount);
		textViewExternalDisplay.setText(strExternalDisplayAllow);
		textViewRootedDevice.setText(strRootingDeviceAllow);
		textViewDeviceModel.setText(new DeviceManager.Builder(this).prefixName("DRM").build().getDeviceModel());

		AlertDialog.Builder	builder = new AlertDialog.Builder( MainActivity.this );
		builder.setView(layout);
		builder.setTitle( DemoLibrary.getFilenameFromFilePath( filePath ))
				.setCancelable(true)
				.setPositiveButton( R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				});
			
		AlertDialog	alert = builder.create();			
		alert.show();

	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	/**
	 * @brief  show relevant dialog by dialog_id
	 * @param  dialog_id
	 * @return void
	 */
	@SuppressWarnings("deprecation")
	private void showDialogEx( int dialog_id ) {
		dismissDialog();
		
		m_dialog_id = dialog_id;
		showDialog( m_dialog_id );
	}
	
	/**
	 * @brief  dismiss dialog
	 * @return void
	 */
	@SuppressWarnings("deprecation")
	private void dismissDialog() {
		if( m_dialog_id != -1 ) {
			dismissDialog( m_dialog_id );
			removeDialog( m_dialog_id );
		}
		m_dialog_id = -1;		
	}
	
		
	
	/**
	 * @brief  move downloaded NCG file from temp folder to download folder
	 * @return void
	 */
	protected void moveTempFileToDownloadDir() {
		// move downloaded file
    	String src	= DemoLibrary.getDonwloadPath(this, mGlobal.mNcgFileNameForDownloading);
    	String dest = DemoLibrary.getItemPath(this, mGlobal.mNcgFileNameForDownloading);
    	Log.d(DemoLibrary.TAG, "moveTempFileToDownloadDir() ++");
    	Log.d(DemoLibrary.TAG, "moveTempFileToDownloadDir() src : " + src);
    	Log.d(DemoLibrary.TAG, "moveTempFileToDownloadDir() dest : " + dest);
    	File srcFileObj = new File( src );
    	if( srcFileObj.exists() == false ) {
    		return;
    	}
		File destFileObj = new File( dest );
		if( destFileObj.exists()) {
			if( srcFileObj.getAbsolutePath().compareTo( destFileObj.getAbsolutePath()) != 0 ) {
				destFileObj.delete();
			}
		}	    		
		srcFileObj.renameTo( destFileObj );		
		Log.d(DemoLibrary.TAG, "moveTempFileToDownloadDir() --");
	}
	
	/**
	 * @brief  show dialog to confirm playback after finishing download
	 * @return void
	 */
	protected void displayDownloadCompleteDlg() {
	    	// show confirmation dialog for playback 
			AlertDialog.Builder	builder = new AlertDialog.Builder(MainActivity.this);
	    	builder.setCancelable(true);
	    	builder.setTitle("Download Completed");	        	
	    	builder.setMessage( getString(R.string.confirm_play) );
	    	builder.setPositiveButton( getString(R.string.ok), new DialogInterface.OnClickListener() {
	    		public void onClick(DialogInterface dialog, int id) {
	    			dialog.dismiss();		        					        					        					    	    	
	    			mGlobal.mNcgFilePath = DemoLibrary.getItemPath(MainActivity.this, mGlobal.mNcgFileNameForDownloading);
	    			startPlayerActivityIfPossible();
	    		}
	    	});
	    	builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {							
					dialog.dismiss();
				}
			});
	    	builder.show();
	    	mGlobal.mDownloadCompleted = false;
	    }

	void checkCENC() {
		Boolean	bErr = false;
		int		ret = -1;
		Ncg2Agent.NcgCenc			ncgCenc	= mNcgSdkWrapper.getNcgAgent().getNcgCenc();
		Ncg2Agent.HeaderInformation	headerInfo	= null;
		Ncg2Agent.LicenseInformation licInfo = null;

		String	resultMsg;
		String	infoMsg = "";

		// decryptCencSample 호출 위한 더미 값
		byte[]	IV = new byte[16];
		int		numOfSubsamples = 2;
		int[]	bytesOfClearData = new int[]{8, 8};
		int[]	bytesOfProtectedData = new int[]{8, 8};
		byte[]	sampleData	= new byte[32];

		try {
			headerInfo	= mNcgSdkWrapper.getHeaderInfo(mGlobal.mNcgFilePath);
			licInfo		= mNcgSdkWrapper.getLicenseInfo(mGlobal.mNcgFilePath);

			if( headerInfo != null )
				infoMsg	+= "\nCID: " + headerInfo.contentID + "\nSID: " + headerInfo.siteID;
			if( licInfo != null )
				infoMsg += "\nExpiration: " + licInfo.playEndDate;

			// prepareCencDecrypt() 이전이므로, exception이 발생해야 한다.
			try {
				byte[]	out = ncgCenc.decryptCencSample(IV, numOfSubsamples, bytesOfClearData, bytesOfProtectedData, sampleData);
				Log.d(DemoLibrary.TAG, "==> [No prepare][No set lic.] decryptCencSample(): normal. --> BAD !!");
				bErr = true;
			} catch(Ncg2Exception e) {
				Log.d(DemoLibrary.TAG, "==> [No prepare][No set lic.] decryptCencSample(): exception. --> good");
			}

			ncgCenc.prepareCencDecrypt();

			// setLicense() 이전이므로, exception이 발생해야 한다.
			try {
				byte[]	out = ncgCenc.decryptCencSample(IV, numOfSubsamples, bytesOfClearData, bytesOfProtectedData, sampleData);
				Log.d(DemoLibrary.TAG, "[prepare][No set lic.] decryptCencSample(): normal. --> BAD !!");
				bErr = true;
			} catch(Ncg2Exception e) {
				Log.d(DemoLibrary.TAG, "[prepare][No set lic.] decryptCencSample(): exception. --> good");
			}

			try {
				ret = ncgCenc.setLicense(headerInfo.contentID, headerInfo.siteID);
				switch(ret) {
					case	Ncg2CoreErrorCode.NCGERR_SUCCEED:
						resultMsg	= "Valid License";
						break;
					default:
						resultMsg	= "!! INVALID License !!";
						break;
				}
				// 라이센스 관련 문자열 출력
				resultMsg += infoMsg;
				Util.showSimpleMsgLong(this, resultMsg);
			} catch(Ncg2Exception e) {
				Util.showSimpleMsgLong(this, e.getMessage());
			}

			if( ret == Ncg2CoreErrorCode.NCGERR_SUCCEED ) {
				// setLicense() 에 성공하였으므로, 정상 종료되어야 한다.
				try {
					byte[]	out = ncgCenc.decryptCencSample(IV, numOfSubsamples, bytesOfClearData, bytesOfProtectedData, sampleData);
					Log.d(DemoLibrary.TAG, "[prepare][set lic. (good)] decryptCencSample(): normal. --> good");
				} catch(Ncg2Exception e) {
					Log.d(DemoLibrary.TAG, "[prepare][set lic. (good)] decryptCencSample(): exception. --> BAD !!");
					bErr = true;
				}
			}
			else {
				// setLicense() 에 실패하였으므로, exception이 발생해야 한다.
				try {
					byte[]	out = ncgCenc.decryptCencSample(IV, numOfSubsamples, bytesOfClearData, bytesOfProtectedData, sampleData);
					Log.d(DemoLibrary.TAG, "[prepare][set lic. (bad)] decryptCencSample(): normal. --> BAD !!");
					bErr = true;
				} catch(Ncg2Exception e) {
					Log.d(DemoLibrary.TAG, "[prepare][set lic. (bad)] decryptCencSample(): exception. --> good");
				}
			}

			ncgCenc.prepareCencDecrypt();

			// prepareCencDecrypt() 를 호출하였으만 다시 호출하고 setLicense() 를 호출하지 않았으므로, exception이 발생해야 한다.
			try {
				byte[]	out = ncgCenc.decryptCencSample(IV, numOfSubsamples, bytesOfClearData, bytesOfProtectedData, sampleData);
				Log.d(DemoLibrary.TAG, "[RE prepare][No set lic.] decryptCencSample(): normal. --> BAD !!");
				bErr = true;
			} catch(Ncg2Exception e) {
				Log.d(DemoLibrary.TAG, "[RE prepare][No set lic.] decryptCencSample(): exception. --> good");
			}

			ncgCenc.clearCencDecrypt();

			// clearCencDecrypt() 호출하였으므로, exception이 발생해야 한다.
			try {
				byte[]	out = ncgCenc.decryptCencSample(IV, numOfSubsamples, bytesOfClearData, bytesOfProtectedData, sampleData);
				Log.d(DemoLibrary.TAG, "[clear] decryptCencSample(): normal. --> BAD !! <==");
				bErr = true;
			} catch(Ncg2Exception e) {
				Log.d(DemoLibrary.TAG, "[clear] decryptCencSample(): exception. --> good <==");
			}

			if( bErr ) {
				Util.showSimpleMsgLong(this, "Some errors on decryptCencSample().\nPlease check log.");
			} else {
				Util.showSimpleMsgLong(this, "All decryptCencSample() are good.");
			}

		} catch (Ncg2Exception e) {
			Util.showSimpleMsgLong(this, e.getMessage());
		}
	}
	
	void startPlayerActivityIfPossible() {
		mNcg2SdkListener.startPlayerActivity(mGlobal.mNcgFilePath, mGlobal.mToken);
	}

	void startImageActivity() {
		String contentPath = mGlobal.mNcgFilePath;

		StrictMode.ThreadPolicy pol = new StrictMode.ThreadPolicy.Builder().permitNetwork().build();
		StrictMode.setThreadPolicy(pol);

		try {
			mNcgSdkWrapper.getNcgAgent().acquireLicenseByToken( mGlobal.mToken , true);
		} catch (Ncg2ServerResponseErrorException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (Ncg2HttpException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (Ncg2Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		Intent intent = new Intent(this, ImageActivity.class);
		intent.putExtra("path", contentPath);
		startActivity(intent);
	}

	void startHlsPlayerActivity(String localM3U8Path) {
		String contentPath;
		boolean isLocalM3U8;
		if (localM3U8Path.startsWith("http")) {
			contentPath = mGlobal.mNcgFilePath;
			isLocalM3U8 = false;
		} else {
			contentPath = localM3U8Path;
			isLocalM3U8 = true;
		}

		StrictMode.ThreadPolicy pol = new StrictMode.ThreadPolicy.Builder().permitNetwork().build();
		StrictMode.setThreadPolicy(pol);

		try {
			String playbackURL = "";
			if(mNcgSdkWrapper.getNcgAgent().isNcgContent(contentPath)) {
				LicenseValidation lv = mNcgSdkWrapper.getNcgAgent().checkLicenseValid(contentPath);
				// TODO : please check license validation
				if(lv != NotExistLicense && lv != ValidLicense) {
					Toast.makeText(MainActivity.this, "please check license validation!", Toast.LENGTH_LONG).show();
					return;
				}

				if (lv != ValidLicense) {
					boolean saveLicense = isLocalM3U8 ? false : true;
					mNcgSdkWrapper.getNcgAgent().acquireLicenseByToken(Global.getInstance().mToken, saveLicense);
				}

				Ncg2LocalWebServer ncgLocalWebServer = mNcgSdkWrapper.getNcgAgent().getLocalWebServer();

				if (isLocalM3U8) {
					playbackURL = ncgLocalWebServer.addHttpLiveStreamUrlForLocalPlayback(contentPath);
				} else {
					if (mGlobal.mIsLive == true) {
						playbackURL = ncgLocalWebServer.addHttpLiveStreamUrlForPlayback(contentPath, true);
					} else {
						playbackURL = ncgLocalWebServer.addHttpLiveStreamUrlForPlayback(contentPath, false);
					}
				}
			} else {
				playbackURL = contentPath;
			}

			Intent intent;
			if (isLocalM3U8) {
				intent = new Intent(this, ExoPlayerActivity.class);
			} else {
				// HLS 컨텐츠는 MediaPlayerActivity 재생합니다(hevc 때문).
				intent = new Intent(this, MediaPlayerActivity.class);
			}
			intent.putExtra("path", playbackURL);
			startActivity(intent);

			return;

		} catch (Ncg2ServerResponseErrorException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (Ncg2HttpException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (Ncg2Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	enum ListItemType {
		Title,
		Download,
		PD,
		HLS,
		CENC,
		LocalDownloaded,
	}
	
	/**
	 * @class  	CustomAdapter
	 * @brief 	custom adaptor class which is inherited from ArrayAdapter.
	 *  It is an actual ArrayAdapter Custom in getView method.
	 * It also makes the change of mCheckButtonFlags array by checkbox selection.
	 *
	 */
	@SuppressLint({ "ViewHolder", "InflateParams" }) class CustomAdapter extends BaseAdapter {
		
		class ListViewItem {
			ListItemType itemType;
			String path;
			String description;
			String token;

			ListViewItem(ListItemType itemType, String path) {
				this.itemType = itemType;
				this.path = path;
				this.description = "";
			}

			ListViewItem(ListItemType itemType, String path, String description) {
				this.itemType = itemType;
				this.path = path;
				this.description = description;
			}

			ListViewItem(ListItemType itemType, String path, String token, String description) {
				this.itemType = itemType;
				this.path = path;
				this.token = token;
				this.description = description;
			}
		}
		
		private ArrayList<ListViewItem> mListViewItems = new ArrayList<ListViewItem>();
		
		public CustomAdapter() {
			mListViewItems.add( new ListViewItem(ListItemType.Title, "Download"));
			mListViewItems.add( new ListViewItem(ListItemType.Download,
					"https://contents.pallycon.com/TEST/PACKAGED_CONTENT/TEST_SIMPLE/sintel-trailer.mp4.ncg",
					"eyJrZXlfcm90YXRpb24iOmZhbHNlLCJyZXNwb25zZV9mb3JtYXQiOiJvcmlnaW5hbCIsInVzZXJfaWQiOiJ1dGVzdCIsImRybV90eXBlIjoibmNnIiwic2l0ZV9pZCI6IkRFTU8iLCJoYXNoIjoicE1DUXFldzFZeGk4WXJDYlJlQnNzM0IwYUI0T3hBVER1UmdZaVRVXC8zbVk9IiwiY2lkIjoiVGVzdFJ1bm5lciIsInBvbGljeSI6IjlXcUlXa2RocHhWR0s4UFNJWWNuSnNjdnVBOXN4Z3ViTHNkK2FqdVwvYm9tUVpQYnFJK3hhZVlmUW9jY2t2dUVmdUx0dlVMWXEwTnVoNVJaOFhGYzQ1RWxHd1dcLzY3WVhUcTJQSDJ4Z3dIR1hDalVuaUgzbDQ4NVNmcDZjbmV1bm5qdjMxeGt5VHd6VlAzdVhIUGJWNWR3PT0iLCJ0aW1lc3RhbXAiOiIyMDIwLTExLTIwVDA0OjE2OjI1WiJ9",
					"Sintel Trailer"));
			mListViewItems.add( new ListViewItem(ListItemType.Download,
					"https://contents.pallycon.com/DEV/yhpark/ncg/ncg-django/django.mp4.ncg",
					"eyJrZXlfcm90YXRpb24iOmZhbHNlLCJyZXNwb25zZV9mb3JtYXQiOiJvcmlnaW5hbCIsInVzZXJfaWQiOiJ1dGVzdCIsImRybV90eXBlIjoibmNnIiwic2l0ZV9pZCI6IkRFTU8iLCJoYXNoIjoiVnJIY1FHMnVBT1FYNTlwSVg4aEFCQlJJQmFmSXoweXU4bGx5NFIxa0xKVT0iLCJjaWQiOiJuY2ctZGphbmdvIiwicG9saWN5IjoiOVdxSVdrZGhweFZHSzhQU0lZY25Kc2N2dUE5c3hndWJMc2QrYWp1XC9ib21RWlBicUkreGFlWWZRb2Nja3Z1RWZ4RGNjbTdjV2RWWHFyZE1nQVFqbXFmVVhja1doNEgwNGFMODlUa0hKOXUxWjJTUUlhSWFUXC9rd09JUFQyaWZMN2NkK0pBK2l0clpzaHNqbXpxR0R6NWVzOVhtbk0rWktUNnF4WUtOM2o0ekV3WURvTHlBeUhTZzVvN3BVQjVZa1YiLCJ0aW1lc3RhbXAiOiIyMDIyLTAxLTE4VDEyOjIwOjQ1WiJ9",
					"ncg-django"));

			mListViewItems.add( new ListViewItem(ListItemType.Title, "ProgressiveDownload") );
			mListViewItems.add( new ListViewItem(ListItemType.PD,
					"https://contents.pallycon.com/TEST/PACKAGED_CONTENT/TEST_SIMPLE/sintel-trailer.mp4.ncg",
					"eyJrZXlfcm90YXRpb24iOmZhbHNlLCJyZXNwb25zZV9mb3JtYXQiOiJvcmlnaW5hbCIsInVzZXJfaWQiOiJ1dGVzdCIsImRybV90eXBlIjoibmNnIiwic2l0ZV9pZCI6IkRFTU8iLCJoYXNoIjoicE1DUXFldzFZeGk4WXJDYlJlQnNzM0IwYUI0T3hBVER1UmdZaVRVXC8zbVk9IiwiY2lkIjoiVGVzdFJ1bm5lciIsInBvbGljeSI6IjlXcUlXa2RocHhWR0s4UFNJWWNuSnNjdnVBOXN4Z3ViTHNkK2FqdVwvYm9tUVpQYnFJK3hhZVlmUW9jY2t2dUVmdUx0dlVMWXEwTnVoNVJaOFhGYzQ1RWxHd1dcLzY3WVhUcTJQSDJ4Z3dIR1hDalVuaUgzbDQ4NVNmcDZjbmV1bm5qdjMxeGt5VHd6VlAzdVhIUGJWNWR3PT0iLCJ0aW1lc3RhbXAiOiIyMDIwLTExLTIwVDA0OjE2OjI1WiJ9",
					"Sintel Trailer"));
			mListViewItems.add( new ListViewItem(ListItemType.PD,
					"https://contents.pallycon.com/DEV/yhpark/ncg/ncg-django/django.mp4.ncg",
					"eyJrZXlfcm90YXRpb24iOmZhbHNlLCJyZXNwb25zZV9mb3JtYXQiOiJvcmlnaW5hbCIsInVzZXJfaWQiOiJ1dGVzdCIsImRybV90eXBlIjoibmNnIiwic2l0ZV9pZCI6IkRFTU8iLCJoYXNoIjoiVnJIY1FHMnVBT1FYNTlwSVg4aEFCQlJJQmFmSXoweXU4bGx5NFIxa0xKVT0iLCJjaWQiOiJuY2ctZGphbmdvIiwicG9saWN5IjoiOVdxSVdrZGhweFZHSzhQU0lZY25Kc2N2dUE5c3hndWJMc2QrYWp1XC9ib21RWlBicUkreGFlWWZRb2Nja3Z1RWZ4RGNjbTdjV2RWWHFyZE1nQVFqbXFmVVhja1doNEgwNGFMODlUa0hKOXUxWjJTUUlhSWFUXC9rd09JUFQyaWZMN2NkK0pBK2l0clpzaHNqbXpxR0R6NWVzOVhtbk0rWktUNnF4WUtOM2o0ekV3WURvTHlBeUhTZzVvN3BVQjVZa1YiLCJ0aW1lc3RhbXAiOiIyMDIyLTAxLTE4VDEyOjIwOjQ1WiJ9",
					"ncg-django"));

			mListViewItems.add( new ListViewItem(ListItemType.Title, "Http Live Streaming") );
			mListViewItems.add( new ListViewItem(ListItemType.HLS,
					"https://contents.pallycon.com/TEST/PACKAGED_CONTENT/TEST_SIMPLE/hls_ncg/master.m3u8",
					"Sintel Trailer-SIMPLE-AES"));
			mListViewItems.add( new ListViewItem(ListItemType.HLS,
					"https://contents.pallycon.com/DEV/yhpark/original/tictoc-hls/prog_index.m3u8",
					"clear",
					"hls original"));

			ArrayList<File> files = DemoLibrary.getFileList(mNcgFilesDir + "/ncg");
			if( files == null ){
				files = DemoLibrary.getFileList(mNcgFilesDir + "/ncgsd");
			}else {
				ArrayList<File> files2 = DemoLibrary.getFileList(mNcgFilesDir + "/ncgsd"); 
				if( files2 != null ) {
					files.addAll( files2 );
				}
			}

			Collections.sort(files, new Comparator<File>() {
				public int compare(File f1, File f2) {
					if( f1.getName().compareTo(f2.getName()) > 0 )
						return 1;
					else
						return -1;
				}
			});

//			mListViewItems.add( new ListViewItem(ListItemType.Title, "CENC-dec. call test") );
//			if( files != null ) {
//				for (File file : files) {
//					if (file.getName().equals("01_llama_drama_1080p_loc.mp4.ncg")) {
//						mListViewItems.add(new ListViewItem(ListItemType.CENC, file.getAbsolutePath()));
//					}
//				}
//			}

			mListViewItems.add( new ListViewItem(ListItemType.Title, "Local Downloaded") );
			if( files != null ) {
				for( File file : files ) {
					if( file.getName().endsWith(".ncg") ||
							file.getName().endsWith(".mp4") ||
							file.getName().endsWith(".jpg") ||
							file.getName().endsWith(".png") ||
							file.getName().endsWith(".m4a") ||
							file.getName().endsWith(".mp3")) {
						for (int i = 0; i < mListViewItems.size(); i++) {
							if (mListViewItems.get(i).itemType == ListItemType.Download) {
								String fileName = new File(mListViewItems.get(i).path).getName();
								String localName = file.getName();
								if (localName.equalsIgnoreCase(fileName)) {
 									mListViewItems.add(new ListViewItem(ListItemType.LocalDownloaded,
											file.getAbsolutePath(),
											mListViewItems.get(i).token,
											"Local Download Token"));
								}
							}
						}
					}				
				}
			}
		}
		
		@Override
		public View getView(final int position, View convertView, ViewGroup parent){
			LayoutInflater inflater = getLayoutInflater(); 
			View row = (View)inflater.inflate(R.layout.layout_listview, null); 
			TextView fileName = (TextView)row.findViewById(R.id.file_name_text);
			TextView txtFileSize = (TextView)row.findViewById(R.id.file_size_text);
			TextView txtDescription = (TextView)row.findViewById(R.id.file_description_text);
			ImageView imgDrmLockIcon = (ImageView)row.findViewById(R.id.drmLockIcon);
			ImageView mediaTypeIcon = (ImageView)row.findViewById(R.id.mediaTypeIcon);
			final CheckBox chkBoxLocalFile = (CheckBox)row.findViewById(R.id.local_file_cell_ck);
			RelativeLayout relativeLayout = (RelativeLayout)row.findViewById(R.id.customLayout);
			ListViewItem lvi = mListViewItems.get(position);
			
			if (!lvi.description.isEmpty())
			{
				txtDescription.setText( lvi.description );
				txtDescription.setVisibility(View.VISIBLE);
			}
			
			if( mediaTypeIcon != null ) {
				if( lvi.path.contains( ".mp4" ) || lvi.path.contains( ".m3u8" )) {
					mediaTypeIcon.setBackgroundResource( R.drawable.video );
				} else if( lvi.path.contains( ".mp3" ) || lvi.path.contains( ".m4a" )) {
					mediaTypeIcon.setBackgroundResource( R.drawable.audio );
				} else if( lvi.path.contains( ".pyv" )) {
					mediaTypeIcon.setBackgroundResource( R.drawable.video );
				}
				else {
					mediaTypeIcon.setVisibility( View.INVISIBLE );
				}
			}
			if( lvi.itemType == ListItemType.LocalDownloaded ){
				
				String isBinding;
				try {
					isBinding = mNcgSdkWrapper.getNcgAgent().getHeaderInfo(lvi.path).binding;
					if( isBinding == null || isBinding.length() < 1 ) {
						
						if( mNcgSdkWrapper.getNcgAgent().isLicenseValid( lvi.path)){
							imgDrmLockIcon.setBackgroundResource( R.drawable.drm_unlock );
						} else {
							imgDrmLockIcon.setBackgroundResource( R.drawable.drm_lock );
						}
					} else {
						if( isBinding.compareTo( "sdcard") == 0 ) {
							imgDrmLockIcon.setBackgroundResource( R.drawable.sdcard );
						}
					}
				} catch (Ncg2Exception e) {
					e.printStackTrace();
				}
				
				txtFileSize.setText( 
						DemoLibrary.getFileSizeString(new File(lvi.path).length()) );
				
				mCheckButtonFlags = new boolean[mListViewItems.size()];
				for( int i = 0; i < mCheckButtonFlags.length; i++ ) {
					mCheckButtonFlags[i] = false;
				}
				
				chkBoxLocalFile.setOnClickListener(new OnClickListener(){
					
					@Override
					public void onClick(View arg0) {
						if(chkBoxLocalFile.isChecked()){
							mCheckButtonFlags[position] = true;
						}else{
							mCheckButtonFlags[position] = false;
						}
					}
					
				});
			} else {
				txtFileSize.setVisibility(View.GONE);
				chkBoxLocalFile.setVisibility(View.GONE);
				imgDrmLockIcon.setVisibility(View.GONE);
				
				// category View
				if( lvi.itemType == ListItemType.Title ){
						relativeLayout.setBackgroundColor(Color.GRAY);
						imgDrmLockIcon.setVisibility(View.GONE);
						mediaTypeIcon.setVisibility(View.GONE);
				}
			}
			
			fileName.setText( new File(lvi.path).getName() );
			return row; 
		}

		@Override
		public int getCount() {
			return mListViewItems.size();
		}

		@Override
		public Object getItem(int position) {
			return mListViewItems.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}
	}
}
