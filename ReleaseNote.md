> **<font color="blue">This product supports Android 5.0(API Level 21) or later </font>**

### **Version 2.16.1**

> **What's New in Version 2.16.1**

<font color="orangered">**`Changed`**</font> Improves the performance of the open() function of the NcgEpubFile class.

- If there are A1.ncg, A2.ncg, A3.ncg, ... etc. inside the AAA.epub file, it is faster to read the A2 and A3 files because the DRM header is read only when the A1.ncg file is opened for the first time.

### **Version 2.16.0**

> **What's New in Version 2.16.0**

<font color="darkviolet">**`Added`**</font> Added Builder class.

- Users can now use the Builder class to create Ncg2Agent objects when they create them.

<font color="darkviolet">**`Added`**</font> Added Ncg2Agent functions.

- New functions using LicenseConfig have been added, including the ability to request licenses without using AsyncTask.
  - void acquireLicense(LicenseConfig, Callback<Void>, Callback<Ncg2Exception>)
  - void removeLicense(LicenseConfig)
  - void checkLicenseValid(LicenseConfig)
  - void getLicenseInfo(LicenseConfig)
  - void updateSecureTime(Callback<Void>, Callback<Ncg2Exception>)

<font color="orangered">**`Changed`**</font> Internal library enhancements and performance improvements.

- Fixed a crash in the registerReceiver() function during NCG init().

<font color="orangered">**`Changed`**</font> Change from DoxyGen documentation to javadoc documentation.
<font color="orangered">**`Changed`**</font> The library distribution method has been changed to AARs.

### **Version 2.15.0**

> **What's New in Version 2.15.0**

<font color="darkviolet">**`Added`**</font> Added LicenseConfig class.

 - The LicenseConfig class allows users to set the required parameters when requesting a license.

### **Version 2.14.1**

> **What's New in Version 2.14.1**

<font color="darkslategray">**`Removal`**</font> NcgZipArchive related classes have been removed.

- The NcgZipArchive, NcgZipCore, NcgZipEntry, NcgZipException, and NcgZipInputStream classes have been removed due to speed and performance issues.
- Among the library so files, libPocoFoundation.so and libPocoZip.so have been removed.
- For customers using NcgZipArchive related features, please use NcgEpubFile from now on.
- 
### **Version 2.14.0**

> **What's New in Version 2.14.0**

<font color="darkviolet">**`Added`**</font> Added the NcgEpubFile class.

- The NcgEpubFile class allows you to quickly consume a series of NCG contents with the same CID.
- **Ncg2Agent.createNcgEpub()** function to create an object.
- NcgEpubFile
  - void release()
  - void open(String path)
  - void prepare()
  - void close()
  - long read(byte[] buff, long sizeToRead)
  - void seek(long offset, NcgEpubFile.SeekMethod seekMethod)
  - long getCurrentFilePointer()
  - int getHeaderSize()
  - InputStream getInputStream()
  - int decreasePlayCount()

### **Version 2.13.0**

> **What's New in Version 2.13.0**

<font color="darkviolet">**`Added`**</font> You can register callbacks for Local File

- LocaFileCallback allows you to register callbacks to access and use local files internally in the SDK, allowing for intermediate processing in your app.
- You can register a callback using the **setLocalFileCallback(LocalFileCallback callback)** function.
- LocalFileCallback
  - Boolean fileOpen(String filePath, String openMode, int cloneNum)
  - void fileClose(int cloneNum)
  - byte[] fileRead(long numBytes, int cloneNum)
  - long fileWrite(byte[] data, long numBytes, int cloneNum)
  - long getFileSize(int cloneNum)
  - long setFilePointer(long distanceToMove, int moveMethod, int cloneNum)

### **Version 2.12.2**

> **What's New in Version 2.12.2**

<font color="darkviolet">**`Added`**</font> New parameter for the init function.

- init(Context context, OfflineSupportPolicy policy, String deviceID, String roDbPath, **String prefixPreferenceName**, boolean isFirstCallAfterBoot)
- From now on, prefixPreferenceName has been added to the init function parameters.
- The prefixPreferenceName parameter allows you to change the SharedPreference name being used inside the SDK.
- However, if you set prefixPreferenceName in init, you need to set prefixPreferenceName when using DeviceManager.

