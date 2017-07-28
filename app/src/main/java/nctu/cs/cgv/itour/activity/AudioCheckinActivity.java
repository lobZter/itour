package nctu.cs.cgv.itour.activity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import cz.msebera.android.httpclient.Header;
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
    private MediaRecorder mediaRecorder;
    private String filename = " ";
    // UI references
    private EditText locationEdit;
    private RelativeLayout playBtn;
    private ImageView playBtnIcon;
    private ImageView recordBtn;
    private ProgressBar progressBar;
    private TextView progressText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_checkin);

        // Verify that the device has a mic first
        PackageManager packageManager = this.getPackageManager();
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)) {
            Toast.makeText(getApplicationContext(), "找不到麥克風QQ", Toast.LENGTH_LONG).show();
            finish();
        }

        Intent intent = getIntent();
        lat = intent.getFloatExtra("lat", 0);
        lng = intent.getFloatExtra("lng", 0);
        mapTag = intent.getStringExtra("mapTag");

        locationEdit = (EditText) findViewById(R.id.et_location);
        progressBar = (ProgressBar) findViewById(R.id.progress_record);
        progressText = (TextView) findViewById(R.id.tv_progress);
        playBtn = (RelativeLayout) findViewById(R.id.btn_play);
        playBtnIcon = (ImageView) findViewById(R.id.btn_play_icon);
        recordBtn = (ImageView) findViewById(R.id.btn_record);

        recordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isRecording)
                    startAudioRecord();
                else
                    stopAudioRecord();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_submit, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()){
            case R.id.btn_submit:
                Intent intent = new Intent(AudioCheckinActivity.this, LocationChooseActivity.class);
                intent.putExtra("mapTag", mapTag);
                intent.putExtra("audioFileName", filename);
                startActivity(intent);
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void startAudioRecord() {
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
//            recordBtn.setImageDrawable(getResources().getDrawable(R.drawable.ic_stop_black_24dp));
            isRecording = true;
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
        }

    }

    private void stopAudioRecord() {
        mediaRecorder.stop();
        mediaRecorder.release();
        mediaRecorder = null;
        isRecording = false;

//        recordBtn.setImageDrawable(getResources().getDrawable(R.drawable.ic_play_arrow_black_24dp));
    }
}
