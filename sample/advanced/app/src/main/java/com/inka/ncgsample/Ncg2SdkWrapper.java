package com.inka.ncgsample;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.inka.ncg2.Ncg2Agent;
import com.inka.ncg2.Ncg2Agent.HeaderInformation;
import com.inka.ncg2.Ncg2Agent.HttpRequestCallback;
import com.inka.ncg2.Ncg2Agent.LicenseInformation;
import com.inka.ncg2.Ncg2Agent.LicenseValidation;
import com.inka.ncg2.Ncg2Agent.NcgFile;
import com.inka.ncg2.Ncg2Agent.NcgFile.SeekMethod;
import com.inka.ncg2.Ncg2Agent.OfflineSupportPolicy;
import com.inka.ncg2.Ncg2Exception;
import com.inka.ncg2.Ncg2HttpException;
import com.inka.ncg2.Ncg2LocalWebServer;
import com.inka.ncg2.Ncg2LocalWebServer.WebServerListener;
import com.inka.ncg2.Ncg2ReadPhoneStateException;
import com.inka.ncg2.Ncg2SdkFactory;
import com.inka.ncg2.Ncg2FatalException;
import com.inka.ncg2.Ncg2ServerResponseErrorException;


@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class Ncg2SdkWrapper {
	interface Ncg2SdkWrapperListener {
		void onError(Exception e, String msg);

		void onInvalidNcgLicense(String contentPath, LicenseValidation lv, String token);

		void onCompletedAcquireLicense(String mFilePath, String token);

		void onServerError(Ncg2ServerResponseErrorException e, String string,
				int errorCode);

		void onWebserverNotification(int notifyCode, String notifyMsg);

		void onWebserverError(int errorCode, String errorMessage);

		void onCompletedUpdateSecureTime();
		
		void onSecurityError(Exception e);
		
		void onAppFinishedError(Exception e);
	}
	
	private Ncg2SdkWrapperListener mListener;
	private Ncg2Agent mNcg2Agent;
	private HttpRequestCallback mHttpRequestCallback;
	private static Ncg2SdkWrapper mInstance;
	private Handler mHandler;
	private Context mContext;
	
	
	
	public static Ncg2SdkWrapper getInstance() {
		if( mInstance == null ) {
			mInstance = new Ncg2SdkWrapper();
		}
		return mInstance;
	}
	
	public Ncg2SdkWrapper() {
		mNcg2Agent = Ncg2SdkFactory.getNcgAgentInstance();
	}
	
	
	public Ncg2Agent getNcgAgent() {
		return mNcg2Agent;
	}
	
	
	public boolean init(Context context, Ncg2SdkWrapperListener listener) {
		mListener = listener;
		mContext = context;
		mHandler = new Handler();
		mHttpRequestCallback = new NcgHttpRequestCallbackImpl(context);
		OfflineSupportPolicy policy = OfflineSupportPolicy.OfflineSupport;
		policy.setCountOfExecutionLimit(0);
		try {
			mNcg2Agent.init(context, policy);
			mNcg2Agent.setHttpRequestCallback(mHttpRequestCallback );
			//mNcg2Agent.disableLog();
		} catch (Ncg2FatalException e) {
			e.printStackTrace();
			mListener.onAppFinishedError(e);
			return false;
			
		} catch (Ncg2ReadPhoneStateException e) {
			e.printStackTrace();
			mListener.onSecurityError(e); 
			return false;
			
		} catch (Ncg2Exception e) {
			e.printStackTrace();
			mListener.onError(e, "init() Exception : " + e.getMessage()); 
			return false;
		}
		
		return true;
	}
	
	public void release() {
		mNcg2Agent.release();
	}

	public boolean unpackNcg2File(String ncgFilePath, String unpackFilePath) {
		BufferedOutputStream fileOutStream = null;					
		byte[] buffer = new byte[1024];
		NcgFile ncgFile = mNcg2Agent.createNcgFile();
		try {
			ncgFile.open(ncgFilePath);
			ncgFile.seek(0, SeekMethod.End);
			
			// the original file's size will be used for checking success of decryption.
			long contentFileSize = ncgFile.getCurrentFilePointer();
			ncgFile.seek(0, SeekMethod.Begin);
			Log.d(DemoLibrary.TAG, "Content FileSize : " + contentFileSize);
			
			// removes ".ncg" in the path of NCG file
			fileOutStream = new BufferedOutputStream(new FileOutputStream(unpackFilePath));
			long totalReadBytes = 0;
			while( true ) {
				long readBytes = ncgFile.read(buffer, 1024);
				if( readBytes <= 0 ) {
					break;
				}							
				fileOutStream.write(buffer, 0, (int)readBytes);
				totalReadBytes += readBytes;
			}
			
			if( totalReadBytes == contentFileSize ) {
				return true;
			}
		} catch(Ncg2Exception e) {
			e.printStackTrace();
			mListener.onError(e, "unpackNcg2File() Ncg2Exception : " + e.getMessage());
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			mListener.onError(e, "unpackNcg2File() FileNotFoundException : " + e.getMessage());
			
		} catch (IOException e) {
			e.printStackTrace();
			mListener.onError(e, "unpackNcg2File() IOException : " + e.getMessage());
		}  finally {
			
			try {
				if( ncgFile != null ) {
					ncgFile.close();
				}
				if( fileOutStream != null ) {
					fileOutStream.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return false;
	}
	
	public boolean isNcgContent(String path) {
		try {
			return mNcg2Agent.isNcgContent(path);
		} catch (Ncg2HttpException e) {
			e.printStackTrace();
			mListener.onError(e, "isNcgContent() : Ncg2HttpException : " + e.getMessage()); 
		} catch (Ncg2Exception e) {
			e.printStackTrace();
			mListener.onError(e, "isNcgContent() : Ncg2HttpException : " + e.getMessage());
		}
		
		return false;
	}
	
	

	public HeaderInformation getHeaderInfo(String ncgFilePath) {
		try {
			return mNcg2Agent.getHeaderInfo(ncgFilePath);
		} catch (Ncg2HttpException e) {
			e.printStackTrace();
			mListener.onError(e, "getHeaderInfo() : Ncg2HttpException : " + e.getMessage()); 
		} catch (Ncg2Exception e) {
			e.printStackTrace();
			mListener.onError(e, "getHeaderInfo() : Ncg2Exception : " + e.getMessage());
		}
		return null;
	}

	public LicenseInformation getLicenseInfo(String ncgFilePath) {
		try {
			return mNcg2Agent.getLicenseInfo(ncgFilePath);
		} catch (Ncg2HttpException e) {
			e.printStackTrace();
			mListener.onError(e, "getLicenseInfo() : Ncg2HttpException : " + e.getMessage());
		} catch (Ncg2Exception e) {
			e.printStackTrace();
			mListener.onError(e, "getLicenseInfo() : Ncg2Exception : " + e.getMessage());
		}
		return null;
	}
	
	
	public String getPlaybackUrl(String contentPath, String remotePath, long fileSize, String token) {
		// if the content is not play-count limit license, It is ignored.
		try {
			boolean isNcgContent = mNcg2Agent.isNcgContent(contentPath);
			Ncg2LocalWebServer ncgLocalWebServer = mNcg2Agent.getLocalWebServer();
			String playbackURL = "";
			if( isNcgContent == false ) {
				//
				//	Non-DRM Content
				//
				if( contentPath.startsWith("http://") || contentPath.startsWith("https://") ) {
					playbackURL = contentPath;
				}
				else {
					// non-drm local playback for DnP
					if( remotePath != null || fileSize != 0 ) {
						playbackURL = ncgLocalWebServer.addLocalFilePathForPlayback(contentPath, remotePath, fileSize);
					}
					else {
						playbackURL = contentPath;
					}
				}
			}
			else {
				//
				//	DRM Content
				//
				
				if( checkLicenseAndNotifyIfInvalid(contentPath, token) == false ) {
					return null;
				}
				if( contentPath.contains(".m3u8") ) {
					// HLS content
					playbackURL = ncgLocalWebServer.addHttpLiveStreamUrlForPlayback(contentPath);
				} else if(contentPath.startsWith("http://") || contentPath.startsWith("https://")) {
					// PD content
					playbackURL = ncgLocalWebServer.addProgressiveDownloadUrlForPlayback(contentPath);
				} else {
					// Local Content
					playbackURL = ncgLocalWebServer.addLocalFilePathForPlayback(contentPath, remotePath, fileSize);
				}
				
				decreasePlayCountAndDisplayMsg(contentPath);
				
			}
			
			return playbackURL;
		}
		catch(Ncg2Exception e) {
			e.printStackTrace();
			mListener.onError(e, "getPlaybackUrl() Exception : " + e.getMessage());
			return null;
		}
	}
	
	
	
	private void decreasePlayCountAndDisplayMsg(String contentPath) throws Ncg2Exception {
		if( mNcg2Agent.isNcgContent(contentPath) ) {
			final Ncg2Agent.LicenseInformation licInfo = mNcg2Agent.getLicenseInfo(contentPath);
			if( licInfo.playTotalCount <= 0 ) {
				// Skip
				return;
			}
			
			mNcg2Agent.decreasePlayCount(contentPath);
			mHandler.post(new Runnable() {
				
				@Override
				public void run() {
					Toast.makeText(mContext, "PlayCount Decreases. remain playcount is " + (licInfo.playRemainCount-1), Toast.LENGTH_LONG).show();
				}
			});	
		}
		
	}

	private boolean checkLicenseAndNotifyIfInvalid(String contentPath, String token) throws Ncg2HttpException, Ncg2Exception {
		LicenseValidation lv = mNcg2Agent.checkLicenseValid(contentPath);
		if( lv == LicenseValidation.ValidLicense ) {
			return true;
		}
		
		mListener.onInvalidNcgLicense(contentPath, lv, token);
		return false;
	}


	@SuppressLint("DefaultLocale") 
	private WebServerListener mWebserverListener = new WebServerListener(){

		@Override
		public void onNotification(final int notifyCode, final String notifyMsg) {
		
			mListener.onWebserverNotification(notifyCode, notifyMsg);
		
		}

		@Override
		public void onError(final int errorCode, final String errorMessage) {
			mListener.onWebserverError(errorCode, errorMessage);
		}

		
		@Override
		public PlayerState onCheckPlayerStatus(String uri) {
			// if you have not been prepared you should return 'PlayerState.Fail' instead of 'PlayerState.ReadyToPlay'
			return PlayerState.ReadyToPlay;
		}
	};

	public void acquireLicenseByToken(Activity activity, String contentPath, String token) {
		AcquireLicenseByTokenTask task = new AcquireLicenseByTokenTask(activity, contentPath, token);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		} else {
			task.execute();
		}
	}

	public void acquireLicense(Activity activity, String contentPath, String userID, String orderID) {
		AcquireLicenseTask task = new AcquireLicenseTask(activity, contentPath, userID, orderID);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		} else {
			task.execute();
		}
	}

	public void updateSecureTime() {
		UpdateSecureTimeTask task = new UpdateSecureTimeTask();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		} else {
			task.execute();
		}
	}
	
	class UpdateSecureTimeTask extends AsyncTask<Void, Void, Void> {

		private boolean isSuccess;

		@Override
		protected void onPostExecute(Void result) {
			if( isSuccess ) {
				mListener.onCompletedUpdateSecureTime();
			}
			else {
				// DO NOTHING..
			}
			super.onPostExecute(result);
		}

		@Override
		protected Void doInBackground(Void... params) {
			try {
				mNcg2Agent.updateSecureTime();
				isSuccess = true;
			}
			catch(final Ncg2Exception e) {
				mListener.onError(e, "Failed to update SecureTime");
			}
			return null;
		}
		
	}

	private class AcquireLicenseByTokenTask extends AsyncTask<Void, Void, Void>{
		String mUrl;
		String mToken;
		private ProgressDialog mProgressDlg;
		private Activity mActivity;
		private boolean mIsSucceeded;

		AcquireLicenseByTokenTask(Activity activity, String url, String token) {
			mUrl = url;
			mActivity = activity;
			mToken = token;
		}

		@Override
		protected void onPreExecute() {
			mProgressDlg = ProgressDialog.show(mActivity, "Wait", "Please wait...", true);
		}

		@Override
		protected void onPostExecute(Void result) {
			if(mProgressDlg != null){
				try {
					mProgressDlg.dismiss();
					mProgressDlg = null;
				}
				catch(Exception e) {
					e.printStackTrace();
				}
			}
			if( mIsSucceeded ) {
				mListener.onCompletedAcquireLicense(mUrl, mToken);
			}
		}

		@Override
		protected Void doInBackground(Void... params) {
			try {
				boolean isTemporaryLicense;
				if( mUrl.startsWith("http://") || mUrl.startsWith("https://") ) {
					// 스트리밍 라이선스인 경우.
					isTemporaryLicense = true;
				}
				else {
					isTemporaryLicense = false;
				}
				mNcg2Agent.acquireLicenseByToken(mToken, isTemporaryLicense);
				mIsSucceeded = true;
			}
			catch(final Ncg2HttpException e) {
				mListener.onError(e, "[Ncg2HttpException] Cannot acquire license");
				return null;
			}
			catch(final Ncg2ServerResponseErrorException e) {
				mListener.onServerError(e, "[Ncg2ServerResponseErrorException] Cannot acquire license",
						e.getServerErrorCode());
			}
			catch(final Ncg2Exception e) {
				mListener.onError(e, " Cannot acquire license");
				return null;
			}

			return null;
		}
	}

	private class AcquireLicenseTask extends AsyncTask<Void, Void, Void>{
		String mFilePath;
		private ProgressDialog mProgressDlg;
		private Activity mActivity;
		private boolean mIsSucceeded;
		private String mUserID;
		private String mOrderID;
		
		AcquireLicenseTask(Activity activity, String path, String userID, String orderID) {
			mActivity = activity;
			mFilePath = path;
			mUserID = userID;

			if( path.endsWith("01_llama_drama_1080p_loc.mp4.ncg") )
				mOrderID = "duration180";
			else
				mOrderID = orderID;
		}
		
		@Override
		protected void onPreExecute() {
			mProgressDlg = ProgressDialog.show(mActivity, "Wait", "Please wait...", true);
		}
		
		@Override
		protected void onPostExecute(Void result) {
			if(mProgressDlg != null){
				try {					
					mProgressDlg.dismiss();
					mProgressDlg = null;
				}
				catch(Exception e) {
					e.printStackTrace();
				}
			}
			if( mIsSucceeded ) {
				mListener.onCompletedAcquireLicense(mFilePath, "");
			}
		}
		

		@Override
		protected Void doInBackground(Void... params) {
			try {
				boolean isTemporaryLicense;
				if( mFilePath.startsWith("http://") || mFilePath.startsWith("https://") ) {
					// 스트리밍 라이선스인 경우.
					isTemporaryLicense = true;
				}
				else {
					isTemporaryLicense = false;	
				}

				String testUrl = "https://license-sqa.pallycon.com";
				HeaderInformation header = mNcg2Agent.getHeaderInfo( mFilePath );
				if( mFilePath.contains("token") ) {
					mNcg2Agent.acquireLicenseByToken(
							header.contentID, mUserID, header.siteID,
							"eyJyZXNwb25zZV9mb3JtYXQiOiJvcmlnaW5hbCIsInVzZXJfaWQiOiJ1dGVzdCIsImRybV90eXBlIjoibmNnIiwic2l0ZV9pZCI6IkRFTU8iLCJoYXNoIjoidUpiZWJvWXM5K0EwWlJHVzhFZ1AzaDhtMitMTXpVbEkwWjI0TmY0WERTUT0iLCJjaWQiOiJuY2ctbGxhbWEiLCJwb2xpY3kiOiI5V3FJV2tkaHB4VkdLOFBTSVljbkpzY3Z1QTlzeGd1YkxzZCthanVcL2JvbVFaUGJxSSt4YWVZZlFvY2NrdnVFZnVMdHZVTFlxME51aDVSWjhYRmM0NUVsR3dXXC82N1lYVHEyUEgyeGd3SEdYQ2pVbmlIM2w0ODVTZnA2Y25ldW5uanYzMXhreVR3elZQM3VYSFBiVjVkdz09IiwidGltZXN0YW1wIjoiMjAyMC0wOC0yNVQxNTo1NTo0NFoifQ==",
							testUrl, isTemporaryLicense);
				} else {
					mNcg2Agent.acquireLicenseByCID(
							header.contentID, mUserID,
							header.siteID,
							mOrderID,
							testUrl, isTemporaryLicense);
				}
				mIsSucceeded = true;
			}
			catch(final Ncg2HttpException e) {
				mListener.onError(e, "[Ncg2HttpException] Cannot acquire license");
				return null;
			}
			catch(final Ncg2ServerResponseErrorException e) {
				mListener.onServerError(e, "[Ncg2ServerResponseErrorException] Cannot acquire license", 
						e.getServerErrorCode());
			}
			catch(final Ncg2Exception e) {
				mListener.onError(e, " Cannot acquire license");
				return null;
			}
			
			return null;
		}
	
	}

	
}

