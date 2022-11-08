package com.inka.ncgsample;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import com.inka.ncg2.Ncg2Agent;
import com.inka.ncg2.Ncg2Exception;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by jwlee on 2018-06-12.
 */
public class ImageActivity extends Activity {
    private String mPlaybackUrl;
    private ImageView mImageView;
    private Ncg2Agent mNcg2Agent = Ncg2SdkWrapper.getInstance().getNcgAgent();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d(DemoLibrary.TAG, "[ImageActivity] onCreate() ++");

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_image_player);

        Intent intent = getIntent();
        mPlaybackUrl = intent.getStringExtra("path");
        if( mPlaybackUrl == null || mPlaybackUrl.length() == 0 ) {
            throw new RuntimeException("'path' param must be provided.");
        }

        initUI();
        unpackNcg2File(mPlaybackUrl);
    }

    private void initUI() {
        mImageView = (ImageView) findViewById(R.id.iv_src);
    }

    public boolean unpackNcg2File(String ncgFilePath) {
        ByteArrayOutputStream bos = null;

        byte[] buffer = new byte[1024];
        Ncg2Agent.NcgFile ncgFile = mNcg2Agent.createNcgFile();
        try {
            ncgFile.open(ncgFilePath);
            ncgFile.seek(0, Ncg2Agent.NcgFile.SeekMethod.End);

            // the original file's size will be used for checking success of decryption.
            long contentFileSize = ncgFile.getCurrentFilePointer();
            ncgFile.seek(0, Ncg2Agent.NcgFile.SeekMethod.Begin);
            Log.d(DemoLibrary.TAG, "Content FileSize : " + contentFileSize);

            // removes ".ncg" in the path of NCG file

            bos = new ByteArrayOutputStream();
            long totalReadBytes = 0;
            while( true ) {
                long readBytes = ncgFile.read(buffer, 1024);
                if( readBytes <= 0 ) {
                    break;
                }
                //fileOutStream.write(buffer, 0, (int)readBytes);
                bos.write(buffer, 0, (int)readBytes);
                totalReadBytes += readBytes;
            }

            if( totalReadBytes == contentFileSize ) {
                byte[] bytes = bos.toByteArray();
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                mImageView.setImageBitmap(bitmap);
                return true;
            }

        } catch(Ncg2Exception e) {
            e.printStackTrace();

        }  finally {

            try {
                if( ncgFile != null ) {
                    ncgFile.close();
                }

                if(bos != null) {
                    bos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return false;
    }
}
