
<script src="raphael-min.js"></script>
<script src="flowchart-latest.js"></script>
<script src="webfont.js"></script>
<script src="underscore-min.js"></script>
<script src="sequence-diagram-min.js"></script>

# <a name="doc_top"></a>PallyCon SDK Sample App Scenario

When planning, designing, and implementing Application, it is very important to understand PallyCon SDK. 
This document helps understand **how SDK processes DRM contents** in Flow Chat and describes **the entire interaction between the operation and roles of SDK in the reflected Sample App** ultimately preventing the error in planning/designing Application.

 - [SDK DRM Processing](#sdk-drm-processing) describes the flow for SDK to process DRM contents (Downloaded Contents).
 - [SDK Sample App Sequence](#sdk-sample-app-sequence) describes the entire interactions among User, App (Application), SDK, and License Server.  

----------

## SDK DRM Processing

Only when you understand the flow how PallyCon SDK processes DRM contents, you can plan the scenario of Application (App) to be implemented.
The following flow chart indicates the flow for SDK to process DRM video contents. The following flow focuses on DRM video contents; besides, audio, document, and picture are also processed in the similar flow.

<center>
<div id="flow"></div>
<script>
  var diagram = flowchart.parse(
  "st=>start: Contents Select\n" +
  "e=>end\n" +
  "isNCG=>condition: 1. Encrypted ?\n" +
  "licenseExist=>condition: 2. License\n" +
  "  Exist ?\n" +
  "licenseValid=>condition: 3. Valid\n" +
  "License ?\n" +
  "acquire=>subroutine: 4. License\n" +
  "Acquisition\n" +
  "acquireLicensecheck=>condition: 5. Valid\n" +
  "License ?\n" +
  "Proxy=>subroutine: 6. Local WebServer\n" +
  "io=>inputoutput: 7. Player(Playback)\n" +
  "st->isNCG->licenseExist->licenseValid->Proxy->io->e\n" +
  "isNCG(yes)->licenseExist\n" +
  "isNCG(no)->io\n" +
  "licenseExist(yes)->licenseValid\n" +
  "licenseExist(no)->acquire\n" +
  "licenseValid(yes)->Proxy\n" +
  "licenseValid(no)->acquire(right)->acquireLicensecheck\n" +
  "acquireLicensecheck(yes)->Proxy\n" +
  "acquireLicensecheck(no)->e\n");
  diagram.drawSVG('flow');
</script>
</center>

The following is a description about ‘Flow Chart`.
> If you try to play video contents on App
>
> 1. Determine whether the video content is a DRM file.
>    - if not a DRM file, since it is not an encrypted file, following the general play procedure as set forth in Step 7.
>    - If it is a DRM file, find the license as set forth in Step 2.
> 2. If you are trying to find the license of video content,
>    - if there exists license, confirm the license for validity as set forth in Step 3.
>    - If there is no license, try to acquire license (from license server) as set forth in Step 4.
> 3. If there exist license for video content, confirm it for validity.
>    - If it is valid, move to Step 6 for play trying.
>    - If it is not valid, try to acquire license as set forth in Step 4 (you may also restrict playing).
> 4. Try to acquire license from license server.
> 5. Confirm the issued license for validity.
>    - If it is valid, move to Step 6 Local Server.
>    - If it is not valid, restrict playing (which depends on scenario).
> 6. Local WebServer returns virtual URL for Player to play DRM contents.  
> 7. Player plays an ordinary normal file or a normal DRM content.

[Top](#doc_top)

----------

## SDK Sample App Sequence

It is a sequence diagram indicating the entire interaction and flow of Sample App to which SDK is applied.
In order for `User` to use ‘DRM contents`, there should be App with `SDK (PallyCon SDK)` applied as well as ‘License Server’ should be ready to acquire license for DRM contents.
Depending User event, indicate the acts of App, SDK, and License Server in order, describing such acts focusing on App and SDK.   
The following describes the case of DRM Contents, not considering Non-DRM File.

<center>
<div id="sequence"></div>
<script>
  var diagram = Diagram.parse(
  "User->App: App Excute\n" +
  "App->SDK: 1. init\n" +
  "SDK->>SDK: initialize: Local Server Start\n" +
  "User->App: Playback or Download\n" +
  "App->SDK: 2. Check License of Contents\n" +
  "SDK-->App: License Validation Return\n" +
  "App->SDK: 3. License Acquisition\n" +
  "SDK->License Server: Device info Enroll\n" +
  "License Server-->SDK: Certification Issue\n" +
  "Note over SDK: Certification Save\n" +
  "SDK->License Server: License Acquire\n" +
  "License Server-->SDK: License Issue\n" +
  "Note over SDK: License Check\n" +
  "App->SDK: 4. Playback URL Request\n" +
  "Note over SDK: Playable URL\n" +
  "SDK-->App: 5. Virtual URL Return\n" +
  "App->>App: Playback\n" +
  "App-->User: Watching\n" +
  "User->App: App Background Entry\n" +
  "App->>App: Play Stop\n" +
  "App->SDK: 6. Remove All Temporary License\n" +
  "App->SDK: 7. App Background Entry\n" +
  "User->App: App Foreground Entry\n" +
  "App->SDK: 8. App Foreground Entry\n" +
  "App->>App: Playback\n" +
  "App-->User: Watching");
  diagram.drawSVG("sequence", {theme: 'simple'});
</script>
</center>

The following describes `Sequence Diagram`.

> If a device is installed an App to which SDK is applied
>
> 1. init
>    - When a user runs App, the App carries out initialization work with init function of PallyCon SDK.
>    - At this time, verify DRM license file and authentication file for validity before loading data.
>    - You can also set offline policy at the initializing time and reference 'OfflineSupportPolicy' enum class for the details
>    - Current time is internally set in PallyCon SDK, where time value is periodically updated.
> 2. Check License of Contents
> - Verify license of the content for validity before a user plays the content.
> 3. License Acquisition
>    - **If there exists valid license**, you skip the process to acquire license.
>    - If no license, first acquire authentication for device, and then request and acquire license. 
>    - Acquiring license requires calling `aquireLicenseByCID` for such acquisition when a user knows `aquireLicenseByPath`, CID; request HTTP to send it to license server and then interpret HTTP Response to store license. **If there need temporary license**, you can be acquired a temporary license used temporary parameter.
> 4. Playback URL Request 
>    - When the received license is confirmed for validity, call `addLocalFilePathForPlayback`, `addProgressiveDownloadUrlForPlayback`, and  `addHttpLiveStreamUrlForPlayback` functions of LocalWebserver Interface to request playable virtual URL.
> 5. Virtual URL Return
>    - SDK returns virtual URL of DRM contents that player can play with No. 3 function while player plays and shows it to a user.
> 6. Remove All Temporary License
> **If there acquired temporary license**, exit the program or call the `removeAllTemporaryLicense`, temporary license is deleted.
> 7. App Background Entry
>    - When a user makes App enter Background while playing DRM content, App stops to run and makes Player reset.
> 8. App Foreground Entry
>    - When a user makes App situated in Background enter Foreground again, App sets URL again in setDataSource of Player and plays.

[Top](#doc_top)

----------

## PallyCon SDK Sample App UI

SDK Sample App consists of the UIs as shown in the following figure.
<p><img src="guideImages/main_activity.png" width="500" height="800"></p>
List consists of `Download`, `ProgressiveDownload`, and `Http Live Streaming, Local Downloaded`; the respective meaning is as shown in the following table.

|Item                           | Description                        |
| ----------------------------- | ---------------------------------- |
| Download | Download file in remote area |
| Progressive Download(PD)      | Play media file in remote area             |
| HTTP Live Streaming(HLS)      | Play HTTP-based media streaming            |
| Local Downloaded      | Play media file in device (Local File)            |

`Download` downloads file in remote area.
`Progressive Download(PD)` downloads the encrypted content file in remote area while playing it. File is downloaded but not stored in a physical file only existing in memory.
`HTTP Live Streaming(HLS)` is a protocol provided by Apple in 2009 for iOS 3.0 and QuickTime X, which is an HTTP-based media streaming. The information about playing is sent to player using m3u8 file; which is played decrypting the encrypted ‘Key’.
`Local Downloaded` means to play the encrypted contents in device, which can be a file downloaded or directly placed in App.

[Top](#doc_top)

----------

## Sample App Playback Test

Use SDK Sample App Project to conduct play test respectively for each content type (Download, Progressive Download, Http Live Streaming).

### Download

 1. Click on the list, the content that you desire to play in Download category to download the file.
 2. If downloading is complete, the file is generated in Local Downloaded category.
 3. When pressing Play button while downloading, you can play the content while downloading.
 4. When playing is complete or if clicking ‘Complete’ while playing, you go back to List.
 
### Progressive Download Playback

 1. Click on the list, the content that you desire to play in PD category.
 2. Player runs and the content starts to play.
 3. When playing is complete or if clicking ‘Complete’ while playing, you go back to List.

### HTTP Live Streaming Playback

 1. Click on the list, the content that you desire to play in HLS category.
 2. Player runs and the content starts to play.
 3. When playing is complete or if clicking ‘Complete’ while playing, you go back to List.
 
### Local Downloaded
 1. Click on the list, the content that you desire to play in Local Downloaded category.
 2. Player runs and the content starts to play.
 3. When playing is complete or if clicking ‘Complete’ while playing, you go back to List.
 1. Press longer on the list, the content that you desire to confirm a license in Local Downloaded category.
 2. A dialog is generated indicating license and header information.

> **Note:**
> - For Progressive Download contents, if moov tag is located behind, the contents may not normally work.

[Top](#doc_top)

----------

## Test Contents URL
The contents provided for test are shown in the following table.

| Download Contents |
|-|
| https://contents.pallycon.com/TEST/PACKAGED_CONTENT/TEST_SIMPLE/sintel-trailer.mp4.ncg |

| Progressive Download |
|-|
| https://contents.pallycon.com/TEST/PACKAGED_CONTENT/TEST_SIMPLE/sintel-trailer.mp4.ncg |

| HTTP Live Streaming |
|-|
| https://contents.pallycon.com/TEST/PACKAGED_CONTENT/TEST_SIMPLE/hls_ncg/master.m3u8 |

[Top](#doc_top)

----------