<font color="darkviolet">**`Added`**</font> Added Builder pattern when creating DeviceManager

- From now on, you can create it using the Builder pattern, and you can set the prefixName.
- If you set the prefixPreferenceName in init, you need to set the same prefixName when using DeviceManager.

### **Version 2.12.1**

> **What's New in Version 2.12.1**

<font color="darkviolet">**`Added`**</font> Ncg2DeviceIdException has been added.

- Occurs when there is a problem with the state of the device when generating the Device ID inside the NCG (reboot the device and restart MediaDrm).

### **Version 2.12.0**

> **What's New in Version 2.12.0**

<font color="darkviolet">**`Added`**</font> Now support epub (zip).

- Supports the zip.ncg file, so you can use it without decrypting the zip file.

```java
NcgZipArchive ncgZipArchive = createNcgZipArchive();
ncgZipArchive.open("file path"); // file open

NcgZipEntry zipEntry = ncgZipArchive.findEntry("entry name"); // get NcgZipEntry for entry name in zip file.
InputStream in = zipEntry.getInputStream(); // get InputStream for file in zip.

ncgZipArchive.getZipEntries(); // get all files in zip.

ncgZipArchive.close(); // file close
```

### **Version 2.11.2**

> **What's New in Version 2.11.2**

<font color="darkviolet">**`Added`**</font> An API to set whether to allow emulators during development is added.

```java
Ncg2Agent - enableVirtualMachine()
Ncg2Agent - disableVirtualMachine()
```

### **Version 2.11.1**

> **What's New in Version 2.11.1**

<font color="green">**`Update`**</font> The download storage location is changed in the sample app (Scoped Storage compatible).

### **Version 2.11.0**

> **What's New in Version 2.11.0**

<font color="royalblue">**`Fixed`**</font> `NCG Core` bug fixes and stability improvements.

### **Version 2.10.3**

> **What's New in Version 2.10.3**

<font color="royalblue">**`Fixed`**</font> In case of license issuance error (`7003`) and `Device ID` is `Widevine UUID`, `Device ID` and `RODB` are initialized.

- The stored license information is deleted by initialization of `RODB`, and `Ncg2Agent.init()` function must be called.

  ```java
  // Occurs when the license issuance error NCGERR_LICENSE_SERVER_RESPONSE_CERTIFICATE_ERROR and
  // the Device ID is a Widevine UUID. In this case, Ncg2Agent.Init()must be called again.
  Ncg2CoreErrorCode.NCGERR_LICENSE_SERVER_RESPONSE_CERTIFICATE_ERROR_AND_DEVICEID_IS_WIDEVINE_UUID
  ```

- Please refer to the example in the `Ncg2SdkWrapperListenerImpl.java` file.

  ```java
  	@Override
  	public void onServerError(final Ncg2ServerResponseErrorException e,
  			String string, final int serverErrorCode) {

  		mHandler.post(new Runnable() {

  			@Override
  			public void run() {
  				if (serverErrorCode == Ncg2CoreErrorCode.NCGERR_LICENSE_SERVER_RESPONSE_CERTIFICATE_ERROR_AND_DEVICEID_IS_WIDEVINE_UUID) {
  					Ncg2SdkWrapper.getInstance().init(mContext, Ncg2SdkWrapperListenerImpl.this);
        }

        ...

     }
  }
  ```

### **Version 2.10.2**

> **What's New in Version 2.10.2**

<font color="orangered">**`Changed`**</font> Changed to `SqlCipher` library built with `Android 11` target and version `4.4.3`.

- The `SqlCipher` version has changed so you must add `androidx.sqlite:sqlite` dependency to your app module `build.gradle`.

  ```shell
  // Android App build.gradle
  dependencies {
   	implementation "androidx.sqlite:sqlite:2.1.0"
  }
  ```

<font color="royalblue">**`Fixed`**</font> The buffer size for downloading `m3u8` files is changed.

