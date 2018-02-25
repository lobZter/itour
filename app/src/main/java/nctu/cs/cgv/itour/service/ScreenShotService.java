package nctu.cs.cgv.itour.service;

import android.app.Service;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.view.WindowManager;

import com.google.firebase.auth.FirebaseAuth;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import java.io.File;
import java.io.FileNotFoundException;

import cz.msebera.android.httpclient.Header;
import nctu.cs.cgv.itour.custom.MyImageReader;

import static nctu.cs.cgv.itour.MyApplication.APPServerURL;
import static nctu.cs.cgv.itour.MyApplication.imageLogPath;
import static nctu.cs.cgv.itour.MyApplication.logFlag;

public class ScreenShotService extends Service {

    private static final String TAG = "ScreenShotService";

    final HandlerThread handlerThread = new HandlerThread(TAG, android.os.Process.THREAD_PRIORITY_BACKGROUND);
    final HandlerThread loopThread = new HandlerThread(TAG + "Loop", android.os.Process.THREAD_PRIORITY_BACKGROUND);
    private Handler handler, loopHandler;
    private MediaProjection projection;
    private VirtualDisplay virtualDisplay;
    private MediaProjectionManager mediaProjectionManager;
    private WindowManager windowManager;
    private MyImageReader imageReader;
    private int resultCode;
    private Intent resultData;

    @Override
    public void onCreate() {
        super.onCreate();

        mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        loopThread.start();
        loopHandler = new Handler(loopThread.getLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null) {
            resultCode = intent.getIntExtra("resultCode", -1);
            resultData = intent.getParcelableExtra("resultData");

            File imageDir = new File(imageLogPath);
            if (!imageDir.exists()) {
                imageDir.mkdirs();
            }

            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    startCapture();
                    loopHandler.postDelayed(this, 3000);
                }
            };
            loopHandler.post(runnable);
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopCapture();

        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void stopCapture() {
        if (projection != null) {
            projection.stop();
            virtualDisplay.release();
            imageReader.close();
            imageReader = null;
            virtualDisplay = null;
            projection = null;
        }
    }

    private void startCapture() {

        projection = mediaProjectionManager.getMediaProjection(resultCode, resultData);
        projection.registerCallback(new MediaProjection.Callback() {
            @Override
            public void onStop() {
                if (virtualDisplay != null) virtualDisplay.release();
            }
        }, handler);

        imageReader = new MyImageReader(this);

        virtualDisplay = projection.createVirtualDisplay(
                "itourVirtualDisplay",
                imageReader.getWidth(),
                imageReader.getHeight(),
                getResources().getDisplayMetrics().densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                imageReader.getSurface(),
                null,
                handler);
    }

    public WindowManager getWindowManager() {
        return windowManager;
    }

    public Handler getHandler() {
        return handler;
    }

    public void screenShotLog(String filePath) {
        if (!logFlag || FirebaseAuth.getInstance().getCurrentUser() == null)
            return;

        // upload files to app server
        AsyncHttpClient client = new AsyncHttpClient();
        String url = APPServerURL + "/screenShotLog";
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
