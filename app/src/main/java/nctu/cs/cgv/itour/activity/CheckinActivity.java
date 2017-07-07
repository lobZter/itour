package nctu.cs.cgv.itour.activity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
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

public class CheckinActivity extends AppCompatActivity {

    private static final String TAG = "CheckinActivity";
    private Boolean isRecording = false;
    private MediaRecorder mediaRecorder;
    private EditText locationEdit;
    private EditText descriptionEdit;
    private ImageButton recordBtn;
    private TextView recordFilename;
    private Button submitBtn;
    private ProgressBar progressBar;
    private String filename = " ";
    private double latitude = 0, longitude = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkin);

        // TODO intent pass lat lng
        Intent intent = getIntent();
        latitude = intent.getDoubleExtra("lat", 0);
        longitude = intent.getDoubleExtra("lng", 0);

        setView();
    }

    private void setView() {

        locationEdit = (EditText) findViewById(R.id.et_location);
        descriptionEdit = (EditText) findViewById(R.id.et_description);
        recordBtn = (ImageButton) findViewById(R.id.btn_record);
        recordFilename = (TextView) findViewById(R.id.tv_record_filename);
        submitBtn = (Button) findViewById(R.id.btn_submit);
        progressBar = (ProgressBar) findViewById(R.id.loading_circle);

        // Verify that the device has a mic first
        PackageManager packageManager = this.getPackageManager();
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)) {
            recordBtn.setEnabled(false);
            submitBtn.setEnabled(false);
            Toast.makeText(this, "找不到麥克風QQ", Toast.LENGTH_LONG).show();
        }

        recordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isRecording)
                    startAudioRecord();
                else
                    stopAudioRecord();
            }
        });

        submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                AsyncHttpClient client = new AsyncHttpClient();
                RequestParams params = new RequestParams();
                params.setForceMultipartEntityContentType(true);
                try {
                    params.put("checkIn-audio", new File(filename));
                    params.put("lat", latitude);
                    params.put("lng", longitude);
                    params.put("type", "audio");

                    client.post("https://itour-lobst3rd.c9users.io/upload", params, new AsyncHttpResponseHandler() {
                        @Override
                        public void onStart() {
                            progressBar.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                            progressBar.setVisibility(View.GONE);
                            finish();
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(CheckinActivity.this, "網路錯誤QQ", Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (FileNotFoundException e) {
                    Log.d(TAG, e.getMessage());
                }
            }
        });
    }

    private void startAudioRecord() {
        // TODO prevent name collision
        filename = audioPath + new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime()) + ".mp4";
        Log.d(TAG, filename);

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setOutputFile(filename);

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            recordBtn.setImageDrawable(getResources().getDrawable(R.drawable.ic_stop_black_24dp));
            recordFilename.setText("錄音中...");
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

        recordBtn.setImageDrawable(getResources().getDrawable(R.drawable.ic_play_arrow_black_24dp));
        recordFilename.setText(filename);
    }
}
