package nctu.cs.cgv.itour.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import id.zelory.compressor.Compressor;
import nctu.cs.cgv.itour.R;

import static nctu.cs.cgv.itour.Utility.hideSoftKeyboard;
import static nctu.cs.cgv.itour.Utility.moveFile;

public class CheckinActivity extends AppCompatActivity {

    private static final String TAG = "CheckinActivity";
    public static final int REQUEST_CODE_CHECKIN_FINISH = 456;
    public static final int RESULT_CODE_CHECKIN_FINISH = 456;
    private static final int MIC_PERMISSION_REQUEST = 123;
    // UI references
    private EditText descriptionEdit;
    private RelativeLayout photoBtn;
    private RelativeLayout audioBtn;
    private RelativeLayout pickedPhotoLayout;
    private LinearLayout recordAudioLayout;
    private ImageView cancelPhotoBtn;
    private ImageView cancelAudioBtn;
    private ImageView pickedPhoto;
    private ProgressBar progressBar;
    private TextView progressTextCurrent;
    private TextView progressTextDuration;
    private Button recordBtn;
    private Button stopBtn;
    private Button playBtn;
    private Button pauseBtn;
    private Button redoBtn;

    private String photoFile = "";
    private String audioFile = "";

    // mediaRecorder
    private boolean micAvailable = false;
    private boolean audioReady = false;
    private boolean isPlaying = false;
    private boolean isRecording = false;
    private MediaRecorder mediaRecorder;
    private MediaPlayer mediaPlayer;

    private int timeTick = 0;
    private CountDownTimer countDownTimer;
    private Handler progressBarHandler;
    private Runnable progressBarRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkin);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
        }

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("");
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_close_black_24dp);

//        checkPermission();

        setView();
    }

    private void setView() {

        // Verify that the device has a mic first
        PackageManager packageManager = this.getPackageManager();
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)) {
            micAvailable = false;
        }

        // set view
        descriptionEdit = findViewById(R.id.et_description);
        photoBtn = findViewById(R.id.btn_photo);
        audioBtn = findViewById(R.id.btn_audio);
        pickedPhotoLayout = findViewById(R.id.picked_photo_layout);
        recordAudioLayout = findViewById(R.id.recode_audio_layout);
        cancelPhotoBtn = findViewById(R.id.btn_cancel_photo);
        cancelAudioBtn = findViewById(R.id.btn_cancel_audio);

        pickedPhoto = findViewById(R.id.picked_photo);

        // set progress bar
        progressBar = findViewById(R.id.progressbar);
        progressTextCurrent = findViewById(R.id.tv_progress_current);
        progressTextDuration = findViewById(R.id.tv_progress_duration);
        final int timeTotal = 10000;
        final int timeInterval = 100;
        countDownTimer = new CountDownTimer(timeTotal, timeInterval) {

            @Override
            public void onTick(long millisUntilFinished) {
                timeTick++;
                String str = String.format("%d:%02d", timeTick * timeInterval / 1000, ((timeTick * timeInterval) % 1000) * 60 / 1000);
                progressTextCurrent.setText(str);
                progressBar.setProgress(timeTick * 100 / (timeTotal / timeInterval));
            }

            @Override
            public void onFinish() {
                progressTextCurrent.setText(getString(R.string.default_maximum_time));
                progressBar.setProgress(100);
                if (stopRecording())
                    initAudio();
            }
        };


        recordBtn = findViewById(R.id.btn_record);
        stopBtn = findViewById(R.id.btn_stop);
        playBtn = findViewById(R.id.btn_play);
        pauseBtn = findViewById(R.id.btn_pause);
        redoBtn = findViewById(R.id.btn_redo);

        photoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isRecording) {
                    Toast.makeText(getApplicationContext(), getString(R.string.toast_is_recording), Toast.LENGTH_SHORT).show();
                    return;
                }
                CropImage.activity()
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .setFixAspectRatio(true)
                        .setAspectRatio(1, 1)
                        .start(CheckinActivity.this);
            }
        });

        audioBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (micAvailable) {
                    audioBtn.setVisibility(View.GONE);
                    recordAudioLayout.setVisibility(View.VISIBLE);
                } else {
                    Toast.makeText(getApplicationContext(), getString(R.string.toast_mic_not_found), Toast.LENGTH_LONG).show();
                }
            }
        });

        cancelPhotoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                photoFile = "";
                pickedPhotoLayout.setVisibility(View.GONE);
                photoBtn.setVisibility(View.VISIBLE);
            }
        });

        cancelAudioBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                audioFile = "";
                recordAudioLayout.setVisibility(View.GONE);
                audioBtn.setVisibility(View.VISIBLE);
            }
        });

        progressBarHandler = new Handler();

        recordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (startRecording()) {
                    recordBtn.setVisibility(View.GONE);
                    stopBtn.setVisibility(View.VISIBLE);
                }
            }
        });

        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (stopRecording()) {
                    initAudio();
                    stopBtn.setVisibility(View.GONE);
                    redoBtn.setVisibility(View.VISIBLE);
                } else {
                    stopBtn.setVisibility(View.GONE);
                    recordBtn.setVisibility(View.VISIBLE);
                }
            }
        });

        playBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (audioReady) {
                    playAudio();
                    playBtn.setVisibility(View.GONE);
                    pauseBtn.setVisibility(View.VISIBLE);
                }
            }
        });

        pauseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pauseAudio();
                playBtn.setVisibility(View.VISIBLE);
                pauseBtn.setVisibility(View.GONE);
            }
        });

        redoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressBarHandler.removeCallbacksAndMessages(null);
                progressTextCurrent.setText(getString(R.string.default_start_time));
                progressTextDuration.setText(getString(R.string.default_maximum_time));
                progressBar.setProgress(0);
                mediaPlayer.release();
                mediaPlayer = null;
                audioFile = "";
                audioReady = false;
                isPlaying = false;

                recordBtn.setVisibility(View.VISIBLE);
                playBtn.setVisibility(View.GONE);
                pauseBtn.setVisibility(View.GONE);
                redoBtn.setVisibility(View.GONE);
            }
        });

        setHideKeyboard(findViewById(R.id.parent_layout));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_next, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.btn_next:
                if (isRecording) {
                    Toast.makeText(getApplicationContext(), "請先完成錄音", Toast.LENGTH_LONG).show();
                    return true;
                }
                Intent intent = new Intent(CheckinActivity.this, LocationChooseActivity.class);
                intent.putExtra("description", descriptionEdit.getText().toString().trim());
                intent.putExtra("photo", photoFile);
                intent.putExtra("audio", audioFile);
                startActivityForResult(intent, REQUEST_CODE_CHECKIN_FINISH);
                return true;
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                // /data/user/0/nctu.cs.cgv.itour/cache/cropped1795714260.jpg
                // getCacheDir()
                String path = result.getUri().getPath();
                photoFile = path.substring(path.lastIndexOf("/") + 1);
