package nctu.cs.cgv.itour.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import nctu.cs.cgv.itour.R;

public class AudioCheckinActivity extends AppCompatActivity {

    private static final String TAG = "AudioCheckinActivity";
    // mediaRecorder
    private boolean audioReady = false;
    private boolean isPlaying = false;
    private boolean isRecording = false;
    private MediaRecorder mediaRecorder;
    private MediaPlayer mediaPlayer;
    private String filename = null;
    // UI references
    private ProgressBar progressBar;
    private TextView progressTextCurrent;
    private TextView progressTextDuration;
    private Button recordBtn;
    private Button stopBtn;
    private Button playBtn;
    private Button pauseBtn;
    private Button redoBtn;

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermission();
        } else {
            setView();
        }
    }

    private void setView() {

        progressBarHandler = new Handler();

        // set view
        progressBar = (ProgressBar) findViewById(R.id.progressbar);
        progressTextCurrent = (TextView) findViewById(R.id.tv_progress_current);
        progressTextDuration = (TextView) findViewById(R.id.tv_progress_duration);
        recordBtn = (Button) findViewById(R.id.btn_record);
        stopBtn = (Button) findViewById(R.id.btn_stop);
        playBtn = (Button) findViewById(R.id.btn_play);
        pauseBtn = (Button) findViewById(R.id.btn_pause);
        redoBtn = (Button) findViewById(R.id.btn_redo);

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
                filename = null;
                audioReady = false;
                recordBtn.setVisibility(View.VISIBLE);
                playBtn.setVisibility(View.GONE);
                pauseBtn.setVisibility(View.GONE);
                redoBtn.setVisibility(View.GONE);
            }
        });
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
                if (filename == null) {
                    Toast.makeText(getApplicationContext(), "無錄音音檔", Toast.LENGTH_LONG).show();
                    return true;
                }
                if (isRecording) {
                    Toast.makeText(getApplicationContext(), "請先完成錄音", Toast.LENGTH_LONG).show();
                    return true;
                }
                Intent intent = new Intent(AudioCheckinActivity.this, LocationChooseActivity.class);
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
        filename = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime()) + ".mp4";
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setOutputFile(getExternalCacheDir().toString() + "/" + filename);
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
            mediaPlayer.setDataSource(getExternalCacheDir().toString() + "/" + filename);
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

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void checkPermission() {
        final int PERMISSIONS_MULTIPLE_REQUEST = 123;
        int micPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);

        if (micPermission != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_MULTIPLE_REQUEST);
        } else {
            setView();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        final int PERMISSIONS_MULTIPLE_REQUEST = 123;

        switch (requestCode) {
            case PERMISSIONS_MULTIPLE_REQUEST:
                if (grantResults.length > 0) {
                    boolean micPermission = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                    if(micPermission)
                    {
                        setView();
                    } else {
                        Toast.makeText(getApplicationContext(), "需要麥克風權限", Toast.LENGTH_LONG).show();
                        finish();
                    }
                }
                break;
        }
    }
}
