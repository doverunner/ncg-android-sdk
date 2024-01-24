package com.pallycon.epub.sample;

import androidx.appcompat.app.AppCompatActivity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.inka.ncg2.Ncg2Agent;
import com.inka.ncg2.Ncg2Exception;
import com.inka.ncg2.Ncg2SdkFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class MainActivity extends AppCompatActivity {
    private String contentUrl = "https://contents.pallycon.com/DEV/sglee/ziptest.zip";

    private String rootAbsolutePath = "";
    private String ncgFileName = "ziptest.zip";
    private Ncg2Agent ncg2Agent = null;
    private NcgHttpRequestCallbackImpl mHttpRequestCallback = null;
    private NcgLocalFileCallbackImpl mLocalFileCallback = null;

    Button unpackButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ncg2Agent = Ncg2SdkFactory.getNcgAgentInstance();
        Ncg2Agent.OfflineSupportPolicy policy = Ncg2Agent.OfflineSupportPolicy.OfflineSupport;
        try {
            // init ncg2Agent.
            ncg2Agent.init(getApplicationContext(), policy);
//            ncg2Agent.disableLog();
        } catch (Ncg2Exception e) {
            e.printStackTrace();
        }

        mHttpRequestCallback = new NcgHttpRequestCallbackImpl(getApplicationContext());

        File root = getApplicationContext().getExternalFilesDir(null);
        rootAbsolutePath = root.getAbsolutePath();
        String path = rootAbsolutePath + "/" + ncgFileName;

        mLocalFileCallback = new NcgLocalFileCallbackImpl(ncg2Agent, path);
        ncg2Agent.setLocalFileCallback(mLocalFileCallback);
        ncg2Agent.setHttpRequestCallback(mHttpRequestCallback);

        // download content.
        Button downloadButton = findViewById(R.id.downloadButton);
        downloadButton.setOnClickListener(v -> {
            DownloadFileFromURL downloadTask = new DownloadFileFromURL();
            downloadTask.execute(contentUrl);
        });

        // unpack content.
        unpackButton = findViewById(R.id.unpackButton);
        unpackButton.setEnabled(false);
        unpackButton.setOnClickListener(v -> {
            UnpackTask unpackTask = new UnpackTask();
            unpackTask.execute();
        });
        checkDownload();
    }

    private void checkDownload() {
        String path = rootAbsolutePath + "/" + ncgFileName;
        File file = new File(path);
        if (file.exists()) {
            unpackButton.setEnabled(true);
        }
    }

    private class UnpackTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            long startTime = System.currentTimeMillis();
            try {
                ncg2Agent.acquireLicenseByToken("eyJrZXlfcm90YXRpb24iOmZhbHNlLCJyZXNwb25zZV9mb3JtYXQiOiJvcmlnaW5hbCIsInVzZXJfaWQiOiJ0ZXN0VXNlciIsImRybV90eXBlIjoiTkNHIiwic2l0ZV9pZCI6IkRFTU8iLCJoYXNoIjoiUzJuUHBncXBONUEybVZPQlVDT2hmS2NcL0I4dlJrSzVJVTVYXC9KSnFtbCtrPSIsImNpZCI6InppcHRlc3QiLCJwb2xpY3kiOiI5V3FJV2tkaHB4VkdLOFBTSVljbkpzY3Z1QTlzeGd1YkxzZCthanVcL2JvbVFaUGJxSSt4YWVZZlFvY2NrdnVFZkFhcWRXNWhYZ0pOZ2NTUzNmUzdvOE5zandzempNdXZ0KzBRekxrWlZWTm14MGtlZk9lMndDczJUSVRnZFU0QnZOOWFiaGQwclFrTUlybW9JZW9KSHFJZUhjUnZWZjZUMTRSbVRBREVwQ1k3UEhmUGZcL1ZGWVwvVmJYdXhYXC9XVHRWYXM0T1VwQ0RkNW0xc3BUWG04RFwvTUhGcGlieWZacERMRnBHeFArNkR4OThKSXhtTmFwWmRaRmlTTXB3aVpZRTIiLCJ0aW1lc3RhbXAiOiIyMDI0LTAxLTI0VDA0OjU5OjExWiJ9", true);

                Enumeration<? extends NcgLocalFileCallbackImpl.FileEntry> entries = mLocalFileCallback.entries();

                while (entries.hasMoreElements()) {
                    NcgLocalFileCallbackImpl.FileEntry entry = entries.nextElement();
                    if (!entry.isDirectory()) {
                        String fileName = entry.getName();
                        String savePath = rootAbsolutePath + "/" + fileName.substring(0, fileName.length()-4);
                        File file = new File(savePath);
                        File dir = file.getParentFile();
                        if (!dir.exists()) {
                            dir.mkdirs();
                        }

                        FileOutputStream out = new FileOutputStream(file, true);

                        byte[] buffer = new byte[8192];
                        int count;

                        // We'll work on getting the DRM license inside the function.
                        entry.open();

                        while ((count = entry.read(buffer)) > 0) {
                            out.write(buffer, 0, count);
                        }

                        out.close();
                    }
                }

                mLocalFileCallback.release();
            } catch (Ncg2Exception e) {
                Log.d("unpack","Ncg2Exception : " + e.getMessage());
            } catch (IOException e) {
                Log.d("unpack","IOException : " + e.getMessage());
            }

            long endTime = System.currentTimeMillis();
            System.out.println((endTime - startTime));
            return null;
        }
    }

    private class UnpackTask2 extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... f_url) {
            long startTime = System.currentTimeMillis();
            try {
                ncg2Agent.acquireLicenseByToken("eyJrZXlfcm90YXRpb24iOmZhbHNlLCJyZXNwb25zZV9mb3JtYXQiOiJvcmlnaW5hbCIsInVzZXJfaWQiOiJ1dGVzdCIsImRybV90eXBlIjoiTkNHIiwic2l0ZV9pZCI6IkpRNEkiLCJoYXNoIjoiYTRXVzNKcm1GZ0UxWlFuVTRGZEFZUnhSeDNDT0RcL1FTRlNmVXhiZklTWjg9IiwiY2lkIjoiMjAyMzA2MTYtZTE4ZDBjYmExOTFiMjVkZTgyNzdiNzlhYmE5YWYyZjIiLCJwb2xpY3kiOiJyRFBPZjV5NzcrWThQd3ErYXBGWXBOZElsczBCVHg4N1pCZTdzK3lHSG5JVE1TRktPeUltZGtnUytiQWlXaE9zQUt4a2VaMlQ2N1hqTVZXemExUzZ4U1ZlU0RtK2xCY2dpQ1JTaTJleDZQUHNoWG5oOEs2ZHdBV2tUKzhGZlZXUUV6cVl5VTZGM04wRFliOHNGSDFtdnNDdDdqVDlLbEVEMXlhVmd2SlRQS1FOQUN2WXl0akg0cGkzRDgzN2dmTTQ0RlRXb3hGQWEwV3k1cytySTBoNjNlQ3RsU1gyeUNCZVhyR1wvMko5Q1A1NjZ0NlNjbzFwaUhkcnQxdHhUbnlNNCIsInRpbWVzdGFtcCI6IjIwMjMtMDctMjdUMDA6MjE6NTlaIn0=", true);

                ZipFile zipFile = new ZipFile(f_url[0]);
                Enumeration e = zipFile.entries();
                Ncg2Agent.NcgEpubFile ncgEpubFile = ncg2Agent.createNcgEpub();

                while (e.hasMoreElements()) {
                    ZipEntry entry = (ZipEntry) e.nextElement();
                    File destinationFilePath = new File(f_url[0], entry.getName());

                    destinationFilePath.getParentFile().mkdirs();

                    if (entry.isDirectory())
                        continue;

                    String fileName = entry.getName();
                    String savePath = rootAbsolutePath + "/" + fileName;
                    String saveDecryptedPath = rootAbsolutePath + "/" + fileName.substring(0, fileName.length()-4);
                    File file = new File(savePath);
                    File file2 = new File(saveDecryptedPath);
                    File dir = file.getParentFile();
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }

                    FileOutputStream out = new FileOutputStream(file, true);
                    byte[] buffer = new byte[8192];
                    int count;

                    BufferedInputStream bis = new BufferedInputStream(zipFile.getInputStream(entry));

                    while ((count = bis.read(buffer, 0, 8192)) != -1) {
                        out.write(buffer, 0, count);
                    }
                    out.close();
                    bis.close();

                    ncgEpubFile.open(savePath);

                    FileOutputStream out2 = new FileOutputStream(file2, true);

                    while ((count = (int) ncgEpubFile.read(buffer, 8192)) > 0) {
                        out2.write(buffer, 0, count);
                    }

                    ncgEpubFile.close();

                    out2.close();
                }

                ncgEpubFile.release();
            } catch (Ncg2Exception e) {
                Log.d("unpack","Ncg2Exception : " + e.getMessage());
            } catch (IOException e) {
                Log.d("unpack","IOException : " + e.getMessage());
            }

            long endTime = System.currentTimeMillis();
            System.out.println((endTime - startTime));
            return null;
        }
    }

    class DownloadFileFromURL extends AsyncTask<String, String, String> {
        /**
         * Before starting background thread Show Progress Bar Dialog
         **/
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        /**
         * Downloading file in background thread
         **/
        @Override
        protected String doInBackground(String... f_url) {
            File root = getApplicationContext().getExternalFilesDir(null);

            int count;
            try {
                URL url = new URL(f_url[0]);
                URLConnection conection = url.openConnection();
                conection.connect();

                // this will be useful so that you can show a typical 0-100%
                // progress bar
                int lenghtOfFile = conection.getContentLength();

                // download the file
                InputStream input = new BufferedInputStream(url.openStream(),
                        8192);

                // Output stream
                File file = new File(root, ncgFileName);
                OutputStream output = new FileOutputStream(file);

                byte data[] = new byte[1024];

                long total = 0;

                while ((count = input.read(data)) != -1) {
                    total += count;
                    // publishing the progress....
                    // After this onProgressUpdate will be called
                    publishProgress("" + (int) ((total * 100) / lenghtOfFile));

                    // writing data to file
                    output.write(data, 0, count);
                }

                // flushing output
                output.flush();

                // closing streams
                output.close();
                input.close();
            } catch (Exception e) {
                Log.e("Error: ", e.getMessage());
            }

            return null;
        }

        /**
         * Updating progress bar
         **/
        protected void onProgressUpdate(String... progress) {
            // setting progress percentage
            Log.d("download","progress[0] = " + progress[0]);
        }

        /**
         * After completing background task Dismiss the progress dialog
         **/
        @Override
        protected void onPostExecute(String file_url) {
            // dismiss the dialog after the file was downloaded
            checkDownload();
        }
    }
}