### **Version 2.10.1**

> **What's New in Version 2.10.1**

<code><font color="royalblue">Fixed</font></code> Fixed to release `MediaDrm`.

### **Version 2.10.0**

> **What's New in Version 2.10.0**

<font color="DarkGreen">**`Improve`**</font> `Widevine UUID` generation logic is improved.

- The `Widevine UUID` generation logic and the generated `MediaDrm` release timing are improved.

### **Version 2.9.0**

> **What's New in Version 2.9.0**

<font color="darkviolet">**`Added`**</font> Http Live Streaming content key file protection logic is added.

- A logic has been added to prevent the leakage of HLS content keys by accessing the local web server.

### **Version 2.8.0**

> **What's New in Version 2.8.0**

<font color="darkviolet">**`Added`**</font> The `getTokenInfo()`function is added.

- Check the given `Token` information through `getTokenInfo()` function.

```java
public abstract class Ncg2Agent {
    public abstract PallyconTokenInfo getTokenInformation(String token) throws Ncg2Exception;
}
```

<font color="green">**`Update`**</font> The Sample is updated to acquire a license with `Token`

### **Version 2.7.0**

> **What's New in Version 2.7.0**

<code><font color="darkviolet">Add</font></code> Playback of downloaded NCG HLS content is supported.

- Downloaded NCG-HLS content is played in `Exoplayer`.

```java
// If you set the saved HLS path, it returns the URL (path) to play the encrypted content.
public String addHttpLiveStreamUrlForLocalPlayback(String path) throws Ncg2InvalidLicenseException, Ncg2Exception;
```

- Sample

```java
//
// advanced sample -> MainActivity.java
//
void startHlsPlayerActivity(String localM3U8Path) {
    ...
    if (isLocalM3U8) {
        playbackURL = ncgLocalWebServer.addHttpLiveStreamUrlForLocalPlayback(contentPath);
    } else {
        if (mGlobal.mIsLive == true) {
            playbackURL = ncgLocalWebServer.addHttpLiveStreamUrlForPlayback(contentPath, true);
        } else {
            playbackURL = ncgLocalWebServer.addHttpLiveStreamUrlForPlayback(contentPath, false);
        }
    }
    ...
}
```

> **NOTE**
>
> If the downloaded NCG HLS content cannot be played through the SDK, please contact us with the content.
>
> SDK does not support `m3u8` file download function.

<code><font color="LimeGreen">Update</font></code> `NCG Core` has been updated to `v2.4.0`.

- Supports players to play encrypted NCG HLS content.

### **Version 2.6.3**

> **What's New in Version 2.6.3**

<code><font color="darkviolet">Add</font></code> 'scoped storage' permission is added to the SDK Sample project.

- When downloading files on Android 10, you need to set `android:requestLegacyExternalStorage` to `true`.

```manifest
// https://developer.android.com/training/data-storage/use-cases#opt-out-scoped-storage
<manifest ... >
    <!-- This attribute is "false" by default on apps targeting
        Android 10 or higher. -->
    <application android:requestLegacyExternalStorage="true" ... >
        ...
    </application>
</manifest>
```

### **Version 2.6.2**

> **What's New in Version 2.6.2**

<code><font color="darkviolet">Added</font></code> APIs that can be licensed with `tokens` are added.

- It is an API that can acquire licenses using `token` information.

```java
public void acquireLicenseByToken(String token, String acquisitionURL, boolean isTemporary )
			throws Ncg2ServerResponseErrorException, Ncg2HttpException, Ncg2Exception
```

### **Version 2.6.1**

> **What's New in Version 2.6.1**

<code><font color="royalblue">Fixed</font></code> Sample project errors are fixed.

- Fixed a problem that could not be executed due to an error when executing the sample project.

### **Version 2.6.0**

> **What's New in Version 2.6.0**

<code><font color="darkviolet">Added</font></code> Added `setRootingCheckLevel()` API, which allows you to specify the strength when checking Android routing.

- It is divided into 1 to 4 levels, and the default is `level 1`.

