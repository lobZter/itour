package nctu.cs.cgv.itour.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
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

import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import nctu.cs.cgv.itour.R;

import static nctu.cs.cgv.itour.Utility.hideSoftKeyboard;
import static nctu.cs.cgv.itour.Utility.moveFile;

public class CheckinActivity extends AppCompatActivity {

    private static final String TAG = "CheckinActivity";
    private static final int REQUEST_CODE = 123;
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

    // mediaRecorder
    private boolean micAvailable = false;
    private boolean audioReady = false;
    private boolean isPlaying = false;
    private boolean isRecording = false;
    private MediaRecorder mediaRecorder;
    private MediaPlayer mediaPlayer;
    private String audioFile = "";

    private int timeTick = 0;
    private CountDownTimer countDownTimer;
    private Handler progressBarHandler;
    private Runnable progressBarRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkin);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("");
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_close_black_24dp);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermission();
        }

        setView();
    }

    private void setView() {

        // Verify that the device has a mic first
        PackageManager packageManager = this.getPackageManager();
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)) {
            micAvailable = false;
        }

        // set view
        descriptionEdit = (EditText) findViewById(R.id.et_description);
        photoBtn = (RelativeLayout) findViewById(R.id.btn_photo);
        audioBtn = (RelativeLayout) findViewById(R.id.btn_audio);
        pickedPhotoLayout = (RelativeLayout) findViewById(R.id.picked_photo_layout);
        recordAudioLayout = (LinearLayout) findViewById(R.id.recode_audio_layout);
        cancelPhotoBtn = (ImageView) findViewById(R.id.btn_cancel_photo);
        cancelAudioBtn = (ImageView) findViewById(R.id.btn_cancel_audio);

        pickedPhoto = (ImageView) findViewById(R.id.picked_photo);

        progressBar = (ProgressBar) findViewById(R.id.progressbar);
        progressTextCurrent = (TextView) findViewById(R.id.tv_progress_current);
        progressTextDuration = (TextView) findViewById(R.id.tv_progress_duration);
        recordBtn = (Button) findViewById(R.id.btn_record);
        stopBtn = (Button) findViewById(R.id.btn_stop);
        playBtn = (Button) findViewById(R.id.btn_play);
        pauseBtn = (Button) findViewById(R.id.btn_pause);
        redoBtn = (Button) findViewById(R.id.btn_redo);

        photoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
                if(micAvailable) {
                    audioBtn.setVisibility(View.GONE);
                    recordAudioLayout.setVisibility(View.VISIBLE);
                } else {
                    Toast.makeText(getApplicationContext(), "找不到麥克風", Toast.LENGTH_LONG);
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
                startRecording();
                recordBtn.setVisibility(View.GONE);
                stopBtn.setVisibility(View.VISIBLE);
            }
        });

        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRecording();
                stopBtn.setVisibility(View.GONE);
                playBtn.setVisibility(View.VISIBLE);
                redoBtn.setVisibility(View.VISIBLE);
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
                progressBarHandler.removeCallbacks(progressBarRunnable);
                progressTextCurrent.setText("0:00");
                progressTextDuration.setText("10:00");
                progressBar.setProgress(0);
                mediaPlayer.release();
                mediaPlayer = null;
                audioFile = "";
                audioReady = false;
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
                startActivity(intent);
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
                String path = result.getUri().getPath();
                Bitmap bitmap = BitmapFactory.decodeFile(path);
                // /data/user/0/nctu.cs.cgv.itour/cache/cropped1795714260.jpg
                // getCacheDir()
                photoFile = path.substring(path.lastIndexOf("/") + 1);
                moveFile(getCacheDir().toString(), photoFile, getExternalCacheDir().toString());
                pickedPhoto.setImageBitmap(bitmap);
                photoBtn.setVisibility(View.GONE);
                pickedPhotoLayout.setVisibility(View.VISIBLE);
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }

    private void startRecording() {
        audioFile = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime()) + ".mp4";
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setOutputFile(getExternalCacheDir().toString() + "/" + audioFile);
        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
            return;
        }

        // set flag
        isRecording = true;

        // set progress bar
        timeTick = 0;
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
                stopRecording();
                progressTextCurrent.setText("10:00");
                progressBar.setProgress(100);
            }
        };
        countDownTimer.start();
    }

    private void stopRecording() {
        // stop recording
        mediaRecorder.stop();
        mediaRecorder.release();
        mediaRecorder = null;

        // stop timer
        countDownTimer.cancel();
        countDownTimer = null;

        isRecording = false;

        initAudio();
    }

    private void initAudio() {
        progressBar.setProgress(0);
        progressTextCurrent.setText("0:00");
        progressTextDuration.setText("0:00");

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            mediaPlayer.setDataSource(getExternalCacheDir().toString() + "/" + audioFile);
            mediaPlayer.prepare();
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    progressBarHandler.removeCallbacks(progressBarRunnable);
                    initAudio();
                    pauseBtn.setVisibility(View.GONE);
                    playBtn.setVisibility(View.VISIBLE);
                }
            });

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

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void checkPermission() {
        int micPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);

        if (micPermission != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_CODE);
        } else {
            micAvailable = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE:
                if (grantResults.length > 0) {
                    boolean micPermission = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                    if(micPermission)
                    {
                        micAvailable = true;
                    }
                }
                break;
        }
    }
}
