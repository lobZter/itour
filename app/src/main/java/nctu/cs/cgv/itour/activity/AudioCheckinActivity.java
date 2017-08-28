package nctu.cs.cgv.itour.activity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telecom.Call;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Timer;

import nctu.cs.cgv.itour.R;

import static nctu.cs.cgv.itour.MyApplication.audioPath;

public class AudioCheckinActivity extends AppCompatActivity {

    private static final String TAG = "AudioCheckinActivity";
    private String mapTag;
    private float lat = 0;
    private float lng = 0;
    // mediaRecorder
    private boolean isRecording = false;
    private boolean audioReady = false;
    private boolean isPlaying = false;
    private MediaRecorder mediaRecorder;
    private MediaPlayer mediaPlayer;
    private String filename = " ";
    // UI references
    private EditText locationEdit;
    private ProgressBar progressBar;
    private TextView progressTextCurrent;
    private TextView progressTextDuration;
    private RelativeLayout playBtn;
    private ImageView playBtnCircle;
    private ImageView playBtnIcon;
    private RelativeLayout recordBtn;
    private ImageView recordBtnIcon;

    private int timeTick = 0;
    private CountDownTimer countDownTimer;
    private Handler progressBarHandler;
    private Runnable progressBarRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_checkin);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("");
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_close_black_24dp);

        // Verify that the device has a mic first
        PackageManager packageManager = this.getPackageManager();
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)) {
            Toast.makeText(getApplicationContext(), "找不到麥克風QQ", Toast.LENGTH_LONG).show();
            finish();
        }

        // get information from previous activity
        Intent intent = getIntent();
        lat = intent.getFloatExtra("lat", 0);
        lng = intent.getFloatExtra("lng", 0);
        mapTag = intent.getStringExtra("mapTag");

        // set view
        locationEdit = (EditText) findViewById(R.id.et_location);
        progressBar = (ProgressBar) findViewById(R.id.progress);
        progressTextCurrent = (TextView) findViewById(R.id.tv_progress_current);
        progressTextDuration = (TextView) findViewById(R.id.tv_progress_duration);
//        playBtn = (RelativeLayout) findViewById(R.id.btn_play);
//        playBtnIcon = (ImageView) findViewById(R.id.btn_play_icon);
//        playBtnCircle = (ImageView) findViewById(R.id.btn_play_circle);
//        recordBtn = (RelativeLayout) findViewById(R.id.btn_record);
//        recordBtnIcon = (ImageView) findViewById(R.id.btn_record_icon);

//        recordBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (!isRecording)
//                    startRecording();
//                else
//                    stopRecording();
//            }
//        });
//
//        playBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (audioReady) {
//                    if (isPlaying)
//                        pauseAudio();
//                    else
//                        playAudio();
//                }
//            }
//        });

        progressBarHandler = new Handler();
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
                Intent intent = new Intent(AudioCheckinActivity.this, LocationChooseActivity.class);
                intent.putExtra("location", locationEdit.getText().toString().trim());
                intent.putExtra("description", "");
                intent.putExtra("filename", filename);
                intent.putExtra("type", "audio");
                startActivity(intent);
                return true;
            case android.R.id.home:
                finish();

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void startRecording() {

        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
            progressBarHandler.removeCallbacks(progressBarRunnable);
        }

        // TODO prevent name collision
        filename = audioPath + new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime()) + ".mp4";

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setOutputFile(filename);
        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
            return;
        }

        // set flags
        isRecording = true;
        isPlaying = false;
        audioReady = false;

        // set view
        playBtnIcon.setImageDrawable(getDrawable(R.drawable.ic_play_arrow_grey_700_48dp));
        playBtnCircle.setImageDrawable(getDrawable(R.drawable.circle_btn_grey_700));
        recordBtnIcon.setImageDrawable(getDrawable(R.drawable.ic_stop_red_a700_24dp));
        // set progress bar
        timeTick = 0;
        progressTextCurrent.setText("0:00");
        progressTextDuration.setText("6:00");
        countDownTimer = new CountDownTimer(6000, 60) {

            @Override
            public void onTick(long millisUntilFinished) {
                timeTick++;
                String str = String.format("%d:%02d", timeTick * 60 / 1000, ((timeTick * 60) % 1000) * 60 / 1000);
                progressTextCurrent.setText(str);
                progressBar.setProgress(timeTick * 100 / (6000 / 60));
            }

            @Override
            public void onFinish() {
                stopRecording();
                timeTick++;
                progressTextCurrent.setText("6:00");
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

        // set flags
        isRecording = false;
        audioReady = true;

        // set view
        playBtnIcon.setImageDrawable(getDrawable(R.drawable.ic_play_arrow_white_48dp));
        playBtnCircle.setImageDrawable(getDrawable(R.drawable.circle_btn_white));
        recordBtnIcon.setImageDrawable(getDrawable(R.drawable.circle_record));

        initAudio();
    }

    private void initAudio() {
        progressBar.setProgress(0);
        progressTextCurrent.setText("0:00");
        progressTextDuration.setText("0:00");

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            mediaPlayer.setDataSource(filename);
            mediaPlayer.prepare();
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    progressBarHandler.removeCallbacks(progressBarRunnable);

                    isPlaying = false;
                    playBtnIcon.setImageDrawable(getDrawable(R.drawable.ic_play_arrow_white_48dp));
                    initAudio();
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
    }

    private void playAudio() {
        mediaPlayer.start();

        isPlaying = true;
        playBtnIcon.setImageDrawable(getDrawable(R.drawable.ic_pause_white_48dp));
    }

    private void pauseAudio() {
        mediaPlayer.pause();

        isPlaying = false;
        playBtnIcon.setImageDrawable(getDrawable(R.drawable.ic_play_arrow_white_48dp));
    }
}