```java
level = 1 Default Level.
  	 SU EXISTS,SU BINARY,SUPERUSER APK,PERMISSIVE SELINUX,RESETPROP,WRONG PATH PERMITION,
  	 DANGEROUS PROPS,BUSYBOX BINARY,XPOSED,RESETPROP,TEST KEYS,DEV KEYS,NON RELEASE KEYS
level = 2 SU EXISTS,SU BINARY,SUPERUSER APK,PERMISSIVE SELINUX,RESETPROP,WRONG PATH PERMITION,
  	 DANGEROUS PROPS,BUSYBOX BINARY,XPOSED,RESETPROP
level = 3 SU EXISTS,SU BINARY,SUPERUSER APK,PERMISSIVE SELINUX,RESETPROP,WRONG PATH PERMITION
level = 4 SU EXISTS,SU BINARY
```

### Version 2.5.5

> **What's New in Version 2.5.5**

<code><font color="orangered">Changed</font></code> Changed to Target 28 -> 29.

- Target version is changed to 29 according to Google policy.

<code><font color="orangered">Changed</font></code> From Android Q(API 29), the'Widevine' UUID is used as the `Device ID`.

- As of Android Q (API 29), `TELEPHONY_SERVICE` cannot be used.
- `Widevine` UUID is used when all the following conditions are met.
  - Device is Android Q (API 29) or higher
  - `Widevine` DRM support
  - In case of `Widevine` security level (`securityLevel`) `L1`

### **Version 2.5.4**

> **What's New in Version 2.5.4**

<code><font color="orangered">Changed</font></code> The changed `NCG Core` is applied.

- URI extraction function in `EXT-X-MAP` from m3u8 file is added to `NCG Core` and applied to NCG SDK.

<code><font color="orangered">Changed</font></code> The `NCG Advanced` Sample changes to call different players depending on whether they are HLS content.

- For HLS content, it is played by calling `MediaPlayer` from `startHlsPlayerActivity()`.
- Played through `Exoplayer` if not HLS content.

### **Version 2.5.3**

> **What's New in Version 2.5.3**

<code><font color="orangered">Changed</font></code> Android Rooting check is changed.

### **Version 2.5.2**

> **What's New in Version 2.5.2**

<code><font color="orangered">Changed</font></code> `NCG Core` logs are output outside the SDK.

- `Secure Time` related logs are output to `NcgExceptionalEventListener`.
- `NCG Core` All logs are not output.

---

### **Version 2.5.1**

> **What's New in Version 2.5.1**

<code><font color="orangered">Changed</font></code> Android Target Sdk Version Change(27 -> 28)

- Android Target Sdk Version changed from 27 to 28.
- AndroidX is applied.

<code><font color="royalblue">Fixed</font></code> Fixed External Display Detection and Rooting Checking.

- Fixed an issue that prevented external output and routing checking on some devices.

---

### **Version 2.5.0**

> **What's New in Version 2.5.0**

<code><font color="royalblue">Fixed</font></code> Removed crash on local web server.

- The crash on the local web server has been removed.

---

### **Version 2.4.0**

> **What's New in Version 2.4.0**

<code><font color="orangered">Changed</font></code> Support for Android 64bit(arm64-v8a, x86_64)

- Google has included **<font color="red">Android ABI arm64-v8a, x86_64 in the NCG SDK</font>** with the requirement to support Android 64bit since August 2019.
- NCG SDK v2.4.0 must be applied in order to register apps for Google Play since August.

<code><font color="orangered">Changed</font></code> Change Android API Level minimum support(API 15 -> 16)

- Minimal support in NDK has been changed to **<font color="red">Android API Lebel 16</font>** while supporting NCG SDK 64bit.
- Minimum support for Android at 64bit is **<font color="red">Android API Lebel 21</font>**.

---

### Version 20190509 (released 2019-05-09)

> **What's New in Version 2.3.3**

<code><font color="darkviolet">Added</font></code> BlueStacks 4 detection add

- Fixed SDK to detect new BlueStacks 4.
- Limited use of the SDK in BlueStacks4.

---

### Version 20190226 (released 2019-02-26)

