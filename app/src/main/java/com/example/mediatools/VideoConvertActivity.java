package com.example.mediatools;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import com.example.mediatools.databinding.ActivityMainBinding;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.ui.StyledPlayerView;

import java.io.File;

public class VideoConvertActivity extends AppCompatActivity {
    private static final String TAG = "VideoConvertActivity";
    private StyledPlayerView playerView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_convert);
        playerView = findViewById(R.id.player_view);
    }

    @Override
    public void onResume(){
        super.onResume();
        ExoPlayer exoPlayer = new ExoPlayer.Builder(getApplicationContext()).build();
        try {
            File cameraDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Camera");
            if (!cameraDirectory.exists()) {
                cameraDirectory.mkdir();
            }
            File videosample = new File(cameraDirectory + "/sample_video.mp4");
            if (videosample.canRead()) {
                MediaItem mediaItem = MediaItem.fromUri(videosample.toURI().toString());
                exoPlayer.setMediaItem(mediaItem);
            }
        } catch (Exception exception) {
            Log.e(TAG, exception.toString());
        }
        exoPlayer.prepare();
        playerView.setPlayer(exoPlayer);
        exoPlayer.play();
    }

}