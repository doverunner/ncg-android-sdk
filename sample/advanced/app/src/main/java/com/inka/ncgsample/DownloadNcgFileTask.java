package com.inka.ncgsample;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;



/**
 * @class  	DownloadNcgFileTask
 * @brief   process NCG content download
 *
 */
@SuppressLint("InflateParams")
@SuppressWarnings("deprecation")
class DownloadNcgFileTask extends AsyncTask<Void, Integer, Void> {
	
	static final int NOTIFY_ID_ACQUIRED_DOWNLOAD_URL = 1;
	static final int NOTIFY_ID_DOWNLOAD_STATE_UPDATE = 2;
	static final int MB1	= 1048657;
	static final int MB100	= 104865700;
				
	private DownloadProgressDialog mProgressDialog = new DownloadProgressDialog();			
	private String mNcgFileDownloadURL;			
	private String mExeptionMsg;
	private int mDownloadPercent;
	private String mDownlaodedSize;			
	private boolean mCancelDownload;		
	private String mRemoteFileSizeForDisplay;		
	private String mDestFilePath;
	private String mTokenDownload;
	private MainActivity mActivity;
	private Global mGlobal = Global.getInstance();
	
	DownloadNcgFileTask(MainActivity activity, String source, String filePath, String token) {
		mActivity = activity;
		mNcgFileDownloadURL = source;
		mGlobal.mNcgFileNameForDownloading = DemoLibrary.getFilenameFromFilePath(filePath);
		mDestFilePath = filePath;
		mTokenDownload = token;
	}
	
	/**
	 * @brief	recreate Progress Dialog
	 *
	 * @return	void
	 *
	 */
	public void recreated() {
		mProgressDialog.init();
		mProgressDialog.show();
	}
	
	
	@Override
	protected void onPreExecute() {
		mGlobal.mDownloadCompleted = false;
		// init download popup
		mProgressDialog.init();
		mProgressDialog.show();
	}
	
	
    @Override
    protected Void doInBackground(Void... unused) {	        	
    	if( downloadNCG2File(mNcgFileDownloadURL, mDestFilePath ) ) {        	
    		mGlobal.mDownloadCompleted = true;
    	}
        return(null);
    }
    
