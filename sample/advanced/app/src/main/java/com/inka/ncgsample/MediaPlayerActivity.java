package com.inka.ncgsample;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ads.interactivemedia.v3.api.AdDisplayContainer;
import com.google.ads.interactivemedia.v3.api.AdErrorEvent;
import com.google.ads.interactivemedia.v3.api.AdEvent;
import com.google.ads.interactivemedia.v3.api.AdsLoader;
import com.google.ads.interactivemedia.v3.api.AdsManager;
import com.google.ads.interactivemedia.v3.api.AdsManagerLoadedEvent;
import com.google.ads.interactivemedia.v3.api.AdsRequest;
import com.google.ads.interactivemedia.v3.api.ImaSdkFactory;
import com.google.ads.interactivemedia.v3.api.player.ContentProgressProvider;
import com.google.ads.interactivemedia.v3.api.player.VideoProgressUpdate;
import com.inka.ncg2.Ncg2Agent;
import com.inka.ncg2.Ncg2LocalWebServer;
import com.inka.ncg2.Ncg2LocalWebServer.WebServerListener;
import com.inka.ncg2.Ncg2SdkFactory;
import com.inka.ncgsample.DemoLibrary.DownloadNotifyHelper.OnDownloadEvent;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import com.inka.ncgsample.R;

/**
 * This class plays NCG contents by using Ncg2MediaPlayer class.<br> 
 */
@SuppressLint("") public class MediaPlayerActivity extends Activity {

	private static final String TAG = "Ncg2MediaPlayerActivity";
	public static final int REQUEST_PLAY_NORMAL = 1000;
	public static final int REQUEST_PLAY_DNP = 1001;
	
	private static boolean mIsForeground = false;
	
	private Global mGlobal = Global.getInstance();
	
	private DataSourceSetupTask mDataSourceSetupTask;
	private MediaPlayer mPlayer;	
	private SeekBar mSeekBar;
	ViewGroup m_mplayerHeader;
	ViewGroup m_mplayerFooter;		
	private String mStrMsg;	
	private GestureDetector mGestureDetector;
	private Timer mTimeCheckTimer;
		
	private TimerTask mTimeCheckTimerTask;
	private DisplayMode mCurrentDisplayMode;
	
	/**
	 * If local webserver error is detected Player error should be skiped to notify webserver error.
	 */
	private boolean mSkipErrorHandling;
	
	/**
	 * this flag becomes true after onPrepared callback. 
	 */
	private boolean mIsPrepared;
	/**
	 * NCG file's size is needed for DnP.
	 */
	private long mNcgFileSize;
	
	
	/**
	 * a flag to check download completion for DnP.
	 */
	private boolean mIsDownloadComplete = false;
	
	private int mDownloadPercent;
	
	
	/**
	 * latest playback time 
	 */
	private int mLastestPlaytime;	
	
	/**
	 * duration of content playback
	 */
	private int mPlayDuration;
	
	/**
	 * a currently expected end time.
	 * It is for expecting seekable range by checking downloaded size of DnP.
	 */
	private int mExpectedCurrentEndTime;
	
	private SurfaceView mSurfaceView;
	private SurfaceHolder mSurfaceHoder;
	
	/**
	 * SurfaceView is added to relevant Layout
	 */
	private FrameLayout mPlayerArea;

	// Factory class for creating SDK objects.
	private ImaSdkFactory mSdkFactory;
	// The AdsLoader instance exposes the requestAds method.
	private AdsLoader mAdsLoader;
	// AdsManager exposes methods to control ad playback and listen to ad events.
	private AdsManager mAdsManager;
	// Whether an ad is displayed.
	private boolean mIsAdDisplayed;
	// The play button to trigger the ad request.
	private View mPlayButton;
	// The container for the ad's UI.
	private ViewGroup mAdUiContainer;

	private AdEvent.AdEventListener adEventListener = new AdEvent.AdEventListener() {
		@Override
		public void onAdEvent(AdEvent adEvent) {
			Log.i(TAG, "Event: " + adEvent.getType());

			// These are the suggested event types to handle. For full list of all ad event
			// types, see the documentation for AdEvent.AdEventType.
			switch (adEvent.getType()) {
				case LOADED:
					// AdEventType.LOADED will be fired when ads are ready to be played.
					// AdsManager.start() begins ad playback. This method is ignored for VMAP or
					// ad rules playlists, as the SDK will automatically start executing the
					// playlist.
					mAdsManager.start();
					break;
				case CONTENT_PAUSE_REQUESTED:
					// AdEventType.CONTENT_PAUSE_REQUESTED is fired immediately before a video
					// ad is played.
					mIsAdDisplayed = true;
					mPlayer.pause();
					break;
				case CONTENT_RESUME_REQUESTED:
					// AdEventType.CONTENT_RESUME_REQUESTED is fired when the ad is completed
					// and you should start playing your content.
					mIsAdDisplayed = false;
					mPlayer.start();
					break;
				case ALL_ADS_COMPLETED:
					if (mAdsManager != null) {
						mAdsManager.destroy();
						mAdsManager = null;
					}
					break;
				default:
					break;
			}
		}
	};

