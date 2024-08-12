package com.inka.ncgsample;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;
import com.inka.ncg2.Ncg2Agent;
import com.inka.ncg2.Ncg2LocalWebServer;
import com.inka.ncg2.Ncg2SdkFactory;

public class ExoPlayerActivity extends Activity {
    private static final String TAG = "ExoPlayerActivity";
    private SimpleExoPlayer player;
    private boolean shouldAutoPlay;
    private Handler eventHandler;
    private SimpleExoPlayerView simpleExoPlayerView;
    private DefaultBandwidthMeter bandwidthMeter;
    private DefaultTrackSelector trackSelector;
    private DefaultTrackSelector.Parameters trackSelectorParameters;
    private DataSource.Factory mediaDataSourceFactory;
    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();
    private String cookie;
    private String playbackUrl;
    private Handler mHandler = new Handler();

    PlaybackParameters playbackParameters;
    private float playSpeed = 1.0f;

    Ncg2LocalWebServer.WebServerListener webServerListener = new Ncg2LocalWebServer.WebServerListener() {
        @Override
        public void onNotification(final int notifyCode, final String notifyMsg) {
            runOnUiThread(new Runnable() {

                @Override
                public void run() {

                    if( notifyCode == Ncg2LocalWebServer.WebServerListener.LWS_NOTIFY_DNP_READ_FAIL_PLAY_ERROR
                            || notifyCode == Ncg2LocalWebServer.WebServerListener.LWS_NOTIFY_HDMI_DETECTED
                            || notifyCode == Ncg2LocalWebServer.WebServerListener.LWS_NOTIFY_SCREEN_RECORDER_DETECTED ) {

                        String msg = String.format("NOTIFY CODE : [%d]\nNOTIFY MSG:[%s]\n", notifyCode, notifyMsg);
                        AlertDialog.Builder builder = new AlertDialog.Builder(ExoPlayerActivity.this);
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
                        player.stop();
                    }
                }
            });
        }

        @Override
        public void onError(final int errorCode, final String errorMessage) {

            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    String msg = String.format("ERROR CODE : [%d]\nERROR MSG:[%s]\n", errorCode, errorMessage);
                    AlertDialog.Builder builder = new AlertDialog.Builder(ExoPlayerActivity.this);
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
                    player.stop();
                }
            });

        }

        @Override
        public PlayerState onCheckPlayerStatus(String uri) {
            return PlayerState.ReadyToPlay;
        }
    };

    private Player.EventListener playerEventListener = new Player.DefaultEventListener() {
        @Override
        public void onPlayerError(ExoPlaybackException error) {
            AlertDialog.Builder builder = new AlertDialog.Builder(ExoPlayerActivity.this);
            builder.setTitle("Play Error");
            builder.setMessage(error.getMessage());
            builder.setPositiveButton("OK", null);
            Dialog dialog = builder.create();
            dialog.show();
        }
    };

    View.OnClickListener mButtonClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            int id = v.getId();
            if (id == R.id.btn_play_speed_up) {
                if (playSpeed < 2.0f) {
                    playSpeed += 0.1f;
                    playbackParameters = new PlaybackParameters(playSpeed, 1.0f);
                    player.setPlaybackParameters(playbackParameters);

                    TextView tv1 = (TextView) findViewById(R.id.tx_play_speed);
                    if (tv1 != null) {
                        tv1.setText(String.format("%.1fx", playSpeed));
                    }
                }
            } else if (id == R.id.btn_play_speed_down) {
                if (playSpeed > 0.5f) {
                    playSpeed -= 0.1f;
                    playbackParameters = new PlaybackParameters(playSpeed, 1.0f);
                    player.setPlaybackParameters(playbackParameters);

                    TextView tv2 = (TextView) findViewById(R.id.tx_play_speed);
                    if (tv2 != null) {
                        tv2.setText(String.format("%.1fx", playSpeed));
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_exoplayer);

        shouldAutoPlay = true;
        simpleExoPlayerView = findViewById(R.id.player_view);
        simpleExoPlayerView.requestFocus();
        eventHandler = new Handler();
        bandwidthMeter = new DefaultBandwidthMeter();

        trackSelectorParameters = new DefaultTrackSelector.ParametersBuilder().build();
        mediaDataSourceFactory = buildDataSourceFactory();

        Intent intent = getIntent();
        playbackUrl = intent.getStringExtra("path");
        if (playbackUrl == null || playbackUrl.length() == 0) {
            throw new RuntimeException("'path' param must be provided.");
        }

        Ncg2Agent ncgAgent = Ncg2SdkFactory.getNcgAgentInstance();
        Ncg2LocalWebServer ncgLocalWebServer = ncgAgent.getLocalWebServer();
        ncgLocalWebServer.setWebServerListener(webServerListener);

        findViewById(R.id.btn_play_speed_up).setOnClickListener(mButtonClickListener);
        findViewById(R.id.btn_play_speed_down).setOnClickListener(mButtonClickListener);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (Util.SDK_INT <= 23 || player == null) {
            try {
                initializePlayer();
            } catch (Exception e) {
                showDialog(e.getMessage());
            }
        } else {
            simpleExoPlayerView.setUseController(true);
            player.setPlayWhenReady(shouldAutoPlay);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (Util.SDK_INT <= 23) {
            releasePlayer();
        }

        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (Util.SDK_INT > 23) {
            releasePlayer();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Ncg2Agent ncgAgent = Ncg2SdkFactory.getNcgAgentInstance();
        Ncg2LocalWebServer ncgLocalWebServer = ncgAgent.getLocalWebServer();
        ncgLocalWebServer.clearPlaybackUrls();
        DemoLibrary.getNcgAgent().removeAllTemporaryLicense();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            try {
                initializePlayer();
            } catch (Exception e) {
                showDialog(e.getMessage());
            }
        } else {
            Toast.makeText(getApplicationContext(), "Permission to access storage was denied", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initializePlayer() throws Exception {
        Uri uri = Uri.parse(playbackUrl);

        if (uri == null || uri.toString().length() < 1) {
            throw new Exception("The content url is missing");
        }

        if (player == null) {
            TrackSelection.Factory adaptiveTrackSelectionFactory =
                    new AdaptiveTrackSelection.Factory(BANDWIDTH_METER);
            trackSelector = new DefaultTrackSelector(adaptiveTrackSelectionFactory);
            trackSelector.setParameters(trackSelectorParameters);

            @DefaultRenderersFactory.ExtensionRendererMode int extensionRendererMode = DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON;
            // TODO : Set Pallycon drmSessionManager for drm controller.
            DefaultRenderersFactory renderersFactory = new DefaultRenderersFactory(this,
                    null, extensionRendererMode);

            player = ExoPlayerFactory.newSimpleInstance(this, renderersFactory, trackSelector);
            // TODO : Set Pallycon drmSessionManager for listener.
            player.addListener(playerEventListener);

            // TODO : Set Sercurity API to protect media recording by screen recorder
            SurfaceView view = (SurfaceView) simpleExoPlayerView.getVideoSurfaceView();
            if (Build.VERSION.SDK_INT >= 17) {
                view.setSecure(true);
            }

            simpleExoPlayerView.setPlayer(player);
            player.setPlayWhenReady(shouldAutoPlay);
        }

        if (Util.maybeRequestReadExternalStoragePermission(this, uri)) {
            return;
        }

        PlaybackParameters playbackParameters = new PlaybackParameters(playSpeed, 1.0f);
        player.setPlaybackParameters(playbackParameters);
        MediaSource mediaSource = buildMediaSource(uri);
        player.prepare(mediaSource);
    }

    private void releasePlayer() {
        if (player != null) {
            shouldAutoPlay = player.getPlayWhenReady();
            player.release();
            player = null;
            trackSelector = null;
        }
    }

    private MediaSource buildMediaSource(Uri uri) {
        int type = Util.inferContentType(uri.getLastPathSegment());
        switch (type) {
            case C.TYPE_DASH:
                return new DashMediaSource(uri, buildDataSourceFactory(), new DefaultDashChunkSource.Factory(mediaDataSourceFactory), eventHandler, null);
            case C.TYPE_OTHER:
                return new ExtractorMediaSource(uri, mediaDataSourceFactory, new DefaultExtractorsFactory(), eventHandler, null);
            case C.TYPE_HLS:
                //return new HlsMediaSource(uri, mediaDataSourceFactory, eventHandler, null);
                return new HlsMediaSource.Factory(mediaDataSourceFactory).createMediaSource(uri);
            default:
                throw new IllegalStateException("Unsupported type: " + type);
        }
    }

    private DataSource.Factory buildDataSourceFactory() {
        HttpDataSource.Factory httpDataSourceFactory = buildHttpDataSourceFactory();
        httpDataSourceFactory.setDefaultRequestProperty("Cookie", cookie);
        return new DefaultDataSourceFactory(this, null, httpDataSourceFactory);
    }

    private HttpDataSource.Factory buildHttpDataSourceFactory() {
        return new DefaultHttpDataSourceFactory(Util.getUserAgent(this, "ExoPlayerSample"), null);
    }

    private void showDialog(String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ExoPlayerActivity.this);
        builder.setTitle("Play Error");
        builder.setMessage(msg);
        builder.setPositiveButton("OK", null);
        Dialog dialog = builder.create();
        dialog.show();
    }
}
