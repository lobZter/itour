package nctu.cs.cgv.itour.service;

import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
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

import static nctu.cs.cgv.itour.MyApplication.APPServerURL;
import static nctu.cs.cgv.itour.MyApplication.audioLogPath;
import static nctu.cs.cgv.itour.MyApplication.logFlag;

public class AudioFeedbackService extends Service {

    private static final String TAG = "AudioFeedbackService";
    private WindowManager windowManager;
    private View overlayView;
    private FloatingActionButton fab;
    private boolean isRecording = false;
    private boolean shouldClick = false;

    private String filePath = "";
    private MediaRecorder mediaRecorder;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        setTheme(R.style.AppTheme);

        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_layout, null);

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 100;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            params.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        windowManager.addView(overlayView, params);

        fab = overlayView.findViewById(R.id.fab);

        fab.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        shouldClick = true;

                        //remember the initial position.
                        initialX = params.x;
                        initialY = params.y;

                        //get the touch location
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();

                        return true;

                    case MotionEvent.ACTION_UP:
                        if (shouldClick)
                            fab.performClick();
                        return true;

                    case MotionEvent.ACTION_MOVE:

                        float diffX = Math.round(event.getRawX() - initialTouchX);
                        float diffY = Math.round(event.getRawY() - initialTouchY);

                        if (diffX > 0.0f || diffY > 0.0f)
                            shouldClick = false;

                        //Calculate the X and Y coordinates of the view.
                        params.x = initialX + (int) diffX;
                        params.y = initialY + (int) diffY;

                        //Update the layout with new X & Y coordinates
                        windowManager.updateViewLayout(overlayView, params);

                        return true;
                }
                return false;
            }
        });

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isRecording) {
                    areYouSureYouWantToStopRecord();
                } else {
                    areYouSureYouWantToRecord();
                }
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (overlayView != null)
            windowManager.removeView(overlayView);
    }

    private void areYouSureYouWantToRecord() {
        android.support.v7.app.AlertDialog alertDialog = new android.support.v7.app.AlertDialog.Builder(this)
                .setTitle(R.string.dialog_recording_title)
                .setMessage(R.string.dialog_recording_message)
                .setPositiveButton(R.string.dialog_positive_btn, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (startRecording()) {
                            fab.setImageResource(R.drawable.ic_stop_white_24dp);
                        }
                    }
                })
                .setNegativeButton(R.string.dialog_negative_btn, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).create();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        } else {
            alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        }

        alertDialog.show();
    }

    private void areYouSureYouWantToStopRecord() {
        android.support.v7.app.AlertDialog alertDialog = new android.support.v7.app.AlertDialog.Builder(this)
                .setTitle(R.string.dialog_recording_title)
                .setMessage(R.string.dialog_stop_recording_message)
                .setPositiveButton(R.string.dialog_positive_btn, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (stopRecording()) {
                            fab.setImageResource(R.drawable.ic_mic_white_24dp);
                            audioFeedbackLog();
                        }
                    }
                })
                .setNegativeButton(R.string.dialog_negative_btn, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).create();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        } else {
            alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        }
        alertDialog.show();
    }

    private boolean startRecording() {
        String filename = FirebaseAuth.getInstance().getCurrentUser().getUid() + "-";
        filename += new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
        filePath = audioLogPath + "/" + filename + ".mp4";
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setOutputFile(filePath);
        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            // set flag
            isRecording = true;
            Toast.makeText(getApplicationContext(), R.string.recording_start, Toast.LENGTH_SHORT).show();
            return true;
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), getString(R.string.toast_audio_recorder_prepare_failed), Toast.LENGTH_SHORT).show();
            // stop recording
            mediaRecorder.release();
            mediaRecorder = null;
            // set flag
            isRecording = false;
            filePath = "";
            return false;
        }
    }

    private boolean stopRecording() {
        // stop recording
        try {
            mediaRecorder.stop();
            Toast.makeText(getApplicationContext(), R.string.recording_finish, Toast.LENGTH_SHORT).show();
        } catch (RuntimeException stopException) {
            Toast.makeText(getApplicationContext(), getString(R.string.toast_audio_recorder_stop_failed), Toast.LENGTH_SHORT).show();
            filePath = "";
            return false;
        } finally {
            mediaRecorder.release();
            mediaRecorder = null;
            // set flag
            isRecording = false;
        }

        return true;
    }

    private void audioFeedbackLog() {
        if (!logFlag || FirebaseAuth.getInstance().getCurrentUser() == null)
            return;

        // upload files to app server
        AsyncHttpClient client = new AsyncHttpClient();
        String url = APPServerURL + "/audioFeedbackLog";
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String timeStamp = String.valueOf(System.currentTimeMillis() / 1000);
        RequestParams requestParams = new RequestParams();
        requestParams.put("username", FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
        requestParams.put("uid", uid);
        requestParams.put("timestamp", timeStamp);
        requestParams.setForceMultipartEntityContentType(true);
        try {
            File output = new File(filePath);
            requestParams.put("file", output);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        client.post(url, requestParams, new AsyncHttpResponseHandler() {

            @Override
            public void onStart() {
                // called before request is started
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                // called when response HTTP status is "200 OK"
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                // called when response HTTP status is "4XX" (eg. 401, 403, 404)
            }

            @Override
            public void onRetry(int retryNo) {
                // called when request is retried
            }
        });
    }
}