> - **<font color="red">Applying ExoPlayer 2.9.3</font>**
> - **<font color="red">Fixed internal errors when running on Android 9.0</font>**

#### **1. SDK API**

<code><font color="darkviolet">Added</font></code>

<code><font color="orangered">Changed</font></code>

<code><font color="darkslategray">Removal</font></code>

<code><font color="royalblue">Fixed</font></code>

#### **2. Sample Source**

<code><font color="darkviolet">Added</font></code>

<code><font color="orangered">Changed</font></code>

<code><font color="darkslategray">Removal</font></code>

<code><font color="royalblue">Fixed</font></code>

---

### Version 20181221 (released 2018-12-21)

> - **<font color="red">Core patch to disallow emulators provided by Android SDK.</font>**

#### **1. SDK API**

<code><font color="darkviolet">Added</font></code>

<code><font color="orangered">Changed</font></code>

<code><font color="darkslategray">Removal</font></code>

<code><font color="royalblue">Fixed</font></code>

#### **2. Sample Source**

<code><font color="darkviolet">Added</font></code>

<code><font color="orangered">Changed</font></code>

<code><font color="darkslategray">Removal</font></code>

<code><font color="royalblue">Fixed</font></code>

---

### Version 20180912 (released 2018-09-12)

> - **<font color="red">Core patch for HLS playback error.</font>**
> - **<font color="red">added simple sample.</font>**

#### **1. SDK API**

<code><font color="darkviolet">Added</font></code>

<code><font color="orangered">Changed</font></code>

<code><font color="darkslategray">Removal</font></code>

<code><font color="royalblue">Fixed</font></code>

#### **2. Sample Source**

<code><font color="darkviolet">Added</font></code>

<code><font color="orangered">Changed</font></code>

<code><font color="darkslategray">Removal</font></code>

<code><font color="royalblue">Fixed</font></code>

---

### Version 20180903 (released 2018-09-03)

> - **<font color="red">Correct the previous securetime and the current securetime acquisition logic equally.</font>**

#### **1. SDK API**

<code><font color="darkviolet">Added</font></code>

<code><font color="orangered">Changed</font></code>

<code><font color="darkslategray">Removal</font></code>

<code><font color="royalblue">Fixed</font></code>

#### **2. Sample Source**

<code><font color="darkviolet">Added</font></code>

<code><font color="orangered">Changed</font></code>

<code><font color="darkslategray">Removal</font></code>

<code><font color="royalblue">Fixed</font></code>

---

### Version 20180726 (released 2018-08-06)

> - **<font color="red">HLS live</font>**
> - **<font color="red">DefaultPlayer : ExoPlayer</font>**

#### **1. SDK API**

<code><font color="darkviolet">Added</font></code>

```java
Ncg2LocalWebServer - addHttpLiveStreamUrlForPlayback(String url, boolean isLiveHLS)
Ncg2LocalWebServer - addHttpLiveStreamUrlForPlayback(String url, String cid, String siteID, boolean isLiveHLS)
```

<code><font color="orangered">Changed</font></code>

```java
Default Activity : MediaPlayerActivity -> ExoPlayerActivity
```

<code><font color="darkslategray">Removal</font></code>

<code><font color="royalblue">Fixed</font></code>

#### **2. Sample Source**

<code><font color="darkviolet">Added</font></code>

```java
ExoPlayerActivity.java for hls live
layout_exoplayer.xml for hls live
```

<code><font color="orangered">Changed</font></code>

```java
Global.java for hls live
MainActivity.java for hls live
```

<code><font color="darkslategray">Removal</font></code>

<code><font color="royalblue">Fixed</font></code>

---

### Version 20170215 (released 2017-02-21)

> - **<font color="red">Enhanced security patch has been updated.</font>**

#### **1. SDK API**

<code><font color="darkviolet">Added</font></code>

<code><font color="orangered">Changed</font></code>

<code><font color="darkslategray">Removal</font></code>

<code><font color="royalblue">Fixed</font></code>

#### **2. Sample Source**

<code><font color="darkviolet">Added</font></code>

