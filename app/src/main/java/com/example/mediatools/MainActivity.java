package com.example.mediatools;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.arthenica.ffmpegkit.FFmpegKitConfig;
import com.arthenica.ffmpegkit.FFmpegSession;
import com.arthenica.ffmpegkit.FFmpegSessionCompleteCallback;
import com.arthenica.ffmpegkit.ReturnCode;
import com.arthenica.ffmpegkit.SessionState;
import com.example.mediatools.databinding.ActivityMainBinding;
import com.arthenica.ffmpegkit.FFmpegKit;
import com.google.android.exoplayer2.util.Log;

import java.io.File;

@RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
public class MainActivity extends AppCompatActivity {
    // Used to load the 'mediatools' library on application startup.
    static {
        System.loadLibrary("mediatools");
    }

    private static final String TAG = "MainActivity";
    private final int PICK_MEDIA_REQUEST_CODE = 1;
    private final int DELAY_TIME_MS = 2500;
    private ActivityMainBinding binding;
    private Uri lastSelectedMediaUri;
    private int curPos;

    public static void verifyStoragePermissions(Activity activity) {
        final int REQUEST_EXTERNAL_STORAGE = 1;
        final String[] PERMISSIONS_STORAGE;

        String readImagePermission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            readImagePermission = Manifest.permission.READ_MEDIA_VIDEO;
            PERMISSIONS_STORAGE = new String[]{
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.READ_MEDIA_IMAGES,
            };
        } else {
            readImagePermission = Manifest.permission.READ_EXTERNAL_STORAGE;
            PERMISSIONS_STORAGE = new String[]{
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            };
        }

        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, readImagePermission);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        verifyStoragePermissions(this);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            Intent intentManageAllFile = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
//            intentManageAllFile.setData(Uri.parse("package:com.android.external-storage"));
//            startActivity(intentManageAllFile);
//        }

        Intent intentVideoConvert = new Intent(MainActivity.this, VideoConvertActivity.class);
        binding.startVideoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//              startActivity(intentVideoConvert);
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                String[] mimeTypes = {"image/*", "video/*"};
                intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
                startActivityForResult(Intent.createChooser(intent, "Select Media File"), PICK_MEDIA_REQUEST_CODE);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (lastSelectedMediaUri != null)
            displayMediaContent();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (lastSelectedMediaUri != null) {
            curPos = binding.videoView.getCurrentPosition() + DELAY_TIME_MS;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            // The user has successfully picked an image
            lastSelectedMediaUri = data.getData();
            displayMediaContent();
        }
    }

    public void displayMediaContent() {
        File cameraDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "ZrtcDemoLog");
        File inputVideo = new File(cameraDirectory + "/sample_video.mp4");
        File outputVideo = new File(cameraDirectory + "/out");

        FFmpegKit.executeAsync("-i " + inputVideo + " -vframes 1 " + outputVideo + (int) (100000 * Math.random()) + ".jpg", new FFmpegSessionCompleteCallback() {
            @Override
            public void apply(FFmpegSession session) {
                SessionState state = session.getState();
                ReturnCode returnCode = session.getReturnCode();

                // CALLED WHEN SESSION IS EXECUTED
                Log.d(TAG, String.format("FFmpeg process exited with state %s and rc %s.%s", state, returnCode, session.getFailStackTrace()));
            }
        });

        binding.imageView.setImageDrawable(null);
        binding.videoView.setVideoURI(null);
        binding.videoView.stopPlayback();
        binding.videoView.setVisibility(View.INVISIBLE);

        ContentResolver contentResolver = getContentResolver();
        String mediaType = contentResolver.getType(lastSelectedMediaUri);
        boolean isImage = mediaType.contains("image");
        boolean isVideo = mediaType.contains("video");

        if (isVideo) {
            binding.videoView.setVisibility(View.VISIBLE);
            binding.videoView.setVideoURI(lastSelectedMediaUri);
            if (curPos > 0) {
                binding.videoView.seekTo(curPos);
            }
            binding.videoView.start();
            binding.videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    lastSelectedMediaUri = null;
                    binding.videoView.setVisibility(View.INVISIBLE);
                }
            });
        } else if (isImage) {
            binding.imageView.setVisibility(View.VISIBLE);
            binding.imageView.setImageURI(lastSelectedMediaUri);
        }
    }

    /**
     * A native method that is implemented by the 'mediatools' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}