	private AdErrorEvent.AdErrorListener adErrorListener = new AdErrorEvent.AdErrorListener() {
		@Override
		public void onAdError(AdErrorEvent adErrorEvent) {
			Log.e(TAG, "Ad Error: " + adErrorEvent.getError().getMessage());
			mPlayer.start();
		}
	};

	private OnDownloadEvent mDownloadEvent = new OnDownloadEvent() {
		
		@Override
		public void onProgress(int percent) {
			
			mDownloadPercent = percent;
			mExpectedCurrentEndTime = mDownloadPercent * mPlayDuration / 100;
			// Seek operation is likely to fail without some amount of gap setting.
			// Playback error will occur when seek operation fails, so it should be done within appropriate range.
			mExpectedCurrentEndTime -= (mPlayDuration / 100 * 3);   
			if( mExpectedCurrentEndTime < 0 ) {
				mExpectedCurrentEndTime = 0;
			}
			
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					mSeekBar.setSecondaryProgress( mPlayDuration / 100 * mDownloadPercent);
				}
			});
			
		}
		
		@Override
		public void onDownloadComplete() {
			// download complete
			Log.d(DemoLibrary.TAG, "[OnDownloadEvent] onDownloadComplete Called.");
			mIsDownloadComplete = true;
		}
	};
	
	/**
	 *  ACTION_USER_PRESENT action should be handled in case of unlocking screen
	 * 
	 * @author jypark
	 *
	 */
	private class UnlockReceiver extends BroadcastReceiver {  		  
	    @Override  
	    public void onReceive(Context context, Intent intent) {  
	        String action = intent.getAction();  
	        if (action.equals(Intent.ACTION_USER_PRESENT) || action.equals(Intent.ACTION_SCREEN_ON)) {
	        	if( mIsForeground ) {
					if (mDataSourceSetupTask == null) {
						mDataSourceSetupTask = new DataSourceSetupTask();
						mDataSourceSetupTask.execute((Void[]) null);
					}
				} else {
					if (mAdsManager != null && mIsAdDisplayed) {
						mAdsManager.resume();
					} else {
						mPlayer.start();
					}
				}
	        }
	    }  
	}
	
	UnlockReceiver mUnlockReceiver;
	
	/**
	 * 
	 */
	private String mPlaybackUrl;
	
	/**
	 * variable to save current position for Pause & Resume function 
	 */
	private int mCurrentPosition;
	
	
	@Override
	protected void onPause() {
		Log.d(DemoLibrary.TAG, "[Ncg2MediaPlayerActivity2] onPause() ++");
		super.onPause();

		mIsForeground = false;
		stopPosTimer();
		mCurrentPosition = mPlayer.getCurrentPosition();

		if (mAdsManager != null && mIsAdDisplayed) {
			mAdsManager.pause();
		} else {
			mPlayer.pause();
			mIsPrepared = false;
		}
	}

	@Override
	protected void onResume() {
		Log.d(DemoLibrary.TAG, "[Ncg2MediaPlayerActivity2] onResume() ++");
		super.onResume();
		mIsForeground = true;
	}
	
	private boolean isDownloadAndPlay() {
		if( mNcgFileSize != 0 ) {
			if( mIsDownloadComplete == false ) {
				return true;
			} else {
				return false;
			}
		}
		
		return false;		
	}
	
	private Ncg2LocalWebServer.WebServerListener mWebServerListener = new Ncg2LocalWebServer.WebServerListener(){

		@Override
		public void onNotification(final int notifyCode, final String notifyMsg) {

			if( MediaPlayerActivity.this.isFinishing() == false ) {
				runOnUiThread(new Runnable() {

					@SuppressLint("DefaultLocale") @Override
					public void run() {
			        	if( mPlayer.isPlaying() ) {
			        		mPlayer.stop();
			        	}
			        	if( notifyCode != WebServerListener.LWS_NOTIFY_HDMI_DETECTED ) {
			        		// In case of NCG_NOTIFY_HDMI_DETECTED, NCG SDK processes this notification internally.
			        		// So, Here, We don't neet to display msg again.
			        		String msg = String.format("NOTIFICATION CODE : [%d]", notifyCode);
							AlertDialog.Builder builder = new AlertDialog.Builder(MediaPlayerActivity.this);
							builder.setCancelable(true);
							builder.setTitle("NCG DRM SDK");
							builder.setMessage(msg);
							builder.setNegativeButton("OK", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int id) {
									dialog.dismiss();
									finish();
								}
							});
							builder.show();
			        	}
					}
				});
			}
		}

		
		@Override
		public void onError(final int errorCode, final String errorMessage) {
			
			
			
			runOnUiThread(new Runnable() {

				@SuppressLint("DefaultLocale") @Override
				public void run() {
					if( mSkipErrorHandling ) {
						return;
					}
					mSkipErrorHandling = true;
					if( isFinishing() ) {
						return;
					}
					else {
						if( mPlayer.isPlaying() ) {
			        		mPlayer.stop();
			        	}
					}
					String msg = String.format("ERROR CODE : [%d]\nERROR MSG:[%s]\n", errorCode, errorMessage);
					AlertDialog.Builder builder = new AlertDialog.Builder(MediaPlayerActivity.this);
					builder.setCancelable(true);
					builder.setTitle("NCG Sample");
					builder.setMessage(msg);
					builder.setNegativeButton("OK", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.dismiss();
							finish();
						}
					});
					builder.show();
				}
			});
		}

		@Override
		public PlayerState onCheckPlayerStatus(String uri) {
			if( MediaPlayerActivity.this.isFinishing() == false ) {				
				try {
					//mPlayer.isPlaying();
					Log.i(DemoLibrary.TAG, true+"");
					Log.i(DemoLibrary.TAG, mPlayer.getCurrentPosition()+"");
					playerIsPlaying();
					mPlayer.getCurrentPosition();
				}
				catch(IllegalStateException e) {
					Log.e(DemoLibrary.TAG, "[onCheckPlayerStatus] IllegalStateException exception occured!");
					e.printStackTrace();
					return PlayerState.Fail;
				}
				catch(Exception e) {
					Log.e(DemoLibrary.TAG, "[onCheckPlayerStatus] Unknown exception occured!");
					e.printStackTrace();
					return PlayerState.Fail;
				}
			}
			return PlayerState.ReadyToPlay;
		}
	};
	boolean playerIsPlaying(){
		return true;
	}
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(DemoLibrary.TAG, "[Ncg2MediaPlayerActivity] onCreate() ++");
		
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		super.onCreate(savedInstanceState);
		
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.layout_mplayer);				
		
		Intent intent = getIntent();		
		mPlaybackUrl = intent.getStringExtra("path");
		if( mPlaybackUrl == null || mPlaybackUrl.length() == 0 ) {
			throw new RuntimeException("'path' param must be provided.");		
		}
				
		DemoLibrary.getDownloadNotifier().registerDownloadCompleteEvent(mDownloadEvent);
		
		Log.i(TAG, "[onCreate] path : " + mPlaybackUrl);						
		
		mPlayer = new MediaPlayer();
		mPlayer.setOnCompletionListener(mCompletionListener);
		mPlayer.setOnErrorListener(mErrorListener);
		mPlayer.setOnPreparedListener(mPreparedListener);
		mPlayer.setOnSeekCompleteListener(mSeekCompleteListener);
		
		Ncg2Agent ncgAgent = Ncg2SdkFactory.getNcgAgentInstance();
		Ncg2LocalWebServer ncgLocalWebServer = ncgAgent.getLocalWebServer();
		ncgLocalWebServer.setEnableUserAgentChecking(true);
		
		mPlayerArea = (FrameLayout)findViewById(R.id.frm_player_area);						
		// SurfaceView creation
		RelativeLayout relativeLayout = new RelativeLayout(this);
		RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.MATCH_PARENT,
				RelativeLayout.LayoutParams.MATCH_PARENT); 
		relativeLayout.setLayoutParams(layoutParams);		
		mPlayerArea.addView( relativeLayout, 0 );
		
		mSurfaceView = new SurfaceView( this );
		if( android.os.Build.VERSION.SDK_INT >= 17)
			mSurfaceView.setSecure(true);
		relativeLayout.addView(mSurfaceView);
		mSurfaceHoder = mSurfaceView.getHolder();
		mSurfaceHoder.addCallback(mSurfaceHolderCallback);
		mSurfaceHoder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		mSurfaceHoder.setKeepScreenOn( true );				
		mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);		

		initUI();
		registerActionEvent();
		initAds();
	}
	
	private void registerActionEvent() {
		IntentFilter unlockReceiverfilter = new IntentFilter();  
        unlockReceiverfilter.addAction(Intent.ACTION_USER_PRESENT);  
        unlockReceiverfilter.addAction(Intent.ACTION_SCREEN_ON);
        mUnlockReceiver = new UnlockReceiver();  
        registerReceiver(mUnlockReceiver, unlockReceiverfilter);		
	}
	
	private MediaPlayer.OnCompletionListener mCompletionListener = new MediaPlayer.OnCompletionListener() {

		@Override
		public void onCompletion(MediaPlayer ncgPlayer) {
			stopPosTimer();
			finish();
		}
	};
	
	private MediaPlayer.OnErrorListener mErrorListener = new MediaPlayer.OnErrorListener() {
		
		@Override
		public boolean onError(MediaPlayer ncgPlayer, int what, int extra) {
			if( MediaPlayerActivity.this.isFinishing() || mSkipErrorHandling ) {
				return true;
			}
			
			final int errorCode = what;
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					Toast.makeText(MediaPlayerActivity.this,
							"[onError] Error Occured. ErrorCode: " + Integer.toHexString(errorCode), Toast.LENGTH_LONG).show();
					if( mPlayer.isPlaying() ) {
						mPlayer.stop();
					}
					else {
						mPlayer.reset();
						mIsPrepared = false;
					}
				}
			});
			return false;
		}
	};
	
	private MediaPlayer.OnPreparedListener mPreparedListener = new MediaPlayer.OnPreparedListener() {

		@Override
		public void onPrepared(MediaPlayer ncgPlayer) {
			
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					mPlayer.setScreenOnWhilePlaying( true );
					mPlayDuration = mPlayer.getDuration();
					mSeekBar.setMax( mPlayDuration );
					mSeekBar.setProgress(mCurrentPosition);
					mSeekBar.setSecondaryProgress(0);
					mPlayer.seekTo(mCurrentPosition);

					mPlayer.start();
					if(mGlobal.mIsAds == true) {
						requestAds(getString(R.string.ad_tag_url));
					}

					mCurrentDisplayMode = DisplayMode.FullScreenWithKeepRatio;
					setDisplayMode(mCurrentDisplayMode);
				}
			});
			
			Log.d(DemoLibrary.TAG, "[Ncg2MediaPlayerActivity2] onPrepared() ++");
			startPosTimer();
			mIsPrepared = true;
		}	
	};
	private int mScreenWidth;
	private int mScreenHeight;
	private int mVideoWidth;
	private int mVideoHeight;
	
	
	
	
	@SuppressWarnings("deprecation")
	public void setDisplayMode(DisplayMode mode) {
		Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
				.getDefaultDisplay();
		mScreenWidth = display.getWidth(); 
		mScreenHeight = display.getHeight();
		int	displayWidth = 0;
		int	displayHeight = 0;
		float scale = 1.0f;
		
		// mVideoWidth 와 mVideoHeight 값은 OnVideoSizeChanged에서 설정되어야 하는데
		// 만약 OnVideoSizeChanged가 호출되지 않을 경우를 대비하여 값을 확인하여 적절히 처리해준다.
		if( mVideoWidth == 0 || mVideoHeight == 0 ) { 
			mVideoWidth = mPlayer.getVideoWidth();
			mVideoHeight = mPlayer.getVideoHeight();
			if( mVideoWidth == 0 || mVideoHeight == 0 ) {
				mVideoWidth = mScreenWidth;
				mVideoHeight = mScreenHeight;
			}
		}
		scale = Math.min((float) mScreenWidth / (float) mVideoWidth, (float) mScreenHeight / (float) mVideoHeight);
		switch(mode) {
		case OriginalContentSize:
			displayWidth = mVideoWidth;
			displayHeight = mVideoHeight;
			break;
			
		case OriginalContentSizeButNotExceed:
			if( scale < 1.0f ) {
				displayWidth = (int) (mVideoWidth * scale);
				displayHeight = (int) (mVideoWidth * scale);
			} else {
				displayWidth = mVideoWidth;
				displayHeight = mVideoHeight;
			}
			break;
			
		case FullScreenWithKeepRatio:
			displayWidth = (int) (mVideoWidth * scale);
			displayHeight = (int) (mVideoHeight * scale);
			break;
			
		case FitToScreen:
			displayWidth = mScreenWidth;
			displayHeight = mScreenHeight;
			break;
			
		default:
			new RuntimeException("Unrecognized DisplayMode : " + mode);
		}
		
		RelativeLayout.LayoutParams rl = ( RelativeLayout.LayoutParams ) mSurfaceView.getLayoutParams();
		rl.addRule( RelativeLayout.CENTER_IN_PARENT );
		rl.width  = displayWidth;
		rl.height = displayHeight;
		mSurfaceView.setLayoutParams( rl );		
		mSurfaceView.setVisibility( View.VISIBLE );
		mSurfaceHoder.setFixedSize( displayWidth, displayHeight );
	}
	private MediaPlayer.OnSeekCompleteListener mSeekCompleteListener = new MediaPlayer.OnSeekCompleteListener() {

		@Override
		public void onSeekComplete(MediaPlayer ncgPlayer) {
			Log.d(TAG, "[mSeekCompleteListener] onSeekComplete()");
			startPosTimer();
		}
	};
	

	private SurfaceHolder.Callback mSurfaceHolderCallback = new SurfaceHolder.Callback() {
		
		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			Log.d(DemoLibrary.TAG, "[Ncg2MediaPlayerActivity2] surfaceDestroyed() ++");
		}
		
		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			Log.d(DemoLibrary.TAG, "[Ncg2MediaPlayerActivity2] surfaceCreated() ++");
			
			mPlayer.setDisplay(mSurfaceHoder);
			if(mDataSourceSetupTask == null) {
				mDataSourceSetupTask = new DataSourceSetupTask();
				mDataSourceSetupTask.execute();
			} else {
				if (mAdsManager != null && mIsAdDisplayed) {
					mAdsManager.resume();
				} else {
					mPlayer.start();
				}
			}
		}
		
		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width,
				int height) {	
			Log.d(DemoLibrary.TAG, "[Ncg2MediaPlayerActivity2] surfaceChanged() ++ [" + width + "], [" + height + "]");			
			
		}
	};

	OnClickListener mButtonClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			int timePos;
			
			if( mIsPrepared == false ) {
				Log.d(DemoLibrary.TAG,"You need to complete the prepre task before this call.");
				return;
			}

			int id = v.getId();
			if (id == R.id.btn_advance) {
				timePos = mPlayer.getCurrentPosition() + 10000;
				if (timePos > mPlayer.getDuration()) {
					timePos = mPlayer.getDuration();
				}
				seekTo(timePos);
				resetControllHideCount();
			} else if (id == R.id.btn_retour) {
				timePos = mPlayer.getCurrentPosition() - 10000;
				if (timePos < 0) {
					timePos = 0;
				}
				seekTo(timePos);
				resetControllHideCount();
			} else if (id == R.id.btn_playnpause) {
				if (mPlayer != null) {
					if (mPlayer.isPlaying()) {
						mPlayer.pause();
					} else {
						mPlayer.start();
					}
					resetControllHideCount();
				}
			}
		}
	};
	
	@Override
	protected void onDestroy() {
		if (mUnlockReceiver != null) {  
            unregisterReceiver(mUnlockReceiver);  
            mUnlockReceiver = null;  
        }
		
		stopPosTimer();
		if( mPlayer.isPlaying() ) {
			mPlayer.stop();
		}
		mPlayer.release();
		DemoLibrary.getDownloadNotifier().ungisterDownloadCompleteEvent(mDownloadEvent);
		
		Ncg2Agent ncgAgent = Ncg2SdkFactory.getNcgAgentInstance();
		Ncg2LocalWebServer ncgLocalWebServer = ncgAgent.getLocalWebServer();
		ncgLocalWebServer.clearPlaybackUrls();
		DemoLibrary.getNcgAgent().removeAllTemporaryLicense();
		super.onDestroy();		
	}
	
	@Override
	public void onBackPressed() {
		stopPosTimer();
		if( mPlayer.isPlaying() ) {
			mPlayer.stop();
		}		
		
		super.onBackPressed();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	private void initAds() {
		// Create an AdsLoader.
		mSdkFactory = ImaSdkFactory.getInstance();
		mAdsLoader = mSdkFactory.createAdsLoader(this);
		// Add listeners for when ads are loaded and for errors.
		mAdsLoader.addAdErrorListener(adErrorListener);
		mAdsLoader.addAdsLoadedListener(new AdsLoader.AdsLoadedListener() {
			@Override
			public void onAdsManagerLoaded(AdsManagerLoadedEvent adsManagerLoadedEvent) {
				// Ads were successfully loaded, so get the AdsManager instance. AdsManager has
				// events for ad playback and errors.
				mAdsManager = adsManagerLoadedEvent.getAdsManager();

				// Attach event and error event listeners.
				mAdsManager.addAdErrorListener(adErrorListener);
				mAdsManager.addAdEventListener(adEventListener);
				mAdsManager.init();
			}
		});
	}

	@SuppressLint("ClickableViewAccessibility") private void initUI() {
		mSeekBar = ( SeekBar ) findViewById( R.id.posSeekBar );
		mSeekBar.setOnSeekBarChangeListener( mOnSeekBarChangeListener );
		
		mGestureDetector = new GestureDetector( this, mNullListener);    	// create double tap gesture
		mGestureDetector.setOnDoubleTapListener(mDoubleTapListener);      	// register double tap listener
		
		View rootViewGroup = findViewById(R.id.mplayerArea);
		rootViewGroup.setOnTouchListener( new OnTouchListener(){
			@Override public boolean onTouch(View v, MotionEvent event) {
				if( mGestureDetector != null)
					mGestureDetector.onTouchEvent(event);    // only double tap gesture is used
				return true;
			}
        });
		
		m_mplayerHeader = ( ViewGroup ) findViewById( R.id.mplayerHeader );
		m_mplayerFooter = ( ViewGroup ) findViewById( R.id.mplayerFooter );
		
		TextView title = ( TextView ) m_mplayerHeader.findViewById( R.id.mplayerTitle );
		if( title != null ) {
			title.setText( DemoLibrary.getFilenameFromFilePath( Global.getInstance().mNcgFilePath ));
		}		
		
		findViewById( R.id.btn_advance ).setOnClickListener( mButtonClickListener );
		findViewById( R.id.btn_retour ).setOnClickListener( mButtonClickListener );		   			
		findViewById( R.id.btn_playnpause ).setOnClickListener( mButtonClickListener );

		mAdUiContainer = (ViewGroup) rootViewGroup;

		updateUserMessage("Connecting...");
	}
	
	private boolean mIsSeekBarTrackingNow = false;
	OnSeekBarChangeListener mOnSeekBarChangeListener = new OnSeekBarChangeListener() {
		
		
		@Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
			resetControllHideCount();
			if( isDownloadAndPlay() ) {
				Log.d(TAG, "[onProgressChanged] progress : [" + progress + "], LastestPlayTime : [" + getSeekableEndTime() + "]");
				
				if( fromUser == true && progress > getSeekableEndTime() ) {
					seekBar.setProgress( getSeekableEndTime() );
				}
			}
		}
		
		@Override public void onStartTrackingTouch(SeekBar seekBar) { 
			mIsSeekBarTrackingNow = true;
		}
		
		@Override public void onStopTrackingTouch(SeekBar seekBar) {
			mIsSeekBarTrackingNow = false;
			Log.d(TAG, "onStopTrackingTouch ++ ");
			int nSeekPos = seekBar.getProgress();			
			seekTo(nSeekPos);						
			resetControllHideCount();			
			Log.d(TAG, "onStopTrackingTouch -- ");
		}
	};	

	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		Log.d(TAG, "TouchEvent: " + event.getAction());
		return super.onTouchEvent(event);
	}
	
	
	private int m_player_cnt_hide_count = 10;
	private int m_player_count = 0;
	
	public void resetControllHideCount() {
		m_player_count = 0;
	}
	
	public void incrementControllHideCount() {
		if( View.VISIBLE == m_mplayerHeader.getVisibility()) {
			m_player_count++;
			if( m_player_count > m_player_cnt_hide_count ) {
				m_player_count = 0;
				
				if( View.GONE != m_mplayerHeader.getVisibility()) {
					m_mplayerHeader.setVisibility( View.GONE );
					m_mplayerFooter.setVisibility( View.GONE );
				}				
			}
		}
	}	
	
	/**
	 * stop timer for playback position
	 */
	private void stopPosTimer(){
		if( mTimeCheckTimer != null ) {
			mTimeCheckTimer.cancel();
		}
		mTimeCheckTimer = null;
		mTimeCheckTimerTask = null;
	}
	
	/**
	 * timer to update playing time 
	 */
	private void startPosTimer() {
		stopPosTimer();
		
		if( mTimeCheckTimer == null ) {
			mTimeCheckTimer = new Timer();
		}
		if( mTimeCheckTimerTask == null ) {
			mTimeCheckTimerTask = new TimerTask(){
				@Override public void run() {
					mLastestPlaytime = Math.max(mLastestPlaytime, mPlayer.getCurrentPosition());
					updatePlayingTime(mPlayer.getCurrentPosition());
				}
			};
		}
		
		mTimeCheckTimer.schedule( mTimeCheckTimerTask, 10, 500 );
	}
	
	/**
	 * update playing time
	 * @param millisecond
	 */
	@SuppressLint("DefaultLocale") private void updatePlayingTime(int millisecond) {
		//PreparedListener에서 Player의 Duration을 0으로 가져오는 경우가 있다.
		//이런 경우에는 플레이어가 재생이 되는 시점에서 다시 한번 Duration을 가져와서 SeekBar에 설정한다.
		if(mPlayDuration == 0){
			mPlayDuration = mPlayer.getDuration();
			mSeekBar.setMax( mPlayDuration );
		}
		final int currentMSec = millisecond;
		runOnUiThread(new Runnable() {

			@SuppressLint("DefaultLocale") @Override
			public void run() {

				if( mIsSeekBarTrackingNow == false ) {
					mSeekBar.setProgress(currentMSec);
				}
				try {
					TextView txtProgress = (TextView) findViewById(R.id.posTextView);
					if( txtProgress != null ) {
						int nTemp 	= mPlayer.getCurrentPosition() / 1000;

						int nHour 	= nTemp / 3600; 	nTemp -= nHour * 3600;
						int nMin	= nTemp / 60;		nTemp -= nMin * 60;
						int nSec	= nTemp;

						String strTimeInfo = String.format("%02d:%02d:%02d", nHour, nMin, nSec );
						txtProgress.setText( strTimeInfo );
					}

					txtProgress = (TextView) findViewById(R.id.durTextView);
					if( txtProgress != null ) {
						int nTemp 	= (mPlayer.getDuration() / 1000) - (mPlayer.getCurrentPosition() / 1000);

						int nDurHour 	= nTemp / 3600; 	nTemp -= nDurHour * 3600;
						int nDurMin		= nTemp / 60;		nTemp -= nDurMin * 60;
						int nDurSec		= nTemp;

						String strTimeInfo = String.format("-%02d:%02d:%02d", nDurHour, nDurMin, nDurSec );
						txtProgress.setText( strTimeInfo );
					}

					incrementControllHideCount();


				} catch (Throwable e) {
					e.printStackTrace();
				}

			}
		});
	}
	
	private void updateUserMessage(String strMsg) {
		mStrMsg = strMsg;
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				try {
					TextView txtProgress = (TextView) findViewById(R.id.posTextView);
					txtProgress.setText(mStrMsg);

				} catch (Throwable e) {
					e.printStackTrace();
				}

			}
		});
	}
	
	protected OnGestureListener mNullListener = new OnGestureListener() {
		 @Override public boolean onSingleTapUp(MotionEvent e) { return false; }
		 @Override public void onShowPress(MotionEvent e) {}
		 @Override public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) { return false; }
		 @Override public void onLongPress(MotionEvent e) {}
		 @Override public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) { return false; }
		 @Override public boolean onDown(MotionEvent e) { return false; }
	 };
	 
	 protected OnDoubleTapListener mDoubleTapListener = new OnDoubleTapListener() {
		
		@Override
		public boolean onSingleTapConfirmed(MotionEvent e) {
			if (e.getAction() == MotionEvent.ACTION_DOWN) {
				
				if( m_mplayerHeader.getVisibility() == View.GONE ) {
					m_player_count = 0;
					
					m_mplayerHeader.setVisibility( View.VISIBLE );
					m_mplayerFooter.setVisibility( View.VISIBLE );
					
				} else {
					m_mplayerHeader.setVisibility( View.GONE );
					m_mplayerFooter.setVisibility( View.GONE );										
				}
			}			
			
			return true;
		}
		
		@Override
		public boolean onDoubleTapEvent(MotionEvent e) {			
			return false;
		}
		
		@Override
		public boolean onDoubleTap(MotionEvent e) {
			switchDisplayMode();
			return true;
		}
	};
	
	private void switchDisplayMode() {
		switch( mCurrentDisplayMode ) {
		case FitToScreen:
			mCurrentDisplayMode = DisplayMode.FullScreenWithKeepRatio;
			break;
			
		case FullScreenWithKeepRatio:
			mCurrentDisplayMode = DisplayMode.OriginalContentSize;
			break;
			
		case OriginalContentSize:
			mCurrentDisplayMode = DisplayMode.FitToScreen;
			break;
			
		default:
			mCurrentDisplayMode = DisplayMode.FitToScreen;
			break;
		}			
		
		setDisplayMode(mCurrentDisplayMode);
	}
	
	private void seekTo(int timePos) {
		if( mIsPrepared == false ) {
			Log.d(DemoLibrary.TAG,"You need to complete the prepre task before this call.");			
			return;
		}
		if( isDownloadAndPlay() == true && getSeekableEndTime() < timePos ) {
			timePos = getSeekableEndTime();
		}
		
		mPlayer.seekTo( timePos );
	}
	
	/**
	 * returns seekable end position of content
	 * @return
	 */
	private int getSeekableEndTime() {		
		return mExpectedCurrentEndTime;
	}
	
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		setDisplayMode(mCurrentDisplayMode);
	}
	
	public enum DisplayMode {
		/**
		 * @if KOREA
		 * 컨텐츠의 원래 크기대로 화면에 나오도록 한다. 
		 * @endif
		 * 
		 * @if ENGLISH
		 * It makes to let it come out to screen as the original sized of content. 
		 * @endif
		 */
		OriginalContentSize,
		
		/**
		 * @if KOREA
		 * 컨텐츠의 원래 크기대로 화면에 나오도록 한다. 컨텐츠의 원래 크기가 화면보다 클 경우
		 * 화면크기에 컨텐츠크기를 조절하고,
		 * 그렇지 않을 경우에는 원본크기를 보여준다.
		 * @endif
		 * 
		 * @if ENGLISH
		 * It controls content sized on screen size when the original sized of content is bigger than screen. And it shows the original sized if it does not. 
		 * @endif
		 */
		OriginalContentSizeButNotExceed,
		
		/**
		 * @if KOREA
		 * 컨텐츠의 원래 크기대로 화면에 나오도록 한다. 디바이스의 화면크기에 컨텐츠크기를 조절한다.
		 * 해당 모드는 컨텐츠 가로/세로 비율이 유지된다. 
		 * @endif
		 * 
		 * @if ENGLISH
		 * It controls content sized on screen sized of device. And the relevant mode keeps a horizontal to vertical ratio of content.
		 * @endif 
		 */
		FullScreenWithKeepRatio,
		
		/**
		 * @if KOREA
		 * 컨텐츠의 원래 크기대로 화면에 나오도록 한다. 디바이스의 화면전체를 영상컨텐츠로 채운다.
		 * @endif
		 * 
		 * @if ENGLISH
		 * It fills the whole screen with video content of device.
		 * @endif
		 */
		FitToScreen
	}
	
	class DataSourceSetupTask extends AsyncTask<Void, Void, Void> {
		String errorMsg;
		
		@Override
		protected void onPreExecute() {
			updateUserMessage("Connecting...");
			super.onPreExecute();
		}

		@Override
		protected Void doInBackground(Void... params) {
			
			try {
				mPlayer.setDataSource(mPlaybackUrl);
				mPlayer.prepareAsync();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
				errorMsg = e.getMessage();
			} catch (IllegalStateException e) {
				e.printStackTrace();
				errorMsg = e.getMessage();
			} catch (IOException e) {
				e.printStackTrace();
				errorMsg = e.getMessage();
			}
			
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			if( errorMsg != null ) {
				Toast.makeText(MediaPlayerActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
			}
			super.onPostExecute(result);
		}		
	}

	private void requestAds(String adTagUrl) {
		AdDisplayContainer adDisplayContainer = mSdkFactory.createAdDisplayContainer();
		adDisplayContainer.setAdContainer(mAdUiContainer);

		// Create the ads request.
		AdsRequest request = mSdkFactory.createAdsRequest();
		request.setAdTagUrl(adTagUrl);
		request.setAdDisplayContainer(adDisplayContainer);
		request.setContentProgressProvider(new ContentProgressProvider() {
			@Override
			public VideoProgressUpdate getContentProgress() {
				if (mIsAdDisplayed || mPlayer == null || mPlayer.getDuration() <= 0) {
					return VideoProgressUpdate.VIDEO_TIME_NOT_READY;
				}
				return new VideoProgressUpdate(mPlayer.getCurrentPosition(),
						mPlayer.getDuration());
			}
		});

		// Request the ad. After the ad is loaded, onAdsManagerLoaded() will be called.
		mAdsLoader.requestAds(request);
	}
}