<code><font color="orangered">Changed</font></code>

<code><font color="darkslategray">Removal</font></code>

<code><font color="royalblue">Fixed</font></code>

---

### Version 20170207 (released 2017-02-08)

> - **<font color="red">Virtual machine detection added.</font>**<br> > **Ncg2FatalException has been added.**<br> > **Please add the code for the user guide and the app exit in the exception.**
> - **<font color="red">The maximum length of OId in license request has increased to 4096 bytes</font>**

#### **1. SDK API**

<code><font color="darkviolet">Added</font></code>

<code><font color="orangered">Changed</font></code>

<code><font color="darkslategray">Removal</font></code>

<code><font color="royalblue">Fixed</font></code>

#### **2. Sample Source**

<code><font color="darkviolet">Added</font></code>

<code><font color="orangered">Changed</font></code>

```java
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
        mNcg2Agent.enableLog();
        mNcg2Agent.getLocalWebServer().setWebServerListener(mWebserverListener);

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
```

<code><font color="darkslategray">Removal</font></code>

<code><font color="royalblue">Fixed</font></code>

---

### Version 20161010 (released 2016-10-14)

> - **<font color="red">READ_PHONE_STATE Error</font>**<br> > **Ncg2ReadPhoneStateException have been added.**<br> > **1. Please add the code for the app such as user guides and exit on the right in this exception.**
> - **<font color="red">NCGERR_MODIFIED_DBFILE_INO Error** > **Ncg2ModifiedDBFileInoException have been added</font>**<br> > **1. please remove RODBFile in this exception by using new SDK API.**<br> > **2. Please add the user guide and code of the app exit in this exception.**

#### **1. SDK API**

<code><font color="darkviolet">Added</font></code>

```java
public abstract void removeRODBFile(Context context);
public abstract void removeRODBFile(Context context, String roDbPath);
```

<code><font color="orangered">Changed</font></code>

```java
public abstract void init(Context context, OfflineSupportPolicy policy) throws Ncg2ReadPhoneStateException, Ncg2ModifiedDBFileInoException, Ncg2Exception;
```

<code><font color="darkslategray">Removal</font></code>

<code><font color="royalblue">Fixed</font></code>

#### **2. Sample Source**

<code><font color="darkviolet">Added</font></code>

<code><font color="orangered">Changed</font></code>

```java
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
        mNcg2Agent.enableLog();
        mNcg2Agent.getLocalWebServer().setWebServerListener(mWebserverListener);

    } catch (Ncg2ReadPhoneStateException e) {
        e.printStackTrace();
        mListener.onSecurityError(e);
        return false;

    } catch (Ncg2ModifiedDBFileInoException e) {
        e.printStackTrace();
        mNcg2Agent.removeRODBFile(context);
        mListener.onModifiedDBFileInoError(e);
        return false;

    } catch (Ncg2Exception e) {
        e.printStackTrace();
        mListener.onError(e, "init() Exception : " + e.getMessage());
        return false;
    }

    return true;
}
```

```java
@Override
public void onModifiedDBFileInoError(Exception e) {
    // TODO Auto-generated method stub
    AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
    builder.setTitle(R.string.error_dialog_title);
    builder.setMessage(mActivity.getString(R.string.modified_dbfile_ino_error));
    builder.setPositiveButton(mActivity.getString(R.string.confirm), new DialogInterface.OnClickListener() {

        @Override
        public void onClick(DialogInterface dialog, int which) {
            // TODO Auto-generated method stub
            mActivity.finish();
        }
    });

    Dialog dialog = builder.create();
    dialog.setCancelable(false);
    dialog.setCanceledOnTouchOutside(false);
    dialog.show();
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
            mActivity.finish();
        }
    });

    Dialog dialog = builder.create();
    dialog.setCancelable(false);
    dialog.setCanceledOnTouchOutside(false);
    dialog.show();
}
```

<code><font color="darkslategray">Removal</font></code>

<code><font color="royalblue">Fixed</font></code>

---

### Version 20160531 (released 2016-06-15)

