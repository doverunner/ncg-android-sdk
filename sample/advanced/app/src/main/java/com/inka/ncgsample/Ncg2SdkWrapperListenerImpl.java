package com.inka.ncgsample;

import java.util.HashMap;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.inka.ncg.jni.Ncg2CoreErrorCode;
import com.inka.ncg2.Ncg2Agent.LicenseValidation;
import com.inka.ncg2.Ncg2CencException;
import com.inka.ncg2.Ncg2LocalWebServer.WebServerListener;
import com.inka.ncg2.Ncg2SdkFactory;
import com.inka.ncg2.Ncg2ServerResponseErrorException;
import com.inka.ncgsample.Ncg2SdkWrapper.Ncg2SdkWrapperListener;

@SuppressLint({ "DefaultLocale", "NewApi" })
public class Ncg2SdkWrapperListenerImpl implements Ncg2SdkWrapperListener {

	private static final String TAG = "Ncg2SdkWrapperImple";
	private Context mContext;
	private Activity mActivity;
	private Handler mHandler;
	private Class<?> mPlayerClass;
	private boolean mIsErrorState;
	private StartPlayerActivityTask mStartPlayerActivityTask;
	private AlertDialog mAcquireLicenseDlg;

	public Ncg2SdkWrapperListenerImpl(Context context,
			Class<?> playerActivityCls, Activity activity) {
		mActivity = activity;
		mContext = context;
		mHandler = new Handler();
		mPlayerClass = playerActivityCls;
	}

