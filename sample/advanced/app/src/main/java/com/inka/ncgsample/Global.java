package com.inka.ncgsample;

import android.app.ProgressDialog;



/**
 * @class  	Global
 * @brief 	This class has variables for file downloading and playback function.
 */
public class Global {
	
	private static Global global = new Global();
	
	public static Global getInstance(){
		return global;
	}

	public String mPlayerMode = "NexPlayer";
	
	/** 
	 * @brief	used for 'Download And Play' to set NCG file size to player
	 */
	public long mNcgFileSizeForDnp;
	
	/** 
	 * @brief	NCG file path
	 * <br>It can be a local path or HTTP URL. 
	 */
	public String mNcgFilePath;

	/**
	 * @brief	Token String
	 * <br>Token for license acquisition.
	 */
	public String mToken;

	/** 
	 * @brief	local download path of NCG file 
	 */
	public String mNcgFileDownloadPath = null;
	
	/** 
	 * @brief	NCG file name while download
	 * <br> It will be used to create temp folder and download folder.
	 */
	public String mNcgFileNameForDownloading;
	
	/** 
	 * @brief	flag for download completion 
	 * <br>It will be true when NCG file download is finished.
	 */
	public boolean mDownloadCompleted;
	
	/** 
	 * @brief	flag for DnP setting
	 * <br> It will be set by 'Play' button's event handler in DownloadProgressDialog class.
	 */
	public boolean mIsDnpExcuted;
	
	/** 
	 * @brief	flag for checking Activity's pause status
	 */
	public boolean mIsActivityPaused;
	
	/** 
	 * @brief	capsulated object for download task 
	 */
	public DownloadNcgFileTask mDownloadTask;
	
	/**
	 * @brief ProgressDialog
	 */
	public ProgressDialog mProgressDlg = null;
	
	/**
	 * @brief remote Url path for DnP
	 */
	public String mRemoteUrlForDnp;
	
	/**
	 * @brief reset variables
	 */
	public void reset() {
		mIsDnpExcuted = false; // resets DnP flag
		mRemoteUrlForDnp = "";
		mDownloadCompleted = false;
	}
	
	public boolean mIsSwCodecForced;
	public boolean mIsAds;
	public boolean mIsLive;
	public boolean mTmp;
}