> - **After July 11, 2016, Google Play will block publishing of any new apps or updates that use older versions of OpenSSL.**<br> > **After July 11th 2016, <font color="red">You should use this SDK to update your app.</font>**
> - **<font color="red">All so & jar file should be changed. </font>**

<code><font color="darkviolet">Added</font></code>

OpenSSL version is updated to 1.0.2g (libsqlcipher_android.so)

<code><font color="orangered">Changed</font></code>

<code><font color="darkslategray">Removal</font></code>

<code><font color="royalblue">Fixed</font></code>

---

### Version 20160531 (released 2016-06-03)

> - **After July 11, 2016, Google Play will block publishing of any new apps or updates that use older versions of OpenSSL.**<br> > **After July 11th 2016, <font color="red">You should use this SDK to update your app.</font>**

<code><font color="darkviolet">Added</font></code>

OpenSSL version updated to 1.0.1s.

<code><font color="orangered">Changed</font></code>

<code><font color="darkslategray">Removal</font></code>

<code><font color="royalblue">Fixed</font></code>

---

### Version 20160111 (released 2016-01-11)

<code><font color="darkviolet">Added</font></code>

Your application should use a package name which is authorized by INKA. Media player may not work properly if unauthorized package name is used.
In case of changing package name, please contact us for the authorization process.

<code><font color="orangered">Changed</font></code>

<code><font color="darkslategray">Removal</font></code>

<code><font color="royalblue">Fixed</font></code>

---

### Version 20160108 (released 2016-01-08)

<code><font color="darkviolet">Added</font></code>

<code><font color="orangered">Changed</font></code>

<code><font color="darkslategray">Removal</font></code>

<code><font color="royalblue">Fixed</font></code>

allowable secure time's error range is increased from 24 hour to 1 hours. ( by reverting libNCG2_JNI.so which is created at 20151029)

---

### Version 20151218 (released 2015-12-18)

<code><font color="darkviolet">Added</font></code>

suspend function is same as pause function functionally.

```java
public abstract void suspend() throws Ncg2Exception;
```

<code><font color="orangered">Changed</font></code>

allowable secure time's error range is increased from 1 hour to 24 hours

<code><font color="darkslategray">Removal</font></code>

<code><font color="royalblue">Fixed</font></code>

---

### Version 20151120 (released 2015-11-20)

<code><font color="darkviolet">Added</font></code>

Can set secure setting for preventing media from screencapture (default : true)

```java
public abstract class Ncg2Player {
    public abstract void setSecure(boolean isSecureSurfaceView);
}
```

<code><font color="orangered">Changed</font></code>

<code><font color="darkslategray">Removal</font></code>

<code><font color="royalblue">Fixed</font></code>

---

### Version 20151029 (released 2015-10-29)

<code><font color="darkviolet">Added</font></code>

Cookie data can be sent when playing DnP(download and play).
NCG LocalWebServer can request with the cookie data.

```java
public abstract class Ncg2Player {
    public abstract void setDataSource(String path, String remoteUrlForDnp, long fileSize, String customCookie) throws IllegalArgumentException, IllegalStateException, IOException,Ncg2Exception;
}
```

<code><font color="orangered">Changed</font></code>

<code><font color="darkslategray">Removal</font></code>

<code><font color="royalblue">Fixed</font></code>

---

# Previous Version

<code><font color="darkviolet">Added</font></code>

S/W speed control(trick play) option is added. Previously the option was configured automatically, but now the H/W and S/W speed control option should be set manually in configuration. Must be added to the SW speed settings feature that allows you to turn on some terminals, because if HW playback is not smooth (default: HW)

```java
public abstract class Ncg2Player {
    public abstract void setSwCodecMode(boolean enableSwCodec);
}
```

You can turn off function of detecting screen recorder app. (default: On)

```java
public abstract class Ncg2Agent {
    public abstract void enableScreenRecorderDetecting(boolean isEnabled);
}
```

<code><font color="orangered">Changed</font></code>

<code><font color="darkslategray">Removal</font></code>

<code><font color="royalblue">Fixed</font></code>

---

History of previous version is given on the release_note.txt file. Please refer it.
