package com.inka.ncgsample;

import java.io.File;
import java.util.ArrayList;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

/**
 * @class  	UnpackNcgFileTask
 * @brief 	a class inherited from AsyncTask. performs unpacking of NCG file.
 *
 */
public class UnpackNcgFileTask extends AsyncTask<Void, Void, Void> {
	private ProgressDialog mProgressDlg;
	private MainActivity mainActivity;
	private  ArrayList<File> mFile;
	private boolean mUnpackResult;
	
	UnpackNcgFileTask(MainActivity mainActivity, ArrayList<File> mFile){
		this.mainActivity = mainActivity;
		this.mFile = mFile;
	}
	
	@Override
	protected void onPreExecute() {
		mProgressDlg = ProgressDialog.show(mainActivity, "Unpackaging", "Please wait...", true);
		Log.d(DemoLibrary.TAG, "Unpack Task Started.");
	}
	
	@Override
	protected void onPostExecute(Void result) {
		Log.d(DemoLibrary.TAG, "Unpack Task has been ended.");

		mProgressDlg.hide();
		
		if( mUnpackResult ) {
			Log.d(DemoLibrary.TAG, "Decryption succeeded");
			mainActivity.refreshListViewData();
			Toast.makeText(mainActivity, "Unpack completed successfully.", Toast.LENGTH_LONG).show();
		}
		else {
			Log.d(DemoLibrary.TAG, "Decryption failed");
			Toast.makeText(mainActivity, "[unpackNcgFiles] decryption failed", Toast.LENGTH_LONG).show();
		}
		
		
	}
	
	@Override
	protected Void doInBackground(Void... params) {
		int nLen = mFile.size();
		for( int i = 0; i < nLen; i++ ) {
			File file = (File)mFile.get(i);
			if( file != null ) {
				String ncgFilePath = file.getAbsolutePath();
				int pos = file.getAbsolutePath().lastIndexOf("/");
				String unpackFilePath = ncgFilePath.substring(0, pos+1) + file.getName().replace(".ncg", "");
				if( ncgFilePath.endsWith(".ncg") == false ) {
					continue;
				}
				mUnpackResult = Ncg2SdkWrapper.getInstance().unpackNcg2File(ncgFilePath, unpackFilePath);
				
			}				
		}			
		
		return null;
	}
} 
