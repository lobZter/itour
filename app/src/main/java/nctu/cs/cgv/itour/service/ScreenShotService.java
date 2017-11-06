package nctu.cs.cgv.itour.service;

import android.app.Service;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaScannerConnection;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.WindowManager;

import java.io.File;
import java.io.FileOutputStream;

import nctu.cs.cgv.itour.custom.ImageTransmogrifier;

import static nctu.cs.cgv.itour.Utility.screenShotLog;

public class ScreenShotService extends Service {

    private static final String TAG = "ScreenShotService";

    private Handler handler;
    private Handler loopHandler;
    private MediaProjection projection;
    private VirtualDisplay virtualDisplay;
    private MediaProjectionManager mediaProjectionManager;
    private WindowManager windowManager;
    private ImageTransmogrifier imageTransmogrifier;
    private int resultCode;
    private Intent resultData;

    @Override
    public void onCreate() {
        super.onCreate();

        mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        final HandlerThread handlerThread = new HandlerThread(TAG, android.os.Process.THREAD_PRIORITY_BACKGROUND);
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        resultCode = intent.getIntExtra("resultCode", -1);
        resultData = intent.getParcelableExtra("resultData");

        projection = mediaProjectionManager.getMediaProjection(resultCode, resultData);

        final HandlerThread handlerThread = new HandlerThread(TAG + ".loopHandler");
        handlerThread.start();
        loopHandler = new Handler(handlerThread.getLooper());
        final Runnable runnable = new Runnable() {
            public void run() {
                startCapture();
                loopHandler.postDelayed(this, 10000);
            }
        };
        loopHandler.post(runnable);

        return START_NOT_STICKY;
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

    private void stopCapture() {
        loopHandler.removeCallbacks(null);

        if (projection != null) {
            projection.stop();
            virtualDisplay.release();
            projection = null;
        }
    }

    private void startCapture() {
        imageTransmogrifier = new ImageTransmogrifier(this);

        virtualDisplay = projection.createVirtualDisplay(
                "itourVirtualDisplay",
                imageTransmogrifier.getWidth(),
                imageTransmogrifier.getHeight(),
                getResources().getDisplayMetrics().densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                imageTransmogrifier.getSurface(),
                null,
                handler);

        projection.registerCallback(new MediaProjection.Callback() {
            @Override
            public void onStop() {
                virtualDisplay.release();
            }
        }, handler);
    }

    public WindowManager getWindowManager() {
        return windowManager;
    }

    public Handler getHandler() {
        return handler;
    }

    public void processImage(final byte[] png) {
        screenShotLog(getApplicationContext(), png);
//        Thread thread = new Thread() {
//            @Override
//            public void run() {
//                File output = new File(getExternalFilesDir(null), "screenshot.png");
//
//                try {
//                    FileOutputStream fileOutputStream = new FileOutputStream(output);
//                    fileOutputStream.write(png);
//                    fileOutputStream.flush();
//                    fileOutputStream.getFD().sync();
//                    fileOutputStream.close();
//                } catch (Exception e) {
//                    Log.e(TAG, "Exception writing out screenshot", e);
//                }
//
//                screenShotLog(output);
//            }
//        };
//        thread.start();
    }
}
