package com.inka.simple.sample;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.util.Log;
import com.google.android.exoplayer2.util.Util;
import com.inka.ncg2.Ncg2Agent;
import com.inka.ncg2.Ncg2Exception;
import com.inka.ncg2.Ncg2LocalWebServer;
import com.inka.ncg2.Ncg2SdkFactory;

public class MainActivity extends AppCompatActivity {
    private final int MY_PERMISSION = 1;
    private final int MY_WRITE_EXTERNAL_STORAGE = 2;
    private DefaultBandwidthMeter BANDWIDTH_METER;

    private ExoPlayer player;
    private PlayerView exoPlayerView;
    private DataSource.Factory mediaDataSourceFactory;
    private DefaultTrackSelector trackSelector;
    private MediaSource mediaSource;
    private String userAgent;

    // TODO : set content information.
    private String contentUrl = "https://contents.pallycon.com/TEST/PACKAGED_CONTENT/TEST_SIMPLE/sintel-trailer.mp4.ncg";
    private String token = "eyJrZXlfcm90YXRpb24iOmZhbHNlLCJyZXNwb25zZV9mb3JtYXQiOiJvcmlnaW5hbCIsInVzZXJfaWQiOiJ1dGVzdCIsImRybV90eXBlIjoibmNnIiwic2l0ZV9pZCI6IkRFTU8iLCJoYXNoIjoicE1DUXFldzFZeGk4WXJDYlJlQnNzM0IwYUI0T3hBVER1UmdZaVRVXC8zbVk9IiwiY2lkIjoiVGVzdFJ1bm5lciIsInBvbGljeSI6IjlXcUlXa2RocHhWR0s4UFNJWWNuSnNjdnVBOXN4Z3ViTHNkK2FqdVwvYm9tUVpQYnFJK3hhZVlmUW9jY2t2dUVmdUx0dlVMWXEwTnVoNVJaOFhGYzQ1RWxHd1dcLzY3WVhUcTJQSDJ4Z3dIR1hDalVuaUgzbDQ4NVNmcDZjbmV1bm5qdjMxeGt5VHd6VlAzdVhIUGJWNWR3PT0iLCJ0aW1lc3RhbXAiOiIyMDIwLTExLTIwVDA0OjE2OjI1WiJ9";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BANDWIDTH_METER = new DefaultBandwidthMeter.Builder(this).build();

        StrictMode.ThreadPolicy pol = new StrictMode.ThreadPolicy.Builder().permitNetwork().build();
        StrictMode.setThreadPolicy(pol);

        exoPlayerView = findViewById(R.id.player_view);
        exoPlayerView.requestFocus();

        userAgent = Util.getUserAgent(this, "ExoPlayerDemo");
        mediaDataSourceFactory = buildDataSourceFactory(true);
        trackSelector = new DefaultTrackSelector(this);

        if (Build.VERSION.SDK_INT >= 23) {
            if( checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_DENIED ) {
                requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE}, MY_PERMISSION);
            }
            if( checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED ) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSION);
            }
        }
    }

    private void initializePlayer() {
        String playbackUrl = "";
        try {
            // TODO: 1. initialize NCG SDK.
            Ncg2Agent ncg2Agent = Ncg2SdkFactory.getNcgAgentInstance();
            Ncg2Agent.OfflineSupportPolicy policy = Ncg2Agent.OfflineSupportPolicy.OfflineSupport;

            long start = System.currentTimeMillis();
            ncg2Agent.init(this, policy);
            long end = System.currentTimeMillis();
            long time = end-start;
            Log.d("PALLYCON", String.format("ncg2Agent.init. %d", time));

            // TODO 2. get license.
            start = System.currentTimeMillis();
            ncg2Agent.acquireLicenseByToken(token, true);
            end = System.currentTimeMillis();
            time = end-start;
            Log.d("PALLYCON", String.format("acquireLicenseByToken. %d", time));


            // TODO 3. get playback url.
            start = System.currentTimeMillis();
            Ncg2LocalWebServer localWebServer = ncg2Agent.getLocalWebServer();
            playbackUrl = localWebServer.addProgressiveDownloadUrlForPlayback(contentUrl);
            end = System.currentTimeMillis();
            time = end-start;
            Log.d("PALLYCON", String.format("addProgressiveDownloadUrlForPlayback %d", time));


        } catch (Ncg2Exception e) {
            Toast.makeText(this, "errorCode : " + e.getErrorCode() + "/" + " msg : " + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        // player setting
        player = new ExoPlayer.Builder(this)
                .setTrackSelector(trackSelector)
                .build();
        exoPlayerView.setPlayer(player);
        player.setPlayWhenReady(true);
        Uri uri = Uri.parse(playbackUrl);
        mediaSource = buildMediaSource(uri, null);
        player.setMediaSource(mediaSource);
        player.prepare();
    }

    private void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }

        Ncg2Agent ncg2Agent = Ncg2SdkFactory.getNcgAgentInstance();
        if( ncg2Agent != null ) {
            ncg2Agent.release();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSION:
                for (int grant : grantResults) {
                    if (grant != PackageManager.PERMISSION_GRANTED) {
                        break;
                    }
                    Toast.makeText(this, "permission is allowed!!", Toast.LENGTH_LONG).show();
                }
                break;
            default:
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (Util.SDK_INT > 23) {
            initializePlayer();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (Util.SDK_INT <= 23 || player == null) {
            initializePlayer();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (Util.SDK_INT <= 23) {
            releasePlayer();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (Util.SDK_INT > 23) {
            releasePlayer();
        }
    }



    @SuppressWarnings("unchecked")
    private MediaSource buildMediaSource(Uri uri, @Nullable String overrideExtension) {
        @C.ContentType int type = Util.inferContentType(uri, overrideExtension);
        switch (type) {
            case C.TYPE_DASH:
                return new DashMediaSource.Factory(mediaDataSourceFactory)
                        .createMediaSource(MediaItem.fromUri(uri));
            case C.TYPE_SS:
                return new SsMediaSource.Factory(mediaDataSourceFactory)
                        .createMediaSource(MediaItem.fromUri(uri));
            case C.TYPE_HLS:
                return new HlsMediaSource.Factory(mediaDataSourceFactory)
                        .createMediaSource(MediaItem.fromUri(uri));
            case C.TYPE_OTHER:
                return new ProgressiveMediaSource.Factory(mediaDataSourceFactory)
                        .createMediaSource(MediaItem.fromUri(uri));
            default:
                throw new IllegalStateException("Unsupported type: " + type);
        }
    }

    private DataSource.Factory buildDataSourceFactory(boolean useBandwidthMeter) {
        return buildDataSourceFactory(useBandwidthMeter ? BANDWIDTH_METER : null);
    }

    public DataSource.Factory buildDataSourceFactory(TransferListener listener) {
        DefaultDataSource.Factory upstreamFactory = new DefaultDataSource.Factory(this,
                buildHttpDataSourceFactory(listener));
        return upstreamFactory;
    }

    public HttpDataSource.Factory buildHttpDataSourceFactory(TransferListener listener) {
        return new DefaultHttpDataSource.Factory()
                .setUserAgent(userAgent)
                .setTransferListener(listener);
    }
}
