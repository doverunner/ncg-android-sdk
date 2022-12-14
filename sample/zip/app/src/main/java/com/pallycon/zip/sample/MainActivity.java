package com.pallycon.zip.sample;

import androidx.appcompat.app.AppCompatActivity;

import android.os.AsyncTask;
import android.os.Bundle;

import com.inka.ncg.zip.NcgZipArchive;
import com.inka.ncg.zip.NcgZipEntry;
import com.inka.ncg.zip.NcgZipException;
import com.inka.ncg2.Ncg2Agent;
import com.inka.ncg2.Ncg2Exception;
import com.inka.ncg2.Ncg2SdkFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    // TODO : set content information.
    private String contentUrl = "https://contents.pallycon.com/DEV/yhpark/ncg/ncg-zip-6bytetxt/6bytetxt.zip.ncg";
    private String ncgFileName = "6bytetxt.zip.ncg";
    private Ncg2Agent ncg2Agent = null;
    private NcgHttpRequestCallbackImpl mHttpRequestCallback = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ncg2Agent = Ncg2SdkFactory.getNcgAgentInstance();
        Ncg2Agent.OfflineSupportPolicy policy = Ncg2Agent.OfflineSupportPolicy.OfflineSupport;
        try {
            ncg2Agent.init(getApplicationContext(), policy);
        } catch (Ncg2Exception e) {
            e.printStackTrace();
        }

        mHttpRequestCallback = new NcgHttpRequestCallbackImpl(getApplicationContext());
        ncg2Agent.setHttpRequestCallback(mHttpRequestCallback);

        UnpackTask unpackTask = new UnpackTask();
        unpackTask.execute();
//        unPack();
    }

    private class UnpackTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            File root = getApplicationContext().getExternalFilesDir(null);
            try {

                // TODO 2. get license.
//            ncg2Agent.acquireLicenseByToken(token, true);

                InputStream inputStream = getAssets().open(ncgFileName);
                File file = new File(root, ncgFileName);
                try (OutputStream output = new FileOutputStream(file)) {
                    byte[] buffer = new byte[4 * 1024]; // or other buffer size
                    int read;

                    while ((read = inputStream.read(buffer)) != -1) {
                        output.write(buffer, 0, read);
                    }

                    output.flush();
                }
                inputStream.close();

                String ncg = root.getAbsolutePath() + "/" + ncgFileName;
                ncg2Agent.acquireLicenseByPath(ncg, "DEMO", "");
                NcgZipArchive ncgZipArchive = ncg2Agent.createNcgZipArchive();

                ncgZipArchive.open(ncg);
                String[] list = ncgZipArchive.getZipEntries();

                for (int i=0; i<list.length; i++) {
                    NcgZipEntry entry = ncgZipArchive.findEntry(list[i]);

                    InputStream in = entry.getInputStream();

                    String path = root.getAbsolutePath() + "/" + list[i];
                    file = new File(path); //path는 파일의 경로를 가리키는 문자열이다.

                    FileOutputStream out = new FileOutputStream(file, true);

                    byte[] buffer = new byte[8192];
                    int count;
                    while ((count = in.read(buffer)) > 0)
                    {
                        out.write(buffer, 0, count);
                    }

                    in.close();
                    out.close();
                    entry.close();
                }

                ncgZipArchive.close();
            } catch (Ncg2Exception e) {
                e.printStackTrace();
            } catch (NcgZipException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {

        }
    }
}