//                Bitmap bitmap = BitmapFactory.decodeFile(path);
//                photoFile = path.substring(path.lastIndexOf("/") + 1);
//                pickedPhoto.setImageBitmap(bitmap);

                try {
                    File compressedImageFile = new Compressor(this).compressToFile(new File(path));
                    moveFile(getCacheDir().toString() + "/image", photoFile, getCacheDir().toString());
                    Glide.with(this)
                            .load(new File(result.getUri().getPath()))
                            .into(pickedPhoto);
                    photoBtn.setVisibility(View.GONE);
                    pickedPhotoLayout.setVisibility(View.VISIBLE);
                } catch (IOException e) {
                    Toast.makeText(this, R.string.error_image_file, Toast.LENGTH_SHORT).show();
                }
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
        if (requestCode == REQUEST_CODE_CHECKIN_FINISH && resultCode == RESULT_CODE_CHECKIN_FINISH) {
            finish();
        }
    }

    private boolean startRecording() {
        audioFile = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime()) + ".mp4";
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setOutputFile(getCacheDir().toString() + "/" + audioFile);
        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            // start timer
            timeTick = 0;
            countDownTimer.start();
            // set flag
            isRecording = true;

            return true;
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), getString(R.string.toast_audio_recorder_prepare_failed), Toast.LENGTH_SHORT).show();
            audioFile = "";
            // stop recording
            mediaRecorder.release();
            mediaRecorder = null;
            // stop timer
            countDownTimer.cancel();
            // set flag
            isRecording = false;

            return false;
        }
    }

    private boolean stopRecording() {
        // stop recording
        try {
            mediaRecorder.stop();
        } catch (RuntimeException stopException) {
            Toast.makeText(getApplicationContext(), getString(R.string.toast_audio_recorder_stop_failed), Toast.LENGTH_SHORT).show();
            audioFile = "";
            return false;
        } finally {
            mediaRecorder.release();
            mediaRecorder = null;
            countDownTimer.cancel();
            // set flag
            isRecording = false;
            // init progress bar
            progressTextCurrent.setText(getString(R.string.default_start_time));
            progressTextDuration.setText(getString(R.string.default_maximum_time));
            progressBar.setProgress(0);
        }

        return true;
    }

    private void initAudio() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                progressBarHandler.removeCallbacksAndMessages(null);
                initAudio();
            }
        });
        try {
            mediaPlayer.setDataSource(getCacheDir().toString() + "/" + audioFile);
            mediaPlayer.prepare();

            String str = String.format("%d:%02d", mediaPlayer.getDuration() / 1000, (mediaPlayer.getDuration() % 1000) * 60 / 1000);
            progressTextDuration.setText(str);

            progressBarRunnable = new Runnable() {
                @Override
                public void run() {
                    if (isPlaying && mediaPlayer != null) {
                        progressBar.setProgress(mediaPlayer.getCurrentPosition() * 100 / mediaPlayer.getDuration());
                        String str = String.format("%d:%02d", mediaPlayer.getCurrentPosition() / 1000, (mediaPlayer.getCurrentPosition() % 1000) * 60 / 1000);
                        progressTextCurrent.setText(str);
                    }
                    progressBarHandler.postDelayed(this, 100);
                }
            };
            progressBarRunnable.run();
        } catch (IOException e) {
            e.printStackTrace();
        }

        audioReady = true;
        isPlaying = false;
        pauseBtn.setVisibility(View.GONE);
        playBtn.setVisibility(View.VISIBLE);
    }

    private void playAudio() {
        mediaPlayer.start();
        isPlaying = true;
    }

    private void pauseAudio() {
        mediaPlayer.pause();
        isPlaying = false;
    }

    public void setHideKeyboard(View view) {

        // Set up touch listener for non-text box views to hide keyboard.
        if (!(view instanceof EditText)) {
            view.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    hideSoftKeyboard(CheckinActivity.this);
                    return false;
                }
            });
        }

        //If a layout container, iterate over children and seed recursion.
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                View innerView = ((ViewGroup) view).getChildAt(i);
                setHideKeyboard(innerView);
            }
        }
    }

    private void checkPermission() {
        int micPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);

        if (micPermission != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, MIC_PERMISSION_REQUEST);
        } else {
            micAvailable = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MIC_PERMISSION_REQUEST:
                if (grantResults.length > 0) {
                    boolean micPermission = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                    if (micPermission) {
                        micAvailable = true;
                    }
                }
                break;
        }
    }
}