    /**
	 * @brief	File Download method
	 * 
	 * @param   downloadURL
	 * @param   destFilePath
	 * @return	true: download completed, false: failed or canceled
	 *
	 */
	private boolean downloadNCG2File(String downloadURL, String destFilePath) {
    	Log.i(DemoLibrary.TAG, "Download Started.");
		long	nFilesize	= 0;
		float	fDisplaySize= 0.0f;
		int nStatusCode	= 0;
		final int HTTP_ERROR_REQUESTED_RANGE_NOT_SATISFIABLE = 416;
		final int HTTP_ERROR_OK = 200;
		final int HTTP_ERROR_PARTIAL_CONTENT = 206;
		// file download
		InputStream inputStream = null;

		try {
			HttpClient client	= new DefaultHttpClient();
			// request file
	        HttpGet request		= new HttpGet();
	        request.setURI(new URI(downloadURL));
			request.addHeader("Connection", "close");
			
			long existingFileSize = 0;
			// check whether the file already exists
            File destFileObj = new File ( destFilePath );
            if( destFileObj.exists()) { 	                	
            	existingFileSize = destFileObj.length();
            }	    			
			
			if( existingFileSize > 0 ) {
				request.addHeader("Range", "bytes=" + existingFileSize + "-");
            }	    			

			Log.i(DemoLibrary.TAG, "HTTP execute request: " + downloadURL);

			final HttpResponse response = client.execute(request);
			inputStream = response.getEntity().getContent();

			nStatusCode	= response.getStatusLine().getStatusCode();
    		Log.i(DemoLibrary.TAG, "HTTP Response Status Code: " + nStatusCode);
    		if( nStatusCode == HTTP_ERROR_REQUESTED_RANGE_NOT_SATISFIABLE ) {
    			return true;
    		}
			if( nStatusCode != HTTP_ERROR_OK && nStatusCode != HTTP_ERROR_PARTIAL_CONTENT )
			{
				Log.i(DemoLibrary.TAG, "no file or permission.");
				mExeptionMsg = "HTTP Response Error\n" + response.getStatusLine();
				return false;
			}

			// get file length from response message
			Header[] resHeader	= response.getHeaders("Content-Length");
            if( resHeader.length > 0 )
            {
            	String		strValue 	= resHeader[0].getValue();
            	if( strValue != null );
            	{
            		// update UI if there is file size info
            		nFilesize = Long.parseLong(strValue);
            		nFilesize += existingFileSize; 
            		Log.i(DemoLibrary.TAG, "filesize: " + nFilesize);
            		mGlobal.mNcgFileSizeForDnp = nFilesize;
        	        fDisplaySize	= (float)nFilesize / (float)MB1;
        	        if( nFilesize >= MB100 )
        	        {
        	        	mRemoteFileSizeForDisplay	= String.format("%3.0f MB", fDisplaySize);
        	        }
        	        else
        	        {
        	        	mRemoteFileSizeForDisplay	= String.format("%2.1f MB", fDisplaySize);
        	        }
            	}
            }
            
            // write file	                	                	              
            byte[]	bszDownBuf	= new byte[20480];
            int		nRead		= 0;
            long	nTotalRead	= 0;
            BufferedOutputStream outputStream = 
            		new BufferedOutputStream(new FileOutputStream(destFilePath, existingFileSize != 0 ));	                
            
            nTotalRead = existingFileSize;
            do {                	
            	nRead = inputStream.read(bszDownBuf);
            	
            	if( nRead < 1 )
            		break;
            	
        		outputStream.write(bszDownBuf, 0, nRead);

        		nTotalRead	+= nRead;
        		
        		int mOldDownloadPercent = mDownloadPercent;
        		if( nFilesize > 0 )
        		{
        			mDownloadPercent = (int)(((float)nTotalRead / (float)nFilesize) * 100.0f);
        		}
        		if( mDownloadPercent > mOldDownloadPercent ) {
        			Log.d(DemoLibrary.TAG, "Downloading...(Dnp?" + mGlobal.mIsDnpExcuted + ") [ " + mDownloadPercent + " %]" );
        			DemoLibrary.getDownloadNotifier().fireOnProgress(mDownloadPercent);
        		}
            	
    	        fDisplaySize	= (float)nTotalRead / (float)MB1;
    	        if( nTotalRead >= MB100 )
    	        {
    	        	mDownlaodedSize	= String.format("%3.0f MB", fDisplaySize);
    	        }
    	        else
    	        {
    	        	mDownlaodedSize	= String.format("%2.1f MB", fDisplaySize);
    	        }

    	        publishProgress(NOTIFY_ID_DOWNLOAD_STATE_UPDATE, mDownloadPercent);
            	
            } while( !mCancelDownload );
            
            Log.i(DemoLibrary.TAG, "nTotalRead: " + nTotalRead);                
            outputStream.close();
            if( mCancelDownload ) {
            	return false;
            }
            else {
            	return true;
            }
		} catch (URISyntaxException e) {
			e.printStackTrace();
			mExeptionMsg = e.getMessage();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			mExeptionMsg = e.getMessage();
		} catch (IOException e) {
			e.printStackTrace();
			mExeptionMsg = e.getMessage();
		} finally {
            if( inputStream != null ) {
                try {
                	inputStream.close();
                	inputStream = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
		}
		
		return false;
	}
    

    @Override
    protected void onProgressUpdate(Integer... item) { 
    	// use the first value of array : Notify ID 
    	int notifyID = item[0];
    	if( notifyID == NOTIFY_ID_DOWNLOAD_STATE_UPDATE ) {
    		// update Download UI
    		mProgressDialog.mTxtDownloadPercen.setText(String.format("%3d %%", mDownloadPercent));
			mProgressDialog.mTxtDownloadedSize.setText(mDownlaodedSize);
			mProgressDialog.mTxtRemoteFileSize.setText(mRemoteFileSizeForDisplay);
	        mProgressDialog.mProgressBar.setProgress(mDownloadPercent);
    	}
    	else {
    		throw new RuntimeException("onProgressUpdate(): Invalid NotifyID");
    	}
    }
    
    
    @Override
    protected void onPostExecute(Void unused) {
    	Log.d(DemoLibrary.TAG, "[DownloadNcgFileTask.onPostExecute] ++");
    	if( mExeptionMsg != null ) {
    		mProgressDialog.dismiss();
    		AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        	builder.setCancelable(true);
        	builder.setTitle("Download Error");
        	builder.setMessage(mExeptionMsg);
        	builder.setNegativeButton("OK", new DialogInterface.OnClickListener() {
        		public void onClick(DialogInterface dialog, int id) {
        			dialog.dismiss();
        		}
        	});
        	builder.show();
        	return;
    	}
    	mProgressDialog.mTxtDownloadPercen.setText("0");
		mProgressDialog.mTxtDownloadedSize.setText("ready");
		mProgressDialog.mTxtRemoteFileSize.setText("ready");
        mProgressDialog.mProgressBar.setProgress(0);
        mProgressDialog.dismiss();
        mGlobal.mNcgFileSizeForDnp = 0;
        mGlobal.mDownloadTask = null;
        if( mCancelDownload ) {
        	// process cancel button event
        	AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        	builder.setCancelable(true);
        	builder.setTitle("Download Canceled");
        	builder.setMessage("Download has been canceled. You can resume this download later.");
        	builder.setNegativeButton("OK", new DialogInterface.OnClickListener() {
        		public void onClick(DialogInterface dialog, int id) {
        			dialog.dismiss();
        		}
        	});
        	builder.show();
        }
        else if( mGlobal.mDownloadCompleted ) {
        	//
        	// download completed.
        	//	        	
        	// is the content playing?
        	if( mGlobal.mIsDnpExcuted == true ) {
        		// show toast message about downlaod completion.  
        		Toast.makeText(mActivity, "The download has been Completed", Toast.LENGTH_LONG).show();
        		Log.d(DemoLibrary.TAG, "[onPostExecute] mIsDnpExcuted == true... Download Completed.");
        		// The file should be moved later from temp folder to download folder, because the content is being played.
        		// It should be moved when Player Activity is closed.
        		DemoLibrary.getDownloadNotifier().fireOnDownloadCompleted();
        	}
        	else {	        		
        		if( mGlobal.mIsActivityPaused == false ) {
        			// Move the file to download folder
            		mActivity.moveTempFileToDownloadDir();
            		
        			mActivity.displayDownloadCompleteDlg();
        			//ListView refresh
        			mActivity.refreshListViewData();
        			
        		}
        	}	        	
        }	        
    }

    /**
     * @class  	DownloadProgressDialog
     * @brief 	this dialog object shows dowload status of content.
	 * It is a wrapper class of Dialog object.
     *
     */
	private class DownloadProgressDialog {
		private ProgressBar mProgressBar;
		private TextView mTxtDownloadPercen;
		private TextView mTxtDownloadedSize;
		private TextView mTxtRemoteFileSize;
		private Dialog mDialog;
	
		public DownloadProgressDialog() {
		}
		
		/**
		 * @brief	ProgressDialog initialization
		 *
		 * @return	void
		 *
		 */
		public void init() {
			LayoutInflater inflater = (LayoutInflater)mActivity.getSystemService(android.content.Context.LAYOUT_INFLATER_SERVICE);
			View layout = inflater.inflate(R.layout.layout_download, null);
			TextView	textViewDialog	= (TextView)layout.findViewById(R.id.downloadName);
			textViewDialog.setText( mGlobal.mNcgFileNameForDownloading );
			mProgressBar	= (ProgressBar)layout.findViewById(R.id.downloadProgess);
			mTxtDownloadPercen	= (TextView)layout.findViewById(R.id.downloadPercent);				
			mTxtDownloadedSize	= (TextView)layout.findViewById(R.id.TextView_DialogDownedSize);
			mTxtRemoteFileSize	= (TextView)layout.findViewById(R.id.TextView_DialogDownloadFileSize);
			
			// event register for Play button while downloading
			layout.findViewById( R.id.btn_play ).setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					if( mDownloadPercent <= 5 ) {
						Toast.makeText(mActivity, 
								"The downloaded size is too small to play the content. please retry later.", 
								Toast.LENGTH_LONG).show();
						return;
					}
					mGlobal.mNcgFilePath = mDestFilePath;
					mGlobal.mToken = mTokenDownload;
					mGlobal.mRemoteUrlForDnp = mNcgFileDownloadURL;
					Log.d(DemoLibrary.TAG, "DnP Button Clicked!");
					mActivity.startPlayerActivityIfPossible();
					mGlobal.mIsDnpExcuted = true;
				}
			});
			
			AlertDialog.Builder	builder = new AlertDialog.Builder(mActivity);
			builder.setView(layout);
			builder.setCancelable(false);
			builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					mCancelDownload = true;
					dialog.cancel();
				}
			});
			mDialog = builder.create();			        
		}
		
		/**
		 * @brief	shows ProgressDialog
		 *
		 * @return	void
		 *
		 */
		public void show() {
			mTxtDownloadPercen.setText("0");
			mTxtDownloadedSize.setText("ready");
			mTxtRemoteFileSize.setText("ready");
	        mProgressBar.setProgress(0);
	        mDialog.show();
		}
	
		/**
		 * @brief	dismiss ProgressDialog
		 *
		 * @return	void
		 *
		 */
		public void dismiss() {
			Log.d(DemoLibrary.TAG, "[DownloadProgressDialog.dismiss] ++");
			try {
				if( mProgressDialog.mDialog.isShowing() ) {
					mProgressDialog.mDialog.dismiss();
				}
			}catch( IllegalArgumentException e ) {
				// FIXME exception occurs occasionally on Android ICS version. 
				e.printStackTrace();
			}
		}
	}
}