	@Override
	public void onError(final Exception e, final String msg) {

		mHandler.post(new Runnable() {

			@Override
			public void run() {
				AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
				builder.setTitle(R.string.error_dialog_title);
				builder.setMessage(msg);
				builder.setPositiveButton(
						mActivity.getString(R.string.view_error), new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								showDialog(mActivity.getString(R.string.error_dialog_title), e.getMessage());
							}
						});
				builder.setNegativeButton(mActivity.getString(R.string.cancel),
						null);
				builder.show();

			}
		});
	}
	
	@Override
	public void onSecurityError(Exception e) {
		// TODO Auto-generated method stub
		AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
		builder.setTitle(R.string.error_dialog_title);
		builder.setMessage(mActivity.getString(R.string.security_read_phone_state_error));
		builder.setPositiveButton(mActivity.getString(R.string.confirm), new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				Ncg2SdkFactory.getNcgAgentInstance().release();
				mActivity.finish();
			}
		});
		
		Dialog dialog = builder.create();
		dialog.setCancelable(false);
		dialog.setCanceledOnTouchOutside(false);
		dialog.show();
	}

	@Override
	public void onAppFinishedError(Exception e) {
		// TODO Auto-generated method stub
		AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
		builder.setTitle(R.string.error_dialog_title);
		builder.setMessage(e.getMessage());
		builder.setPositiveButton(mActivity.getString(R.string.confirm), new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				Ncg2SdkFactory.getNcgAgentInstance().release();
				mActivity.finish();
			}
		});
		
		Dialog dialog = builder.create();
		dialog.setCancelable(false);
		dialog.setCanceledOnTouchOutside(false);
		dialog.show();
	}

	public void showDialog(String title, String msg) {
		AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
		builder.setTitle(R.string.error_dialog_title);
		builder.setMessage(msg);
		builder.setPositiveButton(mActivity.getString(R.string.ok), null);
		builder.show();
	}

	public void showUpdateSecureTimeDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
		builder.setTitle(mActivity.getString(R.string.error_dialog_title));
		builder.setMessage("You need to update secure time.\n"
				+ "Try again after checking online connection.");

		builder.setPositiveButton("Update SecureTime",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Ncg2SdkWrapper.getInstance().updateSecureTime();
					}
				});
		builder.setNegativeButton(mActivity.getString(R.string.cancel), null);
		builder.show();
	}

	@Override
	public void onInvalidNcgLicense(final String contentPath,
			final LicenseValidation lv,	final  String token) {
		mHandler.post(new Runnable() {

			@Override
			public void run() {

				switch (lv) {
				case NotExistLicense:
				case ExceededPlayCount:
				case ExpiredLicense:
					// In this case, We need to acqure licese
					break;

				case RootedDeviceDisallowed:
					Toast.makeText(mActivity, "Detected Rooting Device",
							Toast.LENGTH_LONG).show();
					return;

				case ExternalDeviceDisallowed:
					Toast.makeText(mActivity,
							"External Display device disallow",
							Toast.LENGTH_LONG).show();
					return;

				case DeviceTimeModified:
					Toast.makeText(mActivity, "Detected device time modified.",
							Toast.LENGTH_LONG).show();
					showUpdateSecureTimeDialog();
					return;

				case NotAuthorizedAppID:
					Toast.makeText(mActivity, "NotAuthorizedAppID",
							Toast.LENGTH_LONG).show();
					return;

				case OfflineNotSupported:
					Toast.makeText(mActivity, "OfflineNotSupported",
							Toast.LENGTH_LONG).show();
					return;

				case ScreenRecorderDetected:
					HashMap<String, String> data;
					data = lv.getExtraData();
					String appName = data.get("AppName");
					String packageName = data.get("AppPackageName");
					String msg = String
							.format("ScreenRecorderDetected : \nAppName : [%s]\nPackageName : [%s]",
									appName, packageName);
					Toast.makeText(mActivity, msg, Toast.LENGTH_LONG).show();
					return;

				case OfflineStatusTooLong:
					Toast.makeText(mActivity, "OfflineStatusTooLong",
							Toast.LENGTH_LONG).show();
					showUpdateSecureTimeDialog();
					return;

				case ValidLicense:
					Toast.makeText(
							mActivity,
							"onInvalidNcgLicense() : license is valid. incorrect logic.",
							Toast.LENGTH_LONG).show();
					return;

				case BeforeStartDate:
					Toast.makeText(mActivity,
							mActivity.getString(R.string.before_start_date),
							Toast.LENGTH_LONG).show();

					return;

				default:
					Toast.makeText(mActivity,
							"Unknown LicenseValidation : " + lv,
							Toast.LENGTH_LONG).show();
					return;
				}

				if(mAcquireLicenseDlg != null && mAcquireLicenseDlg.isShowing() ) {
					Log.d(TAG, "[onInvalidNcgLicense] AcquireLicenseDlg Already Displaed!");
					return;
				}
				
				//
				// Acquire License
				//
				AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
				if (lv == LicenseValidation.NotExistLicense) {
					builder.setMessage(R.string.aquire_license);
				} else if (lv == LicenseValidation.ExceededPlayCount) {
					builder.setTitle(R.string.renew_license_title);
					builder.setMessage(R.string.renew_license_of_count_info_msg);
				} else if (lv == LicenseValidation.ExpiredLicense) {
					builder.setTitle(R.string.renew_license_title);
					builder.setMessage(R.string.renew_license_of_period_info_msg);
				}

				builder.setNegativeButton(R.string.cancel, 
						new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						mAcquireLicenseDlg = null;
					}

				});
				builder.setPositiveButton(R.string.ok,
						new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {

//						Ncg2SdkWrapper.getInstance().acquireLicenseByToken(
//								mActivity, contentPath,	token);
						Ncg2SdkWrapper.getInstance().acquireLicense(
								mActivity, contentPath, token);
						mAcquireLicenseDlg = null;
					}
				});
				mAcquireLicenseDlg = builder.show();
			}
		});
	}

	@Override
	public void onCompletedUpdateSecureTime() {

		mHandler.post(new Runnable() {

			@Override
			public void run() {
				Toast.makeText(mActivity, "Succeeded. Please try again.",
						Toast.LENGTH_SHORT).show();
				
				((MainActivity)mActivity).refreshListViewData();
			}
		});
	}

	@Override
	public void onCompletedAcquireLicense(final String mFilePath, final String token) {

		mHandler.post(new Runnable() {

			@Override
			public void run() {
				Toast.makeText(mActivity, "Succeeded in acquiring a license",
						Toast.LENGTH_LONG).show();
				startPlayerActivity(mFilePath, token);
			}
		});
	}

	@Override
	public void onServerError(final Ncg2ServerResponseErrorException e,
			String string, final int serverErrorCode) {

		mHandler.post(new Runnable() {

			@Override
			public void run() {
				// 라이선스 요청 시 NCGERR_LICENSE_SERVER_RESPONSE_CERTIFICATE_ERROR_AND_DEVICEID_IS_WIDEVINE_UUID 가
				// 발생한 경우 Ncg2Agent.init()을 호출하거나, 앱을 재시작해야 한다.
				if (serverErrorCode == Ncg2CoreErrorCode.NCGERR_LICENSE_SERVER_RESPONSE_CERTIFICATE_ERROR_AND_DEVICEID_IS_WIDEVINE_UUID) {
					Ncg2SdkWrapper.getInstance().init(mContext, Ncg2SdkWrapperListenerImpl.this);
				}

				AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
				String errMsg = String
						.format("LicenseServer responses an error(errorcode:[%d]) \n"
								+ "You need to contact the server administrator.",
								serverErrorCode);

				builder.setTitle(R.string.error_dialog_title);
				builder.setMessage(errMsg);
				builder.setPositiveButton(
						mActivity.getString(R.string.view_error),
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								showDialog(
										mActivity
												.getString(R.string.error_dialog_title),
										e.getMessage());
							}
						});
				builder.setNegativeButton(mActivity.getString(R.string.cancel),
						null);
				builder.show();
			}
		});
	}

	public void startPlayerActivity(String contentPath, String token) {
		mIsErrorState = false; // reset an error status.

		if( mStartPlayerActivityTask != null ) {
			Log.d(TAG, "[startPlayerActivity] Task Already Executed");
			return;
		}
		
		mStartPlayerActivityTask = new StartPlayerActivityTask(contentPath, token);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			mStartPlayerActivityTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		} else {
			mStartPlayerActivityTask.execute();
		}
	}

	@Override
	public void onWebserverNotification(final int notifyCode,
			final String notifyMsg) {

		mHandler.post(new Runnable() {
			@Override
			public void run() {

				if (notifyCode == WebServerListener.LWS_NOTIFY_DNP_READ_FAIL_PLAY_ERROR
						|| notifyCode == WebServerListener.LWS_NOTIFY_HDMI_DETECTED
						|| notifyCode == WebServerListener.LWS_NOTIFY_SCREEN_RECORDER_DETECTED) {

					mIsErrorState = true;

					String msg = String.format(
							"NOTIFY CODE : [%d]\nNOTIFY MSG:[%s]\n",
							notifyCode, notifyMsg);
					try {
						AlertDialog.Builder builder = new AlertDialog.Builder(
								mActivity);
						builder.setCancelable(true);
						builder.setTitle("NCG Sample");
						builder.setMessage(msg);
						builder.setPositiveButton("OK",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int id) {
										Ncg2SdkFactory.getNcgAgentInstance().release();
										mActivity.finish();
									}
								});
						builder.show();
					} catch (Exception e) {
						Toast.makeText(mActivity, "onNotification : " + msg,
								Toast.LENGTH_LONG).show();
					}
				}
			}
		});
	}

	@Override
	public void onWebserverError(final int errorCode, final String errorMessage) {
		mHandler.post(new Runnable() {

			@Override
			public void run() {

				if (mIsErrorState) {
					return;
				}
				if (mActivity.isFinishing()) {
					return;
				}

				mIsErrorState = true;
				String msg = String.format(
						"ERROR CODE : [%d]\nERROR MSG:[%s]\n", errorCode,
						errorMessage);
				try {
					AlertDialog.Builder builder = new AlertDialog.Builder(
							mActivity);
					builder.setCancelable(true);
					builder.setTitle("NCG Sample");
					builder.setMessage(msg);
					builder.setPositiveButton("OK",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									Ncg2SdkFactory.getNcgAgentInstance().release();
									mActivity.finish();
								}
							});
					builder.show();
				} catch (Exception e) {
					Toast.makeText(mActivity, "onError : " + msg,
							Toast.LENGTH_LONG).show();
				}
			}
		});
	}

	class StartPlayerActivityTask extends AsyncTask<Void, Void, Void> {

		private boolean isSuccess;
		private String playbackUrl;
		private String token;
		private String smiFileUrl;
		private String contentPath;
		private ProgressDialog mProgressDlg;

		StartPlayerActivityTask(String contentPath, String token) {
			this.contentPath = contentPath;
			this.token = token;
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
			
			if (isSuccess) {
				Intent intent = new Intent(mContext, mPlayerClass);
				intent.putExtra("path", playbackUrl);
				smiFileUrl = "";
				String[] parts = contentPath.split("\\.");
				for (int i=0; i < parts.length; i++)
				{
					if (parts[i].startsWith("mp4"))
					{
						break;
					}
					
					smiFileUrl += parts[i] + ".";
				}				
				smiFileUrl += "smi";
				
				intent.putExtra("smiPath", smiFileUrl);
				
				mActivity.startActivity(intent);
			} else {
				// DO NOTHING.
			}
			mStartPlayerActivityTask = null;
			super.onPostExecute(result);
		}

		@Override
		protected Void doInBackground(Void... params) {

			playbackUrl = Ncg2SdkWrapper.getInstance().getPlaybackUrl(
					contentPath, Global.getInstance().mRemoteUrlForDnp,
					Global.getInstance().mNcgFileSizeForDnp,
					Global.getInstance().mToken);
			if (playbackUrl == null || playbackUrl.isEmpty()) {
				return null;
			}

			isSuccess = true;

			return null;
		}

	}

}
